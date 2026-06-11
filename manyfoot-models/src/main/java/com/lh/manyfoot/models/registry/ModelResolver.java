package com.lh.manyfoot.models.registry;

import com.lh.manyfoot.config.properties.AiProvidersProperties;
import com.lh.manyfoot.domain.ModelRole;
import com.lh.manyfoot.models.failover.FailoverChatModel;
import com.lh.manyfoot.models.failover.FailoverChatModel.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型解析器。
 * <p>
 * 按 provider id 管理三类 Spring AI 模型（Chat/Embedding/Image），同时维护
 * 角色（{@link ModelRole}）和 Agent 名到 provider id 的绑定表，并在查询时自动
 * 套上 {@link FailoverChatModel} 做 primary/fallback 降级。
 *
 * <p>注册由 {@link AiModelRegistrar} 在启动期完成，本类只负责查询与装配。
 */
@Component
@Slf4j
public class ModelResolver {

    /** 按 provider id 存放的 ChatModel 实例。 */
    private final Map<String, ChatModel> chatById = new ConcurrentHashMap<>();

    /** 按 provider id 存放的 EmbeddingModel 实例（可选）。 */
    private final Map<String, EmbeddingModel> embeddingById = new ConcurrentHashMap<>();

    /** 按 provider id 存放的 ImageModel 实例（可选）。 */
    private final Map<String, ImageModel> imageById = new ConcurrentHashMap<>();

    /** 按 provider id 存放 ChatModel 是否支持多模态输入。 */
    private final Map<String, Boolean> multimodalById = new ConcurrentHashMap<>();

    /** 角色 -> 绑定。 */
    private final Map<ModelRole, AiProvidersProperties.Binding> roleBindings =
        new EnumMap<>(ModelRole.class);

    /** Agent#getName() -> 绑定。 */
    private final Map<String, AiProvidersProperties.AgentBinding> agentBindings = new HashMap<>();

    /** 全局默认 provider id。 */
    private String defaultProviderId;

    // ==================== 注册期 API（供 AiModelRegistrar 使用） ====================

    public void registerChatModel(String id, ChatModel model) {
        require(id, "provider id");
        chatById.put(id, model);
    }

    public void registerChatModel(String id, ChatModel model, boolean multimodal) {
        registerChatModel(id, model);
        multimodalById.put(id, multimodal);
    }

    public void registerEmbeddingModel(String id, EmbeddingModel model) {
        require(id, "provider id");
        embeddingById.put(id, model);
    }

    public void registerImageModel(String id, ImageModel model) {
        require(id, "provider id");
        imageById.put(id, model);
    }

    public void bindRoles(Map<ModelRole, AiProvidersProperties.Binding> bindings) {
        roleBindings.clear();
        if (bindings != null) {
            bindings.forEach((k, v) -> {
                if (k != null && v != null && v.getPrimary() != null) {
                    roleBindings.put(k, v);
                }
            });
        }
    }

    public void bindAgents(Map<String, AiProvidersProperties.AgentBinding> bindings) {
        agentBindings.clear();
        if (bindings != null) {
            agentBindings.putAll(bindings);
        }
    }

    public void setDefaultProviderId(String defaultProviderId) {
        this.defaultProviderId = defaultProviderId;
    }

    public void clear() {
        chatById.clear();
        embeddingById.clear();
        imageById.clear();
        multimodalById.clear();
        roleBindings.clear();
        agentBindings.clear();
        defaultProviderId = null;
    }

    // ==================== 查询 API（供业务消费） ====================

    /**
     * 按 provider id 取 ChatModel（不含 failover）。
     */
    public ChatModel getById(String id) {
        ChatModel m = chatById.get(id);
        if (m == null) {
            throw new IllegalStateException("未找到 provider id=[" + id + "] 对应的 ChatModel");
        }
        return m;
    }

    public Optional<ChatModel> findById(String id) {
        return Optional.ofNullable(chatById.get(id));
    }

    /**
     * 判断指定 provider 的 ChatModel 是否声明支持多模态输入。
     */
    public boolean supportsMultimodal(String id) {
        return Boolean.TRUE.equals(multimodalById.get(id));
    }

    /**
     * 判断角色绑定的主 ChatModel 是否声明支持多模态输入。
     */
    public boolean supportsMultimodalForRole(ModelRole role) {
        String providerId = resolvePrimaryForRole(role);
        return providerId != null && supportsMultimodal(providerId);
    }

    /**
     * 判断 Agent 实际命中的主 ChatModel 是否声明支持多模态输入。
     */
    public boolean supportsMultimodalForAgent(String agentName, ModelRole fallbackRole) {
        AiProvidersProperties.AgentBinding ab = agentBindings.get(agentName);
        if (ab != null && ab.getPrimary() != null) {
            return supportsMultimodal(ab.getPrimary());
        }
        ModelRole role = (ab != null && ab.getRole() != null) ? ab.getRole() : fallbackRole;
        return supportsMultimodalForRole(role);
    }

    /**
     * 按角色取 ChatModel，自动套 primary + fallbacks。
     */
    public ChatModel forRole(ModelRole role) {
        AiProvidersProperties.Binding b = roleBindings.get(role);
        if (b == null || b.getPrimary() == null) {
            if (defaultProviderId != null) {
                log.debug("角色 [{}] 未显式绑定，回落到默认 provider=[{}]", role, defaultProviderId);
                return getById(defaultProviderId);
            }
            throw new IllegalStateException("角色 [" + role + "] 未绑定任何 provider，也没有配置 default");
        }
        return buildFailover(b.getPrimary(), b.getFallbacks(), "role=" + role);
    }

    /**
     * 按 Agent 名取 ChatModel，优先命中 {@code agents.*} 覆盖，否则回退到 role 绑定。
     *
     * @param agentName       Agent#getName()
     * @param fallbackRole    Agent 声明的默认角色（agents 覆盖不存在时用此角色查 roles）
     */
    public ChatModel forAgent(String agentName, ModelRole fallbackRole) {
        AiProvidersProperties.AgentBinding ab = agentBindings.get(agentName);
        if (ab != null && ab.getPrimary() != null) {
            return buildFailover(ab.getPrimary(), ab.getFallbacks(), "agent=" + agentName);
        }
        ModelRole r = (ab != null && ab.getRole() != null) ? ab.getRole() : fallbackRole;
        return forRole(r);
    }

    public EmbeddingModel embeddingById(String id) {
        EmbeddingModel m = embeddingById.get(id);
        if (m == null) {
            throw new IllegalStateException("未找到 provider id=[" + id + "] 对应的 EmbeddingModel");
        }
        return m;
    }

    public Optional<EmbeddingModel> findEmbeddingById(String id) {
        return Optional.ofNullable(embeddingById.get(id));
    }

    public ImageModel imageById(String id) {
        ImageModel m = imageById.get(id);
        if (m == null) {
            throw new IllegalStateException("未找到 provider id=[" + id + "] 对应的 ImageModel");
        }
        return m;
    }

    public Optional<ImageModel> findImageById(String id) {
        return Optional.ofNullable(imageById.get(id));
    }

    // ==================== 内省（日志/监控用） ====================

    public Set<String> getChatProviderIds() {
        return Collections.unmodifiableSet(chatById.keySet());
    }

    public Set<String> getEmbeddingProviderIds() {
        return Collections.unmodifiableSet(embeddingById.keySet());
    }

    public Set<String> getImageProviderIds() {
        return Collections.unmodifiableSet(imageById.keySet());
    }

    public int getChatModelCount() {
        return chatById.size();
    }

    public boolean containsChat(String id) {
        return chatById.containsKey(id);
    }

    // ==================== 内部 ====================

    private ChatModel buildFailover(String primaryId, List<String> fallbackIds, String ctx) {
        ChatModel primary = chatById.get(primaryId);
        if (primary == null) {
            throw new IllegalStateException("[" + ctx + "] 指向的 primary provider id=["
                + primaryId + "] 不存在，请检查 many-foot.ai.providers 配置");
        }
        List<Named> fallbacks = new ArrayList<>();
        if (fallbackIds != null) {
            for (String id : fallbackIds) {
                ChatModel m = chatById.get(id);
                if (m == null) {
                    log.warn("[{}] 的 fallback provider id=[{}] 不存在，跳过", ctx, id);
                    continue;
                }
                fallbacks.add(new Named(id, m));
            }
        }
        return FailoverChatModel.of(new Named(primaryId, primary), fallbacks);
    }

    private String resolvePrimaryForRole(ModelRole role) {
        AiProvidersProperties.Binding binding = roleBindings.get(role);
        if (binding != null && binding.getPrimary() != null) {
            return binding.getPrimary();
        }
        return defaultProviderId;
    }

    private static void require(String v, String label) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(label + " 不能为空");
        }
    }
}
