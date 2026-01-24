// server/db.js
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const mysql = require("mysql2/promise");

const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  port: process.env.DB_PORT ? Number(process.env.DB_PORT) : 3306,
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "hr_system",
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  decimalNumbers: true,
});

(async () => {
  try {
    const conn = await pool.getConnection();
    await conn.ping();
    conn.release();
    console.log(
      "DB pool: connected to",
      process.env.DB_NAME,
      "as",
      process.env.DB_USER
    );
  } catch (err) {
    console.warn(
      "DB pool: connection test failed. Check server/.env and MySQL status."
    );
  }
})();

module.exports = pool;
