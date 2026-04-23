package com.giga.nexas.dto.clarias.mek.mekcpu;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

@Data
public class CCpuEventAttack extends CCpuEvent {

    @Data
    @AllArgsConstructor
    public static class CCpuEventAttackType {
        private Integer type;
        private String label;
    }

    public static final CCpuEventAttackType[] CCPU_EVENT_ATTACK_TYPES = {
            new CCpuEventAttackType(0xFFFFFFFF, "slot01")
    };

    private Integer attackField1;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.attackField1 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.attackField1);
    }
}
