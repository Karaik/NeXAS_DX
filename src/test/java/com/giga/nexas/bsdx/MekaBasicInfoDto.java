package com.giga.nexas.bsdx;

import com.giga.nexas.dto.bsdx.mek.Mek;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MekaBasicInfoDto {

    private String mekName;
    private String mekNameEnglish;
    private String pilotNameKanji;
    private String pilotNameRoma;
    private String mekDescription;
    private String wazFileSequence;
    private String spmFileSequence;
    private String mekType;
    private String healthRecovery;
    private String forceOnKill;
    private String baseHealth;
    private String energyIncreaseLevel1;
    private String energyIncreaseLevel2;
    private String boosterLevel;
    private String boosterIncreaseLevel;
    private String permanentArmor;
    private String comboImpactFactor;
    private String fightingAbility;
    private String shootingAbility;
    private String durability;
    private String mobility;
    private String physicsWeight;
    private String walkingSpeed;
    private String normalDashSpeed;
    private String searchDashSpeed;
    private String boostDashSpeed;
    private String autoHoverHeight;

    public MekaBasicInfoDto(Mek.MekBasicInfo info) {
        this.mekName = nvl(String.valueOf(info.getMekName()));
        this.mekNameEnglish = nvl(String.valueOf(info.getMekNameEnglish()));
        this.pilotNameKanji = nvl(String.valueOf(info.getPilotNameKanji()));
        this.pilotNameRoma = nvl(String.valueOf(info.getPilotNameRoma()));
        this.mekDescription = nvl(String.valueOf(info.getMekDescription()));
        this.wazFileSequence = nvl(String.valueOf(info.getWazFileSequence()));
        this.spmFileSequence = nvl(String.valueOf(info.getSpmFileSequence()));
        this.mekType = nvl(String.valueOf(info.getMekType()));
        this.healthRecovery = nvl(String.valueOf(info.getHealthRecovery()));
        this.forceOnKill = nvl(String.valueOf(info.getForceOnKill()));
        this.baseHealth = nvl(String.valueOf(info.getBaseHealth()));
        this.energyIncreaseLevel1 = nvl(String.valueOf(info.getEnergyIncreaseLevel1()));
        this.energyIncreaseLevel2 = nvl(String.valueOf(info.getEnergyIncreaseLevel2()));
        this.boosterLevel = nvl(String.valueOf(info.getBoosterLevel()));
        this.boosterIncreaseLevel = nvl(String.valueOf(info.getBoosterIncreaseLevel()));
        this.permanentArmor = nvl(String.valueOf(info.getPermanentArmor()));
        this.comboImpactFactor = nvl(String.valueOf(info.getComboImpactFactor()));
        this.fightingAbility = nvl(String.valueOf(info.getFightingAbility()));
        this.shootingAbility = nvl(String.valueOf(info.getShootingAbility()));
        this.durability = nvl(String.valueOf(info.getDurability()));
        this.mobility = nvl(String.valueOf(info.getMobility()));
        this.physicsWeight = nvl(String.valueOf(info.getPhysicsWeight()));
        this.walkingSpeed = nvl(String.valueOf(info.getWalkingSpeed()));
        this.normalDashSpeed = nvl(String.valueOf(info.getNormalDashSpeed()));
        this.searchDashSpeed = nvl(String.valueOf(info.getSearchDashSpeed()));
        this.boostDashSpeed = nvl(String.valueOf(info.getBoostDashSpeed()));
        this.autoHoverHeight = nvl(String.valueOf(info.getAutoHoverHeight()));
    }

    private String nvl(String val) {
        if ("-999".equals(val)) {
            return "デフォルト";
        }
        return val == null ? "" : val;
    }

    public List<String> outputHead() {
        List<String> head = new ArrayList<>();
        head.add("メカ名");
        head.add("メカのルビ");
        head.add("パイロット名");
        head.add("パイロットのルビ");
        head.add("メカ解説");
        head.add("デフォルト技グループ");
        head.add("デフォルトスプライトグループ");
        head.add("フラグ");
        head.add("強さＬｖ");
        head.add("フォース数");
        head.add("耐久力");
        head.add("ＦＣゲージLv1");
        head.add("ＦＣゲージLv2");
        head.add("ブースターが１になるLv");
        head.add("ブースター増分");
        head.add("常時装甲");
        head.add("コンボ感想反映率");
        head.add("性能：格闘");
        head.add("性能：射撃");
        head.add("性能：耐久力");
        head.add("性能：機動");
        head.add("重量");
        head.add("速度：歩行");
        head.add("速度：ダッシュ");
        head.add("速度：サーチダッシュ");
        head.add("速度：ブーストダッシュ");
        head.add("浮遊高度");
        return head;
    }

    public List<String> outputData() {
        List<String> row = new ArrayList<>();
        row.add(mekName);
        row.add(mekNameEnglish);
        row.add(pilotNameKanji);
        row.add(pilotNameRoma);
        row.add("略");
        row.add(wazFileSequence);
        row.add(spmFileSequence);
        row.add(mekType);
        row.add(healthRecovery);
        row.add(forceOnKill);
        row.add(baseHealth);
        row.add(energyIncreaseLevel1);
        row.add(energyIncreaseLevel2);
        row.add(boosterLevel);
        row.add(boosterIncreaseLevel);
        row.add(permanentArmor);
        row.add(comboImpactFactor);
        row.add(fightingAbility);
        row.add(shootingAbility);
        row.add(durability);
        row.add(mobility);
        row.add(physicsWeight);
        row.add(walkingSpeed);
        row.add(normalDashSpeed);
        row.add(searchDashSpeed);
        row.add(boostDashSpeed);
        row.add(autoHoverHeight);
        return row;
    }
}
