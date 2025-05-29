package com.giga.nexas.util;

import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/23
 * @Description TransferUtil
 */
public class TransferUtil {

    public static void transJA2ZH(Mek jaMek, Mek zhMek) {
        Mek.MekBasicInfo jaMekBasicInfo = jaMek.getMekBasicInfo();
        Mek.MekBasicInfo zhMekBasicInfo = zhMek.getMekBasicInfo();

        jaMekBasicInfo.setMekName(zhMekBasicInfo.getMekName());
        jaMekBasicInfo.setPilotNameKanji(zhMekBasicInfo.getPilotNameKanji());
        jaMekBasicInfo.setMekDescription(zhMekBasicInfo.getMekDescription());

        Map<Integer, Mek.MekWeaponInfo> jaMekWeaponInfoMap = jaMek.getMekWeaponInfoMap();
        Map<Integer, Mek.MekWeaponInfo> zhMekWeaponInfoMap = zhMek.getMekWeaponInfoMap();

        jaMekWeaponInfoMap.forEach((key, jaWeaponInfo) -> {
            Mek.MekWeaponInfo zhWeaponInfo = zhMekWeaponInfoMap.get(key);
            if (zhWeaponInfo != null) {
                jaWeaponInfo.setWeaponName(zhWeaponInfo.getWeaponName());
                jaWeaponInfo.setWeaponDescription(zhWeaponInfo.getWeaponDescription());
            }
        });

    }

}
