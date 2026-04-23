package com.giga.nexas.dto.clarias.mek.checker;

import com.giga.nexas.dto.clarias.mek.Mek;

public class MekChecker {

    public static boolean checkMek(Mek mek, byte[] bytes) {
        Mek.MekHead mekHead = mek.getMekHead();
        boolean isValid = true;

        int sequence1 = mekHead.getSequence1();
        int sequence2 = mekHead.getSequence2();
        int sequence3 = mekHead.getSequence3();
        int sequence4 = mekHead.getSequence4();
        int sequence5 = mekHead.getSequence5();
        int sequence6 = mekHead.getSequence6();

        if (sequence1 <= sequence2) isValid = false;
        if (sequence2 <= sequence3) isValid = false;
        if (sequence3 <= sequence4) isValid = false;
        if (sequence4 <= sequence5) isValid = false;
        if (sequence5 <= sequence6) isValid = false;

        return isValid;
    }
}
