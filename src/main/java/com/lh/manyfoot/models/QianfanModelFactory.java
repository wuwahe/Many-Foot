package com.lh.manyfoot.models;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.core.StreamIterator;
import com.baidubce.qianfan.core.builder.ChatBuilder;
import com.baidubce.qianfan.model.chat.Message;
import com.lh.manyfoot.config.properties.VendorEnums;
import com.lh.manyfoot.domain.AiModelConfig;
import com.lh.manyfoot.models.support.ChatOptionsBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 百度千帆厂商工厂。
 * <p>
 * 百度 qianfan 原生 SDK 非 Spring AI 原生支持，这里用一个薄包装把 {@code ChatBuilder}
 * 适配成 {@link ChatModel}，从而统一纳入 ModelResolver 管理。
 */
@Component
@Slf4j
public class QianfanModelFactory implements AiModelFactory {

    @Override
    public boolean supports(String providerCode) {
        return VendorEnums.QIANFAN.getVendor().equalsIgnoreCase(providerCode);
    }

    @Override
    public ChatModel createChatModel(AiModelConfig config) {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("qianfan provider [" + config.getId() + "] 必须提供 api-key");
        }
        // 千帆的 apiKey 既可以是 IAM 方式的 AK:SK，也可以是单独的 token。
        // 这里约定以英文冒号拆分: "AK:SK"；否则按单 token 传入。
        Qianfan client = apiKey.contains(":")
            ? new Qianfan(apiKey.substring(0, apiKey.indexOf(':')), apiKey.substring(apiKey.indexOf(':') + 1))
            : new Qianfan(apiKey);

        return new QianfanChatModelAdapter(client, config);
    }

    @Override
    public EmbeddingModel createEmbeddingModel(AiModelConfig config) {
        log.info("Qianfan Embedding 暂未接入，跳过 provider=[{}]", config.getId());
        return null;
    }

    @Override
    public ImageModel createImageModel(AiModelConfig config) {
        log.info("Qianfan 图像生成暂未接入，跳过 provider=[{}]", config.getId());
        return null;
    }

    /**
     * 把千帆 SDK 薄包装成 Spring AI {@link ChatModel} 的最小实现。
     * 只覆盖 text 对话 + 采样参数，工具调用等高级特性按需再扩。
     */
    private static class QianfanChatModelAdapter implements ChatModel {

        private final Qianfan client;
        private final String modelName;
        private final Double temperature;
        private final Double topP;
        private final Double penaltyScore;
        private final Integer maxOutputTokens;
        private final List<String> stop;

        QianfanChatModelAdapter(Qianfan client, AiModelConfig config) {
            this.client = client;
            this.modelName = config.getModelName();
            this.temperature = ChatOptionsBinder.asDouble(config.getOptions(), "temperature");
            this.topP = ChatOptionsBinder.asDouble(config.getOptions(), "top-p", "topP");
            this.penaltyScore = ChatOptionsBinder.asDouble(
                config.getOptions(), "penalty-score", "penaltyScore", "presence-penalty");
            this.maxOutputTokens = ChatOptionsBinder.asInteger(
                config.getOptions(), "max-tokens", "max-output-tokens", "maxOutputTokens");
            Object rawStop = ChatOptionsBinder.findValue(config.getOptions(), "stop");
            if (rawStop instanceof List<?> l) {
                List<String> s = new ArrayList<>();
                for (Object o : l) {
                    if (o != null) {
                        s.add(o.toString());
                    }
                }
                this.stop = s.isEmpty() ? null : s;
            } else if (rawStop != null) {
                this.stop = List.of(rawStop.toString());
            } else {
                this.stop = null;
            }
        }

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(Prompt prompt) {
            ChatBuilder builder = prepare(prompt);
            com.baidubce.qianfan.model.chat.ChatResponse resp = builder.execute();
            String text = resp.getResult() == null ? "" : resp.getResult();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<org.springframework.ai.chat.model.ChatResponse> stream(Prompt prompt) {
            return Flux.create(sink -> {
                try (StreamIterator<com.baidubce.qianfan.model.chat.ChatResponse> it =
                         prepare(prompt).executeStream()) {
                    while (it.hasNext()) {
                        com.baidubce.qianfan.model.chat.ChatResponse chunk = it.next();
                        String text = chunk.getResult() == null ? "" : chunk.getResult();
                        sink.next(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
                    }
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        }

        private ChatBuilder prepare(Prompt prompt) {
            ChatBuilder builder = client.chatCompletion();
            builder.model(modelName);
            List<Message> messages = new ArrayList<>();
            String system = null;
            for (org.springframework.ai.chat.messages.Message m : prompt.getInstructions()) {
                MessageType type = m.getMessageType();
                if (type == MessageType.SYSTEM) {
                    system = m.getText();
                    continue;
                }
                Message qm = new Message();
                qm.setRole(type == MessageType.ASSISTANT ? "assistant" : "user");
                qm.setContent(m.getText() == null ? "" : m.getText());
                messages.add(qm);
            }
            builder.messages(messages);
            if (system != null && !system.isBlank()) {
                builder.system(system);
            }
            if (temperature != null) {
                builder.temperature(temperature);
            }
            if (topP != null) {
                builder.topP(topP);
            }
            if (penaltyScore != null) {
                builder.penaltyScore(penaltyScore);
            }
            if (maxOutputTokens != null) {
                builder.maxOutputTokens(maxOutputTokens);
            }
            if (stop != null) {
                builder.stop(stop);
            }
            return builder;
        }
    }
}
