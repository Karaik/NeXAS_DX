package com.giga.nexas.dto.clarias.grp.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.clarias.ClariasGenerator;
import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.generator.impl.*;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.io.BinaryWriter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/16
 */
public class GrpGenerator implements ClariasGenerator<Grp> {

    private final Map<String, GrpFileGenerator<Grp>> generatorMap = new HashMap<>();

    public GrpGenerator() {
        // 娉ㄥ唽瑙ｆ瀽鍣
        registerGenerator(new BatVoiceGrpGenerator());
        registerGenerator(new MapGroupGrpGenerator());
        registerGenerator(new MekaGroupGrpGenerator());
        registerGenerator(new ProgramMaterialGrpGenerator());
        registerGenerator(new SeGroupGrpGenerator());
        registerGenerator(new SpriteGroupGrpGenerator());
        registerGenerator(new TermGrpGenerator());
        registerGenerator(new WazaGroupGrpGenerator());
    }

    private void registerGenerator(GrpFileGenerator<Grp> Generator) {
        String key = getGeneratorKey(Generator);
        generatorMap.put(key.toLowerCase(), Generator);
    }

    private String getGeneratorKey(GrpFileGenerator<Grp> generator) {
        return generator.getGeneratorKey();
    }
    
    @Override
    public String supportExtension() {
        return "grp";
    }

    @Override
    public void generate(String path, Grp grp, String charset) throws IOException {
        GrpFileGenerator<Grp> matchedGenerator = generatorMap.get(grp.getFileName().toLowerCase());

        FileUtil.mkdir(FileUtil.getParent(path, 1));
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
             BinaryWriter writer = new BinaryWriter(os, charset)) {
            matchedGenerator.generate(writer, grp);
        }
    }
}

