package com.giga.nexas.dto.clarias.grp.parser;

import com.giga.nexas.dto.clarias.ClariasParser;
import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.parser.impl.*;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.io.BinaryReader;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/2/2
 * @Description GroupMapParser
 */
public class GrpParser implements ClariasParser<Grp> {

    private final Map<String, GrpFileParser<? extends Grp>> parserMap = new HashMap<>();

    public GrpParser() {
        // 娉ㄥ唽瑙ｆ瀽鍣?
        registerParser(new BatVoiceGrpParser());
        registerParser(new MapGroupGrpParser());
        registerParser(new MekaGroupGrpParser());
        registerParser(new ProgramMaterialGrpParser());
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
            throw new OperationException(500, "unsupported .grp file for parsing: " + filename);
        }

        BinaryReader reader = new BinaryReader(data);
        reader.setCharset(charset);
        Grp grp = matchedParser.parse(reader);
        grp.setFileName(filename);
        return grp;
    }
}
