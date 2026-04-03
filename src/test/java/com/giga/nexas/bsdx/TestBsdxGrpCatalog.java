package com.giga.nexas.bsdx;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.TermGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestBsdxGrpCatalog {

    private static final String CHARSET = "windows-31j";

    private static final Path BAT_VOICE_GRP = Paths.get("src/main/resources/game/bsdx/grp/BatVoice.grp");
    private static final Path MAP_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/MapGroup.grp");
    private static final Path MEKA_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/MekaGroup.grp");
    private static final Path PROGRAM_MATERIAL_GRP = Paths.get("src/main/resources/game/bsdx/grp/ProgramMaterial.grp");
    private static final Path SE_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/SeGroup.grp");
    private static final Path SPRITE_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/SpriteGroup.grp");
    private static final Path TERM_GRP = Paths.get("src/main/resources/game/bsdx/grp/Term.grp");
    private static final Path WAZA_GROUP_GRP = Paths.get("src/main/resources/game/bsdx/grp/WazaGroup.grp");

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    void testBatVoiceGrpRealDataShape() throws Exception {
        BatVoiceGrp batVoice = parse(BAT_VOICE_GRP, BatVoiceGrp.class);

        assertEquals(13, batVoice.getVoiceTypeList().size());
        assertEquals(30, batVoice.getVoiceList().size());

        assertEquals("DAM_S", batVoice.getVoiceTypeList().get(0).getVoiceTypeCodeName());
        assertEquals("DAM_L", batVoice.getVoiceTypeList().get(1).getVoiceTypeCodeName());
        assertEquals("REINFORCE", batVoice.getVoiceTypeList().get(12).getVoiceTypeCodeName());

        assertEquals("KOU", batVoice.getVoiceList().get(0).getCharacterCodeName());
        assertEquals("AT_A01", batVoice.getVoiceList().get(0).getVoices().get(0).getVoiceCodeName());
        assertEquals("Kou_a0101", batVoice.getVoiceList().get(0).getVoices().get(0).getVoiceFileName());

        assertEquals("CHRIS_NAVI", batVoice.getVoiceList().get(29).getCharacterCodeName());
        assertEquals("START01", batVoice.getVoiceList().get(29).getVoices().get(0).getVoiceCodeName());
    }

    @Test
    void testMapGroupGrpRealDataShape() throws Exception {
        MapGroupGrp mapGroup = parse(MAP_GROUP_GRP, MapGroupGrp.class);

        assertEquals(372, mapGroup.getGroupList().size());

        assertEquals("PRACTICE03", mapGroup.getGroupList().get(0).getGroupCodeName());
        assertEquals("map_PRACTICE_L02", mapGroup.getGroupList().get(0).getGroupResourceName());
        assertEquals(0, mapGroup.getGroupList().get(0).getItems().size());

        assertEquals("ASE_L01", mapGroup.getGroupList().get(39).getGroupCodeName());
        assertEquals("mapASSEMBLER_L01_01", mapGroup.getGroupList().get(39).getGroupResourceName());

        assertEquals("ARK_S02", mapGroup.getGroupList().get(72).getGroupCodeName());
        assertEquals("mapARK_S01_02", mapGroup.getGroupList().get(72).getGroupResourceName());
    }

    @Test
    void testMekaGroupGrpRealDataShape() throws Exception {
        MekaGroupGrp mekaGroup = parse(MEKA_GROUP_GRP, MekaGroupGrp.class);

        assertEquals(103, mekaGroup.getMekaList().size());

        assertEquals("KOU", mekaGroup.getMekaList().get(0).getMekaCodeName());
        assertEquals("SORA2", mekaGroup.getMekaList().get(10).getMekaCodeName());
        assertEquals("ZAKO215A", mekaGroup.getMekaList().get(102).getMekaCodeName());
    }

    @Test
    void testProgramMaterialGrpRealDataShape() throws Exception {
        ProgramMaterialGrp programMaterial = parse(PROGRAM_MATERIAL_GRP, ProgramMaterialGrp.class);

        assertEquals(138, programMaterial.getArray1().size());
        assertEquals(38, programMaterial.getArray2().size());
        assertEquals(30, programMaterial.getArray3().size());
    }

    @Test
    void testSeGroupGrpRealDataShape() throws Exception {
        SeGroupGrp seGroup = parse(SE_GROUP_GRP, SeGroupGrp.class);

        assertEquals(38, seGroup.getSeList().size());

        assertEquals("REACTION", seGroup.getSeList().get(0).getSeTypeCodeName());
        assertEquals("BOM01", seGroup.getSeList().get(0).getSeItems().get(0).getSeItemCodeName());

        assertEquals("PROGRAM", seGroup.getSeList().get(36).getSeTypeCodeName());
        assertEquals("DASH01", seGroup.getSeList().get(36).getSeItems().get(0).getSeItemCodeName());
        assertEquals("COMBOFAILURE", seGroup.getSeList().get(36).getSeItems().get(25).getSeItemCodeName());

        assertEquals("UDAGAWA", seGroup.getSeList().get(37).getSeTypeCodeName());
        assertEquals("BOMB01", seGroup.getSeList().get(37).getSeItems().get(51).getSeItemCodeName());
    }

    @Test
    void testSpriteGroupGrpRealDataShape() throws Exception {
        SpriteGroupGrp spriteGroup = parse(SPRITE_GROUP_GRP, SpriteGroupGrp.class);

        assertEquals(138, spriteGroup.getSpriteList().size());

        assertEquals("TAMA", spriteGroup.getSpriteList().get(0).getSpriteCodeName());
        assertEquals("Tama.spm", spriteGroup.getSpriteList().get(0).getSpriteFileName());

        assertEquals("PIC", spriteGroup.getSpriteList().get(5).getSpriteCodeName());
        assertEquals("COMBOINFO", spriteGroup.getSpriteList().get(137).getSpriteCodeName());
    }

    @Test
    void testTermGrpRealDataShape() throws Exception {
        TermGrp termGrp = parse(TERM_GRP, TermGrp.class);

        assertEquals(30, termGrp.getTermList().size());

        assertEquals("PARENT", termGrp.getTermList().get(0).getTermGroupCodeName());
        assertEquals("OBJECT1", termGrp.getTermList().get(0).getTermItemList().get(2).getTermItemDescription());

        assertEquals("OBJECT1", termGrp.getTermList().get(7).getTermGroupCodeName());
        assertEquals("TOUCH", termGrp.getTermList().get(7).getTermItemList().get(7).getTermItemDescription());

        assertEquals("TOUCH", termGrp.getTermList().get(15).getTermGroupCodeName());
        assertEquals("GROUND", termGrp.getTermList().get(15).getTermItemList().get(17).getTermItemDescription());
    }

    @Test
    void testWazaGroupGrpRealDataShape() throws Exception {
        WazaGroupGrp wazaGroup = parse(WAZA_GROUP_GRP, WazaGroupGrp.class);

        assertEquals(110, wazaGroup.getWazaList().size());

        assertEquals("EFFECT", wazaGroup.getWazaList().get(0).getWazaCodeName());
        assertEquals("Effect", wazaGroup.getWazaList().get(0).getWazaDisplayName());

        assertEquals("ZAKO215A", wazaGroup.getWazaList().get(109).getWazaCodeName());
        assertEquals("Zako215a", wazaGroup.getWazaList().get(109).getWazaDisplayName());
    }

    private <T> T parse(Path path, Class<T> type) throws Exception {
        assumeTrue(Files.isRegularFile(path), "missing file: " + path.toAbsolutePath());
        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
        return type.cast(dto.getData());
    }
}
