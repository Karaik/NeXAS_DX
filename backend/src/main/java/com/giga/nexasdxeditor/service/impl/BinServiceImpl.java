package com.giga.nexasdxeditor.service.impl;

import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.*;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.parser.MekParser;
import com.giga.nexasdxeditor.service.BinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class BinServiceImpl implements BinService {
    private static final Logger log = LoggerFactory.getLogger(BinServiceImpl.class);

    @Override
    public ResponseDTO parse(String path) throws IOException {


        byte[] mekFile = Files.readAllBytes(Paths.get(path));
        Mek mek = MekParser.parseMek(mekFile);

        return new ResponseDTO<>(mek, "ok");
    }

}
