package com.giga.nexas.bsdx;

import cn.hutool.core.util.StrUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
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
import java.util.stream.Collectors;

/**
 * 为导出实际技能信息做准备的测试类……
 * 先模拟游戏的注册行为，再根据实际waz的结构再反复循环抽出想要抽出的数据，
 * 但说实话过于复杂，一天不看就忘记自己写了啥了，所以需要的时候还是直接重写为好
 */
public class TestTransMekaInfoWithWaza {

    private static final Logger log = LoggerFactory.getLogger(TestTransMekaInfoWithWaza.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private static final Path GAME_WAZ_DIR = Paths.get("src/main/resources/game/bsdx/waz");
    private static final Path MEK_DIR = Paths.get("src/main/resources/game/bsdx/mek");
    private static final Path GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");

    public static final Map<Integer, String> MEK_GRP  = new HashMap<>(1<<7,1);
    public static final Map<Integer, String> WAZ_GRP  = new HashMap<>(1<<7,1);
    public static final Map<Integer, String> SPM_GRP  = new HashMap<>(1<<7,1);

    @Test
    void testTrans () throws IOException {
        register();
        List<Waz> allWazList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_WAZ_DIR, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();

                try {
                    ResponseDTO dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    Waz waz = (Waz) dto.getData();
                    allWazList.add(waz);
                } catch (Exception e) {
                    log.warn("Failed to parse: {}", fileName, e);
                }
            }
        }

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

        parseMeka(allWazList, allMek);

    }

    private void parseMeka(List<Waz> wazList, List<Mek> mekList) throws IOException {
        List<List<String>> tableRows = new ArrayList<>();
        tableRows.add(List.of(
                "機体ID",
                "名",
                "機体名",
                "重量",
                "ND",
                "SD",
                "BD",
                "技名",
                "技段階",
                "影響期間（Frame）",
                "始動補正",
                "補正",
                "技終了時補正",
                "攻撃力最低値",
                ""
        ));
        for (int i = 0; i < mekList.size(); i++) {
            Meka meka = new Meka();
            Mek mek = mekList.get(i);
            Waz waz = null;
            Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();
            String wazKey = WAZ_GRP.get(mekBasicInfo.getWazFileSequence());
            for (Waz waza : wazList) {
                if (waza.getFileName().equals(wazKey)) {
                    waz = waza;
                    // chris == 29, chirs2 == 30
                    break;
                }
            }
            if (waz == null) {
                log.info("waz not found");
                break;
            }

            String mekSequence = StrUtil.toString(i);
            String mekFileName = mek.getFileName();
            String mekName = mekBasicInfo.getMekName();
            String mekNormalDashSpeed = mekBasicInfo.getNormalDashSpeed().toString();
            String mekSearchDashSpeed = mekBasicInfo.getSearchDashSpeed().toString();
            String mekBoostDashSpeed = mekBasicInfo.getBoostDashSpeed().toString();

            if ("Kou".equals(mek.getFileName()) || 0==0){
                List<Waza> wazaList = parseWaza(mek, waz);
                for (Waza waza : wazaList) {
                    tableRows.add(List.of(
                            mekSequence,
                            mekFileName,
                            mekName,
                            mekNormalDashSpeed,
                            mekSearchDashSpeed,
                            mekBoostDashSpeed
                    ));
                }
            }

        }
    }

    private List<Waza> parseWaza(Mek mek, Waz waz) {
        List<Waza> wazaList = new ArrayList<>();
        Map<Integer, Mek.MekWeaponInfo> mekWeaponInfoMap = mek.getMekWeaponInfoMap();
        for (Map.Entry<Integer, Mek.MekWeaponInfo> entry : mekWeaponInfoMap.entrySet()) {
            Mek.MekWeaponInfo weaponInfo = entry.getValue();
            Integer wazSequence = weaponInfo.getWazSequence();

            List<Waz.Skill> skillList = waz.getSkillList();
            Waz.Skill skill = null;
            try {
                skill = skillList.get("Kou".equals(mek.getFileName()) && weaponInfo.getWeaponCategory()==0 ?
                        wazSequence+2 : wazSequence);
            } catch (Exception e) {

                // 只有克里斯shift时会报错，重新赋值
                // chris == 29, chris2 == 30
                log.info("skill not found, at mekName={}, weaponName={}", mek.getMekBasicInfo().getMekName(), weaponInfo.getWeaponName());
                continue;
            }

//            String weaponName1 = weaponInfo.getWeaponName();
//            if (weaponName1.contains("クライマックス")) {
//                int flag = 1;
//            } else {
//                continue;
//            }

            Integer phaseNum = 0; // 该技能的第几个阶段
            for (Waz.Skill.SkillPhase phaseInfo : skill.getPhasesInfo()) {
                Integer actionNum = 0; // 第几个动作数
                for (SkillUnit skillUnitCollection : phaseInfo.getSkillUnitCollection()) {
                    for (SkillInfoObject skillUnit : skillUnitCollection.getSkillInfoObjectList()) {
                        if (skillUnit instanceof CEventHit) {
                            CEventHit hit = (CEventHit) skillUnit;
                            for (CEventHit.CEventHitUnit hitUnit : hit.getCeventHitUnitList()) {
                                if (hitUnit.getCeventHitUnitQuantity()==6) { // 攻撃力
                                    CEventTerm term = (CEventTerm) hitUnit.getData();
                                    List<BsdxInfoCollection> attackList = term.getBsdxInfoCollectionList();
                                    BsdxInfoCollection attackInfo = attackList.get(0);
//                                    if (attackList.size() > 1){
//                                        // 没有找到大于1的情况，可以认作每个攻击力模块有且仅有一个
//                                        log.error("weaponInfo == {} ", weaponInfo.getWeaponName());
//                                    }

                                    String weaponName = weaponInfo.getWeaponName();
                                    String phaseStr = phaseNum.toString();
                                    String actionStr = actionNum.toString();
                                    String activeFrameRange = skillUnit.getStartFrame()+"~"+ skillUnit.getEndFrame();
                                    String attackTargetType = switch (hit.getAttackTargetType()) {
                                        case 1 -> "敵";
                                        case 6 -> "自分";
                                        case 3 -> "全員";
                                        default -> "未知" + String.valueOf(hit.getAttackTargetType());
                                    };
                                    String hitCount = hit.getHitCount().toString();
                                    String hitInterval = hit.getHitInterval().toString();
                                    String attackAttribute = switch (hit.getInt2()) {
                                        case 0 -> "通常";
                                        case 4 -> "燃焼";
                                        case 2, 10 -> "電撃";
                                        case 8, 24, 32, 64, 88 -> "早期放熱"+hit.getInt2()/8;
                                        default -> "未知" + hit.getInt2();
                                    };
                                    String selfStunFrame = hit.getSelfStunFrame().toString();
                                    String damageType = attackInfo.getTypeList().stream()
                                            .map(t -> switch (t) {
                                                case 0 -> "通常攻撃力";
                                                case 1 -> "自機攻撃力(%)";
                                                case 2 -> "敵機攻撃力(%)";
                                                default -> "未知" + t;
                                            })
                                            .collect(Collectors.joining(","));
                                    String damage = attackInfo.getParamList()
                                            .stream()
                                            .map(String::valueOf)
                                            .collect(Collectors.joining(","));
                                    String minDamage = hit.getMinDamage().toString();
                                    String startComboCorrection = hit.getStartComboCorrection().toString();
                                    String midComboCorrection = hit.getMidComboCorrection().toString();
                                    String endCorrection = hit.getEndCorrection().toString();
                                    String internalCorrection = hit.getInternalCorrection().toString();

                                    if (hit.getInt20()!=0) {
                                        log.info("mekName == {}, weaponName == {}, phaseNum == {}, actionNum == {} , int20 == {}",mek.getMekBasicInfo().getMekName() , weaponName, phaseNum, actionNum, hit.getInt20());
                                    }

                                    // todo
                                    int abc=1;
                                }
                            }

                        }
                    }
                    actionNum++;
                }
                phaseNum++;
            }

        }
        return wazaList;
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
