package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class WilhelmCustomizer implements FollowupMekaCustomizer {

    public static final WilhelmCustomizer INSTANCE = new WilhelmCustomizer();

    private WilhelmCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        var mek = requireMek(context, "Wilhelm");
        var waz = requireWaz(context, "Wilhelm");
        var source = skill(waz, "キャタピラ：主砲斉射", "0017", 2);
        var sourceWeapon = weapon(mek, waz, source, "主砲斉射", "0007");
        var continuous = skill(waz, "キャタピラ：持続", "TACKLE07", 2);
        var ending = skill(waz, "キャタピラ：終了", "TACKLE05", 1);
        var endingWeapon = weapon(mek, waz, ending, "キャタピラ：終了", "0022");
        requireNormalEnd(event(ending, 1, 71, CEventChange.class, 25, 25, 25, 25));
        int group = mek.getMekBasicInfo().getWazFileSequence();
        int expected = waz.getSkillList().indexOf(continuous);
        int target = waz.getSkillList().indexOf(ending);
        CharacterWeaponVariants.append(mek, waz, sourceWeapon, source,
                "交互砲撃・離脱", "DX_ALTERNATE_CANNON_EXIT", 40,
                "左右の主砲を時間差で撃ち、四脚形態へ戻る。ミサイルで相手を動かし、二拍の砲撃を置いて間合いを取り直す。", copy -> {
                    event(copy, 1, 71, CEventChange.class, 43, 43, 43, 43);
                    var effects = copy.getPhasesInfo().get(1).getSkillUnitCollection().stream()
                            .filter(value -> Integer.valueOf(48).equals(value.getUnitQuantity()))
                            .flatMap(value -> value.getSkillInfoObjectList().stream()).toList();
                    if (effects.size() != 10) throw new IllegalStateException("主砲斉射Effect数量漂移");
                    List<CEventEffect> right = new ArrayList<>();
                    int left = 0;
                    int center = 0;
                    for (var object : effects) {
                        if (!(object instanceof CEventEffect effect) || !frames(effect, 0, 0)) {
                            throw new IllegalStateException("主砲斉射外层帧或事件类型漂移");
                        }
                        var positions = effect.getCeventEffectUnitList().stream()
                                .filter(value -> Integer.valueOf(7).equals(value.getUnitSlotNum())).toList();
                        if (positions.size() != 1 || !Integer.valueOf(1).equals(positions.get(0).getBuffer())
                                || !(positions.get(0).getData() instanceof CEventTerm position)
                                || position.getBsdxInfoCollectionList().size() != 1) {
                            throw new IllegalStateException("主砲斉射Unit7结构漂移");
                        }
                        var collection = position.getBsdxInfoCollectionList().get(0);
                        // 依据当前两份Term字典核实：最终POS/OBJECT仍为root20/type[1]。
                        if (!Integer.valueOf(20).equals(collection.getInt1()) || !List.of(1).equals(collection.getTypeList())
                                || !collection.getParamList().isEmpty() || !collection.getIntList4().isEmpty()
                                || !Integer.valueOf(0).equals(collection.getInt2())) {
                            throw new IllegalStateException("主砲斉射发射位置表达式漂移");
                        }
                        if (List.of(0, 2).equals(collection.getIntList3())) right.add(effect);
                        else if (List.of(0, 1).equals(collection.getIntList3())) left++;
                        else if (List.of(0, 0).equals(collection.getIntList3())) center++;
                        else throw new IllegalStateException("主砲斉射锚点不再是左右或中心");
                    }
                    if (right.size() != 4 || left != 4 || center != 2) throw new IllegalStateException("主砲左右分配漂移");
                    CEventChange exit = event(copy, 2, 71, CEventChange.class, 50, 50, 50, 50);
                    requireJump(exit, group, expected);
                    for (CEventEffect effect : right) {
                        effect.setStartFrame(8);
                        effect.setEndFrame(8);
                    }
                    exit.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(group, target)));
                }, endingWeapon);
    }

    private static void requireJump(CEventChange change, int group, int skill) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2().size() != 1) throw new IllegalStateException("主砲退出结构漂移");
        var condition = change.getBsdxInfoCollectionList1().get(0);
        var jump = change.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(0).equals(condition.getInt1()) || !List.of(0).equals(condition.getTypeList())
                || !condition.getParamList().isEmpty() || !Integer.valueOf(16).equals(jump.getInt1())
                || !List.of(1).equals(jump.getTypeList()) || !List.of(group, skill).equals(jump.getParamList())) {
            throw new IllegalStateException("主砲退出目标不再是本角色履带持続");
        }
    }
}
