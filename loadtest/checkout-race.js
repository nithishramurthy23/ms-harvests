// Flash sale: 500 customers try to buy the same product at the same moment,
// but only 100 are in stock. Exactly 100 orders must succeed, and stock must
// end at 0, never below.
//
// Needs loadtest/seed.sql loaded first (every run uses up the stock, so re-seed).
//   k6 run -e BASE_URL=http://localhost:8081 loadtest/checkout-race.js

import http from "k6/http";
import { check } from "k6";
import { Counter } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081";
const CUSTOMERS = 500;
const PRODUCT_ID = 9001;

const ordersPlaced = new Counter("orders_placed");
const soldOut = new Counter("orders_rejected_sold_out");
const unexpected = new Counter("orders_unexpected_response");

export const options = {
  setupTimeout: "5m",
  scenarios: {
    rush: {
      executor: "per-vu-iterations",
      vus: CUSTOMERS,
      iterations: 1,
      maxDuration: "2m",
    },
  },
  thresholds: {
    orders_placed: ["count==100"],
    orders_unexpected_response: ["count==0"],
    "http_req_duration{name:place-order}": ["p(95)<10000"],
  },
};

// Logging in is not part of the race, so do it for everyone up front.
export function setup() {
  const requests = [];
  for (let i = 1; i <= CUSTOMERS; i++) {
    requests.push({
      method: "POST",
      url: `${BASE_URL}/api/auth/login`,
      body: JSON.stringify({ email: `loadtest${i}@example.com`, password: "LoadTest123!" }),
      params: { headers: { "Content-Type": "application/json" }, tags: { name: "login" } },
    });
  }
  const tokens = [];
  // batches of 20 so setup doesn't overload the server before the test starts
  for (let i = 0; i < requests.length; i += 20) {
    http.batch(requests.slice(i, i + 20)).forEach((res, j) => {
      if (res.status !== 200) {
        throw new Error(`setup: login for customer ${i + j + 1} failed with ${res.status} ${res.body}`);
      }
      tokens.push(res.json("token"));
    });
  }
  return { tokens };
}

export default function ({ tokens }) {
  const n = __VU;
  const order = {
    paymentMethod: "cod",
    orderConfirmationOtp: "111111",
    items: [{ productId: PRODUCT_ID, quantity: 1 }],
    shippingInfo: {
      name: `Load Test ${n}`,
      address: "1 Test Street",
      city: "Bengaluru",
      postalCode: "560001",
      phone: "+91 98765 43210",
      email: `loadtest${n}@example.com`,
      expectedDeliveryDate: "2030-01-01",
    },
  };

  const res = http.post(`${BASE_URL}/api/orders/place-order`, JSON.stringify(order), {
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${tokens[n - 1]}` },
    tags: { name: "place-order" },
  });

  if (res.status === 201) {
    ordersPlaced.add(1);
  } else if (res.status === 400 && res.body.includes("Not enough stock")) {
    soldOut.add(1);
  } else {
    unexpected.add(1);
    console.warn(`customer ${n}: ${res.status} ${res.body}`);
  }
  check(res, { "placed or sold out": (r) => r.status === 201 || r.status === 400 });
}
