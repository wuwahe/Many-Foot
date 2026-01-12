package com.lh.manyfoot.orchestrator;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.lh.manyfoot.models.registry.AiModelStorage;
import com.lh.manyfoot.models.registry.ModelRole;
import com.lh.manyfoot.orchestrator.domain.Subtask;
import com.lh.manyfoot.orchestrator.domain.SubtaskType;
import com.lh.manyfoot.orchestrator.domain.TaskAnalysisResult;
import com.lh.manyfoot.orchestrator.domain.TaskComplexity;
import com.lh.manyfoot.orchestrator.prompts.ManusPrompts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class MAgentBuilderService {


    private final ToolCallbackProvider toolCallbackProvider;

    private final AiModelStorage aiModelStorage;

    /**
     * 第一步：评估任务复杂度
     *
     * @param query 用户任务
     * @return 任务复杂度
     */
    public TaskComplexity assessComplexity(String query) {
        ChatModel chatModel = getChatModel(ModelRole.ANALYZE);
        String prompt = ManusPrompts.buildComplexityAssessPrompt(query);

        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String result = response.getResult().getOutput().getText();
            if (StrUtil.isNotBlank(result)) {
                String trimmed = result.trim().toUpperCase();
                // 处理可能包含额外字符的情况
                if (trimmed.contains("SIMPLE")) {
                    return TaskComplexity.SIMPLE;
                } else if (trimmed.contains("COMPLEX")) {
                    return TaskComplexity.COMPLEX;
                } else if (trimmed.contains("MEDIUM")) {
                    return TaskComplexity.MEDIUM;
                }
            }
            return TaskComplexity.MEDIUM; // 默认中等复杂度
        } catch (Exception e) {
            log.warn("评估任务复杂度失败，使用默认值: {}", e.getMessage());
            return TaskComplexity.MEDIUM;
        }
    }

    /**
     * 第二步：构建分析智能体，进行任务分解
     *
     * @param query     用户任务
     * @param sessionId 会话ID
     * @return 任务分析结果
     */
    public TaskAnalysisResult buildAnalyzerAgent(String query, String sessionId) {
        // 先评估复杂度
        TaskComplexity complexity = assessComplexity(query);
        log.info("任务复杂度评估结果: {} - query: {}", complexity, query);

        // 根据复杂度决定是否需要详细分解
        if (complexity == TaskComplexity.SIMPLE) {
            // 简单任务直接返回单步骤分析结果
            return TaskAnalysisResult.builder()
                .complexity(TaskComplexity.SIMPLE)
                .taskAnalysis("简单任务，直接执行")
                .subtasks(java.util.List.of(
                    Subtask.builder()
                        .id(1)
                        .name("执行任务")
                        .description(query)
                        .type(SubtaskType.CODE_EXECUTION)
                        .dependencies(java.util.List.of())
                        .parallel(false)
                        .build()
                ))
                .executionPlan("直接执行任务")
                .rawAnalysis(query)
                .build();
        }

        // 中等或复杂任务使用分析智能体进行分解
        ChatModel chatModel = getChatModel(ModelRole.ANALYZE);

        ReactAgent analyzerAgent = ReactAgent.builder()
            .name("Analyzer_agent")
            .systemPrompt(ManusPrompts.buildAnalyzerAgentPrompt(sessionId))
            .model(chatModel)
            .build();

        try {
            String input = "请分析以下任务并制定执行计划：\n" + query;
            AssistantMessage response = analyzerAgent.call(input);
            String analysisText = response.getText();

            // 解析分析结果
            TaskAnalysisResult result = TaskAnalysisResult.parseFromResponse(analysisText);
            // 覆盖已评估的复杂度
            result.setComplexity(complexity);

            log.info("任务分析完成，共 {} 个子任务", result.getSubtasks().size());
            return result;

        } catch (GraphRunnerException e) {
            log.error("分析智能体执行失败: {}", e.getMessage());
            throw new RuntimeException("任务分析失败", e);
        }
    }

    /**
     * 构建执行器Agent
     */
    public Flux<NodeOutput> buildExecutorAgent(String query, String observations, String sessionId) {
        ChatModel chatModel = getChatModel(ModelRole.EXECUTE);

        ReactAgent reactAgent = ReactAgent.builder()
            .name("Executor_agent")
            .tools(toolCallbackProvider.getToolCallbacks())
            .systemPrompt(ManusPrompts.buildExecutorPrompt(query, observations, sessionId))
            .model(chatModel)
            .build();
        try {
            return reactAgent.stream("任务清单：" + query + "\n" + "历史观察结果：" + observations);
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建子任务执行器Agent
     * 针对特定子任务构建专门的执行器
     *
     * @param subtask     子任务
     * @param context     上下文信息
     * @param sessionId   会话ID
     * @return Agent执行流
     */
    public Flux<NodeOutput> buildSubtaskExecutorAgent(
        Subtask subtask,
        String context,
        String sessionId) {

        ChatModel chatModel = getChatModel(ModelRole.EXECUTE);

        String systemPrompt = buildSubtaskExecutorPrompt(subtask, sessionId);

        ReactAgent reactAgent = ReactAgent.builder()
            .name("Subtask_Executor_" + subtask.getId())
            .tools(toolCallbackProvider.getToolCallbacks())
            .systemPrompt(systemPrompt)
            .model(chatModel)
            .build();

        try {
            String input = String.format("""
                ## 当前子任务
                - 名称：%s
                - 描述：%s
                - 类型：%s

                ## 上下文
                %s

                请执行此子任务。
                """,
                subtask.getName(),
                subtask.getDescription(),
                subtask.getType(),
                StrUtil.isNotBlank(context) ? context : "无"
            );
            return reactAgent.stream(input);
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建子任务执行器提示词
     */
    private String buildSubtaskExecutorPrompt(
        Subtask subtask,
        String sessionId) {

        return String.format("""
            你是一个专门执行特定子任务的智能体。

            ## 当前会话id: %s

            ## 你的能力
            1. **executePython** - 执行Python代码
            2. **executeShell** - 执行Shell命令
            3. **readSandboxFile** - 读取沙箱中的文件
            4. **writeSandboxFile** - 写入文件到沙箱
            5. **listSandboxDirectory** - 列出目录内容
            6. **installPythonPackage** - 安装Python包

            ## 当前任务类型: %s

            ## 工作原则
            1. 专注于当前分配的子任务
            2. 执行完成后输出清晰的结果
            3. 遇到问题及时报告

            ## 输出格式
            完成任务后，请输出：
            - 执行的操作
            - 执行结果
            - 是否成功
            """, sessionId, subtask.getType());
    }

    /**
     * 构建观察者Agent
     * 用于分析和验证执行器的执行结果
     *
     * @param query 原始任务
     * @param executeResult 执行器的执行结果
     * @param observations 历史观察结果
     * @return 观察分析结果
     */
    public String buildObserverAgent(String query, String executeResult, String observations, String sessionId) {
        ChatModel chatModel = getChatModel(ModelRole.OBSERVE);

        ReactAgent reactAgent = ReactAgent.builder()
            .name("Observer_agent")
            .tools(toolCallbackProvider.getToolCallbacks())
            .systemPrompt(ManusPrompts.buildObserverAgentPrompt(sessionId))
            .model(chatModel)
            .build();
        try {
            String input = buildObserverInput(query, executeResult, observations);
            AssistantMessage call = reactAgent.call(input);
            return call.getText();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建观察者Agent的输入
     */
    private String buildObserverInput(String query, String executeResult, String observations) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 原始任务\n").append(query).append("\n\n");
        sb.append("## 执行器输出结果\n").append(executeResult).append("\n\n");
        if (observations != null && !observations.isEmpty()) {
            sb.append("## 历史观察记录\n").append(observations).append("\n\n");
        }
        sb.append("请对以上执行结果进行观察和验证。");
        return sb.toString();
    }

    /**
     * 获取ChatModel
     */
    private ChatModel getChatModel(ModelRole modelRole) {
        return aiModelStorage.getChatModel(modelRole).get();
    }
}
