package com.lh.manyfoot.config.properties;

import com.lh.manyfoot.domain.ModelRole;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多厂商 AI 模型配置根对象。
 *
 * <pre>
 * many-foot:
 *   ai:
 *     providers:
 *       qwen-max:
 *         vendor: dashscope
 *         model: qwen-max
 *         api-key: ...
 *         base-url: ...
 *         options: { temperature: 0.2, max-tokens: 4096 }
 *         timeout-ms: 60000
 *         headers: { X-Custom: ... }
 *     roles:
 *       PLANNER_ROUTER:
 *         primary: gpt-4o
 *         fallbacks: [qwen-max, kimi]
 *     agents:
 *       Tool_Action_Executor_agent:
 *         role: TOOL_ACTION_EXECUTOR
 *         primary: claude-sonnet
 *         fallbacks: [qwen3-coder]
 *     default: qwen-max
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "many-foot.ai")
public class AiProvidersProperties {

    /**
     * 命名 provider 字典。key 即 provider id，被角色/Agent 绑定和 getById 使用。
     */
    private Map<String, ProviderDef> providers = new HashMap<>();

    /**
     * 角色级绑定：Supervisor 专业智能体角色 -> 主备 provider。
     */
    private Map<ModelRole, Binding> roles = new HashMap<>();

    /**
     * Agent 级覆盖：以 Agent#getName() 为 key，优先级高于 roles。
     */
    private Map<String, AgentBinding> agents = new HashMap<>();

    /**
     * 默认 provider id（未指定 role/agent 时走这里）。
     */
    private String defaultProvider;

    /** 单个 provider 定义。 */
    @Data
    public static class ProviderDef {
        /** 厂商 code，对应 {@link VendorEnums}。 */
        private String vendor;
        /** 模型名称（如 gpt-4o）。 */
        private String model;
        /** API 密钥（可为空，如 Ollama）。 */
        private String apiKey;
        /** Base URL（可选）。 */
        private String baseUrl;
        /** 采样等参数（temperature 等）。 */
        private Map<String, Object> options = new HashMap<>();
        /** 请求超时（毫秒）。 */
        private Integer timeoutMs;
        /** 附加 HTTP Header。 */
        private Map<String, String> headers = new HashMap<>();
        /** 是否支持在 Chat 消息中直接接收图片等多模态输入。 */
        private boolean multimodal;
    }

    /** 角色绑定：primary + fallbacks。 */
    @Data
    public static class Binding {
        private String primary;
        private List<String> fallbacks = Collections.emptyList();
    }

    /** Agent 绑定：允许指定 role，也允许直接指 primary + fallbacks。 */
    @Data
    public static class AgentBinding {
        /** 绑定到的角色，用于 Resolver 记日志 & 没配 primary 时回退到 roles。 */
        private ModelRole role;
        private String primary;
        private List<String> fallbacks = Collections.emptyList();
    }
}
