const admin = require("firebase-admin");
const path = require("path");

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(
      require("../config/firebase-service-account.json")
    ),
    storageBucket: "humio-profile-images.appspot.com",
  });
}

const bucket = admin.storage().bucket();

module.exports = { admin, bucket };
