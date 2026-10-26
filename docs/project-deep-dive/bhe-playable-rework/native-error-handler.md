# 保留原生错误处理入口

本轮不以吞掉异常作为“不崩溃”的验收方式。JINKI V2、Tsukuyomi与Followup三套现行ExePatchProfile仅移除文件偏移0x2789F8处8BFF→C390的全局CRT早退；容量、菜单及两处战斗语音兼容字节保持原样。旧JINKI v1未参与当前入口，不扩展其逻辑。

NativeErrorHandlerPreservationTest读取真实干净BaldrSky.exe，经三套真实Apply写出到JUnit临时目录，依次核104、105、106容量，并在每层比较原生入口后的32字节；末尾全字节核对原始EXE未改写。

RED：原profile在第一层将入口8B改为C3，1test/1failure/0errors，记录target/bhe-audit-20261026/native-handler-red.log。

GREEN：移除旁路后该测试1/0/0/0，单独Maven成功且静默日志为空；可复核Surefire文本。后与Freja6/Yuri5/源隔离2/旧重绑2/公共跳转4组合，共20项全部通过，记录target/bhe-audit-20261026/rework-core-green.log。

独立审查确认三个profile diff只移除该旁路、测试覆盖真实输出、原始EXE SHA仍为2F8CFC695FFC30DF2F02DEB8D4902A01ABAD6E71475E609D1CDA703C991C3492。新增测试换行已归一为CRLF。

删除profile条目不会主动修复已经带C390的旧输入。因此新九角色构建固定从干净游戏基底开始，隔离运行工具再次拒绝非8BFF入口。旧EXE和旧PAC保持原状，没有原地恢复或删除。本项完成不代表游戏稳定性已经通过。
