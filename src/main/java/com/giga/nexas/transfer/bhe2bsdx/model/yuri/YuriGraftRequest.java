package com.giga.nexas.transfer.bhe2bsdx.model.yuri;

import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import lombok.Data;

import java.nio.file.Paths;

@Data
public class YuriGraftRequest extends FollowupGraftRequest {

    public YuriGraftRequest() {
        setTsukuyomiResourceDir(Paths.get("yuri"));
        setBheMekDir(Paths.get("src/main/resources/game/bhe/mek/yuri"));
        setBheSpmDir(Paths.get("src/main/resources/game/bhe/spm/yuri"));
        setBheWazDir(Paths.get("src/main/resources/game/bhe/waz/yuri"));
        setMekaCodeName("YURI" );
        setWazCodeName("YURI" );
        setSpriteCodeName("YURI" );
        setSpriteFileName("yuri.spm");
        setMekFileName("yuri.mek");
        setWazFileName("yuri.waz");
    }
}
