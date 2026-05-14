<div align="center">
  <img src="src/main/resources/images/head.png" width="400" alt="NeXAS_DX Preview">
  <h1>NeXAS_DX</h1>
  <p>
    <strong>用于 NeXAS 引擎（GIGA/戯画/Entergram）资源转换与 BHE → BSDX 角色移植的 JavaFX 桌面工具</strong>
  </p>


  <p>
    <img src="https://img.shields.io/badge/Language-Java_17-ed8b00?style=flat-square&logo=openjdk" alt="Java">
    <img src="https://img.shields.io/badge/GUI-JavaFX_21-blue?style=flat-square" alt="JavaFX">
    <img src="https://img.shields.io/badge/Engine-NeXAS-purple?style=flat-square" alt="Engine">
    <img src="https://img.shields.io/badge/Status-Active-success?style=flat-square" alt="Status">
  </p>
</div>

---

> 首先非常感谢 [kdw-code](https://github.com/kdw-code)，感谢他引领我入门逆向工程做出的巨大贡献，没他就没有此项目。（恩情还不完）

[English](README.md)

---

## 📖 概述

本应用为 JavaFX 桌面工具，用于在 NeXAS 引擎游戏资源与 JSON 之间互转，面向 Mod 制作、格式研究，以及 BHE → BSDX 移植实验。

**最终目标**：将 BHE（Baldr Heart EXE）中的角色移植到 BSDX（Baldr Sky DiveX）。虽然逆向出了文件结构，但因为符号缺失，许多字段含义未知，需要大量测试。

**如果你对此感兴趣，就算不懂编程也无所谓，可随时联系我。**

---

## ⚙️ 功能

- **GUI 转换**：选择文件/目录，单个或批量执行解析/回写
- **往返测试**：保证二进制 ↔ JSON 一致性
- **跨平台 JAR** + 可选 Windows 自包含包（内置 JRE）
- **附带真实游戏资产**用于研究（位于 `src/main/resources/game`）

---

## 🎮 支持的引擎与格式

| 引擎/游戏 | 解析 | 生成 | 说明 |
|-----------|------|------|------|
| **BSDX** (Baldr Sky DiveX) | `.waz` `.mek` `.spm` `.grp` `.bin` `.dat` `.map` | 同左 | 覆盖度最高 |
| **BHE** (Baldr Heart EXE) | `.waz` `.mek` `.spm` `.grp` | `.spm` `.grp` | 用于对照/迁移 |
| **CLARIAS** | `.dat` `.mek` `.grp` | `.dat` `.mek` `.grp` | `mek/grp` 当前按现有 CLARIAS 伪代码/反编译证据实现，待真实样本补入后再做实测校准 |

---

## 🔧 环境要求

- Windows 10+（JavaFX 依赖使用 `javafx-*-win`）
- JDK 17（推荐 BellSoft Full JDK）
- Maven 3.9+

---

## 🚀 快速开始

> 建议直接使用 IDEA 并将其作为 Maven 项目打开，直接执行 `NeXASConverter` → `Lifecycle` → `package` 即可打包出产物。默认为自包含的 Windows 包，有需要请自行编辑 `pom.xml`

### 构建

```bash
git clone <repo-url>
cd NeXAS_DX
mvn clean package -DskipTests
```

**产物**：
- Shaded JAR：`target/NeXAS_DX-x.x.x-FULL.jar`
- Windows 自包含包（可选）：
  ```bash
  mvn clean package -DskipTests -Dpackager.jdk="C:\Program Files\BellSoft\jdk-17"
  ```

### 运行

1. **推荐**：从 [Releases](../../releases) 下载预构建的 Windows 包
2. **自构建包**：运行 `target/javapackager/NeXASConverter.exe`
3. **Shaded JAR**：
   
   ```powershell
   set PATH_TO_FX=C:\path\to\javafx-sdk-21\lib
   java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -jar target/NeXAS_DX-x.x.x-FULL.jar
   ```

---

## 📂 项目结构

```
├── controller/          # GUI 控制器
├── service/             # 格式注册与引擎调度
├── dto/<engine>/        # 各引擎/格式的 DTO （重点代码，只对文件格式感兴趣的请直接看该包下的 parser 和相关类）
├── io/                  # BinaryReader/Writer
├── util/                # 辅助工具与 PAC 工具
├── src/test/.../bhe2bsdx/  # BHE→BSDX 移植工具
└── src/main/resources/game/  # 真实游戏资产（请谨慎处理）
```

---

## 🔬 BHE → BSDX 移植

实验性流水线，用于将 BHE 角色移植到 BSDX。

```bash
mvn "-Dtest=com.giga.nexas.bhe2bsdx.TransferTest#testPipeline" test
```

**输出**：`src/main/resources/testBhe/` → `Update3.pac`

详细文档请参阅 [`src/test/java/com/giga/nexas/bhe2bsdx/README.md`](src/test/java/com/giga/nexas/bhe2bsdx/README.md)

---

## ⚠️ 注意事项

- 默认字符集：`windows-31j`（日语 Shift-JIS）
- JSON/DTO 必须包含 `extensionName`，适配器靠它分派
- 测试假定路径为 `D:\Code\NeXAS_DX`；若不同请修改常量或创建符号链接
- `develop` 分支有最新代码；release 仅在 `main` 进行

---

## 📝 许可

MIT License，详见 [LICENSE](LICENSE)。
