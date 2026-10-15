param(
    [ValidateSet('Run','Prepare','Metro','Emulator')][string]$Mode = 'Run',
    [string]$ToolchainRoot = (Join-Path $env:USERPROFILE '.codex\toolchains\kitap-android'),
    [ValidateRange(1024,65535)][int]$MetroPort = 8087,
    [ValidateRange(5554,5682)][int]$EmulatorPort = 5580,
    [ValidatePattern('^[A-Za-z0-9_.-]+$')][string]$Avd = 'kitap-functional'
)
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path $PSScriptRoot -Parent
$taskMobileRoot = Join-Path $taskProjectRoot 'mobile'
$taskEnvironmentFile = Join-Path $ToolchainRoot 'environment.json'
if (-not (Test-Path -LiteralPath $taskEnvironmentFile)) { throw 'Önce scripts/bootstrap-android.ps1 -InstallSdk çalıştırın.' }
$taskEnvironment = Get-Content -LiteralPath $taskEnvironmentFile -Raw | ConvertFrom-Json
$env:JAVA_HOME = $taskEnvironment.javaHome
$env:ANDROID_HOME = $taskEnvironment.androidHome
$env:PATH = "$($taskEnvironment.javaHome)\bin;$($taskEnvironment.androidHome)\platform-tools;$env:PATH"
$taskAdb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'

function Get-KitapMetro {
    $taskResponse = $null
    try { $taskResponse = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$MetroPort/status" -TimeoutSec 10 }
    catch { return $false }
    $taskStatus = if ($taskResponse.Content -is [byte[]]) { [System.Text.Encoding]::UTF8.GetString($taskResponse.Content) } else { [string]$taskResponse.Content }
    $taskServerRoot = [string](@($taskResponse.Headers['X-React-Native-Project-Root'])[0])
    if ($taskStatus.Trim() -ne 'packager-status:running' -or [string]::IsNullOrWhiteSpace($taskServerRoot) -or
        [System.IO.Path]::GetFullPath($taskServerRoot) -ne [System.IO.Path]::GetFullPath($taskMobileRoot)) {
        throw "Port $MetroPort başka bir projeye ait. Onu kapatmadan bu porta Kitap bağlanmayacak."
    }
    return $true
}

function Get-KitapEmulator {
    $taskDeviceLines = & $taskAdb devices
    if ($LASTEXITCODE -ne 0) { throw 'ADB başlatılamadı.' }
    foreach ($taskLine in $taskDeviceLines) {
        if ($taskLine -match '^(emulator-\d+)\s+device\s*$') {
            $taskCandidate = $Matches[1]
            $taskName = & $taskAdb -s $taskCandidate emu avd name 2>$null
            if ($LASTEXITCODE -eq 0 -and ($taskName | Where-Object { $_.Trim() -eq $Avd })) { return $taskCandidate }
        }
    }
    return $null
}

function Wait-KitapEmulator {
    $taskBootDeadline = [DateTime]::UtcNow.AddMinutes(5)
    do {
        $taskSerial = Get-KitapEmulator
        if ($taskSerial) {
            $taskBoot = & $taskAdb -s $taskSerial shell getprop sys.boot_completed 2>$null
            if ($LASTEXITCODE -eq 0 -and "$taskBoot".Trim() -eq '1') { return $taskSerial }
        }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $taskBootDeadline)
    throw 'Android açılışı zaman aşımına uğradı. Emülatör ekranını kontrol edin.'
}

if ($Mode -eq 'Metro') {
    if (Get-KitapMetro) { Write-Host 'Dev server ready. Mevcut Kitap Metro kullanılıyor.'; exit 0 }
    $env:FORCE_COLOR = '0'
    Push-Location $taskMobileRoot
    try {
        & node (Join-Path $taskMobileRoot 'node_modules\react-native\cli.js') start --host 127.0.0.1 --port $MetroPort --max-workers 2
        if ($LASTEXITCODE -ne 0) { throw 'Metro başlatılamadı.' }
        if (-not (Get-KitapMetro)) { throw 'Metro hazır duruma geçmeden kapandı. Port ve Metro terminalini kontrol edin.' }
        Write-Host 'Dev server ready. Mevcut Kitap Metro kullanılıyor.'
    } finally { Pop-Location }
    exit 0
}
if ($Mode -eq 'Emulator') {
    if (Get-KitapEmulator) { Write-Host "Kitap emulator ready: $(Wait-KitapEmulator)"; exit 0 }
    if ($EmulatorPort % 2 -ne 0) { throw 'EmulatorPort çift sayı olmalı.' }
    if (((& $taskAdb devices) -join "`n") -match "(?m)^emulator-$EmulatorPort\s") { throw "Emulator portu $EmulatorPort kullanımda. Başka emülatör kapatılmadı." }
    $taskEmulator = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
    if ((& $taskEmulator -list-avds) -notcontains $Avd) { throw "AVD bulunamadı: $Avd" }
    Write-Host "Android emülatörü açılıyor: $Avd"
    # Keep the interactive emulator owned by its own long-running VS Code task.
    & $taskEmulator -avd $Avd -port $EmulatorPort -gpu software -no-snapshot -no-boot-anim -no-audio
    if ($LASTEXITCODE -ne 0) { throw 'Emülatör kapandı; emülatör görev terminalini inceleyin.' }
    exit 0
}
if (-not (Get-KitapMetro)) { throw 'Önce VS Code içinden Kitap: Metro görevini başlatın.' }

if ($Mode -eq 'Prepare') {
# Build before booting a new emulator to reduce pressure on the Windows host.
Push-Location (Join-Path $taskMobileRoot 'android')
try {
    & '.\gradlew.bat' ':app:assembleDebug' '-PreactNativeArchitectures=x86_64' "-PreactNativeDevServerPort=$MetroPort" '-PreactNativeDevServerIp=127.0.0.1' --no-daemon --max-workers=2 --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android geliştirme derlemesi başarısız.' }
} finally { Pop-Location }
Write-Host 'Metro ilk JavaScript paketini hazırlıyor…'
$taskWarmFile = Join-Path $ToolchainRoot ("dev-warmup-" + [Guid]::NewGuid().ToString('N') + '.js')
try {
    Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:$MetroPort/index.bundle?platform=android&dev=true&lazy=true&minify=false&app=com.kitap.dev&modulesOnly=false&runModule=true&excludeSource=true&sourcePaths=url-server" -TimeoutSec 600 -OutFile $taskWarmFile
} finally {
    if (Test-Path -LiteralPath $taskWarmFile) { Remove-Item -LiteralPath $taskWarmFile -Force }
}
Write-Host 'JavaScript paketi hazır.'
exit 0
}
$taskTargetSerial = Wait-KitapEmulator
& $taskAdb -s $taskTargetSerial reverse "tcp:$MetroPort" "tcp:$MetroPort"
if ($LASTEXITCODE -ne 0) { throw 'Metro bağlantısı kurulamadı.' }
& $taskAdb -s $taskTargetSerial install -r (Join-Path $taskMobileRoot 'android\app\build\outputs\apk\debug\app-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'Bookmuse Dev kurulamadı; mevcut release kaldırılmadı.' }
& $taskAdb -s $taskTargetSerial shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.kitap.dev/com.kitap.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'Bookmuse Dev açılamadı.' }
Write-Host 'Bookmuse Dev açıldı. TypeScript/React dosyalarını kaydedince Fast Refresh uygulanır.'
Write-Host 'Kotlin/JNI değişikliklerinde Ctrl+Shift+B ile yeniden derleyin. Metro terminali açık kalsın.'
