// web/public/js/forgot_password.js
// Attach forgot-password modal behavior to the existing login page.
// Requires fetch endpoints:
// POST /api/auth/forgot        { email }
// POST /api/auth/verify-otp    { email, otp }
// POST /api/auth/reset-password { email, otp, new_password }
// (server code provided below)

document.addEventListener("DOMContentLoaded", () => {
  const forgotModal = document.getElementById("forgotModal");
  const openLink = document.getElementById("forgot");
  const fp = {
    modal: forgotModal,
    step1: document.getElementById("fp-step-1"),
    step2: document.getElementById("fp-step-2"),
    step3: document.getElementById("fp-step-3"),
    step4: document.getElementById("fp-step-4"),
    emailInput: document.getElementById("fp-email"),
    emailDisplay: document.getElementById("fp-email-display"),
    codeInput: document.getElementById("fp-code"),
    newPw: document.getElementById("fp-new-password"),
    newPw2: document.getElementById("fp-new-password-confirm"),
    msg1: document.getElementById("fp-msg-1"),
    msg2: document.getElementById("fp-msg-2"),
    msg3: document.getElementById("fp-msg-3"),
  };

  function openModal() {
    fp.emailInput.value = "";
    fp.codeInput.value = "";
    fp.newPw.value = "";
    fp.newPw2.value = "";
    fp.msg1.textContent = "";
    fp.msg2.textContent = "";
    fp.msg3.textContent = "";
    showStep(1);
    forgotModal.classList.remove("hidden");
    forgotModal.classList.add("flex");
    fp.emailInput.focus();
  }

  function closeModal() {
    forgotModal.classList.add("hidden");
    forgotModal.classList.remove("flex");
  }

  function showStep(n) {
    [1, 2, 3, 4].forEach((i) => {
      const el = document.getElementById(`fp-step-${i}`);
      if (el) el.classList.toggle("hidden", i !== n);
    });
  }

  // open modal
  if (openLink)
    openLink.addEventListener("click", (e) => {
      e.preventDefault();
      openModal();
    });

  // cancel buttons
  document.getElementById("fp-cancel-1").addEventListener("click", closeModal);
  document.getElementById("fp-cancel-2").addEventListener("click", closeModal);
  document.getElementById("fp-cancel-3").addEventListener("click", closeModal);

  // send verification code
  document
    .getElementById("fp-send-code")
    .addEventListener("click", async () => {
      fp.msg1.textContent = "";
      const email = (fp.emailInput.value || "").trim();
      if (!email) {
        fp.msg1.textContent = "Please enter your email.";
        return;
      }
      try {
        const res = await fetch("/api/auth/forgot", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email }),
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
          fp.msg1.textContent =
            data.error || data.message || "Failed to send code";
          return;
        }
        fp.emailDisplay.textContent = email;
        showStep(2);
        fp.codeInput.focus();
        fp.msg2.textContent = "Code sent — check your email.";
        fp.msg2.style.color = "green";
      } catch (err) {
        fp.msg1.textContent = "Network error";
      }
    });

  // verify code
  document
    .getElementById("fp-verify-code")
    .addEventListener("click", async () => {
      fp.msg2.textContent = "";
      const email = fp.emailDisplay.textContent;
      const otp = (fp.codeInput.value || "").trim();
      if (!otp || otp.length < 4) {
        fp.msg2.textContent = "Enter the 6-digit code";
        return;
      }
      try {
        const res = await fetch("/api/auth/verify-otp", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, otp }),
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
          fp.msg2.textContent = data.error || data.message || "Invalid code";
          return;
        }
        showStep(3);
        fp.newPw.focus();
      } catch (err) {
        fp.msg2.textContent = "Network error";
      }
    });

  // resend
  document.getElementById("fp-resend").addEventListener("click", async () => {
    fp.msg2.textContent = "";
    const email = fp.emailDisplay.textContent || fp.emailInput.value.trim();
    if (!email) {
      fp.msg2.textContent = "No email to resend to";
      return;
    }
    try {
      const res = await fetch("/api/auth/forgot", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        fp.msg2.textContent = data.error || "Resend failed";
        return;
      }
      fp.msg2.textContent = "Code resent";
      fp.msg2.style.color = "green";
    } catch (err) {
      fp.msg2.textContent = "Network error";
    }
  });

  // reset password
  document
    .getElementById("fp-reset-password")
    .addEventListener("click", async () => {
      fp.msg3.textContent = "";
      const email = fp.emailDisplay.textContent;
      const otp = (fp.codeInput.value || "").trim();
      const pw = fp.newPw.value || "";
      const pw2 = fp.newPw2.value || "";
      if (!pw || pw.length < 8) {
        fp.msg3.textContent = "Password must be at least 8 characters";
        return;
      }
      if (pw !== pw2) {
        fp.msg3.textContent = "Passwords do not match";
        return;
      }
      try {
        const res = await fetch("/api/auth/reset-password", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, otp, new_password: pw }),
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
          fp.msg3.textContent = data.error || "Reset failed";
          return;
        }
        showStep(4);
      } catch (err) {
        fp.msg3.textContent = "Network error";
      }
    });

  // done button
  document.getElementById("fp-done").addEventListener("click", () => {
    closeModal();
    // focus username
    document.getElementById("username")?.focus();
  });

  // close if click outside dialog content
  forgotModal.addEventListener("click", (e) => {
    if (e.target === forgotModal) closeModal();
  });
});
