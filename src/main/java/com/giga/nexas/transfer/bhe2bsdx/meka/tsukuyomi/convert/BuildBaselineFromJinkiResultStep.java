package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BuildBaselineFromJinkiResultStep {

    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    /**
     * 把 JINKI 层结果整理成 BHE Tsukuyomi 层可以直接消费的基线视图。
     *
     * <p>这一步不重新跑 JINKI，也不重新解释 JINKI 的业务；它只负责把 JINKI 已经产出的
     * patched DAT/SPM、重绑后的 AKAO.mek / AKAO.waz、扩容后的 group 结构交给 BHE 继续使用。</p>
     */
    public TsukuyomiBsdxBaselineBundle buildBaselineFromJinkiResult(AkaoGraftResult inheritedJinkiResult) {
        TsukuyomiBsdxBaselineBundle inheritedBaseline = new TsukuyomiBsdxBaselineBundle();
        if (inheritedJinkiResult == null || inheritedJinkiResult.getBsdxBaseline() == null) {
            return inheritedBaseline;
        }

        var jinkiBaseline = inheritedJinkiResult.getBsdxBaseline();

        inheritedBaseline.setBatVoiceGrp(jinkiBaseline.getBatVoiceGrp());
        inheritedBaseline.setMapGroupGrp(jinkiBaseline.getMapGroupGrp());
        inheritedBaseline.setMekaGroupGrp(jinkiBaseline.getMekaGroupGrp());
        inheritedBaseline.setSeGroupGrp(jinkiBaseline.getSeGroupGrp());
        inheritedBaseline.setSpriteGroupGrp(jinkiBaseline.getSpriteGroupGrp());
        inheritedBaseline.setWazaGroupGrp(jinkiBaseline.getWazaGroupGrp());

        inheritedBaseline.setProgramMaterialGrp(
                inheritedJinkiResult.getSyncedProgramMaterial() != null
                        ? inheritedJinkiResult.getSyncedProgramMaterial()
                        : jinkiBaseline.getProgramMaterialGrp()
        );

        inheritedBaseline.setMekaDat(
                inheritedJinkiResult.getPatchedMekaDat() != null
                        ? inheritedJinkiResult.getPatchedMekaDat()
                        : jinkiBaseline.getMekaDat()
        );
        inheritedBaseline.setMekaPilotDat(
                inheritedJinkiResult.getPatchedMekaPilotDat() != null
                        ? inheritedJinkiResult.getPatchedMekaPilotDat()
                        : jinkiBaseline.getMekaPilotDat()
        );
        inheritedBaseline.setSelectMekaMenuDat(
                inheritedJinkiResult.getPatchedSelectMekaMenuDat() != null
                        ? inheritedJinkiResult.getPatchedSelectMekaMenuDat()
                        : jinkiBaseline.getSelectMekaMenuDat()
        );

        inheritedBaseline.setWeaponEquipDat(resolveInheritedWeaponEquipDat(inheritedJinkiResult));

        inheritedBaseline.getMekByFileName().putAll(jinkiBaseline.getMekByFileName());
        inheritedBaseline.getSpmByFileName().putAll(jinkiBaseline.getSpmByFileName());
        inheritedBaseline.getWazByFileName().putAll(jinkiBaseline.getWazByFileName());

        inheritJinkiSidecarBinaryAssets(inheritedJinkiResult, inheritedBaseline);

        if (inheritedJinkiResult.getReboundAkaoMek() != null) {
            inheritedBaseline.getMekByFileName().entrySet().removeIf(entry ->
                    entry.getValue() == inheritedJinkiResult.getReboundAkaoMek()
                            || "akao".equalsIgnoreCase(entry.getKey())
            );
            inheritedBaseline.getMekByFileName().put(
                    resolveInheritedAkaoMekFileName(inheritedJinkiResult, inheritedBaseline),
                    inheritedJinkiResult.getReboundAkaoMek()
            );
        }
        if (inheritedJinkiResult.getReboundAkaoWaz() != null) {
            inheritedBaseline.getWazByFileName().entrySet().removeIf(entry ->
                    entry.getValue() == inheritedJinkiResult.getReboundAkaoWaz()
                            || "akao".equalsIgnoreCase(entry.getKey())
            );
            inheritedBaseline.getWazByFileName().put(
                    resolveInheritedAkaoWazFileName(inheritedJinkiResult, inheritedBaseline),
                    inheritedJinkiResult.getReboundAkaoWaz()
            );
        }

        inheritedBaseline.setMekaPilotSpm(
                inheritedJinkiResult.getPatchedMekaPilotSpm() != null
                        ? inheritedJinkiResult.getPatchedMekaPilotSpm()
                        : jinkiBaseline.getMekaPilotSpm()
        );
        inheritedBaseline.setSelectMekaMenuMekaSpm(
                inheritedJinkiResult.getPatchedSelectMekaMenuMekaSpm() != null
                        ? inheritedJinkiResult.getPatchedSelectMekaMenuMekaSpm()
                        : jinkiBaseline.getSelectMekaMenuMekaSpm()
        );

        if (inheritedBaseline.getMekaPilotSpm() != null) {
            inheritedBaseline.getSpmByFileName().put("MekaPilot.spm", inheritedBaseline.getMekaPilotSpm());
        }
        if (inheritedBaseline.getSelectMekaMenuMekaSpm() != null) {
            inheritedBaseline.getSpmByFileName().put("SelectMekaMenuMeka.spm", inheritedBaseline.getSelectMekaMenuMekaSpm());
        }

        return inheritedBaseline;
    }

    private void inheritJinkiSidecarBinaryAssets(
            AkaoGraftResult inheritedJinkiResult,
            TsukuyomiBsdxBaselineBundle inheritedBaseline
    ) {
        if (inheritedJinkiResult.getImportedAssetSet() == null
                || inheritedJinkiResult.getImportedAssetSet().getOutputRootDir() == null) {
            return;
        }
        Path outputRoot = inheritedJinkiResult.getImportedAssetSet().getOutputRootDir();
        if (!Files.isDirectory(outputRoot)) {
            return;
        }

        /*
         * JINKI 层已经会把同名公共 WAZ 做 key-based merge 后落盘。
         * BHE 公共弹幕会继续往 Effect/Tama/Laser/Bomb 这些宿主 WAZ 里追加技能，
         * 因此这里必须以 JINKI sidecar 目录里的二进制成果物覆盖内存基线。
         * 否则 BHE 层会拿原始 BSDX WAZ 当宿主，导致 JINKI 公共技能被回退。
         */
        inheritSidecarFiles(outputRoot, "*.waz", Waz.class, inheritedBaseline.getWazByFileName());
        /*
         * SPM 和 MEK 也按同一条链式规则继承：
         * SPM 可能被 JINKI 菜单或公共资源写回，MEK 尾部 material 容量也会随 GRP 扩容补齐。
         * 这里不重新解释 JINKI 业务，只把上一层真实落盘的二进制状态接成当前层基线。
         */
        inheritSidecarFiles(outputRoot, "*.spm", Spm.class, inheritedBaseline.getSpmByFileName());
        inheritSidecarFiles(outputRoot, "*.mek", Mek.class, inheritedBaseline.getMekByFileName());
    }

    private <T extends Bsdx> void inheritSidecarFiles(
            Path outputRoot,
            String glob,
            Class<T> expectedType,
            Map<String, T> target
    ) {
        if (target == null) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputRoot, glob)) {
            for (Path path : stream) {
                ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                Object data = dto.getData();
                if (!expectedType.isInstance(data)) {
                    continue;
                }
                String fileName = path.getFileName().toString();
                putReplacingCaseInsensitive(target, fileName, expectedType.cast(data));
            }
        } catch (IOException e) {
            throw new IllegalStateException("继承 JINKI sidecar 二进制成果物失败: " + outputRoot, e);
        }
    }

    private <T> void putReplacingCaseInsensitive(Map<String, T> target, String fileName, T value) {
        String normalized = normalizeFileName(fileName);
        target.entrySet().removeIf(entry -> normalizeFileName(entry.getKey()).equals(normalized));
        target.put(fileName, value);
    }

    private String resolveInheritedAkaoMekFileName(
            AkaoGraftResult inheritedJinkiResult,
            TsukuyomiBsdxBaselineBundle inheritedBaseline
    ) {
        if (hasExtension(inheritedJinkiResult.getReboundAkaoMek().getFileName(), ".mek")) {
            return inheritedJinkiResult.getReboundAkaoMek().getFileName();
        }
        if (inheritedJinkiResult.getGrpAppendPlan() != null
                && inheritedBaseline.getMekaGroupGrp() != null
                && inheritedBaseline.getMekaGroupGrp().getMekaList() != null) {
            int index = inheritedJinkiResult.getGrpAppendPlan().getMekaGroupIndex();
            if (index >= 0 && index < inheritedBaseline.getMekaGroupGrp().getMekaList().size()) {
                String mekaName = inheritedBaseline.getMekaGroupGrp().getMekaList().get(index).getMekaName();
                if (mekaName != null && !mekaName.isBlank()) {
                    // JINKI 成果物落盘时文件名来自 MekaGroup.mekaName，而不是 Mek DTO 自身。
                    return mekaName + ".mek";
                }
            }
        }
        return "Akao.mek";
    }

    private String resolveInheritedAkaoWazFileName(
            AkaoGraftResult inheritedJinkiResult,
            TsukuyomiBsdxBaselineBundle inheritedBaseline
    ) {
        if (hasExtension(inheritedJinkiResult.getReboundAkaoWaz().getFileName(), ".waz")) {
            return inheritedJinkiResult.getReboundAkaoWaz().getFileName();
        }
        if (inheritedJinkiResult.getGrpAppendPlan() != null
                && inheritedBaseline.getWazaGroupGrp() != null
                && inheritedBaseline.getWazaGroupGrp().getWazaList() != null
                && !inheritedJinkiResult.getGrpAppendPlan().getSourceWazGroupIndexToTargetIndex().isEmpty()) {
            int index = inheritedJinkiResult.getGrpAppendPlan().getSourceWazGroupIndexToTargetIndex().values()
                    .stream()
                    .filter(value -> value != null && value >= 0)
                    .findFirst()
                    .orElse(-1);
            if (index >= 0 && index < inheritedBaseline.getWazaGroupGrp().getWazaList().size()) {
                String wazaName = inheritedBaseline.getWazaGroupGrp().getWazaList().get(index).getWazaName();
                if (wazaName != null && !wazaName.isBlank()) {
                    // JINKI 主 WAZ 的落盘文件名前缀来自追加后的 WazaGroup 名称。
                    return wazaName + ".waz";
                }
            }
        }
        return "Akao.waz";
    }

    private boolean hasExtension(String fileName, String extension) {
        return fileName != null && fileName.toLowerCase(java.util.Locale.ROOT).endsWith(extension);
    }

    private String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }

    private Dat resolveInheritedWeaponEquipDat(AkaoGraftResult inheritedJinkiResult) {
        Dat patched = buildJinkiPatchedWeaponEquipDat(inheritedJinkiResult);
        if (patched != null) {
            return patched;
        }
        return inheritedJinkiResult.getBsdxBaseline().getWeaponEquipDat();
    }

    private Dat buildJinkiPatchedWeaponEquipDat(AkaoGraftResult inheritedJinkiResult) {
        if (inheritedJinkiResult.getJinkiPackage() == null || inheritedJinkiResult.getBsdxBaseline() == null) {
            return null;
        }
        Dat source = inheritedJinkiResult.getJinkiPackage().getWeaponEquipDat();
        Dat baseline = inheritedJinkiResult.getBsdxBaseline().getWeaponEquipDat();
        if (source == null || baseline == null || source.getData() == null || baseline.getData() == null) {
            return null;
        }

        Dat patched = new Dat();
        patched.setFileName(baseline.getFileName());
        patched.setExtensionName(baseline.getExtensionName());
        patched.setColumnCount(baseline.getColumnCount());
        patched.setColumnTypes(baseline.getColumnTypes() == null ? new ArrayList<>() : new ArrayList<>(baseline.getColumnTypes()));

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
}
