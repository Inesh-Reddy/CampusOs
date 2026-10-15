import { api, requireAuth } from "../api/client.js";
import { renderNav } from "../components/nav.js";
import { badge, el, fieldErrorText, fmtDate, showNotice, toInstant } from "../utils/format.js";

const me = await requireAuth();
renderNav(me);
const notice = document.getElementById("notice");
const conflictMount = document.getElementById("conflict");
const listMount = document.getElementById("booking-list");
const isAdmin = me.role === "DEPT_ADMIN" || me.role === "CAMPUS_ADMIN";
let mineOnly = true;

function defaultWindow(inputStart, inputEnd, daysAhead) {
  const start = new Date(); start.setDate(start.getDate() + daysAhead); start.setMinutes(0, 0, 0);
  const end = new Date(start.getTime() + 2 * 3600 * 1000);
  const local = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  inputStart.value = local(start); inputEnd.value = local(end);
}
defaultWindow(document.getElementById("b-start"), document.getElementById("b-end"), 1);
defaultWindow(document.getElementById("o-start"), document.getElementById("o-end"), 1);
defaultWindow(document.getElementById("s-from"), document.getElementById("s-to"), 0);

async function fillResourceSelects() {
  const resources = await api.get("/resources");
  const wanted = new URLSearchParams(location.search).get("resourceId");
  for (const selectId of ["b-resource", "s-resource"]) {
    const select = document.getElementById(selectId);
    select.replaceChildren();
    for (const resource of resources) {
      const option = el("option", resource.name + " · " + resource.building);
      option.value = resource.id;
      select.appendChild(option);
    }
    if (wanted) select.value = wanted;
  }
  return resources;
}

function renderBookings(rows) {
  if (!rows.length) { listMount.replaceChildren(el("p", "No bookings to show.", "muted")); return; }
  const table = document.createElement("table");
  const head = document.createElement("tr");
  for (const text of ["Resource", "Purpose", "When", "Status", "Owner", ""]) head.appendChild(el("th", text));
  table.appendChild(head);
  for (const booking of rows) {
    const tr = document.createElement("tr");
    tr.appendChild(el("td", booking.resourceName));
    tr.appendChild(el("td", booking.purpose));
    tr.appendChild(el("td", fmtDate(booking.startAt) + " → " + fmtDate(booking.endAt)));
    const s = document.createElement("td"); s.appendChild(badge(booking.status)); tr.appendChild(s);
    tr.appendChild(el("td", booking.ownerName));
    const actions = document.createElement("td");
    const wrap = el("span", null, "actions");
    if (isAdmin && booking.status === "PENDING") {
      wrap.appendChild(actionButton("Approve", () => act(booking.id, "approve")));
      wrap.appendChild(actionButton("Reject", () => actWithReason(booking.id, "reject")));
    }
    if (booking.status !== "CANCELLED" && booking.status !== "REJECTED") {
      wrap.appendChild(actionButton("Cancel", () => actWithReason(booking.id, "cancel")));
    }
    actions.appendChild(wrap);
    tr.appendChild(actions);
    table.appendChild(tr);
  }
  listMount.replaceChildren(table);
}

function actionButton(label, handler) {
  const button = el("button", label, "secondary");
  button.type = "button";
  button.addEventListener("click", handler);
  return button;
}

async function act(idValue, action) {
  try { await api.post("/bookings/" + idValue + "/" + action); showNotice(notice, "Booking " + action + "d.", "success"); await loadBookings(); }
  catch (error) { showNotice(notice, error.message, "error"); }
}
async function actWithReason(idValue, action) {
  try { await api.post("/bookings/" + idValue + "/" + action, { reason: null }); showNotice(notice, "Booking updated.", "success"); await loadBookings(); }
  catch (error) { showNotice(notice, error.message, "error"); }
}

async function loadBookings() {
  const params = new URLSearchParams({ size: "20", sort: "createdAt,desc" });
  if (mineOnly) params.set("mine", "true");
  try {
    const page = await api.get("/bookings?" + params.toString());
    renderBookings(page.content || []);
  } catch (error) { showNotice(notice, error.message, "error"); }
}

document.getElementById("mine-btn").addEventListener("click", () => { mineOnly = true; loadBookings(); });
document.getElementById("all-btn").addEventListener("click", () => { mineOnly = false; loadBookings(); });

document.getElementById("booking-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  conflictMount.replaceChildren();
  const body = {
    resourceId: Number(document.getElementById("b-resource").value),
    purpose: document.getElementById("b-purpose").value.trim(),
    startAt: toInstant(document.getElementById("b-start").value),
    endAt: toInstant(document.getElementById("b-end").value),
  };
  await createBooking(body);
});

async function createBooking(body) {
  try {
    const created = await api.post("/bookings", body);
    showNotice(notice, "Booking created for " + created.resourceName + " (" + created.status + ").", "success");
    mineOnly = true;
    await loadBookings();
  } catch (error) {
    showNotice(notice, fieldErrorText(error), "error");
    const alternatives = (error.data && error.data.alternatives) || [];
    conflictMount.replaceChildren();
    if (alternatives.length) {
      conflictMount.appendChild(el("strong", "Alternative slots for the same resource:"));
      for (const slot of alternatives) {
        const row = el("div", fmtDate(slot.startAt) + " → " + fmtDate(slot.endAt) + " — " + slot.reason, "notice");
        const book = el("button", "Book this slot", "secondary");
        book.type = "button";
        book.addEventListener("click", () => createBooking({
          resourceId: body.resourceId, purpose: body.purpose, startAt: slot.startAt, endAt: slot.endAt,
        }));
        row.appendChild(book);
        conflictMount.appendChild(row);
      }
    }
  }
}

document.getElementById("optimizer-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const body = {
    resourceType: document.getElementById("o-type").value,
    capacity: Number(document.getElementById("o-capacity").value || 1),
    equipment: document.getElementById("o-equipment").value.split(",").map((s) => s.trim()).filter(Boolean),
    building: document.getElementById("o-building").value.trim() || null,
    startAt: toInstant(document.getElementById("o-start").value),
    endAt: toInstant(document.getElementById("o-end").value),
  };
  try {
    const recommendations = await api.post("/analytics/optimization/recommendations", body);
    const mount = document.getElementById("recommendations");
    mount.replaceChildren();
    if (!recommendations.length) { mount.appendChild(el("p", "No resource matches those requirements.", "muted")); return; }
    for (const rec of recommendations) {
      const panel = el("div", null, "notice");
      panel.appendChild(el("strong", rec.resourceName + " · score " + rec.score + "/100"));
      panel.appendChild(el("div", rec.explanation, "small muted"));
      const book = el("button", "Book this recommendation");
      book.type = "button";
      book.addEventListener("click", () => createBooking({
        resourceId: rec.resourceId, purpose: "Booked from simulator recommendation",
        startAt: rec.startAt, endAt: rec.endAt,
      }));
      panel.appendChild(book);
      mount.appendChild(panel);
    }
  } catch (error) { showNotice(notice, fieldErrorText(error), "error"); }
});

document.getElementById("schedule-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const resourceId = document.getElementById("s-resource").value;
  const from = encodeURIComponent(toInstant(document.getElementById("s-from").value));
  const to = encodeURIComponent(toInstant(document.getElementById("s-to").value));
  try {
    const rows = await api.get("/resources/" + resourceId + "/schedule?from=" + from + "&to=" + to);
    const mount = document.getElementById("schedule");
    if (!rows.length) { mount.replaceChildren(el("p", "No bookings in that window.", "muted")); return; }
    const table = document.createElement("table");
    const head = document.createElement("tr");
    for (const text of ["Purpose", "When", "Status", "Owner"]) head.appendChild(el("th", text));
    table.appendChild(head);
    for (const booking of rows) {
      const tr = document.createElement("tr");
      tr.appendChild(el("td", booking.purpose));
      tr.appendChild(el("td", fmtDate(booking.startAt) + " → " + fmtDate(booking.endAt)));
      const s = document.createElement("td"); s.appendChild(badge(booking.status)); tr.appendChild(s);
      tr.appendChild(el("td", booking.ownerName));
      table.appendChild(tr);
    }
    mount.replaceChildren(table);
  } catch (error) { showNotice(notice, error.message, "error"); }
});

try { await fillResourceSelects(); } catch (error) { showNotice(notice, error.message, "error"); }
await loadBookings();
