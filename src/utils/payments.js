import api from "../api/axiosInstance";

// Online payment with Razorpay's own payment window (checkout.js). Card and UPI
// details are typed into Razorpay's window, never into our page, so they never
// reach our server.

const CHECKOUT_SCRIPT = "https://checkout.razorpay.com/v1/checkout.js";

export const PAYMENT_LABEL = {
  PAY_ON_DELIVERY: "Pay on delivery",
  AWAITING_PAYMENT: "Waiting for payment",
  PAID: "Paid",
  UNPAID: "Not paid",
  REFUND_PENDING: "Refund on its way",
  REFUNDED: "Refunded",
};

let scriptPromise = null;

export function loadRazorpay() {
  if (window.Razorpay) return Promise.resolve(window.Razorpay);
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = CHECKOUT_SCRIPT;
      script.onload = () => resolve(window.Razorpay);
      script.onerror = () => {
        scriptPromise = null;
        reject(new Error("Couldn't load the payment window. Check your connection and try again."));
      };
      document.body.appendChild(script);
    });
  }
  return scriptPromise;
}

/**
 * Opens the payment window for one order and, once the customer pays, asks our
 * server to check the payment. Resolves with the payment status ("PAID").
 * Rejects with { dismissed: true } if the customer closes the window.
 */
export async function payForOrder(orderId, payment, customer = {}) {
  const Razorpay = await loadRazorpay();

  const result = await new Promise((resolve, reject) => {
    const checkout = new Razorpay({
      key: payment.keyId,
      order_id: payment.gatewayOrderId,
      amount: payment.amountPaise,
      currency: payment.currency,
      name: "Farm Fresh",
      description: `Order #${orderId}`,
      prefill: { name: customer.name, email: customer.email, contact: customer.phone },
      theme: { color: "#15803d" },
      handler: resolve,
      modal: { ondismiss: () => reject({ dismissed: true }) },
    });
    checkout.on("payment.failed", (response) =>
      reject(new Error(response.error?.description || "The payment didn't go through."))
    );
    checkout.open();
  });

  const { data } = await api.post("/payments/verify", {
    orderId,
    razorpayOrderId: result.razorpay_order_id,
    razorpayPaymentId: result.razorpay_payment_id,
    razorpaySignature: result.razorpay_signature,
  });
  return data.paymentStatus;
}

export const paymentConfig = () => api.get("/payments/config").then((r) => r.data);

export const retryPayment = (orderId) => api.post(`/payments/orders/${orderId}/retry`).then((r) => r.data);

// One id per checkout. Sent with every try, so a retried request can't create a second order.
export const newCheckoutKey = () =>
  window.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
