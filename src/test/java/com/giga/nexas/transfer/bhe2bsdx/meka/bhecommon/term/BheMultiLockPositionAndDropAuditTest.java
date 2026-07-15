package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bhe.waz.Waz;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionSemantic;
import com.giga.nexas.dto.bsdx.grp.groupmap.TermGrp;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert.BheWazConvertResult;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert.BheWazSlotDropAudit;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert.BheWazSlotDropRecord;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁住 MultiLock 相关的两层策略：
 * <ul>
 *   <li>WAZ convert 丢弃 66-69 与 CEventEffect[28] 时必须留下 drop audit；</li>
 *   <li>term 侧 MULTILOCK_LOCK 映射到保存目标快照的 ROCK，而不是动态搜索的 ENEMY。</li>
 * </ul>
 */
class BheMultiLockPositionAndDropAuditTest {

    private static final Path TAMA05_BHE_JSON =
            Paths.get("src/main/resources/wazBheJson/tama05.waz.json");
    private static final Path BHE_WAZ_JSON_DIR = Paths.get("src/main/resources/wazBheJson");

    private final BheInfoCollectionTermConverter termConverter = new BheInfoCollectionTermConverter();
    private final BheToBsdxWazConverter wazConverter = new BheToBsdxWazConverter();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void multiLockAuxUsesRockSnapshot() {
        // tama05 みさき床召還 / あたためる 使用的 POS/OBJECT + intList3=[23,0]
        BheInfoCollection source = new BheInfoCollection();
        source.setInt1(20); // POS
        source.getTypeList().add(1); // OBJECT
        source.getIntList3().add(findBheItemIndex("OBJECTPOS", "MULTILOCK_LOCK"));
        source.getIntList3().add(0); // OBJECTPOS2/NONE
        source.setInt2(0);

        BsdxInfoCollection converted = termConverter.convert(source, "tama05-style MULTILOCK_LOCK POS");
        BsdxInfoCollectionSemantic semantic = BsdxInfoCollectionAnalyzer.analyze(converted);

        assertTrue(semantic.getWarnings().isEmpty(), "MULTILOCK_LOCK->ROCK 必须保持 BSDX 可解析: " + semantic.getWarnings());
        assertEquals("POS/OBJECT", semantic.getSyntaxPathByDescription());

        int enemyObjectPosIndex = requireBsdxItemIndex("OBJECTPOS", "ENEMY");
        int rockObjectPosIndex = requireBsdxItemIndex("OBJECTPOS", "ROCK");
        assertEquals(rockObjectPosIndex, converted.getIntList3().get(0),
                "MULTILOCK_LOCK aux 第一位应落到 OBJECTPOS/ROCK");
        assertNotEquals(enemyObjectPosIndex, converted.getIntList3().get(0),
                "MULTILOCK_LOCK 不应退化成动态搜索最近敌人");
        assertEquals(requireBsdxItemIndex("OBJECTPOS2", "NONE"), converted.getIntList3().get(1));
    }

    @Test
    void multiLockObjectUsesRockSnapshot() {
        int enemyObjectIndex = requireBsdxItemIndex("OBJECT", "ENEMY");
        int rockObjectIndex = requireBsdxItemIndex("OBJECT", "ROCK");
        assertNotEquals(enemyObjectIndex, rockObjectIndex);

        int bheObjectMultilockIndex = findBheItemIndex("OBJECT", "MULTILOCK_LOCK");
        assertTrue(bheObjectMultilockIndex >= 0, "BHE term 应含 OBJECT/MULTILOCK_LOCK");

        BheInfoCollection objectSelector = new BheInfoCollection();
        objectSelector.setInt1(1); // BHE OBJECT group
        objectSelector.getTypeList().add(bheObjectMultilockIndex);
        objectSelector.setInt2(0);

        BsdxInfoCollection converted = termConverter.convert(objectSelector, "OBJECT/MULTILOCK_LOCK");
        BsdxInfoCollectionSemantic semantic = BsdxInfoCollectionAnalyzer.analyze(converted);
        assertTrue(semantic.getWarnings().isEmpty(), semantic.getWarnings().toString());
        assertEquals("OBJECT/ROCK", semantic.getSyntaxPathByDescription());
        assertEquals(rockObjectIndex, converted.getTypeList().get(0));
        assertNotEquals(enemyObjectIndex, converted.getTypeList().get(0));
    }

    @Test
    void tama05DropsAreAudited() throws Exception {
        Waz source = mapper.readValue(TAMA05_BHE_JSON.toFile(), Waz.class);
        assertNotNull(source);
        assertTrue(source.getSkillList().size() > 336, "tama05 应含床召還 skill[336]");

        BheWazConvertResult result = wazConverter.convertWithAudit(source);
        assertNotNull(result.waz());
        BheWazSlotDropAudit audit = result.dropAudit();
        assertFalse(audit.getDrops().isEmpty(), "含 MultiLock 的 tama05 转换不应产生空 drop audit");

        List<BheWazSlotDropRecord> multiLockDrops = audit.multiLockDrops();
        assertFalse(multiLockDrops.isEmpty(), "应至少记录 66-69 MultiLock 槽丢弃");

        Set<Integer> droppedSlotsOnSkill336 = multiLockDrops.stream()
                .filter(drop -> drop.skillIndex() == 336)
                .map(BheWazSlotDropRecord::sourceSlot)
                .collect(Collectors.toCollection(HashSet::new));

        assertTrue(droppedSlotsOnSkill336.containsAll(BheWazSlotDropAudit.MULTILOCK_SOURCE_SLOTS),
                "床召還 skill[336] 应丢弃并审计 MultiLock 槽 66-69，实际=" + droppedSlotsOnSkill336);

        BheWazSlotDropRecord sample = multiLockDrops.stream()
                .filter(drop -> drop.skillIndex() == 336)
                .findFirst()
                .orElseThrow();
        assertNotNull(sample.fileName());
        assertTrue(sample.fileName().toLowerCase().contains("tama05")
                        || "tama05".equalsIgnoreCase(sample.fileName()),
                "drop 记录应带文件名: " + sample.fileName());
        assertEquals(0, sample.phaseIndex());

        List<BheWazSlotDropRecord> nestedTargetDrops = audit.nestedCEventEffectTargetDrops();
        assertFalse(nestedTargetDrops.isEmpty(), "tama05 的 CEventEffect[28] 标的槽丢弃必须被审计");
        BheWazSlotDropRecord nestedSample = nestedTargetDrops.get(0);
        assertEquals(54, nestedSample.sourceSlot());
        assertEquals(BheWazSlotDropAudit.CEVENT_EFFECT_TARGET_SOURCE_SLOT, nestedSample.nestedSourceSlot());
        assertNotNull(nestedSample.sourceObjectIndex());
        assertTrue(nestedSample.sourceSlotPath().contains("CEventEffect[28]"));

        var followupAudit = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter()
                .convertWithAudit(source)
                .dropAudit();
        assertEquals(audit.getDrops(), followupAudit.getDrops());
    }

    @Test
    void fullBheWazCorpusDropCountsStayStable() throws Exception {
        List<Path> wazFiles;
        try (var paths = Files.list(BHE_WAZ_JSON_DIR)) {
            wazFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".waz.json"))
                    .sorted()
                    .toList();
        }
        assertEquals(103, wazFiles.size(), "BHE WAZ JSON 语料数量变化时必须重新审计");

        long auditedTopLevelTargets = 0;
        long auditedNestedTargets = 0;
        for (Path wazFile : wazFiles) {
            Waz source = mapper.readValue(wazFile.toFile(), Waz.class);
            BheWazSlotDropAudit audit = wazConverter.convertWithAudit(source).dropAudit();
            auditedTopLevelTargets += audit.getDrops().stream()
                    .filter(drop -> !drop.isNested()
                            && drop.sourceSlot() == BheWazSlotDropAudit.TARGET_SOURCE_SLOT)
                    .count();
            auditedNestedTargets += audit.nestedCEventEffectTargetDrops().size();
        }

        assertEquals(138, auditedTopLevelTargets, "顶层槽 35 的审计数量变化");
        assertEquals(45, auditedNestedTargets, "CEventEffect[28] 的审计数量变化");
    }

    private int requireBsdxItemIndex(String groupCodeName, String itemDescription) {
        TermGrp termGrp = BsdxInfoCollectionAnalyzer.getCachedTermGrp();
        assertNotNull(termGrp, "BSDX Term.grp.json 必须存在");
        for (TermGrp.TermGroup group : termGrp.getTermList()) {
            if (!groupCodeName.equals(group.getTermGroupCodeName())) {
                continue;
            }
            for (int i = 0; i < group.getTermItemList().size(); i++) {
                if (itemDescription.equals(group.getTermItemList().get(i).getTermItemDescription())) {
                    return i;
                }
            }
        }
        throw new AssertionError("BSDX term 缺少 " + groupCodeName + "/" + itemDescription);
    }

    private int findBheItemIndex(String groupCodeName, String itemDescription) {
        var termGrp = com.giga.nexas.dto.bhe.BheInfoCollectionAnalyzer.getCachedTermGrp();
        assertNotNull(termGrp);
        for (var group : termGrp.getTermList()) {
            if (!groupCodeName.equals(group.getTermGroupCodeName())) {
                continue;
            }
            for (int i = 0; i < group.getTermItemList().size(); i++) {
                if (itemDescription.equals(group.getTermItemList().get(i).getTermItemDescription())) {
                    return i;
                }
            }
        }
        return -1;
    }
}
