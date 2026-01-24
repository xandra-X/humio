// const admin = require("firebase-admin");

// if (!admin.apps.length) {
//   admin.initializeApp({
//     credential: admin.credential.cert(
//       require("./firebase-service-account.json")
//     ),
//   });
// }

// module.exports = admin;

let admin = null;

try {
  const firebaseAdmin = require("firebase-admin");
  const serviceAccount = require("./firebase-service-account.json");

  firebaseAdmin.initializeApp({
    credential: firebaseAdmin.credential.cert(serviceAccount),
  });

  admin = firebaseAdmin;
  console.log("✅ Firebase admin initialized");
} catch (err) {
  console.warn("⚠️ Firebase disabled:", err.message);
}

module.exports = admin;
