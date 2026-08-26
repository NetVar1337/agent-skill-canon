#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

TARGET_ID="${TARGET_ID:-sample-target}"
TARGET_BINARY="${TARGET_BINARY:-}"
GHIDRA_INSTALL_DIR="${GHIDRA_INSTALL_DIR:-}"
WORKFLOW_ACTION="${WORKFLOW_ACTION:-baseline}"
SELECTED_FUNCTIONS="${SELECTED_FUNCTIONS:-}"
WORKSPACE_ROOT="${GHIDRA_WORKSPACE_ROOT:-$(git -C "${PWD}" rev-parse --show-toplevel 2>/dev/null || "${PWD}")}"
ARTIFACT_ROOT="${ARTIFACT_ROOT:-}"

CLI="ghidra-agent-cli"
if ! command -v "${CLI}" >/dev/null 2>&1; then
  CLI_DIR="${SCRIPT_DIR}/../../../ghidra-agent-cli"
  if [[ -f "${CLI_DIR}/target/release/ghidra-agent-cli" ]]; then
    CLI="${CLI_DIR}/target/release/ghidra-agent-cli"
  elif [[ -f "${CLI_DIR}/target/debug/ghidra-agent-cli" ]]; then
    CLI="${CLI_DIR}/target/debug/ghidra-agent-cli"
  else
    echo "ghidra-agent-cli not found. Build it first: cargo build --release" >&2
    exit 1
  fi
fi

GLOBAL_ARGS=(--target "${TARGET_ID}" --workspace "${WORKSPACE_ROOT}")
if [[ -n "${GHIDRA_INSTALL_DIR}" ]]; then
  GLOBAL_ARGS+=(--install-dir "${GHIDRA_INSTALL_DIR}")
fi

usage() {
  cat <<'EOF'
Environment-driven wrapper around ghidra-agent-cli.

Supported WORKFLOW_ACTION values:
  import       — import binary into Ghidra project
  analyze      — run auto-analysis on imported binary
  baseline     — import, analyze, and export baseline YAML
  export       — export baseline metadata from analyzed program
  decompile    — decompile selected functions

Required environment:
  TARGET_BINARY for import, analyze, baseline, and decompile
  SELECTED_FUNCTIONS as a comma-separated address list for decompile

Optional environment:
  GHIDRA_WORKSPACE_ROOT to override the default workspace-root detection
  GHIDRA_INSTALL_DIR to specify Ghidra installation directory
EOF
}

require_binary() {
  if [[ -z "${TARGET_BINARY}" ]]; then
    cat <<'EOF' >&2
TARGET_BINARY is not set.
Provide the absolute path to the binary you want to analyze, for example:
  export TARGET_BINARY=/absolute/path/to/binary
EOF
    exit 1
  fi
}

case "${WORKFLOW_ACTION}" in
--help | -h)
  usage
  exit 0
  ;;
import)
  require_binary
  "${CLI}" "${GLOBAL_ARGS[@]}" workspace init --binary "${TARGET_BINARY}"
  "${CLI}" "${GLOBAL_ARGS[@]}" ghidra import
  ;;
analyze)
  "${CLI}" "${GLOBAL_ARGS[@]}" ghidra auto-analyze
  ;;
baseline)
  require_binary
  "${CLI}" "${GLOBAL_ARGS[@]}" workspace init --binary "${TARGET_BINARY}"
  "${CLI}" "${GLOBAL_ARGS[@]}" ghidra import
  "${CLI}" "${GLOBAL_ARGS[@]}" ghidra auto-analyze
  "${CLI}" "${GLOBAL_ARGS[@]}" ghidra export-baseline
  ;;
export)
  "${CLI}" "${GLOBAL_ARGS[@]}" ghidra export-baseline
  ;;
decompile)
  require_binary
  if [[ -z "${SELECTED_FUNCTIONS}" ]]; then
    cat <<'EOF' >&2
SELECTED_FUNCTIONS is not set.
Provide a comma-separated address list, for example:
  export SELECTED_FUNCTIONS='0x100004a4,0x10000520'
EOF
    exit 1
  fi
  IFS=',' read -ra addrs <<<"${SELECTED_FUNCTIONS}"
  for addr in "${addrs[@]}"; do
    "${CLI}" "${GLOBAL_ARGS[@]}" ghidra decompile --addr "${addr}"
  done
  ;;
*)
  printf 'Unsupported WORKFLOW_ACTION: %s\n' "${WORKFLOW_ACTION}" >&2
  usage >&2
  exit 1
  ;;
esac
