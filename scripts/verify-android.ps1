param([Parameter(Mandatory=$true)][ValidatePattern('^(mvp-[0-6]|ux-mvp-[1-6]|ui-simplification)$')][string]$Milestone,
  [ValidateSet('arm64-v8a','universal')][string]$Artifact='arm64-v8a',
  [string]$ToolchainRoot = (Join-Path $env:USERPROFILE '.codex\toolchains\kitap-android'))
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path $PSScriptRoot -Parent
$taskApk = Join-Path $taskProjectRoot "artifacts\$Milestone\kitap-$($Milestone.Replace('-', ''))-$Artifact.apk"
$taskReportLabel = if($Artifact -eq 'arm64-v8a') { $Milestone.ToUpperInvariant() } else { "$($Milestone.ToUpperInvariant())-$Artifact" }
if (-not (Test-Path -LiteralPath $taskApk)) { throw 'Release APK missing' }
$taskReportRoot = Join-Path $taskProjectRoot 'docs\acceptance\reports'
$taskEnvironment = Get-Content (Join-Path $ToolchainRoot 'environment.json') -Raw | ConvertFrom-Json
$env:JAVA_HOME = $taskEnvironment.javaHome
$env:PATH = "$($taskEnvironment.javaHome)\bin;$env:PATH"
$taskBuildTools = Join-Path $taskEnvironment.androidHome 'build-tools\37.0.0'
& python -X utf8 (Join-Path $PSScriptRoot 'inspect-apk.py') $taskApk --output (Join-Path $taskReportRoot "$taskReportLabel-apk-inspection.json") > $null
if ($LASTEXITCODE -ne 0) { throw 'APK inspection failed' }
$taskInspection = Get-Content (Join-Path $taskReportRoot "$taskReportLabel-apk-inspection.json") -Raw | ConvertFrom-Json
if (-not ($taskInspection.allElf16kAligned -and $taskInspection.allStoredLibrariesZip16kAligned -and $taskInspection.hasBundledJavaScript -and $taskInspection.hasPdfiumLibrary -and $taskInspection.hasHermesLibrary)) { throw 'APK packaging checks failed' }
if (($Milestone -match '^ux-' -or $Milestone -eq 'ui-simplification') -or [int]$Milestone.Split('-')[-1] -ge 4) {
  $taskLlvmNm = Join-Path $taskEnvironment.androidHome 'ndk\27.1.12297006\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-nm.exe'
  & python -X utf8 (Join-Path $PSScriptRoot 'inspect-geometry-bridge.py') $taskApk --llvm-nm $taskLlvmNm --output (Join-Path $taskReportRoot "$taskReportLabel-geometry-bridge.json")
  if ($LASTEXITCODE -ne 0) { throw 'Shrunk JNI geometry bridge verification failed' }
}
& (Join-Path $taskBuildTools 'apksigner.bat') verify --verbose --print-certs $taskApk > (Join-Path $taskReportRoot "$taskReportLabel-signature.txt")
if ($LASTEXITCODE -ne 0) { throw 'Signature verification failed' }
$taskSignature = Get-Content (Join-Path $taskReportRoot "$taskReportLabel-signature.txt") -Raw
if ($taskSignature -notmatch 'certificate SHA-256 digest: 1a1f883707fe5009a8c25582a2ca0fa9ee55c844d802936f4b311a8a2502dc5c') { throw 'Expected persistent signing certificate differs' }
& (Join-Path $taskBuildTools 'zipalign.exe') -c -P 16 4 $taskApk
if ($LASTEXITCODE -ne 0) { throw 'ZIP alignment verification failed' }
& (Join-Path $taskBuildTools 'aapt2.exe') dump badging $taskApk > (Join-Path $taskReportRoot "$taskReportLabel-package.txt")
if ($LASTEXITCODE -ne 0) { throw 'Package verification failed' }
& (Join-Path $taskBuildTools 'aapt2.exe') dump permissions $taskApk > (Join-Path $taskReportRoot "$taskReportLabel-permissions.txt")
if ($LASTEXITCODE -ne 0) { throw 'Permission verification failed' }
$taskPermissions = Get-Content (Join-Path $taskReportRoot "$taskReportLabel-permissions.txt") -Raw
if ($taskPermissions -match 'android.permission.(INTERNET|MANAGE_EXTERNAL_STORAGE|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE)') { throw 'Unexpected network/storage permission' }
$taskJunitRoot = Join-Path $taskReportRoot "$taskReportLabel-junit"
New-Item -ItemType Directory -Path $taskJunitRoot -Force | Out-Null
$taskSuites = Get-ChildItem -LiteralPath (Join-Path $taskProjectRoot 'mobile\android\app\build\test-results\testDebugUnitTest') -Filter 'TEST-*.xml' | ForEach-Object {
  Copy-Item -LiteralPath $_.FullName -Destination $taskJunitRoot -Force
  [xml]$taskXml = Get-Content -LiteralPath $_.FullName -Raw
  [pscustomobject]@{name=$taskXml.testsuite.name;tests=[int]$taskXml.testsuite.tests;failures=[int]$taskXml.testsuite.failures;errors=[int]$taskXml.testsuite.errors}
}
if (-not $taskSuites -or ($taskSuites | Where-Object { $_.failures -gt 0 -or $_.errors -gt 0 })) { throw 'Host unit tests absent or failed' }
$taskSuites | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $taskReportRoot "$taskReportLabel-junit-summary.json") -Encoding utf8
@{milestone=$Milestone;apk=$taskApk;sha256=$taskInspection.sha256;sizeMiB=$taskInspection.sizeMiB;nativeLibraries=$taskInspection.libraries.Count;
  junitTests=($taskSuites | Measure-Object -Property tests -Sum).Sum;signatureVerified=$true;elfAndZip16k=$true;physicalDeviceValidated=$false} |
  ConvertTo-Json | Tee-Object -FilePath (Join-Path $taskReportRoot "$taskReportLabel-verification.json")
