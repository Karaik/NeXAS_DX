package com.giga.nexas.bsdx;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MainView.fxml 的烟雾测试。
 *
 * <p>这条测试验证主界面 FXML 在 JavaFX 线程上完整加载。
 */
class MainViewFxmlSmokeTest {

    /**
     * 初始化 JavaFX toolkit。
     */
    @BeforeAll
    static void bootstrapJavaFx() throws Exception {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // toolkit already started
        }
    }

    /**
     * 验证 MainView.fxml 的完整加载。
     */
    @Test
    void mainViewFxmlLoadsSuccessfully() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Parent> loaded = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(MainViewFxmlSmokeTest.class.getResource("/fxml/MainView.fxml"));
                loaded.set(loader.load());
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        Assertions.assertTrue(latch.await(20, TimeUnit.SECONDS), "Timed out while loading MainView.fxml");
        if (error.get() != null) {
            Assertions.fail("Failed to load MainView.fxml: " + error.get().getMessage());
        }
        Assertions.assertNotNull(loaded.get());
    }
}
