// server/routes/password_reset.js
const express = require("express");
const router = express.Router();
const pool = require("../db");
const crypto = require("crypto");
const bcrypt = require("bcryptjs");
const nodemailer = require("nodemailer");
require("dotenv").config();

const OTP_TTL_MINUTES = parseInt(process.env.OTP_TTL_MINUTES || "15", 10);

// configure nodemailer transporter (set env vars)
function getTransporter() {
  // Example: use SMTP credentials in .env: SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS
  if (!process.env.SMTP_HOST) {
    // fallback: use console logger transport (no email)
    return {
      sendMail: async (opts) => {
        console.log("Email (dev) send:", opts);
        return Promise.resolve();
      },
    };
  }
  return nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: Number(process.env.SMTP_PORT || 587),
    secure: process.env.SMTP_SECURE === "true",
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
    },
  });
}

async function sendOtpEmail(email, otp) {
  const transporter = getTransporter();
  const mail = {
    from: process.env.SMTP_FROM || "no-reply@example.com",
    to: email,
    subject: "Your password reset code",
    text: `Your password reset code: ${otp}\nThis code expires in ${OTP_TTL_MINUTES} minutes.`,
    html: `<p>Your password reset code: <strong>${otp}</strong></p><p>This code expires in ${OTP_TTL_MINUTES} minutes.</p>`,
  };
  await transporter.sendMail(mail);
}

// POST /api/auth/forgot
router.post("/forgot", async (req, res) => {
  const email = ((req.body && req.body.email) || "").trim().toLowerCase();
  if (!email) return res.status(400).json({ error: "email required" });

  try {
    const [rows] = await pool.query(
      "SELECT user_id FROM `User` WHERE LOWER(email) = ? LIMIT 1",
      [email]
    );
    if (!rows || rows.length === 0) {
      // don't reveal: send 404? Your UI expects Not Found. We'll return 404 with friendly message.
      return res.status(404).json({ error: "Not Found" });
    }
    const userId = rows[0].user_id;

    // generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    // expiry
    const expiresAt = new Date(Date.now() + OTP_TTL_MINUTES * 60 * 1000);

    // insert into password_reset_tokens
    await pool.query(
      `INSERT INTO password_reset_tokens (user_id, otp_code, expires_at, used, created_at)
       VALUES (?, ?, ?, 0, NOW())`,
      [userId, otp, expiresAt]
    );

    // send mail (best-effort)
    try {
      await sendOtpEmail(email, otp);
    } catch (err) {
      console.warn(
        "Failed to send OTP email:",
        err && err.message ? err.message : err
      );
    }

    return res.json({ ok: true, message: "OTP sent" });
  } catch (err) {
    console.error("forgot error", err);
    return res.status(500).json({ error: "server error" });
  }
});

// POST /api/auth/verify-otp
router.post("/verify-otp", async (req, res) => {
  const email = ((req.body && req.body.email) || "").trim().toLowerCase();
  const otp = ((req.body && req.body.otp) || "").trim();
  if (!email || !otp)
    return res.status(400).json({ error: "email & otp required" });

  try {
    const [[u]] = await pool.query(
      "SELECT user_id FROM `User` WHERE LOWER(email) = ? LIMIT 1",
      [email]
    );
    if (!u) return res.status(404).json({ error: "Not Found" });

    const [tokens] = await pool.query(
      `SELECT id, otp_code, expires_at, used FROM password_reset_tokens
       WHERE user_id = ? ORDER BY created_at DESC LIMIT 5`,
      [u.user_id]
    );

    if (!tokens || tokens.length === 0) {
      return res.status(400).json({ error: "Invalid or expired code" });
    }

    // find matching unused token
    const found = tokens.find(
      (t) =>
        !t.used &&
        String(t.otp_code) === String(otp) &&
        new Date(t.expires_at) > new Date()
    );
    if (!found) {
      return res.status(400).json({ error: "Invalid or expired code" });
    }

    return res.json({ ok: true, message: "verified" });
  } catch (err) {
    console.error("verify-otp err", err);
    return res.status(500).json({ error: "server error" });
  }
});

// POST /api/auth/reset-password
router.post("/reset-password", async (req, res) => {
  const email = ((req.body && req.body.email) || "").trim().toLowerCase();
  const otp = ((req.body && req.body.otp) || "").trim();
  const newPw = ((req.body && req.body.new_password) || "").toString();
  if (!email || !otp || !newPw)
    return res
      .status(400)
      .json({ error: "email, otp & new_password required" });

  if (newPw.length < 8)
    return res.status(400).json({ error: "password too short" });

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    const [[u]] = await conn.query(
      "SELECT user_id FROM `User` WHERE LOWER(email) = ? LIMIT 1",
      [email]
    );
    if (!u) {
      await conn.rollback();
      conn.release();
      return res.status(404).json({ error: "Not Found" });
    }

    const [tokens] = await conn.query(
      `SELECT id, otp_code, expires_at, used FROM password_reset_tokens
       WHERE user_id = ? ORDER BY created_at DESC LIMIT 10`,
      [u.user_id]
    );

    const found = tokens.find(
      (t) =>
        !t.used &&
        String(t.otp_code) === String(otp) &&
        new Date(t.expires_at) > new Date()
    );
    if (!found) {
      await conn.rollback();
      conn.release();
      return res.status(400).json({ error: "Invalid or expired code" });
    }

    // mark token used
    await conn.query("UPDATE password_reset_tokens SET used = 1 WHERE id = ?", [
      found.id,
    ]);

    // hash password and update User
    const hash = await bcrypt.hash(newPw, 12);
    await conn.query("UPDATE `User` SET password_hash = ? WHERE user_id = ?", [
      hash,
      u.user_id,
    ]);

    // optional: revoke refresh tokens, etc. (not included)

    await conn.commit();
    conn.release();
    return res.json({ ok: true });
  } catch (err) {
    await conn.rollback().catch(() => {});
    conn.release();
    console.error("reset-password err", err);
    return res.status(500).json({ error: "server error" });
  }
});

module.exports = router;
