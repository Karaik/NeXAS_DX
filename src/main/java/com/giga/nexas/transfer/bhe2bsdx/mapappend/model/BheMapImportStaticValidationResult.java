package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BheMapImportStaticValidationResult {

    private boolean validForStaticSubset;
    private boolean canContinueStaticPipeline;
    private int fatalProblemCount;
    private int warningCount;
    private List<BheMapImportStaticValidationProblem> validationProblems = new ArrayList<>();

    public boolean hasProblem(String key) {
        return validationProblems.stream()
                .anyMatch(problem -> key != null && key.equals(problem.getKey()));
    }
}
