# Campus Eateries — Takeaway Ordering & Admin Analytics

A modular Java + MySQL takeaway ordering platform designed using layered MVC architecture, transactional JDBC workflows, and analytics-driven administration tooling.
Stack: **Java 17+, JDBC + MySQL 8+, layered MVC**, console view (swap for JavaFX/Spring later).

![Java](https://img.shields.io/badge/Java-17-orange)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Architecture](https://img.shields.io/badge/Architecture-MVC-green)
![Build](https://img.shields.io/badge/Build-Maven-red)

## Feature checklist

| Area | Implementation |
| --- | --- |
| Auth | `AuthService` + `UserDao` registrations with transactional uniqueness guards |
| Catalog | Restaurants + categorized menus via JDBC joins |
| Cart | Upsert/remove flows with monetary rollups (`CartOrchestrationService`) |
| Checkout | Multi-statement JDBC transaction (`OrderFulfillmentService`) |
| Payments | OO strategy hierarchy + `CALL sp_simulate_pay_order(...)` |
| Tracking | Historical orders + granular line inspection |
| Admin | KPI panels powered by aggregates, VIEWs, stored procedures |

## Project structure (`src/main/java`)

```
com.campuseateries/
├── CampusEateriesApp.java          # bootstrap
├── controller/                     # MVC controller (scanner orchestration)
│   └── MvcShellController.java
├── service/                        # business rules & transactions
│   ├── AuthService.java
│   ├── RestaurantCatalogService.java
│   ├── CartOrchestrationService.java
│   ├── OrderFulfillmentService.java
│   ├── OrderReadService.java
│   ├── PaymentOrchestrationService.java
│   ├── AnalyticsDashboardService.java
│   └── ...interfaces + DTO records
├── dao/                            # JDBC repositories
├── model/                          # entities, enums, OO hierarchies (`user`, `payment`)
├── view/ConsoleView.java           # ASCII rendering helpers
├── database/DbConnectionManager.java
└── util/PasswordHasher.java        # demo SHA-256 helper (upgrade for prod)
```

## Database artifacts

Run in order inside MySQL:

1. `sql/01_schema.sql` — 10 × 3NF tables (`users`, `admin_profiles`, `categories`, `restaurants`, `menu_items`, `shopping_cart_items`, `orders`, `order_items`, `payments`, `delivery_details`)  
2. `sql/02_triggers_procedures_views.sql` — referential safeguards + automation (`trg_bi_order_items_enforce_stock`, payment validation, KPI VIEW + routines)  
3. `sql/03_sample_data.sql` — eateries + seeded accounts  
4. `sql/05_synthetic_transaction_seed.sql` — extra students + paid order history + one pending payment (fills analytics)  
5. `sql/04_reporting_queries.sql` — illustrative analytics SELECTs (`CALL` at end is optional)  

See `docs/SYNTHETIC_DATA_RESULTS.md` for expected counts and report behavior.

### Relationship mapping cheat sheet

| Relationship | Modeling approach |
| --- | --- |
| User → Cart lines | FK `shopping_cart_items.user_id` + unique `(user_id, menu_item_id)` |
| Restaurant → Menu items | FK `menu_items.restaurant_id` |
| Category → Menu items | FK `menu_items.category_id` |
| User → Orders | FK `orders.user_id` |
| Order → Lines | FK `order_items.order_id` + FK `menu_item_id` (price snapshot persisted) |
| Order → Payment | 1:1 via `payments.order_id` UNIQUE |
| Admin extension | Optional `admin_profiles.admin_user_id` references `users` |

## Architecture

MVC layered architecture:

View (ConsoleView)
   ↓
Controller Layer
   ↓
Service Layer
   ↓
DAO Layer (JDBC)
   ↓
MySQL Database


### Object-oriented motifs

- **Inheritance**: `StudentUser` / `AdminAccount` specialize `AbstractUser`.  
- **Polymorphism + abstraction**: Payment strategies extend `AbstractPayment` and expose `jdbcChannel()` for DAO mapping.  
- **Interfaces**: `IAuthService`, `IOrderFulfillment`, `IAuthenticatedUser`, `IPaymentSimulator`.  
- **Encapsulation**: entity fields mutated through beans + service validations.

UML source: [`docs/UML_CLASS_DIAGRAM.puml`](docs/UML_CLASS_DIAGRAM.puml) (PlantUML).  
ER narrative: [`docs/ER_DIAGRAM.md`](docs/ER_DIAGRAM.md) (Mermaid).

## JDBC configuration

1. Copy `src/main/resources/db.properties.example` → `src/main/resources/db.properties` (**git‑ignored locally**—never commit passwords).  
2. Point `jdbc.url` at your cluster; keep `serverTimezone`, SSL flags aligned with infra policy.

## Running

```

CLI tips print inside the anonymous menu (`[4] readme`).  
Seed accounts (SHA‑256 / `password123`):

| Role | Email | Password |
| --- | --- | --- |
| Student | `alice.student@campus.edu` | `password123` |
| Admin | `admin.ops@campus.edu` | `password123` |

