package com.giga.nexas.transfer.bhe2bsdx.model.wilhelm;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class WilhelmGraftRequest extends TsukuyomiGraftRequest {

    private TsukuyomiGraftResult previousCharacterResult;

    public WilhelmGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("wilhelm"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/wilhelm"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/wilhelm"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/wilhelm"));
        setMekaCodeName("WILHELM");
        setWazCodeName("WILHELM");
        setSpriteCodeName("WILHELM");
        setSpriteFileName("wilhelm.spm");
        setMekFileName("wilhelm.mek");
        setWazFileName("wilhelm.waz");
    }
}
