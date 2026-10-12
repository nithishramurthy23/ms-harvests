// Steady traffic of logged-in customers: log in, then keep checking order history.
// Measures throughput and latency of the authenticated read path (JWT check + DB query).
//
//   k6 run -e BASE_URL=http://localhost:8081 loadtest/browse.js

import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081";
const USERS = 100;

export const options = {
  scenarios: {
    browse: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: USERS },
        { duration: "60s", target: USERS },
        { duration: "10s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{name:history}": ["p(95)<500"],
  },
};

// Log every test user in once, before the load starts.
export function setup() {
  const tokens = [];
  for (let i = 1; i <= USERS; i++) {
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email: `loadtest${i}@example.com`, password: "LoadTest123!" }),
      { headers: { "Content-Type": "application/json" }, tags: { name: "login" } }
    );
    check(res, { "login ok": (r) => r.status === 200 });
    tokens.push(res.json("token"));
  }
  return { tokens };
}

export default function ({ tokens }) {
  const token = tokens[(__VU - 1) % tokens.length];
  const res = http.get(`${BASE_URL}/api/orders/history`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "history" },
  });
  check(res, { "history 200": (r) => r.status === 200 });
  sleep(0.1);
}
