# DATABASE_SPECIFICATION.md

**Project:** Smart Inventory & Billing Management System
**Version:** 1.0
**Author:** Dharma S
**Phase:** 2 — Database Design
**Status:** Draft — Awaiting Review

---

## Table of Contents

1. [Database Overview](#1-database-overview)
2. [Naming Conventions](#2-naming-conventions)
3. [Entity Relationship Overview](#3-entity-relationship-overview)
4. [Complete Table Specifications](#4-complete-table-specifications)
5. [Relationship Explanations](#5-relationship-explanations)
6. [Constraints](#6-constraints)
7. [Index Strategy](#7-index-strategy)
8. [Seed Data Strategy](#8-seed-data-strategy)
9. [Future Database Improvements](#9-future-database-improvements)
10. [Design Decisions](#10-design-decisions)

---

## 1. Database Overview

### 1.1 Purpose

The database is the single persistent storage layer for the Smart Inventory & Billing Management System. It stores all business data including products, inventory levels, customers, suppliers, purchase records, bills, payments, and notifications.

All three backend microservices — `inventory-service`, `payment-service`, and `notification-service` — share this single PostgreSQL database in Version 1. This decision is intentional and is explained in detail in [Section 10 — Design Decisions](#10-design-decisions).

### 1.2 Database Engine

- **Engine:** PostgreSQL 16
- **Database Name:** `smart_inventory_db`
- **Character Set:** UTF-8
- **Timezone:** UTC (all timestamps stored in UTC using `TIMESTAMPTZ`)

### 1.3 Normalization Strategy

The database is designed to **Third Normal Form (3NF)**.

**What this means:**
- **1NF:** Every column holds an atomic (indivisible) value. No repeating groups. Every row is uniquely identifiable.
- **2NF:** Every non-key column is fully functionally dependent on the entire primary key (no partial dependencies).
- **3NF:** Every non-key column depends only on the primary key, not on other non-key columns (no transitive dependencies).

**Why 3NF and not higher?**
- 3NF provides an excellent balance between data integrity and query simplicity for an inventory and billing domain.
- BCNF and 4NF introduce additional join complexity that is not justified for this application's data volume and complexity in Version 1.
- Denormalization may be selectively introduced in future versions if performance profiling reveals specific bottlenecks.

### 1.4 Design Principles

| Principle | Description |
|---|---|
| **Single Source of Truth** | Each business fact is stored in exactly one place |
| **Referential Integrity** | All foreign keys are enforced at the database level |
| **Not Null by Default** | Columns are NOT NULL unless there is a specific business reason to allow null |
| **Explicit Defaults** | Default values are defined at the database level, not only in application code |
| **Audit Timestamps** | Every table includes `created_at` and, where relevant, `updated_at` (as `TIMESTAMPTZ`) |
| **Singular Table Names** | Tables are named as singular nouns (e.g., `product`, not `products`) |
| **No Magic Numbers** | Enum-like values (status fields) use descriptive string constants, not integers |
| **Separation of Concerns** | Billing and payment are separate tables because they represent separate business events |

---

## 2. Naming Conventions

### 2.1 Table Names

- **Format:** `snake_case`, singular noun
- **Examples:** `product`, `bill_item`, `purchase_item`
- **Rationale:** Consistent with PostgreSQL community conventions. Singular nouns align with the JPA entity naming convention (`@Entity` class names are singular).

### 2.2 Column Names

- **Format:** `snake_case`
- **Examples:** `product_name`, `unit_price`, `created_at`, `reorder_level`
- **Rationale:** PostgreSQL is case-insensitive by default for unquoted identifiers. `snake_case` avoids ambiguity and is idiomatic in PostgreSQL.

### 2.3 Primary Keys

- **Format:** `id`
- **Type:** `BIGSERIAL` (auto-incrementing 64-bit integer)
- **Convention:** Every table has a single-column surrogate primary key named `id`
- **Rationale:** Surrogate keys decouple physical identity from business identity. Business identifiers (e.g., product codes) may change; surrogate keys never change.

### 2.4 Foreign Keys

- **Format:** `{referenced_table}_id`
- **Examples:** `product_id`, `supplier_id`, `customer_id`, `bill_id`
- **Rationale:** Immediately communicates what entity is being referenced without needing to inspect the constraint definition.

### 2.5 Constraint Names

| Constraint Type | Format | Example |
|---|---|---|
| Primary Key | `pk_{table}` | `pk_product` |
| Foreign Key | `fk_{table}_{referenced_table}` | `fk_product_category` |
| Unique | `uq_{table}_{column}` | `uq_product_sku` |
| Check | `chk_{table}_{column}` | `chk_product_unit_price` |
| Index | `idx_{table}_{column(s)}` | `idx_product_category_id` |

### 2.6 Timestamp Columns

- **Data Type:** `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`)
- **Timezone:** UTC (`CURRENT_TIMESTAMP`)
- `created_at` — the moment the row was first inserted. Always NOT NULL, default `CURRENT_TIMESTAMP`.
- `updated_at` — the moment the row was last modified. NOT NULL, updated by application logic on every UPDATE.
- `last_updated_at` / `payment_date` / `sent_at` — specific operational timestamps, all uniformly typed as `TIMESTAMPTZ`.
- **Rationale:** Storing timezone-aware timestamps normalized to UTC prevents timezone drift, clock skew, and reporting discrepancies across different application nodes, Docker containers, and client locales.

### 2.7 Status Columns

- **Type:** `VARCHAR`
- **Values:** Uppercase string constants (e.g., `'ACTIVE'`, `'PENDING'`, `'COMPLETED'`, `'CANCELLED'`)
- **Enforced by:** `CHECK` constraint listing all valid values
- **Rationale:** String constants are human-readable in the database, in logs, and in API responses. They are less fragile than integers and more flexible than PostgreSQL native ENUM types (which are difficult to alter after creation).

---

## 3. Entity Relationship Overview

### 3.1 All Tables

| # | Table | Domain Area |
|---|---|---|
| 1 | `category` | Product catalogue |
| 2 | `product` | Product catalogue |
| 3 | `inventory` | Stock management |
| 4 | `supplier` | Supplier management |
| 5 | `customer` | Customer management |
| 6 | `purchase` | Procurement |
| 7 | `purchase_item` | Procurement (line items) |
| 8 | `bill` | Sales / Billing |
| 9 | `bill_item` | Sales (line items) |
| 10 | `payment` | Payment processing |
| 11 | `notification` | Event notifications |
| 12 | `settings` | Application configuration |

### 3.2 Relationship Map

```
category ──────────────────────── product (1:N)
                                      │
                          ┌───────────┤
                          │           │
                       inventory   purchase_item
                       (1:1)       (N:1)
                                      │
supplier ─────────────── purchase (1:N from supplier)
                          │
                       purchase_item (1:N from purchase)

customer ──────────────── bill (1:N)
                          │
                       bill_item (1:N)
                          │
                       product (N:1)

bill ─────────────────── payment (1:1)

notification ─────────── (standalone, linked to events by reference)
settings ─────────────── (standalone, key-value configuration)
```

### 3.3 Cardinality Summary

| Relationship | Type | Description |
|---|---|---|
| `category` → `product` | One-to-Many | One category can have many products |
| `product` → `inventory` | One-to-One | Each product has exactly one inventory record |
| `supplier` → `purchase` | One-to-Many | One supplier can have many purchase orders |
| `purchase` → `purchase_item` | One-to-Many | One purchase can have many line items |
| `product` → `purchase_item` | One-to-Many | One product can appear in many purchase items |
| `customer` → `bill` | One-to-Many | One customer can have many bills |
| `bill` → `bill_item` | One-to-Many | One bill can have many line items |
| `product` → `bill_item` | One-to-Many | One product can appear in many bill items |
| `bill` → `payment` | One-to-One | Each bill has at most one payment record |

---

## 4. Complete Table Specifications

---

### 4.1 `category`

**Purpose:**
Groups products into logical categories (e.g., Electronics, Food & Beverages, Stationery). Enables product filtering, reporting by category, and business organisation.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `name` | `VARCHAR(100)` | NOT NULL | — | Unique name of the category |
| `description` | `VARCHAR(255)` | NULL | — | Optional description |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**Primary Key:** `id`

**Unique Constraints:**
- `uq_category_name` on `name` — Category names must be unique across the system

**Check Constraints:** None

**Indexes:**
- Primary key index on `id` (automatic)
- Unique index on `name` (from unique constraint)

**Business Rules:**
- A category cannot be deleted if any product references it (referential integrity enforced by foreign key on `product`)
- `name` must be trimmed of leading and trailing whitespace (enforced at application layer)

---

### 4.2 `product`

**Purpose:**
Represents a product in the catalogue. Stores the core product definition including pricing and category classification. Does NOT store quantity — quantity is managed by the `inventory` table.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `category_id` | `BIGINT` | NOT NULL | — | Foreign key → `category.id` |
| `supplier_id` | `BIGINT` | NULL | — | Default/primary supplier (optional) |
| `name` | `VARCHAR(200)` | NOT NULL | — | Product display name |
| `sku` | `VARCHAR(100)` | NOT NULL | — | Stock Keeping Unit — unique business identifier |
| `description` | `TEXT` | NULL | — | Detailed product description |
| `unit_price` | `NUMERIC(12, 2)` | NOT NULL | — | Selling price per unit |
| `purchase_price` | `NUMERIC(12, 2)` | NOT NULL | — | Cost price per unit (last purchase price) |
| `unit_of_measure` | `VARCHAR(50)` | NOT NULL | — | e.g., `'PCS'`, `'KG'`, `'LTR'`, `'BOX'` |
| `status` | `VARCHAR(20)` | NOT NULL | `'ACTIVE'` | Product status |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**Primary Key:** `id`

**Foreign Keys:**
- `fk_product_category` → `category(id)` — RESTRICT on delete (cannot delete category with products)
- `fk_product_supplier` → `supplier(id)` — SET NULL on delete (product remains if default supplier is removed)

**Unique Constraints:**
- `uq_product_sku` on `sku` — SKU must be globally unique

**Check Constraints:**
- `chk_product_unit_price` — `unit_price > 0`
- `chk_product_purchase_price` — `purchase_price >= 0`
- `chk_product_status` — `status IN ('ACTIVE', 'INACTIVE')`

**Indexes:**
- Primary key index on `id` (automatic)
- Unique index on `sku`
- `idx_product_category_id` on `category_id`
- `idx_product_status` on `status`
- `idx_product_name` on `name`

**Business Rules:**
- A product with `status = 'INACTIVE'` cannot be added to a new bill or purchase
- `sku` is assigned by the user and must be unique — it serves as the human-readable identifier
- `purchase_price` is updated each time a new purchase is received (updated by application logic)
- `supplier_id` is a convenience reference to the typical/default supplier — a product can still be purchased from other suppliers

---

### 4.3 `inventory`

**Purpose:**
Tracks the current stock level for each product. Maintains the quantity on hand, reorder level, and maximum stock capacity. Separated from `product` so that stock updates do not require updating the product record itself, and to support clear audit boundaries.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `product_id` | `BIGINT` | NOT NULL | — | Foreign key → `product.id` (unique — 1:1) |
| `quantity_on_hand` | `INTEGER` | NOT NULL | `0` | Current stock quantity |
| `reorder_level` | `INTEGER` | NOT NULL | `10` | Trigger level for low-stock notifications |
| `max_stock_level` | `INTEGER` | NOT NULL | `100` | Maximum storage capacity |
| `version` | `BIGINT` | NOT NULL | `0` | Optimistic locking version for concurrent stock updates |
| `last_updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last stock movement timestamp |

**Primary Key:** `id`

**Foreign Keys:**
- `fk_inventory_product` → `product(id)` — CASCADE on delete (if a product is deleted, its inventory record is also deleted)

**Unique Constraints:**
- `uq_inventory_product_id` on `product_id` — enforces the one-to-one relationship

**Check Constraints:**
- `chk_inventory_quantity_on_hand` — `quantity_on_hand >= 0`
- `chk_inventory_reorder_level` — `reorder_level >= 0`
- `chk_inventory_max_stock_level` — `max_stock_level > 0`
- `chk_inventory_reorder_vs_max` — `reorder_level < max_stock_level`
- `chk_inventory_version` — `version >= 0`

**Indexes:**
- Primary key index on `id` (automatic)
- Unique index on `product_id` (from unique constraint)
- `idx_inventory_quantity_on_hand` on `quantity_on_hand`

**Business Rules:**
- An `inventory` record is automatically created when a product is created (application logic)
- `version` supports JPA `@Version` optimistic locking to prevent race conditions and stock overselling during concurrent billing checkouts
- `quantity_on_hand` increases when a purchase is received
- `quantity_on_hand` decreases when a sale (bill) is confirmed
- When `quantity_on_hand <= reorder_level`, a `LowStockDetected` event is published to RabbitMQ
- `quantity_on_hand` can never go below `0` (enforced by check constraint and application logic)

---

### 4.4 `supplier`

**Purpose:**
Stores supplier contact and business information. Suppliers are the vendors from whom the business purchases products. The notification service emails suppliers when a low-stock alert is approved.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `name` | `VARCHAR(200)` | NOT NULL | — | Supplier company or individual name |
| `contact_person` | `VARCHAR(150)` | NULL | — | Primary contact name |
| `email` | `VARCHAR(255)` | NOT NULL | — | Email address for notifications and communication |
| `phone` | `VARCHAR(20)` | NULL | — | Contact phone number |
| `address` | `TEXT` | NULL | — | Physical or mailing address |
| `status` | `VARCHAR(20)` | NOT NULL | `'ACTIVE'` | Supplier status |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**Primary Key:** `id`

**Foreign Keys:** None

**Unique Constraints:**
- `uq_supplier_email` on `email` — each supplier must have a unique email

**Check Constraints:**
- `chk_supplier_status` — `status IN ('ACTIVE', 'INACTIVE')`

**Indexes:**
- Primary key index on `id` (automatic)
- Unique index on `email`
- `idx_supplier_name` on `name`

**Business Rules:**
- `email` is the address used by the notification service to send low-stock alerts
- A supplier with `status = 'INACTIVE'` cannot be selected for new purchase orders (application layer)
- Deleting a supplier is not permitted if they have existing purchase records (referential integrity via `purchase.supplier_id`)

---

### 4.5 `customer`

**Purpose:**
Stores customer contact information. Customers are the individuals or businesses that purchase products from the system. The notification service emails customers their invoice after a payment is completed.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `name` | `VARCHAR(200)` | NOT NULL | — | Customer name |
| `email` | `VARCHAR(255)` | NULL | — | Email for invoice delivery (can be null for walk-in) |
| `phone` | `VARCHAR(20)` | NULL | — | Contact phone number |
| `address` | `TEXT` | NULL | — | Billing or delivery address |
| `status` | `VARCHAR(20)` | NOT NULL | `'ACTIVE'` | Customer status |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**Primary Key:** `id`

**Foreign Keys:** None

**Unique Constraints:** None — `email` is intentionally not unique; some businesses share an email address. Application layer can warn but not block.

**Check Constraints:**
- `chk_customer_status` — `status IN ('ACTIVE', 'INACTIVE')`

**Indexes:**
- Primary key index on `id` (automatic)
- `idx_customer_email` on `email`
- `idx_customer_name` on `name`
- `idx_customer_phone` on `phone`

**Business Rules:**
- If `email` is NULL, the notification service skips invoice email dispatch and logs a warning
- A customer with `status = 'INACTIVE'` cannot be selected for a new bill (application layer)
- Walk-in customers may be recorded with minimal information (only `name` is required)

---

### 4.6 `purchase`

**Purpose:**
Represents a purchase order placed with a supplier. A purchase is the header record — it captures the supplier, total amount, status, and date. The individual items within the purchase are stored in `purchase_item`.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `supplier_id` | `BIGINT` | NOT NULL | — | Foreign key → `supplier.id` |
| `purchase_date` | `DATE` | NOT NULL | `CURRENT_DATE` | Date the purchase was placed |
| `total_amount` | `NUMERIC(14, 2)` | NOT NULL | `0.00` | Sum of all purchase item totals |
| `status` | `VARCHAR(20)` | NOT NULL | `'PENDING'` | Purchase status |
| `notes` | `TEXT` | NULL | — | Optional remarks or reference numbers |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**Primary Key:** `id`

**Foreign Keys:**
- `fk_purchase_supplier` → `supplier(id)` — RESTRICT on delete

**Check Constraints:**
- `chk_purchase_total_amount` — `total_amount >= 0`
- `chk_purchase_status` — `status IN ('PENDING', 'RECEIVED', 'CANCELLED')`

**Indexes:**
- `idx_purchase_supplier_id` on `supplier_id`
- `idx_purchase_status` on `status`
- `idx_purchase_purchase_date` on `purchase_date`

**Purchase Status Lifecycle:**

```
PENDING → RECEIVED
   └──→ CANCELLED
```

**Business Rules:**
- When `status` changes from `'PENDING'` to `'RECEIVED'`, the application updates `inventory.quantity_on_hand` for each item
- A `CANCELLED` purchase does not affect inventory
- `total_amount` is computed by the application as the sum of `(quantity × unit_cost)` across all `purchase_item` rows
- A purchase must have at least one `purchase_item` before it can be saved

---

### 4.7 `purchase_item`

**Purpose:**
Represents an individual product line within a purchase order. Each row captures the product, the quantity ordered, and the unit cost at the time of purchase. This historical unit cost is stored separately from `product.purchase_price` to preserve accurate financial records even if prices change later.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `purchase_id` | `BIGINT` | NOT NULL | — | Foreign key → `purchase.id` |
| `product_id` | `BIGINT` | NOT NULL | — | Foreign key → `product.id` |
| `quantity` | `INTEGER` | NOT NULL | — | Quantity of product ordered |
| `unit_cost` | `NUMERIC(12, 2)` | NOT NULL | — | Cost per unit at time of purchase |
| `total_cost` | `NUMERIC(14, 2)` | NOT NULL | — | Computed: `quantity × unit_cost` |

**Primary Key:** `id`

**Foreign Keys:**
- `fk_purchase_item_purchase` → `purchase(id)` — CASCADE on delete
- `fk_purchase_item_product` → `product(id)` — RESTRICT on delete

**Unique Constraints:**
- `uq_purchase_item_purchase_product` on `(purchase_id, product_id)` — a product cannot appear twice in the same purchase

**Check Constraints:**
- `chk_purchase_item_quantity` — `quantity > 0`
- `chk_purchase_item_unit_cost` — `unit_cost >= 0`
- `chk_purchase_item_total_cost` — `total_cost >= 0`

**Indexes:**
- `idx_purchase_item_purchase_id` on `purchase_id`
- `idx_purchase_item_product_id` on `product_id`

**Business Rules:**
- `total_cost` is stored (a controlled departure from strict 3NF) to preserve historical accuracy and improve report performance
- The unit cost at the time of purchase is stored in this row regardless of subsequent price changes on the `product` table
- The application updates `product.purchase_price` to the latest `unit_cost` after a purchase is received

---

### 4.8 `bill`

**Purpose:**
Represents a sales bill (invoice) issued to a customer. The bill is the header record — it captures the customer, date, totals, discount, and status. Line items are stored in `bill_item`. Payment is tracked separately in the `payment` table.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `bill_number` | `VARCHAR(50)` | NOT NULL | — | Unique human-readable invoice code (e.g., 'INV-2026-0001') |
| `customer_id` | `BIGINT` | NOT NULL | — | Foreign key → `customer.id` |
| `bill_date` | `DATE` | NOT NULL | `CURRENT_DATE` | Date the bill was created |
| `subtotal` | `NUMERIC(14, 2)` | NOT NULL | `0.00` | Sum of all line item totals before discount |
| `discount_amount` | `NUMERIC(14, 2)` | NOT NULL | `0.00` | Total discount applied |
| `tax_amount` | `NUMERIC(14, 2)` | NOT NULL | `0.00` | Total tax applied |
| `total_amount` | `NUMERIC(14, 2)` | NOT NULL | `0.00` | Final payable amount: `subtotal - discount + tax` |
| `status` | `VARCHAR(20)` | NOT NULL | `'DRAFT'` | Bill lifecycle status |
| `invoice_sent` | `BOOLEAN` | NOT NULL | `FALSE` | Tracks whether invoice email was successfully sent to customer |
| `notes` | `TEXT` | NULL | — | Optional remarks |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**Primary Key:** `id`

**Foreign Keys:**
- `fk_bill_customer` → `customer(id)` — RESTRICT on delete

**Unique Constraints:**
- `uq_bill_bill_number` on `bill_number` — Bill/invoice numbers must be unique across the system

**Check Constraints:**
- `chk_bill_subtotal` — `subtotal >= 0`
- `chk_bill_discount_amount` — `discount_amount >= 0`
- `chk_bill_tax_amount` — `tax_amount >= 0`
- `chk_bill_total_amount` — `total_amount >= 0`
- `chk_bill_status` — `status IN ('DRAFT', 'CONFIRMED', 'PAID', 'CANCELLED')`

**Indexes:**
- Primary key index on `id` (automatic)
- Unique index on `bill_number` (from unique constraint)
- `idx_bill_customer_id` on `customer_id`
- `idx_bill_status` on `status`
- `idx_bill_bill_date` on `bill_date`
- `idx_bill_invoice_sent` on `invoice_sent`

**Bill Status Lifecycle:**

```
DRAFT → CONFIRMED → PAID
  └──→ CANCELLED
```

| Status | Meaning |
|---|---|
| `DRAFT` | Bill is being assembled; inventory is not yet deducted |
| `CONFIRMED` | Bill is finalised; inventory is deducted; awaiting payment |
| `PAID` | Payment has been received and linked |
| `CANCELLED` | Bill was cancelled; if confirmed, inventory is restocked |

**Business Rules:**
- `bill_number` is a unique human-readable invoice code generated when the bill is created (e.g., `INV-YYYY-XXXX`) and cannot be modified once set
- Inventory is deducted when the bill status changes from `DRAFT` to `CONFIRMED`
- A bill cannot be edited once it is `CONFIRMED` or `PAID`
- `total_amount = subtotal - discount_amount + tax_amount`
- A bill must have at least one `bill_item` before it can be confirmed
- `invoice_sent` defaults to `FALSE` and is set to `TRUE` by the `notification-service` once the invoice PDF has been successfully emailed to the customer

---

### 4.9 `bill_item`

**Purpose:**
Represents an individual product line within a bill. Captures the product, quantity sold, unit price at time of sale, and any discount applied to that specific line. Historical pricing is preserved independently of the product's current price.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `bill_id` | `BIGINT` | NOT NULL | — | Foreign key → `bill.id` |
| `product_id` | `BIGINT` | NOT NULL | — | Foreign key → `product.id` |
| `quantity` | `INTEGER` | NOT NULL | — | Quantity sold |
| `unit_price` | `NUMERIC(12, 2)` | NOT NULL | — | Selling price per unit at time of sale |
| `discount_percent` | `NUMERIC(5, 2)` | NOT NULL | `0.00` | Line-level discount percentage |
| `total_price` | `NUMERIC(14, 2)` | NOT NULL | — | `quantity × unit_price × (1 - discount_percent/100)` |

**Primary Key:** `id`

**Foreign Keys:**
- `fk_bill_item_bill` → `bill(id)` — CASCADE on delete
- `fk_bill_item_product` → `product(id)` — RESTRICT on delete

**Unique Constraints:**
- `uq_bill_item_bill_product` on `(bill_id, product_id)` — a product cannot appear twice in the same bill

**Check Constraints:**
- `chk_bill_item_quantity` — `quantity > 0`
- `chk_bill_item_unit_price` — `unit_price > 0`
- `chk_bill_item_discount_percent` — `discount_percent >= 0 AND discount_percent < 100`
- `chk_bill_item_total_price` — `total_price >= 0`

**Indexes:**
- `idx_bill_item_bill_id` on `bill_id`
- `idx_bill_item_product_id` on `product_id`

**Business Rules:**
- `unit_price` captures the selling price at the time the bill was created, not the current product price
- `total_price` is stored to preserve accuracy and avoid recalculating historical totals
- The quantity sold from `bill_item` is used to decrement `inventory.quantity_on_hand` when the bill is confirmed

---

### 4.10 `payment`

**Purpose:**
Records the payment transaction for a confirmed bill. Each bill has at most one payment record. Payment is a separate table from `bill` because they represent distinct business events — a bill is a commitment to pay, and a payment is the actual financial transaction.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `bill_id` | `BIGINT` | NOT NULL | — | Foreign key → `bill.id` (unique — 1:1) |
| `payment_date` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Date and time of payment |
| `amount_paid` | `NUMERIC(14, 2)` | NOT NULL | — | Amount received from customer |
| `payment_method` | `VARCHAR(50)` | NOT NULL | — | Method used for payment |
| `reference_number` | `VARCHAR(100)` | NULL | — | External reference (transaction ID, cheque number, etc.) |
| `status` | `VARCHAR(20)` | NOT NULL | `'COMPLETED'` | Payment status |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |

**Primary Key:** `id`

**Foreign Keys:**
- `fk_payment_bill` → `bill(id)` — RESTRICT on delete

**Unique Constraints:**
- `uq_payment_bill_id` on `bill_id` — enforces the one-to-one relationship

**Check Constraints:**
- `chk_payment_amount_paid` — `amount_paid > 0`
- `chk_payment_method` — `payment_method IN ('CASH', 'CARD', 'UPI', 'BANK_TRANSFER', 'CHEQUE')`
- `chk_payment_status` — `status IN ('COMPLETED', 'FAILED', 'REFUNDED')`

**Indexes:**
- Unique index on `bill_id` (from unique constraint)
- `idx_payment_payment_date` on `payment_date`
- `idx_payment_payment_method` on `payment_method`

**Business Rules:**
- A `payment` record is created by the `payment-service` after receiving a payment request from the frontend
- When a payment is created successfully, the `payment-service` publishes a `PaymentCompleted` event to RabbitMQ
- The `notification-service` consumes this event and emails the invoice to the customer
- `bill.status` is updated to `'PAID'` once the payment is persisted
- Only bills with `status = 'CONFIRMED'` are eligible for payment

---

### 4.11 `notification`

**Purpose:**
Persists all notification events that the `notification-service` processes. This includes both customer invoice emails (triggered by `PaymentCompleted`) and supplier low-stock alerts (triggered by `LowStockDetected`). The table also supports the supplier approval workflow — a `LowStockDetected` notification must be approved by the owner before the supplier email is dispatched.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `type` | `VARCHAR(50)` | NOT NULL | — | Notification type |
| `status` | `VARCHAR(20)` | NOT NULL | `'PENDING'` | Notification lifecycle status |
| `recipient_email` | `VARCHAR(255)` | NOT NULL | — | Target email address |
| `subject` | `VARCHAR(255)` | NOT NULL | — | Email subject line |
| `body` | `TEXT` | NOT NULL | — | Email body content (plain text or HTML) |
| `reference_id` | `BIGINT` | NULL | — | ID of the related business entity (bill_id or product_id) |
| `reference_type` | `VARCHAR(50)` | NULL | — | Type of entity being referenced |
| `error_message` | `TEXT` | NULL | — | Populated if sending fails |
| `sent_at` | `TIMESTAMPTZ` | NULL | — | Timestamp of successful email dispatch |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**`type` valid values:** `'INVOICE_EMAIL'`, `'LOW_STOCK_ALERT'`

**`status` valid values:**

| Status | Description |
|---|---|
| `PENDING` | Created from an event; not yet approved or sent |
| `APPROVED` | Owner approved a low-stock supplier email; queued for dispatch |
| `SENT` | Email successfully dispatched |
| `FAILED` | Email dispatch failed; `error_message` is populated |

**Primary Key:** `id`

**Foreign Keys:** None
- `reference_id` is an application-level soft reference, not a database foreign key. This preserves service independence and avoids cross-table constraints between tables owned by different services.

**Check Constraints:**
- `chk_notification_type` — `type IN ('INVOICE_EMAIL', 'LOW_STOCK_ALERT')`
- `chk_notification_status` — `status IN ('PENDING', 'APPROVED', 'SENT', 'FAILED')`

**Indexes:**
- `idx_notification_status` on `status`
- `idx_notification_type` on `type`
- `idx_notification_created_at` on `created_at`

**Business Rules:**
- `INVOICE_EMAIL` notifications are sent immediately without owner approval
- `LOW_STOCK_ALERT` notifications start as `PENDING` and require owner approval before sending
- If email dispatch fails, `status` is set to `'FAILED'` and `error_message` is populated
- `reference_id` and `reference_type` allow the UI to display contextual information

---

### 4.12 `settings`

**Purpose:**
Stores application-level configuration as key-value pairs. Allows runtime configuration of business rules (e.g., default tax rate, company name, currency) without requiring code changes or redeployment.

| Column | Data Type | Nullable | Default | Description |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Surrogate primary key |
| `setting_key` | `VARCHAR(100)` | NOT NULL | — | Unique configuration key |
| `setting_value` | `TEXT` | NOT NULL | — | Configuration value |
| `description` | `VARCHAR(255)` | NULL | — | Human-readable description of the setting |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Row creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | `CURRENT_TIMESTAMP` | Last update timestamp |

**Primary Key:** `id`

**Unique Constraints:**
- `uq_settings_setting_key` on `setting_key`

**Planned Initial Settings (Seed Data):**

| Key | Example Value | Description |
|---|---|---|
| `company_name` | `'My Business'` | Business name printed on invoices |
| `company_email` | `''` | Sender email on notifications |
| `company_phone` | `''` | Company phone number |
| `company_address` | `''` | Company address on invoices |
| `currency_symbol` | `'₹'` | Currency symbol used in UI |
| `currency_code` | `'INR'` | ISO 4217 currency code |
| `default_tax_rate` | `'18.00'` | Default GST/tax percentage |
| `low_stock_notification_enabled` | `'true'` | Toggle for low-stock alerts |

---

## 5. Relationship Explanations

### 5.1 `category` → `product` (One-to-Many)

Every product belongs to exactly one category. Categories provide the primary dimension for filtering, searching, and reporting on products. Without categories, the product list becomes a flat, unsearchable collection. It is One-to-Many (not Many-to-Many) because a product belonging to a single category is the correct simplification for this domain at this scope.

### 5.2 `product` → `inventory` (One-to-One)

Every product must have a corresponding inventory record. These are operationally distinct — stock levels change frequently (on every sale and purchase), while product definitions change rarely. A separate `inventory` table makes it possible to add warehouse-specific stock levels in a future version without restructuring `product`.

### 5.3 `supplier` → `purchase` (One-to-Many)

A supplier is the party from whom goods are purchased. Each purchase order must reference a supplier. A supplier can have many purchase orders over time.

### 5.4 `purchase` → `purchase_item` (One-to-Many)

A purchase order is a header document. The products being ordered are line items within that order. This is the standard Header-Detail pattern. Storing all items in a single row would violate 1NF.

### 5.5 `customer` → `bill` (One-to-Many)

A bill is always associated with a customer. A customer may make multiple purchases over time, each resulting in a separate bill. This relationship enables customer purchase history and outstanding bill tracking.

### 5.6 `bill` → `bill_item` (One-to-Many)

Identical rationale to `purchase` → `purchase_item`. A bill header captures the overall transaction while line items capture each individual product sold.

### 5.7 `bill` → `payment` (One-to-One)

A bill and its payment are two distinct events that occur at different times and are processed by different services. A bill is a sales commitment; a payment is the actual financial transaction. Keeping them separate allows the system to track unpaid bills. It is One-to-One in Version 1 because only full payment is supported. The constraint is enforced by `uq_payment_bill_id`.

### 5.8 `notification` (Standalone with Soft References)

The `notification` table uses a soft `reference_id` (no database-level foreign key) to preserve logical service boundaries. In Version 1 we share a database, but the design respects future service separation by avoiding cross-service foreign key constraints.

### 5.9 `settings` (Standalone)

Settings are application-wide configuration that do not relate to any specific business entity. No foreign key relationships by design.

---

## 6. Constraints

### 6.1 Primary Keys

Every table has a `BIGSERIAL` primary key named `id`.

| Table | Constraint Name |
|---|---|
| `category` | `pk_category` |
| `product` | `pk_product` |
| `inventory` | `pk_inventory` |
| `supplier` | `pk_supplier` |
| `customer` | `pk_customer` |
| `purchase` | `pk_purchase` |
| `purchase_item` | `pk_purchase_item` |
| `bill` | `pk_bill` |
| `bill_item` | `pk_bill_item` |
| `payment` | `pk_payment` |
| `notification` | `pk_notification` |
| `settings` | `pk_settings` |

### 6.2 Foreign Keys

| Constraint Name | On Table | Column | References | On Delete |
|---|---|---|---|---|
| `fk_product_category` | `product` | `category_id` | `category(id)` | RESTRICT |
| `fk_product_supplier` | `product` | `supplier_id` | `supplier(id)` | SET NULL |
| `fk_inventory_product` | `inventory` | `product_id` | `product(id)` | CASCADE |
| `fk_purchase_supplier` | `purchase` | `supplier_id` | `supplier(id)` | RESTRICT |
| `fk_purchase_item_purchase` | `purchase_item` | `purchase_id` | `purchase(id)` | CASCADE |
| `fk_purchase_item_product` | `purchase_item` | `product_id` | `product(id)` | RESTRICT |
| `fk_bill_customer` | `bill` | `customer_id` | `customer(id)` | RESTRICT |
| `fk_bill_item_bill` | `bill_item` | `bill_id` | `bill(id)` | CASCADE |
| `fk_bill_item_product` | `bill_item` | `product_id` | `product(id)` | RESTRICT |
| `fk_payment_bill` | `payment` | `bill_id` | `bill(id)` | RESTRICT |

### 6.3 Unique Constraints

| Constraint Name | Table | Column(s) |
|---|---|---|
| `uq_category_name` | `category` | `name` |
| `uq_product_sku` | `product` | `sku` |
| `uq_inventory_product_id` | `inventory` | `product_id` |
| `uq_supplier_email` | `supplier` | `email` |
| `uq_purchase_item_purchase_product` | `purchase_item` | `(purchase_id, product_id)` |
| `uq_bill_bill_number` | `bill` | `bill_number` |
| `uq_bill_item_bill_product` | `bill_item` | `(bill_id, product_id)` |
| `uq_payment_bill_id` | `payment` | `bill_id` |
| `uq_settings_setting_key` | `settings` | `setting_key` |

### 6.4 Check Constraints

| Constraint Name | Table | Rule |
|---|---|---|
| `chk_product_unit_price` | `product` | `unit_price > 0` |
| `chk_product_purchase_price` | `product` | `purchase_price >= 0` |
| `chk_product_status` | `product` | `status IN ('ACTIVE', 'INACTIVE')` |
| `chk_inventory_quantity_on_hand` | `inventory` | `quantity_on_hand >= 0` |
| `chk_inventory_reorder_level` | `inventory` | `reorder_level >= 0` |
| `chk_inventory_max_stock_level` | `inventory` | `max_stock_level > 0` |
| `chk_inventory_reorder_vs_max` | `inventory` | `reorder_level < max_stock_level` |
| `chk_inventory_version` | `inventory` | `version >= 0` |
| `chk_supplier_status` | `supplier` | `status IN ('ACTIVE', 'INACTIVE')` |
| `chk_customer_status` | `customer` | `status IN ('ACTIVE', 'INACTIVE')` |
| `chk_purchase_total_amount` | `purchase` | `total_amount >= 0` |
| `chk_purchase_status` | `purchase` | `status IN ('PENDING', 'RECEIVED', 'CANCELLED')` |
| `chk_purchase_item_quantity` | `purchase_item` | `quantity > 0` |
| `chk_purchase_item_unit_cost` | `purchase_item` | `unit_cost >= 0` |
| `chk_purchase_item_total_cost` | `purchase_item` | `total_cost >= 0` |
| `chk_bill_subtotal` | `bill` | `subtotal >= 0` |
| `chk_bill_discount_amount` | `bill` | `discount_amount >= 0` |
| `chk_bill_tax_amount` | `bill` | `tax_amount >= 0` |
| `chk_bill_total_amount` | `bill` | `total_amount >= 0` |
| `chk_bill_status` | `bill` | `status IN ('DRAFT', 'CONFIRMED', 'PAID', 'CANCELLED')` |
| `chk_bill_item_quantity` | `bill_item` | `quantity > 0` |
| `chk_bill_item_unit_price` | `bill_item` | `unit_price > 0` |
| `chk_bill_item_discount_percent` | `bill_item` | `discount_percent >= 0 AND discount_percent < 100` |
| `chk_bill_item_total_price` | `bill_item` | `total_price >= 0` |
| `chk_payment_amount_paid` | `payment` | `amount_paid > 0` |
| `chk_payment_method` | `payment` | `payment_method IN ('CASH', 'CARD', 'UPI', 'BANK_TRANSFER', 'CHEQUE')` |
| `chk_payment_status` | `payment` | `status IN ('COMPLETED', 'FAILED', 'REFUNDED')` |
| `chk_notification_type` | `notification` | `type IN ('INVOICE_EMAIL', 'LOW_STOCK_ALERT')` |
| `chk_notification_status` | `notification` | `status IN ('PENDING', 'APPROVED', 'SENT', 'FAILED')` |

### 6.5 Referential Integrity Policy

| ON DELETE Rule | Applied When |
|---|---|
| RESTRICT | The parent row has dependent children that must be preserved (e.g., a customer with bills, a product with purchase history) |
| CASCADE | The child rows have no independent meaning without the parent (e.g., `purchase_item` without a `purchase`, `bill_item` without a `bill`) |
| SET NULL | The child row can exist without the parent (e.g., a product can exist after its default supplier is removed) |

---

## 7. Index Strategy

### 7.1 Primary Key Indexes

PostgreSQL automatically creates a unique B-tree index for every primary key. No explicit definition needed.

### 7.2 Unique Constraint Indexes

PostgreSQL automatically creates a unique B-tree index for every UNIQUE constraint (`sku`, `bill_number`, `product_id` on inventory, `email` on supplier, `bill_id` on payment, `setting_key`, and composite keys). See Section 6.3.

### 7.3 Foreign Key Indexes

PostgreSQL does **not** automatically index foreign key columns. These explicit indexes are required to avoid sequential scans on JOIN operations:

| Index Name | Table | Column | Purpose |
|---|---|---|---|
| `idx_product_category_id` | `product` | `category_id` | JOIN product → category |
| `idx_purchase_supplier_id` | `purchase` | `supplier_id` | JOIN purchase → supplier |
| `idx_purchase_item_purchase_id` | `purchase_item` | `purchase_id` | JOIN purchase → purchase_item |
| `idx_purchase_item_product_id` | `purchase_item` | `product_id` | Purchase history by product |
| `idx_bill_customer_id` | `bill` | `customer_id` | Bills by customer |
| `idx_bill_item_bill_id` | `bill_item` | `bill_id` | JOIN bill → bill_item |
| `idx_bill_item_product_id` | `bill_item` | `product_id` | Sales history by product |

### 7.4 Search and Filter Indexes

| Index Name | Table | Column | Purpose |
|---|---|---|---|
| `idx_product_status` | `product` | `status` | Active product filter (most queries) |
| `idx_product_name` | `product` | `name` | Product search |
| `idx_supplier_name` | `supplier` | `name` | Supplier search |
| `idx_customer_name` | `customer` | `name` | Customer search |
| `idx_customer_email` | `customer` | `email` | Customer lookup by email |
| `idx_customer_phone` | `customer` | `phone` | Customer lookup by phone |
| `idx_purchase_status` | `purchase` | `status` | Filter pending/received purchases |
| `idx_bill_status` | `bill` | `status` | Filter unpaid/draft bills |
| `idx_bill_invoice_sent` | `bill` | `invoice_sent` | Filter bills pending invoice email delivery |
| `idx_notification_status` | `notification` | `status` | Pending notification dashboard |
| `idx_notification_type` | `notification` | `type` | Filter by notification type |

### 7.5 Date Range Indexes (Reporting)

| Index Name | Table | Column | Purpose |
|---|---|---|---|
| `idx_purchase_purchase_date` | `purchase` | `purchase_date` | Purchase reports by date range |
| `idx_bill_bill_date` | `bill` | `bill_date` | Sales reports by date range |
| `idx_payment_payment_date` | `payment` | `payment_date` | Revenue reports by date range |
| `idx_notification_created_at` | `notification` | `created_at` | Notification history |

### 7.6 Low-Stock Query Index

| Index Name | Table | Column | Purpose |
|---|---|---|---|
| `idx_inventory_quantity_on_hand` | `inventory` | `quantity_on_hand` | Low-stock detection query |

### 7.7 Performance Considerations

- All indexes defined are **B-tree indexes** (PostgreSQL default) — optimal for equality and range queries.
- **No speculative indexes** — every index here is justified by a specific, identified query pattern.
- **Partial indexes** are excluded from Version 1 but are a valid future optimisation (e.g., partial index on `bill(status)` for only active statuses).
- **Composite indexes** (e.g., `bill(customer_id, status)`) can be added after performance profiling in a later version.
- Each index consumes disk space and adds overhead to INSERT/UPDATE/DELETE operations. Index count is deliberately conservative.

---

## 8. Seed Data Strategy

### 8.1 Required Seed Data (Application Cannot Function Without)

**`settings` table:** All settings listed in Section 4.12 must be pre-populated before the application starts.

### 8.2 Recommended Development Seed Data

**`category` table:**

| Name | Description |
|---|---|
| Electronics | Electronic devices and accessories |
| Food & Beverages | Consumable food and drink products |
| Stationery | Office and writing supplies |
| Clothing | Garments and apparel |
| Household | Home and kitchen products |
| Tools & Hardware | Hand tools, power tools, and hardware |
| Health & Personal Care | Medicines, toiletries, and personal care items |
| Others | Uncategorised products |

**`supplier` table:** 2–3 sample suppliers with valid email addresses to allow purchase order creation.

**`customer` table:** 2–3 sample customers to allow bill creation during development.

**`product` table:** 5–10 sample products across different categories.

**`inventory` table:** Corresponding inventory records with non-zero `quantity_on_hand` so that sample sales can be processed.

### 8.3 Tables That Start Empty

`purchase`, `purchase_item`, `bill`, `bill_item`, `payment`, `notification` — transactional tables populated by normal application use.

### 8.4 Environment Strategy

| Environment | Seed Data |
|---|---|
| Local Development | All seed data including sample products, customers, and suppliers |
| Testing | Seed data created programmatically within test setup/teardown |
| Production | Settings and categories only; real business data entered through the UI |

---

## 9. Future Database Improvements

The following features are intentionally excluded from Version 1.

### 9.1 Soft Delete

Adding a `deleted_at TIMESTAMPTZ` column to every table so records are logically deleted rather than physically removed. Excluded from V1 because it adds complexity to every query and requires partial indexes on all tables.

### 9.2 Audit Tables

Separate `_audit` / `_history` tables logging every change with old value, new value, changed-by user, and timestamp — typically implemented via PostgreSQL triggers. Excluded because V1 has no authentication and therefore no "changed by" identity.

### 9.3 Multi-Tenancy

Adding an `organisation_id` column to every table to support multiple businesses in a single instance. Excluded from V1 — single-business system.

### 9.4 Separate Databases per Microservice

Giving each service its own isolated PostgreSQL database. Cross-service reads would go through API calls rather than SQL joins. Excluded from V1 to reduce infrastructure complexity.

### 9.5 Database Migration Tool (Flyway / Liquibase)

Versioned migration scripts to manage schema evolution across environments. Excluded from V1; manual SQL scripts are sufficient. Will be introduced when schema evolution across deployments becomes a concern.

### 9.6 Partial Payments

Allowing a bill to be paid in multiple instalments. Requires removing `uq_payment_bill_id` and adding payment balance tracking on `bill`. Excluded from V1.

### 9.7 Read Replicas and Caching

Redis caching for frequently read, rarely changed data (settings, categories, active product list). Database replication for high availability. Excluded from V1.

---

## 10. Design Decisions

### 10.1 Why a Single PostgreSQL Database?

All three microservices share one PostgreSQL database in Version 1.

**Reasons:**
- The primary goal of Version 1 is to demonstrate application architecture (microservices, event-driven communication, layered design), not database infrastructure.
- A shared database eliminates deployment complexity — no need to manage three separate PostgreSQL instances, connection pools, or cross-service distributed transactions during development.
- Each service is written to only read and write its own logical set of tables, making future database separation a configuration-level change rather than an architectural rewrite.

---

### 10.2 Why Third Normal Form (3NF)?

**Reasons:**
- 3NF eliminates all data redundancy caused by transitive dependencies. In an inventory and billing system, this is critical — price changes, supplier changes, and category renames should not require updates to historical records.
- 3NF is the standard for transactional (OLTP) databases — many small reads and writes, not large analytical queries.
- Maps cleanly to JPA entity relationships.

**Controlled denormalization:**
`purchase_item.total_cost` and `bill_item.total_price` are stored computed columns. This is a deliberate, documented departure from strict 3NF for two reasons: (1) historical accuracy — preserving the exact computed amount at the time of the transaction, and (2) reporting performance — avoiding recalculation across millions of rows.

---

### 10.3 Why a Separate `Notification` Table?

**Reasons:**
- The supplier approval workflow requires notifications to have a persistent lifecycle (PENDING → APPROVED → SENT). They cannot exist only in memory or message queue.
- Persisting notifications enables the Notifications module in the frontend UI.
- Failed email dispatches can be identified from the database and retried.
- Notification history provides an audit trail of when invoices were emailed and when supplier alerts were sent.

---

### 10.4 Why Separate `Bill` and `Payment` Tables?

**Reasons:**
- A bill and its payment are distinct business events that occur at different times and are processed by different services (`inventory-service` creates bills; `payment-service` creates payments).
- Separating them allows unpaid bills to exist without a payment record, which is essential for tracking outstanding balances.
- Payment details (method, reference number, payment timestamp) do not belong on the bill — this would violate the Single Responsibility Principle at the data layer.
- Future support for partial payments requires this separation.

---

### 10.5 Why a Separate `Inventory` Table?

**Reasons:**
- Product definition and stock levels have completely different ownership and change rates. Product details change rarely; stock levels change on every sale and purchase.
- A separate table allows inventory-specific columns (`reorder_level`, `max_stock_level`, `last_updated_at`) without cluttering product definition.
- Future warehouse or multi-location support would require multiple inventory records per product — impossible if quantity is a single column on `product`.

---

### 10.6 Why `BIGSERIAL` for Primary Keys?

**Reasons:**
- `BIGSERIAL` avoids the overflow risk of `SERIAL` (32-bit, max ~2.1 billion rows).
- Integer primary keys produce compact foreign key columns and efficient B-tree index lookups.
- Maps naturally to `Long` in Java/JPA with `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- UUIDs were considered and rejected: they produce larger indexes, fragmented B-tree pages, and are harder to read in development. The benefits of UUIDs (global uniqueness without coordination) are not needed in Version 1.

---

### 10.7 Why `VARCHAR` Status Columns Instead of PostgreSQL `ENUM`?

**Reasons:**
- Adding a new value to a PostgreSQL native ENUM requires an `ALTER TYPE` DDL statement, which is non-trivial to roll back.
- `VARCHAR` + `CHECK` constraint allows new values to be added by updating the check constraint — a simpler migration.
- String status values are directly readable in SQL queries, database dumps, logs, and API responses without requiring a type mapping layer.
- Spring Boot and JPA handle `String` enums (`@Enumerated(EnumType.STRING)`) more simply than PostgreSQL native enum types.

---

### 10.8 Why `TIMESTAMPTZ` for All Timestamp Fields?

**Reasons:**
- `TIMESTAMPTZ` stores the absolute UTC instant, eliminating ambiguity when servers, Docker containers, or client browsers operate across different timezones.
- Aligns directly with Java 8+ `java.time.Instant` and `java.time.OffsetDateTime` in JPA / Hibernate 6, preventing timezone shifts on database read/write.
- Ensures accurate time-series reporting and auditing without requiring manual timezone conversion logic in SQL.

---

### 10.9 Why Optimistic Locking (`version`) on `inventory`?

**Reasons:**
- In high-throughput billing and Point-of-Sale systems, two cashiers or API requests might attempt to sell the last unit of an item at the exact same moment.
- Without concurrency controls, both transactions read the old quantity and write conflicting values, leading to overselling or negative stock.
- The `version BIGINT NOT NULL DEFAULT 0` column allows Spring Data JPA's `@Version` mechanism to verify that the stock row was not modified by another transaction between read and write, throwing an `OptimisticLockException` if a conflict occurs.

---

### 10.10 Why Human-Readable `bill_number` and `invoice_sent` on `bill`?

**Reasons:**
- Commercial invoices require a distinct, sequential business identifier (e.g. `INV-2026-0001`) separate from internal database surrogate IDs (`id`). Customers and accounting tools reference this number.
- `invoice_sent BOOLEAN DEFAULT FALSE` decouples asynchronous email dispatch from synchronous payment processing. When `payment-service` commits a payment, it publishes `PaymentCompleted`. When `notification-service` finishes emailing the PDF invoice, it updates `invoice_sent = TRUE`, allowing the UI to clearly show whether the customer received their invoice or if re-sending is necessary.

---

*This document is the single source of truth for the database design.*
*No SQL scripts, JPA entities, or any implementation code should be written until this specification is reviewed and approved.*
*Author: Dharma S | Phase 2 — Database Design | Version 1.0*
