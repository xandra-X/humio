const express = require("express");
const router = express.Router();
const pool = require("../db");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const path = require("path");

require("dotenv").config({
  path: path.join(__dirname, "..", ".env"),
});

function normalizeLogin(raw) {
  if (!raw) return "";
  const s = String(raw).trim();
  return s.includes("@") ? s.toLowerCase() : s;
}

router.post("/login", async (req, res) => {
  const { username, password } = req.body || {};

  if (!username || !password) {
    return res.status(400).json({ error: "username and password required" });
  }

  const loginId = normalizeLogin(username);

  try {
    const sql = `
      SELECT user_id, username, password_hash, user_type, full_name, email
      FROM \`User\`
      WHERE username = ? OR email = ?
      LIMIT 1
    `;

    const [rows] = await pool.query(sql, [loginId, loginId]);

    if (!rows.length) {
      return res.status(401).json({ error: "Invalid credentials" });
    }

    const user = rows[0];
    const ok = await bcrypt.compare(password, user.password_hash);

    if (!ok) {
      return res.status(401).json({ error: "Invalid credentials" });
    }

    const payload = {
      user_id: user.user_id,
      user_type: user.user_type,
      username: user.username,
    };

    const token = jwt.sign(payload, process.env.JWT_SECRET || "dev-secret", {
      expiresIn: process.env.JWT_EXP || "1h",
    });

    await pool.query("UPDATE `User` SET last_login = NOW() WHERE user_id = ?", [
      user.user_id,
    ]);

    res.json({
      access_token: token,
      user: {
        user_id: user.user_id,
        username: user.username,
        full_name: user.full_name,
        email: user.email,
        user_type: user.user_type,
      },
    });
  } catch (err) {
    console.error("Auth login error:", err);
    res.status(500).json({ error: "server error" });
  }
});

module.exports = router;
