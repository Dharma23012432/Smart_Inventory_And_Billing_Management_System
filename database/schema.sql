-- =============================================================================
-- Smart Inventory & Billing Management System
-- Database Schema Definition (PostgreSQL 16)
-- Specification: database/DATABASE_SPECIFICATION.md
-- Author: Dharma S
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Table: category
-- Groups products into logical classifications.
-- -----------------------------------------------------------------------------
CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_category_name UNIQUE (name)
);

COMMENT ON TABLE category IS 'Product catalogue categories';
COMMENT ON COLUMN category.name IS 'Unique display name of the category';

-- -----------------------------------------------------------------------------
-- 2. Table: supplier
-- Vendor contact details for purchasing and low-stock alert dispatch.
-- -----------------------------------------------------------------------------
CREATE TABLE supplier (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    contact_person VARCHAR(150),
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(20),
    address        TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_supplier_email UNIQUE (email),
    CONSTRAINT chk_supplier_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

COMMENT ON TABLE supplier IS 'Supplier business records and communication endpoints';
COMMENT ON COLUMN supplier.email IS 'Notification recipient email for purchase orders and low stock alerts';

-- -----------------------------------------------------------------------------
-- 3. Table: customer
-- Customer profile data for billing and invoice dispatch.
-- -----------------------------------------------------------------------------
CREATE TABLE customer (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(255),
    phone      VARCHAR(20),
    address    TEXT,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

COMMENT ON TABLE customer IS 'Customer records for sales and invoice delivery';
COMMENT ON COLUMN customer.email IS 'Email address used for sending electronic invoice PDFs';

-- -----------------------------------------------------------------------------
-- 4. Table: settings
-- Global runtime key-value application configuration.
-- -----------------------------------------------------------------------------
CREATE TABLE settings (
    id            BIGSERIAL PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    description   VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_settings_setting_key UNIQUE (setting_key)
);

COMMENT ON TABLE settings IS 'Global application and business profile settings';

-- -----------------------------------------------------------------------------
-- 5. Table: product
-- Product catalogue definition (pricing, SKU, category link).
-- -----------------------------------------------------------------------------
CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL,
    supplier_id     BIGINT,
    name            VARCHAR(200) NOT NULL,
    sku             VARCHAR(100) NOT NULL,
    description     TEXT,
    unit_price      NUMERIC(12, 2) NOT NULL,
    purchase_price  NUMERIC(12, 2) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id) ON DELETE SET NULL,
    CONSTRAINT uq_product_sku UNIQUE (sku),
    CONSTRAINT chk_product_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_product_purchase_price CHECK (purchase_price >= 0),
    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

COMMENT ON TABLE product IS 'Product definitions and pricing';
COMMENT ON COLUMN product.sku IS 'Unique Stock Keeping Unit business code';
COMMENT ON COLUMN product.purchase_price IS 'Last recorded unit purchase price from suppliers';

-- -----------------------------------------------------------------------------
-- 6. Table: inventory
-- Real-time stock levels with optimistic locking versioning (1:1 with product).
-- -----------------------------------------------------------------------------
CREATE TABLE inventory (
    id               BIGSERIAL PRIMARY KEY,
    product_id       BIGINT NOT NULL,
    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    reorder_level    INTEGER NOT NULL DEFAULT 10,
    max_stock_level  INTEGER NOT NULL DEFAULT 100,
    version          BIGINT NOT NULL DEFAULT 0,
    last_updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT uq_inventory_product_id UNIQUE (product_id),
    CONSTRAINT chk_inventory_quantity_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_inventory_reorder_level CHECK (reorder_level >= 0),
    CONSTRAINT chk_inventory_max_stock_level CHECK (max_stock_level > 0),
    CONSTRAINT chk_inventory_reorder_vs_max CHECK (reorder_level < max_stock_level),
    CONSTRAINT chk_inventory_version CHECK (version >= 0)
);

COMMENT ON TABLE inventory IS 'Product stock levels and reorder parameters';
COMMENT ON COLUMN inventory.version IS 'Optimistic locking version column for JPA concurrent checkout safety';

-- -----------------------------------------------------------------------------
-- 7. Table: purchase
-- Purchase orders placed with suppliers (Header).
-- -----------------------------------------------------------------------------
CREATE TABLE purchase (
    id            BIGSERIAL PRIMARY KEY,
    supplier_id   BIGINT NOT NULL,
    purchase_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_amount  NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id) ON DELETE RESTRICT,
    CONSTRAINT chk_purchase_total_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_purchase_status CHECK (status IN ('PENDING', 'RECEIVED', 'CANCELLED'))
);

COMMENT ON TABLE purchase IS 'Supplier purchase orders header';

-- -----------------------------------------------------------------------------
-- 8. Table: purchase_item
-- Line items within a purchase order (Detail).
-- -----------------------------------------------------------------------------
CREATE TABLE purchase_item (
    id          BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INTEGER NOT NULL,
    unit_cost   NUMERIC(12, 2) NOT NULL,
    total_cost  NUMERIC(14, 2) NOT NULL,
    CONSTRAINT fk_purchase_item_purchase FOREIGN KEY (purchase_id) REFERENCES purchase (id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_item_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT,
    CONSTRAINT uq_purchase_item_purchase_product UNIQUE (purchase_id, product_id),
    CONSTRAINT chk_purchase_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchase_item_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT chk_purchase_item_total_cost CHECK (total_cost >= 0)
);

COMMENT ON TABLE purchase_item IS 'Line items for purchase orders';
COMMENT ON COLUMN purchase_item.total_cost IS 'Stored computed amount: quantity * unit_cost';

-- -----------------------------------------------------------------------------
-- 9. Table: bill
-- Customer sales invoice (Header).
-- -----------------------------------------------------------------------------
CREATE TABLE bill (
    id              BIGSERIAL PRIMARY KEY,
    bill_number     VARCHAR(50) NOT NULL,
    customer_id     BIGINT NOT NULL,
    bill_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    subtotal        NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    tax_amount      NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_amount    NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    invoice_sent    BOOLEAN NOT NULL DEFAULT FALSE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bill_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE RESTRICT,
    CONSTRAINT uq_bill_bill_number UNIQUE (bill_number),
    CONSTRAINT chk_bill_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_bill_discount_amount CHECK (discount_amount >= 0),
    CONSTRAINT chk_bill_tax_amount CHECK (tax_amount >= 0),
    CONSTRAINT chk_bill_total_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_bill_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'PAID', 'CANCELLED'))
);

COMMENT ON TABLE bill IS 'Customer billing and invoice header';
COMMENT ON COLUMN bill.bill_number IS 'Unique business invoice identifier (e.g., INV-2026-0001)';
COMMENT ON COLUMN bill.invoice_sent IS 'Flag indicating whether invoice PDF email was dispatched to customer';

-- -----------------------------------------------------------------------------
-- 10. Table: bill_item
-- Line items within a customer bill (Detail).
-- -----------------------------------------------------------------------------
CREATE TABLE bill_item (
    id               BIGSERIAL PRIMARY KEY,
    bill_id          BIGINT NOT NULL,
    product_id       BIGINT NOT NULL,
    quantity         INTEGER NOT NULL,
    unit_price       NUMERIC(12, 2) NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    total_price      NUMERIC(14, 2) NOT NULL,
    CONSTRAINT fk_bill_item_bill FOREIGN KEY (bill_id) REFERENCES bill (id) ON DELETE CASCADE,
    CONSTRAINT fk_bill_item_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT,
    CONSTRAINT uq_bill_item_bill_product UNIQUE (bill_id, product_id),
    CONSTRAINT chk_bill_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_bill_item_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_bill_item_discount_percent CHECK (discount_percent >= 0 AND discount_percent < 100),
    CONSTRAINT chk_bill_item_total_price CHECK (total_price >= 0)
);

COMMENT ON TABLE bill_item IS 'Line items for customer sales bills';
COMMENT ON COLUMN bill_item.total_price IS 'Stored computed amount: quantity * unit_price * (1 - discount_percent/100)';

-- -----------------------------------------------------------------------------
-- 11. Table: payment
-- Payment transaction settling a confirmed bill (1:1 with bill).
-- -----------------------------------------------------------------------------
CREATE TABLE payment (
    id               BIGSERIAL PRIMARY KEY,
    bill_id          BIGINT NOT NULL,
    payment_date     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount_paid      NUMERIC(14, 2) NOT NULL,
    payment_method   VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    status           VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id) REFERENCES bill (id) ON DELETE RESTRICT,
    CONSTRAINT uq_payment_bill_id UNIQUE (bill_id),
    CONSTRAINT chk_payment_amount_paid CHECK (amount_paid > 0),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'CARD', 'UPI', 'BANK_TRANSFER', 'CHEQUE')),
    CONSTRAINT chk_payment_status CHECK (status IN ('COMPLETED', 'FAILED', 'REFUNDED'))
);

COMMENT ON TABLE payment IS 'Payment transactions for customer bills';

-- -----------------------------------------------------------------------------
-- 12. Table: notification
-- Persistent audit log and approval tracking for async email dispatches.
-- -----------------------------------------------------------------------------
CREATE TABLE notification (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    recipient_email VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    reference_id    BIGINT,
    reference_type  VARCHAR(50),
    error_message   TEXT,
    sent_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notification_type CHECK (type IN ('INVOICE_EMAIL', 'LOW_STOCK_ALERT')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'APPROVED', 'SENT', 'FAILED'))
);

COMMENT ON TABLE notification IS 'Notification lifecycle and email audit history';
COMMENT ON COLUMN notification.reference_id IS 'Soft foreign key to bill.id or product.id';

-- =============================================================================
-- INDEX DEFINITIONS
-- =============================================================================

-- Foreign Key Indexes
CREATE INDEX idx_product_category_id       ON product (category_id);
CREATE INDEX idx_purchase_supplier_id      ON purchase (supplier_id);
CREATE INDEX idx_purchase_item_purchase_id ON purchase_item (purchase_id);
CREATE INDEX idx_purchase_item_product_id  ON purchase_item (product_id);
CREATE INDEX idx_bill_customer_id          ON bill (customer_id);
CREATE INDEX idx_bill_item_bill_id         ON bill_item (bill_id);
CREATE INDEX idx_bill_item_product_id      ON bill_item (product_id);

-- Search and Filter Indexes
CREATE INDEX idx_product_status            ON product (status);
CREATE INDEX idx_product_name              ON product (name);
CREATE INDEX idx_supplier_name             ON supplier (name);
CREATE INDEX idx_customer_name             ON customer (name);
CREATE INDEX idx_customer_email            ON customer (email);
CREATE INDEX idx_customer_phone            ON customer (phone);
CREATE INDEX idx_purchase_status           ON purchase (status);
CREATE INDEX idx_bill_status               ON bill (status);
CREATE INDEX idx_bill_invoice_sent         ON bill (invoice_sent);
CREATE INDEX idx_notification_status       ON notification (status);
CREATE INDEX idx_notification_type         ON notification (type);

-- Date Range Indexes
CREATE INDEX idx_purchase_purchase_date    ON purchase (purchase_date);
CREATE INDEX idx_bill_bill_date            ON bill (bill_date);
CREATE INDEX idx_payment_payment_date      ON payment (payment_date);
CREATE INDEX idx_notification_created_at   ON notification (created_at);

-- Operational Inventory Index
CREATE INDEX idx_inventory_quantity_on_hand ON inventory (quantity_on_hand);

COMMIT;
