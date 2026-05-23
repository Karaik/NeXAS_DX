package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一张 BHE .map 内部声明的资源引用清单。
 *
 * <p>Step1 只负责把引用收集清楚并判断源资源是否存在；
 * 已存在的引用归入 deferredResourceFiles，表示后续转换/复制阶段再消费。</p>
 */
@Data
public class BheMapResourceReferences {

    private String foregroundImage;
    private BheMapResourceReference foregroundReference;
    private List<String> resourceSlotFiles = new ArrayList<>();
    private List<String> spriteMapFiles = new ArrayList<>();
    private List<BheMapResourceReference> references = new ArrayList<>();
    private List<String> deferredResourceFiles = new ArrayList<>();
    private List<String> missingResourceFiles = new ArrayList<>();
}
