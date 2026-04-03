package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录 step10 的容量汇总与 exe patch 结果。
 */
@Data
public class ExePatchPlan {

    private boolean patched;

    private int requiredMekaCapacity = -1;
    private int requiredWazaCapacity = -1;
    private int requiredSpriteCapacity = -1;
    private int requiredBatVoiceCapacity = -1;
    private int requiredSeCapacity = -1;

    private Path sourceExePath;
    private Path outputExePath;

    private List<String> notes = new ArrayList<>();
    private List<String> targetOffsets = new ArrayList<>();
}
