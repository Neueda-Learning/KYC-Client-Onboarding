#!/bin/bash
# Expiring documents report generation script

echo "Generating expiring documents report..."
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p kyc_db -B -e "SELECT * FROM expiring_documents_vw;" | sed 's/\t/,/g' > expiring_docs_report.csv
echo "Report successfully saved to expiring_docs_report.csv!"