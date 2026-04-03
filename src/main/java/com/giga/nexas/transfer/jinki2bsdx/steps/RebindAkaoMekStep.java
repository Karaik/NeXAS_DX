package com.giga.nexas.transfer.jinki2bsdx.steps;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.MekRebindContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责重建并回写 Akao.mek。
 *
 * <p>当前实现重点是把 Mek 拆成多个分片入口分别调用重建方法。</p>
 *
 * <p>第一轮真正做语义修改的只有 {@code MekBasicInfo}：</p>
 * <ul>
 *     <li>{@code wazFileSequence}</li>
 *     <li>{@code spmFileSequence}</li>
 * </ul>
 *
 * <p>其他分片先显式拆出重建方法，当前按“原语义复制/挂接”处理。</p>
 */
public class RebindAkaoMekStep {

    public Mek rebindAkaoMek(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            GrpAppendPlan grpAppendPlan
    ) {
        if (request == null || jinkiPackage == null || grpAppendPlan == null) {
            return null;
        }

        Mek sourceMek = jinkiPackage.getAkaoMek();
        if (sourceMek == null) {
            return null;
        }

        // Step 7-1: 先根据源 Mek 和 grp 追加结果构建重建上下文。
        MekRebindContext context = buildContext(sourceMek, grpAppendPlan);

        // Step 7-2: 创建目标 Mek 外壳，只保留最顶层公共信息。
        Mek targetMek = createTargetMekShell(context);

        // Step 7-3: 按 Mek.java 的分片顺序，逐个重建头部信息。
        targetMek.setMekHead(rebuildMekHead(context));

        // Step 7-4: 重建区块大小信息。
        targetMek.setMekBlocks(rebuildMekBlocks(context));

        // Step 7-5: 重建并真正回写 MekBasicInfo 的顶层外部索引。
        targetMek.setMekBasicInfo(rebuildMekBasicInfo(context));

        // Step 7-6: 重建 pair block。
        targetMek.setMekPairBlock(rebuildMekPairBlock(context));

        // Step 7-7: 重建武装表。
        // 当前只复制结构，不改 MekWeaponInfo.wazSequence。
        targetMek.setMekWeaponInfoMap(rebuildWeaponInfoMap(context));

        // Step 7-8: 重建 AI 分片。
        // 当前先保持原语义，不改 CPU 事件内部内容。
        targetMek.setMekAiInfoList(rebuildAiInfoList(context));

        // Step 7-9: 重建 Voice 分片。
        // 当前先保持原语义，不改 table 里的 groupId。
        targetMek.setMekVoiceInfo(rebuildMekVoiceInfo(context));

        // Step 7-10: 重建 Material 分片。
        // 当前先按原结构复制，不改 sprite/se/voice 组内引用语义。
        targetMek.setMekMaterialBlock(rebuildMekMaterialBlock(context));

        // Step 7-11: 对当前已经明确会改的字段做结果校验。
        validateRebindResult(targetMek, context);
        return targetMek;
    }

    private MekRebindContext buildContext(Mek sourceMek, GrpAppendPlan grpAppendPlan) {
        MekRebindContext context = new MekRebindContext();
        context.setSourceMek(sourceMek);
        context.setSourceFileName(sourceMek.getFileName());
        context.setSourceWazFileSequence(
                sourceMek.getMekBasicInfo() != null ? sourceMek.getMekBasicInfo().getWazFileSequence() : null
        );
        context.setSourceSpmFileSequence(
                sourceMek.getMekBasicInfo() != null ? sourceMek.getMekBasicInfo().getSpmFileSequence() : null
        );
        context.setTargetWazaGroupIndex(grpAppendPlan.getWazaGroupIndex());
        context.setTargetSpriteGroupIndex(grpAppendPlan.getSpriteGroupIndex());
        context.setTargetBatVoiceGroupIndex(grpAppendPlan.getBatVoiceGroupIndex());
        return context;
    }

    private Mek createTargetMekShell(MekRebindContext context) {
        Mek targetMek = new Mek();
        if (context.getSourceMek() != null) {
            targetMek.setFileName(context.getSourceMek().getFileName());
            targetMek.setExtensionName(context.getSourceMek().getExtensionName());
        }
        return targetMek;
    }

    private Mek.MekHead rebuildMekHead(MekRebindContext context) {
        Mek.MekHead source = context.getSourceMek().getMekHead();
        if (source == null) {
            return null;
        }

        Mek.MekHead target = new Mek.MekHead();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    private Mek.MekBlocks rebuildMekBlocks(MekRebindContext context) {
        Mek.MekBlocks source = context.getSourceMek().getMekBlocks();
        if (source == null) {
            return null;
        }

        Mek.MekBlocks target = new Mek.MekBlocks();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    private Mek.MekBasicInfo rebuildMekBasicInfo(MekRebindContext context) {
        Mek.MekBasicInfo source = context.getSourceMek().getMekBasicInfo();
        if (source == null) {
            return null;
        }

        Mek.MekBasicInfo target = new Mek.MekBasicInfo();
        BeanUtil.copyProperties(source, target);

        // 当前 step7 第一轮只明确重绑两个顶层外部路由字段。
        target.setWazFileSequence(context.getTargetWazaGroupIndex());
        target.setSpmFileSequence(context.getTargetSpriteGroupIndex());
        return target;
    }

    private Mek.MekPairBlock rebuildMekPairBlock(MekRebindContext context) {
        Mek.MekPairBlock source = context.getSourceMek().getMekPairBlock();
        if (source == null) {
            return null;
        }

        Mek.MekPairBlock target = new Mek.MekPairBlock();
        List<Mek.MekPairBlock.Pair> pairs = new ArrayList<>();
        if (source.getUnkPair() != null) {
            for (Mek.MekPairBlock.Pair pair : source.getUnkPair()) {
                if (pair == null) {
                    pairs.add(null);
                    continue;
                }
                Mek.MekPairBlock.Pair copied = new Mek.MekPairBlock.Pair();
                BeanUtil.copyProperties(pair, copied);
                pairs.add(copied);
            }
        }
        target.setUnkPair(pairs);
        return target;
    }

    private Map<Integer, Mek.MekWeaponInfo> rebuildWeaponInfoMap(MekRebindContext context) {
        Map<Integer, Mek.MekWeaponInfo> source = context.getSourceMek().getMekWeaponInfoMap();
        Map<Integer, Mek.MekWeaponInfo> target = new LinkedHashMap<>();
        if (source == null) {
            return target;
        }

        // 当前这里只做结构级复制。
        // MekWeaponInfo.wazSequence 仍然解释为 Akao.waz 内部 skill 索引，因此不在 step7 修改。
        for (Map.Entry<Integer, Mek.MekWeaponInfo> entry : source.entrySet()) {
            Mek.MekWeaponInfo copied = new Mek.MekWeaponInfo();
            BeanUtil.copyProperties(entry.getValue(), copied);
            target.put(entry.getKey(), copied);
        }
        return target;
    }

    private List<Mek.MekAiInfo> rebuildAiInfoList(MekRebindContext context) {
        List<Mek.MekAiInfo> source = context.getSourceMek().getMekAiInfoList();
        List<Mek.MekAiInfo> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        // 当前先显式拆出 AI 重建入口，但不更改 CPU 事件语义。
        for (Mek.MekAiInfo aiInfo : source) {
            if (aiInfo == null) {
                target.add(null);
                continue;
            }
            Mek.MekAiInfo copied = new Mek.MekAiInfo();
            BeanUtil.copyProperties(aiInfo, copied);
            copied.setCpuEventList(aiInfo.getCpuEventList() == null ? new ArrayList<>() : new ArrayList<>(aiInfo.getCpuEventList()));
            target.add(copied);
        }
        return target;
    }

    private Mek.MekVoiceInfo rebuildMekVoiceInfo(MekRebindContext context) {
        Mek.MekVoiceInfo source = context.getSourceMek().getMekVoiceInfo();
        if (source == null) {
            return null;
        }

        Mek.MekVoiceInfo target = new Mek.MekVoiceInfo();
        target.setVersion(source.getVersion());
        target.builtinEmotionCount = source.builtinEmotionCount;

        // emotions 当前做结构级深拷贝。
        List<Mek.MekVoiceInfo.Emotion> emotions = new ArrayList<>();
        for (Mek.MekVoiceInfo.Emotion emotion : source.getEmotions()) {
            Mek.MekVoiceInfo.Emotion copied = new Mek.MekVoiceInfo.Emotion();
            BeanUtil.copyProperties(emotion, copied);
            emotions.add(copied);
        }
        target.setEmotions(emotions);

        // voiceSlots 当前做结构级深拷贝。
        List<Mek.MekVoiceInfo.VoiceSlot> voiceSlots = new ArrayList<>();
        for (Mek.MekVoiceInfo.VoiceSlot voiceSlot : source.getVoiceSlots()) {
            Mek.MekVoiceInfo.VoiceSlot copied = new Mek.MekVoiceInfo.VoiceSlot();
            BeanUtil.copyProperties(voiceSlot, copied);
            voiceSlots.add(copied);
        }
        target.setVoiceSlots(voiceSlots);

        // table 当前也显式重建容器，但不修改 Entry.groupId。
        List<List<List<Mek.MekVoiceInfo.Entry>>> table = new ArrayList<>();
        for (List<List<Mek.MekVoiceInfo.Entry>> row : source.getTable()) {
            List<List<Mek.MekVoiceInfo.Entry>> copiedRow = new ArrayList<>();
            for (List<Mek.MekVoiceInfo.Entry> cell : row) {
                List<Mek.MekVoiceInfo.Entry> copiedCell = new ArrayList<>();
                for (Mek.MekVoiceInfo.Entry entry : cell) {
                    Mek.MekVoiceInfo.Entry copiedEntry = new Mek.MekVoiceInfo.Entry();
                    BeanUtil.copyProperties(entry, copiedEntry);
                    copiedCell.add(copiedEntry);
                }
                copiedRow.add(copiedCell);
            }
            table.add(copiedRow);
        }
        target.setTable(table);
        return target;
    }

    private Mek.MekMaterialBlock rebuildMekMaterialBlock(MekRebindContext context) {
        Mek.MekMaterialBlock source = context.getSourceMek().getMekMaterialBlock();
        if (source == null) {
            return null;
        }

        Mek.MekMaterialBlock target = new Mek.MekMaterialBlock();
        target.setExtraRegularCount(source.getExtraRegularCount());
        target.regularCount = source.regularCount;

        // entries 当前显式重建 PluginEntry 容器。
        target.setEntries(copyPluginEntries(source.getEntries()));

        // regularEntries 当前显式重建 PluginEntry 容器。
        target.setRegularEntries(copyPluginEntries(source.getRegularEntries()));

        // trailingEntries 当前显式重建 PluginEntry 容器。
        target.setTrailingEntries(copyPluginEntries(source.getTrailingEntries()));
        return target;
    }

    private List<Mek.MekMaterialBlock.PluginEntry> copyPluginEntries(List<Mek.MekMaterialBlock.PluginEntry> source) {
        List<Mek.MekMaterialBlock.PluginEntry> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (Mek.MekMaterialBlock.PluginEntry entry : source) {
            if (entry == null) {
                target.add(null);
                continue;
            }

            Mek.MekMaterialBlock.PluginEntry copied = new Mek.MekMaterialBlock.PluginEntry();
            copied.offset = entry.offset;
            copied.length = entry.length;
            copied.setSpriteGroups(copyIntArrayGroups(entry.getSpriteGroups()));
            copied.setSeGroups(copyIntArrayGroups(entry.getSeGroups()));
            copied.setVoiceGroups(copyIntArrayGroups(entry.getVoiceGroups()));
            target.add(copied);
        }
        return target;
    }

    private List<int[]> copyIntArrayGroups(List<int[]> source) {
        List<int[]> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (int[] arr : source) {
            target.add(arr == null ? null : arr.clone());
        }
        return target;
    }

    private void validateRebindResult(Mek targetMek, MekRebindContext context) {
        if (targetMek == null || targetMek.getMekBasicInfo() == null) {
            throw new IllegalStateException("step7 重建后的 MekBasicInfo 不能为空");
        }
        if (!Integer.valueOf(context.getTargetWazaGroupIndex()).equals(targetMek.getMekBasicInfo().getWazFileSequence())) {
            throw new IllegalStateException("step7 wazFileSequence 回写失败");
        }
        if (!Integer.valueOf(context.getTargetSpriteGroupIndex()).equals(targetMek.getMekBasicInfo().getSpmFileSequence())) {
            throw new IllegalStateException("step7 spmFileSequence 回写失败");
        }
    }
}
