import { api } from "../api/client.js";
import { el } from "../utils/format.js";

const LINKS = [
  ["dashboard.html", "Dashboard"],
  ["issues.html", "Issues"],
  ["resources.html", "Resources"],
  ["bookings.html", "Bookings"],
  ["analytics.html", "Analytics"],
  ["admin.html", "Admin"],
];

export function renderNav(me) {
  const mount = document.getElementById("topbar");
  if (!mount) return;
  const header = el("header", null, "topbar");
  header.appendChild(el("span", "CampusOS", "brand"));
  const nav = document.createElement("nav");
  const here = location.pathname.split("/").pop() || "dashboard.html";
  for (const [href, label] of LINKS) {
    if (href === "admin.html" && !(me && (me.role === "CAMPUS_ADMIN" || me.role === "DEPT_ADMIN"))) continue;
    const link = el("a", label, href === here ? "active" : "");
    link.href = href;
    nav.appendChild(link);
  }
  header.appendChild(nav);
  header.appendChild(el("span", null, "spacer"));
  if (me) {
    header.appendChild(el("span", me.displayName + " · " + me.role.replaceAll("_", " "), "who"));
    const logout = el("button", "Log out", "secondary");
    logout.type = "button";
    logout.addEventListener("click", async () => {
      try { await api.post("/auth/logout"); } catch { /* session already gone */ }
      location.href = "login.html";
    });
    header.appendChild(logout);
  }
  mount.replaceChildren(header);
}
