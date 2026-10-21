-- =============================================================================
-- Smart Inventory & Billing Management System
-- Seed Data Script for Development & Testing
-- Specification: database/DATABASE_SPECIFICATION.md
-- Author: Dharma S
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Seed: settings
-- Pre-populate required system and business configuration.
-- -----------------------------------------------------------------------------
INSERT INTO settings (setting_key, setting_value, description) VALUES
('company_name', 'Apex Retail & Inventory Solutions', 'Business legal name printed on sales invoices and receipts'),
('company_email', 'contact@apexretail.com', 'System email address used as sender on notifications'),
('company_phone', '+91 98765 43210', 'Primary customer care phone number'),
('company_address', '102 Commercial Arcade, Brigade Road, Bangalore, KA 560001', 'Official business address printed on invoice headers'),
('currency_symbol', '₹', 'Currency symbol displayed across the frontend UI'),
('currency_code', 'INR', 'ISO 4217 currency code for financial transactions'),
('default_tax_rate', '18.00', 'Standard default GST/tax percentage for bill calculations'),
('low_stock_notification_enabled', 'true', 'Master toggle to trigger LowStockDetected events on RabbitMQ');

-- -----------------------------------------------------------------------------
-- 2. Seed: category
-- Standard product taxonomy categories.
-- -----------------------------------------------------------------------------
INSERT INTO category (name, description) VALUES
('Electronics', 'Electronic devices, computer peripherals, and accessories'),
('Food & Beverages', 'Consumable packaged food, beverages, and snacks'),
('Stationery', 'Office supplies, paper products, and writing instruments'),
('Clothing', 'Garments, apparel, uniforms, and textiles'),
('Household', 'Home cleaning supplies, kitchenware, and daily essentials'),
('Tools & Hardware', 'Hand tools, fasteners, power accessories, and hardware'),
('Health & Personal Care', 'Medicines, first-aid, toiletries, and personal care items'),
('Others', 'Miscellaneous and uncategorised inventory items');

-- -----------------------------------------------------------------------------
-- 3. Seed: supplier
-- Verified vendors with valid contact details for procurement and alert testing.
-- -----------------------------------------------------------------------------
INSERT INTO supplier (name, contact_person, email, phone, address, status) VALUES
('TechSource Distribution Ltd', 'Rahul Verma', 'orders@techsource.com', '+91 98450 11223', 'Plot 45, Peenya Industrial Area, Phase 2, Bangalore, KA 560058', 'ACTIVE'),
('Evergreen FMCG Wholesalers', 'Ananya Iyer', 'sales@evergreenfmcg.in', '+91 98200 44556', 'Warehouse 12, APMC Market Yard, Vashi, Navi Mumbai, MH 400703', 'ACTIVE'),
('Metro Office & Stationery Supplies', 'Karthik Sundaram', 'supply@metrostores.co.in', '+91 94440 77889', '88 Anna Salai, Guindy, Chennai, TN 600032', 'ACTIVE');

-- -----------------------------------------------------------------------------
-- 4. Seed: customer
-- Baseline customers: walk-in, individual consumer, and corporate account.
-- -----------------------------------------------------------------------------
INSERT INTO customer (name, email, phone, address, status) VALUES
('Walk-in Customer', NULL, NULL, NULL, 'ACTIVE'),
('Priya Sharma', 'priya.sharma@example.com', '+91 99887 66554', 'Flat 402, Green Glen Layout, Bellandur, Bangalore, KA 560103', 'ACTIVE'),
('BlueHorizon Tech Pvt Ltd', 'finance@bluehorizon.io', '+91 80 4123 5678', 'Level 3, Embassy Tech Village, Outer Ring Road, Bangalore, KA 560103', 'ACTIVE');

-- -----------------------------------------------------------------------------
-- 5. Seed: product
-- Sample catalog items mapped to categories and default suppliers.
-- -----------------------------------------------------------------------------
INSERT INTO product (category_id, supplier_id, name, sku, description, unit_price, purchase_price, unit_of_measure, status) VALUES
((SELECT id FROM category WHERE name = 'Electronics'), (SELECT id FROM supplier WHERE name = 'TechSource Distribution Ltd'),
 'Logitech Wireless Mouse M220 Silent', 'ELEC-LOG-M220', 'Compact 2.4GHz wireless optical mouse with 90% noise reduction', 899.00, 620.00, 'PCS', 'ACTIVE'),

((SELECT id FROM category WHERE name = 'Electronics'), (SELECT id FROM supplier WHERE name = 'TechSource Distribution Ltd'),
 'SanDisk Ultra 64GB USB 3.0 Pen Drive', 'ELEC-SAN-64GB', 'High-speed USB 3.0 flash drive with read speeds up to 130MB/s', 549.00, 380.00, 'PCS', 'ACTIVE'),

((SELECT id FROM category WHERE name = 'Stationery'), (SELECT id FROM supplier WHERE name = 'Metro Office & Stationery Supplies'),
 'JK Copier Paper A4 75 GSM (500 Sheets)', 'STAT-JK-A4-75', 'Premium multi-purpose office printing and photocopying paper', 320.00, 240.00, 'BOX', 'ACTIVE'),

((SELECT id FROM category WHERE name = 'Stationery'), (SELECT id FROM supplier WHERE name = 'Metro Office & Stationery Supplies'),
 'Pilot G2 0.7mm Retractable Gel Pen (Blue)', 'STAT-PIL-G2', 'Smooth writing rollerball gel pen with dynamic gel ink formula', 60.00, 42.00, 'PCS', 'ACTIVE'),

((SELECT id FROM category WHERE name = 'Food & Beverages'), (SELECT id FROM supplier WHERE name = 'Evergreen FMCG Wholesalers'),
 'Nescafe Classic Instant Coffee 200g Jar', 'FMCG-NES-CL200', '100% pure natural robusta coffee blend with signature rich aroma', 480.00, 390.00, 'PCS', 'ACTIVE'),

((SELECT id FROM category WHERE name = 'Tools & Hardware'), (SELECT id FROM supplier WHERE name = 'TechSource Distribution Ltd'),
 'Bosch Screwdriver Bit Set (10 Pieces)', 'TOOL-BOS-SD10', 'Universal magnetic screwdriver bits for home and workshop DIY', 750.00, 510.00, 'BOX', 'ACTIVE');

-- -----------------------------------------------------------------------------
-- 6. Seed: inventory
-- Initial stock counts corresponding 1:1 with seeded products.
-- Note: 'STAT-PIL-G2' is seeded with stock below reorder level (8 <= 12)
-- to facilitate testing of low-stock detection mechanisms.
-- -----------------------------------------------------------------------------
INSERT INTO inventory (product_id, quantity_on_hand, reorder_level, max_stock_level, version) VALUES
((SELECT id FROM product WHERE sku = 'ELEC-LOG-M220'),  35, 10, 100, 0),
((SELECT id FROM product WHERE sku = 'ELEC-SAN-64GB'),  50, 15, 150, 0),
((SELECT id FROM product WHERE sku = 'STAT-JK-A4-75'),  20,  8,  80, 0),
((SELECT id FROM product WHERE sku = 'STAT-PIL-G2'),     8, 12, 100, 0), -- Low stock scenario
((SELECT id FROM product WHERE sku = 'FMCG-NES-CL200'), 40, 10, 120, 0),
((SELECT id FROM product WHERE sku = 'TOOL-BOS-SD10'),  15,  5,  50, 0);

COMMIT;
