package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SouCustomizerTest {

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path output;

    @Test
    void actualSouSelectsUpperLv3AndSurvivesBinaryRoundTrip() throws Exception {
        var source = (com.giga.nexas.dto.bhe.mek.Mek) new com.giga.nexas.service.BheBinService()
                .parse("src/main/resources/game/bhe/mek/sou/sou.mek", "windows-31j").getData();
        var mek = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter().convert(source);
        int[] upperLv3 = {66, 66, 66, 67, 67, 67, 67, 67, 67, 68};
        assertArrayEquals(upperLv3, mek.getMekMaterialBlock().getTrailingEntries().get(5).getSpriteGroups().get(12));
        SouCustomizer.INSTANCE.customizeBeforeWrite(contextFor(mek));
        assertEquals(211, mek.getMekWeaponInfoMap().size());
        assertEquals(211, mek.getMekMaterialBlock().getTrailingEntries().size());
        assertEquals("UPPER", mek.getMekWeaponInfoMap().get(1).getWeaponSequence());
        assertEquals(44, mek.getMekWeaponInfoMap().get(1).getWazSequence());
        assertArrayEquals(upperLv3, mek.getMekMaterialBlock().getTrailingEntries().get(1).getSpriteGroups().get(12));
        var service = new com.giga.nexas.service.BsdxBinService();
        var file = output.resolve("sou.mek");
        mek.setExtensionName("mek");
        service.generate(file.toString(), mek, "windows-31j");
        var parsed = (Mek) service.parse(file.toString(), "windows-31j").getData();
        assertEquals(211, parsed.getMekMaterialBlock().getTrailingEntries().size());
        assertArrayEquals(upperLv3, parsed.getMekMaterialBlock().getTrailingEntries().get(1).getSpriteGroups().get(12));
        for (var ai : parsed.getMekAiInfoList()) for (var event : ai.getCpuEventList()) {
            if (event instanceof CCpuEventAttack attack && attack.getMekWeaponInfoMapNo() >= 0) {
                assertNotNull(parsed.getMekWeaponInfoMap().get(attack.getMekWeaponInfoMapNo()));
            }
        }
    }

    @Test
    void nativeMenuCandidatesFitPlayerBankWithoutDroppingSouWeaponResources() throws Exception {
        var source = (com.giga.nexas.dto.bhe.mek.Mek) new com.giga.nexas.service.BheBinService()
                .parse("src/main/resources/game/bhe/mek/sou/sou.mek", "windows-31j").getData();
        var mek = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter().convert(source);
        SouCustomizer.INSTANCE.customizeBeforeWrite(contextFor(mek));

        assertEquals(211, mek.getMekWeaponInfoMap().size(), "隐藏菜单条目不能删除武装或改变其编号");
        assertEquals(211, mek.getMekMaterialBlock().getTrailingEntries().size());
        assertEquals("EX_HAMMER", mek.getMekWeaponInfoMap().get(153).getWeaponSequence());
        assertEquals("INITIALIZER", mek.getMekWeaponInfoMap().get(174).getWeaponSequence());
        assertEquals(java.util.stream.IntStream.range(0, 153).boxed().toList(),
                mek.getMekWeaponInfoMap().entrySet().stream()
                        .filter(entry -> (entry.getValue().getWeaponIdentifier() & 2) == 0)
                        .map(java.util.Map.Entry::getKey).toList(),
                "4BEFF0在访问153格统计bank前只按identifier bit2过滤，菜单不得枚举越界编号");

        var first = mek.getMekWeaponInfoMap().get(153);
        var lowBoundary = mek.getMekWeaponInfoMap().get(152);
        lowBoundary.setWeaponIdentifier(6);
        first.setWeaponIdentifier(first.getWeaponIdentifier() | 5);
        SouCustomizer.INSTANCE.customizeBeforeWrite(contextFor(mek));
        assertEquals(7, first.getWeaponIdentifier(), "隐藏位不能清除已有bit1/4或改变重复处理结果");
        assertEquals(6, lowBoundary.getWeaponIdentifier(), "低编号已有隐藏位和其它标记必须原样保留");
        var service = new com.giga.nexas.service.BsdxBinService();
        Path file = output.resolve("sou-hidden-menu.mek");
        mek.setExtensionName("mek");
        service.generate(file.toString(), mek, "windows-31j");
        var parsed = (Mek) service.parse(file.toString(), "windows-31j").getData();
        assertEquals(211, parsed.getMekWeaponInfoMap().size());
        assertEquals(211, parsed.getMekMaterialBlock().getTrailingEntries().size());
        assertEquals(7, parsed.getMekWeaponInfoMap().get(153).getWeaponIdentifier());
        assertEquals(6, parsed.getMekWeaponInfoMap().get(152).getWeaponIdentifier());
        assertEquals(2, parsed.getMekWeaponInfoMap().get(174).getWeaponIdentifier() & 2);
        assertTrue(parsed.getMekWeaponInfoMap().entrySet().stream().filter(entry -> entry.getKey() >= 153)
                .allMatch(entry -> (entry.getValue().getWeaponIdentifier() & 2) != 0));
    }

    @Test
    void nineOtherOriginalCharactersRetainSingleMaterialLayout() throws Exception {
        var service = new com.giga.nexas.service.BheBinService();
        var converter = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter();
        for (String role : List.of("tsukuyomi", "yuri", "freja", "nagi", "misaki", "naoto", "katou", "wilhelm", "motoki")) {
            var source = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(
                    "src/main/resources/game/bhe/mek/" + role + "/" + role + ".mek", "windows-31j").getData();
            var converted = converter.convert(source);
            assertEquals(source.getMekWeaponInfoMap().size(), converted.getMekMaterialBlock().getTrailingEntries().size(), role);
        }
    }

    @Test
    void selectsLv3MaterialBeforeFilteringSparseWeaponSlots() {
        Mek mek = materialFixture(true, 100, 2);
        FollowupMekaContext context = contextFor(mek);

        SouCustomizer.INSTANCE.customizeBeforeWrite(context);

        assertEquals(List.of("STRAIGHT", "UPPER", "EX_KNUCKLE", "SP_ISSEN", "INITIALIZER"),
                mek.getMekWeaponInfoMap().values().stream().map(Mek.MekWeaponInfo::getWeaponSequence).toList());
        assertEquals(5, mek.getMekMaterialBlock().getTrailingEntries().size());
        // 原切片：STRAIGHT[0..2]、UPPER[3..5]、dummy[6..8]、FC[9]、SCRIPT[10..12]、SP[13]、INIT[14]。
        assertMaterialMarkers(mek, 2, 102, 105, 109, 113, 114);
        assertEquals(41, mek.getMekWeaponInfoMap().get(0).getWazSequence());
        assertEquals(44, mek.getMekWeaponInfoMap().get(1).getWazSequence());
        assertEquals(606, mek.getMekWeaponInfoMap().get(2).getWazSequence());
        assertEquals(680, mek.getMekWeaponInfoMap().get(3).getWazSequence());
        assertEquals(664, mek.getMekWeaponInfoMap().get(4).getWazSequence());
        assertEquals(3, mek.getMekWeaponInfoMap().get(0).getHeatMaxConsumption());
        assertEquals(0, mek.getMekWeaponInfoMap().get(0).getUpgradeExp());
        assertEquals(8, mek.getMekMaterialBlock().getRegularEntries().size());
        assertEquals(13, mek.getMekMaterialBlock().getEntries().size());
        assertArrayEquals(new int[]{900}, mek.getMekMaterialBlock().getRegularEntries().get(0).getSpriteGroups().get(0));
        assertEquals(List.of(1, 2, 3), mek.getMekAiInfoList().get(0).getCpuEventList().stream()
                .map(event -> ((CCpuEventAttack) event).getMekWeaponInfoMapNo()).toList());

        SouCustomizer.INSTANCE.customizeBeforeWrite(context);
        assertMaterialMarkers(mek, 2, 102, 105, 109, 113, 114);
        assertEquals(13, mek.getMekMaterialBlock().getEntries().size());
        assertEquals(List.of(1, 2, 3), mek.getMekAiInfoList().get(0).getCpuEventList().stream()
                .map(event -> ((CCpuEventAttack) event).getMekWeaponInfoMapNo()).toList());
    }

    @Test
    void keepsSingleMaterialLayoutInsteadOfSelectingEveryThirdEntry() {
        Mek mek = materialFixture(false, 100, 2);
        SouCustomizer.INSTANCE.customizeBeforeWrite(contextFor(mek));

        assertEquals(5, mek.getMekMaterialBlock().getTrailingEntries().size());
        assertMaterialMarkers(mek, 2, 100, 101, 103, 105, 106);
    }

    @Test
    void selectsEachDtoMaterialInItsOwnResourceIndexSpaceWithoutSharingArrays() {
        Mek selected = materialFixture(true, 100, 2);
        Mek rebound = materialFixture(true, 1000, 52);
        TsukuyomiPackageBundle selectedPackage = new TsukuyomiPackageBundle();
        selectedPackage.setTsukuyomiMek(selected);
        FollowupMekaContext context = contextFor(rebound);
        context.setSelectedPackage(selectedPackage);

        SouCustomizer.INSTANCE.customizeBeforeWrite(context);

        assertMaterialMarkers(selected, 2, 102, 105, 109, 113, 114);
        assertMaterialMarkers(rebound, 52, 1002, 1005, 1009, 1013, 1014);
        rebound.getMekMaterialBlock().getTrailingEntries().get(0).getSpriteGroups().get(52)[0] = -123;
        rebound.getMekMaterialBlock().getTrailingEntries().get(0).getSeGroups().get(0)[0] = -123;
        rebound.getMekMaterialBlock().getTrailingEntries().get(0).getVoiceGroups().get(0)[0] = -123;
        assertArrayEquals(new int[]{102}, selected.getMekMaterialBlock().getTrailingEntries().get(0).getSpriteGroups().get(2));
        assertArrayEquals(new int[]{202}, selected.getMekMaterialBlock().getTrailingEntries().get(0).getSeGroups().get(0));
        assertArrayEquals(new int[]{302}, selected.getMekMaterialBlock().getTrailingEntries().get(0).getVoiceGroups().get(0));
    }

    @Test
    void refusesAiReferenceToRemovedWeaponInsteadOfRetargetingItsNewSlot() {
        Mek mek = materialFixture(true, 100, 2);
        mek.getMekAiInfoList().get(0).getCpuEventList().add(attack(3));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> SouCustomizer.INSTANCE.customizeBeforeWrite(contextFor(mek)));
        assertTrue(error.getMessage().contains("3"));
        assertTrue(error.getMessage().contains("AI"));
        assertEquals("W_DUMMY", mek.getMekWeaponInfoMap().get(3).getWeaponSequence());
    }

    @Test
    void refusesAiReferenceToMissingWeapon() {
        Mek mek = materialFixture(true, 100, 2);
        mek.getMekAiInfoList().get(0).getCpuEventList().add(attack(99));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> SouCustomizer.INSTANCE.customizeBeforeWrite(contextFor(mek)));
        assertTrue(error.getMessage().contains("99"));
    }

    @Test
    void refusesAmbiguousMaterialLayoutBeforeChangingWeaponSlots() {
        Mek mek = materialFixture(true, 100, 2);
        mek.getMekMaterialBlock().getTrailingEntries().remove(14);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> SouCustomizer.INSTANCE.customizeBeforeWrite(contextFor(mek)));
        assertTrue(error.getMessage().contains("Material"));
        assertEquals(7, mek.getMekWeaponInfoMap().size());
        assertEquals(39, mek.getMekWeaponInfoMap().get(0).getWazSequence());
    }

    private static FollowupMekaContext contextFor(Mek mek) {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setReboundMek(mek);
        return context;
    }

    private static Mek materialFixture(boolean tiered, int markerBase, int spriteGroup) {
        Mek mek = new Mek();
        mek.getMekWeaponInfoMap().put(0, weapon("アームストレート", "STRAIGHT", 0, 39));
        mek.getMekWeaponInfoMap().put(2, weapon("アームアッパー", "UPPER", 0, 42));
        mek.getMekWeaponInfoMap().put(3, weapon("★武器ダミー", "W_DUMMY", 0, 247));
        mek.getMekWeaponInfoMap().put(7, weapon("Ｔ．Ｋ．Ｏ", "EX_KNUCKLE", 1, 606));
        mek.getMekWeaponInfoMap().put(10, weapon("SCRIPT:アームストレート", "SCR_STRAIGHT", 0, 713));
        mek.getMekWeaponInfoMap().put(15, weapon("一閃", "SP_ISSEN", 3, 680));
        mek.getMekWeaponInfoMap().put(18, weapon("イニシャライザ", "INITIALIZER", 2, 664));
        Mek.MekMaterialBlock block = mek.getMekMaterialBlock();
        block.setRegularCount(8);
        block.setExtraRegularCount(1);
        for (int i = 0; i < 8; i++) {
            block.getRegularEntries().add(material(900 + i, 0));
        }
        int marker = markerBase;
        for (Mek.MekWeaponInfo weapon : mek.getMekWeaponInfoMap().values()) {
            int count = tiered && weapon.getWeaponCategory() == 0 ? 3 : 1;
            for (int i = 0; i < count; i++) {
                block.getTrailingEntries().add(material(marker++, spriteGroup));
            }
        }
        block.getEntries().addAll(block.getRegularEntries());
        block.getEntries().addAll(block.getTrailingEntries());
        Mek.MekAiInfo ai = new Mek.MekAiInfo();
        ai.getCpuEventList().addAll(List.of(attack(2), attack(7), attack(15)));
        mek.getMekAiInfoList().add(ai);
        return mek;
    }

    private static Mek.MekWeaponInfo weapon(String name, String sequence, int category, int skill) {
        Mek.MekWeaponInfo weapon = new Mek.MekWeaponInfo();
        weapon.setWeaponName(name);
        weapon.setWeaponSequence(sequence);
        weapon.setWeaponCategory(category);
        weapon.setWazSequence(skill);
        weapon.setHeatMinConsumption(3);
        weapon.setHeatMaxConsumption(10);
        weapon.setUpgradeExp(600);
        return weapon;
    }

    private static Mek.MekMaterialBlock.PluginEntry material(int marker, int spriteGroup) {
        Mek.MekMaterialBlock.PluginEntry entry = new Mek.MekMaterialBlock.PluginEntry();
        entry.setOffset(marker);
        entry.setLength(123);
        for (int i = 0; i <= spriteGroup; i++) {
            entry.getSpriteGroups().add(i == spriteGroup ? new int[]{marker} : new int[0]);
        }
        entry.getSeGroups().add(new int[]{marker + 100});
        entry.getVoiceGroups().add(new int[]{marker + 200});
        return entry;
    }

    private static CCpuEventAttack attack(int weaponIndex) {
        CCpuEventAttack attack = new CCpuEventAttack();
        attack.setMekWeaponInfoMapNo(weaponIndex);
        return attack;
    }

    private static void assertMaterialMarkers(Mek mek, int spriteGroup, int... markers) {
        List<Mek.MekMaterialBlock.PluginEntry> entries = mek.getMekMaterialBlock().getTrailingEntries();
        assertEquals(markers.length, entries.size());
        for (int i = 0; i < markers.length; i++) {
            assertArrayEquals(new int[]{markers[i]}, entries.get(i).getSpriteGroups().get(spriteGroup), "weapon " + i);
        }
    }

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

