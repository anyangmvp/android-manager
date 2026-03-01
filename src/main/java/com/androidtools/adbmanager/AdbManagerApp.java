package com.androidtools.adbmanager;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import com.androidtools.adbmanager.ui.MainLayout;
import com.androidtools.adbmanager.manager.DeviceManager;
import com.androidtools.adbmanager.manager.AdbManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * ADB 管理工具主应用程序
 * 提供漂亮的浅色系主题界面，支持设备管理、Gradle 构建、APK 安装、截屏等功能
 */
public class AdbManagerApp extends Application {
    
    // 使用统一的配置目录和文件
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.adb-manager";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.properties";
    private static final String KEY_WINDOW_MAXIMIZED = "window.maximized";
    
    private static AdbManagerApp instance;
    private AdbManager adbManager;
    private DeviceManager deviceManager;
    private Stage primaryStage;
    
    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;
        
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
        
        // 恢复窗口最大化状态
        boolean wasMaximized = loadMaximizedState();
        if (wasMaximized) {
            primaryStage.setMaximized(true);
        }
        
        // 监听窗口最大化状态变化并保存
        primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            saveMaximizedState(newVal);
        });
        
        primaryStage.show();
        
        // 初始扫描设备
        deviceManager.scanDevices();
    }
    
    /**
     * 加载窗口最大化状态
     */
    private boolean loadMaximizedState() {
        Properties props = loadProperties();
        String value = props.getProperty(KEY_WINDOW_MAXIMIZED, "false");
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 保存窗口最大化状态
     */
    private void saveMaximizedState(boolean maximized) {
        Properties props = loadProperties();
        props.setProperty(KEY_WINDOW_MAXIMIZED, String.valueOf(maximized));
        saveProperties(props);
    }
    
    /**
     * 加载配置文件
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("[配置] 加载配置文件失败: " + e.getMessage());
            }
        }
        return props;
    }
    
    /**
     * 保存配置文件
     */
    private void saveProperties(Properties props) {
        try {
            // 确保配置目录存在
            Files.createDirectories(Paths.get(CONFIG_DIR));
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                props.store(fos, "ADB Manager Configuration");
            }
        } catch (IOException e) {
            System.err.println("[配置] 保存配置文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        // 保存最终状态
        if (primaryStage != null) {
            saveMaximizedState(primaryStage.isMaximized());
        }
        
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
