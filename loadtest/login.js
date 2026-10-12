// Password logins under load. bcrypt is deliberately slow (about 100 ms of CPU
// per check), so this shows how many logins per second the backend can handle.
//
//   k6 run -e BASE_URL=http://localhost:8081 loadtest/login.js

import http from "k6/http";
import { check } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081";

export const options = {
  scenarios: {
    logins: {
      executor: "constant-vus",
      vus: 20,
      duration: "45s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
  },
};

export default function () {
  const n = ((__VU * 1000 + __ITER) % 500) + 1;
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: `loadtest${n}@example.com`, password: "LoadTest123!" }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(res, { "login 200": (r) => r.status === 200 });
}
