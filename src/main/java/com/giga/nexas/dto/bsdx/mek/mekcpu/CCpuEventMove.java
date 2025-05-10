package com.giga.nexas.dto.bsdx.mek.mekcpu;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description CCpuEventMove
 */
@Data
public class CCpuEventMove extends CCpuEvent {

    @Data
    @AllArgsConstructor
    public static class CCpuEventMoveType {
        private Integer type;
        private String description;
    }

    public static final CCpuEventMoveType[] CCPU_EVENT_MOVE_TYPES = {
            new CCpuEventMoveType(0xFFFFFFFF, "移動タイプ"),
            new CCpuEventMoveType(0xFFFFFFFF, "移動速度(/10)"),
            new CCpuEventMoveType(0xFFFFFFFF, "移動慣性"),
            new CCpuEventMoveType(0xFFFFFFFF, "移動目標タイプ"),
            new CCpuEventMoveType(0xFFFFFFFF, "移動目標角度補正"),
            new CCpuEventMoveType(0xFFFFFFFF, "視点タイプ"),
            new CCpuEventMoveType(0xFFFFFFFF, "視点角度補正"),
            new CCpuEventMoveType(0xFFFFFFFF, "ジャンプタイプ"),
            new CCpuEventMoveType(0xFFFFFFFF, "上昇処理用変数1"),
            new CCpuEventMoveType(0xFFFFFFFF, "上昇処理用変数2"),
            new CCpuEventMoveType(0xFFFFFFFF, "汎用フラグ"),
            new CCpuEventMoveType(0xFFFFFFFF, "攻撃確率補正")
    };

    private Integer moveType;
    private Integer moveSpeed;
    private Integer moveInertia;
    private Integer moveTargetType;
    private Integer moveTargetAngleCorrection;
    private Integer viewpointType;
    private Integer viewpointAngleCorrection;
    private Integer jumpType;
    private Integer ascentVar1;
    private Integer ascentVar2;
    private Integer genericFlag;
    private Integer attackProbabilityCorrection;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.moveType = reader.readInt();
        this.moveSpeed = reader.readInt();
        this.moveInertia = reader.readInt();
        this.moveTargetType = reader.readInt();
        this.moveTargetAngleCorrection = reader.readInt();
        this.viewpointType = reader.readInt();
        this.viewpointAngleCorrection = reader.readInt();
        this.jumpType = reader.readInt();
        this.ascentVar1 = reader.readInt();
        this.ascentVar2 = reader.readInt();
        this.genericFlag = reader.readInt();
        this.attackProbabilityCorrection = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.moveType);
        writer.writeInt(this.moveSpeed);
        writer.writeInt(this.moveInertia);
        writer.writeInt(this.moveTargetType);
        writer.writeInt(this.moveTargetAngleCorrection);
        writer.writeInt(this.viewpointType);
        writer.writeInt(this.viewpointAngleCorrection);
        writer.writeInt(this.jumpType);
        writer.writeInt(this.ascentVar1);
        writer.writeInt(this.ascentVar2);
        writer.writeInt(this.genericFlag);
        writer.writeInt(this.attackProbabilityCorrection);
    }
}
