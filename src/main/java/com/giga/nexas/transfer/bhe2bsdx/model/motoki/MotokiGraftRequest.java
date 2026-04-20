package com.giga.nexas.transfer.bhe2bsdx.model.motoki;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class MotokiGraftRequest extends TsukuyomiGraftRequest {

    private TsukuyomiGraftResult previousCharacterResult;

    public MotokiGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("motoki"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/motoki"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/motoki"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/motoki"));
        setMekaCodeName("MOTOKI");
        setWazCodeName("MOTOKI");
        setSpriteCodeName("MOTOKI");
        setSpriteFileName("motoki.spm");
        setMekFileName("motoki.mek");
        setWazFileName("motoki.waz");
    }
}
