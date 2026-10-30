[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9_.$,#*?]+$')]
    [string]$Test,
    [ValidatePattern('^[1-9][0-9]*[mMgG]$')]
    [string]$Heap = '6g'
)

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

function Assert-OwnedPath([string]$Path) {
    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($repoRoot + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase)) {
        throw "Output escapes repository: $full"
    }
    $cursor = $full
    while ($cursor) {
        if (Test-Path -LiteralPath $cursor) {
            $item = Get-Item -LiteralPath $cursor -Force
            if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw "Reparse point is not allowed for managed output: $cursor"
            }
        }
        $cursor = [IO.Path]::GetDirectoryName($cursor)
    }
    return $full
}

$temporary = Assert-OwnedPath (Join-Path $repoRoot 'target/bhe-tmp')
$repository = Assert-OwnedPath (Join-Path $repoRoot '.mvn/repository')
$maven = Get-Command mvn.cmd -CommandType Application -ErrorAction Stop | Select-Object -First 1
$keys = @('TEMP', 'TMP', 'TMPDIR', 'MAVEN_OPTS', 'PYTHONDONTWRITEBYTECODE')
$previous = @{}
foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
$previousLocation = Get-Location
try {
    [IO.Directory]::CreateDirectory($temporary) | Out-Null
    [IO.Directory]::CreateDirectory($repository) | Out-Null
    $env:TEMP = $temporary
    $env:TMP = $temporary
    $env:TMPDIR = $temporary
    $env:PYTHONDONTWRITEBYTECODE = '1'
    $tmpOption = '-Djava.io.tmpdir="' + $temporary + '"'
    $env:MAVEN_OPTS = (($previous['MAVEN_OPTS'], $tmpOption) -join ' ').Trim()
    Set-Location -LiteralPath $repoRoot
    $mavenArguments = @('-q', "-Dtest=$Test", "-Dmaven.repo.local=$repository",
        "-DargLine=-Xmx$Heap $tmpOption", 'test')
    & $maven.Source @mavenArguments
    if ($LASTEXITCODE -ne 0) { throw "Maven exited with code $LASTEXITCODE" }
}
finally {
    Set-Location -LiteralPath $previousLocation.Path
    foreach ($key in $keys) {
        [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process')
    }
}
