package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TkyManualGapReferenceAuditTest {

    private static final String CHARSET = "windows-31j";
    private static final Path REPORT_JSON = Paths.get("src/main/java/com/giga/nexas/transfer/bhe2bsdx/tky.pac.report.json");
    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/testBhe/tsukuyomi");
    private static final Path STATIC_ROOT = Paths.get("D:/BDY/bsdx_bhe/bhe_resources");
    private static final Path DX_RE_SE_DIR = Paths.get("D:/Code/DX_re/se");
    private static final Path DX_RE_MISSING_DIR = Paths.get("D:/Code/DX_re/未找到的SE文件");
    private static final Path OUT_AUDIT_JSON = Paths.get("target/tky_manual_gap_reference_audit.json");

    @Test
    void auditOnlyInTkyReferencesAgainstCurrentOutput() throws Exception {
        Assumptions.assumeTrue(Files.isRegularFile(REPORT_JSON), "missing report: " + REPORT_JSON.toAbsolutePath());
        Assumptions.assumeTrue(Files.isDirectory(OUTPUT_DIR), "missing output dir: " + OUTPUT_DIR.toAbsolutePath());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(REPORT_JSON.toFile());
        ArrayNode onlyInTky = (ArrayNode) root.path("onlyInTky");
        Assumptions.assumeTrue(onlyInTky != null && !onlyInTky.isEmpty(), "no onlyInTky entries in report");

        BsdxBinService service = new BsdxBinService();
        RuntimeReferenceSnapshot snapshot = collectRuntimeReferences(service, OUTPUT_DIR);

        List<AuditRow> rows = new ArrayList<>();
        int mustFixCount = 0;
        int extraManualCount = 0;

        for (JsonNode node : onlyInTky) {
            String fileName = node.asText("");
            if (fileName.isBlank()) {
                continue;
            }
            AuditRow row = buildRow(fileName, snapshot);
            if (row.referencedByRuntime && !row.existsInTransferOutput) {
                row.classification = "MISSING_REQUIRED";
                mustFixCount++;
            } else if (row.referencedByRuntime) {
                row.classification = "ALREADY_PRESENT_IN_TRANSFER";
            } else {
                row.classification = "EXTRA_MANUAL_OR_UNUSED";
                extraManualCount++;
            }
            rows.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sourceReport", REPORT_JSON.toString());
        out.put("outputDir", OUTPUT_DIR.toString());
        out.put("totalOnlyInTky", rows.size());
        out.put("mustFixCount", mustFixCount);
        out.put("extraManualCount", extraManualCount);
        out.put("runtimeReferencedImageCount", snapshot.referencedImageNames.size());
        out.put("runtimeReferencedAudioCount", snapshot.runtimeAudioNames.size());
        out.put("allOutputSpmImageCount", snapshot.allSpmImageNames.size());
        out.put("rows", rows);

        Files.createDirectories(OUT_AUDIT_JSON.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(OUT_AUDIT_JSON.toFile(), out);

        assertEquals(rows.size(), onlyInTky.size(), "audit row size mismatch");
        assertTrue(Files.isRegularFile(OUT_AUDIT_JSON), "audit output not generated");
    }

    private AuditRow buildRow(String fileName, RuntimeReferenceSnapshot snapshot) {
        String normalizedFile = normalizeFileName(fileName);
        String normalizedBase = normalizeBaseName(fileName);
        String ext = extensionOf(fileName);

        boolean referencedByImage = snapshot.referencedImageNames.contains(normalizedFile)
                || snapshot.referencedImageNames.contains(normalizedBase);
        boolean referencedByAudio = snapshot.runtimeAudioNames.contains(normalizedFile)
                || snapshot.runtimeAudioNames.contains(normalizedBase);
        boolean referencedByAnySpm = snapshot.allSpmImageNames.contains(normalizedFile)
                || snapshot.allSpmImageNames.contains(normalizedBase);

        AuditRow row = new AuditRow();
        row.fileName = fileName;
        row.extension = ext;
        row.referencedByRuntime = referencedByImage || referencedByAudio;
        row.referencedByOutputSpm = referencedByImage;
        row.referencedByOutputGrp = referencedByAudio;
        row.appearsInAnyOutputSpmImageData = referencedByAnySpm;
        row.existsInTransferOutput = existsChild(OUTPUT_DIR, fileName);
        row.existsInStaticRoot = existsChild(STATIC_ROOT, fileName);
        row.existsInDxReSeDir = existsChild(DX_RE_SE_DIR, fileName);
        row.existsInDxReMissingDir = existsChild(DX_RE_MISSING_DIR, fileName);
        return row;
    }

    private RuntimeReferenceSnapshot collectRuntimeReferences(BsdxBinService service, Path outputDir) throws Exception {
        RuntimeReferenceSnapshot snapshot = new RuntimeReferenceSnapshot();
        Map<String, Spm> spmMap = new LinkedHashMap<>();
        Map<String, Waz> wazMap = new LinkedHashMap<>();
        SpriteGroupGrp spriteGroup = null;
        try (Stream<Path> stream = Files.list(outputDir)) {
            for (Path path : stream.toList()) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".spm")) {
                    Spm spm = parseSpm(service, path);
                    if (spm != null) {
                        spmMap.put(normalizeBaseName(name), spm);
                    }
                } else if (name.endsWith(".waz")) {
                    Waz waz = parseWaz(service, path);
                    if (waz != null) {
                        wazMap.put(normalizeBaseName(name), waz);
                    }
                } else if (name.endsWith(".grp")) {
                    Grp grp = parseGrp(service, path);
                    if (grp instanceof SeGroupGrp) {
                        collectSeNames((SeGroupGrp) grp, snapshot.runtimeAudioNames);
                    } else if (grp instanceof BatVoiceGrp) {
                        collectBatVoiceNames((BatVoiceGrp) grp, snapshot.runtimeAudioNames);
                    } else if (grp instanceof SpriteGroupGrp) {
                        spriteGroup = (SpriteGroupGrp) grp;
                    }
                }
            }
        }

        TransferDependencyCollector collector = new TransferDependencyCollector();
        Map<String, Set<Integer>> actionGroupMap = collector.collectSpmActionGroups(wazMap, spriteGroup);
        for (Map.Entry<String, Spm> entry : spmMap.entrySet()) {
            collectSpmImageRefs(entry.getValue(), snapshot, actionGroupMap.get(entry.getKey()));
        }
        return snapshot;
    }

    private Spm parseSpm(BsdxBinService service, Path path) throws Exception {
        ResponseDTO<?> dto = service.parse(path.toString(), CHARSET);
        if (dto == null || dto.getData() == null) {
            return null;
        }
        return (Spm) dto.getData();
    }

    private Grp parseGrp(BsdxBinService service, Path path) throws Exception {
        ResponseDTO<?> dto = service.parse(path.toString(), CHARSET);
        if (dto == null || dto.getData() == null) {
            return null;
        }
        return (Grp) dto.getData();
    }

    private Waz parseWaz(BsdxBinService service, Path path) throws Exception {
        ResponseDTO<?> dto = service.parse(path.toString(), CHARSET);
        if (dto == null || dto.getData() == null) {
            return null;
        }
        return (Waz) dto.getData();
    }

    private void collectSpmImageRefs(Spm spm, RuntimeReferenceSnapshot snapshot, Set<Integer> actionGroupSet) {
        if (spm.getImageData() != null) {
            for (Spm.SPMImageData imageData : spm.getImageData()) {
                if (imageData == null) {
                    continue;
                }
                String imageName = imageData.getImageName();
                if (imageName == null || imageName.isBlank()) {
                    continue;
                }
                snapshot.allSpmImageNames.add(normalizeFileName(imageName));
                snapshot.allSpmImageNames.add(normalizeBaseName(imageName));
            }
        }

        if (spm.getImageData() == null || spm.getImageData().isEmpty()) {
            return;
        }

        Set<Integer> pageNos = collectReferencedPageNos(spm, actionGroupSet);
        if (pageNos.isEmpty() && spm.getPageData() != null) {
            for (int i = 0; i < spm.getPageData().size(); i++) {
                pageNos.add(i);
            }
        }

        Set<Integer> imageNos = new LinkedHashSet<>();
        if (spm.getPageData() != null) {
            for (Integer pageNo : pageNos) {
                if (pageNo == null || pageNo < 0 || pageNo >= spm.getPageData().size()) {
                    continue;
                }
                Spm.SPMPageData pageData = spm.getPageData().get(pageNo);
                if (pageData == null || pageData.getChipData() == null) {
                    continue;
                }
                for (Spm.SPMChipData chipData : pageData.getChipData()) {
                    if (chipData == null || chipData.getImageNo() == null) {
                        continue;
                    }
                    if (chipData.getImageNo() >= 0) {
                        imageNos.add(chipData.getImageNo());
                    }
                }
            }
        }

        for (Integer imageNo : imageNos) {
            if (imageNo == null || imageNo < 0 || imageNo >= spm.getImageData().size()) {
                continue;
            }
            Spm.SPMImageData imageData = spm.getImageData().get(imageNo);
            if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                continue;
            }
            snapshot.referencedImageNames.add(normalizeFileName(imageData.getImageName()));
            snapshot.referencedImageNames.add(normalizeBaseName(imageData.getImageName()));
        }
    }

    private Set<Integer> collectReferencedPageNos(Spm spm, Set<Integer> actionGroupSet) {
        Set<Integer> pageNos = new LinkedHashSet<>();
        if (spm.getAnimData() == null) {
            return pageNos;
        }
        int patPageNum = spm.getPatPageNum() == null ? 0 : Math.max(spm.getPatPageNum(), 0);
        List<Spm.SPMAnimData> animDataList = spm.getAnimData();
        for (int animIndex = 0; animIndex < animDataList.size(); animIndex++) {
            if (actionGroupSet != null && !actionGroupSet.isEmpty() && !actionGroupSet.contains(animIndex)) {
                continue;
            }
            Spm.SPMAnimData animData = animDataList.get(animIndex);
            if (animData == null || animData.getPatData() == null) {
                continue;
            }
            for (Spm.SPMPatData patData : animData.getPatData()) {
                if (patData == null || patData.getPageNo() == null) {
                    continue;
                }
                int max = patPageNum > 0 ? Math.min(patPageNum, patData.getPageNo().size()) : patData.getPageNo().size();
                for (int i = 0; i < max; i++) {
                    Integer pageNo = patData.getPageNo().get(i);
                    if (pageNo != null && pageNo >= 0) {
                        pageNos.add(pageNo);
                    }
                }
            }
        }
        return pageNos;
    }

    private void collectSeNames(SeGroupGrp grp, Set<String> out) {
        if (grp == null || grp.getSeList() == null) {
            return;
        }
        for (SeGroupGrp.SeGroupGroup group : grp.getSeList()) {
            if (group == null || group.getSeItems() == null) {
                continue;
            }
            for (SeGroupGrp.SeGroupItem item : group.getSeItems()) {
                if (item == null || item.getSeFileName() == null || item.getSeFileName().isBlank()) {
                    continue;
                }
                out.add(normalizeFileName(item.getSeFileName()));
                out.add(normalizeBaseName(item.getSeFileName()));
            }
        }
    }

    private void collectBatVoiceNames(BatVoiceGrp grp, Set<String> out) {
        if (grp == null || grp.getVoiceList() == null) {
            return;
        }
        for (BatVoiceGrp.BatVoiceGroup group : grp.getVoiceList()) {
            if (group == null || group.getVoices() == null) {
                continue;
            }
            for (BatVoiceGrp.BatVoice voice : group.getVoices()) {
                if (voice == null || voice.getVoiceFileName() == null || voice.getVoiceFileName().isBlank()) {
                    continue;
                }
                out.add(normalizeFileName(voice.getVoiceFileName()));
                out.add(normalizeBaseName(voice.getVoiceFileName()));
            }
        }
    }

    private boolean existsChild(Path dir, String fileName) {
        if (dir == null || fileName == null || fileName.isBlank() || !Files.isDirectory(dir)) {
            return false;
        }
        return Files.exists(dir.resolve(fileName));
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        String fixed = fileName.trim().replace("\\", "/");
        int slash = fixed.lastIndexOf('/');
        String name = slash >= 0 ? fixed.substring(slash + 1) : fixed;
        return name.toLowerCase(Locale.ROOT);
    }

    private String normalizeBaseName(String fileName) {
        String name = normalizeFileName(fileName);
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            return name.substring(0, dot);
        }
        return name;
    }

    private String extensionOf(String fileName) {
        String name = normalizeFileName(fileName);
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot + 1 < name.length()) {
            return name.substring(dot + 1);
        }
        return "";
    }

    private static final class RuntimeReferenceSnapshot {
        private final Set<String> referencedImageNames = new LinkedHashSet<>();
        private final Set<String> allSpmImageNames = new LinkedHashSet<>();
        private final Set<String> runtimeAudioNames = new LinkedHashSet<>();
    }

    private static final class AuditRow {
        public String fileName;
        public String extension;
        public boolean referencedByRuntime;
        public boolean referencedByOutputSpm;
        public boolean referencedByOutputGrp;
        public boolean appearsInAnyOutputSpmImageData;
        public boolean existsInTransferOutput;
        public boolean existsInStaticRoot;
        public boolean existsInDxReSeDir;
        public boolean existsInDxReMissingDir;
        public String classification;
    }
}
