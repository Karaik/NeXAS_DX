package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadTsukuyomiSourceAssetsStepTest {

    @TempDir
    Path tempDir;

    @Test
    void loadOnlySelectedWazAndSpmFiles() throws Exception {
        Path grpDir = tempDir.resolve("grp");
        Path datDir = tempDir.resolve("dat");
        Path mekDir = tempDir.resolve("mek");
        Path wazDir = tempDir.resolve("waz");
        Path spmDir = tempDir.resolve("spm");
        Files.createDirectories(grpDir);
        Files.createDirectories(datDir);
        Files.createDirectories(mekDir);
        Files.createDirectories(wazDir);
        Files.createDirectories(spmDir);

        touch(grpDir, "batvoice.grp", "mekagroup.grp", "segroup.grp", "spritegroup.grp", "wazagroup.grp");
        touch(datDir, "meka.dat", "weaponequip.dat", "mekapilot.dat");
        touch(mekDir, "tsukuyomi.mek");
        touch(wazDir, "effect.waz", "bomb.waz", "tsukuyomi.waz");
        touch(spmDir, "Tama.spm", "MekaEffect.spm", "tsukuyomi.spm");

        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setBheGameResourceRoot(null);
        request.setBheGrpDir(grpDir);
        request.setBheDatDir(datDir);
        request.setBheMekDir(mekDir);
        request.setBheWazDir(wazDir);
        request.setBheSpmDir(spmDir);

        LoadTsukuyomiSourceAssetsStep loader = new LoadTsukuyomiSourceAssetsStep(new FakeBsdxBinService());
        TsukuyomiPackageBundle bundle = loader.load(request);

        assertTrue(bundle.getWazByFileName().containsKey("tsukuyomi.waz"));
        assertFalse(bundle.getWazByFileName().containsKey("effect.waz"));
        assertFalse(bundle.getWazByFileName().containsKey("bomb.waz"));
        assertTrue(bundle.getSpmByFileName().containsKey("tsukuyomi.spm"));
        assertFalse(bundle.getSpmByFileName().containsKey("Tama.spm"));
        assertFalse(bundle.getSpmByFileName().containsKey("MekaEffect.spm"));
    }

    private void touch(Path dir, String... fileNames) throws IOException {
        for (String fileName : fileNames) {
            Files.writeString(dir.resolve(fileName), "");
        }
    }

    private static class FakeBsdxBinService extends BsdxBinService {
        @Override
        public ResponseDTO<?> parse(String path, String charset) {
            String fileName = Path.of(path).getFileName().toString().toLowerCase(Locale.ROOT);
            Bsdx parsed = switch (fileName) {
                case "batvoice.grp" -> new BatVoiceGrp();
                case "mekagroup.grp" -> new MekaGroupGrp();
                case "segroup.grp" -> new SeGroupGrp();
                case "spritegroup.grp" -> new SpriteGroupGrp();
                case "wazagroup.grp" -> new WazaGroupGrp();
                case "meka.dat", "weaponequip.dat", "mekapilot.dat" -> new Dat();
                case "tsukuyomi.mek" -> new Mek();
                default -> fileName.endsWith(".waz") ? new Waz() : new Spm();
            };
            return new ResponseDTO<>(parsed, "ok");
        }
    }
}
