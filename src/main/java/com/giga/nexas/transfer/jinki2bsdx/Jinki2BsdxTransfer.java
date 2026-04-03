package com.giga.nexas.transfer.jinki2bsdx;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;

/**
 * JINKI -> BSDX 迁移分支的正式入口占位类。
 *
 * <p>这条分支刻意独立于 {@code bhe2bsdx}，因为源资源本身已经兼容 BSDX 系引擎，
 * 应该按“同引擎资源 graft”处理，而不是按跨游戏语义转译处理。</p>
 */
public class Jinki2BsdxTransfer {

    public static AkaoGraftResult process(AkaoGraftRequest request) {
        // Step 0: 把正式请求对象交给 AKAO graft pipeline 执行。
        return new AkaoGraftPipeline().execute(request);
    }
}
