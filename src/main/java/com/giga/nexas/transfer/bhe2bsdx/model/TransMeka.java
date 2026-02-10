package com.giga.nexas.transfer.bhe2bsdx.model;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.transfer.bhe2bsdx.converter.TransMekaPipeline;
import lombok.extern.slf4j.Slf4j;

/**
 * BHE -> BSDX 机体迁移入口（facade）。
 * <p>
 * 保持"旧参数"签名，内部转交给 Pipeline，避免外部调用方大面积改动。
 * 返回 TransMekaResult，便于写盘与后续调试。
 */
@Slf4j
public class TransMeka {

    public static TransMekaResult process(
            com.giga.nexas.dto.bhe.mek.Mek bheMek,
            com.giga.nexas.dto.bhe.waz.Waz bheWaz,
            com.giga.nexas.dto.bhe.spm.Spm bheSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheCSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheSSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheGSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheMSpm,

            com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup bheBatVoiceGroup,
            BatVoiceGrp bsdxBatVoice,

            com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup bheMekaGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry bheWazaGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry bheSpriteGroupEntry,
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheSpriteGroup,
            MekaGroupGrp bsdxMekaGroup,
            WazaGroupGrp bsdxWazaGroup,
            SpriteGroupGrp bsdxSpriteGroup,
            java.util.Map<String, com.giga.nexas.dto.bsdx.waz.Waz> bsdxWazRegistry,

            com.giga.nexas.dto.bsdx.spm.Spm mekaPilotSpm,
            com.giga.nexas.dto.bsdx.spm.Spm selectMekaMenuMekaSpm,
            com.giga.nexas.dto.bsdx.dat.Dat selectMekaMenuDat,
            com.giga.nexas.dto.bsdx.mek.Mek targetBsdxMek,
            String targetCodeName,
            boolean keepTargetKey
    ) {
        // Step0: 统一组装输入
        TransMekaRequest request = TransMekaRequest.fromLegacy(
                bheMek, bheWaz, bheSpm, bheCSpm, bheSSpm, bheGSpm, bheMSpm,
                bheBatVoiceGroup, bsdxBatVoice,
                bheMekaGroup, bheWazaGroup, bheSpriteGroupEntry, bheSpriteGroup,
                bsdxMekaGroup, bsdxWazaGroup, bsdxSpriteGroup, bsdxWazRegistry,
                mekaPilotSpm, selectMekaMenuMekaSpm, selectMekaMenuDat,
                targetBsdxMek, targetCodeName, keepTargetKey
        );

        // Step1~Step6: 执行迁移流水线
        TransMekaResult result = new TransMekaPipeline().execute(request);

        // 输出关键索引，便于检查 grp 对齐是否正确
        if (result != null) {
            log.info("grp index => meka:{}, waza:{}, sprite:{}, batvoice:{}",
                    result.getMekaGroupIndex(),
                    result.getWazaGroupIndex(),
                    result.getSpriteGroupIndex(),
                    result.getBatVoiceIndex());
        }

        return result;
    }
}
