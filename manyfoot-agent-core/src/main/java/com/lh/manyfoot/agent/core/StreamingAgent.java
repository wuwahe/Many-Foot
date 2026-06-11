package com.lh.manyfoot.agent.core;

import com.lh.manyfoot.agent.context.AgentContext;
import reactor.core.publisher.Flux;

/**
 * 流式输出智能体接口
 * 继承自Agent，增加流式执行能力
 *
 * @param <T> 流式输出的元素类型
 * @author airx
 */
public interface StreamingAgent<T> extends Agent<Flux<T>> {

    @Override
    default boolean supportsStreaming() {
        return true;
    }

    /**
     * 流式执行
     *
     * @param context 执行上下文
     * @return 流式输出
     */
    Flux<T> stream(AgentContext context);

    /**
     * 默认execute实现调用stream
     */
    @Override
    default Flux<T> execute(AgentContext context) {
        return stream(context);
    }
}
