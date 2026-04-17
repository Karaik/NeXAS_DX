package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.grp;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 转换选定的 BHE grp 文件。
 */
public class ConvertSelectedGrpFilesStep {

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        // TODO 20260417：转换 BatVoice/MekaGroup/SeGroup/SpriteGroup/WazaGroup，暂时只保留空实现骨架。
    }
}
