package com.giga.nexasdxeditor.dto.bsdx.mecha.waz.parser;

import com.giga.nexasdxeditor.dto.bsdx.mecha.waz.Waz;
import com.giga.nexasdxeditor.util.ParserUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.findNullTerminator;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description WazParser
 */
@Slf4j
public class WazParser {

    public static List<Waz> parseWaz(byte[] bytes) {

        List<Waz> wazList = new ArrayList<Waz>();
        try {
            Integer offset = 0;

            while (offset < bytes.length) {

                Waz waz = new Waz();

                // 起始符
                if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.FLAG_DATA)) {
                    offset += 4;
                }

                // 1.技能信息
                waz.setSkillNameKanji(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName("Shift-JIS")));
                offset += findNullTerminator(bytes, offset) + 1;
                waz.setSkillNameEnglish(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName("UTF-8")));
                offset += findNullTerminator(bytes, offset) + 1;

                // 2.技能数据
                offset += parseWazDataInfo(waz, bytes, offset);

                wazList.add(waz);
            }

        } catch (Exception e) {
            log.info("error === {}", e.getMessage());
        }
        return wazList;
    }

    private static int parseWazDataInfo(Waz waz, byte[] bytes, int offset) {

        int start = offset;

        /**
         * 遇到ParserUtil.FLAG_DATA（值为1）后，将后两个字节以Shift-JIS读出
         *
         * 如果shift-jis解析下两个字符不为日语或英语或数字 累加offset
         * 如果shift-jis解析下两个字符为日语或英语或数字 累加offset
         *
         *
          */
        while (start < bytes.length) {
            Integer i = ParserUtil.readInt32(bytes, offset);
            offset += 4;

            if (i == 1) {
                // 读取下四个字节（Shift-JIS编码）
                String nextChars = new String(Arrays.copyOfRange(bytes, offset, offset + 4), Charset.forName("Shift-JIS"));

                // 是否为有效的shift-jis
                if (isJapaneseCharacter(nextChars)) {
                    // 是则代表读到了块起始符，回退
                    offset -= 4;
                    break;
                }
            }
        }

        byte[] dataBytes = Arrays.copyOfRange(bytes, start, offset);
        waz.setData(dataBytes);
        return offset;
    }

    private static boolean isJapaneseCharacter(String str) {
        if (str == null || str.length() != 2) {
            return false; // 必须是两个字符
        }

        for (char c : str.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block != Character.UnicodeBlock.HIRAGANA &&
                    block != Character.UnicodeBlock.KATAKANA &&
                    block != Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                return false; // 不是平假名、片假名或汉字
            }
        }

        return true; // 两个字符均为日语字符
    }

}
