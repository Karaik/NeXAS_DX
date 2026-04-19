package com.giga.nexas.transfer.bhe2bsdx.meka.initializer;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.service.BsdxBinService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 给缺少 initializer 的 BHE 单机体补齐 initializer 武装。
 *
 * <p>initializer 不是普通 MEK 表行。已有数据表明它必须存在于 MEK 的
 * {@code mekWeaponInfoMap}；如果当前 WAZ 本身已有 {@code INITIALIZER} skill，则武装行指向它。
 * 如果当前 WAZ 没有 initializer skill，本步骤会从 BSDX 原生模板构造一条“通用 initializer skill”：
 * 保留关灯、画面演出、blur、change 等共性事件，剥离机体专属 sprite/SE/voice 资源。</p>
 *
 * <p>这里刻意使用 BSDX 原生模板，而不是把 BHE initializer skill 转换后塞入目标 WAZ。
 * 2026-04-19 动态验证发现，BHE 模板转换后的 initializer 会在目标引擎运行时触发
 * {@code std::length_error("vector<T> too long")}；BSDX 原生 Zako116a initializer skill
 * 虽能解析，但在 Tsukuyomi 武装演示 hover 时仍会崩溃。因此这里不会原样追加外部 WAZ skill，
 * 而是按 initializer 共性重新拼一条目标机体可消费的 skill。material trailing entry 使用当前机体已有条目复制，
 * 避免把 Zako116a 自身的演示素材链挂进 Tsukuyomi/Yuri 等其他机体。
 * 本步骤必须放在单机体 MEK/WAZ 重绑之后、静态资源写出之前执行，避免补齐出来的目标侧武装
 * 反过来污染前置 BHE 资源闭包扫描。</p>
 */
public class EnsureInitializerWeaponStep {

    private static final String CHARSET = "windows-31j";

    /**
     * 数据级模板来源。
     *
     * <p>允许把这两个路径作为本包内的固定值，原因是用户已确认 initializer 补齐策略
     * 可以在独立包中承载模板来源。这里必须保留来源说明，避免后续误以为这些值是隐藏业务规则。
     * 选择 Zako116a 是因为它在 BSDX 侧已有完整 initializer 武装行和通用 initializer skill 骨架；
     * 后续移植 Yuri 等缺 initializer 的 BHE 单机体时可以复用同一套目标侧武装定义。</p>
     */
    private static final Path TEMPLATE_MEK_PATH = Paths.get("src/main/resources/game/bsdx/mek/Zako116a.mek");
    private static final Path TEMPLATE_WAZ_PATH = Paths.get("src/main/resources/game/bsdx/waz/Zako116a.waz");
    private static final String INITIALIZER_SEQUENCE = "INITIALIZER";
    private static final String INITIALIZER_NAME = "イニシャライザ";
    private static final String SAFE_FALLBACK_SKILL = "STAND";

    private final BsdxBinService bsdxBinService;

    public EnsureInitializerWeaponStep() {
        this(new BsdxBinService());
    }

    public EnsureInitializerWeaponStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public void ensureAfterRebind(
            String targetWazFileName,
            Mek targetMek,
            Waz targetWaz,
            List<String> notes
    ) {
        if (targetMek == null || targetWaz == null) {
            return;
        }
        if (hasInitializerWeapon(targetMek)) {
            addNote(notes, "initializer: target MEK already has initializer weapon");
            return;
        }

        int initializerSkillIndex = findInitializerSkillIndex(targetWaz);
        if (initializerSkillIndex < 0) {
            initializerSkillIndex = appendGenericInitializerSkill(targetWaz);
            addNote(notes, "initializer: current WAZ has no INITIALIZER skill; append sanitized generic skill index "
                    + initializerSkillIndex + " for " + targetWazFileName);
        }

        appendInitializerWeaponAndMaterial(targetMek, initializerSkillIndex);
        addNote(notes, "initializer: appended initializer weapon to target MEK tail, wazSequence="
                + initializerSkillIndex);
    }

    private boolean hasInitializerWeapon(Mek mek) {
        if (mek.getMekWeaponInfoMap() == null || mek.getMekWeaponInfoMap().isEmpty()) {
            return false;
        }
        return mek.getMekWeaponInfoMap().values().stream().anyMatch(this::isInitializerWeapon);
    }

    private boolean isInitializerWeapon(Mek.MekWeaponInfo weapon) {
        if (weapon == null) {
            return false;
        }
        return equalsIgnoreCase(weapon.getWeaponSequence(), INITIALIZER_SEQUENCE)
                || contains(weapon.getWeaponName(), INITIALIZER_NAME);
    }

    private int findInitializerSkillIndex(Waz waz) {
        if (waz.getSkillList() == null) {
            return -1;
        }
        for (int i = 0; i < waz.getSkillList().size(); i++) {
            Waz.Skill skill = waz.getSkillList().get(i);
            if (skill == null) {
                continue;
            }
            if (equalsIgnoreCase(skill.getSkillNameEnglish(), INITIALIZER_SEQUENCE)
                    || contains(skill.getSkillNameJapanese(), INITIALIZER_NAME)) {
                return i;
            }
        }
        return -1;
    }

    private int chooseSafeFallbackSkillIndex(Waz waz) {
        if (waz.getSkillList() == null || waz.getSkillList().isEmpty()) {
            throw new IllegalStateException("initializer 补齐无法选择 fallback skill：当前 WAZ 没有 skill");
        }

        // STAND 是当前机体自带、最小副作用的演示 skill；找不到时再退到 0 号 skill。
        for (int i = 0; i < waz.getSkillList().size(); i++) {
            Waz.Skill skill = waz.getSkillList().get(i);
            if (skill != null && equalsIgnoreCase(skill.getSkillNameEnglish(), SAFE_FALLBACK_SKILL)) {
                return i;
            }
        }
        return 0;
    }

    private int appendGenericInitializerSkill(Waz targetWaz) {
        Waz templateWaz = loadTemplateWaz();
        int templateSkillIndex = findInitializerSkillIndex(templateWaz);
        if (templateSkillIndex < 0) {
            throw new IllegalStateException("initializer 模板 WAZ 中找不到 initializer skill: " + TEMPLATE_WAZ_PATH);
        }

        Waz.Skill initializerSkill = templateWaz.getSkillList().get(templateSkillIndex);
        sanitizeInitializerSkillForTargetMek(targetWaz, initializerSkill);
        targetWaz.getSkillList().add(initializerSkill);
        return targetWaz.getSkillList().size() - 1;
    }

    private void sanitizeInitializerSkillForTargetMek(Waz targetWaz, Waz.Skill initializerSkill) {
        Waz.Skill safeSkill = targetWaz.getSkillList().get(chooseSafeFallbackSkillIndex(targetWaz));
        SkillUnit safeSpriteUnit = findFirstUnit(safeSkill, 0);

        for (Waz.Skill.SkillPhase phase : initializerSkill.getPhasesInfo()) {
            if (phase == null || phase.getSkillUnitCollection() == null) {
                continue;
            }
            for (SkillUnit unit : phase.getSkillUnitCollection()) {
                if (unit == null || unit.getUnitQuantity() == null) {
                    continue;
                }
                if (unit.getUnitQuantity() == 0) {
                    replaceSpriteUnitWithCurrentMek(unit, safeSpriteUnit);
                } else if (unit.getUnitQuantity() == 52 || unit.getUnitQuantity() == 53) {
                    clearActorSpecificAudio(unit);
                }
            }
        }
    }

    private SkillUnit findFirstUnit(Waz.Skill skill, int unitQuantity) {
        if (skill == null || skill.getPhasesInfo() == null) {
            return null;
        }
        for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
            if (phase == null || phase.getSkillUnitCollection() == null) {
                continue;
            }
            for (SkillUnit unit : phase.getSkillUnitCollection()) {
                if (unit != null && unit.getUnitQuantity() != null && unit.getUnitQuantity() == unitQuantity) {
                    return unit;
                }
            }
        }
        return null;
    }

    private void replaceSpriteUnitWithCurrentMek(SkillUnit targetUnit, SkillUnit safeSpriteUnit) {
        if (safeSpriteUnit == null) {
            targetUnit.setSkillInfoObjectList(new java.util.ArrayList<>());
            targetUnit.setSkillInfoUnknownList(new java.util.ArrayList<>());
            return;
        }
        // 这里复用当前机体 STAND/0 号 skill 的 sprite 对象，只读写出，不修改原对象。
        targetUnit.setSkillInfoObjectList(new java.util.ArrayList<SkillInfoObject>(safeSpriteUnit.getSkillInfoObjectList()));
        targetUnit.setSkillInfoUnknownList(new java.util.ArrayList<SkillInfoUnknown>(safeSpriteUnit.getSkillInfoUnknownList()));
    }

    private void clearActorSpecificAudio(SkillUnit unit) {
        // unit 52/53 在 initializer 共性中分别承载 SE/Voice；跨机体复制会引入模板角色的声音链。
        unit.setSkillInfoObjectList(new java.util.ArrayList<>());
        unit.setSkillInfoUnknownList(new java.util.ArrayList<>());
    }

    private void appendInitializerWeaponAndMaterial(Mek targetMek, int initializerSkillIndex) {
        Mek templateMek = loadTemplateMek();
        int templateWeaponIndex = findInitializerWeaponIndex(templateMek);
        if (templateWeaponIndex < 0) {
            throw new IllegalStateException("initializer 模板 MEK 中找不到 initializer 武装: " + TEMPLATE_MEK_PATH);
        }

        Mek.MekWeaponInfo templateWeapon = templateMek.getMekWeaponInfoMap().get(templateWeaponIndex);
        if (templateWeapon == null) {
            throw new IllegalStateException("initializer 模板 MEK 武装索引没有对应数据: " + templateWeaponIndex);
        }

        Mek.MekWeaponInfo copied = new Mek.MekWeaponInfo();
        BeanUtil.copyProperties(templateWeapon, copied);
        // offset 只是解析时记录的原文件偏移；生成器会按新表重新写入，保留模板偏移反而会误导审查。
        copied.offset = 0;
        copied.setWazSequence(initializerSkillIndex);

        int targetIndex = nextWeaponIndex(targetMek);
        Mek.MekMaterialBlock.PluginEntry safeMaterial = chooseCurrentMekMaterialFallback(targetMek);
        targetMek.getMekWeaponInfoMap().put(targetIndex, copied);

        // MEK 尾部 material 与武装表按同位顺序消费；只追加武装、不追加 material 会让武装菜单/演示读取错位。
        // 这里复制当前机体已有 material，而不是模板 material，避免跨机体演示资源链进入 hover 预览。
        appendMaterialForInitializer(targetMek, safeMaterial);
    }

    private int findInitializerWeaponIndex(Mek mek) {
        if (mek == null || mek.getMekWeaponInfoMap() == null) {
            return -1;
        }
        return mek.getMekWeaponInfoMap().entrySet().stream()
                .filter(entry -> isInitializerWeapon(entry.getValue()))
                .map(Map.Entry::getKey)
                .filter(index -> index != null && index >= 0)
                .findFirst()
                .orElse(-1);
    }

    private Mek.MekMaterialBlock.PluginEntry chooseCurrentMekMaterialFallback(Mek targetMek) {
        if (targetMek == null || targetMek.getMekMaterialBlock() == null) {
            return new Mek.MekMaterialBlock.PluginEntry();
        }
        List<Mek.MekMaterialBlock.PluginEntry> trailingEntries = targetMek.getMekMaterialBlock().getTrailingEntries();
        if (trailingEntries != null && !trailingEntries.isEmpty() && trailingEntries.get(0) != null) {
            return trailingEntries.get(0);
        }
        List<Mek.MekMaterialBlock.PluginEntry> regularEntries = targetMek.getMekMaterialBlock().getRegularEntries();
        if (regularEntries != null && !regularEntries.isEmpty() && regularEntries.get(0) != null) {
            return regularEntries.get(0);
        }
        return new Mek.MekMaterialBlock.PluginEntry();
    }

    private void appendMaterialForInitializer(
            Mek targetMek,
            Mek.MekMaterialBlock.PluginEntry sourceMaterial
    ) {
        if (targetMek.getMekMaterialBlock() == null) {
            targetMek.setMekMaterialBlock(new Mek.MekMaterialBlock());
        }

        Mek.MekMaterialBlock.PluginEntry copied = copyPluginEntry(sourceMaterial);
        targetMek.getMekMaterialBlock().getTrailingEntries().add(copied);
        targetMek.getMekMaterialBlock().getEntries().add(copyPluginEntry(sourceMaterial));
    }

    private Mek.MekMaterialBlock.PluginEntry copyPluginEntry(Mek.MekMaterialBlock.PluginEntry source) {
        Mek.MekMaterialBlock.PluginEntry copied = new Mek.MekMaterialBlock.PluginEntry();
        if (source == null) {
            return copied;
        }
        copied.offset = 0;
        copied.length = null;
        copied.setSpriteGroups(copyGroupList(source.getSpriteGroups()));
        copied.setSeGroups(copyGroupList(source.getSeGroups()));
        copied.setVoiceGroups(copyGroupList(source.getVoiceGroups()));
        return copied;
    }

    private java.util.List<int[]> copyGroupList(java.util.List<int[]> source) {
        java.util.List<int[]> copied = new java.util.ArrayList<>();
        if (source == null) {
            return copied;
        }
        for (int[] group : source) {
            copied.add(group == null ? null : java.util.Arrays.copyOf(group, group.length));
        }
        return copied;
    }

    private int nextWeaponIndex(Mek targetMek) {
        if (targetMek.getMekWeaponInfoMap() == null || targetMek.getMekWeaponInfoMap().isEmpty()) {
            return 0;
        }
        return targetMek.getMekWeaponInfoMap().keySet().stream()
                .filter(index -> index != null && index >= 0)
                .max(Comparator.naturalOrder())
                .orElse(-1) + 1;
    }

    private Waz loadTemplateWaz() {
        ensureTemplateFile(TEMPLATE_WAZ_PATH);
        try {
            ResponseDTO<?> dto = bsdxBinService.parse(TEMPLATE_WAZ_PATH.toString(), CHARSET);
            return (Waz) dto.getData();
        } catch (IOException e) {
            throw new IllegalStateException("读取 initializer WAZ 模板失败: " + TEMPLATE_WAZ_PATH, e);
        }
    }

    private Mek loadTemplateMek() {
        ensureTemplateFile(TEMPLATE_MEK_PATH);
        try {
            ResponseDTO<?> dto = bsdxBinService.parse(TEMPLATE_MEK_PATH.toString(), CHARSET);
            return (Mek) dto.getData();
        } catch (IOException e) {
            throw new IllegalStateException("读取 initializer MEK 模板失败: " + TEMPLATE_MEK_PATH, e);
        }
    }

    private void ensureTemplateFile(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("initializer 模板文件不存在: " + path);
        }
    }

    private boolean equalsIgnoreCase(String value, String expected) {
        return value != null && expected != null && value.trim().equalsIgnoreCase(expected);
    }

    private boolean contains(String value, String expected) {
        return value != null && expected != null && value.contains(expected);
    }

    private void addNote(List<String> notes, String note) {
        if (notes != null) {
            notes.add(note);
        }
    }
}
