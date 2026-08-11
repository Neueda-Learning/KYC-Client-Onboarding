#!/bin/bash
# Index optimization script

echo "Rebuilding and optimizing database indexes..."
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p kyc_db -e "OPTIMIZE TABLE client, document, onboarding_case, client_address;"
echo "Indexes successfully rebuilt!"