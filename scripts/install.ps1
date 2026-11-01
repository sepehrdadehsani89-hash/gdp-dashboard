[CmdletBinding()]
param([string]$Serial = '', [string]$Apk = '', [switch]$NoBuild)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/common.ps1"

try {
    $rootPath = Get-JarvisRoot
    $adbBin = Get-JarvisAdb
    $listing = @(& $adbBin devices)
    if ($LASTEXITCODE -ne 0) { throw 'ADB could not list connected devices.' }
    $devices = @($listing | ForEach-Object {
        if ($_ -match '^(\S+)\s+device\s*$') { $Matches[1] }
    })
    if ($Serial) {
        if ($devices -notcontains $Serial) { throw "Device $Serial is not connected and authorized." }
    } elseif ($devices.Count -eq 1) { $Serial = $devices[0] }
    else { $listing | Write-Host; throw 'Authorize exactly one device, or select one with -Serial SERIAL.' }

    if (-not $Apk) {
        $Apk = Join-Path $rootPath 'app/build/outputs/apk/debug/app-debug.apk'
        if (-not $NoBuild) {
            & "$PSScriptRoot/build.ps1" ':app:assembleDebug'
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
    }
    if (-not (Test-Path -LiteralPath $Apk)) { throw "APK not found: $Apk" }
    Write-Host "Installing on authorized device: $Serial"
    & $adbBin -s $Serial install -r $Apk
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & $adbBin -s $Serial shell am start -n com.jarvis.assistant/.MainActivity
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host 'Jarvis is open. Choose permissions and model downloads in onboarding/Settings.'
    Write-Host 'No permissions or special access were granted automatically.'
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
