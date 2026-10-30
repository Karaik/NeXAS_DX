package com.giga.nexas.transfer.bhe2bsdx.meka.nagi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;


/** Nagi専用の六武装バッチ。元技能と公共弾体を変更せず、全検査後に追加行だけ公開する。 */
final class NagiAdditionalVariants {
    private static final List<String> NEW_NAMES = List.of("斬上げ・接続", "乱れ突き・三連", "転移突き・追撃", "盾打ち・反攻", "単発リフレクト・解放");
    private static final List<String> NEW_ENGLISH = List.of("DX_NAGI_CLOSE_LAUNCH", "DX_NAGI_THREE_THRUST", "DX_NAGI_TRANSFER_LINK", "DX_NAGI_BASH_CONFIRM", "DX_NAGI_SINGLE_REFLECT");
    private NagiAdditionalVariants() { }

    static void releaseCounterArmor(Waz.Skill copy) {
        if (!unit(copy, 1, 36).getSkillInfoObjectList().isEmpty() || !unit(copy, 1, 36).getSkillInfoUnknownList().isEmpty())
            throw invalid("原反击成功段已有未确认的装甲事件");
        setArmor(copy, 1, 0, 0);
    }

    private static void appendStaged(Mek mek, Waz waz) {
        var launch = donor(waz, "斬り上げ", "CLOW06", 2, "0=13:0:2,13:3:7,13:8:15~;6=14:8:22~;10=3:0:0,3:8:8~;15=10:8:8~;16=3:8:8~;18=3:8:8~;24=0:0:0~;26=0:0:0~;28=0:0:0~;30=0:0:23~;33=12:0:0~;45=17:0:23~;47=0:3:23~;51=22:13:13~;52=23:8:8~;53=24:8:8~;70=0:0:0,0:11:23~;71=34:12:23,34:23:23~|0=13:0:1,13:2:6,13:7:8,13:9:11,13:12:14,13:15:17,13:18:40~;6=14:0:13~;16=3:13:13~;18=3:0:3,3:13:33~;19=3:7:9~;21=2:7:7~;27=0:7:7~;28=0:7:7~;30=0:0:12~;33=12:0:0~;41=16:7:21~;45=17:0:12~;47=0:8:46~;51=22:0:0~;52=23:0:0,23:6:6~;67=0:0:48~;70=0:0:12~;71=34:48:48~");
        var thrust = donor(waz, "乱れ突き", "0002", 4, "0=13:0:7,13:8:17,13:18:23~;6=14:10:23~;10=3:0:15~;24=0:0:0~;45=17:13:23~;67=0:0:23~;71=34:0:0,34:23:23~|0=13:0:4,13:5:10,13:11:13~;6=14:7:13~;10=3:0:13~;45=17:8:13~;67=0:0:13~;71=34:13:13~|0=13:0:0,13:1:1,13:2:2,13:3:3,13:4:4,13:5:5,13:6:6,13:7:7,13:8:8,13:9:9,13:10:10,13:11:11~255:0:11;6=14:2:11~;10=3:0:0,3:4:4~;15=10:1:1~;18=3:0:5~;35=0:0:0~;45=17:0:11~;48=19:0:0,19:5:5,19:10:10~;50=21:28:28~;52=23:0:0,23:6:6~;71=34:11:11,34:19:19,34:11:11~|0=13:0:0,13:1:22,13:23:26,13:27:51~;18=3:0:0~;21=2:0:0~;27=0:0:0~;33=12:0:0~;47=0:0:26~;52=23:0:0~;53=24:10:10~;70=0:0:0~;71=34:51:51~");
        var transfer = donor(waz, "転移突き", "WARP_P1_04", 2, "0=13:0:7,13:8:37,13:38:39,13:40:79~;6=14:40:62~;10=3:0:0,3:20:20~;24=0:0:0~;27=0:0:17~;28=0:0:17~;47=0:41:79~;48=19:40:40,19:40:40~;52=23:0:0,23:36:36~;53=24:30:30~;70=0:0:0,0:55:55~;71=34:79:79~|0=13:0:2,13:3:12,13:13:20~;52=23:0:0~;71=34:20:20~");
        var bash = donor(waz, "シールド：シールドバッシュ", "HAMMER04", 4, "52=23:0:0~;70=0:0:0~;71=34:0:0,34:0:0~|0=13:0:7,13:8:29~;6=14:8:29~;10=3:0:20~;15=10:0:20~;18=3:5:20,3:21:21~;24=0:0:0~;26=0:0:0~;45=17:15:29~;67=0:0:29~;71=34:29:29~|0=13:0:7,13:8:16~;10=3:0:0~;24=0:0:0~;26=0:0:0~;27=0:8:30~;45=17:4:105~255:4:105;50=21:0:0~255:0:0;67=0:0:105~255:0:105;71=34:8:105~255:8:105|0=13:0:2,13:3:4,13:5:6,13:7:14,13:15:17,13:18:42,13:43:52~;6=14:3:38~;10=3:0:0~;15=10:0:0~;18=3:4:7~;30=0:0:0~;41=16:4:7~;45=17:0:7~;48=19:3:3,19:3:3,19:3:3,19:3:22~;52=23:0:0,23:3:3~;53=24:0:0,24:42:42~;60=25:3:22~;70=0:7:7~;71=34:52:52~");
        var reflect = donor(waz, "シールド：単発リフレクション", "0017", 3, "0=13:0:4,13:5:98,13:99:118~;10=3:0:0~;24=0:0:0~;26=0:0:0~;36=0:5:5,0:99:99~;37=0:5:5~;47=0:79:118~;52=23:0:0,23:5:5~;70=0:0:0,0:6:6~;71=34:5:98,34:118:118~|0=13:0:0~;48=19:0:0~;71=34:0:0~|0=13:0:5,13:6:22,13:23:25,13:26:71,13:72:75,13:76:79,13:80:85,13:86:91~;6=14:0:7,14:26:61~;10=3:0:0,3:23:23~;15=10:0:0~;16=3:0:0~;18=3:0:2~;28=0:57:71~;33=12:0:0~;36=0:26:26~;47=0:45:91~;48=19:0:0,19:0:4,19:0:4~;52=23:0:0,23:24:24,23:72:72~;53=24:26:26~;70=0:0:0,0:26:26~;71=34:0:22,34:91:91~");
        var ending = donor(waz, "シールド：終了", "0015", 1, "0=13:0:5,13:6:9~;36=0:0:0~;71=34:9:9~");
        var endingWeapon = weapon(mek, waz, ending, "シールド：終了", "0016");
        var launchWeapon = weapon(mek, waz, launch, "斬り上げ", "0028");
        condition(at(ending, 0, 71, CEventChange.class, 9, 9), List.of(0), List.of());
        destination(at(ending, 0, 71, CEventChange.class, 9, 9), List.of(0, 2), List.of());
        int group = mek.getMekBasicInfo().getWazFileSequence(), endIndex = waz.getSkillList().indexOf(ending);

        append(mek, waz, launch, "斬り上げ", "0028", 0, 14,
                "近ければすぐに斬り上げ、離れていれば短く踏み込む。届かないときは打ち切り、命中から三連突きへつなぎたい。", s -> {
            var close = at(s, 0, 71, CEventChange.class, 12, 23);
            sourceBranch(close, List.of(2, 3, 6, 2), List.of(135), List.of(0, 0), List.of(1, 0), List.of(0, 0), List.of());
            var timeout = at(s, 0, 71, CEventChange.class, 23, 23); requireNormalEnd(timeout);
            hitPower(at(s, 1, 41, CEventHit.class, 7, 21), 7, 21, 60, 1);
            requireSix(at(s, 0, 18, CEventValRandom.class, 8, 8), 0, 16, 0, 0, 16, 0);
            attackEntries(s); window(close, 0, 23);
            set(s, 0, 71, List.of(close, end(24))); speedZero(s, 0, 23);
            gate(s, 1, 22, 31); set(s, 1, 71, List.of(end(32)));
            clip(s, 0, 24); clip(s, 1, 32);
        });
        int launchIndex = waz.getSkillList().indexOf(skill(waz, NEW_NAMES.get(0), NEW_ENGLISH.get(0), 2));

        append(mek, waz, thrust, "乱れ突き", "0020", 1, 12,
                "短い準備から三度だけ突きを放ち、盾を解いて終える。押し続けても連打を繰り返さず、近距離の追撃に使う。", s -> {
            var e = effects(s, 2); if (e.size() != 3) throw invalid("三刺发射批次数漂移");
            for (int i = 0; i < 3; i++) emitter(e.get(i), i * 5, i * 5, 0, 1, 0, -1, -1, 1, 0);
            int continuous = waz.getSkillList().indexOf(skill(waz, "シールド：持続", "0014", 2));
            requireJump(at(s, 3, 71, CEventChange.class, 51, 51), group, continuous);
            attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2}, 0, 1, 2, 3, 4, 5);
            poses(s, 1, new int[]{0, 1, 2}, 0, 1, 2, 3, 4, 5);
            for (int p : List.of(0, 1)) { set(s, p, 71, List.of(input(5, 2))); counter(s, p); clip(s, p, 5); }
            for (int i = 0; i < 3; i++) window(e.get(i), i * 3, i * 3);
            poses(s, 2, new int[]{0, 2, 4, 6, 8, 10, 11}, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 8);
            set(s, 2, 71, List.of(next(8))); counter(s, 2); clip(s, 2, 8);
            poses(s, 3, new int[]{0, 1, 2, 3}, 0, 0, 1, 9, 10, 13, 14, 18);
            set(s, 3, 18, List.of(six(3, 0, 0, 0, 0, 0, 0, 0, 0)));
            gate(s, 3, 0, 17); counter(s, 3); set(s, 3, 71, List.of(jump(18, group, endIndex))); clip(s, 3, 18);
        }, endingWeapon);

        append(mek, waz, transfer, "転移突き", "0035", 2, 18,
                "転移の突きを早めに放ち、短く構え直す。斬上げや三連突きの追撃に使い、弾の命中を見て次の攻めを選ぶ。", s -> {
            var e = effects(s, 0); if (e.size() != 2) throw invalid("転移発射組漂移");
            emitter(e.get(0), 40, 40, 0, 1, 0, -1, -1, 1, 0); emitter(e.get(1), 40, 40, 0, 0, 0, -1, -1, 1, 0);
            requireNormalEnd(at(s, 0, 71, CEventChange.class, 79, 79)); requireNormalEnd(at(s, 1, 71, CEventChange.class, 20, 20));
            attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2, 3}, 0, 3, 4, 11, 12, 13, 14, 39);
            window(at(s, 0, 6, CEventSpriteYure.class, 40, 62), 14, 36);
            window(at(s, 0, 10, CEventValRandom.class, 20, 20), 8, 8);
            window(at(s, 0, 52, CEventSe.class, 36, 36), 10, 10);
            window(at(s, 0, 53, CEventVoice.class, 30, 30), 4, 4);
            window(at(s, 0, 70, CEventVal.class, 55, 55), 29, 29);
            for (var v : e) window(v, 14, 14);
            gate(s, 0, 32, 38); set(s, 0, 71, List.of(next(39))); clip(s, 0, 39);
            poses(s, 1, new int[]{0, 1, 2}, 0, 2, 3, 12, 13, 16);
            set(s, 1, 71, List.of(end(16))); clip(s, 1, 16);
        });

        append(mek, waz, bash, "シールド：シールドバッシュ", "0004", 3, 28,
                "盾で踏み込み、命中したときだけ斬上げへつなぐ。空中では着地を待ち、着地できなければ攻撃せず盾を解く。", s -> {
            var groundRoutes = unit(s, 0, 71).getSkillInfoObjectList().stream().map(e -> (CEventChange) e).toList();
            var ground = groundRoutes.stream().filter(c -> List.of(2, 7, 16).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())).toList();
            if (ground.size() != 1 || groundRoutes.size() != 2) throw invalid("盾打地面分支漂移");
            sourceBranch(ground.get(0), List.of(2, 7, 16), List.of(), List.of(0, 0), List.of(), List.of(0, 0), List.of());
            var land = at(s, 2, 71, CEventChange.class, 8, 105);
            sourceBranch(land, List.of(2, 7, 17), List.of(), List.of(0, 0), List.of(), List.of(0, 0), List.of());
            hitPower(at(s, 3, 41, CEventHit.class, 4, 7), 4, 7, 200, 1);
            requireSix(at(s, 1, 18, CEventValRandom.class, 5, 20), 0, 0, 0, 0, 24, 0);
            attackEntries(s);
            set(s, 0, 71, List.of(ground.get(0), input(0, 2)));
            poses(s, 1, new int[]{0, 1}, 0, 7, 8, 14);
            window(at(s, 1, 18, CEventValRandom.class, 5, 20), 5, 12);
            var stop = at(s, 1, 18, CEventValRandom.class, 21, 21); window(stop, 13, 13);
            set(s, 1, 71, List.of(input(14, 3))); clip(s, 1, 14);
            poses(s, 2, new int[]{0, 1}, 0, 7, 8, 91);
            set(s, 2, 19, List.of(six(3, 0, 0, 0, 0, 0, 0, 0, 0)));
            window(land, 8, 90); set(s, 2, 71, List.of(land, jump(91, group, endIndex))); clip(s, 2, 91);
            poses(s, 3, new int[]{0, 1, 2, 3, 4, 5, 6}, 0, 2, 3, 4, 5, 6, 7, 14, 15, 17, 18, 21, 22, 24);
            set(s, 3, 71, List.of(route(8, 9, hit(), List.of(1), List.of(group, launchIndex)), jump(24, group, endIndex)));
            gate(s, 3, 10, 23); clip(s, 3, 24);
        }, launchWeapon, endingWeapon);

        append(mek, waz, reflect, "シールド：単発リフレクション", "0036", 4, 16,
                "射撃を受けたとき一度だけ反射を試み、盾を解いて攻めへ戻る。反射できる弾の種類は元の単発反射と同じ。", s -> {
            var guard = at(s, 0, 71, CEventChange.class, 5, 98);
            sourceBranch(guard, List.of(2, 6, 6), List.of(), List.of(0, 0), List.of(), List.of(0, 0), List.of());
            CEventVal on = at(s, 0, 36, CEventVal.class, 5, 5), off = at(s, 0, 36, CEventVal.class, 99, 99);
            if (!List.of(0, 0, 0, 6).equals(Arrays.asList(on.getInt1(), on.getInt2(), on.getInt3(), on.getInt4()))
                    || !List.of(0, 0, 0, 0).equals(Arrays.asList(off.getInt1(), off.getInt2(), off.getInt3(), off.getInt4())))
                throw invalid("单反装甲6或清理值漂移");
            var e = effects(s, 1); if (e.size() != 1) throw invalid("单反链接发射数量漂移");
            emitter(e.get(0), 0, 0, 0, 1, 0, -1, -1, 1, 0);
            noCharge(s); for (int p = 0; p < 3; p++) physics(s, p);
            poses(s, 0, new int[]{0, 1, 2}, 0, 4, 5, 17, 18, 32);
            window(guard, 5, 17); window(off, 18, 18);
            var entryArmor = val(0, 0, 0);
            set(s, 0, 36, List.of(entryArmor, on, off));
            set(s, 0, 71, List.of(guard, jump(32, group, endIndex))); gate(s, 0, 19, 31); clip(s, 0, 32);
            poses(s, 2, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, 0, 5, 6, 13, 14, 15, 16, 20, 21, 22, 23, 24, 25, 27, 28, 30);
            window(at(s, 2, 6, CEventSpriteYure.class, 26, 61), 16, 20);
            window(at(s, 2, 10, CEventValRandom.class, 23, 23), 14, 14);
            window(at(s, 2, 52, CEventSe.class, 24, 24), 13, 13);
            window(at(s, 2, 52, CEventSe.class, 72, 72), 21, 21);
            window(at(s, 2, 53, CEventVoice.class, 26, 26), 14, 14);
            window(at(s, 2, 70, CEventVal.class, 26, 26), 14, 14);
            setArmor(s, 2, 14, 0); gate(s, 2, 15, 29);
            set(s, 2, 71, List.of(jump(30, group, endIndex))); clip(s, 2, 30);
        }, endingWeapon);
    }

    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static void appendAll(Mek mek, Waz waz) {
        NagiCustomizer.validateBaseSuffixes(waz);
        var closeSource = skill(waz, "斬り上げ", "CLOW06", 2);
        sourceBranch(at(closeSource, 0, 71, CEventChange.class, 12, 23),
                List.of(2, 3, 6, 2), List.of(135), List.of(0, 0), List.of(1, 0), List.of(0, 0), List.of());
        Mek stagedMek = copy(mek, Mek.class); Waz stagedWaz = copy(waz, Waz.class);
        boolean existing = removeNewBatch(stagedMek, stagedWaz);
        NagiCustomizer.appendBase(stagedMek, stagedWaz);
        appendStaged(stagedMek, stagedWaz);
        if (existing) {
            if (!payload(mek).equals(payload(stagedMek)) || !payload(waz).equals(payload(stagedWaz)))
                throw invalid("既有六武装、私有技能或Material载荷已漂移");
            return;
        }
        int oldWeapons = mek.getMekWeaponInfoMap().size(), oldSkills = waz.getSkillList().size();
        int added = stagedMek.getMekWeaponInfoMap().size() - oldWeapons;
        int extraSkills = stagedWaz.getSkillList().size() - oldSkills;
        if ((added != 5 && added != 6) || extraSkills != added)
            throw invalid("完整六武装追加数量与私有技能不对应");
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(mek.getMekWeaponInfoMap());
        for (int i = oldWeapons; i < stagedMek.getMekWeaponInfoMap().size(); i++) weapons.put(i, stagedMek.getMekWeaponInfoMap().get(i));
        List<Waz.Skill> skills = new ArrayList<>(waz.getSkillList());
        skills.addAll(stagedWaz.getSkillList().subList(oldSkills, stagedWaz.getSkillList().size()));
        var block = mek.getMekMaterialBlock();
        var regular = new ArrayList<>(block.getRegularEntries()); var trailing = new ArrayList<>(block.getTrailingEntries());
        trailing.addAll(stagedMek.getMekMaterialBlock().getTrailingEntries().subList(oldWeapons, stagedMek.getMekWeaponInfoMap().size()));
        var entries = new ArrayList<>(regular); entries.addAll(trailing);
        // All validation, copying and list construction has finished. Preserve every original row/object.
        mek.setMekWeaponInfoMap(weapons); waz.setSkillList(skills);
        block.setRegularEntries(regular); block.setTrailingEntries(trailing); block.setEntries(entries);
    }

    private static boolean removeNewBatch(Mek mek, Waz waz) {
        long names = mek.getMekWeaponInfoMap().values().stream().filter(w -> NEW_NAMES.contains(w.getWeaponName())).count();
        long identities = waz.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> s.getSkillNameJapanese() != null && NEW_NAMES.contains(s.getSkillNameJapanese())
                        || s.getSkillNameEnglish() != null && NEW_ENGLISH.contains(s.getSkillNameEnglish())).count();
        if (names == 0 && identities == 0) return false;
        if (names != 5 || identities != 5) throw invalid("新增五武装身份缺失、重复或只存在一半");
        int firstWeapon = mek.getMekWeaponInfoMap().size() - 5, firstSkill = waz.getSkillList().size() - 5;
        var b = mek.getMekMaterialBlock();
        if (firstWeapon < 0 || firstSkill < 0 || b == null || b.getTrailingEntries().size() != mek.getMekWeaponInfoMap().size())
            throw invalid("新增五武装尾表或Material布局不完整");
        var combined = new ArrayList<>(b.getRegularEntries()); combined.addAll(b.getTrailingEntries());
        if (!payload(combined).equals(payload(b.getEntries()))) throw invalid("Material三视图漂移");
        for (int i = 0; i < 5; i++) {
            var w = mek.getMekWeaponInfoMap().get(firstWeapon + i); var s = waz.getSkillList().get(firstSkill + i);
            if (w == null || !NEW_NAMES.get(i).equals(w.getWeaponName()) || !Integer.valueOf(firstSkill + i).equals(w.getWazSequence())
                    || !NEW_NAMES.get(i).equals(s.getSkillNameJapanese()) || !NEW_ENGLISH.get(i).equals(s.getSkillNameEnglish()))
                throw invalid("新增五武装必须保持完整双名、引用和尾部顺序");
        }
        for (int i = 0; i < 5; i++) mek.getMekWeaponInfoMap().remove(firstWeapon + i);
        waz.getSkillList().subList(firstSkill, firstSkill + 5).clear();
        b.getTrailingEntries().subList(firstWeapon, firstWeapon + 5).clear();
        b.getEntries().subList(b.getRegularEntries().size() + firstWeapon, b.getEntries().size()).clear();
        return true;
    }

    private static Waz.Skill donor(Waz waz, String jp, String en, int phases, String expectedShape) {
        Waz.Skill s = skill(waz, jp, en, phases);
        if (!Integer.valueOf(phases).equals(s.getPhaseQuantity()) || s.getSkillSuffixList().size() != phases)
            throw invalid(jp + " phase/suffix数量漂移");
        for (int i = 0; i < phases; i++) if (!Integer.valueOf(i).equals(s.getSkillSuffixList().get(i).getInt1())
                || !Integer.valueOf(0).equals(s.getSkillSuffixList().get(i).getInt2())) throw invalid(jp + " suffix入口漂移");
        if (!expectedShape.equals(shape(s))) throw invalid(jp + " 原事件类型、帧窗或未知循环记录漂移");
        return s;
    }
    /** Resource-independent shape signature: final group/skill/SPM numbers remain late-bound. */
    private static String shape(Waz.Skill s) {
        List<String> phases = new ArrayList<>();
        for (var p : s.getPhasesInfo()) {
            List<String> units = new ArrayList<>();
            for (var u : p.getSkillUnitCollection().stream().sorted(Comparator.comparing(SkillUnit::getUnitQuantity)).toList())
                if (!u.getSkillInfoObjectList().isEmpty() || !u.getSkillInfoUnknownList().isEmpty())
                    units.add(u.getUnitQuantity() + "=" + eventShape(u.getSkillInfoObjectList()) + "~" + eventShape(u.getSkillInfoUnknownList()));
            phases.add(String.join(";", units));
        }
        return String.join("|", phases);
    }
    private static String eventShape(List<? extends SkillInfoObject> events) {
        return String.join(",", events.stream().map(e -> e.getTypeId() + ":" + e.getStartFrame() + ":" + e.getEndFrame()).toList());
    }
    private static void append(Mek mek, Waz waz, Waz.Skill source, String sourceName, String sequence,
                               int variant, int heat, String description, Consumer<Waz.Skill> editor, Mek.MekWeaponInfo... materials) {
        CharacterWeaponVariants.append(mek, waz, weapon(mek, waz, source, sourceName, sequence), source,
                NEW_NAMES.get(variant), NEW_ENGLISH.get(variant), heat, description, editor, materials);
    }
    private static <T extends SkillInfoObject> T at(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) {
        return event(s, p + 1, slot, type, start, end, start, end);
    }
    private static SkillUnit unit(Waz.Skill s, int p, int slot) {
        var units = s.getPhasesInfo().get(p).getSkillUnitCollection();
        var found = units.stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        if (found.size() > 1) throw invalid("槽位重复");
        if (!found.isEmpty()) return found.get(0);
        SkillUnit u = new SkillUnit(slot, "追加武装専用"); units.add(u); units.sort(Comparator.comparing(SkillUnit::getUnitQuantity)); return u;
    }
    private static void set(Waz.Skill s, int p, int slot, List<? extends SkillInfoObject> events) {
        SkillUnit u = unit(s, p, slot); u.setSkillInfoObjectList(new ArrayList<>(events)); u.setSkillInfoUnknownList(new ArrayList<>());
    }
    private static void clear(Waz.Skill s, int p, int... slots) { for (int slot : slots) s.getPhasesInfo().get(p).getSkillUnitCollection().removeIf(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())); }
    private static List<CEventEffect> effects(Waz.Skill s, int p) { return unit(s, p, 48).getSkillInfoObjectList().stream().map(e -> {
        if (!(e instanceof CEventEffect v)) throw invalid("Effect类型漂移"); return v;
    }).toList(); }
    private static List<CEventSprite> sprites(Waz.Skill s, int p) { return unit(s, p, 0).getSkillInfoObjectList().stream().map(e -> {
        if (!(e instanceof CEventSprite v)) throw invalid("Sprite类型漂移"); return v;
    }).toList(); }
    private static void window(SkillInfoObject e, int start, int end) { e.setStartFrame(start); e.setEndFrame(end); }
    private static void poses(Waz.Skill s, int p, int[] indices, int... windows) {
        var source = sprites(s, p); List<SkillInfoObject> result = new ArrayList<>();
        if (windows.length != indices.length * 2) throw invalid("姿势时间表不完整");
        for (int i = 0; i < indices.length; i++) { var v = copy(source.get(indices[i]), CEventSprite.class); window(v, windows[i * 2], windows[i * 2 + 1]); result.add(v); }
        set(s, p, 0, result);
    }
    private static void clip(Waz.Skill s, int p, int last) {
        for (var u : s.getPhasesInfo().get(p).getSkillUnitCollection()) {
            u.setSkillInfoUnknownList(new ArrayList<>());
            u.getSkillInfoObjectList().removeIf(e -> e.getStartFrame() > last);
            for (var e : u.getSkillInfoObjectList()) if (e.getEndFrame() > last) {
                if (e instanceof CEventEffect) throw invalid("多帧Effect必须显式校准内层时间");
                e.setEndFrame(last);
            }
        }
        // Only new private copies call clip: remove units emptied by this batch's Charge/loop removal or timing edit.
        s.getPhasesInfo().get(p).getSkillUnitCollection().removeIf(u ->
                u.getSkillInfoObjectList().isEmpty() && u.getSkillInfoUnknownList().isEmpty());
    }
    private static void noCharge(Waz.Skill s) {
        for (var p : s.getPhasesInfo()) for (var u : p.getSkillUnitCollection()) {
            u.getSkillInfoObjectList().removeIf(CEventCharge.class::isInstance); u.setSkillInfoUnknownList(new ArrayList<>());
        }
    }
    private static CEventVal val(int frame, int end, int value) { var v = new CEventVal(0); window(v, frame, end); v.setInt1(0); v.setInt2(value); v.setInt3(0); v.setInt4(value); return v; }
    private static CEventValRandom six(int type, int start, int end, int a, int b, int c, int d, int e, int f) {
        var v = new CEventValRandom(type); window(v, start, end); v.setInt1(a); v.setInt2(b); v.setInt3(c); v.setInt4(d); v.setInt5(e); v.setInt6(f); return v;
    }
    private static void physics(Waz.Skill s, int p) {
        set(s, p, 21, List.of(six(2, 0, 0, 0, 50, 0, 0, 50, 0)));
        set(s, p, 27, List.of(val(0, 0, 100))); set(s, p, 28, List.of(val(0, 0, 100)));
    }
    private static void attackEntries(Waz.Skill s) {
        noCharge(s);
        for (int p = 0; p < s.getPhasesInfo().size(); p++) { physics(s, p); setArmor(s, p, 0, 0); }
    }
    private static void setArmor(Waz.Skill s, int p, int frame, int value) {
        var v = val(frame, frame, value); v.setInt2(0); set(s, p, 36, List.of(v));
    }
    private static void counter(Waz.Skill s, int p) { set(s, p, 35, List.of(val(0, 0, 0))); }
    private static void gate(Waz.Skill s, int p, int frame, int sdLast) {
        List<SkillInfoObject> result = new ArrayList<>(); result.add(val(frame, frame, 1));
        if (sdLast > frame) result.add(val(frame + 1, sdLast, 3)); set(s, p, 47, result);
    }
    private static void speedZero(Waz.Skill s, int p, int frame) { var v = six(3, frame, frame, 0, 0, 0, 0, 0, 0); unit(s, p, 18).getSkillInfoObjectList().add(v); }
    private static BsdxInfoCollection term(int root, List<Integer> types, List<Integer> params, List<Integer> objects) {
        var v = new BsdxInfoCollection(); v.setInt1(root); v.setInt2(0); v.setTypeList(new ArrayList<>(types)); v.setParamList(new ArrayList<>(params)); v.setIntList3(new ArrayList<>(objects)); return v;
    }
    private static CEventChange route(int start, int end, BsdxInfoCollection condition, List<Integer> types, List<Integer> params) {
        var c = new CEventChange(34); window(c, start, end); c.setFlag(1); c.setInt1(0);
        c.setBsdxInfoCollectionList1(new ArrayList<>(List.of(condition))); c.setBsdxInfoCollectionList2(new ArrayList<>(List.of(term(16, types, params, List.of())))); return c;
    }
    private static BsdxInfoCollection always() { return term(0, List.of(0), List.of(), List.of()); }
    private static BsdxInfoCollection hit() { return term(0, List.of(2, 6, 0, 0), List.of(0), List.of(0, 0)); }
    private static CEventChange next(int frame) { return route(frame, frame, always(), List.of(0, 0), List.of()); }
    private static CEventChange end(int frame) { return route(frame, frame, always(), List.of(0, 2), List.of()); }
    private static CEventChange input(int frame, int entry) { return route(frame, frame, always(), List.of(0, 1), List.of(entry)); }
    private static CEventChange jump(int frame, int group, int index) { return route(frame, frame, always(), List.of(1), List.of(group, index)); }
    private static void condition(CEventChange c, List<Integer> types, List<Integer> params) {
        if (!Integer.valueOf(1).equals(c.getFlag()) || !Integer.valueOf(0).equals(c.getInt1())
                || c.getBsdxInfoCollectionList1() == null || c.getBsdxInfoCollectionList1().size() != 1)
            throw invalid("条件分支数量或flag漂移");
        var v = c.getBsdxInfoCollectionList1().get(0);
        if (!collection(v, 0, types, params, List.of(), List.of()))
            throw invalid("条件语义漂移");
    }
    private static void destination(CEventChange c, List<Integer> types, List<Integer> params) {
        if (c.getBsdxInfoCollectionList2() == null || c.getBsdxInfoCollectionList2().size() != 1) throw invalid("出口数量漂移");
        var d = c.getBsdxInfoCollectionList2().get(0);
        if (!collection(d, 16, types, params, List.of(), List.of()))
            throw invalid("出口语义漂移");
    }
    private static void sourceBranch(CEventChange c, List<Integer> conditionTypes, List<Integer> params,
                                     List<Integer> objects, List<Integer> auxiliary,
                                     List<Integer> targetTypes, List<Integer> targetParams) {
        if (c == null || !Integer.valueOf(1).equals(c.getFlag()) || !Integer.valueOf(0).equals(c.getInt1())
                || c.getBsdxInfoCollectionList1() == null || c.getBsdxInfoCollectionList1().size() != 1
                || !collection(c.getBsdxInfoCollectionList1().get(0), 0, conditionTypes, params, objects, auxiliary))
            throw invalid("原条件完整对象/aux/mode签名漂移");
        destination(c, targetTypes, targetParams);
    }
    private static boolean collection(BsdxInfoCollection c, int root, List<Integer> types, List<Integer> params,
                                      List<Integer> objects, List<Integer> auxiliary) {
        return c != null && Integer.valueOf(root).equals(c.getInt1()) && Integer.valueOf(0).equals(c.getInt2())
                && types.equals(c.getTypeList()) && params.equals(c.getParamList())
                && objects.equals(c.getIntList3()) && auxiliary.equals(c.getIntList4());
    }
    private static void requireJump(CEventChange c, int group, int index) {
        condition(c, List.of(0), List.of());
        destination(c, List.of(1), List.of(group, index));
    }
    private static void hitPower(CEventHit h, int start, int end, int power, int cancel) {
        attack(h, start, end, power, start, end, power);
        if (!Integer.valueOf(cancel).equals(h.getSelfStunFrame())) throw invalid("Hit int23取消旗漂移");
    }
    private static SkillInfoObject effectData(CEventEffect e, int slot) {
        var found = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && Integer.valueOf(1).equals(u.getBuffer())).toList();
        if (found.size() != 1 || found.get(0).getData() == null) throw invalid("Effect内层槽位漂移"); return found.get(0).getData();
    }
    private static void emitter(CEventEffect e, int start, int end, int innerEnd, int int1, int flags, int lifespan, int int6, int rate, int direction) {
        if (!frames(e, start, end) || !List.of(int1, 0, flags, 0, lifespan, int6, -1, 0, rate, 0, direction).equals(Arrays.asList(
                e.getInt1(), e.getInt2(), e.getInt3(), e.getInt4(), e.getInt5(), e.getInt6(), e.getInt7(), e.getInt8(), e.getInt9(), e.getInt10(), e.getInt11())))
            throw invalid("Effect外层发射合同漂移");
        for (var u : e.getCeventEffectUnitList()) if (!Integer.valueOf(1).equals(u.getBuffer()) || u.getData() == null || !frames(u.getData(), 0, innerEnd))
            throw invalid("Effect内层发射时间漂移");
        if (!(effectData(e, 3) instanceof CEventWazaSelect r) || r.getWazFileNo() < 0 || r.getWazSequenceNo() < 0) throw invalid("Effect资源引用类型漂移");
    }
    private static void requireSix(SkillInfoObject event, int... values) {
        if (!(event instanceof CEventValRandom v) || !Arrays.stream(values).boxed().toList().equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6())))
            throw invalid("六整数参数漂移");
    }
    private static CEventEffect movedEmitter(CEventEffect source, int frame, boolean single) {
        var e = copy(source, CEventEffect.class); window(e, frame, frame);
        if (single) for (var u : e.getCeventEffectUnitList()) window(u.getData(), 0, 0);
        return e;
    }
    private static void limitedRecovery(Waz.Skill s, int p, int frames, int nextEntry) {
        var pose = copy(sprites(s, p).get(0), CEventSprite.class); window(pose, 0, frames);
        s.getPhasesInfo().get(p).setSkillUnitCollection(new ArrayList<>());
        set(s, p, 0, List.of(pose)); physics(s, p); setArmor(s, p, 0, 0); counter(s, p); speedZero(s, p, 0);
        set(s, p, 71, List.of(nextEntry < 0 ? end(frames) : input(frames, nextEntry)));
    }
    private static <T> T copy(T value, Class<T> type) { try { return JSON.readValue(JSON.writeValueAsBytes(value), type); } catch (IOException e) { throw new IllegalStateException("追加武装DTO复制失败", e); } }
    private static JsonNode payload(Object value) { JsonNode n = JSON.valueToTree(value); clean(n); return n; }
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(NagiAdditionalVariants::clean); }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Nagi新六武装输入不符: " + message); }

}
