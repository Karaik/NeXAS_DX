package com.giga.nexas.transfer.bhe2bsdx.meka.yuri;

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

/** Yuri専用の六武装。既存の重做を基線に、追加分だけを独立したバッチとして公開する。 */
final class YuriAdditionalVariants {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private YuriAdditionalVariants() { }

    static void append(Mek mek, Waz waz) {
        Mek stagedMek = copy(mek, Mek.class);
        Waz stagedWaz = copy(waz, Waz.class);
        appendStaged(stagedMek, stagedWaz);
        publishNewRows(mek, waz, stagedMek, stagedWaz);
    }

    private static void publishNewRows(Mek mek, Waz waz, Mek stagedMek, Waz stagedWaz) {
        int oldWeapons = mek.getMekWeaponInfoMap().size();
        int oldSkills = waz.getSkillList().size();
        int appended = stagedMek.getMekWeaponInfoMap().size() - oldWeapons;
        if (appended < 0 || stagedWaz.getSkillList().size() - oldSkills != appended)
            throw invalid("追加表の件数が対応していない");
        if (appended == 0) return; // 再実行でも、呼出元が保持する既存行の同一性は変えない。
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(mek.getMekWeaponInfoMap());
        for (int i = oldWeapons; i < stagedMek.getMekWeaponInfoMap().size(); i++)
            weapons.put(i, stagedMek.getMekWeaponInfoMap().get(i));
        List<Waz.Skill> skills = new ArrayList<>(waz.getSkillList());
        skills.addAll(stagedWaz.getSkillList().subList(oldSkills, stagedWaz.getSkillList().size()));
        var material = copy(mek.getMekMaterialBlock(), Mek.MekMaterialBlock.class);
        var regular = new ArrayList<>(mek.getMekMaterialBlock().getRegularEntries());
        var trailing = new ArrayList<>(mek.getMekMaterialBlock().getTrailingEntries());
        trailing.addAll(stagedMek.getMekMaterialBlock().getTrailingEntries().subList(oldWeapons,
                stagedMek.getMekMaterialBlock().getTrailingEntries().size()));
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>(regular);
        entries.addAll(trailing);
        material.setRegularEntries(regular);
        material.setTrailingEntries(trailing);
        material.setEntries(entries);
        // 全供体・副本の検査完了後、新しい行だけを公開し既存オブジェクトは保持する。
        mek.setMekWeaponInfoMap(weapons);
        mek.setMekMaterialBlock(material);
        waz.setSkillList(skills);
    }

    private static void appendStaged(Mek mek, Waz waz) {
        var dagger = skill(waz, "ダガー", "0007", 1);
        var assassin = skill(waz, "アサシンダガー", "STRAIGHT04", 4);
        var sidestep = skill(waz, "サイドステップキック", "SIDESTEPKICK04", 2);
        var pulse = skill(waz, "パルスライフル", "RIFLE4", 3);
        var sniper = skill(waz, "スナイパーライフル", "RIFLE04", 4);
        var mine = skill(waz, "マインスイーパ：避", "MINE_S05", 2);
        CEventVal daggerUnlock = at(dagger, 1, 47, CEventVal.class, 20, 20);
        requireVal(daggerUnlock, 0, 0, 0, 2);
        CEventVal ordinaryGate = at(pulse, 2, 47, CEventVal.class, 3, 3);
        requireVal(ordinaryGate, 0, 0, 0, 1);
        CEventChange normalEnd = at(dagger, 1, 71, CEventChange.class, 43, 43);
        requireNormalEnd(normalEnd);

        appendOne(mek, waz, dagger, "PUNCH", "ダガー・刻み", "DX_DAGGER_LINK", 10,
                "踏み込みを抑えて短いダガーを放ち、早めに次の武装へ移る。近くの相手を刻み、地上で攻めを組み立てる。", value -> {
                    replaceSix(at(value, 1, 18, CEventValRandom.class, 11, 12),
                            List.of(0, 48, 0, 0, 32, 0), List.of(0, 16, 0, 0, 4, 0));
                    requireSix(at(value, 1, 18, CEventValRandom.class, 13, 13), 3, List.of(0, 0, 0, 0, 0, 0));
                    requireRealLaunch(value, 1, 13);
                    requireEventCount(value, 1, 48, 4);
                    CEventVal unlock = at(value, 1, 47, CEventVal.class, 20, 20);
                    requireVal(unlock, 0, 0, 0, 2);
                    setFrames(unlock, 14, 14);
                    replaceGravity(value, 1, 0, List.of(0, 0, 0, 0, 10, 0));
                    requireNormalEnd(at(value, 1, 71, CEventChange.class, 43, 43));
                });

        appendOne(mek, waz, assassin, "0004", "アサシンダガー・速刺", "DX_ASSASSIN_QUICK", 24,
                "刺し込む直前の動きを速めたアサシンダガー。一撃の重さと引き換えに攻めの間を短くし、近距離で次のダガーへつなぐ。", value -> {
                    requireTimeline(value, 3, ASSASSIN_ATTACK_CLOCKS);
                    requireSix(at(value, 3, 18, CEventValRandom.class, 36, 43), 3, List.of(0, 0, 0, 0, 8, 0));
                    requireSix(at(value, 3, 18, CEventValRandom.class, 44, 46), 3, List.of(0, 32, 0, 0, 0, 0));
                    CEventHit hit = at(value, 3, 41, CEventHit.class, 44, 50);
                    attack(hit, 44, 50, 140, 44, 50, 100).run();
                    CEventChange hitExit = at(value, 3, 71, CEventChange.class, 44, 50);
                    requireChange(hitExit, 1, List.of(List.of(2, 6, 0, 0)), List.of(List.of(0)),
                            List.of(List.of(0, 0)), List.of(0, 0), List.of());
                    // 原版P3の後備はTURN/END [0,2]。ダガーの[0,0]供体と混同せず、
                    // 完全な元署名を検証して時刻だけを61へ縮める。宛先・suffixは保全する。
                    requireChange(at(value, 3, 71, CEventChange.class, 92, 92), 1,
                            List.of(List.of(0)), List.of(List.of()), List.of(List.of()), List.of(0, 2), List.of());
                    CEventEffect decoration = at(value, 3, 48, CEventEffect.class, 0, 52);
                    requireDecorationWindow(decoration, 0, 52);
                    // この一つのEffectは全子項が親と同じ相対0..52窓を使う。Hit内部には降りない。
                    for (var inner : decoration.getCeventEffectUnitList())
                        setFrames(inner.getData(), inner.getData().getStartFrame() * 2 / 3,
                                inner.getData().getEndFrame() * 2 / 3);
                    compressTopLevel(value, 3, 2, 3);
                    addConstant(value, 3, 21, 2, 0, 50);
                });

        appendOne(mek, waz, sidestep, "NAGE", "サイドステップ・地走", "DX_SIDESTEP_GROUND", 14,
                "跳び上がらず、地面を横へ滑って蹴る。相手の近くに残りやすく、ダガーへのつなぎに向く。高く浮いた相手には届きにくい。", value -> {
                    CEventHeight height = at(value, 1, 20, CEventHeight.class, 14, 15);
                    requireTrackingJump(height);
                    requireEventCount(value, 1, 20, 1);
                    SkillUnit tracking = unit(value, 1, 20);
                    if (!tracking.getSkillInfoUnknownList().isEmpty()) throw invalid("追尾跳躍slotに未審査のFFが存在する");
                    value.getPhasesInfo().get(0).getSkillUnitCollection().remove(tracking);
                    replaceSix(at(value, 1, 19, CEventValRandom.class, 29, 46),
                            List.of(0, 0, 0, 0, -4, 0), List.of(0, 0, 0, 0, 0, 0));
                    addConstant(value, 1, 19, 3, 0, 0);
                    replaceSix(at(value, 2, 19, CEventValRandom.class, 0, 9),
                            List.of(0, 6, 0, 0, 1, 0), List.of(0, 0, 0, 0, 0, 0));
                    addConstant(value, 1, 21, 2, 0, 50);
                    replaceGravity(value, 2, 0, List.of(0, 0, 0, 0, 100, 0));
                    replaceSix(at(value, 1, 18, CEventValRandom.class, 14, 28),
                            List.of(0, 40, 0, 0, 8, 0), List.of(0, 24, 0, 0, 4, 0));
                    attack(at(value, 1, 41, CEventHit.class, 18, 42), 18, 42, 80, 18, 42, 80).run();
                    CEventVal sd = at(value, 1, 47, CEventVal.class, 0, 53);
                    requireVal(sd, 0, 3, 0, 3);
                    requireEventCount(value, 1, 47, 1);
                    if (!unit(value, 1, 47).getSkillInfoUnknownList().isEmpty())
                        throw invalid("地走の取消slotに未審査のFFが存在する");
                    sd.setEndFrame(42);
                    addGate(value, 1, 43, ordinaryGate);
                    at(value, 1, 71, CEventChange.class, 53, 53);
                    at(value, 2, 71, CEventChange.class, 29, 29);
                });

        appendOne(mek, waz, pulse, "0001", "パルス・三点射", "DX_PULSE_THREE", 12,
                "最大三発の短い連射で相手を牽制する。撃ち続けずに接近へ切り替えやすく、残量やボタンを離すタイミングによっては早く終わる。", value -> {
                    CEventVal budget = at(value, 1, 35, CEventVal.class, 0, 0);
                    requireVal(budget, 0, 0, 0, 15);
                    requireVal(at(value, 2, 35, CEventVal.class, 0, 0), 0, 0, 1, -1);
                    requirePulseLoop(value);
                    requireRealLaunch(value, 2, 3);
                    requireEventCount(value, 2, 48, 2);
                    requireNormalEnd(at(value, 3, 71, CEventChange.class, 23, 23));
                    budget.setInt4(3);
                });

        appendOne(mek, waz, sniper, "0002", "狙撃・一射離脱", "DX_SNIPER_SINGLE", 42,
                "構えの入りを短くし、一射を終えると早めに構えを解く狙撃。照準を合わせる手順は必要で、外した後は移動や設置へ切り替えたい。", value -> {
                    requireTimeline(value, 1, SNIPER_PREP_CLOCKS);
                    requireNormalEnd(at(value, 1, 71, CEventChange.class, 40, 40));
                    requireRealLaunch(value, 2, 0); // native aiming child remains at its original entry
                    at(value, 2, 71, CEventChange.class, 200, 200);
                    requireVal(at(value, 3, 35, CEventVal.class, 0, 0), 0, 0, 1, -1);
                    requireRealLaunch(value, 3, 3);
                    requireEventCount(value, 3, 48, 3);
                    CEventEffect lateDecoration = at(value, 3, 48, CEventEffect.class, 40, 40);
                    if (!Integer.valueOf(0).equals(effectReference(lateDecoration).getWazFileNo()))
                        throw invalid("狙撃P3後半Effectが装飾ではない");
                    requireSniperExits(value);
                    requireNormalEnd(at(value, 4, 71, CEventChange.class, 40, 40));
                    compressTopLevel(value, 1, 3, 5);
                    replaceSix(at(value, 3, 18, CEventValRandom.class, 3, 7),
                            List.of(0, 4, 0, 0, 0, 0), List.of(0, 0, 0, 0, 0, 0));
                    unit(value, 3, 48).getSkillInfoObjectList().remove(lateDecoration);
                    CEventChange end = copy(normalEnd, CEventChange.class);
                    setFrames(end, 24, 24);
                    unit(value, 3, 71).setSkillInfoObjectList(new ArrayList<>(List.of(end)));
                });

        appendOne(mek, waz, mine, "0009", "マイン・置き逃げ", "DX_MINE_RETREAT", 16,
                "地面に一枚のマインを残し、低い姿勢で間合いを外す。追ってくる相手を牽制し、離れた位置から射撃を組み立てる。", value -> {
                    replaceSix(at(value, 1, 19, CEventValRandom.class, 10, 25),
                            List.of(0, 10, 0, 0, -4, 0), List.of(0, 0, 0, 0, 0, 0));
                    addConstant(value, 1, 19, 3, 0, 0);
                    replaceGravity(value, 1, 10, List.of(0, 100, 0, 0, 150, 0));
                    addConstant(value, 1, 21, 2, 0, 50);
                    requireRealLaunch(value, 1, 10);
                    requireEventCount(value, 1, 48, 1);
                    replaceSix(at(value, 1, 18, CEventValRandom.class, 10, 25),
                            List.of(0, 24, 0, 0, 4, 0), List.of(0, 16, 0, 0, 0, 0));
                    addConstant(value, 1, 18, 3, 26, 0);
                    requireVal(at(value, 1, 47, CEventVal.class, 3, 9), 0, 3, 0, 3);
                    requireVal(at(value, 1, 47, CEventVal.class, 10, 10), 0, 3, 0, 1);
                    requireVal(at(value, 1, 47, CEventVal.class, 11, 46), 0, 3, 0, 3);
                    requireChange(at(value, 1, 71, CEventChange.class, 31, 129), 1,
                            List.of(List.of(2, 7, 16)), List.of(List.of()), List.of(List.of(0, 0)), List.of(0, 0), List.of());
                    at(value, 2, 71, CEventChange.class, 28, 28);
                });
    }

    private static void appendOne(Mek mek, Waz waz, Waz.Skill source, String sequence,
                                  String name, String english, int heat, String description,
                                  Consumer<Waz.Skill> edit) {
        requireReviewedSuffix(source);
        var sourceWeapon = weapon(mek, waz, source, source.getSkillNameJapanese(), sequence);
        CharacterWeaponVariants.append(mek, waz, sourceWeapon, source, name, english, heat, description, edit);
    }

    private static <T extends SkillInfoObject> T at(Waz.Skill value, int phase, int slot, Class<T> type, int start, int end) {
        return event(value, phase, slot, type, start, end, start, end);
    }

    private static SkillUnit unit(Waz.Skill value, int phase, int slot) {
        var units = value.getPhasesInfo().get(phase - 1).getSkillUnitCollection();
        var matches = units.stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        if (matches.size() != 1) throw invalid("P" + phase + " slot" + slot + " が一意ではない");
        return matches.get(0);
    }

    private static void requireEventCount(Waz.Skill value, int phase, int slot, int expected) {
        if (unit(value, phase, slot).getSkillInfoObjectList().size() != expected)
            throw invalid("P" + phase + " slot" + slot + " イベント数が変更された");
    }

    private static void requireSix(CEventValRandom value, int type, List<Integer> expected) {
        if (!Integer.valueOf(type).equals(value.getTypeId()) || !expected.equals(Arrays.asList(value.getInt1(), value.getInt2(),
                value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6())))
            throw invalid("速度・升力・重力の六整数署名が変更された");
    }

    private static void replaceSix(CEventValRandom value, List<Integer> expected, List<Integer> target) {
        requireSix(value, 3, expected);
        setSix(value, target);
    }

    private static void setSix(CEventValRandom value, List<Integer> target) {
        value.setInt1(target.get(0)); value.setInt2(target.get(1)); value.setInt3(target.get(2));
        value.setInt4(target.get(3)); value.setInt5(target.get(4)); value.setInt6(target.get(5));
    }

    private static void replaceGravity(Waz.Skill value, int phase, int frame, List<Integer> expected) {
        CEventValRandom gravity = at(value, phase, 21, CEventValRandom.class, frame, frame);
        requireSix(gravity, 2, expected);
        setSix(gravity, List.of(0, 50, 0, 0, 50, 0));
    }

    private static void requireVal(CEventVal value, int a, int b, int c, int d) {
        if (!Integer.valueOf(0).equals(value.getTypeId())
                || !List.of(a, b, c, d).equals(Arrays.asList(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4())))
            throw invalid("取消またはchannel0署名が変更された");
    }

    private static void addConstant(Waz.Skill skill, int phase, int slot, int type, int frame, int number) {
        if ((slot == 21 && type != 2) || ((slot == 18 || slot == 19) && type != 3))
            throw invalid("定数イベントの型が槽位に合わない");
        SkillUnit target = additionalUnit(skill, phase, slot);
        if (target.getSkillInfoObjectList().stream().anyMatch(e -> frames(e, frame, frame)))
            throw invalid("定数イベントの追加位置が既に占有されている");
        CEventValRandom value = new CEventValRandom(type);
        setFrames(value, frame, frame);
        value.setSlotNum(slot);
        setSix(value, List.of(0, number, 0, 0, number, 0));
        target.getSkillInfoObjectList().add(value);
        target.getSkillInfoObjectList().sort(Comparator.comparing(SkillInfoObject::getStartFrame));
    }

    private static void addGate(Waz.Skill skill, int phase, int frame, CEventVal donor) {
        CEventVal gate = copy(donor, CEventVal.class);
        setFrames(gate, frame, frame);
        SkillUnit target = additionalUnit(skill, phase, 47);
        if (target.getSkillInfoObjectList().stream().anyMatch(e -> frames(e, frame, frame)))
            throw invalid("取消窓が既に占有されている");
        gate.setSlotNum(47);
        target.getSkillInfoObjectList().add(gate);
        target.getSkillInfoObjectList().sort(Comparator.comparing(SkillInfoObject::getStartFrame));
    }

    private static SkillUnit additionalUnit(Waz.Skill skill, int phase, int slot) {
        var units = skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection();
        var matches = units.stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        if (matches.size() > 1) throw invalid("追加先の槽位が重複している");
        if (matches.size() == 1) return matches.get(0);
        SkillUnit created = new SkillUnit(slot, "追加武装の専用イベント");
        units.add(created);
        units.sort(Comparator.comparing(SkillUnit::getUnitQuantity));
        return created;
    }

    private static void requireTrackingJump(CEventHeight height) {
        if (!Integer.valueOf(8).equals(height.getTypeId()) || !Integer.valueOf(0).equals(height.getInt1())
                || height.getCeventHeightUnitList().size() != 2) throw invalid("追尾跳躍の構造が変更された");
        for (int slot : List.of(1, 2)) {
            var matches = height.getCeventHeightUnitList().stream()
                    .filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum())).toList();
            if (matches.size() != 1 || !Integer.valueOf(1).equals(matches.get(0).getBuffer())
                    || !(matches.get(0).getData() instanceof CEventValRandom number))
                throw invalid("追尾跳躍の内部供体が変更された");
            requireSix(number, slot == 1 ? 3 : 2,
                    slot == 1 ? List.of(0, 500, 0, 0, 500, 0) : List.of(0, 20, 0, 0, 20, 0));
        }
    }

    private static CEventWazaSelect effectReference(CEventEffect effect) {
        var matches = effect.getCeventEffectUnitList().stream()
                .filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())).toList();
        if (matches.size() != 1 || !Integer.valueOf(1).equals(matches.get(0).getBuffer())
                || !(matches.get(0).getData() instanceof CEventWazaSelect ref)
                || !Integer.valueOf(4).equals(ref.getTypeId()) || ref.getWazFileNo() == null
                || ref.getWazSequenceNo() == null || ref.getWazSequenceNo() < 0)
            throw invalid("EffectのWAZ参照署名が変更された");
        return ref;
    }

    private static CEventEffect requireRealLaunch(Waz.Skill skill, int phase, int frame) {
        var matches = unit(skill, phase, 48).getSkillInfoObjectList().stream()
                .filter(CEventEffect.class::isInstance).map(CEventEffect.class::cast)
                .filter(e -> frames(e, frame, frame) && effectReference(e).getWazFileNo() > 0).toList();
        if (matches.size() != 1) throw invalid("実弾発射供体が一意ではない");
        CEventEffect launch = matches.get(0);
        if (!Integer.valueOf(1).equals(launch.getInt1())) throw invalid("実弾発射の外側タイプが変更された");
        var count = launch.getCeventEffectUnitList().stream()
                .filter(u -> Integer.valueOf(4).equals(u.getUnitSlotNum())).toList();
        if (count.size() != 1 || !Integer.valueOf(1).equals(count.get(0).getBuffer())
                || !(count.get(0).getData() instanceof CEventValRandom number))
            throw invalid("実弾発射数が不明");
        requireSix(number, 2, List.of(0, 1, 0, 0, 1, 0));
        return launch;
    }

    private static void requireDecorationWindow(CEventEffect value, int start, int end) {
        if (!Integer.valueOf(0).equals(effectReference(value).getWazFileNo())
                || value.getCeventEffectUnitList().size() != 8) throw invalid("速刺の相対Effect供体が変更された");
        Set<Integer> slots = new HashSet<>();
        for (var inner : value.getCeventEffectUnitList()) {
            if (!Integer.valueOf(1).equals(inner.getBuffer()) || !frames(inner.getData(), start, end))
                throw invalid("速刺Effectの相対窓が変更された");
            slots.add(inner.getUnitSlotNum());
        }
        if (!slots.equals(Set.of(3, 4, 6, 7, 16, 28, 29, 31))) throw invalid("速刺Effectの内部槽位が変更された");
    }

    private static void requirePulseLoop(Waz.Skill value) {
        requireEventCount(value, 2, 71, 2);
        var changes = unit(value, 2, 71).getSkillInfoObjectList();
        if (!(changes.get(0) instanceof CEventChange loop) || !(changes.get(1) instanceof CEventChange fallback)
                || !frames(loop, 6, 6) || !frames(fallback, 6, 6)) throw invalid("パルスのループ窓が変更された");
        requireChange(loop, 3, List.of(List.of(2, 5, 2, 5), List.of(2, 0, 0, 5), List.of(2, 4, 1)),
                List.of(List.of(0), List.of(0), List.of()), List.of(List.of(0, 0), List.of(0, 0), List.of()),
                List.of(0, 1), List.of(1));
        requireNormalEnd(fallback);
    }

    private static void requireSniperExits(Waz.Skill value) {
        requireEventCount(value, 3, 71, 2);
        var changes = unit(value, 3, 71).getSkillInfoObjectList();
        if (!(changes.get(0) instanceof CEventChange empty) || !(changes.get(1) instanceof CEventChange repeat)
                || !frames(empty, 57, 57) || !frames(repeat, 57, 57)) throw invalid("狙撃の射撃後分岐が変更された");
        requireChange(empty, 1, List.of(List.of(2, 5, 2, 2)), List.of(List.of(0)), List.of(List.of(0, 0)),
                List.of(0, 0), List.of());
        requireChange(repeat, 1, List.of(List.of(0)), List.of(List.of()), List.of(List.of()),
                List.of(0, 1), List.of(1));
    }

    private static void requireChange(CEventChange value, int flag, List<List<Integer>> types,
                                      List<List<Integer>> params, List<List<Integer>> objects,
                                      List<Integer> targetTypes, List<Integer> targetParams) {
        if (!Integer.valueOf(flag).equals(value.getFlag()) || !Integer.valueOf(0).equals(value.getInt1())
                || value.getBsdxInfoCollectionList1().size() != types.size()
                || value.getBsdxInfoCollectionList2().size() != 1) throw invalid("Change構造が変更された");
        for (int i = 0; i < types.size(); i++)
            requireCollection(value.getBsdxInfoCollectionList1().get(i), 0, types.get(i), params.get(i), objects.get(i));
        requireCollection(value.getBsdxInfoCollectionList2().get(0), 16, targetTypes, targetParams, List.of());
    }

    private static void requireCollection(BsdxInfoCollection value, int root, List<Integer> types,
                                          List<Integer> params, List<Integer> objects) {
        if (!Integer.valueOf(root).equals(value.getInt1()) || !types.equals(value.getTypeList())
                || !params.equals(value.getParamList()) || !objects.equals(value.getIntList3())
                || !List.of().equals(value.getIntList4()) || !Integer.valueOf(0).equals(value.getInt2()))
            throw invalid("条件・遷移式の署名が変更された");
    }

    private static void requireTimeline(Waz.Skill value, int phase, Map<Integer, List<Integer>> expected) {
        Map<Integer, List<Integer>> actual = new LinkedHashMap<>();
        for (var unit : value.getPhasesInfo().get(phase - 1).getSkillUnitCollection()) {
            if (unit.getSkillInfoObjectList().isEmpty()) continue;
            List<Integer> clocks = new ArrayList<>();
            for (var event : unit.getSkillInfoObjectList()) {
                if (event.getStartFrame() == null || event.getEndFrame() == null
                        || event.getStartFrame() < 0 || event.getEndFrame() < event.getStartFrame())
                    throw invalid("圧縮対象の外側時刻が無効");
                clocks.add(event.getTypeId()); clocks.add(event.getStartFrame()); clocks.add(event.getEndFrame());
            }
            if (actual.put(unit.getUnitQuantity(), clocks) != null) throw invalid("圧縮対象の槽位が重複");
        }
        if (!expected.equals(actual)) throw invalid("圧縮対象phaseの外側時刻署名が変更された");
    }

    private static void compressTopLevel(Waz.Skill value, int phase, int numerator, int denominator) {
        for (var unit : value.getPhasesInfo().get(phase - 1).getSkillUnitCollection())
            for (var event : unit.getSkillInfoObjectList())
                setFrames(event, event.getStartFrame() * numerator / denominator, event.getEndFrame() * numerator / denominator);
    }

    private static void setFrames(SkillInfoObject value, int start, int end) {
        value.setStartFrame(start); value.setEndFrame(end);
    }

    private static void requireReviewedSuffix(Waz.Skill source) {
        List<List<Integer>> expected = switch (source.getSkillNameEnglish()) {
            case "0007" -> List.of(List.of(0, 0));
            case "SIDESTEPKICK04", "MINE_S05" -> List.of(List.of(0, 0), List.of(1, 0));
            case "RIFLE4" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0));
            case "STRAIGHT04", "RIFLE04" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0));
            default -> throw invalid("未審査の供体suffixです: " + source.getSkillNameEnglish());
        };
        if (source.getSkillSuffixList() == null || source.getSkillSuffixList().size() != expected.size())
            throw invalid("供体suffixの件数が変更されています: " + source.getSkillNameEnglish());
        for (int i = 0; i < expected.size(); i++) {
            var entry = source.getSkillSuffixList().get(i);
            if (entry == null || !Objects.equals(expected.get(i).get(0), entry.getInt1())
                    || !Objects.equals(expected.get(i).get(1), entry.getInt2()))
                throw invalid("供体suffixの入口またはskip標識が変更されています: " + source.getSkillNameEnglish());
        }
    }

    private static <T> T copy(T value, Class<T> type) {
        try {
            return JSON.readValue(JSON.writeValueAsBytes(value), type);
        } catch (IOException error) {
            throw new IllegalStateException("Yuri追加武装の独立DTO複製に失敗", error);
        }
    }

    private static IllegalStateException invalid(String text) {
        return new IllegalStateException("Yuri追加武装の供体を拒否: " + text);
    }

    // typeId/start/end triples: only these reviewed outer-clock fields may be compressed.
    private static final Map<Integer, List<Integer>> ASSASSIN_ATTACK_CLOCKS = Map.ofEntries(
            Map.entry(0, List.of(13,0,29, 13,30,45, 13,46,75, 13,76,80, 13,81,86, 13,87,92)),
            Map.entry(6, List.of(14,0,45, 14,46,67)), Map.entry(7, List.of(15,0,2)),
            Map.entry(10, List.of(3,0,29)), Map.entry(15, List.of(10,0,0)), Map.entry(16, List.of(3,0,0)),
            Map.entry(18, List.of(3,0,0, 3,36,43, 3,44,46)), Map.entry(27, List.of(0,76,92)),
            Map.entry(41, List.of(16,44,50)), Map.entry(45, List.of(17,0,48)), Map.entry(48, List.of(19,0,52)),
            Map.entry(51, List.of(22,0,0)), Map.entry(52, List.of(23,43,43)), Map.entry(53, List.of(24,60,60)),
            Map.entry(71, List.of(34,44,50, 34,92,92)));
    private static final Map<Integer, List<Integer>> SNIPER_PREP_CLOCKS = Map.ofEntries(
            Map.entry(0, List.of(13,0,2, 13,3,5, 13,6,8, 13,9,11, 13,12,17,
                    13,18,21, 13,22,24, 13,25,27, 13,28,33, 13,34,39)),
            Map.entry(24, List.of(0,0,5)), Map.entry(26, List.of(0,0,0)), Map.entry(28, List.of(0,0,5)),
            Map.entry(45, List.of(17,28,39)), Map.entry(52, List.of(23,0,0, 23,34,34)),
            Map.entry(54, List.of(0,34,39)), Map.entry(70, List.of(0,0,17)), Map.entry(71, List.of(34,40,40)));
}
