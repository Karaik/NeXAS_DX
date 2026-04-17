package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 转换选定的 BHE mek 文件。
 */
public class ConvertSelectedMekFilesStep {

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        // TODO 20260417：转换 Tsukuyomi 私有 mek，暂时只保留空实现骨架。
    }
}
