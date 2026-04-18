package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;

import java.util.ArrayList;
import java.util.List;

public class BuildBaselineFromJinkiResultStep {

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
