const jwt = require("jsonwebtoken");
require("dotenv").config({
  path: require("path").join(__dirname, "..", ".env"),
});

/**
 * requireAuth(requiredRoles = [])
 * - requiredRoles: array of roles that are allowed, e.g. ['MANAGER','HR']
 */
module.exports = function requireAuth(requiredRoles = []) {
  return (req, res, next) => {
    const auth = req.headers.authorization || "";
    const token = auth.startsWith("Bearer ") ? auth.slice(7) : null;

    if (!token) {
      return res.status(401).json({ error: "Unauthorized" });
    }

    try {
      const payload = jwt.verify(token, process.env.JWT_SECRET || "dev-secret");

      // ✅ normalized user (USED EVERYWHERE)
      req.user = {
        id: payload.user_id,
        user_type: payload.user_type,
        username: payload.username,
      };

      if (Array.isArray(requiredRoles) && requiredRoles.length > 0) {
        const role = String(req.user.user_type || "")
          .trim()
          .toUpperCase();

        // normalize roles
        const roleMap = {
          ADMIN: "MANAGER",
          SUPERADMIN: "MANAGER",
        };

        const normalizedRole = roleMap[role] || role;

        if (requiredRoles.length && !requiredRoles.includes(normalizedRole)) {
          console.warn("Forbidden access:", {
            requiredRoles,
            userRole: role,
            normalizedRole,
          });
          return res.status(403).json({ error: "Forbidden" });
        }

      }

      next();
    } catch (err) {
      console.error("Auth middleware error:", err.message || err);
      return res.status(401).json({ error: "Invalid token" });
    }
  };
};
