package com.giga.nexas.transfer.bhe2bsdx.model.freja;

import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class FrejaGraftRequest extends FollowupGraftRequest {

    public FrejaGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("freja"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/freja"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/freja"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/freja"));
        setMekaCodeName("FREJA" );
        setWazCodeName("FREJA" );
        setSpriteCodeName("FREJA" );
        setSpriteFileName("freja.spm");
        setMekFileName("freja.mek");
        setWazFileName("freja.waz");
    }
}
