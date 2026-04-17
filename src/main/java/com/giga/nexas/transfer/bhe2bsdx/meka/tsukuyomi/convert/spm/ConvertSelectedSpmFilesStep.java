package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.spm;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 转换选定的 BHE spm 文件。
 */
public class ConvertSelectedSpmFilesStep {

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        // TODO 20260417：转换 Tsukuyomi 私有 spm 和公共资源依赖 spm，暂时只保留空实现骨架。
    }
}
