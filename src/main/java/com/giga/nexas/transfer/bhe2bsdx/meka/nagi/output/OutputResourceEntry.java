package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutputResourceEntry {

    
    private String relativePath;

    
    private String fileName;

    
    private String category;

    
    private String sourceDescription;

    
    private long size;

    
    private String sha256;

    
    private boolean overwritten;
}
