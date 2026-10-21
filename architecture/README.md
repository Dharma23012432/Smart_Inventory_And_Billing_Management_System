# architecture/

This directory contains architecture documentation and diagrams for the Smart Inventory & Billing Management System.

## Planned Contents

| File | Description |
|---|---|
| `system-overview.png` | High-level system architecture diagram |
| `communication-flow.png` | Synchronous and asynchronous communication flow |
| `deployment-diagram.png` | Docker Compose deployment layout |
| `event-flow.png` | RabbitMQ event routing diagram |

> **Note:** Diagrams will be added progressively as each phase is completed.

## Architecture Summary

### Services

| Service | Port | Responsibilities |
|---|---|---|
| `inventory-service` | 8080 | Products, Customers, Suppliers, Purchases, Billing, Inventory, Reports |
| `payment-service` | 8081 | Payment Processing, PaymentCompleted events |
| `notification-service` | 8082 | Event Consumption, Invoice PDF, Email, Notifications |

### Communication

- **Synchronous:** React → inventory-service → payment-service (HTTP REST)
- **Asynchronous:** inventory-service → RabbitMQ → notification-service
- **Asynchronous:** payment-service → RabbitMQ → notification-service

### Events

| Event | Producer | Consumer |
|---|---|---|
| `PaymentCompleted` | payment-service | notification-service |
| `LowStockDetected` | inventory-service | notification-service |
