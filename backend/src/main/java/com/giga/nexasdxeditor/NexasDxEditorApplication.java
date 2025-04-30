package com.giga.nexasdxeditor;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class NexasDxEditorApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 输入框
        JFXTextField input = new JFXTextField();
        input.setPromptText("请输入内容");

        // 按钮
        JFXButton button = new JFXButton("提交");
        button.setOnAction(e -> {
            System.out.println("输入内容: " + input.getText());
        });

        // 布局
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 40; -fx-alignment: center;");
        root.getChildren().addAll(input, button);

        // 场景设置
        Scene scene = new Scene(root, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NeXAS Dx Editor");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
