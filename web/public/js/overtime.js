const headers = {
  "Content-Type": "application/json",
  Authorization: "Bearer " + localStorage.getItem("access_token"),
};

function formatDate(d) {
  return new Date(d).toISOString().slice(0, 10);
}

async function loadOvertime() {
  const res = await fetch("/api/overtime", { headers });
  const data = await res.json();
  const tbody = document.getElementById("overtimeTable");

  tbody.innerHTML = data
    .map(
      (o) => `
    <tr class="border-t hover:bg-gray-50">
      <td class="p-3 text-center">${formatDate(o.overtime_date)}</td>
      <td class="p-3 text-center">${o.hours}</td>
      <td class="p-3 text-center">$${o.hourly_rate}</td>
      <td class="p-3 text-center">
        <span class="px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs">
          ${o.employee_count} employee(s)
        </span>
      </td>
      <td class="p-3 text-center space-x-3">
        <button
          onclick="editOvertime(${o.overtime_id})"
          class="text-blue-600 hover:underline">
          Edit
        </button>
        <button
          onclick="remove(${o.overtime_id})"
          class="text-red-600 hover:underline">
          Delete
        </button>
      </td>
    </tr>
  `
    )
    .join("");
}


async function openModal(id = null) {
  document.getElementById("form").reset();
  document.getElementById("overtime_id").value = id || "";
  document.getElementById("modal").classList.remove("hidden");

  const empRes = await fetch("/api/admin/employees", { headers });
  const emps = await empRes.json();
  document.getElementById("employees").innerHTML = emps
    .map((e) => `<option value="${e.employee_id}">${e.employee_code}</option>`)
    .join("");
}

function closeModal() {
  document.getElementById("modal").classList.add("hidden");
}

document.getElementById("form").onsubmit = async (e) => {
  e.preventDefault();

  const id = overtime_id.value;
  const payload = {
    overtime_date: date.value,
    hours: hours.value,
    hourly_rate: rate.value,
  };

  const method = id ? "PUT" : "POST";
  const url = "/api/overtime" + (id ? "/" + id : "");

  const res = await fetch(url, {
    method,
    headers,
    body: JSON.stringify(payload),
  });

  const out = await res.json();
  const overtimeId = out.overtime_id || id;

  const employee_ids = [...employees.selectedOptions].map((o) => o.value);

  await fetch(`/api/overtime/${overtimeId}/assign`, {
    method: "POST",
    headers,
    body: JSON.stringify({ employee_ids }),
  });

  closeModal();
  loadOvertime();
};

async function remove(id) {
  if (!confirm("Delete overtime?")) return;
  await fetch("/api/overtime/" + id, { method: "DELETE", headers });
  loadOvertime();
}

loadOvertime();
async function editOvertime(id) {
  openModal(id);

  // load overtime
  const res = await fetch("/api/overtime", { headers });
  const list = await res.json();
  const overtime = list.find((o) => o.overtime_id === id);

  if (!overtime) return alert("Overtime not found");

  document.getElementById("overtime_id").value = id;
  document.getElementById("date").value = formatDate(overtime.overtime_date);
  document.getElementById("hours").value = overtime.hours;
  document.getElementById("rate").value = overtime.hourly_rate;

  // load assigned employees
  const empRes = await fetch(`/api/overtime/${id}/employees`, { headers });
  const assigned = await empRes.json();
  const assignedIds = assigned.map((e) => String(e.employee_id));

  // preselect employees
  [...employees.options].forEach((opt) => {
    opt.selected = assignedIds.includes(opt.value);
  });
}
