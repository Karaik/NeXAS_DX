package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * mek AI 转换：
 * 迁移 CCpuEvent，并补齐 BSDX 必需字段（如 int26）。
 */
@Slf4j
public class MekAiConverter {

    public List<Mek.MekAiInfo> convert(List<com.giga.nexas.dto.bhe.mek.Mek.MekAiInfo> bheAiList) {
        List<Mek.MekAiInfo> out = new ArrayList<>();
        if (bheAiList == null) {
            return out;
        }
        for (com.giga.nexas.dto.bhe.mek.Mek.MekAiInfo src : bheAiList) {
            Mek.MekAiInfo dst = new Mek.MekAiInfo();
            dst.setAiTypeJapanese(src.getAiTypeJapanese());
            dst.setAiTypeEnglish(src.getAiTypeEnglish());

            if (src.getCpuEventList() != null) {
                for (com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEvent event : src.getCpuEventList()) {
                    com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent mapped = convertEvent(event);
                    if (mapped != null) {
                        dst.getCpuEventList().add(mapped);
                    }
                }
            }
            out.add(dst);
        }
        return out;
    }

    private com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent convertEvent(
            com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEvent src
    ) {
        if (src == null) {
            return null;
        }
        if (src instanceof com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEventMove move) {
            com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove dst =
                    new com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove();
            copyCpuEventBase(move, dst);
            dst.setMoveType(move.getMoveType());
            dst.setMoveSpeed(move.getMoveSpeed());
            dst.setMoveInertia(move.getMoveInertia());
            dst.setMoveTargetType(move.getMoveTargetType());
            dst.setMoveTargetAngleCorrection(move.getMoveTargetAngleCorrection());
            dst.setViewpointType(move.getViewpointType());
            dst.setViewpointAngleCorrection(move.getViewpointAngleCorrection());
            dst.setJumpType(move.getJumpType());
            dst.setAscentVar1(move.getAscentVar1());
            dst.setAscentVar2(move.getAscentVar2());
            dst.setGenericFlag(move.getGenericFlag());
            dst.setAttackProbabilityCorrection(move.getAttackProbabilityCorrection());
            return dst;
        }
        if (src instanceof com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEventAttack attack) {
            com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack dst =
                    new com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack();
            copyCpuEventBase(attack, dst);
            dst.setMekWeaponInfoMapNo(attack.getMekWeaponInfoMapNo());
            dst.setWazaName(attack.getWazaName());
            return dst;
        }
        log.warn("未知 BHE CCpuEvent 类型: {}", src.getClass().getSimpleName());
        return null;
    }

    private void copyCpuEventBase(
            com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEvent src,
            com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent dst
    ) {
        dst.setType(src.getType());
        dst.setShort1(src.getShort1());
        dst.setInt1(src.getInt1());
        dst.setInt2(src.getInt2());
        dst.setActivationProbability(src.getActivationProbability());
        dst.setActivationProbabilityWhenCounter(src.getActivationProbabilityWhenCounter());
        dst.setInt5(src.getInt5());
        dst.setActivationRangeMin(src.getActivationRangeMin());
        dst.setActivationRangeMax(src.getActivationRangeMax());
        dst.setActivationAngleRangeMin(src.getActivationAngleRangeMin());
        dst.setActivationAngleRangeMax(src.getActivationAngleRangeMax());
        dst.setActivationAltitudeMin(src.getActivationAltitudeMin());
        dst.setActivationAltitudeMax(src.getActivationAltitudeMax());
        dst.setActivationDurabilityMinPercentage(src.getActivationDurabilityMinPercentage());
        dst.setActivationDurabilityMaxPercentage(src.getActivationDurabilityMaxPercentage());
        dst.setInt14(src.getInt14());
        dst.setInt15(src.getInt15());
        dst.setActivationHeatMin(src.getActivationHeatMin());
        dst.setActivationHeatMax(src.getActivationHeatMax());
        dst.setInt18(src.getInt18());
        dst.setInt19(src.getInt19());
        dst.setInt20(src.getInt20());
        dst.setInt21(src.getInt21());
        dst.setInt22(src.getInt22());
        dst.setInt23(src.getInt23());
        dst.setInt24(src.getInt24());
        // todo
        // 20260331 0
        dst.setInt25(src.getInt27());
        dst.setInt26(src.getInt28());
        dst.setShort2(src.getShort3());
        dst.setInt27(src.getInt29());
        dst.setInt28(src.getInt30());
        dst.setShort3(src.getShort4());
        dst.setShort4(src.getShort5());

        // BsdxInfoCollection 与 BheInfoCollection 结构一致，逐项深拷贝
        List<BsdxInfoCollection> collections = new ArrayList<>();
        if (src.getBheInfoCollectionList() != null) {
            for (BheInfoCollection col : src.getBheInfoCollectionList()) {
                collections.add(convertInfoCollection(col));
            }
        }
        dst.setBsdxInfoCollectionList(collections);
    }

    private BsdxInfoCollection convertInfoCollection(BheInfoCollection src) {
        BsdxInfoCollection dst = new BsdxInfoCollection();
        if (src == null) {
            return dst;
        }
        dst.setInt1(src.getInt1());
        dst.setInt2(src.getInt2());
        dst.setTypeList(src.getTypeList() != null ? new ArrayList<>(src.getTypeList()) : new ArrayList<>());
        dst.setParamList(src.getParamList() != null ? new ArrayList<>(src.getParamList()) : new ArrayList<>());
        dst.setIntList3(src.getIntList3() != null ? new ArrayList<>(src.getIntList3()) : new ArrayList<>());
        dst.setIntList4(src.getIntList4() != null ? new ArrayList<>(src.getIntList4()) : new ArrayList<>());
        return dst;
    }
}
