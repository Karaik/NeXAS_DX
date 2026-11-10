package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import lombok.Data;

import java.nio.file.Path;

@Data
public class BheMapOutputWriteAudit {

    private boolean written;
    private boolean existingIdentical;
    private Path path;
}
