package com.giga.nexas.transfer.bhe2bsdx.meka.yuri;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class YuriCustomizer implements FollowupMekaCustomizer {

    public static final YuriCustomizer INSTANCE = new YuriCustomizer();

    private YuriCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        var mek = requireMek(context, "Yuri");
        var waz = requireWaz(context, "Yuri");
        var dagger = skill(waz, "アサシンダガー", "STRAIGHT04", 4);
        var sidestep = skill(waz, "サイドステップキック", "SIDESTEPKICK04", 2);
        var daggerWeapon = weapon(mek, waz, dagger, "アサシンダガー", "0004");
        var sidestepWeapon = weapon(mek, waz, sidestep, "サイドステップキック", "NAGE");
        List<Runnable> edits = new ArrayList<>();
        edits.add(attack(event(dagger, 3, 41, CEventHit.class, 46, 48, 44, 50),
                46, 48, 100, 44, 50, 140));
        CEventChange hitExit = event(dagger, 3, 71, CEventChange.class, 46, 48, 44, 50);
        requireOriginalHitExit(hitExit);
        // 命中派生と別の92フレーム終了経路を、存在確認だけしてそのまま残す。
        event(dagger, 3, 71, CEventChange.class, 92, 92, 92, 92);
        edits.add(() -> {
            hitExit.setStartFrame(44);
            hitExit.setEndFrame(50);
        });
        edits.add(speed(event(sidestep, 1, 18, CEventValRandom.class, 14, 28, 14, 28),
                List.of(0, 32, 0, 0, 8, 0), List.of(0, 40, 0, 0, 8, 0)));
        edits.add(weaponCost(daggerWeapon, 0, 40, 28,
                "踏み込みから鋭く刺し込む一撃。攻撃の有効時間と単発威力を強化し、近距離での反撃に向く。"));
        edits.add(weaponCost(sidestepWeapon, 0, 10, 12,
                "側方への位置調整を強化した蹴り。相手の正面を外し、ダガーなどの近距離攻撃へつなげたい。"));

        // 全対象を検証する前に、先に見つかった攻撃だけを書き換えない。
        edits.forEach(Runnable::run);
        mek.getMekBasicInfo().setMekDescription(
                "ダガーとエネルギーガンを使い分ける高速戦闘型。サイドステップキックで位置を変え、アサシンダガーで鋭く刺し込む。近距離の切り返しと射撃による牽制を組み合わせ、同じ場所に留まらず戦いたい。");
    }

    private static void requireOriginalHitExit(CEventChange change) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1
                || !collection(change.getBsdxInfoCollectionList1().get(0), 0,
                List.of(2, 6, 0, 0), List.of(0), List.of(0, 0))
                || !collection(change.getBsdxInfoCollectionList2().get(0), 16,
                List.of(0, 0), List.of(), List.of())) {
            throw new IllegalStateException("Yuri アサシンダガー P3命中派生の条件または遷移が変更されています");
        }
    }

    private static boolean collection(BsdxInfoCollection value, int root, List<Integer> types,
                                      List<Integer> parameters, List<Integer> objects) {
        return value != null && Integer.valueOf(root).equals(value.getInt1())
                && types.equals(value.getTypeList()) && parameters.equals(value.getParamList())
                && objects.equals(value.getIntList3()) && List.of().equals(value.getIntList4())
                && Integer.valueOf(0).equals(value.getInt2());
    }
}
