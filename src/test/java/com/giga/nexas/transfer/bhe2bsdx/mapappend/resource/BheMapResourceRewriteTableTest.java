package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BheMapResourceRewriteTableTest {

    @Test
    void referenceTypeAndSourceTextFormStableRewriteKey() {
        BheMapResourceReference foreground = reference(
                BheMapReferenceType.FOREGROUND,
                "fg/shared.spm",
                "fg/bhe_shared.spm"
        );
        BheMapResourceReference resourceSlot = reference(
                BheMapReferenceType.RESOURCE_SLOT,
                "slot/shared.spm",
                "slot/bhe_shared.spm"
        );
        BheMapResourceRewriteTable table = new BheMapResourceRewriteTable();

        table.put(foreground);
        table.put(resourceSlot);

        assertEquals(2, table.size());
        assertSame(foreground, table.find(BheMapReferenceType.FOREGROUND, "fg/shared.spm"));
        assertSame(resourceSlot, table.find(BheMapReferenceType.RESOURCE_SLOT, "slot/shared.spm"));
        assertNull(table.find(BheMapReferenceType.FOREGROUND, "slot/shared.spm"));
        assertNull(table.find(BheMapReferenceType.RESOURCE_SLOT, "fg/shared.spm"));
    }

    private BheMapResourceReference reference(BheMapReferenceType type, String sourceText, String targetText) {
        BheMapResourceReference reference = new BheMapResourceReference();
        reference.setType(type);
        reference.setSourceText(sourceText);
        reference.setTargetText(targetText);
        reference.setStatus(BheMapReferenceStatus.FOUND_DEFERRED);
        return reference;
    }
}
