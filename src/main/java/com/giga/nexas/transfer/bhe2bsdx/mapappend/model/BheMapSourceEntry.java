package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

/**
 * BHE MapGroup.grp 中的一条有效地图记录。
 *
 * <p>sourceMapIndex 保留原始 MapGroup 下标，因为 HellConfig.dat 等外部表会用这个下标作为 mapId。
 * import plan construction 只消费 groupResourceName 推导源/目标资源名，写入流程继续消费同一个对象。</p>
 */
@Data
public class BheMapSourceEntry {

    private int sourceMapIndex;
    private String groupName;
    private String groupCodeName;
    private String groupResourceName;
    private int mapGroupInt1;
    private int itemCount;
    private int pairArray1Count;
    private int array2Count;
    private int array3Count;
    private BheMapGroupEntrySnapshot mapGroupSnapshot = new BheMapGroupEntrySnapshot();
}
