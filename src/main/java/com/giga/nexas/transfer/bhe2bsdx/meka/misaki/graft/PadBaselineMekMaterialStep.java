package com.giga.nexas.transfer.bhe2bsdx.meka.misaki.graft;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;

import java.util.List;
import java.util.Map;

public class PadBaselineMekMaterialStep {

    public void padMaterialBlock(TsukuyomiBsdxBaselineBundle bsdxBaseline) {
        if (bsdxBaseline == null) {
            return;
        }

        int targetSpriteGroups = bsdxBaseline.getSpriteGroupGrp().getSpriteList().size();
        int targetSeGroups = bsdxBaseline.getSeGroupGrp().getSeList().size();
        int targetVoiceGroups = bsdxBaseline.getBatVoiceGrp().getVoiceList().size();

        for (Map.Entry<String, Mek> entry : bsdxBaseline.getMekByFileName().entrySet()) {
            Mek mek = entry.getValue();
            Mek.MekMaterialBlock materialBlock = mek.getMekMaterialBlock();
            if (materialBlock == null) {
                continue;
            }

            padEntries(materialBlock.getEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
            padEntries(materialBlock.getRegularEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
            padEntries(materialBlock.getTrailingEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
        }
    }

    public void padMaterialBlock(Mek mek, TsukuyomiBsdxBaselineBundle bsdxBaseline) {
        if (mek == null || bsdxBaseline == null || mek.getMekMaterialBlock() == null) {
            return;
        }

        int targetSpriteGroups = bsdxBaseline.getSpriteGroupGrp().getSpriteList().size();
        int targetSeGroups = bsdxBaseline.getSeGroupGrp().getSeList().size();
        int targetVoiceGroups = bsdxBaseline.getBatVoiceGrp().getVoiceList().size();

        Mek.MekMaterialBlock materialBlock = mek.getMekMaterialBlock();
        padEntries(materialBlock.getEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
        padEntries(materialBlock.getRegularEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
        padEntries(materialBlock.getTrailingEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
    }

    private void padEntries(
            List<Mek.MekMaterialBlock.PluginEntry> entries,
            int targetSpriteGroups,
            int targetSeGroups,
            int targetVoiceGroups
    ) {
        if (entries == null) {
            return;
        }

        for (Mek.MekMaterialBlock.PluginEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            padIntArrayList(entry.getSpriteGroups(), targetSpriteGroups);
            padIntArrayList(entry.getSeGroups(), targetSeGroups);
            padIntArrayList(entry.getVoiceGroups(), targetVoiceGroups);
        }
    }

    private void padIntArrayList(List<int[]> values, int targetSize) {
        if (values == null) {
            return;
        }
        while (values.size() < targetSize) {
            values.add(new int[0]);
        }
    }
}
