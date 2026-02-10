package com.giga.nexas.transfer.bhe2bsdx;

import com.giga.nexas.transfer.bhe2bsdx.model.MekaSource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * BHE->BSDX 批量移植顶层编排器。
 * <p>
 * 自动发现所有合法 BHE 源机体，逐个调用 {@link Bhe2BsdxSingleRunner} 完成转换并打包为 PAC。
 */
@Slf4j
public class Bhe2BsdxBatchRunner {

    private final Bhe2BsdxConfig config;

    public Bhe2BsdxBatchRunner(Bhe2BsdxConfig config) {
        this.config = config;
    }

    /**
     * 批量转换入口。
     * 自动发现所有合法 BHE 源机体，逐个转换并打包为 PAC。
     */
    public void run() throws Exception {
        Bhe2BsdxResourceLoader loader = new Bhe2BsdxResourceLoader(config);
        Bhe2BsdxSourceDiscovery discovery = new Bhe2BsdxSourceDiscovery(config, loader);
        Bhe2BsdxSingleRunner singleRunner = new Bhe2BsdxSingleRunner(config, loader, discovery);

        List<MekaSource> sources = discovery.discoverSources();
        if (sources == null || sources.isEmpty()) {
            log.warn("未发现可用源机体，跳过转换。");
            return;
        }

        log.info("========== BHE→BSDX 批量移植开始 ==========");
        log.info("本次转换源机体数量: {}", sources.size());

        int success = 0;
        int failed = 0;
        for (MekaSource source : sources) {
            try {
                singleRunner.run(source);
                success++;
            } catch (Exception e) {
                log.error("转换失败: {}", source, e);
                failed++;
            }
        }

        log.info("========== BHE→BSDX 批量移植完成 ==========");
        log.info("成功: {}, 失败: {}, 总计: {}", success, failed, sources.size());
    }

    /**
     * 命令行入口。
     * 支持系统属性：
     * -Dtransfer.sources=misaki,sora  仅转换指定机体
     * -Dtransfer.limit=3              最多转换 N 个
     */
    public static void main(String[] args) throws Exception {
        Bhe2BsdxConfig config = Bhe2BsdxConfig.defaults();
        new Bhe2BsdxBatchRunner(config).run();
    }
}
