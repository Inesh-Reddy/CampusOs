import { api, fetchMe } from "../api/client.js";
import { el, showNotice } from "../utils/format.js";

const notice = document.getElementById("notice");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");

const DEMO = [
  ["admin@campus.edu", "Admin@123", "Campus Admin"],
  ["dept@campus.edu", "Dept@123", "Dept Admin"],
  ["tech1@campus.edu", "Tech@123", "Technician"],
  ["faculty@campus.edu", "Faculty@123", "Faculty"],
  ["student@campus.edu", "Student@123", "Student"],
];

const demoMount = document.getElementById("demo-accounts");
for (const [email, password, label] of DEMO) {
  const button = el("button", label + " · " + email, "secondary");
  button.type = "button";
  button.addEventListener("click", () => {
    emailInput.value = email;
    passwordInput.value = password;
    showNotice(notice, "Demo credentials filled. They only work when the backend runs with the local profile.", "");
  });
  demoMount.appendChild(button);
}

fetchMe().then(() => { location.href = "dashboard.html"; }).catch(() => {});

document.getElementById("login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await api.post("/auth/login", { email: emailInput.value.trim(), password: passwordInput.value });
    location.href = "dashboard.html";
  } catch (error) {
    showNotice(notice, error.message, "error");
  }
});
