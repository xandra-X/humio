const express = require("express");
const router = express.Router();
const pool = require("../db");
const requireAuth = require("../middleware/requireAuth");

/**
 * GET /api/dashboard/hr
 */
router.get(
  "/hr/recent-activities",
  requireAuth(["HR", "MANAGER"]),
  async (req, res) => {
    try {
      const [rows] = await pool.query(`
        SELECT
          a.action,
          a.entity_name,
          a.entity_id,
          a.created_at,
          u.full_name
        FROM AuditLog a
        LEFT JOIN User u ON a.performed_by = u.user_id
        ORDER BY a.created_at DESC
        LIMIT 20
      `);

      res.json(rows);
    } catch (err) {
      console.error("Recent activities error:", err);
      res.status(500).json({ error: "Failed to load activities" });
    }
  }
);

router.get("/hr", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  try {
    const [[emp]] = await pool.query("SELECT COUNT(*) AS total FROM Employee");
    const [[pendingLeave]] = await pool.query(
      "SELECT COUNT(*) AS total FROM LeaveRequest WHERE status = 'PENDING'"
    );
    const [[reports]] = await pool.query(
      "SELECT COUNT(*) AS total FROM Report"
    );

    res.json({
      totals: {
        employees: emp.total,
        pendingLeave: pendingLeave.total,
        reports: reports.total,
      },
    });
  } catch (err) {
    console.error("Dashboard error:", err);
    res.status(500).json({ error: "dashboard failed" });
  }
});
router.get(
  "/hr/new-employees-this-month",
  requireAuth(["HR", "MANAGER"]),
  async (req, res) => {
    try {
      const [rows] = await pool.query(`
        SELECT
          WEEK(hire_date, 1)
          - WEEK(DATE_SUB(hire_date, INTERVAL DAYOFMONTH(hire_date)-1 DAY), 1)
          + 1 AS week,
          COUNT(*) AS total
        FROM Employee
        WHERE hire_date IS NOT NULL
          AND MONTH(hire_date) = MONTH(CURDATE())
          AND YEAR(hire_date) = YEAR(CURDATE())
        GROUP BY week
        ORDER BY week
      `);

      res.json(rows);
    } catch (err) {
      console.error("New employees this month error:", err);
      res.status(500).json({ error: "Failed to load new employees chart" });
    }
  }
);
module.exports = router;
