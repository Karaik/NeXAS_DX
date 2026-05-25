package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

public class BheMapOutputConflictException extends RuntimeException {

    private final BheMapOutputConflict conflict;

    public BheMapOutputConflictException(BheMapOutputConflict conflict) {
        super(conflict == null ? "BHE map output conflict" : conflict.getReason());
        this.conflict = conflict;
    }

    public BheMapOutputConflict getConflict() {
        return conflict;
    }
}
