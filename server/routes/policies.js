// server/routes/policies.js
const express = require("express");
const router = express.Router();
const pool = require("../db");
const requireAuth = require("../middleware/requireAuth");

// GET /api/policies
router.get("/", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  try {
    const [rows] = await pool.query(
      "SELECT * FROM `Policy` ORDER BY created_at DESC"
    );
    res.json(rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// POST /api/policies
router.post("/", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const { title, category, description, effective_date } = req.body || {};
  if (!title) return res.status(400).json({ error: "title required" });
  try {
    const created_by = req.user && req.user.user_id ? req.user.user_id : null;
    const [r] = await pool.query(
      "INSERT INTO `Policy` (title, category, description, effective_date, created_by) VALUES (?, ?, ?, ?, ?)",
      [
        title,
        category || null,
        description || null,
        effective_date || null,
        created_by,
      ]
    );
    const [rows] = await pool.query(
      "SELECT * FROM `Policy` WHERE policy_id = ?",
      [r.insertId]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// PUT /api/policies/:id
router.put("/:id", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);
  if (!id) return res.status(400).json({ error: "invalid id" });
  const fields = [
    "title",
    "category",
    "description",
    "effective_date",
    "status",
  ];
  const parts = [],
    params = [];
  fields.forEach((f) => {
    if (req.body[f] !== undefined) {
      parts.push(`${f} = ?`);
      params.push(req.body[f]);
    }
  });
  if (!parts.length) return res.status(400).json({ error: "no fields" });
  params.push(id);
  try {
    await pool.query(
      `UPDATE \`Policy\` SET ${parts.join(", ")} WHERE policy_id = ?`,
      params
    );
    const [rows] = await pool.query(
      "SELECT * FROM `Policy` WHERE policy_id = ?",
      [id]
    );
    res.json(rows[0] || null);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

// DELETE /api/policies/:id
router.delete("/:id", requireAuth(["HR", "MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);
  if (!id) return res.status(400).json({ error: "invalid id" });
  try {
    await pool.query("DELETE FROM `Policy` WHERE policy_id = ?", [id]);
    res.json({ ok: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "server error" });
  }
});

module.exports = router;
