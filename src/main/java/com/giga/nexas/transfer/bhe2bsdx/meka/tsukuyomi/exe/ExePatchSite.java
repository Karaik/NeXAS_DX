package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExePatchSite {

    /**
     * EXE 文件内的绝对字节偏移。
     *
     * <p>这是针对目标 exe 文件本身的 offset，不是 RVA，也不是反汇编工具里的虚拟地址。
     * ApplyExePatchStep 会直接在 byte[] 上按这个偏移读写。</p>
     */
    private int offset;

    /**
     * 未 patch baseline 在该位置应出现的字节。
     *
     * <p>如果当前输入 exe 匹配 expectedBytes，说明这个 site 还没 patch，
     * 可以安全写入 targetBytes。</p>
     */
    private byte[] expectedBytes;

    /**
     * patch 后该位置应出现的目标字节。
     *
     * <p>如果当前输入 exe 已经匹配 targetBytes，说明链式成果物里这个 site 已经被上一层 patch 过，
     * 应记录为 already patched，而不是失败。</p>
     */
    private byte[] targetBytes;

    /**
     * 给人看的逆向说明。
     *
     * <p>必须写清这个 patch site 修的是什么容量、循环边界或 gate，
     * 方便最终 exe byte diff 失败时按 site 追查。</p>
     */
    private String label;
}
