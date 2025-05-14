package com.giga.nexas.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BinService;
import com.giga.nexas.service.PacService;
import javafx.scene.control.TreeItem;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static com.giga.nexas.controller.consts.MainConst.*;

@RequiredArgsConstructor
public class ActionButtonController {

    private final MainViewController view;

    public void bind() {
        view.getActionButton().setOnAction(e -> {
            File file = new File(view.getInputField().getText());
            if (!file.exists()) {
                view.getLogArea().appendText("⚠ 输入路径无效\n");
                return;
            }

            TreeItem<String> selected = view.getTree()
                    .getSelectionModel()
                    .getSelectedItem();
            if (selected == null || selected.getParent() == null) {
                view.getLogArea().appendText("⚠ 请选择子功能！\n");
                return;
            }
            String func = selected.getValue();

            // 根据子功能名称调用对应方法
            switch (func) {
                case UNPAC -> unPac(file);
                case PAC -> pac(file);
                case PARSE -> parse(file);
                case GENERATE -> generate(file);
                default     -> view.getLogArea()
                        .appendText("⚠ 未知功能: " + func + "\n");
            }
        });
    }

    private void parse(File selectedFile) {
        try {
            BinService service = new BinService();
            Object result = service.parse(selectedFile.getAbsolutePath(), "windows-31j").getData();
            String json = JSONUtil.toJsonStr(result);
            File outputFile = new File(view.getOutputField().getText(), FileUtil.getName(selectedFile) + ".json");
            FileUtil.writeUtf8String(json, outputFile);
            view.getLogArea().appendText("✔ 解析成功: " + selectedFile.getName() + "\n");
            view.getLogArea().appendText("✔ JSON 文件输出至:\n" + outputFile.getAbsolutePath() + "\n");
        } catch (Exception e) {
            view.getLogArea().appendText("⚠ 解析失败: " + e + "\n");
            e.printStackTrace();
        }
    }

    private void generate(File selectedFile) {
        try {
            String jsonStr = FileUtil.readUtf8String(selectedFile);
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Bsdx obj;
            String ext = objectMapper.readValue(jsonStr, Bsdx.class).getExtensionName().toLowerCase();
            obj = switch (ext) {
                case WAZ_EXT -> objectMapper.readValue(jsonStr, Waz.class);
                case MEK_EXT -> objectMapper.readValue(jsonStr, Mek.class);
                default -> throw new IllegalArgumentException("不支持的扩展名: " + ext);
            };

            File outputPath = new File(view.getOutputField().getText(), FileUtil.mainName(selectedFile));
            new BinService().generate(outputPath.getAbsolutePath(), obj, "windows-31j");

            view.getLogArea().appendText("✔ 已生成游戏文件:\n" + outputPath.getAbsolutePath() + "\n");
        } catch (Exception ex) {
            view.getLogArea().appendText("⚠ 生成失败: " + ex + "\n");
            ex.printStackTrace();
        }
    }

    private void unPac(File input) {
        try {
            ResponseDTO result = new PacService().unPac(input.getAbsolutePath());
            view.getLogArea().appendText("✔ 解包详情:\n" + result.getMsg() + "\n");
        } catch (Exception e) {
            view.getLogArea().appendText("⚠ 解包失败: " + e + "\n");
            e.printStackTrace();
        }
    }

    private void pac(File input) {
        try {
            ResponseDTO result = new PacService().pac(input.getAbsolutePath(), "4");
            view.getLogArea().appendText("✔ 封包详情:\n" + result.getMsg() + "\n");
        } catch (Exception e) {
            view.getLogArea().appendText("⚠ 封包失败: " + e + "\n");
            e.printStackTrace();
        }
    }
}
