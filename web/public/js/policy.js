// web/public/js/policy.js
document.addEventListener("DOMContentLoaded", () => {
  // --- helpers ---
  function authHeaders(contentType = "application/json") {
    const token = localStorage.getItem("access_token");
    const h = {};
    if (contentType) h["Content-Type"] = contentType;
    if (token) h["Authorization"] = "Bearer " + token;
    return h;
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
    const res = await fetch(url, opts);
    if (res.status === 401) {
      // unauthorized - redirect to login
      localStorage.removeItem("access_token");
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
      const err =
        (data && (data.error || data.message)) || res.statusText || "API error";
      throw new Error(err);
    }
    return data;
  }

  const api = {
    list: () => apiFetch("/api/policies", { method: "GET" }),
    create: (p) =>
      apiFetch("/api/policies", { method: "POST", body: JSON.stringify(p) }),
    update: (id, p) =>
      apiFetch("/api/policies/" + id, {
        method: "PUT",
        body: JSON.stringify(p),
      }),
    del: (id) => apiFetch("/api/policies/" + id, { method: "DELETE" }),
  };

  // DOM refs
  const listEl = document.getElementById("policiesList");
  const createPanel = document.getElementById("createPanel");
  const toggleCreate = document.getElementById("toggleCreate");
  const cancelCreate = document.getElementById("cancelCreate");
  const createBtn = document.getElementById("createPolicyBtn");
  const createMsg = document.getElementById("createMsg");
  const searchInput = document.getElementById("policySearch");

  // Edit modal refs
  const editModal = document.getElementById("editModal");
  const closeEdit = document.getElementById("closeEdit");
  const cancelEditBtn = document.getElementById("cancelEditBtn");
  const editForm = document.getElementById("editForm");
  const editMsg = document.getElementById("editMsg");
  const deletePolicyBtn = document.getElementById("deletePolicyBtn");

  // show/hide create panel
  toggleCreate.addEventListener("click", () =>
    createPanel.classList.toggle("hidden")
  );
  cancelCreate.addEventListener("click", () =>
    createPanel.classList.add("hidden")
  );

  // logout button (if present)
  document.getElementById("logoutBtn")?.addEventListener("click", () => {
    localStorage.removeItem("access_token");
    localStorage.removeItem("user");
    location.href = "/index.html";
  });

  // escape html
  function escapeHtml(s) {
    if (!s && s !== 0) return "";
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  }

  // render list
  async function load() {
    listEl.innerHTML = `<div class="p-4 text-gray-600">Loading...</div>`;
    let rows = [];
    try {
      rows = await api.list();
    } catch (err) {
      listEl.innerHTML = `<div class="text-red-500 p-4">Failed to load policies: ${escapeHtml(
        err.message || ""
      )}</div>`;
      console.error(err);
      return;
    }

    const q = (searchInput.value || "").toLowerCase().trim();
    if (q)
      rows = rows.filter(
        (r) =>
          (r.title || "").toLowerCase().includes(q) ||
          (r.category || "").toLowerCase().includes(q) ||
          (r.description || "").toLowerCase().includes(q)
      );

    if (!rows || !rows.length) {
      listEl.innerHTML = `<div class="p-4 text-gray-600">No policies found.</div>`;
      return;
    }

    listEl.innerHTML = rows
      .map((p) => {
        const eff = p.effective_date
          ? new Date(p.effective_date).toLocaleDateString()
          : "—";
        const statusTag = `<span class="px-3 py-1 rounded ${
          p.status === "ACTIVE"
            ? "bg-green-100 text-green-700"
            : "bg-gray-100 text-gray-700"
        }">${escapeHtml(p.status || "UNKNOWN")}</span>`;
        return `<div class="bg-white p-6 rounded shadow flex justify-between">
        <div>
          <h3 class="font-semibold">${escapeHtml(p.title)}</h3>
          <div class="text-sm text-gray-500">${escapeHtml(
            p.category || "General"
          )}</div>
          <p class="mt-3 text-gray-700">${escapeHtml(p.description || "")}</p>
          <div class="mt-3 text-sm text-gray-500">Effective: ${escapeHtml(
            eff
          )}</div>
        </div>
        <div class="flex flex-col items-end gap-2">
          ${statusTag}
          <div class="flex gap-2">
            <button data-id="${
              p.policy_id
            }" data-action="edit" class="text-blue-600">Edit</button>
            <button data-id="${
              p.policy_id
            }" data-action="delete" class="text-red-600">Delete</button>
          </div>
        </div>
      </div>`;
      })
      .join("");

    // attach handlers
    listEl.querySelectorAll("button[data-action]").forEach((btn) => {
      btn.addEventListener("click", async (ev) => {
        const id = Number(btn.dataset.id);
        const act = btn.dataset.action;
        if (act === "edit") {
          const policy = rows.find((r) => r.policy_id === id);
          if (!policy) return alert("Policy not found in current list");
          openEditModal(policy);
        } else if (act === "delete") {
          if (!confirm("Delete policy?")) return;
          try {
            await api.del(id);
            await load();
          } catch (err) {
            alert("Delete failed: " + (err.message || "error"));
            console.error(err);
          }
        }
      });
    });
  }

  // create new policy
  createBtn.addEventListener("click", async () => {
    createMsg.textContent = "";
    const title = document.getElementById("p_title").value.trim();
    const category = document.getElementById("p_category").value.trim();
    const description = document.getElementById("p_description").value.trim();
    const effective_date = document.getElementById("p_effective").value || null;
    if (!title) {
      createMsg.textContent = "Title required";
      createMsg.style.color = "red";
      return;
    }
    createBtn.disabled = true;
    createBtn.textContent = "Creating...";
    try {
      await api.create({
        title,
        category: category || null,
        description: description || null,
        effective_date,
      });
      document.getElementById("p_title").value = "";
      document.getElementById("p_category").value = "";
      document.getElementById("p_description").value = "";
      document.getElementById("p_effective").value = "";
      createPanel.classList.add("hidden");
      createMsg.textContent = "";
      await load();
    } catch (err) {
      createMsg.textContent = err.message || "Create failed";
      createMsg.style.color = "red";
      console.error(err);
    } finally {
      createBtn.disabled = false;
      createBtn.textContent = "Create Policy";
    }
  });

  // search
  searchInput.addEventListener("input", debounce(load, 250));

  // --- edit modal functions ---
  function openEditModal(policy) {
    editMsg.textContent = "";
    document.getElementById("edit_policy_id").value = policy.policy_id;
    document.getElementById("edit_title").value = policy.title || "";
    document.getElementById("edit_category").value = policy.category || "";
    document.getElementById("edit_description").value =
      policy.description || "";
    document.getElementById("edit_effective").value = policy.effective_date
      ? policy.effective_date.split("T")[0]
      : "";
    editModal.classList.remove("hidden");
    editModal.classList.add("flex");
    setTimeout(() => document.getElementById("edit_title").focus(), 50);
  }

  function closeEditModal() {
    editModal.classList.add("hidden");
    editModal.classList.remove("flex");
  }

  closeEdit.addEventListener("click", closeEditModal);
  cancelEditBtn.addEventListener("click", (e) => {
    e.preventDefault();
    closeEditModal();
  });

  editForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    editMsg.textContent = "";
    const id = Number(document.getElementById("edit_policy_id").value);
    const payload = {
      title: document.getElementById("edit_title").value.trim(),
      category: document.getElementById("edit_category").value.trim() || null,
      description:
        document.getElementById("edit_description").value.trim() || null,
      effective_date: document.getElementById("edit_effective").value || null,
    };
    if (!payload.title) {
      editMsg.textContent = "Title required";
      editMsg.style.color = "red";
      return;
    }
    document.getElementById("saveEditBtn").disabled = true;
    document.getElementById("saveEditBtn").textContent = "Saving...";
    try {
      await api.update(id, payload);
      editMsg.textContent = "Saved";
      editMsg.style.color = "green";
      await load();
      setTimeout(closeEditModal, 500);
    } catch (err) {
      editMsg.textContent = err.message || "Save failed";
      editMsg.style.color = "red";
      console.error(err);
    } finally {
      document.getElementById("saveEditBtn").disabled = false;
      document.getElementById("saveEditBtn").textContent = "Save";
    }
  });

  deletePolicyBtn.addEventListener("click", async () => {
    const id = Number(document.getElementById("edit_policy_id").value);
    if (!id) return;
    if (!confirm("Delete policy?")) return;
    try {
      await api.del(id);
      closeEditModal();
      await load();
    } catch (err) {
      editMsg.textContent = err.message || "Delete failed";
      editMsg.style.color = "red";
      console.error(err);
    }
  });

  // small debounce
  function debounce(fn, wait) {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), wait);
    };
  }

  // initial load
  load();
});
