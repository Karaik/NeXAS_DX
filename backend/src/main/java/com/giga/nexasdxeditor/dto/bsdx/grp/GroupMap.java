package com.giga.nexasdxeditor.dto.bsdx.grp;

import com.giga.nexasdxeditor.dto.bsdx.Bsdx;
import lombok.Data;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/2
 * @Description GroupMap
 */
@Data
public class GroupMap extends Bsdx {

    // 数量
    private Integer itemQuantity;

    // 文件名
    private String fileName;

    // 代号
    private String codeName;

}
