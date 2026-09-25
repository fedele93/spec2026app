// Orari dell'evento (blocco "schedule" di event-data.json), segnaposto nei testi e calendario.
// Stessa logica di app/schedule.py nel backend e di tools/gen-event-data.py per Android.
//   {partyTime}                        -> orario, oppure "da definire"
//   {partyTime|ora da definire}        -> orario, oppure il testo dopo la barra
//   {partyTime|Inizio ore $.|Da confermare.} -> con orario il 2° pezzo ($ = orario), senza il 3°
// In modalità server i testi arrivano già risolti da GET /api/event; qui servono per il
// fallback offline (shared/event-data.json). Risolvere due volte non cambia nulla.

export const SCHEDULE_KEYS = ["ceremonyDate", "ceremonyTime", "partyDate", "partyTime", "busDepartureTime", "busReturnTime"];
const PLACEHOLDER = /\{(\w+)(?:\|([^|}]*))?(?:\|([^}]*))?\}/g;

export function getSchedule(ev) {
  const raw = (ev && ev.schedule) || {};
  const out = {};
  for (const k of SCHEDULE_KEYS) out[k] = String(raw[k] ?? "").trim();
  return out;
}

export function resolveText(text, schedule) {
  return String(text).replace(PLACEHOLDER, (_m, key, a, b) => {
    const value = schedule[key] ?? "";
    if (b !== undefined) return value ? a.replaceAll("$", value) : b;
    if (a !== undefined) return value || a;
    return value || "da definire";
  });
}

export function resolvePlaceholders(obj, schedule) {
  if (typeof obj === "string") return resolveText(obj, schedule);
  if (Array.isArray(obj)) return obj.map((x) => resolvePlaceholders(x, schedule));
  if (obj && typeof obj === "object") {
    const out = {};
    for (const [k, v] of Object.entries(obj)) out[k] = resolvePlaceholders(v, schedule);
    return out;
  }
  return obj;
}

// ---- Calendario (.ics) generato nel browser: usato in modalità locale/demo. ------------
// In modalità server si preferisce GET /api/event/calendar.ics (stesso contenuto, ma con il
// Content-Type giusto, che su iPhone apre direttamente "Aggiungi al calendario").
const pad = (n) => String(n).padStart(2, "0");
const esc = (s) => String(s).replace(/\\/g, "\\\\").replace(/;/g, "\;").replace(/,/g, "\\,").replace(/\n/g, "\\n");
const parseTime = (s) => { const m = /^(\d{1,2})[:.](\d{2})$/.exec(s || ""); return m ? [Number(m[1]), Number(m[2])] : null; };

function vevent(uid, summary, location, description, dateIso, timeStr, hours, url) {
  const [y, mo, d] = dateIso.split("-").map(Number);
  const lines = ["BEGIN:VEVENT", `UID:${uid}`, "DTSTAMP:" + new Date().toISOString().replace(/[-:]/g, "").replace(/\.\d+Z$/, "Z")];
  const t = parseTime(timeStr);
  if (!t) {
    const next = new Date(Date.UTC(y, mo - 1, d + 1));
    lines.push(`DTSTART;VALUE=DATE:${y}${pad(mo)}${pad(d)}`);
    lines.push(`DTEND;VALUE=DATE:${next.getUTCFullYear()}${pad(next.getUTCMonth() + 1)}${pad(next.getUTCDate())}`);
  } else {
    const start = new Date(y, mo - 1, d, t[0], t[1]);
    const end = new Date(start.getTime() + hours * 3600000);
    const fmt = (x) => `${x.getFullYear()}${pad(x.getMonth() + 1)}${pad(x.getDate())}T${pad(x.getHours())}${pad(x.getMinutes())}00`;
    lines.push(`DTSTART;TZID=Europe/Rome:${fmt(start)}`, `DTEND;TZID=Europe/Rome:${fmt(end)}`);
  }
  lines.push("SUMMARY:" + esc(summary));
  if (location) lines.push("LOCATION:" + esc(location));
  if (description) lines.push("DESCRIPTION:" + esc(description));
  if (url) lines.push("URL:" + url);
  lines.push("END:VEVENT");
  return lines;
}

export function buildIcs(ev, url = "") {
  const sch = getSchedule(ev);
  const points = Object.fromEntries((ev.mapPoints || []).map((p) => [p.id, p]));
  const seduta = points.seduta || {}, festa = points.festa || {};
  let lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//NeuroParty//PWA//IT", "CALSCALE:GREGORIAN", "METHOD:PUBLISH",
    "X-WR-CALNAME:" + esc(ev.program?.title || "NeuroParty")];
  if (sch.ceremonyDate) lines = lines.concat(vevent("neuroparty-seduta@neurospec", "Seduta di Specializzazione in Neurologia",
    seduta.address, resolveText(seduta.description || "", sch), sch.ceremonyDate, sch.ceremonyTime, 3, url));
  if (sch.partyDate) lines = lines.concat(vevent("neuroparty-festa@neurospec", "Festa di Specializzazione in Neurologia",
    festa.address, resolveText(festa.description || "", sch), sch.partyDate, sch.partyTime, 5, url));
  lines.push("END:VCALENDAR");
  return lines.join("\r\n") + "\r\n";
}

export function downloadIcs(ev) {
  const blob = new Blob([buildIcs(ev, location.href)], { type: "text/calendar;charset=utf-8" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "neuroparty.ics";
  document.body.appendChild(a);
  a.click();
  setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1000);
}
