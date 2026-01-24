// web/public/js/reports.js
document.addEventListener("DOMContentLoaded", async () => {
  // auth header helper
  function authHeaders(contentType = "application/json") {
    const token = localStorage.getItem("access_token");
    const headers = {};
    if (contentType) headers["Content-Type"] = contentType;
    if (token) headers["Authorization"] = "Bearer " + token;
    return headers;
  }

  async function fetchJson(url, opts = {}) {
    opts.headers = Object.assign(
      {},
      opts.headers || {},
      authHeaders(opts.headers && opts.headers["Content-Type"])
    );
    const res = await fetch(url, opts);
    if (res.status === 401) throw new Error("Unauthorized (401)");
    const text = await res.text();
    if (!res.ok) {
      // if server returned HTML (SPA fallback), show the body text
      let msg = res.statusText || "HTTP error";
      try {
        const j = JSON.parse(text || "{}");
        msg = j.error || j.message || msg;
      } catch (e) {
        if (text) msg = text;
      }
      throw new Error(msg);
    }
    try {
      return text ? JSON.parse(text) : null;
    } catch (e) {
      return text;
    } // return raw if not JSON
  }

  // DOM elements
  const kpiPresent = document.getElementById("kpiPresent");
  const kpiAbsent = document.getElementById("kpiAbsent");
  const kpiLate = document.getElementById("kpiLate");
  const kpiRate = document.getElementById("kpiRate");
  const recentList = document.getElementById("recentList");
  const selectYear = document.getElementById("selectYear");
  const refreshBtn = document.getElementById("refreshCharts");

  let monthlyChart = null;
  let monthlyCompChart = null;
  let deptChart = null;

  // logout
  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      localStorage.removeItem("access_token");
      localStorage.removeItem("user");
      location.href = "/index.html";
    });
  }

  function ensureArray(a, len = 0) {
    if (!Array.isArray(a)) return Array(len).fill(0);
    if (a.length < len) return a.concat(Array(len - a.length).fill(0));
    return a;
  }

  function escapeHtml(s) {
    if (!s && s !== 0) return "";
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;");
  }

  // load overview KPIs
  async function loadOverview() {
    try {
      const out = await fetchJson("/api/reports/summary/overview", {
        method: "GET",
      });
      if (kpiPresent)
        kpiPresent.textContent =
          out && typeof out.present !== "undefined" ? out.present : "—";
      if (kpiAbsent)
        kpiAbsent.textContent =
          out && typeof out.absent !== "undefined" ? out.absent : "—";
      if (kpiLate)
        kpiLate.textContent =
          out && typeof out.late !== "undefined" ? out.late : "—";
      if (kpiRate)
        kpiRate.textContent =
          out && typeof out.attendance_rate === "number"
            ? `${out.attendance_rate.toFixed(1)}%`
            : "—";
    } catch (err) {
      console.warn("loadOverview failed:", err.message || err);
      if (kpiPresent) kpiPresent.textContent = "—";
      if (kpiAbsent) kpiAbsent.textContent = "—";
      if (kpiLate) kpiLate.textContent = "—";
      if (kpiRate) kpiRate.textContent = "—";
    }
  }

  // draw monthly trend (single year) - area chart kept as-is but safe tooltip
  async function drawMonthly(year) {
    const target = document.querySelector("#chartMonthly");
    if (!target) return;
    try {
      const y = year || new Date().getFullYear();
      const data = await fetchJson(
        `/api/reports/summary/monthly?year=${encodeURIComponent(y)}`,
        { method: "GET" }
      );

      const labels = (data && data.labels) || [
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
      ];
      const present = ensureArray((data && data.present) || [], labels.length);
      const absent = ensureArray((data && data.absent) || [], labels.length);
      const late = ensureArray((data && data.late) || [], labels.length);

      const options = {
        chart: { type: "area", height: 320, toolbar: { show: true } },
        series: [
          { name: "Present", data: present },
          { name: "Absent", data: absent },
          { name: "Late", data: late },
        ],
        xaxis: { categories: labels },
        stroke: { curve: "smooth" },
        tooltip: { shared: true, intersect: false }, // IMPORTANT: intersect false
        legend: { position: "top" },
      };

      if (monthlyChart) {
        try {
          monthlyChart.destroy();
        } catch (e) {}
        monthlyChart = null;
      }
      monthlyChart = new ApexCharts(target, options);
      monthlyChart.render();
    } catch (err) {
      console.error("drawMonthly error:", err);
      if (target)
        target.innerHTML = `<div class="text-red-500 p-4">Failed to load monthly data: ${escapeHtml(
          String(err.message || err)
        )}</div>`;
      if (monthlyChart)
        try {
          monthlyChart.destroy();
        } catch (e) {}
      monthlyChart = null;
    }
  }

  // draw monthly comparison (selected year vs previous year) -> GROUPED BAR (was requested)
  async function drawMonthlyComparison(year) {
    const target = document.querySelector("#chartMonthlyComp");
    if (!target) return;
    try {
      const y = Number(year) || new Date().getFullYear();
      const prev = y - 1;

      // fetch both years in parallel, tolerate failures individually
      const [dThis, dPrev] = await Promise.all([
        fetchJson(
          `/api/reports/summary/monthly?year=${encodeURIComponent(y)}`,
          { method: "GET" }
        ).catch(() => null),
        fetchJson(
          `/api/reports/summary/monthly?year=${encodeURIComponent(prev)}`,
          { method: "GET" }
        ).catch(() => null),
      ]);

      if (!dThis && !dPrev) {
        target.innerHTML = `<div class="text-red-500 p-4">Failed to load monthly comparison data for ${y} / ${prev}</div>`;
        if (monthlyCompChart)
          try {
            monthlyCompChart.destroy();
          } catch (e) {}
        monthlyCompChart = null;
        return;
      }

      const labels = (dThis && dThis.labels) ||
        (dPrev && dPrev.labels) || [
          "Jan",
          "Feb",
          "Mar",
          "Apr",
          "May",
          "Jun",
          "Jul",
          "Aug",
          "Sep",
          "Oct",
          "Nov",
          "Dec",
        ];
      const thisPresent = ensureArray(
        (dThis && dThis.present) || [],
        labels.length
      );
      const prevPresent = ensureArray(
        (dPrev && dPrev.present) || [],
        labels.length
      );

      const options = {
        chart: { type: "bar", height: 320, toolbar: { show: true } },
        plotOptions: { bar: { horizontal: false, columnWidth: "45%" } },
        series: [
          { name: String(y), data: thisPresent },
          { name: String(prev), data: prevPresent },
        ],
        xaxis: { categories: labels },
        tooltip: { shared: true, intersect: false }, // IMPORTANT: intersect false
        legend: { position: "top" },
      };

      if (monthlyCompChart) {
        try {
          monthlyCompChart.destroy();
        } catch (e) {}
        monthlyCompChart = null;
      }
      monthlyCompChart = new ApexCharts(target, options);
      monthlyCompChart.render();
    } catch (err) {
      console.error("drawMonthlyComparison error:", err);
      if (target)
        target.innerHTML = `<div class="text-red-500 p-4">Failed to load monthly comparison: ${escapeHtml(
          String(err.message || err)
        )}</div>`;
      if (monthlyCompChart)
        try {
          monthlyCompChart.destroy();
        } catch (e) {}
      monthlyCompChart = null;
    }
  }

  // draw by-department chart
  async function drawDept(year) {
    const target = document.querySelector("#chartDept");
    if (!target) return;
    try {
      const y = year || new Date().getFullYear();
      const data = await fetchJson(
        `/api/reports/summary/departments?year=${encodeURIComponent(y)}`,
        { method: "GET" }
      );

      const labels = data && Array.isArray(data.labels) ? data.labels : [];
      const present = ensureArray((data && data.present) || [], labels.length);
      const absent = ensureArray((data && data.absent) || [], labels.length);
      const late = ensureArray((data && data.late) || [], labels.length);

      if (!labels.length) {
        target.innerHTML = `<div class="text-gray-600 p-6">No department data for ${y}</div>`;
        if (deptChart)
          try {
            deptChart.destroy();
          } catch (e) {}
        deptChart = null;
        return;
      }

      const options = {
        chart: { type: "bar", height: 360, stacked: false },
        series: [
          { name: "Present", data: present },
          { name: "Absent", data: absent },
          { name: "Late", data: late },
        ],
        xaxis: { categories: labels },
        plotOptions: { bar: { horizontal: false } },
        tooltip: { shared: true, intersect: false }, // IMPORTANT: intersect false
        legend: { position: "top" },
      };

      if (deptChart)
        try {
          deptChart.destroy();
        } catch (e) {}
      deptChart = new ApexCharts(target, options);
      deptChart.render();
    } catch (err) {
      console.error("drawDept error:", err);
      if (target)
        target.innerHTML = `<div class="text-red-500 p-4">Failed to load department data: ${escapeHtml(
          String(err.message || err)
        )}</div>`;
      if (deptChart)
        try {
          deptChart.destroy();
        } catch (e) {}
      deptChart = null;
    }
  }

  // recent reports list
  async function loadRecentReports() {
    if (recentList) recentList.innerHTML = "Loading reports...";
    try {
      const rows = await fetchJson("/api/reports", { method: "GET" });
      if (!Array.isArray(rows) || rows.length === 0) {
        if (recentList)
          recentList.innerHTML = `<div class="text-gray-500">No reports yet</div>`;
        return;
      }
      if (!recentList) return;
      recentList.innerHTML = rows
        .map((r) => {
          let params = {};
          if (r.parameters) {
            try {
              params =
                typeof r.parameters === "string"
                  ? JSON.parse(r.parameters)
                  : r.parameters;
            } catch (e) {
              params = r.parameters || {};
            }
          }
          const prettyParams = (() => {
            const from = params.from || params.start || null;
            const to = params.to || params.end || null;
            if (from && to) return `From: ${from} → To: ${to}`;
            if (from) return `From: ${from}`;
            if (to) return `To: ${to}`;
            if (params.month) return `Month: ${params.month}`;
            if (params.year) return `Year: ${params.year}`;
            return "No parameters";
          })();
          return `<div class="card flex justify-between items-start">
            <div>
              <h3 class="font-semibold">${escapeHtml(r.report_type)}</h3>
              <div class="text-sm text-gray-500">Generated by: ${escapeHtml(
                r.username || "system"
              )} — ${new Date(r.generated_at).toLocaleString()}</div>
              <div class="text-sm text-gray-700 mt-2">${escapeHtml(
                prettyParams
              )}</div>
            </div>
            <div class="flex flex-col gap-2">
              <button data-id="${
                r.report_id
              }" class="downloadBtn bg-blue-600 text-white px-4 py-2 rounded">Download (PDF)</button>
              <button data-id="${
                r.report_id
              }" class="openBtn bg-gray-100 text-gray-700 px-3 py-2 rounded border">Open</button>
            </div>
          </div>`;
        })
        .join("");

      recentList
        .querySelectorAll(".downloadBtn")
        .forEach((b) =>
          b.addEventListener("click", () =>
            downloadReportPdf(b.dataset.id, true)
          )
        );
      recentList
        .querySelectorAll(".openBtn")
        .forEach((b) =>
          b.addEventListener("click", () =>
            downloadReportPdf(b.dataset.id, false)
          )
        );
    } catch (err) {
      console.error("loadRecentReports error:", err);
      if (recentList)
        recentList.innerHTML = `<div class="text-red-500 p-4">Failed to load reports: ${escapeHtml(
          String(err.message || err)
        )}</div>`;
    }
  }

  async function downloadReportPdf(id, forceDownload = false) {
    try {
      const token = localStorage.getItem("access_token");
      const headers = {};
      if (token) headers["Authorization"] = "Bearer " + token;
      const res = await fetch(`/api/reports/${id}/download`, {
        method: "GET",
        headers,
      });
      if (res.status === 401) {
        alert("Request failed: Unauthorized");
        return;
      }
      if (!res.ok) {
        const txt = await res.text();
        alert("Request failed: " + (txt || res.statusText));
        return;
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      if (forceDownload) {
        const a = document.createElement("a");
        a.href = url;
        a.download = `report-${id}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
      } else {
        window.open(url, "_blank");
      }
      setTimeout(() => URL.revokeObjectURL(url), 60 * 1000);
    } catch (err) {
      console.error("downloadReportPdf error:", err);
      alert("Download failed: " + (err.message || "Unknown"));
    }
  }

  // build year select options (last 6 years)
  function populateYearSelect() {
    if (!selectYear) return;
    const current = new Date().getFullYear();
    const years = [];
    for (let i = 0; i < 6; i++) years.push(current - i);
    selectYear.innerHTML = years
      .map((y) => `<option value="${y}">${y}</option>`)
      .join("");
    selectYear.value = current;
  }

  // refresh handler
  async function refreshAll() {
    const year =
      (selectYear && Number(selectYear.value)) || new Date().getFullYear();
    await loadOverview();
    await drawMonthly(year);
    await drawMonthlyComparison(year);
    await drawDept(year);
    await loadRecentReports();
  }

  // initial
  populateYearSelect();
  if (refreshBtn) refreshBtn.addEventListener("click", refreshAll);

  // auto-run once
  // auto-run once (do not block inbox)
  refreshAll().catch(console.error);

  // ===============================
  // HR / USER REPORTS (INBOX)
  // ===============================

  const inboxList = document.getElementById("inboxList");
  const reportModal = document.getElementById("reportModal");
  const modalTitle = document.getElementById("modalTitle");
  const modalMeta = document.getElementById("modalMeta");
  const modalMessage = document.getElementById("modalMessage");

  // Load HR/User reports
  async function loadInboxReports() {
    if (!inboxList) return;

    inboxList.innerHTML = "Loading reports...";
    try {
      const rows = await fetchJson("/api/reports/inbox", { method: "GET" });

      if (!rows || rows.length === 0) {
        inboxList.innerHTML = `<div class="text-gray-500">No reports submitted yet.</div>`;
        return;
      }

      inboxList.innerHTML = rows
        .map((r) => {
          // 1. Determine report type (backend DOES NOT send this)
          let type = null;

          // 2. Extract type ONLY from title prefix
          let cleanTitle = r.title || "";

          const match = cleanTitle.match(
            /^\[(ISSUE|REQUEST|COMPLAINT|OTHER)\]\s*/i
          );

          if (match) {
            type = match[1].toUpperCase();
            cleanTitle = cleanTitle.replace(match[0], "");
          }

          // 3. Badge color by type
          let badgeClass = "";
          switch (type) {
            case "ISSUE":
              badgeClass = "bg-red-100 text-red-700";
              break;
            case "REQUEST":
              badgeClass = "bg-blue-100 text-blue-700";
              break;
            case "COMPLAINT":
              badgeClass = "bg-orange-100 text-orange-700";
              break;
            case "OTHER":
              badgeClass = "bg-purple-100 text-purple-700";
              break;
          }

          // 4. Render badge ONLY if type exists
          const badgeHtml = type
            ? `<span class="px-2 py-0.5 text-xs font-semibold rounded ${badgeClass}">
           ${type}
         </span>`
            : "";

          return `
      <div class="border rounded-lg p-4 hover:bg-gray-50 cursor-pointer"
           onclick="openReportModal(${r.report_id})">

        <div class="flex items-center gap-2">
          ${badgeHtml}
          <div class="font-medium text-gray-800">
            ${escapeHtml(cleanTitle)}
          </div>
        </div>

        <div class="text-xs text-gray-500 mt-1">
          ${escapeHtml(r.sender_role)} • Submitted by ${escapeHtml(
            r.sender_name
          )}
          • ${new Date(r.created_at).toLocaleString()}
        </div>
      </div>
    `;
        })
        .join("");
} catch (err) {
      console.error("Inbox load failed:", err);
      inboxList.innerHTML = `<div class="text-red-500">Failed to load reports</div>`;
    }
  }


  // Open modal with report detail
  window.openReportModal = async function (id) {
    try {
      const r = await fetchJson(`/api/reports/inbox/${id}`, { method: "GET" });

      modalTitle.textContent = r.title;
      modalMeta.textContent = `${r.sender_role} • From ${
        r.sender_name
      } • ${new Date(r.created_at).toLocaleString()}`;

      modalMessage.textContent = r.message;

      reportModal.classList.remove("hidden");
      reportModal.classList.add("flex");
    } catch (err) {
      alert("Failed to load report details");
    }
  };

  window.closeReportModal = function () {
    reportModal.classList.add("hidden");
    reportModal.classList.remove("flex");
  };
  // load inbox immediately
  loadInboxReports().catch(console.error);
});
