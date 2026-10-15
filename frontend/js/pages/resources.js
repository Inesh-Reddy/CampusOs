import { api, requireAuth } from "../api/client.js";
import { renderNav } from "../components/nav.js";
import { badge, el, fieldErrorText, showNotice } from "../utils/format.js";

const me = await requireAuth();
renderNav(me);
const notice = document.getElementById("notice");
const listMount = document.getElementById("resource-list");
const isAdmin = me.role === "DEPT_ADMIN" || me.role === "CAMPUS_ADMIN";
if (!isAdmin) document.getElementById("form-title").textContent = "Add a resource (administrators only)";

function renderResources(rows) {
  if (!rows.length) { listMount.replaceChildren(el("p", "No resources match.", "muted")); return; }
  const table = document.createElement("table");
  const head = document.createElement("tr");
  for (const text of ["Name", "Type", "Building", "Capacity", "Status", "Approval", "Equipment", ""]) head.appendChild(el("th", text));
  table.appendChild(head);
  for (const resource of rows) {
    const tr = document.createElement("tr");
    tr.appendChild(el("td", resource.name));
    tr.appendChild(el("td", resource.type.replaceAll("_", " ")));
    tr.appendChild(el("td", resource.building + (resource.location ? " — " + resource.location : "")));
    tr.appendChild(el("td", resource.capacity ?? "—"));
    const s = document.createElement("td"); s.appendChild(badge(resource.status)); tr.appendChild(s);
    tr.appendChild(el("td", resource.requiresApproval ? "Required" : "Not required"));
    tr.appendChild(el("td", (resource.equipment || []).join(", ") || "—"));
    const actions = document.createElement("td");
    const book = el("a", "Book", "btn secondary"); book.href = "bookings.html?resourceId=" + resource.id;
    actions.appendChild(book);
    if (isAdmin) {
      const edit = el("button", "Edit", "secondary");
      edit.type = "button";
      edit.addEventListener("click", () => fillForm(resource));
      actions.appendChild(edit);
    }
    tr.appendChild(actions);
    table.appendChild(tr);
  }
  listMount.replaceChildren(table);
}

function fillForm(resource) {
  document.getElementById("r-id").value = resource.id;
  document.getElementById("r-name").value = resource.name;
  document.getElementById("r-type").value = resource.type;
  document.getElementById("r-building").value = resource.building;
  document.getElementById("r-location").value = resource.location || "";
  document.getElementById("r-capacity").value = resource.capacity ?? "";
  document.getElementById("r-description").value = resource.description || "";
  document.getElementById("r-equipment").value = (resource.equipment || []).join(", ");
  document.getElementById("r-approval").value = String(resource.requiresApproval);
  document.getElementById("r-status").value = resource.status;
  document.getElementById("form-title").textContent = "Edit resource #" + resource.id;
  window.scrollTo({ top: 0 });
}

async function load() {
  const params = new URLSearchParams();
  const q = document.getElementById("q").value.trim();
  const type = document.getElementById("f-type").value;
  const building = document.getElementById("f-building").value.trim();
  const capacity = document.getElementById("f-capacity").value;
  if (q) params.set("q", q);
  if (type) params.set("type", type);
  if (building) params.set("building", building);
  if (capacity) params.set("minCapacity", capacity);
  try { renderResources(await api.get("/resources?" + params.toString())); }
  catch (error) { showNotice(notice, error.message, "error"); }
}

document.getElementById("filter-form").addEventListener("submit", (e) => { e.preventDefault(); load(); });
document.getElementById("reset-btn").addEventListener("click", () => {
  document.getElementById("resource-form").reset();
  document.getElementById("r-id").value = "";
  document.getElementById("form-title").textContent = "Add a resource";
});

document.getElementById("resource-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const equipment = document.getElementById("r-equipment").value.split(",").map((s) => s.trim()).filter(Boolean);
  const body = {
    name: document.getElementById("r-name").value.trim(),
    type: document.getElementById("r-type").value,
    building: document.getElementById("r-building").value.trim(),
    location: document.getElementById("r-location").value.trim() || null,
    capacity: document.getElementById("r-capacity").value ? Number(document.getElementById("r-capacity").value) : null,
    description: document.getElementById("r-description").value.trim() || null,
    requiresApproval: document.getElementById("r-approval").value === "true",
    equipment,
  };
  const idValue = document.getElementById("r-id").value;
  try {
    if (idValue) {
      body.status = document.getElementById("r-status").value;
      await api.patch("/resources/" + idValue, body);
      showNotice(notice, "Resource updated.", "success");
    } else {
      await api.post("/resources", body);
      showNotice(notice, "Resource created.", "success");
    }
    event.target.reset();
    document.getElementById("r-id").value = "";
    await load();
  } catch (error) { showNotice(notice, fieldErrorText(error), "error"); }
});

await load();
