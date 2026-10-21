# Smart Inventory & Billing Management System

**Version:** 1.0 | **Author:** Dharma S | **Architecture:** Microservices

---

## Overview

A modern web-based inventory and billing management system designed for small and medium-sized businesses.  
Built to demonstrate microservices architecture, event-driven communication, and industry-standard backend development practices.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS |
| Backend | Java 21, Spring Boot 3.3.5, Spring Data JPA, Maven |
| Database | PostgreSQL |
| Messaging | RabbitMQ |
| Email | Spring Mail, SMTP |
| Containerization | Docker, Docker Compose (Phase 9) |

---

## Project Structure

```
smart-inventory-system/
│
├── frontend/                   # React + Vite + Tailwind CSS
│   └── src/
│       ├── pages/              # Dashboard, Sales, Products, etc.
│       ├── components/         # Shared UI components
│       ├── layouts/            # MainLayout shell
│       ├── services/           # API client
│       ├── hooks/              # Custom React hooks
│       └── utils/              # Formatters and helpers
│
├── backend/                    # Microservices backend
│   ├── pom.xml                 # Aggregator Maven POM
│   ├── inventory-service/      # Spring Boot — port 8080
│   │   └── src/main/java/com/smartinventory/inventoryservice/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── entity/
│   │       ├── dto/
│   │       ├── config/
│   │       ├── exception/
│   │       └── event/
│   │
│   ├── payment-service/        # Spring Boot — port 8081
│   │   └── src/main/java/com/smartinventory/paymentservice/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── entity/
│   │       ├── dto/
│   │       ├── config/
│   │       ├── exception/
│   │       └── event/
│   │
│   └── notification-service/   # Spring Boot — port 8082
│       └── src/main/java/com/smartinventory/notificationservice/
│           ├── controller/
│           ├── service/
│           ├── repository/
│           ├── entity/
│           ├── dto/
│           ├── config/
│           ├── exception/
│           └── event/
│
├── database/                   # SQL scripts and ERD (Phase 2)
├── architecture/               # Architecture diagrams
├── docs/                       # Project documentation
└── README.md
```

---

## Services

| Service | Port | Key Responsibilities |
|---|---|---|
| `inventory-service` | 8080 | Products, Customers, Suppliers, Purchases, Billing, Inventory, Reports |
| `payment-service` | 8081 | Payment processing, PaymentCompleted events |
| `notification-service` | 8082 | Event consumption, Invoice PDFs, Email dispatch |

---

## Communication Architecture

```
React Frontend
      │
      ▼ (HTTP REST)
inventory-service (8080)
      │ ──────────────────────────► RabbitMQ ──► notification-service (8082)
      │
      ▼ (HTTP REST)
payment-service (8081)
      │ ──────────────────────────► RabbitMQ ──► notification-service (8082)
                                                         │
                                                         ▼ (SMTP)
                                                   Customer / Supplier Email
```

---

## RabbitMQ Events

| Event | Producer | Consumer | Trigger |
|---|---|---|---|
| `PaymentCompleted` | payment-service | notification-service | Payment successful → Email invoice to customer |
| `LowStockDetected` | inventory-service | notification-service | Stock < reorder level → Notify supplier (with approval) |

---

## Development Phases

| Phase | Description | Status |
|---|---|---|
| Phase 1 | Project Setup | ✅ Complete |
| Phase 2 | Database Design | ✅ Complete |
| Phase 3 | API Design | ✅ Complete |
| Phase 4 | Inventory & Billing Service | ✅ Complete (186/186 tests passing) |
| Phase 5 | Frontend | ⏳ In Progress |
| Phase 6 | Payment Service | ✅ Complete (17/17 tests passing) |
| Phase 7 | RabbitMQ Integration | ✅ Complete |
| Phase 8 | Notification Service | ✅ Complete (15/15 tests passing) |
| Phase 9 | Docker | ⏳ Pending |
| Phase 10 | Deployment | ⏳ Pending |

---

## Local Development Setup

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 22+
- PostgreSQL 16+
- RabbitMQ 3.13+

### 1. Database

```sql
CREATE DATABASE smart_inventory_db;
```

Then run `database/schema.sql` (available in Phase 2).

### 2. Backend Services

```bash
# Build all backend services (optional):
cd backend
mvn clean compile

# Or run individual services in separate terminals:
# Terminal 1 — inventory-service (port 8080)
cd backend/inventory-service
mvn spring-boot:run

# Terminal 2 — payment-service (port 8081)
cd backend/payment-service
mvn spring-boot:run

# Terminal 3 — notification-service (port 8082)
cd backend/notification-service
mvn spring-boot:run
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at: http://localhost:3000  
API proxy target: http://localhost:8080

---

## Coding Standards

- SOLID Principles
- Layered Architecture (Controller → Service → Repository)
- Constructor Injection (no field injection)
- DTO Pattern
- RESTful API Design (plural nouns, standard HTTP methods)
- Global Exception Handling (consistent JSON error responses)
- Bean Validation (`@Valid`, `@NotNull`, etc.)
- SLF4J Logging (no `System.out.println`)
- Clean Code Principles

---

## Frontend Modules

| Module | Route | Description |
|---|---|---|
| Dashboard | `/` | Business overview and KPIs |
| Sales | `/sales` | Create new sales / bills |
| Sales History | `/sales-history` | Past transactions |
| Products | `/products` | Product catalog management |
| Inventory | `/inventory` | Stock level management |
| Purchases | `/purchases` | Supplier purchase orders |
| Customers | `/customers` | Customer management |
| Suppliers | `/suppliers` | Supplier management |
| Reports | `/reports` | Business reports |
| Notifications | `/notifications` | System notifications |
| Settings | `/settings` | Application settings |

---

*This project is a portfolio-quality demonstration of microservices architecture, event-driven communication, and modern full-stack development practices.*
