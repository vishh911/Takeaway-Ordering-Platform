-- Campus Eateries: normalized schema (3NF), MySQL 8+
-- Charset and engine defaults for FK support.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS campus_eateries;
CREATE DATABASE campus_eateries CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_eateries;

-- ---------------------------------------------------------------------------
-- Core identity
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    user_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email              VARCHAR(191) NOT NULL,
    password_hash      CHAR(64) NOT NULL COMMENT 'SHA-256 hex (demo only)',
    full_name          VARCHAR(120) NOT NULL,
    phone              VARCHAR(32) DEFAULT NULL,
    role               ENUM ('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    is_active          TINYINT(1) NOT NULL DEFAULT 1,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY ux_users_email (email)
) ENGINE=InnoDB;

-- Optional 1:1 extension for ADMIN users only (implements Admin entity logically).
CREATE TABLE admin_profiles (
    admin_user_id      BIGINT UNSIGNED NOT NULL,
    department         VARCHAR(100) DEFAULT NULL,
    clearance_level    TINYINT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (admin_user_id),
    CONSTRAINT fk_admin_user FOREIGN KEY (admin_user_id) REFERENCES users (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Catalog
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    category_id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name               VARCHAR(80) NOT NULL,
    PRIMARY KEY (category_id),
    UNIQUE KEY ux_categories_name (name)
) ENGINE=InnoDB;

CREATE TABLE restaurants (
    restaurant_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name               VARCHAR(160) NOT NULL,
    description        VARCHAR(512) DEFAULT NULL,
    campus_zone        VARCHAR(80) NOT NULL DEFAULT 'Main Quad',
    is_active          TINYINT(1) NOT NULL DEFAULT 1,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (restaurant_id),
    UNIQUE KEY ux_restaurants_name_zone (name, campus_zone)
) ENGINE=InnoDB;

CREATE TABLE menu_items (
    menu_item_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    restaurant_id      BIGINT UNSIGNED NOT NULL,
    category_id        INT UNSIGNED NOT NULL,
    item_name          VARCHAR(160) NOT NULL,
    description        VARCHAR(512) DEFAULT NULL,
    unit_price         DECIMAL(10,2) NOT NULL,
    stock_quantity     INT NOT NULL DEFAULT 0,
    is_available       TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (menu_item_id),
    CONSTRAINT fk_menu_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (restaurant_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_menu_category FOREIGN KEY (category_id) REFERENCES categories (category_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_menu_price CHECK (unit_price >= 0),
    CONSTRAINT chk_menu_stock CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Cart (shopping relationship: user × menu_item, many rows per user)
-- ---------------------------------------------------------------------------
CREATE TABLE shopping_cart_items (
    cart_entry_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id            BIGINT UNSIGNED NOT NULL,
    menu_item_id       BIGINT UNSIGNED NOT NULL,
    quantity           INT NOT NULL,
    PRIMARY KEY (cart_entry_id),
    UNIQUE KEY ux_cart_user_item (user_id, menu_item_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_cart_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (menu_item_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_cart_qty CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Orders / lines / fulfillment / monetary
-- ---------------------------------------------------------------------------
CREATE TABLE orders (
    order_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id            BIGINT UNSIGNED NOT NULL,
    restaurant_id      BIGINT UNSIGNED NOT NULL,
    status             ENUM (
        'DRAFT', 'PLACED', 'AWAITING_PAYMENT', 'PAID', 'PREPARING',
        'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'
    ) NOT NULL DEFAULT 'DRAFT',
    subtotal           DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_amount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    placed_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_orders_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (restaurant_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_orders_money CHECK (
        subtotal >= 0 AND tax_amount >= 0 AND total_amount >= 0
    )
) ENGINE=InnoDB;

CREATE TABLE order_items (
    order_item_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id           BIGINT UNSIGNED NOT NULL,
    menu_item_id       BIGINT UNSIGNED NOT NULL,
    quantity           INT NOT NULL,
    snapshot_unit_price DECIMAL(10,2) NOT NULL,
    line_total         DECIMAL(10,2) NOT NULL COMMENT 'qty * snapshot_price at order time',
    PRIMARY KEY (order_item_id),
    CONSTRAINT fk_lines_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_lines_menu FOREIGN KEY (menu_item_id) REFERENCES menu_items (menu_item_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_lines_qty CHECK (quantity > 0),
    CONSTRAINT chk_lines_line_total CHECK (line_total >= 0)
) ENGINE=InnoDB;

CREATE TABLE payments (
    payment_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id           BIGINT UNSIGNED NOT NULL,
    payment_method     ENUM ('CARD_SIM', 'DIGITAL_SIM', 'COD_SIM') NOT NULL,
    amount             DECIMAL(10,2) NOT NULL,
    transaction_ref    VARCHAR(64) DEFAULT NULL,
    status             ENUM ('INITIATED', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'INITIATED',
    paid_at            TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY ux_payments_order (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE delivery_details (
    delivery_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id           BIGINT UNSIGNED NOT NULL,
    building_code      VARCHAR(40) NOT NULL DEFAULT 'LIB',
    room_number        VARCHAR(40) DEFAULT NULL,
    special_instructions VARCHAR(512) DEFAULT NULL,
    eta_minutes        INT UNSIGNED DEFAULT NULL,
    PRIMARY KEY (delivery_id),
    UNIQUE KEY ux_delivery_order (order_id),
    CONSTRAINT fk_delivery_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;
