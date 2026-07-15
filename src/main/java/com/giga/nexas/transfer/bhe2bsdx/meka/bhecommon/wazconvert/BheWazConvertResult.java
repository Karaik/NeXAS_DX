package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert;

import com.giga.nexas.dto.bsdx.waz.Waz;

/**
 * BHE WAZ 格式转换结果：目标侧 WAZ 对象 + 本文件丢弃的 BHE-only 槽位审计。
 */
public record BheWazConvertResult(
        Waz waz,
        BheWazSlotDropAudit dropAudit
) {
}
