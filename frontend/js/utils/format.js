export function el(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = String(text);
  if (className) node.className = className;
  return node;
}

export function fmtDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata", day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  }).format(date);
}

export function badge(text) {
  const span = el("span", String(text || "—").replaceAll("_", " "), "badge " + String(text || "").toLowerCase());
  return span;
}

export function showNotice(container, message, kind) {
  container.replaceChildren(el("div", message, "notice " + (kind || "")));
}

export function fieldErrorText(apiError) {
  if (!apiError || !apiError.fieldErrors) return apiError ? apiError.message : "Request failed.";
  const parts = Object.entries(apiError.fieldErrors).map(([field, msg]) => field + ": " + msg);
  return apiError.message + " " + parts.join("; ");
}

export function toInstant(localValue) {
  // datetime-local has no zone; interpret as campus-local input and send ISO instant.
  return new Date(localValue).toISOString();
}
