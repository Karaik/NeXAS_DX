package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import java.util.LinkedHashSet;
import java.util.Set;

/** 跟随角色链传递验收根，避免把未消费的原版资源池误作本轮迁移行为。 */
public final class AssetDeliveryValidationContext {
    private final TsukuyomiBsdxBaselineBundle baseline;
    private final Set<String> mekFiles = new LinkedHashSet<>();
    private final Set<String> fullWazFiles = new LinkedHashSet<>();

    public AssetDeliveryValidationContext(TsukuyomiGraftRequest request, TsukuyomiBsdxBaselineBundle baseline, String mainMek) {
        this.baseline = baseline;
        if (request instanceof FollowupGraftRequest followup && followup.getPreviousCharacterResult() != null
                && followup.getPreviousCharacterResult().getImportedAssetSet() != null) {
            AssetDeliveryValidationContext previous = followup.getPreviousCharacterResult().getImportedAssetSet().getValidationContext();
            if (previous != null) { mekFiles.addAll(previous.mekFiles); fullWazFiles.addAll(previous.fullWazFiles); }
        }
        if (request.getInheritedJinkiResult() != null) {
            var plan = request.getInheritedJinkiResult().getGrpAppendPlan();
            if (plan == null || baseline == null || baseline.getMekaGroupGrp() == null || baseline.getWazaGroupGrp() == null)
                throw new IllegalStateException("继承 JINKI 的迁移验收根缺少有效 GRP/append plan");
            int meka = plan.getMekaGroupIndex(), waza = plan.getWazaGroupIndex();
            if (meka < 0 || meka >= baseline.getMekaGroupGrp().getMekaList().size()
                    || waza < 0 || waza >= baseline.getWazaGroupGrp().getWazaList().size())
                throw new IllegalStateException("继承 JINKI 的迁移验收根索引越界: meka=" + meka + ", waza=" + waza);
            var mekaEntry = baseline.getMekaGroupGrp().getMekaList().get(meka);
            var wazaEntry = baseline.getWazaGroupGrp().getWazaList().get(waza);
            if (mekaEntry == null || mekaEntry.getExistFlag() == null || mekaEntry.getExistFlag() == 0
                    || mekaEntry.getMekaName() == null || wazaEntry == null || wazaEntry.getExistFlag() == null
                    || wazaEntry.getExistFlag() == 0 || wazaEntry.getWazaDisplayName() == null)
                throw new IllegalStateException("继承 JINKI 的迁移验收根被禁用/缺少载荷名");
            mekFiles.add(mekaEntry.getMekaName() + ".mek");
            fullWazFiles.add(wazaEntry.getWazaDisplayName() + ".waz");
        }
        mekFiles.add(mainMek);
        fullWazFiles.add(request.getWazFileName());
    }
    public TsukuyomiBsdxBaselineBundle baseline() { return baseline; }
    public Set<String> mekFiles() { return Set.copyOf(mekFiles); }
    public Set<String> fullWazFiles() { return Set.copyOf(fullWazFiles); }
}
