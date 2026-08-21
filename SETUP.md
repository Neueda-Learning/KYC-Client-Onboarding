# KYC Client Onboarding System: Developer Onboarding Guide

Welcome to the **KYC Client Onboarding System** project! This guide will help you set up your local development environment, build the backend and database, and run the full stack in **under 10 minutes**.

---

## ⏱️ Quick Start Checklist (Under 10 Minutes)

1. **Prerequisites Verification** (~2 mins)
2. **Database Bootstrapping** (~2 mins)
3. **Backend API Server Compilation & Startup** (~2 mins)
4. **Running Automated Tests** (~2 mins)
5. **Frontend Development Server Startup** (~2 mins)

---

## 🛠️ Step 1: Prerequisites

Ensure you have the following installed on your machine:

* **MySQL 8.x** (Standalone MySQL Server 8.0 or XAMPP)
* **Java Development Kit (JDK 17+)**
* **Node.js & npm** (for the Vite + React frontend)
* **Git Bash** or any Unix-like terminal (recommended for Windows users)

---

## 🗄️ Step 2: MySQL Database Setup

1. If using **XAMPP**, configure your MySQL binary path in your terminal session:
```bash
export MYSQL_BIN="/c/xampp/mysql/bin"
```


2. (Optional) Set your credentials to bypass prompts:
```bash
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password
```


3. Run the automated bootstrap script to create the database (`kyc_db`), apply the DDL schema, load stored procedures/views, and seed initial test data:
```bash
chmod +x scripts/db/*.sh
./scripts/db/bootstrap_db.sh
```



---

## ☕ Step 3: Java API Server Startup

1. Navigate to the source directory:
```bash
cd src
```


2. Compile the Java server classes with all dependencies on the classpath:
```bash
javac -cp "lib/*" -d out $(find . -maxdepth 1 -name "*.java") $(find controller repository service util -name "*.java")
```


3. Start the API server (ensure your working directory contains `logback.xml`):
* **Windows (Git Bash):**
```bash
java -cp "out;.;lib/*" KycApiServer
```


* **Linux / macOS:**
```bash
java -cp "out:.:lib/*" KycApiServer
```




4. Verify the server is running by hitting the health check endpoint in a new terminal:
```bash
curl http://localhost:8080/health
```



---

## 🧪 Step 4: Running the Test Suite

Unit and mock tests (JUnit 5 + Mockito) verify the domain logic (such as risk classification rules and case state transitions).

1. Download test libraries (one-time setup):
```powershell
powershell -ExecutionPolicy Bypass -File scripts\download-test-libs.ps1
```


2. Run the test suite from the repository root:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
```



---

## 💻 Step 5: Frontend (React) Setup & Execution

1. Open a new terminal and navigate to the frontend directory:
```bash
cd frontend
```


2. Install dependencies (first time only):
```bash
npm install
```


3. Start the development server:
```bash
npm run dev
```


* *Note for Windows PowerShell users:* If script execution is restricted, use `npm.cmd run dev`.


4. Access the user interface in your browser at `http://localhost:5173`. It automatically communicates with the backend API running at `http://localhost:8080`.

---

## 📞 Quick API Verification (`curl`)

You can test core endpoints immediately after starting the backend:

* **List all clients:**
```bash
curl http://localhost:8080/api/clients
```


* **Check expiring documents:**
```bash
curl http://localhost:8080/api/clients/expiring-documents?days=30
```



