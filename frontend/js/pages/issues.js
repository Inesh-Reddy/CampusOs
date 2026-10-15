import { api, requireAuth } from "../api/client.js";
import { renderNav } from "../components/nav.js";
import { badge, el, fieldErrorText, fmtDate, showNotice } from "../utils/format.js";

const me = await requireAuth();
renderNav(me);
const notice = document.getElementById("notice");
const listMount = document.getElementById("issue-list");
let mode = {};

function renderIssues(rows) {
  if (!rows.length) { listMount.replaceChildren(el("p", "No issues match. Try widening the filters.", "muted")); return; }
  const table = document.createElement("table");
  const head = document.createElement("tr");
  for (const text of ["Reference", "Title", "Category", "Priority", "Status", "Assignee", "Reported"]) head.appendChild(el("th", text));
  table.appendChild(head);
  for (const issue of rows) {
    const tr = document.createElement("tr");
    tr.className = "clickable";
    tr.addEventListener("click", () => { location.href = "issue-details.html?id=" + issue.id; });
    tr.appendChild(el("td", issue.reference));
    tr.appendChild(el("td", issue.title));
    tr.appendChild(el("td", issue.category.replaceAll("_", " ")));
    const p = document.createElement("td"); p.appendChild(badge(issue.priority)); tr.appendChild(p);
    const s = document.createElement("td"); s.appendChild(badge(issue.status)); tr.appendChild(s);
    tr.appendChild(el("td", issue.assigneeName || "—"));
    tr.appendChild(el("td", fmtDate(issue.createdAt)));
    table.appendChild(tr);
  }
  listMount.replaceChildren(table);
}

async function load() {
  const params = new URLSearchParams({ size: "20", sort: "createdAt,desc" });
  const q = document.getElementById("q").value.trim();
  const status = document.getElementById("status").value;
  const category = document.getElementById("category").value;
  if (q) params.set("q", q);
  if (status) params.set("status", status);
  if (category) params.set("category", category);
  if (mode.mine) params.set("mine", "true");
  if (mode.assignedToMe) params.set("assignedToMe", "true");
  try {
    const page = await api.get("/issues?" + params.toString());
    renderIssues(page.content || []);
  } catch (error) { showNotice(notice, error.message, "error"); }
}

document.getElementById("filter-form").addEventListener("submit", (e) => { e.preventDefault(); mode = {}; load(); });
document.getElementById("mine-btn").addEventListener("click", () => { mode = { mine: true }; load(); });
document.getElementById("assigned-btn").addEventListener("click", () => { mode = { assignedToMe: true }; load(); });
document.getElementById("overdue-btn").addEventListener("click", async () => {
  try { renderIssues(await api.get("/issues/overdue")); }
  catch (error) { showNotice(notice, error.message, "error"); }
});

document.getElementById("create-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const body = {
    title: document.getElementById("f-title").value.trim(),
    description: document.getElementById("f-description").value.trim(),
    category: document.getElementById("f-category").value,
    building: document.getElementById("f-building").value.trim(),
    locationDetail: document.getElementById("f-location").value.trim() || null,
    affectedPeople: Number(document.getElementById("f-people").value || 1),
    safetyRisk: document.getElementById("f-safety").value === "true",
  };
  const priority = document.getElementById("f-priority").value;
  if (priority) body.priority = priority;
  try {
    const created = await api.post("/issues", body);
    document.getElementById("recommendation").textContent =
      "Created " + created.reference + " as " + created.priority + ". " + (created.priorityExplanation || "");
    showNotice(notice, "Issue " + created.reference + " reported.", "success");
    event.target.reset();
    document.getElementById("f-people").value = "1";
    mode = { mine: true };
    await load();
  } catch (error) { showNotice(notice, fieldErrorText(error), "error"); }
});

await load();
