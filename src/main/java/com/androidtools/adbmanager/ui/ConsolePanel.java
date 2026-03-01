package com.androidtools.adbmanager.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 控制台输出面板
 */
public class ConsolePanel extends VBox {
    
    private TextArea consoleArea;
    
    public ConsolePanel() {
        buildUI();
    }
    
    private void buildUI() {
        getStyleClass().add("console-panel");
        setPadding(new Insets(10));
        setSpacing(8);
        
        // 标题区域
        VBox titleSection = new VBox(4);
        Label titleLabel = new Label("控制台输出");
        titleLabel.getStyleClass().add("title-label");
        
        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.getStyleClass().add("title-separator");
        
        titleSection.getChildren().addAll(titleLabel, separator);
        
        // 控制台文本区域
        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.getStyleClass().add("console-area");
        
        consoleArea.setPromptText("等待操作...");
        
        VBox.setVgrow(consoleArea, Priority.ALWAYS);
        
        getChildren().addAll(titleSection, consoleArea);
    }
    
    public void appendLog(String message) {
        // 使用 Platform.runLater 确保在 JavaFX 应用线程上执行
        // 避免并发访问导致的渲染问题
        if (Platform.isFxApplicationThread()) {
            consoleArea.appendText(message + "\n");
        } else {
            Platform.runLater(() -> consoleArea.appendText(message + "\n"));
        }
    }
    
    public void clearLog() {
        if (Platform.isFxApplicationThread()) {
            consoleArea.clear();
        } else {
            Platform.runLater(() -> consoleArea.clear());
        }
    }
}
