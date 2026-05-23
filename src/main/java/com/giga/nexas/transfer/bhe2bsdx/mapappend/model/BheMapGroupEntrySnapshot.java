package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE MapGroup 单条记录的原始结构快照。
 *
 * <p>Step1 不改写 MapGroup，但必须把后续追加可能用到的 item/pair/int 数组保留下来，
 * 避免后续阶段为了补信息重新解析 MapGroup.grp。</p>
 */
@Data
public class BheMapGroupEntrySnapshot {

    private int existFlag;
    private String groupName;
    private String groupCodeName;
    private String groupResourceName;
    private int int1;
    private List<List<Integer>> items = new ArrayList<>();
    private List<List<Integer>> pairArray1 = new ArrayList<>();
    private List<List<Integer>> array2 = new ArrayList<>();
    private List<List<Integer>> array3 = new ArrayList<>();
}
