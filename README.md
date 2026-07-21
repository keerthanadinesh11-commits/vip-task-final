# TaskFlow — Microservices Project

Spring Boot 3.2 + React 18 microservices application.

---

## Services

| Service              | Port | Database              |
|----------------------|------|-----------------------|
| eureka-server        | 8761 | —                     |
| api-gateway          | 8080 | —                     |
| auth-service         | 8081 | taskflow_auth         |
| user-service         | 8082 | taskflow_user         |
| task-service         | 8083 | taskflow_task         |
| notification-service | 8084 | taskflow_notification |
| React frontend       | 3000 | —                     |

---

## Prerequisites

- Java 17
- Maven 3.8+
- MySQL 8+
- Node.js 18+ / npm 9+

---

## MySQL Setup

```sql
CREATE DATABASE IF NOT EXISTS taskflow_auth;
CREATE DATABASE IF NOT EXISTS taskflow_user;
CREATE DATABASE IF NOT EXISTS taskflow_task;
CREATE DATABASE IF NOT EXISTS taskflow_notification;
```

All tables are created automatically by `spring.jpa.hibernate.ddl-auto=update`.

---

## Environment Variables (optional overrides)

| Variable             | Default                                          | Notes                        |
|----------------------|--------------------------------------------------|------------------------------|
| `DB_USERNAME`        | `root`                                           | MySQL username               |
| `DB_PASSWORD`        | `root`                                           | MySQL password               |
| `JWT_SECRET`         | `taskflowsupersecret…` (64+ chars)               | Must match across all services |
| `MAILTRAP_USERNAME`  | (empty)                                          | Mailtrap SMTP username       |
| `MAILTRAP_PASSWORD`  | (empty)                                          | Mailtrap SMTP password       |
| `DEFAULT_ADMIN_EMAIL`| `admin@taskflow.com`                             | See Section 1 notes below    |
| `DEFAULT_ADMIN_PASSWORD` | `Admin123`                                   | See Section 1 notes below    |

---

## Start Order

Start services in this exact order (wait for each to register in Eureka before starting the next):

```bash
# 1. Eureka
cd eureka-server && mvn spring-boot:run

# 2. API Gateway
cd api-gateway && mvn spring-boot:run

# 3. Auth Service
cd auth-service && mvn spring-boot:run

# 4. User Service
cd user-service && mvn spring-boot:run

# 5. Task Service
cd task-service && mvn spring-boot:run

# 6. Notification Service
cd notification-service && mvn spring-boot:run

# 7. React frontend
cd taskflow-frontend && npm install && npm start
```

Or from the parent directory:
```bash
mvn spring-boot:run -pl eureka-server &
# wait ~15s, then:
mvn spring-boot:run -pl api-gateway,auth-service,user-service,task-service,notification-service
```

---

## Default Admin Account (Section 1)

A default administrator account is created automatically at **auth-service startup** if it does not already exist. The credentials are read from `DEFAULT_ADMIN_EMAIL` / `DEFAULT_ADMIN_PASSWORD` environment variables and are **not** displayed in the UI, logs, Swagger UI, or documentation.

---

## Password Policy (Section 5)

All passwords (registration, admin user creation, password change) must contain:

- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit
- At least 1 special character
- Minimum 6 characters

Enforced both on the frontend (React) and backend (`PasswordPolicy.java`).

---

## Admin Creating Users (Section 2)

Admin creates users via:

```
POST /auth/admin/users
Authorization: Bearer <admin-jwt>
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Admin123!",
  "role": "USER"
}
```

This creates BCrypt-hashed credentials in `auth-service` **and** syncs a profile to `user-service` via Feign. The new user can log in **immediately**.

The frontend "Create User" form in the Users page calls this endpoint.

---

## Role-Based Task Authorization (Section 7)

| Caller    | Can update task?                     |
|-----------|--------------------------------------|
| ADMIN     | Any task                             |
| Assignee  | Only their own task                  |
| Other     | 403 Forbidden                        |

The server includes a `canEdit` field in every task response. The frontend uses this to show or hide the edit button.

---

## Notifications (Section 6)

`task-service` fires a notification via Feign to `notification-service` when:
- A task is created with an assignee
- A task is assigned via `POST /tasks/assign`
- A task status transitions to `COMPLETED`

Failures in notification delivery do **not** fail the task operation.

---

## Swagger UI

Each service exposes Swagger UI at `/swagger-ui.html`:

- Auth:         http://localhost:8081/swagger-ui.html
- User:         http://localhost:8082/swagger-ui.html
- Task:         http://localhost:8083/swagger-ui.html
- Notification: http://localhost:8084/swagger-ui.html

---

## Running Tests

### Backend (per service)

```bash
cd auth-service && mvn test
cd user-service && mvn test
cd task-service && mvn test
cd notification-service && mvn test
```

Tests use H2 in-memory database — no MySQL needed for tests.

### Frontend

```bash
cd taskflow-frontend
npm install
npm test                  # run tests once
npm run test:coverage     # run with coverage report
```

---

## Mailtrap (Section 8)

Email is sent via Mailtrap SMTP. Set `MAILTRAP_USERNAME` and `MAILTRAP_PASSWORD` environment variables before starting `auth-service`. Email is not required for core functionality (notifications go to the database directly via Feign).
