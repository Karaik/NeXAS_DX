package com.giga.nexas.transfer.bhe2bsdx.model.katou;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class KatouGraftRequest extends TsukuyomiGraftRequest {

    private TsukuyomiGraftResult previousCharacterResult;

    public KatouGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("katou"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/katou"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/katou"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/katou"));
        setMekaCodeName("KATOU");
        setWazCodeName("KATOU");
        setSpriteCodeName("KATOU");
        setSpriteFileName("katou.spm");
        setMekFileName("katou.mek");
        setWazFileName("katou.waz");
    }
}
