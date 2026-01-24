const express = require("express");
const router = express.Router();
const pool = require("../db");
const requireAuth = require("../middleware/requireAuth");

/**
 * GET all shifts
 */
router.get("/", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const [rows] = await pool.query("SELECT * FROM Shift ORDER BY name");
  res.json(rows);
});

router.get("/employees", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const [rows] = await pool.query(`
    SELECT 
      employee_id,
      employee_code,
      job_title,
      shift_name
    FROM Employee
    ORDER BY employee_code
  `);

  res.json(rows);
});

router.get("/assignments", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const [rows] = await pool.query(`
    SELECT 
      e.employee_id,
      u.full_name,
      s.shift_id,
      s.name AS shift_name,
      es.effective_from,
      s.start_time,
      s.end_time
    FROM EmployeeShift es
    JOIN Employee e ON e.employee_id = es.employee_id
    JOIN User u ON u.user_id = e.user_id
    JOIN Shift s ON s.shift_id = es.shift_id
    WHERE es.effective_to IS NULL
    ORDER BY u.full_name
  `);

  res.json(rows);
});
/**
 * CREATE shift
 */
router.post("/", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const { name, start_time, end_time, crosses_midnight } = req.body;

  if (!name || !start_time || !end_time)
    return res.status(400).json({ error: "Missing fields" });

  const [r] = await pool.query(
    `INSERT INTO Shift (name, start_time, end_time, crosses_midnight)
     VALUES (?, ?, ?, ?)`,
    [name, start_time, end_time, crosses_midnight ? 1 : 0]
  );

  const [[shift]] = await pool.query("SELECT * FROM Shift WHERE shift_id = ?", [
    r.insertId,
  ]);

  res.status(201).json(shift);
});

/**
 * UPDATE shift
 */
router.put("/:id", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);
  const { name, start_time, end_time, crosses_midnight } = req.body;

  const fields = [];
  const params = [];

  if (name !== undefined) {
    fields.push("name=?");
    params.push(name);
  }
  if (start_time !== undefined) {
    fields.push("start_time=?");
    params.push(start_time);
  }
  if (end_time !== undefined) {
    fields.push("end_time=?");
    params.push(end_time);
  }
  if (crosses_midnight !== undefined) {
    fields.push("crosses_midnight=?");
    params.push(crosses_midnight ? 1 : 0);
  }

  if (!fields.length) return res.status(400).json({ error: "No fields" });

  params.push(id);

  await pool.query(
    `UPDATE Shift SET ${fields.join(", ")} WHERE shift_id = ?`,
    params
  );

  const [[shift]] = await pool.query("SELECT * FROM Shift WHERE shift_id = ?", [
    id,
  ]);

  res.json(shift);
});

/**
 * DELETE shift (only if not assigned)
 */
router.delete("/:id", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);

  const [[used]] = await pool.query(
    `SELECT COUNT(*) AS cnt
     FROM EmployeeShift
     WHERE shift_id = ? AND effective_to IS NULL`,
    [id]
  );

  if (used.cnt > 0)
    return res.status(400).json({ error: "Shift is currently assigned" });

  await pool.query("DELETE FROM Shift WHERE shift_id = ?", [id]);
  res.json({ ok: true });
});

/**
 * ASSIGN shift to employee
 */
router.post("/assign", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const { employee_id, shift_id, effective_from } = req.body;

  if (!employee_id || !shift_id || !effective_from)
    return res.status(400).json({ error: "Missing fields" });

  const conn = await pool.getConnection();

  try {
    await conn.beginTransaction();

    // 1️⃣ Close previous shift
    await conn.query(
      `UPDATE EmployeeShift
       SET effective_to = DATE_SUB(?, INTERVAL 1 DAY)
       WHERE employee_id = ? AND effective_to IS NULL`,
      [effective_from, employee_id]
    );

    // 2️⃣ Insert new shift history
    await conn.query(
      `INSERT INTO EmployeeShift (employee_id, shift_id, effective_from)
       VALUES (?, ?, ?)`,
      [employee_id, shift_id, effective_from]
    );

    // 3️⃣ UPDATE Employee.shift_name (THIS IS OPTION 2)
    await conn.query(
      `UPDATE Employee
       SET shift_name = (
         SELECT name FROM Shift WHERE shift_id = ?
       )
       WHERE employee_id = ?`,
      [shift_id, employee_id]
    );

    await conn.commit();
    res.json({ success: true });
  } catch (err) {
    await conn.rollback();
    console.error(err);
    res.status(500).json({ error: "Assignment failed" });
  } finally {
    conn.release();
  }
});


module.exports = router;
