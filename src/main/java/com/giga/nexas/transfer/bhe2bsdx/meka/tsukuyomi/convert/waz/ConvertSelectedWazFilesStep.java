package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.common.TsukuyomiSpecifiedCommonResources;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.util.Map;

/**
 * 转换选定的 BHE waz 文件。
 *
 * <p>本步骤只做源侧格式转换：BHE WAZ DTO -> BSDX WAZ DTO 形状。
 * 目标侧 WazaGroup/SPM/SE/Voice 重绑、skill merge/reorder、sanitizer 兜底均由后续阶段处理。</p>
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

            // 公共弹幕 WAZ 使用共享常量分流，保证 load 阶段和 convert 阶段的清单完全一致。
            if (TsukuyomiSpecifiedCommonResources.isSpecifiedCommonWaz(fileName)) {
                put(convertedBundle.getSpecifiedCommonResourceBundle(), fileName, convertedWaz);
            } else {
                put(convertedBundle.getSelectedResourceBundle(), fileName, convertedWaz);
            }

            // 后续主流程消费 merged 视图，不需要自己关心资源来自私有目录还是指定公共资源。
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
