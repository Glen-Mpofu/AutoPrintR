param(
    [string]$filepath
)

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
    $app.Quit()
}
elseif ($ext -like ".xls*") {
    $wb = $app.Workbooks.Open($filepath)
    $wb.PrintOut()
    $wb.Close($false)
    $app.Quit()
}
elseif ($ext -like ".ppt*") {
    $ppt = $app.Presentations.Open($filepath, $false, $false, $false)
    $ppt.PrintOut()
    $ppt.Close()
    $app.Quit()
}
