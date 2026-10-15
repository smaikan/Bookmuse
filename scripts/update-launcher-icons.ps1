param(
    [string]$Source = (Join-Path $PSScriptRoot '..\mobile\assets\branding\bookmuse-icon.png')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$taskProjectRoot = Split-Path $PSScriptRoot -Parent
$taskResourceRoot = Join-Path $taskProjectRoot 'mobile\android\app\src\main\res'
$taskSourcePath = (Resolve-Path -LiteralPath $Source).Path
$taskSourceImage = [System.Drawing.Image]::FromFile($taskSourcePath)

# Deterministic asset sizing only: the supplied artwork is never regenerated or overwritten.
function Save-LauncherImage {
    param([string]$Destination, [int]$Size, [bool]$Round = $false, [double]$ContentScale = 1.0)
    $taskWorkSize = $Size * 4
    $taskWorkImage = [System.Drawing.Bitmap]::new($taskWorkSize, $taskWorkSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $taskOutputImage = [System.Drawing.Bitmap]::new($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $taskWorkGraphics = [System.Drawing.Graphics]::FromImage($taskWorkImage)
    $taskOutputGraphics = [System.Drawing.Graphics]::FromImage($taskOutputImage)
    $taskWrap = [System.Drawing.Imaging.ImageAttributes]::new()
    $taskCircle = [System.Drawing.Drawing2D.GraphicsPath]::new()
    try {
        foreach ($taskGraphics in @($taskWorkGraphics, $taskOutputGraphics)) {
            $taskFill = if (-not $Round -and $ContentScale -eq 1.0) {
                [System.Drawing.ColorTranslator]::FromHtml('#F5F3ED')
            } else { [System.Drawing.Color]::Transparent }
            $taskGraphics.Clear($taskFill)
            $taskGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $taskGraphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $taskGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $taskGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        }
        if ($Round) {
            $taskCircle.AddEllipse(0, 0, $taskWorkSize, $taskWorkSize)
            $taskWorkGraphics.SetClip($taskCircle)
        }
        $taskWrap.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::TileFlipXY)
        $taskContentSize = [int][Math]::Round($taskWorkSize * $ContentScale)
        $taskOffset = [int][Math]::Round(($taskWorkSize - $taskContentSize) / 2)
        $taskRectangle = [System.Drawing.Rectangle]::new($taskOffset, $taskOffset, $taskContentSize, $taskContentSize)
        $taskWorkGraphics.DrawImage($taskSourceImage, $taskRectangle, 0, 0, $taskSourceImage.Width, $taskSourceImage.Height, [System.Drawing.GraphicsUnit]::Pixel, $taskWrap)
        $taskOutputGraphics.DrawImage($taskWorkImage, [System.Drawing.Rectangle]::new(0, 0, $Size, $Size))
        New-Item -ItemType Directory -Path (Split-Path $Destination -Parent) -Force | Out-Null
        $taskOutputImage.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $taskCircle.Dispose()
        $taskWrap.Dispose()
        $taskOutputGraphics.Dispose()
        $taskWorkGraphics.Dispose()
        $taskOutputImage.Dispose()
        $taskWorkImage.Dispose()
    }
}

try {
    if ($taskSourceImage.Width -ne $taskSourceImage.Height -or $taskSourceImage.Width -lt 192) {
        throw 'Launcher source must be square and at least 192 x 192 pixels.'
    }
    foreach ($taskDensity in @(
        @{Name='mdpi'; Size=48}, @{Name='hdpi'; Size=72}, @{Name='xhdpi'; Size=96},
        @{Name='xxhdpi'; Size=144}, @{Name='xxxhdpi'; Size=192}
    )) {
        $taskDensityDirectory = Join-Path $taskResourceRoot "mipmap-$($taskDensity.Name)"
        Save-LauncherImage -Destination (Join-Path $taskDensityDirectory 'ic_launcher.png') -Size $taskDensity.Size
        Save-LauncherImage -Destination (Join-Path $taskDensityDirectory 'ic_launcher_round.png') -Size $taskDensity.Size -Round $true
    }
    # 108 dp at 4x density. Padding keeps the supplied book mark in the central 66 dp safe circle.
    Save-LauncherImage -Destination (Join-Path $taskResourceRoot 'drawable-nodpi\bookmuse_launcher_foreground.png') -Size 432 -ContentScale 0.8
    Write-Output "Bookmuse launcher resources generated from $taskSourcePath ($($taskSourceImage.Width) x $($taskSourceImage.Height))."
} finally {
    $taskSourceImage.Dispose()
}
