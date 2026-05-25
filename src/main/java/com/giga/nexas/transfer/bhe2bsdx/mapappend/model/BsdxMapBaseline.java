package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * BSDX 侧 MapGroup.grp 的轻量基线视图。
 *
 * <p>这里只保存追加前已经存在的 groupResourceName，用于阻止 BHE 目标名撞上 BSDX 原生地图。
 * 不把完整 BSDX MapGroup DTO 暴露给 preview 层，避免 import plan construction 提前承担 grp 改写职责。</p>
 */
@Data
public class BsdxMapBaseline {

    private Set<String> existingGroupResourceNames = new LinkedHashSet<>();
}
