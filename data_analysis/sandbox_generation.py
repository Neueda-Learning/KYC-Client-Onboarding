"""Script version of sandbox_generation.ipynb so it can be run headlessly by
scripts/db/bootstrap_db.sh (no jupyter/nbconvert dependency needed).

Generates a large synthetic KYC dataset (client/address/case/document/risk
classification), writes it to CSV files alongside this script, then loads it
into MySQL - TRUNCATING those 5 tables first. Run standalone:
    python sandbox_generation.py
"""
import csv
import os
import random
from datetime import date, timedelta

import numpy as np
import pandas as pd
from faker import Faker
from sqlalchemy import create_engine, text

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Multi-locale for rich global diversity
locales = [
    "en_US",
    "en_GB",
    "de_DE",
    "fr_FR",
    "es_ES",
    "it_IT",
    "ja_JP",
    "pt_BR",
    "ar_AA",
]
fake = Faker(locales)
Faker.seed(42)
np.random.seed(42)
random.seed(42)

NUM_RECORDS = int(os.environ.get("KYC_SANDBOX_NUM_RECORDS", "10000"))

# --- TAXONOMIES & RISK MAPPINGS ---
CLIENT_TYPES = ["INDIVIDUAL", "CORPORATE", "TRUST", "POLITICAL"]
CLIENT_TYPE_WEIGHTS = [0.65, 0.20, 0.08, 0.07]

COUNTRY_RISK_MAP = {
    # High-Risk / Sanctions (FATF)
    "IR": "HIGH",
    "KP": "HIGH",
    "MM": "HIGH",
    "SY": "HIGH",
    "YE": "HIGH",
    "RU": "HIGH",
    # Medium-Risk / Enhanced Monitoring
    "PA": "MEDIUM",
    "KY": "MEDIUM",
    "BS": "MEDIUM",
    "NG": "MEDIUM",
    "TR": "MEDIUM",
    "ZA": "MEDIUM",
    # Low-Risk
    "GB": "LOW",
    "US": "LOW",
    "DE": "LOW",
    "FR": "LOW",
    "ES": "LOW",
    "IT": "LOW",
    "NL": "LOW",
    "SE": "LOW",
    "JP": "LOW",
    "CA": "LOW",
    "AU": "LOW",
    "SG": "LOW",
    "CH": "LOW",
}

COUNTRIES = list(COUNTRY_RISK_MAP.keys())
COUNTRY_WEIGHTS = [
    0.02,
    0.01,
    0.01,
    0.01,
    0.01,
    0.04,
    0.03,
    0.03,
    0.03,
    0.03,
    0.04,
    0.04,
    0.10,
    0.12,
    0.08,
    0.08,
    0.06,
    0.06,
    0.04,
    0.03,
    0.04,
    0.04,
    0.02,
    0.02,
    0.01,
]

INCOME_BANDS = ["<25K", "25-50K", "50-100K", "100-250K", "250K+"]

OCCUPATIONS = {
    "<25K": [
        "Student", "Retail Associate", "Barista", "Hospitality Staff",
        "Cashier", "Delivery Driver", "Cook / Line Cook", "Rideshare Driver",
        "Freelance Creative", "Intern", "Warehouse Associate", "Janitor / Cleaner"
    ],
    "25-50K": [
        "School Teacher", "Graphic Designer", "Junior Developer", "Nurse",
        "Paralegal", "Administrative Assistant", "Social Worker", "Electrician Apprentice",
        "Content Creator", "Customer Support Specialist", "Account Coordinator", "Photographer"
    ],
    "50-100K": [
        "Software Engineer", "Project Manager", "Data Analyst", "Consultant",
        "Accountant (CPA)", "Marketing Manager", "UX/UI Designer", "Civil Engineer",
        "Physician Assistant", "Financial Analyst", "Operations Manager", "Technical Writer"
    ],
    "100-250K": [
        "Corporate Lawyer", "Surgeon", "Investment Banker", "Director",
        "Senior Software Architect", "Solutions Architect", "Anesthesiologist", "VP of Marketing",
        "Quantitative Analyst", "Management Consulting Principal", "General Counsel", "Orthodontist"
    ],
    "250K+": [
        "C-Suite Executive", "Hedge Fund Partner", "Tech Founder", "Principal",
        "Venture Capital Partner", "Managing Director (IB/PE)", "Chief Medical Officer",
        "Equity Partner (Big Law)", "Big Tech Distinguished Engineer", "Specialist Private Surgeon"
    ],
}

SOURCE_OF_FUNDS = {
    "INDIVIDUAL": ["Employment Income", "Inheritance", "Savings & Investments", "Property Sale"],
    "CORPORATE": ["Business Operations", "Retained Earnings", "Equity Financing"],
    "TRUST": ["Trust Distribution", "Generational Wealth Settlement", "Asset Liquidation"],
    "POLITICAL": ["State/Government Salary", "Diplomatic Stipend", "Personal Investments"],
}

PRODUCTS = {
    "INDIVIDUAL": [
        "RETAIL_BANKING",
        "HIGH_YIELD_SAVINGS",
        "CREDIT_CARD_ISSUANCE",
        "MORTGAGE_ORIGINATION",
        "AUTO_FINANCING",
        "PERSONAL_LOAN",
        "BUY_NOW_PAY_LATER",
        "ROBO_ADVISORY",
        "RETAIL_EQUITY_BROKERAGE",
        "CRYPTO_BROKERAGE",
        "TERM_LIFE_INSURANCE",
        "CROSS_BORDER_REMITTANCE",
    ],
    "CORPORATE": [
        "CORPORATE_ACCOUNT",
        "TREASURY_MANAGEMENT",
        "COMMERCIAL_LENDING",
        "SYNDICATED_LOANS",
        "TRADE_FINANCE_CREDIT_INSURANCE",
        "LETTER_OF_CREDIT",
        "ESCROW_SERVICES",
        "MERCHANT_ACQUIRING",
        "CORPORATE_CREDIT_CARD",
        "PAYROLL_SERVICES",
        "FX_HEDGING_SOLUTIONS",
        "LIQUIDITY_MANAGEMENT",
        "INSTITUTIONAL_CRYPTO_CUSTODY",
    ],
    "TRUST": [
        "WEALTH_MANAGEMENT",
        "FIDUCIARY_CUSTODY_ACCOUNT",
        "ESTATE_PLANNING_SERVICES",
        "BENEFICIARY_DISBURSEMENT_SERVICES",
        "TRUSTEE_ADMINISTRATION",
        "ASSET_PROTECTION_TRUSTS",
        "BESPOKE_PORTFOLIO_MANAGEMENT",
        "SECURITIES_BASED_LENDING",
        "TAX_OPTIMIZED_INVESTING",
        "PHILANTHROPY_ENDOWMENT_MANAGEMENT",
        "PRIVATE_EQUITY_FEEDER",
        "DIRECT_INDEXING",
    ],
    "POLITICAL": [
        "CAMPAIGN_CHECKING_ACCOUNT",
        "PAC_TREASURY_MANAGEMENT",
        "POLITICAL_DONATION_PROCESSING",
        "ESCROW_SERVICES",
        "BALLOT_INITIATIVE_ESCROW",
        "REGULATORY_REPORTING_INTEGRATION",
        "HIGH_VOLUME_MERCHANT_GATEWAY",
        "DISBURSEMENT_PAYROLL_SERVICES",
        "SHORT_TERM_LIQUIDITY_MANAGEMENT",
        "GOVERNMENT_RELATIONS_ESCROW",
        "COMPLIANCE_RESTRICTED_DEPOSIT_ACCOUNT",
    ],
}


def generate_dataset():
    """Builds the 5 in-memory synthetic tables (client, address, case, document, risk)."""
    clients, addresses, cases, documents, risk_classifications = [], [], [], [], []
    doc_id_counter = 1

    for i in range(1, NUM_RECORDS + 1):
        c_type = np.random.choice(CLIENT_TYPES, p=CLIENT_TYPE_WEIGHTS)
        nationality = np.random.choice(COUNTRIES, p=COUNTRY_WEIGHTS)
        country_risk = COUNTRY_RISK_MAP[nationality]
        tax_res = np.random.choice([nationality, np.random.choice(COUNTRIES)], p=[0.85, 0.15])

        income_band = np.random.choice(INCOME_BANDS, p=[0.20, 0.35, 0.25, 0.15, 0.05])
        is_pep = (c_type == "POLITICAL") or (np.random.rand() < 0.02)
        adverse_media = int(np.random.choice([0, 1, 2], p=[0.92, 0.06, 0.02]))

        if c_type == "CORPORATE":
            full_name = fake.company()
            occupation, employer = None, None
            dob = fake.date_between(start_date="-20y", end_date="-1y")
            funds = random.choice(SOURCE_OF_FUNDS["CORPORATE"])
        elif c_type == "TRUST":
            full_name = f"The {fake.last_name()} Settlement Trust"
            occupation, employer = "Trustee", "Fiduciary Services Ltd"
            dob = fake.date_between(start_date="-30y", end_date="-2y")
            funds = random.choice(SOURCE_OF_FUNDS["TRUST"])
        elif c_type == "POLITICAL":
            full_name = fake.name()
            occupation = random.choice(["Diplomat", "Ministry Director", "MP", "Ambassador"])
            employer = "State Department"
            dob = fake.date_of_birth(minimum_age=35, maximum_age=72)
            funds = random.choice(SOURCE_OF_FUNDS["POLITICAL"])
        else:
            full_name = fake.name()
            occupation = random.choice(OCCUPATIONS[income_band])
            employer = fake.company()
            dob = fake.date_of_birth(minimum_age=18, maximum_age=75)
            funds = random.choice(SOURCE_OF_FUNDS["INDIVIDUAL"])

        # Compliance Risk Engine
        risk_score = 0
        if country_risk == "HIGH":
            risk_score += 40
        elif country_risk == "MEDIUM":
            risk_score += 15
        if is_pep:
            risk_score += 25
        if adverse_media > 0:
            risk_score += 35
        if tax_res != nationality:
            risk_score += 10

        # Verification & Document Failures
        doc_fail_rate = min(0.85, risk_score / 100.0)
        has_unverified = np.random.rand() < doc_fail_rate
        if has_unverified:
            risk_score += 30

        # 18% of cases remain in progress (not approved/rejected yet)
        is_in_progress = np.random.rand() < 0.18

        # Decision Outcome (with discretion noise for closed cases)
        final_score = risk_score + np.random.normal(0, 5)
        is_rejected = (final_score >= 50) and (not is_in_progress)

        if is_in_progress:
            status = "PENDING_REVIEW"
            case_status = np.random.choice(["OPEN", "IN_REVIEW", "PENDING_DOCUMENTS"], p=[0.45, 0.35, 0.20])
        else:
            status = "REJECTED" if is_rejected else "APPROVED"
            case_status = "CLOSED"

        risk_level = "HIGH" if final_score > 65 else ("MEDIUM" if final_score > 35 else "LOW")

        # In-progress cases may not yet have an assigned officer
        officer_assigned = (not is_in_progress) or (np.random.rand() < 0.55)
        officer_id = random.randint(1, 5) if officer_assigned else None
        opened_date = fake.date_time_between(start_date="-1y", end_date="now")
        completed_date = None if is_in_progress else opened_date + timedelta(days=random.randint(1, 6))

        rej_reason = None
        if is_rejected:
            if has_unverified:
                rej_reason = "Unverified or expired identity documents"
            elif country_risk == "HIGH":
                rej_reason = "High-risk sanctions/FATF jurisdiction"
            elif is_pep or adverse_media > 0:
                rej_reason = "Adverse media and PEP screening threshold exceeded"
            else:
                rej_reason = "Failed holistic compliance and affordability criteria"

        # 1. CLIENT ROW
        clients.append({
            "client_id": i,
            "full_name": full_name,
            "client_type": c_type,
            "nationality": nationality,
            "date_of_birth": dob.strftime("%Y-%m-%d"),
            "country_of_birth": nationality,
            "tax_residency": tax_res,
            "occupation": occupation,
            "employer": employer,
            "main_source_of_funds": funds,
            "annual_income_band": income_band,
            "status": status,
            "is_active": status in ["APPROVED", "PENDING_REVIEW"],
            "username": f"user_{i}_{fake.user_name()[:6]}",
            "password_hash": "pbkdf2_sha256$210000$+Zuau15k6cIQYfCWdMLwIw==$7v4A8RgF4rbi57jRvQ9jKmLWjaxtHpVCyJoOF95+rAU=",
            "is_pep": is_pep,
            "adverse_media_hits": adverse_media
        })

        # 2. ADDRESS ROW
        addresses.append({
            "address_id": i,
            "client_id": i,
            "address_type": "REGISTERED",
            "line1": fake.street_address(),
            "line2": None,
            "city": fake.city(),
            "country": nationality,
            "state": fake.state() if hasattr(fake, 'state') else None,
            "postcode": fake.postcode(),
            "is_current": "TRUE"
        })

        # 3. ONBOARDING CASE ROW
        cases.append({
            "case_id": i,
            "client_id": i,
            "opened_date": opened_date.strftime("%Y-%m-%d %H:%M:%S"),
            "product_type": random.choice(PRODUCTS[c_type]),
            "case_status": case_status,
            "assigned_officer_id": officer_id,
            "due_date": (opened_date + timedelta(days=14)).strftime("%Y-%m-%d"),
            "completed_date": completed_date.strftime("%Y-%m-%d %H:%M:%S") if completed_date else None,
            "rejection_reason": rej_reason,
            "jurisdiction_risk": country_risk
        })

        # 4. DOCUMENT ROWS
        base_doc_types = [16, 21] if c_type == "CORPORATE" else ([25, 26] if c_type == "TRUST" else [1, 4])

        if is_in_progress:
            # In-progress cases may have no docs yet or partially submitted docs.
            doc_state = np.random.choice(["NONE", "PARTIAL_UNVERIFIED", "PARTIAL_MIXED"], p=[0.45, 0.40, 0.15])
            if doc_state == "NONE":
                selected_doc_types = []
            else:
                doc_count = random.randint(1, len(base_doc_types))
                selected_doc_types = random.sample(base_doc_types, k=doc_count)
        else:
            selected_doc_types = base_doc_types
            doc_state = "CLOSED_STANDARD"

        for dtype in selected_doc_types:
            if is_in_progress:
                is_doc_valid = (doc_state == "PARTIAL_MIXED") and (np.random.rand() < 0.25)
            else:
                is_doc_valid = not has_unverified

            exp_date = (
                date.today() + timedelta(days=random.randint(300, 1800))
                if is_doc_valid
                else date.today() - timedelta(days=random.randint(10, 300))
            )

            documents.append({
                "doc_id": doc_id_counter,
                "case_id": i,
                "doc_type_id": dtype,
                "submission_date": opened_date.strftime("%Y-%m-%d %H:%M:%S"),
                "verified_flag": is_doc_valid,
                "verified_by": officer_id if is_doc_valid else None,
                "verified_at": (opened_date + timedelta(hours=4)).strftime("%Y-%m-%d %H:%M:%S") if is_doc_valid else None,
                "expiry_date": exp_date.strftime("%Y-%m-%d"),
                "rejection_reason": "Document illegible or expired" if not is_doc_valid else None
            })
            doc_id_counter += 1

        # 5. RISK CLASSIFICATION ROW
        risk_classifications.append({
            "classification_id": i,
            "case_id": i,
            "risk_level": "PENDING" if is_in_progress else risk_level,
            "classification_date": completed_date.strftime("%Y-%m-%d %H:%M:%S") if completed_date else None,
            "assessed_by": officer_id,
            "rationale": (
                f"Preliminary profile generated. Current state: {case_status}."
                if is_in_progress
                else f"Automated risk profile evaluated: {risk_level} risk score. Outcome: {status}."
            ),
            "next_review_date": (
                (opened_date + timedelta(days=30)).strftime("%Y-%m-%d")
                if is_in_progress
                else (completed_date + timedelta(days=365)).strftime("%Y-%m-%d")
            )
        })

    return {
        "client.csv": clients,
        "client_address.csv": addresses,
        "onboarding_case.csv": cases,
        "document.csv": documents,
        "risk_classification.csv": risk_classifications,
    }


def write_csvs(tables):
    for filename, data in tables.items():
        path = os.path.join(BASE_DIR, filename)
        with open(path, "w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=data[0].keys())
            writer.writeheader()
            writer.writerows(data)
        print(f"Generated {filename} with {len(data)} rows.")


# Matches the env var names used by scripts/db/bootstrap_db.sh so both tools
# share the same credentials/target database.
DB_USER = os.environ.get("MYSQL_USER", "root")
DB_PASS = os.environ.get("MYSQL_PASSWORD", "")
DB_HOST = os.environ.get("MYSQL_HOST", "localhost")
DB_PORT = os.environ.get("MYSQL_PORT", "3306")
DB_NAME = os.environ.get("MYSQL_DATABASE", "kyc_db")

TABLE_CSV_MAP = [
    ("client", "client.csv"),
    ("client_address", "client_address.csv"),
    ("onboarding_case", "onboarding_case.csv"),
    ("document", "document.csv"),
    ("risk_classification", "risk_classification.csv"),
]


def load_csvs_to_db():
    engine = create_engine(f"mysql+pymysql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}")
    with engine.begin() as conn:
        # Disable foreign key checks for this session
        conn.execute(text("SET FOREIGN_KEY_CHECKS = 0;"))
        try:
            for table_name, csv_file in TABLE_CSV_MAP:
                # 1. Clear existing table data while preserving table structure & indexes
                print(f"Clearing existing data from `{table_name}`...")
                conn.execute(text(f"TRUNCATE TABLE `{table_name}`;"))

                # 2. Load and insert the new data
                print(f"Uploading {csv_file} into `{table_name}`...")
                df = pd.read_csv(os.path.join(BASE_DIR, csv_file))
                df.to_sql(
                    name=table_name,
                    con=conn,
                    if_exists="append",
                    index=False,
                    chunksize=1000
                )
        finally:
            # Re-enable foreign key checks regardless of success or failure
            conn.execute(text("SET FOREIGN_KEY_CHECKS = 1;"))

    print("All datasets overwritten and loaded successfully.")


if __name__ == "__main__":
    write_csvs(generate_dataset())
    load_csvs_to_db()
