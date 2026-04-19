package com.giga.nexas.transfer.bhe2bsdx.meka.katou.menu;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Data
public class MenuOverrideAudit {

    
    private MenuSlotMapping slotMapping;

    
    private List<String> slotMappingNotes = new ArrayList<>();

    
    private List<String> datPatchNotes = new ArrayList<>();

    
    private List<String> spmPatchNotes = new ArrayList<>();

    
    private List<Path> writtenFiles = new ArrayList<>();

    
    private List<Path> copiedImages = new ArrayList<>();

    
    private List<String> missingImages = new ArrayList<>();

    public void addSlotMappingNote(String note) {
        addNote(slotMappingNotes, note);
    }

    public void addDatPatchNote(String note) {
        addNote(datPatchNotes, note);
    }

    public void addSpmPatchNote(String note) {
        addNote(spmPatchNotes, note);
    }

    public void addWrittenFile(Path path) {
        addPath(writtenFiles, path);
    }

    public void addCopiedImage(Path path) {
        addPath(copiedImages, path);
    }

    public void addMissingImage(String imageName) {
        addNote(missingImages, imageName);
    }

    private void addNote(List<String> notes, String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        notes.add(note);
    }

    private void addPath(List<Path> paths, Path path) {
        if (path == null) {
            return;
        }
        paths.add(path);
    }
}
