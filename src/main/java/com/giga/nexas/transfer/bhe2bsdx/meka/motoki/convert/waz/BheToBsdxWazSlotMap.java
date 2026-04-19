package com.giga.nexas.transfer.bhe2bsdx.meka.motoki.convert.waz;

import java.util.HashMap;
import java.util.Map;


final class BheToBsdxWazSlotMap {

    private final Map<Integer, Integer> sourceToTargetSlot = buildSourceToTargetSlotMap();

    Integer resolveTargetSlot(Integer sourceSlot) {
        if (sourceSlot == null) {
            return null;
        }
        return sourceToTargetSlot.get(sourceSlot);
    }

    private Map<Integer, Integer> buildSourceToTargetSlotMap() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 0);
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        map.put(4, 4);
        map.put(5, 5);
        map.put(6, 6);
        map.put(7, 7);
        map.put(8, 8);
        map.put(9, 9);
        map.put(10, 10);
        map.put(11, 11);
        map.put(12, 12);
        map.put(13, 13);
        map.put(14, 14);
        map.put(15, 15);
        map.put(16, 16);
        map.put(17, 17);
        map.put(18, 18);
        map.put(19, 19);
        map.put(20, 20);
        map.put(21, 21);
        map.put(22, 22);
        map.put(23, -1);
        map.put(24, 23);
        map.put(25, 24);
        map.put(26, 25);
        map.put(27, 26);
        map.put(28, 27);
        map.put(29, 28);
        map.put(30, 29);
        map.put(31, 30);
        map.put(32, 31);
        map.put(33, 32);
        map.put(34, 33);
        map.put(35, -1);
        map.put(36, 34);
        map.put(37, 35);
        map.put(38, -1);
        map.put(39, 36);
        map.put(40, -1);
        map.put(41, 37);
        map.put(42, 38);
        map.put(43, -1);
        map.put(44, 39);
        map.put(45, 40);
        map.put(46, 41);
        map.put(47, 42);
        map.put(48, 43);
        map.put(49, 44);
        map.put(50, 45);
        map.put(51, 46);
        map.put(52, -1);
        map.put(53, 47);
        map.put(54, 48);
        map.put(55, 49);
        map.put(56, 50);
        map.put(57, 51);
        map.put(58, 52);
        map.put(59, 53);
        map.put(60, 54);
        map.put(61, 55);
        map.put(62, -1);
        map.put(63, 56);
        map.put(64, 57);
        map.put(65, 58);
        map.put(66, -1);
        map.put(67, -1);
        map.put(68, -1);
        map.put(69, -1);
        map.put(70, 59);
        map.put(71, 60);
        map.put(72, 61);
        map.put(73, 62);
        map.put(74, 63);
        map.put(75, 64);
        map.put(76, 65);
        map.put(77, 66);
        map.put(78, 67);
        map.put(79, 68);
        map.put(80, 69);
        map.put(81, 70);
        map.put(82, 71);

        return map;
    }
}
