// Cloudflare Worker: signed Lemon Squeezy events -> Firebase Cloud Messaging (FCM).
// Secrets (never source-controlled): LEMON_WEBHOOK_SECRET, DEVICE_PAIRING_SECRET,
// FIREBASE_SERVICE_ACCOUNT_JSON. D1 contains only event fingerprints + one FCM token.
const EVENTS = new Set([
  "order_created", "order_refunded", "subscription_payment_success",
  "subscription_payment_refunded"
]);
const encoder = new TextEncoder();
const json = (obj, status = 200) => new Response(JSON.stringify(obj), {
  status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
});

function equalBytes(a, b) {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
  return diff === 0;
}
function hexBytes(value) {
  if (!/^[\da-f]{64}$/i.test(value || "")) return null;
  return Uint8Array.from(value.match(/.{2}/g), x => parseInt(x, 16));
}
export async function verifySignature(raw, signature, secret) {
  const supplied = hexBytes(signature);
  if (!supplied || !secret) return false;
  const key = await crypto.subtle.importKey("raw", encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const calculated = new Uint8Array(await crypto.subtle.sign("HMAC", key, raw));
  return equalBytes(calculated, supplied);
}
function isAuthorized(request, secret) {
  const received = request.headers.get("authorization") || "";
  const expected = "Bearer " + secret;
  return Boolean(secret) && equalBytes(encoder.encode(received), encoder.encode(expected));
}
function allowedStore(attributes, env) {
  const ids = String(env.ALLOWED_STORE_IDS || "").split(",").map(s => s.trim()).filter(Boolean);
  return ids.length > 0 && ids.includes(String(attributes.store_id || ""));
}
function base64url(data) {
  const bytes = data instanceof Uint8Array ? data : encoder.encode(data);
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}
function money(cents, currency) {
  const value = Number(cents);
  const code = /^[A-Z]{3}$/.test(String(currency || "").toUpperCase())
    ? String(currency).toUpperCase() : "USD";
  if (!Number.isSafeInteger(value)) return "";
  try {
    return new Intl.NumberFormat("fr-FR", { style: "currency", currency: code }).format(value / 100);
  } catch {
    return String((value / 100).toFixed(2)) + " " + code;
  }
}
function description(event, a) {
  // No customer name, email, phone number, or payment information leaves this service.
  const product = String(a.first_order_item?.product_name || a.product_name || a.variant_name || "Produit")
    .slice(0, 72);
  if (event === "order_created") {
    if (String(a.status || "").toLowerCase() !== "paid") return null;
    return { title: "Nouvelle vente 🍋", body: product + " • " + money(a.total, a.currency) };
  }
  if (event === "subscription_payment_success") {
    // The initial invoice accompanies order_created: don't notify it twice.
    if (a.billing_reason === "initial" || !["renewal", "updated"].includes(a.billing_reason)) return null;
    if (a.status !== "paid") return null;
    return { title: "Renouvellement reçu 🔄", body: product + " • " + money(a.total, a.currency) };
  }
  if (event === "order_refunded" || event === "subscription_payment_refunded") {
    const refunded = Number(a.refunded_amount);
    if (!Number.isSafeInteger(refunded) || refunded <= 0) return null;
    return {
      title: "Remboursement enregistré",
      body: product + " • total remboursé : " + money(refunded, a.currency)
    };
  }
  return null;
}
export function classifyWebhook(event, payload, env) {
  if (!EVENTS.has(event) || payload?.meta?.event_name !== event) return null;
  const data = payload?.data;
  const a = data?.attributes;
  if (!data?.id || !a || !allowedStore(a, env)) return null;
  if (a.test_mode === true && env.ALLOW_TEST_MODE !== "true") return null;
  const display = description(event, a);
  if (!display) return null;
  // Replayed identical payloads get one notification. Later partial refunds can
  // change refunded_amount and updated_at and are therefore distinct.
  const key = [event, String(data.id), String(a.updated_at || a.created_at || ""),
    String(a.refunded_amount ?? "")].join(":");
  return { key, event, ...display };
}
async function readBody(request, maxBytes) {
  const declared = Number(request.headers.get("content-length") || 0);
  if (declared > maxBytes) throw new Error("Payload too large");
  const raw = new Uint8Array(await request.arrayBuffer());
  if (raw.length > maxBytes) throw new Error("Payload too large");
  return raw;
}
async function accessToken(env) {
  const credentials = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON);
  if (!credentials.private_key || credentials.project_id !== env.FIREBASE_PROJECT_ID) {
    throw new Error("Firebase service account/project mismatch");
  }
  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const now = Math.floor(Date.now() / 1000);
  const claims = base64url(JSON.stringify({
    iss: credentials.client_email, sub: credentials.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3300
  }));
  const text = header + "." + claims;
  const pem = credentials.private_key.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const raw = Uint8Array.from(atob(pem), c => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", raw, {
    name: "RSASSA-PKCS1-v1_5", hash: "SHA-256"
  }, false, ["sign"]);
  const signature = base64url(new Uint8Array(await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5", key, encoder.encode(text))));
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: text + "." + signature
    })
  });
  if (!response.ok) throw new Error("Firebase OAuth failed: " + response.status);
  const result = await response.json();
  if (!result.access_token) throw new Error("Missing Firebase access token");
  return result.access_token;
}
async function sendPush(env, token, event) {
  const access = await accessToken(env);
  const response = await fetch("https://fcm.googleapis.com/v1/projects/" +
    encodeURIComponent(env.FIREBASE_PROJECT_ID) + "/messages:send", {
    method: "POST",
    headers: { authorization: "Bearer " + access, "content-type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        notification: { title: event.title, body: event.body },
        data: { event: event.event, identifier: event.key },
        android: { priority: "high", notification: {
          channel_id: "lemon_sales", sound: "default"
        } }
      }
    })
  });
  if (!response.ok) {
    const error = await response.text();
    if (error.includes("UNREGISTERED")) return "unregistered";
    throw new Error("FCM rejected notification (" + response.status + ")");
  }
  return "sent";
}
async function handleDevice(request, env) {
  if (!isAuthorized(request, env.DEVICE_PAIRING_SECRET)) return json({ error: "unauthorized" }, 401);
  if (request.method === "DELETE") {
    await env.DB.prepare("DELETE FROM devices WHERE id = 1").run();
    return json({ registered: false });
  }
  if (request.method === "GET") {
    const row = await env.DB.prepare("SELECT token FROM devices WHERE id = 1").first();
    return json({ registered: Boolean(row?.token) });
  }
  if (request.method !== "POST") return json({ error: "method" }, 405);
  let body;
  try { body = JSON.parse(new TextDecoder().decode(await readBody(request, 4096))); }
  catch { return json({ error: "bad request" }, 400); }
  const token = body?.token;
  if (typeof token !== "string" || token.length < 20 || token.length > 4096 ||
      !/^[\w.:\-]+$/.test(token)) return json({ error: "invalid token" }, 400);
  await env.DB.prepare("INSERT INTO devices(id, token, updated_at) VALUES(1, ?, ?) " +
    "ON CONFLICT(id) DO UPDATE SET token = excluded.token, updated_at = excluded.updated_at")
    .bind(token, Math.floor(Date.now() / 1000)).run();
  return json({ registered: true });
}
export async function handleWebhook(request, env) {
  let raw;
  try { raw = await readBody(request, 256 * 1024); }
  catch { return json({ error: "payload too large" }, 413); }
  if (!await verifySignature(raw, request.headers.get("x-signature"), env.LEMON_WEBHOOK_SECRET))
    return json({ error: "invalid signature" }, 401);
  let payload;
  try { payload = JSON.parse(new TextDecoder().decode(raw)); }
  catch { return json({ error: "invalid JSON" }, 400); }
  const eventName = request.headers.get("x-event-name");
  if (payload?.meta?.event_name !== eventName) return json({ error: "event mismatch" }, 400);
  const event = classifyWebhook(eventName, payload, env);
  if (!event) return json({ ignored: true });
  const device = await env.DB.prepare("SELECT token FROM devices WHERE id = 1").first();
  if (!device?.token) return json({ ignored: true, reason: "no device" });
  // D1 unique key enforces idempotency even under concurrent webhook deliveries.
  await env.DB.prepare("INSERT OR IGNORE INTO events(event_key, sent, created_at) VALUES (?, 0, ?)")
    .bind(event.key, Math.floor(Date.now() / 1000)).run();
  const row = await env.DB.prepare("SELECT sent FROM events WHERE event_key = ?")
    .bind(event.key).first();
  if (row?.sent) return json({ duplicate: true });
  try {
    const result = await sendPush(env, device.token, event);
    if (result === "unregistered") {
      await env.DB.prepare("DELETE FROM devices WHERE id = 1 AND token = ?").bind(device.token).run();
    }
    await env.DB.prepare("UPDATE events SET sent = 1 WHERE event_key = ?").bind(event.key).run();
    return json({ delivered: result === "sent" });
  } catch {
    // Do not lose transient failures: Lemon Squeezy will retry non-2xx responses.
    return json({ error: "temporary delivery failure" }, 503);
  }
}
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/health" && request.method === "GET") return json({ ok: true });
    if (!env.DB || !env.LEMON_WEBHOOK_SECRET || !env.DEVICE_PAIRING_SECRET ||
        !env.FIREBASE_PROJECT_ID || !env.FIREBASE_SERVICE_ACCOUNT_JSON ||
        !env.ALLOWED_STORE_IDS) return json({ error: "server not configured" }, 503);
    if (url.pathname === "/device/register" &&
        ["GET", "POST", "DELETE"].includes(request.method)) return handleDevice(request, env);
    if (url.pathname === "/webhook" && request.method === "POST") return handleWebhook(request, env);
    return json({ error: "not found" }, 404);
  }
};
