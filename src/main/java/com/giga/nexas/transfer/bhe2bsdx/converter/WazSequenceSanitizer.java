package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WAZ 序号清理器：
 * - 修正 CEventWazaSelect 的 wazSequenceNo，避免引用 BSDX 中不存在的效果/子弹资源
 * - 当 BHE 引用了 BSDX 不存在的序号时，回退到"空技能"或 0 号技能
 */
@Slf4j
public class WazSequenceSanitizer {

    private static final Map<Integer, String> FILE_NO_TO_KEY = Map.of(
            0, "effect",
            1, "tama01",
            2, "tama02",
            3, "tama03",
            4, "tama04",
            5, "tama05",
            6, "laser",
            7, "bomb"
    );

    private final Map<Integer, WazIndexPolicy> policyMap = new HashMap<>();

    public static WazSequenceSanitizer fromBsdxWaz(Map<String, Waz> bsdxWazRegistry) {
        if (bsdxWazRegistry == null || bsdxWazRegistry.isEmpty()) {
            return null;
        }

        WazSequenceSanitizer sanitizer = new WazSequenceSanitizer();
        for (Map.Entry<Integer, String> entry : FILE_NO_TO_KEY.entrySet()) {
            Integer fileNo = entry.getKey();
            String key = entry.getValue();
            Waz waz = bsdxWazRegistry.get(key);
            if (waz == null) {
                // 兼容大小写
                waz = bsdxWazRegistry.get(key.toLowerCase());
            }
            int maxIndex = maxIndexOf(waz);
            int fallbackIndex = findEmptySkillIndex(waz);
            if (fallbackIndex < 0) {
                fallbackIndex = 0;
            }
            sanitizer.policyMap.put(fileNo, new WazIndexPolicy(maxIndex, fallbackIndex));
        }
        return sanitizer;
    }

    public void sanitize(Waz waz) {
        if (waz == null || waz.getSkillList() == null) {
            return;
        }
        for (Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (SkillInfoObject info : unit.getSkillInfoObjectList()) {
                        visit(info);
                    }
                }
            }
        }
    }

    private void visit(SkillInfoObject info) {
        if (info == null) {
            return;
        }
        if (info instanceof CEventWazaSelect select) {
            sanitizeWazaSelect(select);
            return;
        }
        if (info instanceof CEventEffect ev) {
            List<CEventEffect.CEventEffectUnit> units = ev.getCeventEffectUnitList();
            if (units != null) {
                for (CEventEffect.CEventEffectUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventHit ev) {
            List<CEventHit.CEventHitUnit> units = ev.getCeventHitUnitList();
            if (units != null) {
                for (CEventHit.CEventHitUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventEscape ev) {
            List<CEventEscape.CEventEscapeUnit> units = ev.getCeventEscapeUnitList();
            if (units != null) {
                for (CEventEscape.CEventEscapeUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventScreenLine ev) {
            List<CEventScreenLine.CEventScreenLineUnit> units = ev.getCeventScreenLineUnitList();
            if (units != null) {
                for (CEventScreenLine.CEventScreenLineUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventScreenEffect ev) {
            List<CEventScreenEffect.CEventScreenEffectUnit> units = ev.getCeventScreenEffectUnitList();
            if (units != null) {
                for (CEventScreenEffect.CEventScreenEffectUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventRadialLine ev) {
            List<CEventRadialLine.CEventRadialLineUnit> units = ev.getCeventRadialLineUnitList();
            if (units != null) {
                for (CEventRadialLine.CEventRadialLineUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventCharge ev) {
            List<CEventCharge.CEventChargeUnit> units = ev.getCeventChargeUnitList();
            if (units != null) {
                for (CEventCharge.CEventChargeUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventCamera ev) {
            List<CEventCamera.CEventCameraUnit> units = ev.getCeventCameraUnitList();
            if (units != null) {
                for (CEventCamera.CEventCameraUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventHeight ev) {
            List<CEventHeight.CEventHeightUnit> units = ev.getCeventHeightUnitList();
            if (units != null) {
                for (CEventHeight.CEventHeightUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventStatus ev) {
            List<CEventStatus.CEventStatusUnit> units = ev.getCeventStatusUnitList();
            if (units != null) {
                for (CEventStatus.CEventStatusUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventNokezori ev) {
            List<CEventNokezori.CEventNokezoriUnit> units = ev.getCeventNokezoriUnitList();
            if (units != null) {
                for (CEventNokezori.CEventNokezoriUnit unit : units) {
                    visit(unit.getData());
                }
            }
            return;
        }
        if (info instanceof CEventCpuButton ev) {
            List<CEventCpuButton.CEventCpuButtonUnit> units = ev.getCeventCpuButtonUnitList();
            if (units != null) {
                for (CEventCpuButton.CEventCpuButtonUnit unit : units) {
                    visit(unit.getData());
                }
            }
        }
    }

    private void sanitizeWazaSelect(CEventWazaSelect select) {
        Integer fileNo = select.getWazFileNo();
        Integer seq = select.getWazSequenceNo();
        if (fileNo == null || seq == null) {
            return;
        }
        WazIndexPolicy policy = policyMap.get(fileNo);
        if (policy == null || policy.maxIndex < 0) {
            return;
        }
        if (seq < 0 || seq > policy.maxIndex) {
            int fallback = policy.fallbackIndex;
            log.warn("CEventWazaSelect 序号越界: fileNo={}, seq={}, max={} -> fallback={}",
                    fileNo, seq, policy.maxIndex, fallback);
            select.setWazSequenceNo(fallback);
        }
    }

    private static int maxIndexOf(Waz waz) {
        if (waz == null || waz.getSkillList() == null || waz.getSkillList().isEmpty()) {
            return -1;
        }
        return waz.getSkillList().size() - 1;
    }

    private static int findEmptySkillIndex(Waz waz) {
        if (waz == null || waz.getSkillList() == null) {
            return -1;
        }
        List<Waz.Skill> skills = waz.getSkillList();
        for (int i = 0; i < skills.size(); i++) {
            Waz.Skill skill = skills.get(i);
            if (skill == null) {
                continue;
            }
            Integer phaseQuantity = skill.getPhaseQuantity();
            List<Waz.Skill.SkillPhase> phases = skill.getPhasesInfo();
            if (phaseQuantity == null || phaseQuantity == 0 || phases == null || phases.isEmpty()) {
                return i;
            }
            boolean allEmpty = true;
            for (Waz.Skill.SkillPhase phase : phases) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null) {
                        continue;
                    }
                    if ((unit.getSkillInfoObjectList() != null && !unit.getSkillInfoObjectList().isEmpty())
                            || (unit.getSkillInfoUnknownList() != null && !unit.getSkillInfoUnknownList().isEmpty())) {
                        allEmpty = false;
                        break;
                    }
                }
                if (!allEmpty) {
                    break;
                }
            }
            if (allEmpty) {
                return i;
            }
        }
        return -1;
    }

    private static final class WazIndexPolicy {
        private final int maxIndex;
        private final int fallbackIndex;

        private WazIndexPolicy(int maxIndex, int fallbackIndex) {
            this.maxIndex = maxIndex;
            this.fallbackIndex = fallbackIndex;
        }
    }
}
