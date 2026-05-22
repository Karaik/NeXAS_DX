package com.giga.nexas.controller;

import com.giga.nexas.controller.model.MainViewMode;
import javafx.scene.Node;

/**
 * 主界面页面模式的最小控制接口。
 *
 * <p>该接口只保留模式切换必需能力，避免 MainViewController 直接关心每个页面的内部实现。
 */
public interface ModeController {

    MainViewMode mode();

    Node content();

    void activate();

    void deactivate();

    boolean canLeave(String reason);
}
