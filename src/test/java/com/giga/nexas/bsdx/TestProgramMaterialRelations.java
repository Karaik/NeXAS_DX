package com.giga.nexas.bsdx;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestProgramMaterialRelations {

    private static final String CHARSET = "windows-31j";

    private static final Path PROGRAM_MATERIAL_GRP = Paths.get("src/main/resources/game/bsdx/grp/ProgramMaterial.grp");
    private static final Path SPRITE_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/SpriteGroup.grp");
    private static final Path SE_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/SeGroup.grp");
    private static final Path BAT_VOICE_GRP = Paths.get("src/main/resources/game/bsdx/grp/BatVoice.grp");
    private static final Path MAP_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/MapGroup.grp");

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    void testTopLevelCountsMatchBoundGrpSizes() throws Exception {
        ProgramMaterialGrp programMaterial = parseProgramMaterial();
        SpriteGroupGrp spriteGroup = parseSpriteGroup();
        SeGroupGrp seGroup = parseSeGroup();
        BatVoiceGrp batVoice = parseBatVoice();

        assertEquals(spriteGroup.getSpriteList().size(), programMaterial.getArray1().size(),
                "ProgramMaterial.array1 size should track SpriteGroup.spriteList size");
        assertEquals(seGroup.getSeList().size(), programMaterial.getArray2().size(),
                "ProgramMaterial.array2 size should track SeGroup.seList size");
        assertEquals(batVoice.getVoiceList().size(), programMaterial.getArray3().size(),
                "ProgramMaterial.array3 size should track BatVoice.voiceList size");
    }

    @Test
    void testArray1ValuesBehaveAsMapGroupIndicesUnderSpriteSlots() throws Exception {
        ProgramMaterialGrp programMaterial = parseProgramMaterial();
        SpriteGroupGrp spriteGroup = parseSpriteGroup();
        MapGroupGrp mapGroup = parseMapGroup();

        for (int i = 0; i < programMaterial.getArray1().size(); i++) {
            for (Integer value : programMaterial.getArray1().get(i).getValues()) {
                assertTrue(value >= 0 && value < mapGroup.getGroupList().size(),
                        "array1[" + i + "] contains out-of-range MapGroup index: " + value);
            }
        }

        assertEquals("TAMA", spriteGroup.getSpriteList().get(0).getSpriteCodeName());
        assertEquals("Tama.spm", spriteGroup.getSpriteList().get(0).getSpriteFileName());
        assertEquals(List.of(39, 45, 72), programMaterial.getArray1().get(0).getValues());
        assertEquals("ASE_L01", mapGroup.getGroupList().get(39).getGroupCodeName());
        assertEquals("mapASSEMBLER_L01_01", mapGroup.getGroupList().get(39).getGroupResourceName());
        assertEquals("NPC_L03", mapGroup.getGroupList().get(45).getGroupCodeName());
        assertEquals("ARK_S02", mapGroup.getGroupList().get(72).getGroupCodeName());

        assertEquals("SMOKE", spriteGroup.getSpriteList().get(3).getSpriteCodeName());
        assertEquals(List.of(0, 1, 2, 9, 11, 26, 33, 34, 35, 36, 37, 38), programMaterial.getArray1().get(3).getValues());
        assertEquals("PRACTICE03", mapGroup.getGroupList().get(0).getGroupCodeName());
        assertEquals("ARK_L06", mapGroup.getGroupList().get(33).getGroupCodeName());
        assertEquals("MILITARY_L01", mapGroup.getGroupList().get(36).getGroupCodeName());

        assertEquals("PIC", spriteGroup.getSpriteList().get(5).getSpriteCodeName());
        assertEquals(List.of(82, 83, 84, 85, 100, 101, 102, 103, 104, 105), programMaterial.getArray1().get(5).getValues());
        assertEquals("ARK_S12", mapGroup.getGroupList().get(82).getGroupCodeName());
        assertEquals("MILITARY_S13", mapGroup.getGroupList().get(105).getGroupCodeName());
    }

    @Test
    void testArray2ValuesBehaveAsSeItemIndicesInsideSeGroups() throws Exception {
        ProgramMaterialGrp programMaterial = parseProgramMaterial();
        SeGroupGrp seGroup = parseSeGroup();

        for (int i = 0; i < programMaterial.getArray2().size(); i++) {
            List<SeGroupGrp.SeGroupItem> seItems = seGroup.getSeList().get(i).getSeItems();
            for (Integer value : programMaterial.getArray2().get(i).getValues()) {
                assertTrue(value >= 0 && value < seItems.size(),
                        "array2[" + i + "] contains out-of-range SeGroup item index: " + value);
            }
        }

        assertEquals("PROGRAM", seGroup.getSeList().get(36).getSeTypeCodeName());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25),
                programMaterial.getArray2().get(36).getValues());
        assertEquals("DASH01", seGroup.getSeList().get(36).getSeItems().get(0).getSeItemCodeName());
        assertEquals("P_Dash01b", seGroup.getSeList().get(36).getSeItems().get(0).getSeFileName());
        assertEquals("EXCANCEL", seGroup.getSeList().get(36).getSeItems().get(7).getSeItemCodeName());
        assertEquals("FCFINISH", seGroup.getSeList().get(36).getSeItems().get(21).getSeItemCodeName());
        assertEquals("COMBOFAILURE", seGroup.getSeList().get(36).getSeItems().get(25).getSeItemCodeName());

        assertEquals("UDAGAWA", seGroup.getSeList().get(37).getSeTypeCodeName());
        assertEquals(List.of(51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65),
                programMaterial.getArray2().get(37).getValues());
        assertEquals("BOMB01", seGroup.getSeList().get(37).getSeItems().get(51).getSeItemCodeName());
        assertEquals("U_bomb01_b", seGroup.getSeList().get(37).getSeItems().get(51).getSeFileName());
        assertEquals("HAHEN14", seGroup.getSeList().get(37).getSeItems().get(65).getSeItemCodeName());

        assertEquals("REACTION", seGroup.getSeList().get(0).getSeTypeCodeName());
        assertEquals(List.of(299, 301, 302, 303, 304, 305, 306, 307, 309), programMaterial.getArray2().get(0).getValues());
        assertEquals("GROUND02", seGroup.getSeList().get(0).getSeItems().get(299).getSeItemCodeName());
        assertEquals("RE_Water01", seGroup.getSeList().get(0).getSeItems().get(301).getSeFileName());
        assertEquals("NUMA04", seGroup.getSeList().get(0).getSeItems().get(309).getSeItemCodeName());
    }

    @Test
    void testArray3TopLevelTracksBatVoiceAndCurrentDatasetHasNoNonEmptyRows() throws Exception {
        ProgramMaterialGrp programMaterial = parseProgramMaterial();
        BatVoiceGrp batVoice = parseBatVoice();

        for (int i = 0; i < programMaterial.getArray3().size(); i++) {
            List<BatVoiceGrp.BatVoice> voices = batVoice.getVoiceList().get(i).getVoices();
            for (Integer value : programMaterial.getArray3().get(i).getValues()) {
                assertTrue(value >= 0 && value < voices.size(),
                        "array3[" + i + "] contains out-of-range BatVoice index: " + value);
            }
        }

        assertEquals("KOU", batVoice.getVoiceList().get(0).getCharacterCodeName());
        assertEquals("AT_A01", batVoice.getVoiceList().get(0).getVoices().get(0).getVoiceCodeName());
        assertEquals("Kou_a0101", batVoice.getVoiceList().get(0).getVoices().get(0).getVoiceFileName());

        assertEquals("RAIN", batVoice.getVoiceList().get(1).getCharacterCodeName());
        assertEquals("NANOHA", batVoice.getVoiceList().get(2).getCharacterCodeName());
        assertEquals("EIZI", batVoice.getVoiceList().get(10).getCharacterCodeName());
        assertEquals("CHRIS_NAVI", batVoice.getVoiceList().get(29).getCharacterCodeName());

        int nonEmptyRows = 0;
        for (ProgramMaterialGrp.IntArray row : programMaterial.getArray3()) {
            if (!row.getValues().isEmpty()) {
                nonEmptyRows++;
            }
        }
        assertEquals(0, nonEmptyRows,
                "current BSDX dataset has no non-empty ProgramMaterial.array3 rows, so value-level voice examples remain unavailable");
    }

    private ProgramMaterialGrp parseProgramMaterial() throws Exception {
        assumeFileExists(PROGRAM_MATERIAL_GRP);
        return (ProgramMaterialGrp) parse(PROGRAM_MATERIAL_GRP).getData();
    }

    private SpriteGroupGrp parseSpriteGroup() throws Exception {
        assumeFileExists(SPRITE_GROUP_GRP);
        return (SpriteGroupGrp) parse(SPRITE_GROUP_GRP).getData();
    }

    private SeGroupGrp parseSeGroup() throws Exception {
        assumeFileExists(SE_GROUP_GRP);
        return (SeGroupGrp) parse(SE_GROUP_GRP).getData();
    }

    private BatVoiceGrp parseBatVoice() throws Exception {
        assumeFileExists(BAT_VOICE_GRP);
        return (BatVoiceGrp) parse(BAT_VOICE_GRP).getData();
    }

    private MapGroupGrp parseMapGroup() throws Exception {
        assumeFileExists(MAP_GROUP_GRP);
        return (MapGroupGrp) parse(MAP_GROUP_GRP).getData();
    }

    private ResponseDTO<?> parse(Path path) throws Exception {
        return bsdxBinService.parse(path.toString(), CHARSET);
    }

    private void assumeFileExists(Path path) {
        assumeTrue(Files.isRegularFile(path), "missing file: " + path.toAbsolutePath());
    }
}
