package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.spm;

import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.util.Map;

/**
 * 转换选定的 BHE spm 文件。
 *
 * <p>本步骤只做源侧格式转换：BHE SPM DTO -> BSDX SPM DTO 形状。
 * 目标侧 SpriteGroup 重绑、WAZ 引用回写、图片 sidecar 闭包均由 graft/output 阶段处理。</p>
 */
public class ConvertSelectedSpmFilesStep {

    private final BheToBsdxSpmConverter converter = new BheToBsdxSpmConverter();

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        if (rawSourceBundle == null || convertedBundle == null) {
            return;
        }
        for (Map.Entry<String, com.giga.nexas.dto.bhe.spm.Spm> entry : rawSourceBundle.getSpmByFileName().entrySet()) {
            String fileName = entry.getKey();
            Spm convertedSpm = converter.convert(entry.getValue());
            put(convertedBundle.getSelectedResourceBundle(), fileName, convertedSpm);
            put(convertedBundle.getMergedPackageBundle(), fileName, convertedSpm);
        }
    }

    private void put(TsukuyomiPackageBundle bundle, String fileName, Spm spm) {
        if (bundle == null || fileName == null || spm == null) {
            return;
        }
        bundle.getSpmByFileName().put(fileName, spm);
    }
}
