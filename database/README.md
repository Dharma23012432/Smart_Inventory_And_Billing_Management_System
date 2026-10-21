# database/

This directory contains all database-related artifacts for the Smart Inventory & Billing Management System.

## Contents (Phase 2)

| File | Description |
|---|---|
| `DATABASE_SPECIFICATION.md` | Source of truth specification for database schema & design |
| `schema.sql` | DDL script — 12 table definitions, constraints, indexes |
| `seed.sql` | Realistic development seed data (settings, categories, suppliers, customers, products, inventory) |

## Database Details

- **Engine:** PostgreSQL
- **Database name:** `smart_inventory_db`
- **Strategy:** Single shared database (Version 1)
- **DDL management:** Manual SQL scripts (no Flyway / Liquibase in Version 1)
- **JPA DDL Auto:** `validate` — Spring Boot validates entities against the existing schema; it does not auto-create tables.

## Setup (local)

```sql
CREATE DATABASE smart_inventory_db;
```

Then run `schema.sql` followed by `seed.sql`.
