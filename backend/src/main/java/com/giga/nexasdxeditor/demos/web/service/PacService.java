package com.giga.nexasdxeditor.demos.web.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface PacService {
    Map<String, String> unPac(MultipartFile pac) throws Exception;
}
