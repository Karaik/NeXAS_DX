<div align="center">
  <img src="src/main/resources/images/head.png" width="400" alt="NeXAS_DX Preview">
  <h1>NeXAS_DX</h1>
  <p>
    <strong>JavaFX desktop tool for NeXAS engine (GIGA/戯画/Entergram) resource conversion and BHE → BSDX character porting</strong>
  </p>


  <p>
    <img src="https://img.shields.io/badge/Language-Java_17-ed8b00?style=flat-square&logo=openjdk" alt="Java">
    <img src="https://img.shields.io/badge/GUI-JavaFX_21-blue?style=flat-square" alt="JavaFX">
    <img src="https://img.shields.io/badge/Engine-NeXAS-purple?style=flat-square" alt="Engine">
    <img src="https://img.shields.io/badge/Status-Active-success?style=flat-square" alt="Status">
  </p>
</div>

---

> Huge thanks to [kdw-code](https://github.com/kdw-code) for leading me into reverse engineering and for his massive contributions. This project wouldn't exist without him.

[中文说明](README_zh.md)

---

## 📖 Overview

A JavaFX desktop application for converting **NeXAS engine** game resources between binary and JSON formats. Designed for modding, format research, and an experimental BHE → BSDX character transplant project.

**End goal**: Port BHE (Baldr Heart EXE) characters into BSDX (Baldr Sky DiveX). File structures are reverse-engineered, but missing symbols mean many fields remain unknown and require extensive testing.

If you're interested—even without coding experience—feel free to reach out.

---

## ⚙️ Features

- **GUI-based conversion**: Select files/folders, run single or batch conversions
- **Round-trip testing**: Ensures binary ↔ JSON consistency for each format
- **Cross-platform JAR** + optional Windows bundle with embedded JRE
- **Real game assets included** for research (under `src/main/resources/game`)

---

## 🎮 Supported Engines & Formats

| Engine/Game | Parse | Generate | Notes |
|-------------|-------|----------|-------|
| **BSDX** (Baldr Sky DiveX) | `.waz` `.mek` `.spm` `.grp` `.bin` `.dat` | Same | Most complete coverage |
| **BHE** (Baldr Heart EXE) | `.waz` `.mek` `.spm` `.grp` | `.spm` `.grp` | Used for comparison/transfer |
| **CLARIAS** | `.dat` `.mek` `.grp` | `.dat` `.mek` `.grp` | `mek/grp` currently follow the available CLARIAS pseudocode/decompile evidence; real samples are still needed for calibration |

---

## 🔧 Requirements

- Windows 10+ (JavaFX uses `javafx-*-win` classifiers)
- JDK 17 (BellSoft Full JDK recommended)
- Maven 3.9+

---

## 🚀 Quick Start

> Recommended: Open in IntelliJ IDEA as a Maven project, then run `NeXASConverter` -> `Lifecycle` -> `package` to build. Default output is a self-contained Windows package; edit `pom.xml` if needed.

### Build

```bash
git clone <repo-url>
cd NeXAS_DX
mvn clean package -DskipTests
```

**Outputs**:
- Shaded JAR: `target/NeXAS_DX-x.x.x-FULL.jar`
- Windows bundle (optional):
  ```bash
  mvn clean package -DskipTests -Dpackager.jdk="C:\Program Files\BellSoft\jdk-17"
  ```

### Run

1. **Recommended**: Download prebuilt Windows package from [Releases](../../releases)
2. **Self-built bundle**: Run `target/javapackager/NeXASConverter.exe`
3. **Shaded JAR**:

   ```powershell
   set PATH_TO_FX=C:\path\to\javafx-sdk-21\lib
   java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -jar target/NeXAS_DX-x.x.x-FULL.jar
   ```

---

## 📂 Project Structure

```
├── controller/          # GUI controllers
├── service/             # Format registration & engine dispatch
├── dto/<engine>/        # DTOs per engine/format (key code - check parsers here if you only care about file formats)
├── io/                  # BinaryReader/Writer
├── util/                # Helpers & PAC utilities
├── src/test/.../bhe2bsdx/  # BHE→BSDX transplant tooling
└── src/main/resources/game/  # Real game assets (handle with care)
```

---

## 🔬 BHE → BSDX Transplant

Experimental pipeline to port BHE characters into BSDX.

```bash
mvn "-Dtest=com.giga.nexas.bhe2bsdx.TransferTest#testPipeline" test
```

**Output**: `src/main/resources/testBhe/` → `Update3.pac`

For detailed documentation, see [`src/test/java/com/giga/nexas/bhe2bsdx/README.md`](src/test/java/com/giga/nexas/bhe2bsdx/README.md)

---

## ⚠️ Notes

- Default charset: `windows-31j` (Japanese Shift-JIS)
- JSON/DTOs must include `extensionName` for adapter dispatch
- Tests assume path `D:\Code\NeXAS_DX`; adjust or create symlink if different
- `develop` branch has latest code; releases are from `main` only

---

## 📝 License

MIT License. See [LICENSE](LICENSE).
