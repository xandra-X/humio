const express = require("express");
const router = express.Router();
const pool = require("../db");
const requireAuth = require("../middleware/requireAuth");

// ---------------------------------------------------
// Helper: get employee_id from user_id
// ---------------------------------------------------
async function getEmployeeId(userId) {
  const [rows] = await pool.query(
    "SELECT employee_id FROM Employee WHERE user_id = ? LIMIT 1",
    [userId]
  );
  return rows.length ? rows[0].employee_id : null;
}

// ---------------------------------------------------
// GET leave balance (Employee App – 4 cards)
// ---------------------------------------------------
router.get("/balance", requireAuth(), async (req, res) => {
  try {
    const empId = await getEmployeeId(req.user.user_id);
    if (!empId) return res.status(404).json({ error: "Employee not found" });

    const TOTAL = {
      ANNUAL: 12,
      MEDICAL: 10,
      CASUAL: 7,
      UNPAID: 999,
    };

    const [rows] = await pool.query(
      `
      SELECT leave_type, IFNULL(SUM(days),0) used
      FROM LeaveRequest
      WHERE employee_id = ?
        AND status = 'APPROVED'
      GROUP BY leave_type
      `,
      [empId]
    );

    const map = {};
    rows.forEach((r) => (map[r.leave_type] = Number(r.used)));

    res.json({
      ANNUAL: {
        total: TOTAL.ANNUAL,
        used: map.ANNUAL || 0,
        remaining: TOTAL.ANNUAL - (map.ANNUAL || 0),
      },
      MEDICAL: {
        total: TOTAL.MEDICAL,
        used: map.MEDICAL || 0,
        remaining: TOTAL.MEDICAL - (map.MEDICAL || 0),
      },
      CASUAL: {
        total: TOTAL.CASUAL,
        used: map.CASUAL || 0,
        remaining: TOTAL.CASUAL - (map.CASUAL || 0),
      },
      UNPAID: {
        total: TOTAL.UNPAID,
        used: map.UNPAID || 0,
        remaining: TOTAL.UNPAID,
      },
    });
  } catch (err) {
    console.error("leave balance error:", err);
    res.status(500).json({ error: "Failed to load leave balance" });
  }
});

// ---------------------------------------------------
// POST leave request (Employee App submit)
// ---------------------------------------------------
router.post("/request", requireAuth(), async (req, res) => {
  try {
    const empId = await getEmployeeId(req.user.user_id);
    if (!empId) return res.status(404).json({ error: "Employee not found" });

    const { leave_type, start_date, end_date, days, reason } = req.body;

    if (!leave_type || !start_date || !end_date || !days) {
      return res.status(400).json({ error: "Missing fields" });
    }

    await pool.query(
      `
      INSERT INTO LeaveRequest
        (employee_id, leave_type, start_date, end_date, days, reason, status)
      VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
      `,
      [empId, leave_type, start_date, end_date, days, reason || null]
    );

    res.json({ success: true, message: "Leave request submitted" });
  } catch (err) {
    console.error("leave submit error:", err);
    res.status(500).json({ error: "Submit failed" });
  }
});
module.exports = router;
