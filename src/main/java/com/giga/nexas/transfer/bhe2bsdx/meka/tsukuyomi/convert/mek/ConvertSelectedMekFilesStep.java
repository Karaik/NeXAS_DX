package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 转换选定的 BHE mek 文件。
 *
 * <p>本步骤只做源侧格式转换：BHE Mek DTO -> BSDX Mek DTO 形状。
 * 目标侧索引重绑、WAZ skill 重排补偿、WeaponEquip 默认预设都由后续阶段处理。</p>
 */
public class ConvertSelectedMekFilesStep {

    private final BheToBsdxMekConverter converter = new BheToBsdxMekConverter();

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        if (rawSourceBundle == null || convertedBundle == null) {
            return;
        }
        if (rawSourceBundle.getTsukuyomiMek() == null) {
            throw new IllegalStateException("BHE 原始 Tsukuyomi MEK 不存在，无法执行 MEK 转换");
        }

        com.giga.nexas.dto.bsdx.mek.Mek convertedMek =
                converter.convert(rawSourceBundle.getTsukuyomiMek());

        TsukuyomiPackageBundle selectedBundle = convertedBundle.getSelectedResourceBundle();
        selectedBundle.setTsukuyomiMek(convertedMek);

        // 当前 common 资源尚未改写 MEK，本阶段 merged 包直接继承选定 MEK 转换结果。
        TsukuyomiPackageBundle mergedBundle = convertedBundle.getMergedPackageBundle();
        mergedBundle.setTsukuyomiMek(convertedMek);
    }
}
