package com.lh.manyfoot.agent.impl;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import com.lh.manyfoot.orchestrator.domain.TaskComplexity;
import com.lh.manyfoot.orchestrator.prompts.ManusPrompts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

/**
 * 复杂度评估器
 * 独立的复杂度评估组件，可被多个智能体复用
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplexityAssessor {

    private final ModelResolver modelResolver;

    /**
     * 评估任务复杂度
     *
     * @param query 用户任务
     * @return 任务复杂度
     */
    public TaskComplexity assess(String query) {
        ChatModel chatModel = modelResolver.forRole(ModelRole.ANALYZE);
        String prompt = ManusPrompts.buildComplexityAssessPrompt(query);

        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String result = response.getResult().getOutput().getText();

            if (StrUtil.isBlank(result)) {
                return TaskComplexity.MEDIUM; // 默认中等复杂度
            }
            String trimmed = result.trim().toUpperCase();
            // 处理可能包含额外字符的情况
            return switch (trimmed) {
                case "SIMPLE" -> TaskComplexity.SIMPLE;
                case "COMPLEX" -> TaskComplexity.COMPLEX;
                case "MEDIUM" -> TaskComplexity.MEDIUM;
                default -> throw new IllegalAccessException("query: " + query + "result: " + result); // 异常
            };
        } catch (Exception e) {
            log.error("评估任务复杂度失败，使用默认值: {}", e.getMessage());
            return TaskComplexity.MEDIUM;
        }
    }
}
