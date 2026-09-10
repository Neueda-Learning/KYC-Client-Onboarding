# KYC Client Onboarding API — Reference Guide

Full request/response reference for every endpoint exposed by the Java relay server
([KycApiServer.java](src/KycApiServer.java)) plus the Python ML/XAI microservice
([data_analysis/app.py](data_analysis/app.py)). See [openapi.yaml](src/openapi.yaml) for the
Java relay server's machine-readable spec (also served live at `GET /openapi.yaml`).

All examples assume the Java server is running locally on port `8080` (see README section 3)
unless noted otherwise; the ML microservice section below uses port `8000`.

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

### `PATCH /api/clients/{id}/status`

Updates a client's onboarding status. Used by officers/admins from the case view to
approve or reject a client on top of the case's own status.

```bash
curl -X PATCH http://localhost:8080/api/clients/11/status \
  -H "Content-Type: application/json" \
  -d '{"status": "APPROVED"}'
```

```json
{"message":"Client status updated successfully","client_id":11,"status":"APPROVED"}
```

Invalid status, `400`:
```json
{"error":"Invalid client status: FOO"}
```

Not found, `404`:
```json
{"error":"Client not found"}
```

---

## Onboarding Cases

### `GET /api/onboarding/cases?status={status}&assigned_officer_id={id}&limit={n}&offset={n}`

All query parameters are optional. `status` accepts a comma-separated list (OR'd together).
`limit` defaults to 50 and is capped at 500; `offset` defaults to 0.

```bash
curl "http://localhost:8080/api/onboarding/cases?status=IN_REVIEW&limit=20&offset=0"
```

```json
{
  "total": 1,
  "offset": 0,
  "limit": 20,
  "cases": [
    {"case_id":1,"client_id":1,"client_name":"Jane Doe","client_type":"INDIVIDUAL","client_status":"ACTIVE","product_type":"CURRENT_ACCOUNT","case_status":"IN_REVIEW","opened_date":"2026-08-01","due_date":"2026-08-31","assigned_officer_id":2,"officer_name":"Grace Whitman","ml_prediction":"APPROVED"}
  ]
}
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
  "client_status":"ACTIVE",
  "product_type":"CURRENT_ACCOUNT",
  "case_status":"IN_REVIEW",
  "opened_date":"2026-08-01 09:00:00",
  "due_date":"2026-08-31",
  "completed_date":null,
  "rejection_reason":null,
  "assigned_officer_id":2,
  "officer_name":"Grace Whitman",
  "date_of_birth":"1985-03-14",
  "country_of_birth":"GB",
  "nationality":"GB",
  "tax_residency":"GB",
  "occupation":"Engineer",
  "employer":"Acme Ltd",
  "main_source_of_funds":"SALARY",
  "annual_income_band":"50-100K",
  "ml_prediction":"APPROVED",
  "ml_approval_probability":0.8734,
  "ml_recommendations":null,
  "ml_predicted_at":"2026-08-01 09:05:00",
  "documents":[
    {"doc_id":7,"doc_type":"PASSPORT","submission_date":"2026-08-01","verified":true,"expiry_date":"2026-09-01","rejection_reason":null}
  ]
}
```

`ml_prediction`/`ml_approval_probability`/`ml_recommendations`/`ml_predicted_at` are populated by
[MLPredictionService](src/service/MLPredictionService.java) calling the Python ML microservice (see
[below](#ml--xai-microservice-python-fastapi)) on case open, document submission, and document
verification, plus at DB bootstrap time for any non-CLOSED case
(`util.RunOpenCasePredictions`, see [README.md](README.md)). All four are `null` until a
prediction has run. `ml_recommendations` is a raw JSON array of
`{feature, current_value, suggested_value}` objects, only populated when `ml_prediction` is
`"REJECTED"`.

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

### `POST /api/onboarding/cases/predict-all`

Starts a background thread that runs ML prediction for every case in the system and returns
immediately (`202`); check server logs for progress/completion
([MLPredictionService.predictAllCases](src/service/MLPredictionService.java)).

```bash
curl -X POST http://localhost:8080/api/onboarding/cases/predict-all
```

```json
{"message":"Bulk ML prediction started in background","status":"processing"}
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

---

## ML / XAI Microservice (Python FastAPI)

A separate service, [data_analysis/app.py](data_analysis/app.py), runs on port `8000` and is
**not** part of the Java relay server. The Java backend calls the prediction endpoint
server-to-server (`ML_SERVICE_URL` env var, default `http://localhost:8000/api/v1/predict-and-explain`);
the frontend admin dashboard calls the plot endpoints directly from the browser
(`VITE_ML_SERVICE_URL`, default `http://localhost:8000`). All examples assume it's running locally
on port `8000`. See [SETUP.md](SETUP.md) for how to install/train/start it.

### `GET /health`

```bash
curl http://localhost:8000/health
```
```json
{"status":"ok"}
```

### `POST /api/v1/predict-and-explain`

Predicts APPROVED/REJECTED for a case's raw features and, when REJECTED, generates DiCE
counterfactual recommendations over the client-actionable fields
(`verified_docs_count`, `expired_docs_count`, `has_unverified_docs`, `annual_income_band_encoded`).
Called by `MLPredictionService`, not directly by the frontend.

```bash
curl -X POST http://localhost:8000/api/v1/predict-and-explain \
  -H "Content-Type: application/json" \
  -d '{"client_type":"INDIVIDUAL","nationality":"GB","jurisdiction_risk":"LOW","age":30,
       "annual_income_band":"50-100K","main_source_of_funds":"Employment Income","is_pep":0,
       "adverse_media_hits":0,"is_cross_border":0,"product_type":"STANDARD",
       "total_docs_submitted":2,"verified_docs_count":1,"expired_docs_count":0,"has_unverified_docs":1}'
```

```json
{"decision":"REJECTED","approval_probability":0.3421,
 "recommendations":[{"feature":"verified_docs_count","current_value":"1","suggested_value":"2"}]}
```

### `POST /api/v1/predict-and-explain-batch`

Scores many cases in a single request via one vectorized `pipeline.predict` call, instead of one
HTTP round-trip + model call per case. Use this instead of looping `/predict-and-explain` when
scoring thousands of cases (e.g. the 9-10k row sandbox dataset) — a single-case-at-a-time loop
over that many rows is dominated by per-request overhead and (if recommendations are enabled)
DiCE counterfactual search. `recommendations` are generated per REJECTED case and are the
expensive part, so `include_recommendations` defaults to `false`; leave it off for large batches
and only enable it for smaller ones. For very large datasets, chunk client-side (e.g. 500-1000
cases per request) to keep request/response payload size reasonable.

```bash
curl -X POST http://localhost:8000/api/v1/predict-and-explain-batch \
  -H "Content-Type: application/json" \
  -d '{"cases": [
        {"client_type":"INDIVIDUAL","nationality":"GB","jurisdiction_risk":"LOW","age":30,
         "annual_income_band":"50-100K","main_source_of_funds":"Employment Income","is_pep":0,
         "adverse_media_hits":0,"is_cross_border":0,"product_type":"STANDARD",
         "total_docs_submitted":2,"verified_docs_count":1,"expired_docs_count":0,"has_unverified_docs":1}
      ],
      "include_recommendations": false}'
```

```json
{"results":[{"decision":"REJECTED","approval_probability":0.3421,"recommendations":null}]}
```

### `POST /api/v1/explain/shap-plot`

Debug endpoint - returns a base64 PNG of the local SHAP bar plot explaining a single case's
prediction. Same request body as `/predict-and-explain`.

```json
{"shap_waterfall_plot":"data:image/png;base64,iVBORw0KG..."}
```

### `GET /api/v1/explain/feature-importance-plot`

Bar plot (base64 PNG) of the top features driving approval decisions, by LightGBM information
gain. Used by the admin dashboard's "What drives approval decisions" card.
```json
{"feature_importance_plot":"data:image/png;base64,iVBORw0KG..."}
```

### `GET /api/v1/eval/confusion-matrix-plot`

Confusion matrix (base64 PNG) computed on the held-out test split. Requires `X_test`/`y_test`
to have been persisted in `kyc_train_data.pkl` (re-run `analysis.ipynb`'s last cell if this 404s).
```json
{"confusion_matrix_plot":"data:image/png;base64,iVBORw0KG..."}
```

### `GET /api/v1/eval/roc-curve-plot`

ROC curve (base64 PNG) on the held-out test split. Same `X_test`/`y_test` requirement as above.
```json
{"roc_curve_plot":"data:image/png;base64,iVBORw0KG..."}
```

### `GET /api/v1/eval/approval-rate-plot`

Bar chart (base64 PNG) of historical approved-vs-rejected case counts.
```json
{"approval_rate_plot":"data:image/png;base64,iVBORw0KG..."}
```

### `GET /api/v1/eval/probability-distribution-plot`

Histogram (base64 PNG) of the model's predicted approval probability across the test split
(or training split if no test split is persisted).
```json
{"probability_distribution_plot":"data:image/png;base64,iVBORw0KG..."}
```

### `GET /api/v1/eval/top-rejection-factors-plot?sample_size={n}`

Runs DiCE over up to `sample_size` (default 15) rejected test-split cases and tallies which
actionable fields most often needed to change to flip the decision. Requires `X_test`/`y_test`.
```json
{"top_rejection_factors_plot":"data:image/png;base64,iVBORw0KG..."}
```

### `POST /api/v1/debug/echo`

Debug-only endpoint that echoes back the raw request body and headers, used to troubleshoot
malformed requests. Not called by the Java backend or frontend.

