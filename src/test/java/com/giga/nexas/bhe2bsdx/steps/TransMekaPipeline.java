package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.Map;

/**
 * 迁移流程编排器：按步骤执行并产出结果。
 *
 * 流程（文字流程图）：
 * 输入(BHE: mek/waz/spm/grp/batvoice)
 * -> Step1: BatVoice 深拷贝并追加到 BSDX batvoice.grp
 * -> Step2: grp 对齐(upsert meka/waza/sprite)并生成索引
 * -> Step3: spritegroup 索引映射(BHE index -> BSDX index)
 * -> Step4: 资源转换(mek/waz/spm，含 hitbox)
 * -> Step5: 回写 MekBasicInfo 的 waz/spm 索引
 * -> Step6: UI SPM 挂接（替换 Nanoha 槽位）
 */
public class TransMekaPipeline {

    private final BatVoiceConverter batVoiceConverter = new BatVoiceConverter();
    private final GrpRegistryUpdater grpRegistryUpdater = new GrpRegistryUpdater();
    private final SpriteGroupIndexMapper spriteGroupIndexMapper = new SpriteGroupIndexMapper();
    private final MekConverter mekConverter = new MekConverter();
    private final WazConverter wazConverter = new WazConverter();
    private final SpmConverter spmConverter = new SpmConverter();
    private final UiSpmReplacer uiSpmReplacer = new UiSpmReplacer();

    public TransMekaResult execute(TransMekaRequest request) {
        TransMekaResult result = new TransMekaResult();
        if (request == null) {
            return result;
        }

        // Step1: batvoice 深拷贝并追加到 BSDX 注册表
        if (request.getBheBatVoiceGroup() != null && request.getBsdxBatVoice() != null) {
            BatVoiceGrp.BatVoiceGroup batVoice = batVoiceConverter.convert(request.getBheBatVoiceGroup());
            request.getBsdxBatVoice().getVoiceList().add(batVoice);
            result.setBsdxBatVoiceGroup(batVoice);
            result.setBatVoiceIndex(request.getBsdxBatVoice().getVoiceList().size() - 1);
        }

        // Step2: grp 对齐，返回最终序号（索引用于 mek/waz/spm 对齐）
        result.setMekaGroupIndex(
                grpRegistryUpdater.upsertMekaGroup(request.getBsdxMekaGroup(), request.getBheMekaGroup())
        );
        result.setWazaGroupIndex(
                grpRegistryUpdater.upsertWazaGroup(request.getBsdxWazaGroup(), request.getBheWazaGroup())
        );
        result.setSpriteGroupIndex(
                grpRegistryUpdater.upsertSpriteGroup(request.getBsdxSpriteGroup(), request.getBheSpriteGroupEntry())
        );

        // Step3: spritegroup 映射（BHE 索引 -> BSDX 索引）
        // 先从 BHE mek 的 materialBlock 抽取需要的 spritegroup 索引，再按 BHE grp 重建到 BSDX
        Map<Integer, Integer> spriteIndexMap = spriteGroupIndexMapper.buildMap(
                request.getBheSpriteGroup(),
                request.getBsdxSpriteGroup(),
                spriteGroupIndexMapper.collectRequiredIndicesFromMek(request.getBheMek())
        );
        result.setSpriteIndexMap(spriteIndexMap);

        // Step4: 转换核心资源（mek/waz/spm）
        result.setBsdxMeka(mekConverter.convert(request.getBheMek(), spriteIndexMap));
        result.setBsdxWaz(wazConverter.convert(request.getBheWaz()));
        result.setBsdxSpm(spmConverter.convert(request.getBheSpm()));
        result.setBsdxCSpm(spmConverter.convert(request.getBheCSpm()));
        result.setBsdxSSpm(spmConverter.convert(request.getBheSSpm()));
        result.setBsdxGSpm(spmConverter.convert(request.getBheGSpm()));
        result.setBsdxMSpm(spmConverter.convert(request.getBheMSpm()));

        // Step5: 回写 mek 内部的 waz/spm 索引
        alignMekIndex(result.getBsdxMeka(), result.getWazaGroupIndex(), result.getSpriteGroupIndex());

        // Step6: UI 资源表挂接（用 tsukuyomi 替换 nanoha 槽位，便于测试）
        UiSpmReplacer.UiSpmReplaceResult uiReplaceResult = uiSpmReplacer.replaceNanohaWithTsukuyomi(
                request.getMekaPilotSpm(),
                request.getSelectMekaMenuMekaSpm(),
                result.getBsdxMSpm(),
                result.getBsdxSSpm(),
                request.getSelectMekaMenuDat(),
                request.getBsdxMekaGroup(),
                result.getBsdxMeka()
        );
        if (uiReplaceResult.isMekaPilotReplaced()) {
            result.setBsdxMekaPilotSpm(request.getMekaPilotSpm());
        }
        if (uiReplaceResult.isSelectMekaMenuReplaced()) {
            result.setBsdxSelectMekaMenuMekaSpm(request.getSelectMekaMenuMekaSpm());
        }

        return result;
    }

    private void alignMekIndex(Mek mek, int wazaIndex, int spriteIndex) {
        if (mek == null || mek.getMekBasicInfo() == null) {
            return;
        }
        if (wazaIndex >= 0) {
            mek.getMekBasicInfo().setWazFileSequence(wazaIndex);
        }
        if (spriteIndex >= 0) {
            mek.getMekBasicInfo().setSpmFileSequence(spriteIndex);
        }
    }
}
