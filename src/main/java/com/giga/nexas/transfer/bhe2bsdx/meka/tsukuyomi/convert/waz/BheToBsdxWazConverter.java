package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz;

import com.giga.nexas.dto.bsdx.waz.Waz;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE WAZ -> BSDX WAZ DTO 形状转换器。
 *
 * <p>本类只负责源侧格式转换：BHE 的 83 个事件槽位按结构映射转成 BSDX 的 72 个事件槽位。
 * WAZ 文件组索引、SPM 文件组索引、SE 索引、语音组索引、skill merge/reorder 均由后续 rebind 阶段处理。</p>
 */
public class BheToBsdxWazConverter {

    private final BheToBsdxWazSkillConverter skillConverter = new BheToBsdxWazSkillConverter();

    public Waz convert(com.giga.nexas.dto.bhe.waz.Waz source) {
        Waz target = new Waz();
        if (source == null) {
            return target;
        }

        target.setFileName(source.getFileName());
        target.setExtensionName(source.getExtensionName());
        target.setSkillList(convertSkills(source.getSkillList()));
        return target;
    }

    private List<Waz.Skill> convertSkills(List<com.giga.nexas.dto.bhe.waz.Waz.Skill> sourceList) {
        List<Waz.Skill> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }

        for (com.giga.nexas.dto.bhe.waz.Waz.Skill source : sourceList) {
            targetList.add(skillConverter.convert(source));
        }
        return targetList;
    }
}
