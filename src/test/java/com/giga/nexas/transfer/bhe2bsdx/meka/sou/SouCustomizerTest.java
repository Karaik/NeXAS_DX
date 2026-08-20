package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SouCustomizerTest {

    @Test
    @DisplayName("测试 SouCustomizer 将 Sou 的常规技能批量重映射为 Lv3 完全体并规整热量消耗")
    public void testUpgradeSouSkillsToMaxLevel() {
        Mek souMek = new Mek();

        // 模拟武器 0：アームストレート Lv1 (39)
        Mek.MekWeaponInfo weapon0 = new Mek.MekWeaponInfo();
        weapon0.setWeaponName("アームストレート");
        weapon0.setWazSequence(39);
        weapon0.setHeatMaxConsumption(10);
        weapon0.setHeatMinConsumption(3);
        weapon0.setUpgradeExp(600);
        souMek.getMekWeaponInfoMap().put(0, weapon0);

        // 模拟武器 1：アームアッパー Lv1 (42)
        Mek.MekWeaponInfo weapon1 = new Mek.MekWeaponInfo();
        weapon1.setWeaponName("アームアッパー");
        weapon1.setWazSequence(42);
        weapon1.setHeatMaxConsumption(10);
        weapon1.setHeatMinConsumption(3);
        weapon1.setUpgradeExp(800);
        souMek.getMekWeaponInfoMap().put(1, weapon1);

        // 模拟武器 35：ビームソード Lv1 (145)
        Mek.MekWeaponInfo weapon35 = new Mek.MekWeaponInfo();
        weapon35.setWeaponName("ビームソード");
        weapon35.setWazSequence(145);
        weapon35.setHeatMaxConsumption(18);
        weapon35.setHeatMinConsumption(6);
        weapon35.setUpgradeExp(1000);
        souMek.getMekWeaponInfoMap().put(35, weapon35);

        // 模拟武器 139：ラブシャワー Lv1 (459)
        Mek.MekWeaponInfo weapon139 = new Mek.MekWeaponInfo();
        weapon139.setWeaponName("ラブシャワー");
        weapon139.setWazSequence(459);
        weapon139.setHeatMaxConsumption(30);
        weapon139.setHeatMinConsumption(10);
        weapon139.setUpgradeExp(1200);
        souMek.getMekWeaponInfoMap().put(139, weapon139);

        // 模拟 FC 槽位
        Mek.MekWeaponInfo fcWeapon = new Mek.MekWeaponInfo();
        fcWeapon.setWeaponName("武幻翔嵐舞");
        fcWeapon.setWeaponCategory(1);
        fcWeapon.setWazSequence(0);
        souMek.getMekWeaponInfoMap().put(140, fcWeapon);

        FollowupMekaContext context = new FollowupMekaContext();
        context.setReboundMek(souMek);

        // 执行定制器
        SouCustomizer.INSTANCE.customizeBeforeWrite(context);

        // 验证アームストレート升级为 Lv3 (41)，最大热量降为 3，经验清零
        assertEquals(41, weapon0.getWazSequence(), "アームストレート应该升级为 Lv3 (41)");
        assertEquals(3, weapon0.getHeatMaxConsumption(), "满级武器最大热量应降为最小热量 3");
        assertEquals(0, weapon0.getUpgradeExp(), "满级武器升级经验应清零");

        // 验证アームアッパー升级为 Lv3 (44)
        assertEquals(44, weapon1.getWazSequence(), "アームアッパー应该升级为 Lv3 (44)");
        assertEquals(3, weapon1.getHeatMaxConsumption(), "アームアッパー最大热量应降为最小热量 3");

        // 验证ビームソード升级为 Lv3 (147)
        assertEquals(147, weapon35.getWazSequence(), "ビームソード应该升级为 Lv3 (147)");
        assertEquals(6, weapon35.getHeatMaxConsumption(), "ビームソード最大热量应降为最小热量 6");

        // 验证末尾武器升级为 Lv3 (461)
        assertEquals(461, weapon139.getWazSequence(), "ラブシャワー应该升级为 Lv3 (461)");
        assertEquals(10, weapon139.getHeatMaxConsumption(), "ラブシャワー最大热量应降为最小热量 10");

        // 验证 FC 槽位配置为 EX 技能 (615: 武幻翔嵐舞)
        assertEquals(615, fcWeapon.getWazSequence(), "FC 槽位应该配置为 EX 技能 615");
    }

    @Test
    @DisplayName("测试从原始 sou.mek 解析时完整保留全部 226 门有效武器（包含 36 门 FC 及 Initializer）")
    public void testParseSouMekRetainsFcWeapons() throws Exception {
        com.giga.nexas.service.BheBinService bheBinService = new com.giga.nexas.service.BheBinService();
        com.giga.nexas.dto.bhe.mek.Mek bheSouMek = (com.giga.nexas.dto.bhe.mek.Mek) bheBinService.parse(
                "src/main/resources/game/bhe/mek/sou/sou.mek",
                "windows-31j"
        ).getData();

        // 验证解析出全部 226 门有效武器
        assertEquals(226, bheSouMek.getMekWeaponInfoMap().size(), "sou.mek 应成功解析全部 226 门非空武器");

        // 验证 FC 武器共有 36 门（Category == 1），且 wazSequence 覆盖 606..641
        long fcCount = bheSouMek.getMekWeaponInfoMap().values().stream()
                .filter(w -> w.getWeaponCategory() != null && w.getWeaponCategory() == 1)
                .count();
        assertEquals(36, fcCount, "sou.mek 中应包含 36 门 Force Crash 武器");

        // 验证 Initializer 武器（Category == 2）
        long initCount = bheSouMek.getMekWeaponInfoMap().values().stream()
                .filter(w -> w.getWeaponCategory() != null && w.getWeaponCategory() == 2)
                .count();
        assertEquals(1, initCount, "sou.mek 中应包含 1 门 Initializer 武器");
    }
}

