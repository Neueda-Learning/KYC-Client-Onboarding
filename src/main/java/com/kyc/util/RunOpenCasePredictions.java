package util;

import service.MLPredictionService;

/**
 * One-shot CLI entry point that predicts every non-CLOSED case. Intended to
 * be run once after seeding the database (see scripts/db/bootstrap_db.sh),
 * NOT on every app start - so ML predictions show up immediately for freshly
 * seeded data without needing a case-mutating action. Requires the FastAPI
 * ML service (data_analysis/app.py) to already be running.
 */
public final class RunOpenCasePredictions {

    private RunOpenCasePredictions() {
    }

    public static void main(String[] args) {
        new MLPredictionService().predictOpenCases();
    }
}
