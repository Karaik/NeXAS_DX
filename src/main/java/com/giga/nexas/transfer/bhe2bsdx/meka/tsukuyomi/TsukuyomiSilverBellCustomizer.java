package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** 只修本轮公共追加段中月詠BIT44的两个不可达射击窗口；必须在cross重绑完成后调用。 */
public final class TsukuyomiSilverBellCustomizer {
    private static final int SOURCE_TAMA03 = 3;
    private static final String JAPANESE = "◆月詠：銀鈴";
    private static final String ENGLISH = "BIT44";
    private static final List<Integer> EMPTY = List.of();
    private static final List<Integer> SELF = List.of(0, 0);
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private TsukuyomiSilverBellCustomizer() { }

    public static void customizeAppended(TsukuyomiBsdxBaselineBundle baseline,
                                         BheCommonProjectileAppendPlan plan) {
        if (plan == null || plan.getSourceWazIndexToTargetIndex().get(SOURCE_TAMA03) == null) return;
        String hostName = plan.getSourceWazIndexToTargetFileName().get(SOURCE_TAMA03);
        Integer base = plan.getSourceWazIndexToTargetSkillBase().get(SOURCE_TAMA03);
        Integer count = plan.getSourceWazIndexToTargetSkillCount().get(SOURCE_TAMA03);
        if (baseline == null || baseline.getWazByFileName() == null || hostName == null
                || base == null || count == null || base < 0 || count < 0) {
            throw invalid("缺少源Tama03的宿主或本轮追加范围");
        }
        var hosts = baseline.getWazByFileName().entrySet().stream()
                .filter(entry -> hostName.equalsIgnoreCase(entry.getKey())).toList();
        if (hosts.size() != 1 || hosts.get(0).getValue() == null
                || hosts.get(0).getValue().getSkillList() == null) {
            throw invalid("公共宿主不存在或身份不唯一: " + hostName);
        }
        Waz host = hosts.get(0).getValue();
        if ((long) base + count > host.getSkillList().size()) throw invalid("本轮追加范围越界");
        int targetIndex = -1;
        for (int index = base; index < base + count; index++) {
            Waz.Skill candidate = host.getSkillList().get(index);
            if (candidate == null) continue;
            boolean japanese = JAPANESE.equals(candidate.getSkillNameJapanese());
            boolean english = ENGLISH.equals(candidate.getSkillNameEnglish());
            if (!japanese && !english) continue;
            if (!japanese || !english || targetIndex >= 0) throw invalid("本轮BIT44双名不一致或不唯一");
            targetIndex = index;
        }
        if (targetIndex < 0) return;
        Waz.Skill original = host.getSkillList().get(targetIndex);
        if (!Integer.valueOf(5).equals(original.getPhaseQuantity()) || original.getPhasesInfo() == null
                || original.getPhasesInfo().size() != 5) throw invalid("BIT44必须保持五个phase");
        CEventChange initial = validatePhase(original, 0, 159);
        CEventChange pursuit = validatePhase(original, 1, 165);
        boolean initialPatched = Integer.valueOf(0).equals(initial.getStartFrame());
        boolean pursuitPatched = Integer.valueOf(0).equals(pursuit.getStartFrame());
        if (initialPatched != pursuitPatched) throw invalid("BIT44射击窗口只修了一半");
        if (initialPatched) return;

        // addAll后的宿主与convertedBundle共享Skill；只替换独立深副本，保留源视图和原DX前缀。
        Waz.Skill copy;
        try {
            copy = JSON.readValue(JSON.writeValueAsBytes(original), Waz.Skill.class);
        } catch (IOException error) {
            throw new IllegalStateException("月詠銀鈴DTO深复制失败", error);
        }
        for (int phase = 0; phase < 2; phase++) {
            CEventChange firing = (CEventChange) changeSlot(copy, phase).getSkillInfoObjectList().get(1);
            firing.setStartFrame(0);
            firing.setEndFrame(147);
        }
        host.getSkillList().set(targetIndex, copy);
    }

    private static CEventChange validatePhase(Waz.Skill skill, int phase, int sourceFrame) {
        SkillUnit slot = changeSlot(skill, phase);
        if (slot.getSkillInfoObjectList() == null || slot.getSkillInfoObjectList().size() != 3
                || slot.getSkillInfoUnknownList() == null || slot.getSkillInfoUnknownList().size() != 1) {
            throw invalid("phase" + phase + "必须保持三个Change和一个loop");
        }
        SkillInfoUnknown loop = slot.getSkillInfoUnknownList().get(0);
        if (loop == null || !Integer.valueOf(255).equals(loop.getTypeId())
                || !Integer.valueOf(0).equals(loop.getStartFrame()) || !Integer.valueOf(147).equals(loop.getEndFrame())
                || !Integer.valueOf(-1).equals(loop.getInt1()) || !Integer.valueOf(1).equals(loop.getInt2())
                || !Integer.valueOf(-2).equals(loop.getInt3())) throw invalid("phase" + phase + "无限loop签名漂移");

        CEventChange cleanup = change(slot.getSkillInfoObjectList().get(0), 4, 1);
        requireFrame(cleanup, 0, 147);
        collection(cleanup.getBsdxInfoCollectionList1().get(0), 0, List.of(2, 2, 1), EMPTY, EMPTY, EMPTY);
        collection(cleanup.getBsdxInfoCollectionList1().get(1), 0, List.of(2, 0, 5), EMPTY, SELF, EMPTY);
        collection(cleanup.getBsdxInfoCollectionList1().get(2), 0, List.of(2, 7, 6), List.of(10), SELF, EMPTY);
        collection(cleanup.getBsdxInfoCollectionList1().get(3), 0, List.of(2, 7, 14), List.of(10), SELF, EMPTY);
        destination(cleanup, List.of(0, 1), List.of(4));

        CEventChange firing = change(slot.getSkillInfoObjectList().get(1), phase == 0 ? 1 : 2, 0);
        boolean source = Integer.valueOf(sourceFrame).equals(firing.getStartFrame())
                && Integer.valueOf(sourceFrame).equals(firing.getEndFrame());
        boolean patched = Integer.valueOf(0).equals(firing.getStartFrame())
                && Integer.valueOf(147).equals(firing.getEndFrame());
        if (!source && !patched) throw invalid("phase" + phase + "射击窗口签名漂移");
        collection(firing.getBsdxInfoCollectionList1().get(0), 0, List.of(2, 3, 6, 2),
                List.of(300), SELF, List.of(11, 0));
        if (phase == 1) collection(firing.getBsdxInfoCollectionList1().get(1), 0, List.of(2, 3, 4, 2),
                List.of(50), SELF, List.of(11, 0));
        destination(firing, phase == 0 ? List.of(0, 1) : List.of(0, 0), phase == 0 ? List.of(2) : EMPTY);

        CEventChange separation = change(slot.getSkillInfoObjectList().get(2), 1, 0);
        requireFrame(separation, 0, 147);
        collection(separation.getBsdxInfoCollectionList1().get(0), 0,
                phase == 0 ? List.of(2, 3, 6, 3) : List.of(2, 3, 6, 2),
                phase == 0 ? List.of(150) : List.of(100), SELF, List.of(2, 0));
        destination(separation, phase == 0 ? List.of(0, 0) : List.of(0, 1), phase == 0 ? EMPTY : List.of(0));
        return firing;
    }

    private static SkillUnit changeSlot(Waz.Skill skill, int phase) {
        var data = skill.getPhasesInfo().get(phase);
        if (data == null || data.getSkillUnitCollection() == null) throw invalid("phase" + phase + "为空");
        var slots = data.getSkillUnitCollection().stream()
                .filter(unit -> unit != null && Integer.valueOf(71).equals(unit.getUnitQuantity())).toList();
        if (slots.size() != 1) throw invalid("phase" + phase + "的Change槽不唯一");
        return slots.get(0);
    }

    private static CEventChange change(SkillInfoObject value, int conditions, int mode) {
        if (!(value instanceof CEventChange change) || !Integer.valueOf(71).equals(change.getSlotNum())
                || !Integer.valueOf(34).equals(change.getTypeId()) || !Integer.valueOf(conditions).equals(change.getFlag())
                || !Integer.valueOf(mode).equals(change.getInt1()) || change.getBsdxInfoCollectionList1() == null
                || change.getBsdxInfoCollectionList1().size() != conditions || change.getBsdxInfoCollectionList2() == null
                || change.getBsdxInfoCollectionList2().size() != 1) throw invalid("Change类型、条件数或模式漂移");
        return change;
    }

    private static void requireFrame(CEventChange change, int start, int end) {
        if (!Integer.valueOf(start).equals(change.getStartFrame()) || !Integer.valueOf(end).equals(change.getEndFrame())) {
            throw invalid("Change帧窗口漂移");
        }
    }

    private static void destination(CEventChange change, List<Integer> types, List<Integer> parameters) {
        collection(change.getBsdxInfoCollectionList2().get(0), 16, types, parameters, EMPTY, EMPTY);
    }

    private static void collection(BsdxInfoCollection value, int root, List<Integer> types,
                                   List<Integer> parameters, List<Integer> aux3, List<Integer> aux4) {
        if (value == null || !Integer.valueOf(root).equals(value.getInt1()) || !Integer.valueOf(0).equals(value.getInt2())
                || !Objects.equals(types, value.getTypeList()) || !Objects.equals(parameters, value.getParamList())
                || !Objects.equals(aux3, value.getIntList3()) || !Objects.equals(aux4, value.getIntList4())) {
            throw invalid("已重绑条件、对象或跳转身份漂移");
        }
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException("月詠銀鈴拒绝: " + message);
    }
}
