-- Test data for the load tests. Run it against a THROWAWAY database only,
-- after Untitled.sql has created the tables:
--   mysql -uroot -p farm_fresh < loadtest/seed.sql
--
-- Creates 500 activated users (loadtest1@example.com ... loadtest500@example.com),
-- all with the password LoadTest123! and a pending order OTP of 111111, and a
-- product "Load Test Mangoes" (id 9001) with exactly 100 in stock.

SET SESSION cte_max_recursion_depth = 1000;

DELETE oi FROM order_items oi JOIN orders o ON o.id = oi.order_id
  JOIN users u ON u.id = o.user_id WHERE u.email LIKE 'loadtest%@example.com';
DELETE o FROM orders o JOIN users u ON u.id = o.user_id WHERE u.email LIKE 'loadtest%@example.com';
DELETE FROM users WHERE email LIKE 'loadtest%@example.com';
DELETE FROM products WHERE id = 9001;

INSERT INTO users (name, email, password, role, enabled, failed_attempts, otp_code, otp_expiry)
WITH RECURSIVE seq (n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 500
)
SELECT CONCAT('Load Test ', n),
       CONCAT('loadtest', n, '@example.com'),
       -- bcrypt hash of LoadTest123!
       '$2a$10$sEoNLfjRWbqI34I7zFKr/udpXCzylppAvTWeSrmWbQpJ/JtjD9ok.',
       'ROLE_USER', TRUE, 0, '111111',
       UTC_TIMESTAMP() + INTERVAL 1 DAY
FROM seq;

INSERT INTO products (id, name, price, original_price, weight, stock_quantity)
VALUES (9001, 'Load Test Mangoes', 250.00, 250.00, '1 kg', 100);
