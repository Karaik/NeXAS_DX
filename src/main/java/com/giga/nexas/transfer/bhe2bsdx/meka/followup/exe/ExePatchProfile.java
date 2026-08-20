package com.giga.nexas.transfer.bhe2bsdx.meka.followup.exe;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExePatchProfile {

    
    private List<ExePatchSite> sites = new ArrayList<>();

    public static ExePatchProfile defaultTsukuyomiCompatibilityProfile() {
        return forCapacities(104, 104);
    }

    public static ExePatchProfile forMekaCapacity(int targetMekaCapacity) {
        return forCapacities(targetMekaCapacity, targetMekaCapacity);
    }

    public static ExePatchProfile forCapacities(int targetMekaCapacity, int targetWeaponEquipRows) {
        if (targetMekaCapacity < 104 || targetMekaCapacity > 127) {
            throw new IllegalArgumentException("当前只审计过 104..127 范围内的 imm8 机体容量: " + targetMekaCapacity);
        }
        if (targetWeaponEquipRows < 104) {
            throw new IllegalArgumentException("WeaponEquip 行数不能小于 JINKI 成果物的 104 行: " + targetWeaponEquipRows);
        }
        return forCapacities(targetMekaCapacity - 1, targetMekaCapacity, targetWeaponEquipRows - 1, targetWeaponEquipRows);
    }

    public static ExePatchProfile forMekaCapacity(int sourceMekaCapacity, int targetMekaCapacity) {
        return forCapacities(sourceMekaCapacity, targetMekaCapacity, targetMekaCapacity - 1, targetMekaCapacity);
    }

    public static ExePatchProfile forCapacities(
            int sourceMekaCapacity,
            int targetMekaCapacity,
            int sourceWeaponEquipRows,
            int targetWeaponEquipRows
    ) {
        if (sourceMekaCapacity < 103 || targetMekaCapacity <= sourceMekaCapacity || targetMekaCapacity > 127) {
            throw new IllegalArgumentException(
                    "EXE 机体容量 patch 只支持逐层递增的已审计链式成果物: "
                            + sourceMekaCapacity + " -> " + targetMekaCapacity
            );
        }
        if (sourceWeaponEquipRows < 103 || targetWeaponEquipRows < sourceWeaponEquipRows) {
            throw new IllegalArgumentException(
                    "WeaponEquip 行数 patch 只支持保持或递增: "
                            + sourceWeaponEquipRows + " -> " + targetWeaponEquipRows
            );
        }

        ExePatchProfile profile = new ExePatchProfile();
        // ── 1. 机体槽位扩容位点（动态链式递增：sourceMekaCapacity -> targetMekaCapacity）─────────
        profile.addImm8(0x1E3F40, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap init: sub_5E4B20 ensureStructArrayCapacity", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x1E3DA2, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap scene cleanup: sub_5E46A0 do-while", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x276427, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap save read loop#1: sub_676E00 type read", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x2762EB, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap save read loop#2: sub_676E00 state read", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x275336, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap save write: sub_675520 ensureStructArrayCapacity", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x063A85, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap resource calc: sub_464630 do-while", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x2749ED, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap save read loop#3", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x056CE4, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap runtime table prealloc: sub_4576F0 / sub_45C5B0", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm32(0x056F45, sourceMekaCapacity * 4336, targetMekaCapacity * 4336, capacityLabel("meka cap runtime table init loop bound: sub_4576F0 cmp edi", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x056F9B, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap init prealloc alt path", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x275158, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap save write alt path", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm8(0x30390A, sourceMekaCapacity, targetMekaCapacity, capacityLabel("meka cap standalone prealloc path", sourceMekaCapacity, targetMekaCapacity));
        profile.addImm32(0x05498B, sourceWeaponEquipRows * 56, targetWeaponEquipRows * 56, capacityLabel("weapon-equip fill hard cap: sub_454E60 cmp eax", sourceWeaponEquipRows, targetWeaponEquipRows));

        // ── 2. 战斗语音门禁旁路（支持移植机体战斗与 FC 发声）─────────────
        profile.addBytes(
                0x20C1FD,
                new byte[]{(byte) 0x84, (byte) 0xC0, (byte) 0x75, (byte) 0x06},
                new byte[]{(byte) 0x90, (byte) 0x90, (byte) 0xEB, (byte) 0x06},
                "battle voice gate bypass: sub_60CDF0 ignores sub_60CC20 zero-return for direct AT/FC requests"
        );
        profile.addBytes(
                0x20C2CD,
                new byte[]{(byte) 0x84, (byte) 0xC0, (byte) 0x75, (byte) 0x06},
                new byte[]{(byte) 0x90, (byte) 0x90, (byte) 0xEB, (byte) 0x06},
                "battle voice gate bypass: sub_60CEC0 ignores sub_60CC20 zero-return for table-driven combat voice requests"
        );

        // ── 3. CRT 非法参数 Watson 弹窗旁路─────────────────────────────
        profile.addBytes(
                0x2789F8,
                new byte[]{(byte) 0x8B, (byte) 0xFF},
                new byte[]{(byte) 0xC3, (byte) 0x90},
                "CRT invalid parameter Watson bypass: _invalid_parameter_noinfo returns safely instead of aborting"
        );

        return profile;
    }

    public void addImm8(int offset, int expected, int target, String label) {
        addBytes(offset, new byte[]{(byte) expected}, new byte[]{(byte) target}, label);
    }

    public void addImm32(int offset, int expected, int target, String label) {
        addBytes(offset, littleEndian(expected), littleEndian(target), label);
    }

    public void addBytes(int offset, byte[] expectedBytes, byte[] targetBytes, String label) {
        sites.add(ExePatchSite.builder()
                .offset(offset)
                .expectedBytes(expectedBytes)
                .targetBytes(targetBytes)
                .label(label)
                .build());
    }

    private static String capacityLabel(String label, int sourceMekaCapacity, int targetMekaCapacity) {
        return label + ": " + sourceMekaCapacity + " -> " + targetMekaCapacity;
    }

    private static byte[] littleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >>> 8) & 0xFF),
                (byte) ((value >>> 16) & 0xFF),
                (byte) ((value >>> 24) & 0xFF)
        };
    }
}
