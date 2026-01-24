document.addEventListener("DOMContentLoaded", () => {
  const cards = document.querySelectorAll("[data-key]");

  cards.forEach((card) => {
    card.style.cursor = "pointer";
    card.addEventListener("click", onCardClick);
    card.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        onCardClick.call(card, e);
      }
    });
  });

  // Logout
  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      localStorage.removeItem("access_token");
      localStorage.removeItem("user");
      window.location.href = "/index.html";
    });
  }
});

function onCardClick() {
  const key = this.getAttribute("data-key");

  switch (key) {
    // Employee Management (Users & Profiles)
    case "employee-management":
      window.location.href = "/employee_man.html";
      break;

    // Department structure & Head assignment
    case "org-structure":
      window.location.href = "/org_structure.html";
      break;

    // Policy Decision page
    case "policy":
      window.location.href = "/policy_decision.html";
      break;

    // Attendance Monitoring Dashboard
    case "attendance":
      window.location.href = "/attendance_monitoring.html";
      break;

    // Reports Page
    case "reports":
      window.location.href = "/report.html";
      break;

    default:
      console.warn("Unknown card key:", key);
  }
}
