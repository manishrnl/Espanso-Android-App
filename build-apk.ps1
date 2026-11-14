param(
    [ValidateSet("debug", "release")]
    [string]$Variant = "debug"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$variantTitle = (Get-Culture).TextInfo.ToTitleCase($Variant)

if (!$env:JAVA_HOME -or $env:JAVA_HOME.Contains("26")) {
    $candidateJdks = @(
        "C:\Program Files\Java\jdk-21.0.7+6",
        "C:\Program Files\Android\Android Studio\jbr",
        "C:\Users\MANISH\.jdks\ms-21.0.11"
    )
    foreach ($jdk in $candidateJdks) {
        if (Test-Path $jdk) {
            $env:JAVA_HOME = $jdk
            break
        }
    }
}

Push-Location $projectRoot
try {
    if ($Variant -eq "release" -and !(Test-Path "keystore.properties")) {
        throw "Release signing requires keystore.properties in the project root."
    }

    $tasks = if ($Variant -eq "release") {
        @("test", "lintRelease", "assembleRelease")
    } else {
        @("testDebugUnitTest", "lintDebug", "assembleDebug")
    }

    & "$projectRoot\gradlew.bat" $tasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle $Variant build failed with exit code $LASTEXITCODE."
    }

    $buildRoot = if ($env:LOCALAPPDATA) {
        Join-Path $env:LOCALAPPDATA "EspansoAndroid\build"
    } else {
        Join-Path $projectRoot "build"
    }
    $sourceApk = Join-Path $buildRoot "app\outputs\apk\$Variant\app-$Variant.apk"
    if (!(Test-Path $sourceApk)) {
        throw "Built APK was not found at $sourceApk."
    }

    $destinationApk = Join-Path $projectRoot "rnl-espanso-$Variant.apk"
    Copy-Item -LiteralPath $sourceApk -Destination $destinationApk -Force

    $apk = Get-Item -LiteralPath $destinationApk
    Write-Host ""
    Write-Host "$variantTitle APK copied to:"
    Write-Host $apk.FullName
    Write-Host ("Size: {0:N0} bytes" -f $apk.Length)
} finally {
    Pop-Location
}
