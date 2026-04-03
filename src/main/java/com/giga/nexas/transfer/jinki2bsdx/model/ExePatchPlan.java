package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于记录 exe 补丁输出结果的对象。
 */
@Data
public class ExePatchPlan {

    private boolean patched;
    private int requiredMekaCapacity = -1;
    private Path outputExePath;
    private List<String> notes = new ArrayList<>();
    private List<String> targetOffsets = new ArrayList<>();
}
