# KYC Client Onboarding System

System for managing client identity verification processes (Know Your Customer - KYC). The project includes a MySQL relational database structure, a set of administrative automation scripts, and a relay server (Relay API) written in Java.

---

## 📁 Project Structure


PROJECT-KYC/
├── scripts/
│   └── db/
│       ├── db_dump.sh               # Database backup script
│       ├── db_reload.sh             # Script for restoring the database from a .sql file
│       ├── expiring_docs_report.sh  # Script generating a report of expiring documents
│       ├── expiring_docs_report.csv # Generated expiring documents report
│       ├── kyc_db_backup.sql        # Database backup file
│       └── rebuild_indexes.sh       # Script for optimizing and rebuilding indexes
├── src/
│   ├── lib/
│   │   └── mysql-connector-j-8.3.0.jar # MySQL JDBC Driver
│   └── KycApiServer.java            # Lightweight HTTP server (Java Relay API)
├── 01_seed_data.sql                 # Script with test data (10 clients)
├── create_database.sql              # Database creation (kyc_db)
├── ddl_schema.sql                   # Schema for tables, keys, and relationships
├── select_all_clients.sql           # Auxiliary query for client verification
├── views_stored_procedures.sql      # SQL views and stored procedures
├── EDB Diagram.pdf                  # ERD diagram of the database
├── Project-Brief-02-KYC-...pdf      # Business requirements documentation
└── README.md                        # Project documentation


---

## 🛠️ Prerequisites

* **MySQL Server 8.x** (running on port `3306`)
* **Java Development Kit (JDK 17+)**
* **Git Bash** or any Unix-like terminal

---

## 🚀 Setup and Execution Instructions

### 1. MySQL Database Setup

Execute the SQL scripts in the following order to create and seed the database:

```bash
# 1. Create the database
mysql -u root -p < create_database.sql

# 2. Create tables and relationships
mysql -u root -p kyc_db < ddl_schema.sql

# 3. Add views and stored procedures
mysql -u root -p kyc_db < views_stored_procedures.sql

# 4. Seed database with test data
mysql -u root -p kyc_db < 01_seed_data.sql

```

---

### 2. Administrative Automation Scripts

The scripts are located in the `scripts/db/` directory. Before executing them, grant execution permissions:

```bash
chmod +x scripts/db/*.sh

```

* **Database Backup:**
```bash
./scripts/db/db_dump.sh

```


* **Database Restore:**
```bash
./scripts/db/db_reload.sh

```


* **Generate Expiring Documents CSV Report:**
```bash
./scripts/db/expiring_docs_report.sh

```


* **Database Index Optimization:**
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