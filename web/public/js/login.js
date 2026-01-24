document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("loginForm");
  const errEl = document.getElementById("error");
  const infoEl = document.getElementById("info");
  const submitBtn = document.getElementById("submitBtn");
  const togglePw = document.getElementById("togglePw");
  const pwInput = document.getElementById("password");
  const eyeIcon = document.getElementById("eyeIcon");

  const EYE_SVG = `
  <path d="M12 5C7 5 2.73 8.11 1 12c1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7z" 
        stroke="#cbd5e1" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="12" cy="12" r="3" stroke="#cbd5e1" stroke-width="1.6"/>
`;

  const EYE_OFF_SVG = `
  <path d="M12 5C7 5 2.73 8.11 1 12c1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7z" 
        stroke="#cbd5e1" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="12" cy="12" r="3" stroke="#cbd5e1" stroke-width="1.6"/>
  <line x1="5" y1="5" x2="19" y2="19" 
        stroke="#cbd5e1" stroke-width="1.6" stroke-linecap="round" />
`;

  if (
    !form ||
    !errEl ||
    !infoEl ||
    !submitBtn ||
    !togglePw ||
    !pwInput ||
    !eyeIcon
  ) {
    console.error("Login: required DOM elements missing");
    return;
  }

  eyeIcon.innerHTML = EYE_SVG;
  togglePw.setAttribute("aria-label", "Show password");

  togglePw.addEventListener("click", () => {
    const isPwd = pwInput.type === "password";
    pwInput.type = isPwd ? "text" : "password";
    eyeIcon.innerHTML = isPwd ? EYE_OFF_SVG : EYE_SVG;
    togglePw.setAttribute(
      "aria-label",
      pwInput.type === "password" ? "Show password" : "Hide password"
    );
  });

  function showError(msg) {
    errEl.textContent = msg;
    errEl.classList.remove("hidden");
    infoEl.classList.add("hidden");
  }
  function showInfo(msg) {
    infoEl.textContent = msg;
    infoEl.classList.remove("hidden");
    errEl.classList.add("hidden");
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    errEl.classList.add("hidden");
    infoEl.classList.add("hidden");

    const username = document.getElementById("username").value.trim();
    const password = pwInput.value;

    if (!username || !password) {
      showError("Please enter username and password.");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Signing in...";

    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        const msg = data.error || data.message || "Login failed";
        showError(msg);
        submitBtn.disabled = false;
        submitBtn.textContent = "Log In";
        return;
      }

      if (!data.access_token || !data.user) {
        showError("Invalid server response");
        submitBtn.disabled = false;
        submitBtn.textContent = "Log In";
        return;
      }

      localStorage.setItem("access_token", data.access_token);
      localStorage.setItem("user", JSON.stringify(data.user));

      showInfo("Login successful — redirecting...");

      const role = (data.user.user_type || "").toUpperCase();
      setTimeout(() => {
        if (role === "MANAGER") {
          window.location.href = "/admin_dashboard.html";
        } else if (role === "HR") {
          window.location.href = "/hr.html";
        } else {
          window.location.href = "/";
        }
      }, 600);
    } catch (err) {
      console.error(err);
      showError("Network error — check server and try again.");
      submitBtn.disabled = false;
      submitBtn.textContent = "Log In";
    }
  });
});
