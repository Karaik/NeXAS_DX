package com.giga.nexas.bsdx;

import lombok.Data;

import java.util.List;

@Data
public class Meka {
    private String mekSequence;
    private String mekFileName;
    private String mekName;
    private String mekNormalDashSpeed;
    private String mekSearchDashSpeed;
    private String mekBoostDashSpeed;
    private List<Waza> wazaList;
}
