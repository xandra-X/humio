document.addEventListener("DOMContentLoaded", () => {
  const tbody = document.getElementById("employeeTbody");
  const searchInput = document.getElementById("searchInput");

  const editModal = document.getElementById("editModal");
  const editBackdrop = document.getElementById("editBackdrop");
  const closeEdit = document.getElementById("closeEdit");
  const cancelEdit = document.getElementById("cancelEdit");
  const editForm = document.getElementById("editForm");
  const editMsg = document.getElementById("editMsg");

  /* ===============================
     CONFIG
  =============================== */
  const API_BASE =
    window.location.hostname === "localhost"
      ? "http://localhost:3000"
      : "https://humio-production.up.railway.app";


  function authHeaders() {
    const token = localStorage.getItem("access_token");
    return token ? { Authorization: "Bearer " + token } : {};
  }

  /* ===============================
     MODAL
  =============================== */
  function openEdit() {
    editModal.classList.remove("hidden");
    editModal.classList.add("flex");
  }

  function closeEditModal() {
    editModal.classList.add("hidden");
    editModal.classList.remove("flex");
    editForm.reset();
    editMsg.textContent = "";
  }

  closeEdit.onclick =
    cancelEdit.onclick =
    editBackdrop.onclick =
      closeEditModal;

  /* ===============================
     FETCH
  =============================== */
  async function fetchEmployees(q = "") {
    const url =
      "/api/admin/employees" + (q ? "?q=" + encodeURIComponent(q) : "");
    const res = await fetch(url, { headers: authHeaders() });
    if (!res.ok) throw new Error("Failed to load employees");
    return res.json();
  }

  /* ===============================
     RENDER
  =============================== */
  function render(rows) {
    tbody.innerHTML = rows
      .map((e) => {
        // ✅ FIX IMAGE URL
        const img = e.profile_image ? `${API_BASE}${e.profile_image}` : "";

        return `
          <tr class="border-t"
            data-id="${e.employee_id}"
            data-username="${e.username || ""}"
            data-email="${e.email || ""}"
            data-full_name="${e.full_name || ""}"
            data-user_type="${e.user_type || "EMPLOYEE"}"
            data-employee_code="${e.employee_code || ""}"
            data-job_title="${e.job_title || ""}"
            data-hire_date="${e.hire_date || ""}"
            data-department_id="${e.department_id || ""}"
            data-profile_image="${img}"
          >
            <td class="p-3">
              ${
                img
                  ? `<img src="${img}" class="w-10 h-10 rounded-full object-cover border" />`
                  : `<div class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-xs text-gray-500">N/A</div>`
              }
            </td>
            <td class="p-3">${e.employee_code || ""}</td>
            <td class="p-3">${e.full_name || e.username}</td>
            <td class="p-3">${e.job_title || e.user_type}</td>
            <td class="p-3">${e.department_name || ""}</td>
            <td class="p-3">${e.email}</td>
            <td class="p-3 space-x-3">
              <button class="editBtn text-blue-600 hover:underline">Edit</button>
              <button class="delBtn text-red-600 hover:underline">Delete</button>
            </td>
          </tr>
        `;
      })
      .join("");

    attachRowActions();
  }

  /* ===============================
     ROW ACTIONS
  =============================== */
  function attachRowActions() {
    document.querySelectorAll(".editBtn").forEach((btn) => {
      btn.onclick = () => {
        const tr = btn.closest("tr");

        editForm.edit_employee_id.value = tr.dataset.id;
        editForm.edit_username.value = tr.dataset.username;
        editForm.edit_email.value = tr.dataset.email;
        editForm.edit_full_name.value = tr.dataset.full_name;
        editForm.edit_user_type.value = tr.dataset.user_type;
        editForm.edit_employee_code.value = tr.dataset.employee_code;
        editForm.edit_job_title.value = tr.dataset.job_title;
        editForm.edit_hire_date.value =
          tr.dataset.hire_date?.split("T")[0] || "";
        editForm.edit_department_id.value = tr.dataset.department_id;

        // ✅ IMAGE PREVIEW
        const preview = document.getElementById("editProfilePreview");
        if (preview && tr.dataset.profile_image) {
          preview.src = tr.dataset.profile_image;
          preview.classList.remove("hidden");
        }

        openEdit();
      };
    });

    document.querySelectorAll(".delBtn").forEach((btn) => {
      btn.onclick = async () => {
        const id = btn.closest("tr").dataset.id;
        if (!confirm("Delete this employee?")) return;

        await fetch("/api/admin/employees/" + id, {
          method: "DELETE",
          headers: authHeaders(),
        });

        load(searchInput.value);
      };
    });
  }

  /* ===============================
     LOAD
  =============================== */
  async function load(q = "") {
    tbody.innerHTML = `<tr><td colspan="7" class="p-4">Loading...</td></tr>`;
    render(await fetchEmployees(q));
  }

  /* ===============================
     EDIT SUBMIT
  =============================== */
  editForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    editMsg.textContent = "";

    const id = editForm.edit_employee_id.value;
    const fd = new FormData(editForm);

    // Fix ISO date
    const hireDate = fd.get("hire_date");
    if (hireDate && hireDate.includes("T")) {
      fd.set("hire_date", hireDate.split("T")[0]);
    }

    // Prevent overwriting image with empty
    const imgInput = editForm.querySelector('input[name="profile_image"]');
    if (imgInput && imgInput.files.length === 0) {
      fd.delete("profile_image");
    }

    const res = await fetch("/api/admin/employees/" + id, {
      method: "PUT",
      headers: authHeaders(),
      body: fd,
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      editMsg.textContent = err.error || "Update failed";
      editMsg.className = "text-red-600";
      return;
    }

    editMsg.textContent = "Saved successfully";
    editMsg.className = "text-green-600";

    setTimeout(() => {
      closeEditModal();
      load(searchInput.value);
    }, 600);
  });

  /* ===============================
     SEARCH
  =============================== */
  let t;
  searchInput.oninput = () => {
    clearTimeout(t);
    t = setTimeout(() => load(searchInput.value), 300);
  };

  /* ===============================
     INIT
  =============================== */
  load();
});
