package com.giga.nexas.transfer.bhe2bsdx.meka.followup.exe;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExePatchAudit {

    
    private List<String> appliedSites = new ArrayList<>();

    
    private List<String> alreadyPatchedSites = new ArrayList<>();

    
    private List<String> notes = new ArrayList<>();

    public void addAppliedSite(String note) {
        add(appliedSites, note);
    }

    public void addAlreadyPatchedSite(String note) {
        add(alreadyPatchedSites, note);
    }

    public void addNote(String note) {
        add(notes, note);
    }

    private void add(List<String> target, String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        target.add(note);
    }
}
