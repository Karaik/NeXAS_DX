package com.giga.nexas.util;

import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps BHE InfoCollection lists to BSDX lists.
 * Keeps list sizes aligned with BSDX read/write expectations.
 */
public final class InfoCollectionMapper {

    private InfoCollectionMapper() {
    }

    public static void copyBheToBsdx(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject src,
            SkillInfoObject dst
    ) {
        if (src == null || dst == null) {
            return;
        }

        if (src instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventTerm bheTerm
                && dst instanceof CEventTerm bsdxTerm) {
            bsdxTerm.setBsdxInfoCollectionList(convertList(bheTerm.getBheInfoCollectionList(), 1));
            return;
        }

        if (src instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventMove bheMove
                && dst instanceof CEventMove bsdxMove) {
            bsdxMove.setBsdxInfoCollectionList1(convertList(bheMove.getBheInfoCollectionList1(), 1));
            bsdxMove.setBsdxInfoCollectionList2(convertList(bheMove.getBheInfoCollectionList2(), 1));
            return;
        }

        if (src instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange bheChange
                && dst instanceof CEventChange bsdxChange) {
            Integer flag = bsdxChange.getFlag();
            int count = flag != null ? Math.max(0, flag)
                    : (bheChange.getBheInfoCollectionList1() == null ? 0 : bheChange.getBheInfoCollectionList1().size());
            bsdxChange.setBsdxInfoCollectionList1(convertList(bheChange.getBheInfoCollectionList1(), count));
            bsdxChange.setBsdxInfoCollectionList2(convertList(bheChange.getBheInfoCollectionList2(), 1));
            return;
        }

        if (src instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventBlur bheBlur
                && dst instanceof CEventBlur bsdxBlur) {
            bsdxBlur.setBsdxInfoCollectionList(convertList(bheBlur.getBheInfoCollectionList(), 1));
            return;
        }

        if (src instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventScreenYure bheScreenYure
                && dst instanceof CEventScreenYure bsdxScreenYure) {
            bsdxScreenYure.setBsdxInfoCollectionList(convertList(bheScreenYure.getBheInfoCollectionList(), 1));
            return;
        }

        if (src instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventSpriteYure bheSpriteYure
                && dst instanceof CEventSpriteYure bsdxSpriteYure) {
            bsdxSpriteYure.setBsdxInfoCollectionList(convertList(bheSpriteYure.getBheInfoCollectionList(), 1));
        }
    }

    private static List<BsdxInfoCollection> convertList(List<BheInfoCollection> src, int ensureSize) {
        int safeSize = Math.max(0, ensureSize);
        int srcSize = src == null ? 0 : src.size();
        int count = safeSize > 0 ? safeSize : srcSize;

        List<BsdxInfoCollection> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BheInfoCollection item = (src != null && i < srcSize) ? src.get(i) : null;
            out.add(convertOne(item));
        }
        return out;
    }

    private static BsdxInfoCollection convertOne(BheInfoCollection src) {
        BsdxInfoCollection dst = new BsdxInfoCollection();
        if (src == null) {
            dst.setInt1(0);
            dst.setInt2(0);
            return dst;
        }

        dst.setInt1(src.getInt1() != null ? src.getInt1() : 0);
        if (src.getTypeList() != null) {
            dst.getTypeList().addAll(src.getTypeList());
        }
        if (src.getParamList() != null) {
            dst.getParamList().addAll(src.getParamList());
        }
        if (src.getIntList3() != null) {
            dst.getIntList3().addAll(src.getIntList3());
        }
        if (src.getIntList4() != null) {
            dst.getIntList4().addAll(src.getIntList4());
        }
        dst.setInt2(src.getInt2() != null ? src.getInt2() : 0);
        return dst;
    }
}
