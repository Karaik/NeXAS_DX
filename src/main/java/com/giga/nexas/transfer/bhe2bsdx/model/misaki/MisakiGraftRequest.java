package com.giga.nexas.transfer.bhe2bsdx.model.misaki;

import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class MisakiGraftRequest extends FollowupGraftRequest {

    public MisakiGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("misaki"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/misaki"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/misaki"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/misaki"));
        setMekaCodeName("MISAKI" );
        setWazCodeName("MISAKI" );
        setSpriteCodeName("MISAKI" );
        setSpriteFileName("misaki.spm");
        setMekFileName("misaki.mek");
        setWazFileName("misaki.waz");
    }
}
