package com.lh.manyfoot.agent.strategy;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.exception.AgentExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * 同步调用策略
 * 适用于Analyzer和Observer等需要等待完整结果的场景
 *
 * @author airx
 */
@Slf4j
public class SyncCallStrategy implements ExecutionStrategy<String> {

    @Override
    public String execute(ReactAgent agent, String input, AgentContext context) {
        try {
            log.debug("同步执行智能体: sessionId={}", context.getSessionId());
            AssistantMessage response = agent.call(input);

            String text = response.getText();
            if (text == null || text.isBlank()) {
                // LLM 最后一步可能只发了 tool call 没有文本回复
                log.warn("智能体返回空文本: name={}, hasToolCalls={}",
                        context.getSessionId(),
                        response.getToolCalls() != null && !response.getToolCalls().isEmpty());
                return response.getText();
            }
            return text;
        } catch (GraphRunnerException e) {
            log.error("同步执行失败: sessionId={}", context.getSessionId(), e);
            throw new AgentExecutionException("智能体执行失败", e);
        }
    }

    @Override
    public String getStrategyName() {
        return "SYNC_CALL";
    }
}
