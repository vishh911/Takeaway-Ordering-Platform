USE campus_eateries;

DELIMITER $$

DROP TRIGGER IF EXISTS trg_bi_order_items_enforce_stock$$
CREATE TRIGGER trg_bi_order_items_enforce_stock
BEFORE INSERT ON order_items
FOR EACH ROW
BEGIN
    DECLARE v_stock INT UNSIGNED;

    SELECT stock_quantity
    INTO v_stock
    FROM menu_items
    WHERE menu_item_id = NEW.menu_item_id
    LIMIT 1;

    IF v_stock IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Menu item missing for inventory check.';
    ELSEIF v_stock < NEW.quantity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient inventory for line item.';
    END IF;
END$$


/* IMPORTANT:
   Removed AFTER INSERT trigger that updated menu_items.
   That trigger was causing ERROR 1442.
*/


DROP TRIGGER IF EXISTS trg_au_payments_finalize_order$$
CREATE TRIGGER trg_au_payments_finalize_order
AFTER UPDATE ON payments
FOR EACH ROW
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status <> 'COMPLETED' THEN
        UPDATE orders o
        SET o.status = 'PREPARING',
            o.updated_at = CURRENT_TIMESTAMP
        WHERE o.order_id = NEW.order_id
          AND o.status IN ('PLACED','AWAITING_PAYMENT','PAID');
    END IF;
END$$


DROP TRIGGER IF EXISTS trg_bu_payments_validate_completion$$
CREATE TRIGGER trg_bu_payments_validate_completion
BEFORE UPDATE ON payments
FOR EACH ROW
BEGIN
    DECLARE v_total DECIMAL(10,2);

    IF NEW.status = 'COMPLETED' AND OLD.status <> 'COMPLETED' THEN
        SELECT total_amount
        INTO v_total
        FROM orders
        WHERE order_id = NEW.order_id;

        IF v_total IS NULL OR NEW.amount <> v_total THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Payment amount must match order total to complete.';
        END IF;
    END IF;
END$$


DROP PROCEDURE IF EXISTS sp_simulate_pay_order$$
CREATE PROCEDURE sp_simulate_pay_order (
    IN p_order_id BIGINT UNSIGNED,
    IN p_method VARCHAR(20),
    OUT p_ok TINYINT
)
sp_sim_pay:BEGIN
    DECLARE v_total DECIMAL(10,2);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_ok = 0;
    END;

    SET p_ok = 0;

    START TRANSACTION;

    SELECT total_amount
    INTO v_total
    FROM orders
    WHERE order_id = p_order_id
    FOR UPDATE;

    IF v_total IS NULL THEN
        ROLLBACK;
        LEAVE sp_sim_pay;
    END IF;

    UPDATE payments
    SET payment_method = p_method,
        amount = v_total,
        status = 'COMPLETED',
        transaction_ref = CONCAT('SIM-', REPLACE(UUID(), '-', '')),
        paid_at = CURRENT_TIMESTAMP
    WHERE order_id = p_order_id
      AND status = 'INITIATED'
    LIMIT 1;

    IF ROW_COUNT() = 0 THEN
        ROLLBACK;
        LEAVE sp_sim_pay;
    END IF;

    COMMIT;
    SET p_ok = 1;
END$$


DROP PROCEDURE IF EXISTS sp_report_restaurant_daily_revenue$$
CREATE PROCEDURE sp_report_restaurant_daily_revenue(IN p_day DATE)
BEGIN
    SELECT
        r.restaurant_id,
        r.name AS restaurant_name,
        COUNT(DISTINCT CASE
            WHEN DATE(o.placed_at) = p_day THEN o.order_id
        END) AS orders_placed_that_day,
        ROUND(IFNULL(SUM(CASE
            WHEN DATE(o.placed_at) = p_day
             AND py.status = 'COMPLETED'
            THEN py.amount
        END), 0), 2) AS revenue_completed_that_day
    FROM restaurants r
    LEFT JOIN orders o ON o.restaurant_id = r.restaurant_id
    LEFT JOIN payments py ON py.order_id = o.order_id
    GROUP BY r.restaurant_id, r.name
    ORDER BY revenue_completed_that_day DESC;
END$$


DROP PROCEDURE IF EXISTS sp_analytics_order_insights$$
CREATE PROCEDURE sp_analytics_order_insights(IN p_recent_hours INT UNSIGNED)
BEGIN
    SELECT status, COUNT(*) AS order_count
    FROM orders
    WHERE placed_at >= (NOW() - INTERVAL p_recent_hours HOUR)
    GROUP BY status
    ORDER BY order_count DESC;
END$$

DELIMITER ;


DROP VIEW IF EXISTS v_restaurant_performance;

CREATE VIEW v_restaurant_performance AS
SELECT
    r.restaurant_id,
    r.name AS restaurant_name,
    COUNT(DISTINCT o.order_id) AS lifetime_orders,
    ROUND(IFNULL(SUM(oi.quantity), 0), 0) AS lifetime_units_sold,
    ROUND(IFNULL(SUM(py.amount), 0), 2) AS lifetime_paid_amount
FROM restaurants r
LEFT JOIN orders o ON o.restaurant_id = r.restaurant_id
LEFT JOIN order_items oi ON oi.order_id = o.order_id
LEFT JOIN payments py ON py.order_id = o.order_id
                  AND py.status = 'COMPLETED'
GROUP BY r.restaurant_id, r.name;