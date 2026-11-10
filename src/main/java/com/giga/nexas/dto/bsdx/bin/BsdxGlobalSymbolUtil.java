package com.giga.nexas.dto.bsdx.bin;

import java.util.List;
import java.util.Locale;

/**
 * BSDX 伪代码别名辅助工具。
 *
 * <p>这里专门解决两件事：
 * 一是把原始全局符号名或本地属性名转换成伪代码里可安全显示、可回编识别的别名；
 * 二是保证 renderer、recognizer、测试导出使用同一套命名规则。
 */
public final class BsdxGlobalSymbolUtil {

    /**
     * 统一的全局符号别名前缀。
     *
     * <p>加前缀是为了避免与普通属性名、局部变量名发生冲突。
     */
    public static final String ALIAS_PREFIX = "g";

    /**
     * 本地属性安全别名前缀。
     */
    public static final String PROPERTY_ALIAS_PREFIX = "p";

    private BsdxGlobalSymbolUtil() {
    }

    /**
     * 为给定的全局索引生成可逆别名。
     *
     * <p>别名格式固定为 `g{index}_{sanitizedSymbol}`，其中
     * `sanitizedSymbol` 会把不适合表达式解析器的字符归一成下划线。
     */
    public static String aliasFor(List<String> globalSymbols, int index) {
        if (globalSymbols == null || index < 0 || index >= globalSymbols.size()) {
            return null;
        }
        String raw = globalSymbols.get(index);
        return buildAlias(ALIAS_PREFIX, index, raw);
    }

    /**
     * 为本地属性生成安全别名。
     */
    public static String aliasForProperty(int index, String raw) {
        return buildAlias(PROPERTY_ALIAS_PREFIX, index, raw);
    }

    /**
     * 判断一个标识符是否长得像全局符号别名。
     */
    public static boolean isAlias(String text) {
        return text != null
                && text.length() > ALIAS_PREFIX.length()
                && text.startsWith(ALIAS_PREFIX)
                && Character.isDigit(text.charAt(ALIAS_PREFIX.length()));
    }

    /**
     * 从别名里反推出原始全局索引。
     *
     * <p>这里只依赖前缀后的数字部分，不依赖后面的展示名，因此改进展示名规则时
     * 不会破坏回编兼容性。
     */
    public static Integer tryParseAliasIndex(String alias) {
        if (!isAlias(alias)) {
            return null;
        }
        int cursor = ALIAS_PREFIX.length();
        while (cursor < alias.length() && Character.isDigit(alias.charAt(cursor))) {
            cursor++;
        }
        try {
            return Integer.parseInt(alias.substring(ALIAS_PREFIX.length(), cursor));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 把原始符号名归一成适合旧 pseudo 解析器的标识符。
     */
    private static String sanitize(String raw) {
        StringBuilder builder = new StringBuilder(raw.length());
        boolean lastUnderscore = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            boolean keep = Character.isLetterOrDigit(c) || c == '_';
            if (keep) {
                builder.append(c);
                lastUnderscore = false;
            } else if (!lastUnderscore) {
                builder.append('_');
                lastUnderscore = true;
            }
        }
        String sanitized = builder.toString().replaceAll("^_+|_+$", "");
        if (sanitized.isBlank()) {
            return "symbol";
        }
        return sanitized;
    }

    /**
     * 统一的别名构造逻辑。
     */
    private static String buildAlias(String prefix, int index, String raw) {
        if (raw == null || raw.isBlank()) {
            return prefix + index;
        }
        return prefix + index + "_" + sanitize(raw);
    }

    /**
     * 生成调试用的原始符号说明。
     */
    public static String debugLabel(List<String> globalSymbols, int index) {
        if (globalSymbols == null || index < 0 || index >= globalSymbols.size()) {
            return "global[" + index + "]";
        }
        return String.format(Locale.ROOT, "global[%d] = %s", index, globalSymbols.get(index));
    }
}
