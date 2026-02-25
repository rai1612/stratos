# Stratos

**Stratos** is a Collaborative Task Management System built with Spring Boot. It provides a structured hierarchy of **Workspaces → Projects → Tasks**, secured with Role-Based Access Control (RBAC) and JWT authentication.

## 🚀 Tech Stack

*   **Framework:** Spring Boot 3.4.0
*   **Language:** Java 17
*   **Database:** PostgreSQL 16
*   **ORM:** Spring Data JPA / Hibernate
*   **Security:** Spring Security + JWT (`jjwt` 0.12.3)
*   **Testing:** Spring Boot Test, Testcontainers
*   **Build Tool:** Maven

## 📦 Domain Model

The relationship between entities in the system is hierarchical:

```text
User ──< ManyToMany >── Role (USER, MANAGER, ADMIN)
  │
  ├── owns ──→ Workspace
  │                │
  │                └── contains ──→ Project
  │                                    │
  └── assigned to ──→ Task ←── belongs to ──┘
```

*   **Workspace:** A high-level container owned by a User.
*   **Project:** A categorization within a Workspace (identified by a unique `projectKey`, e.g., "STRAT").
*   **Task:** The core actionable item. Features statuses (`TODO`, `IN_PROGRESS`, `DONE`), priorities (`LOW`, `MEDIUM`, `HIGH`), due dates, and assignees.

## 🔐 Security & Roles

The system uses stateless JWT authentication. On application startup, three roles are automatically seeded into the database:

1.  `ROLE_USER`: Standard access to create/manage workspaces, projects, and tasks.
2.  `ROLE_MANAGER`: Elevated access.
3.  `ROLE_ADMIN`: Full system access.

All API endpoints (except for `/api/auth/**` and `/api/test/**`) require a valid JWT passed in the `Authorization: Bearer <token>` header.

## ⚙️ Running the Application

### Prerequisites
*   Docker and Docker Compose (for the database)
*   Java 17
*   Maven

### 1. Start the Database
The project includes a `docker-compose.yml` file to easily spin up a PostgreSQL instance.
```bash
docker compose up -d
```
*This starts Postgres on port `5433` (mapped from container port `5432`) with user `stratos_user` and database `stratos_db`.*

### 2. Run the Spring Boot App
```bash
mvn spring-boot:run
```
The application will start on `http://localhost:8080`. Hibernate is configured to automatically update the schema (`ddl-auto=update`).

### 3. Quick Start (cURL)

**Register a new user:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

**Log in to get a JWT:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

**Access a protected endpoint:**
```bash
curl http://localhost:8080/api/workspaces \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

## 🧪 Testing

The test suite uses **Testcontainers** to spin up a disposable, isolated PostgreSQL instance during the test phase. You do **not** need to have the `docker compose` database running to execute tests, but the Docker daemon must be running on your machine.

```bash
mvn test
```

The integration tests cover authentication workflows, entity constraints, CRUD operations for all domains, RBAC enforcement, and advanced task search specifications.

## 📡 Key API Endpoints

### Auth
*   `POST /api/auth/register` - Create a new user
*   `POST /api/auth/login` - Authenticate and retrieve a JWT

### Workspaces
*   `POST /api/workspaces` - Create a workspace
*   `GET /api/workspaces` - List current user's workspaces
*   `GET /api/workspaces/{id}` - Get a specific workspace

### Projects
*   `POST /api/projects` - Create a project inside a workspace
*   `GET /api/projects/workspace/{workspaceId}` - List all projects in a workspace

### Tasks
*   `POST /api/tasks` - Create a task
*   `GET /api/tasks/project/{projectId}` - List tasks in a project
*   `PATCH /api/tasks/{id}/status` - Update task status
*   `GET /api/tasks/search` - Advanced paginated search with dynamic filters (status, priority, assignee, date range, etc.)
