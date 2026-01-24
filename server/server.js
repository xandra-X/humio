require("dotenv").config();
const express = require("express");
const cors = require("cors");
const path = require("path");
const fs = require("fs");

const app = express();

// -------------------- ENV WARNING --------------------
if (!process.env.JWT_SECRET) {
  console.warn("⚠️  Warning: JWT_SECRET is not set. Set it in production.");
}

// -------------------- MIDDLEWARE --------------------
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// -------------------- CORS --------------------
const clientOrigin = process.env.CLIENT_ORIGIN || "*";
app.use(
  cors({
    origin: clientOrigin,
    methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allowedHeaders: ["Content-Type", "Authorization", "X-User-Id"],
  })
);

// -------------------- DEV LOGGER --------------------
if (process.env.NODE_ENV !== "production") {
  app.use((req, res, next) => {
    console.log(`${req.method} ${req.path}`);
    next();
  });
}

// -------------------- STATIC FRONTEND --------------------
const publicPath = path.join(__dirname, "..", "web", "public");
app.use(express.static(publicPath));

// -------------------- UPLOADS --------------------
const uploadsRoot = path.join(__dirname, "..", "uploads");
const profileImageDir = path.join(uploadsRoot, "profile_images");

if (!fs.existsSync(uploadsRoot)) fs.mkdirSync(uploadsRoot);
if (!fs.existsSync(profileImageDir)) fs.mkdirSync(profileImageDir);

app.use("/uploads", express.static(uploadsRoot));

// -------------------- API ROUTES --------------------

// Auth
try {
  app.use("/api/auth", require("./routes/auth"));
} catch (err) {
  console.warn("⚠️ auth routes not found:", err.message);
}

// Password reset
try {
  app.use("/api/auth", require("./routes/password_reset"));
} catch (err) {
  console.warn("⚠️ password reset routes not found:", err.message);
}
app.use("/api/overtime", require("./routes/overtime.routes"));
app.use("/api/reports", require("./routes/reports"));       
app.use("/api/reports", require("./routes/reportInbox"));  

app.use("/api/shifts", require("./routes/shifts"));
try {
  app.use("/api/leave", require("./routes/leave.routes"));
  console.log("✅ leave routes loaded");
} catch (err) {
  console.warn("⚠️ leave.routes.js not found:", err.message);
}

try {
  app.use("/api/hr", require("./routes/hr.routes"));
  console.log("✅ hr routes loaded");
} catch (err) {
  console.warn("⚠️ hr.routes.js not found:", err.message);
}

try {
  app.use("/api/notifications", require("./routes/notification.routes"));
  console.log("✅ notification routes loaded");
} catch (err) {
  console.warn("⚠️ notification.routes.js not found:", err.message);
}

app.use("/api/dashboard", require("./routes/dashboard"));

const optionalRoutes = [
  { mount: "/api/shifts", path: "./routes/shifts" },
  { mount: "/api/admin", path: "./routes/admin" },
  { mount: "/api/departments", path: "./routes/departments" },
  { mount: "/api/policies", path: "./routes/policies" },
  { mount: "/api/attendance", path: "./routes/attendance" },
];

optionalRoutes.forEach((r) => {
  try {
    app.use(r.mount, require(r.path));
  } catch (err) {
    if (process.env.NODE_ENV !== "production") {
      console.log(`Notice: ${r.path} not found`);
    }
  }
});

app.get("/api/health", (req, res) =>
  res.json({ ok: true, env: process.env.NODE_ENV || "development" })
);

app.get("*", (req, res, next) => {
  if (req.path.startsWith("/api/")) return next();
  res.sendFile(path.join(publicPath, "index.html"), (err) => {
    if (err) next(err);
  });
});

app.use((err, req, res, next) => {
  console.error("Server error:", err?.stack || err);
  if (!res.headersSent) {
    res.status(500).json({ error: "Internal server error" });
  } else {
    next(err);
  }
});

const PORT = parseInt(process.env.PORT || "3000", 10);
app.listen(PORT, () => {
  console.log(`🚀 Server running at http://localhost:${PORT}`);
  console.log(` - static files: ${publicPath}`);
  console.log(` - uploads: ${uploadsRoot}`);
});
