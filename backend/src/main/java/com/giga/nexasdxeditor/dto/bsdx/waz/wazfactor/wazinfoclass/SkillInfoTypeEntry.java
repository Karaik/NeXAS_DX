package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/28
 * 逆向所得，存储类型的数组
 */
@Data
@AllArgsConstructor
public class SkillInfoTypeEntry {

    private Integer type;
    private Integer address;
    private String description;

}
