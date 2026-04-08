package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 负责补选人菜单链。
 *
 * <p>当前明确采用“复用第 25 个可见槽位 + 复用 mekaIndex=32”的验证策略，
 * 所以这里不做尾插式菜单扩容，而是把原槽位的 dat/spm 资源整体重绑到 AKAO。</p>
 */
public class PatchMenuDataStep {

    private static final String CHARSET = "windows-31j";
    private static final int REPLACE_VISIBLE_SELECT_MENU_SLOT_INDEX = 24;
    private static final int SELECT_MENU_STATE_DONOR_ROW_INDEX = 23;

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    public void patchMenuData(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan,
            AkaoGraftResult result
    ) {
        if (request == null || !request.isPatchMenuData()) {
            return;
        }
        if (jinkiPackage == null || bsdxBaseline == null || grpAppendPlan == null || result == null) {
            return;
        }

        Dat patchedMekaDat = patchMekaDat(request, jinkiPackage, bsdxBaseline, grpAppendPlan);
        Dat patchedMekaPilotDat = patchMekaPilotDat(bsdxBaseline, grpAppendPlan);
        Dat patchedSelectMekaMenuDat = patchSelectMekaMenuDat(bsdxBaseline, grpAppendPlan);

        Mek menuMek = resolveMenuMek(result, jinkiPackage);
        Spm mekaPilotSourceSpm = loadRequiredExternalSpm(request, buildExternalUiSpmName("M_", request.getSpriteFileName()));
        Spm patchedMekaPilotSpm = patchMekaPilotSpm(bsdxBaseline, mekaPilotSourceSpm, menuMek);
        Spm patchedSelectMekaMenuMekaSpm = patchSelectMekaMenuMekaSpm(bsdxBaseline, menuMek);

        result.setPatchedMekaDat(patchedMekaDat);
        result.setPatchedMekaPilotDat(patchedMekaPilotDat);
        result.setPatchedSelectMekaMenuDat(patchedSelectMekaMenuDat);
        result.setPatchedMekaPilotSpm(patchedMekaPilotSpm);
        result.setPatchedSelectMekaMenuMekaSpm(patchedSelectMekaMenuMekaSpm);

        writePatchedMenuOutputs(
                request,
                result.getImportedAssetSet(),
                patchedMekaDat,
                patchedMekaPilotDat,
                patchedSelectMekaMenuDat,
                patchedMekaPilotSpm,
                patchedSelectMekaMenuMekaSpm,
                mekaPilotSourceSpm
        );
    }

    private Dat patchMekaDat(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        Dat baseline = bsdxBaseline.getMekaDat();
        if (baseline == null) {
            return null;
        }

        Dat patched = copyDat(baseline);

        Dat source = jinkiPackage.getMekaDat();
        if (source == null) {
            source = loadExternalOptionalDat(request == null ? null : request.getExternalStaticAssetRoot(), "Meka.dat");
        }
        if (source == null) {
            return patched;
        }

        // Meka.dat 的第一列不是 JINKI 当前包内 MekaGroup 的局部槽位号，
        // 而是更接近“全局机体编号”的字段。
        // 对 AKAO 来说，JINKI 的 MekaGroup 槽位是 27，但 Meka.dat 里真正对应的行是 [103, 200]，
        // 如果按 27 去取第 27 行，就会错误取到别的机体配置 [31, 0]。
        // 这里直接用 JINKI/BSDX 的 Meka.dat 做差，把源侧新增出来的那一行当成 AKAO 的真实配置行。
        List<Object> sourceRow = resolveSourceOnlyMekaDatRow(source, baseline);
        if (sourceRow == null) {
            return patched;
        }

        List<Object> targetRow = new ArrayList<>(sourceRow);
        if (!targetRow.isEmpty()) {
            targetRow.set(0, grpAppendPlan.getMekaGroupIndex());
        }
        appendOrReplaceRowByFirstColumn(patched, grpAppendPlan.getMekaGroupIndex(), targetRow);
        return patched;
    }

    /**
     * 从 JINKI 的 Meka.dat 中找出“BSDX 基线里不存在、但 JINKI 多出来”的那一行。
     *
     * 当前 AKAO 迁移里，JINKI 相比 BSDX 只多一台机体：
     * - BSDX 末行: [102, 200]
     * - JINKI 末行: [103, 200]
     *
     * 所以这里应该返回 [103, 200]，而不能按 JINKI MekaGroup 的局部索引 27 去取第 27 行。
     */
    private List<Object> resolveSourceOnlyMekaDatRow(Dat source, Dat baseline) {
        if (source == null || source.getData() == null) {
            return null;
        }

        Set<Integer> baselineIds = new LinkedHashSet<>();
        if (baseline != null && baseline.getData() != null) {
            for (List<Object> row : baseline.getData()) {
                Integer id = readFirstColumnAsInt(row);
                if (id != null) {
                    baselineIds.add(id);
                }
            }
        }

        List<List<Object>> sourceOnlyRows = new ArrayList<>();
        for (List<Object> row : source.getData()) {
            Integer id = readFirstColumnAsInt(row);
            if (id == null) {
                continue;
            }
            if (!baselineIds.contains(id)) {
                sourceOnlyRows.add(row);
            }
        }

        if (sourceOnlyRows.size() == 1) {
            return sourceOnlyRows.get(0);
        }

        // 兜底：如果未来源包里出现多行新增，这里先优先拿编号最大的那一行，
        // 至少保持“追加新机体时优先吃源侧新增编号”这条策略。
        List<Object> bestRow = null;
        Integer bestId = null;
        for (List<Object> row : sourceOnlyRows) {
            Integer id = readFirstColumnAsInt(row);
            if (id != null && (bestId == null || id > bestId)) {
                bestId = id;
                bestRow = row;
            }
        }
        return bestRow;
    }

    private Dat patchMekaPilotDat(
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        Dat baseline = bsdxBaseline.getMekaPilotDat();
        if (baseline == null) {
            return null;
        }

        Dat patched = copyDat(baseline);
        ReplacementSlot slot = resolveReplacementSlot(bsdxBaseline);

        if (slot.pilotRowIndex >= 0 && slot.pilotRowIndex < safeSize(patched.getData())) {
            List<Object> row = new ArrayList<>(patched.getData().get(slot.pilotRowIndex));
            if (!row.isEmpty()) {
                row.set(0, grpAppendPlan.getMekaGroupIndex());
                patched.getData().set(slot.pilotRowIndex, row);
            }
        } else {
            appendSingleColumnIndexIfAbsent(patched, grpAppendPlan.getMekaGroupIndex());
        }
        return patched;
    }

    private Dat patchSelectMekaMenuDat(
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        Dat baseline = bsdxBaseline.getSelectMekaMenuDat();
        if (baseline == null) {
            return null;
        }

        Dat patched = copyDat(baseline);
        ReplacementSlot slot = resolveReplacementSlot(bsdxBaseline);

        if (slot.selectMenuRowIndex >= 0 && slot.selectMenuRowIndex < safeSize(patched.getData())) {
            List<Object> row = new ArrayList<>(patched.getData().get(slot.selectMenuRowIndex));
            row.set(0, grpAppendPlan.getMekaGroupIndex());
            if (row.size() > 1) {
                row.set(1, slot.selectMenuAnimIndex);
            }
            if (row.size() > 2) {
                row.set(2, slot.selectMenuState);
            }
            patched.getData().set(slot.selectMenuRowIndex, row);
        }
        return patched;
    }

    private Spm patchMekaPilotSpm(
            BsdxBaselineBundle bsdxBaseline,
            Spm sourceSpm,
            Mek menuMek
    ) {
        if (bsdxBaseline.getMekaPilotSpm() == null) {
            return null;
        }

        ReplacementSlot slot = resolveReplacementSlot(bsdxBaseline);
        Spm target = copySpm(bsdxBaseline.getMekaPilotSpm());
        replaceTargetUiSpmAnim(target, slot.pilotAnimIndex, sourceSpm, buildPilotAnimName(menuMek), true);
        return target;
    }

    private Spm patchSelectMekaMenuMekaSpm(
            BsdxBaselineBundle bsdxBaseline,
            Mek menuMek
    ) {
        if (bsdxBaseline.getSelectMekaMenuMekaSpm() == null) {
            return null;
        }

        ReplacementSlot slot = resolveReplacementSlot(bsdxBaseline);
        Spm target = copySpm(bsdxBaseline.getSelectMekaMenuMekaSpm());
        updateTargetUiSpmAnimName(target, slot.selectMenuAnimIndex, buildStableSelectMenuAnimName(menuMek));
        return target;
    }

    /**
     * `SelectMekaMenuMeka.spm` 这里保留 BSDX 目标槽位原本的 page/chip/imageName 结构。
     * 菜单链只改对应槽位的 animName，不再把 `G_moribito_2.spm` 的图片名覆盖进来。
     */
    private void updateTargetUiSpmAnimName(
            Spm target,
            int targetAnimIndex,
            String animName
    ) {
        if (target == null || target.getAnimData() == null) {
            return;
        }
        if (targetAnimIndex < 0 || targetAnimIndex >= target.getAnimData().size()) {
            return;
        }

        Spm.SPMAnimData targetAnim = target.getAnimData().get(targetAnimIndex);
        if (targetAnim == null) {
            return;
        }

        targetAnim.setAnimName(animName);
        targetAnim.setNumPat(targetAnim.getPatData() == null ? 0 : targetAnim.getPatData().size());
    }

    private void replaceTargetUiSpmAnim(
            Spm target,
            int targetAnimIndex,
            Spm source,
            String animName,
            boolean duplicateSinglePageWhenNeeded
    ) {
        if (target == null || source == null || targetAnimIndex < 0 || targetAnimIndex >= safeSize(target.getAnimData())) {
            return;
        }

        Spm.SPMAnimData targetAnim = target.getAnimData().get(targetAnimIndex);
        List<Integer> targetPageIndices = collectTargetPageIndices(targetAnim);
        if (targetPageIndices.isEmpty()) {
            return;
        }

        List<Spm.SPMPageData> sourcePages = source.getPageData() == null ? new ArrayList<>() : source.getPageData();
        if (sourcePages.isEmpty()) {
            return;
        }

        List<Integer> targetImageIndices = collectTargetImageIndices(target, targetPageIndices);
        if (targetImageIndices.isEmpty()) {
            return;
        }

        Map<Integer, Integer> sourceImageIndexToTargetImageIndex = buildImageReplacementMap(source, targetImageIndices);
        applyImageNameReplacement(target, source, sourceImageIndexToTargetImageIndex);

        List<Integer> pageSequence = collectSourcePageSequence(source);
        if (pageSequence.isEmpty()) {
            pageSequence.add(0);
        }
        if (duplicateSinglePageWhenNeeded && pageSequence.size() == 1) {
            pageSequence.add(pageSequence.get(0));
        }

        for (int i = 0; i < targetPageIndices.size(); i++) {
            Integer targetPageIndex = targetPageIndices.get(i);
            Integer sourcePageIndex = pageSequence.get(i % pageSequence.size());
            if (sourcePageIndex == null || sourcePageIndex < 0 || sourcePageIndex >= safeSize(source.getPageData())) {
                continue;
            }
            target.getPageData().set(
                    targetPageIndex,
                    copyPageData(source.getPageData().get(sourcePageIndex), sourceImageIndexToTargetImageIndex)
            );
        }

        targetAnim.setAnimName(animName);
        targetAnim.setNumPat(targetAnim.getPatData() == null ? 0 : targetAnim.getPatData().size());
    }

    private List<Integer> collectSourcePageSequence(Spm source) {
        List<Integer> pageSequence = new ArrayList<>();
        if (source == null || source.getAnimData() == null) {
            return pageSequence;
        }

        for (Spm.SPMAnimData animData : source.getAnimData()) {
            if (animData == null || animData.getPatData() == null) {
                continue;
            }
            for (Spm.SPMPatData patData : animData.getPatData()) {
                if (patData == null || patData.getPageNo() == null) {
                    continue;
                }
                for (Integer sourcePageIndex : patData.getPageNo()) {
                    if (sourcePageIndex != null) {
                        pageSequence.add(sourcePageIndex);
                    }
                }
            }
        }
        return pageSequence;
    }

    private List<Integer> collectTargetPageIndices(Spm.SPMAnimData animData) {
        List<Integer> pageIndices = new ArrayList<>();
        if (animData == null || animData.getPatData() == null) {
            return pageIndices;
        }

        for (Spm.SPMPatData patData : animData.getPatData()) {
            if (patData == null || patData.getPageNo() == null) {
                continue;
            }
            for (Integer pageIndex : patData.getPageNo()) {
                if (pageIndex != null) {
                    pageIndices.add(pageIndex);
                }
            }
        }
        return pageIndices;
    }

    private List<Integer> collectTargetImageIndices(Spm target, List<Integer> pageIndices) {
        List<Integer> imageIndices = new ArrayList<>();
        if (target == null || target.getPageData() == null) {
            return imageIndices;
        }

        for (Integer pageIndex : pageIndices) {
            if (pageIndex == null || pageIndex < 0 || pageIndex >= safeSize(target.getPageData())) {
                continue;
            }
            Spm.SPMPageData pageData = target.getPageData().get(pageIndex);
            if (pageData == null || pageData.getChipData() == null) {
                continue;
            }
            for (Spm.SPMChipData chipData : pageData.getChipData()) {
                if (chipData != null && chipData.getImageNo() != null && !imageIndices.contains(chipData.getImageNo())) {
                    imageIndices.add(chipData.getImageNo());
                }
            }
        }
        return imageIndices;
    }

    private Map<Integer, Integer> buildImageReplacementMap(Spm source, List<Integer> targetImageIndices) {
        Map<Integer, Integer> imageMap = new LinkedHashMap<>();
        if (source == null || source.getImageData() == null) {
            return imageMap;
        }

        for (int i = 0; i < source.getImageData().size(); i++) {
            int targetIndex = targetImageIndices.get(Math.min(i, targetImageIndices.size() - 1));
            imageMap.put(i, targetIndex);
        }
        return imageMap;
    }

    private void applyImageNameReplacement(
            Spm target,
            Spm source,
            Map<Integer, Integer> sourceImageIndexToTargetImageIndex
    ) {
        if (target == null || target.getImageData() == null || source == null || source.getImageData() == null) {
            return;
        }

        for (Map.Entry<Integer, Integer> entry : sourceImageIndexToTargetImageIndex.entrySet()) {
            int sourceImageIndex = entry.getKey();
            int targetImageIndex = entry.getValue();
            if (sourceImageIndex < 0 || sourceImageIndex >= safeSize(source.getImageData())
                    || targetImageIndex < 0 || targetImageIndex >= safeSize(target.getImageData())) {
                continue;
            }
            target.getImageData().get(targetImageIndex).setImageName(source.getImageData().get(sourceImageIndex).getImageName());
        }
    }

    private void writePatchedMenuOutputs(
            AkaoGraftRequest request,
            ImportedAssetSet importedAssetSet,
            Dat patchedMekaDat,
            Dat patchedMekaPilotDat,
            Dat patchedSelectMekaMenuDat,
            Spm patchedMekaPilotSpm,
            Spm patchedSelectMekaMenuMekaSpm,
            Spm mekaPilotSourceSpm
    ) {
        if (importedAssetSet == null || importedAssetSet.getOutputRootDir() == null) {
            return;
        }

        try {
            writeDat(importedAssetSet, "Meka.dat", patchedMekaDat);
            writeDat(importedAssetSet, "MekaPilot.dat", patchedMekaPilotDat);
            writeDat(importedAssetSet, "SelectMekaMenu.dat", patchedSelectMekaMenuDat);

            writeSpm(importedAssetSet, "MekaPilot.spm", patchedMekaPilotSpm);
            writeSpm(importedAssetSet, "SelectMekaMenuMeka.spm", patchedSelectMekaMenuMekaSpm);

            copyMenuSpmImages(request, importedAssetSet, patchedMekaPilotSpm, patchedSelectMekaMenuMekaSpm, mekaPilotSourceSpm);
        } catch (IOException e) {
            throw new IllegalStateException("写出 step9 菜单链产物失败", e);
        }
    }

    private void writeDat(ImportedAssetSet importedAssetSet, String fileName, Dat dat) throws IOException {
        if (dat == null) {
            return;
        }
        Path output = importedAssetSet.getOutputRootDir().resolve(fileName);
        bsdxBinService.generate(output.toString(), dat, CHARSET);
        importedAssetSet.getGeneratedDatFiles().add(output);
    }

    private void writeSpm(ImportedAssetSet importedAssetSet, String fileName, Spm spm) throws IOException {
        if (spm == null) {
            return;
        }
        Path output = importedAssetSet.getOutputRootDir().resolve(fileName);
        bsdxBinService.generate(output.toString(), spm, CHARSET);
        importedAssetSet.getCopiedSpmFiles().add(output);
    }

    private void copyMenuSpmImages(
            AkaoGraftRequest request,
            ImportedAssetSet importedAssetSet,
            Spm... spms
    ) throws IOException {
        if (request == null || request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            return;
        }

        Set<String> imageNames = new LinkedHashSet<>();
        for (Spm spm : spms) {
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

        for (String imageName : imageNames) {
            Path source = request.getExternalStaticAssetRoot().resolve(imageName);
            if (!Files.exists(source)) {
                continue;
            }
            Path output = importedAssetSet.getOutputRootDir().resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedImageFiles().add(output);
        }
    }

    private Dat loadExternalOptionalDat(Path root, String fileName) {
        if (root == null || !Files.exists(root)) {
            return null;
        }

        Path path = root.resolve(fileName);
        if (!Files.exists(path)) {
            return null;
        }

        try {
            ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
            return (Dat) dto.getData();
        } catch (IOException e) {
            throw new IllegalStateException("解析外部 dat 失败: " + path, e);
        }
    }

    private Spm loadRequiredExternalSpm(AkaoGraftRequest request, String fileName) {
        if (request == null || request.getExternalStaticAssetRoot() == null) {
            throw new IllegalStateException("外部静态资源目录未配置，无法加载菜单 spm: " + fileName);
        }

        Path path = request.getExternalStaticAssetRoot().resolve(fileName);
        if (!Files.exists(path)) {
            throw new IllegalStateException("缺少菜单 spm 源文件: " + path);
        }

        try {
            ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
            return (Spm) dto.getData();
        } catch (IOException e) {
            throw new IllegalStateException("解析菜单 spm 源文件失败: " + path, e);
        }
    }

    private Mek resolveMenuMek(AkaoGraftResult result, JinkiPackageBundle jinkiPackage) {
        if (result != null && result.getReboundAkaoMek() != null) {
            return result.getReboundAkaoMek();
        }
        return jinkiPackage == null ? null : jinkiPackage.getAkaoMek();
    }

    private String buildExternalUiSpmName(String prefix, String spriteFileName) {
        if (spriteFileName == null || spriteFileName.isBlank()) {
            throw new IllegalStateException("spriteFileName 不能为空，无法推导外部菜单 spm 文件名");
        }
        String base = spriteFileName;
        if (base.toLowerCase(Locale.ROOT).endsWith(".spm")) {
            base = base.substring(0, base.length() - 4);
        }
        return prefix + base + ".spm";
    }

    private String buildPilotAnimName(Mek meka) {
        if (meka == null || meka.getMekBasicInfo() == null) {
            return "";
        }
        return safeTrim(meka.getMekBasicInfo().getPilotNameKanji());
    }

    private String buildSelectMenuAnimName(Mek meka) {
        if (meka == null || meka.getMekBasicInfo() == null) {
            return "";
        }

        String pilot = safeTrim(meka.getMekBasicInfo().getPilotNameKanji());
        String mekaName = safeTrim(meka.getMekBasicInfo().getMekName());
        String pilotRoma = safeTrim(meka.getMekBasicInfo().getPilotNameRoma());
        String mekaRoma = safeTrim(meka.getMekBasicInfo().getMekNameEnglish());

        StringBuilder builder = new StringBuilder();
        if (!pilot.isEmpty()) {
            builder.append(pilot);
        }
        if (!mekaName.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("/");
            }
            builder.append(mekaName);
        }

        String roma = joinNonEmpty(pilotRoma, mekaRoma);
        if (!roma.isEmpty()) {
            builder.append("：").append(roma);
        }
        builder.append("\r");
        return builder.toString();
    }

    /**
     * 菜单机体图当前只需要稳定地把 animName 改成 AKAO 的文本。
     * 这里不再复用旧的 G_moribito_2.spm 命名格式，避免把错误的图片名链带进来。
     */
    private String buildStableSelectMenuAnimName(Mek meka) {
        if (meka == null || meka.getMekBasicInfo() == null) {
            return "";
        }

        String pilot = safeTrim(meka.getMekBasicInfo().getPilotNameKanji());
        String mekaName = safeTrim(meka.getMekBasicInfo().getMekName());
        String pilotRoma = safeTrim(meka.getMekBasicInfo().getPilotNameRoma());
        String mekaRoma = safeTrim(meka.getMekBasicInfo().getMekNameEnglish());

        StringBuilder builder = new StringBuilder();
        if (!pilot.isEmpty()) {
            builder.append(pilot);
        }
        if (!mekaName.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("/");
            }
            builder.append(mekaName);
        }

        String roma = joinNonEmpty(pilotRoma, mekaRoma);
        if (!roma.isEmpty()) {
            builder.append("：").append(roma);
        }
        return builder.toString();
    }

    private String joinNonEmpty(String left, String right) {
        if (left == null || left.isBlank()) {
            return right == null ? "" : right;
        }
        if (right == null || right.isBlank()) {
            return left;
        }
        return left + "/" + right;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private Dat copyDat(Dat source) {
        Dat target = new Dat();
        target.setFileName(source.getFileName());
        target.setExtensionName(source.getExtensionName());
        target.setColumnCount(source.getColumnCount());
        target.setColumnTypes(source.getColumnTypes() == null ? new ArrayList<>() : new ArrayList<>(source.getColumnTypes()));

        List<List<Object>> rows = new ArrayList<>();
        if (source.getData() != null) {
            for (List<Object> row : source.getData()) {
                rows.add(row == null ? null : new ArrayList<>(row));
            }
        }
        target.setData(rows);
        return target;
    }

    private Spm copySpm(Spm source) {
        Spm target = new Spm();
        target.setExtensionName(source.getExtensionName());
        target.setSpmVersion(source.getSpmVersion());
        target.setPatPageNum(source.getPatPageNum());

        List<Spm.SPMImageData> imageData = new ArrayList<>();
        if (source.getImageData() != null) {
            for (Spm.SPMImageData image : source.getImageData()) {
                imageData.add(copyImageData(image));
            }
        }
        target.setImageData(imageData);
        target.setNumImageData(imageData.size());

        List<Spm.SPMPageData> pageData = new ArrayList<>();
        if (source.getPageData() != null) {
            for (Spm.SPMPageData page : source.getPageData()) {
                pageData.add(copyPageData(page, null));
            }
        }
        target.setPageData(pageData);
        target.setNumPageData(pageData.size());

        List<Spm.SPMAnimData> animData = new ArrayList<>();
        if (source.getAnimData() != null) {
            for (Spm.SPMAnimData anim : source.getAnimData()) {
                animData.add(copyAnimData(anim));
            }
        }
        target.setAnimData(animData);
        target.setNumAnimData(animData.size());
        return target;
    }

    private Spm.SPMImageData copyImageData(Spm.SPMImageData source) {
        Spm.SPMImageData target = new Spm.SPMImageData();
        if (source != null) {
            target.setImageName(source.getImageName());
        }
        return target;
    }

    private Spm.SPMPageData copyPageData(Spm.SPMPageData source, Map<Integer, Integer> imageIndexMap) {
        Spm.SPMPageData target = new Spm.SPMPageData();
        if (source == null) {
            return target;
        }

        target.setNumChipData(source.getNumChipData());
        target.setPageWidth(source.getPageWidth());
        target.setPageHeight(source.getPageHeight());
        target.setPageRect(copyRect(source.getPageRect()));
        target.setPageOption(source.getPageOption());
        target.setRotateCenterX(source.getRotateCenterX());
        target.setRotateCenterY(source.getRotateCenterY());
        target.setHitFlag(source.getHitFlag());
        target.setUnk3(source.getUnk3());

        List<Spm.SPMHitArea> hitRects = new ArrayList<>();
        if (source.getHitRects() != null) {
            for (Spm.SPMHitArea hitArea : source.getHitRects()) {
                hitRects.add(copyHitArea(hitArea));
            }
        }
        target.setHitRects(hitRects);

        List<Spm.SPMChipData> chipData = new ArrayList<>();
        if (source.getChipData() != null) {
            for (Spm.SPMChipData chip : source.getChipData()) {
                chipData.add(copyChipData(chip, imageIndexMap));
            }
        }
        target.setChipData(chipData);
        target.setNumChipData(chipData.size());
        return target;
    }

    private Spm.SPMChipData copyChipData(Spm.SPMChipData source, Map<Integer, Integer> imageIndexMap) {
        Spm.SPMChipData target = new Spm.SPMChipData();
        if (source == null) {
            return target;
        }

        Integer targetImageNo = source.getImageNo();
        if (targetImageNo != null && imageIndexMap != null && imageIndexMap.containsKey(targetImageNo)) {
            targetImageNo = imageIndexMap.get(targetImageNo);
        }
        target.setImageNo(targetImageNo);
        target.setDstRect(copyRect(source.getDstRect()));
        target.setChipWidth(source.getChipWidth());
        target.setChipHeight(source.getChipHeight());
        target.setSrcRect(copyRect(source.getSrcRect()));
        target.setDrawOption(source.getDrawOption());
        target.setUnk5(source.getUnk5());
        target.setDrawOptionValue(source.getDrawOptionValue());
        target.setOption(source.getOption());
        return target;
    }

    private Spm.SPMAnimData copyAnimData(Spm.SPMAnimData source) {
        Spm.SPMAnimData target = new Spm.SPMAnimData();
        if (source == null) {
            return target;
        }

        target.setAnimName(source.getAnimName());
        target.setNumPat(source.getNumPat());
        target.setAnimRotateDirection(source.getAnimRotateDirection());
        target.setAnimReverseDirection(source.getAnimReverseDirection());

        List<Spm.SPMPatData> patData = new ArrayList<>();
        if (source.getPatData() != null) {
            for (Spm.SPMPatData pat : source.getPatData()) {
                Spm.SPMPatData copiedPat = new Spm.SPMPatData();
                copiedPat.setWaitFrame(pat == null ? null : pat.getWaitFrame());
                copiedPat.setPageNo(pat == null || pat.getPageNo() == null ? new ArrayList<>() : new ArrayList<>(pat.getPageNo()));
                patData.add(copiedPat);
            }
        }
        target.setPatData(patData);
        target.setNumPat(patData.size());
        return target;
    }

    private Spm.SPMRect copyRect(Spm.SPMRect source) {
        Spm.SPMRect target = new Spm.SPMRect();
        if (source == null) {
            return target;
        }
        target.setLeft(source.getLeft());
        target.setTop(source.getTop());
        target.setRight(source.getRight());
        target.setBottom(source.getBottom());
        return target;
    }

    private Spm.SPMHitArea copyHitArea(Spm.SPMHitArea source) {
        Spm.SPMHitArea target = new Spm.SPMHitArea();
        if (source == null) {
            return target;
        }
        target.setUnk0(source.getUnk0());
        target.setHitRect(copyRect(source.getHitRect()));
        target.setUnk1(source.getUnk1());
        target.setUnk2(source.getUnk2());
        return target;
    }

    private Integer findMekaIndexByCode(MekaGroupGrp group, String codeName) {
        if (group == null || group.getMekaList() == null || codeName == null) {
            return null;
        }
        for (int i = 0; i < group.getMekaList().size(); i++) {
            MekaGroupGrp.MekaGroup entry = group.getMekaList().get(i);
            if (entry != null
                    && (entry.getExistFlag() == null || entry.getExistFlag() != 0)
                    && codeName.equalsIgnoreCase(entry.getMekaCodeName())) {
                return i;
            }
        }
        return null;
    }

    /**
     * 按行索引直接取行，用于 Meka.dat 等行顺序与 Grp 索引一致的场景。
     */
    private List<Object> getRowByIndex(Dat dat, int index) {
        if (dat == null || dat.getData() == null) {
            return null;
        }
        if (index < 0 || index >= dat.getData().size()) {
            return null;
        }
        return dat.getData().get(index);
    }

    private List<Object> findFirstRowByFirstColumn(Dat dat, Integer index) {
        if (dat == null || dat.getData() == null || index == null) {
            return null;
        }
        for (List<Object> row : dat.getData()) {
            Integer value = readFirstColumnAsInt(row);
            if (value != null && value.equals(index)) {
                return row;
            }
        }
        return null;
    }

    private Integer readFirstColumnAsInt(List<Object> row) {
        if (row == null || row.isEmpty() || row.get(0) == null) {
            return null;
        }
        return toInt(row.get(0));
    }

    private Integer toInt(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue.intValue();
        }
        if (value instanceof Short shortValue) {
            return shortValue.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void appendSingleColumnIndexIfAbsent(Dat dat, int index) {
        if (findFirstRowByFirstColumn(dat, index) != null) {
            return;
        }
        List<Object> row = new ArrayList<>();
        row.add(index);
        dat.getData().add(row);
    }

    private void appendOrReplaceRowByFirstColumn(Dat dat, int index, List<Object> targetRow) {
        List<List<Object>> data = dat.getData();
        if (data == null) {
            data = new ArrayList<>();
            dat.setData(data);
        }

        for (int i = 0; i < data.size(); i++) {
            Integer value = readFirstColumnAsInt(data.get(i));
            if (value != null && value == index) {
                data.set(i, targetRow);
                return;
            }
        }

        data.add(targetRow);
    }

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private ReplacementSlot resolveReplacementSlot(BsdxBaselineBundle bsdxBaseline) {
        if (bsdxBaseline == null || bsdxBaseline.getSelectMekaMenuDat() == null || bsdxBaseline.getSelectMekaMenuDat().getData() == null) {
            throw new IllegalStateException("缺少 SelectMekaMenu.dat 基线，无法定位替换槽");
        }

        List<List<Object>> selectRows = bsdxBaseline.getSelectMekaMenuDat().getData();
        if (REPLACE_VISIBLE_SELECT_MENU_SLOT_INDEX < 0 || REPLACE_VISIBLE_SELECT_MENU_SLOT_INDEX >= selectRows.size()) {
            throw new IllegalStateException("替换槽位越界: " + REPLACE_VISIBLE_SELECT_MENU_SLOT_INDEX);
        }

        List<Object> selectRow = selectRows.get(REPLACE_VISIBLE_SELECT_MENU_SLOT_INDEX);
        int sourceMekaIndex = toInt(selectRow.get(0));
        int selectAnimIndex = toInt(selectRow.get(1));
        int selectState = resolveSelectMenuState(selectRows, selectRow);

        int pilotRowIndex = findPilotRowIndex(bsdxBaseline.getMekaPilotDat(), sourceMekaIndex);
        if (pilotRowIndex < 0) {
            throw new IllegalStateException("MekaPilot.dat 中找不到 SelectMekaMenu 槽位对应的 pilot 行: " + sourceMekaIndex);
        }

        ReplacementSlot slot = new ReplacementSlot();
        slot.selectMenuRowIndex = REPLACE_VISIBLE_SELECT_MENU_SLOT_INDEX;
        slot.sourceMekaIndex = sourceMekaIndex;
        slot.selectMenuAnimIndex = selectAnimIndex;
        slot.selectMenuState = selectState;
        slot.pilotRowIndex = pilotRowIndex;
        slot.pilotAnimIndex = pilotRowIndex;
        return slot;
    }

    private int resolveSelectMenuState(List<List<Object>> selectRows, List<Object> replacementRow) {
        if (selectRows != null
                && SELECT_MENU_STATE_DONOR_ROW_INDEX >= 0
                && SELECT_MENU_STATE_DONOR_ROW_INDEX < selectRows.size()) {
            List<Object> donorRow = selectRows.get(SELECT_MENU_STATE_DONOR_ROW_INDEX);
            if (donorRow != null && donorRow.size() > 2) {
                return toInt(donorRow.get(2));
            }
        }
        return replacementRow != null && replacementRow.size() > 2 ? toInt(replacementRow.get(2)) : 108;
    }

    private int findPilotRowIndex(Dat mekaPilotDat, int mekaIndex) {
        if (mekaPilotDat == null || mekaPilotDat.getData() == null) {
            return -1;
        }
        for (int i = 0; i < mekaPilotDat.getData().size(); i++) {
            List<Object> row = mekaPilotDat.getData().get(i);
            if (row != null && !row.isEmpty() && toInt(row.get(0)) == mekaIndex) {
                return i;
            }
        }
        return -1;
    }

    private static class ReplacementSlot {
        private int selectMenuRowIndex;
        private int sourceMekaIndex;
        private int selectMenuAnimIndex;
        private int selectMenuState;
        private int pilotRowIndex;
        private int pilotAnimIndex;
    }
}
