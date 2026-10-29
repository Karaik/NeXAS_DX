package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;

import java.io.IOException;
import java.util.*;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

/** Motoki私有的起飞、准备、投弹、接地链；不改变通用变体的单phase约束。 */
public final class MotokiFlightBombVariant {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String NAME = "集中爆撃・帰投";
    private static final String ROUTER = "DX_FOCUSED_BOMB_RETURN";
    private static final String EXECUTION_NAME = "集中爆撃・帰投：制御";
    private static final String EXECUTION = "DX_FOCUSED_BOMB_EXECUTION";
    private static final String DESCRIPTION = "自ら離陸し、投下準備後に敵との中心高度差を再確認して六枚の爆弾を投下する。高度が足りなければ上昇をやり直し、確保できない場合は投下せず帰投する。爆撃後は降下し、着地して動作を終える。";

    private MotokiFlightBombVariant() { }

    public static Mek.MekWeaponInfo append(Mek mek, Waz waz) {
        if (mek == null || waz == null || mek.getMekBasicInfo() == null
                || mek.getMekWeaponInfoMap() == null || waz.getSkillList() == null
                || mek.getMekBasicInfo().getWazFileSequence() == null
                || mek.getMekBasicInfo().getWazFileSequence() < 0) throw invalid("目标已重绑MEK/WAZ缺失");
        // 包括通用append在内的一切可失败操作只作用于独立副本。
        Mek stagedMek = copy(mek, Mek.class);
        Waz stagedWaz = copy(waz, Waz.class);
        Existing existing = removeExisting(stagedMek, stagedWaz);
        int weaponIndex = stagedMek.getMekWeaponInfoMap().size();
        int routerIndex = stagedWaz.getSkillList().size();
        var bomb = donor(stagedWaz, "飛行形態：集中爆撃", "0023");
        var takeoff = donor(stagedWaz, "飛行形態：開始", "BOOST_DASH4");
        var flying = donor(stagedWaz, "飛行形態：飛行中", "BOOST_DASH5");
        var landing = donor(stagedWaz, "飛行形態：終了", "BOOST_DASH6");
        var bombWeapon = weapon(stagedMek, stagedWaz, bomb, "飛行形態：集中爆撃", "0020");
        var takeoffWeapon = weapon(stagedMek, stagedWaz, takeoff, "飛行形態：開始", "0014");
        var landingWeapon = weapon(stagedMek, stagedWaz, landing, "飛行形態：終了", "0015");
        int group = stagedMek.getMekBasicInfo().getWazFileSequence();
        guardDonors(takeoff, bomb, landing, group, stagedWaz.getSkillList().indexOf(flying));
        Waz.Skill execution = execution(takeoff, bomb, landing);
        CharacterWeaponVariants.append(stagedMek, stagedWaz, bombWeapon, bomb,
                NAME, ROUTER, 40, DESCRIPTION, route -> {
                    route.getPhasesInfo().get(0).setSkillUnitCollection(new ArrayList<>());
                    route.setSkillSuffixList(executableSuffixes(1));
                    setEvents(route, 71, jump(templateChange(bomb), 0, 0,
                            always(), term(16, List.of(1), List.of(group, routerIndex + 1), List.of(), List.of())));
                }, takeoffWeapon, landingWeapon);
        var addedMaterial = stagedMek.getMekMaterialBlock().getTrailingEntries().get(weaponIndex);
        addedMaterial.setSpriteGroups(distinctGroups(addedMaterial.getSpriteGroups()));
        addedMaterial.setSeGroups(distinctGroups(addedMaterial.getSeGroups()));
        addedMaterial.setVoiceGroups(distinctGroups(addedMaterial.getVoiceGroups()));
        stagedMek.getMekMaterialBlock().getEntries().set(
                stagedMek.getMekMaterialBlock().getRegularEntries().size() + weaponIndex, addedMaterial);
        stagedWaz.getSkillList().add(execution);
        if (existing.present) {
            if (!payload(mek).equals(payload(stagedMek)) || !payload(waz).equals(payload(stagedWaz)))
                throw invalid("既有路由、执行技能、武装或Material载荷已漂移");
            return mek.getMekWeaponInfoMap().get(weaponIndex);
        }

        // 预先构造全部提交视图；此后仅赋值，原武装、技能、AI和Material行保留原对象。
        Mek.MekWeaponInfo added = stagedMek.getMekWeaponInfoMap().get(weaponIndex);
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(mek.getMekWeaponInfoMap());
        weapons.put(weaponIndex, added);
        List<Waz.Skill> skills = new ArrayList<>(waz.getSkillList());
        skills.add(stagedWaz.getSkillList().get(routerIndex)); skills.add(execution);
        var block = mek.getMekMaterialBlock();
        List<Mek.MekMaterialBlock.PluginEntry> regular = new ArrayList<>(block.getRegularEntries());
        List<Mek.MekMaterialBlock.PluginEntry> trailing = new ArrayList<>(block.getTrailingEntries());
        trailing.add(stagedMek.getMekMaterialBlock().getTrailingEntries().get(weaponIndex));
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>(regular); entries.addAll(trailing);
        mek.setMekWeaponInfoMap(weapons);
        waz.setSkillList(skills);
        block.setRegularEntries(regular); block.setTrailingEntries(trailing); block.setEntries(entries);
        return added;
    }

    private static Existing removeExisting(Mek mek, Waz waz) {
        var weapons = mek.getMekWeaponInfoMap().values().stream().filter(Objects::nonNull)
                .filter(w -> NAME.equals(w.getWeaponName()) || EXECUTION_NAME.equals(w.getWeaponName())).toList();
        var routes = waz.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> NAME.equals(s.getSkillNameJapanese()) || ROUTER.equals(s.getSkillNameEnglish())).toList();
        var executions = waz.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> EXECUTION_NAME.equals(s.getSkillNameJapanese()) || EXECUTION.equals(s.getSkillNameEnglish())).toList();
        if (weapons.isEmpty() && routes.isEmpty() && executions.isEmpty()) return new Existing(false);
        int row = mek.getMekWeaponInfoMap().size() - 1;
        int routeIndex = waz.getSkillList().size() - 2;
        if (weapons.size() != 1 || routes.size() != 1 || executions.size() != 1 || row < 0 || routeIndex < 0
                || !NAME.equals(weapons.get(0).getWeaponName())
                || mek.getMekWeaponInfoMap().get(row) != weapons.get(0)
                || !Integer.valueOf(routeIndex).equals(weapons.get(0).getWazSequence())
                || waz.getSkillList().get(routeIndex) != routes.get(0)
                || waz.getSkillList().get(routeIndex + 1) != executions.get(0)
                || !NAME.equals(routes.get(0).getSkillNameJapanese()) || !ROUTER.equals(routes.get(0).getSkillNameEnglish())
                || !EXECUTION_NAME.equals(executions.get(0).getSkillNameJapanese()) || !EXECUTION.equals(executions.get(0).getSkillNameEnglish()))
            throw invalid("路由与执行技能必须完整、双名唯一并按顺序位于本角色尾部");
        var block = mek.getMekMaterialBlock();
        if (block == null || block.getTrailingEntries() == null || block.getRegularEntries() == null
                || block.getEntries() == null || block.getTrailingEntries().size() != row + 1)
            throw invalid("既有变体Material布局不完整");
        List<Mek.MekMaterialBlock.PluginEntry> combined = new ArrayList<>(block.getRegularEntries());
        combined.addAll(block.getTrailingEntries());
        if (!payload(combined).equals(payload(block.getEntries()))) throw invalid("既有Material三视图不一致");
        mek.getMekWeaponInfoMap().remove(row);
        waz.getSkillList().subList(routeIndex, routeIndex + 2).clear();
        block.getTrailingEntries().remove(row);
        block.getEntries().remove(block.getRegularEntries().size() + row);
        return new Existing(true);
    }

    private static Waz.Skill donor(Waz waz, String japanese, String english) {
        var skill = skill(waz, japanese, english, 1);
        if (!Integer.valueOf(1).equals(skill.getPhaseQuantity())) throw invalid(japanese + " phaseQuantity漂移");
        var suffixes = skill.getSkillSuffixList();
        if (suffixes == null || suffixes.size() != 1 || suffixes.get(0) == null
                || !Integer.valueOf(0).equals(suffixes.get(0).getInt1())
                || !Integer.valueOf(0).equals(suffixes.get(0).getInt2()))
            throw invalid(japanese + " 单block供体suffix必须恰为(0,0)");
        return skill;
    }

    private static void guardDonors(Waz.Skill takeoff, Waz.Skill bomb, Waz.Skill landing, int group, int flyingIndex) {
        requireBombSliceLayout(bomb);
        requireEmptySlots(takeoff, 18, 27, 28);
        requireEmptySlots(bomb, 21, 27, 28);
        requireEmptySlots(landing, 18, 27);
        requireSprites(takeoff, new int[][]{{0, 9}, {10, 18}});
        requireSprites(bomb, new int[][]{{0, 5}, {6, 9}, {10, 59}, {60, 89}});
        requireSprites(landing, new int[][]{{0, 7}, {8, 30}});
        requireValues(single(takeoff, 19, CEventValRandom.class, 0, 18), 3, List.of(0, 8, 0, 0, 1, 0));
        requireValues(single(takeoff, 21, CEventValRandom.class, 0, 0), 2, List.of(0, 0, 0, 0, 0, 0));
        var heatUnit = rawUnit(takeoff, 40);
        if (heatUnit.getSkillInfoObjectList().size() != 1
                || !(heatUnit.getSkillInfoObjectList().get(0) instanceof CEventVal heat)
                || !frames(heat, 0, 0) || !Integer.valueOf(0).equals(heat.getTypeId())
                || !List.of(1, 0, 1, -1000).equals(List.of(heat.getInt1(), heat.getInt2(), heat.getInt3(), heat.getInt4())))
            throw invalid("起飞热量签名漂移");
        var heatLoops = heatUnit.getSkillInfoUnknownList();
        if (heatLoops.size() != 1 || !frames(heatLoops.get(0), 0, 0)
                || !Integer.valueOf(255).equals(heatLoops.get(0).getTypeId())
                || !List.of(-1, 1, -2).equals(List.of(heatLoops.get(0).getInt1(), heatLoops.get(0).getInt2(), heatLoops.get(0).getInt3())))
            throw invalid("起飞热量loop签名漂移");
        requireJump(single(takeoff, 71, CEventChange.class, 18, 18), group, flyingIndex);
        requireValues(onlyAt(bomb, 18, CEventValRandom.class, 0, 11, 2), 3, List.of(1, 0, 0, 0, 12, 0));
        requireValues(onlyAt(bomb, 18, CEventValRandom.class, 12, 59, 2), 3, List.of(1, 0, 0, 0, 8, 0));
        requireValues(onlyAt(bomb, 19, CEventValRandom.class, 0, 35, 2), 3, List.of(1, 0, 0, 1, 3, 0));
        requireValues(onlyAt(bomb, 19, CEventValRandom.class, 36, 89, 2), 3, List.of(1, 0, 0, 0, 0, 0));
        var effect = single(bomb, 48, CEventEffect.class, 10, 10);
        var amounts = effect.getCeventEffectUnitList().stream().filter(v -> Integer.valueOf(4).equals(v.getUnitSlotNum())).toList();
        if (amounts.size() != 1 || !Integer.valueOf(1).equals(amounts.get(0).getBuffer())
                || !(amounts.get(0).getData() instanceof CEventValRandom amount)
                || !List.of(0, 1, 0, 0, 6, 0).equals(values(amount))) throw invalid("十帧六枚投弹签名漂移");
        requireJump(single(bomb, 71, CEventChange.class, 89, 89), group, flyingIndex);
        requireValues(single(landing, 19, CEventValRandom.class, 15, 15), 3, List.of(0, 0, 0, 0, 0, 0));
        requireValues(single(landing, 21, CEventValRandom.class, 15, 15), 2, List.of(0, 0, 0, 0, -20, 0));
        var modifier = single(landing, 28, CEventVal.class, 0, 15);
        if (!Integer.valueOf(0).equals(modifier.getTypeId())
                || !List.of(1, 0, 0, 0).equals(List.of(modifier.getInt1(), modifier.getInt2(), modifier.getInt3(), modifier.getInt4())))
            throw invalid("结束JumpPower修正签名漂移");
        requireNormalEnd(single(landing, 71, CEventChange.class, 30, 30));
    }

    private static Waz.Skill execution(Waz.Skill takeoff, Waz.Skill bomb, Waz.Skill landing) {
        Waz.Skill result = copy(bomb, Waz.Skill.class);
        result.setSkillNameJapanese(EXECUTION_NAME); result.setSkillNameEnglish(EXECUTION);
        result.setPhaseQuantity(4);
        // TURN/INPUT寻址suffix入口；每条int1再选择实际block，不能继承供体的单入口。
        result.setSkillSuffixList(executableSuffixes(4));
        Waz.Skill p0 = copy(takeoff, Waz.Skill.class);
        Waz.Skill p1 = sliceBomb(bomb, 0, 9);
        Waz.Skill p2 = sliceBomb(bomb, 10, 89);
        Waz.Skill p3 = copy(landing, Waz.Skill.class);
        CEventValRandom horizontal = onlyAt(bomb, 18, CEventValRandom.class, 0, 11, 2);
        CEventValRandom vertical = single(takeoff, 19, CEventValRandom.class, 0, 18);
        CEventValRandom gravity = single(takeoff, 21, CEventValRandom.class, 0, 0);
        CEventVal modifier = single(landing, 28, CEventVal.class, 0, 15);
        setEvents(p0, 18, vector(horizontal, 0, 239, 0));
        setEvents(p0, 19, vector(vertical, 0, 239, 8));
        setEvents(p0, 21, vector(gravity, 0, 239, 0));
        modifiers(p0, modifier, 239);
        rawUnit(p0, 40).setSkillInfoUnknownList(new ArrayList<>());
        setEvents(p0, 40);
        setEvents(p0, 71,
                jump(templateChange(takeoff), 0, 0, height(), phase(1)),
                jump(templateChange(takeoff), 18, 239, height(), phase(1)),
                jump(templateChange(takeoff), 240, 240, always(), phase(3)));
        setEvents(p1, 19, vector(vertical, 0, 10, 0));
        setEvents(p1, 21, vector(gravity, 0, 10, 0));
        modifiers(p1, modifier, 10);
        // 611820以first-match=1消费Change；高度成立后不会再执行同帧兜底。
        // 换phase会排队重入，目标可能先更新，不能把此门称为同步发弹保证。
        setEvents(p1, 71,
                jump(templateChange(bomb), 10, 10, height(), phase(2)),
                jump(templateChange(bomb), 10, 10, always(), phase(0)));
        setEvents(p2, 19, vector(vertical, 0, 79, 0));
        setEvents(p2, 21, vector(gravity, 0, 79, 0));
        modifiers(p2, modifier, 79);
        setEvents(p2, 71, jump(templateChange(bomb), 79, 79, always(), phase(3)));
        setEvents(p3, 18, vector(horizontal, 0, 360, 0));
        setEvents(p3, 19, vector(vertical, 0, 0, 0));
        // 原生退出及下招START只将重力最低抬到50；不用会向后招泄漏的200。
        setEvents(p3, 21, vector(gravity, 0, 360, 50));
        modifiers(p3, modifier, 360);
        setEvents(p3, 71,
                jump(templateChange(landing), 30, 359, term(0, List.of(2, 7, 16), List.of(), List.of(0, 0), List.of()), end()),
                jump(templateChange(landing), 360, 360, always(), end()));
        result.setPhasesInfo(new ArrayList<>(List.of(p0.getPhasesInfo().get(0), p1.getPhasesInfo().get(0),
                p2.getPhasesInfo().get(0), p3.getPhasesInfo().get(0))));
        return result;
    }

    /** 只切已校验的普通事件；跨界ValRandom按新窗口重新插值，原技能不改。 */
    private static Waz.Skill sliceBomb(Waz.Skill source, int firstFrame, int lastFrame) {
        Waz.Skill result = copy(source, Waz.Skill.class);
        var units = result.getPhasesInfo().get(0).getSkillUnitCollection();
        for (var unit : units) {
            List<SkillInfoObject> retained = new ArrayList<>();
            for (var event : unit.getSkillInfoObjectList()) {
                if (event.getEndFrame() < firstFrame || event.getStartFrame() > lastFrame) continue;
                event.setStartFrame(Math.max(event.getStartFrame(), firstFrame) - firstFrame);
                event.setEndFrame(Math.min(event.getEndFrame(), lastFrame) - firstFrame);
                retained.add(event);
            }
            unit.setSkillInfoObjectList(retained);
        }
        units.removeIf(unit -> unit.getSkillInfoObjectList().isEmpty());
        return result;
    }

    private static void requireBombSliceLayout(Waz.Skill bomb) {
        Map<Integer, int[][]> windows = Map.ofEntries(
                Map.entry(0, new int[][]{{0, 5}, {6, 9}, {10, 59}, {60, 89}}),
                Map.entry(10, new int[][]{{0, 14}, {15, 46}}),
                Map.entry(15, new int[][]{{0, 89}}),
                Map.entry(16, new int[][]{{6, 59}, {60, 89}}),
                Map.entry(18, new int[][]{{0, 11}, {12, 59}}),
                Map.entry(19, new int[][]{{0, 35}, {36, 89}}),
                Map.entry(24, new int[][]{{0, 0}}),
                Map.entry(47, new int[][]{{10, 10}}),
                Map.entry(48, new int[][]{{10, 10}}),
                Map.entry(52, new int[][]{{0, 0}, {9, 9}}),
                Map.entry(53, new int[][]{{0, 0}}),
                Map.entry(71, new int[][]{{89, 89}}));
        Set<Integer> seen = new HashSet<>();
        for (var candidate : bomb.getPhasesInfo().get(0).getSkillUnitCollection()) {
            if (candidate == null || candidate.getUnitQuantity() == null || !seen.add(candidate.getUnitQuantity())
                    || !windows.containsKey(candidate.getUnitQuantity())) throw invalid("投弹切片槽布局漂移");
            var unit = unit(bomb, candidate.getUnitQuantity());
            int[][] expected = windows.get(candidate.getUnitQuantity());
            var prototype = SkillInfoFactory.createEventObjectBsdx(candidate.getUnitQuantity());
            if (unit.getSkillInfoObjectList().size() != expected.length) throw invalid("投弹切片事件数量漂移");
            for (int i = 0; i < expected.length; i++) {
                var event = unit.getSkillInfoObjectList().get(i);
                if (event == null || event.getClass() != prototype.getClass()
                        || !Objects.equals(event.getTypeId(), prototype.getTypeId())
                        || !frames(event, expected[i][0], expected[i][1]))
                    throw invalid("投弹切片事件类型或窗口漂移");
            }
        }
        if (!seen.equals(windows.keySet())) throw invalid("投弹切片缺少原有槽");
    }

    private static void modifiers(Waz.Skill skill, CEventVal source, int lastFrame) {
        for (int slot : List.of(27, 28)) {
            CEventVal value = copy(source, CEventVal.class);
            value.setSlotNum(slot); value.setStartFrame(0); value.setEndFrame(lastFrame);
            value.setInt1(0); value.setInt2(100); value.setInt3(0); value.setInt4(100);
            setEvents(skill, slot, value);
        }
    }
    private static List<Waz.Skill.SkillSuffix> executableSuffixes(int blocks) {
        List<Waz.Skill.SkillSuffix> suffixes = new ArrayList<>();
        for (int block = 0; block < blocks; block++) {
            var suffix = new Waz.Skill.SkillSuffix(); suffix.setInt1(block); suffix.setInt2(0);
            suffixes.add(suffix);
        }
        return suffixes;
    }
    private static CEventValRandom vector(CEventValRandom source, int start, int end, int value) {
        CEventValRandom result = copy(source, CEventValRandom.class);
        result.setStartFrame(start); result.setEndFrame(end);
        result.setInt1(0); result.setInt2(value); result.setInt3(0);
        result.setInt4(0); result.setInt5(value); result.setInt6(0);
        return result;
    }
    private static CEventChange jump(CEventChange source, int start, int end, BsdxInfoCollection condition, BsdxInfoCollection destination) {
        CEventChange result = copy(source, CEventChange.class);
        result.setStartFrame(start); result.setEndFrame(end); result.setFlag(1); result.setInt1(0);
        result.setBsdxInfoCollectionList1(new ArrayList<>(List.of(condition)));
        result.setBsdxInfoCollectionList2(new ArrayList<>(List.of(destination)));
        return result;
    }
    private static BsdxInfoCollection always() { return term(0, List.of(0), List.of(), List.of(), List.of()); }
    private static BsdxInfoCollection height() { return term(0, List.of(2, 3, 4, 3), List.of(200), List.of(0, 0), List.of(1, 0)); }
    private static BsdxInfoCollection phase(int phase) { return term(16, List.of(0, 1), List.of(phase), List.of(), List.of()); }
    private static BsdxInfoCollection end() { return term(16, List.of(0, 2), List.of(), List.of(), List.of()); }
    private static BsdxInfoCollection term(int root, List<Integer> types, List<Integer> params, List<Integer> aux3, List<Integer> aux4) {
        var value = new BsdxInfoCollection(); value.setInt1(root); value.setInt2(0);
        value.setTypeList(new ArrayList<>(types)); value.setParamList(new ArrayList<>(params));
        value.setIntList3(new ArrayList<>(aux3)); value.setIntList4(new ArrayList<>(aux4)); return value;
    }
    private static void requireJump(CEventChange change, int group, int skillIndex) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                || !payload(List.of(always())).equals(payload(change.getBsdxInfoCollectionList1()))
                || !payload(List.of(term(16, List.of(1), List.of(group, skillIndex), List.of(), List.of())))
                .equals(payload(change.getBsdxInfoCollectionList2()))) throw invalid("供体不再跳向本角色飞行中");
    }
    private static void requireValues(CEventValRandom event, int type, List<Integer> expected) {
        if (!Integer.valueOf(type).equals(event.getTypeId()) || !expected.equals(values(event))) throw invalid("供体向量类型或六整数漂移");
    }
    private static List<Integer> values(CEventValRandom value) {
        return Arrays.asList(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6());
    }
    private static void requireSprites(Waz.Skill skill, int[][] windows) {
        var events = unit(skill, 0).getSkillInfoObjectList();
        if (events.size() != windows.length) throw invalid("供体动画数量漂移");
        for (int i = 0; i < windows.length; i++) if (!(events.get(i) instanceof CEventSprite)
                || !frames(events.get(i), windows[i][0], windows[i][1])) throw invalid("供体动画帧漂移");
    }
    private static CEventChange templateChange(Waz.Skill skill) { return (CEventChange) unit(skill, 71).getSkillInfoObjectList().get(0); }
    private static <T extends SkillInfoObject> T single(Waz.Skill skill, int slot, Class<T> type, int start, int end) {
        return onlyAt(skill, slot, type, start, end, 1);
    }
    private static <T extends SkillInfoObject> T onlyAt(Waz.Skill skill, int slot, Class<T> type, int start, int end, int count) {
        var objects = unit(skill, slot).getSkillInfoObjectList();
        var matches = objects.stream().filter(type::isInstance).map(type::cast).filter(value -> frames(value, start, end)).toList();
        if (objects.size() != count || matches.size() != 1) throw invalid("供体slot" + slot + " 数量、类型或帧漂移");
        return matches.get(0);
    }
    private static SkillUnit unit(Waz.Skill skill, int slot) {
        var unit = rawUnit(skill, slot);
        if (!unit.getSkillInfoUnknownList().isEmpty()) throw invalid("供体slot" + slot + " 带未知载荷");
        return unit;
    }
    private static SkillUnit rawUnit(Waz.Skill skill, int slot) {
        var units = skill.getPhasesInfo().get(0).getSkillUnitCollection().stream()
                .filter(Objects::nonNull).filter(value -> Integer.valueOf(slot).equals(value.getUnitQuantity())).toList();
        if (units.size() != 1 || units.get(0).getSkillInfoObjectList() == null
                || units.get(0).getSkillInfoUnknownList() == null)
            throw invalid("供体slot" + slot + " 缺失、重复或载荷为空");
        return units.get(0);
    }
    private static void setEvents(Waz.Skill skill, int slot, SkillInfoObject... events) {
        var units = skill.getPhasesInfo().get(0).getSkillUnitCollection();
        var matches = units.stream().filter(value -> Integer.valueOf(slot).equals(value.getUnitQuantity())).toList();
        if (matches.size() > 1) throw invalid("副本slot" + slot + " 重复");
        SkillUnit unit;
        if (matches.isEmpty()) {
            unit = new SkillUnit(slot, null); units.add(unit);
        } else unit = unit(skill, slot);
        for (var event : events) event.setSlotNum(slot);
        unit.setSkillInfoObjectList(new ArrayList<>(Arrays.asList(events)));
        if (unit.getSkillInfoObjectList().isEmpty() && unit.getSkillInfoUnknownList().isEmpty()) units.remove(unit);
        units.sort(Comparator.comparing(SkillUnit::getUnitQuantity));
    }
    private static void requireEmptySlots(Waz.Skill skill, int... slots) {
        for (int slot : slots) {
            var units = skill.getPhasesInfo().get(0).getSkillUnitCollection().stream()
                    .filter(value -> Integer.valueOf(slot).equals(value.getUnitQuantity())).toList();
            if (units.size() > 1 || (!units.isEmpty() && (!units.get(0).getSkillInfoObjectList().isEmpty()
                    || !units.get(0).getSkillInfoUnknownList().isEmpty()))) throw invalid("原空slot" + slot + " 出现额外载荷");
        }
    }
    private static List<int[]> distinctGroups(List<int[]> groups) {
        List<int[]> result = new ArrayList<>();
        for (int[] group : groups) result.add(Arrays.stream(group).distinct().toArray());
        return result;
    }
    private static <T> T copy(T source, Class<T> type) {
        try { return JSON.readValue(JSON.writeValueAsBytes(source), type); }
        catch (IOException error) { throw new IllegalStateException("Motoki飞行变体DTO深复制失败", error); }
    }
    private static JsonNode payload(Object value) { JsonNode node = JSON.valueToTree(value); removeMetadata(node); return node; }
    private static void removeMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description"));
        node.forEach(MotokiFlightBombVariant::removeMetadata);
    }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Motoki飞行变体输入版本不符: " + message); }
    private record Existing(boolean present) { }
}
