package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE map import plan。
 *
 * <p>这份计划只描述“源 MapGroup -> 源 .map -> 目标命名 -> 源资源引用”的完整关联。
 * preview 复制只是消费这份计划的物料化动作，不能反过来决定计划内容。</p>
 */
@Data
public class BheMapAppendPlan {

    private int sourceMapGroupTotalCount;
    private int sourceMigratableEntryCount;
    private List<BheMapEntryPlan> entries = new ArrayList<>();

    public int size() {
        return entries.size();
    }
}
