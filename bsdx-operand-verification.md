# BSDX Operand（立即数）反编译校验指南

本文件作为 BSDX 虚拟机操作数（Operand）的反编译校验表与逆向开发指南。

## 1. 代码关联与设计背景

*   **Java 枚举类**：所有通过验证并命名的操作数函数均在枚举类 [Operand.java](file:///D:/Code/NeXAS_DX/src/main/java/com/giga/nexas/dto/bsdx/bin/consts/Operand.java) 中进行注册。
*   **数据描述源**：底层的中文描述和校验逻辑来自 CSV 数据表 `/research/bin立即数.csv`，由 `OperandDocRegistry.java` 动态加载。
*   **分发器逻辑**：
    *   **一级分发器（Tier 1）**：位于虚拟地址 `sub_7806F0`，以 `a3` 寄存器作为 opcode。未命中的项会落入二级分发器。
    *   **二级分发器（Tier 2）**：位于虚拟地址 `sub_44D000`，以 `a1` 寄存器作为 opcode，负责地狱战斗关卡、地图对象以及机体行为等特定的运行时分发。

---

## 2. 开发者指南：如何在 IDA Pro 中逆向新的操作数

当你在表单中遇到未命名的操作数（标为 `❓`）时，可以通过以下步骤对其进行下钻分析：

### 步骤 1：定位跳转分支表（Switch Table）
1. 在 IDA Pro 中跳转至 `sub_7806F0` 或 `sub_44D000`。
2. 寻找带有跳转表（Jump Table，如 `off_780A40`）的 switch 分支，双击跳转表即可列出所有对应 Case 的目标地址。

### 步骤 2：判断参数数量与宽度
1. 观察由寄存器 `a4` 传入的目标子函数所读取的数据。`a4` 通常是包含指令操作数的结构体/数组指针。
2. 参数解包规则：
   * `*a4` 或 `a4[0]` 代表第 1 个参数。
   * `a4[1]` 代表第 2 个参数，以此类推。
3. 检查宽度：如果以整型访问 `a4[n]`，则为 32 位 `int`；如果是 `*(char *)(a4 + offset)`，则说明是 `byte` 类型。

### 步骤 3：关联分析子函数语义
1. 分析具体的 Callee 内部逻辑（如 `sub_6FF120` 对应生成随机数 `Random`）。
2. 查看其返回值（通常存入 `eax`）并跟进其对游戏全局状态的影响，由此给操作数定义合理的命名。

---

## 3. 反编译校验表

> **校验图例**
> - ✓ 调度命中，且 callee 形态/行为强支持该名字
> - ◐ 调度命中，语义靠 callee 模式 + 参数槽推断（合理但未深挖底层）
> - ⚠ 调度命中，但 callee 符号损坏/多义，需游戏行为佐证
> - ❓ 未命名，仅给出分发表能确认的 callee + 参数形态
> - NOP 分发表里是空操作

---\r\n\r\n## Tier 1 dispatcher (`sub_7806F0`)

### 系统 / 流程 (0x00–0x16)

| Code | Hex | 名称 | 直接 Callee | 行为/参数 | 校验 |
|---|---|---|---|---|---|
| 0 | 0x00 | InitSystem | `sub_6FFBD0(v5, &v83)` | 系统初始化，结果写回 a1 | ◐ |
| 1 | 0x01 | — | `sub_6FFA10()` | 无参 | ❓ |
| 2 | 0x02 | — | `sub_700750()` | 无参 | ❓ |
| 3 | 0x03 | — | `sub_700710(v5)` | 1 参（全局上下文） | ❓ |
| 4 | 0x04 | — | `sub_7006D0()` | 无参 | ❓ |
| 5 | 0x05 | — | `sub_7006A0(v5)` | 1 参 | ❓ |
| 6 | 0x06 | — | `sub_7005C0(*a4)` | 1 参 | ❓ |
| 7 | 0x07 | — | `sub_7004E0(*a4)` | 1 参 | ❓ |
| 8 | 0x08 | — | `sub_7003B0(v5, *a4)` | 2 参 | ❓ |
| 9 | 0x09 | — | `sub_700280(v5, *a4)` | 2 参 | ❓ |
| 10 | 0x0A | — | `sub_7001A0(*a4)` | 1 参 | ❓ |
| 11 | 0x0B | Exit | `*a4==0 ? PostQuitMessage(0) : sub_6FF5B0()` | `*a4==0` 直接发 WM_QUIT，否则走 fallback | ✓ |
| 13 | 0x0D | Random | `sub_6FF120(*a4)` | 单参，返回 `[0, a)` 随机数（CSV 确认） | ✓ |
| 14 | 0x0E | GetCharCode | 内联数组读 `v84` | 取字符码，越界返回 0 | ✓ |
| 15 | 0x0F | PlayMovie | `sub_6FF640(&v83, a4[1], a4[2])` | 3 参 | ◐ |
| 16 | 0x10 | Wait | `sub_6FFA60(v5, *a4, a4[1])` | 3 参 | ◐ |
| 17 | 0x11 | Loading | `sub_727660()` | 无参 | ◐ |
| 18 | 0x12 | ShowCursor | `(*(*dword_BE88D8+12))(dword_BE88D8)` | vtable+12 调用 | ◐ |
| 19 | 0x13 | HideCursor | `(*(*dword_BE88D8+16))(dword_BE88D8)` | vtable+16 调用 | ◐ |
| 20 | 0x14 | ShowMenu | `sub_6FEC30(v5)` | 1 参 | ◐ |
| 21 | 0x15 | HideMenu | `byte_CB6944 = 0` | 清菜单可见标志 | ✓ |
| 22 | 0x16 | MessageBox | `byte_876F32` 守卫后 `sub_6FF7E0(bool)` | 1 bool 参 | ◐ |

### 系统标志 / 时间 / 调试 (0x17–0x45)

| Code | Hex | 名称 | 直接 Callee | 行为/参数 | 校验 |
|---|---|---|---|---|---|
| 23 | 0x17 | — | `sub_6FEB10()` | 无参 | ❓ |
| 24 | 0x18 | — | `..._119(a4[1])` → 写回 | 1 参 getter 形态 | ⚠ |
| 25 | 0x19 | — | `..._118()` → 写回 | 0 参 getter | ⚠ |
| 26 | 0x1A | — | `..._117(a4[1])` → 写回 | 1 参 getter | ⚠ |
| 27 | 0x1B | — | `sub_6FEE90(a4[1])` | 1 参 | ❓ |
| 28 | 0x1C | — | `sub_6FF9F0()` | 无参 | ❓ |
| 29 | 0x1D | — | `sub_6FF9C0()` | 无参 | ❓ |
| 30 | 0x1E | — | `byte_876F32` 守卫后 `sub_6FF990()` | 无参 | ❓ |
| 31 | 0x1F | — | `byte_876F32` 守卫后 `sub_6FEE50()` | 无参 | ❓ |
| 32 | 0x20 | — | `byte_876F32` 守卫后 `sub_72A180(*a4, a4[1])` | 2 参 | ❓ |
| 33 | 0x21 | — | 写回 `dword_8795EC` | 返回全局 | ❓ getter |
| 34 | 0x22 | — | `sub_6FF8C0(&v83, bool)` | 1 bool 参 | ❓ |
| 35 | 0x23 | — | `sub_6FEE30()` | 无参 | ❓ |
| 36 | 0x24 | — | `sub_6FEE10()` | 无参 | ❓ |
| 37 | 0x25 | — | `sub_6FEAE0()` | 无参 | ❓ |
| 38 | 0x26 | — | `sub_6FEAC0()` | 无参 | ❓ |
| 39 | 0x27 | — | `sub_6FEAA0()` | 无参 | ❓ |
| 40 | 0x28 | — | `byte_CA7509 = 0` | 清标志 | ❓ |
| 41 | 0x29 | — | `sub_6FEA70(a4[1])` | 1 参 | ❓ |
| 42 | 0x2A | — | `byte_CA750A = 0` | 清标志 | ❓ |
| 43 | 0x2B | — | `byte_CA750A = 1` | 置标志 | ❓ |
| 44 | 0x2C | — | `dword_CB68E8 = 0` | 清全局 | ❓ |
| 45 | 0x2D | — | `dword_CB68E8 = 1` | 置全局 | ❓ |
| 46 | 0x2E | — | `sub_6FFA40(v5)` | 1 参 | ❓ |
| 47 | 0x2F | — | `byte_876F32` 守卫后 `sub_6FEA10()` | 无参 | ❓ |
| 48 | 0x30 | — | `au_re_GetLocalTime(a4[3], a4[4], a4[5])` | 取系统本地时间 | ✓ |
| 49 | 0x31 | — | 写回 `byte_876F31` | 返回全局 | ❓ getter |
| 50 | 0x32 | — | 写回 `dword_876F8C` | 返回全局 | ❓ getter |
| 51 | 0x33 | — | 写回 `dword_876F88` | 返回全局 | ❓ getter |
| 52 | 0x34 | — | `au_re__sprintf(&v83, a4[7], a4[8], a4[1..6])` | 格式化字符串 | ✓ |
| 53 | 0x35 | — | `byte_876F29 && dword_867BFC>=0 → 1/0` | 条件返回 | ❓ |
| 54 | 0x36 | — | `byte_876F29 && dword_867C00>=0` | 条件返回 | ❓ |
| 55 | 0x37 | — | `byte_CA98E2 = 0` | 清标志 | ❓ |
| 56 | 0x38 | — | `byte_CA98E2 = 1` | 置标志 | ❓ |
| 58 | 0x3A | — | `sub_722150(v5)` | 1 参 | ❓ |
| 59 | 0x3B | — | `sub_726660(v5, *a4, 0)` | 3 参 | ❓ |
| 60 | 0x3C | — | 写回 `dword_876F44 == 9` | 条件返回 | ❓ |
| 61 | 0x3D | — | 写回 `byte_876F32` | 返回全局 | ❓ getter |
| 62 | 0x3E | — | `sub_6FED50(v85)` | 1 参 | ❓ |
| 63 | 0x3F | — | `sub_6FF6F0(&v83)` | 1 参 | ❓ |
| 64 | 0x40 | — | `sub_6FECD0()` | 无参 | ❓ |
| 65 | 0x41 | — | `sub_6FF4E0(&v83)` → 写回 | getter | ❓ |
| 66 | 0x42 | — | `sub_6FF410(&v83)` → 写回 | getter | ❓ |
| 67 | 0x43 | — | `sub_6FF0B0()` → 写回 | getter | ❓ |
| 68 | 0x44 | — | `sub_6FF030()` → 写回 | getter | ❓ |
| 69 | 0x45 | — | `sub_6FEFB0()` → 写回 | getter | ❓ |

### 对象系统 (0x46–0xBC)

> 这一段对应 `Operand.java` 的 `SetObject(70)`…`StopObject(130)` + 对象 group 系列。分发器里 0x46–0x4F 是带 `v85`/`v86` 缓冲的 Set*Object 族，0x50 起转为单 bool/少参的属性设置。逐条 callee 形态与 Operand.java 命名按位置吻合。

| Code | Hex | 名称 | 直接 Callee | 行为/参数 | 校验 |
|---|---|---|---|---|---|
| 70 | 0x46 | SetObject | `sub_6F7DE0(*a4, v85, a4[2..8])` | 多参 + 缓冲 | ◐ |
| 71 | 0x47 | SetAnimeObject | `sub_6F85A0(v85, a4[2..5])` | 多参 | ◐ |
| 72 | 0x48 | SetSpriteObject | `sub_6F8400(v85, a4[2..6])` | 多参 | ◐ |
| 73 | 0x49 | SetFontObject | `sub_6F7BB0(*a4, v85, v86, a4[3..13])` | 多参 + 双缓冲 | ◐ |
| 74 | 0x4A | SetCharcodeObject | `sub_6F8060(...)` | 多参 | ◐ |
| 75 | 0x4B | SetFillObject | `sub_6F79C0(*a4, a4[1..12])` | 多参 | ◐ |
| 76 | 0x4C | SetCopyObject | `sub_6F89B0(*a4, a4[1..12])` | 多参 | ◐ |
| 77 | 0x4D | SetCopyStandObject | `sub_6F8130(*a4, a4[1..12])` | 多参 | ◐ |
| 78 | 0x4E | SetCopyScreenObject | `sub_6F76F0(v5, *a4, a4[1..11])` | 多参 | ◐ |
| 79 | 0x4F | SetFaceObject | `sub_6F7FA0(a4[1], v86, v86, a4[3..9])` | 多参 + 缓冲 | ◐ |
| 80 | 0x50 | DelObject | `sub_6F74B0(bool)` | 1 bool | ◐ |
| 81 | 0x51 | MoveObject | `sub_6F73B0(v5, *a4, a4[3], bool, bool)` | 5 参 | ◐ |
| 82 | 0x52 | MoveSpeedObject | `sub_6F7290(...)` | 5 参 | ◐ |
| 83 | 0x53 | MoveVectorObject | `sub_6F7170(...)` | 5 参 | ◐ |
| 84 | 0x54 | ViewObject | `sub_6F7070(v5, *a4, bool, bool)` | 4 参 | ◐ |
| 85 | 0x55 | ZoomObject | `sub_6F6F50(...)` | 4 参 | ◐ |
| 86 | 0x56 | ZoomCycleObject | `sub_6F6DF0(*a4, a4[1], a4[5], bool)` | 4 参 | ◐ |
| 87 | 0x57 | RotateObject | `sub_6F6CF0(...)` | 4 参 | ◐ |
| 88 | 0x58 | RotateSpeedObject | `sub_6F6BF0(...)` | 4 参 | ◐ |
| 89 | 0x59 | RotateCycleObject | `sub_6F6AC0(*a4, bool)` | 2 参 | ◐ |
| 90 | 0x5A | TurnObject | `sub_6F69A0(...)` | 4 参 | ◐ |
| 91 | 0x5B | TurnSpeedObject | `sub_6F6880(...)` | 5 参 | ◐ |
| 92 | 0x5C | TurnCycleObject | `sub_6F6710(*a4, a4[1], a4[3], bool)` | 4 参 | ◐ |
| 93 | 0x5D | ShakeObject | `sub_6F6590(...)` | 8 参 | ◐ |
| 94 | 0x5E | RasterXObject | `sub_6F6480(...)` | 5 参 | ◐ |
| 95 | 0x5F | RasterYObject | `sub_6F6370(...)` | 5 参 | ◐ |
| 96 | 0x60 | WaveXObject | `sub_6F6250(...)` | 6 参 | ◐ |
| 97 | 0x61 | WaveYObject | `sub_6F6130(...)` | 6 参 | ◐ |
| 98 | 0x62 | NoiseXObject | `sub_6F6030(...)` | 4 参 | ◐ |
| 99 | 0x63 | NoiseYObject | `sub_6F5F30(...)` | 4 参 | ◐ |
| 100 | 0x64 | PinbokeObject | `sub_6F5E00(...)` | 6 参 | ◐ |
| 101 | 0x65 | AfterimageObject | `sub_6F5BC0(...)` | 9 参 | ◐ |
| 102 | 0x66 | FadeObject | `sub_6F5A80(...)` | 4 参 | ◐ |
| 103 | 0x67 | BlinkObject | `sub_6F5960(*a4, bool)` | 2 参 | ◐ |
| 104 | 0x68 | ClipObject | `sub_6F5850(...)` | 6 参 | ◐ |
| 105 | 0x69 | ScrollLinkObject | `sub_6F4F80(*a4)` | 1 参 | ◐ |
| 106 | 0x6A | BGMLinkObject | `sub_6F4F20(*a4)` | 1 参 | ◐ |
| 107 | 0x6B | RotateLinkObject | `byte_BF3564[452**a4] = bool` + `sub_6F4F00()` | 写数组 + 刷新 | ◐ |
| 108 | 0x6C | MirrorObject | `sub_6F4EB0(*a4, bool)` | 2 参 | ◐ |
| 109 | 0x6D | TileObject | `sub_6F4E70(*a4, bool)` | 2 参 | ◐ |
| 110 | 0x6E | ButtonObject | `sub_6F4DE0(bool, bool, bool)` | 3 bool | ◐ |
| 111 | 0x6F | SetAttributeObject | `dword_BF3548[113**a4] = a4[1]` + `sub_6F4DC0()` | 写数组 + 刷新 | ◐ |
| 112 | 0x70 | WaitObject | `sub_6F57F0(*a4, v5, bool)` | 3 参 | ◐ |
| 113 | 0x71 | WaitMoveObject | `sub_6F5790(...)` | 3 参 | ◐ |
| 114 | 0x72 | WaitMoveSpeedObject | `sub_6F5730(bool)` | 1 bool | ◐ |
| 115 | 0x73 | WaitMoveVectorObject | `sub_6F56D0(bool)` | 1 bool | ◐ |
| 116 | 0x74 | WaitViewObject | `sub_6F5610(bool)` | 1 bool | ◐ |
| 117 | 0x75 | WaitZoomObject | `sub_6F5670(bool)` | 1 bool | ◐ |
| 118 | 0x76 | WaitRotateObject | `sub_6F55B0(bool)` | 1 bool | ◐ |
| 119 | 0x77 | WaitRotateSpeedObject | `sub_6F5550(bool)` | 1 bool | ◐ |
| 120 | 0x78 | WaitTurnObject | `sub_6F54F0(bool)` | 1 bool | ◐ |
| 121 | 0x79 | WaitTurnSpeedObject | `sub_6F5490(bool)` | 1 bool | ◐ |
| 122 | 0x7A | WaitShakeObject | `sub_6F5430(bool)` | 1 bool | ◐ |
| 123 | 0x7B | WaitRasterObject | `sub_6F53D0(bool)` | 1 bool | ◐ |
| 124 | 0x7C | WaitWaveObject | `sub_6F5370(bool)` | 1 bool | ◐ |
| 125 | 0x7D | WaitNoiseObject | `sub_6F5310(bool)` | 1 bool | ◐ |
| 126 | 0x7E | WaitPinbokeObject | `sub_6F52B0(bool)` | 1 bool | ◐ |
| 127 | 0x7F | WaitAfterimageObject | `sub_6F5250(bool)` | 1 bool | ◐ |
| 128 | 0x80 | WaitFadeObject | `sub_6F51F0(bool)` | 1 bool | ◐ |
| 129 | 0x81 | WaitClipObject | `sub_6F5190(*a4, v5, bool)` | 3 参 | ◐ |
| 130 | 0x82 | StopObject | `sub_6F4FD0(*a4 != 0)` | 1 bool | ◐ |
| — | 0x83 | (SetObjectOrigin 131) | `sub_6F4D80(*a4, a4[2])` | 2 参 | ◐ |
| — | 0x84 | (SetObjectZoomCenter 132) | `sub_6F4D00(*a4, a4[1])` | 2 参 | ◐ |
| — | 0x85 | (SetObjectRotateCenter 133) | `sub_6F4C80(*a4, a4[1])` | 2 参 | ◐ |
| — | 0x86 | (SetObjectAnimeNo 134) | `dword_BF3578[113**a4]=a4[1]` + `sub_6F50A0` | 写数组 + 刷新 | ◐ |
| — | 0x87 | (GetObjectAnimeNo 135) | 返回 `dword_BF3578[113**a4]` | getter | ✓ |
| — | 0x88 | (GetObjectX 136) | 返回 `dword_BF3580[113**a4]` | getter | ✓ |
| — | 0x89 | (GetObjectY 137) | 返回 `dword_BF3584[113**a4]` | getter | ✓ |
| — | 0x8A–0x8F | (GetObjectActX..Height 138–143) | `sub_6F8940..6F8730` → 写回 | getter 族 | ◐ |
| — | 0x90 | (IsExistObject 144) | 返回 `dword_BF3548[113**a4] != -1` | 存在判定 | ✓ |
| — | 0x91 | (SearchEmptyObject 145) | `sub_6F4BF0(*a4, a4[1])` → 写回 | 查空槽 | ◐ |
| — | 0x92–0xBC | 对象 group 系列 (146–187) | `sub_6F4AA0` 起的 group 族 | 多参/bool 族 | ◐ |
| 188 | 0xBC | — | 直接 `goto LABEL_485` | 空操作 | NOP |

### 视觉转场 (0xC1–0xF3)

> `Operand.java` 的 `AddIn`…`WaitFade` 滤镜转场族。每条 callee 是 `sub_6F2xxx`，参数普遍以某个 `a4[n] != 0` 收尾作为"是否等待/瞬时"bool。按位置与命名吻合。

| Code | Hex | 名称 | 直接 Callee | 行为 | 校验 |
|---|---|---|---|---|---|
| 193 | 0xC1 | AddIn | `sub_6F2B00(bool)` | 转场 | ◐ |
| 194 | 0xC2 | AddOut | `sub_6F2AB0(bool)` | 转场 | ◐ |
| 195 | 0xC3 | CrossFade | `sub_6F2A40(bool)` | 转场 | ◐ |
| 196–201 | 0xC4–0xC9 | Zoom* 转场 | `sub_6F29E0..6F2830(*a4, a4[1], bool)` | 缩放转场族 | ◐ |
| 202 | 0xCA | ZoomCrossFade | `sub_6F27C0(7 参)` | 转场 | ◐ |
| 203–208 | 0xCB–0xD0 | Rotate* 转场 | `sub_6F2760..6F2580` | 旋转转场族 | ◐ |
| 209 | 0xD1 | RotateCrossFade | `sub_6F2500(13 参)` | 转场 | ◐ |
| 210–216 | 0xD2–0xD8 | Mosaic* 转场 | `sub_6F24B0..6F2E20(bool)` | 马赛克转场族 | ◐ |
| 217–223 | 0xD9–0xDF | Scroll* 转场 | `sub_6F22F0..6F2DD0(bool)` | 滚动转场族 | ◐ |
| 224–226 | 0xE0–0xE2 | ScrollIn/Out/Cross | `sub_6F2110/20C0/2D80(bool)` | 滚动 | ◐ |
| 227–228 | 0xE3–0xE4 | Curtain* | `sub_6F2070/2030(bool)` | 幕布转场 | ◐ |
| 229–233 | 0xE5–0xE9 | Rule* 转场 | `sub_6F1FE0..6F2D30(bool)` | 规则图转场族 | ◐ |
| 234–237 | 0xEA–0xED | Turn* 转场 | `sub_6F1EA0..6F1DB0(bool)` | 翻转转场族 | ◐ |
| 238–242 | 0xEE–0xF2 | Wave* 转场 | `sub_6F1D60..6F2CE0(bool)` | 波纹转场族 | ◐ |
| 243 | 0xF3 | WaitFade | `sub_6F1C50()` | 无参 | ◐ |

### 全屏滤镜 / 镜头 (0xF4–0x122)

> `Fade(244)`…`WaitBGColor(290)`。`sub_701xxx` / `sub_700xxx`，Wait 族无参，效果族带 bool 收尾。

| Code | Hex | 名称 | 直接 Callee | 校验 |
|---|---|---|---|---|
| 244–247 | 0xF4–0xF7 | Fade/Flash/FaderRGB/FlashRGB | `sub_701A10/7016E0/701370/701010` | ◐ |
| 248–254 | 0xF8–0xFE | Shake/RasterX/Y/WaveX/Y/NoiseX/Y | `sub_700F80..700E00` | ◐ |
| 255 | 0xFF | Pinboke | `sub_700DB0(*a4, a4[1], bool)` | ◐ |
| 256–265 | 0x100–0x109 | View/ViewCenter/Zoom/Rotate/Turn/Blur/BlurScroll/BlurZoom/Scroll/Mosaic | `sub_700D60..700AA0` | ◐ |
| 266–267 | 0x10A–0x10B | SpeedLine/RadialLine | `sub_701DC0(10参)/701D30(13参)` | ◐ |
| 268–269 | 0x10C–0x10D | Clip/BGColor | `sub_700A60/700A20` | ◐ |
| 270–289 | 0x10E–0x122 | Wait* 滤镜族 | `sub_700A00..7007A0()` 全无参 | ◐ |

### 立绘 Stand (0x123–0x159)

> `WaitBGColor(290)` 之后的 `SetStand(291)`…`IsExistStand(345)`。Set/Move 族 `sub_6F1xxx`/`sub_6F0xxx`，Wait 族 `sub_6EExxx(bool)`，Get 族写回 `dword_C9D5xx[57*n]`。

| Code | Hex | 名称 | 直接 Callee | 校验 |
|---|---|---|---|---|
| 291 | 0x123 | SetStand→(实为 WaitBGColor 收尾) | `sub_6F1870(v5,*a4,a4[1..3],bool,a4[5])` | ◐ |
| 292 | 0x124 | SetStand | `sub_6F1430(12 参)` | ◐ |
| 293–296 | 0x125–0x128 | SetStandPos/DelStand/ChangeStand/SetStandEx | `sub_6F0E20/0A40/1410/1110` | ◐ |
| 297 | 0x129 | SetStandPosEx | `sub_6F0CA0()` | ◐ |
| 298–308 | 0x12A–0x134 | Move/View/Zoom/Rotate/Turn... Stand | `sub_6F0980..6EFC80` | ◐ |
| 309–332 | 0x135–0x14C | Shake/Raster/Wave/Noise/Pinboke/Afterimage/Fade/Blink + Wait*Stand | `sub_6EFB30..6EE940` | ◐ |
| 333 | 0x14D | WaitAfterimageStand | `sub_6EE8E0(bool)` | ◐ |
| 334 | 0x14E | GetStandNo | 返回 `dword_C9D538[57**a4]` | ✓ |
| 335–338 | 0x14F–0x152 | GetStandX/Y/ActX/ActY | `sub_6F1BA0/1AF0/1A40/1990` → 写回 | ✓ |
| 339–340 | 0x153–0x154 | GetStandPosX/Y | `..._115/..._114` → 写回 | ⚠ |
| 341–342 | 0x155–0x156 | GetStandMoveX/Y | 返回 `dword_C9D53C/C9D540[57*n]` | ✓ |
| 343–344 | 0x157–0x158 | GetStandWidth/Height | `sub_6F1920/18B0` → 写回 | ✓ |
| 345 | 0x159 | IsExistStand | `dword_C9D538[57**a4] >= 0` | ✓ |

### 窗口 / 消息 / 标志 / 脚本 / 语音 / 音频 (0x15A–0x199)

| Code | Hex | 名称 | 直接 Callee | 行为 | 校验 |
|---|---|---|---|---|---|
| 346 | 0x15A | SetWindow | `sub_7A3ED0()` | 无参 | ◐ |
| 347 | 0x15B | DelWindow | `sub_7A3EF0(v5)` | 1 参 | ◐ |
| 348 | 0x15C | SetMessage | `sub_6F35D0(v5, a4[1], v86)` | 3 参 | ◐ |
| 349 | 0x15D | AddMessage | `sub_6F3970(v5, &v83)` | 2 参 | ◐ |
| 350 | 0x15E | DelMessage | `sub_6F3560()` | 无参 | ◐ |
| 351 | 0x15F | WaitMessage | `sub_6F3540()` | 无参 | ◐ |
| 352 | 0x160 | MessagePos | 写 `dword_CA98F0/F4/F8` | 存 x/y/z | ✓ |
| 353 | 0x161 | SetFlg | `dword_CA750C[*a4] = a4[1]` | 写脚本标志数组 | ✓ |
| 354 | 0x162 | GetFlg | 返回 `dword_CA750C[*a4]` | 读脚本标志 | ✓ |
| 355 | 0x163 | SetSystemFlg | 越界检查 + `sub_6EE7D0` | 条件写系统标志 | ◐ |
| 356 | 0x164 | GetSystemFlg | `..._113()` → 写回 | getter | ⚠ |
| 357 | 0x165 | SetCGFlg | 越界检查 + `sub_6EE790` | 条件写 | ◐ |
| 358 | 0x166 | GetCGFlg | `..._112()` → 写回 | getter | ⚠ |
| 359 | 0x167 | SetBGMFlg | 越界检查 + `sub_6EE750` | 条件写 | ◐ |
| 360 | 0x168 | GetBGMFlg | `..._111()` → 写回 | getter | ⚠ |
| 361 | 0x169 | SetReplayFlg | 越界检查 + `sub_6EE710` | 条件写 | ◐ |
| 362 | 0x16A | GetReplayFlg | `..._110()` → 写回 | getter | ⚠ |
| 363 | 0x16B | SetEventFlg | `dword_877C40[*a4]=a4[1]` + 可选 `sub_7288C0()` 存档 | 写事件标志 | ✓ |
| 364 | 0x16C | GetEventFlg | 返回 `dword_877C40[*a4]` | getter | ✓ |
| 365 | 0x16D | LoadScript | `sub_79E3E0(a4[1], v5, &v83, a4[2..9])` | 加载脚本 | ◐ |
| 366 | 0x16E | LoadEvent | `sub_79E2C0(v5, a4[1..8])` | 加载事件 | ◐ |
| 367 | 0x16F | ChangeBank | `sub_79DF80(a4[1..8])` | 切 bank | ◐ |
| 368 | 0x170 | CallScript | `sub_79E1C0(&v83, a4[2..9])` | 调用脚本 | ◐ |
| 369 | 0x171 | CallBank | `sub_79DE80(a4[1..8])` | 调用 bank | ◐ |
| 370 | 0x172 | Return | `sub_79E010(v5)` | 脚本返回 | ◐ |
| 371 | 0x173 | VoicePlay | `sub_6EE430(v85)` | 1 参 | ◐ |
| 372 | 0x174 | VoiceStop | `sub_6EE410()` | 无参 | ◐ |
| 373 | 0x175 | SetVoiceVolume | `dword_CA98EC = *a4` | 存音量 | ✓ |
| 374 | 0x176 | WaitVoice | `sub_6EE590(bool)` | 1 bool | ◐ |
| 375 | 0x177 | SEPlay | `byte_876F32` 守卫 + `sub_6EE350(&v83, a4[2], a4[3])` | 播 SE（CSV 确认） | ✓ |
| 376 | 0x178 | SELoopPlay | `sub_6EE210(v85, a4[2..5])` | 循环 SE | ◐ |
| 377 | 0x179 | SEFadePlay | `sub_6EE0E0(v85, a4[2..7])` | 淡入 SE | ◐ |
| 378–384 | 0x17A–0x180 | SEFadeOut/Fade/Pan/Speed/Stop/WaitSE/IsExistSE | `sub_6EDFE0..` / 数组判定 | SE 控制族 | ◐ |
| 385–406 | 0x181–0x196 | BGM* 族 | `sub_6FE800..` + `dword_CA97xx` 写/读 | BGM 控制族 | ◐ |
| — | 0x197 | (GetBGMNo 收尾附近) | 返回 `dword_CA977C` | getter | ◐ |
| — | 0x198 | — | `sub_453B30(*a4)` → 写回 | getter | ❓ |
| — | 0x199 | — | `sub_6FEC50()` | 无参 | ❓ |

### 尾部特例 (0x2BF–0x2C1)

| Code | Hex | 名称 | 直接 Callee | 校验 |
|---|---|---|---|---|
| 703 | 0x2BF | — | `sub_6FF350(a4[2])` | ❓ |
| 704 | 0x2C0 | — | `sub_6FF2C0()` | ❓ |
| 705 | 0x2C1 | — | `sub_6FF270(*a4)` → 写回 | ❓ getter |

---

## Tier 2 dispatcher (`sub_44D000`)

> 0x199 之后由 Tier 1 default 落入。0x19A 起是 Hell/战斗专用 opcode，含已确认的关卡控制、地图、MapObj、Mek 实例 vtable、条件系统。

### 关卡 / 系统状态 (0x19A–0x1D9)

| Code | Hex | 名称 | 直接 Callee | 行为 | 校验 |
|---|---|---|---|---|---|
| 410–415 | 0x19A–0x19F | — | `sub_452220/4523C0/452130/451FE0/452020/452060` | 多参写入族 | ❓ |
| 416–417 | 0x1A0–0x1A1 | — | `sub_4520A0/4520F0(*a2)` | 1 参 | ❓ |
| 418 | 0x1A2 | — | `return sub_451FC0(*a2)` | getter | ❓ |
| 422 | 0x1A6 | — | 返回 `dword_CA98FC` | getter | ❓ |
| 423 | 0x1A7 | — | 返回 `dword_CA9908[8**a2]` | 数组 getter | ❓ |
| 424 | 0x1A8 | — | 返回 `dword_87975C` | getter | ❓ |
| 425 | 0x1A9 | — | 返回 `dword_87973C` | getter | ❓ |
| 438 | 0x1B6 | — | `sub_445DB0(*a2, a2[1..11])` | 12 参 | ❓ |
| 442–444 | 0x1BA–0x1BC | — | `sub_4C42F0/4C62E0/4C44A0(dword_86CFB8)` | 多参 | ❓ |
| 443 | 0x1BB | — | `sub_4C62E0(*a2, a2[1], a3+56)` | 3 参 + 缓冲 | ❓ |
| 462 | 0x1CE | — | 返回 `dword_86CD24 != 0` | bool getter | ❓ |
| 463 | 0x1CF | — | 返回 `dword_874F04` | getter | ❓ |
| 471 | 0x1D7 | **StartCountdown** | `dword_8750E0 = *a2` | 写倒计时全局（CSV: 设置倒计时） | ✓ |
| 472 | 0x1D8 | — | `dword_8750E0 = -1` | 清倒计时 | ◐ |
| 473 | 0x1D9 | — | 返回 `dword_8750E0` | 读倒计时 | ◐ |
| 466 | 0x1D2 | — | 返回 `dword_8750E4[*a2]` | 数组 getter（脚本里常见判定源） | ◐ |

### 战斗进入 / 难度 (0x1E1–0x1FA)

| Code | Hex | 名称 | 直接 Callee | 行为 | 校验 |
|---|---|---|---|---|---|
| 481 | 0x1E1 | — | `sub_6778C0(*a2, bool)` | 2 参 | ❓ |
| 483 | 0x1E3 | **EnterBattleEsStory** | `sub_672D70(*a2, a2[1])` → `sub_5E5E60` | ES/剧情进入战斗（CSV） | ◐ |
| 486 | 0x1E6 | **ShowOpenCombat** | 符号损坏的方法调用 on `dword_86CDE8` | 显示 OPEN COMBAT（CSV） | ⚠ |
| 490 | 0x1EA | **GetDifficulty** | `return sub_672E00()` → `return dword_872328` | 返回难度 0–4（CSV） | ◐ |
| 498 | 0x1F2 | **HellLoadMekFollow21D21E** | `sub_672EC0(*a2)` → `sub_5F74C0` | 地狱 21D/21E 跟随（CSV） | ◐ |
| 500 | 0x1F4 | **HellLoadMekFollow21D** | `sub_672EE0()` → `sub_5E0560` | 地狱 21D 跟随，**真零参**（CSV） | ◐ |
| 506 | 0x1FA | **LoadMap** | `sub_672F30(a,b,bool)` → `sub_5E26A0`+`sub_651FE0(...)` | 加载地图（CSV） | ✓ |

### MapObj / 弾 (0x203–0x211)

> opcode 经 `sub_673C50(*a2, 1)` 解析出 MapObj 实例，再走 vtable 偏移调用。

| Code | Hex | 名称 | vtable/callee | 校验 |
|---|---|---|---|---|
| 515 | 0x203 | **InitDeployTama** | `sub_673400(6参+bool)` | ◐ |
| 516 | 0x204 | **ActivateMapObjSlot** | `sub_675880(*a2)` | ◐ |
| 519 | 0x207 | **SetMapObjLockPriority** | vtable+92（LABEL_114 块） | ◐ |
| 520 | 0x208 | **SetMapObjWeaponInterference** | `sub_677B60` | ◐ |
| 522 | 0x20A | **GetMapObjHealthPercent** | vtable+96 `return` | ✓ |
| 523 | 0x20B | **MoveMapObj** | vtable+128（4 参） | ◐ |
| 524 | 0x20C | **MapObjVariant256** | vtable+132（4 参） | ◐ |
| 525 | 0x20D | **MapObjVariant257** | vtable+184（a2[1],0,-998） | ◐ |
| 526 | 0x20E | **MapObjVariant281** | vtable+192（LABEL_122 块） | ◐ |
| 527 | 0x20F | **MapObjVariant28B** | vtable+220（LABEL_125 块） | ◐ |
| 528 | 0x210 | **HideMapObj** | vtable+168（2 参） | ◐ |
| 529 | 0x211 | **CreateMapObjAfterimage** | vtable+172（3 参） | ◐ |

### Mek 列表 / 实例化 (0x212–0x22B)

| Code | Hex | 名称 | 直接 Callee | 行为 | 校验 |
|---|---|---|---|---|---|
| 530 | 0x212 | **CreateMekWithoutDeploy** | `sub_675960` | 分配 0x44 字节记录入 Mek list，不登场 | ✓ |
| 531 | 0x213 | **ClearMekBySlot** | `sub_674A20(*a2)` | 清除机体 | ◐ |
| 532 | 0x214 | **DeployCreatedMek** | `sub_674E80(*a2, a2[1..4])` | 让已创建机体登场 | ◐ |
| 534–535 | 0x216–0x217 | **SetMekHealthClampedA / Times10** | `sub_674EF0/674F10(*a2, a2[1])` | 设血量不超上限 | ◐ |
| 536–537 | 0x218–0x219 | **SetMekHealthByLevelA / Times10** | `sub_674F50/674FA0(*a2, a2[1])` | 按等级设血量 | ◐ |
| 538 | 0x21A | **GetMekHealthValue** | `return sub_675030(*a2)` | 读血量值 | ✓ |
| 539 | 0x21B | **GetMekHealthPercent** | `return sub_675000(*a2)` | 读血量百分比 | ✓ |
| 540 | 0x21C | **GetMekHealthMax** | `return sub_675060(*a2)` | 读血量上限 | ✓ |
| 541 | 0x21D | **LoadMek** | `sub_675A30(dword_86CDE8, *a2, a2[1..3])` → CEvent 注册 + `sub_5E3130` | 加载机体（优先于 21E） | ✓ |
| 542 | 0x21E | **EraseMek** | `sub_675AA0(*a2, a2[1..3])` | 删除机体 | ◐ |
| 545 | 0x221 | **SetPlayerWeaponSlot** | `sub_673450(*a2, a2[1], a2[2])` | 设自机武装位 | ◐ |
| 546 | 0x222 | **ClearPlayerWeaponSlot** | `sub_673010(dword_86CDE8)` | 清空武装位 | ◐ |
| 553 | 0x229 | **InitDeployMek** | `sub_675B00(*a2, a2[1..7])` | 机体登场（分配 + 定位） | ✓ |
| 554 | 0x22A | **ClearMekInfo** | `sub_675F50(*a2)` | 清理机体信息 | ◐ |
| 555 | 0x22B | **IsMekAlive** | `return sub_673EF0(*a2)` | 0 死 / 1 存活 | ✓ |

### Mek 实例 vtable (`sub_673F90`) (0x22C–0x293)

> opcode 经 `sub_673F90(*a2, 1)` 解析出 Mek 实例，再按 vtable 偏移调用。偏移连续递增，确认是同一个 Mek 类的方法表。已命名的项与 CSV 描述对齐；未命名项给出 vtable 偏移。

| Code | Hex | 名称 | vtable 偏移 | 参数 | 校验 |
|---|---|---|---|---|---|
| 556 | 0x22C | **SetMekPosition** | +12 | 3 (x,y,z) | ◐ |
| 557 | 0x22D | **SetMekAngle** | +16 | 2 | ◐ |
| 558 | 0x22E | **SetMekTextureModeA** | +20 | 3 | ⚠ 用户自标"可能是贴图" |
| 559 | 0x22F | **SetMekTextureModeB** | +24 | 2 | ⚠ |
| 560 | 0x230 | **SetMekTextureModeC** | +28 | 1 | ⚠ |
| — | 0x231 | — | +32 | 5 | ❓ |
| 562 | 0x232 | **ScaleMek** | +36 | 1 | ◐ |
| 563 | 0x233 | **SetMekTextureModeD** | +40 | 2 | ⚠ |
| 564 | 0x234 | **SetMekHealthVariantA** | +44 | 1 | ◐ |
| 565 | 0x235 | **SetMekHealthVariantATimes10** | +48 | 1 | ◐ |
| 566 | 0x236 | **SetMekHealth** | +52 | 1 | ◐ |
| 567 | 0x237 | **SetMekHealthHellDefault** | +56 | 1 | ◐ |
| 568 | 0x238 | **AddMekHealth** | +60 | 1 | ◐ |
| 569 | 0x239 | **AddMekHealthTimes10** | +64 | 1 | ◐ |
| 570 | 0x23A | **AddMekHealthMax** | +68 | 1 | ◐ |
| 571 | 0x23B | **AddMekHealthMaxTimes10** | +72 | 1 | ◐ |
| 572 | 0x23C | **SetMekEnergy** | LABEL 共用块 `sub_677F40` | 2 | ◐ |
| 573 | 0x23D | **SetMekAi** | `sub_677F70` | 2 | ◐ |
| 574 | 0x23E | **SetMekLevel** | `sub_677F80` | 2 | ◐ |
| 575 | 0x23F | **SetMekAutoHoverHeight** | `sub_677F90` | 2 | ◐ |
| 576 | 0x240 | **SetMekTransparencyMode** | +76 | 1 (bool) | ◐ |
| 577 | 0x241 | **SetMekTransparency** | +80 | 2 | ◐ |
| 578 | 0x242 | **SetMekWeaponAffectMode** | +84 | 1 | ◐ |
| — | 0x243 | — | `sub_677FF0` | 2 | ❓ |
| 580 | 0x244 | **SetMekExplodeAfterTime** | `sub_678130` | 2 | ◐ |
| 581 | 0x245 | **SetMekExplodeOnDeathMode** | `sub_678140` | 2 | ◐ |
| — | 0x246–0x247 | — | `sub_678160/678180` | 2 | ❓ |
| 584 | 0x248 | **SetMekLockPriority** | +92（LABEL_114） | 2 | ◐ |
| — | 0x249 | — | `sub_677FA0` | 2 | ❓ |
| 586 | 0x24A | **SetMekBuff** | LABEL 共用块 `sub_677FB0` | 3 (slot,type,value) | ✓ |
| 587 | 0x24B | **AddMekBuff** | `sub_677FD0` | 3 | ✓ |
| 588 | 0x24C | **GetMekHealthPercentAlt** | +96 `return` | 0 | ✓ |
| — | 0x24D | — | +100 `return` | 0 | ❓ getter |
| 590 | 0x24E | **GetMekInitialX** | +104 `return` | 0 | ✓ |
| 591 | 0x24F | **GetMekInitialY** | +108 `return` | 0 | ✓ |
| 592 | 0x250 | **GetMekInitialZ** | +112 `return` | 0 | ✓ |
| 593 | 0x251 | — | `sub_6740D0(dword_86CDE8)` | 0（全局 self） | ❓ |
| 594 | 0x252 | — | `sub_674220(*a2)` | 1 | ❓ |
| 595–600 | 0x253–0x258 | — | vtable+116..136 | 2–5 参 | ❓ |
| 601 | 0x259 | **SetMekLandingMotion** | +140 | 2 | ✓（CSV: 和降落动画有关） |
| 602–607 | 0x25A–0x25F | — | vtable+144..164 | 1–4 参 | ❓ |
| 608 | 0x260 | **HideMekUntilAction** | +168 | 2 | ✓ |
| 609 | 0x261 | **CreateMekAfterimage** | +172 | 3 | ✓ |
| 610–611 | 0x262–0x263 | — | vtable+176/180 | 1 | ❓ |
| 612–613 | 0x264–0x265 | — | `sub_6730F0/673170(*a2, a2[1])` | 2 | ❓ |
| 614 | 0x266 | — | `sub_677550(*a2, a2[1..9])` | 10 | ❓ |
| 615 | 0x267 | — | `sub_6731F0(dword_86CDE8)` | 0 | ❓ |
| 616 | 0x268 | — | `sub_675C10(*a2, a2[1..5])` | 6 | ❓ |
| 617 | 0x269 | — | `sub_675090(*a2)` | 1 | ❓ |
| 618–638 | 0x26A–0x27E | — | `sub_674030(*a2,1)` + `sub_672960..672AC0` | 次级 vtable 簇（疑似第二 Mek 状态对象/动画控制器） | ❓ |
| 639–641 | 0x27F–0x281 | — | vtable+184/188/192 | 1–3 | ❓ |
| 642–643 | 0x282–0x283 | — | `sub_6781A0` / `sub_6781C0(6参)` | — | ❓ |
| 644 | 0x284 | — | vtable+196 | 3 | ❓（脚本里高频，疑似演出相关） |
| 645 | 0x285 | **SetMekAfterimageEffectRecovery** | +200 | 1 | ✓ |
| 646 | 0x286 | **SetMekAfterimageEffectRetreat** | +204 | 1 | ✓ |
| 647–659 | 0x287–0x293 | — | vtable+208..252 | 0–2 | ❓ |

### 演出 / 条件系统 (0x294–0x2A9)

| Code | Hex | 名称 | 直接 Callee | 行为 | 校验 |
|---|---|---|---|---|---|
| 660 | 0x294 | — | `sub_676720(dword_86CDE8)` | 0（全局） | ❓（脚本里成对出现，疑似 commit/flush 演出队列） |
| 661–667 | 0x295–0x29B | — | `sub_6767E0..676C60` | 1–2 参 | ❓ |
| 668 | 0x29C | **MidDeployMek** | `sub_673200(*a2, a2[1..9])` → `sub_5EF790` | 中途登场（10 参） | ✓ |
| 669 | 0x29D | **MidDeployPreparedMek** | `sub_673210(*a2, a2[1..5])` | 中途登场，地狱与 0x214 配合 | ◐ |
| 670 | 0x29E | — | `sub_673220(dword_86CDE8)` | 0 | ❓（脚本里紧跟 0x29C） |
| 671 | 0x29F | **SetCondition** | `sub_673230(*a2, a2[1..5])` → `sub_5E8B70` | 注册胜利/失败条件（6 参） | ✓ |
| 672 | 0x2A0 | — | `sub_673240(dword_86CDE8)` | 0 | ❓ |
| 673 | 0x2A1 | **GetConditionRelated** | `sub_673250(*a2)` | 条件相关 | ◐ |
| 674 | 0x2A2 | — | `sub_673260(*a2)` | 1 | ❓ |
| 675 | 0x2A3 | — | `sub_673270(*a2, a2[1..8])` | 9 | ❓ |
| 676 | 0x2A4 | — | `return sub_673280(dword_86CDE8)` | getter（脚本里和 99999/99998 比较） | ◐ |
| 677 | 0x2A5 | **IsConditionSatisfied** | `return sub_673290(*a2)` → `sub_5E8440` | 返回条件是否满足，1 满足 | ✓ |
| 678–681 | 0x2A6–0x2A9 | — | `sub_6732B0/673340/673360/673370` | 0–1 参 | ❓ |

### UI / 系统尾段 (0x2AA–0x2BE)

| Code | Hex | 名称 | 直接 Callee | 校验 |
|---|---|---|---|---|
| 682 | 0x2AA | — | `sub_446BA0(*a2, v142..v148)` 字符串拼接 | ❓ |
| 683 | 0x2AB | — | `sub_44CFC0(*a2)` | ❓ |
| 684 | 0x2AC | — | `return sub_445FE0(*a2, a2[1..2])` | ❓ getter |
| 685 | 0x2AD | — | `sub_44CB00(*a2)` | ❓ |
| 686–687 | 0x2AE–0x2AF | — | `sub_55A700/5567F0(dword_BE8610)` | ❓ |
| 688–702 | 0x2B0–0x2BE | — | `sub_44A870/44CCA0/...445C90` 无参/1 参族 | ❓ UI 状态族 |

---

## 结论摘要

### 强确认（✓）的关键 opcode

- **流程**：Exit(0xB)、Random(0xD)、GetCharCode(0xE)、HideMenu(0x15)、GetLocalTime(0x30)、sprintf(0x34)
- **标志**：SetFlg/GetFlg(0x161/0x162)、SetEventFlg/GetEventFlg(0x16B/0x16C)、SetVoiceVolume(0x175)、MessagePos(0x160)
- **Get 族**：GetObject*(0x87–0x90)、GetStand*(0x14E/155/156/159)
- **音频**：SEPlay(0x177)
- **关卡**：StartCountdown(0x1D7)、LoadMap(0x1FA)
- **机体**：CreateMekWithoutDeploy(0x212)、Get血量族(0x21A–0x21C)、LoadMek(0x21D)、InitDeployMek(0x229)、IsMekAlive(0x22B)、Get初始坐标(0x24E–0x250)、SetMekBuff/AddMekBuff(0x24A/0x24B)、SetMekAfterimage*(0x285/0x286)
- **MapObj**：GetMapObjHealthPercent(0x20A)
- **条件**：MidDeployMek(0x29C)、SetCondition(0x29F)、IsConditionSatisfied(0x2A5)

### 仍需下钻才能 100% 钉死

1. `sub_673F90` 返回的 Mek 实例 **vtable 起点** —— 一次能确认 0x22C–0x293 全部 30+ 个 SetMek*/GetMek* 的真名（最高价值）
2. `sub_674030` 簇（0x26A–0x27E）——次级 Mek 状态对象，21 个 opcode 全未命名
3. `sub_673C50` MapObj vtable —— 确认 0x203–0x211 的 MapObj 方法名
4. 底层 `sub_5Exxx` 系列（5E5E60 / 5E0560 / 5E3130 / 5E8B70 / 5EF790 / 5E26A0+651FE0）—— 确认战斗进入、Hell 跟随、条件系统的真实语义
5. 演出 commit 簇（0x294 / 0x29E / 0x2A0）——脚本里成对出现但作用未定

### 未命名但脚本高频出现（建议优先补名）

`0x284`（演出）、`0x294`/`0x29E`/`0x2A0`（演出 commit）、`0x1D2`（判定源数组）、`0x2A4`（与 99999 比较的 getter）、`0x1E1`、`0x1B4`/`0x1B5`/`0x1B6`、`0x267`、`0x251`
