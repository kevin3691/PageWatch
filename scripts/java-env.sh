#!/usr/bin/env bash
# Sourced by dev.sh and backend.sh. Never changes system-wide Java settings.
java_major() { "$1/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.specification.version = //p' | head -1; }
if [[ -z "${JAVA_HOME:-}" ]] || [[ "$(java_major "$JAVA_HOME")" -lt 25 ]]; then
  for candidate in /Library/Java/JavaVirtualMachines/*25*/Contents/Home /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home; do
    if [[ -x "$candidate/bin/java" ]] && [[ "$(java_major "$candidate")" -ge 25 ]]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
if [[ -z "${JAVA_HOME:-}" ]] && command -v javac >/dev/null 2>&1; then
  JAVA_EXE="$(command -v javac)"
  export JAVA_HOME="$(cd "$(dirname "$JAVA_EXE")/.." && pwd)"
fi
if [[ -z "${JAVA_HOME:-}" ]] || [[ ! -x "$JAVA_HOME/bin/java" ]] || [[ "$(java_major "$JAVA_HOME")" -lt 25 ]]; then
  echo '需要 JDK 25 或更新版本。安装后设置 JAVA_HOME，再运行启动脚本。'; exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
