package com.lh.manyfoot.agent.factory;

import com.lh.manyfoot.agent.core.Agent;
import com.lh.manyfoot.agent.impl.AnalyzerAgent;
import com.lh.manyfoot.agent.impl.ExecutorAgent;
import com.lh.manyfoot.agent.impl.ObserverAgent;
import com.lh.manyfoot.agent.impl.SubtaskExecutorAgent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 智能体工厂
 * 提供统一的智能体获取入口
 *
 * @author airx
 */
@Component
@RequiredArgsConstructor
@Getter
public class AgentFactory {

    private final AnalyzerAgent analyzerAgent;
    private final ExecutorAgent executorAgent;
    private final SubtaskExecutorAgent subtaskExecutorAgent;
    private final ObserverAgent observerAgent;

    /**
     * 根据类型获取智能体
     *
     * @param type 智能体类型
     * @return 智能体实例
     */
    public Agent<?> getAgent(AgentType type) {
        return switch (type) {
            case ANALYZER -> analyzerAgent;
            case EXECUTOR -> executorAgent;
            case SUBTASK_EXECUTOR -> subtaskExecutorAgent;
            case OBSERVER -> observerAgent;
        };
    }

    /**
     * 智能体类型枚举
     */
    public enum AgentType {
        /**
         * 分析智能体
         */
        ANALYZER,
        /**
         * 执行器智能体
         */
        EXECUTOR,
        /**
         * 子任务执行器智能体
         */
        SUBTASK_EXECUTOR,
        /**
         * 观察者智能体
         */
        OBSERVER
    }
}
