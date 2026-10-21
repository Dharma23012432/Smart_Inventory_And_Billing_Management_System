# API_SPECIFICATION.md

**Project:** Smart Inventory & Billing Management System  
**Version:** 1.0  
**Author:** Dharma S  
**Phase:** 3 — API Specification  
**Status:** Complete — Ready for Review  

---

## Table of Contents

1. [API Overview](#1-api-overview)
2. [Common Standards](#2-common-standards)
3. [Module-wise API Specifications](#3-module-wise-api-specifications)
   - 3.1 [Category Module](#31-category-module)
   - 3.2 [Product Module](#32-product-module)
   - 3.3 [Inventory Module](#33-inventory-module)
   - 3.4 [Supplier Module](#34-supplier-module)
   - 3.5 [Customer Module](#35-customer-module)
   - 3.6 [Purchase Module (Procurement)](#36-purchase-module-procurement)
   - 3.7 [Sales & Billing Module](#37-sales--billing-module)
   - 3.8 [Payment Module](#38-payment-module)
   - 3.9 [Notification Module](#39-notification-module)
   - 3.10 [Dashboard Module](#310-dashboard-module)
   - 3.11 [Reports Module](#311-reports-module)
   - 3.12 [Settings Module](#312-settings-module)
4. [CRUD Endpoints Matrix](#4-crud-endpoints-matrix)
5. [Sales & Invoicing Workflow](#5-sales--invoicing-workflow)
6. [Purchase & Stock Ingestion Workflow](#6-purchase--stock-ingestion-workflow)
7. [RabbitMQ Event Contracts](#7-rabbitmq-event-contracts)
8. [Validation Rules Reference](#8-validation-rules-reference)
9. [Error Handling & Status Codes](#9-error-handling--status-codes)
10. [Future APIs (Version 2)](#10-future-apis-version-2)

---

## 1. API Overview

### 1.1 Architecture & Design Principles

The Smart Inventory & Billing Management System exposes RESTful web APIs across three microservices. The APIs strictly adhere to:
- **Stateless HTTP**: Every request contains all necessary context; no session state is maintained on servers.
- **REST Resource Orientation**: URIs identify resources using plural nouns (e.g., `/api/v1/products`, `/api/v1/bills`).
- **Standard HTTP Methods**:
  - `GET` — Safe and idempotent retrieval of resources.
  - `POST` — Non-idempotent creation of new resources or triggering domain workflows.
  - `PUT` — Idempotent update or state transition of an existing resource.
  - `DELETE` — Idempotent removal or deactivation of an existing resource.
- **Microservice Port Mapping**:
  - `inventory-service` (Port **8080**): Catalog, Inventory, Customers, Suppliers, Purchases, Sales/Billing, Reports, Dashboard, Settings.
  - `payment-service` (Port **8081**): Payment capture, validation, and settlement events.
  - `notification-service` (Port **8082**): Email dispatch audit, approval queues, and notification logs.
- **Frontend Reverse Proxy**:
  The React/Vite development server (Port **3000**) proxies `/api` requests to `http://localhost:8080`. Payments and notifications route via port 8080 (or dedicated gateway routing in containerized deployments).

### 1.2 Base URLs & Versioning

- Base URL for `inventory-service`: `http://localhost:8080/api/v1`
- Base URL for `payment-service`: `http://localhost:8081/api/v1`
- Base URL for `notification-service`: `http://localhost:8082/api/v1`
- **Versioning Strategy**: All REST endpoints across all microservices are prefixed with `/api/v1/{resource}` for consistency and forward compatibility.

---

## 2. Common Standards

### 2.1 Standard Success Envelope

All successful responses return HTTP `200 OK` (or `201 Created`) with a uniform JSON envelope:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-10-19T10:30:00Z"
}
```

### 2.2 Standard Error Envelope

When an error occurs, the server responds with an appropriate HTTP error status and a uniform error payload:

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Product with ID 42 was not found",
    "details": []
  },
  "timestamp": "2026-10-19T10:30:00Z",
  "path": "/api/v1/products/42"
}
```

For bean validation failures (`400 Bad Request`), `details` contains field-specific error messages:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Input validation failed for 2 fields",
    "details": [
      {
        "field": "unitPrice",
        "rejectedValue": -10.0,
        "message": "Unit price must be greater than zero"
      },
      {
        "field": "sku",
        "rejectedValue": "",
        "message": "SKU cannot be blank"
      }
    ]
  },
  "timestamp": "2026-10-19T10:30:00Z",
  "path": "/api/v1/products"
}
```

### 2.3 Pagination, Sorting & Filtering Format

Paginated endpoints accept standard query parameters:
- `page` — 0-indexed page number (Default: `0`).
- `size` — Number of items per page (Default: `10`, Max: `100`).
- `sortBy` — Field name to sort by (Default: `id`).
- `direction` — Sort order: `ASC` or `DESC` (Default: `DESC`).

Paginated response data structure:

```json
{
  "success": true,
  "message": "Page retrieved successfully",
  "data": {
    "content": [ ... ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 48,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "timestamp": "2026-10-19T10:30:00Z"
}
```

### 2.4 Date & Currency Formatting

- **Timestamps**: ISO-8601 UTC string format: `YYYY-MM-DDTHH:mm:ssZ` (e.g., `2026-10-19T10:30:00Z`).
- **Dates**: Calendar date format: `YYYY-MM-DD` (e.g., `2026-10-19`).
- **Currency & Decimals**: All monetary fields are serialized as JSON numbers with 2 decimal places (e.g., `899.00`). Handled in Java as `java.math.BigDecimal` to eliminate floating-point rounding errors.

---

## 3. Module-wise API Specifications

---

### 3.1 Category Module

Service: `inventory-service` | Base Path: `/api/v1/categories`

#### 3.1.1 Get All Categories
- **Purpose**: Retrieve a list of all product categories.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/categories`
- **Query Parameters**: None
- **Request Body**: None
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Categories retrieved successfully",
    "data": [
      {
        "id": 1,
        "name": "Electronics",
        "description": "Electronic devices and computer peripherals",
        "createdAt": "2026-10-19T09:00:00Z",
        "updatedAt": "2026-10-19T09:00:00Z"
      }
    ],
    "timestamp": "2026-10-19T10:00:00Z"
  }
  ```
- **Error Responses**: `500 Internal Server Error`
- **Database Tables**: `category`
- **RabbitMQ Event**: None

#### 3.1.2 Get Category by ID
- **Purpose**: Retrieve category details by primary key.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/categories/{id}`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK`
- **Error Responses**: `404 Not Found` (Category does not exist)
- **Database Tables**: `category`

#### 3.1.3 Create Category
- **Purpose**: Create a new product category.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/categories`
- **Request Body**:
  ```json
  {
    "name": "Home Appliances",
    "description": "Kitchen and household electrical appliances"
  }
  ```
- **Validation Rules**:
  - `name`: `@NotBlank`, max 100 characters, unique across database.
  - `description`: Optional, max 255 characters.
- **Success Response**: `201 Created`
- **Error Responses**: `400 Bad Request` (Validation error), `409 Conflict` (`uq_category_name` violation)
- **Database Tables**: `category`

#### 3.1.4 Update Category
- **Purpose**: Update an existing category name or description.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/categories/{id}`
- **Path Variables**: `id` (Long, Required)
- **Request Body**: Same as Create Category.
- **Success Response**: `200 OK`
- **Error Responses**: `400 Bad Request`, `404 Not Found`, `409 Conflict` (Name in use by another category)
- **Database Tables**: `category`

#### 3.1.5 Delete Category
- **Purpose**: Delete a category if no products reference it.
- **HTTP Method**: `DELETE`
- **Endpoint**: `/api/v1/categories/{id}`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Category deleted successfully",
    "data": null,
    "timestamp": "2026-10-19T10:00:00Z"
  }
  ```
- **Error Responses**: `404 Not Found`, `409 Conflict` (Cannot delete category with associated products — enforced by FK `RESTRICT`)
- **Database Tables**: `category`, `product`

---

### 3.2 Product Module

Service: `inventory-service` | Base Path: `/api/v1/products`

#### 3.2.1 Get Products (Paginated & Filtered)
- **Purpose**: Search, filter, and page through products with stock quantity information.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/products`
- **Query Parameters**:
  - `search` (String, Optional): Searches product name and SKU.
  - `categoryId` (Long, Optional): Filters by category.
  - `status` (String, Optional): `ACTIVE` or `INACTIVE` (Default: `ACTIVE`).
  - `page` (Integer, Optional, Default: `0`).
  - `size` (Integer, Optional, Default: `10`).
  - `sortBy` (String, Optional, Default: `name`).
  - `direction` (String, Optional, Default: `ASC`).
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Products retrieved successfully",
    "data": {
      "content": [
        {
          "id": 1,
          "name": "Logitech Wireless Mouse M220 Silent",
          "sku": "ELEC-LOG-M220",
          "description": "Compact 2.4GHz wireless optical mouse",
          "unitPrice": 899.00,
          "purchasePrice": 620.00,
          "unitOfMeasure": "PCS",
          "status": "ACTIVE",
          "category": {
            "id": 1,
            "name": "Electronics"
          },
          "supplier": {
            "id": 1,
            "name": "TechSource Distribution Ltd"
          },
          "inventory": {
            "quantityOnHand": 35,
            "reorderLevel": 10,
            "maxStockLevel": 100,
            "isLowStock": false
          },
          "createdAt": "2026-10-19T09:00:00Z",
          "updatedAt": "2026-10-19T09:00:00Z"
        }
      ],
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 6,
      "totalPages": 1,
      "first": true,
      "last": true
    },
    "timestamp": "2026-10-19T10:00:00Z"
  }
  ```

#### 3.2.2 Get Product by ID
- **Purpose**: Retrieve single product details including stock and supplier info.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/products/{id}`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK` (Single product JSON)
- **Error Responses**: `404 Not Found`

#### 3.2.3 Get Product by SKU
- **Purpose**: Fast lookup for barcode scanners and POS entry.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/products/sku/{sku}`
- **Path Variables**: `sku` (String, Required)
- **Success Response**: `200 OK`
- **Error Responses**: `404 Not Found`

#### 3.2.4 Create Product
- **Purpose**: Create a product and automatically initialize its 1:1 inventory record.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/products`
- **Request Body**:
  ```json
  {
    "categoryId": 1,
    "supplierId": 1,
    "name": "Dell Wireless Keyboard KB216",
    "sku": "ELEC-DEL-KB216",
    "description": "Full-sized chiclet multimedia keyboard",
    "unitPrice": 999.00,
    "purchasePrice": 720.00,
    "unitOfMeasure": "PCS",
    "initialStock": 25,
    "reorderLevel": 8,
    "maxStockLevel": 100
  }
  ```
- **Validation Rules**:
  - `categoryId`: `@NotNull`, must reference an existing category.
  - `supplierId`: Optional; if provided, must reference an existing supplier.
  - `name`: `@NotBlank`, max 200 characters.
  - `sku`: `@NotBlank`, max 100 characters, globally unique.
  - `unitPrice`: `@NotNull`, `unitPrice > 0`.
  - `purchasePrice`: `@NotNull`, `purchasePrice >= 0`.
  - `unitOfMeasure`: `@NotBlank` (e.g., `'PCS'`, `'KG'`, `'BOX'`).
  - `initialStock`: `@Min(0)`, defaults to 0 if omitted.
  - `reorderLevel`: `@Min(0)`, must be `< maxStockLevel`.
  - `maxStockLevel`: `@Min(1)`.
- **Success Response**: `201 Created`
- **Business Rules**:
  - Creates row in `product` table.
  - Simultaneously inserts corresponding row in `inventory` table with `version = 0`.
- **Database Tables**: `product`, `inventory`, `category`, `supplier`

#### 3.2.5 Update Product
- **Purpose**: Update product attributes, description, or selling price.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/products/{id}`
- **Path Variables**: `id` (Long, Required)
- **Request Body**: Same fields as Create Product (excluding `initialStock`).
- **Success Response**: `200 OK`
- **Error Responses**: `400 Bad Request`, `404 Not Found`, `409 Conflict` (Duplicate SKU)
- **Database Tables**: `product`

#### 3.2.6 Delete / Deactivate Product
- **Purpose**: Deactivate a product (`status = 'INACTIVE'`).
- **HTTP Method**: `DELETE`
- **Endpoint**: `/api/v1/products/{id}`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK`
- **Business Rules**:
  - If product has historical records in `bill_item` or `purchase_item`, it is soft-deactivated to `INACTIVE`.
  - If no transactions exist, it can be purged along with its inventory record.
- **Database Tables**: `product`, `bill_item`, `purchase_item`, `inventory`

---

### 3.3 Inventory Module

Service: `inventory-service` | Base Path: `/api/v1/inventory`

#### 3.3.1 Get Inventory List
- **Purpose**: View current stock status across all products.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/inventory`
- **Query Parameters**:
  - `lowStockOnly` (Boolean, Optional, Default: `false`): Filters `quantityOnHand <= reorderLevel`.
  - `page` (Integer, Default: `0`), `size` (Integer, Default: `10`).
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Inventory retrieved successfully",
    "data": {
      "content": [
        {
          "inventoryId": 4,
          "productId": 4,
          "productName": "Pilot G2 0.7mm Retractable Gel Pen (Blue)",
          "sku": "STAT-PIL-G2",
          "quantityOnHand": 8,
          "reorderLevel": 12,
          "maxStockLevel": 100,
          "isLowStock": true,
          "version": 0,
          "lastUpdatedAt": "2026-10-19T09:00:00Z"
        }
      ]
    }
  }
  ```

#### 3.3.2 Get Inventory by Product ID
- **Purpose**: Check stock availability for a specific item.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/inventory/product/{productId}`
- **Path Variables**: `productId` (Long, Required)
- **Success Response**: `200 OK`
- **Error Responses**: `404 Not Found`

#### 3.3.3 Update Reorder Settings
- **Purpose**: Adjust reorder threshold and warehouse maximum capacity.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/inventory/product/{productId}/settings`
- **Request Body**:
  ```json
  {
    "reorderLevel": 15,
    "maxStockLevel": 120
  }
  ```
- **Validation Rules**: `reorderLevel >= 0`, `maxStockLevel > reorderLevel`.
- **Success Response**: `200 OK`
- **Database Tables**: `inventory`

#### 3.3.4 Manual Stock Adjustment
- **Purpose**: Stock take corrections (damage, shrinkage, count discrepancies).
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/inventory/product/{productId}/adjust`
- **Request Body**:
  ```json
  {
    "adjustedQuantity": 30,
    "reason": "Physical audit reconciliation"
  }
  ```
- **Validation Rules**: `adjustedQuantity >= 0`, `reason` is `@NotBlank`.
- **Business Rules**:
  - Updates `quantity_on_hand` and increments `version` using optimistic locking.
  - If new quantity `<= reorder_level`, triggers `LowStockDetected` event.
- **RabbitMQ Event**: `LowStockDetected` (if new quantity `<= reorder_level`).
- **Database Tables**: `inventory`

---

### 3.4 Supplier Module

Service: `inventory-service` | Base Path: `/api/v1/suppliers`

#### 3.4.1 Get Suppliers
- **Purpose**: Retrieve list of suppliers.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/suppliers`
- **Query Parameters**: `search` (String, Optional), `status` (String, Optional, Default: `ACTIVE`).
- **Success Response**: `200 OK` (Array or Page of suppliers)

#### 3.4.2 Get Supplier by ID
- **Purpose**: Get supplier detail with contact person, email, and phone.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/suppliers/{id}`
- **Success Response**: `200 OK`
- **Error Responses**: `404 Not Found`

#### 3.4.3 Create Supplier
- **Purpose**: Register a new vendor.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/suppliers`
- **Request Body**:
  ```json
  {
    "name": "Zenith Industrial Supplies",
    "contactPerson": "Ramesh Nair",
    "email": "orders@zenithsupplies.com",
    "phone": "+91 98450 99887",
    "address": "45 Industrial Layout, Hosur Road, Bangalore"
  }
  ```
- **Validation Rules**:
  - `name`: `@NotBlank`, max 200 characters.
  - `email`: `@NotBlank`, `@Email`, unique in database.
- **Success Response**: `201 Created`
- **Error Responses**: `400 Bad Request`, `409 Conflict` (Duplicate email)
- **Database Tables**: `supplier`

#### 3.4.4 Update Supplier
- **Purpose**: Update contact info or status.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/suppliers/{id}`
- **Success Response**: `200 OK`
- **Database Tables**: `supplier`

#### 3.4.5 Delete Supplier
- **Purpose**: Remove a supplier if no purchase history exists.
- **HTTP Method**: `DELETE`
- **Endpoint**: `/api/v1/suppliers/{id}`
- **Error Responses**: `409 Conflict` (Cannot delete supplier with purchases — FK `RESTRICT`)
- **Database Tables**: `supplier`, `purchase`

---

### 3.5 Customer Module

Service: `inventory-service` | Base Path: `/api/v1/customers`

#### 3.5.1 Get Customers
- **Purpose**: Search customers by name, phone, or email.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/customers`
- **Query Parameters**: `search` (String, Optional), `page` (Integer), `size` (Integer).
- **Success Response**: `200 OK`

#### 3.5.2 Get Customer by ID
- **Purpose**: Get customer profile and invoice delivery address.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/customers/{id}`
- **Success Response**: `200 OK`
- **Error Responses**: `404 Not Found`

#### 3.5.3 Create Customer
- **Purpose**: Register a new customer.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/customers`
- **Request Body**:
  ```json
  {
    "name": "Kavita Rao",
    "email": "kavita.rao@example.com",
    "phone": "+91 98765 12345",
    "address": "12 Residency Road, Bangalore, KA"
  }
  ```
- **Validation Rules**:
  - `name`: `@NotBlank`, max 200 characters.
  - `email`: Optional, valid email syntax if provided.
- **Success Response**: `201 Created`
- **Database Tables**: `customer`

#### 3.5.4 Update Customer
- **Purpose**: Modify customer contact information.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/customers/{id}`
- **Success Response**: `200 OK`
- **Database Tables**: `customer`

#### 3.5.5 Delete Customer
- **Purpose**: Delete customer record if no sales bills exist.
- **HTTP Method**: `DELETE`
- **Endpoint**: `/api/v1/customers/{id}`
- **Error Responses**: `409 Conflict` (Customer has sales history — FK `RESTRICT`)
- **Database Tables**: `customer`, `bill`

---

### 3.6 Purchase Module (Procurement)

Service: `inventory-service` | Base Path: `/api/v1/purchases`

#### 3.6.1 Get Purchases (List & Filter)
- **Purpose**: View purchase order ledger.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/purchases`
- **Query Parameters**:
  - `supplierId` (Long, Optional)
  - `status` (String, Optional: `PENDING`, `RECEIVED`, `CANCELLED`)
  - `startDate`, `endDate` (Date, Optional: `YYYY-MM-DD`)
  - `page`, `size`
- **Success Response**: `200 OK`

#### 3.6.2 Get Purchase Order by ID
- **Purpose**: View purchase header and all item line details.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/purchases/{id}`
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Purchase order retrieved",
    "data": {
      "id": 1,
      "supplier": {
        "id": 1,
        "name": "TechSource Distribution Ltd"
      },
      "purchaseDate": "2026-10-19",
      "totalAmount": 12400.00,
      "status": "PENDING",
      "notes": "Urgent stock replenishment",
      "items": [
        {
          "id": 1,
          "productId": 1,
          "productName": "Logitech Wireless Mouse M220 Silent",
          "sku": "ELEC-LOG-M220",
          "quantity": 20,
          "unitCost": 620.00,
          "totalCost": 12400.00
        }
      ],
      "createdAt": "2026-10-19T09:00:00Z"
    }
  }
  ```

#### 3.6.3 Create Purchase Order
- **Purpose**: Place an order with a supplier in `PENDING` status.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/purchases`
- **Request Body**:
  ```json
  {
    "supplierId": 1,
    "purchaseDate": "2026-10-19",
    "notes": "Bulk stationery order",
    "items": [
      {
        "productId": 3,
        "quantity": 50,
        "unitCost": 240.00
      },
      {
        "productId": 4,
        "quantity": 100,
        "unitCost": 42.00
      }
    ]
  }
  ```
- **Validation Rules**:
  - `supplierId`: `@NotNull`, must exist and be `ACTIVE`.
  - `items`: `@NotEmpty`, min 1 item.
  - For each item: `productId` must exist, `quantity > 0`, `unitCost >= 0`.
  - Same product cannot appear twice in request (`uq_purchase_item_purchase_product`).
- **Success Response**: `201 Created`
- **Business Rules**:
  - Server computes `total_cost = quantity * unit_cost` per item.
  - Server computes `total_amount = sum(total_cost)`.
  - Sets `status = 'PENDING'`. Inventory is NOT yet modified.
- **Database Tables**: `purchase`, `purchase_item`, `supplier`, `product`

#### 3.6.4 Receive Purchase Order (Stock Ingestion)
- **Purpose**: Mark goods as received, incrementing inventory stock and updating cost price.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/purchases/{id}/receive`
- **Path Variables**: `id` (Long, Required)
- **Request Body**: None
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Purchase order received and inventory updated successfully",
    "data": {
      "id": 1,
      "status": "RECEIVED",
      "receivedAt": "2026-10-19T10:15:00Z"
    }
  }
  ```
- **Business Rules**:
  - Validates purchase `status == 'PENDING'`. (Fails with `400` if already `RECEIVED` or `CANCELLED`).
  - Iterates line items: increments `inventory.quantity_on_hand += item.quantity`.
  - Updates `product.purchase_price = item.unit_cost` to maintain current cost.
  - Updates purchase `status = 'RECEIVED'`.
  - All operations wrapped in a single database transaction.
- **Database Tables**: `purchase`, `purchase_item`, `inventory`, `product`

#### 3.6.5 Cancel Purchase Order
- **Purpose**: Cancel an unreceived purchase order.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/purchases/{id}/cancel`
- **Success Response**: `200 OK`
- **Business Rules**: Can only cancel if `status == 'PENDING'`.
- **Database Tables**: `purchase`

---

### 3.7 Sales & Billing Module

Service: `inventory-service` | Base Path: `/api/v1/bills`

#### 3.7.1 Get Bills (List & Filter)
- **Purpose**: Query sales transactions and invoice history.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/bills`
- **Query Parameters**:
  - `customerId` (Long, Optional)
  - `status` (String, Optional: `DRAFT`, `CONFIRMED`, `PAID`, `CANCELLED`)
  - `startDate`, `endDate` (Date, Optional)
  - `invoiceSent` (Boolean, Optional)
  - `page`, `size`
- **Success Response**: `200 OK`

#### 3.7.2 Get Bill by ID
- **Purpose**: Retrieve full bill breakdown including items, taxes, discounts, and payment status.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/bills/{id}`
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Bill retrieved successfully",
    "data": {
      "id": 101,
      "billNumber": "INV-2026-0001",
      "customer": {
        "id": 2,
        "name": "Priya Sharma",
        "email": "priya.sharma@example.com"
      },
      "billDate": "2026-10-19",
      "subtotal": 1798.00,
      "discountAmount": 100.00,
      "taxAmount": 305.64,
      "totalAmount": 2003.64,
      "status": "CONFIRMED",
      "invoiceSent": false,
      "notes": "Counter sale",
      "items": [
        {
          "id": 201,
          "productId": 1,
          "productName": "Logitech Wireless Mouse M220 Silent",
          "sku": "ELEC-LOG-M220",
          "quantity": 2,
          "unitPrice": 899.00,
          "discountPercent": 0.00,
          "totalPrice": 1798.00
        }
      ],
      "payment": null,
      "createdAt": "2026-10-19T09:30:00Z"
    }
  }
  ```

#### 3.7.3 Get Bill by Bill Number
- **Purpose**: Lookup invoice by customer-facing invoice code.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/bills/number/{billNumber}`
- **Path Variables**: `billNumber` (String, Required, e.g., `INV-2026-0001`)
- **Success Response**: `200 OK`

#### 3.7.4 Create Bill (Draft)
- **Purpose**: Initialize a customer bill with line items in `DRAFT` status.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/bills`
- **Request Body**:
  ```json
  {
    "customerId": 2,
    "discountAmount": 100.00,
    "taxPercent": 18.00,
    "notes": "Counter checkout",
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "discountPercent": 0.00
      }
    ]
  }
  ```
- **Validation Rules**:
  - `customerId`: `@NotNull`, customer must exist and be `ACTIVE`.
  - `items`: `@NotEmpty`, min 1 item.
  - `quantity > 0` for all items.
  - `discountAmount >= 0`.
- **Business Rules**:
  - Generates unique sequential `bill_number` (format: `INV-YYYY-XXXX`).
  - Fetches active selling price from `product.unit_price` at moment of billing.
  - Computes `item.total_price = quantity * unit_price * (1 - discountPercent/100)`.
  - Computes `subtotal = sum(items.total_price)`.
  - Computes `tax_amount = (subtotal - discount_amount) * (taxPercent / 100)`.
  - Computes `total_amount = subtotal - discount_amount + tax_amount`.
  - Saves in `DRAFT` status. Inventory is NOT deducted in DRAFT status.
- **Success Response**: `201 Created`
- **Database Tables**: `bill`, `bill_item`, `customer`, `product`

#### 3.7.5 Confirm Bill (Checkout & Stock Deduction)
- **Purpose**: Finalize bill, verify stock, deduct inventory, and evaluate low-stock alert triggers.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/bills/{id}/confirm`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Bill confirmed and inventory deducted successfully",
    "data": {
      "id": 101,
      "billNumber": "INV-2026-0001",
      "status": "CONFIRMED",
      "totalAmount": 2003.64
    }
  }
  ```
- **Business Rules**:
  - Bill must currently be in `DRAFT` status.
  - For each line item: checks `inventory.quantity_on_hand >= item.quantity`.
  - If insufficient stock on any item: rolls back transaction and returns `400 Bad Request` ("Insufficient stock for product X. Available: Y, Requested: Z").
  - Deducts `inventory.quantity_on_hand -= item.quantity` using optimistic locking (`version`).
  - If new `quantity_on_hand <= inventory.reorder_level`: publishes `LowStockDetected` event to RabbitMQ.
  - Updates bill status to `CONFIRMED`. Bill is now locked against item modifications.
- **RabbitMQ Event**: `LowStockDetected` (if any item's stock drops to or below its reorder level).
- **Database Tables**: `bill`, `bill_item`, `inventory`

#### 3.7.6 Cancel Bill
- **Purpose**: Cancel a bill. If previously `CONFIRMED`, restocks inventory.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/bills/{id}/cancel`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK`
- **Business Rules**:
  - Bills in `PAID` status cannot be cancelled in V1 (requires formal refund process).
  - If `CONFIRMED`, restocks inventory by adding back each item quantity.
  - Updates bill status to `CANCELLED`.
- **Database Tables**: `bill`, `bill_item`, `inventory`

---

### 3.8 Payment Module

Service: `payment-service` | Port: **8081** | Base Path: `/api/v1/payments`

#### 3.8.1 Process Payment
- **Purpose**: Process customer payment for a confirmed bill and trigger asynchronous invoice email.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/payments`
- **Request Body**:
  ```json
  {
    "billId": 101,
    "amountPaid": 2003.64,
    "paymentMethod": "UPI",
    "referenceNumber": "UPI-IND-9876543210"
  }
  ```
- **Validation Rules**:
  - `billId`: `@NotNull`. Bill must exist and have `status = 'CONFIRMED'`.
  - `amountPaid`: `@NotNull`, must exactly match `bill.total_amount`.
  - `paymentMethod`: `@NotBlank`, must be one of: `'CASH'`, `'CARD'`, `'UPI'`, `'BANK_TRANSFER'`, `'CHEQUE'`.
- **Success Response**: `201 Created`
  ```json
  {
    "success": true,
    "message": "Payment processed successfully",
    "data": {
      "paymentId": 501,
      "billId": 101,
      "billNumber": "INV-2026-0001",
      "amountPaid": 2003.64,
      "paymentMethod": "UPI",
      "referenceNumber": "UPI-IND-9876543210",
      "status": "COMPLETED",
      "paymentDate": "2026-10-19T10:35:00Z"
    },
    "timestamp": "2026-10-19T10:35:00Z"
  }
  ```
- **Error Responses**:
  - `400 Bad Request` (Bill not in `CONFIRMED` status, or amount mismatch).
  - `404 Not Found` (Bill not found).
  - `409 Conflict` (Bill already paid; `uq_payment_bill_id` violation).
- **Business Rules**:
  - Verifies bill status is `CONFIRMED`.
  - Persists payment in `payment` table.
  - Updates `bill.status = 'PAID'`.
  - Publishes `PaymentCompleted` message to RabbitMQ exchange `inventory.exchange` with routing key `payment.completed`.
- **RabbitMQ Event**: `PaymentCompleted`
- **Database Tables**: `payment`, `bill`

#### 3.8.2 Get Payment by Bill ID
- **Purpose**: Retrieve the payment receipt associated with a bill.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/payments/bill/{billId}`
- **Path Variables**: `billId` (Long, Required)
- **Success Response**: `200 OK`
- **Error Responses**: `404 Not Found`

---

### 3.9 Notification Module

Service: `notification-service` | Port: **8082** | Base Path: `/api/v1/notifications`

#### 3.9.1 Get Notifications (Audit Log)
- **Purpose**: View history of sent and pending notifications.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/notifications`
- **Query Parameters**:
  - `status` (String, Optional: `PENDING`, `APPROVED`, `SENT`, `FAILED`)
  - `type` (String, Optional: `INVOICE_EMAIL`, `LOW_STOCK_ALERT`)
  - `page`, `size`
- **Success Response**: `200 OK`

#### 3.9.2 Get Notification by ID
- **Purpose**: View email body, subject, recipient, and delivery error messages.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/notifications/{id}`
- **Success Response**: `200 OK`

#### 3.9.3 Approve Low Stock Alert (Owner Workflow)
- **Purpose**: Owner approves a pending low-stock notification to dispatch an official purchase request email to the supplier.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/notifications/{id}/approve`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Notification approved and dispatched to supplier",
    "data": {
      "id": 12,
      "status": "SENT",
      "recipientEmail": "orders@techsource.com",
      "sentAt": "2026-10-19T10:40:00Z"
    }
  }
  ```
- **Business Rules**:
  - Notification must have `type = 'LOW_STOCK_ALERT'` and `status = 'PENDING'`.
  - Sets `status = 'APPROVED'`.
  - Triggers JavaMailSender dispatch to supplier email.
  - If dispatch succeeds: updates `status = 'SENT'`, `sent_at = CURRENT_TIMESTAMP`.
  - If dispatch fails: updates `status = 'FAILED'`, populates `error_message`.
- **Database Tables**: `notification`

#### 3.9.4 Retry Failed Notification
- **Purpose**: Re-attempt email dispatch for a failed delivery.
- **HTTP Method**: `POST`
- **Endpoint**: `/api/v1/notifications/{id}/retry`
- **Path Variables**: `id` (Long, Required)
- **Success Response**: `200 OK`
- **Database Tables**: `notification`

---

### 3.10 Dashboard Module

Service: `inventory-service` | Base Path: `/api/v1/dashboard`

#### 3.10.1 Get Dashboard Summary KPIs
- **Purpose**: Provide high-level business indicators for the main landing page.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/dashboard/summary`
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Dashboard summary retrieved",
    "data": {
      "todaySales": 14250.00,
      "monthlySales": 348200.00,
      "totalBillsToday": 18,
      "lowStockItemCount": 3,
      "pendingPurchaseOrders": 2,
      "totalActiveProducts": 142,
      "totalCustomers": 89
    },
    "timestamp": "2026-10-19T10:45:00Z"
  }
  ```

#### 3.10.2 Get Recent Sales Activity
- **Purpose**: Display last 5-10 transactions on dashboard table.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/dashboard/recent-sales`
- **Query Parameters**: `limit` (Integer, Default: `5`)
- **Success Response**: `200 OK`

#### 3.10.3 Get Low Stock Alert Items
- **Purpose**: Highlight items needing immediate procurement on dashboard alert widget.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/dashboard/low-stock-items`
- **Success Response**: `200 OK`

---

### 3.11 Reports Module

Service: `inventory-service` | Base Path: `/api/v1/reports`

#### 3.11.1 Sales Revenue Report
- **Purpose**: Aggregate revenue, discounts, tax, and item quantities over a date range.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/reports/sales`
- **Query Parameters**:
  - `startDate` (Date, Required, `YYYY-MM-DD`)
  - `endDate` (Date, Required, `YYYY-MM-DD`)
  - `groupBy` (String, Optional: `DAILY`, `MONTHLY`, `PRODUCT`, `CATEGORY`, Default: `DAILY`)
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Sales report generated",
    "data": {
      "startDate": "2026-10-01",
      "endDate": "2026-10-19",
      "totalRevenue": 245000.00,
      "totalDiscounts": 12500.00,
      "totalTax": 41670.00,
      "netSales": 274170.00,
      "breakdown": [
        {
          "period": "2026-10-19",
          "billCount": 18,
          "totalAmount": 14250.00
        }
      ]
    }
  }
  ```

#### 3.11.2 Inventory Valuation Report
- **Purpose**: Financial audit of capital tied up in stock.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/reports/inventory`
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Inventory valuation report generated",
    "data": {
      "totalStockQuantity": 2450,
      "totalCostValue": 845200.00,
      "totalRetailValue": 1240000.00,
      "potentialProfit": 394800.00,
      "lowStockCount": 3
    }
  }
  ```

#### 3.11.3 Procurement Expense Report
- **Purpose**: Supplier spend tracking over time.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/reports/purchases`
- **Query Parameters**: `startDate`, `endDate`, `supplierId` (Optional).
- **Success Response**: `200 OK`

---

### 3.12 Settings Module

Service: `inventory-service` | Base Path: `/api/v1/settings`

#### 3.12.1 Get All Settings
- **Purpose**: Fetch application profile and configuration dictionary.
- **HTTP Method**: `GET`
- **Endpoint**: `/api/v1/settings`
- **Success Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Settings retrieved",
    "data": {
      "company_name": "Apex Retail Solutions",
      "company_email": "contact@apexretail.com",
      "company_phone": "+91 98765 43210",
      "company_address": "102 Commercial Arcade, Bangalore, KA",
      "currency_symbol": "₹",
      "currency_code": "INR",
      "default_tax_rate": "18.00",
      "low_stock_notification_enabled": "true"
    }
  }
  ```

#### 3.12.2 Update Settings (Batch)
- **Purpose**: Save application configuration from settings screen.
- **HTTP Method**: `PUT`
- **Endpoint**: `/api/v1/settings`
- **Request Body**: Key-value JSON map of settings to update.
- **Success Response**: `200 OK`
- **Database Tables**: `settings`

---

## 4. CRUD Endpoints Matrix

| Module | Create (`POST`) | Read List (`GET`) | Read One (`GET`) | Update (`PUT`) | Delete (`DELETE`) |
|---|---|---|---|---|---|
| **Category** | `/api/v1/categories` | `/api/v1/categories` | `/api/v1/categories/{id}` | `/api/v1/categories/{id}` | `/api/v1/categories/{id}` |
| **Product** | `/api/v1/products` | `/api/v1/products` | `/api/v1/products/{id}` | `/api/v1/products/{id}` | `/api/v1/products/{id}` |
| **Inventory** | — (Auto-created) | `/api/v1/inventory` | `/api/v1/inventory/product/{id}` | `/api/v1/inventory/product/{id}/settings` | — (Cascade with Product) |
| **Supplier** | `/api/v1/suppliers` | `/api/v1/suppliers` | `/api/v1/suppliers/{id}` | `/api/v1/suppliers/{id}` | `/api/v1/suppliers/{id}` |
| **Customer** | `/api/v1/customers` | `/api/v1/customers` | `/api/v1/customers/{id}` | `/api/v1/customers/{id}` | `/api/v1/customers/{id}` |
| **Purchase** | `/api/v1/purchases` | `/api/v1/purchases` | `/api/v1/purchases/{id}` | `/api/v1/purchases/{id}/receive` | `/api/v1/purchases/{id}/cancel` |
| **Bill** | `/api/v1/bills` | `/api/v1/bills` | `/api/v1/bills/{id}` | `/api/v1/bills/{id}/confirm` | `/api/v1/bills/{id}/cancel` |
| **Payment** | `/api/v1/payments` | — | `/api/v1/payments/bill/{billId}` | — | — |
| **Notification**| — (Event-driven) | `/api/v1/notifications` | `/api/v1/notifications/{id}` | `/api/v1/notifications/{id}/approve` | — |
| **Settings** | — (Pre-seeded) | `/api/v1/settings` | `/api/v1/settings/{key}` | `/api/v1/settings` | — |

---

## 5. Sales & Invoicing Workflow

```
[Customer Select/Walk-in]
           │
           ▼
[POST /api/v1/bills] ──────────────────────► Creates Bill Header (Status: DRAFT)
           │                               Computes Subtotal, Tax, and Total Amount
           ▼
[PUT /api/v1/bills/{id}/confirm] ──────────► Checks Stock Availability
           │                               Deducts inventory.quantity_on_hand
           │                               Checks reorder_level
           │                                  │
           │                                  ├── (if stock <= reorder_level)
           │                                  └──► Publishes LowStockDetected ──► RabbitMQ
           ▼
[POST /api/v1/payments] (Port 8081) ───────► Validates Bill is CONFIRMED
           │                               Records payment in payment table
           │                               Updates bill.status = 'PAID'
           │
           ▼
[Publishes PaymentCompleted] ───────────► RabbitMQ Exchange
                                                   │
                                                   ▼
                                         [notification-service]
                                         Consumes PaymentCompleted
                                         Generates Invoice PDF
                                         Sends SMTP Email to Customer
                                         Updates bill.invoice_sent = TRUE
```

---

## 6. Purchase & Stock Ingestion Workflow

```
[Select Supplier]
       │
       ▼
[POST /api/v1/purchases] ──────────────────► Creates Purchase Header + Items
       │                                  Status: PENDING
       │                                  (Inventory NOT modified)
       ▼
[Goods Delivered to Warehouse]
       │
       ▼
[PUT /api/v1/purchases/{id}/receive] ──────► Atomic Transaction:
                                           1. Verify status is PENDING
                                           2. Increment inventory.quantity_on_hand
                                           3. Update product.purchase_price
                                           4. Set purchase.status = 'RECEIVED'
```

---

## 7. RabbitMQ Event Contracts

### 7.1 `PaymentCompleted` Event

- **Trigger**: Published by `payment-service` immediately after a payment record is persisted.
- **Exchange**: `inventory.exchange` (Topic Exchange)
- **Routing Key**: `payment.completed`
- **Consumer**: `notification-service` (`payment.completed.queue`)
- **Payload Schema**:
  ```json
  {
    "eventId": "evt-b91a7c3e-4d8f-4210-9b34-1189ac3f2010",
    "eventType": "PaymentCompleted",
    "timestamp": "2026-10-19T10:35:00Z",
    "payload": {
      "paymentId": 501,
      "billId": 101,
      "billNumber": "INV-2026-0001",
      "customerId": 2,
      "customerName": "Priya Sharma",
      "customerEmail": "priya.sharma@example.com",
      "amountPaid": 2003.64,
      "paymentMethod": "UPI",
      "referenceNumber": "UPI-IND-9876543210"
    }
  }
  ```
- **Consumer Action**:
  1. Verifies `customerEmail` is not null.
  2. Generates invoice PDF document for `billNumber`.
  3. Dispatches email via SMTP.
  4. Records entry in `notification` table with `status = 'SENT'` and `type = 'INVOICE_EMAIL'`.
  5. Updates `bill.invoice_sent = TRUE`.

---

### 7.2 `LowStockDetected` Event

- **Trigger**: Published by `inventory-service` when stock drops to or below `reorder_level` during bill confirmation or manual stock adjustment.
- **Exchange**: `inventory.exchange` (Topic Exchange)
- **Routing Key**: `stock.low`
- **Consumer**: `notification-service` (`stock.low.queue`)
- **Payload Schema**:
  ```json
  {
    "eventId": "evt-c12d4e5f-6a7b-4890-8c23-2290bd4e3021",
    "eventType": "LowStockDetected",
    "timestamp": "2026-10-19T10:32:00Z",
    "payload": {
      "productId": 4,
      "productName": "Pilot G2 0.7mm Retractable Gel Pen (Blue)",
      "sku": "STAT-PIL-G2",
      "currentQuantity": 8,
      "reorderLevel": 12,
      "supplierId": 3,
      "supplierName": "Metro Office & Stationery Supplies",
      "supplierEmail": "supply@metrostores.co.in"
    }
  }
  ```
- **Consumer Action**:
  1. Creates row in `notification` table with `status = 'PENDING'` and `type = 'LOW_STOCK_ALERT'`.
  2. Does **not** send email automatically; awaits business owner approval via `PUT /api/v1/notifications/{id}/approve`.

---

### 7.3 Future Event Contracts (Version 2)

| Event Name | Producer | Consumer | Purpose |
|---|---|---|---|
| `PurchaseReceived` | `inventory-service` | `reporting-service` | Real-time expense metrics |
| `BillCancelled` | `inventory-service` | `payment-service` | Auto-trigger refund workflow |
| `StockAdjusted` | `inventory-service` | `audit-service` | Log shrinkage or discrepancy audits |

---

## 8. Validation Rules Reference

| Field / Rule | Validation Constraint | HTTP Error | Error Message |
|---|---|---|---|
| `product.sku` | Unique, `@NotBlank`, max 100 | `400` / `409` | `"SKU must be unique and cannot be blank"` |
| `product.unit_price` | `@NotNull`, `> 0` | `400` | `"Unit price must be strictly greater than 0"` |
| `product.purchase_price` | `@NotNull`, `>= 0` | `400` | `"Purchase price cannot be negative"` |
| `inventory.quantity_on_hand` | `>= 0` | `400` | `"Stock quantity cannot drop below 0"` |
| `inventory.reorder_level` | `< max_stock_level` | `400` | `"Reorder level must be strictly less than maximum stock level"` |
| `supplier.email` | Unique, `@NotBlank`, `@Email` | `400` / `409` | `"Supplier email must be a valid unique email address"` |
| `customer.name` | `@NotBlank`, max 200 | `400` | `"Customer name is required"` |
| `bill.items` | `@NotEmpty`, min 1 item | `400` | `"Bill must contain at least one product item"` |
| `bill_item.quantity` | `> 0` | `400` | `"Line item quantity must be at least 1"` |
| `bill_item.discount_percent`| `>= 0 AND < 100` | `400` | `"Discount percent must be between 0 and 99.99"` |
| `payment.payment_method` | In `('CASH', 'CARD', 'UPI', 'BANK_TRANSFER', 'CHEQUE')` | `400` | `"Invalid payment method specified"` |
| `payment.amount_paid` | `== bill.total_amount` | `400` | `"Payment amount does not match total bill amount"` |
| Optimistic Concurrency | `version` matches database | `409` / `412` | `"Item was modified by another transaction. Please refresh and retry."` |

---

## 9. Error Handling & Status Codes

### 9.1 Standard HTTP Status Codes Used

| Code | Status | When Used |
|---|---|---|
| `200` | `OK` | Successful `GET`, `PUT`, or `DELETE` requests |
| `201` | `Created` | Successful `POST` entity creation |
| `400` | `Bad Request` | Validation failure, malformed payload, invalid state transition |
| `404` | `Not Found` | Target resource ID does not exist |
| `409` | `Conflict` | Database constraint collision (unique SKU, unique email, FK delete block, optimistic lock) |
| `422` | `Unprocessable Entity` | Syntactically correct JSON that violates business domain invariants |
| `500` | `Internal Server Error` | Uncaught server exception or unexpected infrastructure failure |

### 9.2 Error Response Examples

#### 9.2.1 Resource Not Found (`404`)
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Product with ID 99 was not found",
    "details": []
  },
  "timestamp": "2026-10-19T10:50:00Z",
  "path": "/api/v1/products/99"
}
```

#### 9.2.2 Insufficient Stock on Bill Confirmation (`400`)
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "Insufficient stock for product 'Pilot G2 0.7mm Retractable Gel Pen (Blue)'. Available: 8, Requested: 15",
    "details": [
      {
        "field": "quantity",
        "productId": 4,
        "available": 8,
        "requested": 15
      }
    ]
  },
  "timestamp": "2026-10-19T10:50:00Z",
  "path": "/api/v1/bills/101/confirm"
}
```

#### 9.2.3 Foreign Key Delete Conflict (`409`)
```json
{
  "success": false,
  "error": {
    "code": "DATA_INTEGRITY_VIOLATION",
    "message": "Cannot delete category 'Electronics' because 14 active products reference it",
    "details": []
  },
  "timestamp": "2026-10-19T10:50:00Z",
  "path": "/api/v1/categories/1"
}
```

#### 9.2.4 Optimistic Concurrency Conflict (`409`)
```json
{
  "success": false,
  "error": {
    "code": "OPTIMISTIC_LOCK_CONFLICT",
    "message": "Stock level was modified by another concurrent checkout. Please re-check cart and retry.",
    "details": []
  },
  "timestamp": "2026-10-19T10:50:00Z",
  "path": "/api/v1/bills/102/confirm"
}
```

---

## 10. Future APIs (Version 2)

The following capabilities are architecturally anticipated but deliberately excluded from Version 1:

1. **Authentication & Authorization**:
   - `POST /api/v1/auth/login` (JWT token issuance)
   - `POST /api/v1/auth/refresh`
   - `GET /api/v1/users`, `POST /api/v1/roles` (Role-Based Access Control: `ADMIN`, `CASHIER`, `WAREHOUSE`)
2. **Partial Payments & Multi-Tender Settlement**:
   - `POST /api/v1/bills/{id}/payments` (Splitting payment between Cash + UPI)
3. **Multi-Location & Warehouse Transfers**:
   - `GET /api/v1/warehouses`
   - `POST /api/v1/inventory/transfer` (Stock movement across stores/warehouses)
4. **Barcode & Label Generation**:
   - `GET /api/v1/products/{id}/barcode` (ZPL or SVG barcode label stream)
5. **Customer Loyalty & Store Credit**:
   - `GET /api/v1/customers/{id}/points`
   - `POST /api/v1/customers/{id}/redeem`

---

*This specification serves as the formal contract between frontend components and backend services.*  
*Author: Dharma S | Phase 3 — API Specification | Version 1.0*
