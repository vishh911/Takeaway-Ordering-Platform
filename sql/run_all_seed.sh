#!/usr/bin/env bash
# Load full campus_eateries dataset (requires mysql client on PATH).
# Usage: ./run_all_seed.sh [mysql_user]   (default user: root; you will be prompted for password)
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
USER="${1:-root}"
MYSQL=(mysql -u "$USER" -p --default-character-set=utf8mb4)

echo "Running 01_schema.sql …"
"${MYSQL[@]}" < "$ROOT_DIR/01_schema.sql"
echo "Running 02_triggers_procedures_views.sql …"
"${MYSQL[@]}" < "$ROOT_DIR/02_triggers_procedures_views.sql"
echo "Running 03_sample_data.sql …"
"${MYSQL[@]}" < "$ROOT_DIR/03_sample_data.sql"
echo "Running 05_synthetic_transaction_seed.sql …"
"${MYSQL[@]}" < "$ROOT_DIR/05_synthetic_transaction_seed.sql"
echo "Done. Run 04_reporting_queries.sql manually in Workbench or: mysql -u $USER -p campus_eateries < sql/04_reporting_queries.sql"
