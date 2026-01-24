// server/routes/reportInbox.js
const express = require("express");
const pool = require("../db");
const requireAuth = require("../middleware/requireAuth");

const router = express.Router();

/**
 * =========================================
 * POST /api/reports/inbox
 * HR / EMPLOYEE submit a report to manager
 * =========================================
 */
router.post("/inbox", requireAuth(["HR", "EMPLOYEE"]), async (req, res) => {
  const { title, message, type } = req.body || {};


  if (!title || !message) {
    return res.status(400).json({ error: "Title and message are required" });
  }

  try {
    const senderId = req.user.id;
    const senderRole = String(req.user.user_type || "")
      .trim()
      .toUpperCase();


    const finalTitle =
      type && ["ISSUE", "REQUEST", "COMPLAINT", "OTHER"].includes(type)
        ? `[${type}] ${title}`
        : title;

    await pool.query(
      `
  INSERT INTO ReportInbox
    (sender_id, sender_role, title, message)
  VALUES (?, ?, ?, ?)
  `,
      [senderId, senderRole, finalTitle, message]
    );


    res.status(201).json({ success: true, message: "Report submitted" });
  } catch (err) {
    console.error("POST /reports/inbox error:", err);
    res.status(500).json({ error: "Server error" });
  }
});

/**
 * =========================================
 * GET /api/reports/inbox
 * MANAGER view all reports
 * =========================================
 */
router.get("/inbox", requireAuth(), async (req, res) => {
  try {
    const [rows] = await pool.query(
      `
      SELECT
        r.report_id,
        r.title,
        r.message,
        r.sender_role,
        r.status,
        r.created_at,
        u.full_name AS sender_name
      FROM ReportInbox r
      LEFT JOIN User u ON u.user_id = r.sender_id
      ORDER BY r.created_at DESC
      `
    );

    res.json(rows);
  } catch (err) {
    console.error("GET /reports/inbox error:", err);
    res.status(500).json({ error: "Server error" });
  }
});

/**
 * =========================================
 * GET /api/reports/inbox/:id
 * MANAGER view single report (detail modal)
 * =========================================
 */
router.get(
  "/inbox/:id",
  requireAuth(["MANAGER", "ADMIN"]),
  async (req, res) => {
    const id = Number(req.params.id);
    if (!id) return res.status(400).json({ error: "Invalid report id" });

    try {
      const [rows] = await pool.query(
        `
      SELECT
        r.report_id,
        r.title,
        r.message,
        r.sender_role,
        r.status,
        r.created_at,
        u.full_name AS sender_name
      FROM ReportInbox r
      LEFT JOIN User u ON u.user_id = r.sender_id
      WHERE r.report_id = ?
      `,
        [id]
      );

      if (!rows.length) {
        return res.status(404).json({ error: "Report not found" });
      }

      // Mark as READ automatically
      await pool.query(
        `UPDATE ReportInbox SET status = 'READ' WHERE report_id = ?`,
        [id]
      );

      res.json(rows[0]);
    } catch (err) {
      console.error("GET /reports/inbox/:id error:", err);
      res.status(500).json({ error: "Server error" });
    }
  }
);

/**
 * =========================================
 * PUT /api/reports/inbox/:id/close
 * MANAGER close a report
 * =========================================
 */
router.put("/inbox/:id/close", requireAuth(["MANAGER"]), async (req, res) => {
  const id = Number(req.params.id);
  if (!id) return res.status(400).json({ error: "Invalid report id" });

  try {
    await pool.query(
      `UPDATE ReportInbox SET status = 'CLOSED' WHERE report_id = ?`,
      [id]
    );

    res.json({ success: true, message: "Report closed" });
  } catch (err) {
    console.error("PUT /reports/inbox/:id/close error:", err);
    res.status(500).json({ error: "Server error" });
  }
});

module.exports = router;
