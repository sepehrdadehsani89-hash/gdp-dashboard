Set-StrictMode -Version Latest

function Get-JarvisRoot {
    return (Split-Path -Parent $PSScriptRoot)
}

function Get-JarvisSdk {
    if ($env:ANDROID_HOME) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { return $env:ANDROID_SDK_ROOT }
    return ''
}

function Get-JarvisAdb {
    $sdkPath = Get-JarvisSdk
    if ($sdkPath) {
        $candidate = Join-Path $sdkPath 'platform-tools/adb.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw 'ADB is missing. Install Android platform-tools, or install the APK directly on your phone.'
}

function Test-JarvisJdk {
    if ($env:JAVA_HOME) {
        $javaBin = Join-Path $env:JAVA_HOME 'bin/java.exe'
        $javacBin = Join-Path $env:JAVA_HOME 'bin/javac.exe'
    } else {
        $javaCommand = Get-Command java -ErrorAction SilentlyContinue
        $javacCommand = Get-Command javac -ErrorAction SilentlyContinue
        if (-not $javaCommand -or -not $javacCommand) { throw 'Install JDK 17 or 21 and set JAVA_HOME.' }
        $javaBin = $javaCommand.Source
        $javacBin = $javacCommand.Source
    }
    if (-not (Test-Path -LiteralPath $javaBin) -or -not (Test-Path -LiteralPath $javacBin)) {
        throw 'JAVA_HOME must identify a full JDK, not a JRE. Use JDK 17 or 21.'
    }
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $javaBin
    $startInfo.Arguments = '-version'
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardError = $true
    $startInfo.RedirectStandardOutput = $true
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $versionText = $process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd()
    $process.WaitForExit()
    $versionMatch = [regex]::Match($versionText, 'version "(\d+)')
    if ($process.ExitCode -ne 0 -or -not $versionMatch.Success) { throw 'Could not determine the JDK version.' }
    $major = [int]$versionMatch.Groups[1].Value
    if ($major -lt 17 -or $major -gt 22) { throw 'Gradle 8.9 requires a supported JDK. Use JDK 17 or 21.' }
    Write-Host "JDK ${major}: available"
}
