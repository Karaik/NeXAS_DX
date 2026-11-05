package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.grp.groupmap.*;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** 验收最终 bytes 与有效加载层。明确字段消费者，不对未解码的 Term 条件猜测引用。 */
public final class AssetDeliveryValidator {
    private final BsdxBinService service = new BsdxBinService();
    private final TsukuyomiGraftRequest request;
    private final TsukuyomiImportedAssetSet assets;
    private final TsukuyomiBsdxBaselineBundle baseline;
    private final LayeredAssetResolver resolver;
    private final Map<String, Object> cache = new HashMap<>();
    private final Set<String> visited = new HashSet<>();
    private SpriteGroupGrp sprites;
    private WazaGroupGrp wazas;
    private SeGroupGrp sounds;
    private BatVoiceGrp voices;
    private record MekaContext(Integer wazIndex, Integer spmIndex) { }

    private AssetDeliveryValidator(TsukuyomiGraftRequest request, TsukuyomiImportedAssetSet assets) {
        this.request = request;
        this.assets = assets;
        this.baseline = assets.getValidationContext() == null ? null : assets.getValidationContext().baseline();
        this.resolver = new LayeredAssetResolver(request, assets.getOutputRootDir(), assets);
    }

    public static void validate(TsukuyomiGraftRequest request, TsukuyomiImportedAssetSet assets) {
        if (assets == null || assets.getOutputRootDir() == null) throw new IllegalStateException("缺少最终资源输出目录");
        if (!assets.getMissingAssets().isEmpty()) throw new IllegalStateException("最终必需资源缺失: " + String.join("; ", assets.getMissingAssets()));
        try {
            new AssetDeliveryValidator(request, assets).validate();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("最终资源验收失败: " + e.getMessage(), e);
        }
    }

    private void validate() throws IOException {
        Path output = assets.getOutputRootDir();
        require(Files.isDirectory(output), "输出目录不存在: " + output);
        List<Path> declared = new ArrayList<>();
        declared.addAll(assets.getGeneratedDatFiles()); declared.addAll(assets.getGeneratedGrpFiles());
        declared.addAll(assets.getGeneratedMekFiles()); declared.addAll(assets.getGeneratedWazFiles());
        declared.addAll(assets.getCopiedSpmFiles()); declared.addAll(assets.getCopiedImageFiles()); declared.addAll(assets.getCopiedAudioFiles());
        for (Path path : declared) require(path != null && Files.isRegularFile(path), "声明产物未落盘: " + path);
        // 不保留全体原版 DTO 的第二份副本；仅缓存引用闭包实际使用的载荷。
        try (var files = Files.walk(output)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String name = LayeredAssetResolver.normalize(file.getFileName().toString());
                resolver.resolveFile(output, name); // 同层大小写冲突必须先拒绝。
                if (!name.matches(".*\\.(mek|waz|spm|grp|dat)$")) continue;
                Object parsed = parse(file, name);
                if (parsed instanceof Spm spm) validateSpmStructure(spm, name);
            }
        }
        sprites = binary("SpriteGroup.grp", SpriteGroupGrp.class, baseline == null ? null : baseline.getSpriteGroupGrp(), request.getBsdxGrpDir());
        wazas = binary("WazaGroup.grp", WazaGroupGrp.class, baseline == null ? null : baseline.getWazaGroupGrp(), request.getBsdxGrpDir());
        sounds = binary("SeGroup.grp", SeGroupGrp.class, baseline == null ? null : baseline.getSeGroupGrp(), request.getBsdxGrpDir());
        voices = binary("BatVoice.grp", BatVoiceGrp.class, baseline == null ? null : baseline.getBatVoiceGrp(), request.getBsdxGrpDir());
        if (baseline != null) {
            require(sprites != null && wazas != null && sounds != null && voices != null, "缺少有效 GRP 索引表");
            ProgramMaterialGrp program = binary("ProgramMaterial.grp", ProgramMaterialGrp.class, baseline.getProgramMaterialGrp(), request.getBsdxGrpDir());
            require(program != null, "缺少 ProgramMaterial.grp");
            require(program.getArray1().size() == sprites.getSpriteList().size()
                    && program.getArray2().size() == sounds.getSeList().size()
                    && program.getArray3().size() == voices.getVoiceList().size(), "ProgramMaterial 外层容量与有效 GRP 不匹配");
        }

        AssetDeliveryValidationContext context = assets.getValidationContext();
        validateWeaponCandidates();
        Set<String> mekRoots = context == null ? new LinkedHashSet<>() : new LinkedHashSet<>(context.mekFiles());
        if (context == null) for (Path path : assets.getGeneratedMekFiles()) mekRoots.add(path.getFileName().toString());
        for (String file : mekRoots) validateMek(file);

        Set<String> fullWaz = context == null ? new LinkedHashSet<>() : new LinkedHashSet<>(context.fullWazFiles());
        if (context == null) for (Path path : assets.getGeneratedWazFiles()) fullWaz.add(path.getFileName().toString());
        for (String file : fullWaz) {
            Waz waz = waz(file, file);
            MekaContext self = null;
            for (String mekName : mekRoots) {
                Mek mek = mek(mekName);
                if (wazas != null && validIndex(mek.getMekBasicInfo().getWazFileSequence(), wazas.getWazaList().size())) {
                    var entry = wazas.getWazaList().get(mek.getMekBasicInfo().getWazFileSequence());
                    if (LayeredAssetResolver.normalize(entry.getWazaDisplayName() + ".waz").equals(LayeredAssetResolver.normalize(file))) self = new MekaContext(mek.getMekBasicInfo().getWazFileSequence(), mek.getMekBasicInfo().getSpmFileSequence());
                }
            }
            for (int index = 0; index < waz.getSkillList().size(); index++) visitSkill(file, index, self, file + "[" + index + "]");
        }
    }

    private void validateMek(String name) throws IOException {
        Mek mek = mek(name);
        require(mek != null && mek.getMekBasicInfo() != null, name + " -> MEK 载荷缺失");
        Integer fileIndex = mek.getMekBasicInfo().getWazFileSequence();
        require(wazas != null && validIndex(fileIndex, wazas.getWazaList().size()), name + " -> WazaGroup[" + fileIndex + "] 越界");
        var entry = wazas.getWazaList().get(fileIndex);
        require(entry != null && enabled(entry.getExistFlag()) && entry.getWazaDisplayName() != null, name + " -> 禁用 WazaGroup[" + fileIndex + "]");
        String wazName = entry.getWazaDisplayName() + ".waz";
        Waz waz = waz(wazName, name);
        require(entry.getParam() != null && entry.getParam() == waz.getSkillList().size(), name + " -> " + wazName + " skill数量与WazaGroup.param不匹配");
        Integer self = mek.getMekBasicInfo().getSpmFileSequence();
        spm(self, name + " -> self SpriteGroup[" + self + "]");
        require(mek.getMekWeaponInfoMap() != null, name + " -> 缺少武装表");
        for (var weapon : mek.getMekWeaponInfoMap().entrySet()) {
            if (weapon.getValue() == null) continue;
            Integer skill = weapon.getValue().getWazSequence();
            if (skill != null && skill == -1) continue;
            require(validIndex(skill, waz.getSkillList().size()), name + " -> weapon[" + weapon.getKey() + "] -> " + wazName + "[" + skill + "] 越界");
            visitSkill(wazName, skill, new MekaContext(fileIndex, self), name + " -> weapon[" + weapon.getKey() + "] -> " + wazName + "[" + skill + "]");
        }
        for (var ai : mek.getMekAiInfoList()) for (var event : ai.getCpuEventList()) {
            if (event instanceof com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack attack) {
                Integer index = attack.getMekWeaponInfoMapNo();
                if (Objects.equals(index, -1)) continue;
                require(index != null && mek.getMekWeaponInfoMap().get(index) != null,
                        name + " -> AI attack weapon[" + index + "] 不存在/禁用");
            }
        }
        validatePlayerEquipment(mek, name);
        validateMekVoices(mek, name);
        validateMaterials(mek, name);
    }

    private void validateMaterials(Mek mek, String chain) throws IOException {
        Mek.MekMaterialBlock material = mek.getMekMaterialBlock();
        require(material != null && material.getEntries() != null, chain + " -> Material 缺失");
        int regular = 7 + (material.getExtraRegularCount() == null ? 0 : material.getExtraRegularCount());
        // KOU 是唯一类别0按三等级消费的原版路径；用有效 MekaGroup 身份，而非文件名猜测。
        boolean kou = isKou(chain);
        int consumedWeaponRows = mek.getMekWeaponInfoMap().values().stream().filter(Objects::nonNull)
                .mapToInt(weapon -> kou && Objects.equals(weapon.getWeaponCategory(), 0) ? 3 : 1).sum();
        require(material.getEntries().size() >= regular + consumedWeaponRows, chain + " -> Material 武装尾表不足");
        for (int i = 0; i < material.getEntries().size(); i++) {
            if (i >= regular + consumedWeaponRows) continue;
            var entry = material.getEntries().get(i);
            String at = chain + " -> Material[" + i + "]";
            // 源Material可以保留超出目标GRP的空预留组；只有实际条目会消费对应资源索引。
            for (int group = 0; group < entry.getSpriteGroups().size(); group++) {
                int[] values = entry.getSpriteGroups().get(group);
                if (values == null) continue;
                for (int animation : values) if (animation >= 0) {
                    Spm spm = spm(group, at);
                    require(animation < spm.getAnimData().size(), at + " -> SpriteGroup[" + group + "].animation[" + animation + "] 越界");
                    validateAnimation(spm, animation, null, at);
                }
            }
            validateAudioMaterial(entry.getSeGroups(), true, at);
            validateAudioMaterial(entry.getVoiceGroups(), false, at);
        }
    }

    private boolean isKou(String name) throws IOException {
        MekaGroupGrp group = binary("MekaGroup.grp", MekaGroupGrp.class, baseline == null ? null : baseline.getMekaGroupGrp(), request.getBsdxGrpDir());
        if (group == null) return false;
        String wanted = LayeredAssetResolver.normalize(name);
        return group.getMekaList().stream().anyMatch(entry -> entry != null && enabled(entry.getExistFlag())
                && LayeredAssetResolver.normalize(entry.getMekaName() + ".mek").equals(wanted)
                && "KOU".equalsIgnoreCase(entry.getMekaCodeName()));
    }

    private void validatePlayerEquipment(Mek mek, String name) throws IOException {
        MekaGroupGrp mekas = binary("MekaGroup.grp", MekaGroupGrp.class, baseline == null ? null : baseline.getMekaGroupGrp(), request.getBsdxGrpDir());
        Dat equip = binary("WeaponEquip.dat", Dat.class, null, request.getBsdxDatDir());
        if (mekas == null || equip == null) return;
        int mekaIndex = -1;
        for (int i = 0; i < mekas.getMekaList().size(); i++) {
            var entry = mekas.getMekaList().get(i);
            if (entry != null && enabled(entry.getExistFlag()) && LayeredAssetResolver.normalize(entry.getMekaName() + ".mek").equals(LayeredAssetResolver.normalize(name))) mekaIndex = i;
        }
        if (mekaIndex < 0 || mekaIndex >= equip.getData().size()) return;
        List<Object> row = equip.getData().get(mekaIndex);
        // 14 列均为装备武装号（56 bytes）；第0列也是首个装备槽，不能当作配置标志跳过。
        for (int column = 0; column < row.size(); column++) {
            Object value = row.get(column);
            require(value instanceof Number, name + " -> WeaponEquip[" + mekaIndex + "][" + column + "] 编号不是整数: " + value);
            Number number = (Number) value;
            int index = number.intValue();
            if (index == -1) continue;
            require(index >= 0 && index <= 152, name + " -> WeaponEquip[" + mekaIndex + "][" + column + "]=" + index + " 超出玩家统计bank 0..152");
            require(mek.getMekWeaponInfoMap().get(index) != null, name + " -> WeaponEquip weapon[" + index + "] 不存在/禁用");
        }
    }

    private void validateWeaponCandidates() throws IOException {
        Dat weapon = binary("Weapon.dat", Dat.class, null, request.getBsdxDatDir());
        if (weapon == null) return;
        for (int row = 0; row < weapon.getData().size(); row++) {
            List<Object> values = weapon.getData().get(row);
            require(values != null && !values.isEmpty() && values.get(0) instanceof Number,
                    "Weapon.dat[" + row + "] 候选武装编号缺失/不是整数");
            int index = ((Number) values.get(0)).intValue();
            require(index >= 0 && index <= 152, "Weapon.dat[" + row + "][0]=" + index + " 超出玩家统计bank 0..152");
        }
    }

    private void validateAudioMaterial(List<int[]> groups, boolean se, String chain) throws IOException {
        for (int group = 0; group < groups.size(); group++) if (groups.get(group) != null)
            for (int item : groups.get(group)) if (item >= 0) validateAudio(group, item, se, chain);
    }

    private void visitSkill(String file, int index, MekaContext self, String chain) throws IOException {
        Waz waz = waz(file, chain);
        require(validIndex(index, waz.getSkillList().size()), chain + " -> skill 越界, size=" + waz.getSkillList().size());
        if (!visited.add(LayeredAssetResolver.normalize(file) + "#" + index + "#" + self)) return;
        Waz.Skill skill = waz.getSkillList().get(index);
        if (skill == null || skill.getPhaseQuantity() == null) return; // 范围内空槽是合法池条目。
        for (int p = 0; p < skill.getPhasesInfo().size(); p++) {
            for (var unit : skill.getPhasesInfo().get(p).getSkillUnitCollection()) {
                for (int e = 0; e < unit.getSkillInfoObjectList().size(); e++) {
                    visitObject(unit.getSkillInfoObjectList().get(e), self, chain + ".phase[" + p + "].slot[" + unit.getUnitQuantity() + "].event[" + e + "]");
                }
            }
        }
    }

    private void visitObject(SkillInfoObject object, MekaContext self, String chain) throws IOException {
        if (object == null) return;
        if (object instanceof CEventSprite sprite) {
            Integer group = sprite.getSpmFileSequence();
            if (Objects.equals(group, -2)) return; // Map/parts 的运行时自动指定；不是静态 GRP 文件号。
            if (Objects.equals(group, -1)) group = self == null ? null : self.spmIndex();
            require(group != null, chain + " -> 自动 SPM 缺少主 MEK 上下文");
            Spm spm = spm(group, chain);
            Integer animation = sprite.getActionGroupNumber();
            if (!Objects.equals(animation, -1)) validateAnimation(spm, animation, sprite.getActionNumber(), chain + " -> SpriteGroup[" + group + "]");
        }
        if (object instanceof CEventWazaSelect select) {
            visitWazReference(select.getWazFileNo(), select.getWazSequenceNo(), self, chain);
        }
        if (object instanceof CEventChange change) {
            for (int i = 0; i < change.getBsdxInfoCollectionList2().size(); i++) {
                var collection = change.getBsdxInfoCollectionList2().get(i);
                if (collection == null || collection.getTypeList() == null || collection.getTypeList().isEmpty()) continue;
                Integer mode = collection.getTypeList().get(0);
                if (!Objects.equals(mode, 1) && !Objects.equals(mode, 2)) continue; // 0/3 是 phase 消费者。
                List<Integer> params = collection.getParamList();
                Integer group = params == null || params.isEmpty() ? -1 : params.get(0);
                Integer skill = params == null || params.size() < 2 ? -1 : params.get(1);
                visitWazReference(group, skill, self, chain + ".Change.List2[" + i + "]");
            }
        }
        if (object instanceof CEventSe se) for (byte[] bytes : se.getByteDataList()) validateAudioBytes(bytes, true, chain);
        if (object instanceof CEventVoice voice) for (byte[] bytes : voice.getByteDataList()) validateAudioBytes(bytes, false, chain);
        for (Class<?> type = object.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !List.class.isAssignableFrom(field.getType()) || !field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) continue;
                try {
                    field.setAccessible(true);
                    if (!(field.get(object) instanceof List<?> list)) continue;
                    for (int i = 0; i < list.size(); i++) {
                        Object nested = list.get(i);
                        if (nested == null) continue;
                        Object data = nested.getClass().getMethod("getData").invoke(nested);
                        if (data instanceof SkillInfoObject child) visitObject(child, self, chain + "." + field.getName() + "[" + i + "]");
                    }
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(chain + " -> 嵌套事件无法读取", e); }
            }
        }
    }

    private void visitWazReference(Integer group, Integer skill, MekaContext self, String chain) throws IOException {
        require(skill != null, chain + " -> WAZ skill 未解析");
        if (skill < 0) return; // native 不消费负 skill，不应先校验其未使用的 group。
        require(group != null, chain + " -> WAZ group 未解析");
        if (group < 0) group = self == null ? null : self.wazIndex();
        require(wazas != null && validIndex(group, wazas.getWazaList().size()), chain + " -> WazaGroup[" + group + "] 越界/缺少 MEK 自身上下文");
        var entry = wazas.getWazaList().get(group);
        require(entry != null && enabled(entry.getExistFlag()) && entry.getWazaDisplayName() != null, chain + " -> 禁用 WazaGroup[" + group + "]");
        String target = entry.getWazaDisplayName() + ".waz";
        visitSkill(target, skill, self, chain + " -> " + target + "[" + skill + "]");
    }

    private void validateMekVoices(Mek mek, String name) throws IOException {
        if (mek.getMekVoiceInfo() == null || mek.getMekVoiceInfo().getTable() == null) return;
        var table = mek.getMekVoiceInfo().getTable();
        for (int row = 0; row < table.size(); row++) for (int column = 0; column < table.get(row).size(); column++) {
            var cell = table.get(row).get(column);
            for (int item = 0; item < cell.size(); item++) {
                var reference = cell.get(item);
                String chain = name + " -> VoiceInfo.table[" + row + "][" + column + "][" + item + "]";
                require(reference != null && reference.getGroupId() != null, chain + " 语音号未解析");
                if (reference.getGroupId() < 0) continue;
                Integer override = reference.getVoiceType();
                require(override != null, chain + " 覆盖语音组未解析");
                Integer group = override < 0 ? mek.getMekVoiceInfo().getVersion() : override;
                require(group != null && group >= 0, chain + " 默认/覆盖语音组缺失");
                require(voices != null && validIndex(group, voices.getVoiceList().size()), chain + " -> BatVoice[" + group + "] 越界");
                var voiceGroup = voices.getVoiceList().get(group);
                require(enabled(voiceGroup.getExistFlag()), chain + " -> 禁用 BatVoice[" + group + "]");
                if (!validIndex(reference.getGroupId(), voiceGroup.getVoices().size())) {
                    // Nagi源表已有101/102等未提供的语音号；666150/667F30对此返回0、不播放。
                    assets.getUnresolvedMekVoiceReferences().putIfAbsent(chain, "BatVoice[" + group + "][" + reference.getGroupId() + "]");
                    continue;
                }
                validateAudio(group, reference.getGroupId(), false, chain);
            }
        }
    }

    private void validateAudioBytes(byte[] bytes, boolean se, String chain) throws IOException {
        require(bytes != null && bytes.length == (se ? 16 : 12), chain + " -> 音频引用结构损坏");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int group = buffer.getInt(), item = buffer.getInt();
        if (group == -1 || item == -1) return; // Voice 的默认类型/默认组由机体运行时决定。
        validateAudio(group, item, se, chain);
    }

    private void validateAudio(int group, int item, boolean se, String chain) throws IOException {
        String filename;
        if (se) {
            require(sounds != null && validIndex(group, sounds.getSeList().size()), chain + " -> SeGroup[" + group + "] 越界");
            var entry = sounds.getSeList().get(group);
            require(enabled(entry.getExistFlag()) && validIndex(item, entry.getSeItems().size()), chain + " -> SeGroup[" + group + "][" + item + "] 越界/禁用");
            var sound = entry.getSeItems().get(item);
            require(enabled(sound.getExistFlag()), chain + " -> 禁用 SE[" + group + "][" + item + "]"); filename = sound.getSeFileName();
        } else {
            require(voices != null && validIndex(group, voices.getVoiceList().size()), chain + " -> BatVoice[" + group + "] 越界");
            var entry = voices.getVoiceList().get(group);
            require(enabled(entry.getExistFlag()) && validIndex(item, entry.getVoices().size()), chain + " -> BatVoice[" + group + "][" + item + "] 越界/禁用");
            var voice = entry.getVoices().get(item);
            // 原生667F30遇到空语音指针返回0；保留表中显式禁用的无声占位项。
            if (!enabled(voice.getExistFlag())) return;
            filename = voice.getVoiceFileName();
        }
        require(filename != null && resolver.resolveMedia(filename, true) != null, chain + " -> 必需音频缺失: " + filename);
    }

    private Spm spm(Integer group, String chain) throws IOException {
        require(sprites != null && validIndex(group, sprites.getSpriteList().size()), chain + " -> SpriteGroup[" + group + "] 越界");
        var entry = sprites.getSpriteList().get(group);
        require(entry != null && enabled(entry.getExistFlag()) && entry.getSpriteFileName() != null, chain + " -> 禁用 SpriteGroup[" + group + "]");
        Spm spm = binary(entry.getSpriteFileName(), Spm.class, fromMap(baseline == null ? null : baseline.getSpmByFileName(), entry.getSpriteFileName()), request.getBsdxSpmDir());
        require(spm != null, chain + " -> SPM 载荷缺失: " + entry.getSpriteFileName()); return spm;
    }

    private void validateAnimation(Spm spm, Integer animation, Integer pattern, String chain) throws IOException {
        require(validIndex(animation, spm.getAnimData().size()), chain + " -> actionGroup[" + animation + "] 越界, size=" + spm.getAnimData().size());
        var data = spm.getAnimData().get(animation);
        require(pattern == null || pattern == -1 || validIndex(pattern, data.getPatData().size()), chain + " -> action[" + pattern + "] 越界, size=" + data.getPatData().size());
        // 动画会自行播放后续 patterns，因此验收整组真正使用的 page/chip 媒体。
        for (Integer page : SpmAnimationPages.reachablePages(spm, data)) {
            require(validIndex(page, spm.getPageData().size()), chain + " -> page[" + page + "] 越界");
            for (var chip : spm.getPageData().get(page).getChipData()) {
                require(validIndex(chip.getImageNo(), spm.getImageData().size()), chain + " -> imageNo[" + chip.getImageNo() + "] 越界");
                String image = spm.getImageData().get(chip.getImageNo()).getImageName();
                resolver.auditImageReference(image, chain + " -> image " + image);
            }
        }
    }

    private void validateSpmStructure(Spm spm, String chain) {
        require(spm.getNumAnimData() == spm.getAnimData().size() && spm.getNumPageData() == spm.getPageData().size()
                && spm.getNumImageData() == spm.getImageData().size(), chain + " -> SPM 数量与载荷不匹配");
        for (var anim : spm.getAnimData()) {
            require((anim.getNumPat() & 65535) == anim.getPatData().size(), chain + " -> pattern数量不匹配");
            for (var pat : anim.getPatData()) {
                require(pat.getPageNo().size() == spm.getPatPageNum(), chain + " -> pattern page数量不匹配");
                for (Integer page : pat.getPageNo()) require(page != null && (page == -1 || validIndex(page, spm.getPageData().size())), chain + " -> page[" + page + "] 越界");
            }
        }
        for (var page : spm.getPageData()) for (var chip : page.getChipData()) require(validIndex(chip.getImageNo(), spm.getImageData().size()), chain + " -> imageNo[" + chip.getImageNo() + "] 越界");
    }

    private Mek mek(String name) throws IOException { return binary(name, Mek.class, fromMap(baseline == null ? null : baseline.getMekByFileName(), name), request.getBsdxMekDir()); }
    private Waz waz(String name, String chain) throws IOException {
        Waz waz = binary(name, Waz.class, fromMap(baseline == null ? null : baseline.getWazByFileName(), name), request.getBsdxWazDir());
        require(waz != null, chain + " -> WAZ 载荷缺失: " + name); return waz;
    }
    private <T> T binary(String name, Class<T> type, T fallback, Path original) throws IOException {
        String key = LayeredAssetResolver.normalize(name);
        if (cache.containsKey(key)) return type.cast(cache.get(key));
        Path path = resolver.resolveFile(assets.getOutputRootDir(), name);
        if (path == null) path = resolver.resolveFile(request.getJinkiGeneratedAssetDir(), name);
        if (path == null) path = resolver.resolveFile(original, name);
        // mutable baseline DTO 只能辅助传递验证范围；验收对象必须有有效加载层的真实 bytes。
        T value = path == null ? null : type.cast(parse(path, name));
        cache.put(key, value); return value;
    }
    private Object parse(Path path, String chain) throws IOException {
        try { return service.parse(path.toString(), "windows-31j").getData(); }
        catch (RuntimeException e) { throw new IllegalStateException(chain + " -> 重解析失败: " + path, e); }
    }
    private <T> T fromMap(Map<String, T> values, String name) {
        if (values != null) for (var entry : values.entrySet()) if (LayeredAssetResolver.normalize(entry.getKey()).equals(LayeredAssetResolver.normalize(name))) return entry.getValue();
        return null;
    }
    private static boolean validIndex(Integer index, int size) { return index != null && index >= 0 && index < size; }
    private static boolean enabled(Integer flag) { return flag != null && flag != 0; }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
