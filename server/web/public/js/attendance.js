// web/public/js/attendance.js
document.addEventListener("DOMContentLoaded", async () => {
  const presentEl = document.getElementById("presentCount");
  const absentEl = document.getElementById("absentCount");
  const lateEl = document.getElementById("lateCount");
  const rateEl = document.getElementById("attRate");
  const tbody = document.getElementById("attTable");
  const loading = document.getElementById("loading");
  const kpiRow = document.getElementById("kpiRow");
  const tableCard = document.getElementById("tableCard");
  const pageError = document.getElementById("pageError");

  function showError(msg) {
    if (!pageError) return;
    pageError.querySelector("div").textContent = String(msg || "Unknown error");
    pageError.classList.remove("hidden");
  }

  function hideError() {
    if (!pageError) return;
    pageError.classList.add("hidden");
    pageError.querySelector("div").textContent = "";
  }

  function escapeHtml(s) {
    if (!s && s !== 0) return "";
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;");
  }

  // Build headers with Authorization if token present
  function makeHeaders() {
    const headers = { "Content-Type": "application/json" };
    const token = localStorage.getItem("access_token");
    if (token) headers["Authorization"] = "Bearer " + token;
    return headers;
  }

  try {
    hideError();
    loading && loading.classList.remove("hidden");

    const date = new Date().toISOString().slice(0, 10);
    const res = await fetch(
      `/api/attendance/overview?date=${encodeURIComponent(date)}`,
      {
        method: "GET",
        headers: makeHeaders(),
      }
    );

    // redirect to login on 401
    if (res.status === 401) {
      // Clear token and go to login
      localStorage.removeItem("access_token");
      localStorage.removeItem("user");
      location.href = "/index.html";
      return;
    }

    if (!res.ok) {
      const txt = await res.text().catch(() => "");
      throw new Error(txt || res.statusText || `HTTP ${res.status}`);
    }

    const overview = await res.json();

    loading && loading.classList.add("hidden");

    if (!Array.isArray(overview) || overview.length === 0) {
      // show friendly empty state
      showError(
        "No department attendance data available for the selected date."
      );
      if (kpiRow) kpiRow.classList.add("hidden");
      if (tableCard) tableCard.classList.add("hidden");
      return;
    }

    // populate high-level counts
    let totalEmployees = 0,
      totalPresent = 0,
      totalAbsent = 0,
      totalLate = 0;
    overview.forEach((d) => {
      totalEmployees += Number(d.total_employees) || 0;
      totalPresent += Number(d.present) || 0;
      totalAbsent += Number(d.absent) || 0;
      totalLate += Number(d.late) || 0;
    });

    presentEl && (presentEl.textContent = totalPresent);
    absentEl && (absentEl.textContent = totalAbsent);
    lateEl && (lateEl.textContent = totalLate);
    const rate = totalEmployees
      ? ((totalPresent / totalEmployees) * 100).toFixed(1) + "%"
      : "0.0%";
    rateEl && (rateEl.textContent = rate);

    // render table rows
    if (tbody) {
      tbody.innerHTML = overview
        .map((d) => {
          const deptName = escapeHtml(d.name || "—");
          const totalEmp = Number(d.total_employees) || 0;
          const present = Number(d.present) || 0;
          const absent = Number(d.absent) || 0;
          const late = Number(d.late) || 0;
          const attRate =
            d.attendance_rate ||
            (totalEmp ? ((present / totalEmp) * 100).toFixed(1) + "%" : "0.0%");
          return `<tr class="border-t">
          <td class="py-3">${deptName}</td>
          <td class="py-3">${totalEmp}</td>
          <td class="py-3">${present}</td>
          <td class="py-3">${absent}</td>
          <td class="py-3">${late}</td>
          <td class="py-3">${escapeHtml(attRate)}</td>
        </tr>`;
        })
        .join("");
    }

    // show containers
    if (kpiRow) kpiRow.classList.remove("hidden");
    if (tableCard) tableCard.classList.remove("hidden");
  } catch (err) {
    loading && loading.classList.add("hidden");
    console.error("attendance page error:", err);
    showError(err && err.message ? err.message : String(err));
    if (kpiRow) kpiRow.classList.add("hidden");
    if (tableCard) tableCard.classList.add("hidden");
  }
});
