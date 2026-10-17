import test from "node:test";
import assert from "node:assert/strict";
import { createHmac, webcrypto } from "node:crypto";
import worker, { verifySignature, classifyWebhook } from "../src/index.js";

if (!globalThis.crypto) globalThis.crypto = webcrypto;
const env = { LEMON_WEBHOOK_SECRET: "signing-key", DEVICE_PAIRING_SECRET: "minimum-20-characters-private",
  FIREBASE_PROJECT_ID: "example-project", FIREBASE_SERVICE_ACCOUNT_JSON: "not-used",
  ALLOWED_STORE_IDS: "12", ALLOW_TEST_MODE: "false" };
const data = (event, extra = {}) => ({
  meta: { event_name: event },
  data: { id: "42", type: "orders", attributes: {
    store_id: 12, status: "paid", currency: "USD", total: 2999,
    refunded_amount: 0, updated_at: "2026-10-17T21:00:00Z", test_mode: false,
    first_order_item: { product_name: "Abonnement Premium" }, ...extra
  } }
});

test("HMAC signature checks exactly the unmodified request bytes", async () => {
  const body = '{"a":1}';
  const signature = createHmac("sha256", env.LEMON_WEBHOOK_SECRET).update(body).digest("hex");
  assert.equal(await verifySignature(new TextEncoder().encode(body), signature, env.LEMON_WEBHOOK_SECRET), true);
  assert.equal(await verifySignature(new TextEncoder().encode(body + " "), signature, env.LEMON_WEBHOOK_SECRET), false);
  assert.equal(await verifySignature(new TextEncoder().encode(body), "invalid", env.LEMON_WEBHOOK_SECRET), false);
});

test("a single order yields a sale notification with no customer details", () => {
  const a = data("order_created", { user_email: "private@example.com", user_name: "PRIVATE" });
  const event = classifyWebhook("order_created", a, env);
  assert.match(event.title, /vente/i);
  assert.match(event.body, /29,99/);
  assert.doesNotMatch(JSON.stringify(event), /private@|PRIVATE/);
});

test("subscription initial invoice is ignored, only renewals notify", () => {
  const e = data("subscription_payment_success", { billing_reason: "initial" });
  assert.equal(classifyWebhook("subscription_payment_success", e, env), null);
  e.data.attributes.billing_reason = "renewal";
  assert.match(classifyWebhook("subscription_payment_success", e, env).title, /Renouvellement/);
});

test("refunds have distinct idempotency fingerprints as amounts change", () => {
  const e = data("order_refunded", { status: "partial_refund", refunded_amount: 1000 });
  const first = classifyWebhook("order_refunded", e, env);
  e.data.attributes.refunded_amount = 2000;
  const next = classifyWebhook("order_refunded", e, env);
  assert.notEqual(first.key, next.key);
  assert.match(next.body, /20,00/);
  assert.deepEqual(classifyWebhook("order_refunded", e, env), next);
});

test("unrelated store, test purchases, and spoofed event names are ignored", () => {
  const e = data("order_created", { store_id: 99 });
  assert.equal(classifyWebhook("order_created", e, env), null);
  e.data.attributes.store_id = 12;
  e.data.attributes.test_mode = true;
  assert.equal(classifyWebhook("order_created", e, env), null);
  e.data.attributes.test_mode = false;
  assert.equal(classifyWebhook("order_refunded", e, env), null);
});

test("unsigned webhook is refused before accessing the database", async () => {
  const request = new Request("https://example.workers.dev/webhook", {
    method: "POST", headers: { "x-event-name": "order_created" },
    body: JSON.stringify(data("order_created"))
  });
  const response = await worker.fetch(request, env);
  assert.equal(response.status, 401);
});

test("signed valid event without registered device is safely acknowledged", async () => {
  const body = JSON.stringify(data("order_created"));
  const signature = createHmac("sha256", env.LEMON_WEBHOOK_SECRET).update(body).digest("hex");
  const request = new Request("https://example.workers.dev/webhook", {
    method: "POST",
    headers: { "x-event-name": "order_created", "x-signature": signature }, body
  });
  const response = await worker.fetch(request, {
    ...env, DB: { prepare: () => ({ first: async () => null }) }
  });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { ignored: true, reason: "no device" });
});

test("pairing endpoints reject missing authorization", async () => {
  const response = await worker.fetch(new Request("https://example.workers.dev/device/register", {
    method: "POST", body: JSON.stringify({ token: "abcdefghijklmnopqrstuvwxyzABCDE" })
  }), { ...env, DB: {} });
  assert.equal(response.status, 401);
});
