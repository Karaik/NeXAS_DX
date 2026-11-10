package com.giga.nexas.dto.bsdx.bin.consts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OperandDocRegistry {

    private static final Map<String, OperandDocEntry> DOCS_BY_NAME = loadDocsByName();

    private OperandDocRegistry() {
    }

    public static OperandDocEntry findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return DOCS_BY_NAME.get(name.trim().toUpperCase(Locale.ROOT));
    }

    private static Map<String, OperandDocEntry> loadDocsByName() {
        OperandDocSource source = Operand.class.getAnnotation(OperandDocSource.class);
        if (source == null) {
            return Map.of();
        }

        List<String[]> rows = readCsvRows(source.classpath());
        if (rows.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Operand> operandByCode = new HashMap<>();
        for (Operand operand : Operand.values()) {
            operandByCode.put(operand.code, operand);
        }

        Map<String, OperandDocEntry> result = new LinkedHashMap<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (row.length < 4) {
                continue;
            }
            String codeText = row[0].trim();
            String description = row[3].trim();
            if (codeText.isEmpty() || description.isEmpty()) {
                continue;
            }

            int code;
            try {
                code = Integer.parseInt(codeText);
            } catch (NumberFormatException ex) {
                continue;
            }

            int lookupCode = code;
            if (code >= 1104) {
                lookupCode = code - 694;
            }

            Operand operand = operandByCode.get(lookupCode);
            if (operand == null) {
                continue;
            }

            String hex = row.length > 1 ? row[1].trim() : Integer.toHexString(code).toUpperCase(Locale.ROOT);
            String paramCount = row.length > 4 ? row[4].trim() : "";
            List<String> params = new ArrayList<>();
            for (int col = 5; col < row.length; col++) {
                params.add(row[col] == null ? "" : row[col].trim());
            }

            OperandDocEntry entry = new OperandDocEntry(
                    code,
                    hex,
                    operand.name(),
                    description,
                    paramCount,
                    Collections.unmodifiableList(params)
            );
            result.put(operand.name().toUpperCase(Locale.ROOT), entry);
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<String[]> readCsvRows(String classpathLocation) {
        InputStream stream = OperandDocRegistry.class.getResourceAsStream(classpathLocation);
        if (stream == null) {
            Path fallback = Path.of("src", "main", "resources", classpathLocation.replaceFirst("^/+", ""));
            if (Files.exists(fallback)) {
                try {
                    return parseCsv(Files.newInputStream(fallback));
                } catch (IOException ex) {
                    return List.of();
                }
            }
            return List.of();
        }
        try {
            return parseCsv(stream);
        } catch (IOException ex) {
            return List.of();
        }
    }

    private static List<String[]> parseCsv(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String[]> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(splitCsvLine(line));
            }
            return rows;
        }
    }

    private static String[] splitCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (c == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        cells.add(current.toString());
        return cells.toArray(String[]::new);
    }
}
