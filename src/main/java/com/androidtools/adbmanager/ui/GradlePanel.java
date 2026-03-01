package com.androidtools.adbmanager.ui;

import com.androidtools.adbmanager.manager.AdbManager;
import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.GradleManager;
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
import java.util.Properties;

/**
 * Gradle 构建面板
 */
public class GradlePanel extends VBox {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.adb-manager";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.properties";
    private static final String KEY_PROJECT_DIR = "project.dir";
    
    private final DeviceManager deviceManager;
    private final AdbManager adbManager;
    private final GradleManager gradleManager;
    private final ConsolePanel consolePanel;
    
    private TextField projectDirField;
    private Button browseButton;
    private Button buildDebugButton;
    private Button buildReleaseButton;
    private Button installDebugButton;
    private Button installReleaseButton;
    private Button cleanButton;
    private Label statusLabel;
    private boolean hasProjectDir = false;
    
    public GradlePanel(DeviceManager deviceManager, AdbManager adbManager, ConsolePanel consolePanel) {
        this.deviceManager = deviceManager;
        this.adbManager = adbManager;
        this.gradleManager = new GradleManager();
        this.consolePanel = consolePanel;
        loadSavedProjectDir();
        buildUI();
    }
    
    /**
     * 获取 GradleManager 实例
     */
    public GradleManager getGradleManager() {
        return gradleManager;
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
     * 加载保存的项目目录
     */
    private void loadSavedProjectDir() {
        Properties props = loadProperties();
        String savedDir = props.getProperty(KEY_PROJECT_DIR);
        if (savedDir != null && !savedDir.isEmpty() && Files.exists(Paths.get(savedDir))) {
            projectDirField = new TextField(savedDir);
            gradleManager.setProjectDir(savedDir);
            consolePanel.appendLog("[记忆] 已恢复上次项目目录：" + savedDir);
            hasProjectDir = true;
            return;
        }
        projectDirField = new TextField();
        hasProjectDir = false;
    }
    
    /**
     * 保存项目目录
     */
    private void saveProjectDir(String dir) {
        Properties props = loadProperties();
        props.setProperty(KEY_PROJECT_DIR, dir);
        saveProperties(props);
    }
    
    private void buildUI() {
        getStyleClass().add("gradle-panel");
        
        // 标题
        Label titleLabel = new Label("Gradle 构建");
        titleLabel.getStyleClass().add("title-label");
        
        // 项目目录选择
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label dirLabel = new Label("项目目录：");
        dirLabel.getStyleClass().add("dir-label");
        
        projectDirField.setEditable(false);
        projectDirField.setPromptText("选择 Android 项目根目录");
        projectDirField.setPrefWidth(400);
        projectDirField.getStyleClass().add("project-dir-field");
        
        browseButton = new Button("浏览...");
        browseButton.getStyleClass().add("browse-button");
        browseButton.setOnAction(e -> browseProjectDir());
        
        dirBox.getChildren().addAll(dirLabel, projectDirField, browseButton);
        
        // 构建按钮区域 - 3 列布局
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(15);
        buttonGrid.setVgap(15);
        buttonGrid.setPadding(new Insets(10, 0, 0, 0));
        
        // 第一行
        buildDebugButton = createActionButton("构建 Debug", "#ed8936");
        buildDebugButton.setDisable(true);
        buildDebugButton.setOnAction(e -> {
            System.out.println("[GradlePanel] 点击 构建 Debug");
            buildApk("assembleDebug");
        });
        
        buildReleaseButton = createActionButton("构建 Release", "#9f7aea");
        buildReleaseButton.setDisable(true);
        buildReleaseButton.setOnAction(e -> {
            System.out.println("[GradlePanel] 点击 构建 Release");
            buildApk("assembleRelease");
        });
        
        // 清理按钮 - 跨越两行，宽度是其他按钮的 0.5 倍
        cleanButton = createLargeActionButton("清理", "#718096");
        cleanButton.setDisable(true);
        cleanButton.setOnAction(e -> {
            System.out.println("[GradlePanel] 点击 清理");
            cleanProject();
        });
        
        // 第二行
        installDebugButton = createActionButton("安装 Debug", "#48bb78");
        installDebugButton.setDisable(true);
        installDebugButton.setOnAction(e -> {
            System.out.println("[GradlePanel] 点击 安装 Debug");
            buildAndInstall("assembleDebug");
        });
        
        installReleaseButton = createActionButton("安装 Release", "#f56565");
        installReleaseButton.setDisable(true);
        installReleaseButton.setOnAction(e -> {
            System.out.println("[GradlePanel] 点击 安装 Release");
            buildAndInstall("assembleRelease");
        });
        
        // 添加按钮到网格
        buttonGrid.add(buildDebugButton, 0, 0);
        buttonGrid.add(buildReleaseButton, 1, 0);
        buttonGrid.add(cleanButton, 2, 0, 1, 2); // 跨越两行
        buttonGrid.add(installDebugButton, 0, 1);
        buttonGrid.add(installReleaseButton, 1, 1);
        
        // 设置列宽
        ColumnConstraints col1 = new ColumnConstraints(150);
        ColumnConstraints col2 = new ColumnConstraints(150);
        ColumnConstraints col3 = new ColumnConstraints(230); // 更宽
        buttonGrid.getColumnConstraints().addAll(col1, col2, col3);
        
        // 状态标签
        statusLabel = new Label("请先选择项目目录");
        statusLabel.getStyleClass().add("status-label");
        
        // 监听项目目录变化
        projectDirField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasDir = newVal != null && !newVal.isEmpty();
            updateButtonStates(hasDir);
            
            if (hasDir) {
                try {
                    gradleManager.setProjectDir(newVal);
                    saveProjectDir(newVal); // 保存项目目录
                    statusLabel.setText("项目目录已设置：" + newVal);
                    statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error");
                    statusLabel.getStyleClass().add("status-label-success");
                } catch (Exception e) {
                    statusLabel.setText("无效的项目目录");
                    statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error");
                    statusLabel.getStyleClass().add("status-label-error");
                    updateButtonStates(false);
                }
            } else {
                statusLabel.setText("请先选择项目目录");
                statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error");
                statusLabel.getStyleClass().add("status-label");
            }
        });
        
        // 初始化按钮状态（如果有记忆的项目目录）
        updateButtonStates(hasProjectDir);
        
        // 更新状态标签
        if (hasProjectDir) {
            statusLabel.setText("项目目录已设置：" + projectDirField.getText());
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error");
            statusLabel.getStyleClass().add("status-label-success");
        } else {
            statusLabel.setText("请先选择项目目录");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error");
            statusLabel.getStyleClass().add("status-label");
        }
        
        getChildren().addAll(titleLabel, dirBox, buttonGrid, statusLabel);
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonStates(boolean enabled) {
        if (buildDebugButton != null) {
            buildDebugButton.setDisable(!enabled);
        }
        if (buildReleaseButton != null) {
            buildReleaseButton.setDisable(!enabled);
        }
        if (installDebugButton != null) {
            installDebugButton.setDisable(!enabled);
        }
        if (installReleaseButton != null) {
            installReleaseButton.setDisable(!enabled);
        }
        if (cleanButton != null) {
            cleanButton.setDisable(!enabled);
        }
    }
    
    private void browseProjectDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择 Android 项目目录");
        
        // 使用上次选择的目录或用户目录
        String initialDir = projectDirField.getText();
        if (initialDir != null && !initialDir.isEmpty() && new File(initialDir).exists()) {
            chooser.setInitialDirectory(new File(initialDir));
        } else {
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        
        File selected = chooser.showDialog(null);
        if (selected != null) {
            projectDirField.setText(selected.getAbsolutePath());
        }
    }
    
    private void buildApk(String task) {
        System.out.println("[GradlePanel.buildApk] 开始执行：" + task);
        System.out.println("[GradlePanel.buildApk] 项目目录：" + gradleManager.getProjectDir());
        
        if (gradleManager.getProjectDir() == null) {
            System.out.println("[GradlePanel.buildApk] 错误：项目目录为 null");
            consolePanel.appendLog("[错误] 请先选择项目目录");
            statusLabel.setText("请先选择项目目录");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
            return;
        }
        
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("开始 Gradle 构建");
        consolePanel.appendLog("任务：" + task);
        consolePanel.appendLog("项目：" + gradleManager.getProjectDir());
        consolePanel.appendLog("----------------------------------------");
        
        statusLabel.setText("正在构建：" + task);
        
        System.out.println("[GradlePanel.buildApk] 调用 executeGradleTask");
        var taskExecutor = gradleManager.executeGradleTask(task, line -> {
            consolePanel.appendLog(line);
        });
        
        System.out.println("[GradlePanel.buildApk] 设置监听器");
        taskExecutor.setOnSucceeded(e -> {
            System.out.println("[GradlePanel.buildApk] 构建成功回调");
            consolePanel.appendLog("----------------------------------------");
            consolePanel.appendLog("[成功] 构建完成：" + task);
            statusLabel.setText("构建成功：" + task);
            statusLabel.setStyle("-fx-text-fill: #48bb78; -fx-font-weight: bold;");
        });
        
        taskExecutor.setOnFailed(e -> {
            System.out.println("[GradlePanel.buildApk] 构建失败回调");
            System.out.println("[GradlePanel.buildApk] 异常：" + e.getSource().getException());
            consolePanel.appendLog("[失败] 构建失败：" + e.getSource().getException().getMessage());
            statusLabel.setText("构建失败");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
        });
        
        System.out.println("[GradlePanel.buildApk] 提交任务执行");
        new Thread(taskExecutor).start();
    }
    
    private void buildAndInstall(String task) {
        System.out.println("[GradlePanel.buildAndInstall] 开始执行：" + task);
        System.out.println("[GradlePanel.buildAndInstall] 选中设备：" + deviceManager.getSelectedDevice());
        System.out.println("[GradlePanel.buildAndInstall] 项目目录：" + gradleManager.getProjectDir());
        
        if (deviceManager.getSelectedDevice() == null) {
            System.out.println("[GradlePanel.buildAndInstall] 错误：未选择设备");
            consolePanel.appendLog("[错误] 请先选择设备");
            statusLabel.setText("请先选择设备");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
            return;
        }
        
        if (gradleManager.getProjectDir() == null) {
            System.out.println("[GradlePanel.buildAndInstall] 错误：项目目录为 null");
            consolePanel.appendLog("[错误] 请先选择项目目录");
            statusLabel.setText("请先选择项目目录");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
            return;
        }
        
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("开始构建并安装");
        consolePanel.appendLog("任务：" + task);
        consolePanel.appendLog("设备：" + deviceManager.getSelectedDevice().getDeviceName());
        consolePanel.appendLog("项目：" + gradleManager.getProjectDir());
        consolePanel.appendLog("----------------------------------------");
        
        statusLabel.setText("正在构建并安装：" + task);
        
        System.out.println("[GradlePanel.buildAndInstall] 调用 executeGradleTask");
        // 先构建
        var buildTask = gradleManager.executeGradleTask(task, line -> {
            consolePanel.appendLog(line);
        });
        
        buildTask.setOnSucceeded(e -> {
            consolePanel.appendLog("----------------------------------------");
            consolePanel.appendLog("[构建成功] 开始安装 APK");
            
            // 构建成功后安装 APK
            boolean installed = installBuiltApk(task);
            
            if (installed) {
                consolePanel.appendLog("[成功] 构建并安装完成：" + task);
                statusLabel.setText("安装成功：" + task);
                statusLabel.setStyle("-fx-text-fill: #48bb78; -fx-font-weight: bold;");
            } else {
                consolePanel.appendLog("[失败] 安装失败");
                consolePanel.appendLog("可能原因：证书签名问题、设备连接断开等");
                statusLabel.setText("安装失败，可能是 Debug 和 Release 证书不同样");
                statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
            }
        });
        
        buildTask.setOnFailed(e -> {
            consolePanel.appendLog("[失败] 构建失败：" + e.getSource().getException().getMessage());
            statusLabel.setText("构建失败");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
        });
        
        new Thread(buildTask).start();
    }
    
    /**
     * 安装已构建的 APK
     */
    private boolean installBuiltApk(String task) {
        String variant = task.replace("assemble", "");
        String apkSubDir = variant.toLowerCase();
        
        // 查找 APK 文件
        String apkDir = gradleManager.getProjectDir() + "/app/build/outputs/apk/" + apkSubDir;
        File dir = new File(apkDir);
        if (!dir.exists()) {
            consolePanel.appendLog("[错误] APK 目录不存在：" + apkDir);
            return false;
        }
        
        File[] apkFiles = dir.listFiles((d, name) -> name.endsWith(".apk"));
        if (apkFiles == null || apkFiles.length == 0) {
            consolePanel.appendLog("[错误] 未找到 APK 文件");
            return false;
        }
        
        // 安装第一个 APK
        File apkFile = apkFiles[0];
        consolePanel.appendLog("安装 APK: " + apkFile.getName());
        return adbManager.installApk(apkFile.getAbsolutePath());
    }
    
    /**
     * 清理项目
     */
    private void cleanProject() {
        if (gradleManager.getProjectDir() == null) {
            consolePanel.appendLog("[错误] 请先选择项目目录");
            statusLabel.setText("请先选择项目目录");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
            return;
        }
        
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("开始清理项目");
        consolePanel.appendLog("项目：" + gradleManager.getProjectDir());
        consolePanel.appendLog("----------------------------------------");
        
        statusLabel.setText("正在清理...");
        
        var taskExecutor = gradleManager.executeGradleTask("clean", line -> {
            consolePanel.appendLog(line);
        });
        
        taskExecutor.setOnSucceeded(e -> {
            consolePanel.appendLog("----------------------------------------");
            consolePanel.appendLog("[成功] 清理完成");
            statusLabel.setText("清理完成");
            statusLabel.setStyle("-fx-text-fill: #48bb78; -fx-font-weight: bold;");
        });
        
        taskExecutor.setOnFailed(e -> {
            System.out.println("[GradlePanel.cleanProject] 清理失败回调");
            System.out.println("[GradlePanel.cleanProject] 异常：" + e.getSource().getException());
            consolePanel.appendLog("[失败] 清理失败：" + e.getSource().getException().getMessage());
            statusLabel.setText("清理失败");
            statusLabel.setStyle("-fx-text-fill: #f56565; -fx-font-weight: bold;");
        });
        
        System.out.println("[GradlePanel.cleanProject] 提交任务执行");
        new Thread(taskExecutor).start();
    }
    
    private Button createActionButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefSize(150, 50);
        button.getStyleClass().addAll("action-button", "button-hover-scale");
        
        // 根据颜色添加不同的样式类
        switch (color) {
            case "#ed8936":
                button.getStyleClass().add("gradle-button-orange");
                break;
            case "#9f7aea":
                button.getStyleClass().add("gradle-button-purple");
                break;
            case "#48bb78":
                button.getStyleClass().add("gradle-button-green");
                break;
            case "#f56565":
                button.getStyleClass().add("gradle-button-red");
                break;
            default:
                button.getStyleClass().add("gradle-button-gray");
                break;
        }
        
        return button;
    }
    
    private Button createLargeActionButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefWidth(75);
        button.setPrefHeight(115); // 两行高度
        button.getStyleClass().addAll("large-action-button", "button-hover-scale");
        
        // 根据颜色添加不同的样式类
        if ("#718096".equals(color)) {
            button.getStyleClass().add("gradle-button-gray");
        }
        
        return button;
    }
    
}
