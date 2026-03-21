# FinSet Backend

RESTful API for the FinSet finance application built with Spring Boot 3.5 and Java 21.

Git repo: https://github.com/sophavattey/expense-tracking-backend

---

## Table of Contents

- [FinSet Backend](#finset-backend)
  - [Table of Contents](#table-of-contents)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Environment Variables](#environment-variables)
  - [Project Structure](#project-structure)
  - [Features](#features)
  - [API Reference](#api-reference)
    - [Authentication](#authentication)
    - [Expenses](#expenses)
    - [Budgets](#budgets)
    - [Groups](#groups)
    - [Notifications](#notifications)
  - [Deployment](#deployment)
    - [Docker](#docker)
    - [Render](#render)
    - [application.yml Notes](#applicationyml-notes)
  - [Google Cloud Console Setup](#google-cloud-console-setup)
    - [localhost](#localhost)
    - [production](#production)
  - [Security Notes](#security-notes)

---

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- PostgreSQL 14 or higher
- Docker (for containerized deployment)
- Maven (bundled via `mvnw` wrapper)

---

## Local Development

```bash
# Clone the repository
git clone https://github.com/sophavattey/expense-tracking-backend
cd expense-tracking-backend

# Create a local PostgreSQL database
createdb finset

# Configure application.yml for local use
# Set url: jdbc:postgresql://localhost:5432/finset
# Set ddl-auto: update for local schema management

# Start the application
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## Environment Variables

Set these variables in your environment or in a `.env` file for local development. All variables are required for production.

| Variable                | Description                                                                 |
|-------------------------|-----------------------------------------------------------------------------|
| `SPRING_DATASOURCE_URL` | Full JDBC URL, e.g. `jdbc:postgresql://host:5432/finset`                    |
| `DB_USERNAME`           | Database username                                                           |
| `DB_PASSWORD`           | Database password                                                           |
| `JWT_ACCESS_SECRET`     | Random Base64 string, minimum 64 characters                                 |
| `JWT_REFRESH_SECRET`    | Random Base64 string, minimum 64 characters, different from access secret   |
| `GOOGLE_CLIENT_ID`      | OAuth2 client ID from Google Cloud Console                                  |
| `GOOGLE_CLIENT_SECRET`  | OAuth2 client secret from Google Cloud Console                              |
| `FRONTEND_URL`          | Full Vercel deployment URL with no trailing slash                           |
| `BACKEND_URL`           | Full Render deployment URL with no trailing slash                           |
| `COOKIE_SECURE`         | Set to `true` for HTTPS environments                                        |
| `COOKIE_SAME_SITE`      | Set to `None` for cross-origin cookie support                               |

Generate secure JWT secrets with:

```bash
openssl rand -base64 64
```

Run the command twice to produce two distinct values.

---

## Project Structure

```
src/main/java/com/example/finset/
  config/
    AppProperties.java          Typed configuration properties
    DataSeeder.java             Default category seeding on startup
    SecurityConfig.java         Spring Security filter chain and CORS
  controller/
    AuthController.java         Authentication endpoints
    BudgetController.java       Budget CRUD and status endpoints
    CategoryController.java     Category management endpoints
    ExpenseController.java      Expense CRUD and summary endpoints
    GroupController.java        Group management endpoints
    NotificationController.java Notification list and read endpoints
    UserController.java         User profile and preferences
  dto/                          Request and response data transfer objects
  entity/                       JPA entity classes
  exception/                    Custom exceptions and global error handler
  repository/                   Spring Data JPA repository interfaces
  security/
    jwt/
      CookieService.java        HTTP-only cookie management
      JwtAuthenticationFilter.java  Per-request token validation
      JwtService.java           Token generation and validation
    oauth2/
      CookieOAuth2AuthorizationRequestRepository.java  Mobile-compatible OAuth2 state storage
      OAuth2FailureHandler.java  OAuth2 error redirect
      OAuth2SuccessHandler.java  Token issuance and redirect after OAuth2
  service/
    AuthService.java            Registration, login, token rotation
    BudgetService.java          Budget logic with personal/group isolation
    CategoryService.java        Category management
    ExpenseService.java         Expense management and summaries
    GroupService.java           Group membership and invite codes
    NotificationService.java    Budget threshold and group event notifications
    UserService.java            User profile and OAuth2 user provisioning
```

---

## Features

- JWT authentication with HTTP-only cookies and automatic token rotation
- Google OAuth2 login with cookie-based state storage for mobile browser compatibility
- Personal and group expense tracking with strict data isolation at the query level
- Budget management with daily, weekly, and monthly periods
- Budget and group expenses are isolated: personal queries filter by `group IS NULL`, group queries filter by `group.id`
- Multi-currency support for USD and KHR with automatic base-amount conversion
- Group invite system with expiring invite codes
- Notification system for budget warnings, budget exceeded events, and group membership changes
- Stateless session management using Spring Security with `SessionCreationPolicy.STATELESS`

---

## API Reference

### Authentication

| Method | Path                              | Auth     | Description                              |
|--------|-----------------------------------|----------|------------------------------------------|
| POST   | `/api/auth/register`              | Public   | Register with name, email, and password  |
| POST   | `/api/auth/login`                 | Public   | Login, sets JWT cookies                  |
| POST   | `/api/auth/refresh`               | Public   | Rotate refresh token                     |
| POST   | `/api/auth/logout`                | Required | Revoke tokens and clear cookies          |
| GET    | `/api/auth/me`                    | Required | Return authenticated user profile        |
| GET    | `/oauth2/authorization/google`    | Public   | Initiate Google OAuth2 flow              |

### Expenses

| Method | Path                              | Description                                      |
|--------|-----------------------------------|--------------------------------------------------|
| GET    | `/api/expenses`                   | List personal expenses with filters              |
| POST   | `/api/expenses`                   | Create a personal expense                        |
| PUT    | `/api/expenses/{id}`              | Update an expense                                |
| DELETE | `/api/expenses/{id}`              | Delete an expense                                |
| GET    | `/api/expenses/summary`           | Monthly summary with category breakdown          |
| GET    | `/api/groups/{id}/expenses`       | List expenses for a group                        |
| GET    | `/api/groups/{id}/expenses/summary` | Group monthly summary                          |

### Budgets

| Method | Path                                    | Description                                      |
|--------|-----------------------------------------|--------------------------------------------------|
| GET    | `/api/budgets/status`                   | Personal budget status with current spending     |
| POST   | `/api/budgets`                          | Create a personal budget                         |
| PUT    | `/api/budgets/{id}`                     | Update a budget                                  |
| DELETE | `/api/budgets/{id}`                     | Delete a budget                                  |
| GET    | `/api/budgets/group/{groupId}/status`   | Group budget status                              |
| POST   | `/api/budgets/group/{groupId}`          | Create a group budget (owner only)               |

### Groups

| Method | Path                                    | Description                                      |
|--------|-----------------------------------------|--------------------------------------------------|
| GET    | `/api/groups/mine`                      | List groups the current user belongs to          |
| POST   | `/api/groups`                           | Create a group                                   |
| PUT    | `/api/groups/{id}`                      | Rename a group (owner only)                      |
| DELETE | `/api/groups/{id}`                      | Dissolve a group (owner only)                    |
| POST   | `/api/groups/join`                      | Join a group with an invite code                 |
| POST   | `/api/groups/{id}/leave`                | Leave a group                                    |
| DELETE | `/api/groups/{id}/members/{userId}`     | Remove a member (owner only)                     |
| POST   | `/api/groups/{id}/invite/regenerate`    | Generate a new invite code (owner only)          |

### Notifications

| Method | Path                                | Description                                  |
|--------|-------------------------------------|----------------------------------------------|
| GET    | `/api/notifications`                | List recent notifications with unread count  |
| PUT    | `/api/notifications/{id}/read`      | Mark a notification as read                  |
| PUT    | `/api/notifications/read-all`       | Mark all notifications as read               |

---

## Deployment

### Docker

The Dockerfile uses a two-stage build. The first stage compiles the JAR using the full JDK. The second stage copies only the JAR into a minimal JRE image, producing a final image of approximately 200 MB.

```bash
# Build the JAR
./mvnw clean package -DskipTests

# Build and push the Docker image
docker build -t your-dockerhub-username/finset-app .
docker push your-dockerhub-username/finset-app
```

### Render

1. Create a new Web Service on Render and select Docker as the runtime.
2. Provide the Docker Hub image name.
3. Add all environment variables listed in the Environment Variables section.
4. Deploy. Hibernate will create the database schema automatically on first startup.

For subsequent deploys after code changes:

```bash
./mvnw clean package -DskipTests
docker build -t your-dockerhub-username/finset-app .
docker push your-dockerhub-username/finset-app
```

Then trigger a Manual Deploy in the Render dashboard.

### application.yml Notes

- Use `ddl-auto: update` for local development to auto-create tables.
- Use `ddl-auto: validate` for production to prevent unintended schema changes.
- The `redirect-uri` for OAuth2 must use `${BACKEND_URL}`, not `{baseUrl}`, to ensure the correct URL is sent to Google when deployed behind a proxy.

---

## Google Cloud Console Setup

In [Google Cloud Console](https://console.cloud.google.com), open your OAuth 2.0 client and add:

### localhost
- **Authorized JavaScript Origins:** `http://localhost:3000`
- **Authorized Redirect URIs:** `http://localhost:8080/oauth2/callback/google`
### production
- **Authorized JavaScript Origins:** `https://your-app.vercel.app`
- **Authorized Redirect URIs:** `https://your-app.onrender.com/oauth2/callback/google`
---

## Security Notes

- Access tokens expire after 15 minutes.
- Refresh tokens expire after 7 days and are rotated on each use.
- All tokens are stored in HTTP-only cookies with `Secure=true` and `SameSite=None` for cross-origin support.
- The `CookieOAuth2AuthorizationRequestRepository` stores OAuth2 state in a `SameSite=None` cookie instead of the HTTP session. This is required for mobile browsers that block session cookies during cross-site redirects.
- Personal and group budget spending queries are isolated at the database level. Personal queries include `AND e.group IS NULL`. Group queries filter by `e.group.id = :groupId`.