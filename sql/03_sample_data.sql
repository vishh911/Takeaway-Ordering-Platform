USE campus_eateries;

SET NAMES utf8mb4;

INSERT INTO categories (name) VALUES
  ('Breakfast'),
  ('Lunch'),
  ('Beverages'),
  ('Snacks');

INSERT INTO restaurants (name, description, campus_zone, is_active) VALUES
  ('Quad Brew', 'Coffee, smoothies, pastries', 'Main Quad', 1),
  ('STEM Bytes', 'Meal combos for late labs', 'Engineering Hill', 1),
  ('Arts Pantry', 'Salads & wraps near fine arts', 'Arts Row', 1);

INSERT INTO menu_items (
    restaurant_id, category_id, item_name, description, unit_price, stock_quantity, is_available
)
SELECT r.restaurant_id, c.category_id, d.item_name, d.description, d.unit_price, d.stock_quantity, d.is_available
FROM (
      SELECT 'Quad Brew' AS r_name, 'Beverages' AS c_name,
             'Latte Medium' AS item_name, 'Espresso and steamed milk' AS description, 4.25 AS unit_price, 200 AS stock_quantity, 1 AS is_available
      UNION ALL
      SELECT 'Quad Brew','Beverages','Cold Brew Large','Slow-steep iced coffee',3.95,150,1
      UNION ALL
      SELECT 'Quad Brew','Breakfast','Blueberry Muffin','Buttery crumble top',2.95,80,1
      UNION ALL
      SELECT 'STEM Bytes','Lunch','Veg Combo Box','Rice bowl + steamed veggies',8.95,60,1
      UNION ALL
      SELECT 'STEM Bytes','Lunch','Chicken Teriyaki','Grilled with broccoli',10.95,50,1
      UNION ALL
      SELECT 'STEM Bytes','Beverages','Lemon Ginger Tea','Hot',2.55,120,1
      UNION ALL
      SELECT 'Arts Pantry','Lunch','Caprese Wrap','Tomato mozzarella basil',7.95,40,1
      UNION ALL
      SELECT 'Arts Pantry','Lunch','Medit Bowl','Farro hummus greens',9.95,45,1
    ) AS d(r_name, c_name, item_name, description, unit_price, stock_quantity, is_available)
JOIN restaurants r ON r.name = d.r_name
JOIN categories c ON c.name = d.c_name;

-- Shared demo password SHA-256 hex of "password123" (.PasswordUtil parity)
INSERT INTO users (email, password_hash, full_name, phone, role, is_active) VALUES
  ('alice.student@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
   'Alice Carter', '+1-415-555-0101', 'STUDENT', 1),
  ('bob.student@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
   'Bob Li', '+1-415-555-0102', 'STUDENT', 1),
  ('admin.ops@campus.edu', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f',
   'Ops Admin', '+1-415-555-0199', 'ADMIN', 1);

INSERT INTO admin_profiles (admin_user_id, department, clearance_level)
SELECT user_id, 'Campus Catering', 2 FROM users WHERE email = 'admin.ops@campus.edu';

INSERT INTO shopping_cart_items (user_id, menu_item_id, quantity)
SELECT u.user_id, m.menu_item_id, 2
FROM users u
JOIN menu_items m ON m.item_name='Latte Medium'
WHERE u.email='alice.student@campus.edu'
LIMIT 1;

INSERT INTO shopping_cart_items (user_id, menu_item_id, quantity)
SELECT u.user_id, m.menu_item_id, 1
FROM users u
JOIN menu_items m ON m.item_name='Blueberry Muffin'
WHERE u.email='alice.student@campus.edu'
LIMIT 1;
