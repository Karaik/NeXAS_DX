package com.giga.nexasdxeditor.util;

import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.Mek;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/23
 * @Description TransferUtil
 */
public class TransferUtil {

    public static void transJA2ZH(Mek jaMek, Mek zhMek) {
        Mek.MekBasicInfo jaMekBasicInfo = jaMek.getMekBasicInfo();
        Mek.MekBasicInfo zhMekBasicInfo = zhMek.getMekBasicInfo();

        jaMekBasicInfo.setMekNameKana(zhMekBasicInfo.getMekNameKana());
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
