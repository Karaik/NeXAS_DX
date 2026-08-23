[CmdletBinding()]
param(
    [string]$ReportPath = "docs/project-deep-dive/mod-sample-skill-behavior-diff.md",
    [string]$ManifestPath = "docs/project-deep-dive/mod-sample-skill-behavior-diff.manifest.json"
)

$ErrorActionPreference = "Stop"
$utf8NoBom = [Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom
$env:PYTHONIOENCODING = "utf-8"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$modRoot = Join-Path $repoRoot "src\main\resources\modsample\Update4"
$dumpRoot = Join-Path $repoRoot "target\mod_diff_analyzer\dump"
$classpathFile = Join-Path $repoRoot "target\mod_diff_analyzer\classpath.txt"
$javaSource = Join-Path $PSScriptRoot "ModSampleJsonDumper.java"
$pythonSource = Join-Path $PSScriptRoot "analyze.py"

Push-Location $repoRoot
try {
    New-Item -ItemType Directory -Force -Path (Split-Path $classpathFile) | Out-Null

    if (Test-Path -LiteralPath $dumpRoot) {
        Remove-Item -Recurse -Force -LiteralPath $dumpRoot
    }
    New-Item -ItemType Directory -Force -Path $dumpRoot | Out-Null

    & mvn -q -DskipTests compile
    if ($LASTEXITCODE -ne 0) {
        throw "Maven 编译失败，退出码：$LASTEXITCODE"
    }

    & mvn -q dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven 类路径生成失败，退出码：$LASTEXITCODE"
    }

    $dependencyClasspath = (Get-Content -Raw -LiteralPath $classpathFile).Trim()
    $javaClasspath = (Join-Path $repoRoot "target\classes") + ";" + $dependencyClasspath

    & java "-Dfile.encoding=UTF-8" --class-path $javaClasspath $javaSource $modRoot $dumpRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Mod 载荷解码失败，退出码：$LASTEXITCODE"
    }

    & python $pythonSource `
        --repo-root $repoRoot `
        --mod-dump $dumpRoot `
        --report $ReportPath `
        --manifest $ManifestPath
    if ($LASTEXITCODE -ne 0) {
        throw "战斗行为差分分析失败，退出码：$LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
