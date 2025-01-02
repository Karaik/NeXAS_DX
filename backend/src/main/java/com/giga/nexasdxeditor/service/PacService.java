package com.giga.nexasdxeditor.service;

import com.giga.nexasdxeditor.dto.ResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface PacService {
    ResponseDTO unPac(String sourcePath) throws Exception;
    ResponseDTO pac(String sourcePath) throws Exception;
}
