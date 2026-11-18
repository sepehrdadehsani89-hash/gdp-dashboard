#!/usr/bin/env bash
set -euo pipefail
jarvis_script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
jarvis_runtime_dir="${XDG_DATA_HOME:-$HOME/.local/share}/jarvis-companion/runtime"
python3 -c 'import sys; assert sys.version_info >= (3,11), "Python 3.11+ is required"'
if [[ ! -x "$jarvis_runtime_dir/bin/python" ]]; then python3 -m venv "$jarvis_runtime_dir"; fi
if ! "$jarvis_runtime_dir/bin/python" - "$jarvis_script_dir/requirements.txt" <<'PY'
import importlib.metadata, pathlib, sys
rows = [x.strip().split('==') for x in pathlib.Path(sys.argv[1]).read_text().splitlines() if x.strip() and not x.startswith('#')]
try:
    ready = all(importlib.metadata.version(name) == version for name, version in rows)
except importlib.metadata.PackageNotFoundError:
    ready = False
sys.exit(0 if ready else 1)
PY
then
  "$jarvis_runtime_dir/bin/python" -m pip install -r "$jarvis_script_dir/requirements.txt"
fi
exec "$jarvis_runtime_dir/bin/python" "$jarvis_script_dir/jarvis_companion.py" "$@"
