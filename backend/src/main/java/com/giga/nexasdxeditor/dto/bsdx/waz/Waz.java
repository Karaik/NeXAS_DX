package com.giga.nexasdxeditor.dto.bsdx.waz;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description Waz
 */
@Data
public class Waz {

    private Integer spmSequence;

    private String skillNameKanji;

    private String skillNameEnglish;

    private List<WazPhase> phasesInfo;

    /**
     * 该技能的阶段数
     */
    private Integer phaseQuantity;

    private byte[] data;

    public Waz(Integer spmSequence) {
        this.spmSequence = spmSequence;
        this.phasesInfo = new ArrayList<>();
    }

    /**
     * 技能的阶段
     * 逆向得知，每个阶段信息内，有72个最小单元，每个单元大小为3*4+5*4字节，
     * 其中单元数由读到的第一个整数决定
     */
    @Data
    public static class WazPhase {

        /**
         * 逆向所得，size=72
         */
        private List<List<WazInfoObject>> wazInfoObjectCollection;


        public WazPhase () {
            this.wazInfoObjectCollection = new ArrayList<>();
        }


    }




}
