// web/public/js/employee_man.js
// Replacement with better error reporting + Authorization header support

document.addEventListener("DOMContentLoaded", () => {
  const createForm = document.getElementById("createForm");
  const createBtn = document.getElementById("createBtn");
  const refreshBtn = document.getElementById("refreshBtn");
  const formMsg = document.getElementById("formMsg");
  const tbody = document.getElementById("tbody");
  const search = document.getElementById("search");

  // Edit modal elements
  const editModal = document.getElementById("editModal");
  const editBackdrop = document.getElementById("editModalBackdrop");
  const closeEdit = document.getElementById("closeEdit");
  const editForm = document.getElementById("editForm");
  const saveEditBtn = document.getElementById("saveEditBtn");
  const cancelEditBtn = document.getElementById("cancelEditBtn");
  const editMsg = document.getElementById("editMsg");

  const API_BASE =
    window.location.hostname === "localhost"
      ? "http://localhost:3000"
      : "https://humio-production.up.railway.app";


  // helper: get Authorization header if token present
  function authHeaders() {
    const token = localStorage.getItem("access_token");
    const h = { "Content-Type": "application/json" };
    if (token) h["Authorization"] = "Bearer " + token;
    return h;
  }

  // show/hide modal
  function openEditModal() {
    editModal.classList.remove("hidden");
    editModal.classList.add("flex");
  }
  function closeEditModal() {
    editModal.classList.add("hidden");
    editModal.classList.remove("flex");
    editForm.reset();
    editMsg.textContent = "";
  }

  // display brief messages in a target element
  function showMessage(el, text, ok = true) {
    el.textContent = text;
    el.style.color = ok ? "green" : "red";
    if (text) setTimeout(() => (el.textContent = ""), 3500);
  }

  // Attach modal close handlers
  if (closeEdit) closeEdit.addEventListener("click", closeEditModal);
  if (cancelEditBtn) cancelEditBtn.addEventListener("click", closeEditModal);
  if (editBackdrop) editBackdrop.addEventListener("click", closeEditModal);

  // fetch wrapper with error handling (returns parsed JSON or throws)
  async function fetchJson(url, opts = {}) {
    // merge headers
    opts.headers = Object.assign({}, opts.headers || {}, authHeaders());
    try {
      const res = await fetch(url, opts);
      let payload;
      try {
        payload = await res.json().catch(() => ({}));
      } catch (e) {
        payload = {};
      }
      if (!res.ok) {
        const err = new Error(
          `HTTP ${res.status} ${res.statusText} - ${
            payload.error ||
            payload.message ||
            JSON.stringify(payload) ||
            "no body"
          }`
        );
        err.status = res.status;
        err.payload = payload;
        throw err;
      }
      return payload;
    } catch (err) {
      if (err instanceof TypeError) {
        const e = new Error(
          `Network error or server unreachable. Check backend server or CORS settings.
 (${err.message})`
        );
        e.original = err;
        throw e;
      }
      throw err;
    }
  }

  // load employees (with optional search q)
  async function loadEmployees(q = "") {
    try {
      tbody.innerHTML =
        '<tr><td colspan="7" class="p-4 text-gray-500">Loading…</td></tr>';
      const url =
        `${API_BASE}/api/admin/employees` +
        (q ? "?q=" + encodeURIComponent(q) : "");
      const rows = await fetchJson(url, { method: "GET" });

      if (!rows || rows.length === 0) {
        tbody.innerHTML =
          '<tr><td colspan="7" class="p-4 text-gray-500">No employees</td></tr>';
        return;
      }

      tbody.innerHTML = rows
        .map((r) => {
          const salary = r.salary
            ? `$${Number(r.salary).toLocaleString()}`
            : "";
          const code = r.employee_code || "";
          const role = r.job_title || r.user_type || "";
          const dept = r.department_name || "";

          const avatar = r.profile_image
            ? `<img src="${API_BASE}${escapeHtml(
                r.profile_image
              )}" class="inline-block w-8 h-8 rounded-full object-cover mr-2" />`
            : "";

          return `<tr class="border-t"
                      data-eid="${r.employee_id || ""}"
                      data-user_id="${r.user_id || ""}"
                      data-username="${escapeHtml(r.username || "")}"
                      data-email="${escapeHtml(r.email || "")}"
                      data-full_name="${escapeHtml(r.full_name || "")}"
                      data-user_type="${escapeHtml(r.user_type || "")}"
                      data-employee_code="${escapeHtml(r.employee_code || "")}"
                      data-job_title="${escapeHtml(r.job_title || "")}"
                      data-salary="${
                        r.salary !== null && r.salary !== undefined
                          ? r.salary
                          : ""
                      }"
                      data-hire_date="${escapeHtml(r.hire_date || "")}"
                      data-manager_id="${r.manager_id || ""}"
                      data-department_id="${r.department_id || ""}"
                    >
          <td class="py-3 px-3">${escapeHtml(code)}</td>
          <td class="py-3 px-3">${avatar}${escapeHtml(
            r.full_name || r.username || ""
          )}</td>
          <td class="py-3 px-3">${escapeHtml(role)}</td>
          <td class="py-3 px-3">${escapeHtml(dept)}</td>
          <td class="py-3 px-3">${escapeHtml(r.email || "")}</td>
          <td class="py-3 px-3">${escapeHtml(salary)}</td>
          <td class="py-3 px-3">
            <button data-eid="${
              r.employee_id
            }" class="editBtn text-blue-600 mr-2">Edit</button>
            <button data-eid="${
              r.employee_id
            }" class="delBtn text-red-600">Delete</button>
          </td>
        </tr>`;
        })
        .join("");

      // Attach handlers after DOM insertion
      document.querySelectorAll(".delBtn").forEach((b) => {
        b.addEventListener("click", async () => {
          const id = b.getAttribute("data-eid");
          if (!confirm("Delete this employee?")) return;
          try {
            await fetchJson(`${API_BASE}/api/admin/employees/${id}`, {
              method: "DELETE",
            });

            await loadEmployees(search.value);
            showMessage(formMsg, "Deleted", true);
          } catch (err) {
            console.error("Delete failed:", err);
            alert("Delete failed: " + (err.message || "unknown"));
          }
        });
      });

      document.querySelectorAll(".editBtn").forEach((b) => {
        b.addEventListener("click", () => {
          const tr = b.closest("tr");
          if (!tr) return;
          const eid = tr.getAttribute("data-eid");
          document.getElementById("edit_employee_id").value = eid || "";
          document.getElementById("edit_username").value =
            tr.getAttribute("data-username") || "";
          document.getElementById("edit_email").value =
            tr.getAttribute("data-email") || "";
          document.getElementById("edit_full_name").value =
            tr.getAttribute("data-full_name") || "";
          document.getElementById("edit_user_type").value =
            tr.getAttribute("data-user_type") || "EMPLOYEE";
          document.getElementById("edit_employee_code").value =
            tr.getAttribute("data-employee_code") || "";
          document.getElementById("edit_job_title").value =
            tr.getAttribute("data-job_title") || "";
          document.getElementById("edit_salary").value =
            tr.getAttribute("data-salary") || "";
          document.getElementById("edit_hire_date").value =
            tr.getAttribute("data-hire_date") || "";
          document.getElementById("edit_manager_id").value =
            tr.getAttribute("data-manager_id") || "";
          document.getElementById("edit_department_id").value =
            tr.getAttribute("data-department_id") || "";

          openEditModal();
        });
      });
    } catch (err) {
      console.error("loadEmployees error:", err);
      const friendly = err.message || "Failed to load";
      tbody.innerHTML = `<tr><td colspan="7" class="p-4 text-red-500">${escapeHtml(
        friendly
      )}</td></tr>`;
    }
  }

  // small escape function
  function escapeHtml(s) {
    if (s === null || s === undefined) return "";
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  // CREATE new (multipart with optional profile_image)
  createForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(createForm);

    const username = (formData.get("username") || "").toString().trim();
    const email = (formData.get("email") || "").toString().trim();

    if (!username || !email) {
      showMessage(formMsg, "username and email required", false);
      return;
    }

    formData.set("username", username);
    formData.set("email", email);

    createBtn.disabled = true;
    createBtn.textContent = "Creating...";

    try {
      const headers = authHeaders();
      delete headers["Content-Type"];

      const res = await fetch(`${API_BASE}/api/admin/employees`, {
        method: "POST",
        headers,
        body: formData,
      });

      if (!res.ok) {
        let payload = {};
        try {
          payload = await res.json();
        } catch (_) {}
        throw new Error(payload.error || payload.message || "Create failed");
      }

      showMessage(formMsg, "Created", true);
      createForm.reset();
      await loadEmployees(search.value);
    } catch (err) {
      console.error("Create error:", err);
      showMessage(formMsg, err.message || "Create failed", false);
    } finally {
      createBtn.disabled = false;
      createBtn.textContent = "Create";
    }
  });

  // refresh, search debounce
  refreshBtn.addEventListener("click", () => loadEmployees(search.value));
  search.addEventListener(
    "input",
    debounce(() => loadEmployees(search.value), 300)
  );

  // EDIT form submit (multipart with optional new profile_image)
  editForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    editMsg.textContent = "";
    saveEditBtn.disabled = true;
    saveEditBtn.textContent = "Saving...";

    const eid = document.getElementById("edit_employee_id").value;
    if (!eid) {
      editMsg.textContent = "Missing employee id";
      saveEditBtn.disabled = false;
      saveEditBtn.textContent = "Save changes";
      return;
    }

    const formData = new FormData(editForm);

    // Normalize hire_date to YYYY-MM-DD if present
    const rawHire = formData.get("hire_date") || "";
    if (rawHire) {
      let hire_date = rawHire.toString();
      try {
        if (hire_date.indexOf("T") !== -1) {
          hire_date = hire_date.split("T")[0];
        } else if (hire_date.length > 10) {
          hire_date = hire_date.slice(0, 10);
        }
      } catch {
        /* ignore */
      }
      formData.set("hire_date", hire_date);
    }

    // Trim username/email/full_name
    const uname = (formData.get("username") || "").toString().trim();
    const mail = (formData.get("email") || "").toString().trim();
    const fname = (formData.get("full_name") || "").toString().trim();

    if (!uname || !mail) {
      editMsg.style.color = "red";
      editMsg.textContent = "username and email are required";
      saveEditBtn.disabled = false;
      saveEditBtn.textContent = "Save changes";
      return;
    }

    formData.set("username", uname);
    formData.set("email", mail);
    formData.set("full_name", fname);

    try {
      const headers = authHeaders();
      delete headers["Content-Type"];

      const res = await fetch(`${API_BASE}/api/admin/employees/${eid}`, {
        method: "PUT",
        headers,
        body: formData,
      });

      if (!res.ok) {
        let payload = {};
        try {
          payload = await res.json();
        } catch (_) {}
        throw new Error(payload.error || payload.message || "Update failed");
      }

      editMsg.style.color = "green";
      editMsg.textContent = "Saved — updating list...";
      await loadEmployees(search.value);
      setTimeout(() => closeEditModal(), 700);
    } catch (err) {
      console.error("Edit failed:", err);
      editMsg.style.color = "red";
      editMsg.textContent = err.message || "Update failed";
    } finally {
      saveEditBtn.disabled = false;
      saveEditBtn.textContent = "Save changes";
    }
  });

  // initial load
  loadEmployees();

  // logout (go to login)
  document.getElementById("logoutBtn")?.addEventListener("click", () => {
    localStorage.removeItem("access_token");
    localStorage.removeItem("user");
    window.location.href = "/index.html";
  });

  // small debounce
  function debounce(fn, wait) {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), wait);
    };
  }
});
