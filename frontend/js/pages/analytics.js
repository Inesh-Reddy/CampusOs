import { api, requireAuth } from "../api/client.js";
import { renderNav } from "../components/nav.js";
import { el, showNotice, toInstant } from "../utils/format.js";

const me = await requireAuth();
renderNav(me);
const notice = document.getElementById("notice");

function kpi(label, value) {
  const box = el("div", null, "kpi");
  box.appendChild(el("div", value, "num"));
  box.appendChild(el("div", label, "label"));
  return box;
}

function nameCountTable(rows) {
  if (!rows || !rows.length) return el("p", "No data in this range.", "muted");
  const table = document.createElement("table");
  const head = document.createElement("tr");
  head.appendChild(el("th", "Name")); head.appendChild(el("th", "Count"));
  table.appendChild(head);
  for (const row of rows) {
    const tr = document.createElement("tr");
    tr.appendChild(el("td", String(row.name).replaceAll("_", " ")));
    tr.appendChild(el("td", row.count));
    table.appendChild(tr);
  }
  return table;
}

async function load() {
  const from = document.getElementById("from").value;
  const to = document.getElementById("to").value;
  const params = new URLSearchParams();
  if (from) params.set("from", toInstant(from));
  if (to) params.set("to", toInstant(to));
  const suffix = params.toString() ? "?" + params.toString() : "";
  try {
    const overview = await api.get("/analytics/overview" + suffix);
    document.getElementById("kpis").replaceChildren(
      kpi("Total issues", overview.totalIssues),
      kpi("Open issues", overview.openIssues),
      kpi("Critical open", overview.criticalIssues),
      kpi("Overdue (>48h)", overview.overdueIssues),
      kpi("Avg resolution (h)", overview.avgResolutionHours),
      kpi("Resolved in range", overview.resolvedInRange),
      kpi("Utilisation (estimate)", overview.utilizationRate),
      kpi("Conflict attempts", overview.conflictAttempts),
      kpi("Total bookings", overview.totalBookings),
      kpi("Resources", overview.totalResources),
    );
    const issues = await api.get("/analytics/issues");
    document.getElementById("by-category").replaceChildren(nameCountTable(issues.byCategory));
    document.getElementById("by-status").replaceChildren(nameCountTable(issues.byStatus));
    document.getElementById("top-buildings").replaceChildren(nameCountTable(issues.topBuildings));

    const usage = await api.get("/analytics/resources");
    const mount = document.getElementById("usage");
    if (!usage.length) { mount.replaceChildren(el("p", "No resources yet.", "muted")); return; }
    const table = document.createElement("table");
    const head = document.createElement("tr");
    for (const text of ["Resource", "Bookings", "Booked hours (est.)", "Utilisation (est.)"]) head.appendChild(el("th", text));
    table.appendChild(head);
    for (const row of usage) {
      const tr = document.createElement("tr");
      tr.appendChild(el("td", row.resourceName));
      tr.appendChild(el("td", row.bookingCount));
      tr.appendChild(el("td", row.bookedHours));
      tr.appendChild(el("td", row.utilizationRate));
      table.appendChild(tr);
    }
    mount.replaceChildren(table);
  } catch (error) { showNotice(notice, error.message, "error"); }
}

document.getElementById("range-form").addEventListener("submit", (e) => { e.preventDefault(); load(); });
await load();
