// const admin = require("firebase-admin");

// if (!admin.apps.length) {
//   admin.initializeApp({
//     credential: admin.credential.cert(
//       require("./firebase-service-account.json")
//     ),
//   });
// }

// module.exports = admin;


const admin = require("firebase-admin");

let initialized = false;

try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    admin.initializeApp({
      credential: admin.credential.cert(
        JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)
      ),
    });
    initialized = true;
  }
} catch (err) {
  console.warn("⚠️ Firebase not initialized:", err.message);
}

module.exports = initialized ? admin : null;

