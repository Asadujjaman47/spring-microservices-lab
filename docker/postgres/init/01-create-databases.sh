#!/bin/bash
# Creates one database + owner per service (decision #10 in docs/PLAN.md).
# Runs once, on the Postgres container's first boot (data dir empty).
set -euo pipefail

: "${USERDB_PASSWORD:=userdb_pass}"
: "${PRODUCTDB_PASSWORD:=productdb_pass}"
: "${ORDERDB_PASSWORD:=orderdb_pass}"
: "${NOTIFICATIONDB_PASSWORD:=notificationdb_pass}"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     -v userdb_pw="$USERDB_PASSWORD" \
     -v productdb_pw="$PRODUCTDB_PASSWORD" \
     -v orderdb_pw="$ORDERDB_PASSWORD" \
     -v notificationdb_pw="$NOTIFICATIONDB_PASSWORD" <<-'EOSQL'
    CREATE USER userdb_user WITH PASSWORD :'userdb_pw';
    CREATE DATABASE userdb OWNER userdb_user;

    CREATE USER productdb_user WITH PASSWORD :'productdb_pw';
    CREATE DATABASE productdb OWNER productdb_user;

    CREATE USER orderdb_user WITH PASSWORD :'orderdb_pw';
    CREATE DATABASE orderdb OWNER orderdb_user;

    CREATE USER notificationdb_user WITH PASSWORD :'notificationdb_pw';
    CREATE DATABASE notificationdb OWNER notificationdb_user;
EOSQL