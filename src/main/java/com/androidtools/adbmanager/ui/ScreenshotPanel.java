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
        loadSavedScreenshotDir();
        buildUI();
    }
    
    private void buildUI() {
        setSpacing(20);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        // 标题
        Label titleLabel = new Label("截屏功能");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");
        
        // 输出目录设置
        VBox dirBox = createDirSection();
        
        // 文件名设置
        VBox nameBox = createFileNameSection();
        
        // 截屏按钮
        screenshotButton = new Button("开始截屏");
        screenshotButton.setPrefHeight(50);
        screenshotButton.setStyle("-fx-background-color: linear-gradient(to right, #667eea 0%, #764ba2 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 10px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");
        screenshotButton.setOnAction(e -> takeScreenshot());
        
        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #718096;");
        
        getChildren().addAll(titleLabel, dirBox, nameBox, screenshotButton, statusLabel);
    }
    
    private VBox createDirSection() {
        VBox container = new VBox(10);
        
        Label sectionLabel = new Label("输出目录：");
        sectionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        outputDirField = new TextField();
        outputDirField.setPromptText("选择截屏保存目录");
        outputDirField.setPrefWidth(500);
        outputDirField.setEditable(false);
        outputDirField.setStyle("-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px;");
        
        browseDirButton = new Button("浏览...");
        browseDirButton.setStyle("-fx-background-color: #4299e1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand;");
        browseDirButton.setOnAction(e -> browseOutputDir());
        
        dirBox.getChildren().addAll(outputDirField, browseDirButton);
        container.getChildren().addAll(sectionLabel, dirBox);
        
        return container;
    }
    
    private VBox createFileNameSection() {
        VBox container = new VBox(10);
        
        Label sectionLabel = new Label("文件名设置：");
        sectionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        
        GridPane nameGrid = new GridPane();
        nameGrid.setHgap(15);
        nameGrid.setVgap(10);
        
        Label prefixLabel = new Label("文件名前缀：");
        prefixLabel.setStyle("-fx-text-fill: #718096;");
        
        prefixField = new TextField("Screenshot_");
        prefixField.setPromptText("例如：Screenshot_");
        prefixField.setPrefWidth(200);
        prefixField.setStyle("-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px;");
        
        Label suffixLabel = new Label("文件名后缀：");
        suffixLabel.setStyle("-fx-text-fill: #718096;");
        
        suffixField = new TextField("Home");
        suffixField.setPromptText("例如：Home");
        suffixField.setPrefWidth(200);
        suffixField.setStyle("-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px;");
        
        Label hintLabel = new Label("最终文件名格式：前缀 + 后缀 + 时间戳.png");
        hintLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 12px; -fx-font-style: italic;");
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
            outputDirField = new TextField(savedDir);
            return;
        }
        outputDirField = new TextField();
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
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
            return;
        }
        
        // 检查输出目录
        String outputDir = outputDirField.getText();
        if (outputDir == null || outputDir.isEmpty()) {
            consolePanel.appendLog("[错误] 请先选择输出目录");
            statusLabel.setText("请先选择输出目录");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
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
        statusLabel.setStyle("-fx-text-fill: #667eea; -fx-font-weight: bold;");
        
        // 执行截屏
        boolean success = adbManager.takeScreenshot(outputPath);
        
        if (success) {
            consolePanel.appendLog("[成功] 截屏已保存：" + fileName);
            consolePanel.appendLog("完整路径：" + outputPath);
            statusLabel.setText("截屏成功：" + fileName);
            statusLabel.setStyle("-fx-text-fill: #48bb78; -fx-font-weight: bold;");
        } else {
            consolePanel.appendLog("[失败] 截屏操作失败");
            statusLabel.setText("截屏失败");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
        }
    }
    
}
