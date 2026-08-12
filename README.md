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
│   │   └── mysql-connector-j-8.3.0.jar # MySQL JDBC Driver
│   └── KycApiServer.java            # Lightweight HTTP server (Java Relay API)
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


2. Compile the Java server with the JDBC driver on the classpath:
```bash
javac -cp "lib/mysql-connector-j-8.3.0.jar" KycApiServer.java

```


3. Run the API server:
* **In Windows environment (Git Bash):**
```bash
java -cp ".;lib/mysql-connector-j-8.3.0.jar" KycApiServer

```


* **In Linux / macOS environment:**
```bash
java -cp ".:lib/mysql-connector-j-8.3.0.jar" KycApiServer

```


---

## 🌐 API Endpoints

Once running, the API server is available at:

| Method | Endpoint | Description |
| ------ | -------- | ----------- |
| `GET`  | `http://localhost:8080/api/clients` | Returns a list of all clients in JSON format |