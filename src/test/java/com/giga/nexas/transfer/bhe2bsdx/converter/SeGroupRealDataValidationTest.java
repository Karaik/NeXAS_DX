package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SeGroupRealDataValidationTest {

    private static final String CHARSET = "windows-31j";
    private static final Path BHE_SEGROUP = Paths.get("src/main/resources/game/bhe/grp/segroup.grp");
    private static final Path BSDX_SEGROUP = Paths.get("src/main/resources/game/bsdx/grp/segroup.grp");
    private static final Path BHE_WAZ_DIR = Paths.get("src/main/resources/game/bhe/waz");

    private final BheBinService bheBinService = new BheBinService();
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    void realData_shouldKeepSeNameConsistencyAfterMapping() throws Exception {
        assumeTrue(Files.isRegularFile(BHE_SEGROUP), "missing file: " + BHE_SEGROUP.toAbsolutePath());
        assumeTrue(Files.isRegularFile(BSDX_SEGROUP), "missing file: " + BSDX_SEGROUP.toAbsolutePath());
        assumeTrue(Files.isDirectory(BHE_WAZ_DIR), "missing dir: " + BHE_WAZ_DIR.toAbsolutePath());

        com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp bheSeGroup = parseBheSeGroup(BHE_SEGROUP);
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp bsdxSeGroup = parseBsdxSeGroup(BSDX_SEGROUP);

        SeGroupIndexMapper mapper = new SeGroupIndexMapper();
        SeGroupIndexMapper.SeGroupMap map = mapper.build(bheSeGroup, bsdxSeGroup, 11);
        assertFalse(map.isEmpty(), "segroup mapping is empty");
        assertTrue(map.getMappedPairCount() > 0, "mapped pair count should be > 0");
        assertTrue(map.getAppendedItems() > 0, "appended item count should be > 0 on real data");

        int changedPairsInSeGroup = 0;
        int checkedPairsInSeGroup = 0;
        for (int bheGroup = 0; bheGroup < bheSeGroup.getSeList().size(); bheGroup++) {
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupGroup group = bheSeGroup.getSeList().get(bheGroup);
            if (group == null || group.getSeItems() == null) {
                continue;
            }
            for (int bheSeq = 0; bheSeq < group.getSeItems().size(); bheSeq++) {
                String sourceName = lookupBheSeName(bheSeGroup, bheGroup, bheSeq);
                if (sourceName.isBlank()) {
                    continue;
                }
                byte[] remapped = map.remapBlock(buildBlock(bheGroup, bheSeq));
                int dstGroup = readIntLE(remapped, 0);
                int dstSeq = readIntLE(remapped, 4);
                String targetName = lookupBsdxSeName(bsdxSeGroup, dstGroup, dstSeq);
                assertFalse(
                        targetName.isBlank(),
                        "missing mapped BSDX se item for BHE pair " + bheGroup + ":" + bheSeq + " -> " + dstGroup + ":" + dstSeq
                );
                assertEquals(
                        normalize(sourceName),
                        normalize(targetName),
                        "seFileName mismatch for BHE pair " + bheGroup + ":" + bheSeq + " -> " + dstGroup + ":" + dstSeq
                );
                if (bheGroup != dstGroup || bheSeq != dstSeq) {
                    changedPairsInSeGroup++;
                }
                checkedPairsInSeGroup++;
            }
        }
        assertTrue(checkedPairsInSeGroup > 0, "no valid segroup pair found for verification");
        assertTrue(changedPairsInSeGroup > 0, "real segroup mapping produced no changed pair");

        List<Path> wazFiles;
        try (var stream = Files.list(BHE_WAZ_DIR)) {
            wazFiles = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".waz"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }
        assertTrue(!wazFiles.isEmpty(), "no BHE waz files found under " + BHE_WAZ_DIR.toAbsolutePath());

        int checkedBlocks = 0;
        int changedBlocks = 0;
        int skippedUnknownPairs = 0;

        for (Path wazPath : wazFiles) {
            com.giga.nexas.dto.bhe.waz.Waz bheWaz = parseBheWaz(wazPath);
            for (byte[] block : collectSeBlocks(bheWaz)) {
                if (block == null || block.length < 8) {
                    continue;
                }
                int srcGroup = readIntLE(block, 0);
                int srcSeq = readIntLE(block, 4);
                String sourceName = lookupBheSeName(bheSeGroup, srcGroup, srcSeq);
                if (sourceName.isBlank()) {
                    skippedUnknownPairs++;
                    continue;
                }

                byte[] remapped = map.remapBlock(block);
                int dstGroup = readIntLE(remapped, 0);
                int dstSeq = readIntLE(remapped, 4);
                String targetName = lookupBsdxSeName(bsdxSeGroup, dstGroup, dstSeq);

                assertFalse(
                        targetName.isBlank(),
                        "waz=" + wazPath.getFileName() + " mapped pair out-of-range: " + srcGroup + ":" + srcSeq + " -> " + dstGroup + ":" + dstSeq
                );
                assertEquals(
                        normalize(sourceName),
                        normalize(targetName),
                        "waz=" + wazPath.getFileName() + " source/target seFileName mismatch: " + srcGroup + ":" + srcSeq + " -> " + dstGroup + ":" + dstSeq
                );

                if (srcGroup != dstGroup || srcSeq != dstSeq) {
                    changedBlocks++;
                }
                checkedBlocks++;
            }
        }

        assertTrue(checkedBlocks > 0, "no real CEventSe block matched segroup names");
        assertTrue(changedBlocks > 0, "no CEventSe block changed after remap on real data");
        assertTrue(skippedUnknownPairs >= 0, "counter sanity check");
        System.out.println(
                "[SeGroupRealDataValidationTest] checkedPairsInSeGroup=" + checkedPairsInSeGroup
                        + ", changedPairsInSeGroup=" + changedPairsInSeGroup
                        + ", checkedBlocks=" + checkedBlocks
                        + ", changedBlocks=" + changedBlocks
                        + ", skippedUnknownPairs=" + skippedUnknownPairs
        );
    }

    private com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp parseBheSeGroup(Path path) throws Exception {
        ResponseDTO<?> dto = bheBinService.parse(path.toString(), CHARSET);
        return (com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp) dto.getData();
    }

    private com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp parseBsdxSeGroup(Path path) throws Exception {
        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
        return (com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp) dto.getData();
    }

    private com.giga.nexas.dto.bhe.waz.Waz parseBheWaz(Path path) throws Exception {
        ResponseDTO<?> dto = bheBinService.parse(path.toString(), CHARSET);
        return (com.giga.nexas.dto.bhe.waz.Waz) dto.getData();
    }

    private List<byte[]> collectSeBlocks(com.giga.nexas.dto.bhe.waz.Waz waz) {
        List<byte[]> out = new ArrayList<>();
        if (waz == null || waz.getSkillList() == null) {
            return out;
        }
        for (com.giga.nexas.dto.bhe.waz.Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (SkillInfoObject info : unit.getSkillInfoObjectList()) {
                        if (!(info instanceof CEventSe)) {
                            continue;
                        }
                        CEventSe event = (CEventSe) info;
                        if (event.getByteDataList() == null) {
                            continue;
                        }
                        out.addAll(event.getByteDataList());
                    }
                }
            }
        }
        return out;
    }

    private String lookupBheSeName(com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp grp, int groupIndex, int seqIndex) {
        if (grp == null || grp.getSeList() == null || groupIndex < 0 || seqIndex < 0 || groupIndex >= grp.getSeList().size()) {
            return "";
        }
        com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupGroup group = grp.getSeList().get(groupIndex);
        if (group == null || group.getSeItems() == null || seqIndex >= group.getSeItems().size()) {
            return "";
        }
        com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem item = group.getSeItems().get(seqIndex);
        if (item == null || item.getSeFileName() == null) {
            return "";
        }
        return item.getSeFileName();
    }

    private String lookupBsdxSeName(com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp grp, int groupIndex, int seqIndex) {
        if (grp == null || grp.getSeList() == null || groupIndex < 0 || seqIndex < 0 || groupIndex >= grp.getSeList().size()) {
            return "";
        }
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupGroup group = grp.getSeList().get(groupIndex);
        if (group == null || group.getSeItems() == null || seqIndex >= group.getSeItems().size()) {
            return "";
        }
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupItem item = group.getSeItems().get(seqIndex);
        if (item == null || item.getSeFileName() == null) {
            return "";
        }
        return item.getSeFileName();
    }

    private byte[] buildBlock(int groupIndex, int seqIndex) {
        byte[] out = new byte[16];
        writeIntLE(out, 0, groupIndex);
        writeIntLE(out, 4, seqIndex);
        return out;
    }

    private void writeIntLE(byte[] out, int offset, int value) {
        out[offset] = (byte) (value & 0xFF);
        out[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        out[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        out[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private int readIntLE(byte[] src, int offset) {
        return (src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
