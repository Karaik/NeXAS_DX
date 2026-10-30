package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

/** Misaki追加五武装だけを検査してから公開する。公共弾と原武装は編集しない。 */
final class MisakiAdditionalVariants {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MisakiAdditionalVariants() { }

    static void append(Mek mek, Waz waz, FollowupMekaContext context) {
        Mek stagedMek = copy(mek, Mek.class);
        Waz stagedWaz = copy(waz, Waz.class);
        appendStaged(stagedMek, stagedWaz, context);
        int weaponsBefore = mek.getMekWeaponInfoMap().size(), skillsBefore = waz.getSkillList().size();
        int added = stagedMek.getMekWeaponInfoMap().size() - weaponsBefore;
        if ((added != 0 && added != 5) || stagedWaz.getSkillList().size() - skillsBefore != added)
            throw invalid("追加件数が対応しない");
        if (added == 0) return;
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(mek.getMekWeaponInfoMap());
        for (int i = weaponsBefore; i < stagedMek.getMekWeaponInfoMap().size(); i++) weapons.put(i, stagedMek.getMekWeaponInfoMap().get(i));
        List<Waz.Skill> skills = new ArrayList<>(waz.getSkillList());
        skills.addAll(stagedWaz.getSkillList().subList(skillsBefore, stagedWaz.getSkillList().size()));
        var material = copy(mek.getMekMaterialBlock(), Mek.MekMaterialBlock.class);
        var regular = new ArrayList<>(mek.getMekMaterialBlock().getRegularEntries());
        var trailing = new ArrayList<>(mek.getMekMaterialBlock().getTrailingEntries());
        trailing.addAll(stagedMek.getMekMaterialBlock().getTrailingEntries().subList(weaponsBefore, weaponsBefore + added));
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>(regular); entries.addAll(trailing);
        material.setRegularEntries(regular); material.setTrailingEntries(trailing); material.setEntries(entries);
        mek.setMekWeaponInfoMap(weapons); mek.setMekMaterialBlock(material); waz.setSkillList(skills);
    }

    private static void appendStaged(Mek mek, Waz waz, FollowupMekaContext context) {
        Waz.Skill ore = donor(waz, "鉱石発射", "WARP_P1_08", 3);
        Waz.Skill hands = donor(waz, "転移手：掴み", "WARP_P1_06", 4);
        Waz.Skill spear = donor(waz, "本気：血水晶のヤリ", "BEAM04", 3);
        Waz.Skill sword = donor(waz, "本気：血水晶のツルギ", "S_HAMMER06", 4);
        Waz.Skill floor = donor(waz, "転移手：床召還", "G_BIT07", 1);
        Reference oreBullet = common(context, "◆みさき：塊：本体", "0303");
        Reference hand = common(context, "◆みさき：転移グラップ", "0276");
        Reference trail = common(context, "◆みさき：転移グラップ発射口", "0277");
        Reference spearBullet = common(context, "◆みさき：鉱石ヤリ", "0042");
        Reference swordBullet = common(context, "◆みさき：ツルギ", "0319");
        Reference floorBullet = common(context, "◆みさき：床召還", "0304");

        appendOne(mek, waz, ore, "鉱石発射", "0017", "鉱石・二拍射", "DX_ORE_DOUBLE_TAP", 18,
                "鉱石を二度に分けて放つ。二射目を出した後は早めに動きを戻し、転移手による挟撃や掴みの間合いを作る。", value -> {
                    normal(at(value, 0, 71, CEventChange.class, 9, 9));
                    CEventEffect launch = onlyEffect(value, 1, 0, 0); requireLaunch(launch, oreBullet, 0, 0);
                    CEventVal gate = at(value, 1, 47, CEventVal.class, 0, 0); val(gate, 0, 0, 0, 1); framesTo(gate, 11, 11);
                    CEventChange end = at(value, 1, 71, CEventChange.class, 30, 30); normal(end); framesTo(end, 18, 18);
                    CEventEffect second = copy(launch, CEventEffect.class); framesTo(second, 10, 10); unit(value, 1, 48, false).getSkillInfoObjectList().add(second);
                    normal(at(value, 2, 71, CEventChange.class, 25, 25));
                    val(at(value, 2, 47, CEventVal.class, 0, 0), 0, 0, 0, 1);
                    // Preserve both recovery poses, including the final pose originally at20..25.
                    for (var u : value.getPhasesInfo().get(2).getSkillUnitCollection()) {
                        for (var event : u.getSkillInfoObjectList()) framesTo(event, event.getStartFrame() * 18 / 25, event.getEndFrame() * 18 / 25);
                        for (var event : u.getSkillInfoUnknownList()) framesTo(event, event.getStartFrame() * 18 / 25, event.getEndFrame() * 18 / 25);
                    }
                });

        appendOne(mek, waz, hands, "転移手：掴み", "0025", "転移手・二度招き", "DX_HAND_TWO_REACH", 24,
                "転移手を最大二度送り、相手を捕らえる機会を作る。長押しで繰り返さず、捕らえた後は手を戻して次の攻めへ移る。", value -> {
                    CEventChange entry = at(value, 0, 71, CEventChange.class, 40, 40);
                    change(entry, List.of(2, 0, 0, 5), List.of(0), List.of(0, 0), List.of(0, 1), List.of(1)); always(entry);
                    for (int slot : List.of(27, 28)) { CEventVal modifier = at(value, 0, slot, CEventVal.class, 0, 17); val(modifier, 1, 0, 0, 10); setVal(modifier, 100); }
                    for (int p : List.of(1, 2)) {
                        var emitters = effects(value, p); if (emitters.size() != 2) throw invalid("掴み手と軌跡の組が変更された");
                        requireLaunch(emitters.get(0), hand, 0, 0); requireLaunch(emitters.get(1), trail, 0, 0);
                        var routes = changes(value, p); if (routes.size() != 3 || routes.stream().anyMatch(c -> !frames(c, 40, 40))) throw invalid("掴み分岐の窓が変更された");
                        change(routes.get(0), List.of(2, 0, 4, 5), List.of(0), List.of(0, 0), p == 1 ? List.of(0, 1) : List.of(0, 0), p == 1 ? List.of(3) : List.of());
                        change(routes.get(1), List.of(2, 0, 0, 5), List.of(0), List.of(0, 0), p == 1 ? List.of(0, 0) : List.of(0, 1), p == 1 ? List.of() : List.of(1));
                        change(routes.get(2), List.of(0), List.of(), List.of(), p == 1 ? List.of(0, 1) : List.of(0, 0), p == 1 ? List.of(3) : List.of());
                        for (int i = 0; i < routes.size(); i++) {
                            if (p == 2 || i > 0) always(routes.get(i));
                            phase(routes.get(i), p == 1 && i > 0 ? 2 : 3);
                        }
                        restore(value, p, 0);
                    }
                    normal(at(value, 3, 71, CEventChange.class, 61, 61)); restore(value, 3, 0); gate(value, 2, 2);
                });

        appendOne(mek, waz, spear, "本気：血水晶のヤリ", "0024", "血晶槍・短束", "DX_CRYSTAL_SHORT_SPEAR", 18,
                "血水晶のヤリを短く放ち、長く構えずに攻めを切り替える。持続よりも手数を重視し、鉱石や転移手と組み合わせたい。", value -> {
                    normal(at(value, 0, 71, CEventChange.class, 14, 14));
                    CEventEffect launch = onlyEffect(value, 1, 2, 60); requireLaunch(launch, spearBullet, 0, 58);
                    for (var child : launch.getCeventEffectUnitList()) if (child.getBuffer() != 0 && child.getData() != null) {
                        if (!frames(child.getData(), 0, 58)) throw invalid("ヤリの相対窓が変更された"); framesTo(child.getData(), 0, 16);
                    }
                    framesTo(launch, 2, 18);
                    CEventVal oldGate = at(value, 1, 47, CEventVal.class, 2, 2); val(oldGate, 0, 0, 0, 1); unit(value, 1, 47, false).getSkillInfoObjectList().remove(oldGate);
                    CEventChange end = at(value, 1, 71, CEventChange.class, 60, 60); normal(end); framesTo(end, 18, 18);
                    normal(at(value, 2, 71, CEventChange.class, 37, 37));
                    at(value, 2, 24, CEventVal.class, 28, 37);
                    for (var u : value.getPhasesInfo().get(2).getSkillUnitCollection()) {
                        for (var event : u.getSkillInfoObjectList()) recoveryClock(event, 28, 8);
                        for (var event : u.getSkillInfoUnknownList()) recoveryClock(event, 28, 8);
                    }
                    CEventVal oldMultiplier = at(value, 2, 28, CEventVal.class, 0, 0); val(oldMultiplier, 0, 0, 0, 50); setVal(oldMultiplier, 100);
                    gravity(value, 2, 0); modifier(value, 2, 27, 0); gate(value, 2, 0);
                });

        appendOne(mek, waz, sword, "本気：血水晶のツルギ", "0022", "血晶剣・帰着", "DX_CRYSTAL_SWORD_RETURN", 32,
                "血水晶のツルギで空中から間合いを詰め、着地後は姿勢を整える。長く追い続けず、届かなければ収束して次の行動へ移る。", value -> {
                    guardSwordEntry(value);
                    var routes = changes(value, 2); if (routes.size() != 2) throw invalid("ツルギの空中出口が変更された");
                    CEventChange distance = routes.get(0), landing = routes.get(1);
                    change(distance, List.of(2, 3, 7, 2), List.of(300), List.of(0, 19), List.of(0, 0), List.of());
                    if (!List.of(1, 19).equals(distance.getBsdxInfoCollectionList1().get(0).getIntList4()) || !frames(distance, 0, 123)) throw invalid("ツルギの距離条件が変更された");
                    change(landing, List.of(2, 7, 16), List.of(), List.of(0, 0), List.of(0, 1), List.of(6));
                    if (!frames(landing, 0, 123)) throw invalid("ツルギの着地窓が変更された"); phase(landing, 3);
                    CEventValRandom g0 = at(value, 2, 21, CEventValRandom.class, 0, 0), gRise = at(value, 2, 21, CEventValRandom.class, 34, 64);
                    six(g0, 2, List.of(0, 0, 0, 0, 0, 0)); six(gRise, 2, List.of(1, 0, 0, 0, 200, 0));
                    setSix(g0, List.of(0, 50, 0, 0, 50, 0)); unit(value, 2, 21, false).getSkillInfoObjectList().remove(gRise);
                    modifier(value, 2, 27, 0); modifier(value, 2, 28, 0);
                    CEventVal sd = at(value, 2, 47, CEventVal.class, 0, 123); val(sd, 0, 3, 0, 3);
                    clip(value, 2, 72); framesTo(sd, 0, 71);
                    for (var loop : unit(value, 2, 47, false).getSkillInfoUnknownList()) framesTo(loop, 0, 71);
                    CEventChange timeout = copy(at(value, 3, 71, CEventChange.class, 78, 78), CEventChange.class); normal(timeout); always(timeout); phase(timeout, 3); framesTo(timeout, 72, 72); unit(value, 2, 71, false).getSkillInfoObjectList().add(timeout);
                    CEventValRandom gEnd = at(value, 3, 21, CEventValRandom.class, 39, 39); six(gEnd, 2, List.of(0, 0, 0, 0, 50, 0)); framesTo(gEnd, 0, 0); setSix(gEnd, List.of(0, 50, 0, 0, 50, 0));
                    for (int slot : List.of(27, 28)) {
                        CEventVal freeze = at(value, 3, slot, CEventVal.class, 0, 0), recover = at(value, 3, slot, CEventVal.class, 39, 39);
                        val(freeze, 0, 0, 0, 0); val(recover, 0, 0, 0, 100); setVal(freeze, 100); unit(value, 3, slot, false).getSkillInfoObjectList().remove(recover);
                    }
                    CEventVal endSd = at(value, 3, 47, CEventVal.class, 0, 78); val(endSd, 0, 3, 0, 3); framesTo(endSd, 0, 17); gate(value, 3, 18);
                    normal(at(value, 3, 71, CEventChange.class, 78, 78));
                    var explosions = effects(value, 3).stream().filter(e -> Integer.valueOf(swordBullet.group).equals(reference(e).getWazFileNo())).toList();
                    if (explosions.size() != 1) throw invalid("ツルギ爆点が一意ではない"); requireLaunch(explosions.get(0), swordBullet, 0, 0);
                });

        appendOne(mek, waz, floor, "転移手：床召還", "0034", "床召還・重ね置き", "DX_FLOOR_DOUBLE_MARK", 34,
                "敵の位置へ時間差で床召還を二度送り込む。二度目の発動後は早めに動きを戻し、掴みや転移手の間合いを作る。", value -> {
                    CEventEffect launch = onlyEffect(value, 0, 18, 18); requireLaunch(launch, floorBullet, 0, 0);
                    CEventVal unlock = at(value, 0, 47, CEventVal.class, 18, 18); val(unlock, 0, 0, 0, 1); framesTo(unlock, 35, 35);
                    normal(at(value, 0, 71, CEventChange.class, 87, 87)); at(value, 0, 24, CEventVal.class, 68, 87);
                    for (var u : value.getPhasesInfo().get(0).getSkillUnitCollection()) {
                        for (var event : u.getSkillInfoObjectList()) shortenWait(event, 68, 42);
                        for (var event : u.getSkillInfoUnknownList()) shortenWait(event, 68, 42);
                    }
                    CEventEffect second = copy(launch, CEventEffect.class); framesTo(second, 34, 34); unit(value, 0, 48, false).getSkillInfoObjectList().add(second);
                });
    }

    private static void appendOne(Mek mek, Waz waz, Waz.Skill source, String sourceName, String sequence, String name, String english, int heat, String description, Consumer<Waz.Skill> edit) {
        CharacterWeaponVariants.append(mek, waz, weapon(mek, waz, source, sourceName, sequence), source, name, english, heat, description, edit);
    }

    private static void guardSwordEntry(Waz.Skill sword) {
        var entries = changes(sword, 0);
        if (entries.size() != 2 || !unit(sword, 0, 71, false).getSkillInfoUnknownList().isEmpty())
            throw invalid("ツルギの入口分岐数・ループが変更された");
        CEventChange height = at(sword, 0, 71, CEventChange.class, 1, 36);
        CEventChange fallback = at(sword, 0, 71, CEventChange.class, 37, 37);
        if (entries.get(0) != height || entries.get(1) != fallback)
            throw invalid("ツルギの入口分岐順序が変更された");
        change(height, List.of(2, 3, 4, 2), List.of(100), List.of(1, 19), List.of(0, 0), List.of());
        var condition = height.getBsdxInfoCollectionList1().get(0);
        var target = height.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(0).equals(condition.getInt2())
                || !List.of(0, 19).equals(condition.getIntList4())
                || !Integer.valueOf(0).equals(target.getInt2())
                || !target.getIntList3().isEmpty() || !target.getIntList4().isEmpty())
            throw invalid("ツルギの入口比較対象が変更された");
        normal(fallback);
        if (changes(sword, 1).size() != 1 || !unit(sword, 1, 71, false).getSkillInfoUnknownList().isEmpty())
            throw invalid("ツルギの準備後出口数・ループが変更された");
        // A finite P1 must retain this native unconditional NEXT before reaching the new P2 recovery.
        normal(at(sword, 1, 71, CEventChange.class, 25, 25));
    }

    private static Waz.Skill donor(Waz waz, String jp, String en, int count) {
        Waz.Skill s = skill(waz, jp, en, count);
        if (!Integer.valueOf(count).equals(s.getPhaseQuantity()) || s.getSkillSuffixList().size() != count) throw invalid(jp + " phase/suffix件数が変更された");
        for (int i = 0; i < count; i++) if (!Integer.valueOf(i).equals(s.getSkillSuffixList().get(i).getInt1()) || !Integer.valueOf(0).equals(s.getSkillSuffixList().get(i).getInt2())) throw invalid(jp + " suffixが変更された");
        return s;
    }
    private static Reference common(FollowupMekaContext context, String jp, String en) {
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        var plan = context.getConvertedBundle().getCommonProjectileAppendPlan();
        Integer base = plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup), count = plan.getSourceWazIndexToTargetSkillCount().get(sourceGroup), group = plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        String file = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        if (base == null || count == null || group == null || file == null || base < 0 || count <= 0 || group < 0 || context.getBaseline() == null) throw invalid("公共追加計画が不完全");
        var files = context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> file.equalsIgnoreCase(e.getKey())).toList();
        if (files.size() != 1 || files.get(0).getValue().getSkillList().size() < (long) base + count) throw invalid("公共宿主が不完全");
        List<Integer> matches = new ArrayList<>();
        for (int i = base; i < base + count; i++) { var s = files.get(0).getValue().getSkillList().get(i); if (s != null && jp.equals(s.getSkillNameJapanese()) && en.equals(s.getSkillNameEnglish())) matches.add(i); }
        if (matches.size() != 1) throw invalid("公共弾の双名が一意ではない: " + jp);
        return new Reference(group, matches.get(0));
    }
    private static <T extends SkillInfoObject> T at(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) { return event(s, p + 1, slot, type, start, end, start, end); }
    private static SkillUnit unit(Waz.Skill s, int p, int slot, boolean create) {
        var list = s.getPhasesInfo().get(p).getSkillUnitCollection(); var matches = list.stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        if (matches.size() > 1 || (matches.isEmpty() && !create)) throw invalid("P" + p + " slot" + slot + " が一意ではない");
        if (!matches.isEmpty()) return matches.get(0); var result = new SkillUnit(slot, "追加武装専用"); list.add(result); list.sort(Comparator.comparing(SkillUnit::getUnitQuantity)); return result;
    }
    private static List<CEventEffect> effects(Waz.Skill s, int p) { return unit(s, p, 48, false).getSkillInfoObjectList().stream().map(e -> { if (!(e instanceof CEventEffect effect)) throw invalid("Effect型が変更された"); return effect; }).toList(); }
    private static List<CEventChange> changes(Waz.Skill s, int p) { return unit(s, p, 71, false).getSkillInfoObjectList().stream().map(e -> { if (!(e instanceof CEventChange c)) throw invalid("Change型が変更された"); return c; }).toList(); }
    private static CEventEffect onlyEffect(Waz.Skill s, int p, int start, int end) { var list = effects(s, p); if (list.size() != 1 || !frames(list.get(0), start, end)) throw invalid("発射窓が変更された"); return list.get(0); }
    private static SkillInfoObject effectData(CEventEffect e, int slot) { var values = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && Integer.valueOf(1).equals(u.getBuffer())).toList(); if (values.size() != 1 || values.get(0).getData() == null) throw invalid("Effect子槽が変更された"); return values.get(0).getData(); }
    private static CEventWazaSelect reference(CEventEffect e) { if (!(effectData(e, 3) instanceof CEventWazaSelect r)) throw invalid("発射参照型が変更された"); return r; }
    private static void requireLaunch(CEventEffect e, Reference expected, int start, int end) { var r = reference(e); if (!frames(r, start, end) || !Integer.valueOf(expected.group).equals(r.getWazFileNo()) || !Integer.valueOf(expected.skill).equals(r.getWazSequenceNo())) throw invalid("公共弾参照が変更された"); if (!(effectData(e, 4) instanceof CEventValRandom amount)) throw invalid("発射数型が変更された"); six(amount, 2, List.of(0, 1, 0, 0, 1, 0)); }
    private static void change(CEventChange c, List<Integer> types, List<Integer> params, List<Integer> objects, List<Integer> destination, List<Integer> target) {
        if (!Integer.valueOf(1).equals(c.getFlag()) || !Integer.valueOf(0).equals(c.getInt1()) || c.getBsdxInfoCollectionList1().size() != 1 || c.getBsdxInfoCollectionList2().size() != 1) throw invalid("Change署名が変更された");
        var a = c.getBsdxInfoCollectionList1().get(0); var b = c.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(0).equals(a.getInt1()) || !types.equals(a.getTypeList()) || !params.equals(a.getParamList()) || !objects.equals(a.getIntList3()) || !Integer.valueOf(16).equals(b.getInt1()) || !destination.equals(b.getTypeList()) || !target.equals(b.getParamList())) throw invalid("条件・出口署名が変更された");
    }
    private static void normal(CEventChange c) { requireNormalEnd(c); }
    private static void always(CEventChange c) { c.setFlag(1); c.setInt1(0); BsdxInfoCollection condition = collection(0, List.of(0), List.of()); c.setBsdxInfoCollectionList1(new ArrayList<>(List.of(condition))); }
    private static void phase(CEventChange c, int p) { c.setBsdxInfoCollectionList2(new ArrayList<>(List.of(collection(16, List.of(0, 1), List.of(p))))); }
    private static BsdxInfoCollection collection(int root, List<Integer> types, List<Integer> params) { var c = new BsdxInfoCollection(); c.setInt1(root); c.setInt2(0); c.setTypeList(new ArrayList<>(types)); c.setParamList(new ArrayList<>(params)); return c; }
    private static void val(CEventVal v, int a, int b, int c, int d) { if (!Integer.valueOf(0).equals(v.getTypeId()) || !List.of(a, b, c, d).equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4()))) throw invalid("倍率・取消値が変更された"); }
    private static void setVal(CEventVal v, int number) { v.setInt1(0); v.setInt2(number); v.setInt3(0); v.setInt4(number); }
    private static void six(CEventValRandom v, int type, List<Integer> values) { if (!Integer.valueOf(type).equals(v.getTypeId()) || !values.equals(Arrays.asList(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()))) throw invalid("六整数署名が変更された"); }
    private static void setSix(CEventValRandom v, List<Integer> values) { v.setInt1(values.get(0)); v.setInt2(values.get(1)); v.setInt3(values.get(2)); v.setInt4(values.get(3)); v.setInt5(values.get(4)); v.setInt6(values.get(5)); }
    private static void gravity(Waz.Skill s, int p, int frame) { var v = new CEventValRandom(2); v.setSlotNum(21); framesTo(v, frame, frame); setSix(v, List.of(0, 50, 0, 0, 50, 0)); add(s, p, 21, v); }
    private static void modifier(Waz.Skill s, int p, int slot, int frame) { var v = new CEventVal(0); v.setSlotNum(slot); framesTo(v, frame, frame); setVal(v, 100); add(s, p, slot, v); }
    private static void restore(Waz.Skill s, int p, int frame) { gravity(s, p, frame); modifier(s, p, 27, frame); modifier(s, p, 28, frame); }
    private static void gate(Waz.Skill s, int p, int frame) { var v = new CEventVal(0); v.setSlotNum(47); framesTo(v, frame, frame); setVal(v, 1); add(s, p, 47, v); }
    private static void add(Waz.Skill s, int p, int slot, SkillInfoObject event) { var u = unit(s, p, slot, true); if (u.getSkillInfoObjectList().stream().anyMatch(e -> frames(e, event.getStartFrame(), event.getEndFrame()))) throw invalid("追加先の時刻が占有されている"); u.getSkillInfoObjectList().add(event); u.getSkillInfoObjectList().sort(Comparator.comparing(SkillInfoObject::getStartFrame)); }
    private static void framesTo(SkillInfoObject e, int start, int end) { e.setStartFrame(start); e.setEndFrame(end); }
    private static void recoveryClock(SkillInfoObject e, int oldStart, int newStart) { int a = e.getStartFrame(), b = e.getEndFrame(); int start = a < oldStart ? a * newStart / oldStart : a - oldStart + newStart; int end = b < oldStart ? Math.max(start, (b + 1) * newStart / oldStart - 1) : b - oldStart + newStart; framesTo(e, start, end); }
    private static void shortenWait(SkillInfoObject e, int oldStart, int newStart) { int a = e.getStartFrame(), b = e.getEndFrame(); if (a >= oldStart) framesTo(e, a - oldStart + newStart, b - oldStart + newStart); else if (b >= newStart) e.setEndFrame(newStart - 1); }
    private static void clip(Waz.Skill s, int p, int end) { for (var u : s.getPhasesInfo().get(p).getSkillUnitCollection()) { for (var e : u.getSkillInfoObjectList()) { if (e.getStartFrame() > end) throw invalid("出口より後の必要イベントがある"); e.setEndFrame(Math.min(e.getEndFrame(), end)); } for (var e : u.getSkillInfoUnknownList()) { if (e.getStartFrame() > end) throw invalid("出口より後のループがある"); e.setEndFrame(Math.min(e.getEndFrame(), end)); } } }
    private static <T> T copy(T value, Class<T> type) { try { return JSON.readValue(JSON.writeValueAsBytes(value), type); } catch (IOException e) { throw new IllegalStateException("Misaki DTOコピー失敗", e); } }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Misaki追加武装入力不符: " + message); }
    private record Reference(int group, int skill) { }
}
