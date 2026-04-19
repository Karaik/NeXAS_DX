package com.giga.nexas.transfer.bhe2bsdx.meka.motoki.convert.waz;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.util.Map;


public class ConvertSelectedWazFilesStep {

    private final BheToBsdxWazConverter converter = new BheToBsdxWazConverter();

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        if (rawSourceBundle == null || convertedBundle == null) {
            return;
        }

        for (Map.Entry<String, com.giga.nexas.dto.bhe.waz.Waz> entry : rawSourceBundle.getWazByFileName().entrySet()) {
            String fileName = entry.getKey();
            Waz convertedWaz = converter.convert(entry.getValue());
            put(convertedBundle.getSelectedResourceBundle(), fileName, convertedWaz);
            put(convertedBundle.getMergedPackageBundle(), fileName, convertedWaz);
        }
    }

    private void put(TsukuyomiPackageBundle bundle, String fileName, Waz waz) {
        if (bundle == null || fileName == null || waz == null) {
            return;
        }
        bundle.getWazByFileName().put(fileName, waz);
    }
}
