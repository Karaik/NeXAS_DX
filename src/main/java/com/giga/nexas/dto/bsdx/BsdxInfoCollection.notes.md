# BsdxInfoCollection 语义笔记（真实样本）

## 证据来源

- 结构读取：`src/main/java/com/giga/nexas/dto/bsdx/BsdxInfoCollection.java:60`
- 结构写入：`src/main/java/com/giga/nexas/dto/bsdx/BsdxInfoCollection.java:87`
- 关联事件：`src/main/java/com/giga/nexas/dto/bsdx/waz/wazfactory/wazinfoclass/obj/CEventChange.java:50`
- 术语树：`src/main/java/com/giga/nexas/dto/bsdx/grp/parser/impl/TermGrpParser.java:21`
- 真实样本：
  - `src/main/resources/game/bsdx/waz/makoto.waz`
  - `src/main/resources/game/bsdx/grp/term.grp`
  - `src/main/resources/wazBsdxJson/Makoto.waz.json`

## 1. 二进制结构

```mermaid
flowchart LR
  A[int1] --> B[count(typeList)+typeList]
  B --> C[count(paramList)+paramList]
  C --> D[count(intList3)+intList3]
  D --> E[count(intList4)+intList4]
  E --> F[int2]
```

字段顺序是固定的，不能调换。

## 2. 与 Term.grp 的链式关联

`int1 + typeList[*]` 会在 `Term.grp` 内形成跳转链：

1. `int1` 选择起始 group。
2. `typeList[i]` 选择当前 group 的 item。
3. item 的 `param2` 决定下一跳 group。
4. `param2 < 0` 终止。

```mermaid
flowchart TD
  A[int1 -> group] --> B[typeList[i] -> item]
  B --> C[param2]
  C --> D{param2 >= 0}
  D -->|Yes| B
  D -->|No| E[终止]
```

## 3. 真实案例（Makoto）

来源：

- `src/main/resources/wazBsdxJson/Makoto.waz.json`
- 样例 path：
  - `$.skillList[0].phasesInfo[0].skillUnitCollection[3].skillInfoObjectList[0].bsdxInfoCollectionList1[0]`

样例值：

- `int1=0`
- `typeList=[2,7,17]`
- `int2=1`

真实 `term.grp` 解码：

- `group[0]/PARENT item[2] -> OBJECT1 (param2=7)`
- `group[7]/OBJECT1 item[7] -> TOUCH (param2=15)`
- `group[15]/TOUCH item[17] -> GROUND (param2=-3)`

语义拼接：

- `OBJECT1.TOUCH.GROUND`

## 4. int2 统计现状（已验证）

统计范围：

- `src/main/resources/wazBsdxJson/*.json`（112 文件）

统计结果：

- `collection 总数 = 59449`
- `int2=0 -> 59149`
- `int2=1 -> 300`

定位特征：

- `int2=1` 仅出现在：
  - `listName = bsdxInfoCollectionList1`
  - `ownerTypeId = 34`
  - `ownerSlot = 71`

当前结论分级：

- `verified`：`int2=1` 出现范围与宿主类型。
- `inferred`：`int2` 可能是条件链关系位（仍需行为级验证）。

## 5. 二进制对照提醒

- 不要只看转出的 int 值。
- 当语义解释不通时，回到原始 bytes 看：
  - `i32`
  - `f32`
  - `i16x2`
  - `bitmask`

对照命令（在 `D:\Code\NeXAS_DX_Tauri` 执行）：

```powershell
cargo run -p nexas-cli -- inspect-collection D:\Code\NeXAS_DX\src\main\resources\game\bsdx\waz\makoto.waz --offset 743
cargo run -p nexas-cli -- inspect-ceventchange D:\Code\NeXAS_DX\src\main\resources\game\bsdx\waz\makoto.waz --offset 731 --term-grp D:\Code\NeXAS_DX\src\main\resources\game\bsdx\grp\term.grp
```
