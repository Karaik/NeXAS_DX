# 05. BSDX 机体武装超过 153 的最终根因

## 结论

最终根因不是 `.mek` 文件格式本身，也不是单纯的武装列表 UI 可见槽位数量。

真正的硬限制在于：

1. 游戏内部存在一组按 `weaponId` 直接索引的武装参数表。
2. 这组参数表的元素大小是 `208` 字节。
3. 这组参数表在初始化时被固定分配为 `153` 个元素。
4. 武装页在渲染第 N 条武装时，会先从当前武装列表里取出 `weaponId`，再用这个 `weaponId` 去访问这张 `208-byte` 参数表。
5. 当某个可见武装条目的 `weaponId >= 153` 时，这个访问就越过了这张表的合法范围。

所以：

- 表面现象是“滚到第 154 个武装时闪退”
- 真实根因是“第 154 个位置对应到的 `weaponId` 已经超过了内部 `208-byte` 武装参数表的上限”

更精确地说：

- **限制的是内部 `weaponId` 可用范围，不是 `.mek` 里的 `weaponCount` 字段本身**
- 在默认顺序下，第 154 个显示槽位通常就会落到 `weaponId == 153`
- 因此现象稳定地表现为“滚到第 154 个武装就挂”

## 证据链

### 1. `.mek` 文件本身没有 153 上限

参考：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/656C70.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/65F6E0.c`
- `src/main/java/com/giga/nexas/dto/bsdx/mek/parser/MekParser.java`

可以确认：

- `read_mek_skills` 先读一个 count，然后循环 `malloc(0xB0)` 读取每个武装条目
- Java 侧 `MekParser` 也是先读 `weaponCount` 再按 count 循环

因此：

- `.mek` 协议没有把武装数量硬卡在 `153`
- 资源加载阶段能够读入 `>153` 条武装，是正常现象

### 2. 武装页不是直接用显示序号，而是先取当前列表里的 `weaponId`

参考：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/513EB0.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/50BC90.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/50AF60.c`

关键逻辑：

在 `CWeaponDataItem::sub_513EB0` 里：

- `index = *(this + 156) + dword_877478 - 236`
- `*(this + 912) = index`
- `*(this + 916) = *( *(dword_BE8514 + 320) + 4 * index )`

这里：

- `this + 912` 存的是当前显示序号的零基索引
- `this + 916` 存的是当前条目真正的 `weaponId`

而 `sub_50BC90` 会把 `dword_BE8804 .. dword_BE8808` 这份 16-byte 武装列表的第一个字段拷进 `CWeaponData` 的当前列表。

因此武装页访问顺序是：

1. 先按当前滚动位置算出“第几个显示槽位”
2. 再从当前武装列表里取出该槽位对应的 `weaponId`
3. 后续所有武装参数、排序、详情都按这个 `weaponId` 去查表

这说明真正危险的是 `weaponId`，不是屏幕上看到的序号本身。

### 3. 武装页详情和排序都在访问同一张 208-byte 参数表

参考：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/513EB0.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/510100.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/50DE50.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/457C60.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/50B260.c`

关键点：

`au_re___invalid_parameter_noinfo_49` 的实现非常直接：

- 如果 `a2 >= (this[4] - this[3]) / 208` 就 `_invalid_parameter_noinfo()`
- 否则返回 `this[3] + 208 * a2`

也就是说，它就是一个：

- “按索引访问 208-byte 元素数组”的 accessor

而在武装页里：

- `sub_513EB0` 用 `weaponId` 去访问这张 208-byte 表
- `sub_510100` 也用 `weaponId` 去访问这张 208-byte 表
- `sub_50DE50` 仍然用 `weaponId` 去访问这张 208-byte 表
- `sub_50B260` 在排序时也会访问同一张 208-byte 表

所以这不是某一个孤立渲染点的问题，而是整个武装页共同依赖的一张核心参数表。

### 4. 这张 208-byte 表的容量就是 153

参考：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/4576F0.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/45B330.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/454970.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/452F30.c`

关键事实：

`sub_45B330` 管理的是：

- 元素大小 `208` 字节的动态数组

`sub_4576F0` 初始化时，明确调用：

- `sub_45B330(153, ...)`

而 `452F30 / 454970` 对同一批字段做存档读写时，也明确按：

- `208 * ((end - begin) / 208)`

来序列化和反序列化这批表

这说明：

- 武装页依赖的 `208-byte` 参数表，初始化容量就是 `153`
- 不是猜测，不是间接影射，而是这组运行时数据结构的真实容量

### 5. 为什么现象刚好是“第 154 个武装闪退”

因为武装页显示给玩家的是 1-based 编号：

- 第 1 个武装 -> 内部索引 0
- 第 154 个武装 -> 内部索引 153

在默认顺序下：

- 第 154 个显示槽位通常就对应 `weaponId == 153`

一旦进入 `sub_513EB0 / sub_510100 / sub_50DE50` 这样的路径：

- 程序就会尝试访问 `208-byte table[153]`

但这张表只有 `153` 个元素：

- 合法范围是 `0 .. 152`

因此第一个越界点正好就是：

- **显示序号 154**
- **内部零基索引 153**
- **`weaponId == 153`**

## 为什么之前只改 exe 里的那几处 `153 -> 200` 不够

之前追到的：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/4576F0.c`

里确实有 `153`

但只改那几处 `push 0x99 -> push 0xC8` 还不够，原因是：

1. 武装页不是只依赖一处容量数字
2. 它依赖整组 `208-byte` 参数表，以及和它并行的其他表
3. 这些表会被读档、写档、排序、详情渲染、选中态同步共同访问

也就是说：

- 你不能把问题理解成“找到一个 153，改成 200 就完了”
- 真正要扩容，必须把这整组 `208-byte` 武装参数表及其并行结构完整扩掉

否则就会出现：

- 启动阶段坏掉
- 详情页坏掉
- 排序坏掉
- 读档/写档坏掉

## 最终定性

可以把这次问题定性为：

**BSDX 武装页依赖一组按 `weaponId` 索引的固定容量武装参数表，这组表的容量是 153。**

所以：

- 追加 `.mek` 武装条目到 154 个以上，本身不一定立刻崩
- 但只要武装页滚动或选中逻辑第一次访问到 `weaponId >= 153` 的条目
- 就会命中这组 `208-byte` 参数表的越界边界
- 从而触发崩溃

## 对后续 patch 的意义

如果要真正支持 `>153` 武装，不是只改资产，也不是只改一两个 immediates。

至少要一起处理：

1. `sub_4576F0` 初始化出来的 `208-byte` 武装参数表容量
2. 相关并行表的容量
3. `452F30 / 454970` 的读档写档兼容性
4. 武装页 `CWeaponData / CWeaponDataItem / detail / sort` 这一整条按 `weaponId` 查表的访问链

在这套东西没一起改完之前：

- **资产层安全上限仍然应视为 `weaponId <= 152`**
- 换成显示序号就是：
  - **最多只安全到第 153 个武装**

