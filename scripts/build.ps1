[CmdletBinding()]
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/common.ps1"

try {
    Test-JarvisJdk
    $rootPath = Get-JarvisRoot
    $sdkPath = Get-JarvisSdk
    if (-not $sdkPath -or -not (Test-Path -LiteralPath $sdkPath)) { throw 'Set ANDROID_HOME to an installed Android SDK.' }
    $wrapper = Join-Path $rootPath 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $wrapper)) { throw 'Missing Gradle Wrapper. Use the complete source archive.' }
    if (-not $GradleArguments -or $GradleArguments.Count -eq 0) {
        $GradleArguments = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug')
    }
    Push-Location $rootPath
    try {
        & $wrapper --console=plain @GradleArguments
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } finally { Pop-Location }
    $apkPath = Join-Path $rootPath 'app/build/outputs/apk/debug/app-debug.apk'
    if (Test-Path -LiteralPath $apkPath) {
        Write-Host "Debug APK: $apkPath"
        Get-FileHash -Algorithm SHA256 -LiteralPath $apkPath | Format-List
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
