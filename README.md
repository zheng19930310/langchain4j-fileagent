# Langchain4J FileAgent

基于Langchain4J实现的智能文件助手AI Agent，支持文件操作和对话功能。

## 功能特性

- 📁 文件管理：查看磁盘、列出目录、获取文件大小
- 📝 文件操作：读取、创建、编辑、删除文件
- 💬 智能对话：基于OpenAI GPT模型的智能对话
- 🔄 会话管理：支持多会话和历史记录保存
- 🚀 流式响应：支持SSE流式输出

## 技术栈

- **Spring Boot 3.2.5** - Web框架
- **Langchain4J 0.36.2** - Java AI应用框架
- **OpenAI API** - 大语言模型
- **Apache POI** - Office文档处理
- **Apache PDFBox** - PDF文档处理

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+
- OpenAI API Key

### 2. 配置API Key

复制配置文件模板：
```bash
copy src\main\resources\application-example.yml src\main\resources\application.yml
```

编辑 `application.yml`，填入你的OpenAI API Key：
```yaml
langchain4j:
  openai:
    api-key: sk-your-openai-api-key-here
```

### 3. 启动应用

#### 方式一：使用启动脚本（推荐）
```bash
start.bat
```

#### 方式二：使用Maven命令
```bash
mvn spring-boot:run
```

#### 方式三：打包后运行
```bash
mvn clean package
java -jar target/langchain4j-fileagent-1.0.0.jar
```

### 4. 访问应用

应用启动后，默认运行在 http://localhost:8081

## API接口

### 聊天接口

**非流式聊天**
```http
POST /api/chat
Content-Type: application/json

{
  "message": "帮我查看D盘有哪些文件",
  "sessionId": "session_001"
}
```

**流式聊天**
```http
POST /api/chat/stream
Content-Type: application/json

{
  "message": "创建一个测试文件",
  "sessionId": "session_001"
}
```

### 会话管理接口

**列出所有会话**
```http
GET /api/chat/sessions
```

**获取会话详情**
```http
GET /api/chat/session/{sessionId}
```

**删除会话**
```http
DELETE /api/chat/session/{sessionId}
```

**清除所有会话**
```http
DELETE /api/chat/sessions
```

## 文件操作工具

AI Agent支持以下文件操作工具：

1. **listDisk** - 查看所有磁盘驱动器及容量信息
2. **listFiles** - 列出目录内容
3. **getFileSize** - 获取文件/目录大小
4. **readFile** - 读取文件内容
5. **createFile** - 创建新文件
6. **editFile** - 编辑文件内容
7. **deleteFile** - 删除文件/目录

## 项目结构

```
langchain4j-fileagent/
├── src/main/java/com/example/fileagent/
│   ├── Langchain4JFileAgentApplication.java  # 主应用类
│   ├── controller/
│   │   └── ChatController.java               # 聊天控制器
│   ├── service/
│   │   ├── ChatService.java                  # 聊天服务
│   │   └── ChatHistoryService.java           # 会话历史服务
│   ├── model/
│   │   ├── ChatRequest.java                  # 聊天请求模型
│   │   └── ChatSessionInfo.java              # 会话信息模型
│   └── skill/
│       └── FileTools.java                    # 文件操作工具
├── src/main/resources/
│   └── application-example.yml               # 配置示例
├── pom.xml                                    # Maven配置
└── start.bat                                  # 启动脚本
```

## 与原fileAgent的区别

| 特性 | fileAgent (Spring AI) | langchain4j-fileagent |
|------|----------------------|----------------------|
| AI框架 | Spring AI Alibaba | Langchain4J |
| 模型提供商 | 阿里云通义千问 | OpenAI |
| 工具调用 | 自定义标记解析 | Langchain4J原生支持 |
| 配置方式 | spring.ai.dashscope | langchain4j.openai |

## 注意事项

1. **API费用**：使用OpenAI API会产生费用，请注意控制使用量
2. **安全限制**：建议配置允许操作的基础目录，避免误操作重要文件
3. **会话存储**：会话历史保存在 `./chat-history` 目录下

## 许可证

MIT License
