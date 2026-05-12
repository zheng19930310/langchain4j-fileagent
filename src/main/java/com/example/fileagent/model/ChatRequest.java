package com.example.fileagent.model;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
    private List<String> filePaths;
    private List<String> fileNames;
}
