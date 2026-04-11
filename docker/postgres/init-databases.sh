#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE DATABASE collection_manager_db;
    GRANT ALL PRIVILEGES ON DATABASE collection_manager_db TO "$POSTGRES_USER";

    CREATE DATABASE draftsim_parser_db;
    GRANT ALL PRIVILEGES ON DATABASE draftsim_parser_db TO "$POSTGRES_USER";

    CREATE DATABASE wizard_stat_db;
    GRANT ALL PRIVILEGES ON DATABASE wizard_stat_db TO "$POSTGRES_USER";
EOSQL