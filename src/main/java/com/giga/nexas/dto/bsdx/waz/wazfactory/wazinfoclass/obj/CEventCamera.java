package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import com.giga.nexas.util.InfoCollectionMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventCamera__Read
 */
@Data
@NoArgsConstructor
public class CEventCamera extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventCameraType {
        private Integer type;
        private String description;
    }

    public static final CEventCameraType[] CEVENT_CAMERA_ENTRIES = {
            new CEventCameraType(0x5, "位置"),
            new CEventCameraType(0x9, "方向"),
            new CEventCameraType(0x0, "距離"),
            new CEventCameraType(0x0, "高さ"),
            new CEventCameraType(0xFFFFFFFF, "フラグ")
    };

    private Short short1;
    private List<CEventCameraUnit> ceventCameraUnitList = new ArrayList<>();

    public CEventCamera(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventCameraUnit {
        private Integer ceventCameraUnitQuantity;
        private String description;
        private Integer buffer;
        private Integer unitSlotNum; // 记录槽位便于write
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        setShort1(reader.readShort());

        this.ceventCameraUnitList.clear();

        for (int i = 0; i < 5; i++) {
            int buffer = reader.readInt();

            CEventCameraUnit unit = new CEventCameraUnit();
            unit.setCeventCameraUnitQuantity(i);
            unit.setDescription(CEVENT_CAMERA_ENTRIES[i].getDescription());
            unit.setBuffer(buffer);
            unit.setUnitSlotNum(i);

            if (buffer != 0) {
                int typeId = CEVENT_CAMERA_ENTRIES[i].getType();
                SkillInfoObject obj = SkillInfoFactory.createCEventObjectByTypeBsdx(typeId);
                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
                ceventCameraUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeShort(this.short1);

        for (int i = 0; i < 5; i++) {
            CEventCameraUnit target = null;

            for (CEventCameraUnit unit : this.ceventCameraUnitList) {
                if (unit.getUnitSlotNum() != null && unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
            }

            if (target != null) {
                writer.writeInt(target.getBuffer());
                if (target.getBuffer() != 0 && target.getData() != null) {
                    target.getData().writeInfo(writer);
                }
            } else {
                writer.writeInt(0);
            }
        }
    }

    public void transBheCEventCameraToBsdx(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject src,
            CEventCamera bsdx) {

        if (!(src instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera bhe)) {
            return;
        }

        bsdx.getCeventCameraUnitList().clear();
        var bheUnits = bhe.getCeventCameraUnitList();
        if (bheUnits == null || bheUnits.isEmpty()) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            CEventCameraUnit bsdxUnit = new CEventCameraUnit();
            bsdxUnit.setCeventCameraUnitQuantity(i);
            bsdxUnit.setDescription(CEVENT_CAMERA_ENTRIES[i].getDescription());
            bsdxUnit.setUnitSlotNum(i);
            bsdxUnit.setBuffer(0);

            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera.CEventCameraUnit bheUnit = null;
            for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera.CEventCameraUnit unit : bheUnits) {
                if (unit.getUnitSlotNum() != null && unit.getUnitSlotNum() == i) {
                    bheUnit = unit;
                    break;
                }
            }
            if (bheUnit != null) {
                Integer buffer = bheUnit.getBuffer();
                if (buffer == null) buffer = (bheUnit.getData() != null ? 1 : 0);
                if (buffer != 0 && bheUnit.getData() != null) {
                    SkillInfoObject inner = SkillInfoFactory.createCEventObjectByTypeBsdx(CEVENT_CAMERA_ENTRIES[i].getType());
                    cn.hutool.core.bean.BeanUtil.copyProperties(bheUnit.getData(), inner);
                    InfoCollectionMapper.copyBheToBsdx(bheUnit.getData(), inner);
                    inner.setSlotNum(CEVENT_CAMERA_ENTRIES[i].getType());
                    inner.typeId = CEVENT_CAMERA_ENTRIES[i].getType(); // 修复：恢复正确的 BSDX typeId
                    // DX camera requires an object target; this verified self position changes snapshot to live object.
                    // Check source and target: the mapper normalizes nulls and truncates source collections.
                    if (i == 0
                            && bheUnit.getData() instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventTerm sourcePosition
                            && Integer.valueOf(7).equals(sourcePosition.getTypeId())
                            && inner instanceof CEventTerm targetPosition
                            && Integer.valueOf(5).equals(targetPosition.getTypeId())
                            && sourcePosition.getBheInfoCollectionList() != null
                            && sourcePosition.getBheInfoCollectionList().size() == 1
                            && targetPosition.getBsdxInfoCollectionList() != null
                            && targetPosition.getBsdxInfoCollectionList().size() == 1) {
                        var sourceCollection = sourcePosition.getBheInfoCollectionList().get(0);
                        var targetCollection = targetPosition.getBsdxInfoCollectionList().get(0);
                        if (sourceCollection != null && targetCollection != null
                                && Integer.valueOf(20).equals(sourceCollection.getInt1())
                                && List.of(3).equals(sourceCollection.getTypeList())
                                && sourceCollection.getParamList() != null && sourceCollection.getParamList().isEmpty()
                                && List.of(0, 0).equals(sourceCollection.getIntList3())
                                && sourceCollection.getIntList4() != null && sourceCollection.getIntList4().isEmpty()
                                && Integer.valueOf(0).equals(sourceCollection.getInt2())
                                && Integer.valueOf(20).equals(targetCollection.getInt1())
                                && List.of(3).equals(targetCollection.getTypeList())
                                && targetCollection.getParamList() != null && targetCollection.getParamList().isEmpty()
                                && List.of(0, 0).equals(targetCollection.getIntList3())
                                && targetCollection.getIntList4() != null && targetCollection.getIntList4().isEmpty()
                                && Integer.valueOf(0).equals(targetCollection.getInt2())) {
                            targetCollection.setTypeList(new ArrayList<>(List.of(1)));
                        }
                    }
                    bsdxUnit.setBuffer(1);
                    bsdxUnit.setData(inner);
                }
            }

            if (bsdxUnit.getBuffer() != null && bsdxUnit.getBuffer() != 0) {
                bsdx.getCeventCameraUnitList().add(bsdxUnit);
            }
        }
    }

}

