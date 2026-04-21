package com.giga.nexas.bsdx;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellEditorReferenceData;
import com.giga.nexas.controller.model.MapLookupEntry;
import com.giga.nexas.controller.model.MapPreviewDescriptor;
import com.giga.nexas.controller.model.MekaLookupEntry;
import com.giga.nexas.controller.support.HellEditorReferenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hell 编辑器引用数据层的真实资源回归测试。
 *
 * <p>这组测试覆盖：
 * 1. 机体速查表是否仍然从 `MekaGroup.grp` 正确建表
 * 2. 地图预览是否已经切到 `T_map*.bmp` 真实资源链
 */
class HellEditorReferenceServiceTest {

    private static final Path GAME_GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");
    private static final String CHARSET = "windows-31j";

    /**
     * 被测引用数据服务。
     */
    private final HellEditorReferenceService service = new HellEditorReferenceService();

    /**
     * 验证 `MekaGroup.grp` 的编号仍然能被整理成 `mekaIndex -> 机体` 速查表。
     */
    @Test
    void loadMekaAndMapReferenceDataFromRealGrpFiles() throws Exception {
        if (!Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("hell-editor-reference-root");
        try {
            copyRequired(root, GAME_GRP_DIR.resolve("MekaGroup.grp"));
            copyRequired(root, GAME_GRP_DIR.resolve("MapGroup.grp"));
            copyRequired(root, Paths.get("src/main/resources/game/bsdx/mek/Noi.mek"));

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(root);
            HellEditorReferenceData referenceData = service.loadReferenceData(session, CHARSET);

            MekaLookupEntry noi = referenceData.getMekaByIndex().get(21);
            Assertions.assertNotNull(noi);
            Assertions.assertEquals(21, noi.getMekaIndex());
            Assertions.assertEquals("Noi", noi.getMekaName());

            MapLookupEntry arena02 = referenceData.getMapById().get(5);
            Assertions.assertNotNull(arena02);
            Assertions.assertEquals("ARENA02", arena02.getGroupCodeName());
            Assertions.assertEquals("mapARENA_L02_01", arena02.getGroupResourceName());
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 验证地图预览当前按 `MapGroup.grp -> T_map*.bmp` 真实链命中图片。
     *
     * <p>这里选 `mapId=5`，因为它的 `groupResourceName` 稳定是 `mapARENA_L02_01`，
     * 适合直接验证命名规则和最终资源路径。
     */
    @Test
    void resolveMapPreviewUsesMapGroupAndPreviewBitmapName() throws Exception {
        if (!Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("hell-editor-map-preview-root");
        try {
            copyRequired(root, GAME_GRP_DIR.resolve("MekaGroup.grp"));
            copyRequired(root, GAME_GRP_DIR.resolve("MapGroup.grp"));

            Path previewImage = root.resolve("T_mapARENA_L02_01.bmp");
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(image, "bmp", previewImage.toFile());

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(root);
            HellEditorReferenceData referenceData = service.loadReferenceData(session, CHARSET);
            MapPreviewDescriptor descriptor = service.resolveMapPreview(session, CHARSET, referenceData.getMapById().get(5));
            BufferedImage rendered = service.renderMapPreviewImage(session, CHARSET, descriptor);

            Assertions.assertNotNull(descriptor);
            Assertions.assertEquals("T_mapARENA_L02_01.bmp", descriptor.getPreviewImageName());
            Assertions.assertEquals(previewImage.toAbsolutePath().normalize(), descriptor.getPreviewImagePath().toAbsolutePath().normalize());
            Assertions.assertNotNull(rendered);
            Assertions.assertEquals(16, rendered.getWidth());
            Assertions.assertEquals(16, rendered.getHeight());
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 把指定资源复制到测试平铺目录。
     */
    private void copyRequired(Path targetRoot, Path source) throws Exception {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Missing source resource: " + source);
        }
        Files.copy(source, targetRoot.resolve(source.getFileName().toString()));
    }

    /**
     * 清理测试目录。
     */
    private void deleteRecursively(Path root) throws Exception {
        Files.walk(root)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> path.toFile().delete());
    }
}
