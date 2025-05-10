package com.giga.nexas.dto.bsdx.grp.parser;

import com.giga.nexas.dto.bsdx.BsdxParser;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.parser.impl.*;
import com.giga.nexas.exception.BusinessException;
import com.giga.nexas.io.BinaryReader;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/2
 * @Description GroupMapParser
 */
public class GrpParser implements BsdxParser<Grp> {

    private final Map<String, GrpFileParser<? extends Grp>> parserMap = new HashMap<>();

    public GrpParser() {
        // 注册解析器
        registerParser(new BatVoiceGrpParser());
//        registerParser(new MapGroupGrpParser());
        registerParser(new MekaGroupGrpParser());
//        registerParser(new ProgramMaterialGrpParser());
        registerParser(new SeGroupGrpParser());
        registerParser(new SpriteGroupGrpParser());
        registerParser(new TermGrpParser());
        registerParser(new WazaGroupGrpParser());
    }

    private void registerParser(GrpFileParser<? extends Grp> parser) {
        String key = getParserKey(parser);
        parserMap.put(key, parser);
    }

    private String getParserKey(GrpFileParser<?> parser) {
        return parser.getParserKey();
    }

    @Override
    public String supportExtension() {
        return "grp";
    }

    @Override
    public Grp parse(byte[] data, String filename, String charset) {
        GrpFileParser<? extends Grp> matchedParser = parserMap.get(filename);
        if (matchedParser == null) {
            throw new BusinessException(500, "不支持的grp子文件(请注意文件大小写)：" + filename);
        }

        BinaryReader reader = new BinaryReader(data);
        reader.setCharset(charset);
        Grp grp = matchedParser.parse(reader);
        grp.setFileName(filename);
        return grp;
    }
}