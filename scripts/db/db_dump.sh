#!/bin/bash
# Database backup script

echo "Creating database backup for kyc_db..."
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe" -u root -p kyc_db > kyc_db_backup.sql
echo "Database backup successfully saved to kyc_db_backup.sql!"