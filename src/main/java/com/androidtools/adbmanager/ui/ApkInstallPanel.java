package com.androidtools.adbmanager.ui;

import com.androidtools.adbmanager.manager.AdbManager;
import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.GradleManager;
import com.androidtools.adbmanager.manager.GradleManager.ApkInfo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * APK 安装面板
 */
public class ApkInstallPanel extends VBox {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.adb-manager";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.properties";
    private static final String KEY_APK_OUTPUT_DIR = "apk.output.dir";
    private static final String KEY_APK_FILE_PATH = "apk.file.path";
    
    private final DeviceManager deviceManager;
    private final AdbManager adbManager;
    private final GradleManager gradleManager;
    private final ConsolePanel consolePanel;
    
    private ComboBox<ApkInfo> apkComboBox;
    private Button refreshButton;
    private Button installButton;
    private Button browseButton;
    private TextField apkPathField;
    private Label statusLabel;
    
    public ApkInstallPanel(DeviceManager deviceManager, AdbManager adbManager, GradleManager gradleManager, ConsolePanel consolePanel) {
        this.deviceManager = deviceManager;
        this.adbManager = adbManager;
        this.gradleManager = gradleManager;
        this.consolePanel = consolePanel;
        loadSavedPaths();
        buildUI();
    }
    
    private void buildUI() {
        getStyleClass().add("apk-install-panel");
        setSpacing(8);
        
        // 从 Gradle 项目选择 APK
        VBox projectApkBox = createProjectApkSection();
        
        // 或者直接选择 APK 文件
        VBox directApkBox = createDirectApkSection();
        
        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.getStyleClass().add("status-label");
        
        getChildren().addAll(projectApkBox, new Separator(), directApkBox, statusLabel);
    }
    
    private VBox createProjectApkSection() {
        VBox container = new VBox(15);
        
        Label sectionLabel = new Label("从项目选择 APK：");
        sectionLabel.getStyleClass().add("section-label");
        
        HBox apkSelectBox = new HBox(10);
        apkSelectBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        apkComboBox = new ComboBox<>();
        apkComboBox.setPrefWidth(500);
        apkComboBox.getStyleClass().add("combo-box");
        apkComboBox.setCellFactory(listView -> {
            ListCell<ApkInfo> cell = new ListCell<>() {
                @Override
                protected void updateItem(ApkInfo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        getStyleClass().addAll("apk-list-cell");
                    } else {
                        setText(item.getFileName());
                        getStyleClass().addAll("apk-list-cell");
                    }
                }
            };
            return cell;
        });
        apkComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ApkInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().addAll("apk-list-cell");
                } else {
                    setText(item.getFileName());
                    getStyleClass().addAll("apk-list-cell");
                }
            }
        });
        
        refreshButton = new Button("刷新 APK 列表");
        refreshButton.getStyleClass().add("refresh-button");
        refreshButton.setOnAction(e -> refreshApkList());
        
        apkSelectBox.getChildren().addAll(apkComboBox, refreshButton);
        
        // 安装按钮
        installButton = new Button("安装选中 APK");
        installButton.setPrefHeight(45);
        installButton.getStyleClass().addAll("install-button", "install-button-green");
        installButton.setOnAction(e -> installSelectedApk());
        
        container.getChildren().addAll(sectionLabel, apkSelectBox, installButton);
        return container;
    }
    
    private VBox createDirectApkSection() {
        VBox container = new VBox(15);
        
        Label sectionLabel = new Label("直接选择 APK 文件：");
        sectionLabel.getStyleClass().add("section-label");
        
        HBox fileSelectBox = new HBox(10);
        fileSelectBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        apkPathField = new TextField();
        apkPathField.setPromptText("选择 APK 文件路径");
        apkPathField.setPrefWidth(500);
        apkPathField.setEditable(false);
        apkPathField.getStyleClass().add("text-field");
        
        browseButton = new Button("浏览...");
        browseButton.getStyleClass().add("browse-button-orange");
        browseButton.setOnAction(e -> browseApkFile());
        
        fileSelectBox.getChildren().addAll(apkPathField, browseButton);
        
        // 安装按钮
        Button installDirectButton = new Button("安装此 APK");
        installDirectButton.setPrefHeight(45);
        installDirectButton.getStyleClass().addAll("install-button", "install-button-red");
        installDirectButton.setOnAction(e -> installDirectApk());
        
        container.getChildren().addAll(sectionLabel, fileSelectBox, installDirectButton);
        return container;
    }
    
    private void refreshApkList() {
        // 检查是否设置了项目目录
        if (gradleManager == null || gradleManager.getProjectDir() == null) {
            consolePanel.appendLog("[错误] 请先在 Gradle 构建面板中选择项目目录");
            statusLabel.setText("请先在 Gradle 构建面板中选择项目目录");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-warning");
            statusLabel.getStyleClass().add("status-label-warning");
            return;
        }
        
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("扫描 APK 文件");
        consolePanel.appendLog("项目目录：" + gradleManager.getProjectDir());
        consolePanel.appendLog("----------------------------------------");
        
        List<ApkInfo> apks = gradleManager.findApks();
        
        if (apks.isEmpty()) {
            consolePanel.appendLog("[未找到] 未发现 APK 文件，请先构建项目");
            statusLabel.setText("未找到 APK 文件，请先构建项目");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-warning");
            statusLabel.getStyleClass().add("status-label-warning");
            return;
        }
        
        consolePanel.appendLog("[成功] 找到 " + apks.size() + " 个 APK 文件");
        for (ApkInfo apk : apks) {
            consolePanel.appendLog("  - " + apk.getFileName() + " (" + apk.getType() + ")");
        }
        
        apkComboBox.getItems().clear();
        apkComboBox.getItems().addAll(apks);
        apkComboBox.getSelectionModel().selectFirst();
        
        statusLabel.setText("找到 " + apks.size() + " 个 APK 文件");
        statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-warning");
        statusLabel.getStyleClass().add("status-label-success");
    }
    
    /**
     * 加载保存的路径
     */
    private void loadSavedPaths() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        
        try {
            // 如果配置文件存在，加载它
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
            }
            
            // 加载 APK 输出目录
            String savedDir = props.getProperty(KEY_APK_OUTPUT_DIR);
            if (savedDir != null && !savedDir.isEmpty() && Files.exists(Paths.get(savedDir))) {
                // 已恢复，但不输出日志
            }
            
            // 加载 APK 文件路径
            String savedPath = props.getProperty(KEY_APK_FILE_PATH);
            if (savedPath != null && !savedPath.isEmpty() && Files.exists(Paths.get(savedPath))) {
                apkPathField = new TextField(savedPath);
                return;
            }
        } catch (IOException e) {
            // 忽略错误
        }
        apkPathField = new TextField();
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
     * 保存 APK 输出目录
     */
    private void saveApkOutputDir(String dir) {
        Properties props = loadProperties();
        props.setProperty(KEY_APK_OUTPUT_DIR, dir);
        saveProperties(props);
    }
    
    /**
     * 保存 APK 文件路径
     */
    private void saveApkFilePath(String path) {
        Properties props = loadProperties();
        props.setProperty(KEY_APK_FILE_PATH, path);
        saveProperties(props);
    }
    
    private void browseApkFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择 APK 文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("APK 文件", "*.apk"));
        
        // 使用上次选择的目录
        String currentPath = apkPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.exists()) {
                chooser.setInitialDirectory(currentFile.getParentFile());
            }
        }
        
        File selected = chooser.showOpenDialog(null);
        if (selected != null) {
            apkPathField.setText(selected.getAbsolutePath());
            saveApkFilePath(selected.getAbsolutePath()); // 保存路径
        }
    }
    
    private void installSelectedApk() {
        ApkInfo selected = apkComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            consolePanel.appendLog("[错误] 请先选择 APK 文件");
            return;
        }
        
        if (deviceManager.getSelectedDevice() == null) {
            consolePanel.appendLog("[错误] 请先选择设备");
            return;
        }
        
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("开始安装 APK");
        consolePanel.appendLog("文件：" + selected.getFileName());
        consolePanel.appendLog("设备：" + deviceManager.getSelectedDevice().getDeviceName());
        consolePanel.appendLog("----------------------------------------");
        
        installApk(selected.getPath());
    }
    
    private void installDirectApk() {
        String path = apkPathField.getText();
        if (path == null || path.isEmpty()) {
            consolePanel.appendLog("[错误] 请先选择 APK 文件");
            return;
        }
        
        if (deviceManager.getSelectedDevice() == null) {
            consolePanel.appendLog("[错误] 请先选择设备");
            return;
        }
        
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("开始安装 APK");
        consolePanel.appendLog("文件：" + Path.of(path).getFileName());
        consolePanel.appendLog("设备：" + deviceManager.getSelectedDevice().getDeviceName());
        consolePanel.appendLog("----------------------------------------");
        
        installApk(Path.of(path));
    }
    
    private void installApk(Path apkPath) {
        consolePanel.appendLog("APK 路径：" + apkPath);
        
        statusLabel.setText("正在安装：" + apkPath.getFileName());
        
        boolean success = adbManager.installApk(apkPath.toString());
        
        if (success) {
            consolePanel.appendLog("[成功] APK 安装完成");
            statusLabel.setText("安装成功！");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-warning");
            statusLabel.getStyleClass().add("status-label-success");
        } else {
            consolePanel.appendLog("[失败] APK 安装失败, ");
            consolePanel.appendLog("可能原因：证书签名问题、设备连接断开等");
            // 检查是否是证书问题
            statusLabel.setText("安装失败，可能是证书问题");
            statusLabel.getStyleClass().removeAll("status-label", "status-label-success", "status-label-error", "status-label-warning");
            statusLabel.getStyleClass().add("status-label-error");
        }
    }
}
