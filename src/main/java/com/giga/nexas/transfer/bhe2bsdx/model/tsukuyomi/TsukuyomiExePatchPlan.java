package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录 exe patch 计划与执行结果。
 */
@Data
public class TsukuyomiExePatchPlan {

    private boolean patched;

    private int requiredMekaCapacity = -1;
    private int requiredWazaCapacity = -1;
    private int requiredSpriteCapacity = -1;
    private int requiredBatVoiceCapacity = -1;
    private int requiredSeCapacity = -1;
    private int requiredSelectMekaMenuRows = -1;

    private Path sourceExePath;
    private Path outputExePath;

    private List<String> notes = new ArrayList<>();
    private List<String> targetOffsets = new ArrayList<>();
}
