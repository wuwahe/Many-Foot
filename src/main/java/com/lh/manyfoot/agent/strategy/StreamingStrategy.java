package com.lh.manyfoot.agent.strategy;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.exception.AgentExecutionException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 流式执行策略
 * 适用于Executor等需要实时输出的场景
 *
 * @author airx
 */
@Slf4j
public class StreamingStrategy implements ExecutionStrategy<Flux<NodeOutput>> {

    @Override
    public Flux<NodeOutput> execute(ReactAgent agent, String input, AgentContext context) {
        try {
            log.debug("流式执行智能体: sessionId={}", context.getSessionId());
            return agent.stream(input);
        } catch (GraphRunnerException e) {
            log.error("流式执行失败: sessionId={}", context.getSessionId(), e);
            return Flux.error(new AgentExecutionException("智能体流式执行失败", e));
        }
    }

    @Override
    public String getStrategyName() {
        return "STREAMING";
    }
}
