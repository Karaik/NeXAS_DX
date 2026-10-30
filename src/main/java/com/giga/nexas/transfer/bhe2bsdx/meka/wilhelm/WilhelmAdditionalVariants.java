package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm;

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


/** Wilhelm専用。一波／二拍の発射組を保ち、終端は最終WAZ身份から求める。 */
final class WilhelmAdditionalVariants {
    private static final List<String> NEW_NAMES = List.of("キック・導弾追撃", "サインミサイル・二拍", "照準砲撃・制動", "肩ミサイル・圧制", "対空砲火・収束");
    private static final List<String> NEW_ENGLISH = List.of("DX_WILHELM_KICK_MISSILE", "DX_WILHELM_TWO_BEAT_MISSILE", "DX_WILHELM_AIMED_CANNON_EXIT", "DX_WILHELM_SHOULDER_PRESSURE", "DX_WILHELM_AA_FINISH");
    private WilhelmAdditionalVariants() { }

    static void prepareAlternate(Waz.Skill s) { physics(s, 0); setArmor(s, 0, 0, 0); }

    private static void appendStaged(Mek mek, Waz waz) {
        var kick = donor(waz, "キック", "TACKLE03", 2, "0=13:0:7,13:8:17,13:18:20,13:21:22,13:23:26,13:27:38,13:39:43~;6=14:21:43~;10=3:0:0,3:8:8~;15=10:0:8~;18=3:8:20,3:21:30~;24=0:0:0~;26=0:0:0~;33=12:0:0~;41=16:21:38~;45=17:11:38~;47=0:21:38~;48=19:21:21,19:21:38,19:21:38,19:21:38~;52=23:0:0,23:21:21,23:21:21,23:21:21~;53=24:15:15~;70=0:0:0,0:18:29~;71=34:43:43~|0=13:0:14,13:15:16,13:17:18,13:19:20,13:21:40,13:41:42,13:43:44,13:45:46~;18=3:0:14~;21=2:0:0~;27=0:0:0~;33=12:0:0~;47=0:0:46~;48=19:15:15,19:15:15,19:15:15,19:15:15,19:15:15~;52=23:14:14,23:14:14~;71=34:46:46~");
        var sign = donor(waz, "サインミサイル", "S_MISSILE04", 1, "0=13:0:23,13:24:97,13:98:127~;6=14:24:92~;10=3:0:0,3:23:23~;24=0:0:13,0:108:127~;26=0:0:0~;27=0:10:30,0:66:103~;28=0:10:30,0:66:103~;47=0:60:60,0:61:127~;48=19:24:60~;52=23:4:4,23:24:24,23:32:32,23:40:40,23:48:48~;53=24:10:10~;70=0:0:0,0:60:60~;71=34:127:127~");
        var shoulder = donor(waz, "肩ミサイル", "S_MISSILE05", 1, "0=13:0:5,13:6:13,13:14:31,13:32:56,13:57:62,13:63:80,13:81:120,13:121:126,13:127:141,13:142:156~;6=14:24:92~;10=3:0:0,3:13:13~;24=0:0:13,0:127:156~;26=0:0:0~;27=0:10:30,0:66:103~;28=0:10:30,0:66:103~;47=0:32:32,0:33:156~;48=19:32:32,19:32:32,19:32:32,19:81:81,19:81:81,19:81:81~;52=23:5:5,23:56:56,23:32:32,23:81:81~;53=24:25:25,24:75:75~;70=0:0:0,0:32:32~;71=34:156:156~");
        var cannon = donor(waz, "キャタピラ：砲撃", "0018", 2, "0=13:0:7,13:8:15,13:16:27~;6=14:0:15,14:16:27~;10=3:0:0~;24=0:0:0~;26=0:0:0~;36=0:0:0~;52=23:0:0,23:0:0,23:15:15~;53=24:0:0~;71=34:27:27~|0=13:0:0,13:1:3,13:4:5,13:6:10,13:11:11,13:12:14,13:15:28,13:29:32,13:33:42,13:43:50,13:51:59~;6=14:0:32,14:43:48,14:51:58~;10=3:0:0~;40=0:0:0~;46=18:32:32~;47=0:11:11~;48=19:0:0,19:0:0,19:0:0,19:0:0,19:11:11,19:11:11,19:11:11,19:11:11,19:0:0,19:11:11~;52=23:0:0,23:11:11,23:33:33,23:42:42,23:50:50~;60=25:0:10~;71=34:32:32,34:59:59~");
        var aa = donor(waz, "キャタピラ：対空砲火", "0020", 1, "0=13:0:5,13:6:13,13:14:27,13:28:34,13:35:49,13:50:56,13:57:70,13:71:82,13:83:112~;6=14:14:41,14:50:82~;10=3:0:0,3:13:13~;24=0:0:0~;26=0:0:0~;45=17:0:66~;47=0:35:35~;48=19:14:14,19:21:21,19:28:28,19:35:35,19:17:17,19:24:24,19:31:31,19:38:38,19:14:14,19:21:21,19:28:28,19:35:35,19:49:49,19:56:56,19:63:63,19:45:45,19:52:52,19:59:59,19:66:66,19:50:50,19:57:57,19:64:64~;52=23:0:0,23:13:13,23:20:20,23:27:27,23:34:34,23:50:50,23:57:57,23:64:64,23:66:66~;53=24:5:5~;71=34:112:112~");
        var ending = donor(waz, "キャタピラ：終了", "TACKLE05", 1, "0=13:0:7,13:8:15,13:16:25~;6=14:0:25~;10=3:0:0~;24=0:0:0~;26=0:0:0~;52=23:0:0~;70=0:0:0,0:25:25~;71=34:25:25~");
        var endingWeapon = weapon(mek, waz, ending, "キャタピラ：終了", "0022");
        var signWeapon = weapon(mek, waz, sign, "サインミサイル", "0026");
        var shoulderWeapon = weapon(mek, waz, shoulder, "肩ミサイル", "0028");
        int group = mek.getMekBasicInfo().getWazFileSequence(), endIndex = waz.getSkillList().indexOf(ending);
        int continuous = waz.getSkillList().indexOf(skill(waz, "キャタピラ：持続", "TACKLE07", 2));
        requireNormalEnd(at(ending, 0, 71, CEventChange.class, 25, 25));
        var signEffects = effects(sign, 0); if (signEffects.size() != 1) throw invalid("サイン导弹emitter数量漂移");
        var signEmitter = signEffects.get(0);
        emitter(signEmitter, 24, 60, 36, 1, 16, -1, -1, 1, 0);
        requireSix(effectData(signEmitter, 4), 0, 1, 0, 0, 1, 0); requireSix(effectData(signEmitter, 6), 0, 5, 0, 0, 5, 0);

        append(mek, waz, kick, "キック", "0001", 0, 22,
                "キックが当たると一発の導弾で追撃する。空振りでは導弾を出さず、命中後は追撃か離脱を選べる。", s -> {
            hitPower(at(s, 0, 41, CEventHit.class, 21, 38), 21, 38, 120, 1);
            var oldExit = at(s, 0, 71, CEventChange.class, 43, 43); condition(oldExit, List.of(0), List.of()); destination(oldExit, List.of(0, 1), List.of(1));
            attackEntries(s);
            set(s, 0, 71, List.of(route(23, 39, hit(), List.of(0, 1), List.of(1)), end(43))); clip(s, 0, 43);
            var sourcePoses = sprites(shoulder, 0); List<SkillInfoObject> bridgePoses = new ArrayList<>();
            int[] chosen = {3, 4, 8, 9}; int[] times = {0, 7, 8, 11, 12, 19, 20, 24};
            for (int i = 0; i < chosen.length; i++) { var v = copy(sourcePoses.get(chosen[i]), CEventSprite.class); window(v, times[i * 2], times[i * 2 + 1]); bridgePoses.add(v); }
            s.getPhasesInfo().get(1).setSkillUnitCollection(new ArrayList<>());
            set(s, 1, 0, bridgePoses); physics(s, 1); setArmor(s, 1, 0, 0); speedZero(s, 1, 0);
            set(s, 1, 24, List.of(val(0, 0, 100))); set(s, 1, 48, List.of(singleSign(signEmitter, 0)));
            gate(s, 1, 1, 23); set(s, 1, 71, List.of(end(24)));
        }, signWeapon, shoulderWeapon);

        append(mek, waz, sign, "サインミサイル", "0026", 1, 16,
                "サイン導弾を二拍で放って打ち切る。長く撒き続けず、二発目に合わせてキックや砲撃へ移りたい。", s -> {
            requireNormalEnd(at(s, 0, 71, CEventChange.class, 127, 127)); attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2}, 0, 11, 12, 31, 32, 43);
            window(at(s, 0, 6, CEventSpriteYure.class, 24, 92), 12, 31);
            window(at(s, 0, 10, CEventValRandom.class, 23, 23), 11, 11);
            window(at(s, 0, 24, CEventVal.class, 0, 13), 0, 11);
            var recoverMove = at(s, 0, 24, CEventVal.class, 108, 127); window(recoverMove, 32, 43);
            recoverMove.setInt1(0); recoverMove.setInt2(100); recoverMove.setInt3(0); recoverMove.setInt4(100);
            var mount = at(s, 0, 52, CEventSe.class, 4, 4);
            var first = at(s, 0, 52, CEventSe.class, 24, 24); window(first, 12, 12);
            var second = at(s, 0, 52, CEventSe.class, 32, 32); window(second, 20, 20);
            set(s, 0, 52, List.of(mount, first, second));
            window(at(s, 0, 53, CEventVoice.class, 10, 10), 6, 6);
            window(at(s, 0, 70, CEventVal.class, 60, 60), 21, 21);
            set(s, 0, 48, List.of(singleSign(signEmitter, 12), singleSign(signEmitter, 20)));
            gate(s, 0, 21, 42); set(s, 0, 71, List.of(end(43))); clip(s, 0, 43);
        });

        append(mek, waz, cannon, "砲撃", "0008", 2, 24,
                "狙いを付けた二連砲撃を一度だけ放ち、四脚へ戻る。撃ち続けず、導弾の拘束から短く撃ち込む。", s -> {
            requireNormalEnd(at(s, 0, 71, CEventChange.class, 27, 27));
            requireJump(at(s, 1, 71, CEventChange.class, 59, 59), group, continuous);
            if (effects(s, 1).size() != 10) throw invalid("二连炮完整发射组数量漂移");
            for (var e : effects(s, 1)) emitter(e, e.getStartFrame(), e.getStartFrame(), 0, e.getInt1(), e.getInt3(), -1, -1, 1, 0);
            attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2}, 0, 3, 4, 7, 8, 14);
            window(at(s, 0, 6, CEventSpriteYure.class, 0, 15), 0, 7);
            window(at(s, 0, 6, CEventSpriteYure.class, 16, 27), 8, 14);
            window(at(s, 0, 52, CEventSe.class, 15, 15), 7, 7);
            speedZero(s, 0, 0); set(s, 0, 71, List.of(next(14))); clip(s, 0, 14);
            clear(s, 1, 40, 46); speedZero(s, 1, 0);
            gate(s, 1, 12, 23); set(s, 1, 71, List.of(jump(24, group, endIndex))); clip(s, 1, 24);
        }, endingWeapon);

        append(mek, waz, shoulder, "肩ミサイル", "0028", 3, 28,
                "肩の導弾を短い間隔で二発放ち、相手の動きを抑える。二発目を出してから、主砲か近接へ切り替える。", s -> {
            var e = effects(s, 0); if (e.size() != 6) throw invalid("肩导弹双批数量漂移");
            for (int i = 0; i < e.size(); i++) emitter(e.get(i), i < 3 ? 32 : 81, i < 3 ? 32 : 81, 0, i % 3 == 2 ? 1 : 0, i % 3 == 2 ? 16 : 0, -1, -1, 1, 0);
            requireNormalEnd(at(s, 0, 71, CEventChange.class, 156, 156)); attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 0, 5, 6, 11, 12, 23, 24, 31, 32, 35, 36, 39, 40, 49, 50, 55, 56, 63, 64, 72);
            window(at(s, 0, 6, CEventSpriteYure.class, 24, 92), 24, 49);
            window(at(s, 0, 10, CEventValRandom.class, 13, 13), 11, 11);
            window(at(s, 0, 24, CEventVal.class, 0, 13), 0, 11);
            var recover = at(s, 0, 24, CEventVal.class, 127, 156); window(recover, 56, 72);
            recover.setInt1(0); recover.setInt2(100); recover.setInt3(0); recover.setInt4(100);
            window(at(s, 0, 52, CEventSe.class, 32, 32), 24, 24);
            window(at(s, 0, 52, CEventSe.class, 56, 56), 32, 32);
            window(at(s, 0, 52, CEventSe.class, 81, 81), 40, 40);
            window(at(s, 0, 53, CEventVoice.class, 25, 25), 18, 18);
            window(at(s, 0, 53, CEventVoice.class, 75, 75), 34, 34);
            window(at(s, 0, 70, CEventVal.class, 32, 32), 24, 24);
            for (int i = 0; i < 6; i++) window(e.get(i), i < 3 ? 24 : 40, i < 3 ? 24 : 40);
            gate(s, 0, 41, 71); set(s, 0, 71, List.of(end(72))); clip(s, 0, 72);
        });

        append(mek, waz, aa, "対空砲火", "0010", 4, 22,
                "一波だけ対空砲火を広げ、四脚へ戻る。導弾で浮かせた相手への締めに使い、撃ち終えたら間合いを取り直す。", s -> {
            requireJump(at(s, 0, 71, CEventChange.class, 112, 112), group, continuous);
            var e = effects(s, 0); if (e.size() != 22) throw invalid("对空首/第二波发射总数漂移");
            for (int i = 0; i < e.size(); i++) {
                var v = e.get(i); boolean bullet = i < 8 || i >= 12 && i < 19;
                emitter(v, v.getStartFrame(), v.getStartFrame(), 0, bullet ? 1 : 0, bullet ? 16 : 0, -1, -1, 1, 0);
            }
            attackEntries(s);
            poses(s, 0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, 0, 5, 6, 13, 14, 27, 28, 34, 35, 38, 39, 42, 43, 46, 47, 49, 50, 52);
            set(s, 0, 48, e.stream().filter(v -> v.getStartFrame() <= 38).toList());
            var vibration = at(s, 0, 6, CEventSpriteYure.class, 14, 41); window(vibration, 14, 38); set(s, 0, 6, List.of(vibration));
            window(at(s, 0, 45, CEventEscape.class, 0, 66), 0, 38);
            unit(s, 0, 52).getSkillInfoObjectList().removeIf(v -> v.getStartFrame() >= 50);
            gate(s, 0, 39, 51); set(s, 0, 71, List.of(jump(52, group, endIndex))); clip(s, 0, 52);
        }, endingWeapon);
    }

    private static CEventEffect singleSign(CEventEffect source, int frame) {
        var e = movedEmitter(source, frame, true);
        var interval = (CEventValRandom) effectData(e, 6); interval.setInt2(0); interval.setInt5(0);
        return e;
    }

    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static void appendAll(Mek mek, Waz waz) {
        WilhelmCustomizer.validateBaseSuffixes(waz);
        Mek stagedMek = copy(mek, Mek.class); Waz stagedWaz = copy(waz, Waz.class);
        boolean existing = removeNewBatch(stagedMek, stagedWaz);
        WilhelmCustomizer.appendBase(stagedMek, stagedWaz);
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
        if (!Integer.valueOf(1).equals(c.getFlag()) || c.getBsdxInfoCollectionList1().size() != 1)
            throw invalid("条件分支数量或flag漂移");
        var v = c.getBsdxInfoCollectionList1().get(0);
        if (!Integer.valueOf(0).equals(v.getInt1()) || !types.equals(v.getTypeList()) || !params.equals(v.getParamList()))
            throw invalid("条件语义漂移");
    }
    private static void destination(CEventChange c, List<Integer> types, List<Integer> params) {
        if (c.getBsdxInfoCollectionList2().size() != 1) throw invalid("出口数量漂移");
        var d = c.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(16).equals(d.getInt1()) || !types.equals(d.getTypeList()) || !params.equals(d.getParamList()))
            throw invalid("出口语义漂移");
    }
    private static void requireJump(CEventChange c, int group, int index) {
        condition(c, List.of(0), List.of());
        if (c.getBsdxInfoCollectionList2().size() != 1) throw invalid("WAZA出口数量漂移");
        var d = c.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(16).equals(d.getInt1()) || !List.of(1).equals(d.getTypeList()) || !List.of(group, index).equals(d.getParamList()))
            throw invalid("WAZA出口身份漂移");
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
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(WilhelmAdditionalVariants::clean); }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Wilhelm新六武装输入不符: " + message); }

}
