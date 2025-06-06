package com.giga.nexas.dto.bsdx.bin;

import com.giga.nexas.dto.bsdx.Bsdx;
import lombok.Data;

import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/11
 * @Description __GLOBAL.bin 对应类
 */
// TODO not yet
@Data
public class GLOBAL extends Bsdx {

    private List<String> table1;
    private List<String> table2;
    private List<String> table3;
    private List<String> table4;
}
