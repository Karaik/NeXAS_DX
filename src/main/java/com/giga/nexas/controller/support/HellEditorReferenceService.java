package com.giga.nexas.controller.support;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellEditorReferenceData;
import com.giga.nexas.controller.model.MapLookupEntry;
import com.giga.nexas.controller.model.MapPreviewDescriptor;
import com.giga.nexas.controller.model.MekaLookupEntry;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.service.BsdxBinService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hell 编辑器的引用数据与预览解析服务。
 *
 * <p>这一层属于编辑器引用层。
 * 它负责读取 `MekaGroup.grp`、`MapGroup.grp` 和 `.mek`，
 * 并且把 Hell 地图预览解析到 exe 当前实际使用的 `T_map*.bmp` 资源链。
 *
 * <p>输入是当前 BSDX 平铺资源目录的覆盖会话和字符集。
 * 输出是：
 * 1. 机体速查表
 * 2. 地图索引表
 * 3. 某个地图 id 对应的真实预览图路径
 */
public class HellEditorReferenceService {

    private static final String MEKA_GROUP_FILE = "MekaGroup.grp";
    private static final String MAP_GROUP_FILE = "MapGroup.grp";
    private static final String MAP_PREVIEW_PREFIX = "T_";
    private static final String MAP_PREVIEW_EXTENSION = ".bmp";

    /**
     * 二进制解析服务。
     *
     * <p>这一层继续复用仓库现有解析器，不额外引入新的资源读取路径。
     */
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    /**
     * 加载当前会话的机体与地图引用快照。
     *
     * <p>这个方法把 `MekaGroup.grp` 与 `MapGroup.grp` 整理成
     * GUI 可直接使用的索引结构。
     * 这一段不丢失索引号和资源名信息。
     */
    public HellEditorReferenceData loadReferenceData(BsdxOverlayResourceSession session, String charset) throws Exception {
        if (session == null) {
            throw new IllegalArgumentException("BSDX overlay session is required.");
        }

        List<MekaLookupEntry> mekaEntries = loadMekaEntries(session, charset);
        Map<Integer, MekaLookupEntry> mekaByIndex = new LinkedHashMap<>();
        for (MekaLookupEntry entry : mekaEntries) {
            mekaByIndex.put(entry.getMekaIndex(), entry);
        }

        List<MapLookupEntry> mapEntries = loadMapEntries(session, charset);
        Map<Integer, MapLookupEntry> mapById = new LinkedHashMap<>();
        for (MapLookupEntry entry : mapEntries) {
            mapById.put(entry.getMapId(), entry);
        }

        return HellEditorReferenceData.builder()
                .mekaEntries(List.copyOf(mekaEntries))
                .mekaByIndex(Map.copyOf(mekaByIndex))
                .mapEntries(List.copyOf(mapEntries))
                .mapById(Map.copyOf(mapById))
                .build();
    }

    /**
     * 解析某个地图 id 对应的真实预览图。
     *
     * <p>这一段复用 exe 当前的命名规则：
     * `mapId -> MapGroup.grp.groupResourceName -> T_ + resourceName + .bmp`。
     *
     * <p>输出结果保留真实资源文件名与命中路径，不再走旧的 `SPM -> PNG` 推导。
     * 这保证编辑器显示的就是引擎正在按名称查找的那张图。
     */
    public MapPreviewDescriptor resolveMapPreview(
            BsdxOverlayResourceSession session,
            String charset,
            MapLookupEntry mapEntry
    ) {
        if (session == null || mapEntry == null) {
            return null;
        }

        String previewImageName = buildPreviewBitmapFileName(mapEntry.getGroupResourceName());
        if (previewImageName == null) {
            return MapPreviewDescriptor.builder()
                    .mapEntry(mapEntry)
                    .statusText("Preview image name is unavailable for this map.")
                    .build();
        }

        Path previewImagePath = session.resolveReadPath(previewImageName).orElse(null);
        String statusText = previewImagePath != null
                ? "Preview image resolved: " + previewImageName
                : "Preview image not found: " + previewImageName;

        return MapPreviewDescriptor.builder()
                .mapEntry(mapEntry)
                .previewImageName(previewImageName)
                .previewImagePath(previewImagePath)
                .previewImageNames(List.of(previewImageName))
                .statusText(statusText)
                .build();
    }

    /**
     * 读取当前命中的真实预览图。
     *
     * <p>这一步和 round-trip 无关，但它保持资源命中 lossless：
     * `resolveMapPreview` 解析到哪张图，这里就直接读取哪张图，不做二次猜测。
     */
    public BufferedImage renderMapPreviewImage(
            BsdxOverlayResourceSession session,
            String charset,
            MapPreviewDescriptor descriptor
    ) throws Exception {
        if (descriptor == null || descriptor.getPreviewImagePath() == null) {
            return null;
        }
        return ImageIO.read(descriptor.getPreviewImagePath().toFile());
    }

    /**
     * 从 `MekaGroup.grp` 建立机体索引表。
     *
     * <p>机体编号来自 `MekaGroup.grp` 的下标。
     * `.mek` 只用于补充显示名，不参与编号定义。
     */
    private List<MekaLookupEntry> loadMekaEntries(BsdxOverlayResourceSession session, String charset) throws Exception {
        MekaGroupGrp mekaGroupGrp = loadGrp(session, charset, MEKA_GROUP_FILE, MekaGroupGrp.class);
        List<MekaLookupEntry> result = new ArrayList<>();
        List<MekaGroupGrp.MekaGroup> mekaList = mekaGroupGrp.getMekaList() == null ? List.of() : mekaGroupGrp.getMekaList();

        for (int mekaIndex = 0; mekaIndex < mekaList.size(); mekaIndex++) {
            MekaGroupGrp.MekaGroup group = mekaList.get(mekaIndex);
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }

            String mekFileName = group.getMekaName() == null ? null : group.getMekaName() + ".mek";
            Mek mek = tryLoadMek(session, charset, mekFileName);

            result.add(MekaLookupEntry.builder()
                    .mekaIndex(mekaIndex)
                    .mekaName(group.getMekaName())
                    .mekaCodeName(group.getMekaCodeName())
                    .mekFileName(mekFileName)
                    .displayName(mek == null || mek.getMekBasicInfo() == null ? group.getMekaName() : mek.getMekBasicInfo().getMekName())
                    .displayNameEnglish(mek == null || mek.getMekBasicInfo() == null ? null : mek.getMekBasicInfo().getMekNameEnglish())
                    .build());
        }
        return result;
    }

    /**
     * 从 `MapGroup.grp` 建立地图索引表。
     *
     * <p>地图 id 来自 `MapGroup.grp.groupList` 的下标。
     * `groupResourceName` 就是后续拼 `T_map*.bmp` 的基础资源名。
     */
    private List<MapLookupEntry> loadMapEntries(BsdxOverlayResourceSession session, String charset) throws Exception {
        MapGroupGrp mapGroupGrp = loadGrp(session, charset, MAP_GROUP_FILE, MapGroupGrp.class);
        List<MapLookupEntry> result = new ArrayList<>();
        List<MapGroupGrp.MapGroup> groups = mapGroupGrp.getGroupList() == null ? List.of() : mapGroupGrp.getGroupList();

        for (int mapId = 0; mapId < groups.size(); mapId++) {
            MapGroupGrp.MapGroup group = groups.get(mapId);
            if (group == null || group.getExistFlag() == 0) {
                continue;
            }
            result.add(MapLookupEntry.builder()
                    .mapId(mapId)
                    .groupName(group.getGroupName())
                    .groupCodeName(group.getGroupCodeName())
                    .groupResourceName(group.getGroupResourceName())
                    .build());
        }
        return result;
    }

    /**
     * 按 BSDX 地图预览的固定规则拼出 `T_map*.bmp` 文件名。
     */
    private String buildPreviewBitmapFileName(String resourceName) {
        if (resourceName == null || resourceName.isBlank()) {
            return null;
        }
        return MAP_PREVIEW_PREFIX + resourceName.trim() + MAP_PREVIEW_EXTENSION;
    }

    /**
     * 读取一份指定类型的 GRP。
     *
     * <p>这一段直接复用仓库已有解析路径。
     */
    private <T> T loadGrp(BsdxOverlayResourceSession session, String charset, String fileName, Class<T> type) throws Exception {
        Path path = session.resolveReadPath(fileName)
                .orElseThrow(() -> new IllegalStateException("Missing " + fileName));
        ResponseDTO<?> response = bsdxBinService.parse(path.toString(), charset);
        return type.cast(response.getData());
    }

    /**
     * 尝试读取 `.mek`。
     *
     * <p>这一步只服务于机体显示名增强。
     * 解析失败时直接回退到 `MekaGroup.grp` 原始名称，不中断整个引用表加载。
     */
    private Mek tryLoadMek(BsdxOverlayResourceSession session, String charset, String fileName) {
        try {
            if (fileName == null || !session.exists(fileName)) {
                return null;
            }
            Path path = session.resolveReadPath(fileName).orElse(null);
            if (path == null) {
                return null;
            }
            ResponseDTO<?> response = bsdxBinService.parse(path.toString(), charset);
            return (Mek) response.getData();
        } catch (Exception ex) {
            return null;
        }
    }
}
