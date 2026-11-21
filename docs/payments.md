# Online payments

Customers can pay at checkout with UPI, a card or net banking, through [Razorpay](https://razorpay.com). Cash on delivery still works as before. If you don't add Razorpay keys, the shop just shows cash on delivery and nothing else changes.

This page covers how a payment works, how to turn it on for testing, and what happens when something goes wrong.

## How a payment works

1. The customer fills in their address, gets the email code and picks **Pay online**.
2. Our server saves the order and holds the stock for it, then asks Razorpay to expect a payment of exactly that amount. The price always comes from our database, never from the browser.
3. Razorpay's payment window opens. The customer types their card or UPI details there, not on our page, so those details never reach our server.
4. When the payment succeeds, the browser sends us Razorpay's reply. We check its signature with our secret key, so nobody can fake a "paid" message. Then the order is marked **Paid** and the confirmation email goes out.
5. Razorpay also tells our server directly, through a **webhook**. That covers the case where the customer pays and closes the tab before the browser could tell us.

The shop can't pack an online order until it's paid. In the dashboard it shows "Can be packed once the customer pays".

## Payment statuses

| Status | Meaning |
|---|---|
| Pay on delivery | Cash on delivery. Becomes **Paid** when the order is delivered. |
| Waiting for payment | Online order saved, stock held, money not received yet. |
| Paid | Money received. |
| Not paid | The customer never paid. The order was cancelled and the stock released. |
| Refund on its way | A paid order was cancelled and the refund hasn't gone through yet. |
| Refunded | The money has been sent back. |

## Turning it on (test mode)

Test mode is free and no real money moves.

1. Create a free account at [dashboard.razorpay.com](https://dashboard.razorpay.com) and make sure the **Test mode** switch at the top is on.
2. Go to **Account & Settings → API Keys** and generate a key. You get a **Key ID** (starts with `rzp_test_`) and a **Key Secret**.
3. Put them in your `.env` file:
   ```
   RAZORPAY_KEY_ID=rzp_test_xxxxxxxx
   RAZORPAY_KEY_SECRET=xxxxxxxx
   ```
4. Restart the backend (`docker compose up -d` or restart `mvn spring-boot:run`). **Pay online** now shows at checkout.

To try a payment, use Razorpay's test details from [their test card page](https://razorpay.com/docs/payments/payments/test-card-details/). For UPI, `success@razorpay` always succeeds and `failure@razorpay` always fails.

### The webhook (recommended)

Without the webhook, payments still work, but if a customer closes the tab right after paying, the order stays "Waiting for payment" until it expires and the money is refunded. The webhook prevents that.

1. In the Razorpay dashboard go to **Account & Settings → Webhooks → Add new webhook**.
2. URL: `https://your-domain/api/payments/webhook`. Razorpay can't reach `localhost`; for local testing use a tunnel such as `ngrok http 8080`.
3. Make up a long random secret and enter it there, for example from `openssl rand -hex 32`.
4. Tick the events **payment.captured** and **order.paid**.
5. Put the same secret in `.env` as `RAZORPAY_WEBHOOK_SECRET` and restart the backend.

Webhooks whose signature doesn't match the secret are rejected.

### Going live

Switch the dashboard to **Live mode**, complete Razorpay's account checks, generate live keys and a live webhook, and replace the three values in `.env`. Nothing in the code changes.

## When things go wrong

**The customer closes the payment window.** The order is kept and its stock stays held. They can pay from **My orders → Pay now**, or cancel it there.

**Nobody pays within 30 minutes.** A background job runs every minute and cancels unpaid online orders older than 30 minutes, so their stock goes back on sale. Change the time with `PAY_WITHIN_MINUTES`.

**The money arrives after the order was cancelled.** This can happen if someone pays at minute 31. The server notices that the order is already cancelled and refunds the payment by itself.

**A paid order is cancelled** (by the customer before packing, or by an admin). The stock goes back and the refund is sent to Razorpay right after the cancellation is saved. Razorpay usually returns the money to the customer in 5 to 7 working days.

**The refund fails** (for example, Razorpay is down). The order shows **Refund on its way** and a **Retry refund** button in the dashboard's Orders tab. Pressing it twice can't refund twice.

**Razorpay can't be reached when the customer checks out.** The order is cancelled straight away, the stock is released, and the customer is told to try again or use cash on delivery.

**The customer double-clicks, or their connection drops and the browser retries.** Every checkout sends a random `Idempotency-Key`. If an order with that key already exists, the server returns it instead of creating a second one. A database unique key on (customer, key) backs this up if two requests arrive at the same moment.

**The browser and the webhook both report the same payment.** It's only counted once. See "Why it can't go wrong twice" below.

## Why it can't go wrong twice

Several things can touch one order at once: the customer's browser, Razorpay's webhook, the expiry job, an admin. If two of them read the order, both think "not paid yet" and both act, you get a double confirmation email or a refund for an order that was really paid.

So every change to the payment status is one database statement that only works if the status is still what we expect:

```sql
UPDATE orders SET payment_status = 'PAID', gateway_payment_id = ?
WHERE id = ? AND payment_status = 'AWAITING_PAYMENT'
```

If it updates 1 row, this caller won. If it updates 0 rows, someone else already changed it, and we look at the new status and act on that. Checkout already uses the same idea for stock (`... WHERE stock >= qty`), so it's the same trick in two places.

Refunds work the same way: the order is moved to **Refunded** first, and only the caller that made that change sends the refund. If Razorpay then fails, it goes back to **Refund on its way**.

## API

| Method | Path | Who | What it does |
|---|---|---|---|
| GET | `/api/payments/config` | anyone | Whether online payment is on, and the public key |
| POST | `/api/orders/place-order` | customer | With `"paymentMethod": "online"`, saves the order and returns `payment` (Razorpay order id, amount) |
| POST | `/api/payments/verify` | customer | Checks the reply from Razorpay's window and marks the order paid |
| POST | `/api/payments/orders/{id}/retry` | customer | Payment details again, to pay an order that isn't paid yet |
| POST | `/api/payments/webhook` | Razorpay | Payment events, checked by signature |
| POST | `/api/admin/orders/{id}/refund` | admin, owner | Tries a failed refund again |

Send an `Idempotency-Key` header (any random string up to 64 characters) with `place-order`, and the same one on every retry of that checkout.

## Where the code is

| File | What it does |
|---|---|
| `payment/PaymentGateway.java` | The few things we need from a payment provider. Swapping in Stripe would mean one new class. |
| `payment/RazorpayGateway.java` | Razorpay's REST API and the two signature checks (HMAC-SHA256) |
| `payment/PaymentService.java` | Starting, confirming, refunding and expiring payments |
| `service/OrderService.java` | Checkout, including the idempotency key |
| `service/OrderFulfilmentService.java` | Cancelling releases stock and queues the refund |
| `src/utils/payments.js` | Opens Razorpay's window in the browser |
| `PaymentIntegrationTest`, `RazorpayGatewayTest` | Tests for everything above, with Razorpay mocked |
