package com.giga.nexas.transfer.bhe2bsdx.meka.katou.output;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ResourceOverwriteAudit {

    
    private Map<String, String> firstSourceByRelativePath = new LinkedHashMap<>();

    
    private List<String> overwriteNotes = new ArrayList<>();

    public boolean recordOutput(String relativePath, String sourceDescription) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String source = sourceDescription == null || sourceDescription.isBlank()
                ? "unknown"
                : sourceDescription;
        String existing = firstSourceByRelativePath.putIfAbsent(relativePath, source);
        if (existing == null) {
            return false;
        }
        overwriteNotes.add(relativePath + " overwritten: " + existing + " -> " + source);
        firstSourceByRelativePath.put(relativePath, source);
        return true;
    }
}
