# AGENTS.md

本文件为 OpenCode / AI 编程 Agent 在 ManyFoot 代码库中工作时提供高信号指导。

AI 在本项目中不应只完成"能跑的代码"，而应以架构师视角进行设计与实现：优先保证模块边界清晰、职责单一、低耦合、高内聚、易测试、易扩展、易替换。

---

## 一、核心编码原则

### 1. 架构师式编码要求

在新增、修改代码时，必须优先考虑：

- 是否符合现有模块边界
- 是否破坏已有继承链、策略链、工厂注册机制
- 是否引入了不必要的耦合
- 是否方便后续扩展新的 Agent、模型厂商、工具、沙箱能力
- 是否可以通过配置扩展，而不是硬编码
- 是否可以独立测试
- 是否符合 Spring Boot 的 Bean 生命周期和条件装配习惯

不要为了快速实现功能而写临时逻辑、硬编码分支、跨模块直接调用、重复代码或"大而全"的类。

---

## 二、快速参考

```bash
# 构建
mvn clean package

# 运行（端口 8100）
mvn spring-boot:run

# 测试
mvn test
```

---

## 三、项目概述

ManyFoot 是基于 **Spring AI Alibaba Agent Framework** 的 Supervisor 多智能体协作系统，采用 ReactAgent 模式实现智能体编排。

技术栈：

* Spring Boot 3.4.4
* Java 17
* Maven
* Spring AI 1.1.0
* Spring AI Alibaba Agent Framework 1.1.0.0
* Redis (Redisson)
* Docker Sandbox
* Apache Tika（文档解析）

本项目定位：**可扩展的多智能体协作框架**，通过 Supervisor 模式协调 8 个智能体完成复杂任务。

---

## 四、根包与模块边界

根包：

```text
com.lh.manyfoot
```

单模块 Maven 项目。

| 包路径 | 职责 |
|---|---|
| `agent.core.*` | 智能体核心接口：Agent、AbstractAgent、AbstractToolAgent、StreamingAgent、ToolAwareAgent |
| `agent.context.*` | 智能体执行上下文：AgentContext、AgentAttachment、SessionContextHolder |
| `agent.domain.*` | 智能体领域对象：PlanGraph、TaskSpec、ActionCall、ActionResult、DomainDraft 等 |
| `agent.exception.*` | 智能体异常：AgentExecutionException |
| `agent.factory.*` | 智能体工厂：AgentFactory、AgentType |
| `agent.impl.*` | 智能体实现：SupervisorAgent、PlannerRouterAgent、ResearchRetrievalAgent、DomainSpecialistAgent、DocumentSpecialistAgent、ToolActionExecutorAgent、CodeAgent、ChatAgent |
| `agent.prompt.*` | 提示词提供者：AgentPromptProvider 及各智能体专属 PromptProvider |
| `agent.registry.*` | 智能体注册表：AgentRegistry（动态发现所有 Agent Bean） |
| `agent.strategy.*` | 执行策略：ExecutionStrategy、SyncCallStrategy、StreamingStrategy |
| `agent.supervisor.*` | Supervisor 编排：AgentTool（Agent→ToolCallback 适配器）、SupervisorToolProvider |
| `agent.support.*` | 工具类：SpecialistJsonUtils、AgentMessageFactory |
| `agent.tool.*` | 工具提供者：AgentToolProvider、FullToolProvider |
| `agent.tool.sandbox.*` | 沙箱工具：SandboxTool、SandboxEngine、DocumentParser、ExecutionResultFormatter |
| `agent.tool.sandbox.domain.*` | 沙箱领域对象：SandboxExecutionRequest、SandboxExecutionResult、SandboxCodeType |
| `models.registry.*` | 模型注册：ModelResolver、AiModelRegistrar、ModelRole、AiModelInitializer |
| `models.failover.*` | 故障转移：FailoverChatModel |
| `models.support.*` | 模型支持：ChatOptionsBinder |
| `models.*` | 厂商工厂：AiModelFactory 接口及各厂商实现（Dashscope、OpenAI、Anthropic、DeepSeek、Gemini、Ollama、Qianfan 等） |
| `config.*` | Spring 配置：RedissonConfig、DockerClientConfig、McpClientConfig、ManyFootToolConfig |
| `config.properties.*` | 配置属性：AiProvidersProperties、SandboxConfig、VendorEnums |
| `controller.*` | REST API：ChatController、R（统一响应）、HttpStatus |
| `controller.dto.*` | 请求 DTO：ChatRequest |
| `service.*` | 基础设施服务：SandboxContainerManager、RedisUtils |
| `domain` | 共享 DTO / 枚举：ExecutionResult、ExecutionStatus、SandboxContainer、ContainerStatus、AiModelConfig |

### 模块边界要求

AI 修改代码时必须遵守：

* `controller` 只负责 API 入参、出参、协议适配，不写业务编排逻辑
* `agent` 只负责智能体行为抽象、提示词、策略和工具调用组织
* `models` 只负责模型解析、模型注册、厂商适配、故障转移
* `agent.tool.sandbox` 只负责代码执行和沙箱能力，不承载 Agent 决策逻辑
* `config` 只做配置绑定和 Bean 装配，不写运行时业务逻辑
* `service` 可放基础设施服务，但不要变成万能业务层

跨模块调用必须通过接口、抽象类、工厂或服务完成，避免直接依赖具体实现类。

---

## 五、入口点

* 应用入口：

```text
src/main/java/com/lh/manyfoot/ManyFootApplication.java
```

* 智能体工厂：

```text
com.lh.manyfoot.agent.factory.AgentFactory
```

通过 `AgentFactory.getAgent(AgentType)` 获取智能体实例。

---

## 六、关键架构模式

### 1. Supervisor 多智能体协作架构

本项目采用 **Supervisor 模式**，由 8 个智能体协作完成任务：

```text
┌─────────────────────────────────────────────────────────────┐
│                     Supervisor 协作流                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │ PlannerRouter │───▶│  DomainSpec  │───▶│ ToolExecutor │  │
│  │   规划路由     │    │   领域专家    │    │   工具执行    │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                   ▲                   │           │
│         │                   │                   ▼           │
│         ▼                   │            ┌──────────────┐  │
│  ┌──────────────┐           │            │   CodeAgent  │  │
│  │ ResearchRetr │───────────┘            │   代码执行    │  │
│  │   检索证据    │◀───────────────────────│              │  │
│  └──────────────┘                        └──────────────┘  │
│                                                             │
│  ┌──────────────┐                        ┌──────────────┐  │
│  │  DocumentSpec│                        │    Chat      │  │
│  │  文档专家     │                        │  多模态对话   │  │
│  └──────────────┘                        └──────────────┘  │
│                                                             │
│              ┌──────────────────────────┐                  │
│              │       Supervisor         │                  │
│              │    顶层编排调度智能体      │                  │
│              └──────────────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
```

8 个智能体：

| 智能体 | AgentType | ModelRole | 职责 |
|---|---|---|---|
| SupervisorAgent | `SUPERVISOR` | `SUPERVISOR` | 顶层编排：理解意图、分解任务、调度子智能体、综合结果 |
| PlannerRouterAgent | `PLANNER_ROUTER` | `PLANNER_ROUTER` | 拆解目标、选择协作模式、定义完成标准 |
| ResearchRetrievalAgent | `RESEARCH_RETRIEVAL` | `RESEARCH_RETRIEVAL` | 检索证据、收集信息 |
| DomainSpecialistAgent | `DOMAIN_SPECIALIST` | `DOMAIN_SPECIALIST` | 领域专业知识处理（抽象基类） |
| DocumentSpecialistAgent | `DOCUMENT_SPECIALIST` | `DOMAIN_SPECIALIST` | 文档/需求专家：需求分析、文档草拟、验收标准 |
| ToolActionExecutorAgent | `TOOL_ACTION_EXECUTOR` | `TOOL_ACTION_EXECUTOR` | 工具调用、动作执行 |
| CodeAgent | `CODE` | `CODE` | 编写、调试、运行代码，适用于数据分析、自动化脚本和复杂问题验证 |
| ChatAgent | `CHAT` | `CHAT` | 多模态文档分析助手：图片理解、文档提取、图表分析 |

### 2. 智能体体系

当前智能体体系使用：

* 模板方法模式
* 策略模式
* 工具提供者模式
* 工厂模式
* ReactAgent 模式（Spring AI Alibaba）

结构：

```text
Agent<R>                          # 顶层接口
  ├─ ToolAwareAgent<R>            # 支持工具调用的接口
  │    └─ StreamingAgent<T>       # 流式输出接口
  │
  └─ AbstractAgent<R>             # 抽象基类（模板方法）
       ├─ DomainSpecialistAgent   # 领域专家抽象基类
       │    └─ DocumentSpecialistAgent  # 文档/需求专家
       │
       └─ AbstractToolAgent<R>    # 支持工具调用的抽象基类
            ├─ ToolActionExecutorAgent  # 工具执行智能体
            ├─ ChatAgent               # 多模态对话智能体
            └─ SupervisorAgent         # Supervisor 编排智能体（流式）
```

### 3.1 AgentRegistry 动态发现机制

`AgentRegistry` 利用 Spring 的泛型集合注入机制，自动收集容器中所有 `Agent<String>` 类型的 Bean，实现"新增 Agent 只需加 @Component，无需改注册逻辑"的开闭原则。

与 `AgentFactory` 的关系：
- `AgentFactory` 采用硬编码 switch-case，是兼容性门面
- `AgentRegistry` 提供动态发现能力，主要服务于 Supervisor 编排层

### 3.2 Supervisor 子智能体工具化

Supervisor 通过 `SupervisorToolProvider` 获取工具列表，该提供者将 `AgentRegistry` 中注册的所有非 Supervisor 智能体包装为 `ToolCallback`，使得 Supervisor 的 ReactAgent 可以像调用普通工具一样调用子智能体。

```text
AgentRegistry → SupervisorToolProvider → AgentTool（适配器） → ToolCallback
```

### 3. 智能体扩展要求

新增智能体时，不要绕开现有体系。

**无工具调用的智能体**，继承：

```text
AbstractAgent<R>
```

**需要工具调用的智能体**，继承：

```text
AbstractToolAgent<R>
```

并通过：

```text
AgentToolProvider
```

注入工具能力。

不要在 Agent 内部直接 new 工具、new 模型、new 客户端。

错误示例：

```java
public class XxxAgent {
    private final OpenAiChatModel model = new OpenAiChatModel(...);
}
```

正确方向：

```java
@Component
public class XxxAgent extends AbstractToolAgent<String> {

    public XxxAgent(
        ModelResolver modelResolver,
        XxxPromptProvider promptProvider,
        FullToolProvider toolProvider
    ) {
        super(modelResolver, promptProvider, new SyncCallStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Xxx_agent";
    }

    @Override
    public String getDescription() {
        return "Xxx 智能体描述";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.XXX;
    }
}
```

### 4. ReactAgent 集成

本项目使用 Spring AI Alibaba Agent Framework 的 `ReactAgent` 作为底层执行引擎。

`AbstractAgent` 中的 `buildReactAgent()` 方法负责：

1. 通过 `ModelResolver` 获取 ChatModel
2. 通过 `AgentPromptProvider` 构建系统提示词
3. 通过 `AgentToolProvider` 获取工具列表
4. 构建 `ReactAgent` 实例并执行

---

## 七、推荐设计模式使用准则

AI 编程时应优先使用合适的设计模式，而不是堆 if/else。

### 1. 工厂模式

适用于：

* 创建 Agent
* 创建模型 Provider
* 创建工具
* 创建执行器
* 创建沙箱运行器

已有：

```text
AgentFactory        # 智能体工厂
AiModelRegistrar    # 模型注册器
ModelResolver       # 模型解析器
```

新增类型时，应优先注册到工厂，而不是在业务逻辑中直接判断类型后 new 对象。

---

### 2. 策略模式

适用于：

* 同步调用 / 流式调用
* 不同 Agent 执行策略
* 不同模型调用策略
* 不同沙箱执行策略
* 不同任务规划策略
* 不同观察结果处理策略

已有：

```text
ExecutionStrategy
SyncCallStrategy
StreamingStrategy
```

当出现下面结构时，应考虑改为策略模式：

```java
if (type == A) {
    ...
} else if (type == B) {
    ...
} else if (type == C) {
    ...
}
```

---

### 3. 模板方法模式

适用于：

* Agent 执行生命周期
* 任务执行生命周期
* 工具调用生命周期
* 沙箱执行生命周期

已有：

```text
AbstractAgent        # 定义 buildReactAgent() + execute() 流程
AbstractToolAgent    # 扩展工具支持
```

不要把通用执行流程复制到多个 Agent 中。

---

### 4. 装饰器模式

适用于：

* 模型故障转移
* 模型调用增强
* 日志增强
* Token 统计
* Retry
* Rate Limit
* 熔断

已有：

```text
FailoverChatModel   # primary + fallback 故障转移
```

新增模型增强能力时，优先考虑装饰器，而不是修改所有模型实现类。

---

### 5. 适配器模式

适用于：

* 第三方 AI 厂商接入
* MCP 工具接入
* Docker 客户端接入
* Redis 操作封装
* 外部系统 API 接入

已有：

```text
SandboxTool                    # 沙箱工具（@Tool 注解）
AiModelFactory 各厂商实现       # 模型厂商适配
```

外部依赖不要直接泄露到核心 Agent 层。

---

## 八、模型注册与故障转移

当前模型架构：

```text
AiProvidersProperties
  ↓
AiModelRegistrar
  ↓
ModelResolver
  ↓
FailoverChatModel
```

模型按角色组织（ModelRole）：

```text
ModelRole.SUPERVISOR           # Supervisor 编排
ModelRole.PLANNER_ROUTER       # 规划与路由
ModelRole.RESEARCH_RETRIEVAL   # 检索与证据
ModelRole.DOMAIN_SPECIALIST    # 领域专家
ModelRole.TOOL_ACTION_EXECUTOR # 工具执行
ModelRole.CODE                 # 代码执行
ModelRole.CHAT                 # 多模态对话
```

支持按智能体名覆盖模型。

### 模型相关开发要求

* 不要直接在 Agent 中创建模型
* 不要绕过 `ModelResolver`
* 不要直接修改 `ModelResolver` 内部 Map
* 新模型必须通过配置和初始化流程注册
* 新厂商优先走 `openai-compatible`
* 故障转移逻辑优先放在装饰器中

---

## 九、多厂商支持

`VendorEnums` 当前支持：

```text
gemini
openai
openai-compatible
dashscope
ollama
anthropic
deepseek
qianfan
```

大多数 OpenAI 兼容厂商，只需要在 `application.yml` 中新增 provider 配置：

```yaml
vendor: openai-compatible
```

无需修改 Java 代码。

只有在厂商协议不兼容时，才新增 `AiModelFactory` 实现类。

### 新增厂商步骤

1. 在 `VendorEnums` 中添加枚举值
2. 在 `models/` 下创建 `XxxModelFactory` 实现 `AiModelFactory`
3. 使用 `@Component` 注解，确保 `supports()` 方法匹配 vendor code
4. 在 `application.yml` 中配置 provider

---

## 十、配置要求

唯一配置文件：

```text
src/main/resources/application.yml
```

关键配置段：

| 配置段 | 用途 |
|---|---|
| `many-foot.ai.providers.*` | 多厂商 AI 模型配置 |
| `many-foot.ai.roles.*` | 角色绑定（primary + fallbacks） |
| `many-foot.ai.agents.*` | 按智能体名覆盖模型 |
| `many-foot.ai.default-provider` | 全局兜底 provider |
| `spring.ai.*` | Spring AI starter 配置 |
| `spring.data.redis.*` | Redis 连接 |
| `many-foot.sandbox.*` | Docker 沙箱设置 |

### 配置示例

```yaml
many-foot:
  ai:
    providers:
      qwen-max:
        vendor: dashscope
        model: qwen-max
        api-key: sk-xxx
        options:
          temperature: 0.3
      gpt-4o:
        vendor: openai
        model: gpt-4o
        api-key: ${OPENAI_KEY}
    
    roles:
      SUPERVISOR:
        primary: qwen-max
        fallbacks: [qwen-plus]
      PLANNER_ROUTER:
        primary: qwen-max
        fallbacks: [qwen-plus]
      RESEARCH_RETRIEVAL:
        primary: qwen-turbo
        fallbacks: [qwen-max]
      DOMAIN_SPECIALIST:
        primary: qwen-turbo
        fallbacks: [qwen-max]
      TOOL_ACTION_EXECUTOR:
        primary: qwen3-coder
        fallbacks: [qwen-max]
      CODE:
        primary: qwen3-coder
        fallbacks: [qwen-max]
      CHAT:
        primary: qwen-vl-max
        fallbacks: [qwen-max]
    
    default-provider: qwen-max
```

### 配置开发要求

* 新能力优先通过配置开启或关闭
* 涉及外部依赖的 Bean 必须考虑 `@ConditionalOnProperty`
* 不要硬编码 API Key、Redis 密码、Docker 地址
* 不要把环境相关配置写死在 Java 类中
* 配置类必须使用类型安全的 `@ConfigurationProperties`

生产部署中，API Key、Redis 密码、Docker Host 必须外部化配置。

---

## 十一、基础设施要求

| 依赖 | 用途 |
|---|---|
| Java 17+ | 运行时 |
| Maven | 构建 |
| Redis | 状态管理 |
| Docker | 沙箱代码执行 |

Docker 配置：

* 沙箱镜像定义：

```text
src/main/resources/Dockerfile
```

* 默认 Docker 主机：

```text
tcp://localhost:2375
```

* 当：

```yaml
many-foot.sandbox.enabled: true
```

时，必须能访问 Docker daemon。

---

## 十二、REST API

当前 Controller 层提供统一响应工具类：

```text
com.lh.manyfoot.controller.R
com.lh.manyfoot.controller.HttpStatus
```

### Controller 开发要求

Controller 中不要写复杂业务逻辑。

Controller 应只做：

* 参数接收
* 参数基础校验
* 调用应用服务 / 智能体工厂
* 返回统一响应
* SSE 协议适配

复杂逻辑应下沉到：

```text
agent
service
```

---

## 十三、添加新智能体步骤

新增智能体必须遵循以下流程：

1. 在 `agent/impl/` 创建新类
2. 继承：
   - 无工具需求 → `AbstractAgent<R>`
   - 有工具需求 → `AbstractToolAgent<R>`

3. 实现：

```text
getName()           # 智能体名称（唯一标识）
getDescription()    # 智能体描述
getModelRole()      # 模型角色
```

4. 如需工具，注入合适的：

```text
AgentToolProvider
```

例如：

```text
FullToolProvider       # 完整工具（本地 + MCP）
```

5. 在 `agent/prompt/` 创建对应的：

```text
AgentPromptProvider
```

6. 在 `AgentFactory` 中注册：

```text
# 1. 添加字段注入
private final XxxAgent xxxAgent;

# 2. 在 AgentType 枚举中添加
XXX,

# 3. 在 getAgent() 方法中添加 case
case XXX -> xxxAgent;
```

7. 如需新模型角色，在 `ModelRole` 中添加

8. 在 `application.yml` 的 `roles` 中配置模型绑定

9. 必要时补充单元测试

### AgentRegistry 自动发现

新增的智能体只需添加 `@Component` 注解，`AgentRegistry` 会自动发现并注册。Supervisor 编排层会自动将新智能体暴露为可调度的工具。

**注意**：如果新智能体需要被 Supervisor 调度，无需额外配置，AgentRegistry + SupervisorToolProvider 会自动处理。

---

## 十四、添加新工具步骤

新增工具时，应遵守：

1. 工具能力抽象优先放在 `agent.tool` 相关包中
2. 工具实例由 `AgentToolProvider` 提供
3. 工具不要直接依赖具体 Agent
4. 工具输入输出应定义清晰 DTO
5. 工具执行结果应可序列化、可记录、可追踪
6. 工具异常必须转换为 Agent 可理解的失败结果
7. 涉及外部服务的工具必须支持配置开关

工具不能反向调用智能体，避免循环依赖。

### MCP 工具集成

MCP 工具通过 `McpClientConfig` 配置，自动注册为 `ToolCallbackProvider`。

`FullToolProvider` 会自动收集所有 `ToolCallbackProvider` 提供的工具。

---

## 十五、沙箱代码执行要求

`agent.tool.sandbox.*` 负责 Docker 沙箱代码执行。

核心类：

```text
SandboxTool             # 沙箱工具（@Tool 注解，供 Agent 调用）
SandboxEngine           # 沙箱执行引擎
SandboxContainerManager # 容器管理器
DocumentParser          # 文档解析器（基于 Apache Tika）
ExecutionResultFormatter # 执行结果格式化器
```

开发要求：

* 沙箱执行逻辑不要写入 Agent 内部
* Agent 只能通过工具或服务调用沙箱能力
* 沙箱容器数量限制必须保留
* 容器复用、清理、回收策略应独立封装
* 不要把 Docker API 调用散落在多个模块中
* 沙箱执行结果必须包含 stdout、stderr、exitCode、执行状态等信息
* 对用户输入代码必须考虑超时、资源限制和安全隔离

默认沙箱容器最大数量为 10，超出时会触发空闲容器清理。

---

## 十六、领域对象说明

`agent.domain.*` 包含智能体协作的核心领域对象：

| 类 | 用途 |
|---|---|
| `PlanGraph` | 规划与路由 Agent 产出的执行计划图 |
| `PlanGraph.PlanStep` | 计划步骤 |
| `TaskSpec` | 任务规格说明 |
| `ActionCall` | 工具调用请求 |
| `ActionResult` | 工具调用结果 |
| `DomainDraft` | 领域专家产出的草稿 |
| `DomainSpecialistInput` | 领域专家输入 |
| `ResearchBrief` | 研究摘要 |
| `EvidencePack` | 证据包 |
| `TaskSlice` | 任务切片 |

`agent.context.*` 包含智能体执行上下文：

| 类 | 用途 |
|---|---|
| `AgentContext` | 智能体执行上下文（sessionId、query、attachments、attributes） |
| `AgentAttachment` | 附件元数据（sandboxPath、hostPath、filename、mimeType、image） |
| `SessionContextHolder` | 会话上下文持有者（ThreadLocal，支持父子智能体会话传递） |

这些对象用于智能体之间的数据传递，必须可序列化。

---

## 十七、测试现状与补充要求

当前测试较少：

* `ManyFootApplicationTests`

无：

* application-test.yml
* 集成测试配置
* 沙箱集成测试
* Redis 集成测试

新增核心逻辑时，优先补充单元测试。

尤其是以下内容应尽量测试：

* ModelResolver
* FailoverChatModel
* AgentFactory
* Agent 执行策略
* 工具 Provider
* 沙箱执行结果解析
* 各智能体的 plan/execute 逻辑

不要为了通过测试而弱化架构设计。

---

## 十八、Lombok 使用要求

项目使用 Lombok。

允许使用：

```text
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@Slf4j
@Data          # DTO 和领域对象可使用
@NoArgsConstructor
@AllArgsConstructor
```

但注意：

* DTO 可以使用 Lombok 简化
* 核心领域对象不要过度依赖 `@Data`
* 需要不可变对象时优先使用 `final` 字段和构造器注入

`maven-compiler-plugin` 已配置 annotation processor。

`spring-boot-maven-plugin` 会从最终 jar 中排除 Lombok。

---

## 十九、常见陷阱

AI 编码时必须避免：

* 直接修改 `ModelResolver` 内部存储
* 在 Agent 内直接 new 模型或工具
* 在 Agent 内部写复杂编排逻辑（应由 Supervisor 模式协调）
* 新增 Agent 后忘记注册到 `AgentFactory`
* 新增外部依赖 Bean 时不加条件装配
* 破坏现有 `Agent -> AbstractAgent -> AbstractToolAgent` 继承链
* 将沙箱执行逻辑散落在多个模块
* 使用硬编码 API Key、Redis 密码、Docker Host
* 复制粘贴已有 Agent 逻辑但不抽象公共能力
* 为了实现功能引入循环依赖
* 把异常直接抛给前端而不转换为统一响应

---

## 二十、AI 编程行为准则

当 AI 修改本项目代码时，必须遵守以下行为准则：

### 1. 先理解架构，再写代码

修改前先判断当前需求属于哪一层：

```text
API 层（controller）
智能体层（agent）
模型层（models）
工具层（agent.tool）/ 沙箱层（agent.tool.sandbox）
配置层（config）
服务层（service）
```

不要跨层实现。

---

### 2. 优先扩展，不要侵入式修改

优先通过：

* 新增接口实现
* 新增策略
* 新增 Provider
* 新增配置项
* 新增工厂注册
* 新增装饰器
* 新增智能体类型

来完成需求。

除非必要，不要大面积重写已有核心类。

---

### 3. 保持低耦合

禁止出现以下倾向：

* Agent 直接依赖 Controller
* Agent 直接依赖具体模型厂商
* Agent 直接操作 Docker 客户端
* Tool 反向调用 Agent
* Config 类写运行时逻辑

---

### 4. 代码要为后续扩展预留空间

当需求涉及类型扩展时，应优先设计为可插拔结构。

例如：

* 多模型厂商
* 多 Agent 类型
* 多工具类型
* 多沙箱执行器
* 多任务规划策略

不要让后续每新增一个类型都必须修改大量 if/else。

---

### 5. 保持代码优雅

代码应满足：

* 类名表达职责
* 方法名表达动作
* 单个方法不要过长
* 单个类不要承担多个职责
* 入参和返回值语义清晰
* 异常处理明确
* 日志信息可定位问题
* 公共逻辑抽象复用
* 业务逻辑和基础设施逻辑分离

---

### 6. 日志要求

核心流程应有必要日志：

* Agent 开始执行
* Agent 执行完成
* 模型选择结果
* fallback 触发
* 工具调用开始和结束
* 沙箱容器创建、复用、释放
* Supervisor 协作流程

日志不要输出：

* API Key
* Redis 密码
* 用户敏感信息
* 大段 Prompt 原文，除非显式需要调试

---

### 7. 异常处理要求

异常处理应分层：

* Controller 层返回统一响应
* Agent 层转换模型或工具失败结果
* Tool 层封装外部服务异常
* Sandbox 层封装执行异常

不要让底层异常直接穿透到前端。

---

### 8. 配置优先

涉及以下内容时，优先配置化：

* 模型厂商
* 模型名称
* API Base URL
* API Key
* 超时时间
* 沙箱是否启用
* Docker Host
* 最大容器数
* fallback 模型
* Agent 默认模型角色
* 工具是否启用

---

## 二十一、代码风格偏好

* 使用构造器注入，不优先使用字段注入
* 多写中文注释
* 面向接口编程
* 核心服务优先定义接口
* 配置对象使用 `@ConfigurationProperties`
* Bean 装配使用 Spring Boot 自动配置习惯
* 避免静态工具类泛滥
* 不滥用全局单例
* 不在业务代码中读取环境变量
* 不在核心逻辑中拼接复杂 YAML 配置

---

## 二十二、最终目标

ManyFoot 的代码应逐步演进为一个：

* 可插拔的多智能体协作框架
* 可扩展的模型适配平台
* 可复用的工具调用底座
* 可隔离的代码执行平台
* 可配置、可测试、可维护的企业级 AI Agent 框架

AI 在本项目中写代码时，应始终围绕这个目标进行设计和实现。
