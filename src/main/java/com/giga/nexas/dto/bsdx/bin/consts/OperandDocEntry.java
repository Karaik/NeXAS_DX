package com.giga.nexas.dto.bsdx.bin.consts;

import java.util.List;

public record OperandDocEntry(
        int code,
        String hex,
        String enumName,
        String description,
        String paramCount,
        List<String> params
) {

    public String toTooltipText() {
        StringBuilder out = new StringBuilder();
        out.append(enumName != null ? enumName : "UNKNOWN");
        out.append(" (").append(code).append(" / 0x").append(hex).append(")");
        if (description != null && !description.isBlank()) {
            out.append("\n").append(description);
        }
        if (paramCount != null && !paramCount.isBlank()) {
            out.append("\n参数数量: ").append(paramCount);
        }
        if (params != null) {
            String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"};
            for (int i = 0; i < params.size() && i < names.length; i++) {
                String value = params.get(i);
                if (value != null && !value.isBlank()) {
                    out.append("\n").append(names[i]).append(": ").append(value);
                }
            }
        }
        return out.toString();
    }
}
