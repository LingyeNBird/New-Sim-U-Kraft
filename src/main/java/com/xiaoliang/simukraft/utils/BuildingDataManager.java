package com.xiaoliang.simukraft.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BuildingDataManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static class BuildingInfo {
        private String name;
        private String size;
        private String amount;
        private String author;
        private String description;
        private String category;
        private String fileName;
        private String litematicFileName;
        private String jobType;
        private List<String> tags; // 建筑标签列表
        
        public BuildingInfo(String name, String size, String amount, String author, String description, 
                          String category, String fileName, String litematicFileName, String jobType, List<String> tags) {
            this.name = name;
            this.size = size;
            this.amount = amount;
            this.author = author;
            this.description = description;
            this.category = category;
            this.fileName = fileName;
            this.litematicFileName = litematicFileName;
            this.jobType = jobType;
            this.tags = tags != null ? tags : new ArrayList<>();
        }
        
        // Getters
        public String getName() { return name; }
        public String getSize() { return size; }
        public String getAmount() { return amount; }
        public String getAuthor() { return author; }
        public List<String> getTags() { return tags; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public String getFileName() { return fileName; }
        public String getLitematicFileName() { return litematicFileName; }
        public String getJobType() { return jobType; }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private static final String BUILDING_ROOT_PATH = "assets/simukraft/building";
    private static String SIMUKRAFT_BUILDING_FOLDER;
    private static final String[] CATEGORIES = {"residential", "commercial", "industry", "other"};

    private static Path resolveProjectRoot(Path workingDir) {
        Path current = workingDir.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("src/main/resources").resolve(BUILDING_ROOT_PATH))
                    || Files.exists(current.resolve("build.gradle"))
                    || Files.exists(current.resolve("gradlew.bat"))) {
                return current;
            }
            current = current.getParent();
        }
        return workingDir.toAbsolutePath().normalize();
    }

    private static String getBaseName(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex >= 0 ? fileName.substring(0, extensionIndex).toLowerCase() : fileName.toLowerCase();
    }

    private static boolean matchesAny(String baseName, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(baseName)) {
                return true;
            }
        }
        return false;
    }
    
    // 静态初始化块，在类加载时检查和复制建筑文件
    static {
        // 确定建筑文件的存储位置
        determineBuildingFolderLocation();
        // 检查和复制建筑文件
        checkAndCopyBuildingFiles();
    }
    
    // 确定建筑文件的存储位置
    private static void determineBuildingFolderLocation() {
        try {
            SIMUKRAFT_BUILDING_FOLDER = "simukraftbuilding";

            Path workingDir = Paths.get("").toAbsolutePath().normalize();
            LOGGER.info("[BuildingDataManager] 建筑文件将存储在当前工作目录: {}", workingDir.resolve(SIMUKRAFT_BUILDING_FOLDER));
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] 确定建筑文件存储位置失败: {}", e.getMessage());
            // 失败时使用默认值
            SIMUKRAFT_BUILDING_FOLDER = "simukraftbuilding";
        }
    }
    
    public static List<BuildingInfo> getBuildingsByCategory(String category) {
        List<BuildingInfo> buildings = new ArrayList<>();
        
        LOGGER.debug("[BuildingDataManager] ====== 开始加载建筑类别: {} ======", category);
        
        try {
            // 获取当前工作目录
            Path workingDir = Paths.get("").toAbsolutePath();
            LOGGER.debug("[BuildingDataManager] 当前工作目录: {}", workingDir);
            LOGGER.debug("[BuildingDataManager] 建筑文件夹名称: {}", SIMUKRAFT_BUILDING_FOLDER);
            
            // 确定项目根目录（如果当前在run目录，则返回上级）
            Path projectRoot = resolveProjectRoot(workingDir);
            LOGGER.debug("[BuildingDataManager] 解析后的项目根目录: {}", projectRoot);
            
            // 首先尝试从simukraftbuilding文件夹加载（相对于工作目录）
            Path simukraftBuildingPath = workingDir.resolve(SIMUKRAFT_BUILDING_FOLDER).resolve(category);
            LOGGER.debug("[BuildingDataManager] 尝试simukraftbuilding路径: {}", simukraftBuildingPath.toAbsolutePath());
            
            Path targetPath = null;
            
            if (Files.exists(simukraftBuildingPath) && Files.isDirectory(simukraftBuildingPath)) {
                targetPath = simukraftBuildingPath;
                LOGGER.info("[BuildingDataManager] 在simukraftbuilding文件夹中找到有效路径: {}", targetPath.toAbsolutePath());
            } else {
                LOGGER.debug("[BuildingDataManager] simukraftbuilding路径不存在，尝试其他可能的源路径");
                // 尝试多种可能的源路径
                Path[] possiblePaths = {
                    // 开发环境路径1：相对于项目根目录
                    projectRoot.resolve("src/main/resources").resolve(BUILDING_ROOT_PATH).resolve(category),
                    // 开发环境路径2：相对于运行目录
                    workingDir.resolve("..").resolve("src").resolve("main").resolve("resources").resolve(BUILDING_ROOT_PATH).resolve(category),
                    // 运行环境路径：构建后的资源目录
                    projectRoot.resolve("build/resources/main").resolve(BUILDING_ROOT_PATH).resolve(category),
                    // 绝对路径：用户目录下的项目
                    Paths.get(System.getProperty("user.dir"), "src", "main", "resources", BUILDING_ROOT_PATH, category)
                };
                
                // 遍历所有可能的路径，找到存在的第一个
                for (Path path : possiblePaths) {
                    LOGGER.debug("[BuildingDataManager] 检查路径: {}", path.toAbsolutePath());
                    if (Files.exists(path) && Files.isDirectory(path)) {
                        targetPath = path;
                        LOGGER.info("[BuildingDataManager] 找到有效路径: {}", targetPath.toAbsolutePath());
                        break;
                    }
                }
            }
            
            if (targetPath != null) {
                LOGGER.debug("[BuildingDataManager] 最终使用的路径: {}", targetPath.toAbsolutePath());
                
                // 检查目录是否存在
                if (!Files.exists(targetPath)) {
                    LOGGER.error("[BuildingDataManager] 错误: 目录不存在: {}", targetPath);
                    // 尝试从类路径加载作为后备
                    LOGGER.info("[BuildingDataManager] 尝试从类路径加载作为后备");
                    List<BuildingInfo> fallbackBuildings = loadFromClasspathFallback(category);
                    LOGGER.info("[BuildingDataManager] 从类路径后备加载到 {} 个建筑", fallbackBuildings.size());
                    return fallbackBuildings;
                }
                
                LOGGER.debug("[BuildingDataManager] 目录存在，列出所有文件...");
                
                // 先列出目录中的所有文件
                Files.list(targetPath).forEach(file -> {
                    LOGGER.debug("[BuildingDataManager] 目录中的文件: {}", file.getFileName());
                });
                
                AtomicInteger parsedCount = new AtomicInteger(0);
                AtomicInteger addedCount = new AtomicInteger(0);
                
                // 遍历目录中的所有.sk文件
                LOGGER.debug("[BuildingDataManager] 开始遍历目录中的.sk文件...");
                Files.walk(targetPath, 1) // 只遍历当前目录，不递归
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".sk"))
                    .forEach(skFilePath -> {
                        parsedCount.incrementAndGet();
                        try {
                            LOGGER.debug("[BuildingDataManager] 处理.sk文件: {}", skFilePath.getFileName());
                            BuildingInfo info = parseBuildingFile(skFilePath, category);
                            if (info != null) {
                                buildings.add(info);
                                addedCount.incrementAndGet();
                                LOGGER.debug("[BuildingDataManager] 成功加载建筑: {} (文件名: {})", info.getName(), info.getFileName());
                            } else {
                                LOGGER.error("[BuildingDataManager] 无法解析建筑文件: {}", skFilePath);
                            }
                        } catch (Exception e) {
                            LOGGER.error("[BuildingDataManager] 解析文件 {} 失败: {}", skFilePath, e.getMessage());
                        }
                    });
                
                LOGGER.info("[BuildingDataManager] .sk文件处理完成: 共处理 {} 个文件，成功添加 {} 个建筑", parsedCount.get(), addedCount.get());
            } else {
                LOGGER.warn("[BuildingDataManager] 未找到任何有效的建筑源路径");
            }
            
            // 如果没有从文件系统加载到建筑信息，尝试从类路径加载
            if (buildings.isEmpty()) {
                LOGGER.info("[BuildingDataManager] 从文件系统未加载到建筑信息，尝试从类路径加载作为后备");
                List<BuildingInfo> classpathBuildings = loadFromClasspathFallback(category);
                LOGGER.info("[BuildingDataManager] 从类路径加载到 {} 个建筑", classpathBuildings.size());
                buildings.addAll(classpathBuildings);
            }
            
            LOGGER.debug("[BuildingDataManager] ====== 完成加载类别 {}，共加载 {} 个建筑 ======", category, buildings.size());
                
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] 加载建筑类别 {} 时发生严重错误: {}", category, e.getMessage());
            // 发生异常时，尝试从类路径加载
            LOGGER.info("[BuildingDataManager] 发生异常，尝试从类路径加载作为后备");
            List<BuildingInfo> fallbackBuildings = loadFromClasspathFallback(category);
            LOGGER.info("[BuildingDataManager] 从类路径后备加载到 {} 个建筑", fallbackBuildings.size());
            buildings.addAll(fallbackBuildings);
        }
        
        return buildings;
    }
    
    private static BuildingInfo parseBuildingFile(Path skFilePath, String category) throws IOException {
        LOGGER.debug("[BuildingDataManager] Parsing file: {}", skFilePath);
        
        try {
            List<String> lines = Files.readAllLines(skFilePath, StandardCharsets.UTF_8);
            Map<String, String> data = new HashMap<>();
            
            LOGGER.debug("[BuildingDataManager] Reading {} lines from file", lines.size());
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                LOGGER.debug("[BuildingDataManager] Line {}: '{}'", (i+1), line);
                
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        // 去除可能的BOM字符
                        key = key.replace("\uFEFF", "").trim();
                        String value = parts[1].trim();
                        data.put(key, value);
                        LOGGER.debug("[BuildingDataManager] Parsed key-value: {} = {}", key, value);
                    }
                }
            }
            
            LOGGER.debug("[BuildingDataManager] Parsed data map: {}", data);
            
            String name = data.getOrDefault("name", Component.translatable("building.unknown.name").getString());
            String size = data.getOrDefault("size", Component.translatable("building.unknown.size").getString());
            String amount = data.getOrDefault("amount", Component.translatable("building.unknown.price").getString());
            String author = data.getOrDefault("author", Component.translatable("building.unknown.author").getString());
            String description = data.getOrDefault("description", Component.translatable("building.unknown.description").getString());
            String jobType = data.getOrDefault("job_type", null);
            
            // 解析标签 - 支持逗号分隔的多个标签
            List<String> tags = new ArrayList<>();
            String tagsStr = data.get("tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                for (String tag : tagsStr.split(",")) {
                    tags.add(tag.trim());
                }
            }
            
            LOGGER.debug("[BuildingDataManager] Extracted values:");
            LOGGER.debug("  name: {}", name);
            LOGGER.debug("  size: {}", size);
            LOGGER.debug("  amount: {}", amount);
            LOGGER.debug("  author: {}", author);
            LOGGER.debug("  description: {}", description);
            LOGGER.debug("  job_type: {}", jobType);
            LOGGER.debug("  tags: {}", tags);
            
            // 获取对应的litematic文件名
            String baseName = skFilePath.getFileName().toString().replace(".sk", "");
            String litematicFileName = baseName + ".litematic";
            
            BuildingInfo info = new BuildingInfo(name, size, amount, author, description, category, 
                                  skFilePath.getFileName().toString(), litematicFileName, jobType, tags);
            
            LOGGER.debug("[BuildingDataManager] Successfully created BuildingInfo: {}", info.getName());
            return info;
            
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] ERROR parsing file {}: {}", skFilePath, e.getMessage());
            throw e;
        }
    }
    
    private static List<BuildingInfo> loadFromClasspathFallback(String category) {
        List<BuildingInfo> buildings = new ArrayList<>();
        LOGGER.debug("[BuildingDataManager] Using classpath fallback for category: {}", category);
        
        try {
            // 获取类路径中的资源列表
            java.net.URL resourceUrl = BuildingDataManager.class.getClassLoader()
                .getResource(BUILDING_ROOT_PATH + "/" + category);
            
            if (resourceUrl != null) {
                // 尝试列出目录内容
                java.io.InputStream is = BuildingDataManager.class.getClassLoader()
                    .getResourceAsStream(BUILDING_ROOT_PATH + "/" + category);
                
                if (is != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(is));
                    
                    String fileName;
                    while ((fileName = reader.readLine()) != null) {
                        if (fileName.endsWith(".sk")) {
                            java.io.InputStream fileStream = BuildingDataManager.class.getClassLoader()
                                .getResourceAsStream(BUILDING_ROOT_PATH + "/" + category + "/" + fileName);
                            
                            if (fileStream != null) {
                                try {
                                    BuildingInfo info = parseBuildingFromStream(fileStream, category, fileName);
                                    if (info != null) {
                                        buildings.add(info);
                                    }
                                } finally {
                                    fileStream.close();
                                }
                            }
                        }
                    }
                    reader.close();
                }
            }
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] ERROR in classpath fallback: {}", e.getMessage());
        }
        
        return buildings;
    }
    
    private static BuildingInfo parseBuildingFromStream(java.io.InputStream inputStream, String category, String fileName) {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
            
            Map<String, String> data = new HashMap<>();
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim().replace("\uFEFF", "").trim();
                        String value = parts[1].trim();
                        data.put(key, value);
                        LOGGER.debug("[BuildingDataManager] Parsed key-value: {} = {}", key, value);
                    }
                }
            }
            reader.close();
            
            String name = data.getOrDefault("name", Component.translatable("building.unknown.name").getString());
            String size = data.getOrDefault("size", Component.translatable("building.unknown.size").getString());
            String amount = data.getOrDefault("amount", Component.translatable("building.unknown.price").getString());
            String author = data.getOrDefault("author", Component.translatable("building.unknown.author").getString());
            String description = data.getOrDefault("description", Component.translatable("building.unknown.description").getString());
            String jobType = data.getOrDefault("job_type", null);
            
            // 解析标签
            List<String> tags = new ArrayList<>();
            String tagsStr = data.get("tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                for (String tag : tagsStr.split(",")) {
                    tags.add(tag.trim());
                }
            }
            
            String baseName = fileName.replace(".sk", "");
            String litematicFileName = baseName + ".litematic";
            
            return new BuildingInfo(name, size, amount, author, description, category, 
                                   fileName, litematicFileName, jobType, tags);
            
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] ERROR parsing from stream: {}", e.getMessage());
            return null;
        }
    }
    
    public static List<BuildingInfo> searchBuildings(String query) {
        List<BuildingInfo> results = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        
        query = query.toLowerCase().trim();
        
        for (String category : CATEGORIES) {
            List<BuildingInfo> buildings = getBuildingsByCategory(category);
            for (BuildingInfo building : buildings) {
                if (building.getName().toLowerCase().contains(query)) {
                    results.add(building);
                }
            }
        }
        
        return results;
    }
    
    public static BuildingInfo getBuildingInfo(String category, String fileName) {
        List<BuildingInfo> buildings = getBuildingsByCategory(category);
        for (BuildingInfo building : buildings) {
            if (building.getFileName().equals(fileName)) {
                return building;
            }
        }
        return null;
    }

    // 检查和复制建筑文件到simukraftbuilding文件夹
    private static void checkAndCopyBuildingFiles() {
        LOGGER.info("[BuildingDataManager] 开始检查和复制建筑文件...");
        
        try {
            // 获取当前工作目录
            Path rootPath = Paths.get("").toAbsolutePath();
            LOGGER.debug("[BuildingDataManager] 当前工作目录: {}", rootPath);
            
            // 检查simukraftbuilding文件夹是否存在
            Path simukraftBuildingPath;
            if (SIMUKRAFT_BUILDING_FOLDER.startsWith("/") || SIMUKRAFT_BUILDING_FOLDER.matches("[A-Za-z]:.*")) {
                // 绝对路径，直接使用
                simukraftBuildingPath = Paths.get(SIMUKRAFT_BUILDING_FOLDER);
            } else {
                // 相对路径，与rootPath结合
                simukraftBuildingPath = rootPath.resolve(SIMUKRAFT_BUILDING_FOLDER);
            }
            
            LOGGER.debug("[BuildingDataManager] 最终建筑文件夹路径: {}", simukraftBuildingPath);
            
            if (!Files.exists(simukraftBuildingPath)) {
                LOGGER.info("[BuildingDataManager] 创建simukraftbuilding文件夹: {}", simukraftBuildingPath);
                Files.createDirectories(simukraftBuildingPath);
            }
            
            // 检查并创建所有类别子文件夹
            for (String category : CATEGORIES) {
                Path categoryPath = simukraftBuildingPath.resolve(category);
                if (!Files.exists(categoryPath)) {
                    LOGGER.debug("[BuildingDataManager] 创建类别文件夹: {}", categoryPath);
                    Files.createDirectories(categoryPath);
                }
            }
            
            // 复制建筑文件到simukraftbuilding文件夹
            for (String category : CATEGORIES) {
                copyBuildingFilesForCategory(category, simukraftBuildingPath);
            }
            
            LOGGER.info("[BuildingDataManager] 建筑文件检查和复制完成");
            
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] 检查和复制建筑文件失败: {}", e.getMessage());
        }
    }
    
    // 复制指定类别的建筑文件
    private static void copyBuildingFilesForCategory(String category, Path simukraftBuildingPath) {
        try {
            LOGGER.debug("[BuildingDataManager] 复制类别 '{}' 的建筑文件...", category);
            
            // 获取当前工作目录（可能是 run 目录或项目根目录）
            Path workingDir = Paths.get("").toAbsolutePath();
            Path projectRoot = resolveProjectRoot(workingDir);
            
            // 尝试多种可能的源路径
            Path[] possibleSourcePaths = {
                // 开发环境路径（相对于项目根目录）
                projectRoot.resolve("src/main/resources").resolve(BUILDING_ROOT_PATH).resolve(category),
                // 运行环境路径（相对于项目根目录）
                projectRoot.resolve("build/resources/main").resolve(BUILDING_ROOT_PATH).resolve(category),
                // 类路径资源
                null, // 标记使用类路径
            };
            
            Path sourcePath = null;
            boolean useClasspath = false;
            for (Path path : possibleSourcePaths) {
                if (path == null) {
                    useClasspath = true;
                    break;
                }
                LOGGER.debug("[BuildingDataManager] 检查源路径: {}", path);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    sourcePath = path;
                    LOGGER.info("[BuildingDataManager] 找到有效源路径: {}", sourcePath);
                    break;
                }
            }
            
            // 获取目标路径
            Path targetPath = simukraftBuildingPath.resolve(category);
            
            // 确保目标目录存在
            Files.createDirectories(targetPath);
            
            if (sourcePath != null) {
                LOGGER.debug("[BuildingDataManager] 使用源路径: {}", sourcePath);
                
                // 复制.sk、.nbt和.json文件
                long[] copyCount = {0};
                Files.walk(sourcePath, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".sk") || path.toString().endsWith(".nbt") || path.toString().endsWith(".json"))
                    .forEach(sourceFilePath -> {
                        try {
                            Path targetFilePath = targetPath.resolve(sourceFilePath.getFileName());
                            if (!Files.exists(targetFilePath) || Files.size(sourceFilePath) != Files.size(targetFilePath)) {
                                LOGGER.debug("[BuildingDataManager] 复制文件: {} -> {}", sourceFilePath.getFileName(), targetFilePath);
                                Files.copy(sourceFilePath, targetFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                copyCount[0]++;
                            }
                        } catch (Exception e) {
                            Path localTargetFilePath = targetPath.resolve(sourceFilePath.getFileName());
                            LOGGER.error("[BuildingDataManager] 复制文件失败: {} -> {}: {}", sourceFilePath, localTargetFilePath, e.getMessage());
                        }
                    });
                LOGGER.info("[BuildingDataManager] 类别 '{}' 复制完成，共复制 {} 个文件", category, copyCount[0]);
            } else if (useClasspath) {
                LOGGER.info("[BuildingDataManager] 未找到本地源路径，尝试从类路径复制 '{}' 的建筑文件...", category);
                
                // 从类路径复制建筑文件
                copyFilesFromClasspath(category, targetPath);
            } else {
                LOGGER.error("[BuildingDataManager] 错误：无法找到类别 '{}' 的建筑文件源", category);
            }
            
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] 复制类别 '{}' 的建筑文件失败: {}", category, e.getMessage());
        }
    }
    
    // 从类路径复制建筑文件到目标路径
    private static void copyFilesFromClasspath(String category, Path targetPath) {
        try {
            LOGGER.info("[BuildingDataManager] 从类路径复制建筑文件到: {}", targetPath);
            
            // 确保目标目录存在
            Files.createDirectories(targetPath);
            
            // 已知的建筑文件名列表
            String[] knownBuildingFiles = {
                // residential category files
                "2br.nbt", "2br.sk", "2srb.nbt", "2srb.sk", "bhb.nbt", "bhb.sk", "cc.nbt", "cc.sk",
                "dh.nbt", "dh.sk", "dlwh.nbt", "dlwh.sk", "gt.nbt", "gt.sk", "hsv.nbt", "hsv.sk",
                "kmsmr.nbt", "kmsmr.sk", "mc.nbt", "mc.sk", "mh.nbt", "mh.sk",
                "ncat.nbt", "ncat.sk", "rbrh.nbt", "rbrh.sk", "rewh.nbt", "rewh.sk", "sbc.nbt", "sbc.sk",
                "sc.nbt", "sc.sk", "szfv2.nbt", "szfv2.sk", "td.nbt", "td.sk", "u1.nbt", "u1.sk", 
                "wm.nbt", "wm.sk", "wtsv.nbt", "wtsv.sk",
                "bot.nbt", "bot.sk", "cxfz.nbt", "cxfz.sk", "dxmbzz.nbt", "dxmbzz.sk", "dxmzbs.nbt", "dxmzbs.sk",
                "gfdxzz.nbt", "gfdxzz.sk", "hp.nbt", "hp.sk", "jtmzj.nbt", "jtmzj.sk", "klp.nbt", "klp.sk",
                "klpzj.nbt", "klpzj.sk", "lindixiaowu.nbt", "lindixiaowu.sk", "mlxw.nbt", "mlxw.sk",
                "moguwu.nbt", "moguwu.sk", "mw.nbt", "mw.sk", "ptdxfz.nbt", "ptdxfz.sk", "ptdxfz2.nbt", "ptdxfz2.sk",
                "ptdxfz3.nbt", "ptdxfz3.sk", "shxw.nbt", "shxw.sk", "smxxzz.nbt", "smxxzz.sk",
                "ssxw.nbt", "ssxw.sk", "sygl.nbt", "sygl.sk", "syxw.nbt", "syxw.sk", "tfz.nbt", "tfz.sk",
                "tsgzz.nbt", "tsgzz.sk", "xiandaishuangcengbieshu.nbt", "xiandaishuangcengbieshu.sk",
                "xiandaisancengbieshu.nbt", "xiandaisancengbieshu.sk", "ymdbs.nbt", "ymdbs.sk",
                "ysyt.nbt", "ysyt.sk", "ysxzz.nbt", "ysxzz.sk", "zf.nbt", "zf.sk",
                "zhiyuxiaowu.nbt", "zhiyuxiaowu.sk", "zzl.nbt", "zzl.sk", "zkssxw.nbt", "zkssxw.sk",
                "szf.nbt", "szf.sk", "shuwu.nbt", "shuwu.sk", "tonghua.nbt", "tonghua.sk", "xiandaibieshu.nbt", "xiandaibieshu.sk",
                "huanggong.nbt", "huanggong.sk", "shenshe.nbt", "shenshe.sk", "ao.nbt", "ao.sk", "house1.nbt", "house1.sk", "house2.nbt", "house2.sk", "house3.nbt", "house3.sk",
                "house4.nbt", "house4.sk", "dfz1.nbt", "dfz1.sk", "ecdd.nbt", "ecdd.sk", "ecddxj.nbt", "ecddxj.sk", "scdddj.nbt", "scdddj.sk", "scsd.nbt", "scsd.sk", "xfz.nbt", "xfz.sk",
                "dcymf.nbt", "dcymf.sk", "dhz.nbt", "dhz.sk", "jjxw.nbt", "jjxw.sk", "srdbs.nbt", "srdbs.sk", "szxw.nbt", "szxw.sk", "tjysf.nbt", "tjysf.sk",
                "medievalcastle.nbt", "medievalcastle.sk", "qingLing001.nbt", "qingLing001.sk", "qingLing002.nbt", "qingLing002.sk",
                "qingLing003.nbt", "qingLing003.sk", "qingLing012.nbt", "qingLing012.sk", "qingLing013.nbt", "qingLing013.sk", "qingLing014.nbt", "qingLing014.sk",
                "tzb1.nbt", "tzb1.sk", "tzb2.nbt", "tzb2.sk", "tzb3.nbt", "tzb3.sk", "tzb4.nbt", "tzb4.sk", "tzb5.nbt", "tzb5.sk", "tzb6.nbt", "tzb6.sk",
                "tzb7.nbt", "tzb7.sk", "tzb8.nbt", "tzb8.sk", "tzb9.nbt", "tzb9.sk", "tzb10.nbt", "tzb10.sk", "cc2.nbt", "cc2.sk", "cxfz2.nbt", "cxfz2.sk",
                // commercial category files (支持大小写两种形式)
                "jcsd.nbt", "jcsd.sk", "JCSD.nbt", "JCSD.sk",
                "rp.nbt", "rp.sk", "RP.nbt", "RP.sk",
                "sgd.nbt", "sgd.sk", "SGD.nbt", "SGD.sk",
                "mbd.nbt", "mbd.sk", "MBD.nbt", "MBD.sk",
                "yy.nbt", "yy.sk", "YY.nbt", "YY.sk",
                "yh.nbt", "yh.sk", "YH.nbt", "YH.sk",
                // industry category files (支持大小写两种形式)
                "nnj.nbt", "nnj.sk", "nnj.json", "NNJ.nbt", "NNJ.sk", "NNJ.json",
                "yyj.nbt", "yyj.sk", "yyj.json", "YYJ.nbt", "YYJ.sk", "YYJ.json",
                "jjj.nbt", "jjj.sk", "jjj.json", "JJJ.nbt", "JJJ.sk", "JJJ.json",
                "yrj.nbt", "yrj.sk", "yrj.json", "YRJ.nbt", "YRJ.sk", "YRJ.json",
                "zzj.nbt", "zzj.sk", "zzj.json", "ZZJ.nbt", "ZZJ.sk", "ZZJ.json",
                // other category files
                "slszg.nbt", "slszg.sk", "fengyehuiguangjiaotang.nbt", "fengyehuiguangjiaotang.sk", "fengyejianyu.nbt", "fengyejianyu.sk", "fengyeluorichengbao.nbt", "fengyeluorichengbao.sk",
                "fengyeshiluotiankongyiji.nbt", "fengyeshiluotiankongyiji.sk", "fengyexinghuantianqiongta.nbt", "fengyexinghuantianqiongta.sk", "fengyezhongyanjitan.nbt", "fengyezhongyanjitan.sk",
                "huanyinglaidaomonidadushi.nbt", "huanyinglaidaomonidadushi.sk", "shizhongxin.nbt", "shizhongxin.sk",
            };

            
            // 筛选当前类别的文件
            List<String> categoryFiles = new ArrayList<>();
            for (String fileName : knownBuildingFiles) {
                String baseName = getBaseName(fileName);
                if (category.equals("residential") && matchesAny(baseName,
                    "2br", "2srb", "bhb", "cc", "dh", "dlwh", "gt", "hsv", "kmsmr", "mc", "mh", "ncat",
                    "rbrh", "rewh", "sbc", "sc", "szfv2", "td", "u1", "wm", "wtsv", "bot", "cxfz", "dxmbzz",
                    "dxmzbs", "gfdxzz", "hp", "jtmzj", "klp", "klpzj", "lindixiaowu", "mlxw", "moguwu", "mw",
                    "ptdxfz", "ptdxfz2", "ptdxfz3", "shxw", "smxxzz", "ssxw", "sygl", "syxw", "tfz", "tsgzz",
                    "xiandaishuangcengbieshu", "xiandaisancengbieshu", "ymdbs", "ysyt", "ysxzz", "zf",
                    "zhiyuxiaowu", "zzl", "shuwu", "tonghua", "xiandaibieshu", "szf", "zkssxw", "huanggong",
                    "shenshe", "ao", "house1", "house2", "house3", "house4", "dfz1", "ecdd", "ecddxj",
                    "scdddj", "scsd", "xfz", "dcymf", "dhz", "jjxw", "srdbs", "szxw", "tjysf",
                    "medievalcastle", "qingling001", "qingling002", "qingling003", "qingling012",
                    "qingling013", "qingling014", "tzb1", "tzb2", "tzb3", "tzb4", "tzb5", "tzb6", "tzb7",
                    "tzb8", "tzb9", "tzb10", "cc2", "cxfz2")) {
                    categoryFiles.add(fileName);
                } else if (category.equals("commercial") && matchesAny(baseName,
                    "jcsd", "rp", "sgd", "mbd", "yy", "yh")) {
                    categoryFiles.add(fileName);
                } else if (category.equals("industry") && matchesAny(baseName,
                    "nnj", "yyj", "jjj", "yrj", "zzj")) {
                    categoryFiles.add(fileName);
                } else if (category.equals("other") && matchesAny(baseName,
                    "slszg", "fengyehuiguangjiaotang", "fengyejianyu", "fengyeluorichengbao",
                    "fengyeshiluotiankongyiji", "fengyexinghuantianqiongta", "fengyezhongyanjitan",
                    "huanyinglaidaomonidadushi", "shizhongxin")) {
                    categoryFiles.add(fileName);
                }
            }
            
            LOGGER.debug("[BuildingDataManager] 当前类别 '{}' 的文件列表: {}", category, categoryFiles);
            
            // 复制文件
            for (String fileName : categoryFiles) {
                String resourcePath = BUILDING_ROOT_PATH + "/" + category + "/" + fileName;
                Path targetFilePath = targetPath.resolve(fileName);
                
                // 检查文件是否已经存在
                if (Files.exists(targetFilePath)) {
                    LOGGER.debug("[BuildingDataManager] 文件已存在，跳过: {}", targetFilePath);
                    continue;
                }
                
                // 尝试从类路径加载资源
                java.io.InputStream inputStream = BuildingDataManager.class.getClassLoader().getResourceAsStream(resourcePath);
                if (inputStream != null) {
                    try {
                        LOGGER.debug("[BuildingDataManager] 从类路径加载资源: {}", resourcePath);
                        Files.copy(inputStream, targetFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.debug("[BuildingDataManager] 复制文件成功: {} -> {}", fileName, targetFilePath);
                    } catch (Exception e) {
                        LOGGER.error("[BuildingDataManager] 复制文件失败: {} -> {}: {}", resourcePath, targetFilePath, e.getMessage());
                    } finally {
                        inputStream.close();
                    }
                } else {
                    LOGGER.error("[BuildingDataManager] 无法从类路径加载资源: {}", resourcePath);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("[BuildingDataManager] 从类路径复制建筑文件失败: {}", e.getMessage());
        }
    }

    public static CompoundTag loadBuildingData(String buildingName, String category) {
        try {
            // 构建NBT文件路径
            String fileName = buildingName + ".nbt";
            Path nbtPath;
            
            if (SIMUKRAFT_BUILDING_FOLDER.startsWith("/") || SIMUKRAFT_BUILDING_FOLDER.matches("[A-Za-z]:.*")) {
                // 绝对路径，直接使用
                nbtPath = Paths.get(SIMUKRAFT_BUILDING_FOLDER, category, fileName);
            } else {
                // 相对路径，与当前工作目录结合
                Path rootPath = Paths.get("").toAbsolutePath();
                nbtPath = rootPath.resolve(SIMUKRAFT_BUILDING_FOLDER).resolve(category).resolve(fileName);
            }
            
            if (!Files.exists(nbtPath)) {
                // 尝试从类路径加载作为后备
                java.io.InputStream inputStream = BuildingDataManager.class.getClassLoader()
                    .getResourceAsStream(BUILDING_ROOT_PATH + "/" + category + "/" + fileName);
                
                if (inputStream != null) {
                    return net.minecraft.nbt.NbtIo.readCompressed(inputStream);
                }
                
                LOGGER.error("建筑NBT文件不存在: {}", nbtPath.toAbsolutePath());
                return null;
            }
            
            return net.minecraft.nbt.NbtIo.readCompressed(java.util.Objects.requireNonNull(Files.newInputStream(nbtPath)));
            
        } catch (Exception e) {
            LOGGER.error("加载建筑NBT文件失败: {}", e.getMessage());
            return null;
        }
    }
    
    // 初始化方法，用于触发静态初始化块
    public static void init() {
        // 空方法，仅用于触发类的加载和静态初始化
        LOGGER.info("[BuildingDataManager] init() called, static initialization should be completed");
    }
    
    // 主方法，用于测试触发文件复制机制
    public static void main(String[] args) {
        LOGGER.info("Testing BuildingDataManager static initialization...");
        // 触发静态初始化块，执行文件复制
        LOGGER.info("Test completed, check if simukraftbuilding folder is generated");
    }
}
