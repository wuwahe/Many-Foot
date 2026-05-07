package com.lh.manyfoot.agent.supervisor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lh.manyfoot.agent.context.AgentAttachment;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.context.SessionContextHolder;
import com.lh.manyfoot.agent.core.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 智能体工具适配器
 * <p>
 * 将一个 {@link Agent} 实例包装为 Spring AI 的 {@link ToolCallback}，
 * 使得 Supervisor 可以将子智能体当作普通工具来调用。
 * <p>
 * <h3>适配器边界说明</h3>
 * <ul>
 *   <li>本类仅负责"协议转换"：将 LLM 的工具调用请求转换为 Agent.execute() 调用</li>
 *   <li>不包含任何业务路由逻辑，不决定何时调用哪个智能体</li>
 *   <li>不创建 ChatModel、ReactAgent、工具或 Spring Bean</li>
 * </ul>
 * <p>
 * <h3>LLM 工具调用输入格式</h3>
 * LLM 调用工具时，会发送 JSON 字符串作为输入。对于 AgentTool，
 * 期望的输入格式为：
 * <pre>
 * {
 *   "input": "请帮我分析这段代码的性能问题..."
 * }
 * </pre>
 * 其中 "input" 字段包含要传递给子智能体的任务描述。
 * <p>
 * <h3>状态/会话隔离</h3>
 * 每次工具调用都会创建一个独立的子 {@link AgentContext}，具有：
 * <ul>
 *   <li>独立的 sessionId（格式：{agentName}_{uuid}，便于追踪）</li>
 *   <li>独立的 attributes（不与父上下文共享状态）</li>
 *   <li>父级信息记录在 attributes 中，便于日志追踪</li>
 * </ul>
 *
 * @author airx
 * @see ToolCallback
 * @see Agent
 */
@Slf4j
public class AgentTool implements ToolCallback {

    /**
     * JSON 解析器（静态复用，避免在热路径上重复创建）
     * ObjectMapper 是线程安全的，可以安全地在多线程环境中共享
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * LLM 输入中用于提取任务描述的 JSON 字段名
     */
    private static final String INPUT_FIELD = "input";

    /**
     * 被包装的智能体实例
     */
    private final Agent<String> agent;

    /**
     * 工具定义（包含名称、描述、输入 Schema）
     * 在构造时一次性生成，后续只读访问
     */
    private final ToolDefinition toolDefinition;

    /**
     * 私有构造器，通过静态工厂方法 {@link #of(Agent)} 创建实例
     *
     * @param agent 要包装的智能体实例
     */
    private AgentTool(Agent<String> agent) {
        this.agent = agent;
        this.toolDefinition = buildToolDefinition(agent);
    }

    /**
     * 静态工厂方法：将 Agent 包装为 ToolCallback
     * <p>
     * 使用示例：
     * <pre>
     * Agent&lt;String&gt; researchAgent = ...;
     * ToolCallback tool = AgentTool.of(researchAgent);
     * // tool 可以直接传给 ReactAgent.builder().tools(...)
     * </pre>
     *
     * @param agent 要包装的智能体实例
     * @return 包装后的 ToolCallback 实例
     */
    public static AgentTool of(Agent<String> agent) {
        return new AgentTool(agent);
    }

    /**
     * 获取工具定义
     * <p>
     * 返回的 ToolDefinition 包含：
     * <ul>
     *   <li>name: 来自 agent.getName()，如 "Research_Retrieval_agent"</li>
     *   <li>description: 来自 agent.getDescription()，描述智能体能力</li>
     *   <li>inputSchema: 标准 JSON Schema，定义 {"input": "string"} 结构</li>
     * </ul>
     *
     * @return 工具定义
     */
    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    /**
     * 执行工具调用
     * <p>
     * 执行流程：
     * <ol>
     *   <li>解析 LLM 传入的 JSON 字符串，提取 "input" 字段</li>
     *   <li>构建隔离的子 AgentContext（独立 sessionId，记录父级信息）</li>
     *   <li>调用 agent.execute(subContext) 执行子智能体</li>
     *   <li>返回执行结果字符串</li>
     * </ol>
     * <p>
     * <h4>安全回退策略</h4>
     * 如果输入不是合法 JSON，或 JSON 中缺少 "input" 字段，
     * 则将原始 toolInput 整体作为任务描述传递给子智能体。
     * 这确保了即使 LLM 输出格式异常，工具调用也不会失败。
     *
     * @param toolInput LLM 传入的工具调用输入（通常是 JSON 字符串）
     * @return 子智能体的执行结果
     */
    @Override
    public String call(String toolInput) {
        String taskInput = extractInput(toolInput);
        String childSessionId = buildChildSessionId();

        log.info("AgentTool 开始调用子智能体: agent={}, childSessionId={}, inputLength={}",
                agent.getName(), childSessionId, taskInput.length());

        try {
            AgentContext parentContext = SessionContextHolder.getAgentContext();

            // 构建隔离的子上下文；附件元数据会复制给子 Agent，便于共享沙箱文件。
            AgentContext subContext = AgentContext.builder()
                    .sessionId(childSessionId)
                    .query(taskInput)
                    .attachments(copyAttachments(parentContext))
                    .attributes(copyAttributes(parentContext))
                    .build();

            // 记录父级追踪信息（便于日志分析和问题定位）
            subContext.setAttribute("parent.toolName", agent.getName());
            subContext.setAttribute("parent.callType", "AgentTool");
            subContext.setAttribute("parent.originalInput", truncate(toolInput, 200));

            // 调用期间切换线程上下文，保证子 Agent 再次调度时继续继承附件元数据。
            SessionContextHolder.setAgentContext(subContext);
            String result;
            try {
                result = agent.execute(subContext);
            } finally {
                SessionContextHolder.setAgentContext(parentContext);
            }

            log.info("AgentTool 子智能体执行完成: agent={}, resultLength={}",
                    agent.getName(), result != null ? result.length() : 0);

            return result != null ? result : "";

        } catch (Exception e) {
            log.error("AgentTool 子智能体执行失败: agent={}, error={}", agent.getName(), e.getMessage(), e);
            // 返回错误信息而不是抛出异常，让 Supervisor 可以处理失败情况
            return "子智能体执行失败: " + e.getMessage();
        }
    }

    // ========== 内部方法 ==========

    /**
     * 构建工具定义
     * <p>
     * 生成的 inputSchema 遵循 Spring AI Alibaba AgentTool 的包装模式：
     * 将原始输入包装在 "input" 参数中，便于 LLM 理解如何调用。
     *
     * @param agent 智能体实例
     * @return 工具定义
     */
    private ToolDefinition buildToolDefinition(Agent<String> agent) {
        // 输入 Schema：定义 LLM 调用此工具时应传入的参数格式
        // 使用简单的 string 类型，LLM 只需传入任务描述文本
        String inputSchema = """
                {
                  "type": "object",
                  "properties": {
                    "input": {
                      "type": "string",
                      "description": "要传递给智能体的任务描述或问题"
                    }
                  },
                  "required": ["input"]
                }
                """;

        return ToolDefinition.builder()
                .name(agent.getName())
                .description(agent.getDescription())
                .inputSchema(inputSchema)
                .build();
    }

    /**
     * 从 LLM 输入中提取任务描述
     * <p>
     * 解析策略：
     * <ol>
     *   <li>尝试将输入解析为 JSON</li>
     *   <li>如果解析成功且包含 "input" 字段，返回该字段的值</li>
     *   <li>如果解析成功但缺少 "input" 字段，回退到原始输入</li>
     *   <li>如果解析失败（非 JSON），回退到原始输入</li>
     * </ol>
     *
     * @param toolInput LLM 传入的原始输入
     * @return 提取后的任务描述
     */
    private String extractInput(String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            log.warn("AgentTool 收到空输入，使用空字符串作为任务描述");
            return "";
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(toolInput);

            // 检查是否存在 "input" 字段
            JsonNode inputNode = rootNode.get(INPUT_FIELD);
            if (inputNode != null && !inputNode.isNull()) {
                String extracted = inputNode.asText();
                log.debug("AgentTool 成功从 JSON 中提取 input 字段: length={}", extracted.length());
                return extracted;
            }

            // JSON 中没有 "input" 字段，回退到原始输入
            log.warn("AgentTool JSON 输入中缺少 '{}' 字段，回退到原始输入", INPUT_FIELD);
            return toolInput;

        } catch (Exception e) {
            // 非 JSON 输入，回退到原始输入
            log.warn("AgentTool 输入不是合法 JSON，回退到原始输入: error={}", e.getMessage());
            return toolInput;
        }
    }

    private java.util.List<AgentAttachment> copyAttachments(AgentContext parentContext) {
        if (parentContext == null || parentContext.getAttachments() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(parentContext.getAttachments());
    }

    private java.util.Map<String, Object> copyAttributes(AgentContext parentContext) {
        if (parentContext == null || parentContext.getAttributes() == null) {
            return new HashMap<>();
        }
        return new HashMap<>(parentContext.getAttributes());
    }

    /**
     * 构建子会话 ID
     * <p>
     * 优先从 SessionContextHolder 获取父会话ID，确保父子代理会话ID一致。
     * 如果父会话ID不存在，则生成新的会话ID（格式：{agentName}_{uuid}）。
     * <p>
     * 这种设计确保：
     * <ul>
     *   <li>父子代理会话ID一致性（便于日志追踪和沙箱共享）</li>
     *   <li>向后兼容性（在没有父会话ID时仍能正常工作）</li>
     *   <li>可追溯性（包含智能体名称，便于日志分析）</li>
     * </ul>
     *
     * @return 子会话 ID
     */
    private String buildChildSessionId() {
        String parentSessionId = SessionContextHolder.getSessionId();
        if (parentSessionId != null) {
            log.debug("子代理继承父会话ID: parentSessionId={}", parentSessionId);
            return parentSessionId;
        }
        // 没有父会话ID时，生成新的会话ID（向后兼容）
        log.debug("未找到父会话ID，生成新的子会话ID");
        return agent.getName() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 截断字符串（用于日志输出，避免过长）
     *
     * @param str   原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 获取被包装的智能体实例
     * <p>
     * 仅供调试和测试使用，生产代码不应直接访问
     *
     * @return 智能体实例
     */
    public Agent<String> getAgent() {
        return agent;
    }

    @Override
    public String toString() {
        return "AgentTool{" +
                "agentName='" + agent.getName() + '\'' +
                ", toolName='" + toolDefinition.name() + '\'' +
                '}';
    }
}
