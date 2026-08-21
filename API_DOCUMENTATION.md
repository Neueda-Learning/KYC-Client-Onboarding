# KYC Client Onboarding API — Reference Guide

Full request/response reference for every endpoint exposed by the Java relay server
([KycApiServer.java](src/KycApiServer.java)). See [openapi.yaml](src/openapi.yaml) for the
machine-readable spec (also served live at `GET /openapi.yaml`).

All examples assume the server is running locally on port `8080` (see README section 3).

---

## Auth

### `POST /api/auth/login`

Checks the username/password against the `client`, `compliance_officer` and `admin_officer`
tables (in that order) and returns the matched role and entity id. Passwords are verified
against salted PBKDF2-HMAC-SHA256 hashes (see [PasswordHasher.java](src/util/PasswordHasher.java)).

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "michael.brown", "password": "Br0wn#Falcon91"}'
```

```json
{"role":"CLIENT","entity_id":1,"full_name":"Michael Brown","username":"michael.brown"}
```

Invalid credentials, `401` (deliberately generic — doesn't reveal whether the username exists):
```json
{"error":"Invalid username or password"}
```

Missing fields, `400`:
```json
{"error":"Missing required fields: username, password"}
```

---

## System

### `GET /health`

Readiness check — verifies the service can reach the database.

```bash
curl -i http://localhost:8080/health
```

```json
{"status":"UP","database":"UP"}
```

If the database is unreachable, responds `503`:
```json
{"status":"DOWN","database":"DOWN","error":"Connection refused"}
```

### `GET /openapi.yaml`

Returns the OpenAPI 3.0 spec (YAML) for all endpoints.

```bash
curl http://localhost:8080/openapi.yaml
```

---

## Clients

### `GET /api/clients`

```bash
curl http://localhost:8080/api/clients
```

```json
[
  {"client_id":1,"full_name":"Jane Doe","client_type":"INDIVIDUAL","nationality":"GB","status":"ACTIVE","is_active":true},
  {"client_id":2,"full_name":"Acme Ltd","client_type":"CORPORATE","nationality":"GB","status":"PENDING","is_active":false}
]
```

### `GET /api/clients/{id}`

```bash
curl http://localhost:8080/api/clients/1
```

```json
{
  "client_id":1,
  "full_name":"Jane Doe",
  "client_type":"INDIVIDUAL",
  "nationality":"GB",
  "date_of_birth":"1985-03-14",
  "country_of_birth":"GB",
  "tax_residency":"GB",
  "occupation":"Engineer",
  "employer":"Acme Ltd",
  "main_source_of_funds":"SALARY",
  "annual_income_band":"50-100K",
  "status":"ACTIVE",
  "is_active":true
}
```

Not found:
```json
{"error":"Client not found"}
```

### `GET /api/clients/expiring-documents?days={n}`

```bash
curl "http://localhost:8080/api/clients/expiring-documents?days=30"
```

```json
[
  {"client_id":1,"full_name":"Jane Doe","client_type":"INDIVIDUAL","doc_id":7,"doc_type":"PASSPORT","expiry_date":"2026-09-01"}
]
```

### `POST /api/clients`

```bash
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{
    "full_name": "Jane Doe",
    "client_type": "INDIVIDUAL",
    "nationality": "GB",
    "country_of_birth": "GB",
    "date_of_birth": "1985-03-14",
    "tax_residency": "GB",
    "status": "PENDING",
    "is_active": false
  }'
```

```json
{"message":"Client created successfully","client_id":11}
```

Missing fields:
```json
{"error":"Missing required fields: full_name, client_type, nationality, country_of_birth, date_of_birth, tax_residency, status, is_active"}
```

---

## Onboarding Cases

### `GET /api/onboarding/cases?status={status}`

```bash
curl "http://localhost:8080/api/onboarding/cases?status=IN_REVIEW"
```

```json
[
  {"case_id":1,"client_id":1,"client_name":"Jane Doe","client_type":"INDIVIDUAL","product_type":"CURRENT_ACCOUNT","case_status":"IN_REVIEW","opened_date":"2026-08-01 09:00:00"}
]
```

### `GET /api/onboarding/cases/{id}`

```bash
curl http://localhost:8080/api/onboarding/cases/1
```

```json
{
  "case_id":1,
  "client_id":1,
  "client_name":"Jane Doe",
  "client_type":"INDIVIDUAL",
  "product_type":"CURRENT_ACCOUNT",
  "case_status":"IN_REVIEW",
  "opened_date":"2026-08-01 09:00:00",
  "due_date":"2026-08-31",
  "completed_date":null,
  "rejection_reason":null,
  "documents":[
    {"doc_id":7,"doc_type":"PASSPORT","submission_date":"2026-08-01","verified":true,"expiry_date":"2026-09-01","rejection_reason":null}
  ]
}
```

### `POST /api/onboarding/cases`

```bash
curl -X POST http://localhost:8080/api/onboarding/cases \
  -H "Content-Type: application/json" \
  -d '{"client_id": 1, "product_type": "CURRENT_ACCOUNT", "case_status": "OPEN"}'
```

```json
{"message":"Onboarding case opened successfully","case_id":12}
```

### `POST /api/onboarding/cases/open`

Creates a new client, address, and onboarding case in one atomic operation, optionally
recording documents already provided and assigning a compliance officer. Also
automatically provisions the client's login: a username derived from `full_name`
(lowercased, spaces replaced with dots, e.g. `"Jane Doe"` -> `jane.doe`) and a
randomly generated temporary password. Only the password's PBKDF2 hash is stored;
delivery of the credentials to the client is currently a logging stub
(`service.NotificationService`) pending a real email/SMS integration.

```bash
curl -X POST http://localhost:8080/api/onboarding/cases/open \
  -H "Content-Type: application/json" \
  -d '{
        "client": {
          "full_name": "Jane Doe",
          "client_type": "INDIVIDUAL",
          "nationality": "GB",
          "date_of_birth": "1990-01-01",
          "country_of_birth": "GB",
          "tax_residency": "GB"
        },
        "address": {
          "address_type": "REGISTERED",
          "line1": "1 Example Street",
          "city": "London",
          "country": "GB",
          "postcode": "SW1A 1AA"
        },
        "product_type": "CURRENT_ACCOUNT",
        "due_date": "2026-09-01",
        "officer_id": 2,
        "document_type_ids": [1, 4]
      }'
```

```json
{"message":"Case opened successfully","case_id":13,"client_id":16}
```

### `PATCH /api/onboarding/cases/{id}/status`

```bash
curl -X PATCH http://localhost:8080/api/onboarding/cases/1/status \
  -H "Content-Type: application/json" \
  -d '{"case_status": "AWAITING_DOCUMENTS"}'
```

```json
{"message":"Case status updated successfully","case_id":1,"case_status":"AWAITING_DOCUMENTS"}
```

Disallowed transition (e.g. approving with unverified documents), returns `409`:
```json
{"error":"Case 1 has unverified documents and cannot be approved"}
```

### `POST /api/onboarding/cases/{id}/documents`

```bash
curl -X POST http://localhost:8080/api/onboarding/cases/1/documents \
  -H "Content-Type: application/json" \
  -d '{"doc_type_id": 3}'
```

```json
{"message":"Document submitted successfully","doc_id":15}
```

### `PATCH /api/onboarding/cases/{id}/documents/{docId}/verify`

```bash
curl -X PATCH http://localhost:8080/api/onboarding/cases/1/documents/15/verify
```

```json
{"message":"Document verified successfully","doc_id":15}
```

Not found:
```json
{"error":"Document not found or does not match the case"}
```

### `PATCH /api/onboarding/cases/{id}/officer`

Assigns (or unassigns, when `officer_id` is `null`) the compliance officer handling a case.

```bash
curl -X PATCH http://localhost:8080/api/onboarding/cases/1/officer \
  -H "Content-Type: application/json" \
  -d '{"officer_id": 2}'
```

```json
{"message":"Case officer assigned successfully","case_id":1,"assigned_officer_id":2,"officer_name":"Grace Whitman"}
```

Not found, `404`:
```json
{"error":"Case not found"}
```

### `PATCH /api/onboarding/cases/{id}/risk-classification`

Records a new risk classification for a case. The next review date is derived from the
risk level (90/180/365 days out for LOW/MEDIUM/HIGH respectively). A risk classification
must exist before a case can be moved to `APPROVED` or `REJECTED`.

```bash
curl -X PATCH http://localhost:8080/api/onboarding/cases/1/risk-classification \
  -H "Content-Type: application/json" \
  -d '{"risk_level": "MEDIUM", "rationale": "Politically exposed connection", "officer_id": 2}'
```

```json
{"message":"Risk classification updated successfully","case_id":1}
```

Missing/invalid fields, `400`:
```json
{"error":"Missing required fields: risk_level, rationale"}
```

---

## Officers

### `GET /api/officers`

```bash
curl http://localhost:8080/api/officers
```

```json
[
  {"officer_id":1,"full_name":"Alan Turing"},
  {"officer_id":2,"full_name":"Grace Whitman"}
]
```

---

## Document Types

### `GET /api/document-types`

```bash
curl http://localhost:8080/api/document-types
```

```json
[
  {"doc_type_id":1,"doc_type_name":"PASSPORT"},
  {"doc_type_id":2,"doc_type_name":"UTILITY_BILL"}
]
```

