# KYC Client Onboarding API — Reference Guide

Full request/response reference for every endpoint exposed by the Java relay server
([KycApiServer.java](src/KycApiServer.java)). See [openapi.yaml](src/openapi.yaml) for the
machine-readable spec (also served live at `GET /openapi.yaml`).

All examples assume the server is running locally on port `8080` (see README section 3).

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
