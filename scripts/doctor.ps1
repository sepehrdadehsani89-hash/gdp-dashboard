$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/common.ps1"
$issues = 0

try { Test-JarvisJdk } catch { Write-Warning $_; $issues++ }
$rootPath = Get-JarvisRoot
foreach ($relativePath in @('gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar')) {
    if (Test-Path -LiteralPath (Join-Path $rootPath $relativePath)) { Write-Host "Available: $relativePath" }
    else { Write-Warning "Missing: $relativePath"; $issues++ }
}
$sdkPath = Get-JarvisSdk
if (-not $sdkPath -or -not (Test-Path -LiteralPath $sdkPath)) {
    Write-Warning 'Set ANDROID_HOME to an installed Android SDK.'
    $issues++
} else {
    foreach ($component in @('platforms/android-35/android.jar', 'build-tools/35.0.0/aapt2.exe', 'ndk/26.3.11579264/build/cmake/android.toolchain.cmake', 'cmake/3.22.1/bin/cmake.exe')) {
        if (Test-Path -LiteralPath (Join-Path $sdkPath $component)) { Write-Host "SDK component: $component" }
        else { Write-Warning "Missing SDK component: $component"; $issues++ }
    }
}
try {
    $adbBin = Get-JarvisAdb
    & $adbBin devices -l
    if ($LASTEXITCODE -ne 0) { Write-Warning 'ADB could not enumerate devices. Check authorization and local permissions.' }
} catch { Write-Warning 'ADB unavailable; needed only for computer-to-phone installation.' }
if ($issues -gt 0) { Write-Warning "Fix $issues build prerequisite(s); see docs/INSTALL.md."; exit 1 }
Write-Host 'Build prerequisites found. Phone audio and model inference still require device validation.'
