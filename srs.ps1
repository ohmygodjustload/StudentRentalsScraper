# StudentRentalsScraper launcher (Windows)
# Usage: .\srs.ps1 [command] [options]
#        .\srs.ps1                 # interactive menu

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$Jar = Join-Path $Root "target\scraper-all.jar"

function Needs-Rebuild {
    if (-not (Test-Path $Jar)) {
        return $true
    }
    $jarTime = (Get-Item $Jar).LastWriteTimeUtc
    $watch = @("pom.xml") + @(Get-ChildItem -Path (Join-Path $Root "src") -Recurse -File -ErrorAction SilentlyContinue)
    foreach ($item in $watch) {
        $path = if ($item -is [string]) { Join-Path $Root $item } else { $item.FullName }
        if ((Test-Path $path) -and ((Get-Item $path).LastWriteTimeUtc -gt $jarTime)) {
            return $true
        }
    }
    return $false
}

if (Needs-Rebuild) {
    Write-Host "[srs] Building jar..."
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

& java -jar $Jar @args
exit $LASTEXITCODE
