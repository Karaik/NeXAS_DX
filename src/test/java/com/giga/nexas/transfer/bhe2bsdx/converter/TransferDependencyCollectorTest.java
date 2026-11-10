package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxConfig;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxResourceLoader;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxSourceDiscovery;
import com.giga.nexas.transfer.bhe2bsdx.model.MekaSource;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMeka;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TransferDependencyCollectorTest {

    @Test
    void collectWazAndSpmClosure_shouldFollowRecursiveReferences() {
        TransferDependencyCollector collector = new TransferDependencyCollector();

        Waz root = buildWazWithSelect(0, 0);
        Waz effect = buildWazWithSelect(3, 0);
        Waz tama03 = buildWazWithSprite(8);

        Map<String, Waz> registry = new HashMap<>();
        registry.put("effect", effect);
        registry.put("tama03", tama03);

        Map<String, Waz> wazOutput = collector.collectWazOutputMap("nanoha", root, registry);
        assertTrue(wazOutput.containsKey("nanoha"));
        assertTrue(wazOutput.containsKey("effect"));
        assertTrue(wazOutput.containsKey("tama03"));

        SpriteGroupGrp spriteGroup = new SpriteGroupGrp();
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(emptySpriteEntry());
        spriteGroup.getSpriteList().add(spriteEntry("bomb.spm"));

        Map<String, Spm> spmRegistry = new HashMap<>();
        spmRegistry.put("bomb", new Spm());

        Map<String, Spm> spmOutput = collector.collectSpmOutputMap(wazOutput, spriteGroup, spmRegistry);
        assertTrue(spmOutput.containsKey("bomb"));
    }

    @Test
    void realData_tsukuyomi_shouldExposeWazAndSpmDependencies() throws Exception {
        Bhe2BsdxConfig config = Bhe2BsdxConfig.defaults();
        Bhe2BsdxResourceLoader loader = new Bhe2BsdxResourceLoader(config);
        Bhe2BsdxSourceDiscovery discovery = new Bhe2BsdxSourceDiscovery(config, loader);

        List<MekaSource> sources = discovery.discoverSources();
        assumeTrue(sources != null && !sources.isEmpty(), "no source discovered");

        Map<String, MekaSource> sourceMap = sources.stream()
                .collect(Collectors.toMap(MekaSource::getBaseKey, source -> source, (left, right) -> left));
        MekaSource source = sourceMap.get("tsukuyomi");
        assumeTrue(source != null, "tsukuyomi source not found");

        String baseKey = source.getBaseKey();
        String codeName = source.getCodeName();
        String cKey = "c_" + baseKey;
        String sKey = "s_" + baseKey;
        String gKey = "g_" + baseKey;
        String mKey = "m_" + baseKey;

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
        assumeTrue(sourceMek != null && sourceWaz != null && sourceSpm != null, "source resources not ready");

        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp bheBatVoice =
                (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp) bheGrp.get("batvoice");
        com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp bsdxBatVoice =
                (com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp) bsdxGrp.get("batvoice");
        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup sourceBatVoice =
                discovery.findBatVoiceGroupByCode(bheBatVoice, codeName);

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

        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup sourceMekaGroup =
                discovery.findMekaGroupByCode(bheMekaGroup, codeName);
        com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry sourceWazaGroup =
                discovery.findWazaGroupByCode(bheWazaGroup, codeName);
        com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry sourceSpriteGroup =
                discovery.findSpriteGroupByCode(bheSpriteGroup, codeName);
        assumeTrue(sourceMekaGroup != null && sourceWazaGroup != null && sourceSpriteGroup != null, "grp source missing");

        com.giga.nexas.dto.bsdx.mek.Mek targetBsdxMek = bsdxMek.get(config.getTargetKey());
        TransMekaResult result = TransMeka.process(
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
        assumeTrue(result != null && result.getBsdxWaz() != null, "result waz missing");

        boolean useTargetSlot = targetBsdxMek != null;
        String mainWazKey = useTargetSlot ? config.getTargetKey() : baseKey;
        TransferDependencyCollector collector = new TransferDependencyCollector();
        Map<String, Waz> outputWaz = collector.collectWazOutputMap(mainWazKey, result.getBsdxWaz(), bsdxWaz);
        assertTrue(outputWaz.containsKey("effect"));
        assertTrue(outputWaz.containsKey("tama03") || outputWaz.containsKey("tama05"));
        assertTrue(outputWaz.size() <= 6);

        Map<String, Spm> outputSpm = new LinkedHashMap<>(
                collector.collectSpmOutputMap(mainWazKey, result.getBsdxWaz(), bsdxWaz, bsdxSpriteGroup, bsdxSpm)
        );
        if (result.getBsdxMSpm() != null) {
            outputSpm.put(mKey, result.getBsdxMSpm());
        }
        if (result.getBsdxSSpm() != null) {
            outputSpm.put(sKey, result.getBsdxSSpm());
        }
        if (result.getBsdxGSpm() != null) {
            outputSpm.put(gKey, result.getBsdxGSpm());
        }
        assertTrue(outputSpm.containsKey("bomb"));
        assertTrue(outputSpm.containsKey("tama"));
    }

    private Waz buildWazWithSelect(int fileNo, int sequenceNo) {
        Waz waz = new Waz();
        Waz.Skill skill = new Waz.Skill();
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit();
        unit.setUnitQuantity(4);

        CEventWazaSelect select = new CEventWazaSelect(4);
        select.setWazFileNo(fileNo);
        select.setWazSequenceNo(sequenceNo);
        unit.getSkillInfoObjectList().add(select);
        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        waz.getSkillList().add(skill);
        return waz;
    }

    private Waz buildWazWithSprite(int spmSequence) {
        Waz waz = new Waz();
        Waz.Skill skill = new Waz.Skill();
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit();
        unit.setUnitQuantity(20);

        CEventEffect effect = new CEventEffect(19);
        CEventEffect.CEventEffectUnit inner = new CEventEffect.CEventEffectUnit();
        inner.setUnitSlotNum(0);
        inner.setBuffer(1);
        CEventSprite sprite = new CEventSprite(20);
        sprite.setSpmFileSequence(spmSequence);
        inner.setData(sprite);
        effect.getCeventEffectUnitList().add(inner);

        unit.getSkillInfoObjectList().add(effect);
        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        waz.getSkillList().add(skill);
        return waz;
    }

    private SpriteGroupGrp.SpriteGroupEntry emptySpriteEntry() {
        SpriteGroupGrp.SpriteGroupEntry entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(0);
        return entry;
    }

    private SpriteGroupGrp.SpriteGroupEntry spriteEntry(String fileName) {
        SpriteGroupGrp.SpriteGroupEntry entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(1);
        entry.setSpriteFileName(fileName);
        entry.setSpriteCodeName(fileName);
        return entry;
    }
}
