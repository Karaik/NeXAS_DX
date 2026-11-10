package com.giga.nexas.transfer.jinki2bsdx.v2.pack;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.PacPackPlan;
import com.giga.nexas.util.PacUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PackUpdatePacStepV2 {

    /**
     * 等待 `PacUtil.pack` 生成 `.pacNew` 的最大时间。
     *
     * <p>PacUtil 的打包结果先落为同目录同名前缀的 `.pacNew`，
     * V2 再把它移动成最终 `Update3.pac`。这个等待值只处理文件系统落盘延迟，
     * 不代表业务超时或压缩配置。</p>
     */
    private static final long PAC_NEW_WAIT_TIMEOUT_MS = 5000;

    public PacPackPlan packUpdatePac(AkaoGraftRequest request, ImportedAssetSet importedAssetSet) {
        PacPackPlan plan = new PacPackPlan();
        if (request == null || importedAssetSet == null || importedAssetSet.getOutputRootDir() == null) {
            return plan;
        }

        Path sourceFolder = importedAssetSet.getOutputRootDir().toAbsolutePath().normalize();
        plan.setSourceFolder(sourceFolder);
        plan.setCompressMode(request.getPacCompressMode());

        try {
            String packLog = PacUtil.pack(sourceFolder.toString(), request.getPacCompressMode());
            plan.setPackLog(packLog);

            Path pacNew = sourceFolder.resolveSibling(sourceFolder.getFileName().toString() + ".pacNew");
            if (!waitForFile(pacNew, PAC_NEW_WAIT_TIMEOUT_MS)) {
                throw new IllegalStateException("PacUtil.pack 已执行，但未找到 pacNew: " + pacNew);
            }

            Path outputPac = sourceFolder.resolveSibling("Update3.pac");
            Files.move(pacNew, outputPac, StandardCopyOption.REPLACE_EXISTING);
            plan.setOutputPacPath(outputPac);
            plan.setPacked(true);
            return plan;
        } catch (IOException e) {
            throw new IllegalStateException("V2 打包 Update3.pac 失败", e);
        }
    }

    private boolean waitForFile(Path path, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(path)) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Files.exists(path);
            }
        }
        return Files.exists(path);
    }
}
