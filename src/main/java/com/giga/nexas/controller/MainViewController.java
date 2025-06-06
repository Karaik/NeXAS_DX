package com.giga.nexas.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import lombok.Getter;

@Getter
public class MainViewController {

    @FXML private BorderPane root;
    @FXML private MenuBar menuBar;
    @FXML private TreeView<String> tree;
    @FXML private Button inputBrowse;
    @FXML private Button outputBrowse;
    @FXML private TextField inputField;
    @FXML private TextField outputField;
    @FXML private TextArea logArea;
    @FXML private ProgressBar progress;
//    @FXML private Label statusLabel;
    @FXML private Button actionButton;
    @FXML private CheckBox alwaysOnTop;
    @FXML private RadioButton modePackRadio;
    @FXML private RadioButton modeAnalyzeRadio;

    @FXML
    public void initialize() {
        // 绑定解包封包 控制当前操作模式
        new ToggleModeController(this).bind();

        // 设置左侧模式树形结构切换内容
        new ModeTreeController(this).setup();

        // 设置“设置”菜单栏及其弹出设置窗口逻辑
        new SettingsMenuController(this).setup();

        // 设置日志区域的右键菜单（如清空日志）
        new LogContextMenuController(this).setup();

        // 设置“输入”、“输出”路径选择按钮及其默认输出路径逻辑
        new FilePickerController(this).setup();

        // 实现“总在最前”复选框控制窗口置顶状态
        new AlwaysOnTopController(this).bind();

        // 绑定“开始”按钮，执行解包 / 封包操作
        new ActionButtonController(this).bind();


    }

}
