#!/bin/bash
# Database reload script

echo "Dropping and recreating database..."
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p -e "DROP DATABASE IF EXISTS kyc_db; CREATE DATABASE kyc_db;"
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -u root -p kyc_db < kyc_db_backup.sql
echo "Database successfully restored from backup!"