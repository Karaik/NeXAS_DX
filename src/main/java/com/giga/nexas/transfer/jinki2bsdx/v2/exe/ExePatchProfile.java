package com.giga.nexas.transfer.jinki2bsdx.v2.exe;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExePatchProfile {

    /**
     * 本轮需要应用或确认的 EXE patch 位点集合。
     *
     * <p>每个 site 都包含文件偏移、expected bytes、target bytes 和逆向说明。
     * 这里是 EXE 魔法值的唯一集中入口，不要把 offset/bytes 写散到 patch 执行逻辑里。</p>
     */
    private List<ExePatchSite> sites = new ArrayList<>();

    public static ExePatchProfile defaultAkaoCompatibilityProfile() {
        ExePatchProfile profile = new ExePatchProfile();
        // TODO 客制化入口：EXE 里的手写魔法值都集中加在这个 profile。
        // addImm8/addImm32 适合改立即数，addBytes 适合改条件跳转或短指令序列。
        // expectedBytes 必须来自“未 patch 或当前 baseline 的原字节”，targetBytes 是目标字节；
        // 如果后续链式成果物已经写成 targetBytes，PatchBytesVerifier 会把它视为 already patched。
        // 不要把 offset/bytes 写散到 ApplyExePatchStep，否则后续 BSDX+JINKI+BHE 叠加时无法审计。
        profile.addImm8(0x1E3F40, 0x67, 0x68, "meka cap init: push 103 -> push 104 (sub_5E4B20 ensureStructArrayCapacity)");
        profile.addImm8(0x1E3DA2, 0x67, 0x68, "meka cap scene cleanup: cmp ebp,103 -> cmp ebp,104 (sub_5E46A0 do-while)");
        profile.addImm8(0x276427, 0x67, 0x68, "meka cap save read loop#1: cmp esi,103 -> cmp esi,104 (sub_676E00 type read)");
        profile.addImm8(0x2762EB, 0x67, 0x68, "meka cap save read loop#2: cmp esi,103 -> cmp esi,104 (sub_676E00 state read)");
        profile.addImm8(0x275336, 0x67, 0x68, "meka cap save write: push 103 -> push 104 (sub_675520 ensureStructArrayCapacity)");
        profile.addImm8(0x063A85, 0x67, 0x68, "meka cap resource calc: cmp ebx,103 -> cmp ebx,104 (sub_464630 do-while)");
        profile.addImm8(0x2749ED, 0x67, 0x68, "meka cap save read loop#3: cmp esi,103 -> cmp esi,104 (独立函数 do-while)");
        profile.addImm8(0x056CE4, 0x67, 0x68, "meka cap runtime table prealloc: push 103 -> push 104 (sub_4576F0 / sub_45C5B0)");
        profile.addImm32(0x056F45, 0x0006D090, 0x0006E180, "meka cap runtime table init loop bound: 103*4336 -> 104*4336 (sub_4576F0 cmp edi,0x6D090)");
        profile.addImm8(0x056F9B, 0x67, 0x68, "meka cap init prealloc alt: push 103 -> push 104");
        profile.addImm8(0x275158, 0x67, 0x68, "meka cap save write alt: push 103 -> push 104");
        profile.addImm8(0x30390A, 0x67, 0x68, "meka cap standalone prealloc: push 103 -> push 104");
        profile.addImm32(0x05498B, 0x00001688, 0x000016C0, "meka cap weapon-equip fill hard cap: 56*103 -> 56*104 (sub_454E60 cmp eax,0x1688)");
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

    private static byte[] littleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >>> 8) & 0xFF),
                (byte) ((value >>> 16) & 0xFF),
                (byte) ((value >>> 24) & 0xFF)
        };
    }
}
