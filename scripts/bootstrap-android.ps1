param(
    [string]$ToolchainRoot = (Join-Path $env:USERPROFILE '.codex\toolchains\kitap-android'),
    [switch]$InstallSdk,
    [string[]]$SdkPackages = @('platform-tools','platforms;android-37.0','build-tools;37.0.0','build-tools;36.0.0','ndk;27.1.12297006','cmake;3.22.1')
)
$ErrorActionPreference = 'Stop'
$taskLock = Get-Content (Join-Path $PSScriptRoot 'android-toolchain.lock.json') -Raw | ConvertFrom-Json
$taskRoot = [System.IO.Path]::GetFullPath($ToolchainRoot)
New-Item -ItemType Directory -Path $taskRoot -Force | Out-Null

function Get-VerifiedArchive($Entry) {
    $taskDownloadDir = Join-Path $taskRoot 'downloads'
    New-Item -ItemType Directory -Path $taskDownloadDir -Force | Out-Null
    $taskArchive = Join-Path $taskDownloadDir $Entry.archive
    if (-not (Test-Path -LiteralPath $taskArchive)) {
        $taskPartial = "$taskArchive.part"
        & curl.exe --fail --location --retry 3 --connect-timeout 30 --silent --show-error --output $taskPartial $Entry.url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $($Entry.archive)" }
        $taskHash = (Get-FileHash -LiteralPath $taskPartial -Algorithm $Entry.algorithm).Hash
        if ($taskHash -ne $Entry.checksum) { throw "Checksum mismatch: $($Entry.archive)" }
        Move-Item -LiteralPath $taskPartial -Destination $taskArchive
    }
    if ((Get-FileHash -LiteralPath $taskArchive -Algorithm $Entry.algorithm).Hash -ne $Entry.checksum) {
        throw "Cached checksum mismatch: $($Entry.archive)"
    }
    return $taskArchive
}

$taskJdkBase = Join-Path $taskRoot 'jdk'
$taskJavaHome = Join-Path $taskJdkBase $taskLock.jdk.directory
if (-not (Test-Path -LiteralPath (Join-Path $taskJavaHome 'bin\java.exe'))) {
    $taskJdkArchive = Get-VerifiedArchive $taskLock.jdk
    Expand-Archive -LiteralPath $taskJdkArchive -DestinationPath $taskJdkBase -Force
}
# Signing keys are local-only. Preserve an existing development key; a fresh
# clone gets its own standard Android debug key, never the release signing key.
$taskDebugKeystore = Join-Path (Split-Path $PSScriptRoot -Parent) 'mobile\android\app\debug.keystore'
if (-not (Test-Path -LiteralPath $taskDebugKeystore)) {
    & (Join-Path $taskJavaHome 'bin\keytool.exe') -genkeypair -keystore $taskDebugKeystore -storetype JKS -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug, O=Android, C=US'
    if ($LASTEXITCODE -ne 0) { throw 'Local debug signing key generation failed' }
}

$taskCommandBase = Join-Path $taskRoot 'command-line-tools-23'
$taskSdkManager = Join-Path $taskCommandBase 'cmdline-tools\bin\sdkmanager.bat'
if (-not (Test-Path -LiteralPath $taskSdkManager)) {
    $taskToolsArchive = Get-VerifiedArchive $taskLock.commandLineTools
    Expand-Archive -LiteralPath $taskToolsArchive -DestinationPath $taskCommandBase -Force
}
$taskSdkRoot = Join-Path $taskRoot 'sdk'
New-Item -ItemType Directory -Path $taskSdkRoot -Force | Out-Null
# These environment values apply only to this process and its children.
$env:JAVA_HOME = $taskJavaHome
$env:ANDROID_HOME = $taskSdkRoot
$env:PATH = "$taskJavaHome\bin;$taskSdkRoot\platform-tools;$env:PATH"
if ($InstallSdk) {
    $taskAndroidCli = Join-Path $taskCommandBase 'cmdline-tools\bin\android.exe'
    foreach ($taskPackage in $SdkPackages) {
        & $taskAndroidCli --no-metrics "--sdk=$taskSdkRoot" sdk install $taskPackage
        if ($LASTEXITCODE -ne 0) { throw "Android SDK installation failed: $taskPackage" }
        $taskPackagePath = Join-Path $taskSdkRoot ($taskPackage -replace ';', '\')
        if (-not (Test-Path -LiteralPath $taskPackagePath)) { throw "SDK package missing after installation: $taskPackage" }
    }
}
@{
    schemaVersion = 1
    javaHome = $taskJavaHome
    androidHome = $taskSdkRoot
    sdkManager = $taskSdkManager
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $taskRoot 'environment.json') -Encoding utf8
Write-Output "Android toolchain ready: $taskRoot"
