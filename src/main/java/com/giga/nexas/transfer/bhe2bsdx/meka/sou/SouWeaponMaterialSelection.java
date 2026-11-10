package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

/** 为最终非KOU武装选择一条Material，保持等级、过滤和武装顺序一致。 */
final class SouWeaponMaterialSelection {
    private SouWeaponMaterialSelection() { }

    static List<Mek.MekMaterialBlock.PluginEntry> select(
            Mek mek, BiPredicate<String, String> excluded) {
        var block = mek.getMekMaterialBlock();
        if (block == null || block.getTrailingEntries() == null || block.getTrailingEntries().isEmpty()) {
            return null;
        }
        var weapons = mek.getMekWeaponInfoMap();
        List<Mek.MekWeaponInfo> valid = weapons.values().stream().filter(java.util.Objects::nonNull).toList();
        int tieredCount = valid.stream().mapToInt(w -> Integer.valueOf(0).equals(w.getWeaponCategory()) ? 3 : 1).sum();
        var source = block.getTrailingEntries();
        boolean single = source.size() == valid.size();
        if (!single && source.size() != tieredCount) {
            throw new IllegalStateException("Sou Material布局不匹配: weapons=" + valid.size()
                    + ", trailing=" + source.size() + ", tiered=" + tieredCount);
        }
        List<Mek.MekMaterialBlock.PluginEntry> selected = new ArrayList<>();
        int cursor = 0;
        for (var weapon : valid) {
            int width = !single && Integer.valueOf(0).equals(weapon.getWeaponCategory()) ? 3 : 1;
            if (!excluded.test(weapon.getWeaponName(), weapon.getWeaponSequence())) {
                // 常规四组升级Lv3；脚本及其他category0条目保持源默认等级。
                Integer seq = weapon.getWazSequence();
                int level = width == 3 && isRegularSkill(seq) ? 2 : 0;
                selected.add(copy(source.get(cursor + level)));
            }
            cursor += width;
        }
        return selected;
    }

    private static boolean isRegularSkill(Integer seq) {
        return seq != null && ((seq >= 39 && seq <= 143) || (seq >= 145 && seq <= 249)
                || (seq >= 251 && seq <= 355) || (seq >= 357 && seq <= 461));
    }

    private static Mek.MekMaterialBlock.PluginEntry copy(Mek.MekMaterialBlock.PluginEntry source) {
        if (source == null) throw new IllegalStateException("Sou Material切片为空");
        var target = new Mek.MekMaterialBlock.PluginEntry();
        target.setOffset(source.getOffset());
        target.setLength(source.getLength());
        target.setSpriteGroups(copyGroups(source.getSpriteGroups()));
        target.setSeGroups(copyGroups(source.getSeGroups()));
        target.setVoiceGroups(copyGroups(source.getVoiceGroups()));
        return target;
    }

    private static List<int[]> copyGroups(List<int[]> source) {
        List<int[]> target = new ArrayList<>();
        if (source != null) for (int[] group : source) {
            target.add(group == null ? new int[0] : Arrays.copyOf(group, group.length));
        }
        return target;
    }
}
