param(
    [string]$ToolchainRoot = (Join-Path $env:USERPROFILE '.codex\toolchains\kitap-android'),
    [ValidatePattern('^(armeabi-v7a|arm64-v8a|x86|x86_64)(,(armeabi-v7a|arm64-v8a|x86|x86_64))*$')]
    [string]$Architectures = 'arm64-v8a',
    [ValidatePattern('^(mvp-[0-6]|ux-mvp-[1-6]|ui-simplification)$')]
    [string]$Milestone = 'ui-simplification',
    [switch]$SkipTests
)
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path $PSScriptRoot -Parent
$taskEnvironmentPath = Join-Path $ToolchainRoot 'environment.json'
if (-not (Test-Path -LiteralPath $taskEnvironmentPath)) { throw 'Run scripts/bootstrap-android.ps1 -InstallSdk first' }
$taskEnvironment = Get-Content -LiteralPath $taskEnvironmentPath -Raw | ConvertFrom-Json
$env:JAVA_HOME = $taskEnvironment.javaHome
$env:ANDROID_HOME = $taskEnvironment.androidHome
$env:PATH = "$($taskEnvironment.javaHome)\bin;$($taskEnvironment.androidHome)\platform-tools;$env:PATH"

$taskSigningRoot = Join-Path $ToolchainRoot 'signing'
$taskSigningConfig = Join-Path $taskSigningRoot 'release.json'
New-Item -ItemType Directory -Path $taskSigningRoot -Force | Out-Null
if (-not (Test-Path -LiteralPath $taskSigningConfig)) {
    $taskKeystore = Join-Path $taskSigningRoot 'kitap-release.p12'
    if (Test-Path -LiteralPath $taskKeystore) { throw 'Existing signing key found without configuration; recover it instead of replacing it' }
    $taskRandom = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($taskRandom)
    $env:KITAP_KEY_PASSWORD = [Convert]::ToBase64String($taskRandom)
    & (Join-Path $taskEnvironment.javaHome 'bin\keytool.exe') -genkeypair -keystore $taskKeystore -storetype PKCS12 -alias kitap-release -keyalg RSA -keysize 3072 -validity 10000 -storepass:env KITAP_KEY_PASSWORD -keypass:env KITAP_KEY_PASSWORD -dname 'CN=Kitap Release, OU=Reader, O=Local, C=TR'
    if ($LASTEXITCODE -ne 0) { throw 'Signing key generation failed' }
    @{storeFile=$taskKeystore;storePassword=$env:KITAP_KEY_PASSWORD;keyAlias='kitap-release';keyPassword=$env:KITAP_KEY_PASSWORD} | ConvertTo-Json | Set-Content -LiteralPath $taskSigningConfig -Encoding utf8
    Remove-Item Env:KITAP_KEY_PASSWORD
}
$env:KITAP_SIGNING_CONFIG = $taskSigningConfig
$taskAndroidRoot = Join-Path $taskProjectRoot 'mobile\android'
$taskSdkProperty = $taskEnvironment.androidHome.Replace('\', '/').Replace(':', '\:')
Set-Content -LiteralPath (Join-Path $taskAndroidRoot 'local.properties') -Value "sdk.dir=$taskSdkProperty" -Encoding ascii
$taskGradleTasks = @(':app:generateCodegenArtifactsFromSchema')
if (-not $SkipTests) { $taskGradleTasks += ':app:testDebugUnitTest' }
$taskGradleTasks += ':app:assembleRelease'
Push-Location $taskAndroidRoot
try {
    & '.\gradlew.bat' @taskGradleTasks "-PreactNativeArchitectures=$Architectures" --no-daemon --max-workers=2 --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
} finally { Pop-Location }
$taskArtifactRoot = Join-Path $taskProjectRoot "artifacts\$Milestone"
New-Item -ItemType Directory -Path $taskArtifactRoot -Force | Out-Null
$taskSourceApk = Join-Path $taskAndroidRoot 'app\build\outputs\apk\release\app-release.apk'
$taskArtifactSuffix = if($Architectures.Contains(',')) { 'universal' } else { $Architectures }
$taskTargetApk = Join-Path $taskArtifactRoot "kitap-$($Milestone.Replace('-', ''))-$taskArtifactSuffix.apk"
Copy-Item -LiteralPath $taskSourceApk -Destination $taskTargetApk -Force
@{apk=$taskTargetApk;sha256=(Get-FileHash -LiteralPath $taskTargetApk -Algorithm SHA256).Hash;sizeMiB=(Get-Item -LiteralPath $taskTargetApk).Length/1MB;architectures=$Architectures;physicalDeviceValidated=$false} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $taskArtifactRoot 'build-result.json') -Encoding utf8
Write-Output "Release APK: $taskTargetApk"
