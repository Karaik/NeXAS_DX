package com.giga.nexasdxeditor.dto.bsdx.waz.parser;

import com.giga.nexasdxeditor.dto.SpmSequenceConst;
import com.giga.nexasdxeditor.dto.bsdx.waz.Waz;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazUnit;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.WazInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.WazInfoUnknown;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.*;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description WazParser
 * 根据逆向代码调整读取逻辑
 */
@Slf4j
public class WazParser {

    public static Waz parseWaz(byte[] bytes, String fileName, String charset) {

//        Integer spmSequence = SpmSequenceConst.MEK_SPM_SEQUENCE.get(fileName);
//        if (spmSequence == null) {
//            return new Waz();
//        }

        Waz waz = new Waz();
        List<Waz.WazBlock> wazBlockList = waz.getWazBlockList();
        try {

            int offset = 0;
            while (offset < bytes.length) {

                Waz.WazBlock wazBlock = new Waz.WazBlock();

                int flag = readInt32(bytes, offset); offset += 4;
                if (flag == 0) {
                    wazBlockList.add(wazBlock);
                    continue;
                }

                // 1.技能信息1
                wazBlock.setSkillNameJapanese(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
                offset += findNullTerminator(bytes, offset) + 1;

                // 2.技能信息2
                wazBlock.setSkillNameEnglish(new String(bytes, offset, findNullTerminator(bytes, offset), StandardCharsets.UTF_8));
                offset += findNullTerminator(bytes, offset) + 1;

                // 3.阶段数
                int phaseQuantity = readInt32(bytes, offset); offset += 4;
                wazBlock.setPhaseQuantity(phaseQuantity);

                // 4.技能数据
                List<Waz.WazBlock.WazPhase> phaseInfoList = wazBlock.getPhasesInfo();
                for (int i = 0; i < phaseQuantity; i++) {
                    Waz.WazBlock.WazPhase wazPhase = new Waz.WazBlock.WazPhase();
                    offset = parseWazPhaseInfo(wazPhase, bytes, offset);
                    phaseInfoList.add(wazPhase);
                }

                int countSuffix = readInt32(bytes, offset); offset += 4;
                Waz.WazSuffix wazSuffix = new Waz.WazSuffix();
                wazSuffix.setCount(countSuffix);
                for (int i = 0; i < countSuffix; i++) {
                    wazSuffix.setInt1(readInt32(bytes, offset)); offset += 4;
                    wazSuffix.setInt2(readInt32(bytes, offset)); offset += 4;
                }

                wazBlockList.add(wazBlock);
            }

        } catch (Exception e) {
            log.info("filename === {}", fileName);
            log.info("error === {}", e.getMessage());
            throw e;
        }
        return waz;
    }

    private static int parseWazPhaseInfo(Waz.WazBlock.WazPhase wazPhase, byte[] bytes, int offset) {
        // 存放单个阶段的数据信息
        List<WazUnit> wazUnitCollection = wazPhase.getWazUnitCollection();

        for (int i = 0; i < 72; i++) { // 逆向得知循环72次
            WazUnit wazUnit = new WazUnit();

            List<WazInfoObject> wazInfoObjectList = wazUnit.getWazInfoObjectList();
            int count1 = readInt32(bytes, offset); offset += 4;
            for (int j = 0; j < count1; j++) {
                try {
                    WazInfoObject eventObject = WazInfoFactory.createEventObject(i);
                    offset = eventObject.readInfo(bytes, offset);
                    wazInfoObjectList.add(eventObject);
                } catch (Exception e) {
                    log.info("error === i={}", i);
                    throw e;
                }
            }

            List<WazInfoUnknown> wazInfoUnknownList = wazUnit.getWazInfoUnknownList();
            int count2 = readInt32(bytes, offset); offset += 4;
            for (int j = 0; j < count2; j++) {
                try {
                    WazInfoUnknown wazInfoUnknown = (WazInfoUnknown) WazInfoFactory.createEventObject(0xFF);
                    offset = wazInfoUnknown.readInfo(bytes, offset);
                    wazInfoUnknownList.add(wazInfoUnknown);
                } catch (Exception e) {
                    log.info("error === i={}", i);
                    throw e;
                }
            }

            wazUnitCollection.add(wazUnit);
        }

        return offset;
    }


}
