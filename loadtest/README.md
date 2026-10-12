# Load tests

[k6](https://k6.io) scripts that put the full stack (nginx, two backend copies, MySQL) under load. The results and what they led to are in [RESULTS.md](RESULTS.md).

| Script | What it simulates |
|---|---|
| `checkout-race.js` | Flash sale: 500 customers order the same product at once, with only 100 in stock. Passes only if exactly 100 orders succeed and nothing else errors. |
| `browse.js` | 100 logged-in customers repeatedly loading their order history (the JWT check plus a DB read). |
| `login.js` | 20 users logging in with a password non-stop. bcrypt makes this CPU-heavy on purpose. |
| `seed.sql` | 500 test users and a test product. |

## Running them

Use a throwaway database. `seed.sql` adds test users with a known password.

1. Start the stack: `docker compose up -d --build` (see the main README).
2. Load the test data:
   ```bash
   docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" farm_fresh' < loadtest/seed.sql
   ```
3. Run a script with k6, either [installed](https://grafana.com/docs/k6/latest/set-up/install-k6/) or through Docker:
   ```bash
   k6 run -e BASE_URL=http://localhost:8081 loadtest/checkout-race.js

   # or, without installing k6
   docker run --rm -i --network host grafana/k6 run -e BASE_URL=http://localhost:8081 - < loadtest/checkout-race.js
   ```
4. `checkout-race.js` uses up the stock, so load `seed.sql` again before each run.

To keep the test from emailing real people, set `SPRING_MAIL_HOST=localhost` for the backend. Emails then fail quickly in the background, and orders still go through.
