package com.giga.nexas.transfer.jinki2bsdx.v2.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutputResourceEntry {

    /**
     * 输出目录内的相对路径，统一使用 `/` 分隔。
     *
     * <p>最终 parity 按这个字段比较文件身份；目录层级不同即使文件名相同也算失败。</p>
     */
    private String relativePath;

    /**
     * 文件名本身。
     *
     * <p>主要用于阅读 manifest，不能单独作为最终比较 key。</p>
     */
    private String fileName;

    /**
     * 输出资源类别。
     *
     * <p>例如 dat、grp、mek、waz、spm、image、audio、exe 等，用于差异定位和统计。</p>
     */
    private String category;

    /**
     * 资源来源说明。
     *
     * <p>用于说明这个文件来自源 JINKI、BSDX baseline、菜单后置覆盖，还是 sidecar 生成。</p>
     */
    private String sourceDescription;

    /**
     * 文件字节大小。
     *
     * <p>用于快速判断 diff 类型；真正的最终一致性仍以 sha256/byte compare 为准。</p>
     */
    private long size;

    /**
     * 文件内容的 SHA-256。
     *
     * <p>manifest 里用于人类排查差异；最终测试仍会直接逐文件 byte compare。</p>
     */
    private String sha256;

    /**
     * 该相对路径是否覆盖过此前写入的资源。
     *
     * <p>同名覆盖是允许的，但必须可审计；例如菜单后置 DAT/SPM 覆盖主输出目录中的同名文件。</p>
     */
    private boolean overwritten;
}
