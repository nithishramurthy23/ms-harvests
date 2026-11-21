# Rate limits on codes and logins

Farm Fresh emails 6-digit codes for sign-up, login, password reset and checkout. Without limits, anyone could make the shop email a stranger a hundred codes, or keep guessing a code until they got lucky. These limits stop both, and a normal customer never notices them.

## The two limits

**1. Per email address: one code a minute, five an hour.**

If a code was sent to an address less than a minute ago, asking again doesn't send another one. After five codes in an hour, no more go out until the hour is up. Typing a code correctly resets both counts, so someone who just logged in can ask for a checkout code straight away.

These counts are kept on the user's row in the database (`otp_last_sent_at`, `otp_window_started_at`, `otp_sends_in_window`), so they're exact even with several backend copies running.

**2. Per network address: a cap on how often anyone can knock.**

Every request to the login endpoints is counted by the address it came from, in two groups:

| Group | Endpoints | Default limit |
|---|---|---|
| Asking for a code | `register`, `send-otp`, `resend-otp`, `forgot-password` | 10 per 10 minutes |
| Checking a password or code | `login`, `verify-otp`, `reset-password`, `google` | 30 per 10 minutes |

Over the limit, the server answers **429 Too Many Requests** with a `Retry-After` header and a message like "Too many attempts from your network. Please try again in 6 minutes." This limit covers addresses that don't belong to any account, which the first limit can't.

## Why guessing a code doesn't work

A code is cleared after 2 wrong tries, and at most 5 codes go out an hour, so that's at most 10 guesses an hour. Against a million possible codes, that's about a 1 in 90,000 chance per hour. The per-address limit cuts it further for anyone guessing from one machine.

## What the customer sees

When someone asks for a login code or a new sign-up code, the reply is always the same:

> If an account exists for this email, we've sent it a code. It's valid for 5 minutes.

Forgot password works the same way, with its own wording. The reply is the same whether a code was sent, the email isn't registered, or the per-email limit was hit. If the reply were different, someone could type in emails one by one and find out who shops here.

At checkout the customer is already logged in, so there's nothing to hide. If they ask for a new order code too soon, they're told how long to wait: "Please wait 42 seconds before asking for another code."

## Changing the limits

Set these in `.env`:

| Setting | What it does | Default |
|---|---|---|
| `RATE_LIMIT_ENABLED` | Turn the per-address limit on or off | `true` |
| `RATE_LIMIT_SEND_PER_10_MIN` | Code requests allowed from one address in 10 minutes | `10` |
| `RATE_LIMIT_CHECK_PER_10_MIN` | Password and code checks allowed from one address in 10 minutes | `30` |

The per-email limit (one a minute, five an hour) is fixed in `OtpService`, because it protects people's inboxes and shouldn't be switched off by accident.

## Things to know

- **Behind nginx or Caddy**, the app reads the real customer address from the `X-Forwarded-For` header (`server.forward-headers-strategy=native`). Only run it behind a proxy you control, or anyone could fake that header.
- **Several backend copies** each keep their own per-address count in memory, so with 2 copies the real limit is up to twice the setting. Moving the count to Redis would make it exact. The per-email limit is already exact.
- **Restarting** the backend clears the per-address counts. The per-email counts stay.
- **Offices and mobile networks** can put many people behind one address. That's why the per-address limits are generous.
- **Tests** turn the per-address limit off (`src/test/resources/application-test.properties`), except `RateLimitIntegrationTest`, which turns it on with small numbers to check it.

The engineering reasons and the alternatives are in [design-decisions.md, decision 15](design-decisions.md#15-rate-limits-on-codes-and-logins).
