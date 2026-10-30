package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayableWeaponLoadoutsTest {
    @Test
    void everyReworkedCharacterGetsOrdinaryForceCrashAndInitializerSlots() throws Exception {
        for (String role : List.of("tsukuyomi", "yuri", "freja", "nagi", "misaki", "naoto", "katou", "wilhelm", "motoki")) {
            Mek mek = character(role);
            List<Object> row = PlayableWeaponLoadouts.append(baseline(), mek).getData().get(1);
            assertEquals(14, row.size(), role);
            java.util.Set<Object> ids = new java.util.HashSet<>();
            for (int slot = 0; slot < 14; slot++) {
                int id = (Integer) row.get(slot);
                assertTrue(ids.add(id), role + " 同一武装不能占用两个槽");
                assertTrue(id <= 152 && id >= 0, role);
                assertEquals(slot < 12 ? 0 : slot == 12 ? 1 : 2,
                        mek.getMekWeaponInfoMap().get(id).getWeaponCategory(), role);
                assertFalse(mek.getMekWeaponInfoMap().get(id).getWeaponName().startsWith("SCRIPT:"));
            }
            List<String> ordinary = new ArrayList<>();
            for (int slot = 0; slot < 12; slot++) ordinary.add(mek.getMekWeaponInfoMap().get((Integer) row.get(slot)).getWeaponName());
            for (String name : additionalWeapons(role)) assertEquals(1, Collections.frequency(ordinary, name),
                    role + " 的六个追加武装都须在普通槽出现且只出现一次: " + name);
        }
    }

    @Test
    void loadoutUsesFinalWeaponIdentitiesAndKeepsInheritedRows() throws Exception {
        Mek mek = character("tsukuyomi");
        // 新武装保留源sequence，因此必须按最终日文身份区分它与原袈裟。
        assertEquals("PUNCH", mek.getMekWeaponInfoMap().get(19).getWeaponSequence());
        // 不允许将源表的裸编号当作最终武装号。
        Map<Integer, Mek.MekWeaponInfo> relocated = new LinkedHashMap<>();
        mek.getMekWeaponInfoMap().forEach((key, value) -> relocated.put(key + 40, value));
        mek.setMekWeaponInfoMap(relocated);
        Dat baseline = baseline();
        List<Object> inherited = new ArrayList<>(baseline.getData().get(0));

        Dat result = PlayableWeaponLoadouts.append(baseline, mek);

        // 源0..17、initializer18，六stub依次19..24；整体移位40后手工列出批准表。
        assertEquals(List.of(59,60,42,40,62,61,63,51,52,64,45,41,53,58), result.getData().get(1));
        assertEquals(inherited, baseline.getData().get(0));
        assertEquals(1, baseline.getData().size());
        result.getData().get(0).set(0, 99);
        assertEquals(inherited, baseline.getData().get(0), "继承的装备行不能共享可变对象");
        assertEquals(14, result.getColumnCount());
        assertEquals(baseline.getColumnTypes(), result.getColumnTypes());
        assertEquals("WeaponEquip", result.getFileName());
        assertEquals("dat", result.getExtensionName());
    }

    @Test
    void tsukuyomiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("tsukuyomi",
                List.of("袈裟・一閃", "打突・留め", "袈裟"),
                List.of("抜刀", "一威・止刃", "逆風・返し"),
                List.of("銃撃・離脱", "銀鈴", "疾風"),
                List.of("切返・二連", "唐竹", "納刀"), "穿心");
    }

    @Test
    void yuriUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("yuri",
                List.of("ダガー・刻み", "アサシンダガー・速刺", "サイドステップ・地走"),
                List.of("アサシンダガー", "サイドステップキック", "バスターブリッジ"),
                List.of("パルス・三点射", "狙撃・一射離脱", "マインスイーパ：攻"),
                List.of("マイン・置き逃げ", "スモークディスチャージャー", "サマーソルト"), "リベリオンホーク");
    }

    @Test
    void frejaUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("freja",
                List.of("スピア・留め突き", "斬り上げ・低空", "バックキック・退避"),
                List.of("ラッシュ", "スピア", "叩き付け"),
                List.of("ジャベリン・二段投擲", "火炎弾・扇射", "必殺ジャベリン"),
                List.of("スイング・双薙ぎ", "踏み付け", "火炎放射ビット"), "武幻剛乱舞");
    }

    @Test
    void nagiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("nagi",
                List.of("瞬間ディフレクト", "斬上げ・接続", "乱れ突き・三連"),
                List.of("転移突き・追撃", "シールド：開始", "シールド：終了"),
                List.of("転移ケイジブレイド", "シールド：多発リフレクション", "防壁展開"),
                List.of("盾打ち・反攻", "シールド：転移", "単発リフレクト・解放"), "エクスカリバー");
    }

    @Test
    void misakiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("misaki",
                List.of("鉱石・二拍射", "転移手・挟撃", "掴む"),
                List.of("上昇体当たり", "転移手・二度招き", "血晶剣・帰着"),
                List.of("鉱石発射", "血晶槍・短束", "床召還・重ね置き"),
                List.of("転移手：サーチビターン", "抱きしめる", "あたためる"), "運命の赤い糸");
    }

    @Test
    void naotoUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("naoto",
                List.of("ジャブ", "ワンツー・コンファーム", "ストレートパンチ"),
                List.of("クロー展開", "クロー・起動突き", "ダウンネイル・収納"),
                List.of("マシンガン・三点射", "炸裂弾・交差連射", "兆弾針"),
                List.of("フック・リターン", "Ｗアームハンマー", "クロー収納"), "ネイルバイティング");
    }

    @Test
    void katouUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("katou",
                List.of("トンファーアッパー", "トンファー・二連打", "トンファー・引き肘"),
                List.of("トンファー構え", "トンファー・踏込肘", "トンファー・叩き納め"),
                List.of("キャノン砲", "ミサイル・二段牽制", "手榴弾"),
                List.of("蹴崩し・追い肘", "トンファーエルボー", "トンファー収納"), "グランドスラム");
    }

    @Test
    void wilhelmUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("wilhelm",
                List.of("キック・導弾追撃", "ミサイルアサルト", "キック"),
                List.of("キャタピラ：開始", "キャタピラ：前進", "キャタピラ：終了"),
                List.of("サインミサイル・二拍", "肩ミサイル・圧制", "照準砲撃・制動"),
                List.of("交互砲撃・離脱", "対空砲火・収束", "冷却ビット"), "瀧本流導弾格闘術");
    }

    @Test
    void motokiUsesRangedAndDashRowsForTheirIntendedWeaponIdentities() throws Exception {
        assertDefaultRows("motoki",
                List.of("ワンツー・離脱", "ドリル・確定突き", "排熱弾・即発"),
                List.of("飛行形態：開始", "飛行形態：ソニックブーム", "集中爆撃・帰投"),
                List.of("ガンポッド・三連点射", "妨害ミサイル", "ビーム"),
                List.of("飛行形態：終了", "ビーム・短照射", "ハリケーン"), "竜巻");
    }

    @Test
    void rejectsSelectableWeaponOutsidePlayerBankWithoutChangingBaseline() throws Exception {
        Mek mek = character("tsukuyomi");
        Mek.MekWeaponInfo variant = mek.getMekWeaponInfoMap().remove(19);
        mek.getMekWeaponInfoMap().put(153, variant);
        Dat baseline = baseline();

        IllegalStateException rejected = assertThrows(IllegalStateException.class, () -> PlayableWeaponLoadouts.append(baseline, mek));
        assertTrue(rejected.getMessage().contains("0..152"), "完整夹具须因玩家bank越界拒绝，而非因其它身份缺失");

        assertEquals(1, baseline.getData().size());
        assertEquals(Collections.nCopies(14, -1), baseline.getData().get(0));
    }

    @Test
    void ordinarySlotsCannotSilentlyConsumeForceCrashWeapons() throws Exception {
        Mek mek = character("tsukuyomi");
        Mek.MekWeaponInfo variant = mek.getMekWeaponInfoMap().get(19);
        variant.setWeaponCategory(1);
        IllegalStateException rejected = assertThrows(IllegalStateException.class, () -> PlayableWeaponLoadouts.append(baseline(), mek));
        assertTrue(rejected.getMessage().contains("分类不符"), "完整夹具须因普通槽误用FC拒绝");
    }

    @Test
    void unrelatedCharacterKeepsExistingDefaultPreset() throws Exception {
        Mek sou = new Mek();
        sou.setFileName("sou");
        Dat result = PlayableWeaponLoadouts.append(baseline(), sou);
        assertEquals(List.of(0,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1), result.getData().get(1));
    }

    private void assertDefaultRows(String role, List<String> near, List<String> search,
                                   List<String> ranged, List<String> dash, String forceCrash) throws Exception {
        Mek mek = character(role);
        Map<Integer, Mek.MekWeaponInfo> relocated = new LinkedHashMap<>();
        mek.getMekWeaponInfoMap().forEach((key, value) -> relocated.put(key + 40, value));
        mek.setMekWeaponInfoMap(relocated);

        List<Object> row = PlayableWeaponLoadouts.append(baseline(), mek).getData().get(1);

        // DX DAT按3*state+button存放；state2=遠距離，state3=ダッシュ，组内A/B/C顺序固定。
        assertAll(role,
                () -> assertEquals(near, weaponNames(mek, row, 0), "近距A/B/C必须符合批准表"),
                () -> assertEquals(search, weaponNames(mek, row, 3), "搜索冲刺A/B/C必须符合批准表"),
                () -> assertEquals(ranged, weaponNames(mek, row, 6), "远距A/B/C必须选择最终武装身份"),
                () -> assertEquals(dash, weaponNames(mek, row, 9), "普通Dash A/B/C必须选择最终武装身份"),
                () -> assertEquals(forceCrash, mek.getMekWeaponInfoMap().get((Integer) row.get(12)).getWeaponName(), "FC槽保持批准身份"),
                () -> assertEquals("イニシャライザ", mek.getMekWeaponInfoMap().get((Integer) row.get(13)).getWeaponName(), "末槽保持唯一initializer"));
    }

    private List<String> weaponNames(Mek mek, List<Object> row, int start) {
        List<String> result = new ArrayList<>();
        for (int slot = start; slot < start + 3; slot++) {
            int id = (Integer) row.get(slot);
            result.add(id == -1 ? null : mek.getMekWeaponInfoMap().get(id).getWeaponName());
        }
        return result;
    }

    private Mek character(String role) throws Exception {
        var source = (com.giga.nexas.dto.bhe.mek.Mek) new BheBinService()
                .parse("src/main/resources/game/bhe/mek/" + role + ".mek", "windows-31j").getData();
        Mek mek = new BheToBsdxMekConverter().convert(source);
        Mek.MekWeaponInfo initializer = new Mek.MekWeaponInfo();
        initializer.setWeaponName("イニシャライザ"); initializer.setWeaponCategory(2);
        mek.getMekWeaponInfoMap().put(mek.getMekWeaponInfoMap().size(), initializer);
        // Match the existing fixture's minimal MEK stubs; no Customizer or production loadout builder
        // computes their order or the expected four input-state groups above.
        for (String name : additionalWeapons(role)) {
            Mek.MekWeaponInfo variant = new Mek.MekWeaponInfo();
            variant.setWeaponName(name); variant.setWeaponCategory(0);
            if ("tsukuyomi".equals(role) && "袈裟・一閃".equals(name)) variant.setWeaponSequence("PUNCH");
            mek.getMekWeaponInfoMap().put(mek.getMekWeaponInfoMap().size(), variant);
        }
        return mek;
    }

    private List<String> additionalWeapons(String role) {
        return switch (role) {
            case "tsukuyomi" -> List.of("袈裟・一閃", "打突・留め", "逆風・返し", "一威・止刃", "銃撃・離脱", "切返・二連");
            case "yuri" -> List.of("ダガー・刻み", "アサシンダガー・速刺", "サイドステップ・地走", "パルス・三点射", "狙撃・一射離脱", "マイン・置き逃げ");
            case "freja" -> List.of("スピア・留め突き", "斬り上げ・低空", "バックキック・退避", "ジャベリン・二段投擲", "火炎弾・扇射", "スイング・双薙ぎ");
            case "nagi" -> List.of("瞬間ディフレクト", "斬上げ・接続", "盾打ち・反攻", "転移突き・追撃", "乱れ突き・三連", "単発リフレクト・解放");
            case "misaki" -> List.of("転移手・挟撃", "鉱石・二拍射", "転移手・二度招き", "血晶槍・短束", "血晶剣・帰着", "床召還・重ね置き");
            case "naoto" -> List.of("ワンツー・コンファーム", "フック・リターン", "クロー・起動突き", "ダウンネイル・収納", "マシンガン・三点射", "炸裂弾・交差連射");
            case "katou" -> List.of("トンファー・二連打", "トンファー・踏込肘", "トンファー・引き肘", "蹴崩し・追い肘", "トンファー・叩き納め", "ミサイル・二段牽制");
            case "wilhelm" -> List.of("交互砲撃・離脱", "キック・導弾追撃", "サインミサイル・二拍", "肩ミサイル・圧制", "照準砲撃・制動", "対空砲火・収束");
            case "motoki" -> List.of("集中爆撃・帰投", "ワンツー・離脱", "ドリル・確定突き", "排熱弾・即発", "ガンポッド・三連点射", "ビーム・短照射");
            default -> throw new IllegalArgumentException("未定义测试角色: " + role);
        };
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
