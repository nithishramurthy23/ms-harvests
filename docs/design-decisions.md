# Design decisions

This page explains the main engineering choices in Farm Fresh: what problem each one solves, what I picked, what else I considered, and what it costs. The setup guide is in [setup.md](setup.md).

## Architecture at a glance

```
Browser ──► nginx ──┬──► backend copy 1 ─┐
 (React)   (static  │                    ├──► MySQL
           + LB)    └──► backend copy 2 ─┘
                                 │
                                 └──► Gmail SMTP (background thread)
```

- One Spring Boot service (Java 17) with a stateless REST API and JWT auth.
- Two or more identical copies of it run behind nginx, which also serves the built React app.
- MySQL is the only shared state. The backend copies keep nothing in memory that another copy would need.

---

## 1. The server decides the price

**Problem.** The first version trusted the price the browser sent with each order item. Anyone could open dev tools and buy a ₹199 item for ₹1.

**Decision.** `OrderService.placeOrder` ignores the client's price. For every line it loads the product from the database and asks `ProductService.getCurrentPrice` for today's price: the original price minus any active discount rule for that category and weekday. The total is the sum of those server-side prices. The `price` field is still accepted in the request so older clients don't break, but it is never read.

**Tradeoff.** The website used to keep its own copy of products and discount rules, so the price shown could drift from the price charged. It now loads both from the API (`GET /api/products` returns today's price, computed by the same `getCurrentPrice`), so there's one source of truth. The cost is one extra request when the site opens.

**Tested by.** `OrderServiceTest.totalIsCalculatedFromServerPricesNotClientPrices` and the integration test, which sends a fake price of 1 and expects the real total.

---

## 2. Stock that can't oversell, even across several servers

**Problem.** "Read stock, check it, write stock minus quantity" is a race. Two requests can both read `stock = 1`, both pass the check, and both sell the last item. Running two backend copies makes this more likely, and a Java `synchronized` block wouldn't help because each copy has its own JVM.

**Decision.** Let the database do the check and the write in one statement:

```sql
UPDATE products SET stock_quantity = stock_quantity - :qty
WHERE id = :id AND stock_quantity >= :qty
```

(`ProductRepository.decrementStock`). MySQL locks the row for the duration of the update, so concurrent orders for the same product queue up. The method returns the number of rows changed. `1` means the stock was reserved, `0` means there wasn't enough. It runs inside the order's transaction, so if a later line in the same order fails, the whole thing rolls back and the earlier reservations are undone.

**Alternatives considered.**
| Option | Why not |
|---|---|
| `SELECT ... FOR UPDATE`, then update | Correct, but two round trips and a longer lock for the same result. |
| Optimistic locking (`@Version`) | Under contention, most requests fail and must retry. That's poor for a popular item. |
| Distributed lock (Redis) | Adds a new system and a new failure mode to protect one row the database can already protect. |

**Tradeoff.** Every order for the same product waits on the same row lock. At very high volume (flash sales), that row becomes a bottleneck. The usual next step is to split stock across several rows ("buckets"), or to reserve stock in Redis and settle it in the database asynchronously.

**Tested by.** `StockConcurrencyIntegrationTest`: 20 threads race for 5 items. Exactly 5 succeed and stock ends at 0.

---

## 3. Stateless auth with JWT

**Problem.** With several backend copies, a login session stored in one copy's memory would be unknown to the others.

**Decision.** After login (password or email OTP), the backend returns a JWT signed with HMAC-SHA512 (`JwtTokenProvider`). Every request carries it in `Authorization: Bearer ...`, and any copy can verify it with the shared secret. No session storage is needed. The app refuses to start signing with a secret shorter than 512 bits.

**Tradeoff.**
- A JWT can't be revoked before it expires. A stolen token works until expiry (24 hours by default, `JWT_EXPIRATION`). The fix would be short-lived access tokens plus a refresh token that is stored server-side and can be revoked.
- The frontend keeps the token in `localStorage`, which any injected script can read. An `HttpOnly` cookie would avoid that, but it needs CSRF protection in exchange.

**Tested by.** `JwtTokenProviderTest`: tampered, expired, and other-key tokens are all rejected.

---

## 4. Email OTP for sign-up, login and order confirmation

**Decision.** `OtpService` generates a 6-digit code with `SecureRandom`, stores it on the user row with a 5-minute expiry, and emails it. A code works once and is cleared on any wrong attempt. After 2 failures the user has to start over. Placing an order needs a fresh OTP too, so a stolen token alone can't place orders.

**Tradeoff.**
- The code is stored in plain text. If the database leaked, live codes would leak with it. Hashing it the way passwords are hashed fixes that.
- One OTP slot per user means that requesting a login code cancels a pending order code.
- There is no rate limit on *sending* codes, so someone could make the app spam a user's inbox. A per-email limit (in Redis, or a table with timestamps) is the fix.

---

## 5. Email is sent in the background, with retries

**Problem.** Gmail SMTP sometimes takes seconds to respond, or fails. When email was sent inside the request, a slow mail server made sign-up and checkout slow, and a failed email failed the order.

**Decision.** `MailDispatcher.send` is `@Async`. The request returns right away, and a small thread pool (2–8 threads, queue of 500) sends the email, retrying up to 3 times with exponential backoff (2 s, then 4 s). Order placement catches mail errors, so an order is never lost because of email.

**Tradeoff.** The queue lives in memory. If a backend copy crashes with emails still queued, those emails are lost. The standard fix is the *transactional outbox* pattern: write the email to an `outbox` table in the same transaction as the order, and have a worker send and mark it. That guarantees the email is sent at least once.

---

## 6. Scale the monolith, not microservices

**Problem.** Handle more traffic and survive one server going down.

**Decision.** Run several identical copies of the one backend (`docker compose`, `BACKEND_REPLICAS`, default 2) behind nginx:
- nginx uses `least_conn` to send each request to the copy with the fewest open requests.
- Each copy exposes `/actuator/health/readiness`. Docker restarts unhealthy copies, and nginx stops sending them traffic after a failure.
- `server.shutdown=graceful` lets a copy finish in-flight requests before it stops, so rolling restarts drop nothing.
- nginx retries a failed request on another copy, but **never retries a POST**, so a timeout can't place an order twice.
- Database and SMTP calls have 5-second timeouts, so a slow dependency fails fast instead of tying up every thread.

**Why not microservices.** The domain is small (users, products, orders), one person maintains it, and every request needs the same database. Splitting it would add network calls, distributed transactions (order + stock), and more to deploy, without solving a problem the app has. Replicating the monolith gives the availability and horizontal scaling benefits for a fraction of the complexity. If one part later needed to scale on its own (for example, email), it could be split out at that point.

**Tradeoff.** MySQL is now the single point of failure and the scaling limit. The next steps would be a managed database with automatic failover, then read replicas for the catalogue.

---

## 7. Cash on delivery only

**Decision.** The checkout used to collect raw card numbers. I removed that and the backend now rejects anything other than `cod`.

**Why.** Handling card data directly brings the app under PCI-DSS rules. The right way to take card or UPI payments is a gateway such as Razorpay or Stripe: the card details go straight to the gateway, and the backend only receives a payment ID and a signed webhook. That needs an idempotent "payment confirmed" handler, which is a good next project.

---

## 8. Configuration comes from the environment

All secrets (database password, JWT secret, Gmail app password) and per-environment settings (allowed CORS origins, database URL) come from environment variables or a git-ignored `.env` file. `.env.example` documents them all. The same build runs locally, in Docker, and in production with different settings, and no secret is ever committed.

---

## 9. Sign in with Google: verify the ID token, then issue our own JWT

**Problem.** Users wanted to log in with Google instead of making yet another password, but the backend runs as several stateless copies (decision 3), so anything that keeps login state in one copy's memory would break.

**Decision.** The frontend uses Google Identity Services: Google's popup returns a signed **ID token** to the browser, which posts it to `POST /api/auth/google`. `GoogleTokenVerifier` checks it with Spring Security's `NimbusJwtDecoder`: RS256 signature against Google's published public keys (fetched once and cached), issuer is Google, audience is *our* client ID, not expired, and `email_verified` is true. `AuthService.loginWithGoogle` then finds or creates the user by email and returns the same JWT as a password login. From that point on the rest of the app doesn't know or care how the user logged in.

**Alternatives considered.**
- *Spring's `oauth2Login()` (authorization-code flow with redirects).* It stores the OAuth `state` in an HTTP session between the redirect to Google and the callback. With two backend copies behind a load balancer, the callback can land on the copy that doesn't have that session, so I'd need sticky sessions or a shared session store. It also needs a client secret.
- *Trust the email the frontend sends.* Anyone could claim any email. The token's signature and audience check are what make it safe.

**Tradeoff.**
- Linking by email: a Google login with the same email as an existing password account logs into that account. That's safe only because we require Google's `email_verified` claim; Google has proven the person controls that inbox, which is the same thing our sign-up OTP proves.
- Google users get a random, unknown password hash, because the `password` column is `NOT NULL`. A cleaner schema would store the login provider and allow a null password.
- The backend needs to reach `googleapis.com` to fetch the signing keys (cached, refreshed when Google rotates them).

**Tested by.** `GoogleTokenVerifierTest` signs tokens with a test RSA key and checks forged, expired, wrong-audience, wrong-issuer and unverified-email tokens are rejected. `GoogleSignInIntegrationTest` covers first login creating an account, linking to an existing account, and the returned JWT working on `/api/auth/me`.

---

## 10. Every endpoint that takes an id checks who owns it

**Problem.** `OrderItemController` exposed `GET /api/order-items/{id}`, `GET /api/order-items/order/{orderId}` and `DELETE /api/order-items/{id}`. Security only required *a* valid JWT, and the service looked items up by id alone. So any logged-in customer could read, or delete, line items from someone else's order by counting up the ids (an IDOR, part of OWASP's "Broken Access Control"). Authentication said who the caller was. Nothing checked whether the item was theirs.

**Decision.** Remove the endpoints, along with `OrderItemService`. The frontend never called them: order lines are read through `GET /api/orders/{id}`, which already compares the order's owner with the user in the token. Order lines are part of an order and have no reason to be addressable on their own, and a deleted endpoint can't be attacked.

**Alternatives considered.**
- *Keep them and check ownership* (`item.getOrder().getUser().getId()` against the caller, returning 404 on a mismatch so the response doesn't reveal that the id exists). That's the right fix if an admin screen ever needs them, but then they belong under `/api/admin/**` with a role check.
- *Random UUIDs instead of sequential ids.* Makes ids harder to guess, but it only hides the hole and doesn't close it.

**Tradeoff.** None for customers today. An admin feature that edits order lines will need a new, role-protected endpoint.

**Tested by.** `OrderFlowIntegrationTest.otherCustomersCannotReadOrDeleteMyOrderItems`: user A places an order, user B gets 404 on all three item URLs and 403 on A's order, and A's item still exists afterwards. The test was written first and failed (200) against the old code.

---

## 11. Four roles, checked at the URL level

**Problem.** Running the shop meant writing SQL, because every account was `ROLE_USER` and nothing checked roles. The store needs people who can manage products, stock and orders without being able to do everything.

**Decision.** `Role` has four values, stored in the existing `users.role` column as Spring authorities, so old rows still read as `USER`:

| Role | Can do |
|---|---|
| USER, MEMBER | Shop, and cancel their own order while it's PENDING |
| ADMIN | `/api/admin/**`: products, stock, all orders, and moving customers between USER and MEMBER |
| OWNER | Everything an admin can, plus adding and removing admins and owners, and `/api/owner/**` (sales summary) |

The rules are two lines in `SecurityConfig` (`/api/admin/**` needs ADMIN or OWNER, `/api/owner/**` needs OWNER), so one file shows who can reach what. The JWT filter loads the user from the database on every request, which means a role change takes effect on the next request without logging out. The first owner comes from `OWNER_EMAIL`, so setting up a shop needs no SQL.

Who may change whose role is a second layer, because a URL rule can't express it. Admins and the owner both reach `PATCH /api/admin/users/{id}/role`, and `Role.assignableTo(current)` decides the rest: the owner can give any role to anyone, while an admin can only move someone who is currently USER or MEMBER to USER or MEMBER. `UserManagementService` also refuses changing your own role and demoting the last owner, and writes each change to `role_changes` (who, from, to, by whom, when). The people list sends `assignableRoles` for each person, so the dashboard only offers choices the server will accept. The rule lives in one place and the UI just reflects it.

Order statuses are a small state machine in `OrderStatus.canMoveTo`: PENDING → PROCESSING → SHIPPED → DELIVERED, cancellable until shipped. Cancelling returns each item's quantity with the same conditional `UPDATE` admins use, and logs it in `stock_movements`.

**Alternatives considered.**
- *`@PreAuthorize` on each method.* It works, but the rules end up spread across controllers. URL rules are easier to review while the API is small.
- *Roles inside the JWT.* That saves a database read, but a demoted admin would keep access until the token expired. The lookup already happens for every request.
- *A permissions table (role → permission).* More flexible, but four fixed roles don't need it yet.

**Tradeoff.** MEMBER has no perks yet; it's there so member pricing can be added without another migration. Admin errors go through a `@RestControllerAdvice` (404/400/409 with a `message`), while the older customer endpoints still catch exceptions themselves. Moving those over is a follow-up.

**Tested by.** `StoreAdminIntegrationTest` runs the whole cycle (add product → sell out → restock → PROCESSING → SHIPPED → DELIVERED) and checks that users and members get 403 on every admin endpoint, that stock can't go below zero, that hidden products can't be bought, that cancelling restocks, that admins can move customers between user and member but get 403 when granting admin or owner or touching other staff, that nobody changes their own role, that the last owner stays, and that role changes are logged. `RoleTest` covers `assignableTo` for every pair. `OrderStatusTest` covers every status transition.

---

## 12. Online payments: conditional updates instead of locks

**Problem.** Cash on delivery meant nobody could pay up front. Adding a gateway brings new ways to get money wrong: the browser and the webhook both report one payment, a payment arrives after the order expired, a refund is sent twice, or a double-click creates two orders.

**Decision.** Razorpay's standard checkout, behind a small `PaymentGateway` interface. Payment status is its own column (`PaymentStatus`), separate from `OrderStatus`, because "paid" and "shipped" change independently. Every payment change is a conditional `UPDATE ... WHERE payment_status = :expected` (`OrderRepository.movePayment`), so when two callers race, exactly one wins and the other sees 0 rows. Other choices:

- The Razorpay order is created *after* the order transaction commits, not inside it. Inside, the product rows locked by the stock `UPDATE` would stay locked for the length of an HTTP call to Razorpay, and every other checkout for those products would wait. If Razorpay fails, the order is cancelled and its stock released.
- The browser's "paid" message is trusted only after checking Razorpay's HMAC signature. The webhook is checked with a separate secret, and its amount must equal our total.
- Refunds run in an `@TransactionalEventListener` after the cancel commits, so a rolled-back cancel never refunds. The order is moved to REFUNDED *before* calling Razorpay and moved back if that fails, so a retry and the automatic refund can't both send money.
- Unpaid orders hold stock, so a scheduled job cancels them after 30 minutes. A payment that lands after that is refunded automatically.
- `Idempotency-Key` on `place-order`, checked before the OTP (the first request already used the code) and backed by a unique key on (user, key) for two requests arriving at the same time.
- `Order` uses `@DynamicUpdate`, so saving a status change writes only the status column and can't overwrite a payment status a webhook wrote a moment before.

**Alternatives considered.**
- *Pessimistic lock (`SELECT ... FOR UPDATE`) on the order.* Works, but every path has to remember to take the lock. The conditional update can't be forgotten, because it *is* the write.
- *Optimistic locking with `@Version`.* Also works, but the loser gets an exception to retry. Here the loser doesn't need to retry; it only needs to know what happened.
- *The Razorpay Java SDK.* We use three endpoints and two HMACs. Calling the REST API with `RestClient` keeps the dependency list short and the code easy to follow.
- *A transactional outbox for refunds.* More robust if the app crashes between commit and refund. For now a failed or missed refund stays visible as REFUND_PENDING with a retry button. The outbox is the next step (see decision 5).

**Tradeoff.** The expiry job runs on every backend copy. That's safe, because the conditional update lets only one copy cancel each order, but it does duplicate work. ShedLock would fix that if the job ever got expensive.

**Tested by.** `PaymentIntegrationTest` (12 tests, Razorpay mocked): pay then ship, a forged signature, browser plus webhook counted once, wrong webhook signature or amount, refund on cancel, a failed refund retried once, expiry releasing stock, a late payment refunded, Razorpay down at checkout, a retried checkout with the same key, cash on delivery paid on delivery, and customers unable to pay or verify someone else's order. `RazorpayGatewayTest` checks the HMACs against values computed separately in Python.

Plain-language version, with setup steps: [payments.md](payments.md).

## What I'd do next

Roughly in order of value:

1. **Database migrations with Flyway** instead of `spring.jpa.hibernate.ddl-auto=update`, so schema changes are versioned, reviewed, and repeatable.
2. **Move the customer endpoints onto the shared error handler** that the admin API uses.
3. **Transactional outbox for email** (see decision 5).
4. **Rate limit OTP sending and login attempts.**
5. **Refresh tokens** and shorter-lived access tokens (see decision 3).
6. **Testcontainers** to run integration tests against real MySQL instead of H2.
