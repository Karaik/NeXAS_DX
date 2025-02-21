package com.giga.nexasdxeditor.dto.bsdx.waz.parser;

import com.giga.nexasdxeditor.dto.SpmSequenceConst;
import com.giga.nexasdxeditor.dto.bsdx.waz.Waz;
import com.giga.nexasdxeditor.exception.BusinessException;
import com.giga.nexasdxeditor.util.ParserUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.*;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description WazParser
 */
@Slf4j
public class WazParser {

    public static List<Waz> parseWaz(byte[] bytes, String fileName, String charset) {

        List<Waz> wazList = new ArrayList<Waz>();
        try {
            int offset = 0;

            Integer spmSequence = SpmSequenceConst.MEK_SPM_SEQUENCE.get(fileName);

            if (spmSequence == null) {
                return wazList;
            }

            while (offset < bytes.length) {

                Waz waz = new Waz(SpmSequenceConst.MEK_SPM_SEQUENCE.get(fileName));

                // 起始符
                if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.FLAG_DATA)) {
                    offset += 4;
                } else {
                    offset += 4;
//                    throw new BusinessException(500, "文件数据格式错误！（起始符）");
                }

                // 1.技能信息
                waz.setSkillNameKanji(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
                offset += findNullTerminator(bytes, offset) + 1;
                waz.setSkillNameEnglish(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName("UTF-8")));
                offset += findNullTerminator(bytes, offset) + 1;
                // 阶段数
                waz.setPhaseQuantity(readInt32(bytes, offset));
                offset += 4;

                // 3.技能数据
                offset = parseWazDataInfo(waz, bytes, offset);

                wazList.add(waz);
            }

        } catch (Exception e) {
            log.info("filename === {}", fileName);
            log.info("error === {}", e.getMessage());
        }
        return wazList;
    }

    private static int parseWazDataInfo(Waz waz, byte[] bytes, int offset) {

        int start = offset;
        // 循环读
        while (offset < bytes.length) {
            // 如果符合起始符特征，则判断下面的字符是否可能为日语，如果是，大概率为起始符
            if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.FLAG_DATA)) {
                int nextStartPoint = offset + 4;
                int strByteLength = findNullTerminator(bytes, nextStartPoint);
                if (strByteLength > 1) {
                    String nextData = new String(bytes, nextStartPoint, strByteLength, Charset.forName("Shift-JIS"));
                    if (isLikelyJapanese(nextData)) {
                        break;
                    }
                }
            }
            offset  += 2; // 可能存在两个偏移，所以以2字节为单位判断结尾
        }

        byte[] wazPhaseInfo = Arrays.copyOfRange(bytes, start, offset);
        parseWazPhaseInfo(waz, wazPhaseInfo);

        waz.setData(Arrays.copyOfRange(bytes, start, offset));

        return offset;
    }

    private static void parseWazPhaseInfo(Waz waz, byte[] bytes) {
        int offsetInner = 0;

        List<Waz.WazPhase> phasesInfoList = waz.getPhasesInfo();
        for (int i = 0; i < waz.getPhaseQuantity(); i++) {
            Waz.WazPhase wazPhase = new Waz.WazPhase();

            // 动作数
            wazPhase.setActionQuantity(readInt32(bytes, offsetInner));
            offsetInner += 4;

            // 动作信息
            List<Waz.WazPhase.spmCallingInfo> spmCallingInfoList = wazPhase.getSpmCallingInfoList();
            for (int j = 0; j < wazPhase.getActionQuantity(); j++) {
                Waz.WazPhase.spmCallingInfo spmCallingInfo = new Waz.WazPhase.spmCallingInfo();
                // 开始帧
                spmCallingInfo.setStartFrame(readInt32(bytes, offsetInner));
                offsetInner += 4;
                // 结束帧
                spmCallingInfo.setEndFrame(readInt32(bytes, offsetInner));
                offsetInner += 4;
                // spm序列号
                spmCallingInfo.setSpmFileSequence(readInt32(bytes, offsetInner));
                offsetInner += 4;
                // 组
                spmCallingInfo.setActionGroupNumber(readInt32(bytes, offsetInner));
                offsetInner += 4;
                // 动作
                spmCallingInfo.setActionNumber(readInt32(bytes, offsetInner));
                offsetInner += 4;

                spmCallingInfoList.add(spmCallingInfo);
            }

//            int start = offsetInner;
            setWazPhaseUnit(wazPhase, bytes, offsetInner - spmCallingInfoList.size() * 4*5 - 4);
//            offsetInner += padding;

//            wazPhase.setWazPhaseData(Arrays.copyOfRange(bytes, start, offsetInner));


            int start = offsetInner;
            if (i == waz.getPhaseQuantity() - 1) {
                // 最后一个phase
                wazPhase.setWazPhaseData(Arrays.copyOfRange(bytes, start, bytes.length));
            } else {
                offsetInner = findThisWazPhaseEnd(waz, wazPhase, bytes, offsetInner);
                wazPhase.setWazPhaseData(Arrays.copyOfRange(bytes, start, offsetInner));
            }

            phasesInfoList.add(wazPhase);

        }

    }

    private static int setWazPhaseUnit(Waz.WazPhase wazPhase, byte[] bytes, int offset) {
        // 存放单个阶段的数据信息
        List<Waz.WazPhase.wazPhaseDataUnit> wazPhaseDataUnitList = new ArrayList<>();

            int startPoint = offset;
            for (int i = 0; i < 72; i++) { // 逆向得知循环72次
                try{
                    Waz.WazPhase.wazPhaseDataUnit wazPhaseDataUnit = new Waz.WazPhase.wazPhaseDataUnit();

                    // 读?*4的unit1
                    int unit1Count = readInt32(bytes, startPoint);
                    startPoint += 4;
                    List<Waz.WazPhase.wazPhaseDataUnit.Unit1> unit1List = wazPhaseDataUnit.getUnit1List();
                    for (int j = 0; j < unit1Count; j++) {
                        Waz.WazPhase.wazPhaseDataUnit.Unit1 unit1 = new Waz.WazPhase.wazPhaseDataUnit.Unit1();


                        for (int k = 0; k < 5; k++) {
                            unit1.getData().add(readInt32(bytes, startPoint));
                            startPoint += 4;
                        }


                        // todo 万策尽きた！！！

                        unit1List.add(unit1);
                    }

                    // 读5*4的unit2
                    int unit2Count = readInt32(bytes, startPoint);
                    startPoint += 4;
                    List<Waz.WazPhase.wazPhaseDataUnit.Unit2> unit2List = wazPhaseDataUnit.getUnit2List();
                    for (int j = 0; j < unit2Count; j++) {
                        Waz.WazPhase.wazPhaseDataUnit.Unit2 unit2 = new Waz.WazPhase.wazPhaseDataUnit.Unit2();

                        unit2.setInt1(readInt32(bytes, startPoint));
                        startPoint += 4;
                        unit2.setInt2(readInt32(bytes, startPoint));
                        startPoint += 4;
                        unit2.setInt3(readInt32(bytes, startPoint));
                        startPoint += 4;
                        unit2.setInt4(readInt32(bytes, startPoint));
                        startPoint += 4;
                        unit2.setInt5(readInt32(bytes, startPoint));
                        startPoint += 4;

                        unit2List.add(unit2);
                    }

                    wazPhaseDataUnitList.add(wazPhaseDataUnit);
                } catch (Exception e) {
                    log.info("for === {}", i+1);
                    log.info("error === {}", e.getMessage());
        //            throw new RuntimeException(e);
                }
            }

        wazPhase.setWazPhaseDataUnitList(wazPhaseDataUnitList);

        return startPoint;
    }

    /**
     * 逆向过程中发现，在部分武装，尤其是FC中，经常会出现中途开始偏移了两个字节的情况，
     * 从而导致后续读取粒度为4字节时会产生无论如何都找不到标识符的情况，
     * 故此，追加mayPaddingQuantity，对于int32而言，偏移一位就会产生巨大的值上的偏差，
     * 当连续读取多过非常见非0数据时（以2^16为界），暂时认作中间产生了2字节的偏移，将其手动删除
      */
    private static int findThisWazPhaseEnd(Waz waz, Waz.WazPhase wazPhase, byte[] bytes, int offsetInner) {

        int mayPaddingQuantity = 0;
        int paddingOffset = 0;
        while (offsetInner < bytes.length) {

            // 读取当前整数（通常是动作数n或SpmSequence标识）
            int currentValue = readInt32(bytes, offsetInner);
            // 2^16 00 00 01 00
            if (Math.abs(currentValue) >= 1 << 16) {
                // 记录第一次进来时的offset
                if (mayPaddingQuantity == 0) {
                    paddingOffset = offsetInner;
                } else if (mayPaddingQuantity == 10) { // 超过10次，断定为已经产生偏移
                    wazPhase.setPaddingOffset(paddingOffset);
                    offsetInner = paddingOffset - 2; // 直接删掉
                    mayPaddingQuantity = 0;
                    continue;
                }
                mayPaddingQuantity++;
            } else if (currentValue != 0) {
                // 0不计入有效值内
                mayPaddingQuantity = 0;
            }

            // 1. 检查是否为 -1 或 SpmSequence
            if (currentValue == -1 || currentValue == waz.getSpmSequence().intValue()) {

                // 2. 检查前12字节的int是否大于0（动作数）
                if (readInt32(bytes, offsetInner - 12) > 0) {
                    int actionQuantity = readInt32(bytes, offsetInner - 12);
                    int innerStart = offsetInner - 8;

                    if (actionQuantity > 50) {
                        offsetInner += 4;
                        continue;
                    }

                    if (innerStart + actionQuantity * 4 * 5 > bytes.length) {
                        throw new BusinessException(500, "waz解析错误！");
                    }

                    boolean isEnd = true;
                    for (int i = 0; i < actionQuantity; i++) {
                        int startFrame = readInt32(bytes, innerStart + 4 * 5 * i);   // 起始帧
                        int endFrame = readInt32(bytes, innerStart + 4 * 5 * i + 4); // 结束帧
                        int spmSequence = readInt32(bytes, innerStart + 4 * 5 * i + 8); // spm序列号
                        int actionGroupNumber = readInt32(bytes, innerStart + 4 * 5 * i + 12); // 动作组序号
                        int actionNumber = readInt32(bytes, innerStart + 4 * 5 * i + 16); // 动作序号
                        if (startFrame < 0 || endFrame < 0 ||
                                (spmSequence != -1 && spmSequence != waz.getSpmSequence().intValue()) ||
                                actionGroupNumber < 0 || actionNumber < 0) {
                            isEnd = false;
                            break;
                        }
                    }
                    if (isEnd) {
                        return offsetInner - 12;
                    }
                }
            }

            offsetInner += 4; // 向下一个块继续检查
        }

        // 如果找不到合适的阶段结束，返回文件末尾
        return bytes.length;
    }

}
