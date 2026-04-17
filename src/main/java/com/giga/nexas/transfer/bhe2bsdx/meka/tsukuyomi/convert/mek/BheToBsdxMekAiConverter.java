package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE MEK AI 分片转换器。
 *
 * <p>这里以 src/test 中旧 trans 逻辑和同名 MEK 真实数据为准：
 * BHE 的 CCpuEvent 尾部条件字段比 BSDX 多出若干项，不能按同名字段直接复制，
 * 必须走 BSDX DTO 上已经沉淀的 transBhe 方法完成错位映射。</p>
 */
class BheToBsdxMekAiConverter {

    List<Mek.MekAiInfo> convert(List<com.giga.nexas.dto.bhe.mek.Mek.MekAiInfo> sourceList) {
        List<Mek.MekAiInfo> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }

        for (com.giga.nexas.dto.bhe.mek.Mek.MekAiInfo source : sourceList) {
            Mek.MekAiInfo target = new Mek.MekAiInfo();
            if (source != null) {
                target.setAiTypeJapanese(source.getAiTypeJapanese());
                target.setAiTypeEnglish(source.getAiTypeEnglish());
                target.setCpuEventList(convertCpuEvents(source.getCpuEventList()));
            }
            targetList.add(target);
        }
        return targetList;
    }

    private List<CCpuEvent> convertCpuEvents(
            List<com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEvent> sourceList
    ) {
        List<CCpuEvent> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }

        for (com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEvent source : sourceList) {
            targetList.add(convertCpuEvent(source));
        }
        return targetList;
    }

    private CCpuEvent convertCpuEvent(com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEvent source) {
        if (source == null) {
            return null;
        }

        if (source instanceof com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEventMove move) {
            CCpuEventMove target = new CCpuEventMove();
            // AI 事件尾部字段存在 BHE/BSDX 错位，必须使用已验证的 transBhe 规则，不能同名复制。
            target.transBheCCpuEventMoveToBsdx(move, target);
            return target;
        }

        if (source instanceof com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEventAttack attack) {
            CCpuEventAttack target = new CCpuEventAttack();
            // 攻击事件同样走 transBhe，保持 mekWeaponInfoMapNo 之外的条件字段语义对齐。
            target.transBheCCpuEventAttackToBsdx(attack, target);
            return target;
        }

        throw new IllegalArgumentException("未知 BHE MEK AI 事件类型：" + source.getClass().getName());
    }
}
