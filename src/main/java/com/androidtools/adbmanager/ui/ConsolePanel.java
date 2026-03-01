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
        setSpacing(10);
        setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        setPadding(new Insets(15));
        
        // 标题区域
        VBox titleSection = new VBox(6);
        Label titleLabel = new Label("控制台输出");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: linear-gradient(from 0% 0% to 100% 0%, #667eea 0%, #764ba2 100%);");
        
        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #667eea 0%, #764ba2 100%); -fx-background-radius: 1px;");
        
        titleSection.getChildren().addAll(titleLabel, separator);
        
        // 控制台文本区域
        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.setStyle(
            "-fx-background-color: #f7fafc; " +
            "-fx-text-fill: #2d3748; " +
            "-fx-font-family: 'Consolas', 'Monaco', monospace; " +
            "-fx-font-size: 12px; " +
            "-fx-background-radius: 6px; " +
            "-fx-border-color: transparent; " +
            "-fx-border-radius: 6px; " +
            "-fx-padding: 8px;"
        );
        
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
