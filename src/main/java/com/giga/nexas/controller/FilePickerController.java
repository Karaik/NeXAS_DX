package com.giga.nexas.controller;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static com.giga.nexas.controller.consts.MainConst.PAC;

@RequiredArgsConstructor
public class FilePickerController {

    private final MainViewController view;
    private boolean userSelectedOutput = false;

    public void setup() {
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

                // 只有在用户没有手动指定输出目录时，才自动填充
                if (!userSelectedOutput) {
                    String absolutePath = selected.getParentFile().getAbsolutePath();
                    view.getOutputField().setText(absolutePath);
                }
            }
        });

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
                // 标记为用户手动指定了输出路径
                userSelectedOutput = true;
            }
        });
    }

}
