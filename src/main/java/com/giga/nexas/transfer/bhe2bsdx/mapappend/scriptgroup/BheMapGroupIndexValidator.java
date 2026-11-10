package com.giga.nexas.transfer.bhe2bsdx.mapappend.scriptgroup;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;

/**
 * BHE map script group compatibility 验证器。
 *
 * <p>BSDX map 的 ScriptEntry 没有 groupIndex 字段，因此这里固定 BHE groupIndex 与
 * 外层脚本组号之间的真实数据关系，供 MapData conversion 规则消费。</p>
 */
public class BheMapGroupIndexValidator {

    private static final int BHE_SCRIPT_GROUP_COUNT = 9;

    private final BheBinService bheBinService;

    public BheMapGroupIndexValidator() {
        this(new BheBinService());
    }

    public BheMapGroupIndexValidator(BheBinService bheBinService) {
        this.bheBinService = bheBinService == null ? new BheBinService() : bheBinService;
    }

    public BheMapGroupIndexValidationResult validate(BheMapAppendPlan plan, String charset) {
        BheMapGroupIndexValidationResult result = new BheMapGroupIndexValidationResult();
        if (plan == null) {
            return result;
        }

        for (BheMapEntryPlan entry : plan.getEntries()) {
            validateOne(entry, charset, result);
        }
        return result;
    }

    private void validateOne(
            BheMapEntryPlan entry,
            String charset,
            BheMapGroupIndexValidationResult result
    ) {
        if (entry == null || entry.getSourceMapPath() == null) {
            addIssue(result, entry, null, null, null, "缺少 sourceMapPath，无法验证 groupIndex");
            return;
        }

        try {
            MapData mapData = (MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            result.setValidatedMapCount(result.getValidatedMapCount() + 1);
            validateScriptGroups(entry, mapData, result);
        } catch (Exception e) {
            addIssue(result, entry, null, null, null, "解析 BHE .map 失败: " + e.getMessage());
        }
    }

    private void validateScriptGroups(
            BheMapEntryPlan entry,
            MapData mapData,
            BheMapGroupIndexValidationResult result
    ) {
        if (mapData == null || mapData.getScriptEntryGroupBlocks() == null) {
            addIssue(result, entry, null, null, null, "缺少脚本组数据");
            return;
        }

        for (int groupNum = 0; groupNum < BHE_SCRIPT_GROUP_COUNT; groupNum++) {
            if (groupNum >= mapData.getScriptEntryGroupBlocks().size()) {
                addIssue(result, entry, groupNum, null, null, "脚本组数量不足");
                continue;
            }
            MapData.ScriptEntryGroupBlock group = mapData.getScriptEntryGroupBlocks().get(groupNum);
            if (group == null || group.getEntries() == null) {
                continue;
            }

            for (MapData.ScriptEntry scriptEntry : group.getEntries()) {
                result.setInspectedScriptEntryCount(result.getInspectedScriptEntryCount() + 1);
                Integer groupIndex = scriptEntry == null ? null : scriptEntry.getGroupIndex();
                Integer typeId = scriptEntry == null ? null : scriptEntry.getTypeId();
                if (groupIndex == null || groupIndex != groupNum) {
                    addIssue(result, entry, groupNum, groupIndex, typeId, "groupIndex 与外层 groupNum 不一致");
                }
            }
        }
    }

    private void addIssue(
            BheMapGroupIndexValidationResult result,
            BheMapEntryPlan entry,
            Integer groupNum,
            Integer groupIndex,
            Integer typeId,
            String message
    ) {
        BheMapGroupIndexIssue issue = new BheMapGroupIndexIssue();
        issue.setSourceMapFileName(entry == null ? null : entry.getSourceMapFileName());
        issue.setGroupNum(groupNum);
        issue.setGroupIndex(groupIndex);
        issue.setTypeId(typeId);
        issue.setMessage(message);
        result.getIssues().add(issue);
    }
}
