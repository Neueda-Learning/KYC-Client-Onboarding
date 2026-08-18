# KYC Client Onboarding System

System for managing client identity verification processes (Know Your Customer - KYC). The project includes a MySQL relational database structure, a set of administrative automation scripts, and a relay server (Relay API) written in Java.

---

## 📁 Project Structure

```text
PROJECT-KYC/
├── scripts/
│   └── db/
│       ├── _lib.sh                  # Shared MySQL client discovery and password helpers
│       ├── bootstrap_db.sh          # Full DB setup: creates, seeds, and verifies from scratch
│       ├── db_dump.sh               # Database backup script
│       ├── db_reload.sh             # Restore database from backup
│       ├── expiring_docs_report.sh  # Generates expiring_docs_report.csv
│       ├── expiring_docs_report.csv # Generated expiring documents report
│       ├── kyc_db_backup.sql        # Database backup file (produced by db_dump.sh)
│       └── rebuild_indexes.sh       # Optimizes and rebuilds indexes
├── src/
│   ├── lib/
│   │   ├── mysql-connector-j-8.3.0.jar # MySQL JDBC Driver
│   │   ├── slf4j-api-2.0.13.jar        # SLF4J logging API
│   │   ├── logback-core-1.5.6.jar      # Logback core
│   │   └── logback-classic-1.5.6.jar   # Logback SLF4J implementation
│   ├── logback.xml                  # Logging configuration
│   ├── KycApiServer.java            # Lightweight HTTP server (Java Relay API)
│   └── util/HttpResponseUtil.java   # Shared HTTP response helper
├── 01_seed_data.sql                 # Test data (10 clients)
├── create_database.sql              # Database creation (kyc_db)
├── ddl_schema.sql                   # Tables, keys, and relationships
├── select_all_clients.sql           # Auxiliary query for client verification
├── views_stored_procedures.sql      # SQL views and stored procedures
├── EDB Diagram.pdf                  # ERD diagram of the database
├── Project-Brief-02-KYC-...pdf      # Business requirements documentation
└── README.md                        # Project documentation
```
---

## 🛠️ Prerequisites

* **MySQL 8.x** — either MySQL Server 8.0 (standalone) or XAMPP
* **Java Development Kit (JDK 17+)**
* **Git Bash** or any Unix-like terminal

---

## 🚀 Setup and Execution Instructions

### 1. MySQL Database Setup

All scripts auto-discover the MySQL client. On a standalone **MySQL Server 8.0** install no extra configuration is needed. On **XAMPP**, set `MYSQL_BIN` once per terminal session before running any script:

```bash
export MYSQL_BIN="/c/xampp/mysql/bin"
```

You can also pre-set credentials to skip the password prompt:

```bash
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
```

Run the bootstrap script to create the database, apply the schema, load views/procedures, and seed test data in one step:

```bash
chmod +x scripts/db/*.sh
./scripts/db/bootstrap_db.sh
```

Re-run bootstrap any time the schema changes — `ddl_schema.sql` drops and recreates all tables.

---

### 2. Administrative Scripts

All scripts are in `scripts/db/`. They share the same MySQL discovery and password-prompt logic via `_lib.sh`, so `MYSQL_BIN` / `MYSQL_USER` / `MYSQL_PASSWORD` apply to all of them.

* **Backup the database:**
```bash
./scripts/db/db_dump.sh
# Saves to scripts/db/kyc_db_backup.sql
```

* **Restore from backup:**
```bash
./scripts/db/db_reload.sh
# Drops kyc_db, recreates it, and loads scripts/db/kyc_db_backup.sql
```

* **Generate expiring documents CSV report:**
```bash
./scripts/db/expiring_docs_report.sh
# Saves to scripts/db/expiring_docs_report.csv
```

* **Optimize indexes:**
```bash
./scripts/db/rebuild_indexes.sh
```

---

### 3. Java API Server Compilation and Execution

The relay server exposes a local HTTP endpoint that retrieves data directly from the MySQL database.

1. Navigate to the source directory:
```bash
cd src

```


2. Compile the Java server with all dependency jars on the classpath:
```bash
javac -cp "lib/*" -d out $(find . -maxdepth 1 -name "*.java") $(find controller repository service util -name "*.java")

```


3. Run the API server (the working directory must contain `logback.xml`):
* **In Windows environment (Git Bash):**
```bash
java -cp "out;.;lib/*" KycApiServer

```


* **In Linux / macOS environment:**
```bash
java -cp "out:.:lib/*" KycApiServer

```



---

### 4. Frontend (React) Setup and Execution

The frontend is a Vite + React app in `frontend/` that talks to the Java API server above (default `http://localhost:8080`). Start the API server first, then run the frontend:

1. Install dependencies (first time only):
```bash
cd frontend
npm install
```

2. Start the dev server:
```bash
npm run dev
```
The app is served at `http://localhost:5173`.

> **Windows PowerShell note:** if `npm run dev` fails because script execution is disabled, use `npm.cmd run dev` instead of `npm run dev`.

By default the frontend calls the API at `http://localhost:8080`. To point it at a different host/port, set `VITE_API_BASE_URL` before starting the dev server, e.g.:
```bash
VITE_API_BASE_URL=http://localhost:9090 npm run dev
```

3. Build for production:
```bash
npm run build
```

---

### 5. Running Tests

Unit and mock tests (JUnit 5 + Mockito) live in `test/`, mirroring the `src/` package structure. There's no Maven/Gradle — dependencies are plain jars and tests run via the JUnit console launcher.

1. One-time setup — download the JUnit5/Mockito jars into `lib/test/`:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\download-test-libs.ps1
```

2. Compile and run the full test suite from the repository root:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
```

This compiles `src/` into `src/out`, compiles `test/` into `testout`, then runs all tests with a tree-style report.

---

### 6. API Documentation, Health Check, and Scheduled Job

* **OpenAPI spec** — `src/openapi.yaml` documents every endpoint below. Once the server is running, fetch it live at `http://localhost:8080/openapi.yaml` and paste it into [Swagger Editor](https://editor.swagger.io) or a local Swagger UI to explore/try the API.
* **Health / readiness check** — `GET http://localhost:8080/health` checks database connectivity and returns `200 {"status":"UP","database":"UP"}` when the service can accept traffic, or `503 {"status":"DOWN", ...}` when the database is unreachable.
* **Scheduled document expiry check** — on server startup, a daemon background job runs automatically at **07:00 local time every day** (and every 24h afterwards), checking for documents expiring within 30 days and logging the result via `logback.xml`. No manual trigger is needed; check the server logs for entries from `service.DocumentExpiryScheduledJob`.

---

### 7. Calling the API (curl examples)

Full request/response reference for every endpoint (with sample JSON) is in [API_DOCUMENTATION.md](API_DOCUMENTATION.md). A few quick examples:

```bash
# List all clients
curl http://localhost:8080/api/clients

# Create a client
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{"full_name":"Jane Doe","client_type":"INDIVIDUAL","nationality":"GB","country_of_birth":"GB","date_of_birth":"1985-03-14","tax_residency":"GB","status":"PENDING","is_active":false}'
# -> {"message":"Client created successfully","client_id":11}

# Update a case status
curl -X PATCH http://localhost:8080/api/onboarding/cases/1/status \
  -H "Content-Type: application/json" \
  -d '{"case_status":"AWAITING_DOCUMENTS"}'
# -> {"message":"Case status updated successfully","case_id":1,"case_status":"AWAITING_DOCUMENTS"}
```



---

## 🌐 API Endpoints

Once running, the API server is available at:

| Method | Endpoint | Description |
| ------ | -------- | ----------- |
| `GET`  | `http://localhost:8080/api/clients` | Returns a summary list of all clients (`client_id`, `full_name`, `client_type`, `nationality`, `status`, `is_active`) |
| `GET`  | `http://localhost:8080/api/clients/{id}` | Returns the full record for a single client by ID |
| `GET`  | `http://localhost:8080/api/clients/expiring-documents?days={n}` | Returns documents expiring within the given day window (defaults to 30 days) |
| `POST` | `http://localhost:8080/api/clients` | Creates a new client. Body: `full_name`, `client_type`, `nationality`, `country_of_birth`, `date_of_birth`, `tax_residency`, `status`, `is_active` |
| `GET`  | `http://localhost:8080/api/onboarding/cases` | Returns a list of onboarding cases, optionally filtered with `?status={status}` |
| `GET`  | `http://localhost:8080/api/onboarding/cases/{id}` | Returns case details with client info and all submitted documents for the case |
| `POST` | `http://localhost:8080/api/onboarding/cases` | Opens a new onboarding case. Body: `client_id`, `product_type`, `case_status` |
| `PATCH` | `http://localhost:8080/api/onboarding/cases/{id}/status` | Updates the status of a case. Body: `case_status` |
| `POST` | `http://localhost:8080/api/onboarding/cases/{id}/documents` | Submits a new document for a case. Body: `doc_type_id` |
| `PATCH` | `http://localhost:8080/api/onboarding/cases/{id}/documents/{docId}/verify` | Marks a case document as verified |
| `GET`  | `http://localhost:8080/health` | Readiness check — reports whether the service and database can accept traffic |
| `GET`  | `http://localhost:8080/openapi.yaml` | Machine-readable OpenAPI 3.0 spec for all endpoints |
