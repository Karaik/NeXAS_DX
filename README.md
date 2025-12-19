> Huge thanks to [kdw-code](https://github.com/kdw-code) for leading me into reverse engineering and for his massive contributions. This project wouldn’t exist without him, and I wouldn’t have gotten started on RE on my own.

[中文说明](README_zh.md)

# NeXAS_DX

JavaFX desktop tool for converting **NeXAS engine (GIGA/戯画)** resources between binary and JSON, aimed at modding, format research, and a BHE → BSDX transplant experiment. If you’re interested—even without coding experience—feel free to reach out.

The end goal is to port BHE characters into BSDX. We have the file structures, but missing symbols mean many fields are still unknown and require time-consuming testing.

![image-20251215145506259](docimages/image-20251215145506259.png)

## What it does
- Parse and rebuild NeXAS resources via GUI: select files/folders, run single or batch conversions.
- Round-trip testing for each format to ensure binary/JSON consistency.
- Shaded JAR and optional self-contained Windows bundle (embedded JRE via `javapackager`).
- Comes with real game assets for research (kept under `src/main/resources/game`—handle with care).

## Engines & formats
| Engine/Game            | Parse | Generate | Notes |
|------------------------| --- | --- | --- |
| BSDX (Baldr Sky DiveX) | `.waz`, `.mek`, `.spm`, `.grp`, `.bin`, `.dat` | same set | Most complete coverage. `.bin` skips `__GLOBAL.bin` during tests. |
| BHE (Baldr Heart EXE)  | `.waz`, `.mek`, `.spm`, `.grp` | `.spm`, `.grp` | Used mainly for comparison/transfer. |
| CLARIAS                | `.dat` | `.dat` |  |

## Requirements
- Windows 10+ (JavaFX dependencies use `javafx-*-win` classifiers).
- JDK 17 (BellSoft full JDK recommended), Maven 3.9+.
- UTF-8 (no BOM) + CRLF for source/resources.

## Build
```bash
git clone <repo-url>
cd NeXAS_DX
mvn clean package -DskipTests
```
Outputs:
- Shaded JAR: `target/NeXAS_DX-x.x.x-FULL.jar`
- Optional Windows bundle with embedded JRE (needs your JDK path):  
  `mvn clean package -DskipTests -Dpackager.jdk="C:\\Program Files\\BellSoft\\jdk-17"`

## Run the GUI
- Grab the prebuilt Windows package from Releases (unzip and run).
- Bundled app you build yourself: run `target/javapackager/NeXASConverter.exe`.
- Shaded JAR (requires JavaFX on module path):
  ```powershell
  set PATH_TO_FX=C:\path\to\javafx-sdk-21\lib
  java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base `
       -jar target/NeXAS_DX-x.x.x-FULL.jar
  ```

## Using the app (GUI flow)
1. Pick a workspace folder (game resources) and optional output folder.
2. The tree/card view lists detected files by engine/type; double-click or press **Run selected** to parse → JSON, or generate → binary depending on mode.
3. Batch convert by category or **Run all**. Progress/status updates in the footer.
4. Keep backups—only experiment on copies of game data.

## Tests & workflows
- Run GUI without tests: `mvn -q -DskipTests compile` then `mvn javafx:run`.
- BSDX `.dat` chain (in order):  
  1) `mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testGenerateDatJsonFiles" test`  
  2) `mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testGenerateDatFilesByJson" test`  
  3) `mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testDatParseGenerateBinaryConsistency" test` (auto-cleans JSON/Generated on success)  
  Optional CSV patching: run `testToCsv` / `testCsvPatchToJson` before step 3.
- BSDX other formats: `mvn "-Dtest=com.giga.nexas.bsdx.TestBin#testGenerateBinJsonFiles" test` (and similarly `TestGrp/TestMek/TestSpm/TestWaz`); no auto-clean.
- BHE: `mvn "-Dtest=com.giga.nexas.bhe.TestGrp" test`, etc.; outputs keep intermediate files.
- BHE → BSDX pipeline (experimental): `mvn "-Dtest=com.giga.nexas.bhe2bsdx.TransferTest#testPipeline" test` (writes to `src/main/resources/testBhe`; requires full grp/mek/waz/spm sets).

**Test outputs** live under `src/main/resources` (e.g., `datBsdxJson`, `grpBsdxGenerated`) and are gitignored. Delete them when not needed to keep the tree light.

## Project layout
- `MainApplication` + `resources/fxml/MainView.fxml` — JavaFX entry and UI.
- `controller/*` — GUI controllers (file picker, mode tree, branch grid, settings, drag/drop, actions).
- `service/*BinService` + `service/engine/*Adapter` — format registration and engine dispatch.
- `dto/<engine>/<format>` — DTOs per engine/game/format; add new ones here.
- `io/BinaryReader|BinaryWriter`, `util/*` — common binary/JSON helpers and PAC utilities.
- `transfer/*` + `src/test/java/com/giga/nexas/bhe2bsdx` — transplant experiment tooling.
- `src/main/resources/game/*` — real assets for testing; handle responsibly.

## Conventions & pitfalls
- Keep `extensionName` in JSON/DTOs; adapters dispatch by it.
- Default charset is `windows-31j` (Japanese same as `Shift-jis`); if you override it, sync `WorkspaceState.charset`.
- Tests are single-threaded; running multiple heavy suites in parallel will clash on output folders.
- Windows paths are assumed in some tests (e.g., `D:\A\NeXAS_DX`); adjust constants or use a symlink if you use a different path.
- The code on the `develop` branch is generally the most up to date. If you encounter a bug or notice anything that appears unfinished, please switch to the `develop` branch to check. However, releases are made only from `main`.
## License
MIT License. See `LICENSE`.
