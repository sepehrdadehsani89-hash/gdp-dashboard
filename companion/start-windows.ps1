param([string]$Bind = "127.0.0.1", [int]$Port = 8765)
$ErrorActionPreference = "Stop"
$runtimeDir = Join-Path $env:LOCALAPPDATA "JarvisCompanion\runtime"
$pythonPath = Join-Path $runtimeDir "Scripts\python.exe"
if (-not (Test-Path $pythonPath)) {
    $launcher = Get-Command py -ErrorAction SilentlyContinue
    if ($launcher) { & $launcher.Source -3 -m venv $runtimeDir }
    else {
        $launcher = Get-Command python -ErrorAction SilentlyContinue
        if (-not $launcher) { $launcher = Get-Command python3 -ErrorAction SilentlyContinue }
        if (-not $launcher) { throw "No existing usable Python runtime was found. Use Python 3.11+ from your approved installation; existing companion state is preserved." }
        & $launcher.Source -m venv $runtimeDir
    }
    if ($LASTEXITCODE -ne 0) { throw "Could not create the private Python environment. Existing companion state was preserved." }
}
& $pythonPath -c "import sys; assert sys.version_info >= (3, 11), 'Python 3.11+ is required'"
if ($LASTEXITCODE -ne 0) { throw "Python 3.11+ is required." }
$requirements = Join-Path $PSScriptRoot "requirements.txt"
$dependencyCheck = @'
import importlib.metadata, pathlib, sys
rows = [x.strip().split('==') for x in pathlib.Path(sys.argv[1]).read_text().splitlines() if x.strip() and not x.startswith('#')]
try:
    ready = all(importlib.metadata.version(name) == version for name, version in rows)
except importlib.metadata.PackageNotFoundError:
    ready = False
sys.exit(0 if ready else 1)
'@
& $pythonPath -c $dependencyCheck $requirements
if ($LASTEXITCODE -ne 0) {
    & $pythonPath -m pip install -r $requirements
    if ($LASTEXITCODE -ne 0) { throw "Dependency installation failed; no companion was started. No automatic retry was performed." }
} else { Write-Host "Compatible companion dependencies already installed; reusing the existing Python environment." }
& $pythonPath (Join-Path $PSScriptRoot "jarvis_companion.py") --bind $Bind --port $Port
