package com.giga.nexas.transfer.bhe2bsdx.meka.katou.pack;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPacPackPlan;
import com.giga.nexas.util.PacUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PackUpdatePacStep {

    
    private static final long PAC_NEW_WAIT_TIMEOUT_MS = 5000;

    public TsukuyomiPacPackPlan packUpdatePac(TsukuyomiGraftRequest request, TsukuyomiImportedAssetSet importedAssetSet) {
        TsukuyomiPacPackPlan plan = new TsukuyomiPacPackPlan();
        if (request == null || importedAssetSet == null || importedAssetSet.getOutputRootDir() == null) {
            return plan;
        }

        if (!request.isPackUpdatePac()) {
            Path sourceFolder = importedAssetSet.getOutputRootDir().toAbsolutePath().normalize();
            plan.setSourceFolder(sourceFolder);
            plan.setCompressMode(request.getPacCompressMode());
            plan.setPacked(false);
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
            throw new IllegalStateException("当前 打包 Update3.pac 失败", e);
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
