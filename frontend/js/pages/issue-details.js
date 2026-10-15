import { api, requireAuth } from "../api/client.js";
import { renderNav } from "../components/nav.js";
import { badge, el, fmtDate, showNotice } from "../utils/format.js";

const me = await requireAuth();
renderNav(me);
const notice = document.getElementById("notice");
const id = new URLSearchParams(location.search).get("id");
if (!id) { showNotice(notice, "No issue id in the URL.", "error"); throw new Error("missing id"); }

function detailRow(label, valueNode) {
  const wrap = el("p");
  wrap.appendChild(el("strong", label + " "));
  if (typeof valueNode === "string") wrap.appendChild(document.createTextNode(valueNode));
  else wrap.appendChild(valueNode);
  return wrap;
}

async function load() {
  const detail = await api.get("/issues/" + id);
  const issue = detail.issue;
  document.getElementById("title").textContent = issue.reference + " · " + issue.title;
  const mount = document.getElementById("detail");
  const statusCell = document.createElement("span"); statusCell.appendChild(badge(issue.status));
  const priorityCell = document.createElement("span"); priorityCell.appendChild(badge(issue.priority));
  mount.replaceChildren(
    detailRow("Status:", statusCell),
    detailRow("Priority:", priorityCell),
    detailRow("Category:", issue.category.replaceAll("_", " ")),
    detailRow("Where:", issue.building + (issue.locationDetail ? " — " + issue.locationDetail : "")),
    detailRow("Reporter:", issue.reporterName),
    detailRow("Assignee:", issue.assigneeName || "Unassigned"),
    detailRow("Reported:", fmtDate(issue.createdAt)),
    detailRow("Description:", issue.description),
  );

  const transitions = document.getElementById("transitions");
  transitions.replaceChildren();
  for (const target of detail.allowedTransitions || []) {
    const button = el("button", "Move to " + target.replaceAll("_", " "), target === "CANCELLED" || target === "REJECTED" ? "danger" : "");
    button.type = "button";
    button.addEventListener("click", async () => {
      const note = document.getElementById("note").value.trim();
      if (target === "RESOLVED" && !note) { showNotice(notice, "Resolution notes are required to resolve an issue.", "error"); return; }
      try {
        await api.post("/issues/" + id + "/transitions", { toStatus: target, note: note || null });
        showNotice(notice, "Issue moved to " + target + ".", "success");
        await load();
      } catch (error) { showNotice(notice, error.message, "error"); }
    });
    transitions.appendChild(button);
  }
  if (!(detail.allowedTransitions || []).length) transitions.appendChild(el("p", "No further transitions from this status.", "muted"));

  const activity = document.getElementById("activity");
  activity.replaceChildren();
  for (const item of detail.activity || []) {
    const li = document.createElement("li");
    li.appendChild(el("strong", item.action + " "));
    li.appendChild(document.createTextNode("by " + item.actorName + " · " + fmtDate(item.createdAt)));
    if (item.note) li.appendChild(el("div", item.note, "small muted"));
    activity.appendChild(li);
  }
}

try {
  const technicians = await api.get("/admin/users/technicians");
  const select = document.getElementById("technician");
  select.replaceChildren();
  for (const tech of technicians) {
    const option = el("option", tech.displayName + " (" + tech.email + ")");
    option.value = tech.id;
    select.appendChild(option);
  }
} catch {
  document.getElementById("assign-panel").style.display = "none";
}

document.getElementById("assign-btn").addEventListener("click", async () => {
  try {
    await api.post("/issues/" + id + "/assign", { technicianId: Number(document.getElementById("technician").value) });
    showNotice(notice, "Technician assigned.", "success");
    await load();
  } catch (error) { showNotice(notice, error.message, "error"); }
});

try { await load(); } catch (error) { showNotice(notice, error.message, "error"); }
