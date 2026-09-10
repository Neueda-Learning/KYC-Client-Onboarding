import base64
import io
import json
import logging
import os
from collections import Counter
from typing import List, Optional

import dice_ml
import joblib
import matplotlib
matplotlib.use("Agg")  # Non-interactive backend for server rendering
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
import shap
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ValidationError
from sklearn.metrics import confusion_matrix, roc_curve, roc_auc_score

logger = logging.getLogger(__name__)

# /predict-and-explain is called by the Java backend (service.MLPredictionService)
# on: case open, document submission, document verification. The /eval and
# /explain plot endpoints are called directly by the frontend admin dashboard.
# All paths are relative to this file's directory so it can be started from
# any working directory (see run instructions in README).
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.environ.get("KYC_MODEL_PATH", os.path.join(BASE_DIR, "kyc_model.pkl"))
TRAIN_DATA_PATH = os.environ.get("KYC_TRAIN_DATA_PATH", os.path.join(BASE_DIR, "kyc_train_data.pkl"))

# Must match the encodings used when training the model (see analysis.ipynb).
INCOME_MAP = {"<25K": 0, "25-50K": 1, "50-100K": 2, "100-250K": 3, "250K+": 4}
RISK_MAP = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}

# Only these fields are realistically actionable by a client trying to move
# from REJECTED to APPROVED (identity/demographic fields are locked out of DiCE).
ACTIONABLE_FEATURES = [
    "verified_docs_count",
    "expired_docs_count",
    "has_unverified_docs",
    "annual_income_band_encoded",
]

# Plain-language labels so dashboard plots read as business terms, not raw
# column/encoding names, for compliance officers who aren't data scientists.
FEATURE_LABELS = {
    "age": "Client age",
    "adverse_media_hits": "Adverse media hits",
    "is_pep": "Politically exposed person (PEP)",
    "is_cross_border": "Cross-border address",
    "total_docs_submitted": "Documents submitted",
    "verified_docs_count": "Verified documents",
    "expired_docs_count": "Expired documents",
    "has_unverified_docs": "Has unverified documents",
    "annual_income_band_encoded": "Declared income band",
    "jurisdiction_risk_encoded": "Jurisdiction risk level",
    "nationality": "Nationality",
    "product_type": "Product type",
    "client_type": "Client type",
    "main_source_of_funds": "Source of funds",
}


def _friendly_feature_name(name: str) -> str:
    if name in FEATURE_LABELS:
        return FEATURE_LABELS[name]
    base, _, suffix = name.rpartition("_")
    if base in FEATURE_LABELS and suffix:
        return f"{FEATURE_LABELS[base]}: {suffix}"
    return name.replace("_", " ").title()

app = FastAPI(title="KYC ML & XAI Engine")

# Allows the Vite dev server (frontend admin dashboard) to fetch plots directly.
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.environ.get("KYC_CORS_ORIGINS", "http://localhost:5173").split(","),
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

# Add exception handler for validation errors to log raw request details
@app.exception_handler(RequestValidationError)
async def validation_error_handler(request: Request, exc: RequestValidationError):
    try:
        body = await request.body()
        logger.error(f"RequestValidationError: {exc.errors()} | Raw body: {body.decode('utf-8')}")
    except Exception as e:
        logger.error(f"RequestValidationError: {exc.errors()} | Failed to read body: {e}")
    raise HTTPException(status_code=422, detail=exc.errors())

# Load artifacts
pipeline = joblib.load(MODEL_PATH)
clf = pipeline.named_steps["classifier"]
preprocessor = pipeline.named_steps["preprocessor"]

# Training data (raw, pre-preprocessing) is required by DiCE to know the valid
# range/categories for each feature. Saved alongside the model - see the
# "Persist artifacts for the FastAPI service" cell at the end of analysis.ipynb.
_train_bundle = joblib.load(TRAIN_DATA_PATH)
X_train = _train_bundle["X_train"]
y_train = _train_bundle["y_train"]
# Only present if the notebook's persist cell was re-run after adding them;
# powers the /eval/* plots below (older kyc_train_data.pkl files won't have these).
X_test = _train_bundle.get("X_test")
y_test = _train_bundle.get("y_test")

_dice_data = dice_ml.Data(
    dataframe=pd.concat([X_train, y_train], axis=1),
    continuous_features=[
        "age",
        "adverse_media_hits",
        "total_docs_submitted",
        "verified_docs_count",
        "expired_docs_count",
        "annual_income_band_encoded",
        "jurisdiction_risk_encoded",
    ],
    outcome_name="is_approved",
)
_dice_model = dice_ml.Model(model=pipeline, backend="sklearn")
_dice_explainer = dice_ml.Dice(_dice_data, _dice_model, method="random")


class CaseFeatures(BaseModel):
    client_type: Optional[str] = "INDIVIDUAL"
    nationality: Optional[str] = "GB"
    jurisdiction_risk: Optional[str] = "LOW"
    age: int = 30
    annual_income_band: Optional[str] = "50-100K"
    main_source_of_funds: Optional[str] = "Employment Income"
    is_pep: int = 0
    adverse_media_hits: int = 0
    is_cross_border: int = 0
    product_type: Optional[str] = "STANDARD"
    total_docs_submitted: int = 0
    verified_docs_count: int = 0
    expired_docs_count: int = 0
    has_unverified_docs: int = 0


class Recommendation(BaseModel):
    feature: str
    current_value: str
    suggested_value: str


class PredictionResponse(BaseModel):
    decision: str
    approval_probability: float
    # Only populated when decision == "REJECTED".
    recommendations: Optional[List[Recommendation]] = None


class BatchPredictionRequest(BaseModel):
    cases: List[CaseFeatures]
    # DiCE recommendations are the expensive part (one counterfactual search
    # per REJECTED case) - off by default so large batches (thousands of
    # cases) stay fast. Enable only for smaller batches that need them.
    include_recommendations: bool = False


class BatchPredictionResponse(BaseModel):
    results: List[PredictionResponse]


def _encode(features: CaseFeatures) -> pd.DataFrame:
    row = features.model_dump() if hasattr(features, "model_dump") else features.dict()
    row["annual_income_band_encoded"] = INCOME_MAP.get(row.pop("annual_income_band") or "50-100K", 1)
    row["jurisdiction_risk_encoded"] = RISK_MAP.get(row.pop("jurisdiction_risk") or "LOW", 0)
    # Ensure fallback strings for LightGBM/DiCE
    row["client_type"] = row["client_type"] or "INDIVIDUAL"
    row["nationality"] = row["nationality"] or "GB"
    row["main_source_of_funds"] = row["main_source_of_funds"] or "Employment Income"
    row["product_type"] = row["product_type"] or "STANDARD"
    return pd.DataFrame([row])[list(X_train.columns)]

def plot_to_base64(fig) -> str:
    buf = io.BytesIO()
    fig.savefig(buf, format="png", bbox_inches="tight")
    buf.seek(0)
    img_str = base64.b64encode(buf.read()).decode("utf-8")
    plt.close(fig)
    return f"data:image/png;base64,{img_str}"


def _generate_recommendations(df_raw: pd.DataFrame) -> Optional[List[Recommendation]]:
    """Uses DiCE to find the smallest change to client-actionable fields
    (document verification/expiry, income evidence) that flips the model's
    prediction from REJECTED to APPROVED."""
    try:
        logger.info("Generating DiCE recommendations for REJECTED case")
        cf = _dice_explainer.generate_counterfactuals(
            df_raw,
            total_CFs=3,
            desired_class=1,
            features_to_vary=ACTIONABLE_FEATURES,
        )
        cf_df = cf.cf_examples_list[0].final_cfs_df
        if cf_df is None or cf_df.empty:
            logger.info("DiCE returned no counterfactuals (case may already be at limit)")
            return None
        original = df_raw.iloc[0]
        best = cf_df.iloc[0]
        recommendations = [
            Recommendation(feature=col, current_value=str(original[col]), suggested_value=str(best[col]))
            for col in ACTIONABLE_FEATURES
            if col in best and str(best[col]) != str(original[col])
        ]
        logger.info(f"Generated {len(recommendations)} recommendations: {recommendations}")
        return recommendations or None
    except Exception as e:
        # DiCE can fail to find a feasible counterfactual (e.g. already at the
        # limit of actionable fields) - treat as "no recommendation available".
        logger.warning(f"DiCE recommendation generation failed: {e}")
        return None


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/api/v1/debug/echo")
async def debug_echo(request: Request):
    """Debug endpoint to echo back raw request body and headers"""
    try:
        body = await request.body()
        body_str = body.decode('utf-8')
        headers = dict(request.headers)
        logger.info(f"DEBUG ECHO - Headers: {headers}")
        logger.info(f"DEBUG ECHO - Body: {body_str}")
        return {
            "headers": headers,
            "body": body_str,
            "body_length": len(body),
            "message": "Raw request echoed for debugging"
        }
    except Exception as e:
        logger.error(f"Debug echo error: {e}")
        return {"error": str(e)}


@app.post("/api/v1/predict-and-explain", response_model=PredictionResponse)
def predict_and_explain(features: CaseFeatures):
    try:
        df_raw = _encode(features)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid input: {exc}")

    pred = int(pipeline.predict(df_raw)[0])
    proba = float(pipeline.predict_proba(df_raw)[0][1])
    decision = "APPROVED" if pred == 1 else "REJECTED"

    recommendations = _generate_recommendations(df_raw) if decision == "REJECTED" else None
    
    logger.info(f"Prediction result: decision={decision}, proba={proba:.4f}, has_recommendations={recommendations is not None}")
    if recommendations:
        logger.info(f"Recommendations: {[{'feature': r.feature, 'current': r.current_value, 'suggested': r.suggested_value} for r in recommendations]}")

    return PredictionResponse(
        decision=decision,
        approval_probability=round(proba, 4),
        recommendations=recommendations,
    )


@app.post("/api/v1/predict-and-explain-batch", response_model=BatchPredictionResponse)
def predict_and_explain_batch(request: BatchPredictionRequest):
    """Scores many cases in one call via a single vectorized pipeline.predict
    (not one HTTP round-trip + model call per case), for bulk jobs like
    scoring the full 9-10k row sandbox dataset. See docs/README for chunking
    guidance - keep include_recommendations off for large batches."""
    if not request.cases:
        return BatchPredictionResponse(results=[])

    try:
        df_raw = pd.concat([_encode(c) for c in request.cases], ignore_index=True)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid input: {exc}")

    preds = pipeline.predict(df_raw)
    probas = pipeline.predict_proba(df_raw)[:, 1]

    results = []
    for i in range(len(df_raw)):
        decision = "APPROVED" if int(preds[i]) == 1 else "REJECTED"
        recommendations = None
        if decision == "REJECTED" and request.include_recommendations:
            recommendations = _generate_recommendations(df_raw.iloc[[i]])
        results.append(PredictionResponse(
            decision=decision,
            approval_probability=round(float(probas[i]), 4),
            recommendations=recommendations,
        ))

    logger.info(f"Batch prediction: {len(results)} cases scored, "
                f"include_recommendations={request.include_recommendations}")
    return BatchPredictionResponse(results=results)


@app.post("/api/v1/explain/shap-plot")
def shap_plot(features: CaseFeatures):
    """Optional debugging endpoint - returns a base64 PNG of the local SHAP
    bar plot for a single case. Not called by the Java backend."""
    df_raw = _encode(features)
    transformed = preprocessor.transform(df_raw)
    feat_names = preprocessor.get_feature_names_out()
    explainer = shap.TreeExplainer(clf)
    shap_values = explainer.shap_values(transformed)
    shap_target = shap_values[1] if isinstance(shap_values, list) else shap_values

    fig, ax = plt.subplots(figsize=(8, 4))
    shap.bar_plot(shap_target[0], feature_names=feat_names, max_display=6, show=False)
    return {"shap_waterfall_plot": plot_to_base64(fig)}


@app.get("/api/v1/explain/feature-importance-plot")
def feature_importance_plot():
    """Debugging endpoint mirroring the "Top N Features by Information Gain"
    bar plot from analysis.ipynb. Not called by the Java backend."""
    raw_names = preprocessor.get_feature_names_out()
    clean_names = [_friendly_feature_name(name.split("__")[-1]) for name in raw_names]
    importances = clf.booster_.feature_importance(importance_type="gain")

    importance_df = (
        pd.DataFrame({"Feature": clean_names, "Gain": importances})
        .sort_values(by="Gain", ascending=False)
        .head(10)
    )

    fig, ax = plt.subplots(figsize=(10, 6))
    sns.barplot(data=importance_df, x="Gain", y="Feature", hue="Feature", legend=False, palette="viridis", ax=ax)
    ax.set_title("What Drives Approval Decisions Most")
    ax.set_xlabel("Relative influence on the decision")
    ax.set_ylabel("")
    fig.tight_layout()
    return {"feature_importance_plot": plot_to_base64(fig)}


@app.get("/api/v1/eval/confusion-matrix-plot")
def confusion_matrix_plot():
    """Debugging endpoint mirroring the confusion matrix plot from
    analysis.ipynb, computed on the held-out test split. Not called by the
    Java backend."""
    if X_test is None or y_test is None:
        raise HTTPException(status_code=404, detail="No test data persisted in kyc_train_data.pkl")

    y_pred = pipeline.predict(X_test)
    cm = confusion_matrix(y_test, y_pred)

    fig, ax = plt.subplots(figsize=(7, 6))
    sns.heatmap(
        cm,
        annot=True,
        fmt="d",
        cmap="Blues",
        xticklabels=["Rejected", "Approved"],
        yticklabels=["Rejected", "Approved"],
        ax=ax,
    )
    ax.set_title("How Often the AI Gets It Right")
    ax.set_ylabel("Actual outcome")
    ax.set_xlabel("AI decision")
    fig.tight_layout()
    return {"confusion_matrix_plot": plot_to_base64(fig)}


@app.get("/api/v1/eval/roc-curve-plot")
def roc_curve_plot():
    """Debugging endpoint mirroring the ROC curve plot from analysis.ipynb,
    computed on the held-out test split. Not called by the Java backend."""
    if X_test is None or y_test is None:
        raise HTTPException(status_code=404, detail="No test data persisted in kyc_train_data.pkl")

    y_proba = pipeline.predict_proba(X_test)[:, 1]
    fpr, tpr, _ = roc_curve(y_test, y_proba)
    roc_auc = roc_auc_score(y_test, y_proba)

    fig, ax = plt.subplots(figsize=(7, 6))
    ax.plot(fpr, tpr, color="darkorange", lw=2, label=f"Overall reliability score: {roc_auc:.0%}")
    ax.plot([0, 1], [0, 1], color="navy", linestyle="--", label="No better than a coin flip")
    ax.set_xlim([0.0, 1.0])
    ax.set_ylim([0.0, 1.05])
    ax.set_xlabel("Rate of wrongly approving risky clients")
    ax.set_ylabel("Rate of correctly approving good clients")
    ax.set_title("How Reliable Is the Model")
    ax.legend(loc="lower right")
    fig.tight_layout()
    return {"roc_curve_plot": plot_to_base64(fig)}


@app.get("/api/v1/eval/approval-rate-plot")
def approval_rate_plot():
    """Bar chart of historical APPROVED vs REJECTED case counts, across
    whatever labelled data is available (train + test if persisted)."""
    y_all = pd.concat([y_train, y_test]) if y_test is not None else y_train
    counts = y_all.value_counts().reindex([0, 1]).fillna(0)
    total = counts.sum()

    fig, ax = plt.subplots(figsize=(8, 6))
    bars = ax.bar(["Rejected", "Approved"], counts.values, color=["#ef4444", "#22c55e"])
    for bar, value in zip(bars, counts.values):
        pct = (value / total * 100) if total else 0
        ax.text(bar.get_x() + bar.get_width() / 2, value, f"{int(value)} ({pct:.1f}%)", ha="center", va="bottom", fontweight="bold")
    ax.set_title("Overall Approval Rate (Historical Cases)")
    ax.set_ylabel("Number of cases")
    fig.tight_layout()
    return {"approval_rate_plot": plot_to_base64(fig)}


@app.get("/api/v1/eval/probability-distribution-plot")
def probability_distribution_plot():
    """Histogram of the AI's predicted approval probability, showing how
    confident it typically is (used on the test split if available)."""
    data_X = X_test if X_test is not None else X_train
    proba = pipeline.predict_proba(data_X)[:, 1]

    fig, ax = plt.subplots(figsize=(9, 6))
    ax.hist(proba, bins=20, color="#3b82f6", edgecolor="white")
    ax.axvline(0.5, color="#ef4444", linestyle="--", label="Approve / reject cut-off")
    ax.set_title("How Confident Is the AI in Its Decisions")
    ax.set_xlabel("Predicted chance of approval")
    ax.set_ylabel("Number of cases")
    ax.legend()
    fig.tight_layout()
    return {"probability_distribution_plot": plot_to_base64(fig)}


@app.get("/api/v1/eval/top-rejection-factors-plot")
def top_rejection_factors_plot(sample_size: int = 15):
    """Runs DiCE over a sample of rejected test-split cases and tallies which
    actionable fields most often needed to change to flip the decision -
    i.e. the most common real-world reasons behind rejections."""
    if X_test is None or y_test is None:
        raise HTTPException(status_code=404, detail="No test data persisted in kyc_train_data.pkl")

    rejected_idx = y_test[y_test == 0].index[:sample_size]
    if len(rejected_idx) == 0:
        raise HTTPException(status_code=404, detail="No rejected cases found in test data")

    counter = Counter()
    for idx in rejected_idx:
        recs = _generate_recommendations(X_test.loc[[idx]])
        if recs:
            counter.update(r.feature for r in recs)

    if not counter:
        raise HTTPException(status_code=404, detail="No actionable recommendations found for sampled cases")

    items = counter.most_common()
    labels = [_friendly_feature_name(name) for name, _ in items]
    values = [count for _, count in items]

    fig, ax = plt.subplots(figsize=(9, 6))
    sns.barplot(x=values, y=labels, hue=labels, legend=False, palette="rocket", ax=ax)
    ax.set_title("Most Common Reasons Behind Rejections")
    ax.set_xlabel(f"Rejected cases affected (of {len(rejected_idx)} sampled)")
    ax.set_ylabel("")
    fig.tight_layout()
    return {"top_rejection_factors_plot": plot_to_base64(fig)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)