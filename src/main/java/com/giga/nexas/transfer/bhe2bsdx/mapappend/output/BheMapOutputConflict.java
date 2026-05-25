package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import lombok.Data;

import java.nio.file.Path;

@Data
public class BheMapOutputConflict {

    private BheMapOutputConflictType type;
    private Path path;
    private String reason;
}
