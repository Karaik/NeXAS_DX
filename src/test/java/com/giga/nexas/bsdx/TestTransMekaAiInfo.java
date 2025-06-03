package com.giga.nexas.bsdx;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTransMekaAiInfo {

    private static final Logger log = LoggerFactory.getLogger(TestTransMekaAiInfo.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private static final Path GAME_WAZ_DIR = Paths.get("src/main/resources/game/bsdx/waz");
    private static final Path MEK_DIR = Paths.get("src/main/resources/game/bsdx/mek");
    private static final Path GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");

    private static final Path AI_EXCEL_DIR = Paths.get("src/main/resources/aiExcel");
    private static final Path AI_CSV_DIR = Paths.get("src/main/resources/aiCsv");

    public static final Map<Integer, String> MEK_GRP  = new HashMap<>(1<<7,1);
    public static final Map<Integer, String> WAZ_GRP  = new HashMap<>(1<<7,1);
    public static final Map<Integer, String> SPM_GRP  = new HashMap<>(1<<7,1);

    @Test
    void testTransAiInfo() throws IOException {
        register();

        List<Mek> allMek = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                try {
                    ResponseDTO dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    allMek.add((Mek) dto.getData());
                } catch (Exception ignored) {
                    continue;
                }
            }
        }

        parseMeka(allMek);

    }

    // 带了注释one的，意思是这些是输出到同一个文件的，其中注释掉的部分则是一个ai一个文件的，
    // 实在是懒得解耦出来了所以不分开了，就这样吧
    private void parseMeka(List<Mek> mekList) throws IOException {
        List<String> tableHead = (List.of(
                "名", // 1
                "機体名", // 2
                "ND", // 3
                "SD", // 4
                "BD", // 5
                "AI種類", // 6
                "AI行為種類", // 7
                "発動技名", // 8
                "移動タイプ", // 9
                "移動速度(/10)", // 10
                "移動慣性", // 11
                "移動目標タイプ", // 12
                "移動目標角度補正", // 13
                "視点タイプ", // 14
                "視点角度補正", // 15
                "ジャンプタイプ", // 16
                "上昇処理用変数1", // 17
                "上昇処理用変数2", // 18
                "汎用フラグ", // 19
                "攻撃確率補正", // 20
                "type", // 21
                "short1", // 22
                "int1", // 23
                "int2", // 24
                "発動確率", // 25
                "発動確率（反撃時）", // 26
                "int5", // 27
                "発動範囲Min", // 28
                "発動範囲Max", // 29
                "角度範囲Min", // 30
                "角度範囲Max", // 31
                "発動高度Min", // 32
                "発動高度Max", // 33
                "発動耐久力Min(%)", // 34
                "発動耐久力Max(%)", // 35
                "int14", // 36
                "int15", // 37
                "発動熱量Min", // 38
                "発動熱量Max", // 39
                "int18", // 40
                "int19", // 41
                "int20", // 42
                "int21", // 43
                "int22", // 44
                "int23", // 45
                "int24", // 46
                "int25", // 47
                "int26", // 48
                "short2", // 49
                "int27", // 50
                "int28", // 51
                "short3", // 52
                "short4"  // 53
        ));

        // one
        Path outDir = AI_EXCEL_DIR;
        Files.createDirectories(outDir);
        Path xlsxPath = outDir.resolve("All_AIs.xlsx");
        Path csvPath = AI_CSV_DIR.resolve("All_AIs.csv");
        ExcelWriter writer = ExcelUtil.getWriter(xlsxPath.toFile());
        writer.getSheet().createFreezePane(0, 1);
        CsvWriter csvWriter = CsvUtil.getWriter(csvPath.toFile(), CharsetUtil.CHARSET_UTF_8);
        writer.writeRow(tableHead);
        csvWriter.write(tableHead.toArray(new String[0]));

        for (int i = 0; i < mekList.size(); i++) {
            Mek mek = mekList.get(i);
            Waz waz = null;
            Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();

            // 获取ai列表
            List<Mek.MekAiInfo> mekAiInfoList = mek.getMekAiInfoList();
            for (Mek.MekAiInfo mekAiInfo : mekAiInfoList) {
                String aiType = mekAiInfo.getAiTypeJapanese(); // AI種類(每个AI块开头的部分，如通常、逃亡等等)

                List<List<String>> tableRows = new ArrayList<>();
                tableRows.add(tableHead);

                for (CCpuEvent cpuEvent : mekAiInfo.getCpuEventList()) {

                    String mekFileName = mek.getFileName(); // 名
                    String mekName = mekBasicInfo.getMekName(); // 機体名
                    String aiTypeForHabit = null; // AI種類(根据cpu种类来判断)
                    String mekNormalDashSpeed = mekBasicInfo.getNormalDashSpeed().toString(); // ND
                    String mekSearchDashSpeed = mekBasicInfo.getSearchDashSpeed().toString(); // SD
                    String mekBoostDashSpeed = mekBasicInfo.getBoostDashSpeed().toString(); // BD

                    String wazaName = null; // 発動技名
                    String moveType = null; // 移動タイプ
                    String moveSpeed = null; // 移動速度(/10)
                    String moveInertia = null; // 移動慣性
                    String moveTargetType = null; // 移動目標タイプ
                    String moveTargetAngleCorrection = null; // 移動目標角度補正
                    String viewpointType = null; // 視点タイプ
                    String viewpointAngleCorrection = null; // 視点角度補正
                    String jumpType = null; // ジャンプタイプ
                    String ascentVar1 = null; // 上昇処理用変数1
                    String ascentVar2 = null; // 上昇処理用変数2
                    String genericFlag = null; // 汎用フラグ
                    String attackProbabilityCorrection = null; // 攻撃確率補正
                    String type = null;
                    String short1 = null;
                    String int1 = null;
                    String int2 = null;
                    String activationProbability = null; // 発動確率
                    String counterActivationProbability = null; // 発動確率（反撃時）
                    String int5 = null;
                    String activationRangeMin = null; // 発動範囲Min
                    String activationRangeMax = null; // 発動範囲Max
                    String angleRangeMin = null; // 角度範囲Min
                    String angleRangeMax = null; // 角度範囲Max
                    String activationAltitudeMin = null; // 発動高度Min
                    String activationAltitudeMax = null; // 発動高度Max
                    String activationDurabilityMin = null; // 発動耐久力Min(%)
                    String activationDurabilityMax = null; // 発動耐久力Max(%)
                    String int14 = null;
                    String int15 = null;
                    String activationHeatMin = null; // 発動熱量Min
                    String activationHeatMax = null; // 発動熱量Max
                    String int18 = null;
                    String int19 = null;
                    String int20 = null;
                    String int21 = null;
                    String int22 = null;
                    String int23 = null;
                    String int24 = null;
                    String int25 = null;
                    String int26 = null;
                    String short2 = null;
                    String int27 = null;
                    String int28 = null;
                    String short3 = null;
                    String short4 = null;
                    if (cpuEvent instanceof CCpuEventMove) {
                        CCpuEventMove cpuEventMove = (CCpuEventMove) cpuEvent;
                        aiTypeForHabit = "移動系";
                        moveType = String.valueOf(cpuEventMove.getMoveType());
                        moveSpeed = String.valueOf(cpuEventMove.getMoveSpeed());
                        moveInertia = String.valueOf(cpuEventMove.getMoveInertia());
                        moveTargetType = String.valueOf(cpuEventMove.getMoveTargetType());
                        moveTargetAngleCorrection = String.valueOf(cpuEventMove.getMoveTargetAngleCorrection());
                        viewpointType = String.valueOf(cpuEventMove.getViewpointType());
                        viewpointAngleCorrection = String.valueOf(cpuEventMove.getViewpointAngleCorrection());
                        jumpType = String.valueOf(cpuEventMove.getJumpType());
                        ascentVar1 = String.valueOf(cpuEventMove.getAscentVar1());
                        ascentVar2 = String.valueOf(cpuEventMove.getAscentVar2());
                        genericFlag = String.valueOf(cpuEventMove.getGenericFlag());
                        attackProbabilityCorrection = String.valueOf(cpuEventMove.getAttackProbabilityCorrection());
                    } else if (cpuEvent instanceof CCpuEventAttack) {
                        CCpuEventAttack cpuEventAttack = (CCpuEventAttack) cpuEvent;
                        aiTypeForHabit = "攻撃系";
                        wazaName = mek.getMekWeaponInfoMap().get(cpuEventAttack.getMekWeaponInfoMapNo()).getWeaponName();
                    } else {
                        log.error("parse AI error, mek == {}", mekName);
                        throw new RuntimeException("parse AI error, mek == " + mekName);
                    }
                    type = String.valueOf(cpuEvent.getType());
                    short1 = String.valueOf(cpuEvent.getShort1());
                    int1 = String.valueOf(cpuEvent.getInt1());
                    int2 = String.valueOf(cpuEvent.getInt2());
                    activationProbability = String.valueOf(cpuEvent.getActivationProbability());
                    counterActivationProbability = String.valueOf(cpuEvent.getActivationProbabilityWhenCounter());
                    int5 = String.valueOf(cpuEvent.getInt5());
                    activationRangeMin = String.valueOf(cpuEvent.getActivationRangeMin());
                    activationRangeMax = String.valueOf(cpuEvent.getActivationRangeMax());
                    angleRangeMin = String.valueOf(cpuEvent.getActivationAngleRangeMin());
                    angleRangeMax = String.valueOf(cpuEvent.getActivationAngleRangeMax());
                    activationAltitudeMin = String.valueOf(cpuEvent.getActivationAltitudeMin());
                    activationAltitudeMax = String.valueOf(cpuEvent.getActivationAltitudeMax());
                    activationDurabilityMin = String.valueOf(cpuEvent.getActivationDurabilityMinPercentage());
                    activationDurabilityMax = String.valueOf(cpuEvent.getActivationDurabilityMaxPercentage());
                    int14 = String.valueOf(cpuEvent.getInt14());
                    int15 = String.valueOf(cpuEvent.getInt15());
                    activationHeatMin = String.valueOf(cpuEvent.getActivationHeatMin());
                    activationHeatMax = String.valueOf(cpuEvent.getActivationHeatMax());
                    int18 = String.valueOf(cpuEvent.getInt18());
                    int19 = String.valueOf(cpuEvent.getInt19());
                    int20 = String.valueOf(cpuEvent.getInt20());
                    int21 = String.valueOf(cpuEvent.getInt21());
                    int22 = String.valueOf(cpuEvent.getInt22());
                    int23 = String.valueOf(cpuEvent.getInt23());
                    int24 = String.valueOf(cpuEvent.getInt24());
                    int25 = String.valueOf(cpuEvent.getInt25());
                    int26 = String.valueOf(cpuEvent.getInt26());
                    short2 = String.valueOf(cpuEvent.getShort2());
                    int27 = String.valueOf(cpuEvent.getInt27());
                    int28 = String.valueOf(cpuEvent.getInt28());
                    short3 = String.valueOf(cpuEvent.getShort3());
                    short4 = String.valueOf(cpuEvent.getShort4());

                    List<String> row = new ArrayList<>();
                    row.add(nvl(mekFileName));
                    row.add(nvl(mekName));
                    row.add(nvl(mekNormalDashSpeed));
                    row.add(nvl(mekSearchDashSpeed));
                    row.add(nvl(mekBoostDashSpeed));
                    row.add(nvl(aiType));
                    row.add(nvl(aiTypeForHabit));
                    row.add(nvl(wazaName));
                    row.add(nvl(moveType));
                    row.add(nvl(moveSpeed));
                    row.add(nvl(moveInertia));
                    row.add(nvl(moveTargetType));
                    row.add(nvl(moveTargetAngleCorrection));
                    row.add(nvl(viewpointType));
                    row.add(nvl(viewpointAngleCorrection));
                    row.add(nvl(jumpType));
                    row.add(nvl(ascentVar1));
                    row.add(nvl(ascentVar2));
                    row.add(nvl(genericFlag));
                    row.add(nvl(attackProbabilityCorrection));
                    row.add(nvl(type));
                    row.add(nvl(short1));
                    row.add(nvl(int1));
                    row.add(nvl(int2));
                    row.add(nvl(activationProbability));
                    row.add(nvl(counterActivationProbability));
                    row.add(nvl(int5));
                    row.add(nvl(activationRangeMin));
                    row.add(nvl(activationRangeMax));
                    row.add(nvl(angleRangeMin));
                    row.add(nvl(angleRangeMax));
                    row.add(nvl(activationAltitudeMin));
                    row.add(nvl(activationAltitudeMax));
                    row.add(nvl(activationDurabilityMin));
                    row.add(nvl(activationDurabilityMax));
                    row.add(nvl(int14));
                    row.add(nvl(int15));
                    row.add(nvl(activationHeatMin));
                    row.add(nvl(activationHeatMax));
                    row.add(nvl(int18));
                    row.add(nvl(int19));
                    row.add(nvl(int20));
                    row.add(nvl(int21));
                    row.add(nvl(int22));
                    row.add(nvl(int23));
                    row.add(nvl(int24));
                    row.add(nvl(int25));
                    row.add(nvl(int26));
                    row.add(nvl(short2));
                    row.add(nvl(int27));
                    row.add(nvl(int28));
                    row.add(nvl(short3));
                    row.add(nvl(short4));

                    tableRows.add(row);

                    // one
                    writer.writeRow(row);
                    csvWriter.write(row.toArray(new String[0]));
                }
//                Path outDir = AI_EXCEL_DIR;
//                String safeFileName = (mekBasicInfo.getMekName() + "_" + aiType)
//                        .replaceAll("[\\\\/:*?\"<>|]", "_") + ".xlsx";
//                Files.createDirectories(outDir);
//                Path outPath = outDir.resolve(safeFileName);
//                log.info("path: {}", outPath.toAbsolutePath());
//                ExcelWriter writer = ExcelUtil.getWriter(outPath.toFile());
//                writer.getSheet().createFreezePane(0, 1); // 冻结首行
//                writer.write(tableRows, true);
//                writer.close();
//
//
//                String csvFileName = safeFileName.replace(".xlsx", ".csv");
//                Path csvPath = AI_CSV_DIR.resolve(csvFileName);
//                log.info("csv path: {}", csvPath.toAbsolutePath());
//                CsvWriter csvWriter = CsvUtil.getWriter(csvPath.toFile(), CharsetUtil.CHARSET_UTF_8);
//                for (List<String> rowData : tableRows) {
//                    csvWriter.write(rowData.toArray(new String[0]));
//                }
//                csvWriter.close();

            }

        }

        // one
        writer.close();
        csvWriter.close();
    }

    private String nvl(String val) {
        if ("-999".equals(val)) {
            return "無制限";
        }
        return val == null ? "" : val;
    }

    private void register() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    Grp grp = (Grp) dto.getData();
                    if (grp instanceof MekaGroupGrp) {
                        registerMek(grp);
                    } else if (grp instanceof WazaGroupGrp) {
                        registerWaz(grp);
                    } else if (grp instanceof SpriteGroupGrp) {
                        registerSpm(grp);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse: {}", fileName);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerMek(Grp grp) {
        MekaGroupGrp mekaGroupGrp = (MekaGroupGrp) grp;
        for (int i = 0; i < mekaGroupGrp.getMekaList().size(); i++) {
            MEK_GRP.put(i, mekaGroupGrp.getMekaList().get(i).getMekaName());
        }
        MEK_GRP.put(110, "Akao"); // 没有，所以手动注册
    }

    private void registerWaz(Grp grp) {
        WazaGroupGrp wazGroupGrp = (WazaGroupGrp) grp;
        for (int i = 0; i < wazGroupGrp.getWazaList().size(); i++) {
            WAZ_GRP.put(i, wazGroupGrp.getWazaList().get(i).getWazaDisplayName());
        }
        WAZ_GRP.put(110, "Akao"); // 没有，所以手动注册
    }

    private void registerSpm(Grp grp) {
        SpriteGroupGrp spriteGroupGrp = (SpriteGroupGrp) grp;
        for (int i = 0; i < spriteGroupGrp.getSpriteList().size(); i++) {
            SPM_GRP.put(i, spriteGroupGrp.getSpriteList().get(i).getSpriteFileName().split("\\.")[0]);
        }
    }

}
