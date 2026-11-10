package com.giga.nexas.service;

import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxBatchRunner;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * BHE→BSDX 移植服务入口。
 * <p>
 * 委托给 {@link Bhe2BsdxBatchRunner} 执行批量转换。
 * 可由 GUI（JavaFX）或其他模块调用。
 */
@Slf4j
public class TransferService {

    /**
     * 使用默认配置执行 BHE→BSDX 批量移植。
     */
    public void transferBhe2Bsdx() {
        transferBhe2Bsdx(Bhe2BsdxConfig.defaults());
    }

    /**
     * 使用指定配置执行 BHE→BSDX 批量移植。
     */
    public void transferBhe2Bsdx(Bhe2BsdxConfig config) {
        try {
            new Bhe2BsdxBatchRunner(config).run();
        } catch (Exception e) {
            log.error("BHE→BSDX 批量移植失败", e);
            throw new RuntimeException("BHE→BSDX 批量移植失败", e);
        }
    }
}
