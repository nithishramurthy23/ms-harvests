# Order emails

Customers get an email at each step of their order, so they always know where it is without having to log in.

## Which emails, and when

| Email | Sent when | What it says |
|---|---|---|
| **Order confirmed** | Cash on delivery: as soon as the order is placed. Online: once the payment has arrived, not before. | The items, the total, the delivery address, and either "keep ₹398 ready to pay in cash" or "we've received your payment" |
| **On its way** | An admin marks the order as shipped | That it has left the shop, plus a reminder of the cash to keep ready |
| **Delivered** | An admin marks the order as delivered | Thanks, and "reply to this email if anything isn't right" |
| **Cancelled** | The customer or an admin cancels, or an online order wasn't paid within 30 minutes | Why, and what happens to the money: nothing was charged, or a refund is on its way |
| **Refund sent** | Razorpay accepts the refund for a cancelled online order | The amount, and that banks usually show it in 5 to 7 working days |

There's no email when an order moves to "packing". It's a step for the shop, not news for the customer.

Every email has a **See your orders** button that opens the customer's order history. Set the shop's address with `FRONTEND_URL` in `.env` so that button points to the right place.

## What they look like

Each email is sent in two versions in one message: a simple HTML version with a small order table and a green button, and a plain-text version with the same words. Mail apps show whichever they can. Having both also makes spam filters happier.

The words are in one place, `notification/OrderEmailTemplates.java`. Both versions are built from the same list of sentences, so they always say the same thing. Customer names, product names and addresses are escaped before they go into the HTML, so a name like `<script>` shows up as text and can't run.

Amounts use Indian digit grouping: ₹1,25,000, not ₹125,000.

## Three promises

**1. No email for something that didn't happen.** An email is only sent after the change it describes has been saved to the database. If an order fails halfway (for example, the last kiwi was just bought by someone else), the database rolls back and no "confirmed" email goes out. In Spring this is a `@TransactionalEventListener`: the code that changes the order publishes an `OrderEmailEvent`, and the listener runs after the transaction commits.

**2. A slow mail server never slows down an order.** The email is handed to a background thread (`MailDispatcher`), which tries up to 3 times, waiting 2, then 4 seconds between tries. The customer's checkout doesn't wait for any of that.

**3. An email problem never breaks an order.** If building or queuing an email fails, the error is logged and the order carries on. The order matters more than the email.

## The same email twice?

The things that trigger emails already happen only once:

- An online payment is marked paid by a single conditional database update, even when the browser and Razorpay both report it (see [payments.md](payments.md#why-it-cant-go-wrong-twice)). So there's one "confirmed" email.
- An order can only move to shipped or delivered once, because the status rules don't allow going back.
- A cancelled order can't be cancelled again.

## What's not covered yet

- **If the server restarts** between saving the order and handing the email to the background thread, that email is lost. The fix is a transactional outbox: write the email into a table in the same transaction as the order change, and have a job send whatever is waiting. It's on the "what I'd do next" list in [design-decisions.md](design-decisions.md#what-id-do-next).
- **SMS or WhatsApp.** Another listener on the same `OrderEmailEvent` could send these without touching the order code.

## Trying it out

With `MAIL_USERNAME` and `MAIL_PASSWORD` set (see the [setup guide](setup.md)), place an order with your own email, then move it along in the dashboard at `/admin/orders`. Each step sends the next email.

## Where the code is

| File | What it does |
|---|---|
| `notification/OrderEmailType.java` | The five emails |
| `notification/OrderEmailEvent.java` | "Tell the customer about this order", published by the order code |
| `notification/OrderEmailListener.java` | Sends the email after the change is saved |
| `notification/OrderEmailTemplates.java` | The words, in text and HTML |
| `service/MailDispatcher.java` | Background sending with retries |
| `service/OrderService.java`, `service/OrderFulfilmentService.java`, `payment/PaymentService.java` | Where the events are published |
| `OrderEmailTemplatesTest`, `OrderEmailIntegrationTest` | Tests: the wording, which emails go out when, and none for failed changes |
