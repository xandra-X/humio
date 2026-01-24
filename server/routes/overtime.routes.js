const express = require("express");
const router = express.Router();
const pool = require("../db");

/**
 * GET all overtime (HR view)
 */
router.get("/", async (req, res) => {
  try {
    const [rows] = await pool.query(`
      SELECT 
        o.overtime_id,
        o.overtime_date,
        o.hours,
        o.hourly_rate,
        COUNT(oe.employee_id) AS employee_count
      FROM Overtime o
      LEFT JOIN OvertimeEmployee oe ON oe.overtime_id = o.overtime_id
      GROUP BY o.overtime_id
      ORDER BY o.overtime_date DESC
    `);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to load overtime" });
  }
});

/**
 * GET employees assigned to overtime
 */
router.get("/:id/employees", async (req, res) => {
  try {
    const [rows] = await pool.query(
      `
      SELECT 
        e.employee_id,
        e.employee_code,
        u.full_name
      FROM OvertimeEmployee oe
      JOIN Employee e ON e.employee_id = oe.employee_id
      JOIN User u ON u.user_id = e.user_id
      WHERE oe.overtime_id = ?
    `,
      [req.params.id]
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: "Failed to load assigned employees" });
  }
});

/**
 * CREATE overtime
 */
router.post("/", async (req, res) => {
  const { overtime_date, hours, hourly_rate } = req.body;
  const created_by = req.user?.user_id || 1; // fallback for now

  try {
    const [result] = await pool.query(
      `
      INSERT INTO Overtime (overtime_date, hours, hourly_rate, created_by)
      VALUES (?, ?, ?, ?)
    `,
      [overtime_date, hours, hourly_rate, created_by]
    );

    res.json({ overtime_id: result.insertId });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to create overtime" });
  }
});

/**
 * UPDATE overtime
 */
router.put("/:id", async (req, res) => {
  const { overtime_date, hours, hourly_rate } = req.body;

  try {
    await pool.query(
      `
      UPDATE Overtime
      SET overtime_date=?, hours=?, hourly_rate=?
      WHERE overtime_id=?
    `,
      [overtime_date, hours, hourly_rate, req.params.id]
    );
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: "Failed to update overtime" });
  }
});

/**
 * DELETE overtime (cascade removes employees)
 */
router.delete("/:id", async (req, res) => {
  try {
    await pool.query(`DELETE FROM Overtime WHERE overtime_id=?`, [
      req.params.id,
    ]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: "Failed to delete overtime" });
  }
});

/**
 * ASSIGN employees to overtime
 */
router.post("/:id/assign", async (req, res) => {
  const { employee_ids } = req.body;

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    // remove existing
    await conn.query(`DELETE FROM OvertimeEmployee WHERE overtime_id=?`, [
      req.params.id,
    ]);

    for (const empId of employee_ids) {
      await conn.query(
        `
        INSERT INTO OvertimeEmployee (overtime_id, employee_id)
        VALUES (?, ?)
      `,
        [req.params.id, empId]
      );
    }

    await conn.commit();
    res.json({ success: true });
  } catch (err) {
    await conn.rollback();
    res.status(500).json({ error: "Assignment failed" });
  } finally {
    conn.release();
  }
});

module.exports = router;
