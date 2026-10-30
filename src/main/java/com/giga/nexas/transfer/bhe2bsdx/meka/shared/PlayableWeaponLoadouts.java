package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 在现有WeaponEquip输出处选择最终MEK中的武装，不改变武装编号或装备协议。 */
public final class PlayableWeaponLoadouts {
    private PlayableWeaponLoadouts() { }

    public static Dat append(Dat baseline, Mek mek) {
        if (baseline == null || baseline.getData() == null) return null;
        int width = baseline.getColumnTypes() != null && !baseline.getColumnTypes().isEmpty()
                ? baseline.getColumnTypes().size() : baseline.getColumnCount();
        if (width <= 0) throw new IllegalStateException("无法确定 WeaponEquip.dat 的列数");
        List<Object> row = new ArrayList<>(Collections.nCopies(width, -1));
        List<String> weapons = weapons(mek == null ? null : mek.getFileName());
        if (weapons == null) {
            row.set(0, 0); // 未参与本轮重做的角色保留既有默认行为。
        } else {
            if (width != 14) throw new IllegalStateException("可玩预设要求DX原生14槽装备布局");
            for (int slot = 0; slot < 13; slot++) {
                String name = weapons.get(slot);
                if (name != null) row.set(slot, weaponId(mek, name, slot == 12 ? 1 : 0));
            }
            row.set(13, initializerId(mek));
        }
        Dat result = new Dat();
        result.setFileName(baseline.getFileName());
        result.setExtensionName(baseline.getExtensionName());
        result.setColumnCount(baseline.getColumnCount());
        result.setColumnTypes(baseline.getColumnTypes() == null
                ? new ArrayList<>() : new ArrayList<>(baseline.getColumnTypes()));
        for (List<Object> inherited : baseline.getData()) {
            result.addRow(inherited == null ? new ArrayList<>() : new ArrayList<>(inherited));
        }
        result.addRow(row);
        return result;
    }

    private static int weaponId(Mek mek, String name, int category) {
        if (mek.getMekWeaponInfoMap() == null) throw new IllegalStateException("初始装备缺少最终MEK武装表");
        List<Map.Entry<Integer, Mek.MekWeaponInfo>> matches = mek.getMekWeaponInfoMap().entrySet().stream()
                .filter(entry -> entry.getValue() != null && name.equals(entry.getValue().getWeaponName())).toList();
        if (matches.size() != 1) throw new IllegalStateException("初始装备身份不唯一或缺失: " + name);
        var entry = matches.get(0);
        if (!Integer.valueOf(category).equals(entry.getValue().getWeaponCategory())) {
            throw new IllegalStateException("初始装备分类不符: " + name);
        }
        return checkedId(entry.getKey());
    }

    private static int initializerId(Mek mek) {
        List<Integer> matches = mek.getMekWeaponInfoMap().entrySet().stream()
                .filter(entry -> entry.getValue() != null
                        && Integer.valueOf(2).equals(entry.getValue().getWeaponCategory()))
                .map(Map.Entry::getKey).toList();
        if (matches.size() != 1) throw new IllegalStateException("初始装备要求唯一initializer");
        return checkedId(matches.get(0));
    }

    private static int checkedId(Integer id) {
        if (id == null || id < 0 || id > 152) {
            throw new IllegalStateException("初始装备武装号超出玩家bank 0..152: " + id);
        }
        return id;
    }

    private static List<String> weapons(String fileName) {
        if (fileName == null) return null;
        String role = fileName.toLowerCase(Locale.ROOT).replaceFirst("\\.mek$", "");
        // DAT按3*state+button存放，button为A/B/C=0/1/2：
        // state0近距離、1サーチダッシュ、2遠距離、3ダッシュ；FC槽12、initializer槽13。
        return switch (role) {
            case "tsukuyomi" -> List.of("袈裟・一閃", "打突・留め", "袈裟",
                    "抜刀", "一威・止刃", "逆風・返し",
                    "銃撃・離脱", "銀鈴", "疾風",
                    "切返・二連", "唐竹", "納刀",
                    "穿心");
            case "yuri" -> List.of("ダガー・刻み", "アサシンダガー・速刺", "サイドステップ・地走",
                    "アサシンダガー", "サイドステップキック", "バスターブリッジ",
                    "パルス・三点射", "狙撃・一射離脱", "マインスイーパ：攻",
                    "マイン・置き逃げ", "スモークディスチャージャー", "サマーソルト",
                    "リベリオンホーク");
            case "freja" -> List.of("スピア・留め突き", "斬り上げ・低空", "バックキック・退避",
                    "ラッシュ", "スピア", "叩き付け",
                    "ジャベリン・二段投擲", "火炎弾・扇射", "必殺ジャベリン",
                    "スイング・双薙ぎ", "踏み付け", "火炎放射ビット",
                    "武幻剛乱舞");
            case "nagi" -> List.of("瞬間ディフレクト", "斬上げ・接続", "乱れ突き・三連",
                    "転移突き・追撃", "シールド：開始", "シールド：終了",
                    "転移ケイジブレイド", "シールド：多発リフレクション", "防壁展開",
                    "盾打ち・反攻", "シールド：転移", "単発リフレクト・解放",
                    "エクスカリバー");
            case "misaki" -> List.of("鉱石・二拍射", "転移手・挟撃", "掴む",
                    "上昇体当たり", "転移手・二度招き", "血晶剣・帰着",
                    "鉱石発射", "血晶槍・短束", "床召還・重ね置き",
                    "転移手：サーチビターン", "抱きしめる", "あたためる",
                    "運命の赤い糸");
            case "naoto" -> List.of("ジャブ", "ワンツー・コンファーム", "ストレートパンチ",
                    "クロー展開", "クロー・起動突き", "ダウンネイル・収納",
                    "マシンガン・三点射", "炸裂弾・交差連射", "兆弾針",
                    "フック・リターン", "Ｗアームハンマー", "クロー収納",
                    "ネイルバイティング");
            case "katou" -> List.of("トンファーアッパー", "トンファー・二連打", "トンファー・引き肘",
                    "トンファー構え", "トンファー・踏込肘", "トンファー・叩き納め",
                    "キャノン砲", "ミサイル・二段牽制", "手榴弾",
                    "蹴崩し・追い肘", "トンファーエルボー", "トンファー収納",
                    "グランドスラム");
            case "wilhelm" -> List.of("キック・導弾追撃", "ミサイルアサルト", "キック",
                    "キャタピラ：開始", "キャタピラ：前進", "キャタピラ：終了",
                    "サインミサイル・二拍", "肩ミサイル・圧制", "照準砲撃・制動",
                    "交互砲撃・離脱", "対空砲火・収束", "冷却ビット",
                    "瀧本流導弾格闘術");
            case "motoki" -> List.of("ワンツー・離脱", "ドリル・確定突き", "排熱弾・即発",
                    "飛行形態：開始", "飛行形態：ソニックブーム", "集中爆撃・帰投",
                    "ガンポッド・三連点射", "妨害ミサイル", "ビーム",
                    "飛行形態：終了", "ビーム・短照射", "ハリケーン",
                    "竜巻");
            default -> null;
        };
    }
}
