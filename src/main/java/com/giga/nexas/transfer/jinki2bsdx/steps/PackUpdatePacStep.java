package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.PacPackPlan;
import com.giga.nexas.util.PacUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 负责把 step8/9 产物目录打包成 Update3.pac。
 */
public class PackUpdatePacStep {

    public PacPackPlan packUpdatePac(
            AkaoGraftRequest request,
            ImportedAssetSet importedAssetSet
    ) {
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
            if (!waitForFile(pacNew, 5000)) {
                throw new IllegalStateException("PacUtil.pack 已执行，但未找到 pacNew: " + pacNew);
            }

            Path outputPac = sourceFolder.resolveSibling("Update3.pac");
            Files.move(pacNew, outputPac, StandardCopyOption.REPLACE_EXISTING);
            plan.setOutputPacPath(outputPac);
            plan.setPacked(true);
            return plan;
        } catch (IOException e) {
            throw new IllegalStateException("step11 打包 Update3.pac 失败", e);
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
