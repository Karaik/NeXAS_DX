package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert;

import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.spm.BheToBsdxSpmConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.util.Map;

/**
 * BHE 公共弹幕资源簇的格式转换入口。
 *
 * <p>本步骤只处理公共资源：公共 WAZ 与公共 WAZ 直接引用到的公共 SPM。
 * Tsukuyomi 私有 WAZ/SPM 不从这里经过，避免公共资源混入单机体 selected 转换步骤。</p>
 *
 * <p>转换结果只进入 {@code commonProjectileResourceBundle}。公共资源接入完成后属于
 * {@code preparedBaseline}；{@code mergedPackageBundle} 只承载单机体 selected 转换视图，
 * 不能把公共资源喂给单机体 closure/graft 的普通路径。</p>
 */
public class ConvertBheCommonProjectileResourcesStep {

    private final BheToBsdxWazConverter wazConverter = new BheToBsdxWazConverter();
    private final BheToBsdxSpmConverter spmConverter = new BheToBsdxSpmConverter();

    public void convert(TsukuyomiRawSourceBundle rawSourceBundle, TsukuyomiConvertedBundle convertedBundle) {
        if (rawSourceBundle == null || convertedBundle == null) {
            return;
        }

        // 公共 WAZ/SPM 在这里仅做 BHE DTO -> BSDX DTO 形状转换；目标 entry 接入和引用重写交给 redirect 层。
        convertCommonWaz(rawSourceBundle, convertedBundle);
        convertCommonSpm(rawSourceBundle, convertedBundle);
    }

    private void convertCommonWaz(TsukuyomiRawSourceBundle rawSourceBundle, TsukuyomiConvertedBundle convertedBundle) {
        for (Map.Entry<String, com.giga.nexas.dto.bhe.waz.Waz> entry
                : rawSourceBundle.getCommonProjectileWazByFileName().entrySet()) {
            Waz convertedWaz = wazConverter.convert(entry.getValue());
            // 公共资源不写入 selected；selected 只代表单机体私有资源。
            put(convertedBundle.getCommonProjectileResourceBundle(), entry.getKey(), convertedWaz);
        }
    }

    private void convertCommonSpm(TsukuyomiRawSourceBundle rawSourceBundle, TsukuyomiConvertedBundle convertedBundle) {
        for (Map.Entry<String, com.giga.nexas.dto.bhe.spm.Spm> entry
                : rawSourceBundle.getCommonProjectileSpmByFileName().entrySet()) {
            Spm convertedSpm = spmConverter.convert(entry.getValue());
            // 公共 SPM 与公共 WAZ 同属弹幕资源簇，必须留在 commonProjectileResourceBundle。
            put(convertedBundle.getCommonProjectileResourceBundle(), entry.getKey(), convertedSpm);
        }
    }

    private void put(TsukuyomiPackageBundle bundle, String fileName, Waz waz) {
        if (bundle == null || fileName == null || waz == null) {
            return;
        }
        bundle.getWazByFileName().put(fileName, waz);
    }

    private void put(TsukuyomiPackageBundle bundle, String fileName, Spm spm) {
        if (bundle == null || fileName == null || spm == null) {
            return;
        }
        bundle.getSpmByFileName().put(fileName, spm);
    }
}
