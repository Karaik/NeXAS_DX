package com.giga.nexas.bhe2bsdx;

import com.giga.nexas.bhe2bsdx.steps.TransMeka;
import com.giga.nexas.bhe2bsdx.steps.TransMekaOutputWriter;
import com.giga.nexas.bhe2bsdx.steps.TransMekaResult;
import com.giga.nexas.bhe2bsdx.steps.StaticAssetCopier;
import com.giga.nexas.bhe2bsdx.steps.WazConverter;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxBatchRunner;
import com.giga.nexas.transfer.bhe2bsdx.Bhe2BsdxConfig;

import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.util.PacUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransferTest {

    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/testBhe");
    // 闂堟瑦鈧浇绁┃鎰降濠ф劗娲拌ぐ鏇礄閹稿娓舵穱顔芥暭閿?
    private static final Path STATIC_ASSET_ROOT = Paths.get("D:\\BDY\\bsdx_bhe\\bhe_resources");
    private static final boolean COPY_STATIC_ASSETS = true;
    // 閼奉亜濮╅崣鎴犲箛濠ф劖婧€娴ｆ挻妞傛担璺ㄦ暏閿涘牊鐗撮幑?mekBheJson 閻╊喖缍嶉崥宥忕礆
    private static final Path BHE_MEK_JSON_DIR = Paths.get("src/main/resources/mekBheJson");

    // ===== 閸欘垶鍘ょ純顕€銆嶉敍鍫熷Ω閸樼喎鍘涢崘娆愵劥閻?tsukuyomi 閻╃鍙ч柊宥囩枂閹割亜鍩屾潻娆撳櫡閿?=====
    // 濠ф劖婧€娴ｆ搫绱版俊鍌涚亯鏉╂瑩鍣锋稉铏光敄閿涘奔绱伴懛顏勫З閺嶈宓?mekBheJson 閻╊喖缍嶉崥宥呭絺閻滄澘鍙忛柈銊︽簚娴?
    private static final List<MekaSource> SOURCE_MEKA_LIST = List.of();
    // 閻╊喗鐖ｅΣ鎴掔秴閿涘牓绮拋銈嗘禌閹?Nanoha閿?
    private static final String TARGET_KEY = "nanoha";
    private static final String TARGET_CODE_NAME = "NANOHA";
    private static final boolean KEEP_TARGET_KEY = true;

    private static final Logger log = LoggerFactory.getLogger(TransferTest.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BheBinService bheBinService = new BheBinService();

    /**
     * 鐠嬪啰鏁ゅ锝呯础閹电懓顦╅悶鍡椾紣閸忓嚖绱檚rc/main 娑撳娈?Bhe2BsdxBatchRunner閿涘鈧?
     * 娴ｈ法鏁ゆ妯款吇闁板秶鐤嗛敍灞炬暜閹?-Dtransfer.sources / -Dtransfer.limit 鏉╁洦鎶ら妴?
     */
    @Test
    public void testBatchRunner() throws Exception {
        Bhe2BsdxConfig config = Bhe2BsdxConfig.defaults();
        new Bhe2BsdxBatchRunner(config).run();
    }

    /**
     * 缁夌粯顦查悽顭掔礉pipeline濡剝瀚?
     */
    @Test
    public void testPipeline() throws Exception {

        List<MekaSource> sources = SOURCE_MEKA_LIST;
        if (sources == null || sources.isEmpty()) {
            sources = discoverSourcesFromMekJson();
        }
        sources = applyRuntimeFilters(sources);

        if (sources == null || sources.isEmpty()) {
            log.warn("No available source meka found, skip transfer.");
            return;
        }

        log.info("閺堫剚顐兼潪顒佸床濠ф劖婧€娴ｆ挻鏆熼柌? {}", sources.size());
        for (MekaSource source : sources) {
            runSingleSourcePipeline(source);
        }
    }

    private List<MekaSource> discoverSourcesFromMekJson() throws IOException {
        List<MekaSource> sources = new ArrayList<>();
        if (!Files.isDirectory(BHE_MEK_JSON_DIR)) {
            log.warn("mekBheJson 閻╊喖缍嶆稉宥呯摠閸? {}", BHE_MEK_JSON_DIR.toAbsolutePath());
            return sources;
        }

        Map<String, String> codeNameMap = loadBheCodeNameMap();
        final String suffix = ".mek.json";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_MEK_JSON_DIR, "*" + suffix)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(suffix)) {
                    continue;
                }
                String baseKey = normalizeKey(fileName.substring(0, fileName.length() - suffix.length()));
                if (baseKey.isEmpty()) {
                    continue;
                }

                String codeName = codeNameMap.get(baseKey);
                if (codeName == null || codeName.isBlank()) {
                    log.warn("鐠哄疇绻冮張顏勫爱闁?codeName 閻ㄥ嫭婧€娴? baseKey={}", baseKey);
                    continue;
                }
                sources.add(new MekaSource(baseKey, codeName));
            }
        }

        sources.sort(Comparator.comparing(s -> s.baseKey));
        log.info("閼奉亜濮╅崣鎴犲箛濠ф劖婧€娴? {} 娑?(閺夈儴鍤?{})", sources.size(), BHE_MEK_JSON_DIR);
        return sources;
    }

    private List<MekaSource> applyRuntimeFilters(List<MekaSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return sources;
        }

        List<MekaSource> filtered = sources;

        String filterProp = System.getProperty("transfer.sources");
        if (filterProp != null && !filterProp.isBlank()) {
            Set<String> allowList = new LinkedHashSet<>();
            for (String token : filterProp.split(",")) {
                String key = normalizeKey(token);
                if (!key.isEmpty()) {
                    allowList.add(key);
                }
            }
            if (!allowList.isEmpty()) {
                filtered = new ArrayList<>();
                for (MekaSource source : sources) {
                    if (source != null && allowList.contains(source.baseKey)) {
                        filtered.add(source);
                    }
                }
                log.info("Applied transfer.sources filter: {}, remaining={}", allowList, filtered.size());
            }
        }

        String limitProp = System.getProperty("transfer.limit");
        if (limitProp != null && !limitProp.isBlank()) {
            try {
                int limit = Integer.parseInt(limitProp.trim());
                if (limit > 0 && filtered.size() > limit) {
                    filtered = new ArrayList<>(filtered.subList(0, limit));
                    log.info("Applied transfer.limit filter: {}, remaining={}", limit, filtered.size());
                }
            } catch (NumberFormatException e) {
                log.warn("transfer.limit 娑撳秵妲搁張澶嬫櫏閺佸瓨鏆? {}", limitProp);
            }
        }

        return filtered;
    }

    private Map<String, String> loadBheCodeNameMap() throws IOException {
        Map<String, String> codeNameMap = new HashMap<>();

        Map<String, com.giga.nexas.dto.bhe.grp.Grp> bheGrp = registerBheGrp();
        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp bheMekaGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp) bheGrp.get("mekagroup");
        if (bheMekaGroup == null || bheMekaGroup.getMekaList() == null) {
            log.warn("BHE MekaGroup is missing, fallback source discovery by codeName map skipped.");
            return codeNameMap;
        }

        for (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup group : bheMekaGroup.getMekaList()) {
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }
            String mekaName = group.getMekaName();
            String mekaCodeName = group.getMekaCodeName();
            if (mekaName == null || mekaCodeName == null) {
                continue;
            }
            String baseKey = normalizeKey(mekaName);
            String codeName = normalizeCode(mekaCodeName);
            if (!baseKey.isEmpty() && !codeName.isEmpty()) {
                codeNameMap.put(baseKey, codeName);
            }
        }

        log.info("Discovered BHE codeName map entries: {}", codeNameMap.size());
        return codeNameMap;
    }

    @Test
    public void testTransSingle() throws Exception {
        Path resourceDir = Paths.get("src/main/resources");
        String bheWazJsonName = "tkytama.waz.json";
        Path bheWazJsonPath = resourceDir.resolve(bheWazJsonName);

        // 1. 鐠囪褰?BHE WAZ JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String jsonStr = Files.readString(bheWazJsonPath);
        com.giga.nexas.dto.bhe.waz.Waz bheWaz = mapper.readValue(jsonStr, com.giga.nexas.dto.bhe.waz.Waz.class);
        log.info("閴?BHE WAZ loaded: {}, skills={}", bheWaz.getFileName(), bheWaz.getSkillList().size());

        // 2. 鏉烆剚宕叉稉?BSDX WAZ
        WazConverter wazConverter = new WazConverter();
        com.giga.nexas.dto.bsdx.waz.Waz bsdxWaz = wazConverter.convert(bheWaz);
        bsdxWaz.setFileName(bheWaz.getFileName());
        bsdxWaz.setExtensionName("waz");
        log.info("閴?BSDX WAZ converted: {}, skills={}", bsdxWaz.getFileName(), bsdxWaz.getSkillList().size());

        // 3. 鏉堟挸鍤崚鏉挎倱娑撯偓閻╊喖缍?
        String outputName = bheWazJsonName.replace(".waz.json", ".bsdx.waz.json");
        Path outputPath = resourceDir.resolve(outputName);
        String outputJson = cn.hutool.json.JSONUtil.toJsonPrettyStr(bsdxWaz);
        Files.writeString(outputPath, outputJson);
        log.info("閴?BSDX WAZ JSON written: {}", outputPath);

        // 4. 妤犲矁鐦夐敍姘辨暏 BSDX 鐟欙絾鐎介崳銊嚢閸欐牜鏁撻幋鎰畱 JSON
        String bsdxJsonStr = Files.readString(outputPath);
        com.giga.nexas.dto.bsdx.waz.Waz verifyWaz = mapper.readValue(bsdxJsonStr, com.giga.nexas.dto.bsdx.waz.Waz.class);
        log.info("閴?BSDX WAZ verified: {}, skills={}", verifyWaz.getFileName(), verifyWaz.getSkillList().size());
    }

    private String resolveSpriteBaseName(
            com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp spriteGroup,
            com.giga.nexas.dto.bsdx.mek.Mek targetMek,
            String fallback
    ) {
        if (spriteGroup == null || targetMek == null || targetMek.getMekBasicInfo() == null) {
            return fallback;
        }
        Integer index = targetMek.getMekBasicInfo().getSpmFileSequence();
        if (index == null || spriteGroup.getSpriteList() == null
                || index < 0 || index >= spriteGroup.getSpriteList().size()) {
            return fallback;
        }
        com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry =
                spriteGroup.getSpriteList().get(index);
        if (entry == null || entry.getSpriteFileName() == null) {
            return fallback;
        }
        String fileName = entry.getSpriteFileName().trim();
        if (fileName.isEmpty()) {
            return fallback;
        }
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
        return baseName.isEmpty() ? fallback : baseName;
    }

    private void putIfPresent(Map<String, com.giga.nexas.dto.bsdx.spm.Spm> map, String key,
                              com.giga.nexas.dto.bsdx.spm.Spm value) {
        if (map == null || key == null || value == null) {
            return;
        }
        map.put(key, value);
    }

    // grp
    private static final Path BSDX_GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");
    private static final Path BHE_GRP_DIR = Paths.get("src/main/resources/game/bhe/grp");
    private Map<String, com.giga.nexas.dto.bsdx.grp.Grp> registerBsdxGrp() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> grpMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.grp.Grp grp = (com.giga.nexas.dto.bsdx.grp.Grp) dto.getData();
                    grpMap.put(baseName, grp);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bsdxGrp: {}", fileName);
                }
            }
        }

        return grpMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.grp.Grp> registerBheGrp() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.grp.Grp> grpMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.grp.Grp grp = (com.giga.nexas.dto.bhe.grp.Grp) dto.getData();
                    grpMap.put(baseName, grp);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bheGrp: {}", fileName);
                }
            }
        }

        return grpMap;
    }

    // mek
    private static final Path BSDX_MEK_DIR = Paths.get("src/main/resources/game/bsdx/mek");
    private static final Path BHE_MEK_DIR = Paths.get("src/main/resources/game/bhe/mek");
    private Map<String, com.giga.nexas.dto.bsdx.mek.Mek> registerBsdxMek() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> mekMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.mek.Mek mek = (com.giga.nexas.dto.bsdx.mek.Mek) dto.getData();
                    mekMap.put(baseName, mek);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bsdxMek: {}", fileName);
                }
            }
        }

        return mekMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.mek.Mek> registerBheMek() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.mek.Mek> mekMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.mek.Mek mek = (com.giga.nexas.dto.bhe.mek.Mek) dto.getData();
                    mekMap.put(baseName, mek);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bheMek: {}", fileName);
                }
            }
        }

        return mekMap;
    }

    // waz
    private static final Path BSDX_WAZ_DIR = Paths.get("src/main/resources/game/bsdx/waz");
    private static final Path BHE_WAZ_DIR = Paths.get("src/main/resources/game/bhe/waz");
    private Map<String, com.giga.nexas.dto.bsdx.waz.Waz> registerBsdxWaz() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> wazMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_WAZ_DIR, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.waz.Waz waz = (com.giga.nexas.dto.bsdx.waz.Waz) dto.getData();
                    wazMap.put(baseName, waz);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bsdxWaz: {}", fileName);
                }
            }
        }

        return wazMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.waz.Waz> registerBheWaz() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> wazMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_WAZ_DIR, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.waz.Waz waz = (com.giga.nexas.dto.bhe.waz.Waz) dto.getData();
                    wazMap.put(baseName, waz);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bheWaz: {}", fileName);
                }
            }
        }

        return wazMap;
    }

    // spm
    // spm閸︺劌顦挎稉顏嗗閺堫兛鑵戦弮鐘叉▕瀵偊绱濇担鍡楀嚒缂佸繒鈥樼€?.0.0閸︹暈he娑擃厼顦挎禍鍡楀彠娴滃穻itbox閻ㄥ嫪淇婇幁顖ょ礉閸欐ê绶遍弴鏉戭槻閺夊倷绨?
    private static final Path BSDX_SPM_DIR = Paths.get("src/main/resources/game/bsdx/spm");
    private static final Path BHE_SPM_DIR = Paths.get("src/main/resources/game/bhe/spm");
    private Map<String, com.giga.nexas.dto.bsdx.spm.Spm> registerBsdxSpm() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> spmMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_SPM_DIR, "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.spm.Spm spm = (com.giga.nexas.dto.bsdx.spm.Spm) dto.getData();
                    spmMap.put(baseName, spm);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bsdxSpm: {}", fileName);
                }
            }
        }

        return spmMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.spm.Spm> registerBheSpm() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.spm.Spm> spmMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_SPM_DIR, "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.spm.Spm spm = (com.giga.nexas.dto.bhe.spm.Spm) dto.getData();
                    spmMap.put(baseName, spm);
                } catch (Exception e) {
                    log.warn("閴?Failed to parse bheSpm: {}", fileName);
                }
            }
        }

        return spmMap;
    }

    // dat
    // dat閺冪姴妯婇崚顐礉閸忋劋璐焎sv
    private static final Path BSDX_DAT_DIR = Paths.get("src/main/resources/game/bsdx/dat");
    private Dat loadBsdxDat(String fileName) throws IOException {
        Path path = BSDX_DAT_DIR.resolve(fileName);
        try {
            ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
            return (Dat) dto.getData();
        } catch (Exception e) {
            log.warn("閴?Failed to parse bsdxDat: {}", fileName);
            return null;
        }
    }

    @Test
    void outputClassName() {
        HashMap<String, Object> classMapBsdx = new HashMap<>();
        for (int i = 0; i < 72; i++) {
            com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject obj =
                    com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory.createEventObjectBsdx(i);
            log.info("{}", obj.getClass().getSimpleName());
            classMapBsdx.put(obj.getClass().getSimpleName(),null);
        }

        log.info("==========");

        for (int i = 0; i < 83; i++) {
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject obj =
                    com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory.createEventObjectBhe(i);
            log.info("{}", obj.getClass().getSimpleName());

        }

        log.info("==========");
        log.info("==========");
        log.info("==========");

        classMapBsdx.forEach((k, v) -> {log.info("{}", k);});

    }

    private void runSingleSourcePipeline(MekaSource source) throws Exception {
        if (source == null) {
            return;
        }

        String baseKey = normalizeKey(source.baseKey);
        String codeName = normalizeCode(source.codeName);
        if (baseKey.isEmpty() || codeName.isEmpty()) {
            log.warn("濠ф劖婧€娴ｆ捇鍘ょ純顔芥￥閺? baseKey={}, codeName={}", source.baseKey, source.codeName);
            return;
        }

        String cKey = variantKey("c_", baseKey);
        String sKey = variantKey("s_", baseKey);
        String gKey = variantKey("g_", baseKey);
        String mKey = variantKey("m_", baseKey);

        Path outputDir = OUTPUT_DIR.resolve(baseKey);
        String outputPath = outputDir.toAbsolutePath().toString();

        log.info("========== 瀵偓婵娴嗛幑? baseKey={}, codeName={}, outputDir={} ==========",
                baseKey, codeName, outputDir);

        // 1.濞夈劌鍞介崗銊╁劥閹碘偓闂団偓閺傚洣娆㈢挧鍕爱閿涘牊鐦℃稉顏呯爱閺堣桨缍嬮柈浠嬪櫢閺傜増鏁為崘宀嬬礉娣囨繆鐦夐崺铏瑰殠楠炴彃鍣ｉ敍?
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> bsdxGrp = registerBsdxGrp();
        Map<String, com.giga.nexas.dto.bhe.grp.Grp> bheGrp = registerBheGrp();
        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> bsdxMek = registerBsdxMek();
        Map<String, com.giga.nexas.dto.bhe.mek.Mek> bheMek = registerBheMek();
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> bsdxWaz = registerBsdxWaz();
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> bheWaz = registerBheWaz();
        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> bsdxSpm = registerBsdxSpm();
        Map<String, com.giga.nexas.dto.bhe.spm.Spm> bheSpm = registerBheSpm();
        Dat selectMekaMenuDat = loadBsdxDat("SelectMekaMenu.dat");

        // 2.閹惰棄鍤粔缁橆槻閻╊喗鐖?
        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp bheBatVoice =
                (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp) bheGrp.get("batvoice");
        com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp bsdxBatVoice =
                (com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp) bsdxGrp.get("batvoice");
        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup sourceBatvoice =
                findBatVoiceGroupByCode(bheBatVoice, codeName);

        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp bheMekaGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp) bheGrp.get("mekagroup");
        com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp bheWazaGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp) bheGrp.get("wazagroup");
        com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheSpriteGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp) bheGrp.get("spritegroup");
        com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp bheSeGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp) bheGrp.get("segroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp bsdxMekaGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp) bsdxGrp.get("mekagroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp bsdxWazaGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp) bsdxGrp.get("wazagroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp bsdxSpriteGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp) bsdxGrp.get("spritegroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp bsdxSeGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp) bsdxGrp.get("segroup");

        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup sourceMekaGroup =
                findMekaGroupByCode(bheMekaGroup, codeName);
        com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry sourceWazaGroup =
                findWazaGroupByCode(bheWazaGroup, codeName);
        com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry sourceSpriteGroup =
                findSpriteGroupByCode(bheSpriteGroup, codeName);

        com.giga.nexas.dto.bhe.mek.Mek sourceMek = bheMek.get(baseKey);
        com.giga.nexas.dto.bhe.waz.Waz sourceWaz = bheWaz.get(baseKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceSpm = bheSpm.get(baseKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceCSpm = bheSpm.get(cKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceSSpm = bheSpm.get(sKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceGSpm = bheSpm.get(gKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceMSpm = bheSpm.get(mKey);

        if (sourceMek == null || sourceWaz == null || sourceSpm == null) {
            log.warn("濠ф劘绁┃鎰瑝鐎瑰本鏆?baseKey={}): mek={}, waz={}, spm={}",
                    baseKey, sourceMek != null, sourceWaz != null, sourceSpm != null);
            return;
        }

        com.giga.nexas.dto.bsdx.spm.Spm mekaPilotSpm = bsdxSpm.get("mekapilot");
        com.giga.nexas.dto.bsdx.spm.Spm selectMekaMenuMekaSpm = bsdxSpm.get("selectmekamenumeka");
        com.giga.nexas.dto.bsdx.mek.Mek targetBsdxMek = bsdxMek.get(TARGET_KEY);
        boolean useTargetSlot = targetBsdxMek != null;
        String targetSpriteKey = useTargetSlot
                ? resolveSpriteBaseName(bsdxSpriteGroup, targetBsdxMek, TARGET_KEY)
                : baseKey;

        TransMekaResult result = TransMeka.process(
                sourceMek,
                sourceWaz,
                sourceSpm,
                sourceCSpm,
                sourceSSpm,
                sourceGSpm,
                sourceMSpm,

                sourceBatvoice,
                bsdxBatVoice,

                sourceMekaGroup,
                sourceWazaGroup,
                sourceSpriteGroup,
                bheSpriteGroup,
                bheSeGroup,
                bsdxMekaGroup,
                bsdxWazaGroup,
                bsdxSpriteGroup,
                bsdxSeGroup,
                bsdxWaz,

                mekaPilotSpm,
                selectMekaMenuMekaSpm,
                selectMekaMenuDat,
                targetBsdxMek,
                TARGET_CODE_NAME,
                KEEP_TARGET_KEY);

        // 3.閸ョ偛鍟撻崚?BSDX Map閿涘牅绻氶幐浣稿敶鐎涙ü绔撮懛杈剧礆閿涘苯鎮撻弮璺哄涧鏉堟挸鍤張顒侇偧閸欐ɑ娲块敍宀勪缉閸忓秴鍟撻崙鍝勫弿闁?spm
        if (result != null) {
            if (result.getBsdxMeka() != null) {
                if (useTargetSlot) {
                    result.getBsdxMeka().setFileName(TARGET_KEY);
                }
                bsdxMek.put(useTargetSlot ? TARGET_KEY : baseKey, result.getBsdxMeka());
            }
            if (result.getBsdxWaz() != null) {
                if (useTargetSlot) {
                    result.getBsdxWaz().setFileName(TARGET_KEY);
                }
                bsdxWaz.put(useTargetSlot ? TARGET_KEY : baseKey, result.getBsdxWaz());
            }
            if (result.getBsdxSpm() != null) {
                bsdxSpm.put(useTargetSlot ? targetSpriteKey : baseKey, result.getBsdxSpm());
            }
            if (!useTargetSlot && result.getBsdxCSpm() != null) {
                bsdxSpm.put(cKey, result.getBsdxCSpm());
            }
            if (!useTargetSlot && result.getBsdxSSpm() != null) {
                bsdxSpm.put(sKey, result.getBsdxSSpm());
            }
            if (!useTargetSlot && result.getBsdxGSpm() != null) {
                bsdxSpm.put(gKey, result.getBsdxGSpm());
            }
            if (!useTargetSlot && result.getBsdxMSpm() != null) {
                bsdxSpm.put(mKey, result.getBsdxMSpm());
            }
            if (result.getBsdxMekaPilotSpm() != null) {
                bsdxSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
            }
            if (result.getBsdxSelectMekaMenuMekaSpm() != null) {
                bsdxSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
            }
        }

        // 4.鏉堟挸鍤崣妯绘纯閺傚洣娆㈤敍鍧搑p/mek/waz/spm閿?
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> outputGrp = new HashMap<>();
        outputGrp.put("batvoice", bsdxBatVoice);
        outputGrp.put("mekagroup", bsdxMekaGroup);
        outputGrp.put("wazagroup", bsdxWazaGroup);
        outputGrp.put("spritegroup", bsdxSpriteGroup);
        if (bsdxSeGroup != null) {
            outputGrp.put("segroup", bsdxSeGroup);
        }

        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> outputMek = new HashMap<>();
        if (result != null && result.getBsdxMeka() != null) {
            outputMek.put(useTargetSlot ? TARGET_KEY : baseKey, result.getBsdxMeka());
        }

        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> outputWaz = new HashMap<>();
        if (result != null && result.getBsdxWaz() != null) {
            outputWaz.put(useTargetSlot ? TARGET_KEY : baseKey, result.getBsdxWaz());
        }

        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> outputSpm = new HashMap<>();
        if (result != null && result.getBsdxSpm() != null) {
            outputSpm.put(useTargetSlot ? targetSpriteKey : baseKey, result.getBsdxSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxCSpm() != null) {
            outputSpm.put(cKey, result.getBsdxCSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxSSpm() != null) {
            outputSpm.put(sKey, result.getBsdxSSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxGSpm() != null) {
            outputSpm.put(gKey, result.getBsdxGSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxMSpm() != null) {
            outputSpm.put(mKey, result.getBsdxMSpm());
        }
        if (result != null && result.getBsdxMekaPilotSpm() != null) {
            outputSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
        }
        if (result != null && result.getBsdxSelectMekaMenuMekaSpm() != null) {
            outputSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
        }

        TransMekaOutputWriter outputWriter = new TransMekaOutputWriter();
        outputWriter.writeOutputs(outputDir, outputGrp, outputMek, outputWaz, outputSpm);

        if (COPY_STATIC_ASSETS) {
            StaticAssetCopier assetCopier = new StaticAssetCopier();
            Map<String, com.giga.nexas.dto.bsdx.grp.Grp> assetGrp = new HashMap<>();
            if (result != null && result.getBsdxBatVoiceGroup() != null) {
                BatVoiceGrp batVoiceGrp = new BatVoiceGrp();
                batVoiceGrp.getVoiceList().add(result.getBsdxBatVoiceGroup());
                assetGrp.put("batvoice", batVoiceGrp);
            }
            // 娴犲懎顦查崚?BHE 閺夈儲绨惃?spm 闂堟瑦鈧浇绁┃鎰剁礉闁灝鍘ら幖婊呭偍 BSDX 閼奉亜鐢崶鍓у
            Map<String, com.giga.nexas.dto.bsdx.spm.Spm> assetSpm = new HashMap<>();
            if (result != null) {
                putIfPresent(assetSpm, baseKey, result.getBsdxSpm());
                putIfPresent(assetSpm, cKey, result.getBsdxCSpm());
                putIfPresent(assetSpm, sKey, result.getBsdxSSpm());
                putIfPresent(assetSpm, gKey, result.getBsdxGSpm());
                putIfPresent(assetSpm, mKey, result.getBsdxMSpm());
            }
            assetCopier.copyAssets(outputDir, STATIC_ASSET_ROOT, assetSpm, assetGrp);
        }

        // 閹垫挸瀵?
        String packLog = PacUtil.pack(outputPath, "4");
        log.info("outputPath === {}", packLog);

        // 缂佺喍绔存潏鎾冲毉閸栧懎鎮曢敍?outputDir>.pacNew -> Update3_<baseKey>.pac
        Path pacNew = outputDir.resolveSibling(outputDir.getFileName().toString() + ".pacNew");
        Path updatePac = outputDir.resolveSibling("Update3_" + baseKey + ".pac");
        if (Files.exists(pacNew)) {
            Files.move(pacNew, updatePac, StandardCopyOption.REPLACE_EXISTING);
            log.info("閴?pac renamed: {} -> {}", pacNew.getFileName(), updatePac.getFileName());
        } else {
            log.warn("閳跨媴绗?pac not found: {}", pacNew);
        }
    }

    private com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup findBatVoiceGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getVoiceList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup group : grp.getVoiceList()) {
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }
            String code = group.getCharacterCodeName();
            if (code != null && codeName.equalsIgnoreCase(code.trim())) {
                return group;
            }
        }
        log.warn("閺堫亝澹橀崚?BatVoiceGroup: codeName={}", codeName);
        return null;
    }

    private com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup findMekaGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getMekaList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup group : grp.getMekaList()) {
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }
            if (codeName.equalsIgnoreCase(group.getMekaCodeName())) {
                return group;
            }
        }
        log.warn("閺堫亝澹橀崚?MekaGroup: codeName={}", codeName);
        return null;
    }

    private com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry findWazaGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getWazaList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry entry : grp.getWazaList()) {
            if (entry == null || entry.getExistFlag() == null || entry.getExistFlag() == 0) {
                continue;
            }
            if (codeName.equalsIgnoreCase(entry.getWazaCodeName())) {
                return entry;
            }
        }
        log.warn("閺堫亝澹橀崚?WazaGroup: codeName={}", codeName);
        return null;
    }

    private com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry findSpriteGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getSpriteList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry : grp.getSpriteList()) {
            if (entry == null || entry.getExistFlag() == null || entry.getExistFlag() == 0) {
                continue;
            }
            if (codeName.equalsIgnoreCase(entry.getSpriteCodeName())) {
                return entry;
            }
        }
        log.warn("閺堫亝澹橀崚?SpriteGroup: codeName={}", codeName);
        return null;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private String normalizeCode(String codeName) {
        return codeName == null ? "" : codeName.trim().toUpperCase();
    }

    private String variantKey(String prefix, String baseKey) {
        if (prefix == null || baseKey == null || baseKey.isEmpty()) {
            return baseKey;
        }
        return prefix + baseKey;
    }

    private static final class MekaSource {
        private final String baseKey;
        private final String codeName;

        private MekaSource(String baseKey, String codeName) {
            this.baseKey = baseKey;
            this.codeName = codeName;
        }
    }

}

