package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import lombok.Data;

import java.util.Arrays;

import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description CEventStatus
 * CEventStatus__Read
 */
@Data
public class CEventStatus extends WazInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private Integer int11;
    private Integer int12;
    private Integer int13;
    private Integer int14;
    private Integer int15;
    private Integer int16;
    private Integer int17;
    private Integer int18;
    private Integer int19;
    private Integer int20;
    private Integer int21;
    private Integer int22;
    private Integer int23;
    private Integer int24;
    private Integer int25;
    private byte[] byteData1;
    private byte[] byteData2;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset += super.readInfo(bytes, offset);

        byteData1 = Arrays.copyOfRange(bytes, offset, offset + 15); // 读取15字节
        offset += 15;

        byteData2 = Arrays.copyOfRange(bytes, offset, offset + 60); // 读取60字节
        offset += 60;

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;
        setInt4(readInt32(bytes, offset)); offset += 4;
        setInt5(readInt32(bytes, offset)); offset += 4;
        setInt6(readInt32(bytes, offset)); offset += 4;
        setInt7(readInt32(bytes, offset)); offset += 4;
        setInt8(readInt32(bytes, offset)); offset += 4;
        setInt9(readInt32(bytes, offset)); offset += 4;
        setInt10(readInt32(bytes, offset)); offset += 4;
        setInt11(readInt32(bytes, offset)); offset += 4;
        setInt12(readInt32(bytes, offset)); offset += 4;
        setInt13(readInt32(bytes, offset)); offset += 4;
        setInt14(readInt32(bytes, offset)); offset += 4;
        setInt15(readInt32(bytes, offset)); offset += 4;
        setInt16(readInt32(bytes, offset)); offset += 4;
        setInt17(readInt32(bytes, offset)); offset += 4;
        setInt18(readInt32(bytes, offset)); offset += 4;
        setInt19(readInt32(bytes, offset)); offset += 4;
        setInt20(readInt32(bytes, offset)); offset += 4;
        setInt21(readInt32(bytes, offset)); offset += 4;
        setInt22(readInt32(bytes, offset)); offset += 4;
        setInt23(readInt32(bytes, offset)); offset += 4;
        setInt24(readInt32(bytes, offset)); offset += 4;
        setInt25(readInt32(bytes, offset)); offset += 4;

        for (int i = 0; i < 25; i++) {
            // TODO
//            int __thiscall CEventStatus::Read(int *this, int Buffer)
//            {
//                int v2; // edi
//                int result; // eax
//                int v5; // ebx
//                int *v6; // esi
//
//                v2 = Buffer;
//                read_spm_frame(this[1], Buffer);
//                File::read(v2, this + 27, 0xFu);
//                File::read(v2, this + 31, 0x3Cu);
//                File::read(v2, this + 46, 1u);
//                File::read(v2, this + 47, 4u);
//                File::read(v2, this + 48, 4u);
//                File::read(v2, this + 49, 4u);
//                File::read(v2, this + 50, 4u);
//                File::read(v2, this + 51, 4u);
//                File::read(v2, this + 52, 4u);
//                File::read(v2, this + 53, 4u);
//                File::read(v2, this + 54, 4u);
//                result = File::read(v2, this + 55, 4u);
//                v5 = 0;
//                v6 = this + 2;
//                do
//                {
//                    if ( v5 != -99999 )
//                    {
//                        result = File::read(v2, &Buffer, 4u);
//                        if ( Buffer )
//                        {
//                            if ( !*v6 )
//          *v6 = makeObjectOfType(CEventStatus_Types[2 * v5]);
//                            result = (*(**v6 + 24))(*v6, v2);
//                        }
//                    }
//                    ++v5;
//                    ++v6;
//                }
//                while ( v5 < 25 );
//                return result;
//            }
        }

        return offset;
    }
}
