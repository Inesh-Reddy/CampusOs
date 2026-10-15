import { api, requireAuth } from "../api/client.js";
import { renderNav } from "../components/nav.js";
import { badge, el, fieldErrorText, showNotice } from "../utils/format.js";

const me = await requireAuth();
renderNav(me);
const notice = document.getElementById("notice");

function userTable(rows) {
  if (!rows.length) return el("p", "No users.", "muted");
  const table = document.createElement("table");
  const head = document.createElement("tr");
  for (const text of ["Name", "Email", "Role", "Department", "Active"]) head.appendChild(el("th", text));
  table.appendChild(head);
  for (const user of rows) {
    const tr = document.createElement("tr");
    tr.appendChild(el("td", user.displayName));
    tr.appendChild(el("td", user.email));
    const roleCell = document.createElement("td"); roleCell.appendChild(badge(user.role)); tr.appendChild(roleCell);
    tr.appendChild(el("td", user.department || "—"));
    tr.appendChild(el("td", user.active ? "Yes" : "No"));
    table.appendChild(tr);
  }
  return table;
}

async function load() {
  try {
    document.getElementById("users").replaceChildren(userTable(await api.get("/admin/users")));
  } catch (error) {
    document.getElementById("users").replaceChildren(el("p", "Campus administrator access is required to list all users.", "muted"));
    showNotice(notice, error.message, "error");
  }
  try {
    document.getElementById("technicians").replaceChildren(userTable(await api.get("/admin/users/technicians")));
  } catch (error) {
    document.getElementById("technicians").replaceChildren(el("p", error.message, "muted"));
  }
}

document.getElementById("user-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const body = {
    email: document.getElementById("u-email").value.trim(),
    password: document.getElementById("u-password").value,
    displayName: document.getElementById("u-name").value.trim(),
    role: document.getElementById("u-role").value,
    department: document.getElementById("u-dept").value.trim() || null,
  };
  try {
    await api.post("/admin/users", body);
    showNotice(notice, "User created.", "success");
    event.target.reset();
    await load();
  } catch (error) { showNotice(notice, fieldErrorText(error), "error"); }
});

await load();
