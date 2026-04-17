package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SidecarResourceSpec {

    /**
     * 是否输出 patch 后的 `WeaponEquip.dat`。
     *
     * <p>Tsukuyomi 追加第 104 台机体后，WeaponEquip 需要从 TSUKUYOMI 源侧补齐新增行，
     * 否则装备菜单相关文本/槽位会缺失。</p>
     */
    private boolean includeWeaponEquipDat;

    /**
     * `WeaponEquip.dat` 写入输出目录时使用的文件名。
     *
     * <p>默认保持 BSDX 原名，最终打包对象通过同名覆盖 baseline 行为。</p>
     */
    private String weaponEquipFileName;

    /**
     * 外部资源目录中 ProgramMaterial sidecar 的文件名。
     *
     * <p>这里不是直接复制入口，而是用于记录外部 sidecar 是否可用；
     * 真正 ProgramMaterial.grp 的主线同步由 graft 阶段处理。</p>
     */
    private String externalProgramMaterialFileName;

    public static SidecarResourceSpec defaultTsukuyomiSidecars() {
        // TODO 客制化入口：如果某台机体还需要额外 sidecar 文件，先把声明加到这个 spec。
        // BuildSidecarOutputsStep 只消费 spec，不应该在输出 step 里直接写死“顺手多拷贝某文件”。
        // 任何新增 sidecar 都会影响最终输出集合，必须同时补 parity 期望和 audit 说明。
        return SidecarResourceSpec.builder()
                .includeWeaponEquipDat(true)
                .weaponEquipFileName("WeaponEquip.dat")
                .externalProgramMaterialFileName("ProgramMaterial.grp")
                .build();
    }
}
