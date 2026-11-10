package com.giga.nexas.transfer.bhe2bsdx.meka.initializer;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek.BheToBsdxMekConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnsureInitializerWeaponStepTest {

    private static final String CHARSET = "windows-31j";

    private final BheBinService bheBinService = new BheBinService();
    private final BheToBsdxMekConverter mekConverter = new BheToBsdxMekConverter();
    private final BheToBsdxWazConverter wazConverter = new BheToBsdxWazConverter();
    private final EnsureInitializerWeaponStep step = new EnsureInitializerWeaponStep();

    @Test
    void appendInitializerWeaponAndSkillToTsukuyomiWhenMissing() throws Exception {
        TsukuyomiConvertedBundle bundle = new TsukuyomiConvertedBundle();
        Mek convertedMek = loadConvertedMek("src/main/resources/game/bhe/mek/tsukuyomi.mek");
        Waz convertedWaz = loadConvertedWaz("src/main/resources/game/bhe/waz/tsukuyomi.waz");
        int originalWeaponCount = convertedMek.getMekWeaponInfoMap().size();
        int originalSkillCount = convertedWaz.getSkillList().size();
        int originalMaterialTailCount = convertedMek.getMekMaterialBlock().getTrailingEntries().size();

        bundle.getSelectedResourceBundle().setTsukuyomiMek(convertedMek);
        bundle.getSelectedResourceBundle().getWazByFileName().put("tsukuyomi.waz", convertedWaz);

        step.ensureAfterRebind(
                "tsukuyomi.waz",
                bundle.getSelectedResourceBundle().getTsukuyomiMek(),
                convertedWaz,
                bundle.getNotes()
        );

        assertEquals(originalSkillCount + 1, convertedWaz.getSkillList().size());
        Waz.Skill initializerSkill = convertedWaz.getSkillList().get(originalSkillCount);
        assertEquals("INITIALIZER", initializerSkill.getSkillNameEnglish());

        assertEquals(originalWeaponCount + 1, convertedMek.getMekWeaponInfoMap().size());
        Mek.MekWeaponInfo initializerWeapon = convertedMek.getMekWeaponInfoMap().get(originalWeaponCount);
        assertNotNull(initializerWeapon);
        assertEquals("INITIALIZER", initializerWeapon.getWeaponSequence());
        assertEquals("イニシャライザ", initializerWeapon.getWeaponName());
        assertEquals(originalSkillCount, initializerWeapon.getWazSequence());
        assertEquals(2, initializerWeapon.getWeaponCategory());
        assertEquals(
                originalMaterialTailCount + 1,
                convertedMek.getMekMaterialBlock().getTrailingEntries().size()
        );
        assertTrue(bundle.getNotes().stream().anyMatch(note -> note.contains("initializer")));
    }

    private Mek loadConvertedMek(String path) throws Exception {
        ResponseDTO<?> dto = bheBinService.parse(Path.of(path).toString(), CHARSET);
        return mekConverter.convert((com.giga.nexas.dto.bhe.mek.Mek) dto.getData());
    }

    private Waz loadConvertedWaz(String path) throws Exception {
        ResponseDTO<?> dto = bheBinService.parse(Path.of(path).toString(), CHARSET);
        return wazConverter.convert((com.giga.nexas.dto.bhe.waz.Waz) dto.getData());
    }
}
