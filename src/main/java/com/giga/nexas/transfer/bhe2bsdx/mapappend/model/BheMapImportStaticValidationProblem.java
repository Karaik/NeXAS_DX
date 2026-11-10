package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

@Data
public class BheMapImportStaticValidationProblem {

    private BheMapImportStaticValidationProblemSeverity severity;
    private String key;
    private String message;
}
