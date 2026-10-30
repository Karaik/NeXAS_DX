package com.giga.nexas.transfer.bhe2bsdx.meka.freja;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class FrejaCustomizer implements FollowupMekaCustomizer {

    public static final FrejaCustomizer INSTANCE = new FrejaCustomizer();

    private FrejaCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        var mek = requireMek(context, "Freja");
        var waz = requireWaz(context, "Freja");
        var spear = skill(waz, "スピア", "0002", 5);
        var weapon = weapon(mek, waz, spear, "スピア", "0003");
        var heatCombo = skill(waz, "武幻剛乱舞", "0001", 8);
        var heatComboWeapon = weapon(mek, waz, heatCombo, "武幻剛乱舞", "0007");
        List<Runnable> edits = new ArrayList<>();
        edits.add(speed(event(spear, 3, 18, CEventValRandom.class, 1, 8, 1, 8),
                List.of(0, 32, 0, 0, 16, 0), List.of(0, 36, 0, 0, 20, 0)));
        edits.add(speed(event(spear, 5, 18, CEventValRandom.class, 12, 15, 12, 15),
                List.of(0, 32, 0, 0, 16, 0), List.of(0, 36, 0, 0, 20, 0)));
        edits.add(attack(event(spear, 3, 41, CEventHit.class, 3, 10, 3, 10), 3, 10, 150, 3, 10, 180));
        edits.add(attack(event(spear, 5, 41, CEventHit.class, 16, 24, 16, 24), 16, 24, 150, 16, 24, 180));
        edits.add(weaponCost(weapon, 0, 20, 24,
                "踏み込みと単発威力を強化したスピア。長い間合いから接近し、斬り上げやバックキックへつなげたい。"));
        edits.add(() -> heatComboWeapon.setWeaponDescription(
                "ヒートチャージの余力を使う連撃。使用可能な残量に応じて、連撃の継続や締めへの移行が変わる。"));

        // 位置・攻撃・武装の全署名を確認してから、一括で反映する。
        edits.forEach(Runnable::run);
        mek.getMekBasicInfo().setMekDescription(
                "ハルバードの間合いを生かして戦う四足型。踏み込みを強化したスピアを軸に、斬り上げとバックキックを使い分ける。遠距離ではジャベリンで牽制し、相手の動きを見て接近したい。");
        FrejaAdditionalVariants.appendAll(mek, waz);
    }
}
