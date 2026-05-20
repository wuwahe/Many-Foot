package com.lh.manyfoot.agent.support;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工具调用预算限制包装器。
 * <p>
 * 包装一个 {@link ToolCallback}，限制其可被调用的最大次数。
 * 超过预算后，直接返回预设的“预算耗尽”提示，不再委托给底层工具。
 * <p>
 * 用途：防止 ReAct 循环中的模型无节制地反复调用工具（如搜索工具），
 * 将单次 Agent 执行的工具调用成本控制在可预期范围内。
 *
 * @author airx
 */
public class BudgetLimitedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final int maxCalls;
    private final String budgetExhaustedMessage;
    private final AtomicInteger callCounter;

    private BudgetLimitedToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.maxCalls = builder.maxCalls;
        this.budgetExhaustedMessage = builder.budgetExhaustedMessage;
        this.callCounter = new AtomicInteger(0);
    }

    /**
     * 创建限制为 3 次调用的默认预算包装器。
     *
     * @param delegate 被包装的工具
     * @return 预算限制包装器
     */
    public static BudgetLimitedToolCallback of(ToolCallback delegate) {
        return builder().delegate(delegate).build();
    }

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        int current = callCounter.incrementAndGet();
        if (current > maxCalls) {
            return budgetExhaustedMessage;
        }
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        int current = callCounter.incrementAndGet();
        if (current > maxCalls) {
            return budgetExhaustedMessage;
        }
        return delegate.call(toolInput, toolContext);
    }

    /**
     * 获取剩余可用调用次数。
     *
     * @return 剩余次数
     */
    public int remainingCalls() {
        int used = callCounter.get();
        return Math.max(0, maxCalls - used);
    }

    /**
     * 获取已调用次数。
     *
     * @return 已调用次数
     */
    public int usedCalls() {
        return callCounter.get();
    }

    /**
     * Builder 模式构造器。
     */
    public static class Builder {
        private ToolCallback delegate;
        private int maxCalls = 3;
        private String budgetExhaustedMessage = "搜索预算已用完，请基于已有结果生成最终回复。";

        private Builder() {
        }

        public Builder delegate(ToolCallback delegate) {
            this.delegate = delegate;
            return this;
        }

        public Builder maxCalls(int maxCalls) {
            if (maxCalls <= 0) {
                throw new IllegalArgumentException("maxCalls must be > 0");
            }
            this.maxCalls = maxCalls;
            return this;
        }

        public Builder budgetExhaustedMessage(String message) {
            this.budgetExhaustedMessage = message;
            return this;
        }

        public BudgetLimitedToolCallback build() {
            if (delegate == null) {
                throw new IllegalArgumentException("delegate must not be null");
            }
            return new BudgetLimitedToolCallback(this);
        }
    }
}
