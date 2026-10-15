import { api, requireAuth } from "../api/client.js";
import { renderNav } from "../components/nav.js";
import { badge, el, fmtDate, showNotice } from "../utils/format.js";

const notice = document.getElementById("notice");

function kpi(label, value) {
  const box = el("div", null, "kpi");
  box.appendChild(el("div", value, "num"));
  box.appendChild(el("div", label, "label"));
  return box;
}

function issueTable(rows) {
  if (!rows.length) return el("p", "No issues yet.", "muted");
  const table = document.createElement("table");
  const head = document.createElement("tr");
  for (const text of ["Reference", "Title", "Status", "Priority", "Reported"]) head.appendChild(el("th", text));
  table.appendChild(head);
  for (const issue of rows) {
    const tr = document.createElement("tr");
    tr.className = "clickable";
    tr.addEventListener("click", () => { location.href = "issue-details.html?id=" + issue.id; });
    tr.appendChild(el("td", issue.reference));
    tr.appendChild(el("td", issue.title));
    const statusCell = document.createElement("td"); statusCell.appendChild(badge(issue.status)); tr.appendChild(statusCell);
    const priorityCell = document.createElement("td"); priorityCell.appendChild(badge(issue.priority)); tr.appendChild(priorityCell);
    tr.appendChild(el("td", fmtDate(issue.createdAt)));
    table.appendChild(tr);
  }
  return table;
}

function bookingTable(rows) {
  if (!rows.length) return el("p", "No bookings yet.", "muted");
  const table = document.createElement("table");
  const head = document.createElement("tr");
  for (const text of ["Resource", "Purpose", "Starts", "Status"]) head.appendChild(el("th", text));
  table.appendChild(head);
  for (const booking of rows) {
    const tr = document.createElement("tr");
    tr.appendChild(el("td", booking.resourceName));
    tr.appendChild(el("td", booking.purpose));
    tr.appendChild(el("td", fmtDate(booking.startAt)));
    const cell = document.createElement("td"); cell.appendChild(badge(booking.status)); tr.appendChild(cell);
    table.appendChild(tr);
  }
  return table;
}

const me = await requireAuth();
renderNav(me);
document.getElementById("welcome").textContent = "Welcome, " + me.displayName;

try {
  const overview = await api.get("/analytics/overview");
  const kpis = document.getElementById("kpis");
  kpis.replaceChildren(
    kpi("Total issues", overview.totalIssues),
    kpi("Open issues", overview.openIssues),
    kpi("Critical open", overview.criticalIssues),
    kpi("Overdue (>48h)", overview.overdueIssues),
    kpi("Avg resolution (h)", overview.avgResolutionHours),
    kpi("Resources", overview.totalResources),
  );
} catch (error) {
  showNotice(notice, "Could not load campus overview: " + error.message, "error");
}

try {
  const mine = await api.get("/issues?mine=true&size=5&sort=createdAt,desc");
  document.getElementById("my-issues").replaceChildren(issueTable(mine.content || []));
  const bookings = await api.get("/bookings?mine=true&size=5");
  document.getElementById("my-bookings").replaceChildren(bookingTable(bookings.content || []));
} catch (error) {
  showNotice(notice, error.message, "error");
}
