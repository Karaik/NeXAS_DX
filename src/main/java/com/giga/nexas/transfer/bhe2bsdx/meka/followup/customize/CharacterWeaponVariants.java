package com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/** 只复制本角色已重绑的普通武装；所有校验和副本编辑完成后才追加。 */
public final class CharacterWeaponVariants {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private CharacterWeaponVariants() { }

    public static Mek.MekWeaponInfo append(Mek mek, Waz waz, Mek.MekWeaponInfo sourceWeapon,
                                           Waz.Skill sourceSkill, String newWeaponName,
                                           String newSkillEnglish, int heatCost, String description,
                                           Consumer<Waz.Skill> editCopy,
                                           Mek.MekWeaponInfo... materialDonors) {
        if (mek == null || waz == null || mek.getMekBasicInfo() == null
                || mek.getMekBasicInfo().getWazFileSequence() == null
                || mek.getMekBasicInfo().getWazFileSequence() < 0
                || "KOU".equalsIgnoreCase(mek.getMekBasicInfo().getMekNameEnglish())) {
            throw invalid("需要非KOU角色的已重绑MEK/WAZ");
        }
        if (newWeaponName == null || newWeaponName.isBlank() || newSkillEnglish == null
                || !newSkillEnglish.startsWith("DX_") || newSkillEnglish.length() <= 3
                || description == null || description.isBlank() || heatCost < 0 || editCopy == null) {
            throw invalid("新武装身份、说明或热耗无效");
        }
        Map<Integer, Mek.MekWeaponInfo> weapons = mek.getMekWeaponInfoMap();
        if (weapons == null || weapons.isEmpty() || waz.getSkillList() == null) throw invalid("源武装表为空");
        int ordinal = 0;
        for (var entry : weapons.entrySet()) {
            if (!Integer.valueOf(ordinal++).equals(entry.getKey()) || entry.getValue() == null) {
                throw invalid("武装map必须按0起连续顺序保存，不能重排或压缩");
            }
        }
        List<Mek.MekWeaponInfo> ordered = new ArrayList<>(weapons.values());
        int sourceRow = identityIndex(ordered, sourceWeapon);
        int sourceIndex = identityIndex(waz.getSkillList(), sourceSkill);
        if (sourceRow < 0 || sourceIndex < 0 || !Integer.valueOf(sourceIndex).equals(sourceWeapon.getWazSequence())
                || !Integer.valueOf(0).equals(sourceWeapon.getWeaponCategory())
                || sourceSkill.getPhasesInfo() == null || sourceSkill.getPhasesInfo().isEmpty()
                || !Integer.valueOf(sourceSkill.getPhasesInfo().size()).equals(sourceSkill.getPhaseQuantity())) {
            throw invalid("源普通武装与本角色主技能引用不一致");
        }
        if (ordered.stream().filter(value -> Objects.equals(value.getWeaponName(), sourceWeapon.getWeaponName())
                && Objects.equals(value.getWeaponSequence(), sourceWeapon.getWeaponSequence())).count() != 1
                || waz.getSkillList().stream().filter(Objects::nonNull)
                .filter(value -> Objects.equals(value.getSkillNameJapanese(), sourceSkill.getSkillNameJapanese())
                        && Objects.equals(value.getSkillNameEnglish(), sourceSkill.getSkillNameEnglish())).count() != 1) {
            throw invalid("源武装或技能双名不唯一");
        }

        Mek.MekMaterialBlock block = requireSingleMaterialLayout(mek, weapons.size());
        Mek.MekMaterialBlock.PluginEntry material = copyMaterial(block.getTrailingEntries().get(sourceRow));
        if (materialDonors != null) for (var donor : materialDonors) {
            int donorRow = identityIndex(ordered, donor);
            if (donorRow < 0) throw invalid("Material供体不属于当前角色");
            mergeMaterial(material, block.getTrailingEntries().get(donorRow));
        }

        List<Mek.MekWeaponInfo> existingWeapons = ordered.stream()
                .filter(value -> newWeaponName.equals(value.getWeaponName())).toList();
        List<Waz.Skill> existingSkills = waz.getSkillList().stream().filter(Objects::nonNull)
                .filter(value -> newWeaponName.equals(value.getSkillNameJapanese())
                        || newSkillEnglish.equals(value.getSkillNameEnglish())).toList();
        boolean existing = !existingWeapons.isEmpty() || !existingSkills.isEmpty();
        if (existing && (existingWeapons.size() != 1 || existingSkills.size() != 1
                || !newWeaponName.equals(existingSkills.get(0).getSkillNameJapanese())
                || !newSkillEnglish.equals(existingSkills.get(0).getSkillNameEnglish()))) {
            throw invalid("变体名字已占用或武装与技能只存在一半");
        }
        if (!existing && weapons.size() > 152) throw invalid("追加武装号会超过152");
        int targetIndex = existing ? identityIndex(waz.getSkillList(), existingSkills.get(0)) : waz.getSkillList().size();
        Waz.Skill copiedSkill = copy(sourceSkill, Waz.Skill.class);
        copiedSkill.setSkillNameJapanese(newWeaponName);
        copiedSkill.setSkillNameEnglish(newSkillEnglish);
        editCopy.accept(copiedSkill);
        if (!newWeaponName.equals(copiedSkill.getSkillNameJapanese())
                || !newSkillEnglish.equals(copiedSkill.getSkillNameEnglish())
                || !Objects.equals(copiedSkill.getPhaseQuantity(), sourceSkill.getPhaseQuantity())
                || copiedSkill.getPhasesInfo() == null
                || copiedSkill.getPhasesInfo().size() != sourceSkill.getPhasesInfo().size()) {
            throw invalid("副本编辑改变了变体身份或phase结构");
        }
        Mek.MekWeaponInfo copiedWeapon = copy(sourceWeapon, Mek.MekWeaponInfo.class);
        copiedWeapon.setWeaponName(newWeaponName);
        copiedWeapon.setWazSequence(targetIndex);
        copiedWeapon.setHeatMinConsumption(heatCost);
        copiedWeapon.setHeatMaxConsumption(heatCost);
        copiedWeapon.setUpgradeExp(0);
        copiedWeapon.setWeaponDescription(description);

        if (existing) {
            int row = identityIndex(ordered, existingWeapons.get(0));
            if (row > 152 || !payload(copiedSkill).equals(payload(existingSkills.get(0)))
                    || !payload(copiedWeapon).equals(payload(existingWeapons.get(0)))
                    || !payload(material).equals(payload(block.getTrailingEntries().get(row)))) {
                throw invalid("既有变体身份、载荷或Material已漂移");
            }
            return existingWeapons.get(0);
        }

        // 编辑独立副本、检查Material和容量均已完成；以下没有资源读取或可失败的回调。
        Map<Integer, Mek.MekWeaponInfo> appendedWeapons = new LinkedHashMap<>(weapons);
        appendedWeapons.put(weapons.size(), copiedWeapon);
        List<Waz.Skill> appendedSkills = new ArrayList<>(waz.getSkillList());
        appendedSkills.add(copiedSkill);
        List<Mek.MekMaterialBlock.PluginEntry> regular = new ArrayList<>(block.getRegularEntries());
        List<Mek.MekMaterialBlock.PluginEntry> trailing = new ArrayList<>(block.getTrailingEntries());
        trailing.add(material);
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>(regular);
        entries.addAll(trailing);
        mek.setMekWeaponInfoMap(appendedWeapons);
        waz.setSkillList(appendedSkills);
        block.setRegularEntries(regular);
        block.setTrailingEntries(trailing);
        block.setEntries(entries);
        return copiedWeapon;
    }

    private static Mek.MekMaterialBlock requireSingleMaterialLayout(Mek mek, int weapons) {
        var block = mek.getMekMaterialBlock();
        if (block == null || block.getRegularEntries() == null || block.getTrailingEntries() == null
                || block.getEntries() == null || block.getRegularEntries().size() < 7
                || block.getTrailingEntries().size() != weapons) {
            throw invalid("Material尾表必须为每个武装一条，不能套三等级布局");
        }
        int regular = block.getRegularEntries().size();
        List<Mek.MekMaterialBlock.PluginEntry> combined = new ArrayList<>(block.getRegularEntries());
        combined.addAll(block.getTrailingEntries());
        if (!Integer.valueOf(regular).equals(block.getRegularCount())
                || !Integer.valueOf(regular - 7).equals(block.getExtraRegularCount())
                || !payload(combined).equals(payload(block.getEntries()))) {
            throw invalid("Material的regular/trailing/entries三视图不一致");
        }
        return block;
    }

    private static Mek.MekMaterialBlock.PluginEntry copyMaterial(Mek.MekMaterialBlock.PluginEntry source) {
        if (source == null) throw invalid("Material行为空");
        var target = new Mek.MekMaterialBlock.PluginEntry();
        target.setSpriteGroups(copyGroups(source.getSpriteGroups()));
        target.setSeGroups(copyGroups(source.getSeGroups()));
        target.setVoiceGroups(copyGroups(source.getVoiceGroups()));
        return target;
    }

    private static List<int[]> copyGroups(List<int[]> source) {
        if (source == null) throw invalid("Material组列表为空");
        List<int[]> result = new ArrayList<>();
        for (int[] group : source) {
            if (group == null) throw invalid("Material组不能为null");
            result.add(group.clone());
        }
        return result;
    }

    private static void mergeMaterial(Mek.MekMaterialBlock.PluginEntry target, Mek.MekMaterialBlock.PluginEntry donor) {
        if (donor == null) throw invalid("Material供体行为空");
        target.setSpriteGroups(mergeGroups(target.getSpriteGroups(), donor.getSpriteGroups()));
        target.setSeGroups(mergeGroups(target.getSeGroups(), donor.getSeGroups()));
        target.setVoiceGroups(mergeGroups(target.getVoiceGroups(), donor.getVoiceGroups()));
    }

    private static List<int[]> mergeGroups(List<int[]> source, List<int[]> donor) {
        List<int[]> result = copyGroups(source);
        List<int[]> extra = copyGroups(donor);
        while (result.size() < extra.size()) result.add(new int[0]);
        for (int i = 0; i < extra.size(); i++) {
            List<Integer> values = new ArrayList<>();
            for (int value : result.get(i)) values.add(value);
            for (int value : extra.get(i)) if (!values.contains(value)) values.add(value);
            result.set(i, values.stream().mapToInt(Integer::intValue).toArray());
        }
        return result;
    }

    private static <T> T copy(T source, Class<T> type) {
        try {
            return JSON.readValue(JSON.writeValueAsBytes(source), type);
        } catch (IOException error) {
            throw new IllegalStateException("角色武装DTO深复制失败", error);
        }
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        removeMetadata(tree);
        return tree;
    }

    private static void removeMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description"));
        }
        node.forEach(CharacterWeaponVariants::removeMetadata);
    }

    private static <T> int identityIndex(List<T> values, T item) {
        for (int i = 0; i < values.size(); i++) if (values.get(i) == item) return i;
        return -1;
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException("角色变体拒绝: " + message);
    }
}
