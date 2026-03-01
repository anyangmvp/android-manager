package com.androidtools.adbmanager.ui;

import com.androidtools.adbmanager.manager.AdbManager;
import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.DeviceManager.DeviceInfo;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * 主界面布局
 */
public class MainLayout extends BorderPane {
    
    private final DeviceManager deviceManager;
    private final AdbManager adbManager;
    
    private DevicePanel devicePanel;
    private GradlePanel gradlePanel;
    private ApkInstallPanel apkInstallPanel;
    private ScreenshotPanel screenshotPanel;
    private ScreenMirrorPanel screenMirrorPanel;
    private FileTransferPanel fileTransferPanel;
    private ConsolePanel consolePanel;
    
    public MainLayout(DeviceManager deviceManager, AdbManager adbManager) {
        this.deviceManager = deviceManager;
        this.adbManager = adbManager;
        
        getStyleClass().add("main-layout");
        
        // 创建顶部工具栏
        VBox top = createTopBar();
        setTop(top);
        
        // 创建主内容区域
        HBox center = createCenterContent();
        setCenter(center);
        
        // 创建底部状态栏
        HBox bottom = createStatusBar();
        setBottom(bottom);
    }
    
    /**
     * 创建顶部工具栏
     */
    private VBox createTopBar() {
        VBox topBar = new VBox(8);
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.getStyleClass().add("top-bar");
        
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label title = new Label("ADB Manager");
        title.getStyleClass().add("title");
        
        Label subtitle = new Label("专业的 Android 设备管理工具");
        subtitle.getStyleClass().add("subtitle");
        
        titleRow.getChildren().addAll(title, subtitle);
        topBar.getChildren().add(titleRow);
        return topBar;
    }
    
    /**
     * 创建主内容区域
     */
    private HBox createCenterContent() {
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        
        // 左侧面板（设备管理）
        devicePanel = new DevicePanel(deviceManager);
        HBox.setHgrow(devicePanel, Priority.NEVER);
        
        // 中间区域（功能面板）
        VBox functionArea = new VBox(15);
        HBox.setHgrow(functionArea, Priority.ALWAYS);
        
        // 先创建控制台面板（这样其他面板可以使用它）
        consolePanel = new ConsolePanel();
        consolePanel.setMinHeight(120);
        consolePanel.setPrefHeight(150);
        VBox.setVgrow(consolePanel, Priority.ALWAYS);
        
        // 功能选项卡
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Gradle 构建标签页 - 传递 ConsolePanel
        gradlePanel = new GradlePanel(deviceManager, adbManager, consolePanel);
        Tab gradleTab = new Tab("Gradle 构建", gradlePanel);
        gradleTab.setClosable(false);
        
        // APK 安装标签页 - 传递 GradleManager 和 ConsolePanel 实例
        apkInstallPanel = new ApkInstallPanel(deviceManager, adbManager, gradlePanel.getGradleManager(), consolePanel);
        Tab apkTab = new Tab("APK 安装", apkInstallPanel);
        apkTab.setClosable(false);
        
        // 截屏标签页 - 传递 ConsolePanel
        screenshotPanel = new ScreenshotPanel(deviceManager, adbManager, consolePanel);
        Tab screenshotTab = new Tab("截屏功能", screenshotPanel);
        screenshotTab.setClosable(false);
        
        // 屏幕投射标签页（暂时禁用）
        // screenMirrorPanel = new ScreenMirrorPanel(deviceManager, adbManager, consolePanel);
        // Tab mirrorTab = new Tab("屏幕投射", screenMirrorPanel);
        // mirrorTab.setClosable(false);
        
        // 文件管理标签页
        fileTransferPanel = new FileTransferPanel(deviceManager, adbManager, consolePanel);
        Tab fileTab = new Tab("文件管理", fileTransferPanel);
        fileTab.setClosable(false);
        
        tabPane.getTabs().addAll(gradleTab, apkTab, screenshotTab, fileTab);
        
        functionArea.getChildren().addAll(tabPane, consolePanel);
        
        content.getChildren().addAll(devicePanel, functionArea);
        return content;
    }
    
    /**
     * 创建底部状态栏
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(12, 20, 12, 20));
        statusBar.getStyleClass().add("status-bar");
        
        // 左侧区域：设备状态 + 刷新设备按钮（占 2/3）
        HBox leftArea = new HBox(15);
        leftArea.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // 设备状态
        Label deviceStatus = new Label("设备状态：");
        deviceStatus.getStyleClass().add("label");
        
        Label deviceCount = new Label();
        deviceCount.textProperty().bind(deviceManager.statusMessageProperty());
        deviceCount.getStyleClass().add("device-count");
        
        // 刷新设备按钮 - 缩小版本
        Button refreshDeviceButton = new Button("🔄 刷新");
        refreshDeviceButton.getStyleClass().addAll("status-bar-button", "button-hover-scale");
        
        refreshDeviceButton.setOnAction(e -> deviceManager.scanDevices());
        
        leftArea.getChildren().addAll(deviceStatus, deviceCount, refreshDeviceButton);
        
        // 分隔线
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setPrefHeight(24);
        separator.getStyleClass().add("status-bar-separator");
        
        // 右侧区域：重启 ADB 按钮 + 连接指示器（占 1/3）
        HBox rightArea = new HBox(12);
        rightArea.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        HBox.setHgrow(rightArea, Priority.ALWAYS);
        
        // 重启 ADB 按钮
        Button killAdbButton = new Button("🔄 重启 ADB");
        killAdbButton.getStyleClass().addAll("status-bar-button-danger", "button-hover-scale");
        killAdbButton.setOnAction(e -> killAdbProcess());
        
        // 连接指示器
        CircleIndicator connectionIndicator = new CircleIndicator();
        connectionIndicator.setActive(true);
        
        rightArea.getChildren().addAll(killAdbButton, connectionIndicator);
        
        statusBar.getChildren().addAll(leftArea, separator, rightArea);
        return statusBar;
    }
    
    /**
     * 杀死 ADB 进程并重启
     */
    private void killAdbProcess() {
        consolePanel.appendLog("========================================");
        consolePanel.appendLog("正在重启 ADB 进程...");
        consolePanel.appendLog("----------------------------------------");
        
        try {
            // 杀死所有 adb 进程
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", "adb.exe");
            Process process = pb.start();
            process.waitFor();
            
            consolePanel.appendLog("[完成] 已终止所有 ADB 进程");
            Thread.sleep(500);
            
            // 等待一下再重启
            consolePanel.appendLog("正在重启 ADB 服务...");
            pb = new ProcessBuilder("adb", "start-server");
            process = pb.start();
            process.waitFor();
            
            consolePanel.appendLog("[成功] ADB 服务已重启");
            consolePanel.appendLog("----------------------------------------");
            consolePanel.appendLog("提示：请重新插拔设备或点击刷新设备");
            
            // 重新扫描设备
            Thread.sleep(1000);
            deviceManager.scanDevices();
            
        } catch (Exception e) {
            consolePanel.appendLog("[错误] ADB 重启失败：" + e.getMessage());
        }
    }
}
