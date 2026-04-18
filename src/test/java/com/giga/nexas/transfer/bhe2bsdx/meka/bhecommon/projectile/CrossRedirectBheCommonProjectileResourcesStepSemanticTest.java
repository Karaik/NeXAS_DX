package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross.CrossRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self.SelfRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossRedirectBheCommonProjectileResourcesStepSemanticTest {

    private static final Path OUTPUT_DIR =
            Paths.get("src/main/resources/out/bhe2bsdx/common-projectile-cross-redirect");

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ConvertBheCommonProjectileResourcesStep convertStep =
            new ConvertBheCommonProjectileResourcesStep();
    private final SelfRedirectBheCommonProjectileResourcesStep selfRedirectStep =
            new SelfRedirectBheCommonProjectileResourcesStep();
    private final CrossRedirectBheCommonProjectileResourcesStep crossRedirectStep =
            new CrossRedirectBheCommonProjectileResourcesStep();

    @Test
    void rewriteCommonWazReferencesAndWriteAudit() throws Exception {
        TsukuyomiRawSourceBundle rawSourceBundle = loadRawSourceBundle();
        TsukuyomiBsdxBaselineBundle baseline = loadBsdxBaseline();
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();
        BheCommonProjectileAppendPlan appendPlan = new BheCommonProjectileAppendPlan();

        convertStep.convert(rawSourceBundle, convertedBundle);
        selfRedirectStep.redirect(null, rawSourceBundle, baseline, convertedBundle, appendPlan);
        crossRedirectStep.redirect(null, rawSourceBundle, baseline, convertedBundle, appendPlan);

        CrossAudit audit = auditCommonWaz(convertedBundle, baseline, appendPlan);
        assertTrue(audit.wazaSelectCount > 0, "公共 WAZ 应该包含 CEventWazaSelect");
        assertTrue(audit.spriteCount > 0, "公共 WAZ 应该包含 CEventSprite");
        assertTrue(audit.seEntryCount > 0, "公共 WAZ 应该包含 CEventSe");
        assertFalse(appendPlan.getGlobalVoiceReferences().isEmpty(), "公共 WAZ 应记录 SOU/MISAKI voice 外部依赖");
        assertTrue(appendPlan.getGlobalVoiceReferences().stream().anyMatch(ref -> ref.contains("SOU")));
        assertTrue(appendPlan.getGlobalVoiceReferences().stream().anyMatch(ref -> ref.contains("MISAKI")));
        List<String> missingSeTargets = buildMissingSeTargetReport(audit, appendPlan);
        assertTrue(missingSeTargets.size() == 1 && missingSeTargets.get(0).contains("47:55"),
                "转换后公共 WAZ 只允许缺少已知的 BHE slot69 源 SE 引用: " + missingSeTargets);

        writeAuditReport(audit, appendPlan, missingSeTargets);
    }

    private CrossAudit auditCommonWaz(
            TsukuyomiConvertedBundle convertedBundle,
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        CrossAudit audit = new CrossAudit();
        Map<Integer, String> targetWazFileNameByIndex = invert(appendPlan.getSourceWazIndexToTargetIndex(),
                appendPlan.getSourceWazIndexToTargetFileName());
        Map<Integer, String> targetSpriteFileNameByIndex = invert(appendPlan.getSourceSpriteIndexToTargetIndex(),
                appendPlan.getSourceSpriteIndexToTargetFileName());

        for (Map.Entry<String, Waz> entry : convertedBundle.getCommonProjectileResourceBundle().getWazByFileName().entrySet()) {
            auditWaz(entry.getKey(), entry.getValue(), baseline, appendPlan, targetWazFileNameByIndex, targetSpriteFileNameByIndex, audit);
        }
        return audit;
    }

    private void auditWaz(
            String fileName,
            Waz waz,
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan,
            Map<Integer, String> targetWazFileNameByIndex,
            Map<Integer, String> targetSpriteFileNameByIndex,
            CrossAudit audit
    ) {
        if (waz == null || waz.getSkillList() == null) {
            return;
        }
        for (Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                        auditObject(fileName, object, baseline, appendPlan, targetWazFileNameByIndex, targetSpriteFileNameByIndex, audit);
                    }
                }
            }
        }
    }

    private void auditObject(
            String fileName,
            SkillInfoObject object,
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan,
            Map<Integer, String> targetWazFileNameByIndex,
            Map<Integer, String> targetSpriteFileNameByIndex,
            CrossAudit audit
    ) {
        if (object == null) {
            return;
        }
        if (object instanceof CEventWazaSelect select) {
            auditWazaSelect(fileName, select, baseline, targetWazFileNameByIndex, audit);
        } else if (object instanceof CEventSprite sprite) {
            auditSprite(fileName, sprite, baseline, targetSpriteFileNameByIndex, audit);
        } else if (object instanceof CEventSe se) {
            auditSe(fileName, se, appendPlan, audit);
        } else if (object instanceof CEventVoice voice) {
            audit.voiceCount += voice.getByteDataList() == null ? 0 : voice.getByteDataList().size();
        }

        for (Field field : getAllFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType())
                    || !field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
                continue;
            }
            field.setAccessible(true);
            try {
                List<?> units = (List<?>) field.get(object);
                if (units == null) {
                    continue;
                }
                for (Object unit : units) {
                    SkillInfoObject nested = tryGetUnitData(unit);
                    if (nested != null) {
                        auditObject(fileName, nested, baseline, appendPlan, targetWazFileNameByIndex, targetSpriteFileNameByIndex, audit);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("审计公共 WAZ 交叉重定向失败: " + field.getName(), e);
            }
        }
    }

    private void auditWazaSelect(
            String fileName,
            CEventWazaSelect select,
            TsukuyomiBsdxBaselineBundle baseline,
            Map<Integer, String> targetWazFileNameByIndex,
            CrossAudit audit
    ) {
        Integer targetWazIndex = select.getWazFileNo();
        if (targetWazIndex == null || targetWazIndex < 0) {
            return;
        }
        String targetFileName = targetWazFileNameByIndex.get(targetWazIndex);
        assertTrue(targetFileName != null, "CEventWazaSelect 未落到公共 WAZ: " + fileName + " -> " + targetWazIndex);
        Waz targetWaz = baseline.getWazByFileName().get(targetFileName);
        assertTrue(targetWaz != null, "preparedBaseline 缺少目标 WAZ: " + targetFileName);
        assertTrue(select.getWazSequenceNo() == null
                        || select.getWazSequenceNo() < 0
                        || select.getWazSequenceNo() < targetWaz.getSkillList().size(),
                "CEventWazaSelect skill 越界: " + fileName + " -> " + targetFileName + ":" + select.getWazSequenceNo());
        audit.wazaSelectCount++;
        audit.targetWazIndices.add(targetWazIndex);
    }

    private void auditSprite(
            String fileName,
            CEventSprite sprite,
            TsukuyomiBsdxBaselineBundle baseline,
            Map<Integer, String> targetSpriteFileNameByIndex,
            CrossAudit audit
    ) {
        Integer targetSpriteIndex = sprite.getSpmFileSequence();
        if (targetSpriteIndex == null || targetSpriteIndex < 0) {
            return;
        }
        String targetFileName = targetSpriteFileNameByIndex.get(targetSpriteIndex);
        assertTrue(targetFileName != null, "CEventSprite 未落到公共 SPM: " + fileName + " -> " + targetSpriteIndex);
        Spm targetSpm = baseline.getSpmByFileName().get(targetFileName);
        assertTrue(targetSpm != null, "preparedBaseline 缺少目标 SPM: " + targetFileName);
        assertTrue(sprite.getActionGroupNumber() == null
                        || sprite.getActionGroupNumber() < 0
                        || sprite.getActionGroupNumber() < targetSpm.getAnimData().size(),
                "CEventSprite anim 越界: " + fileName + " -> " + targetFileName + ":" + sprite.getActionGroupNumber());
        audit.spriteCount++;
        audit.targetSpriteIndices.add(targetSpriteIndex);
    }

    private void auditSe(
            String fileName,
            CEventSe se,
            BheCommonProjectileAppendPlan appendPlan,
            CrossAudit audit
    ) {
        if (se.getByteDataList() == null) {
            return;
        }
        for (byte[] bytes : se.getByteDataList()) {
            if (bytes == null || bytes.length < 8) {
                continue;
            }
            int targetGroupIndex = readLittleEndianInt(bytes, 0);
            int targetItemIndex = readLittleEndianInt(bytes, 4);
            assertTrue(targetGroupIndex == appendPlan.getCommonProjectileSeGroupIndex(),
                    "CEventSe group 未落到公共聚合组: " + fileName + " -> " + targetGroupIndex);
            assertTrue(targetItemIndex >= 0 && targetItemIndex < appendPlan.getSourceSePairToTargetItemIndex().size(),
                    "CEventSe item 越界: " + fileName + " -> " + targetItemIndex);
            audit.seEntryCount++;
            audit.targetSeItems.add(targetItemIndex);
        }
    }

    private Map<Integer, String> invert(Map<Integer, Integer> sourceToTargetIndex, Map<Integer, String> sourceToFileName) {
        Map<Integer, String> result = new LinkedHashMap<>();
        sourceToTargetIndex.forEach((sourceIndex, targetIndex) ->
                result.put(targetIndex, sourceToFileName.get(sourceIndex)));
        return result;
    }

    private TsukuyomiRawSourceBundle loadRawSourceBundle() throws Exception {
        TsukuyomiRawSourceBundle bundle = new TsukuyomiRawSourceBundle();
        bundle.setBatVoiceGrp(read("src/main/resources/grpBheJson/batvoice.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.class));
        bundle.setWazaGroupGrp(read("src/main/resources/grpBheJson/wazagroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.class));
        bundle.setSpriteGroupGrp(read("src/main/resources/grpBheJson/spritegroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.class));
        bundle.setSeGroupGrp(read("src/main/resources/grpBheJson/segroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.class));
        for (String fileName : BheCommonProjectileResources.COMMON_PROJECTILE_WAZ_FILES) {
            bundle.getCommonProjectileWazByFileName().put(fileName, readWaz(fileName));
        }
        for (String fileName : BheCommonProjectileResources.COMMON_PROJECTILE_SPM_FILES) {
            bundle.getCommonProjectileSpmByFileName().put(fileName, readSpm(fileName));
        }
        return bundle;
    }

    private TsukuyomiBsdxBaselineBundle loadBsdxBaseline() throws Exception {
        TsukuyomiBsdxBaselineBundle bundle = new TsukuyomiBsdxBaselineBundle();
        bundle.setWazaGroupGrp(read("src/main/resources/grpBsdxJson/WazaGroup.grp.json", WazaGroupGrp.class));
        bundle.setSpriteGroupGrp(read("src/main/resources/grpBsdxJson/SpriteGroup.grp.json", SpriteGroupGrp.class));
        bundle.setSeGroupGrp(read("src/main/resources/grpBsdxJson/SeGroup.grp.json", SeGroupGrp.class));
        bundle.setBatVoiceGrp(read("src/main/resources/grpBsdxJson/BatVoice.grp.json", BatVoiceGrp.class));
        bundle.setProgramMaterialGrp(read("src/main/resources/grpBsdxJson/ProgramMaterial.grp.json", ProgramMaterialGrp.class));
        bundle.setMapGroupGrp(read("src/main/resources/grpBsdxJson/MapGroup.grp.json", MapGroupGrp.class));
        bundle.getMekByFileName().put("aki.mek", read("src/main/resources/mekBsdxJson/Aki.mek.json", Mek.class));
        return bundle;
    }

    private com.giga.nexas.dto.bhe.waz.Waz readWaz(String fileName) throws Exception {
        String baseName = fileName.substring(0, fileName.length() - ".waz".length());
        return read("src/main/resources/wazBheJson/" + baseName + ".waz.json", com.giga.nexas.dto.bhe.waz.Waz.class);
    }

    private com.giga.nexas.dto.bhe.spm.Spm readSpm(String fileName) throws Exception {
        return read("src/main/resources/spmBheJson/" + fileName + ".json", com.giga.nexas.dto.bhe.spm.Spm.class);
    }

    private <T> T read(String path, Class<T> type) throws Exception {
        return mapper.readValue(Paths.get(path).toFile(), type);
    }

    private SkillInfoObject tryGetUnitData(Object unit) {
        if (unit == null) {
            return null;
        }
        try {
            Method getter = unit.getClass().getMethod("getData");
            Object value = getter.invoke(unit);
            return value instanceof SkillInfoObject object ? object : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private void writeAuditReport(
            CrossAudit audit,
            BheCommonProjectileAppendPlan appendPlan,
            List<String> missingSeTargets
    ) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        mapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT_DIR.resolve("append-plan-after-cross.json").toFile(), appendPlan);

        StringBuilder report = new StringBuilder();
        report.append("# BHE common projectile cross redirect audit\n\n");
        report.append("- CEventWazaSelect count: ").append(audit.wazaSelectCount).append("\n");
        report.append("- CEventSprite count: ").append(audit.spriteCount).append("\n");
        report.append("- CEventSe byte entries: ").append(audit.seEntryCount).append("\n");
        report.append("- CEventVoice byte entries recorded: ").append(audit.voiceCount).append("\n");
        report.append("- target WAZ indices: ").append(audit.targetWazIndices).append("\n");
        report.append("- target SPM indices: ").append(audit.targetSpriteIndices).append("\n");
        report.append("- unique SE target items: ").append(audit.targetSeItems.size()).append("\n");
        report.append("- SE target items not referenced after WAZ conversion: ").append(missingSeTargets).append("\n");
        report.append("- known reason: `47:55` appears only under BHE WAZ slot 69, ")
                .append("which is declared dropped by the BHE->BSDX slot map.\n");
        report.append("- voice references: ").append(appendPlan.getGlobalVoiceReferences()).append("\n");
        Files.writeString(OUTPUT_DIR.resolve("audit.md"), report.toString(), StandardCharsets.UTF_8);
    }

    private List<String> buildMissingSeTargetReport(CrossAudit audit, BheCommonProjectileAppendPlan appendPlan) {
        List<String> missing = new ArrayList<>();
        appendPlan.getSourceSePairToTargetItemIndex().forEach((sourcePair, targetItemIndex) -> {
            if (!audit.targetSeItems.contains(targetItemIndex)) {
                missing.add(sourcePair + " -> " + appendPlan.getCommonProjectileSeGroupIndex() + ":" + targetItemIndex
                        + " " + appendPlan.getSourceSePairToTargetFileName().get(sourcePair));
            }
        });
        return missing;
    }

    private static class CrossAudit {
        int wazaSelectCount;
        int spriteCount;
        int seEntryCount;
        int voiceCount;
        Set<Integer> targetWazIndices = new LinkedHashSet<>();
        Set<Integer> targetSpriteIndices = new LinkedHashSet<>();
        Set<Integer> targetSeItems = new LinkedHashSet<>();
    }
}
