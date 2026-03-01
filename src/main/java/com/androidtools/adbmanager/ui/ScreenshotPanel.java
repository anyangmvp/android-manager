package com.androidtools.adbmanager.ui;

import com.androidtools.adbmanager.manager.AdbManager;
import com.androidtools.adbmanager.manager.DeviceManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * 截屏功能面板
 */
public class ScreenshotPanel extends VBox {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.adb-manager";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.properties";
    private static final String KEY_SCREENSHOT_DIR = "screenshot.dir";
    
    private final DeviceManager deviceManager;
    private final AdbManager adbManager;
    private final ConsolePanel consolePanel;
    
    private TextField outputDirField;
    private Button browseDirButton;
    private TextField prefixField;
    private TextField suffixField;
    private Button screenshotButton;
    private Label statusLabel;
    
    public ScreenshotPanel(DeviceManager deviceManager, AdbManager adbManager, ConsolePanel consolePanel) {
        this.deviceManager = deviceManager;
        this.adbManager = adbManager;
        this.consolePanel = consolePanel;
        buildUI();
        loadSavedScreenshotDir();
    }
    
    private void buildUI() {
        getStyleClass().add("screenshot-panel");
        
        // 标题
        Label titleLabel = new Label("截屏功能");
        titleLabel.getStyleClass().add("title-label");
        
        // 输出目录设置
        VBox dirBox = createDirSection();
        
        // 文件名设置
        VBox nameBox = createFileNameSection();
        
        // 截屏按钮
        screenshotButton = new Button("开始截屏");
        screenshotButton.setPrefHeight(50);
        screenshotButton.getStyleClass().add("screenshot-button");
        screenshotButton.setOnAction(e -> takeScreenshot());
        
        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.getStyleClass().add("status-label");
        
        getChildren().addAll(titleLabel, dirBox, nameBox, screenshotButton, statusLabel);
    }
    
    private VBox createDirSection() {
        VBox container = new VBox(10);
        
        Label sectionLabel = new Label("输出目录：");
        sectionLabel.getStyleClass().add("section-label");
        
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        outputDirField = new TextField();
        outputDirField.setPromptText("选择截屏保存目录");
        outputDirField.setPrefWidth(500);
        outputDirField.setEditable(false);
        outputDirField.getStyleClass().add("text-field");
        
        browseDirButton = new Button("浏览...");
        browseDirButton.getStyleClass().add("browse-button");
        browseDirButton.setOnAction(e -> browseOutputDir());
        
        dirBox.getChildren().addAll(outputDirField, browseDirButton);
        container.getChildren().addAll(sectionLabel, dirBox);
        
        return container;
    }
    
    private VBox createFileNameSection() {
        VBox container = new VBox(10);
        
        Label sectionLabel = new Label("文件名设置：");
        sectionLabel.getStyleClass().add("section-label");
        
        GridPane nameGrid = new GridPane();
        nameGrid.setHgap(15);
        nameGrid.setVgap(10);
        
        Label prefixLabel = new Label("文件名前缀：");
        prefixLabel.getStyleClass().add("section-label");
        
        prefixField = new TextField("Screenshot_");
        prefixField.setPromptText("例如：Screenshot_");
        prefixField.setPrefWidth(200);
        prefixField.getStyleClass().add("text-field");
        
        Label suffixLabel = new Label("文件名后缀：");
        suffixLabel.getStyleClass().add("section-label");
        
        suffixField = new TextField("Home");
        suffixField.setPromptText("例如：Home");
        suffixField.setPrefWidth(200);
        suffixField.getStyleClass().add("text-field");
        
        Label hintLabel = new Label("最终文件名格式：前缀 + 后缀 + 时间戳.png");
        hintLabel.getStyleClass().add("hint-label");
        GridPane.setConstraints(hintLabel, 0, 2, 2, 1);
        
        nameGrid.add(prefixLabel, 0, 0);
        nameGrid.add(prefixField, 1, 0);
        nameGrid.add(suffixLabel, 0, 1);
        nameGrid.add(suffixField, 1, 1);
        nameGrid.add(hintLabel, 0, 2, 2, 1);
        
        container.getChildren().addAll(sectionLabel, nameGrid);
        return container;
    }
    
    /**
     * 加载配置属性
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                // 忽略错误
            }
        }
        return props;
    }

    /**
     * 保存配置属性
     */
    private void saveProperties(Properties props) {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                props.store(fos, "ADB Manager Configuration");
            }
        } catch (IOException e) {
            consolePanel.appendLog("[警告] 无法保存配置：" + e.getMessage());
        }
    }

    /**
     * 加载保存的截屏目录
     */
    private void loadSavedScreenshotDir() {
        Properties props = loadProperties();
        String savedDir = props.getProperty(KEY_SCREENSHOT_DIR);
        if (savedDir != null && !savedDir.isEmpty() && Files.exists(Paths.get(savedDir))) {
            if (outputDirField != null) {
                outputDirField.setText(savedDir);
            }
        }
    }

    /**
     * 保存截屏目录
     */
    private void saveScreenshotDir(String dir) {
        Properties props = loadProperties();
        props.setProperty(KEY_SCREENSHOT_DIR, dir);
        saveProperties(props);
    }
    
    private void browseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择截屏保存目录");
        
        // 使用上次选择的目录
        String currentDir = outputDirField.getText();
        if (currentDir != null && !currentDir.isEmpty()) {
            File currentFile = new File(currentDir);
            if (currentFile.exists()) {
                chooser.setInitialDirectory(currentFile);
            }
        }
        
        File selected = chooser.showDialog(null);
        if (selected != null) {
            outputDirField.setText(selected.getAbsolutePath());
            saveScreenshotDir(selected.getAbsolutePath()); // 保存目录
        }
    }
    
    private void takeScreenshot() {
        // 检查设备
        if (deviceManager.getSelectedDevice() == null) {
            consolePanel.appendLog("[错误] 请先选择设备");
            statusLabel.setText("请先选择设备");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-info");
            statusLabel.getStyleClass().add("status-label-error");
            return;
        }
        
        // 检查输出目录
        String outputDir = outputDirField.getText();
        if (outputDir == null || outputDir.isEmpty()) {
            consolePanel.appendLog("[错误] 请先选择输出目录");
            statusLabel.setText("请先选择输出目录");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-info");
            statusLabel.getStyleClass().add("status-label-error");
            return;
        }
        
        // 生成文件名
        String prefix = prefixField.getText();
        String suffix = suffixField.getText();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = prefix + suffix + "_" + timestamp + ".png";
        
        String outputPath = new File(outputDir, fileName).getAbsolutePath();
        
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("开始截屏");
        consolePanel.appendLog("设备：" + deviceManager.getSelectedDevice().getDeviceName());
        consolePanel.appendLog("保存路径：" + outputPath);
        consolePanel.appendLog("----------------------------------------");
        
        statusLabel.setText("正在截屏...");
        statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-info");
        statusLabel.getStyleClass().add("status-label-info");
        
        // 执行截屏
        boolean success = adbManager.takeScreenshot(outputPath);
        
        if (success) {
            consolePanel.appendLog("[成功] 截屏已保存：" + fileName);
            consolePanel.appendLog("完整路径：" + outputPath);
            statusLabel.setText("截屏成功：" + fileName);
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-info");
            statusLabel.getStyleClass().add("status-label-success");
        } else {
            consolePanel.appendLog("[失败] 截屏操作失败");
            statusLabel.setText("截屏失败");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-info");
            statusLabel.getStyleClass().add("status-label-error");
        }
    }
    
}
