// server/routes/admin.js
const express = require("express");
const router = express.Router();
const pool = require("../db");
const bcrypt = require("bcryptjs");
const requireAuth = require("../middleware/requireAuth");
const multer = require("multer");
const path = require("path");

// helper
function handleError(res, err) {
  console.error("Admin route error:", err && err.stack ? err.stack : err);
  return res.status(500).json({ error: "server error" });
}

/**
 * Multer setup for profile image upload
 */
const profileImageStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, path.join(__dirname, "..", "..", "uploads", "profile_images"));
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || "";
    const unique = Date.now() + "-" + Math.round(Math.random() * 1e9);
    cb(null, unique + ext.toLowerCase());
  },
});

function imageFileFilter(req, file, cb) {
  if (!file.mimetype || !file.mimetype.startsWith("image/")) {
    return cb(new Error("Only image files are allowed"), false);
  }
  cb(null, true);
}

const uploadProfile = multer({
  storage: profileImageStorage,
  fileFilter: imageFileFilter,
  limits: { fileSize: 2 * 1024 * 1024 }, // 2MB
});

/**
 * GET /api/admin/employees
 */
router.get("/employees", requireAuth(), async (req, res) => {
  const limit = parseInt(req.query.limit, 10) || 200;
  const offset = parseInt(req.query.offset, 10) || 0;
  const q = (req.query.q || "").trim();

  try {
    let sql = `SELECT u.user_id,
                  u.username,
                  u.email,
                  u.full_name,
                  u.user_type,
                  u.profile_image,
                  e.employee_id,
                  e.employee_code,
                  e.hire_date,
                  e.job_title,
                  e.salary,
                  e.manager_id,
                  e.department_id,
                  d.name AS department_name
           FROM \`User\` u
           LEFT JOIN Employee e ON e.user_id = u.user_id
           LEFT JOIN Department d ON d.department_id = e.department_id`;

    const params = [];

    if (q) {
      sql += ` WHERE u.username LIKE ? OR u.email LIKE ? OR u.full_name LIKE ? OR e.employee_code LIKE ?`;
      const like = `%${q}%`;
      params.push(like, like, like, like);
    }

    sql += ` ORDER BY e.employee_code ASC, u.user_id ASC LIMIT ? OFFSET ?`;
    params.push(limit, offset);

    const [rows] = await pool.query(sql, params);
    res.json(rows);
  } catch (err) {
    return handleError(res, err);
  }
});

/**
 * POST /api/admin/employees
 * multipart/form-data, optional profile_image
 */
router.post(
  "/employees",
  requireAuth(),
  uploadProfile.single("profile_image"),
  async (req, res) => {
    const body = req.body || {};

    const username = (body.username || "").trim();
    const email = (body.email || "").trim();
    const full_name = body.full_name ? body.full_name.trim() : null;
    const user_type = body.user_type || "EMPLOYEE";
    const password = body.password;

    const employee_code = body.employee_code ? body.employee_code.trim() : null;
    const hire_date = body.hire_date || null;
    const job_title = body.job_title ? body.job_title.trim() : null;
    const salary = body.salary ? Number(body.salary) : 0;
    const manager_id = body.manager_id ? Number(body.manager_id) : null;
    const department_id = body.department_id
      ? Number(body.department_id)
      : null;

    if (!username || !email) {
      return res.status(400).json({ error: "username and email are required" });
    }

    const profileImagePath = req.file
      ? "/uploads/profile_images/" + req.file.filename
      : null;

    const conn = await pool.getConnection();
    try {
      await conn.beginTransaction();

      const [uExists] = await conn.query(
        "SELECT user_id FROM `User` WHERE username = ? LIMIT 1",
        [username]
      );
      if (uExists && uExists.length) {
        await conn.rollback();
        conn.release();
        return res.status(409).json({ error: "username already exists" });
      }
      const [eExists] = await conn.query(
        "SELECT user_id FROM `User` WHERE email = ? LIMIT 1",
        [email]
      );
      if (eExists && eExists.length) {
        await conn.rollback();
        conn.release();
        return res.status(409).json({ error: "email already exists" });
      }

      const plainPw = password || "Password123!";
      const hash = await bcrypt.hash(plainPw, 12);
      const [uRes] = await conn.query(
        "INSERT INTO `User` (username, password_hash, email, full_name, user_type, profile_image) VALUES (?, ?, ?, ?, ?, ?)",
        [username, hash, email, full_name, user_type, profileImagePath]
      );
      const userId = uRes.insertId;

      const [empRes] = await conn.query(
        `INSERT INTO Employee (user_id, employee_code, hire_date, job_title, salary, manager_id, department_id)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [
          userId,
          employee_code || null,
          hire_date || null,
          job_title || null,
          salary || 0,
          manager_id || null,
          department_id || null,
        ]
      );
      

      await conn.commit();
      conn.release();

      const [createdRows] = await pool.query(
        `SELECT u.user_id,
       u.username,
       u.email,
       u.full_name,
       u.user_type,
       u.profile_image,
       e.employee_id,
       e.employee_code,
       e.hire_date,
       e.job_title,
       e.salary,
       e.manager_id,
       e.department_id,
       d.name AS department_name
FROM \`User\` u
LEFT JOIN Employee e ON e.user_id = u.user_id
LEFT JOIN Department d ON d.department_id = e.department_id
WHERE u.user_id = ?`,
        [userId]
      );

      return res.status(201).json(createdRows[0]);
    } catch (err) {
      await conn.rollback().catch(() => {});
      conn.release();
      if (
        (err && err.errno === 1452) ||
        (err && err.code === "ER_NO_REFERENCED_ROW")
      ) {
        return res.status(400).json({
          error: "Bad foreign key (manager_id or department_id does not exist)",
        });
      }
      return handleError(res, err);
    }
  }
);

/**
 * PUT /api/admin/employees/:employee_id
 * multipart/form-data, optional new profile_image
 */
router.put(
  "/employees/:employee_id",
  requireAuth(),
  uploadProfile.single("profile_image"),
  async (req, res) => {
    const eid = parseInt(req.params.employee_id, 10);
    if (!eid) return res.status(400).json({ error: "invalid employee id" });

    const {
      username,
      email,
      full_name,
      user_type,
      employee_code,
      hire_date,
      job_title,
      salary,
      manager_id,
      department_id,
    } = req.body || {};

    const profileImagePath = req.file
      ? "/uploads/profile_images/" + req.file.filename
      : null;

    const conn = await pool.getConnection();
    try {
      await conn.beginTransaction();

      const [empRows] = await conn.query(
        "SELECT employee_id, user_id FROM Employee WHERE employee_id = ? LIMIT 1",
        [eid]
      );
      if (!empRows || !empRows.length) {
        await conn.rollback();
        conn.release();
        return res.status(404).json({ error: "employee not found" });
      }
      const userId = empRows[0].user_id;

      // user updates
      const userFields = [];
      const userVals = [];
      if (username) {
        userFields.push("username = ?");
        userVals.push(username);
      }
      if (email) {
        userFields.push("email = ?");
        userVals.push(email);
      }
      if (full_name !== undefined) {
        userFields.push("full_name = ?");
        userVals.push(full_name);
      }
      if (user_type) {
        userFields.push("user_type = ?");
        userVals.push(user_type);
      }
      if (profileImagePath !== null) {
        userFields.push("profile_image = ?");
        userVals.push(profileImagePath);
      }

      if (userFields.length) {
        userVals.push(userId);
        await conn.query(
          `UPDATE \`User\` SET ${userFields.join(", ")} WHERE user_id = ?`,
          userVals
        );
      }

      // employee updates
      const empFields = [];
      const empVals = [];
      if (employee_code !== undefined) {
        empFields.push("employee_code = ?");
        empVals.push(employee_code);
      }
      if (hire_date !== undefined) {
        empFields.push("hire_date = ?");
        empVals.push(hire_date);
      }
      if (job_title !== undefined) {
        empFields.push("job_title = ?");
        empVals.push(job_title);
      }
      if (salary !== undefined) {
        empFields.push("salary = ?");
        empVals.push(salary);
      }
      if (manager_id !== undefined) {
        empFields.push("manager_id = ?");
        empVals.push(manager_id || null);
      }
      if (department_id !== undefined) {
        empFields.push("department_id = ?");
        empVals.push(department_id || null);
      }

      if (empFields.length) {
        empVals.push(eid);
        await conn.query(
          `UPDATE Employee SET ${empFields.join(", ")} WHERE employee_id = ?`,
          empVals
        );
      }
      await pool.query(
        `
  INSERT INTO AuditLog (entity_name, entity_id, action, performed_by, details)
  VALUES (?, ?, ?, ?, ?)
  `,
        [
          "Employee",
          eid,
          "EMPLOYEE_UPDATED",
          req.user.user_id,
          JSON.stringify({
            employee_id: eid,
            updated_fields: Object.keys(req.body || {}),
          }),
        ]
      );


      await conn.commit();
      conn.release();

      const [rows] = await pool.query(
        `SELECT u.user_id,
       u.username,
       u.email,
       u.full_name,
       u.user_type,
       u.profile_image,
       e.employee_id,
       e.employee_code,
       e.hire_date,
       e.job_title,
       e.salary,
       e.manager_id,
       e.department_id,
       d.name AS department_name
FROM \`User\` u
LEFT JOIN Employee e ON e.user_id = u.user_id
LEFT JOIN Department d ON d.department_id = e.department_id
WHERE e.employee_id = ?
`,
        [eid]
      );
      res.json(rows[0] || null);
    } catch (err) {
      await conn.rollback().catch(() => {});
      conn.release();
      if (
        (err && err.errno === 1452) ||
        (err && err.code === "ER_NO_REFERENCED_ROW")
      ) {
        return res.status(400).json({
          error: "Bad foreign key (manager_id or department_id does not exist)",
        });
      }
      return handleError(res, err);
    }
  }
);

/**
 * DELETE /api/admin/employees/:employee_id
 */
router.delete("/employees/:employee_id", async (req, res) => {
  const eid = parseInt(req.params.employee_id, 10);
  if (!eid) return res.status(400).json({ error: "invalid employee id" });

  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    const [empRows] = await conn.query(
      "SELECT user_id FROM Employee WHERE employee_id = ? LIMIT 1",
      [eid]
    );
    const empRow = empRows[0];
    if (!empRow) {
      await conn.rollback();
      conn.release();
      return res.status(404).json({ error: "employee not found" });
    }
    const userId = empRow.user_id;

    await conn.query("DELETE FROM Employee WHERE employee_id = ?", [eid]);

    if (userId) {
      await conn.query("DELETE FROM `User` WHERE user_id = ?", [userId]);
    }
    await pool.query(
      `
  INSERT INTO AuditLog (entity_name, entity_id, action, performed_by, details)
  VALUES (?, ?, ?, ?, ?)
  `,
      [
        "Employee",
        eid,
        "EMPLOYEE_DELETED",
        req.user.user_id,
        JSON.stringify({ employee_id: eid }),
      ]
    );


    await conn.commit();
    conn.release();

    res.json({ ok: true });
  } catch (err) {
    await conn.rollback().catch(() => {});
    conn.release();
    console.error("Delete employee error:", err);
    return res.status(500).json({ error: "server error" });
  }
});

module.exports = router;
