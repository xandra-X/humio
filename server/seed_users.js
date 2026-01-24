// server/seed_users.js
require("dotenv").config({ path: require("path").join(__dirname, ".env") });
const pool = require("./db");
const bcrypt = require("bcryptjs");

async function run() {
  try {
    await pool.query("USE ??", [process.env.DB_NAME || "hr_system"]);

    const pw = "PAssword123!";
    const hashed = await bcrypt.hash(pw, 12);

    // manager user (email xanshock.design@gmail.com)
    const [exists] = await pool.query(
      "SELECT user_id FROM `User` WHERE email = ? LIMIT 1",
      ["xanshock.design@gmail.com"]
    );
    if (exists.length) {
      console.log("Manager user already exists, skipping.");
    } else {
      const [r] = await pool.query(
        "INSERT INTO `User` (username, password_hash, email, full_name, user_type) VALUES (?, ?, ?, ?, ?)",
        [
          "manager",
          hashed,
          "xanshock.design@gmail.com",
          "Admin Manager",
          "MANAGER",
        ]
      );
      console.log("Inserted manager user id", r.insertId);
      await pool.query(
        "INSERT INTO Employee (user_id, employee_code) VALUES (?, ?)",
        [r.insertId, "MGR001"]
      );
      console.log("Inserted manager employee row");
    }

    // a sample Department (IT)
    const [d] = await pool.query(
      "SELECT department_id FROM Department WHERE name = ? LIMIT 1",
      ["IT"]
    );
    if (!d.length) {
      const [dep] = await pool.query(
        "INSERT INTO Department (name, location) VALUES (?, ?)",
        ["IT", "Head Office"]
      );
      console.log("Inserted Department IT id", dep.insertId);
    } else {
      console.log("Department IT exists");
    }

    console.log(
      "Seed complete. Manager login: email xanshock.design@gmail.com password:",
      pw
    );
    process.exit(0);
  } catch (err) {
    console.error("Seed error:", err);
    process.exit(1);
  }
}

run();
