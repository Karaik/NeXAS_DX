package com.giga.nexas.dto.bsdx.grp.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.bsdx.BsdxGenerator;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.generator.impl.*;
import com.giga.nexas.exception.BusinessException;
import com.giga.nexas.io.BinaryWriter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 */
public class GrpGenerator implements BsdxGenerator<Grp> {

    private final Map<String, GrpFileGenerator<? extends Grp>> generatorMap = new HashMap<>();

    public GrpGenerator() {
        // 注册解析器
        registerGenerator(new BatVoiceGrpGenerator());
//        registerGenerator(new MapGroupGrpGenerator());
        registerGenerator(new MekaGroupGrpGenerator());
//        registerGenerator(new ProgramMaterialGrpGenerator());
        registerGenerator(new SeGroupGrpGenerator());
        registerGenerator(new SpriteGroupGrpGenerator());
        registerGenerator(new TermGrpGenerator());
        registerGenerator(new WazaGroupGrpGenerator());
    }

    private void registerGenerator(GrpFileGenerator<? extends Grp> Generator) {
        String key = getGeneratorKey(Generator);
        generatorMap.put(key, Generator);
    }

    private String getGeneratorKey(GrpFileGenerator<?> generator) {
        return generator.getGeneratorKey();
    }
    
    @Override
    public String supportExtension() {
        return "grp";
    }

    @Override
    public void generate(String path, Grp grp, String charset) throws IOException {
        GrpFileGenerator<? extends Grp> matchedGenerator = generatorMap.get(grp.getFileName());
        if (matchedGenerator == null) {
            throw new BusinessException(500, "不支持的grp子文件(请注意文件大小写)：" + grp.getFileName());
        }

        FileUtil.mkdir(FileUtil.getParent(path, 1));
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
             BinaryWriter writer = new BinaryWriter(os, charset)) {
            matchedGenerator.generate(writer, grp);
        }
    }
}
