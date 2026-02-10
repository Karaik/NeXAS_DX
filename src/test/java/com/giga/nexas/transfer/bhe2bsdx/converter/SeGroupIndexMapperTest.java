package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventSe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SeGroupIndexMapperTest {

    @Test
    void buildMap_shouldAppendMissingSeAndRemapBytes() {
        SeGroupGrp bhe = buildBheSeGroup();
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp bsdx = buildBsdxSeGroup();

        SeGroupIndexMapper mapper = new SeGroupIndexMapper();
        SeGroupIndexMapper.SeGroupMap map = mapper.build(bhe, bsdx, 11);

        assertFalse(map.isEmpty());
        assertEquals(2, map.getMappedPairCount());
        assertEquals(1, map.getAppendedItems());
        assertEquals(1, bsdx.getSeList().get(11).getSeItems().size());
        assertEquals("new.se", bsdx.getSeList().get(11).getSeItems().get(0).getSeFileName());

        byte[] remapped = map.remapBlock(buildSePayload(0, 1));
        assertEquals(11, readIntLE(remapped, 0));
        assertEquals(0, readIntLE(remapped, 4));
    }

    @Test
    void wazConverter_shouldRemapCEventSeBytePayload() {
        SeGroupGrp bhe = buildBheSeGroup();
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp bsdx = buildBsdxSeGroup();
        SeGroupIndexMapper.SeGroupMap map = new SeGroupIndexMapper().build(bhe, bsdx, 11);

        com.giga.nexas.dto.bhe.waz.Waz bheWaz = new com.giga.nexas.dto.bhe.waz.Waz("demo");
        com.giga.nexas.dto.bhe.waz.Waz.Skill skill = new com.giga.nexas.dto.bhe.waz.Waz.Skill();
        com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase phase = new com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit();
        unit.setUnitQuantity(58);

        CEventSe event = new CEventSe(29);
        event.setStartFrame(0);
        event.setEndFrame(30);
        event.setCount(1);
        event.setByteDataList(List.of(buildSePayload(0, 1)));
        unit.getSkillInfoObjectList().add(event);

        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        bheWaz.getSkillList().add(skill);

        com.giga.nexas.dto.bsdx.waz.Waz out = new WazConverter().convert(bheWaz, null, null, map);
        assertNotNull(out);
        assertEquals(1, out.getSkillList().size());
        assertEquals(1, out.getSkillList().get(0).getPhasesInfo().size());
        assertEquals(1, out.getSkillList().get(0).getPhasesInfo().get(0).getSkillUnitCollection().size());

        com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe outEvent =
                (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe)
                        out.getSkillList().get(0)
                                .getPhasesInfo().get(0)
                                .getSkillUnitCollection().get(0)
                                .getSkillInfoObjectList().get(0);
        assertEquals(1, outEvent.getCount());
        byte[] payload = outEvent.getByteDataList().get(0);
        assertEquals(11, readIntLE(payload, 0));
        assertEquals(0, readIntLE(payload, 4));
    }

    private SeGroupGrp buildBheSeGroup() {
        SeGroupGrp bhe = new SeGroupGrp();
        SeGroupGrp.SeGroupGroup group = new SeGroupGrp.SeGroupGroup();
        group.setExistFlag(1);
        group.setSeType("BHE");
        group.setSeTypeCodeName("BHE");
        group.setSeItems(new ArrayList<>());

        SeGroupGrp.SeGroupItem existing = new SeGroupGrp.SeGroupItem();
        existing.setExistFlag(1);
        existing.setSeFileName("same.se");
        existing.setSeItemName("same");
        existing.setSeItemCodeName("same");
        group.getSeItems().add(existing);

        SeGroupGrp.SeGroupItem missing = new SeGroupGrp.SeGroupItem();
        missing.setExistFlag(1);
        missing.setSeFileName("new.se");
        missing.setSeItemName("new");
        missing.setSeItemCodeName("new");
        group.getSeItems().add(missing);

        bhe.setSeList(new ArrayList<>(List.of(group)));
        return bhe;
    }

    private com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp buildBsdxSeGroup() {
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp bsdx = new com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp();
        List<com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupGroup> groups = new ArrayList<>();
        for (int i = 0; i <= 11; i++) {
            com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupGroup group =
                    new com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupGroup();
            group.setExistFlag(1);
            group.setSeType("G" + i);
            group.setSeTypeCodeName("G" + i);
            group.setSeItems(new ArrayList<>());
            groups.add(group);
        }
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupItem existing =
                new com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupItem();
        existing.setExistFlag(1);
        existing.setSeFileName("same.se");
        existing.setSeItemName("same");
        existing.setSeItemCodeName("same");
        groups.get(0).getSeItems().add(existing);

        bsdx.setSeList(groups);
        return bsdx;
    }

    private byte[] buildSePayload(int groupIndex, int seqIndex) {
        byte[] out = new byte[16];
        writeIntLE(out, 0, groupIndex);
        writeIntLE(out, 4, seqIndex);
        writeIntLE(out, 8, 123);
        writeIntLE(out, 12, 456);
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
}
