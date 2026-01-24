const express = require("express");
const router = express.Router();
const db = require("../db");
const requireAuth = require("../middleware/requireAuth");

// List notifications
router.get("/", requireAuth(), async (req, res) => {
  const [rows] = await db.query(
    `SELECT *
     FROM Notification
     WHERE recipient_user_id = ?
     ORDER BY created_at DESC`,
    [req.user.id]
  );

  res.json(rows);
});

// 🔴 Unread count
router.get("/unread-count", requireAuth(), async (req, res) => {
  const [[row]] = await db.query(
    `SELECT COUNT(*) AS count
     FROM Notification
     WHERE recipient_user_id = ? AND \`read\` = 0`,
    [req.user.id]
  );

  res.json({ count: row.count || 0 });
});

// Mark read
router.post("/:id/read", requireAuth(), async (req, res) => {
  await db.query(
    "UPDATE Notification SET `read` = 1 WHERE notification_id = ?",
    [req.params.id]
  );

  res.json({ success: true });
});

module.exports = router;
