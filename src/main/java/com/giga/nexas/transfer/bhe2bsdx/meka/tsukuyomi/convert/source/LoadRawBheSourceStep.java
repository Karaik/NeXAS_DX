package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.source;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 读取 BHE 原始资源。
 */
public class LoadRawBheSourceStep {

    public TsukuyomiRawSourceBundle load(TsukuyomiGraftRequest request) {
        // TODO 20260417：读取 BHE 原始 grp/dat/mek/waz/spm，暂时只保留空实现骨架。
        return new TsukuyomiRawSourceBundle();
    }
}
