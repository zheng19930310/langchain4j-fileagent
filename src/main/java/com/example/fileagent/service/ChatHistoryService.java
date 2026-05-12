package com.example.fileagent.service;

import com.example.fileagent.model.ChatSessionInfo;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);
    private static final String HISTORY_DIR = "./chat-history";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public String generateSessionId() {
        return "session_" + LocalDateTime.now().format(FORMATTER) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void saveSession(String sessionId, List<ChatMessage> messages) {
        try {
            Path dir = Paths.get(HISTORY_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path sessionFile = dir.resolve(sessionId + ".json");
            StringBuilder json = new StringBuilder("[\n");
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage msg = messages.get(i);
                json.append("  {\n");
                json.append("    \"type\": \"").append(msg.type()).append("\",\n");
                json.append("    \"text\": ").append(escapeJson(msg.text())).append("\n");
                json.append("  }");
                if (i < messages.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("]");

            Files.writeString(sessionFile, json.toString());
            log.info("保存会话历史: {}, 消息数: {}", sessionId, messages.size());
        } catch (IOException e) {
            log.error("保存会话历史失败: {}", sessionId, e);
        }
    }

    public List<ChatMessage> loadSession(String sessionId) {
        try {
            Path sessionFile = Paths.get(HISTORY_DIR, sessionId + ".json");
            if (!Files.exists(sessionFile)) {
                return Collections.emptyList();
            }

            String content = Files.readString(sessionFile);
            // Simple JSON parsing - in production, use a proper JSON library
            List<ChatMessage> messages = new ArrayList<>();
            // For now, return empty list - would need proper JSON deserialization
            log.info("加载会话历史: {}", sessionId);
            return messages;
        } catch (IOException e) {
            log.error("加载会话历史失败: {}", sessionId, e);
            return Collections.emptyList();
        }
    }

    public List<Map<String, String>> loadSessionRaw(String sessionId) {
        List<ChatMessage> messages = loadSession(sessionId);
        return messages.stream()
                .map(msg -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("type", msg.type().toString());
                    map.put("text", msg.text());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<ChatSessionInfo> listSessions() {
        try {
            Path dir = Paths.get(HISTORY_DIR);
            if (!Files.exists(dir)) {
                return Collections.emptyList();
            }

            return Files.list(dir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> {
                        ChatSessionInfo info = new ChatSessionInfo();
                        String fileName = p.getFileName().toString();
                        info.setSessionId(fileName.replace(".json", ""));
                        info.setTitle("会话 " + info.getSessionId().substring(0, Math.min(20, info.getSessionId().length())));
                        try {
                            info.setLastUpdateTime(LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(p).toInstant(),
                                    java.time.ZoneId.systemDefault()));
                        } catch (IOException e) {
                            info.setLastUpdateTime(LocalDateTime.now());
                        }
                        info.setMessageCount(0); // Would need to parse file to get count
                        return info;
                    })
                    .sorted((a, b) -> b.getLastUpdateTime().compareTo(a.getLastUpdateTime()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出会话失败", e);
            return Collections.emptyList();
        }
    }

    public boolean deleteSession(String sessionId) {
        try {
            Path sessionFile = Paths.get(HISTORY_DIR, sessionId + ".json");
            if (Files.exists(sessionFile)) {
                Files.delete(sessionFile);
                log.info("删除会话: {}", sessionId);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("删除会话失败: {}", sessionId, e);
            return false;
        }
    }

    public void clearAll() {
        try {
            Path dir = Paths.get(HISTORY_DIR);
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.error("删除文件失败: {}", p, e);
                            }
                        });
                log.info("清除所有会话历史");
            }
        } catch (IOException e) {
            log.error("清除所有会话失败", e);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
