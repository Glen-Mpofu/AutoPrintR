param(
    [string]$filepath
)

try {
    $ext = [System.IO.Path]::GetExtension($filepath).ToLower()

    switch ($ext) {
        ".docx" { $app = New-Object -ComObject Word.Application }
        ".doc"  { $app = New-Object -ComObject Word.Application }
        ".xls"  { $app = New-Object -ComObject Excel.Application }
        ".xlsx" { $app = New-Object -ComObject Excel.Application }
        ".ppt"  { $app = New-Object -ComObject PowerPoint.Application }
        ".pptx" { $app = New-Object -ComObject PowerPoint.Application }
        default { exit }
    }

    $app.Visible = $false

    if ($ext -like ".doc*") {
        $doc = $app.Documents.Open($filepath, [ref]$false, [ref]$true)
        $doc.PrintOut()
        $doc.Close()
    }
    elseif ($ext -like ".xls*") {
        $wb = $app.Workbooks.Open($filepath)
        $wb.PrintOut()
        $wb.Close($false)
    }
    elseif ($ext -like ".ppt*") {
        $ppt = $app.Presentations.Open($filepath, $false, $false, $false)
        $ppt.PrintOut()
        $ppt.Close()
    }

    $app.Quit()
}
catch {
    $errorMessage = $_.Exception.Message
    $logPath = Join-Path ([System.IO.Path]::GetDirectoryName($filepath)) "print_error_log.txt"
    Add-Content -Path $logPath -Value "$(Get-Date): Error printing file $filepath - $errorMessage"
    exit 1
}
