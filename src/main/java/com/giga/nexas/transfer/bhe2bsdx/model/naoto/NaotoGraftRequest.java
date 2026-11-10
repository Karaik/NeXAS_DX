package com.giga.nexas.transfer.bhe2bsdx.model.naoto;

import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class NaotoGraftRequest extends FollowupGraftRequest {

    public NaotoGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("naoto"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/naoto"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/naoto"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/naoto"));
        setMekaCodeName("NAOTO" );
        setWazCodeName("NAOTO" );
        setSpriteCodeName("NAOTO" );
        setSpriteFileName("naoto.spm");
        setMekFileName("naoto.mek");
        setWazFileName("naoto.waz");
    }
}
