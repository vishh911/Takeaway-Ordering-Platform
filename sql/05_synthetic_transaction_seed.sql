-- Rich synthetic history for analytics (run AFTER 01_schema, 02_triggers_procedures, 03_sample_data).
-- Inserts paid orders over the last ~15 days so 04_reporting_queries + admin KPIs return non-empty rows.
-- Inventory triggers run: stock is padded first.

USE campus_eateries;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Extra diners (same demo password = SHA-256 of "password123")
-- ---------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, phone, role, is_active) VALUES
  ('carol.student@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
   'Carol Nguyen', '+1-415-555-0103', 'STUDENT', 1),
  ('dave.student@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
   'Dave Ortiz', '+1-415-555-0104', 'STUDENT', 1);

-- ---------------------------------------------------------------------------
-- Pad stock so BEFORE INSERT stock trigger passes for bulk historical lines
-- ---------------------------------------------------------------------------
UPDATE menu_items SET stock_quantity = GREATEST(stock_quantity, 4000);

-- ---------------------------------------------------------------------------
-- Helper pattern per order: @oid := LAST_INSERT_ID(); lines; delivery; payment COMPLETED
-- Totals use 8% tax rounded to cents (matches Java MoneyCalculator demo).
-- ---------------------------------------------------------------------------

-- O1: Alice @ Quad Brew, 5 days ago — lattes + muffins (high latte volume)
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'DELIVERED', 18.65, 1.49, 20.14,
       DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'alice.student@campus.edu' AND r.name = 'Quad Brew';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 3, 4.25, 12.75
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'Quad Brew' AND m.item_name = 'Latte Medium'
LIMIT 1;

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 2, 2.95, 5.90
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'Quad Brew' AND m.item_name = 'Blueberry Muffin'
LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'LIB', '204', 'Ring lab bench', 18);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'DIGITAL_SIM', 20.14, 'SYN-O1', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 5 DAY));

-- O2: Bob @ STEM Bytes, 3 days ago
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'DELIVERED', 27.55, 2.20, 29.75,
       DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'bob.student@campus.edu' AND r.name = 'STEM Bytes';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 1, 10.95, 10.95
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'STEM Bytes' AND m.item_name = 'Chicken Teriyaki' LIMIT 1;

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 1, 8.95, 8.95
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'STEM Bytes' AND m.item_name = 'Veg Combo Box' LIMIT 1;

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 3, 2.55, 7.65
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'STEM Bytes' AND m.item_name = 'Lemon Ginger Tea' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'ENG', 'B12', 'Leave with lab TA', 25);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'CARD_SIM', 29.75, 'SYN-O2', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- O3: Alice @ Arts Pantry, 1 day ago
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'PREPARING', 25.85, 2.07, 27.92,
       DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'alice.student@campus.edu' AND r.name = 'Arts Pantry';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 2, 7.95, 15.90
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'Arts Pantry' AND m.item_name = 'Caprese Wrap' LIMIT 1;

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 1, 9.95, 9.95
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'Arts Pantry' AND m.item_name = 'Medit Bowl' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'ART', 'Sculpture yard', 'Call on arrival', 22);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'COD_SIM', 27.92, 'SYN-O3', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- O4: Carol @ Quad Brew, 2 days ago — boosts Cold Brew rankings
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'DELIVERED', 23.70, 1.90, 25.60,
       DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'carol.student@campus.edu' AND r.name = 'Quad Brew';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 6, 3.95, 23.70
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'Quad Brew' AND m.item_name = 'Cold Brew Large' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'QUAD', 'Kiosk A', NULL, 12);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'CARD_SIM', 25.60, 'SYN-O4', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- O5: Dave @ STEM Bytes, 7 days ago — large chicken order
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'DELIVERED', 48.90, 3.91, 52.81,
       DATE_SUB(NOW(), INTERVAL 7 DAY)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'dave.student@campus.edu' AND r.name = 'STEM Bytes';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 4, 10.95, 43.80
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'STEM Bytes' AND m.item_name = 'Chicken Teriyaki' LIMIT 1;

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 2, 2.55, 5.10
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'STEM Bytes' AND m.item_name = 'Lemon Ginger Tea' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'ENG', 'H101', 'Evening section', 30);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'DIGITAL_SIM', 52.81, 'SYN-O5', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 7 DAY));

-- O6: Bob @ Quad Brew, 10 days ago — large latte study session
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'DELIVERED', 85.00, 6.80, 91.80,
       DATE_SUB(NOW(), INTERVAL 10 DAY)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'bob.student@campus.edu' AND r.name = 'Quad Brew';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 20, 4.25, 85.00
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'Quad Brew' AND m.item_name = 'Latte Medium' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'LIB', '301', 'Outside reading room', 15);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'CARD_SIM', 91.80, 'SYN-O6', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 10 DAY));

-- O7: Bob @ Arts Pantry, 6 days ago
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'DELIVERED', 49.75, 3.98, 53.73,
       DATE_SUB(NOW(), INTERVAL 6 DAY)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'bob.student@campus.edu' AND r.name = 'Arts Pantry';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 5, 9.95, 49.75
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'Arts Pantry' AND m.item_name = 'Medit Bowl' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'ART', 'Dance studio', 'Stage door', 20);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'DIGITAL_SIM', 53.73, 'SYN-O7', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 6 DAY));

-- O8: Dave @ STEM Bytes TODAY — populates “daily” procedure if run same calendar day
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'OUT_FOR_DELIVERY', 71.60, 5.73, 77.33,
       NOW()
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'dave.student@campus.edu' AND r.name = 'STEM Bytes';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 8, 8.95, 71.60
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'STEM Bytes' AND m.item_name = 'Veg Combo Box' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'ENG', 'Atrium', 'Meet at elevator', 25);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'CARD_SIM', 77.33, 'SYN-O8-TODAY', 'COMPLETED', NOW());

-- O9: In-flight ticket (INITIATED payment) for status funnel reports
INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount, placed_at)
SELECT u.user_id, r.restaurant_id, 'AWAITING_PAYMENT', 7.65, 0.61, 8.26,
       DATE_SUB(NOW(), INTERVAL 4 HOUR)
FROM users u
CROSS JOIN restaurants r
WHERE u.email = 'carol.student@campus.edu' AND r.name = 'STEM Bytes';

SET @oid := LAST_INSERT_ID();

INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
SELECT @oid, m.menu_item_id, 3, 2.55, 7.65
FROM menu_items m JOIN restaurants rr ON rr.restaurant_id = m.restaurant_id
WHERE rr.name = 'STEM Bytes' AND m.item_name = 'Lemon Ginger Tea' LIMIT 1;

INSERT INTO delivery_details (order_id, building_code, room_number, special_instructions, eta_minutes)
VALUES (@oid, 'ENG', 'Lobby', 'Awaiting payment gateway', 35);

INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status, paid_at)
VALUES (@oid, 'DIGITAL_SIM', 8.26, NULL, 'INITIATED', NULL);

-- ---------------------------------------------------------------------------
-- Quick verification counts (optional; comment out if piping breaks automation)
-- ---------------------------------------------------------------------------
SELECT 'orders' AS tbl, COUNT(*) AS n FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL SELECT 'payments_completed', COUNT(*) FROM payments WHERE status = 'COMPLETED'
UNION ALL SELECT 'payments_pending', COUNT(*) FROM payments WHERE status = 'INITIATED';
