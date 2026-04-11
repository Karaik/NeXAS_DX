package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 负责把 step6/7 产物和当前机体链上的静态资源落盘到输出根目录。
 *
 * <p>当前统一平铺写入：
 * `src/main/resources/out/jinki2bsdx_assets_<timestamp>`</p>
 */
public class ImportStaticAssetsStep {

    private static final String CHARSET = "windows-31j";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final RebindAkaoWazStep rebindAkaoWazStep = new RebindAkaoWazStep();

    public ImportedAssetSet importAssets(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            ProgramMaterialGrp syncedProgramMaterial,
            Mek reboundAkaoMek,
            Waz reboundAkaoWaz,
            GrpAppendPlan grpAppendPlan
    ) {
        ImportedAssetSet importedAssetSet = new ImportedAssetSet();
        if (request == null || jinkiPackage == null || bsdxBaseline == null || importPlan == null) {
            return importedAssetSet;
        }

        importedAssetSet.getWazFiles().addAll(importPlan.getRequiredWazFiles());
        importedAssetSet.getSpmFiles().addAll(importPlan.getRequiredSpmFiles());
        importedAssetSet.getAuxiliaryFiles().addAll(importPlan.getRequiredMekFiles());

        try {
            // Step 8-1: 创建本次迁移产物的输出根目录。
            Path outputRoot = request.getExeOutputDir().resolve("jinki2bsdx_assets_" + LocalDateTime.now().format(TS));
            Files.createDirectories(outputRoot);
            importedAssetSet.setOutputRootDir(outputRoot);

            // Step 8-2: 先把所有修改过的 grp 和 ProgramMaterial 真正写进产物。
            writePatchedGrpOutputs(bsdxBaseline, syncedProgramMaterial, outputRoot, importedAssetSet);
            writePatchedConfigDatOutputs(jinkiPackage, bsdxBaseline, outputRoot, importedAssetSet);

            // Step 8-3: 再把主 mek / waz 产物直接平铺写到根目录。
            writeReboundMek(request, reboundAkaoMek, outputRoot, importedAssetSet);

            // Step 8-3.5: 补齐后的基线机体 .mek 全部写回（grp 追加后 CMaterial 组数需要同步）。
            writePatchedBaselineMeks(bsdxBaseline, request.getMekFileName(), outputRoot, importedAssetSet);
            writeReboundWaz(request, reboundAkaoWaz, outputRoot, importedAssetSet);

            // Step 8-4: 再把辅助 waz 的 merge/rebind 产物平铺写到根目录。
            writeRequiredAuxiliaryWazFiles(request, jinkiPackage, bsdxBaseline, importPlan, grpAppendPlan, outputRoot, importedAssetSet);

            // Step 8-5: 把链上需要的 spm 平铺复制到根目录。
            copyRequiredSpmFiles(request, importPlan, outputRoot, importedAssetSet);

            // Step 8-6: 再按这些 spm 的 imageData，补齐真正用到的图像文件。
            copyRequiredImageFiles(request, jinkiPackage, bsdxBaseline, importPlan, grpAppendPlan, outputRoot, importedAssetSet);

            // Step 8-7: 最后只补当前机体链真实关联到的音频，不再整组打包。
            copyRequiredAudioAssets(request, jinkiPackage, importPlan, reboundAkaoMek, reboundAkaoWaz, outputRoot, importedAssetSet);
            return importedAssetSet;
        } catch (IOException e) {
            throw new IllegalStateException("step8 静态资源落盘失败", e);
        }
    }

    private void writePatchedGrpOutputs(
            BsdxBaselineBundle bsdxBaseline,
            ProgramMaterialGrp syncedProgramMaterial,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        writeGrp(outputRoot, "MekaGroup.grp", bsdxBaseline.getMekaGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "WazaGroup.grp", bsdxBaseline.getWazaGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "SpriteGroup.grp", bsdxBaseline.getSpriteGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "BatVoice.grp", bsdxBaseline.getBatVoiceGrp(), importedAssetSet);
        writeGrp(outputRoot, "MapGroup.grp", bsdxBaseline.getMapGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "SeGroup.grp", bsdxBaseline.getSeGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "ProgramMaterial.grp", syncedProgramMaterial != null ? syncedProgramMaterial : bsdxBaseline.getProgramMaterialGrp(), importedAssetSet);
    }

    private void writeGrp(Path outputRoot, String fileName, Object grp, ImportedAssetSet importedAssetSet) throws IOException {
        if (grp == null) {
            importedAssetSet.getMissingAssets().add("缺少 grp 产物: " + fileName);
            return;
        }
        Path output = outputRoot.resolve(fileName);
        bsdxBinService.generate(output.toString(), (com.giga.nexas.dto.bsdx.Bsdx) grp, CHARSET);
        importedAssetSet.getGeneratedGrpFiles().add(output);
    }

    private void writePatchedConfigDatOutputs(
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        Dat patchedWeaponEquip = buildPatchedWeaponEquipDat(jinkiPackage, bsdxBaseline);
        if (patchedWeaponEquip == null) {
            return;
        }

        Path rootOutput = outputRoot.resolve("WeaponEquip.dat");
        bsdxBinService.generate(rootOutput.toString(), patchedWeaponEquip, CHARSET);
        importedAssetSet.getGeneratedDatFiles().add(rootOutput);
    }

    private Dat buildPatchedWeaponEquipDat(JinkiPackageBundle jinkiPackage, BsdxBaselineBundle bsdxBaseline) {
        if (jinkiPackage == null || bsdxBaseline == null) {
            return null;
        }
        Dat source = jinkiPackage.getWeaponEquipDat();
        Dat baseline = bsdxBaseline.getWeaponEquipDat();
        if (source == null || baseline == null || source.getData() == null || baseline.getData() == null) {
            return null;
        }

        Dat patched = new Dat();
        patched.setFileName(baseline.getFileName());
        patched.setExtensionName(baseline.getExtensionName());
        patched.setColumnCount(baseline.getColumnCount());
        patched.setColumnTypes(new ArrayList<>(baseline.getColumnTypes()));

        for (List<Object> row : baseline.getData()) {
            patched.addRow(copyDatRow(row));
        }
        for (int i = baseline.getData().size(); i < source.getData().size(); i++) {
            patched.addRow(copyDatRow(source.getData().get(i)));
        }
        return patched;
    }

    private List<Object> copyDatRow(List<Object> row) {
        return row == null ? new ArrayList<>() : new ArrayList<>(row);
    }

    private void writeReboundMek(
            AkaoGraftRequest request,
            Mek reboundAkaoMek,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundAkaoMek == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 mek");
            return;
        }

        Path output = outputRoot.resolve(request.getMekFileName());
        bsdxBinService.generate(output.toString(), reboundAkaoMek, CHARSET);
        importedAssetSet.getGeneratedMekFiles().add(output);
    }

    /**
     * 遍历基线中所有 .mek，将经过 padMaterialBlock 补齐后的产物全部写回输出目录。
     * 跳过当前机体（已由 writeReboundMek 单独写入），避免覆盖。
     */
    private void writePatchedBaselineMeks(
            BsdxBaselineBundle bsdxBaseline,
            String currentMekFileName,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (bsdxBaseline == null || bsdxBaseline.getMekByFileName() == null) {
            return;
        }

        for (Map.Entry<String, Mek> entry : bsdxBaseline.getMekByFileName().entrySet()) {
            String fileName = entry.getKey();
            Mek mek = entry.getValue();

            if (mek == null) {
                continue;
            }

            // 跳过当前机体的 mek，避免与 writeReboundMek 重复写入
            if (normalize(fileName).equals(normalize(currentMekFileName))) {
                continue;
            }

            Path output = outputRoot.resolve(fileName);
            bsdxBinService.generate(output.toString(), mek, CHARSET);
            importedAssetSet.getGeneratedMekFiles().add(output);
        }
    }

    private void writeReboundWaz(
            AkaoGraftRequest request,
            Waz reboundAkaoWaz,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundAkaoWaz == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 waz");
            return;
        }

        Path output = outputRoot.resolve(request.getWazFileName());
        bsdxBinService.generate(output.toString(), reboundAkaoWaz, CHARSET);
        importedAssetSet.getGeneratedWazFiles().add(output);
    }

    private void writeRequiredAuxiliaryWazFiles(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            GrpAppendPlan grpAppendPlan,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : importPlan.getRequiredWazFiles()) {
            if (normalize(fileName).equals(normalize(request.getWazFileName()))) {
                continue;
            }

            Waz sourceWaz = findWazByFileName(jinkiPackage.getWazByFileName(), fileName);
            if (sourceWaz == null) {
                importedAssetSet.getMissingAssets().add("缺少辅助 waz: " + fileName);
                continue;
            }

            Integer sourceWazGroupIndex = findSourceWazGroupIndex(importPlan, fileName);
            Waz baselineWaz = findWazByFileName(bsdxBaseline.getWazByFileName(), fileName);

            // 辅助 WAZ 不能直接复制 JINKI 文件：
            // BSDX 同名 WAZ 里有大量原生技能，JINKI 侧也有空槽，所以这里输出 key-based merge 后的 WAZ。
            Waz outputWaz = rebindAkaoWazStep.rebindAuxiliaryWaz(
                    request,
                    sourceWaz,
                    baselineWaz,
                    importPlan,
                    grpAppendPlan,
                    sourceWazGroupIndex
            );

            Path output = outputRoot.resolve(fileName);
            bsdxBinService.generate(output.toString(), outputWaz, CHARSET);
            importedAssetSet.getGeneratedWazFiles().add(output);
        }
    }

    private Integer findSourceWazGroupIndex(JinkiImportPlan importPlan, String fileName) {
        if (importPlan == null || importPlan.getSourceWazIndexByFileName() == null) {
            return null;
        }
        for (Map.Entry<String, Integer> entry : importPlan.getSourceWazIndexByFileName().entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void copyRequiredSpmFiles(
            AkaoGraftRequest request,
            JinkiImportPlan importPlan,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : importPlan.getRequiredSpmFiles()) {
            Path source = resolveFileCaseInsensitive(request.getJinkiSpmDir(), fileName);
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少 spm: " + fileName);
                continue;
            }

            Path output = outputRoot.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedSpmFiles().add(output);
        }
    }

    private void copyRequiredImageFiles(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            GrpAppendPlan grpAppendPlan,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在: " + request.getExternalStaticAssetRoot());
            return;
        }

        Set<String> imageNames = collectRequiredImageNames(jinkiPackage, importPlan);
        imageNames.addAll(collectGraftedSkillImageNames(jinkiPackage, bsdxBaseline, importPlan, grpAppendPlan, outputRoot));
        if (imageNames.isEmpty()) {
            return;
        }

        for (String imageName : imageNames) {
            Path source = resolveExternalFileCaseInsensitive(request.getExternalStaticAssetRoot(), imageName);
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少图像资源: " + imageName);
                continue;
            }

            Path output = outputRoot.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedImageFiles().add(output);
        }
    }

    private Set<String> collectRequiredImageNames(JinkiPackageBundle jinkiPackage, JinkiImportPlan importPlan) {
        Set<String> imageNames = new LinkedHashSet<>();
        Map<String, Spm> spmByFileName = jinkiPackage.getSpmByFileName();
        if (spmByFileName == null) {
            return imageNames;
        }

        for (String fileName : importPlan.getRequiredSpmFiles()) {
            Spm spm = findSpm(spmByFileName, fileName);
            if (spm == null || spm.getImageData() == null) {
                continue;
            }

            for (Spm.SPMImageData imageData : spm.getImageData()) {
                if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                    continue;
                }
                imageNames.add(imageData.getImageName());
            }
        }

        return imageNames;
    }

    private Set<String> collectGraftedSkillImageNames(
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            GrpAppendPlan grpAppendPlan,
            Path outputRoot
    ) throws IOException {
        Set<String> imageNames = new LinkedHashSet<>();
        if (jinkiPackage == null || bsdxBaseline == null || importPlan == null || grpAppendPlan == null || outputRoot == null) {
            return imageNames;
        }

        for (String fileName : importPlan.getRequiredWazFiles()) {
            Integer sourceWazGroupIndex = findSourceWazGroupIndex(importPlan, fileName);
            if (sourceWazGroupIndex == null) {
                continue;
            }

            Map<Integer, Integer> skillIndexMap = grpAppendPlan.getSourceWazSkillIndexToTargetIndexByGroup().get(sourceWazGroupIndex);
            if (skillIndexMap == null || skillIndexMap.isEmpty()) {
                continue;
            }

            Waz sourceWaz = findWazByFileName(jinkiPackage.getWazByFileName(), fileName);
            Waz baselineWaz = findWazByFileName(bsdxBaseline.getWazByFileName(), fileName);
            Waz outputWaz = parseOutputWaz(outputRoot, fileName);
            if (sourceWaz == null || outputWaz == null || sourceWaz.getSkillList() == null || outputWaz.getSkillList() == null) {
                continue;
            }

            Set<String> baselineKeys = collectSkillKeys(baselineWaz);
            int baselineSkillCount = baselineWaz == null || baselineWaz.getSkillList() == null ? 0 : baselineWaz.getSkillList().size();

            for (int sourceSkillIndex = 0; sourceSkillIndex < sourceWaz.getSkillList().size(); sourceSkillIndex++) {
                Waz.Skill sourceSkill = sourceWaz.getSkillList().get(sourceSkillIndex);
                String sourceKey = normalizeSkillKey(sourceSkill);
                if (sourceKey.isEmpty()) {
                    continue;
                }

                Integer targetSkillIndex = skillIndexMap.get(sourceSkillIndex);
                if (targetSkillIndex == null || targetSkillIndex < 0 || targetSkillIndex >= outputWaz.getSkillList().size()) {
                    continue;
                }

                boolean graftedSkill = targetSkillIndex >= baselineSkillCount || !baselineKeys.contains(sourceKey);
                if (!graftedSkill) {
                    continue;
                }

                // 只从 JINKI 新增/新设 skill 出发递归收图；
                // BSDX 原生 skill 的图片仍由原版资源承担，避免把整份 SPM 图片打进 Update3。
                collectImageNamesFromReachableSkill(
                        outputWaz,
                        targetSkillIndex,
                        bsdxBaseline,
                        jinkiPackage,
                        outputRoot,
                        imageNames,
                        new LinkedHashSet<>()
                );
            }
        }

        return imageNames;
    }

    private Set<String> collectSkillKeys(Waz waz) {
        Set<String> keys = new LinkedHashSet<>();
        if (waz == null || waz.getSkillList() == null) {
            return keys;
        }
        for (Waz.Skill skill : waz.getSkillList()) {
            String key = normalizeSkillKey(skill);
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        return keys;
    }

    private String normalizeSkillKey(Waz.Skill skill) {
        if (skill == null || skill.getSkillNameEnglish() == null) {
            return "";
        }
        return skill.getSkillNameEnglish().trim().toLowerCase(Locale.ROOT);
    }

    private Waz parseOutputWaz(Path outputRoot, String fileName) throws IOException {
        Path output = outputRoot.resolve(fileName);
        if (!Files.exists(output)) {
            output = resolveFileCaseInsensitive(outputRoot, fileName);
        }
        if (output == null || !Files.exists(output)) {
            return null;
        }
        return (Waz) bsdxBinService.parse(output.toString(), CHARSET).getData();
    }

    private void collectImageNamesFromReachableSkill(
            Waz waz,
            int skillIndex,
            BsdxBaselineBundle bsdxBaseline,
            JinkiPackageBundle jinkiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (waz == null || waz.getSkillList() == null || skillIndex < 0 || skillIndex >= waz.getSkillList().size()) {
            return;
        }
        String visitKey = normalize(waz.getFileName()) + "#" + skillIndex;
        if (!visited.add(visitKey)) {
            // Effect/Bomb 等特效 WAZ 可能互相引用；同一 skill 已访问过就停止展开。
            return;
        }
        collectImageNamesFromSkill(waz.getSkillList().get(skillIndex), bsdxBaseline, jinkiPackage, outputRoot, imageNames, visited);
    }

    private void collectImageNamesFromSkill(
            Waz.Skill skill,
            BsdxBaselineBundle bsdxBaseline,
            JinkiPackageBundle jinkiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (skill == null || skill.getPhasesInfo() == null) {
            return;
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
                    collectImageNamesFromObject(object, bsdxBaseline, jinkiPackage, outputRoot, imageNames, visited);
                }
            }
        }
    }

    private void collectImageNamesFromObject(
            SkillInfoObject object,
            BsdxBaselineBundle bsdxBaseline,
            JinkiPackageBundle jinkiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (object == null) {
            return;
        }

        if (object instanceof CEventSprite sprite) {
            // 图片依赖最终由 CEventSprite 决定：spmFileSequence + actionGroupNumber 指向具体 SPM 动画。
            collectImageNamesFromSpriteAction(sprite, bsdxBaseline, jinkiPackage, outputRoot, imageNames);
        }

        if (object instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect select) {
            // 新增 skill 可能继续调用其他 WAZ skill，所以图片依赖也要沿 WAZ 引用递归下去。
            collectImageNamesFromWazRef(select, bsdxBaseline, jinkiPackage, outputRoot, imageNames, visited);
        }

        for (Field field : getAllFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType()) || !field.getName().endsWith("UnitList")) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object units = field.get(object);
                if (!(units instanceof List<?> unitList)) {
                    continue;
                }
                for (Object unit : unitList) {
                    SkillInfoObject data = tryGetUnitData(unit);
                    if (data != null) {
                        collectImageNamesFromObject(data, bsdxBaseline, jinkiPackage, outputRoot, imageNames, visited);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("鏀堕泦 WAZ 鍐呭祵 sprite 鍥惧儚澶辫触: " + field.getName(), e);
            }
        }
    }

    private void collectImageNamesFromWazRef(
            com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect select,
            BsdxBaselineBundle bsdxBaseline,
            JinkiPackageBundle jinkiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (select.getWazFileNo() == null || select.getWazFileNo() < 0 || select.getWazSequenceNo() == null || select.getWazSequenceNo() < 0
                || bsdxBaseline.getWazaGroupGrp() == null || bsdxBaseline.getWazaGroupGrp().getWazaList() == null
                || select.getWazFileNo() >= bsdxBaseline.getWazaGroupGrp().getWazaList().size()) {
            return;
        }
        var entry = bsdxBaseline.getWazaGroupGrp().getWazaList().get(select.getWazFileNo());
        if (entry == null || entry.getWazaDisplayName() == null || entry.getWazaDisplayName().isBlank()) {
            return;
        }
        Waz targetWaz = parseOutputWaz(outputRoot, entry.getWazaDisplayName() + ".waz");
        if (targetWaz == null) {
            // 没有输出覆盖的原生 WAZ 仍可从 BSDX baseline 读取结构，用于继续解析可达图片。
            targetWaz = findWazByFileName(bsdxBaseline.getWazByFileName(), entry.getWazaDisplayName() + ".waz");
        }
        collectImageNamesFromReachableSkill(targetWaz, select.getWazSequenceNo(), bsdxBaseline, jinkiPackage, outputRoot, imageNames, visited);
    }

    private void collectImageNamesFromSpriteAction(
            CEventSprite sprite,
            BsdxBaselineBundle bsdxBaseline,
            JinkiPackageBundle jinkiPackage,
            Path outputRoot,
            Set<String> imageNames
    ) throws IOException {
        Integer spriteGroupIndex = sprite.getSpmFileSequence();
        if (spriteGroupIndex == null || spriteGroupIndex < 0 || bsdxBaseline.getSpriteGroupGrp() == null
                || bsdxBaseline.getSpriteGroupGrp().getSpriteList() == null
                || spriteGroupIndex >= bsdxBaseline.getSpriteGroupGrp().getSpriteList().size()) {
            return;
        }

        SpriteGroupGrp.SpriteGroupEntry spriteEntry = bsdxBaseline.getSpriteGroupGrp().getSpriteList().get(spriteGroupIndex);
        if (spriteEntry == null || spriteEntry.getSpriteFileName() == null || spriteEntry.getSpriteFileName().isBlank()) {
            return;
        }

        Spm spm = resolveSpmForImageCollection(spriteEntry.getSpriteFileName(), bsdxBaseline, jinkiPackage, outputRoot);
        if (spm == null || spm.getImageData() == null || spm.getAnimData() == null
                || sprite.getActionGroupNumber() == null || sprite.getActionGroupNumber() < 0
                || sprite.getActionGroupNumber() >= spm.getAnimData().size()) {
            return;
        }

        Spm.SPMAnimData animData = spm.getAnimData().get(sprite.getActionGroupNumber());
        if (animData == null || animData.getPatData() == null) {
            return;
        }

        // 按 actionGroup 收该动画实际 page/chip 用图；
        // 这里不再整份复制 SPM.imageData，避免 bomb/mark/tama 这类大图集被过量带入。
        for (Spm.SPMPatData patData : animData.getPatData()) {
            if (patData == null || patData.getPageNo() == null) {
                continue;
            }
            for (Integer pageNo : patData.getPageNo()) {
                collectImageNamesFromPage(spm, pageNo, imageNames);
            }
        }
    }

    private void collectImageNamesFromPage(Spm spm, Integer pageNo, Set<String> imageNames) {
        if (spm == null || spm.getPageData() == null || spm.getImageData() == null
                || pageNo == null || pageNo < 0 || pageNo >= spm.getPageData().size()) {
            return;
        }
        Spm.SPMPageData pageData = spm.getPageData().get(pageNo);
        if (pageData == null || pageData.getChipData() == null) {
            return;
        }
        for (Spm.SPMChipData chipData : pageData.getChipData()) {
            if (chipData == null || chipData.getImageNo() == null
                    || chipData.getImageNo() < 0 || chipData.getImageNo() >= spm.getImageData().size()) {
                continue;
            }
            Spm.SPMImageData imageData = spm.getImageData().get(chipData.getImageNo());
            if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                continue;
            }
            imageNames.add(imageData.getImageName());
        }
    }

    private Spm resolveSpmForImageCollection(
            String fileName,
            BsdxBaselineBundle bsdxBaseline,
            JinkiPackageBundle jinkiPackage,
            Path outputRoot
    ) throws IOException {
        Path outputSpm = resolveFileCaseInsensitive(outputRoot, fileName);
        if (outputSpm != null && Files.exists(outputSpm)) {
            return (Spm) bsdxBinService.parse(outputSpm.toString(), CHARSET).getData();
        }

        Spm spm = findSpm(jinkiPackage.getSpmByFileName(), fileName);
        if (spm != null) {
            return spm;
        }
        return findSpm(bsdxBaseline.getSpmByFileName(), fileName);
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
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private void copyRequiredAudioAssets(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiImportPlan importPlan,
            Mek reboundAkaoMek,
            Waz reboundAkaoWaz,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在: " + request.getExternalStaticAssetRoot());
            return;
        }

        Set<String> requiredBaseNames = new LinkedHashSet<>();
        requiredBaseNames.addAll(collectRequiredSeBaseNames(jinkiPackage, importPlan));
        requiredBaseNames.addAll(collectRequiredVoiceBaseNames(jinkiPackage, reboundAkaoMek, reboundAkaoWaz, request.getMekaCodeName()));

        if (requiredBaseNames.isEmpty()) {
            return;
        }

        Map<String, Path> externalIndex = buildPreferredExternalAudioIndex(request.getExternalStaticAssetRoot());
        for (String baseName : requiredBaseNames) {
            Path source = externalIndex.get(normalizeBaseName(baseName));
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少音频资源: " + baseName);
                continue;
            }

            Path output = outputRoot.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedAudioFiles().add(output);
        }
    }

    private Set<String> collectRequiredSeBaseNames(JinkiPackageBundle jinkiPackage, JinkiImportPlan importPlan) {
        Set<String> baseNames = new LinkedHashSet<>();
        SeGroupGrp seGroupGrp = jinkiPackage.getSeGroupGrp();
        if (seGroupGrp == null) {
            return baseNames;
        }

        for (Map.Entry<Integer, List<Integer>> entry : importPlan.getReferencedSourceSeItemIndicesByGroupIndex().entrySet()) {
            Integer sourceGroupIndex = entry.getKey();
            if (sourceGroupIndex == null || sourceGroupIndex < 0 || sourceGroupIndex >= seGroupGrp.getSeList().size()) {
                continue;
            }

            SeGroupGrp.SeGroupGroup sourceGroup = seGroupGrp.getSeList().get(sourceGroupIndex);
            if (sourceGroup == null || sourceGroup.getSeItems() == null) {
                continue;
            }

            for (Integer sourceItemIndex : entry.getValue()) {
                if (sourceItemIndex == null || sourceItemIndex < 0 || sourceItemIndex >= sourceGroup.getSeItems().size()) {
                    continue;
                }
                SeGroupGrp.SeGroupItem item = sourceGroup.getSeItems().get(sourceItemIndex);
                if (item == null || !isExisting(item.getExistFlag()) || item.getSeFileName() == null || item.getSeFileName().isBlank()) {
                    continue;
                }
                baseNames.add(item.getSeFileName());
            }
        }
        return baseNames;
    }

    private Set<String> collectRequiredVoiceBaseNames(
            JinkiPackageBundle jinkiPackage,
            Mek reboundAkaoMek,
            Waz reboundAkaoWaz,
            String codeName
    ) {
        Set<String> baseNames = new LinkedHashSet<>();
        if (reboundAkaoMek == null && reboundAkaoWaz == null) {
            return baseNames;
        }

        BatVoiceGrp.BatVoiceGroup akaoVoiceGroup = findAkaoVoiceGroup(jinkiPackage.getBatVoiceGrp(), codeName);
        if (akaoVoiceGroup == null || akaoVoiceGroup.getVoices() == null) {
            return baseNames;
        }

        // WAZ 里的 CEventVoice 负责战斗事件链上的显式语音。
        Set<Integer> usedVoiceIndices = new LinkedHashSet<>(collectUsedVoiceIndices(reboundAkaoWaz));
        // MEK 里的 MekVoiceInfo 则负责 confirm 后立刻消费的 enter / hurt / combo 等语音表。
        usedVoiceIndices.addAll(collectUsedMekVoiceIndices(reboundAkaoMek));
        for (Integer index : usedVoiceIndices) {
            if (index == null || index < 0 || index >= akaoVoiceGroup.getVoices().size()) {
                continue;
            }
            BatVoiceGrp.BatVoice voice = akaoVoiceGroup.getVoices().get(index);
            if (voice == null || !isExisting(voice.getExistFlag()) || voice.getVoiceFileName() == null || voice.getVoiceFileName().isBlank()) {
                continue;
            }
            baseNames.add(voice.getVoiceFileName());
        }

        return baseNames;
    }

    private Set<Integer> collectUsedMekVoiceIndices(Mek mek) {
        Set<Integer> indices = new LinkedHashSet<>();
        if (mek == null || mek.getMekVoiceInfo() == null || mek.getMekVoiceInfo().getTable() == null) {
            return indices;
        }

        for (List<List<Mek.MekVoiceInfo.Entry>> row : mek.getMekVoiceInfo().getTable()) {
            if (row == null) {
                continue;
            }
            for (List<Mek.MekVoiceInfo.Entry> cell : row) {
                if (cell == null) {
                    continue;
                }
                for (Mek.MekVoiceInfo.Entry entry : cell) {
                    if (entry == null || entry.getGroupId() == null || entry.getGroupId() < 0) {
                        continue;
                    }
                    indices.add(entry.getGroupId());
                }
            }
        }

        return indices;
    }

    private List<Integer> collectUsedVoiceIndices(Waz waz) {
        List<Integer> indices = new ArrayList<>();
        if (waz.getSkillList() == null) {
            return indices;
        }

        for (Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (Object object : unit.getSkillInfoObjectList()) {
                        collectUsedVoiceIndicesFromObject(object, indices);
                    }
                }
            }
        }

        return indices;
    }

    private void collectUsedVoiceIndicesFromObject(Object object, List<Integer> indices) {
        if (object == null) {
            return;
        }

        if (object instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice voice) {
            if (voice.getByteDataList() == null) {
                return;
            }
            for (byte[] bytes : voice.getByteDataList()) {
                if (bytes == null || bytes.length < 8) {
                    continue;
                }
                indices.add(readLittleEndianInt(bytes, 4));
            }
        }
    }

    private BatVoiceGrp.BatVoiceGroup findAkaoVoiceGroup(BatVoiceGrp batVoiceGrp, String codeName) {
        if (batVoiceGrp == null || batVoiceGrp.getVoiceList() == null) {
            return null;
        }

        for (BatVoiceGrp.BatVoiceGroup voiceGroup : batVoiceGrp.getVoiceList()) {
            if (voiceGroup == null || !isExisting(voiceGroup.getExistFlag())) {
                continue;
            }
            if (voiceGroup.getCharacterCodeName() != null
                    && voiceGroup.getCharacterCodeName().trim().equalsIgnoreCase(codeName)) {
                return voiceGroup;
            }
        }
        return null;
    }

    private Spm findSpm(Map<String, Spm> spmByFileName, String fileName) {
        for (Map.Entry<String, Spm> entry : spmByFileName.entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Waz findWazByFileName(Map<String, Waz> wazByFileName, String fileName) {
        if (wazByFileName == null || fileName == null) {
            return null;
        }
        for (Map.Entry<String, Waz> entry : wazByFileName.entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Path> buildPreferredExternalAudioIndex(Path root) throws IOException {
        Map<String, Path> index = new HashMap<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String ext = extension(path.getFileName().toString());
                if (!ext.equals(".ogg") && !ext.equals(".wav")) {
                    return;
                }

                String baseName = normalizeBaseName(path.getFileName().toString());
                Path existing = index.get(baseName);
                if (existing == null || preferAudio(path, existing)) {
                    index.put(baseName, path);
                }
            });
        }
        return index;
    }

    private boolean preferAudio(Path candidate, Path existing) {
        String candidateExt = extension(candidate.getFileName().toString());
        String existingExt = extension(existing.getFileName().toString());
        if (candidateExt.equals(existingExt)) {
            return false;
        }
        return candidateExt.equals(".ogg");
    }

    private Path resolveExternalFileCaseInsensitive(Path root, String fileName) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> normalize(path.getFileName().toString()).equals(normalize(fileName)))
                    .findFirst()
                    .orElse(null);
        }
    }

    private Path resolveFileCaseInsensitive(Path dir, String fileName) throws IOException {
        if (dir == null || fileName == null || !Files.exists(dir)) {
            return null;
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> normalize(path.getFileName().toString()).equals(normalize(fileName)))
                    .findFirst()
                    .orElse(null);
        }
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private String normalizeBaseName(String fileName) {
        String normalized = normalize(fileName);
        int dot = normalized.lastIndexOf('.');
        return dot >= 0 ? normalized.substring(0, dot) : normalized;
    }

    private String extension(String fileName) {
        String normalized = normalize(fileName);
        int dot = normalized.lastIndexOf('.');
        return dot >= 0 ? normalized.substring(dot) : "";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isExisting(Integer existFlag) {
        return existFlag == null || existFlag != 0;
    }
}
