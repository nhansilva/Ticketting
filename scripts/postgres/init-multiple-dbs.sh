#!/bin/bash
# Tạo nhiều database trong một PostgreSQL instance.
# Chạy tự động khi container khởi động lần đầu.
# Biến POSTGRES_MULTIPLE_DATABASES: "user_db,booking_db,payment_db,media_db"

set -e

create_database() {
    local db=$1
    echo "Creating database: $db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $db;
        GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Creating multiple databases: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_database $db
    done
    echo "All databases created."
fi
