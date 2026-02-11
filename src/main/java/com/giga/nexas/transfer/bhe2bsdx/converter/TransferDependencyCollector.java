package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Dependency collector for transfer output sets.
 * <p>
 * Rules:
 * - WAZ closure is derived by CEventWazaSelect(fileNo + sequenceNo).
 * - For dependent WAZ files, recursion follows only the referenced sequence skill.
 * - SPM closure is derived by CEventSprite(spmFileSequence) from the same selected scopes.
 */
public class TransferDependencyCollector {

    private static final Map<Integer, String> FILE_NO_TO_KEY = Map.of(
            0, "effect",
            1, "tama01",
            2, "tama02",
            3, "tama03",
            4, "tama04",
            5, "tama05",
            6, "laser",
            7, "bomb"
    );

    private static final List<String> VARIANT_PREFIXES = List.of("c_", "s_", "g_", "m_");

    public Map<String, Waz> collectWazOutputMap(
            String mainKey,
            Waz mainWaz,
            Map<String, Waz> registry
    ) {
        return buildSnapshot(mainKey, mainWaz, registry).wazOutput;
    }

    public Map<String, Spm> collectSpmOutputMap(
            String mainKey,
            Waz mainWaz,
            Map<String, Waz> wazRegistry,
            SpriteGroupGrp spriteGroup,
            Map<String, Spm> spmRegistry
    ) {
        DependencySnapshot snapshot = buildSnapshot(mainKey, mainWaz, wazRegistry);
        return buildSpmOutputFromIndices(snapshot.spriteIndices, spriteGroup, spmRegistry);
    }

    /**
     * Legacy entry for tests or callers that already pass a WAZ set.
     * This scans the full passed WAZ objects.
     */
    public Map<String, Spm> collectSpmOutputMap(
            Map<String, Waz> wazMap,
            SpriteGroupGrp spriteGroup,
            Map<String, Spm> spmRegistry
    ) {
        if (wazMap == null || wazMap.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Set<Integer> spriteIndices = collectSpriteIndices(wazMap.values());
        return buildSpmOutputFromIndices(spriteIndices, spriteGroup, spmRegistry);
    }

    public Set<Integer> collectSpriteIndices(Collection<Waz> wazCollection) {
        LinkedHashSet<Integer> indices = new LinkedHashSet<>();
        if (wazCollection == null) {
            return indices;
        }
        for (Waz waz : wazCollection) {
            if (waz == null) {
                continue;
            }
            for (Waz.Skill skill : waz.getSkillList()) {
                collectFromSkill(skill, null, indices);
            }
        }
        return indices;
    }

    private DependencySnapshot buildSnapshot(
            String mainKey,
            Waz mainWaz,
            Map<String, Waz> registry
    ) {
        DependencySnapshot snapshot = new DependencySnapshot();
        String normalizedMainKey = normalizeKey(mainKey);
        if (normalizedMainKey == null) {
            return snapshot;
        }

        Map<String, Waz> normalizedRegistry = normalizeRegistry(registry);
        Waz root = mainWaz != null ? mainWaz : normalizedRegistry.get(normalizedMainKey);
        if (root == null) {
            return snapshot;
        }

        snapshot.wazOutput.put(normalizedMainKey, root);

        LinkedHashSet<WazRef> rootRefs = new LinkedHashSet<>();
        collectFromWholeWaz(root, rootRefs, snapshot.spriteIndices);

        ArrayDeque<WazRef> queue = new ArrayDeque<>(rootRefs);
        LinkedHashSet<WazRef> visitedRefs = new LinkedHashSet<>();

        while (!queue.isEmpty()) {
            WazRef ref = queue.removeFirst();
            if (!visitedRefs.add(ref)) {
                continue;
            }
            Waz dependent = normalizedRegistry.get(ref.fileKey);
            if (dependent == null) {
                continue;
            }

            snapshot.wazOutput.putIfAbsent(ref.fileKey, dependent);
            Waz.Skill skill = resolveSkill(dependent, ref.sequenceNo);
            if (skill == null) {
                continue;
            }

            LinkedHashSet<WazRef> nextRefs = new LinkedHashSet<>();
            collectFromSkill(skill, nextRefs, snapshot.spriteIndices);
            for (WazRef next : nextRefs) {
                if (!visitedRefs.contains(next)) {
                    queue.addLast(next);
                }
            }
        }

        return snapshot;
    }

    private void collectFromWholeWaz(Waz waz, Set<WazRef> refs, Set<Integer> spriteIndices) {
        if (waz == null || waz.getSkillList() == null) {
            return;
        }
        for (Waz.Skill skill : waz.getSkillList()) {
            collectFromSkill(skill, refs, spriteIndices);
        }
    }

    private void collectFromSkill(Waz.Skill skill, Set<WazRef> refs, Set<Integer> spriteIndices) {
        if (skill == null || skill.getPhasesInfo() == null) {
            return;
        }
        for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
            if (phase == null || phase.getSkillUnitCollection() == null) {
                continue;
            }
            for (SkillUnit unit : phase.getSkillUnitCollection()) {
                if (unit == null || unit.getSkillInfoObjectList() == null) {
                    continue;
                }
                for (SkillInfoObject info : unit.getSkillInfoObjectList()) {
                    visitInfo(info, node -> {
                        if (refs != null && node instanceof CEventWazaSelect select) {
                            String key = FILE_NO_TO_KEY.get(select.getWazFileNo());
                            if (key != null) {
                                refs.add(new WazRef(key, safeInt(select.getWazSequenceNo())));
                            }
                        }
                        if (spriteIndices != null && node instanceof CEventSprite sprite) {
                            Integer seq = sprite.getSpmFileSequence();
                            if (seq != null && seq >= 0) {
                                spriteIndices.add(seq);
                            }
                        }
                    });
                }
            }
        }
    }

    private Waz.Skill resolveSkill(Waz waz, int sequenceNo) {
        if (waz == null || waz.getSkillList() == null || sequenceNo < 0 || sequenceNo >= waz.getSkillList().size()) {
            return null;
        }
        return waz.getSkillList().get(sequenceNo);
    }

    private Map<String, Spm> buildSpmOutputFromIndices(
            Set<Integer> spriteIndices,
            SpriteGroupGrp spriteGroup,
            Map<String, Spm> spmRegistry
    ) {
        LinkedHashMap<String, Spm> output = new LinkedHashMap<>();
        if (spriteIndices == null || spriteIndices.isEmpty() || spriteGroup == null || spmRegistry == null || spmRegistry.isEmpty()) {
            return output;
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (Integer index : spriteIndices) {
            if (index == null || index < 0) {
                continue;
            }
            String key = resolveSpriteSpmKey(spriteGroup, index);
            if (key != null) {
                keys.add(key);
            }
        }

        Map<String, Spm> normalizedSpmRegistry = normalizeRegistry(spmRegistry);
        expandVariantKeys(keys, normalizedSpmRegistry);
        for (String key : keys) {
            Spm spm = normalizedSpmRegistry.get(key);
            if (spm != null) {
                output.put(key, spm);
            }
        }
        return output;
    }

    private void visitInfo(SkillInfoObject info, Consumer<SkillInfoObject> visitor) {
        if (info == null || visitor == null) {
            return;
        }
        visitor.accept(info);
        if (info instanceof CEventEffect ev) {
            List<CEventEffect.CEventEffectUnit> units = ev.getCeventEffectUnitList();
            if (units != null) {
                for (CEventEffect.CEventEffectUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventHit ev) {
            List<CEventHit.CEventHitUnit> units = ev.getCeventHitUnitList();
            if (units != null) {
                for (CEventHit.CEventHitUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventEscape ev) {
            List<CEventEscape.CEventEscapeUnit> units = ev.getCeventEscapeUnitList();
            if (units != null) {
                for (CEventEscape.CEventEscapeUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventScreenLine ev) {
            List<CEventScreenLine.CEventScreenLineUnit> units = ev.getCeventScreenLineUnitList();
            if (units != null) {
                for (CEventScreenLine.CEventScreenLineUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventScreenEffect ev) {
            List<CEventScreenEffect.CEventScreenEffectUnit> units = ev.getCeventScreenEffectUnitList();
            if (units != null) {
                for (CEventScreenEffect.CEventScreenEffectUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventRadialLine ev) {
            List<CEventRadialLine.CEventRadialLineUnit> units = ev.getCeventRadialLineUnitList();
            if (units != null) {
                for (CEventRadialLine.CEventRadialLineUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventCharge ev) {
            List<CEventCharge.CEventChargeUnit> units = ev.getCeventChargeUnitList();
            if (units != null) {
                for (CEventCharge.CEventChargeUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventCamera ev) {
            List<CEventCamera.CEventCameraUnit> units = ev.getCeventCameraUnitList();
            if (units != null) {
                for (CEventCamera.CEventCameraUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventHeight ev) {
            List<CEventHeight.CEventHeightUnit> units = ev.getCeventHeightUnitList();
            if (units != null) {
                for (CEventHeight.CEventHeightUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventStatus ev) {
            List<CEventStatus.CEventStatusUnit> units = ev.getCeventStatusUnitList();
            if (units != null) {
                for (CEventStatus.CEventStatusUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventNokezori ev) {
            List<CEventNokezori.CEventNokezoriUnit> units = ev.getCeventNokezoriUnitList();
            if (units != null) {
                for (CEventNokezori.CEventNokezoriUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
            return;
        }
        if (info instanceof CEventCpuButton ev) {
            List<CEventCpuButton.CEventCpuButtonUnit> units = ev.getCeventCpuButtonUnitList();
            if (units != null) {
                for (CEventCpuButton.CEventCpuButtonUnit unit : units) {
                    visitInfo(unit.getData(), visitor);
                }
            }
        }
    }

    private String resolveSpriteSpmKey(SpriteGroupGrp spriteGroup, int index) {
        if (spriteGroup == null || spriteGroup.getSpriteList() == null || index < 0 || index >= spriteGroup.getSpriteList().size()) {
            return null;
        }
        SpriteGroupGrp.SpriteGroupEntry entry = spriteGroup.getSpriteList().get(index);
        if (entry == null || entry.getExistFlag() == null || entry.getExistFlag() == 0) {
            return null;
        }
        String fileName = normalizeFileName(entry.getSpriteFileName());
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        if (base.isBlank()) {
            return null;
        }
        return base.toLowerCase(Locale.ROOT);
    }

    private String normalizeFileName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replace("\\", "/");
        if (trimmed.isEmpty()) {
            return null;
        }
        int slash = trimmed.lastIndexOf('/');
        String name = slash >= 0 && slash + 1 < trimmed.length() ? trimmed.substring(slash + 1) : trimmed;
        if (name.isBlank()) {
            return null;
        }
        return name;
    }

    private void expandVariantKeys(Set<String> keys, Map<String, Spm> spmRegistry) {
        if (keys == null || keys.isEmpty() || spmRegistry == null || spmRegistry.isEmpty()) {
            return;
        }
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String normalized = key.toLowerCase(Locale.ROOT);
            String base = normalized;
            for (String prefix : VARIANT_PREFIXES) {
                if (normalized.startsWith(prefix) && normalized.length() > prefix.length()) {
                    base = normalized.substring(prefix.length());
                    break;
                }
            }
            expanded.add(base);
            for (String prefix : VARIANT_PREFIXES) {
                expanded.add(prefix + base);
            }
        }
        for (String key : expanded) {
            if (spmRegistry.containsKey(key)) {
                keys.add(key);
            }
        }
    }

    private int safeInt(Integer value) {
        return value == null ? -1 : value;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private <T> Map<String, T> normalizeRegistry(Map<String, T> map) {
        LinkedHashMap<String, T> normalized = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) {
            return normalized;
        }
        for (Map.Entry<String, T> entry : map.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (key == null || entry.getValue() == null) {
                continue;
            }
            normalized.put(key, entry.getValue());
        }
        return normalized;
    }

    private static final class WazRef {
        private final String fileKey;
        private final int sequenceNo;

        private WazRef(String fileKey, int sequenceNo) {
            this.fileKey = fileKey;
            this.sequenceNo = sequenceNo;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof WazRef other)) {
                return false;
            }
            return sequenceNo == other.sequenceNo && fileKey.equals(other.fileKey);
        }

        @Override
        public int hashCode() {
            int result = fileKey.hashCode();
            result = 31 * result + sequenceNo;
            return result;
        }
    }

    private static final class DependencySnapshot {
        private final LinkedHashMap<String, Waz> wazOutput = new LinkedHashMap<>();
        private final LinkedHashSet<Integer> spriteIndices = new LinkedHashSet<>();
    }
}

