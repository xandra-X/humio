const express = require("express");
const router = express.Router();
const pool = require("../db");
const requireAuth = require("../middleware/requireAuth");
const admin = require("../service/fcm");

// ---------------------------------------------------
// GET leave requests (ALL / PENDING / APPROVED / REJECTED)
// ---------------------------------------------------
router.get("/leave", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  try {
    const { status } = req.query; // 👈 read filter from query

    let sql = `
      SELECT
        lr.leave_id,
        u.full_name,
        u.email,
        lr.leave_type,
        lr.start_date,
        lr.end_date,
        lr.days,
        lr.reason,
        lr.status,
        lr.submitted_at
      FROM LeaveRequest lr
      JOIN Employee e ON lr.employee_id = e.employee_id
      JOIN User u ON e.user_id = u.user_id
    `;

    const params = [];

    // ✅ filter only if status is provided
    if (status && status !== "ALL") {
      sql += " WHERE lr.status = ?";
      params.push(status);
    }

    sql += " ORDER BY lr.submitted_at DESC";

    const [rows] = await pool.query(sql, params);
    res.json(rows);

  } catch (err) {
    console.error("HR leave list error:", err);
    res.status(500).json({ error: "Failed to load leave requests" });
  }
});

// ---------------------------------------------------
// APPROVE / REJECT leave + PUSH notification
// ---------------------------------------------------
router.put(
  "/leave/:id/status",
  requireAuth(["HR", "MANAGER"]),
  async (req, res) => {
    try {
      const leaveId = req.params.id;
      const { status } = req.body;

      if (!["APPROVED", "REJECTED"].includes(status)) {
        return res.status(400).json({ error: "Invalid status" });
      }

      // 1️⃣ Update leave
      const [result] = await pool.query(
        `
  UPDATE LeaveRequest
  SET status = ?, approver_id = ?, updated_at = NOW()
  WHERE leave_id = ?
  `,
        [status, req.user.user_id, leaveId]
      );

      if (result.affectedRows === 0) {
        return res.status(404).json({ error: "Leave not found" });
      }

      // ✅ log only if update succeeded
      await pool.query(
        `
  INSERT INTO AuditLog (entity_name, entity_id, action, performed_by, details)
  VALUES (?, ?, ?, ?, ?)
  `,
        [
          "LeaveRequest",
          leaveId,
          status === "APPROVED" ? "LEAVE_APPROVED" : "LEAVE_REJECTED",
          req.user.user_id,
          JSON.stringify({ leave_id: leaveId, status }),
        ]
      );



      if (result.affectedRows === 0) {
        return res.status(404).json({ error: "Leave not found" });
      }

      // 2️⃣ Get FCM token
      const [[row]] = await pool.query(
        `
        SELECT d.fcm_token
        FROM LeaveRequest lr
        JOIN Employee e ON lr.employee_id = e.employee_id
        JOIN Device d ON d.user_id = e.user_id
        WHERE lr.leave_id = ?
        `,
        [leaveId]
      );

      // 3️⃣ Push notification
      if (row?.fcm_token) {
        await admin.messaging().send({
          token: row.fcm_token,
          notification: {
            title: "Leave Update",
            body: `Your leave request was ${status}`,
          },
        });
      }

      res.json({ success: true });
    } catch (err) {
      console.error("Leave approve error:", err);
      res.status(500).json({ error: "Failed to update leave status" });
    }
  }
);
// ---------------------------------------------------
// MONTHLY ATTENDANCE (for chart)
// ---------------------------------------------------
router.get(
  "/attendance/monthly",
  requireAuth(["HR", "MANAGER"]),
  async (req, res) => {
    try {
      const [rows] = await pool.query(`
        SELECT
          DAY(date) AS day,
          SUM(status='PRESENT') AS PRESENT,
          SUM(status='LATE') AS LATE,
          SUM(status='ABSENT') AS ABSENT,
          SUM(status='ON_LEAVE') AS ON_LEAVE
        FROM Attendance
        WHERE MONTH(date) = MONTH(CURDATE())
          AND YEAR(date) = YEAR(CURDATE())
        GROUP BY DAY(date)
        ORDER BY DAY(date)
      `);

      res.json(rows);
    } catch (err) {
      console.error("Monthly attendance error:", err);
      res.status(500).json({ error: "Failed to load monthly attendance" });
    }
  }
);

// ---------------------------------------------------
// MONTHLY LEAVE BY DEPARTMENT (for pie chart)
// ---------------------------------------------------
router.get(
  "/leave/monthly-by-department",
  requireAuth(["HR", "MANAGER"]),
  async (req, res) => {
    try {
      const [rows] = await pool.query(`
        SELECT
          d.name AS department,
          COUNT(*) AS count
        FROM LeaveRequest lr
        JOIN Employee e ON lr.employee_id = e.employee_id
        JOIN Department d ON e.department_id = d.department_id
        WHERE lr.status = 'APPROVED'
          AND MONTH(lr.start_date) = MONTH(CURDATE())
          AND YEAR(lr.start_date) = YEAR(CURDATE())
        GROUP BY d.department_id
      `);

      res.json(rows);
    } catch (err) {
      console.error("Monthly leave by department error:", err);
      res.status(500).json({ error: "Failed to load leave summary" });
    }
  }
);

// ---------------------------------------------------
// TODAY ATTENDANCE (table)
// ---------------------------------------------------
router.get(
  "/attendance/today",
  requireAuth(["HR", "MANAGER"]),
  async (req, res) => {
    try {
      const [rows] = await pool.query(`
        SELECT
          u.full_name,
          u.email,
          IFNULL(u.profile_image, '') AS profile_image,
          a.date,
          TIME(a.check_in) AS check_in,
          TIME(a.check_out) AS check_out,
          a.status,
          a.source
        FROM Attendance a
        JOIN Employee e ON a.employee_id = e.employee_id
        JOIN User u ON e.user_id = u.user_id
        WHERE a.date = CURDATE()
        ORDER BY u.full_name ASC
      `);

      res.json(rows);
    } catch (err) {
      console.error("Today attendance error:", err);
      res.status(500).json({ error: "Failed to load today attendance" });
    }
  }
);
module.exports = router;
