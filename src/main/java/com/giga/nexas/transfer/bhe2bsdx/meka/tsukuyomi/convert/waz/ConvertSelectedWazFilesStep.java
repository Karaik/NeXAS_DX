package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.util.Map;

/**
 * 转换选定的 BHE WAZ 文件。
 *
 * <p>本步骤只负责单机体私有 WAZ 的格式转换：BHE WAZ DTO -> BSDX WAZ DTO。
 * 公共弹幕 WAZ 由 raw source 单独分离，统一交给 bhecommon 公共资源入口转换，
 * 避免公共资源混入单机体 selected 转换步骤。</p>
 */
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
            // rawSourceBundle.wazByFileName 的契约是“单机体私有 WAZ”，不包含公共 WAZ。
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
