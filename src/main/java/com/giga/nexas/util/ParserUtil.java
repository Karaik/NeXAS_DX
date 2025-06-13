package com.giga.nexas.util;


/**
 * @Author 这位同学(Karaik)
 */
public class ParserUtil {

    // 常用于块与块之间分割符
    public static final byte[] FLAG_DATA =  new byte[] { (byte) 0x01, 0x00, 0x00, 0x00 };

    // 常用mek中，武装插槽块与块之间分割符
    public static final byte[] WEAPON_PLUGINS_FLAG_DATA =  new byte[] { (byte) 0x8A, 0x00, 0x00, 0x00 };
    public static final byte[] WEAPON_PLUGINS_FLAG_DATA_2 =  new byte[] { (byte) 0x88, 0x00, 0x00, 0x00 };
    public static final byte[] WEAPON_PLUGINS_FLAG_DATA_3 =  new byte[] { (byte) 0x8B, 0x00, 0x00, 0x00 };

    // BHE
    public static final byte[] WEAPON_PLUGINS_FLAG_DATA_BHE =  new byte[] { (byte) 0xD0, 0x00, 0x00, 0x00 };

    // 数值
    public static final Integer DAT_COLUMN_TYPE_DATA = 0;

    // 字符串
    public static final Integer DAT_COLUMN_TYPE_STRING = 1;

    // 编号？
    public static final Integer DAT_COLUMN_TYPE_UNKNOWN = 2;

    /**
     * 判断句子是否大致上为日语
     */
    public static boolean isLikelyJapanese(String text) {
        int japaneseCharCount = 0;
        for (char c : text.toCharArray()) {
            if ((c >= '\u3040' && c <= '\u309F') || // 平假名
                    (c >= '\u30A0' && c <= '\u30FF') || // 片假名
                    (c >= '\u4E00' && c <= '\u9FFF') || // CJK 统一汉字（包括日语汉字）
                    (c >= '\uFF66' && c <= '\uFF9F')) { // 半角片假名
                japaneseCharCount++;
            }
        }
        // 阈值
        return (japaneseCharCount > text.length() * 0.5);
    }

}
