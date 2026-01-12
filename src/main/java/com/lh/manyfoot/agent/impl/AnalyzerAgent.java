package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractAgent;
import com.lh.manyfoot.agent.prompt.AnalyzerPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.models.registry.AiModelStorage;
import com.lh.manyfoot.models.registry.ModelRole;
import com.lh.manyfoot.orchestrator.domain.Subtask;
import com.lh.manyfoot.orchestrator.domain.SubtaskType;
import com.lh.manyfoot.orchestrator.domain.TaskAnalysisResult;
import com.lh.manyfoot.orchestrator.domain.TaskComplexity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分析智能体
 * 负责评估任务复杂度和进行任务分解
 *
 * 特点：
 * - 使用ANALYZE角色模型
 * - 不需要工具
 * - 同步调用
 * - 返回结构化的TaskAnalysisResult
 *
 * @author airx
 */
@Slf4j
@Component
public class AnalyzerAgent extends AbstractAgent<String> {

    private final ComplexityAssessor complexityAssessor;

    public AnalyzerAgent(AiModelStorage aiModelStorage,
                         AnalyzerPromptProvider promptProvider,
                         ComplexityAssessor complexityAssessor) {
        super(aiModelStorage, promptProvider, new SyncCallStrategy());
        this.complexityAssessor = complexityAssessor;
    }

    @Override
    public String getName() {
        return "Analyzer_agent";
    }

    @Override
    public String getDescription() {
        return "任务分析与规划智能体，负责评估复杂度和分解任务";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.ANALYZE;
    }

    /**
     * 评估任务复杂度
     *
     * @param query 用户任务
     * @return 任务复杂度
     */
    public TaskComplexity assessComplexity(String query) {
        return complexityAssessor.assess(query);
    }

    /**
     * 执行任务分析（包含复杂度评估）
     * 这是主要的业务方法
     *
     * @param context 智能体上下文
     * @return 任务分析结果
     */
    public TaskAnalysisResult analyzeTask(AgentContext context) {
        // 1. 先评估复杂度
        TaskComplexity complexity = complexityAssessor.assess(context.getQuery());
        log.info("任务复杂度评估结果: {} - query: {}", complexity, context.getQuery());

        // 2. 简单任务直接返回单步骤分析结果
        if (complexity == TaskComplexity.SIMPLE) {
            return createSimpleTaskResult(context.getQuery());
        }

        // 3. 中等或复杂任务使用分析智能体进行分解
        String analysisText = execute(context);

        // 4. 解析结果
        TaskAnalysisResult result = TaskAnalysisResult.parseFromResponse(analysisText);
        result.setComplexity(complexity);

        log.info("任务分析完成，共 {} 个子任务", result.getSubtasks().size());
        return result;
    }

    /**
     * 创建简单任务的分析结果
     *
     * @param query 用户任务
     * @return 简单任务分析结果
     */
    private TaskAnalysisResult createSimpleTaskResult(String query) {
        return TaskAnalysisResult.builder()
            .complexity(TaskComplexity.SIMPLE)
            .taskAnalysis("简单任务，直接执行")
            .subtasks(List.of(
                Subtask.builder()
                    .id(1)
                    .name("执行任务")
                    .description(query)
                    .type(SubtaskType.CODE_EXECUTION)
                    .dependencies(List.of())
                    .parallel(false)
                    .build()
            ))
            .executionPlan("直接执行任务")
            .rawAnalysis(query)
            .build();
    }
}
