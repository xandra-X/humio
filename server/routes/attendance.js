// server/routes/attendance.js
const express = require("express");
const pool = require("../db");
const router = express.Router();
const requireAuth = require("../middleware/requireAuth");

// GET /api/attendance/overview - company/department overview for a date (default today)
router.get("/overview", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  try {
    const date = req.query.date || new Date().toISOString().slice(0, 10);

    // department-level aggregates - use COALESCE/IFNULL so empty joins give zeros
    const sql = `
      SELECT d.department_id, d.name,
        COUNT(e.employee_id) AS total_employees,
        SUM(IF(a.status='PRESENT',1,0)) AS present,
        SUM(IF(a.status='ABSENT',1,0)) AS absent,
        SUM(IF(a.status='LATE',1,0)) AS late
      FROM Department d
      LEFT JOIN Employee e ON e.department_id = d.department_id
      LEFT JOIN Attendance a ON a.employee_id = e.employee_id AND a.date = ?
      GROUP BY d.department_id, d.name
      ORDER BY d.name
    `;
    const [rows] = await pool.query(sql, [date]);

    // ensure numeric values and compute attendance_rate string
    const out = rows.map((r) => {
      const total = Number(r.total_employees) || 0;
      const present = Number(r.present) || 0;
      const absent = Number(r.absent) || 0;
      const late = Number(r.late) || 0;
      const attendance_rate = total
        ? ((present / total) * 100).toFixed(1) + "%"
        : "0.0%";
      return {
        department_id: r.department_id,
        name: r.name,
        total_employees: total,
        present,
        absent,
        late,
        attendance_rate,
      };
    });

    res.json(out);
  } catch (err) {
    console.error(
      "GET /api/attendance/overview error:",
      err && err.stack ? err.stack : err
    );
    res.status(500).json({ error: "server error" });
  }
});

module.exports = router;
