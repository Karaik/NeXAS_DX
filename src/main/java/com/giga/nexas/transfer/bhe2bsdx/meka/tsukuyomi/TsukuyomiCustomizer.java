package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

/** 桜火：只处理本角色已重绑资源，由现有主流程在写出前调用。 */
public final class TsukuyomiCustomizer {
    public static final TsukuyomiCustomizer INSTANCE = new TsukuyomiCustomizer();

    private TsukuyomiCustomizer() { }

    public void customizeBeforeWrite(Mek mek, Waz waz) {
        if (mek == null || waz == null || mek.getMekBasicInfo() == null) {
            throw new IllegalStateException("Tsukuyomi变体缺少已重绑MEK/WAZ");
        }
        var source = skill(waz, "袈裟", "PUNCH", 1);
        var sourceWeapon = weapon(mek, waz, source, "袈裟", "PUNCH");
        var drawn = skill(waz, "抜刀中", "0009", 2);
        var sheath = skill(waz, "納刀", "0008", 1);
        var sheathWeapon = weapon(mek, waz, sheath, "納刀", "0003");
        requireNormalEnd(event(sheath, 1, 71, CEventChange.class, 30, 30, 30, 30));
        int group = mek.getMekBasicInfo().getWazFileSequence();
        int expected = waz.getSkillList().indexOf(drawn);
        int target = waz.getSkillList().indexOf(sheath);

        CharacterWeaponVariants.append(mek, waz, sourceWeapon, source,
                "袈裟・一閃", "DX_KESA_ISSEN", 12,
                "踏み込んで一刀を放ち、納刀して仕切り直す。抜刀中の連撃とは使い分け、短い接近戦から次の間合いを作る。", copy -> {
                    CEventValRandom speed = event(copy, 1, 18, CEventValRandom.class, 8, 11, 8, 11);
                    if (!List.of(0, 24, 0, 0, 0, 0).equals(List.of(speed.getInt1(), speed.getInt2(),
                            speed.getInt3(), speed.getInt4(), speed.getInt5(), speed.getInt6()))) {
                        throw new IllegalStateException("袈裟速度签名漂移");
                    }
                    CEventHit hit = event(copy, 1, 41, CEventHit.class, 10, 11, 10, 11);
                    var units = hit.getCeventHitUnitList().stream()
                            .filter(value -> Integer.valueOf(6).equals(value.getUnitSlotNum())).toList();
                    if (units.size() != 1 || !Integer.valueOf(1).equals(units.get(0).getBuffer())
                            || !(units.get(0).getData() instanceof CEventTerm term)
                            || term.getBsdxInfoCollectionList().size() != 1
                            || !Integer.valueOf(23).equals(term.getBsdxInfoCollectionList().get(0).getInt1())
                            || !List.of(0).equals(term.getBsdxInfoCollectionList().get(0).getTypeList())
                            || !List.of(30).equals(term.getBsdxInfoCollectionList().get(0).getParamList())) {
                        throw new IllegalStateException("袈裟攻击签名漂移");
                    }
                    event(copy, 1, 48, CEventEffect.class, 8, 11, 8, 11);
                    CEventChange exit = event(copy, 1, 71, CEventChange.class, 38, 38, 38, 38);
                    requireJump(exit, group, expected);
                    speed.setInt2(32);
                    exit.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(group, target)));
                }, sheathWeapon);
        TsukuyomiAdditionalVariants.appendAll(mek, waz);
    }

    private static void requireJump(CEventChange change, int group, int skill) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2().size() != 1) throw new IllegalStateException("袈裟退出结构漂移");
        var condition = change.getBsdxInfoCollectionList1().get(0);
        var jump = change.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(0).equals(condition.getInt1()) || !List.of(0).equals(condition.getTypeList())
                || !condition.getParamList().isEmpty() || !Integer.valueOf(16).equals(jump.getInt1())
                || !List.of(1).equals(jump.getTypeList()) || !List.of(group, skill).equals(jump.getParamList())) {
            throw new IllegalStateException("袈裟退出目标不再是本角色抜刀中");
        }
    }
}
