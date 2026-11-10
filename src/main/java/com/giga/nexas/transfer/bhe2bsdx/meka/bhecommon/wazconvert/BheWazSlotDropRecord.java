package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert;

/** BHE WAZ 转 BSDX 时无法承载的源槽路径。 */
public record BheWazSlotDropRecord(
        String fileName,
        int skillIndex,
        int phaseIndex,
        int unitIndex,
        int sourceSlot,
        Integer sourceObjectIndex,
        Integer nestedSourceSlot
) {

    public BheWazSlotDropRecord(
            String fileName,
            int skillIndex,
            int phaseIndex,
            int unitIndex,
            int sourceSlot
    ) {
        this(fileName, skillIndex, phaseIndex, unitIndex, sourceSlot, null, null);
    }

    public boolean isNested() {
        return nestedSourceSlot != null;
    }

    public String sourceSlotPath() {
        String path = "eventSlot[" + sourceSlot + "]";
        if (!isNested()) {
            return path;
        }
        return path + "/object[" + sourceObjectIndex + "]/CEventEffect[" + nestedSourceSlot + "]";
    }
}
