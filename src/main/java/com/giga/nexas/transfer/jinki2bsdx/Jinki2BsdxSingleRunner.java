package com.giga.nexas.transfer.jinki2bsdx;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JINKI -> BSDX 的单机体运行入口。
 */
@Slf4j
public class Jinki2BsdxSingleRunner {

    public static void main(String[] args) {
        new Jinki2BsdxSingleRunner().run();
    }

    public AkaoGraftResult run() {
        return run(buildDefaultRequest());
    }

    public AkaoGraftResult run(AkaoGraftRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("AkaoGraftRequest 不能为空");
        }

        log.info("========== start jinki2bsdx graft: codeName={}, mek={}, waz={}, sprite={} ==========",
                request.getMekaCodeName(),
                request.getMekFileName(),
                request.getWazFileName(),
                request.getSpriteFileName());

        AkaoGraftResult result = Jinki2BsdxTransfer.process(request);
        logSummary(result);
        return result;
    }

    private AkaoGraftRequest buildDefaultRequest() {
        AkaoGraftRequest request = new AkaoGraftRequest();

        request.setJinkiGrpDir(resolvePathProp("jinki2bsdx.jinkiGrpDir", request.getJinkiGrpDir()));
        request.setJinkiDatDir(resolvePathProp("jinki2bsdx.jinkiDatDir", request.getJinkiDatDir()));
        request.setJinkiMekDir(resolvePathProp("jinki2bsdx.jinkiMekDir", request.getJinkiMekDir()));
        request.setJinkiSpmDir(resolvePathProp("jinki2bsdx.jinkiSpmDir", request.getJinkiSpmDir()));
        request.setJinkiWazDir(resolvePathProp("jinki2bsdx.jinkiWazDir", request.getJinkiWazDir()));

        request.setBsdxGrpDir(resolvePathProp("jinki2bsdx.bsdxGrpDir", request.getBsdxGrpDir()));
        request.setBsdxDatDir(resolvePathProp("jinki2bsdx.bsdxDatDir", request.getBsdxDatDir()));
        request.setBsdxMekDir(resolvePathProp("jinki2bsdx.bsdxMekDir", request.getBsdxMekDir()));
        request.setBsdxSpmDir(resolvePathProp("jinki2bsdx.bsdxSpmDir", request.getBsdxSpmDir()));
        request.setBsdxWazDir(resolvePathProp("jinki2bsdx.bsdxWazDir", request.getBsdxWazDir()));

        request.setExternalStaticAssetRoot(resolvePathProp(
                "jinki2bsdx.externalStaticAssetRoot",
                request.getExternalStaticAssetRoot()
        ));
        request.setTargetExePath(resolvePathProp("jinki2bsdx.targetExePath", request.getTargetExePath()));
        request.setExeOutputDir(resolvePathProp("jinki2bsdx.exeOutputDir", request.getExeOutputDir()));

        request.setMekaCodeName(resolveStringProp("jinki2bsdx.mekaCodeName", request.getMekaCodeName()));
        request.setWazCodeName(resolveStringProp("jinki2bsdx.wazCodeName", request.getWazCodeName()));
        request.setSpriteCodeName(resolveStringProp("jinki2bsdx.spriteCodeName", request.getSpriteCodeName()));
        request.setSpriteFileName(resolveStringProp("jinki2bsdx.spriteFileName", request.getSpriteFileName()));
        request.setMekFileName(resolveStringProp("jinki2bsdx.mekFileName", request.getMekFileName()));
        request.setWazFileName(resolveStringProp("jinki2bsdx.wazFileName", request.getWazFileName()));

        request.setPatchMenuData(resolveBooleanProp("jinki2bsdx.patchMenuData", request.isPatchMenuData()));
        request.setPlanExeCapacityPatch(resolveBooleanProp(
                "jinki2bsdx.planExeCapacityPatch",
                request.isPlanExeCapacityPatch()
        ));

        return request;
    }

    private void logSummary(AkaoGraftResult result) {
        if (result == null) {
            log.warn("pipeline result is null");
            return;
        }

        if (result.getJinkiPackage() != null) {
            log.info("jinki package => spm={}, waz={}",
                    result.getJinkiPackage().getSpmByFileName().size(),
                    result.getJinkiPackage().getWazByFileName().size());
        }

        if (result.getBsdxBaseline() != null) {
            log.info("bsdx baseline => spm={}, waz={}",
                    result.getBsdxBaseline().getSpmByFileName().size(),
                    result.getBsdxBaseline().getWazByFileName().size());
        }

        if (result.getImportPlan() != null) {
            log.info("import plan => mek={}, waz={}, spm={}, grpTargets={}",
                    result.getImportPlan().getRequiredMekFiles().size(),
                    result.getImportPlan().getRequiredWazFiles().size(),
                    result.getImportPlan().getRequiredSpmFiles().size(),
                    result.getImportPlan().getGrpAppendTargets().size());
            log.info("required mek files => {}", result.getImportPlan().getRequiredMekFiles());
            log.info("required waz files => {}", result.getImportPlan().getRequiredWazFiles());
            log.info("required spm files => {}", result.getImportPlan().getRequiredSpmFiles());
            log.info("grp append targets => {}", result.getImportPlan().getGrpAppendTargets());
        }

        if (result.getGrpAppendPlan() != null) {
            log.info("grp append plan => meka={}, waza={}, sprite={}, batVoice={}",
                    result.getGrpAppendPlan().getMekaGroupIndex(),
                    result.getGrpAppendPlan().getWazaGroupIndex(),
                    result.getGrpAppendPlan().getSpriteGroupIndex(),
                    result.getGrpAppendPlan().getBatVoiceGroupIndex());
        }

        if (result.getExePatchPlan() != null) {
            log.info("exe patch => patched={}, requiredMekaCapacity={}, output={}",
                    result.getExePatchPlan().isPatched(),
                    result.getExePatchPlan().getRequiredMekaCapacity(),
                    result.getExePatchPlan().getOutputExePath());
            log.info("exe patch offsets => {}", result.getExePatchPlan().getTargetOffsets());
            log.info("exe patch notes => {}", result.getExePatchPlan().getNotes());
        }
    }

    private Path resolvePathProp(String key, Path defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Paths.get(raw.trim());
    }

    private String resolveStringProp(String key, String defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return raw.trim();
    }

    private boolean resolveBooleanProp(String key, boolean defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}
