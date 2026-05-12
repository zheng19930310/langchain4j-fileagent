package com.example.fileagent.skill;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class FileTools {

    private static final Logger log = LoggerFactory.getLogger(FileTools.class);

    @Tool("列出所有磁盘驱动器及容量信息")
    public String listDisk() {
        try {
            File[] roots = File.listRoots();
            StringBuilder sb = new StringBuilder("磁盘驱动器列表:\n");
            for (File root : roots) {
                long totalSpace = root.getTotalSpace();
                long freeSpace = root.getFreeSpace();
                long usedSpace = totalSpace - freeSpace;
                double totalGB = totalSpace / (1024.0 * 1024 * 1024);
                double freeGB = freeSpace / (1024.0 * 1024 * 1024);
                double usedGB = usedSpace / (1024.0 * 1024 * 1024);
                sb.append(String.format("  %s - 总容量: %.2f GB, 已用: %.2f GB, 可用: %.2f GB\n",
                        root.getPath(), totalGB, usedGB, freeGB));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取磁盘信息失败", e);
            return "获取磁盘信息失败: " + e.getMessage();
        }
    }

    @Tool("列出目录内容。参数: directoryPath - 目录路径")
    public String listFiles(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                return "目录不存在: " + directoryPath;
            }
            if (!Files.isDirectory(path)) {
                return "路径不是目录: " + directoryPath;
            }

            File dir = path.toFile();
            File[] files = dir.listFiles();
            if (files == null) {
                return "无法读取目录内容: " + directoryPath;
            }

            StringBuilder sb = new StringBuilder("目录内容 (" + directoryPath + "):\n");
            Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File file : files) {
                String type = file.isDirectory() ? "[DIR]" : "[FILE]";
                long size = file.isFile() ? file.length() : 0;
                sb.append(String.format("  %s %-40s %d bytes\n", type, file.getName(), size));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("列出目录失败: {}", directoryPath, e);
            return "列出目录失败: " + e.getMessage();
        }
    }

    @Tool("获取文件或目录大小。参数: filePath - 文件路径")
    public String getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "文件/目录不存在: " + filePath;
            }

            File file = path.toFile();
            long size;
            if (file.isDirectory()) {
                size = Files.walk(path)
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .sum();
            } else {
                size = Files.size(path);
            }

            double sizeKB = size / 1024.0;
            double sizeMB = sizeKB / 1024.0;
            double sizeGB = sizeMB / 1024.0;

            String sizeStr;
            if (sizeGB >= 1) {
                sizeStr = String.format("%.2f GB", sizeGB);
            } else if (sizeMB >= 1) {
                sizeStr = String.format("%.2f MB", sizeMB);
            } else if (sizeKB >= 1) {
                sizeStr = String.format("%.2f KB", sizeKB);
            } else {
                sizeStr = size + " bytes";
            }

            return String.format("%s - 大小: %s (%d bytes)", filePath, sizeStr, size);
        } catch (Exception e) {
            log.error("获取文件大小失败: {}", filePath, e);
            return "获取文件大小失败: " + e.getMessage();
        }
    }

    @Tool("读取文件内容。参数: filePath - 文件路径（可选参数: maxLength - 最大读取长度）")
    public String readFile(String args) {
        try {
            // 智能解析参数：支持单个参数或多个参数（用|分隔）
            String filePath;
            Integer maxLength = null;
            
            if (args.contains("|")) {
                // 多参数格式: filePath|maxLength
                String[] parts = args.split("\\|", 2);
                filePath = parts[0].trim();
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    maxLength = Integer.parseInt(parts[1].trim());
                }
            } else {
                // 单参数格式: 仅文件路径
                filePath = args.trim();
            }

            log.info("读取文件请求 - 路径: {}, 最大长度: {}", filePath, maxLength);

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "文件不存在: " + filePath;
            }
            if (!Files.isRegularFile(path)) {
                return "路径不是文件: " + filePath;
            }

            String content = Files.readString(path);
            log.info("成功读取文件: {}, 内容长度: {} 字符", filePath, content.length());
            
            if (maxLength != null && content.length() > maxLength) {
                content = content.substring(0, maxLength) + "\n... (内容被截断，总长度: " + content.length() + " 字符)";
            }

            return "文件内容 (" + filePath + "):\n" + content;
        } catch (NumberFormatException e) {
            log.error("参数格式错误: {}", args, e);
            return "参数格式错误，请使用: 文件路径 或 文件路径|最大长度";
        } catch (Exception e) {
            log.error("读取文件失败: {}", args, e);
            return "读取文件失败: " + e.getMessage();
        }
    }

    @Tool("创建新文件。参数: filePath|content - 文件路径和内容")
    public String createFile(String args) {
        try {
            String[] parts = args.split("\\|", 2);
            String filePath = parts[0].trim();
            String content = parts.length > 1 ? parts[1] : "";

            Path path = Paths.get(filePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, content);
            return "文件创建成功: " + filePath;
        } catch (Exception e) {
            log.error("创建文件失败: {}", args, e);
            return "创建文件失败: " + e.getMessage();
        }
    }

    @Tool("编辑文件内容。参数: filePath|newContent - 文件路径和新内容")
    public String editFile(String args) {
        try {
            String[] parts = args.split("\\|", 2);
            String filePath = parts[0].trim();
            String newContent = parts.length > 1 ? parts[1] : "";

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "文件不存在: " + filePath;
            }

            Files.writeString(path, newContent);
            return "文件编辑成功: " + filePath;
        } catch (Exception e) {
            log.error("编辑文件失败: {}", args, e);
            return "编辑文件失败: " + e.getMessage();
        }
    }

    @Tool("删除文件或目录。参数: filePath - 文件路径")
    public String deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "文件/目录不存在: " + filePath;
            }

            if (Files.isDirectory(path)) {
                Files.walk(path)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (Exception e) {
                                log.error("删除失败: {}", p, e);
                            }
                        });
            } else {
                Files.delete(path);
            }

            return "删除成功: " + filePath;
        } catch (Exception e) {
            log.error("删除文件失败: {}", filePath, e);
            return "删除文件失败: " + e.getMessage();
        }
    }
}
