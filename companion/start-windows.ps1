param([string]$Bind = "127.0.0.1", [int]$Port = 8765)
$ErrorActionPreference = "Stop"
if (-not (Get-Command py -ErrorAction SilentlyContinue)) { throw "Install Python 3.11 or newer from https://www.python.org/downloads/windows/ and then run this script again." }
$runtimeDir = Join-Path $env:LOCALAPPDATA "JarvisCompanion\runtime"
if (-not (Test-Path (Join-Path $runtimeDir "Scripts\python.exe"))) { py -3 -m venv $runtimeDir; if ($LASTEXITCODE -ne 0) { throw "Could not create the private Python environment." } }
$pythonPath = Join-Path $runtimeDir "Scripts\python.exe"
& $pythonPath -c "import sys; assert sys.version_info >= (3, 11), 'Python 3.11+ is required'"
if ($LASTEXITCODE -ne 0) { throw "Python 3.11+ is required." }
& $pythonPath -m pip install -r (Join-Path $PSScriptRoot "requirements.txt")
if ($LASTEXITCODE -ne 0) { throw "Dependency installation failed; no companion was started." }
& $pythonPath (Join-Path $PSScriptRoot "jarvis_companion.py") --bind $Bind --port $Port
