const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();
const db = admin.database();

// ── Nodemailer transporter (Gmail SMTP) ─────────────────────────────────────
// Credentials are loaded from functions/.env (GMAIL_USER / GMAIL_PASS)
function createTransporter() {
  return nodemailer.createTransport({
    service: "gmail",
    auth: {
      user: process.env.GMAIL_USER,
      pass: process.env.GMAIL_PASS,
    },
  });
}

// ── Helper: generate 6-digit OTP ─────────────────────────────────────────────
function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// ── sendOtp ───────────────────────────────────────────────────────────────────
// POST { email }
// Checks that the email belongs to an admin, generates an OTP, stores it in
// Realtime DB under /otps/<sanitisedEmail> with a 10-minute expiry, then
// sends it via Gmail.
exports.sendOtp = functions.https.onCall(async (request) => {
  const email = (request.data.email || "").trim().toLowerCase();
  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required.");
  }

  // Verify admin exists
  const adminsSnap = await db.ref("admins").get();
  if (!adminsSnap.exists()) {
    throw new functions.https.HttpsError("not-found", "No admin accounts found.");
  }
  const admins = adminsSnap.val();
  const adminEntry = Object.entries(admins).find(
    ([, a]) => (a.email || "").toLowerCase() === email
  );
  if (!adminEntry) {
    throw new functions.https.HttpsError("not-found", "No admin account with that email.");
  }

  const otp = generateOtp();
  const expiresAt = Date.now() + 10 * 60 * 1000; // 10 minutes

  // Sanitise email to use as a DB key (replace . with ,)
  const emailKey = email.replace(/\./g, ",");
  await db.ref(`otps/${emailKey}`).set({ otp, expiresAt });

  // Send email
  const transporter = createTransporter();
  await transporter.sendMail({
    from: `"CornEye Admin" <${process.env.GMAIL_USER}>`,
    to: email,
    subject: "CornEye — Your Password Reset OTP",
    html: `
      <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;border:1px solid #e0e0e0;border-radius:12px;">
        <h2 style="color:#1b3a5c;margin-bottom:8px;">Password Reset</h2>
        <p style="color:#555;font-size:15px;">Use the OTP below to reset your CornEye admin password. It expires in <strong>10 minutes</strong>.</p>
        <div style="text-align:center;margin:32px 0;">
          <span style="display:inline-block;letter-spacing:10px;font-size:40px;font-weight:700;color:#B76E2E;background:#fff8ee;padding:16px 32px;border-radius:10px;border:2px solid #f5b731;">
            ${otp}
          </span>
        </div>
        <p style="color:#999;font-size:13px;">If you did not request this, please ignore this email. Your password will not change.</p>
      </div>
    `,
  });

  return { success: true };
});

// ── verifyOtpAndReset ─────────────────────────────────────────────────────────
// POST { email, otp, newPassword }
// Validates OTP, then updates the matching admin's password in Realtime DB.
exports.verifyOtpAndReset = functions.https.onCall(async (request) => {
  const email = (request.data.email || "").trim().toLowerCase();
  const otp = (request.data.otp || "").trim();
  const newPassword = request.data.newPassword || "";

  if (!email || !otp || !newPassword) {
    throw new functions.https.HttpsError("invalid-argument", "email, otp, and newPassword are required.");
  }
  if (newPassword.length < 6) {
    throw new functions.https.HttpsError("invalid-argument", "Password must be at least 6 characters.");
  }

  const emailKey = email.replace(/\./g, ",");
  const otpSnap = await db.ref(`otps/${emailKey}`).get();

  if (!otpSnap.exists()) {
    throw new functions.https.HttpsError("not-found", "No OTP found for this email. Please request a new one.");
  }

  const { otp: storedOtp, expiresAt } = otpSnap.val();

  if (Date.now() > expiresAt) {
    await db.ref(`otps/${emailKey}`).remove();
    throw new functions.https.HttpsError("deadline-exceeded", "OTP has expired. Please request a new one.");
  }

  if (otp !== storedOtp) {
    throw new functions.https.HttpsError("unauthenticated", "Incorrect OTP.");
  }

  // OTP valid — update admin password
  const adminsSnap = await db.ref("admins").get();
  const admins = adminsSnap.val();
  const [adminKey] = Object.entries(admins).find(
    ([, a]) => (a.email || "").toLowerCase() === email
  );

  await db.ref(`admins/${adminKey}/password`).set(newPassword);

  // Clean up OTP
  await db.ref(`otps/${emailKey}`).remove();

  return { success: true };
});

// ── sendFarmerOtp ─────────────────────────────────────────────────────────────
// POST { email }
// Checks that the email belongs to a registered farmer, generates a 6-digit OTP,
// stores it in Realtime DB under /password_resets/<sanitisedEmail>/otp so the
// mobile OtpScreen can read and verify it, then sends the code via Gmail.
exports.sendFarmerOtp = functions.https.onCall(async (request) => {
  const email = (request.data.email || "").trim().toLowerCase();
  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required.");
  }

  // Verify farmer exists (field is email_address in the farmers node)
  const farmersSnap = await db.ref("farmers")
    .orderByChild("email_address")
    .equalTo(email)
    .get();
  if (!farmersSnap.exists()) {
    throw new functions.https.HttpsError("not-found", "No account found with that email address.");
  }

  const otp = generateOtp();                           // 6-digit string
  const expiresAt = Date.now() + 10 * 60 * 1000;      // 10-minute expiry

  // Key matches FirebaseHelper.emailToKey() — dots replaced with commas
  const emailKey = email.replace(/\./g, ",");

  // Write to the path the mobile OtpScreen reads:
  //   /password_resets/{emailKey}/otp   (String value)
  //   /password_resets/{emailKey}/expiresAt  (timestamp, for future expiry checks)
  await db.ref(`password_resets/${emailKey}`).set({ otp, expiresAt });

  // Send email
  const transporter = createTransporter();
  await transporter.sendMail({
    from: `"CornEye" <${process.env.GMAIL_USER}>`,
    to: email,
    subject: "CornEye — Your Password Reset OTP",
    html: `
      <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;border:1px solid #e0e0e0;border-radius:12px;">
        <h2 style="color:#1b3a5c;margin-bottom:8px;">Password Reset</h2>
        <p style="color:#555;font-size:15px;">Use the OTP below to reset your CornEye password. It expires in <strong>10 minutes</strong>.</p>
        <div style="text-align:center;margin:32px 0;">
          <span style="display:inline-block;letter-spacing:10px;font-size:40px;font-weight:700;color:#B76E2E;background:#fff8ee;padding:16px 32px;border-radius:10px;border:2px solid #f5b731;">
            ${otp}
          </span>
        </div>
        <p style="color:#999;font-size:13px;">If you did not request this, please ignore this email. Your password will not change.</p>
      </div>
    `,
  });

  return { success: true };
});
