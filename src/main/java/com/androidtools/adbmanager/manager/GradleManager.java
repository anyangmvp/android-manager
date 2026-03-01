package com.androidtools.adbmanager.manager;

import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Gradle 构建管理器
 * 负责执行 Gradle 构建命令
 */
public class GradleManager {
    
    private Path projectDir;
    
    /**
     * 设置项目目录
     */
    public void setProjectDir(String path) {
        Path dir = Paths.get(path);
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            // 验证是否是 Android 项目
            if (Files.exists(dir.resolve("build.gradle")) || 
                Files.exists(dir.resolve("build.gradle.kts")) ||
                Files.exists(dir.resolve("settings.gradle")) ||
                Files.exists(dir.resolve("settings.gradle.kts"))) {
                this.projectDir = dir;
            } else {
                throw new IllegalArgumentException("不是有效的 Android 项目目录");
            }
        } else {
            throw new IllegalArgumentException("目录不存在");
        }
    }
    
    /**
     * 获取项目目录
     */
    public Path getProjectDir() {
        return projectDir;
    }
    
    /**
     * 执行 Gradle 命令
     */
    public Task<String> executeGradleTask(String task, Consumer<String> outputConsumer) {
        Task<String> gradleTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                if (projectDir == null) {
                    throw new IllegalStateException("未设置项目目录");
                }
                
                List<String> command = new ArrayList<>();
                command.add("gradle");
                command.add(task);
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(projectDir.toFile());
                pb.redirectErrorStream(true);
                
                Process process = pb.start();
                
                StringBuilder output = new StringBuilder();
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        
                        if (outputConsumer != null) {
                            outputConsumer.accept(line);
                        }
                        
                        updateMessage(line);
                    }
                }
                
                process.waitFor();
                
                if (process.exitValue() == 0) {
                    return "构建成功";
                } else {
                    throw new RuntimeException("构建失败，退出码：" + process.exitValue());
                }
            }
        };
        
        return gradleTask;
    }
    
    /**
     * 查找 APK 文件
     */
    public List<ApkInfo> findApks() {
        List<ApkInfo> apks = new ArrayList<>();
        
        if (projectDir == null) {
            return apks;
        }
        
        Path apkOutputDir = projectDir.resolve("app").resolve("build").resolve("outputs").resolve("apk");
        
        if (!Files.exists(apkOutputDir)) {
            return apks;
        }
        
        try (Stream<Path> walk = Files.walk(apkOutputDir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".apk"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    ApkType type = classifyApk(fileName);
                    if (type != null) {
                        apks.add(new ApkInfo(path, fileName, type));
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 也扫描项目根目录的 APK
        try (Stream<Path> walk = Files.walk(projectDir, 3)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".apk"))
                .filter(p -> !p.toString().contains("build")) // 避免重复
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    ApkType type = classifyApk(fileName);
                    if (type != null) {
                        apks.add(new ApkInfo(path, fileName, type));
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return apks;
    }
    
    /**
     * 根据文件名分类 APK 类型
     */
    private ApkType classifyApk(String fileName) {
        String lower = fileName.toLowerCase();
        
        // 移除版本号等后缀
        if (lower.contains("release") || lower.contains("-r")) {
            return ApkType.RELEASE;
        } else if (lower.contains("debug") || lower.contains("-d")) {
            return ApkType.DEBUG;
        } else if (lower.contains("beta")) {
            return ApkType.BETA;
        } else {
            return ApkType.OTHER;
        }
    }
    
    /**
     * APK 类型枚举
     */
    public enum ApkType {
        DEBUG("调试版"),
        RELEASE("发布版"),
        BETA("测试版"),
        OTHER("其他");
        
        private final String displayName;
        
        ApkType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * APK 信息类
     */
    public static class ApkInfo {
        private final Path path;
        private final String fileName;
        private final ApkType type;
        
        public ApkInfo(Path path, String fileName, ApkType type) {
            this.path = path;
            this.fileName = fileName;
            this.type = type;
        }
        
        public Path getPath() {
            return path;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public ApkType getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return fileName + " (" + type.getDisplayName() + ")";
        }
    }
}
