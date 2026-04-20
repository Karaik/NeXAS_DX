package com.giga.nexas.transfer.bhe2bsdx.model.sou;

import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class SouGraftRequest extends FollowupGraftRequest {

    public SouGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("sou"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/sou"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/sou"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/sou"));
        setMekaCodeName("SOU" );
        setWazCodeName("SOU" );
        setSpriteCodeName("SOU" );
        setSpriteFileName("sou.spm");
        setMekFileName("sou.mek");
        setWazFileName("sou.waz");
    }
}
