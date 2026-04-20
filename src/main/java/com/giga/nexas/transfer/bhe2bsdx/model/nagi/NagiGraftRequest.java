package com.giga.nexas.transfer.bhe2bsdx.model.nagi;

import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class NagiGraftRequest extends FollowupGraftRequest {

    public NagiGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("nagi"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/nagi"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/nagi"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/nagi"));
        setMekaCodeName("NAGI" );
        setWazCodeName("NAGI" );
        setSpriteCodeName("NAGI" );
        setSpriteFileName("nagi.spm");
        setMekFileName("nagi.mek");
        setWazFileName("nagi.waz");
    }
}
