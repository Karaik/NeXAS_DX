package com.giga.nexas.transfer.bhe2bsdx.model;

/**
 * 源机体描述：baseKey（小写文件名前缀）+ codeName（大写 GRP 注册名）。
 * <p>
 * 例如 baseKey="misaki", codeName="MISAKI"
 */
public class MekaSource {

    private final String baseKey;
    private final String codeName;

    public MekaSource(String baseKey, String codeName) {
        this.baseKey = baseKey;
        this.codeName = codeName;
    }

    public String getBaseKey() {
        return baseKey;
    }

    public String getCodeName() {
        return codeName;
    }

    @Override
    public String toString() {
        return "MekaSource{baseKey='" + baseKey + "', codeName='" + codeName + "'}";
    }
}
