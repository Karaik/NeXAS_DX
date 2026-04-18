package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output;

import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * BHE 公共弹幕资源簇的输出沉淀步骤。
 *
 * <p>selfRedirect/crossRedirect 负责把公共资源接入 {@code preparedBaseline} 并修正索引；
 * 本步骤只负责把这些已经自洽的公共资源落到最终输出目录。这样输出职责不会反向污染
 * 公共资源 plan，也不会把公共 WAZ/SPM/SE 混进单机体 selected closure。</p>
 */
public class OutputBheCommonProjectileResourcesStep {

    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService;

    public OutputBheCommonProjectileResourcesStep() {
        this(new BsdxBinService());
    }

    public OutputBheCommonProjectileResourcesStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public void output(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            BheCommonProjectileAppendPlan appendPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request == null || preparedBaseline == null || appendPlan == null || outputRoot == null || importedAssetSet == null) {
            return;
        }

        writeCommonWazFiles(preparedBaseline, appendPlan, outputRoot, importedAssetSet);
        writeCommonSpmFiles(request, preparedBaseline, appendPlan, outputRoot, importedAssetSet);
        copyCommonSpmImages(request, preparedBaseline, appendPlan, outputRoot, importedAssetSet);
        copyCommonSeAudio(request, appendPlan, outputRoot, importedAssetSet);
    }

    private void writeCommonWazFiles(
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            BheCommonProjectileAppendPlan appendPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : appendPlan.getCommonProjectileWazFiles()) {
            Waz waz = findWaz(preparedBaseline, fileName);
            if (waz == null) {
                importedAssetSet.getMissingAssets().add("缺少公共 WAZ 产物: " + fileName);
                continue;
            }
            waz.setExtensionName("waz");
            Path output = outputRoot.resolve(fileName);
            bsdxBinService.generate(output.toString(), waz, CHARSET);
            importedAssetSet.getGeneratedWazFiles().add(output);
            importedAssetSet.getWazFiles().add(fileName);
        }
    }

    private void writeCommonSpmFiles(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            BheCommonProjectileAppendPlan appendPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : appendPlan.getCommonProjectileSpmFiles()) {
            Spm spm = findSpm(preparedBaseline, fileName);
            if (spm == null) {
                importedAssetSet.getMissingAssets().add("缺少公共 SPM 产物: " + fileName);
                continue;
            }
            spm.setExtensionName("spm");

            Path output = outputRoot.resolve(fileName);
            bsdxBinService.generate(output.toString(), spm, CHARSET);
            // TsukuyomiImportedAssetSet 使用 copiedSpmFiles 统一记录 SPM 输出；菜单 SPM 也使用同一集合。
            importedAssetSet.getCopiedSpmFiles().add(output);
            importedAssetSet.getSpmFiles().add(fileName);
        }
    }

    private void copyCommonSpmImages(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            BheCommonProjectileAppendPlan appendPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        /*
         * 公共 SPM 文件会完整输出，但 PNG 不能按整份 imageData 盲目复制。
         * 真实 BHE 静态资源根缺少部分未用图；实际运行链由公共 WAZ 的 CEventSprite 决定，
         * 因此这里只复制公共 WAZ 可达动画真正引用到的图片。
         */
        Set<String> imageNames = collectCommonSpmImageNames(preparedBaseline, appendPlan);
        if (imageNames.isEmpty()) {
            return;
        }
        Path externalRoot = request.getExternalStaticAssetRoot();
        if (externalRoot == null || !Files.exists(externalRoot)) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在，无法复制公共 SPM 图片: " + externalRoot);
            return;
        }

        for (String targetImageName : imageNames) {
            String sourceImageName = stripBhePrefix(targetImageName);
            Path source = resolveExternalFileCaseInsensitive(externalRoot, sourceImageName);
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少公共 SPM 图片: " + targetImageName + " <- " + sourceImageName);
                continue;
            }
            Path output = outputRoot.resolve(targetImageName);
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedImageFiles().add(output);
        }
    }

    private Set<String> collectCommonSpmImageNames(
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        Set<String> imageNames = new LinkedHashSet<>();
        for (String fileName : appendPlan.getCommonProjectileWazFiles()) {
            Waz waz = findWaz(preparedBaseline, fileName);
            collectImageNamesFromWaz(preparedBaseline, waz, imageNames);
        }
        return imageNames;
    }

    private void collectImageNamesFromWaz(
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            Waz waz,
            Set<String> imageNames
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
                        collectImageNamesFromObject(preparedBaseline, object, imageNames);
                    }
                }
            }
        }
    }

    private void collectImageNamesFromObject(
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            SkillInfoObject object,
            Set<String> imageNames
    ) {
        if (object == null) {
            return;
        }
        if (object instanceof CEventSprite sprite) {
            collectImageNamesFromSprite(preparedBaseline, sprite, imageNames);
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
                        collectImageNamesFromObject(preparedBaseline, nested, imageNames);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("收集公共 SPM 图片引用失败: " + field.getName(), e);
            }
        }
    }

    private void collectImageNamesFromSprite(
            TsukuyomiBsdxBaselineBundle preparedBaseline,
            CEventSprite sprite,
            Set<String> imageNames
    ) {
        Integer spriteGroupIndex = sprite.getSpmFileSequence();
        if (spriteGroupIndex == null || spriteGroupIndex < 0
                || preparedBaseline.getSpriteGroupGrp() == null
                || preparedBaseline.getSpriteGroupGrp().getSpriteList() == null
                || spriteGroupIndex >= preparedBaseline.getSpriteGroupGrp().getSpriteList().size()) {
            return;
        }
        var spriteEntry = preparedBaseline.getSpriteGroupGrp().getSpriteList().get(spriteGroupIndex);
        if (spriteEntry == null || spriteEntry.getSpriteFileName() == null) {
            return;
        }
        Spm spm = findSpm(preparedBaseline, spriteEntry.getSpriteFileName());
        if (spm == null || spm.getAnimData() == null || spm.getPageData() == null || spm.getImageData() == null
                || sprite.getActionGroupNumber() == null || sprite.getActionGroupNumber() < 0
                || sprite.getActionGroupNumber() >= spm.getAnimData().size()) {
            return;
        }
        Spm.SPMAnimData animData = spm.getAnimData().get(sprite.getActionGroupNumber());
        if (animData == null || animData.getPatData() == null) {
            return;
        }
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
        if (pageNo == null || pageNo < 0 || pageNo >= spm.getPageData().size()) {
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
            if (imageData != null && imageData.getImageName() != null && !imageData.getImageName().isBlank()) {
                imageNames.add(imageData.getImageName().trim());
            }
        }
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
        List<Field> fields = new java.util.ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private void copyCommonSeAudio(
            TsukuyomiGraftRequest request,
            BheCommonProjectileAppendPlan appendPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (appendPlan.getSourceSePairToTargetFileName().isEmpty()) {
            return;
        }
        Path externalRoot = request.getExternalStaticAssetRoot();
        if (externalRoot == null || !Files.exists(externalRoot)) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在，无法复制公共 SE 音频: " + externalRoot);
            return;
        }

        Map<String, Path> audioIndex = buildPreferredExternalAudioIndex(externalRoot);
        Set<String> copied = new LinkedHashSet<>();
        for (String targetBaseName : appendPlan.getSourceSePairToTargetFileName().values()) {
            if (targetBaseName == null || targetBaseName.isBlank()) {
                continue;
            }
            if (!copied.add(normalizeBaseName(targetBaseName))) {
                continue;
            }
            String sourceBaseName = stripBhePrefix(targetBaseName);
            Path source = audioIndex.get(normalizeBaseName(sourceBaseName));
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少公共 SE 音频: " + targetBaseName + " <- " + sourceBaseName);
                continue;
            }
            Path output = outputRoot.resolve(targetBaseName + extension(source.getFileName().toString()));
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedAudioFiles().add(output);
        }
    }

    private Waz findWaz(TsukuyomiBsdxBaselineBundle preparedBaseline, String fileName) {
        if (preparedBaseline.getWazByFileName() == null || fileName == null) {
            return null;
        }
        for (Map.Entry<String, Waz> entry : preparedBaseline.getWazByFileName().entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Spm findSpm(TsukuyomiBsdxBaselineBundle preparedBaseline, String fileName) {
        if (preparedBaseline.getSpmByFileName() == null || fileName == null) {
            return null;
        }
        for (Map.Entry<String, Spm> entry : preparedBaseline.getSpmByFileName().entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
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

    private String stripBhePrefix(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.regionMatches(true, 0, "bhe_", 0, 4) ? trimmed.substring(4) : trimmed;
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
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
