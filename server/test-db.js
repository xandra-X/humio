// server/test-db.js
require("dotenv").config();
const pool = require("./db");

(async () => {
  try {
    const [rows] = await pool.query("SELECT 1+1 AS ok");
    console.log("DB ok", rows); // e.g. [{ok: 2}]
    process.exit(0);
  } catch (err) {
    console.error("DB connect error:", err.message || err);
    process.exit(1);
  }
})();
