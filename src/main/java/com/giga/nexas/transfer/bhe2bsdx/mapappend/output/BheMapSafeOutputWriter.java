package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BheMapSafeOutputWriter {

    public BheMapOutputWriteAudit write(Path targetPath, byte[] newBytes, BheMapOutputConflictType conflictType)
            throws IOException {
        BheMapPendingOutputFile pending = new BheMapPendingOutputFile();
        pending.setPath(targetPath);
        pending.setBytes(newBytes);
        pending.setConflictType(conflictType);
        BheMapOutputPreflightResult preflightResult = preflight(List.of(pending));
        if (preflightResult.getConflictCount() > 0) {
            throw new BheMapOutputConflictException(preflightResult.getConflicts().get(0));
        }
        commit(preflightResult);
        return preflightResult.getWriteAudits().get(0);
    }

    public BheMapOutputPreflightResult preflight(List<BheMapPendingOutputFile> pendingFiles) throws IOException {
        BheMapOutputPreflightResult result = new BheMapOutputPreflightResult();
        if (pendingFiles == null || pendingFiles.isEmpty()) {
            return result;
        }
        result.setPendingFileCount(pendingFiles.size());

        Map<Path, BheMapPendingOutputFile> uniqueFiles = new LinkedHashMap<>();
        for (BheMapPendingOutputFile pendingFile : pendingFiles) {
            validate(pendingFile);
            Path normalizedPath = pendingFile.getPath().toAbsolutePath().normalize();
            pendingFile.setPath(normalizedPath);
            BheMapPendingOutputFile existingPending = uniqueFiles.get(normalizedPath);
            if (existingPending != null) {
                if (!Arrays.equals(existingPending.getBytes(), pendingFile.getBytes())) {
                    result.getConflicts().add(conflict(
                            pendingFile.getConflictType(),
                            normalizedPath,
                            "multiple pending output files target the same path with different content: " + normalizedPath
                    ));
                }
                continue;
            }
            uniqueFiles.put(normalizedPath, pendingFile);
        }

        if (!result.getConflicts().isEmpty()) {
            result.setConflictCount(result.getConflicts().size());
            return result;
        }

        for (BheMapPendingOutputFile pendingFile : uniqueFiles.values()) {
            BheMapOutputWriteAudit audit = new BheMapOutputWriteAudit();
            audit.setPath(pendingFile.getPath());
            if (Files.exists(pendingFile.getPath())) {
                byte[] existingBytes = Files.readAllBytes(pendingFile.getPath());
                if (Arrays.equals(existingBytes, pendingFile.getBytes())) {
                    audit.setExistingIdentical(true);
                    result.setExistingIdenticalCount(result.getExistingIdenticalCount() + 1);
                    result.getWriteAudits().add(audit);
                    continue;
                }
                if (pendingFile.getWritePolicy() == BheMapOutputWritePolicy.REPLACE_IF_EXISTING_MATCHES_EXPECTED
                        && pendingFile.getExpectedExistingBytes() != null
                        && Arrays.equals(existingBytes, pendingFile.getExpectedExistingBytes())) {
                    audit.setWritten(true);
                    result.getFilesToWrite().add(pendingFile);
                    result.getWriteAudits().add(audit);
                    continue;
                }
                result.getConflicts().add(conflict(
                        pendingFile.getConflictType(),
                        pendingFile.getPath(),
                        pendingFile.getConflictType()
                                + ": existing output file has different content: "
                                + pendingFile.getPath()
                ));
                continue;
            }
            audit.setWritten(true);
            result.getFilesToWrite().add(pendingFile);
            result.getWriteAudits().add(audit);
        }

        result.setConflictCount(result.getConflicts().size());
        return result;
    }

    public void commit(BheMapOutputPreflightResult preflightResult) throws IOException {
        if (preflightResult == null) {
            throw new IllegalArgumentException("preflightResult must not be null");
        }
        if (preflightResult.getConflictCount() > 0) {
            throw new BheMapOutputConflictException(preflightResult.getConflicts().get(0));
        }
        for (BheMapPendingOutputFile file : preflightResult.getFilesToWrite()) {
            Files.createDirectories(file.getPath().getParent());
            Files.write(file.getPath(), file.getBytes());
        }
    }

    private void validate(BheMapPendingOutputFile pendingFile) {
        if (pendingFile == null) {
            throw new IllegalArgumentException("pendingFile must not be null");
        }
        if (pendingFile.getPath() == null) {
            throw new IllegalArgumentException("pending output path must not be null");
        }
        if (pendingFile.getBytes() == null) {
            throw new IllegalArgumentException("pending output bytes must not be null");
        }
        if (pendingFile.getConflictType() == null) {
            throw new IllegalArgumentException("pending output conflictType must not be null");
        }
        if (pendingFile.getWritePolicy() == null) {
            pendingFile.setWritePolicy(BheMapOutputWritePolicy.CREATE_OR_IDENTICAL);
        }
        if (pendingFile.getWritePolicy() == BheMapOutputWritePolicy.REPLACE_IF_EXISTING_MATCHES_EXPECTED
                && pendingFile.getExpectedExistingBytes() == null) {
            throw new IllegalArgumentException("expectedExistingBytes must be provided for replacement writes");
        }
    }

    private BheMapOutputConflict conflict(BheMapOutputConflictType type, Path path, String reason) {
        BheMapOutputConflict conflict = new BheMapOutputConflict();
        conflict.setType(type);
        conflict.setPath(path);
        conflict.setReason(reason);
        return conflict;
    }

    private BheMapOutputWriteAudit writeLegacy(Path targetPath, byte[] newBytes, BheMapOutputConflictType conflictType)
            throws IOException {
        if (targetPath == null) {
            throw new IllegalArgumentException("targetPath must not be null");
        }
        if (newBytes == null) {
            throw new IllegalArgumentException("newBytes must not be null");
        }
        if (conflictType == null) {
            throw new IllegalArgumentException("conflictType must not be null");
        }

        Path normalizedTargetPath = targetPath.toAbsolutePath().normalize();
        BheMapOutputWriteAudit audit = new BheMapOutputWriteAudit();
        audit.setPath(normalizedTargetPath);
        if (Files.exists(normalizedTargetPath)) {
            byte[] existingBytes = Files.readAllBytes(normalizedTargetPath);
            if (Arrays.equals(existingBytes, newBytes)) {
                audit.setExistingIdentical(true);
                return audit;
            }
            BheMapOutputConflict conflict = new BheMapOutputConflict();
            conflict.setType(conflictType);
            conflict.setPath(normalizedTargetPath);
            conflict.setReason(conflictType + ": existing output file has different content: " + normalizedTargetPath);
            throw new BheMapOutputConflictException(conflict);
        }

        Files.createDirectories(normalizedTargetPath.getParent());
        Files.write(normalizedTargetPath, newBytes);
        audit.setWritten(true);
        return audit;
    }
}
