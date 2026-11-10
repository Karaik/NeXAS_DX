package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BHE .map 内部资源引用的重写表。
 *
 * <p>key 使用 referenceType + sourceText，避免只按文件名匹配导致不同目录资源串线。</p>
 */
public class BheMapResourceRewriteTable {

    private final Map<Key, BheMapResourceReference> references = new LinkedHashMap<>();

    public static BheMapResourceRewriteTable fromEntryPlan(BheMapEntryPlan entryPlan) {
        BheMapResourceRewriteTable table = new BheMapResourceRewriteTable();
        if (entryPlan == null
                || entryPlan.getResourceReferences() == null
                || entryPlan.getResourceReferences().getReferences() == null) {
            return table;
        }
        for (BheMapResourceReference reference : entryPlan.getResourceReferences().getReferences()) {
            table.put(reference);
        }
        return table;
    }

    public void put(BheMapResourceReference reference) {
        if (reference == null || reference.getType() == null || isBlank(reference.getSourceText())) {
            return;
        }
        references.put(new Key(reference.getType(), normalize(reference.getSourceText())), reference);
    }

    public BheMapResourceReference find(BheMapReferenceType type, String sourceText) {
        if (type == null || isBlank(sourceText)) {
            return null;
        }
        return references.get(new Key(type, normalize(sourceText)));
    }

    public int size() {
        return references.size();
    }

    private String normalize(String sourceText) {
        return sourceText.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Key(BheMapReferenceType type, String sourceText) {
    }
}
