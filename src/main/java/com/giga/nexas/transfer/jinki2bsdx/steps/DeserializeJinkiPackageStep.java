package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

/**
 * 负责把包内 JINKI 二进制资源反序列化成内存 DTO 的步骤骨架。
 */
public class DeserializeJinkiPackageStep {

    public JinkiPackageBundle deserializePackage(AkaoGraftRequest request) {
        return new JinkiPackageBundle();
    }
}
