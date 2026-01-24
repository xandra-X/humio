const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(
      require("./firebase-service-account.json")
    ),
  });
}

module.exports = admin;
