package com.example.fileagent.service;

import com.example.fileagent.skill.FileTools;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private FileTools fileTools;

    @Autowired
    private ChatHistoryService historyService;

    @Value("${langchain4j.openai.api-key:}")
    private String apiKey;

    @Value("${langchain4j.openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${langchain4j.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            你是一个智能文件助手，可以帮助用户管理本地文件和回答问题。

            你拥有以下文件操作工具，当用户需要时会自动调用：
            1. listDisk - 查看所有磁盘驱动器及容量信息（无参数）
            2. listFiles - 列出目录内容（参数：目录路径）
            3. getFileSize - 获取文件/目录大小（参数：文件路径）
            4. readFile - 读取文件内容（参数：文件路径|maxLength）
            5. createFile - 创建新文件（参数：文件路径|文件内容）
            6. editFile - 编辑文件内容（参数：文件路径|新内容）
            7. deleteFile - 删除文件/目录（参数：文件路径）

            使用规则：
            1. 当需要调用工具时，直接调用相应的工具函数
            2. 调用工具后，用自然语言向用户解释结果
            3. 路径格式使用正斜杠，如 D:/workspace/test.txt
            4. 如果问题不涉及文件操作，直接回答
            """;

    private ChatLanguageModel createChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .build();
    }

    public String chat(String message, String sessionId) {
        log.info("=== [chat] 用户输入: {} ===", message);
        
        ChatMemory memory = getOrCreateSession(sessionId);
        memory.add(UserMessage.from(message));

        ChatLanguageModel model = createChatModel();
        
        // Create AI service with tools
        FileAgent agent = AiServices.builder(FileAgent.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .tools(fileTools)
                .build();

        String response = agent.chat(message);
        log.info("[chat] AI回复: {}", response);
        
        saveSession(sessionId);
        return response;
    }

    public String chatWithFiles(String message, List<String> filePaths, List<String> fileNames, String sessionId) {
        log.info("=== [chatWithFiles] 用户输入: {}, 文件数: {} ===", message, filePaths != null ? filePaths.size() : 0);
        
        // For now, just process as regular chat
        // In a full implementation, you would handle file attachments here
        return chat(message, sessionId);
    }

    public Flux<String> streamChat(String message, String sessionId) {
        log.info("=== [stream] 用户输入: {} ===", message);
        
        // Langchain4J streaming support
        ChatMemory memory = getOrCreateSession(sessionId);
        memory.add(UserMessage.from(message));

        ChatLanguageModel model = createChatModel();
        
        FileAgent agent = AiServices.builder(FileAgent.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .tools(fileTools)
                .build();

        // Note: Full streaming with tools requires more complex implementation
        // For now, return as single flux element
        String response = agent.chat(message);
        saveSession(sessionId);
        
        return Flux.just(response);
    }

    public Flux<String> streamChatWithFiles(String message, List<String> filePaths, List<String> fileNames, String sessionId) {
        return streamChat(message, sessionId);
    }

    private ChatMemory getOrCreateSession(String sessionId) {
        return sessionMemories.computeIfAbsent(sessionId, k -> {
            ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
            memory.add(SystemMessage.from(SYSTEM_PROMPT));
            
            // Load history from file
            List<ChatMessage> saved = historyService.loadSession(sessionId);
            if (!saved.isEmpty()) {
                for (ChatMessage msg : saved) {
                    memory.add(msg);
                }
                log.info("[Session] 从文件加载 {} 条历史消息: {}", saved.size(), sessionId);
            }
            
            return memory;
        });
    }

    private void saveSession(String sessionId) {
        ChatMemory memory = sessionMemories.get(sessionId);
        if (memory != null) {
            List<ChatMessage> messages = memory.messages();
            historyService.saveSession(sessionId, messages);
        }
    }

    public void clearSession(String sessionId) {
        sessionMemories.remove(sessionId);
    }

    /**
     * AI Service interface for Langchain4J
     */
    interface FileAgent {
        String chat(String message);
    }
}
