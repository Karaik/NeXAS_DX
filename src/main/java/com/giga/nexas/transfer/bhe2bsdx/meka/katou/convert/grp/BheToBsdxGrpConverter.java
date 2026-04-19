package com.giga.nexas.transfer.bhe2bsdx.meka.katou.convert.grp;

import com.giga.nexas.dto.bhe.grp.Grp;
import com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp;

import java.util.ArrayList;
import java.util.List;


public class BheToBsdxGrpConverter {

    public com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp convertBatVoice(BatVoiceGrp source) {
        com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp target =
                new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp();
        copyGrpMeta(source, target);
        if (source == null) {
            return target;
        }

        target.setVoiceTypeList(convertBatVoiceTypes(source.getVoiceTypeList()));
        target.setVoiceList(convertBatVoiceGroups(source.getVoiceList()));
        return target;
    }

    public com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp convertMekaGroup(MekaGroupGrp source) {
        com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp target =
                new com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp();
        copyGrpMeta(source, target);
        if (source == null) {
            return target;
        }

        List<com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp.MekaGroup> mekaList = new ArrayList<>();
        if (source.getMekaList() != null) {
            for (MekaGroupGrp.MekaGroup sourceEntry : source.getMekaList()) {
                com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp.MekaGroup targetEntry =
                        new com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp.MekaGroup();
                if (sourceEntry != null) {
                    targetEntry.setExistFlag(sourceEntry.getExistFlag());
                    targetEntry.setMekaName(sourceEntry.getMekaName());
                    targetEntry.setMekaCodeName(sourceEntry.getMekaCodeName());
                }
                mekaList.add(targetEntry);
            }
        }
        target.setMekaList(mekaList);
        return target;
    }

    public com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp convertSeGroup(SeGroupGrp source) {
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp target =
                new com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp();
        copyGrpMeta(source, target);
        if (source == null) {
            return target;
        }

        List<com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupGroup> seList = new ArrayList<>();
        if (source.getSeList() != null) {
            for (SeGroupGrp.SeGroupGroup sourceGroup : source.getSeList()) {
                com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupGroup targetGroup =
                        new com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupGroup();
                if (sourceGroup != null) {
                    targetGroup.setExistFlag(sourceGroup.getExistFlag());
                    targetGroup.setSeType(sourceGroup.getSeType());
                    targetGroup.setSeTypeCodeName(sourceGroup.getSeTypeCodeName());
                    targetGroup.setSeItems(convertSeItems(sourceGroup.getSeItems()));
                }
                seList.add(targetGroup);
            }
        }
        target.setSeList(seList);
        return target;
    }

    public com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp convertSpriteGroup(SpriteGroupGrp source) {
        com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp target =
                new com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp();
        copyGrpMeta(source, target);
        if (source == null) {
            return target;
        }

        List<com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry> spriteList = new ArrayList<>();
        if (source.getSpriteList() != null) {
            for (SpriteGroupGrp.SpriteGroupEntry sourceEntry : source.getSpriteList()) {
                com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry targetEntry =
                        new com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry();
                if (sourceEntry != null) {
                    targetEntry.setExistFlag(sourceEntry.getExistFlag());
                    targetEntry.setSpriteFileName(sourceEntry.getSpriteFileName());
                    targetEntry.setSpriteCodeName(sourceEntry.getSpriteCodeName());
                    targetEntry.setParam(sourceEntry.getParam());
                }
                spriteList.add(targetEntry);
            }
        }
        target.setSpriteList(spriteList);
        return target;
    }

    public com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp convertWazaGroup(WazaGroupGrp source) {
        com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp target =
                new com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp();
        copyGrpMeta(source, target);
        if (source == null) {
            return target;
        }

        List<com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp.WazaGroupEntry> wazaList = new ArrayList<>();
        if (source.getWazaList() != null) {
            for (WazaGroupGrp.WazaGroupEntry sourceEntry : source.getWazaList()) {
                com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp.WazaGroupEntry targetEntry =
                        new com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp.WazaGroupEntry();
                if (sourceEntry != null) {
                    targetEntry.setExistFlag(sourceEntry.getExistFlag());
                    targetEntry.setWazaName(sourceEntry.getWazaName());
                    targetEntry.setWazaCodeName(sourceEntry.getWazaCodeName());
                    targetEntry.setWazaDisplayName(sourceEntry.getWazaDisplayName());
                    targetEntry.setParam(sourceEntry.getParam());
                }
                wazaList.add(targetEntry);
            }
        }
        target.setWazaList(wazaList);
        return target;
    }

    private List<com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceTypeGroup> convertBatVoiceTypes(
            List<BatVoiceGrp.BatVoiceTypeGroup> sourceList
    ) {
        List<com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceTypeGroup> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (BatVoiceGrp.BatVoiceTypeGroup sourceType : sourceList) {
            com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceTypeGroup targetType =
                    new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceTypeGroup();
            if (sourceType != null) {
                targetType.setVoiceType(sourceType.getVoiceType());
                targetType.setVoiceTypeCodeName(sourceType.getVoiceTypeCodeName());
            }
            targetList.add(targetType);
        }
        return targetList;
    }

    private List<com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceGroup> convertBatVoiceGroups(
            List<BatVoiceGrp.BatVoiceGroup> sourceList
    ) {
        List<com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceGroup> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (BatVoiceGrp.BatVoiceGroup sourceGroup : sourceList) {
            com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceGroup targetGroup =
                    new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceGroup();
            if (sourceGroup != null) {
                targetGroup.setExistFlag(sourceGroup.getExistFlag());
                targetGroup.setCharacterName(sourceGroup.getCharacterName());
                targetGroup.setCharacterCodeName(sourceGroup.getCharacterCodeName());
                targetGroup.setVoices(convertBatVoices(sourceGroup.getVoices()));
            }
            targetList.add(targetGroup);
        }
        return targetList;
    }

    private List<com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoice> convertBatVoices(
            List<BatVoiceGrp.BatVoice> sourceList
    ) {
        List<com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoice> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (BatVoiceGrp.BatVoice sourceVoice : sourceList) {
            com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoice targetVoice =
                    new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoice();
            if (sourceVoice != null) {
                targetVoice.setExistFlag(sourceVoice.getExistFlag());
                targetVoice.setVoice(sourceVoice.getVoice());
                targetVoice.setVoiceCodeName(sourceVoice.getVoiceCodeName());
                targetVoice.setVoiceFileName(sourceVoice.getVoiceFileName());
            }
            targetList.add(targetVoice);
        }
        return targetList;
    }

    private List<com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupItem> convertSeItems(
            List<SeGroupGrp.SeGroupItem> sourceList
    ) {
        List<com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupItem> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (SeGroupGrp.SeGroupItem sourceItem : sourceList) {
            com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupItem targetItem =
                    new com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp.SeGroupItem();
            if (sourceItem != null) {
                targetItem.setExistFlag(sourceItem.getExistFlag());
                targetItem.setSeItemName(sourceItem.getSeItemName());
                targetItem.setSeItemCodeName(sourceItem.getSeItemCodeName());
                targetItem.setSeFileName(sourceItem.getSeFileName());
            }
            targetList.add(targetItem);
        }
        return targetList;
    }

    private void copyGrpMeta(Grp source, com.giga.nexas.dto.bsdx.grp.Grp target) {
        if (source == null || target == null) {
            return;
        }
        target.setFileName(source.getFileName());
        target.setExtensionName(source.getExtensionName());
    }
}
