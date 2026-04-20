package com.giga.nexas.transfer.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BaldrSkyCrashDumpScriptWriter {

    private static final String ENABLE_SCRIPT = """
$ErrorActionPreference = 'Stop'
$processName = 'BaldrSky.exe'
$keyPath = 'HKCU:\\Software\\Microsoft\\Windows\\Windows Error Reporting\\LocalDumps\\' + $processName
$dumpDir = Join-Path $PSScriptRoot 'crash_dumps'

New-Item -ItemType Directory -Force -Path $dumpDir | Out-Null
New-Item -Path $keyPath -Force | Out-Null
New-ItemProperty -Path $keyPath -Name DumpFolder -PropertyType ExpandString -Value $dumpDir -Force | Out-Null
New-ItemProperty -Path $keyPath -Name DumpCount -PropertyType DWord -Value 10 -Force | Out-Null
New-ItemProperty -Path $keyPath -Name DumpType -PropertyType DWord -Value 2 -Force | Out-Null

Write-Host "BaldrSky LocalDumps enabled."
Write-Host "Dump folder: $dumpDir"
""";

    private static final String DISABLE_SCRIPT = """
$ErrorActionPreference = 'Stop'
$processName = 'BaldrSky.exe'
$keyPath = 'HKCU:\\Software\\Microsoft\\Windows\\Windows Error Reporting\\LocalDumps\\' + $processName

if (Test-Path $keyPath) {
    Remove-Item -Path $keyPath -Recurse -Force
    Write-Host "BaldrSky LocalDumps disabled."
} else {
    Write-Host "BaldrSky LocalDumps was not configured."
}
""";

    private BaldrSkyCrashDumpScriptWriter() {
    }

    public static void writeScripts(Path outputDir) throws IOException {
        if (outputDir == null) {
            return;
        }
        Files.createDirectories(outputDir);
        Files.writeString(
                outputDir.resolve("enable_baldrsky_dumps.ps1"),
                ENABLE_SCRIPT,
                StandardCharsets.UTF_8
        );
        Files.writeString(
                outputDir.resolve("disable_baldrsky_dumps.ps1"),
                DISABLE_SCRIPT,
                StandardCharsets.UTF_8
        );
    }
}
