import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("../api/axiosInstance", () => ({ default: { post: vi.fn(), get: vi.fn() } }));

import api from "../api/axiosInstance";
import { newCheckoutKey, payForOrder } from "./payments";

const payment = { keyId: "rzp_test_key", gatewayOrderId: "order_RZP1", amountPaise: 29800, currency: "INR" };

// A stand-in for Razorpay's window: `behave` decides what the "customer" does.
function fakeRazorpay(behave) {
  window.Razorpay = vi.fn(function (options) {
    const handlers = {};
    this.on = (event, fn) => (handlers[event] = fn);
    this.open = () => behave(options, handlers);
  });
}

beforeEach(() => vi.clearAllMocks());
afterEach(() => delete window.Razorpay);

describe("payForOrder", () => {
  it("opens Razorpay for the order and lets the server check the payment", async () => {
    fakeRazorpay((options) =>
      options.handler({ razorpay_order_id: "order_RZP1", razorpay_payment_id: "pay_1", razorpay_signature: "sig" })
    );
    api.post.mockResolvedValue({ data: { paymentStatus: "PAID" } });

    await expect(payForOrder(41, payment, { name: "Asha", email: "asha@example.com" })).resolves.toBe("PAID");

    const options = window.Razorpay.mock.calls[0][0];
    expect(options).toMatchObject({ key: "rzp_test_key", order_id: "order_RZP1", amount: 29800 });
    expect(options.prefill.email).toBe("asha@example.com");
    expect(api.post).toHaveBeenCalledWith("/payments/verify", {
      orderId: 41,
      razorpayOrderId: "order_RZP1",
      razorpayPaymentId: "pay_1",
      razorpaySignature: "sig",
    });
  });

  it("reports when the customer closes the window", async () => {
    fakeRazorpay((options) => options.modal.ondismiss());
    await expect(payForOrder(41, payment)).rejects.toEqual({ dismissed: true });
    expect(api.post).not.toHaveBeenCalled();
  });

  it("passes on Razorpay's reason when a payment fails", async () => {
    fakeRazorpay((options, handlers) => handlers["payment.failed"]({ error: { description: "Card declined" } }));
    await expect(payForOrder(41, payment)).rejects.toThrow("Card declined");
  });
});

it("makes a different checkout key each time", () => {
  expect(newCheckoutKey()).not.toBe(newCheckoutKey());
});
