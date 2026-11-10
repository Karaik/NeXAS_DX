package com.giga.nexas.transfer.bhe2bsdx;

import com.giga.nexas.transfer.bhe2bsdx.model.MekaSource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Batch orchestrator for BHE -> BSDX migration.
 */
@Slf4j
public class Bhe2BsdxBatchRunner {

    private final Bhe2BsdxConfig config;

    public Bhe2BsdxBatchRunner(Bhe2BsdxConfig config) {
        this.config = config;
    }

    /**
     * Run batch migration for all discovered sources.
     */
    public void run() throws Exception {
        Bhe2BsdxResourceLoader loader = new Bhe2BsdxResourceLoader(config);
        Bhe2BsdxSourceDiscovery discovery = new Bhe2BsdxSourceDiscovery(config, loader);
        Bhe2BsdxSingleRunner singleRunner = new Bhe2BsdxSingleRunner(config, loader, discovery);

        List<MekaSource> sources = discovery.discoverSources();
        if (sources == null || sources.isEmpty()) {
            log.warn("No available source meka discovered, skip transfer.");
            return;
        }

        log.info("========== BHE->BSDX batch migration start ==========");
        log.info("Discovered source count: {}", sources.size());

        int success = 0;
        int failed = 0;
        for (MekaSource source : sources) {
            try {
                singleRunner.run(source);
                success++;
            } catch (Exception e) {
                log.error("Transfer failed for source: {}", source, e);
                failed++;
            }
        }

        log.info("========== BHE->BSDX batch migration finished ==========");
        log.info("Success: {}, Failed: {}, Total: {}", success, failed, sources.size());
        if (failed > 0) {
            throw new IllegalStateException(
                    "BHE->BSDX batch run finished with failures: success="
                            + success + ", failed=" + failed + ", total=" + sources.size());
        }
    }

    /**
     * CLI entry.
     *
     * Supported system properties:
     * -Dtransfer.sources=misaki,sora
     * -Dtransfer.limit=3
     */
    public static void main(String[] args) throws Exception {
        Bhe2BsdxConfig config = Bhe2BsdxConfig.defaults();
        new Bhe2BsdxBatchRunner(config).run();
    }
}