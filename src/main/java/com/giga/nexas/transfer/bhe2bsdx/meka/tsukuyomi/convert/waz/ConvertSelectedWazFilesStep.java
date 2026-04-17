package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 转换选定的 BHE waz 文件。
 */
public class ConvertSelectedWazFilesStep {

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        // TODO 20260417：转换 Tsukuyomi 私有 waz 和指定公共弹幕 waz，暂时只保留空实现骨架。
    }
}
