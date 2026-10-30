package com.giga.nexas.transfer.bhe2bsdx.meka.katou;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Katou専用の五武装。原架勢・肘・収納への参照は双名と現在のWAZ位置から束縛する。 */
final class KatouAdditionalVariants {
    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private KatouAdditionalVariants() { }

    static void append(Mek mek, Waz waz) {
        Mek stagedMek = copy(mek, Mek.class); Waz stagedWaz = copy(waz, Waz.class); appendStaged(stagedMek, stagedWaz);
        int oldWeapons = mek.getMekWeaponInfoMap().size(), oldSkills = waz.getSkillList().size();
        int added = stagedMek.getMekWeaponInfoMap().size() - oldWeapons;
        if ((added != 0 && added != 5) || stagedWaz.getSkillList().size() - oldSkills != added) throw invalid("追加件数が対応しない");
        if (added == 0) return;
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(mek.getMekWeaponInfoMap());
        for (int i = oldWeapons; i < oldWeapons + added; i++) weapons.put(i, stagedMek.getMekWeaponInfoMap().get(i));
        List<Waz.Skill> skills = new ArrayList<>(waz.getSkillList()); skills.addAll(stagedWaz.getSkillList().subList(oldSkills, oldSkills + added));
        var material = copy(mek.getMekMaterialBlock(), Mek.MekMaterialBlock.class);
        var regular = new ArrayList<>(mek.getMekMaterialBlock().getRegularEntries()); var trailing = new ArrayList<>(mek.getMekMaterialBlock().getTrailingEntries());
        trailing.addAll(stagedMek.getMekMaterialBlock().getTrailingEntries().subList(oldWeapons, oldWeapons + added));
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>(regular); entries.addAll(trailing);
        material.setRegularEntries(regular); material.setTrailingEntries(trailing); material.setEntries(entries);
        mek.setMekWeaponInfoMap(weapons); mek.setMekMaterialBlock(material); waz.setSkillList(skills);
    }

    private static void appendStaged(Mek mek, Waz waz) {
        Waz.Skill entry = donor(waz, "トンファー：トンファー接近", "S_HAMMER07", 2);
        Waz.Skill elbow = donor(waz, "トンファー：トンファーエルボー", "STRAIGHT04", 1);
        Waz.Skill kick = donor(waz, "サッカーボールキック", "LOWKICK04", 1);
        Waz.Skill slam = donor(waz, "トンファー：トンファー振り下ろし", "S_HAMMER08", 2);
        Waz.Skill missile = donor(waz, "ミサイルランチャー", "0002", 3);
        Waz.Skill stance = donor(waz, "トンファー：構え中", "0010", 2), storage = donor(waz, "トンファー：収納", "0009", 1);
        var elbowWeapon = weapon(mek, waz, elbow, "トンファーエルボー", "0001");
        var stanceWeapon = weapon(mek, waz, stance, "トンファー構え中", "0007");
        var storageWeapon = weapon(mek, waz, storage, "トンファー収納", "0008");
        int group = mek.getMekBasicInfo().getWazFileSequence(), stanceIndex = waz.getSkillList().indexOf(stance), elbowIndex = waz.getSkillList().indexOf(elbow), storageIndex = waz.getSkillList().indexOf(storage);
        requireNormalEnd(at(storage, 0, 71, CEventChange.class, 21, 21));
        hit(at(elbow, 0, 41, CEventHit.class, 7, 20), 7, 20, 25, 1);

        appendOne(mek, waz, entry, "トンファー接近", "0009", "トンファー・踏込肘", "DX_TONFA_ENTRY_ELBOW", 18,
                "間合いを詰め、踏み込みを止めてから肘を打ち込む。近づきすぎる前に切り替えることもでき、振り下ろしからの追撃に使いたい。", value -> {
                    CEventValRandom drive = at(value, 0, 18, CEventValRandom.class, 34, 38); six(drive, 3, List.of(0, 128, 0, 0, 24, 0)); setSix(drive, List.of(0, 64, 0, 0, 16, 0));
                    var routes = changes(value, 0); if (routes.size() != 3) throw invalid("接近出口数が変更された");
                    CEventChange view = routes.get(0), distance = routes.get(1), timeout = routes.get(2);
                    condition(view, 1, List.of(2, 3, 8, 3), List.of(180), List.of(0, 0), List.of(1, 0)); destination(view, List.of(0, 0), List.of());
                    condition(distance, 1, List.of(2, 3, 6, 2), List.of(200), List.of(0, 0), List.of(1, 0)); destination(distance, List.of(0, 0), List.of());
                    if (!frames(view, 34, 62) || !frames(distance, 39, 62)) throw invalid("接近条件窓が変更された"); distance.getBsdxInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(110)));
                    requireJump(timeout, group, stanceIndex); if (!frames(timeout, 62, 62)) throw invalid("接近時間上限が変更された"); framesTo(timeout, 44, 44); next(timeout);
                    CEventValRandom g = at(value, 0, 21, CEventValRandom.class, 0, 0); six(g, 2, List.of(0, 0, 0, 0, 0, 0)); setSix(g, List.of(0, 50, 0, 0, 50, 0));
                    clip(value, 0, 44);
                    six(at(value, 1, 18, CEventValRandom.class, 0, 0), 3, List.of(1, 0, 0, 0, 0, 0));
                    CEventChange follow = at(value, 1, 71, CEventChange.class, 8, 8); requireJump(follow, group, stanceIndex); jump(follow, group, elbowIndex);
                    val(at(value, 1, 47, CEventVal.class, 5, 5), 0, 3, 0, 1); restore(value, 1, 0);
                }, elbowWeapon, stanceWeapon);

        appendOne(mek, waz, elbow, "トンファーエルボー", "0001", "トンファー・引き肘", "DX_TONFA_RECOIL_ELBOW", 12,
                "肘を打ち込み、出し切ると後方へ下がってトンファーを収納する。短い追撃から攻めを終え、間合いを取り直したい。", value -> {
                    hit(at(value, 0, 41, CEventHit.class, 7, 20), 7, 20, 25, 1);
                    six(at(value, 0, 18, CEventValRandom.class, 7, 15), 3, List.of(0, 24, 0, 0, 0, 0));
                    val(at(value, 0, 27, CEventVal.class, 31, 38), 1, 0, 0, 100);
                    CEventChange end = at(value, 0, 71, CEventChange.class, 38, 38); requireJump(end, group, stanceIndex); jump(end, group, storageIndex);
                    speed(value, 0, 24, 31, -16); speed(value, 0, 32, 32, 0);
                }, storageWeapon);

        appendOne(mek, waz, kick, "サッカーボールキック", "PUNCH", "蹴崩し・追い肘", "DX_KICK_CONFIRM_ELBOW", 18,
                "蹴りが命中したときだけ肘で追い打ちする。空振りでは追撃せず、当てた後の短い攻めを重視する。", value -> {
                    hit(at(value, 0, 41, CEventHit.class, 37, 43), 37, 43, 80, 1);
                    CEventChange whiff = at(value, 0, 71, CEventChange.class, 70, 70); condition(whiff, 0, List.of(0), List.of(), List.of(), List.of()); destination(whiff, List.of(0, 2), List.of());
                    if (changes(value, 0).size() != 1) throw invalid("蹴りに未確認の出口がある");
                    CEventChange onHit = new CEventChange(0x22); onHit.setSlotNum(71); framesTo(onHit, 37, 43); onHit.setFlag(1); onHit.setInt1(0);
                    var hitCondition = collection(0, List.of(2, 6, 0, 0), List.of(0)); hitCondition.setIntList3(new ArrayList<>(List.of(0, 0)));
                    onHit.setBsdxInfoCollectionList1(new ArrayList<>(List.of(hitCondition))); jump(onHit, group, elbowIndex);
                    unit(value, 0, 71, false).getSkillInfoObjectList().add(0, onHit);
                }, elbowWeapon);

        appendOne(mek, waz, slam, "トンファー振り下ろし", "0011", "トンファー・叩き納め", "DX_TONFA_SLAM_STOW", 28,
                "トンファーを振り下ろし、姿勢を戻してから収納する。最後の打撃を出した後は追撃へ切り替えられ、出し切れば構えを解く。", value -> {
                    for (int p : List.of(0, 1)) {
                        int attackFrame = p == 0 ? 44 : 28, lastHit = p == 0 ? 52 : 36, oldEnd = p == 0 ? 90 : 74, newEnd = p == 0 ? 65 : 49;
                        CEventHit strike = at(value, p, 41, CEventHit.class, attackFrame, lastHit); hit(strike, attackFrame, lastHit, p == 0 ? 120 : 100, 1); strike.setSelfStunFrame(0);
                        CEventValRandom originalGravity = at(value, p, 21, CEventValRandom.class, attackFrame, p == 0 ? 44 : 58);
                        six(originalGravity, 2, p == 0 ? List.of(0, 0, 0, 0, 100, 0) : List.of(0, 100, 0, 0, 300, 0)); framesTo(originalGravity, attackFrame, attackFrame); setSix(originalGravity, List.of(0, 50, 0, 0, 50, 0));
                        CEventVal sd = at(value, p, 47, CEventVal.class, 0, oldEnd); val(sd, 0, 3, 0, 3); framesTo(sd, 0, lastHit);
                        restore(value, p, 0); restore(value, p, lastHit + 1); gate(value, p, lastHit + 1);
                        CEventChange end = at(value, p, 71, CEventChange.class, oldEnd, oldEnd); condition(end, 0, List.of(0), List.of(), List.of(), List.of()); destination(end, List.of(0, 2), List.of()); framesTo(end, newEnd, newEnd); jump(end, group, storageIndex);
                    }
                    CEventChange airEntry = at(value, 0, 71, CEventChange.class, 0, 0); condition(airEntry, 0, List.of(2, 7, 17), List.of(), List.of(0, 0), List.of()); destination(airEntry, List.of(0, 0), List.of());
                    CEventChange landed = at(value, 1, 71, CEventChange.class, 40, 74); condition(landed, 0, List.of(2, 7, 17), List.of(), List.of(0, 0), List.of()); destination(landed, List.of(0, 0), List.of()); landed.setEndFrame(49); jump(landed, group, storageIndex);
                    // Preserve the final recovery sprite instead of cutting it away at the shorter exit.
                    for (var e : unit(value, 0, 0, false).getSkillInfoObjectList()) { if (frames(e, 53, 82)) framesTo(e, 53, 57); else if (frames(e, 83, 90)) framesTo(e, 58, 65); }
                    for (var e : unit(value, 1, 0, false).getSkillInfoObjectList()) if (e.getStartFrame() >= 37) compressRecovery(e, 37, 19, 13);
                    for (var e : unit(value, 1, 0, false).getSkillInfoUnknownList()) if (e.getStartFrame() >= 37) compressRecovery(e, 37, 19, 13);
                }, storageWeapon);

        appendOne(mek, waz, missile, "ミサイルランチャー", "MISSILE", "ミサイル・二段牽制", "DX_MISSILE_TWO_BURST", 16,
                "ミサイルを二度に分けて発射する。長押しで撃ち続けず、二射目の後は踏み込みや近距離攻撃へ切り替える。", value -> {
                    requireNormalEnd(at(value, 0, 71, CEventChange.class, 21, 21));
                    var routes = changes(value, 1); if (routes.size() != 2) throw invalid("ミサイル循環数が変更された");
                    CEventChange release = at(value, 1, 71, CEventChange.class, 0, 0), repeat = at(value, 1, 71, CEventChange.class, 12, 12);
                    condition(release, 0, List.of(2, 5, 3, 4), List.of(10), List.of(0, 0), List.of()); destination(release, List.of(0, 1), List.of(2));
                    condition(repeat, 0, List.of(0), List.of(), List.of(), List.of()); destination(repeat, List.of(0, 1), List.of(1));
                    framesTo(repeat, 25, 25); next(repeat); unit(value, 1, 71, false).setSkillInfoObjectList(new ArrayList<>(List.of(repeat)));
                    var source = effects(value, 1); if (source.size() != 2) throw invalid("弾と口焔の組が変更された"); List<SkillInfoObject> launches = new ArrayList<>();
                    for (int frame : List.of(9, 21)) for (var e : source) { requireBatchEffect(e, 9); var copied = copy(e, CEventEffect.class); framesTo(copied, frame, frame); launches.add(copied); }
                    unit(value, 1, 48, false).setSkillInfoObjectList(launches);
                    CEventVal unlock = at(value, 1, 47, CEventVal.class, 9, 9); val(unlock, 0, 0, 0, 1); framesTo(unlock, 22, 22);
                    CEventSprite hold = at(value, 1, 0, CEventSprite.class, 0, 8); hold.setEndFrame(25);
                    CEventChange end = at(value, 2, 71, CEventChange.class, 39, 39); condition(end, 0, List.of(0), List.of(), List.of(), List.of()); destination(end, List.of(0, 2), List.of());
                });
    }

    private static void appendOne(Mek mek, Waz waz, Waz.Skill source, String sourceName, String sequence, String name, String en, int heat, String description, Consumer<Waz.Skill> edit, Mek.MekWeaponInfo... donors) { CharacterWeaponVariants.append(mek, waz, weapon(mek, waz, source, sourceName, sequence), source, name, en, heat, description, edit, donors); }
    private static Waz.Skill donor(Waz waz, String jp, String en, int count) { var s = skill(waz, jp, en, count); if (!Integer.valueOf(count).equals(s.getPhaseQuantity()) || s.getSkillSuffixList().size() != count) throw invalid(jp + " phase/suffix件数が変更された"); for (int i = 0; i < count; i++) if (!Integer.valueOf(i).equals(s.getSkillSuffixList().get(i).getInt1()) || !Integer.valueOf(0).equals(s.getSkillSuffixList().get(i).getInt2())) throw invalid(jp + " suffixが変更された"); return s; }
    private static <T extends SkillInfoObject> T at(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) { return event(s, p + 1, slot, type, start, end, start, end); }
    private static SkillUnit unit(Waz.Skill s, int p, int slot, boolean create) { var list = s.getPhasesInfo().get(p).getSkillUnitCollection(); var found = list.stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList(); if (found.size() > 1 || (found.isEmpty() && !create)) throw invalid("P" + p + " slot" + slot + " が一意ではない"); if (!found.isEmpty()) return found.get(0); var result = new SkillUnit(slot, "追加武装専用"); list.add(result); list.sort(Comparator.comparing(SkillUnit::getUnitQuantity)); return result; }
    private static List<CEventEffect> effects(Waz.Skill s, int p) { return unit(s, p, 48, false).getSkillInfoObjectList().stream().map(e -> { if (!(e instanceof CEventEffect effect)) throw invalid("Effect型が変更された"); return effect; }).toList(); }
    private static List<CEventChange> changes(Waz.Skill s, int p) { return unit(s, p, 71, false).getSkillInfoObjectList().stream().map(e -> { if (!(e instanceof CEventChange c)) throw invalid("Change型が変更された"); return c; }).toList(); }
    private static SkillInfoObject effectData(CEventEffect e, int slot) { var found = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && Integer.valueOf(1).equals(u.getBuffer())).toList(); if (found.size() != 1 || found.get(0).getData() == null) throw invalid("Effect子槽が変更された"); return found.get(0).getData(); }
    private static void requireBatchEffect(CEventEffect e, int frame) { if (!frames(e, frame, frame) || !(effectData(e, 3) instanceof CEventWazaSelect r) || !frames(r, 0, 0) || r.getWazFileNo() < 0 || r.getWazSequenceNo() < 0) throw invalid("発射窓が変更された"); if (!(effectData(e, 4) instanceof CEventValRandom amount)) throw invalid("発射数型が変更された"); six(amount, 2, List.of(0, 1, 0, 0, 1, 0)); }
    private static void hit(CEventHit h, int start, int end, int power, int cancel) { attack(h, start, end, power, start, end, power); if (!Integer.valueOf(cancel).equals(h.getSelfStunFrame())) throw invalid("int23取消旗が変更された"); }
    private static void condition(CEventChange c, int mode, List<Integer> types, List<Integer> params, List<Integer> objects, List<Integer> others) { if (!Integer.valueOf(1).equals(c.getFlag()) || !Integer.valueOf(mode).equals(c.getInt1()) || c.getBsdxInfoCollectionList1().size() != 1) throw invalid("条件数・modeが変更された"); var t = c.getBsdxInfoCollectionList1().get(0); if (!Integer.valueOf(0).equals(t.getInt1()) || !types.equals(t.getTypeList()) || !params.equals(t.getParamList()) || !objects.equals(t.getIntList3()) || !others.equals(t.getIntList4())) throw invalid("条件署名が変更された"); }
    private static void destination(CEventChange c, List<Integer> types, List<Integer> params) { if (c.getBsdxInfoCollectionList2().size() != 1) throw invalid("出口数が変更された"); var t = c.getBsdxInfoCollectionList2().get(0); if (!Integer.valueOf(16).equals(t.getInt1()) || !types.equals(t.getTypeList()) || !params.equals(t.getParamList())) throw invalid("出口署名が変更された"); }
    private static void requireJump(CEventChange c, int group, int skill) { condition(c, 0, List.of(0), List.of(), List.of(), List.of()); destination(c, List.of(1), List.of(group, skill)); }
    private static BsdxInfoCollection collection(int root, List<Integer> types, List<Integer> params) { var c = new BsdxInfoCollection(); c.setInt1(root); c.setInt2(0); c.setTypeList(new ArrayList<>(types)); c.setParamList(new ArrayList<>(params)); return c; }
    private static void jump(CEventChange c, int group, int skill) { c.setBsdxInfoCollectionList2(new ArrayList<>(List.of(collection(16, List.of(1), List.of(group, skill))))); }
    private static void next(CEventChange c) { c.setBsdxInfoCollectionList2(new ArrayList<>(List.of(collection(16, List.of(0, 0), List.of())))); }
    private static void val(CEventVal v, int a, int b, int c, int d) { if (!Integer.valueOf(0).equals(v.getTypeId()) || !List.of(a, b, c, d).equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4()))) throw invalid("倍率・取消値が変更された"); }
    private static void setVal(CEventVal v, int number) { v.setInt1(0); v.setInt2(number); v.setInt3(0); v.setInt4(number); }
    private static void six(CEventValRandom v, int type, List<Integer> values) { if (!Integer.valueOf(type).equals(v.getTypeId()) || !values.equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()))) throw invalid("六整数署名が変更された"); }
    private static void setSix(CEventValRandom v, List<Integer> values) { v.setInt1(values.get(0)); v.setInt2(values.get(1)); v.setInt3(values.get(2)); v.setInt4(values.get(3)); v.setInt5(values.get(4)); v.setInt6(values.get(5)); }
    private static void gravity(Waz.Skill s, int p, int frame) { var v = new CEventValRandom(2); v.setSlotNum(21); framesTo(v, frame, frame); setSix(v, List.of(0, 50, 0, 0, 50, 0)); add(s, p, 21, v); }
    private static void modifier(Waz.Skill s, int p, int slot, int frame) { var v = new CEventVal(0); v.setSlotNum(slot); framesTo(v, frame, frame); setVal(v, 100); add(s, p, slot, v); }
    private static void restore(Waz.Skill s, int p, int frame) { gravity(s, p, frame); modifier(s, p, 27, frame); modifier(s, p, 28, frame); }
    private static void gate(Waz.Skill s, int p, int frame) { var v = new CEventVal(0); v.setSlotNum(47); framesTo(v, frame, frame); setVal(v, 1); add(s, p, 47, v); }
    private static void speed(Waz.Skill s, int p, int start, int end, int value) { var v = new CEventValRandom(3); v.setSlotNum(18); framesTo(v, start, end); setSix(v, List.of(0, value, 0, 0, value, 0)); add(s, p, 18, v); }
    private static void add(Waz.Skill s, int p, int slot, SkillInfoObject e) { var u = unit(s, p, slot, true); if (u.getSkillInfoObjectList().stream().anyMatch(v -> frames(v, e.getStartFrame(), e.getEndFrame()))) throw invalid("追加時刻が占有されている"); u.getSkillInfoObjectList().add(e); u.getSkillInfoObjectList().sort(Comparator.comparing(SkillInfoObject::getStartFrame)); }
    private static void framesTo(SkillInfoObject e, int start, int end) { e.setStartFrame(start); e.setEndFrame(end); }
    private static void clip(Waz.Skill s, int p, int end) { for (var u : s.getPhasesInfo().get(p).getSkillUnitCollection()) { for (var e : u.getSkillInfoObjectList()) { if (e.getStartFrame() > end) throw invalid("必要イベントが出口後にある"); e.setEndFrame(Math.min(e.getEndFrame(), end)); if (e instanceof CEventEffect effect) for (var child : effect.getCeventEffectUnitList()) if (child.getBuffer() != 0 && child.getData() != null) { int duration = e.getEndFrame() - e.getStartFrame(); if (child.getData().getStartFrame() > duration) throw invalid("相対子イベントが発射窓を越える"); child.getData().setEndFrame(Math.min(child.getData().getEndFrame(), duration)); } } for (var e : u.getSkillInfoUnknownList()) { if (e.getStartFrame() > end) throw invalid("ループが出口後にある"); e.setEndFrame(Math.min(e.getEndFrame(), end)); } } }
    private static void compressRecovery(SkillInfoObject e, int origin, int oldFrames, int newFrames) { int start = origin + (e.getStartFrame() - origin) * newFrames / oldFrames; int end = Math.max(start, origin + (e.getEndFrame() - origin + 1) * newFrames / oldFrames - 1); framesTo(e, start, end); }
    private static <T> T copy(T value, Class<T> type) { try { return JSON.readValue(JSON.writeValueAsBytes(value), type); } catch (IOException e) { throw new IllegalStateException("Katou DTOコピー失敗", e); } }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Katou追加武装入力不符: " + message); }
}
