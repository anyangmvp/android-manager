package com.androidtools.adbmanager;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import com.androidtools.adbmanager.ui.MainLayout;
import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.AdbManager;

/**
 * ADB 管理工具主应用程序
 * 提供漂亮的浅色系主题界面，支持设备管理、Gradle 构建、APK 安装、截屏等功能
 */
public class AdbManagerApp extends Application {
    
    private static AdbManagerApp instance;
    private AdbManager adbManager;
    private DeviceManager deviceManager;
    
    @Override
    public void start(Stage primaryStage) {
        instance = this;
        
        // 初始化管理器
        adbManager = new AdbManager();
        deviceManager = new DeviceManager(adbManager);
        
        // 创建主界面
        BorderPane root = new MainLayout(deviceManager, adbManager);
        
        // 创建场景
        Scene scene = new Scene(root, 1400, 900);
        
        // 加载样式
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
        
        // 设置舞台
        primaryStage.setTitle("ADB Manager - 安卓设备管理工具");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // 初始扫描设备
        deviceManager.scanDevices();
    }
    
    @Override
    public void stop() {
        // 清理资源
        if (adbManager != null) {
            adbManager.close();
        }
    }
    
    public static AdbManagerApp getInstance() {
        return instance;
    }
    
    public AdbManager getAdbManager() {
        return adbManager;
    }
    
    public DeviceManager getDeviceManager() {
        return deviceManager;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
