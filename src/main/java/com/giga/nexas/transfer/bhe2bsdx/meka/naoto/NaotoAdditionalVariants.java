package com.giga.nexas.transfer.bhe2bsdx.meka.naoto;

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

/** 私有Riseと五武装を一つのNaoto専用バッチとして検査し、新規行だけ公開する。 */
final class NaotoAdditionalVariants {
    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String FOLLOWUP_JP = "クロー・起動突き：追撃", FOLLOWUP_EN = "DX_CLAW_DEPLOY_RISE_FOLLOWUP";

    private NaotoAdditionalVariants() { }

    static void append(Mek mek, Waz waz) {
        Mek stagedMek = copy(mek, Mek.class); Waz stagedWaz = copy(waz, Waz.class);
        appendStaged(stagedMek, stagedWaz);
        int oldWeapons = mek.getMekWeaponInfoMap().size(), oldSkills = waz.getSkillList().size();
        int added = stagedMek.getMekWeaponInfoMap().size() - oldWeapons, extraSkills = stagedWaz.getSkillList().size() - oldSkills;
        if ((added != 0 && added != 5) || extraSkills != (added == 0 ? 0 : 6)) throw invalid("主武装と私有Riseの追加件数が対応しない");
        if (added == 0) return;
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(mek.getMekWeaponInfoMap());
        for (int i = oldWeapons; i < oldWeapons + added; i++) weapons.put(i, stagedMek.getMekWeaponInfoMap().get(i));
        List<Waz.Skill> skills = new ArrayList<>(waz.getSkillList()); skills.addAll(stagedWaz.getSkillList().subList(oldSkills, oldSkills + extraSkills));
        var material = copy(mek.getMekMaterialBlock(), Mek.MekMaterialBlock.class);
        var regular = new ArrayList<>(mek.getMekMaterialBlock().getRegularEntries()); var trailing = new ArrayList<>(mek.getMekMaterialBlock().getTrailingEntries());
        trailing.addAll(stagedMek.getMekMaterialBlock().getTrailingEntries().subList(oldWeapons, oldWeapons + added));
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>(regular); entries.addAll(trailing);
        material.setRegularEntries(regular); material.setTrailingEntries(trailing); material.setEntries(entries);
        mek.setMekWeaponInfoMap(weapons); mek.setMekMaterialBlock(material); waz.setSkillList(skills);
    }

    private static void appendStaged(Mek mek, Waz waz) {
        Waz.Skill hook = donor(waz, "ステップインフック", "STRAIGHT05", 1);
        Waz.Skill deploy = donor(waz, "クロー：展開", "STRAIGHT10", 2);
        Waz.Skill rise = donor(waz, "クロー：ライズネイル", "STRAIGHT08", 1);
        Waz.Skill down = donor(waz, "クロー：ダウンネイル", "STRAIGHT09", 1);
        Waz.Skill machinegun = donor(waz, "マシンガン", "GATRING4", 3);
        Waz.Skill explosive = donor(waz, "炸裂弾", "0004", 3);
        Waz.Skill stand = donor(waz, "クロー：立ち", "0018", 1);
        Waz.Skill storage = donor(waz, "クロー：収納", "STRAIGHT11", 1);
        int group = mek.getMekBasicInfo().getWazFileSequence(), standIndex = waz.getSkillList().indexOf(stand), storageIndex = waz.getSkillList().indexOf(storage);
        var riseWeapon = weapon(mek, waz, rise, "ライズネイル", "0008");
        var storageWeapon = weapon(mek, waz, storage, "クロー収納", "0016");
        requireNormalEnd(at(storage, 0, 71, CEventChange.class, 23, 23));
        // A half-present or duplicate pair is never completed by silently making a new helper.
        var existing = waz.getSkillList().stream().filter(Objects::nonNull).filter(s -> FOLLOWUP_JP.equals(s.getSkillNameJapanese()) || FOLLOWUP_EN.equals(s.getSkillNameEnglish())).toList();
        long mains = waz.getSkillList().stream().filter(Objects::nonNull).filter(s -> "クロー・起動突き".equals(s.getSkillNameJapanese()) || "DX_CLAW_DEPLOY_RISE".equals(s.getSkillNameEnglish())).count();
        if (existing.size() > 1 || mains > 1 || existing.isEmpty() != (mains == 0)) throw invalid("展開主技能と私有追撃が完整な一組ではない");
        if (!existing.isEmpty() && (!FOLLOWUP_JP.equals(existing.get(0).getSkillNameJapanese()) || !FOLLOWUP_EN.equals(existing.get(0).getSkillNameEnglish()))) throw invalid("私有追撃の双名が変更された");

        appendOne(mek, waz, hook, "ステップインフック", "0002", "フック・リターン", "DX_HOOK_RETURN", 22,
                "踏み込んでフックを放ち、出し切ると後方へ下がる。命中後はストレートパンチへ切り替えるか、間合いを取り直すかを選べる。", value -> {
                    hit(at(value, 0, 41, CEventHit.class, 34, 36), 34, 36, 100, 1);
                    requireNormalEnd(at(value, 0, 71, CEventChange.class, 70, 70));
                    val(at(value, 0, 27, CEventVal.class, 0, 0), 0, 0, 0, 20);
                    CEventVal recovery = at(value, 0, 27, CEventVal.class, 63, 70); val(recovery, 1, 0, 0, 100); setVal(recovery, 100);
                    requireEmpty(value, 0, 21); requireEmpty(value, 0, 28); requireEmpty(value, 0, 47);
                    restore(value, 0, 34); addSpeed(value, 0, 40, 47, -12); addSpeed(value, 0, 48, 48, 0); gate(value, 0, 48);
                });

        Waz.Skill helper = copy(rise, Waz.Skill.class);
        helper.setSkillNameJapanese(FOLLOWUP_JP); helper.setSkillNameEnglish(FOLLOWUP_EN);
        hit(at(helper, 0, 41, CEventHit.class, 18, 22), 18, 22, 30, 0);
        CEventHit secondHit = at(helper, 0, 41, CEventHit.class, 23, 33); hit(secondHit, 23, 33, 60, 0); secondHit.setSelfStunFrame(1);
        CEventValRandom riseGravity = at(helper, 0, 21, CEventValRandom.class, 45, 45); six(riseGravity, 2, List.of(0, 0, 0, 0, 100, 0)); setSix(riseGravity, List.of(0, 50, 0, 0, 50, 0));
        val(at(helper, 0, 27, CEventVal.class, 0, 0), 0, 0, 0, 0); val(at(helper, 0, 27, CEventVal.class, 45, 45), 0, 0, 0, 100);
        restore(helper, 0, 23);
        CEventChange riseExit = at(helper, 0, 71, CEventChange.class, 71, 71); requireJump(riseExit, group, standIndex); jump(riseExit, group, storageIndex);
        final int helperIndex;
        if (existing.isEmpty()) { helperIndex = waz.getSkillList().size(); waz.getSkillList().add(helper); }
        else { helperIndex = waz.getSkillList().indexOf(existing.get(0)); if (!payload(helper).equals(payload(existing.get(0)))) throw invalid("既有私有追撃の載荷が変更された"); }
        appendOne(mek, waz, deploy, "クロー展開", "0015", "クロー・起動突き", "DX_CLAW_DEPLOY_RISE", 26,
                "クローを展開してライズネイルへつなぎ、出し切ると収納する。展開中は排熱する。二段目の命中後は別の攻撃へ切り替えられる。", value -> {
                    CEventChange ground = at(value, 0, 71, CEventChange.class, 67, 67), airborne = at(value, 1, 71, CEventChange.class, 53, 53);
                    requireJump(ground, group, standIndex); requireJump(airborne, group, standIndex);
                    val(at(value, 0, 47, CEventVal.class, 44, 44), 0, 0, 0, 1); val(at(value, 1, 47, CEventVal.class, 30, 30), 0, 0, 0, 1);
                    jump(ground, group, helperIndex); jump(airborne, group, helperIndex);
                }, riseWeapon, storageWeapon);

        appendOne(mek, waz, down, "ダウンネイル", "0014", "ダウンネイル・収納", "DX_DOWN_NAIL_RETRACT", 24,
                "クローを振り下ろし、重い一撃の後は収納して構え直す。命中を見てジャブへつなぐか、出し切って距離を整えたい。", value -> {
                    hit(at(value, 0, 41, CEventHit.class, 31, 35), 31, 35, 25, 0);
                    CEventHit heavy = at(value, 0, 41, CEventHit.class, 36, 37); hit(heavy, 36, 37, 100, 0); heavy.setSelfStunFrame(1);
                    CEventVal recover = at(value, 0, 27, CEventVal.class, 71, 87); val(recover, 1, 0, 0, 100); setVal(recover, 100); framesTo(recover, 36, 36);
                    gravity(value, 0, 36); modifier(value, 0, 28, 36);
                    CEventChange exit = at(value, 0, 71, CEventChange.class, 87, 87); requireJump(exit, group, standIndex); framesTo(exit, 55, 55); jump(exit, group, storageIndex);
                    // Keep all three final recovery poses, compressing only their post-hit wait.
                    for (var e : unit(value, 0, 0, false).getSkillInfoObjectList()) {
                        if (frames(e, 46, 70)) framesTo(e, 46, 47); else if (frames(e, 71, 77)) framesTo(e, 48, 50);
                        else if (frames(e, 78, 82)) framesTo(e, 51, 52); else if (frames(e, 83, 87)) framesTo(e, 53, 55);
                    }
                }, storageWeapon);

        appendOne(mek, waz, machinegun, "マシンガン", "0005", "マシンガン・三点射", "DX_MACHINEGUN_THREE_BURST", 12,
                "マシンガンを三点射し、長押しせずに攻めを切り替える。撃ち終わりからフックやクローの間合いへ入りたい。", value -> {
                    requireNormalEnd(at(value, 0, 71, CEventChange.class, 35, 35)); requireNormalEnd(at(value, 2, 71, CEventChange.class, 33, 33));
                    var routes = changes(value, 1); if (routes.size() != 2 || routes.stream().anyMatch(c -> !frames(c, 3, 3))) throw invalid("MGの循環窓が変更された");
                    CEventChange loop = routes.get(0), exit = routes.get(1);
                    if (!Integer.valueOf(3).equals(loop.getFlag()) || loop.getBsdxInfoCollectionList1().size() != 3) throw invalid("MGの複合循環が変更された");
                    requireCondition(loop.getBsdxInfoCollectionList1().get(0), List.of(2, 5, 2, 3), List.of(0), List.of(0, 0));
                    requireCondition(loop.getBsdxInfoCollectionList1().get(1), List.of(2, 4, 1), List.of(), List.of());
                    requireCondition(loop.getBsdxInfoCollectionList1().get(2), List.of(2, 0, 0, 5), List.of(0), List.of(0, 0));
                    destination(loop, List.of(0, 1), List.of(1)); requireNormalEnd(exit); framesTo(exit, 12, 12);
                    unit(value, 1, 71, false).setSkillInfoObjectList(new ArrayList<>(List.of(exit)));
                    var source = effects(value, 1); if (source.size() != 3) throw invalid("MGの弾と二口焔が変更された");
                    List<SkillInfoObject> repeated = new ArrayList<>();
                    for (int frame : List.of(0, 4, 8)) for (var e : source) { requireBatchEffect(e, 0); var duplicate = copy(e, CEventEffect.class); framesTo(duplicate, frame, frame); repeated.add(duplicate); }
                    unit(value, 1, 48, false).setSkillInfoObjectList(repeated);
                    CEventVal oldGate = at(value, 1, 47, CEventVal.class, 0, 0); val(oldGate, 0, 0, 0, 1); framesTo(oldGate, 9, 9);
                    CEventVal prepare = at(value, 0, 27, CEventVal.class, 0, 13); val(prepare, 1, 0, 0, 5); setVal(prepare, 100);
                    CEventVal recover = at(value, 2, 27, CEventVal.class, 21, 33); val(recover, 1, 0, 0, 100); setVal(recover, 100);
                    restore(value, 1, 0); restore(value, 2, 0);
                    CEventSprite hold = at(value, 1, 0, CEventSprite.class, 2, 3); hold.setEndFrame(12);
                });

        appendOne(mek, waz, explosive, "炸裂弾", "0013", "炸裂弾・交差連射", "DX_EXPLOSIVE_STAGGER", 32,
                "左右の発射口から炸裂弾を時間差で放つ。一斉射とは異なる二拍で相手を牽制し、撃ち終わりから次の間合いへ移る。", value -> {
                    var emitters = effects(value, 1); if (emitters.size() != 4) throw invalid("左右発射組が変更された"); for (var e : emitters) requireBatchEffect(e, 0);
                    if (!Objects.equals(reference(emitters.get(0)).getWazFileNo(), reference(emitters.get(2)).getWazFileNo()) || !Objects.equals(reference(emitters.get(0)).getWazSequenceNo(), reference(emitters.get(2)).getWazSequenceNo())) throw invalid("左右の炸裂弾参照が一致しない");
                    for (int i : List.of(2, 3)) framesTo(emitters.get(i), 8, 8);
                    CEventVal unlock = at(value, 1, 47, CEventVal.class, 2, 2); val(unlock, 0, 0, 0, 1); framesTo(unlock, 9, 9);
                    CEventChange end = at(value, 1, 71, CEventChange.class, 39, 39); requireNormalEnd(end); framesTo(end, 24, 24);
                    CEventSprite wait = at(value, 1, 0, CEventSprite.class, 10, 39); wait.setEndFrame(24);
                    requireNormalEnd(at(value, 2, 71, CEventChange.class, 34, 34));
                });
    }

    private static void appendOne(Mek mek, Waz waz, Waz.Skill source, String sourceName, String sequence, String name, String en, int heat, String description, Consumer<Waz.Skill> edit, Mek.MekWeaponInfo... donors) { CharacterWeaponVariants.append(mek, waz, weapon(mek, waz, source, sourceName, sequence), source, name, en, heat, description, edit, donors); }
    private static Waz.Skill donor(Waz waz, String jp, String en, int count) { var s = skill(waz, jp, en, count); if (!Integer.valueOf(count).equals(s.getPhaseQuantity()) || s.getSkillSuffixList().size() != count) throw invalid(jp + " phase/suffix件数が変更された"); for (int i = 0; i < count; i++) if (!Integer.valueOf(i).equals(s.getSkillSuffixList().get(i).getInt1()) || !Integer.valueOf(0).equals(s.getSkillSuffixList().get(i).getInt2())) throw invalid(jp + " suffixが変更された"); return s; }
    private static <T extends SkillInfoObject> T at(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) { return event(s, p + 1, slot, type, start, end, start, end); }
    private static SkillUnit unit(Waz.Skill s, int p, int slot, boolean create) { var list = s.getPhasesInfo().get(p).getSkillUnitCollection(); var found = list.stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList(); if (found.size() > 1 || (found.isEmpty() && !create)) throw invalid("P" + p + " slot" + slot + " が一意ではない"); if (!found.isEmpty()) return found.get(0); var result = new SkillUnit(slot, "追加武装専用"); list.add(result); list.sort(Comparator.comparing(SkillUnit::getUnitQuantity)); return result; }
    private static void requireEmpty(Waz.Skill s, int p, int slot) { var matches = s.getPhasesInfo().get(p).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList(); if (matches.size() > 1 || matches.stream().anyMatch(u -> !u.getSkillInfoObjectList().isEmpty() || !u.getSkillInfoUnknownList().isEmpty())) throw invalid("追加予定槽が空でない"); }
    private static List<CEventEffect> effects(Waz.Skill s, int p) { return unit(s, p, 48, false).getSkillInfoObjectList().stream().map(e -> { if (!(e instanceof CEventEffect effect)) throw invalid("Effect型が変更された"); return effect; }).toList(); }
    private static List<CEventChange> changes(Waz.Skill s, int p) { return unit(s, p, 71, false).getSkillInfoObjectList().stream().map(e -> { if (!(e instanceof CEventChange c)) throw invalid("Change型が変更された"); return c; }).toList(); }
    private static SkillInfoObject effectData(CEventEffect e, int slot) { var found = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && Integer.valueOf(1).equals(u.getBuffer())).toList(); if (found.size() != 1 || found.get(0).getData() == null) throw invalid("Effect子槽が変更された"); return found.get(0).getData(); }
    private static CEventWazaSelect reference(CEventEffect e) { if (!(effectData(e, 3) instanceof CEventWazaSelect r)) throw invalid("発射参照型が変更された"); return r; }
    private static void requireBatchEffect(CEventEffect e, int frame) { if (!frames(e, frame, frame) || !frames(reference(e), 0, 0) || reference(e).getWazFileNo() < 0 || reference(e).getWazSequenceNo() < 0) throw invalid("発射窓が変更された"); if (!(effectData(e, 4) instanceof CEventValRandom amount)) throw invalid("発射数型が変更された"); six(amount, 2, List.of(0, 1, 0, 0, 1, 0)); }
    private static void hit(CEventHit h, int start, int end, int power, int cancel) { attack(h, start, end, power, start, end, power); if (!Integer.valueOf(cancel).equals(h.getSelfStunFrame())) throw invalid("int23取消旗が変更された"); }
    private static void requireCondition(BsdxInfoCollection c, List<Integer> types, List<Integer> params, List<Integer> objects) { if (!Integer.valueOf(0).equals(c.getInt1()) || !types.equals(c.getTypeList()) || !params.equals(c.getParamList()) || !objects.equals(c.getIntList3()) || !c.getIntList4().isEmpty()) throw invalid("条件署名が変更された"); }
    private static void destination(CEventChange c, List<Integer> types, List<Integer> params) { if (c.getBsdxInfoCollectionList2().size() != 1 || !Integer.valueOf(16).equals(c.getBsdxInfoCollectionList2().get(0).getInt1()) || !types.equals(c.getBsdxInfoCollectionList2().get(0).getTypeList()) || !params.equals(c.getBsdxInfoCollectionList2().get(0).getParamList())) throw invalid("出口署名が変更された"); }
    private static void requireJump(CEventChange c, int group, int skill) { if (!Integer.valueOf(1).equals(c.getFlag()) || !Integer.valueOf(0).equals(c.getInt1()) || c.getBsdxInfoCollectionList1().size() != 1) throw invalid("JUMP署名が変更された"); requireCondition(c.getBsdxInfoCollectionList1().get(0), List.of(0), List.of(), List.of()); destination(c, List.of(1), List.of(group, skill)); }
    private static void jump(CEventChange c, int group, int skill) { var target = new BsdxInfoCollection(); target.setInt1(16); target.setInt2(0); target.setTypeList(new ArrayList<>(List.of(1))); target.setParamList(new ArrayList<>(List.of(group, skill))); c.setBsdxInfoCollectionList2(new ArrayList<>(List.of(target))); }
    private static void val(CEventVal v, int a, int b, int c, int d) { if (!Integer.valueOf(0).equals(v.getTypeId()) || !List.of(a, b, c, d).equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4()))) throw invalid("倍率・取消値が変更された"); }
    private static void setVal(CEventVal v, int number) { v.setInt1(0); v.setInt2(number); v.setInt3(0); v.setInt4(number); }
    private static void six(CEventValRandom v, int type, List<Integer> values) { if (!Integer.valueOf(type).equals(v.getTypeId()) || !values.equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()))) throw invalid("六整数署名が変更された"); }
    private static void setSix(CEventValRandom v, List<Integer> values) { v.setInt1(values.get(0)); v.setInt2(values.get(1)); v.setInt3(values.get(2)); v.setInt4(values.get(3)); v.setInt5(values.get(4)); v.setInt6(values.get(5)); }
    private static void gravity(Waz.Skill s, int p, int frame) { var v = new CEventValRandom(2); v.setSlotNum(21); framesTo(v, frame, frame); setSix(v, List.of(0, 50, 0, 0, 50, 0)); add(s, p, 21, v); }
    private static void modifier(Waz.Skill s, int p, int slot, int frame) { var v = new CEventVal(0); v.setSlotNum(slot); framesTo(v, frame, frame); setVal(v, 100); add(s, p, slot, v); }
    private static void restore(Waz.Skill s, int p, int frame) { gravity(s, p, frame); modifier(s, p, 27, frame); modifier(s, p, 28, frame); }
    private static void gate(Waz.Skill s, int p, int frame) { var v = new CEventVal(0); v.setSlotNum(47); framesTo(v, frame, frame); setVal(v, 1); add(s, p, 47, v); }
    private static void addSpeed(Waz.Skill s, int p, int start, int end, int speed) { var v = new CEventValRandom(3); v.setSlotNum(18); framesTo(v, start, end); setSix(v, List.of(0, speed, 0, 0, speed, 0)); add(s, p, 18, v); }
    private static void add(Waz.Skill s, int p, int slot, SkillInfoObject e) { var u = unit(s, p, slot, true); if (u.getSkillInfoObjectList().stream().anyMatch(v -> frames(v, e.getStartFrame(), e.getEndFrame()))) throw invalid("追加時刻が占有されている"); u.getSkillInfoObjectList().add(e); u.getSkillInfoObjectList().sort(Comparator.comparing(SkillInfoObject::getStartFrame)); }
    private static void framesTo(SkillInfoObject e, int start, int end) { e.setStartFrame(start); e.setEndFrame(end); }
    private static <T> T copy(T value, Class<T> type) { try { return JSON.readValue(JSON.writeValueAsBytes(value), type); } catch (IOException e) { throw new IllegalStateException("Naoto DTOコピー失敗", e); } }
    private static JsonNode payload(Object value) { JsonNode n = JSON.valueToTree(value); strip(n); return n; }
    private static void strip(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(NaotoAdditionalVariants::strip); }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Naoto追加武装入力不符: " + message); }
}
