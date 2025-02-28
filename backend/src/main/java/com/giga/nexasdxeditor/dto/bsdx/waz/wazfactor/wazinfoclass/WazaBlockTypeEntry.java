package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass;

import lombok.Data;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/28
 * 逆向所得，存储类型的数组
 */
@Data
public class WazaBlockTypeEntry {

    private Integer type;
    private Integer address;
    private String description;

    // 构造函数
    public WazaBlockTypeEntry(Integer type, Integer address, String description) {
        this.type = type;
        this.address = address;
        this.description = description;
    }

}
