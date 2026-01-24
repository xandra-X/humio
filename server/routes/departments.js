// server/routes/departments.js
const express = require("express");
const router = express.Router();
const pool = require("../db");
const requireAuth = require("../middleware/requireAuth");

// GET /api/departments
router.get("/", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  try {
    const sql = `
      SELECT d.department_id, d.name, d.location, d.head_employee_id,
             e.employee_id, e.employee_code, u.full_name as head_name,
             (SELECT COUNT(*) FROM Employee WHERE department_id = d.department_id) AS employee_count
      FROM Department d
      LEFT JOIN Employee e ON e.employee_id = d.head_employee_id
      LEFT JOIN \`User\` u ON u.user_id = e.user_id
      ORDER BY d.name ASC
    `;
    const [rows] = await pool.query(sql);
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// POST /api/departments
router.post("/", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const { name, location } = req.body || {};
  if (!name) return res.status(400).json({ error: "name required" });
  try {
    const [r] = await pool.query(
      "INSERT INTO Department (name, location) VALUES (?, ?)",
      [name, location || null]
    );
    const [rows] = await pool.query(
      "SELECT * FROM Department WHERE department_id = ?",
      [r.insertId]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    if (err && err.code === "ER_DUP_ENTRY")
      return res.status(409).json({ error: "department already exists" });
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// PUT /api/departments/:id
router.put("/:id", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);
  const { name, location } = req.body || {};
  if (!id) return res.status(400).json({ error: "invalid id" });
  try {
    const parts = [];
    const params = [];
    if (name !== undefined) {
      parts.push("name = ?");
      params.push(name);
    }
    if (location !== undefined) {
      parts.push("location = ?");
      params.push(location);
    }
    if (!parts.length) return res.status(400).json({ error: "no fields" });
    params.push(id);
    await pool.query(
      `UPDATE Department SET ${parts.join(", ")} WHERE department_id = ?`,
      params
    );
    const [rows] = await pool.query(
      "SELECT * FROM Department WHERE department_id = ?",
      [id]
    );
    res.json(rows[0] || null);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// DELETE /api/departments/:id
router.delete("/:id", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);
  if (!id) return res.status(400).json({ error: "invalid id" });
  try {
    await pool.query("DELETE FROM Department WHERE department_id = ?", [id]);
    res.json({ ok: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// PUT /api/departments/:id/head - set department head
router.put("/:id/head", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);
  const { head_employee_id } = req.body || {};
  if (!id) return res.status(400).json({ error: "invalid id" });
  try {
    await pool.query(
      "UPDATE Department SET head_employee_id = ? WHERE department_id = ?",
      [head_employee_id || null, id]
    );
    const [rows] = await pool.query(
      "SELECT * FROM Department WHERE department_id = ?",
      [id]
    );
    res.json(rows[0] || null);
  } catch (err) {
    if (err && (err.errno === 1452 || err.code === "ER_NO_REFERENCED_ROW")) {
      return res
        .status(400)
        .json({ error: "bad foreign key for head_employee_id" });
    }
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// POST /api/departments/reassign - reassign employee to another department
router.post("/reassign", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const { employee_id, to_department_id } = req.body || {};
  if (!employee_id || !to_department_id)
    return res
      .status(400)
      .json({ error: "employee_id and to_department_id required" });
  try {
    await pool.query(
      "UPDATE Employee SET department_id = ? WHERE employee_id = ?",
      [to_department_id, employee_id]
    );
    const [rows] = await pool.query(
      "SELECT * FROM Employee WHERE employee_id = ?",
      [employee_id]
    );
    res.json(rows[0] || null);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

module.exports = router;
