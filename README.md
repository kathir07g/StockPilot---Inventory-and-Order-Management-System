# StockPilot — Inventory and Order Management System

StockPilot is a RESTful backend application developed using Java and Spring Boot to manage products, inventory, customer orders, suppliers, and purchase orders. It provides secure access through JWT authentication and role-based authorization.

## Technologies Used

- **Language:** Java 25
- **Backend Framework:** Spring Boot 3.5.5
- **Security:** Spring Security, JWT
- **Database:** MySQL
- **ORM:** Spring Data JPA, Hibernate
- **Database Migration:** Flyway
- **Build Tool:** Maven
- **API Documentation:** OpenAPI / Swagger UI
- **Testing:** JUnit 5, Mockito, Spring Boot Test, MockMvc

## Features

### 1. Authentication and Authorization

- Customer registration and login.
- JWT-based authentication.
- Role-based access for Admin, Inventory Manager, Sales Staff, and Customer.
- Protected REST API endpoints.
- Authorization checks for protected resources.

### 2. Product Management

- Create and update products.
- Retrieve active products and individual product details.
- Deactivate products.
- Validate product information and prevent duplicate SKUs.

### 3. Inventory Management

- Track stock quantities for products.
- Receive stock into inventory.
- Maintain reorder levels.
- Identify products that need replenishment.
- Track reserved stock separately from available stock.
- Validate inventory operations to maintain stock consistency.

### 4. Customer Order Management

- Place orders for available products.
- Calculate order prices using server-side product prices.
- Reserve inventory when an order is placed.
- Cancel orders and release reserved stock.
- Fulfill orders and update inventory quantities.
- Enforce order ownership and valid order lifecycle transitions.
- Retrieve customer orders and paginated staff order lists.

### 5. Customer Management

- Retrieve the authenticated customer's profile.
- Update customer profile information.
- Validate customer information.

### 6. Supplier Management

- Create, retrieve, update, and deactivate suppliers.
- Associate purchase orders with suppliers.

### 7. Purchase Order Management

- Create purchase orders containing multiple products.
- Calculate purchase-order totals using item quantities and unit costs.
- Retrieve individual purchase orders and paginated purchase-order lists.
- Support partial and complete stock receipt through the purchase-order receiving workflow.
- Validate receiving operations and purchase-order status transitions.

### 8. API Documentation

- Interactive API documentation using Swagger UI.
- OpenAPI specification for API discovery.
- Documentation for exploring available endpoints and their request and response formats.

### 9. Automated Testing

- Product service validation tests.
- Customer service tests.
- Inventory behavior and validation tests.
- Order lifecycle and ownership tests.
- Order request validation tests.
- Purchase-order receiving and validation tests.
- JWT authentication-filter tests.
- User-details service tests.
- API security integration tests.

## Architecture

The application follows a layered architecture:

- **Controller Layer:** Exposes REST endpoints and handles HTTP requests.
- **Service Layer:** Contains business logic and transaction management.
- **Repository Layer:** Performs database operations using Spring Data JPA.
- **Entity Layer:** Represents database tables.
- **DTO Layer:** Defines request and response data structures.
- **Security Layer:** Handles JWT authentication and authorization.
- **Migration Layer:** Uses Flyway to manage database schema changes.

## Main API Routes

| Module | Base route |
|---|---|
| Status | `/api/v1/status` |
| Authentication | `/api/v1/auth` |
| Products | `/api/v1/products` |
| Customers | `/api/v1/customers` |
| Inventory | `/api/v1/inventory` |
| Orders | `/api/v1/orders` |
| Suppliers | `/api/v1/suppliers` |
| Purchase Orders | `/api/v1/purchase-orders` |

For the exact HTTP methods, request parameters, and request and response formats, refer to Swagger UI.

## Prerequisites

- Java 25 JDK.
- MySQL Server.
- Git, if cloning the repository.
- Maven Wrapper included in the project.

## Local Setup

### 1. Clone the Repository

```bash
git clone https://github.com/kathir07g/StockPilot---Inventory-and-Order-Management-System.git
cd StockPilot---Inventory-and-Order-Management-System
```

### 2. Create the Database

Create a MySQL database named `stockpilot`.

```sql
CREATE DATABASE stockpilot;
```

Create a MySQL user with appropriate permissions for this database, or use an existing dedicated application user.

### 3. Configure Environment Variables

On Windows PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/stockpilot?serverTimezone=UTC"
$env:DB_USERNAME = "stockpilot_app"
$env:DB_PASSWORD = "YOUR_DATABASE_PASSWORD"
$env:JWT_SECRET = "YOUR_BASE64_ENCODED_SECRET"
```

Replace the placeholder values with your actual credentials. Use a strong, randomly generated Base64-encoded JWT secret compatible with the application's signing algorithm.

Never commit real credentials or JWT secrets to GitHub.

### 4. Run Automated Tests

```powershell
.\mvnw.cmd clean test
```

### 5. Start the Application

```powershell
.\mvnw.cmd spring-boot:run
```

The application runs on port `8080` by default.

Flyway applies pending database migrations on startup, while Hibernate validates the database schema.

## API Documentation

After starting the application, open:

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI specification:** http://localhost:8080/v3/api-docs

Swagger UI allows you to explore available API endpoints and inspect their request and response formats. Protected business endpoints still require authentication and appropriate authorization.

## Health Check

Send a GET request to:

`http://localhost:8080/api/v1/status`

Example response:

```json
{
  "status": "UP",
  "application": "StockPilot"
}
```

## Configuration and Security

- Database credentials and the JWT secret are supplied through environment variables.
- JWT tokens are configured to expire after 15 minutes.
- Hibernate uses schema validation instead of automatically modifying the database schema.
- Flyway manages database schema migrations.
- Admin bootstrap is disabled by default and should only be enabled intentionally with secure credentials.
- Protected API endpoints require authentication and appropriate authorization.
- Swagger access does not bypass application security.

## Running Tests

Run the complete automated test suite using:

```powershell
.\mvnw.cmd clean test
```

The test suite covers product and customer validation, inventory behavior, order lifecycle and ownership, purchase-order receiving, JWT authentication, and API authorization.

## Future Enhancements

Potential future improvements include:

- Reporting and analytics.
- Returns and refund management.
- Docker and Docker Compose support.
- Continuous integration and deployment using GitHub Actions.
- Cloud deployment and production monitoring.
- Expanded integration and concurrency testing.

## Project Purpose

StockPilot demonstrates REST API development, authentication and authorization, relational database design, transaction management, inventory reservation, order processing, supplier purchasing workflows, and automated testing using Java and Spring Boot.
