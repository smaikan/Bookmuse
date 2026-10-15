param(
    [string]$ToolchainRoot = (Join-Path $env:USERPROFILE '.codex\toolchains\kitap-android'),
    [string]$Serial,
    [string]$OutputPath = (Join-Path (Split-Path $PSScriptRoot -Parent) 'docs\acceptance\device-inventory.json')
)
$ErrorActionPreference = 'Stop'
$taskEnvironment = Get-Content -LiteralPath (Join-Path $ToolchainRoot 'environment.json') -Raw | ConvertFrom-Json
$taskAdb = Join-Path $taskEnvironment.androidHome 'platform-tools\adb.exe'
$taskList = & $taskAdb devices -l
if ($LASTEXITCODE -ne 0) { throw 'ADB device enumeration failed' }
$taskDevices = @($taskList | Where-Object { $_ -match '^\S+\s+device(?:\s|$)' } | ForEach-Object { ($_ -split '\s+')[0] })
if (-not $Serial) {
    if ($taskDevices.Count -eq 0) {
        @{schemaVersion=1;status='no_authorized_device';capturedAt=[DateTimeOffset]::UtcNow.ToString('o');androidApi=$null;abi=$null;measuredRamMiB=$null} | ConvertTo-Json | Set-Content -LiteralPath $OutputPath -Encoding utf8
        Write-Output 'No authorized Android device attached; physical acceptance remains pending.'
        exit 0
    }
    if ($taskDevices.Count -gt 1) { throw 'Multiple devices; provide -Serial to select the intended tablet' }
    $Serial = $taskDevices[0]
}
if ($taskDevices -notcontains $Serial) { throw 'The selected device is not authorized/ready' }
function Read-DeviceProperty([string]$Name) {
    $taskValue = & $taskAdb -s $Serial shell getprop $Name
    if ($LASTEXITCODE -ne 0) { throw "Cannot read device property: $Name" }
    return ($taskValue -join '').Trim()
}
$taskMemory = & $taskAdb -s $Serial shell cat /proc/meminfo
$taskMemLine = $taskMemory | Select-String '^MemTotal:\s+(\d+)\s+kB'
$taskMeasuredRam = if ($taskMemLine) { [math]::Round([double]$taskMemLine.Matches[0].Groups[1].Value/1024, 0) } else { $null }
$taskResult = @{
    schemaVersion=1;status='device_observed';capturedAt=[DateTimeOffset]::UtcNow.ToString('o')
    manufacturer=(Read-DeviceProperty 'ro.product.manufacturer')
    model=(Read-DeviceProperty 'ro.product.model')
    androidVersion=(Read-DeviceProperty 'ro.build.version.release')
    androidApi=(Read-DeviceProperty 'ro.build.version.sdk')
    abi=(Read-DeviceProperty 'ro.product.cpu.abilist')
    measuredRamMiB=$taskMeasuredRam
    emulator=((Read-DeviceProperty 'ro.kernel.qemu') -eq '1')
    applicationValidated=$false
}
$taskResult | ConvertTo-Json | Set-Content -LiteralPath $OutputPath -Encoding utf8
$taskResult | Select-Object status,manufacturer,model,androidVersion,androidApi,abi,measuredRamMiB,emulator | ConvertTo-Json
