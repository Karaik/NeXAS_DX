package com.giga.nexas.dto.bsdx.mek.mekcpu;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;


/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description CCpuEventAttack
 */
@Data
public class CCpuEventAttack extends CCpuEvent {

    @Data
    @AllArgsConstructor
    public static class CCpuEventAttackType {
        private Integer type;
        private String description;
    }

    public static final CCpuEventAttackType[] CCPU_EVENT_ATTACK_TYPES = {
            new CCpuEventAttackType(0xFFFFFFFF, "技リスト番号")
    };

    // 仅记录用
    public String wazaName;

    private Integer mekWeaponInfoMapNo;


    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.mekWeaponInfoMapNo = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.mekWeaponInfoMapNo);
    }
}
