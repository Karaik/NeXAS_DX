package com.giga.nexas.transfer.bhe2bsdx.meka.katou.convert.mek;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove;

import java.util.ArrayList;
import java.util.List;


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
            target.transBheCCpuEventMoveToBsdx(move, target);
            return target;
        }

        if (source instanceof com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEventAttack attack) {
            CCpuEventAttack target = new CCpuEventAttack();
            target.transBheCCpuEventAttackToBsdx(attack, target);
            return target;
        }

        throw new IllegalArgumentException("未知 BHE MEK AI 事件类型：" + source.getClass().getName());
    }
}
