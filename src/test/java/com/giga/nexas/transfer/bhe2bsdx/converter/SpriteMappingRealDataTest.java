package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxConfig;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxResourceLoader;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxSourceDiscovery;
import com.giga.nexas.transfer.bhe2bsdx.model.MekaSource;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaResult;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SpriteMappingRealDataTest {

    private static final List<String> SAMPLE_KEYS = List.of("tsukuyomi", "freja", "makoto", "sora");

    @Test
    void targetSlotMapping_shouldCoverMekAndWazSpriteIndices() throws Exception {
        Bhe2BsdxConfig config = Bhe2BsdxConfig.defaults();
        Bhe2BsdxResourceLoader loader = new Bhe2BsdxResourceLoader(config);
        Bhe2BsdxSourceDiscovery discovery = new Bhe2BsdxSourceDiscovery(config, loader);
        SpriteGroupIndexMapper spriteMapper = new SpriteGroupIndexMapper();

        List<MekaSource> sources = discovery.discoverSources();
        assumeTrue(sources != null && !sources.isEmpty(), "no source discovered");

        Map<String, MekaSource> sourceMap = sources.stream()
                .collect(Collectors.toMap(MekaSource::getBaseKey, source -> source, (left, right) -> left));

        int checked = 0;
        for (String baseKey : SAMPLE_KEYS) {
            MekaSource source = sourceMap.get(baseKey);
            if (source == null) {
                continue;
            }

            Map<String, com.giga.nexas.dto.bsdx.grp.Grp> bsdxGrp = loader.registerBsdxGrp();
            Map<String, com.giga.nexas.dto.bhe.grp.Grp> bheGrp = loader.registerBheGrp();
            Map<String, com.giga.nexas.dto.bsdx.mek.Mek> bsdxMek = loader.registerBsdxMek();
            Map<String, com.giga.nexas.dto.bhe.mek.Mek> bheMek = loader.registerBheMek();
            Map<String, com.giga.nexas.dto.bsdx.waz.Waz> bsdxWaz = loader.registerBsdxWaz();
            Map<String, com.giga.nexas.dto.bhe.waz.Waz> bheWaz = loader.registerBheWaz();
            Map<String, com.giga.nexas.dto.bsdx.spm.Spm> bsdxSpm = loader.registerBsdxSpm();
            Map<String, com.giga.nexas.dto.bhe.spm.Spm> bheSpm = loader.registerBheSpm();

            com.giga.nexas.dto.bhe.mek.Mek sourceMek = bheMek.get(baseKey);
            com.giga.nexas.dto.bhe.waz.Waz sourceWaz = bheWaz.get(baseKey);
            com.giga.nexas.dto.bhe.spm.Spm sourceSpm = bheSpm.get(baseKey);
            if (sourceMek == null || sourceWaz == null || sourceSpm == null) {
                continue;
            }

            String cKey = "c_" + baseKey;
            String sKey = "s_" + baseKey;
            String gKey = "g_" + baseKey;
            String mKey = "m_" + baseKey;

            com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp bheBatVoice =
                    (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp) bheGrp.get("batvoice");
            com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp bsdxBatVoice =
                    (com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp) bsdxGrp.get("batvoice");
            com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp bheMekaGroup =
                    (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp) bheGrp.get("mekagroup");
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp bheWazaGroup =
                    (com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp) bheGrp.get("wazagroup");
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheSpriteGroup =
                    (com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp) bheGrp.get("spritegroup");
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp bheSeGroup =
                    (com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp) bheGrp.get("segroup");

            com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp bsdxMekaGroup =
                    (com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp) bsdxGrp.get("mekagroup");
            com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp bsdxWazaGroup =
                    (com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp) bsdxGrp.get("wazagroup");
            com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp bsdxSpriteGroup =
                    (com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp) bsdxGrp.get("spritegroup");
            com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp bsdxSeGroup =
                    (com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp) bsdxGrp.get("segroup");

            com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup sourceBatVoice =
                    discovery.findBatVoiceGroupByCode(bheBatVoice, source.getCodeName());
            com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup sourceMekaGroup =
                    discovery.findMekaGroupByCode(bheMekaGroup, source.getCodeName());
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry sourceWazaGroup =
                    discovery.findWazaGroupByCode(bheWazaGroup, source.getCodeName());
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry sourceSpriteGroup =
                    discovery.findSpriteGroupByCode(bheSpriteGroup, source.getCodeName());

            if (sourceMekaGroup == null || sourceWazaGroup == null || sourceSpriteGroup == null) {
                continue;
            }

            com.giga.nexas.dto.bsdx.mek.Mek targetBsdxMek = bsdxMek.get(config.getTargetKey());
            assumeTrue(targetBsdxMek != null, "target slot mek not found: " + config.getTargetKey());

            TransMekaRequest request = TransMekaRequest.fromLegacy(
                    sourceMek,
                    sourceWaz,
                    sourceSpm,
                    bheSpm.get(cKey),
                    bheSpm.get(sKey),
                    bheSpm.get(gKey),
                    bheSpm.get(mKey),
                    sourceBatVoice,
                    bsdxBatVoice,
                    sourceMekaGroup,
                    sourceWazaGroup,
                    sourceSpriteGroup,
                    bheSpriteGroup,
                    bheSeGroup,
                    bsdxMekaGroup,
                    bsdxWazaGroup,
                    bsdxSpriteGroup,
                    bsdxSeGroup,
                    bsdxWaz,
                    bsdxSpm.get("mekapilot"),
                    bsdxSpm.get("selectmekamenumeka"),
                    loader.loadBsdxDat("SelectMekaMenu.dat"),
                    targetBsdxMek,
                    config.getTargetCodeName(),
                    config.isKeepTargetKey()
            );

            TransMekaResult result = new TransMekaPipeline().execute(request);

            Set<Integer> required = new HashSet<>();
            required.addAll(spriteMapper.collectRequiredIndicesFromMek(sourceMek));
            required.addAll(spriteMapper.collectRequiredIndicesFromWaz(sourceWaz));

            for (Integer index : required) {
                if (index == null || index < 0) {
                    continue;
                }
                assertTrue(
                        result.getSpriteIndexMap().containsKey(index),
                        "missing sprite mapping: source=" + baseKey + ", bheIndex=" + index
                );
            }

            assertMappedSpriteEvents(baseKey, result.getBsdxWaz(), bsdxSpriteGroup);
            checked++;
        }

        assertTrue(checked > 0, "no sample source checked");
    }

    private void assertMappedSpriteEvents(
            String sourceKey,
            com.giga.nexas.dto.bsdx.waz.Waz waz,
            SpriteGroupGrp spriteGroup
    ) {
        if (waz == null || waz.getSkillList() == null || spriteGroup == null || spriteGroup.getSpriteList() == null) {
            return;
        }
        for (com.giga.nexas.dto.bsdx.waz.Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (com.giga.nexas.dto.bsdx.waz.Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (SkillInfoObject info : unit.getSkillInfoObjectList()) {
                        if (!(info instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite)) {
                            continue;
                        }
                        com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite sprite =
                                (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite) info;
                        Integer seq = sprite.getSpmFileSequence();
                        if (seq == null || seq < 0) {
                            continue;
                        }
                        boolean valid = seq < spriteGroup.getSpriteList().size();
                        if (valid) {
                            SpriteGroupGrp.SpriteGroupEntry entry = spriteGroup.getSpriteList().get(seq);
                            valid = entry != null && entry.getExistFlag() != null && entry.getExistFlag() != 0;
                        }
                        assertTrue(
                                valid,
                                "invalid CEventSprite spmFileSequence: source=" + sourceKey + ", seq=" + seq
                        );
                    }
                }
            }
        }
    }
}
