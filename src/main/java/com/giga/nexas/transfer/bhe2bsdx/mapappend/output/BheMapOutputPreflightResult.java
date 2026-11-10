package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class BheMapOutputPreflightResult {

    private int pendingFileCount;
    private int existingIdenticalCount;
    private int conflictCount;
    private List<BheMapOutputConflict> conflicts = new ArrayList<>();
    private List<BheMapPendingOutputFile> filesToWrite = new ArrayList<>();
    private List<BheMapOutputWriteAudit> writeAudits = new ArrayList<>();

    public BheMapOutputWriteAudit auditFor(Path path) {
        if (path == null) {
            return null;
        }
        Path normalizedPath = path.toAbsolutePath().normalize();
        for (BheMapOutputWriteAudit audit : writeAudits) {
            if (audit.getPath() != null && audit.getPath().toAbsolutePath().normalize().equals(normalizedPath)) {
                return audit;
            }
        }
        return null;
    }
}
