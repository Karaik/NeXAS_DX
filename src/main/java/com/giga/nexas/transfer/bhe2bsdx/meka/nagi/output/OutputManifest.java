package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.output;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OutputManifest {

    
    private String outputRoot;

    
    private List<OutputResourceEntry> entries = new ArrayList<>();

    
    private List<String> notes = new ArrayList<>();

    public void addEntry(OutputResourceEntry entry) {
        if (entry == null) {
            return;
        }
        entries.add(entry);
    }

    public void addNote(String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        notes.add(note);
    }
}
