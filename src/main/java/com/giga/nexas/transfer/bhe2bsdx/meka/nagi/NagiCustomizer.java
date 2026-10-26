package com.giga.nexas.transfer.bhe2bsdx.meka.nagi;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVal;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class NagiCustomizer implements FollowupMekaCustomizer {

    public static final NagiCustomizer INSTANCE = new NagiCustomizer();

    private NagiCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        Mek mek = requireMek(context, "Nagi");
        Waz waz = requireWaz(context, "Nagi");
        Waz.Skill original = skill(waz, "シールド：ディフレクト", "0016", 2);
        Mek.MekWeaponInfo source = weapon(mek, waz, original, "シールド：ディフレクト", "0018");
        if (!Integer.valueOf(0).equals(source.getHeatMinConsumption())
                || !Integer.valueOf(20).equals(source.getHeatMaxConsumption())
                || !Integer.valueOf(0).equals(source.getUpgradeExp())) {
            throw drift("原ディフレクト的热耗或升级值不符");
        }
        CharacterWeaponVariants.append(mek, waz, source, original,
                "瞬間ディフレクト", "DX_QUICK_DEFLECT", 12,
                "短い受付で攻撃を受け止め、成功時はディフレクトへ移る。長く構えず、読み合いから素早く攻めへ戻りたい。",
                NagiCustomizer::shortenGuardWindow);
    }

    private static void shortenGuardWindow(Waz.Skill copy) {
        CEventSprite hold = event(copy, 1, 0, CEventSprite.class, 4, 93, 4, 93);
        CEventSprite release = event(copy, 1, 0, CEventSprite.class, 94, 113, 94, 113);
        CEventVal armorOn = event(copy, 1, 36, CEventVal.class, 5, 5, 5, 5);
        CEventVal armorOff = event(copy, 1, 36, CEventVal.class, 94, 94, 94, 94);
        CEventVal cancel = event(copy, 1, 47, CEventVal.class, 79, 113, 79, 113);
        CEventChange counter = event(copy, 1, 71, CEventChange.class, 5, 93, 5, 93);
        CEventChange timeout = event(copy, 1, 71, CEventChange.class, 113, 113, 113, 113);
        requireValues(armorOn, List.of(0, 0, 0, 4));
        requireValues(armorOff, List.of(0, 0, 0, 0));
        requireValues(cancel, List.of(0, 3, 0, 3));
        requireChange(counter, List.of(2, 6, 6), List.of(0, 0), List.of(0, 0));
        requireChange(timeout, List.of(0), List.of(), List.of(0, 2));
        // 只校验原生反击；不执行返回的写操作，不改其判定、攻击值或退出路径。
        attack(event(copy, 2, 41, CEventHit.class, 19, 35, 19, 35), 19, 35, 150, 19, 35, 150);
        CEventChange counterEnd = event(copy, 2, 71, CEventChange.class, 79, 79, 79, 79);
        requireChange(counterEnd, List.of(0), List.of(), List.of(0, 0));

        // 窗口、装甲关闭、恢复动画和取消窗口一起收束，避免缩窗后遗留防御状态。
        hold.setEndFrame(19);
        release.setStartFrame(20);
        release.setEndFrame(39);
        armorOff.setStartFrame(20);
        armorOff.setEndFrame(20);
        cancel.setStartFrame(20);
        cancel.setEndFrame(39);
        counter.setEndFrame(19);
        timeout.setStartFrame(39);
        timeout.setEndFrame(39);
    }

    private static void requireValues(CEventVal event, List<Integer> expected) {
        if (!expected.equals(java.util.Arrays.asList(event.getInt1(), event.getInt2(), event.getInt3(), event.getInt4()))) {
            throw drift("装甲或取消参数不符");
        }
    }

    private static void requireChange(CEventChange change, List<Integer> condition,
                                      List<Integer> objects, List<Integer> destination) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1
                || !collection(change.getBsdxInfoCollectionList1().get(0), 0, condition, objects)
                || !collection(change.getBsdxInfoCollectionList2().get(0), 16, destination, List.of())) {
            throw drift("防御派生或退出签名不符");
        }
    }

    private static boolean collection(BsdxInfoCollection value, int root, List<Integer> types, List<Integer> objects) {
        return value != null && Integer.valueOf(root).equals(value.getInt1())
                && types.equals(value.getTypeList()) && List.of().equals(value.getParamList())
                && objects.equals(value.getIntList3()) && List.of().equals(value.getIntList4())
                && Integer.valueOf(0).equals(value.getInt2());
    }

    private static IllegalStateException drift(String message) {
        return new IllegalStateException("Nagi变体输入版本不符: " + message);
    }
}
