package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapgroup;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapGroupEntrySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE MapGroup 单条记录转 BSDX MapGroup 单条记录的转换器。
 *
 * <p>转换器只消费 import plan 中的 sourceMapGroupSnapshot，
 * 不读写文件，也不重新扫描 BHE MapGroup。</p>
 */
public class BheMapGroupEntryConverter {

    private static final int BHE_MAPGROUP_ITEM_INT_COUNT = 5;
    private static final int BSDX_MAPGROUP_ITEM_INT_COUNT = 4;

    public BheMapGroupEntryConversionResult convert(BheMapEntryPlan entryPlan) {
        if (entryPlan == null || entryPlan.getSourceMapGroupSnapshot() == null) {
            throw new IllegalArgumentException("缺少 BHE MapGroup entry plan");
        }

        BheMapGroupEntrySnapshot source = entryPlan.getSourceMapGroupSnapshot();
        BheMapGroupEntryConversionResult result = new BheMapGroupEntryConversionResult();
        MapGroupGrp.MapGroup target = new MapGroupGrp.MapGroup();
        target.setExistFlag(source.getExistFlag());
        target.setGroupName(source.getGroupName());
        target.setGroupCodeName(source.getGroupCodeName());
        target.setGroupResourceName(entryPlan.getTargetGroupResourceName());
        target.setInt1(source.getInt1());
        target.setItems(convertItems(entryPlan, source.getItems(), result));
        target.setArray1(copyIntArrays(source.getPairArray1()));
        target.setArray2(copyIntArrays(source.getArray2()));
        target.setArray3(copyIntArrays(source.getArray3()));
        result.setTargetGroup(target);
        return result;
    }

    private List<MapGroupGrp.Item> convertItems(
            BheMapEntryPlan entryPlan,
            List<List<Integer>> sourceItems,
            BheMapGroupEntryConversionResult result
    ) {
        List<MapGroupGrp.Item> targetItems = new ArrayList<>();
        if (sourceItems == null) {
            return targetItems;
        }

        for (int itemIndex = 0; itemIndex < sourceItems.size(); itemIndex++) {
            List<Integer> sourceItem = sourceItems.get(itemIndex);
            requireBheItemShape(itemIndex, sourceItem);
            MapGroupGrp.Item targetItem = new MapGroupGrp.Item();
            targetItem.setInt1(valueAt(sourceItem, 0));
            targetItem.setInt2(valueAt(sourceItem, 1));
            targetItem.setInt3(valueAt(sourceItem, 2));
            targetItem.setInt4(valueAt(sourceItem, 3));
            targetItems.add(targetItem);

            BheMapGroupItemDowngrade downgrade = new BheMapGroupItemDowngrade();
            downgrade.setSourceMapFileName(entryPlan.getSourceMapFileName());
            downgrade.setItemIndex(itemIndex);
            downgrade.setDroppedInt5(valueAt(sourceItem, 4));
            result.getItemDowngrades().add(downgrade);
        }
        return targetItems;
    }

    private void requireBheItemShape(int itemIndex, List<Integer> sourceItem) {
        if (sourceItem == null || sourceItem.size() != BHE_MAPGROUP_ITEM_INT_COUNT) {
            throw new IllegalArgumentException("BHE MapGroup item 必须是 5 int: itemIndex=" + itemIndex);
        }
    }

    private List<MapGroupGrp.IntArray> copyIntArrays(List<List<Integer>> sourceArrays) {
        List<MapGroupGrp.IntArray> targetArrays = new ArrayList<>();
        if (sourceArrays == null) {
            return targetArrays;
        }

        for (List<Integer> sourceArray : sourceArrays) {
            MapGroupGrp.IntArray targetArray = new MapGroupGrp.IntArray();
            if (sourceArray != null) {
                targetArray.getValues().addAll(sourceArray);
            }
            targetArrays.add(targetArray);
        }
        return targetArrays;
    }

    private int valueAt(List<Integer> values, int index) {
        Integer value = values.get(index);
        return value == null ? 0 : value;
    }
}
