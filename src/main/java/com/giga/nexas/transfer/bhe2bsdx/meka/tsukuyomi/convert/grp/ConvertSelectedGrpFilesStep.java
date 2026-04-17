package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.grp;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 转换选定的 BHE grp 文件。
 */
public class ConvertSelectedGrpFilesStep {

    private final BheToBsdxGrpConverter converter = new BheToBsdxGrpConverter();

    public void convert(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        if (rawSourceBundle == null || convertedBundle == null) {
            return;
        }

        TsukuyomiPackageBundle selectedBundle = convertedBundle.getSelectedResourceBundle();
        selectedBundle.setBatVoiceGrp(converter.convertBatVoice(rawSourceBundle.getBatVoiceGrp()));
        selectedBundle.setMekaGroupGrp(converter.convertMekaGroup(rawSourceBundle.getMekaGroupGrp()));
        selectedBundle.setSeGroupGrp(converter.convertSeGroup(rawSourceBundle.getSeGroupGrp()));
        selectedBundle.setSpriteGroupGrp(converter.convertSpriteGroup(rawSourceBundle.getSpriteGroupGrp()));
        selectedBundle.setWazaGroupGrp(converter.convertWazaGroup(rawSourceBundle.getWazaGroupGrp()));

        // 当前公共资源转换尚未接入，合并包先继承选定资源的 group 视图。
        TsukuyomiPackageBundle mergedBundle = convertedBundle.getMergedPackageBundle();
        mergedBundle.setBatVoiceGrp(selectedBundle.getBatVoiceGrp());
        mergedBundle.setMekaGroupGrp(selectedBundle.getMekaGroupGrp());
        mergedBundle.setSeGroupGrp(selectedBundle.getSeGroupGrp());
        mergedBundle.setSpriteGroupGrp(selectedBundle.getSpriteGroupGrp());
        mergedBundle.setWazaGroupGrp(selectedBundle.getWazaGroupGrp());
    }
}
