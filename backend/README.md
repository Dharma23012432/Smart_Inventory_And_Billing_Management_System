# backend/

This directory houses the backend microservices for the Smart Inventory & Billing Management System.

---

## Services Overview

| Service | Port | Technology | Key Responsibilities |
|---|---|---|---|
| `inventory-service` | 8080 | Spring Boot 3.3.5, PostgreSQL, JPA, RabbitMQ | Products, Customers, Suppliers, Purchases, Billing, Inventory, Reports |
| `payment-service` | 8081 | Spring Boot 3.3.5, PostgreSQL, JPA, RabbitMQ | Payment processing, validation, publishing `PaymentCompleted` events |
| `notification-service` | 8082 | Spring Boot 3.3.5, PostgreSQL, JPA, RabbitMQ, JavaMail | RabbitMQ event consumption, PDF invoice generation, email dispatch, supplier low-stock alerts |

---

## Directory Structure

```
backend/
├── pom.xml                     # Maven aggregator POM to build/test all services
├── inventory-service/          # Spring Boot service on port 8080
├── payment-service/            # Spring Boot service on port 8081
└── notification-service/       # Spring Boot service on port 8082
```

---

## Running the Services

### Build All Services
From the `backend/` directory:
```bash
mvn clean compile
```

### Running Individual Services
Open separate terminals for each service:

```bash
# Terminal 1 — inventory-service
cd backend/inventory-service
mvn spring-boot:run

# Terminal 2 — payment-service
cd backend/payment-service
mvn spring-boot:run

# Terminal 3 — notification-service
cd backend/notification-service
mvn spring-boot:run
```

---

## Architecture & Integration
- **Database:** All three services connect to the shared PostgreSQL database `smart_inventory_db`.
- **Inter-service Communication:**
  - Synchronous HTTP REST between `inventory-service` and `payment-service`.
  - Asynchronous event-driven messaging via RabbitMQ (`payment.completed.queue`, `stock.low.queue`) consumed by `notification-service`.
