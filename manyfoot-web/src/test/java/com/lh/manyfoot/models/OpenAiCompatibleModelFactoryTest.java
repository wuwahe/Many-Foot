package com.lh.manyfoot.models;

import com.lh.manyfoot.domain.AiModelConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OpenAiCompatibleModelFactoryTest {

    private final OpenAiCompatibleModelFactory factory = new OpenAiCompatibleModelFactory();

    @Test
    void createChatModel_shouldPropagateExtraBodyOptions() {
        AiModelConfig config = new AiModelConfig();
        config.setId("mimo");
        config.setModelName("mimo-v2.5-pro");
        config.setApiKey("test-key");
        config.setBaseUrl("https://token-plan-cn.xiaomimimo.com/v1");
        config.setOptions(Map.of(
                "extra-body", Map.of(
                        "enable_thinking", false,
                        "chat_template_kwargs", Map.of(
                                "enable_thinking", false,
                                "thinking", false
                        )
                )
        ));

        ChatModel model = factory.createChatModel(config);
        OpenAiChatOptions options = assertInstanceOf(OpenAiChatOptions.class, model.getDefaultOptions());

        assertEquals(false, options.getExtraBody().get("enable_thinking"));
        assertEquals(
                Map.of("enable_thinking", false, "thinking", false),
                options.getExtraBody().get("chat_template_kwargs")
        );
    }
}
