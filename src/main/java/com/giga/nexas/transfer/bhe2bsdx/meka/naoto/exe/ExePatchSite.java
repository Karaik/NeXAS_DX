package com.giga.nexas.transfer.bhe2bsdx.meka.naoto.exe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExePatchSite {

    
    private int offset;

    
    private byte[] expectedBytes;

    
    private byte[] targetBytes;

    
    private String label;
}
