package com.giga.nexas.transfer.bhe2bsdx.model.followup;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import lombok.Data;

/**
 * 后续 9 个单机体共用的请求基类。
 *
 * <p>它延续 `TsukuyomiGraftRequest` 的主字段，只额外补一条
 * `previousCharacterResult`，用于链式继承上一机体结果。</p>
 */
@Data
public class FollowupGraftRequest extends TsukuyomiGraftRequest {

    private TsukuyomiGraftResult previousCharacterResult;
}
