package com.giga.nexas.controller;

import javafx.scene.control.TreeItem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

import static com.giga.nexas.controller.consts.MainConst.*;

@RequiredArgsConstructor
public class FilePickerController {

    private final MainViewController view;
    private boolean userSelectedOutput = false;

    // 全局路径变量
    private static final String PREF_INPUT_PATH = "lastInputPath";
    private static final String PREF_OUTPUT_PATH = "lastOutputPath";
    private final Preferences prefs = Preferences.userNodeForPackage(FilePickerController.class);

    public void setup() {

//        prefs.remove(PREF_INPUT_PATH);
//        prefs.remove(PREF_OUTPUT_PATH);

        // 初始化读取上次路径
        loadLastPaths();

        // 输入路径选择逻辑
        bindInputBrowse();

        // 输出路径选择逻辑
        bindOutputBrowse();
    }

    private void loadLastPaths() {
        String lastInput = prefs.get(PREF_INPUT_PATH, "");
        String lastOutput = prefs.get(PREF_OUTPUT_PATH, "");
        if (!lastInput.isEmpty()) {
            view.getInputField().setText(lastInput);
        }
        if (!lastOutput.isEmpty()) {
            view.getOutputField().setText(lastOutput);
        }

        // 如果lastInput存在，自动选择功能
        if (!lastInput.isEmpty()) {
            autoSelectFunction(new File(lastInput));
        }
    }

    private void bindInputBrowse() {
        view.getInputBrowse().setOnAction(e -> {
            Stage stage = (Stage) view.getRoot().getScene().getWindow();
            File selected;

            boolean isPack = false;
            var sel = view.getTree().getSelectionModel().getSelectedItem();
            if (sel != null && PAC.equals(sel.getValue())) {
                isPack = true;
            }

            if (isPack) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("选择输入文件夹");

                File current = new File(view.getInputField().getText());
                if (current.exists() && current.isDirectory()) {
                    chooser.setInitialDirectory(current);
                }

                selected = chooser.showDialog(stage);
            } else {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("选择输入文件");

                File current = new File(view.getInputField().getText());
                if (current.exists()) {
                    chooser.setInitialDirectory(current.isDirectory() ? current : current.getParentFile());
                }

                selected = chooser.showOpenDialog(stage);
            }

            if (selected != null) {
                view.getInputField().setText(selected.getAbsolutePath());
                // 保存全局变量
                prefs.put(PREF_INPUT_PATH, selected.getAbsolutePath());

                // 手动选择时自动判断一个操作
                autoSelectFunction(selected);

                // 只有在用户没有手动指定输出目录时，才自动填充
                if (!userSelectedOutput) {
                    String absolutePath = selected.getParentFile().getAbsolutePath();
                    view.getOutputField().setText(absolutePath);
                    prefs.put(PREF_OUTPUT_PATH, absolutePath);
                }
            }
        });
    }

    private void bindOutputBrowse() {
        view.getOutputBrowse().setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择输出目录");

            File current = new File(view.getOutputField().getText());
            if (current.exists() && current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }

            File selected = chooser.showDialog(view.getRoot().getScene().getWindow());
            if (selected != null) {
                view.getOutputField().setText(selected.getAbsolutePath());
                prefs.put(PREF_OUTPUT_PATH, selected.getAbsolutePath());
                userSelectedOutput = true;
            }
        });
    }

    private void autoSelectFunction(File file) {
        String path = file.getAbsolutePath();
        String autoFunc;

        if (file.isDirectory()) {
            autoFunc = PAC;
        } else if (path.toLowerCase().contains(".pac")) {
            autoFunc = UNPAC;
        } else if (path.toLowerCase().endsWith(".json")) {
            autoFunc = GENERATE;
        } else if (file.isFile()) {
            autoFunc = PARSE;
        } else {
            autoFunc = null;
        }

        // 如果匹配到了功能 自动选中对应子功能
        if (autoFunc != null) {
            TreeItem<String> item = findTreeItemByValue(view.getTree().getRoot(), autoFunc, new HashSet<>());
            if (item != null) {
                view.getTree().getSelectionModel().select(item);
            }
        }
    }

    private TreeItem<String> findTreeItemByValue(TreeItem<String> root, String value, Set<TreeItem<String>> visited) {
        if (root == null || visited.contains(root)) return null;
        visited.add(root);

        if (value.equals(root.getValue())) return root;
        for (TreeItem<String> child : root.getChildren()) {
            TreeItem<String> result = findTreeItemByValue(child, value, visited);
            if (result != null) return result;
        }
        return null;
    }
}
