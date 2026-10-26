package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayableWeaponLoadoutsTest {
    @Test
    void everyReworkedCharacterGetsOrdinaryForceCrashAndInitializerSlots() throws Exception {
        Map<String, String> variants = new LinkedHashMap<>();
        variants.put("tsukuyomi", "袈裟・一閃");
        variants.put("yuri", "");
        variants.put("freja", "");
        variants.put("nagi", "瞬間ディフレクト");
        variants.put("misaki", "転移手・挟撃");
        variants.put("naoto", "ワンツー・コンファーム");
        variants.put("katou", "トンファー・二連打");
        variants.put("wilhelm", "交互砲撃・離脱");
        variants.put("motoki", "集中爆撃・帰投");
        for (var role : variants.entrySet()) {
            var source = (com.giga.nexas.dto.bhe.mek.Mek) new BheBinService().parse(
                    "src/main/resources/game/bhe/mek/" + role.getKey() + ".mek", "windows-31j").getData();
            Mek mek = new BheToBsdxMekConverter().convert(source);
            Mek.MekWeaponInfo initializer = new Mek.MekWeaponInfo();
            initializer.setWeaponName("イニシャライザ");
            initializer.setWeaponCategory(2);
            mek.getMekWeaponInfoMap().put(mek.getMekWeaponInfoMap().size(), initializer);
            if (!role.getValue().isEmpty()) {
                Mek.MekWeaponInfo variant = new Mek.MekWeaponInfo();
                variant.setWeaponName(role.getValue());
                variant.setWeaponCategory(0);
                mek.getMekWeaponInfoMap().put(mek.getMekWeaponInfoMap().size(), variant);
            }
            List<Object> row = PlayableWeaponLoadouts.append(baseline(), mek).getData().get(1);
            assertEquals(14, row.size(), role.getKey());
            java.util.Set<Object> ids = new java.util.HashSet<>();
            for (int slot = 0; slot < 14; slot++) {
                int id = (Integer) row.get(slot);
                if (id == -1) continue;
                assertTrue(ids.add(id), role.getKey() + " 同一武装不能占用两个槽");
                assertTrue(id <= 152 && id >= 0, role.getKey());
                assertEquals(slot < 12 ? 0 : slot == 12 ? 1 : 2,
                        mek.getMekWeaponInfoMap().get(id).getWeaponCategory(), role.getKey());
                assertFalse(mek.getMekWeaponInfoMap().get(id).getWeaponName().startsWith("SCRIPT:"));
            }
        }
    }

    @Test
    void loadoutUsesFinalWeaponIdentitiesAndKeepsInheritedRows() throws Exception {
        Mek mek = tsukuyomi();
        // 新武装保留源sequence，因此必须按最终日文身份区分它与原袈裟。
        Mek.MekWeaponInfo variant = new Mek.MekWeaponInfo();
        variant.setWeaponName("袈裟・一閃");
        variant.setWeaponSequence("PUNCH");
        variant.setWeaponCategory(0);
        mek.getMekWeaponInfoMap().put(19, variant);
        Mek.MekWeaponInfo initializer = new Mek.MekWeaponInfo();
        initializer.setWeaponName("イニシャライザ");
        initializer.setWeaponCategory(2);
        mek.getMekWeaponInfoMap().put(18, initializer);
        // 不允许将源表的裸编号当作最终武装号。
        Map<Integer, Mek.MekWeaponInfo> relocated = new LinkedHashMap<>();
        mek.getMekWeaponInfoMap().forEach((key, value) -> relocated.put(key + 40, value));
        mek.setMekWeaponInfoMap(relocated);
        Dat baseline = baseline();
        List<Object> inherited = new ArrayList<>(baseline.getData().get(0));

        Dat result = PlayableWeaponLoadouts.append(baseline, mek);

        assertEquals(List.of(59,43,44,40,45,46,50,51,52,47,48,41,53,58), result.getData().get(1));
        assertEquals(inherited, baseline.getData().get(0));
        assertEquals(1, baseline.getData().size());
        result.getData().get(0).set(0, 99);
        assertEquals(inherited, baseline.getData().get(0), "继承的装备行不能共享可变对象");
        assertEquals(14, result.getColumnCount());
    }

    @Test
    void tsukuyomiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("tsukuyomi", "袈裟・一閃",
                List.of("銃撃", "銀鈴", "疾風"), List.of("切返", "一威", "納刀"));
    }

    @Test
    void yuriUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("yuri", "",
                List.of("パルスライフル", "スナイパーライフル", "マインスイーパ：攻"),
                Arrays.asList("マインスイーパ：避", "スモークディスチャージャー", null));
    }

    @Test
    void frejaUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("freja", "", List.of("ジャベリン", "必殺ジャベリン", "火炎弾"),
                Arrays.asList("スイング", null, "火炎放射ビット"));
    }

    @Test
    void nagiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("nagi", "瞬間ディフレクト",
                List.of("転移ケイジブレイド", "シールド：単発リフレクション", "シールド：多発リフレクション"),
                List.of("シールド：シールドバッシュ", "シールド：転移", "防壁展開"));
    }

    @Test
    void misakiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("misaki", "転移手・挟撃",
                List.of("鉱石発射", "本気：血水晶のカンオケ", "本気：血水晶のヤリ"),
                List.of("転移手：サーチビターン", "本気：血水晶のツルギ", "あたためる"));
    }

    @Test
    void naotoUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("naoto", "ワンツー・コンファーム", List.of("マシンガン", "炸裂弾", "兆弾針"),
                List.of("エクステンドネイル", "ストンプ", "クロー収納"));
    }

    @Test
    void katouUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("katou", "トンファー・二連打", List.of("キャノン砲", "ミサイルランチャー", "手榴弾"),
                List.of("トンファー振り下ろし", "トンファーアッパー", "トンファー収納"));
    }

    @Test
    void wilhelmUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("wilhelm", "交互砲撃・離脱", List.of("サインミサイル", "ロックオンミサイル", "肩ミサイル"),
                List.of("砲撃", "対空砲火", "冷却ビット"));
    }

    @Test
    void motokiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertRangedAndDashRows("motoki", "集中爆撃・帰投", List.of("ガンポッド", "妨害ミサイル", "ビーム"),
                List.of("飛行形態：終了", "ＶＴＯＬ", "ハリケーン"));
    }

    @Test
    void rejectsSelectableWeaponOutsidePlayerBankWithoutChangingBaseline() throws Exception {
        Mek mek = tsukuyomi();
        Mek.MekWeaponInfo variant = new Mek.MekWeaponInfo();
        variant.setWeaponName("袈裟・一閃");
        variant.setWeaponCategory(0);
        mek.getMekWeaponInfoMap().put(153, variant);
        Mek.MekWeaponInfo initializer = new Mek.MekWeaponInfo();
        initializer.setWeaponName("イニシャライザ");
        initializer.setWeaponCategory(2);
        mek.getMekWeaponInfoMap().put(18, initializer);
        Dat baseline = baseline();

        assertThrows(IllegalStateException.class, () -> PlayableWeaponLoadouts.append(baseline, mek));

        assertEquals(1, baseline.getData().size());
        assertEquals(Collections.nCopies(14, -1), baseline.getData().get(0));
    }

    @Test
    void ordinarySlotsCannotSilentlyConsumeForceCrashWeapons() throws Exception {
        Mek mek = tsukuyomi();
        Mek.MekWeaponInfo variant = new Mek.MekWeaponInfo();
        variant.setWeaponName("袈裟・一閃");
        variant.setWeaponCategory(1);
        mek.getMekWeaponInfoMap().put(19, variant);
        assertThrows(IllegalStateException.class, () -> PlayableWeaponLoadouts.append(baseline(), mek));
    }

    @Test
    void unrelatedCharacterKeepsExistingDefaultPreset() throws Exception {
        Mek sou = new Mek();
        sou.setFileName("sou");
        Dat result = PlayableWeaponLoadouts.append(baseline(), sou);
        assertEquals(List.of(0,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1), result.getData().get(1));
    }

    private void assertRangedAndDashRows(String role, String variantName, List<String> ranged,
                                        List<String> dash) throws Exception {
        var source = (com.giga.nexas.dto.bhe.mek.Mek) new BheBinService().parse(
                "src/main/resources/game/bhe/mek/" + role + ".mek", "windows-31j").getData();
        Mek mek = new BheToBsdxMekConverter().convert(source);
        Mek.MekWeaponInfo initializer = new Mek.MekWeaponInfo();
        initializer.setWeaponName("イニシャライザ");
        initializer.setWeaponCategory(2);
        mek.getMekWeaponInfoMap().put(mek.getMekWeaponInfoMap().size(), initializer);
        if (!variantName.isEmpty()) {
            Mek.MekWeaponInfo variant = new Mek.MekWeaponInfo();
            variant.setWeaponName(variantName);
            variant.setWeaponCategory(0);
            mek.getMekWeaponInfoMap().put(mek.getMekWeaponInfoMap().size(), variant);
        }
        Map<Integer, Mek.MekWeaponInfo> relocated = new LinkedHashMap<>();
        mek.getMekWeaponInfoMap().forEach((key, value) -> relocated.put(key + 40, value));
        mek.setMekWeaponInfoMap(relocated);

        List<Object> row = PlayableWeaponLoadouts.append(baseline(), mek).getData().get(1);

        // DX DAT按3*state+button存放；state2=遠距離，state3=ダッシュ，组内A/B/C顺序固定。
        assertAll(role,
                () -> assertEquals(ranged, weaponNames(mek, row, 6), "远距A/B/C必须选择最终武装身份"),
                () -> assertEquals(dash, weaponNames(mek, row, 9), "普通Dash A/B/C必须选择最终武装身份"));
    }

    private List<String> weaponNames(Mek mek, List<Object> row, int start) {
        List<String> result = new ArrayList<>();
        for (int slot = start; slot < start + 3; slot++) {
            int id = (Integer) row.get(slot);
            result.add(id == -1 ? null : mek.getMekWeaponInfoMap().get(id).getWeaponName());
        }
        return result;
    }

    private Mek tsukuyomi() throws Exception {
        var source = (com.giga.nexas.dto.bhe.mek.Mek) new BheBinService()
                .parse("src/main/resources/game/bhe/mek/tsukuyomi.mek", "windows-31j").getData();
        return new BheToBsdxMekConverter().convert(source);
    }

    private Dat baseline() {
        Dat baseline = new Dat();
        baseline.setFileName("WeaponEquip");
        baseline.setExtensionName("dat");
        baseline.setColumnCount(14);
        baseline.setColumnTypes(new ArrayList<>(Collections.nCopies(14, "IntegerNew")));
        baseline.addRow(new ArrayList<>(Collections.nCopies(14, -1)));
        return baseline;
    }

}
