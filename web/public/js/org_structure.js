// paste over existing web/public/js/org_structure.js
document.addEventListener("DOMContentLoaded", () => {
  function authHeaders(contentType = "application/json") {
    const token = localStorage.getItem("access_token");
    const headers = {};
    if (contentType) headers["Content-Type"] = contentType;
    if (token) headers["Authorization"] = "Bearer " + token;
    return headers;
  }

  async function apiFetch(url, opts = {}) {
    opts.headers = Object.assign(
      {},
      opts.headers || {},
      authHeaders(
        opts.headers && opts.headers["Content-Type"]
          ? opts.headers["Content-Type"]
          : "application/json"
      )
    );
    try {
      const res = await fetch(url, opts);
      if (res.status === 401) {
        localStorage.removeItem("access_token");
        localStorage.removeItem("user");
        alert("Session expired or unauthorized. Redirecting to login.");
        window.location.href = "/index.html";
        throw new Error("Unauthorized");
      }
      const text = await res.text();
      let data = null;
      try {
        data = text ? JSON.parse(text) : null;
      } catch (e) {
        data = text;
      }
      if (!res.ok) {
        const errMsg =
          (data && data.error) ||
          (data && data.message) ||
          res.statusText ||
          "API error";
        const e = new Error(errMsg);
        e._data = data;
        throw e;
      }
      return data;
    } catch (err) {
      throw err;
    }
  }

  const api = {
    getDepartments: () => apiFetch("/api/departments", { method: "GET" }),
    getEmployees: () => apiFetch("/api/admin/employees", { method: "GET" }),
    createDept: (payload) =>
      apiFetch("/api/departments", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    updateDept: (id, payload) =>
      apiFetch(`/api/departments/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    deleteDept: (id) =>
      apiFetch(`/api/departments/${id}`, { method: "DELETE" }),
    assignHead: (deptId, headEmpId) =>
      apiFetch("/api/departments/" + deptId + "/head", {
        method: "PUT",
        body: JSON.stringify({ head_employee_id: headEmpId }),
      }),
    reassign: (employee_id, to_department_id) =>
      apiFetch("/api/departments/reassign", {
        method: "POST",
        body: JSON.stringify({ employee_id, to_department_id }),
      }),
  };

  // Tabs
  document.querySelectorAll(".tab").forEach((btn) => {
    btn.addEventListener("click", () => {
      document
        .querySelectorAll(".tab")
        .forEach((t) =>
          t.classList.remove(
            "active",
            "text-blue-600",
            "border-b-2",
            "border-blue-600"
          )
        );
      btn.classList.add(
        "active",
        "text-blue-600",
        "border-b-2",
        "border-blue-600"
      );
      const tab = btn.dataset.tab;
      document
        .querySelectorAll(".tab-content")
        .forEach((c) => c.classList.add("hidden"));
      document.getElementById("tab-" + tab).classList.remove("hidden");
    });
  });

  // DOM refs
  const grid = document.getElementById("departmentsGrid");
  const openCreateBtn = document.getElementById("openCreateDeptBtn");
  const deptSearch = document.getElementById("deptSearch");

  // Modal elements
  const deptModal = document.getElementById("deptModal");
  const deptForm = document.getElementById("deptForm");
  const deptMsg = document.getElementById("deptMsg");
  const deptModalTitle = document.getElementById("deptModalTitle");
  const closeDeptModal = document.getElementById("closeDeptModal");
  const cancelDeptBtn = document.getElementById("cancelDeptBtn");
  const saveDeptBtn = document.getElementById("saveDeptBtn");
  const deleteDeptBtn = document.getElementById("deleteDeptBtn");

  function showModal(mode = "create", dept = null) {
    deptMsg.textContent = "";
    if (mode === "create") {
      deptModalTitle.textContent = "Create Department";
      document.getElementById("dept_id").value = "";
      document.getElementById("dept_name").value = "";
      document.getElementById("dept_location").value = "";
      deleteDeptBtn.classList.add("hidden");
    } else {
      deptModalTitle.textContent = "Edit Department";
      document.getElementById("dept_id").value = dept.department_id;
      document.getElementById("dept_name").value = dept.name || "";
      document.getElementById("dept_location").value = dept.location || "";
      deleteDeptBtn.classList.remove("hidden");
    }
    deptModal.classList.remove("hidden");
    deptModal.classList.add("flex");
    setTimeout(() => document.getElementById("dept_name").focus(), 90);
    deptModal.setAttribute("aria-hidden", "false");
  }

  function closeModal() {
    deptModal.classList.add("hidden");
    deptModal.classList.remove("flex");
    deptModal.setAttribute("aria-hidden", "true");
  }

  closeDeptModal.addEventListener("click", closeModal);
  cancelDeptBtn.addEventListener("click", (e) => {
    e.preventDefault();
    closeModal();
  });

  deptForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const id = document.getElementById("dept_id").value;
    const payload = {
      name: document.getElementById("dept_name").value.trim(),
      location: document.getElementById("dept_location").value.trim() || null,
    };
    if (!payload.name) {
      deptMsg.textContent = "Name is required";
      deptMsg.style.color = "red";
      return;
    }
    saveDeptBtn.disabled = true;
    saveDeptBtn.textContent = "Saving...";
    try {
      if (id) {
        await api.updateDept(Number(id), payload);
        deptMsg.textContent = "Saved";
        deptMsg.style.color = "green";
      } else {
        await api.createDept(payload);
        deptMsg.textContent = "Created";
        deptMsg.style.color = "green";
      }
      await load();
      setTimeout(closeModal, 600);
    } catch (err) {
      deptMsg.textContent = err.message || "Error";
      deptMsg.style.color = "red";
      console.error(err);
    } finally {
      saveDeptBtn.disabled = false;
      saveDeptBtn.textContent = "Save";
    }
  });

  deleteDeptBtn.addEventListener("click", async () => {
    const id = document.getElementById("dept_id").value;
    if (!id) return;
    if (!confirm("Delete department? This will remove the department record."))
      return;
    try {
      await api.deleteDept(Number(id));
      await load();
      closeModal();
    } catch (err) {
      deptMsg.textContent = err.message || "Delete failed";
      deptMsg.style.color = "red";
      console.error(err);
    }
  });

  async function load() {
    let depts = [];
    try {
      depts = await api.getDepartments();
    } catch (err) {
      grid.innerHTML = `<div class="text-red-500 p-4">Failed to load departments</div>`;
      console.error(err);
      return;
    }

    const q = (deptSearch.value || "").toLowerCase().trim();
    if (q) {
      depts = depts.filter((d) => (d.name || "").toLowerCase().includes(q));
    }

    grid.innerHTML = depts
      .map((d) => {
        const head = d.head_name
          ? `Department Head: ${escapeHtml(d.head_name)}`
          : "Department Head: —";
        const count = d.employee_count || 0;
        const location = d.location
          ? `<div class="text-sm text-gray-500">${escapeHtml(d.location)}</div>`
          : "";
        return `<div class="bg-white p-6 rounded shadow relative">
          <div class="flex items-start gap-4">
            <div class="w-12 h-12 rounded p-3 bg-purple-100 text-purple-700">
              <svg class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M3 10h18"></path><rect x="3" y="5" width="18" height="14" rx="2"/></svg>
            </div>
            <div class="flex-1">
              <h3 class="font-semibold">${escapeHtml(d.name)}</h3>
              ${location}
              <div class="mt-4 text-sm text-gray-600">👥 ${count} employees</div>
              <div class="mt-2 text-sm text-gray-700">${head}</div>
            </div>
            <div class="ml-4 flex gap-2">
              <button data-action="edit" data-id="${
                d.department_id
              }" class="text-sm text-blue-600 hover:underline">Edit</button>
              <button data-action="assign" data-id="${
                d.department_id
              }" class="text-sm text-gray-600 hover:underline">Assign Head</button>
            </div>
          </div>
        </div>`;
      })
      .join("");

    grid.querySelectorAll("button[data-action]").forEach((b) => {
      b.addEventListener("click", (ev) => {
        const id = Number(b.dataset.id);
        const act = b.dataset.action;
        const dept = depts.find((dd) => dd.department_id === id);
        if (act === "edit") {
          showModal("edit", dept);
        } else if (act === "assign") {
          document.querySelector("[data-tab='assign']").click();
          setTimeout(() => {
            const sel = document.getElementById("assign_dept");
            if (sel) sel.value = String(id);
          }, 100);
        }
      });
    });

    await populateSelects();
  }

  function escapeHtml(s) {
    if (!s && s !== 0) return "";
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  }

  async function populateSelects() {
    let depts = [];
    let emps = [];
    try {
      depts = await api.getDepartments();
      emps = await api.getEmployees();
    } catch (err) {
      console.error("populateSelects failed", err);
      return;
    }

    const assignDept = document.getElementById("assign_dept");
    const assignEmp = document.getElementById("assign_emp");
    const reassignEmp = document.getElementById("reassign_emp");
    const toDept = document.getElementById("to_dept");

    assignDept.innerHTML =
      '<option value="">Select department...</option>' +
      depts
        .map(
          (d) =>
            `<option value="${d.department_id}">${escapeHtml(d.name)}</option>`
        )
        .join("");

    toDept.innerHTML =
      '<option value="">Select department...</option>' +
      depts
        .map(
          (d) =>
            `<option value="${d.department_id}">${escapeHtml(d.name)}</option>`
        )
        .join("");

    assignEmp.innerHTML =
      '<option value="">Select employee...</option>' +
      emps
        .map(
          (e) =>
            `<option value="${e.employee_id}">${escapeHtml(
              e.full_name || e.username || e.email
            )}</option>`
        )
        .join("");

    // reassign employee select (no dataset.dept attribute needed now)
    reassignEmp.innerHTML =
      '<option value="">Select employee...</option>' +
      emps
        .map(
          (e) =>
            `<option value="${e.employee_id}">${escapeHtml(
              e.full_name || e.username
            )}</option>`
        )
        .join("");
  }

  // assign head
  document
    .getElementById("assignHeadBtn")
    .addEventListener("click", async () => {
      const dept = document.getElementById("assign_dept").value;
      const emp = document.getElementById("assign_emp").value;
      const msg = document.getElementById("assignMsg");
      if (!dept || !emp) {
        msg.textContent = "Select both department and employee";
        msg.style.color = "red";
        return;
      }
      try {
        await api.assignHead(dept, Number(emp));
        msg.textContent = "Assigned head";
        msg.style.color = "green";
        load();
      } catch (e) {
        msg.textContent = e.message || "Error";
        msg.style.color = "red";
      }
    });

  // reassign
  document.getElementById("reassignBtn").addEventListener("click", async () => {
    const emp = document.getElementById("reassign_emp").value;
    const to = document.getElementById("to_dept").value;
    const msg = document.getElementById("reassignMsg");
    if (!emp || !to) {
      msg.textContent = "Select employee and target department";
      msg.style.color = "red";
      return;
    }
    try {
      await api.reassign(Number(emp), Number(to));
      msg.textContent = "Reassigned";
      msg.style.color = "green";
      load();
    } catch (e) {
      msg.textContent = e.message || "Error";
      msg.style.color = "red";
    }
  });

  openCreateBtn.addEventListener("click", () => showModal("create"));

  // logout
  document.getElementById("logoutBtn").addEventListener("click", () => {
    localStorage.removeItem("access_token");
    localStorage.removeItem("user");
    location.href = "/index.html";
  });

  deptSearch.addEventListener("input", debounce(load, 250));
  load();

  function debounce(fn, wait) {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), wait);
    };
  }
});
