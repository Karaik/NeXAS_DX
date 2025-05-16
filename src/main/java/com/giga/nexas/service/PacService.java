package com.giga.nexas.service;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.util.PacUtil;

public class PacService {

    private final static String PAC_SUFFIX = ".pac";

    public ResponseDTO unPac(String sourcePath) throws Exception {
        String output = PacUtil.unpack(sourcePath);

        ResponseDTO responseDTO = new ResponseDTO<>(output);

        return responseDTO;
    }

    public ResponseDTO pac(String sourcePath, String compressMode) throws Exception {
        String output = PacUtil.pack(sourcePath, compressMode);

        ResponseDTO responseDTO = new ResponseDTO<>(output);

        return responseDTO;
    }

}