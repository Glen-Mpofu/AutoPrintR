param([string]$filepath)

# Define log file
$appFolder = Join-Path $env:ProgramData "AutoPrintR"
if (-not (Test-Path $appFolder)) { New-Item -ItemType Directory -Path $appFolder | Out-Null }

$logFile = Join-Path $appFolder "log_file.txt"
if (-not (Test-Path $logFile)) { New-Item -ItemType File -Path $logFile | Out-Null }

# Print function
function print {
    param([string]$file)
    try {
        Start-Process -FilePath $file -Verb Print -ErrorAction Stop
        Write-Host "Printed: $file"
        Add-Content -Path $logFile -Value "$(Get-Date): Printed $file"
    } catch {
        Write-Host "Print failed: $file"
        Add-Content -Path $logFile -Value "$(Get-Date): Print failed $file - $($_.Exception.Message)"
    }
}

# Print any supported file
$ext = [System.IO.Path]::GetExtension($filepath).ToLower()
switch ($ext) {
    ".doc"  { print $filepath }
    ".docx" { print $filepath }
    ".xls"  { print $filepath }
    ".xlsx" { print $filepath }
    ".ppt"  { print $filepath }
    ".pptx" { print $filepath }
    default { Write-Host "Unsupported file type: $ext"; exit }
}
