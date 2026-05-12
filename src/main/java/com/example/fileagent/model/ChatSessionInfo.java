package com.example.fileagent.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatSessionInfo {
    private String sessionId;
    private String title;
    private LocalDateTime lastUpdateTime;
    private int messageCount;
}
