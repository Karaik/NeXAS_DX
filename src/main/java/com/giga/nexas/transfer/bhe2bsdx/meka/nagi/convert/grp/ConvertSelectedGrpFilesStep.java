package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert.grp;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;


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
        TsukuyomiPackageBundle mergedBundle = convertedBundle.getMergedPackageBundle();
        mergedBundle.setBatVoiceGrp(selectedBundle.getBatVoiceGrp());
        mergedBundle.setMekaGroupGrp(selectedBundle.getMekaGroupGrp());
        mergedBundle.setSeGroupGrp(selectedBundle.getSeGroupGrp());
        mergedBundle.setSpriteGroupGrp(selectedBundle.getSpriteGroupGrp());
        mergedBundle.setWazaGroupGrp(selectedBundle.getWazaGroupGrp());
    }
}
