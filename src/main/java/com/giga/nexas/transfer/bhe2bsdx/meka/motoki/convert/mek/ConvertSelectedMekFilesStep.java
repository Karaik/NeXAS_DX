package com.giga.nexas.transfer.bhe2bsdx.meka.motoki.convert.mek;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;


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
        TsukuyomiPackageBundle mergedBundle = convertedBundle.getMergedPackageBundle();
        mergedBundle.setTsukuyomiMek(convertedMek);
    }
}
