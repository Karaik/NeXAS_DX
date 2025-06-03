package com.giga.nexas.bsdx;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTransMekaInfo {

    private static final Logger log = LoggerFactory.getLogger(TestTransMekaInfo.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private static final Path MEK_DIR = Paths.get("src/main/resources/game/bsdx/mek");
    private static final Path GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");

    private static final Path AI_EXCEL_DIR = Paths.get("src/main/resources/mekaExcel");
    private static final Path AI_CSV_DIR = Paths.get("src/main/resources/mekaCsv");

    public static final Map<Integer, String> MEK_GRP  = new HashMap<>(1<<7,1);

    @Test
    void testTransMekBasicInfo() throws IOException {
        register();

        List<Mek> allMek = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                try {
                    ResponseDTO dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    allMek.add((Mek) dto.getData());
                } catch (Exception ignored) {
                    continue;
                }
            }
        }

        parseMeka(allMek);

    }

    private void parseMeka(List<Mek> mekList) throws IOException {
        List<String> headers = null;
        List<List<String>> allRows = new ArrayList<>();

        for (Mek mek : mekList) {
            Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();
            if (mekBasicInfo == null) {
                continue;
            }

            MekaBasicInfoDto dto = new MekaBasicInfoDto(mekBasicInfo);

            if (headers == null) {
                headers = dto.outputHead();
                allRows.add(headers);
            }

            allRows.add(dto.outputData());
        }

        Path outDir = AI_EXCEL_DIR;
        Files.createDirectories(outDir);
        Path outPath = outDir.resolve("ALL_MEK_INFO.xlsx");
        log.info("Excel path: {}", outPath.toAbsolutePath());
        ExcelWriter writer = ExcelUtil.getWriter(outPath.toFile());
        writer.getSheet().createFreezePane(0, 1);
        writer.write(allRows, true);
        writer.close();

        Path csvOutDir = AI_CSV_DIR;
        Files.createDirectories(csvOutDir);
        Path csvPath = csvOutDir.resolve("ALL_MEK_INFO.csv");
        log.info("CSV path: {}", csvPath.toAbsolutePath());
        CsvWriter csvWriter = CsvUtil.getWriter(csvPath.toFile(), CharsetUtil.CHARSET_UTF_8);
        for (List<String> row : allRows) {
            csvWriter.write(row.toArray(new String[0]));
        }
        csvWriter.close();
    }

    private String nvl(String val) {
        return val == null ? "" : val;
    }

    private void register() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    Grp grp = (Grp) dto.getData();
                    if (grp instanceof MekaGroupGrp) {
                        registerMek(grp);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse: {}", fileName);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerMek(Grp grp) {
        MekaGroupGrp mekaGroupGrp = (MekaGroupGrp) grp;
        for (int i = 0; i < mekaGroupGrp.getMekaList().size(); i++) {
            MEK_GRP.put(i, mekaGroupGrp.getMekaList().get(i).getMekaName());
        }
        MEK_GRP.put(110, "Akao"); // 没有，所以手动注册
    }

}
