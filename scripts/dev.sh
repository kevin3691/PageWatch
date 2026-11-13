#!/usr/bin/env bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$PROJECT_ROOT/scripts/java-env.sh"
# Verify both ports before starting any child process; never reuse another app's health endpoint.
node --input-type=module - "${PORT:-8088}" 5178 <<'JS'
import net from 'node:net';
for (const port of process.argv.slice(2).map(Number)) {
  await new Promise((resolve) => {
    const server = net.createServer();
    server.once('error', () => { console.error(`端口 ${port} 已被占用，请先停止已运行的页哨实例或调整端口。`); process.exit(1); });
    server.listen(port, '127.0.0.1', () => server.close(resolve));
  });
}
JS
if [[ -z "${DATABASE_URL:-}" ]]; then
  "$PROJECT_ROOT/scripts/db.sh" start
  export DATABASE_URL="jdbc:postgresql://127.0.0.1:${PAGEWATCH_DB_PORT:-55432}/pagewatch"
fi
if [[ ! -d "$PROJECT_ROOT/frontend/node_modules" ]]; then
  (cd "$PROJECT_ROOT/frontend" && npm ci)
fi
BACKEND_PID=''
cleanup() { if [[ -n "$BACKEND_PID" ]]; then kill "$BACKEND_PID" 2>/dev/null || true; fi; }
trap cleanup EXIT
trap 'exit 130' INT TERM
"$PROJECT_ROOT/scripts/backend.sh" &
BACKEND_PID=$!
ready=false
for ((attempt=0; attempt<120; attempt++)); do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then echo '后端未能启动，请查看上面的错误信息。'; exit 1; fi
  if curl --noproxy '*' -fsS "http://127.0.0.1:${PORT:-8088}/api/health" >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
if [[ "$ready" != true ]]; then echo '后端启动仍未完成，请检查 Maven 下载情况。'; exit 1; fi
export API_PROXY="${API_PROXY:-http://127.0.0.1:${PORT:-8088}}"
echo '页哨前端：http://127.0.0.1:5178（Ctrl+C 停止前后端，数据库会保留）'
cd "$PROJECT_ROOT/frontend"
npm run dev
