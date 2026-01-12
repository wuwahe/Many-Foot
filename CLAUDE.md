# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 项目概述

ManyFoot 是一个基于 Spring Boot 3.4.4 构建的多厂商 AI 智能体编排框架，实现了 Manus 风格的智能体循环，支持在 Docker 沙箱中自主执行任务。使用 Java 17 和 Maven。

## 构建和运行命令

```bash
# 构建
mvn clean package

# 运行
mvn spring-boot:run
# 或者: java -jar target/ManyFoot-0.0.1-SNAPSHOT.jar

# 运行测试
mvn test
```

应用运行在 8100 端口。

## 架构

### 智能体循环模式

`ManusOrchestrator` 实现了 ReAct 风格的自主执行循环：

```
分析(ANALYZE) → 规划(PLAN) → 执行(EXECUTE) → 观察(OBSERVE) → [循环或完成]
```

每个阶段使用针对任务优化的不同 AI 模型：
- **ANALYZE**：复杂推理和任务分解
- **EXECUTE**：通过 ReactAgent 进行工具调用和代码生成
- **OBSERVE**：轻量级结果验证

### 智能体模块 (`agent/`)

智能体模块使用**模板方法模式**、**策略模式**和**工厂模式**：

**核心接口** (`agent/core/`)：
- `Agent<R>` - 所有智能体的基础接口
- `ToolAwareAgent<R>` - 支持工具的智能体接口
- `StreamingAgent<T>` - 支持流式输出的智能体接口
- `AbstractAgent<R>` - 模板方法基类
- `AbstractToolAgent<R>` - 支持工具的抽象类

**执行策略** (`agent/strategy/`)：
- `SyncCallStrategy` - 同步执行（Analyzer、Observer）
- `StreamingStrategy` - 基于 Flux 的流式执行（Executor）

**具体智能体** (`agent/impl/`)：
| 智能体 | 模型角色 | 工具集 | 策略 |
|-------|---------|-------|------|
| `AnalyzerAgent` | ANALYZE | 无 | 同步 |
| `ExecutorAgent` | EXECUTE | 完整工具集 | 流式 |
| `SubtaskExecutorAgent` | EXECUTE | 完整工具集 | 流式 |
| `ObserverAgent` | OBSERVE | 只读工具集 | 同步 |

**工具提供者** (`agent/tool/`)：
- `FullToolProvider` - 所有 CodeAct 工具（Executor 使用）
- `ReadOnlyToolProvider` - 仅 readFile、listDir（Observer 使用）

**提示词提供者** (`agent/prompt/`)：
每个智能体都有自己的 `AgentPromptProvider` 实现。

**工厂** (`agent/factory/`)：
`AgentFactory` 提供对所有智能体的统一访问。

### 多厂商 AI 模型系统

模型按角色（`ModelRole.ANALYZE`、`EXECUTE`、`OBSERVE`）组织在 `AiModelStorage` 中。工厂模式（`AiModelFactory`）支持厂商特定实现：
- `OpenAiModelFactory`、`GeminiModelFactory`、`DashscopeModelFactory`、`DeepSeekModelFactory`、`AnthropicModelFactory`、`OllamaModelFactory`

`AiModelInitializer` 在启动时根据 `AgentModelProperties` 创建模型。运行时通过 `AiModelStorage.get(ModelRole)` 获取模型。

### 代码执行

`CodeActEngine` 在由 `SandboxContainerManager` 管理的隔离 Docker 容器中执行 Python/Shell 代码。容器具有资源限制（内存、CPU）、空闲超时清理，并支持使用 Base64 编码的文件 I/O。

### 事件流

`EventStreamService` 处理：
- 事件持久化到 Redis Lists，保留 7 天
- 用于实时 SSE 流的 Pub/Sub
- 带序列号的会话状态管理，支持断线重连

### 异步任务管理

`AsyncTaskManager` 提供基于 CompletableFuture 的任务提交、状态跟踪、取消以及通过事件重放的断线恢复。

## 关键模块依赖

```
ManusController
    → ManusOrchestrator
        → AgentFactory（提供所有智能体）
            → AnalyzerAgent、ExecutorAgent、SubtaskExecutorAgent、ObserverAgent
        → CodeActEngine（代码执行）
        → SandboxContainerManager（Docker 生命周期）
        → EventStreamService（事件处理）
        → AiModelStorage（模型获取）

AsyncTaskManager
    → ManusOrchestrator
    → EventStreamService
    → Redis（通过 RedisUtils）
```

## 配置

配置在 `application.yaml` 中。关键部分：
- `many-foot.model.*`：默认 AI 厂商和凭证
- `spring.ai.*`：厂商特定 API 密钥（dashscope、google.genai）
- `manus.sandbox.*`：Docker 沙箱设置（镜像、资源限制、超时）
- `spring.data.redis.*`：用于事件和会话状态的 Redis 连接

## REST API 接口

- `POST /manus/execute/stream` - SSE 流式执行
- `POST /manus/task/submit` - 异步任务提交
- `GET /manus/task/{taskId}/status` - 任务状态轮询
- `POST /manus/task/{taskId}/cancel` - 任务取消
- `GET /manus/events/{sessionId}` - 事件历史
- `GET /manus/session/{sessionId}/exists` - 会话存在检查

## 基础设施要求

- Java 17+
- Docker（远程或本地）用于沙箱执行
- Redis 用于会话状态和事件
- 按配置的 AI 厂商 API 密钥
