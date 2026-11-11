#!/usr/bin/env bash
set -euo pipefail
jarvis_script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
jarvis_runtime_dir="${XDG_DATA_HOME:-$HOME/.local/share}/jarvis-companion/runtime"
python3 -c 'import sys; assert sys.version_info >= (3,11), "Python 3.11+ is required"'
if [[ ! -x "$jarvis_runtime_dir/bin/python" ]]; then python3 -m venv "$jarvis_runtime_dir"; fi
"$jarvis_runtime_dir/bin/python" -m pip install -r "$jarvis_script_dir/requirements.txt"
exec "$jarvis_runtime_dir/bin/python" "$jarvis_script_dir/jarvis_companion.py" "$@"
