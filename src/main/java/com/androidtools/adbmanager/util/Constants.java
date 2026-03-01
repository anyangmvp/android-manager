package com.androidtools.adbmanager.util;

/**
 * 应用常量
 */
public class Constants {
    
    // 颜色常量
    public static final String COLOR_PRIMARY = "#667eea";
    public static final String COLOR_PRIMARY_DARK = "#764ba2";
    public static final String COLOR_SUCCESS = "#48bb78";
    public static final String COLOR_DANGER = "#f56565";
    public static final String COLOR_WARNING = "#ed8936";
    public static final String COLOR_INFO = "#4299e1";
    
    // 样式类
    public static final String STYLE_CARD = "-fx-background-color: white; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);";
    public static final String STYLE_BUTTON_PRIMARY = "-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px;";
    public static final String STYLE_BUTTON_SUCCESS = "-fx-background-color: #48bb78; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px;";
    public static final String STYLE_BUTTON_DANGER = "-fx-background-color: #f56565; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px;";
    
    // ADB 命令
    public static final String ADB_DEVICES = "devices";
    public static final String ADB_INSTALL = "install";
    public static final String ADB_UNINSTALL = "uninstall";
    public static final String ADB_SCREENCAP = "shell screencap -p";
    public static final String ADB_PULL = "pull";
    
    // Gradle 任务
    public static final String GRADLE_ASSEMBLE_DEBUG = "assembleDebug";
    public static final String GRADLE_ASSEMBLE_RELEASE = "assembleRelease";
    public static final String GRADLE_INSTALL_DEBUG = "installDebug";
    public static final String GRADLE_INSTALL_RELEASE = "installRelease";
}
