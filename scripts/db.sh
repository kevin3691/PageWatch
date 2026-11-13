#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUNTIME="${PAGEWATCH_RUNTIME:-$HOME/.local/share/pagewatch}"
PGDATA_PATH="$RUNTIME/pgdata"
SOCKET_PATH="$RUNTIME/pg-socket"
DB_PORT="${PAGEWATCH_DB_PORT:-55432}"
mkdir -p "$RUNTIME" "$SOCKET_PATH"
if ! command -v pg_ctl >/dev/null 2>&1; then
  for location in /opt/homebrew/opt/postgresql@18/bin /usr/local/opt/postgresql@18/bin; do
    if [[ -x "$location/pg_ctl" ]]; then export PATH="$location:$PATH"; break; fi
  done
fi
command -v pg_ctl >/dev/null 2>&1 || { echo '需要 PostgreSQL 18（包含 initdb、pg_ctl 和 createdb），或使用 docker compose up -d。'; exit 1; }
case "${1:-start}" in
  start)
    if pg_ctl -D "$PGDATA_PATH" status >/dev/null 2>&1; then
      echo "页哨数据库已启动（端口 ${DB_PORT}）。"
    else
      if pg_isready -h 127.0.0.1 -p "$DB_PORT" >/dev/null 2>&1; then
        echo "端口 $DB_PORT 已被其他 PostgreSQL 实例占用。请使用 DATABASE_URL 指定已有数据库，或调整 PAGEWATCH_DB_PORT。"; exit 1
      fi
      if [[ ! -f "$PGDATA_PATH/PG_VERSION" ]]; then
        initdb -D "$PGDATA_PATH" -U pagewatch -A trust --encoding=UTF8 --locale=C
      fi
      pg_ctl -D "$PGDATA_PATH" -l "$RUNTIME/postgres.log" -o "-p $DB_PORT -h 127.0.0.1 -k $SOCKET_PATH" start
    fi
    if [[ "$(psql -h 127.0.0.1 -p "$DB_PORT" -U pagewatch -d postgres -Atc "select exists(select 1 from pg_database where datname='pagewatch')")" != 't' ]]; then
      createdb -h 127.0.0.1 -p "$DB_PORT" -U pagewatch pagewatch
    fi
    ;;
  test-db)
    if [[ "$(psql -h 127.0.0.1 -p "$DB_PORT" -U pagewatch -d postgres -Atc "select exists(select 1 from pg_database where datname='pagewatch_test')")" != 't' ]]; then
      createdb -h 127.0.0.1 -p "$DB_PORT" -U pagewatch pagewatch_test
    fi
    echo '独立测试库 pagewatch_test 已就绪。'
    ;;
  stop) pg_ctl -D "$PGDATA_PATH" stop -m fast ;;
  status) pg_ctl -D "$PGDATA_PATH" status ;;
  *) echo '用法：./scripts/db.sh start|stop|status|test-db'; exit 1 ;;
esac
