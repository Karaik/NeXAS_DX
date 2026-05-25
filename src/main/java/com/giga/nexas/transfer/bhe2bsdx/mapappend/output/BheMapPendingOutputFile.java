package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import lombok.Data;

import java.nio.file.Path;

@Data
public class BheMapPendingOutputFile {

    private Path path;
    private byte[] bytes;
    private BheMapOutputConflictType conflictType;
    private BheMapOutputWritePolicy writePolicy = BheMapOutputWritePolicy.CREATE_OR_IDENTICAL;
    private byte[] expectedExistingBytes;
    private String logicalName;
    private String reason;
}
