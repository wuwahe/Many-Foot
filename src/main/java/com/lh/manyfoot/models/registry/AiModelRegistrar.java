package com.lh.manyfoot.models.registry;

import com.lh.manyfoot.config.properties.AiProvidersProperties;
import com.lh.manyfoot.config.properties.AiProvidersProperties.AgentBinding;
import com.lh.manyfoot.config.properties.AiProvidersProperties.Binding;
import com.lh.manyfoot.config.properties.AiProvidersProperties.ProviderDef;
import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.AiModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 模型注册器。
 * <p>
 * 遍历 {@link AiProvidersProperties#getProviders()}，为每个命名 provider 匹配
 * {@link AiModelFactory}（按 {@code vendor} 字段）创建 Chat/Embedding/Image 三类
 * 模型，写入 {@link ModelResolver}；最后把 {@code roles} / {@code agents} 绑定表灌入
 * resolver，并校验所有引用到的 provider id 都存在。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiModelRegistrar {

    private final List<AiModelFactory> modelFactories;
    private final ModelResolver modelResolver;
    private final AiProvidersProperties properties;

    /**
     * 启动期入口。幂等：重复调用会先清空 resolver。
     */
    public void registerAllModels() {
        modelResolver.clear();

        Map<String, ProviderDef> providers = properties.getProviders();
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException(
                "未配置任何 AI provider，请在 many-foot.ai.providers 下至少声明一个");
        }

        log.info("发现 {} 个 provider 声明，开始注册...", providers.size());

        int chatCount = 0, embedCount = 0, imageCount = 0;
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, ProviderDef> e : providers.entrySet()) {
            String id = e.getKey();
            ProviderDef def = e.getValue();
            if (def == null || def.getVendor() == null || def.getVendor().isBlank()) {
                throw new IllegalStateException("provider [" + id + "] 缺少 vendor 字段");
            }
            if (def.getModel() == null || def.getModel().isBlank()) {
                throw new IllegalStateException("provider [" + id + "] 缺少 model 字段");
            }

            VendorEnums vendor = VendorEnums.fromCode(def.getVendor());
            AiModelFactory factory = findFactory(vendor);
            AiModelConfig cfg = toConfig(id, def, vendor);

            ChatModel chat = factory.createChatModel(cfg);
            if (chat == null) {
                throw new IllegalStateException("厂商 [" + vendor + "] 的工厂未返回 ChatModel，provider id=" + id);
            }
            modelResolver.registerChatModel(id, chat, def.isMultimodal());
            chatCount++;

            EmbeddingModel embedding = safe(() -> factory.createEmbeddingModel(cfg), "embedding", id);
            if (embedding != null) {
                modelResolver.registerEmbeddingModel(id, embedding);
                embedCount++;
            }
            ImageModel image = safe(() -> factory.createImageModel(cfg), "image", id);
            if (image != null) {
                modelResolver.registerImageModel(id, image);
                imageCount++;
            }

            summary.add(String.format("  - id=%s, vendor=%s, model=%s, chat=yes, embedding=%s, image=%s, multimodal=%s",
                id, vendor.getVendor(), def.getModel(),
                embedding != null ? "yes" : "no",
                image != null ? "yes" : "no",
                def.isMultimodal() ? "yes" : "no"));
        }

        modelResolver.bindRoles(properties.getRoles());
        modelResolver.bindAgents(properties.getAgents());
        modelResolver.setDefaultProviderId(properties.getDefaultProvider());

        validateBindings();

        log.info("AI 模型注册完成: chat={}, embedding={}, image={}\n{}",
            chatCount, embedCount, imageCount, String.join("\n", summary));
    }

    private void validateBindings() {
        if (properties.getDefaultProvider() != null
            && !modelResolver.containsChat(properties.getDefaultProvider())) {
            throw new IllegalStateException(
                "many-foot.ai.default=" + properties.getDefaultProvider()
                    + " 未在 providers 中声明");
        }
        if (properties.getRoles() != null) {
            properties.getRoles().forEach((role, binding) -> {
                if (binding == null || binding.getPrimary() == null) {
                    return;
                }
                checkProviderExists(binding.getPrimary(), "roles." + role + ".primary");
                if (binding.getFallbacks() != null) {
                    for (String id : binding.getFallbacks()) {
                        checkProviderExists(id, "roles." + role + ".fallbacks");
                    }
                }
            });
        }
        if (properties.getAgents() != null) {
            properties.getAgents().forEach((name, ab) -> {
                if (ab == null) {
                    return;
                }
                if (ab.getPrimary() != null) {
                    checkProviderExists(ab.getPrimary(), "agents." + name + ".primary");
                }
                if (ab.getFallbacks() != null) {
                    for (String id : ab.getFallbacks()) {
                        checkProviderExists(id, "agents." + name + ".fallbacks");
                    }
                }
            });
        }
    }

    private void checkProviderExists(String id, String location) {
        if (!modelResolver.containsChat(id)) {
            throw new IllegalStateException(location + " 引用了未声明的 provider id=[" + id + "]");
        }
    }

    private AiModelFactory findFactory(VendorEnums vendor) {
        return modelFactories.stream()
            .filter(f -> f.supports(vendor.getVendor()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "未找到厂商 [" + vendor + "] 对应的 AiModelFactory 实现。"
                    + "若为 OpenAI 兼容协议请使用 vendor=openai-compatible + base-url 配置"));
    }

    private AiModelConfig toConfig(String id, ProviderDef def, VendorEnums vendor) {
        AiModelConfig cfg = new AiModelConfig();
        cfg.setId(id);
        cfg.setName(id);
        cfg.setProviderCode(vendor.getVendor());
        cfg.setModelName(def.getModel());
        cfg.setApiKey(def.getApiKey());
        cfg.setBaseUrl(def.getBaseUrl());
        cfg.setOptions(def.getOptions());
        cfg.setTimeoutMs(def.getTimeoutMs());
        cfg.setHeaders(def.getHeaders());
        cfg.setModelType("CHAT");
        return cfg;
    }

    private <T> T safe(java.util.function.Supplier<T> supplier, String kind, String id) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("provider [{}] 创建 {} 模型失败，跳过: {}", id, kind, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static List<String> primaryIds(Binding b) {
        return b == null || b.getPrimary() == null ? List.of() : List.of(b.getPrimary());
    }

    @SuppressWarnings("unused")
    private static List<String> primaryIds(AgentBinding b) {
        return b == null || b.getPrimary() == null ? List.of() : List.of(b.getPrimary());
    }
}
