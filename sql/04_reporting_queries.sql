-- Ad-hoc portfolio SQL samples (JOIN + GROUP BY + aggregates) referenced by AnalyticsDao / interviews.
USE campus_eateries;

-- Top-selling paid items across the whole campus (normalized fact table: order_items).
SELECT mi.menu_item_id,
       mi.item_name,
       r.name                                   AS booth,
       SUM(oi.quantity)                         AS units,
       ROUND(SUM(oi.line_total), 2)            AS gross_line_revenue
FROM order_items oi
JOIN orders o      ON o.order_id = oi.order_id
JOIN menu_items mi ON mi.menu_item_id = oi.menu_item_id
JOIN restaurants r ON r.restaurant_id = mi.restaurant_id
JOIN payments py   ON py.order_id = o.order_id AND py.status = 'COMPLETED'
WHERE o.placed_at >= (CURRENT_TIMESTAMP - INTERVAL 30 DAY)
GROUP BY mi.menu_item_id, mi.item_name, r.name
ORDER BY units DESC
LIMIT 5;

-- Heavy users filtered to successful monetary settlement (analytics-friendly).
SELECT u.full_name,
       u.email,
       COUNT(DISTINCT o.order_id) AS settled_orders
FROM users u
JOIN orders o ON o.user_id = u.user_id
JOIN payments py ON py.order_id = o.order_id AND py.status = 'COMPLETED'
GROUP BY u.user_id, u.full_name, u.email
HAVING settled_orders >= 1
ORDER BY settled_orders DESC
LIMIT 5;

-- Daily revenue roll-up combining orders + payments (double-entry style guard).
SELECT DATE(o.placed_at)                AS calendar_day,
       ROUND(SUM(py.amount), 2)        AS paid_revenue
FROM orders o
JOIN payments py ON py.order_id = o.order_id AND py.status = 'COMPLETED'
GROUP BY DATE(o.placed_at)
ORDER BY calendar_day DESC;

-- Example stored procedure invocation for shift planning dashboards.
CALL sp_report_restaurant_daily_revenue(CURDATE());
