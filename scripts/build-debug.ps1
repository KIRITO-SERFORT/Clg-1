$ErrorActionPreference = "Stop"

if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    throw "Gradle 9.3.1 is required. Install it or use Android Studio / the included GitHub Actions workflow."
}
if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw "JDK 21 keytool was not found. Install JDK 21 and reopen PowerShell."
}
if (-not (Test-Path ".\debug.keystore")) {
    keytool -genkeypair -v -keystore debug.keystore -storepass android `
        -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 `
        -validity 10000 -dname "CN=Android Debug,O=Clg 1%,C=IN"
}

gradle testDebugUnitTest --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Unit tests failed. APK build stopped." }

gradle assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Debug APK build failed." }

Write-Host "APK: app\build\outputs\apk\debug\app-debug.apk"
