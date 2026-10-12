# Load test results

## Setup

- 2 backend copies behind nginx (`least_conn`), as in `docker-compose.yml`, each limited to **1 CPU and 1 GB RAM**
- MySQL 8.4, nginx and k6 on the same machine (4 vCPU Intel Xeon 2.8 GHz cloud VM)
- Fresh JVMs for every run (no warm-up), database re-seeded before each run
- Mail pointed at a closed port, so emails fail fast in the background

The numbers are small because each backend has a single CPU and shares the machine with everything else. Compare the before and after columns with each other, not with production traffic. Every scenario was run at least twice; ranges show the spread.

## Summary

| Scenario | Before | After |
|---|---|---|
| **Flash sale**: 500 buyers, 100 in stock | 100 orders, stock 0, but **84–248 of 500 requests failed** (401/500/502) | 100 orders, stock 0, **0 failures** (4 runs) |
| **Order history**: 100 users | 149–157 req/s, p95 760–795 ms | **213–263 req/s, p95 305–397 ms** |
| **Password login**: 20 users | ~20 logins/s, p95 ~1.5 s | ~20 logins/s, p95 ~1.4 s (bcrypt bound, unchanged) |

Overselling never happened, before or after. Every run ended with exactly 100 orders and stock at 0. The conditional `UPDATE` described in [design decisions §2](../docs/design-decisions.md) works. What was broken was everything *around* it under load.

## What I found and changed

### 1. A new JWT parser on every request (≈20% of CPU)

A profile with Java Flight Recorder during `browse.js` showed a lot of time in `URLClassPath` and `ServiceLoader`, which is class and resource lookup, on request threads. The cause: `JwtTokenProvider` called `Jwts.parserBuilder()...build()` for every request, twice (once to validate, once to read the email). jjwt looks up its JSON deserializer with `ServiceLoader` each time a parser is built, and inside a Spring Boot fat jar that means walking every nested jar.

**Fix:** build the signing key and the `JwtParser` once in the constructor and reuse them (`JwtParser` is immutable and thread-safe). As a bonus, a too-short `JWT_SECRET` now stops the app at startup instead of failing the first login.

### 2. 200 threads fighting over 10 DB connections

In the flash sale, 250 requests landed on each backend at once. Tomcat's default is 200 worker threads, so ~200 threads went straight for a Hikari pool of 10 connections. Requests that couldn't get a connection within `connection-timeout=5000` failed. Some of those failures happened inside the JWT filter's user lookup, which catches every exception, so the client saw **401 Unauthorized for what was really a database timeout**.

**Fix:** `server.tomcat.threads.max=20` (configurable with `TOMCAT_MAX_THREADS`). Excess requests now wait in Tomcat's connection queue, which has no 5-second limit, instead of waiting for a DB connection, which does. The burst turns into latency instead of errors. Fewer threads also means less context switching on a single CPU, which is a big part of why order-history throughput went up.

**Tradeoff:** a request stuck on a slow call now blocks one of 20 threads instead of one of 200. That's acceptable here because every DB and SMTP call has a timeout (see `application.properties`), so nothing can hold a thread for long.

## Still open

- **DB errors look like auth errors.** `JwtAuthenticationFilter` swallows every exception and lets the request continue unauthenticated. It should only treat a bad or unknown token as 401, and let infrastructure errors become 5xx so clients retry.
- **Every request reloads the user from MySQL** (`loadUserByUsername` in the JWT filter). That's about a fifth of the remaining CPU per request. Caching it for a short time, or trusting the claims in the token, would cut that, but disabled accounts would then take effect less quickly.
- **Flash-sale latency** is still high: p95 ≈ 9–10 s for 500 orders at once on 2 single-CPU backends. All orders for one product take turns on the same row lock. More CPU helps the waiting around it. Sharding stock across rows, or putting a queue in front of checkout, would be the next step for real flash sales.
- **Logins top out at ~10/s per CPU** because bcrypt (cost 10) is deliberately slow. That's the right tradeoff for password security, but login traffic needs its own capacity planning, and it's a good reason to add rate limiting.
