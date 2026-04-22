package com.lh.manyfoot.orchestrator;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.factory.AgentFactory;
import com.lh.manyfoot.async.domain.TaskStatus;
import com.lh.manyfoot.codeact.CodeActEngine;
import com.lh.manyfoot.codeact.domain.CodeExecutionResult;
import com.lh.manyfoot.codeact.domain.CodeType;
import com.lh.manyfoot.domain.SandboxContainer;
import com.lh.manyfoot.event.domain.LoopPhase;
import com.lh.manyfoot.event.domain.ManusEvent;
import com.lh.manyfoot.event.domain.SessionState;
import com.lh.manyfoot.event.service.EventStreamService;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import com.lh.manyfoot.orchestrator.domain.LoopContext;
import com.lh.manyfoot.orchestrator.domain.ManusRequest;
import com.lh.manyfoot.orchestrator.domain.TaskAnalysisResult;
import com.lh.manyfoot.orchestrator.prompts.ManusPrompts;
import com.lh.manyfoot.service.SandboxContainerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manus 编排器
 * 实现 Agent Loop：分析→规划→执行→观察 循环
 *
 * @author airx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "manus.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ManusOrchestrator {

    private final SandboxContainerManager containerManager;
    private final CodeActEngine codeActEngine;
    private final EventStreamService eventStreamService;
    private final AgentFactory agentFactory;
    private final ModelResolver modelResolver;

    // 代码块匹配正则
    private static final Pattern PYTHON_CODE_PATTERN = Pattern.compile("```python\\s*([\\s\\S]*?)```", Pattern.MULTILINE);
    private static final Pattern SHELL_CODE_PATTERN = Pattern.compile("```(?:shell|bash)\\s*([\\s\\S]*?)```", Pattern.MULTILINE);
    private static final Pattern TASK_COMPLETE_PATTERN = Pattern.compile("\\[TASK_COMPLETE\\]", Pattern.CASE_INSENSITIVE);

    // 默认最大迭代次数
    private static final int DEFAULT_MAX_ITERATIONS = 10;

    /**
     * 执行 Manus 风格的 Agent Loop
     *
     * @param request 请求
     * @return 事件流
     */
    public Flux<ManusEvent> execute(ManusRequest request) {
        String sessionId = StrUtil.isBlank(request.getSessionId())
            ? IdUtil.fastSimpleUUID()
            : request.getSessionId();

        int maxIterations = request.getMaxIterations() != null
            ? request.getMaxIterations()
            : DEFAULT_MAX_ITERATIONS;

        return Flux.create(sink -> {
            try {
                executeLoop(sink, sessionId, request, maxIterations);
            } catch (Exception e) {
                log.error("Agent Loop 执行失败: sessionId={}", sessionId, e);
                emitEvent(sink, sessionId, ManusEvent.error(sessionId, e.getMessage()));
                sink.error(e);
            }
        });
    }

    /**
     * 执行循环
     */
    private void executeLoop(FluxSink<ManusEvent> sink, String sessionId,
                             ManusRequest request, int maxIterations) {
        // 1. 初始化
        LoopContext context = initializeContext(sessionId, request, maxIterations);
        emitEvent(sink, sessionId, ManusEvent.loopStart(sessionId, maxIterations));

        // 2. 获取或创建沙箱容器
        getTheSandbox(sink, sessionId, request, context);

        // 分析阶段 - 使用分析智能体进行任务分解
        context.setCurrentPhase(LoopPhase.ANALYZE);
        TaskAnalysisResult analysisResult = analyzePhaseWithAgent(context);
        context.setTaskAnalysisResult(analysisResult);
        String analysisText = analysisResult.toTaskListFormat();
        context.setCurrentAnalysis(analysisText);
        emitEvent(sink, sessionId, ManusEvent.analyze(sessionId, context.getCurrentIteration(), analysisText));
        log.info("任务分析完成 - 复杂度: {}, 子任务数: {}", analysisResult.getComplexity(), analysisResult.getSubtasks().size());

        // 3. Agent Loop
        while (!context.getCompleted() && context.getCurrentIteration() < maxIterations) {
            context.setCurrentIteration(context.getCurrentIteration() + 1);
            emitEvent(sink, sessionId, ManusEvent.loopIteration(sessionId, context.getCurrentIteration()));

            try {
                // 3.1 执行器智能体根据任务清单进行执行
                context.setCurrentPhase(LoopPhase.EXECUTE);

                // 构建执行器上下文
                AgentContext executorContext = AgentContext.builder()
                    .sessionId(context.getSessionId())
                    .query(context.getQuery())
                    .observations(context.getObservationsSummary())
                    .build();

                // 使用 ExecutorAgent 执行
                Flux<NodeOutput> nodeOutputFlux = agentFactory.getExecutorAgent().execute(executorContext);

                // 处理流式输出，收集执行结果并发送事件
                String executeResult = processNodeOutputFlux(
                    nodeOutputFlux,
                    sink,
                    sessionId,
                    context.getCurrentIteration(),
                    agentFactory.getExecutorAgent().getName()
                );

                // 发送执行结果汇总事件
                emitEvent(sink, sessionId, ManusEvent.plan(sessionId, context.getCurrentIteration(), executeResult));

                if (StrUtil.isNotBlank(executeResult)) {
                    // 3.2 观察阶段 - 使用观察者智能体对执行结果进行观察和验证
                    context.setCurrentPhase(LoopPhase.OBSERVE);

                    // 构建观察者上下文
                    AgentContext observerContext = AgentContext.builder()
                        .sessionId(context.getSessionId())
                        .query(context.getQuery())
                        .executeResult(executeResult)
                        .observations(context.getObservationsSummary())
                        .build();

                    // 使用 ObserverAgent 执行
                    String observation = agentFactory.getObserverAgent().execute(observerContext);

                    context.addObservation(observation);
                    emitEvent(sink, sessionId, ManusEvent.observe(sessionId, context.getCurrentIteration(), observation));

                    // 检查观察者是否判断任务完成
                    if (isTaskComplete(observation)) {
                        context.markCompleted("任务已完成");
                        break;
                    }
                } else {
                    // 没有执行结果，添加空观察
                    context.addObservation("执行器无输出");
                }

                // 保存状态
                saveSessionState(context);

            } catch (Exception e) {
                log.error("迭代执行失败: iteration={}", context.getCurrentIteration(), e);
                emitEvent(sink, sessionId, ManusEvent.error(sessionId, "迭代执行失败: " + e.getMessage()));
                // 继续下一次迭代
            }
        }

        // 4. 生成总结
        String summary = generateSummary(context);
        emitEvent(sink, sessionId, ManusEvent.taskComplete(sessionId, summary));

        // 5. 更新最终状态
        SessionState state = eventStreamService.restoreSessionState(sessionId);
        if (state != null) {
            state.setTaskStatus(TaskStatus.COMPLETED);
            state.setCompleted(true);
            state.setFinalSummary(summary);
            eventStreamService.saveSessionState(sessionId, state);
        }

        sink.complete();
    }

    private void getTheSandbox(FluxSink<ManusEvent> sink, String sessionId, ManusRequest request, LoopContext context) {
        if (Boolean.TRUE.equals(request.getEnableCodeExecution())) {
            try {
                SandboxContainer container = containerManager.getOrCreateContainer(
                    sessionId, request.getTenantId(), request.getUserId());
                context.setContainer(container);
                emitEvent(sink, sessionId, ManusEvent.containerCreated(sessionId, container.getContainerId()));
            } catch (Exception e) {
                log.warn("创建沙箱容器失败，将禁用代码执行: {}", e.getMessage());
                context.setEnableCodeExecution(false);
                emitEvent(sink, sessionId, ManusEvent.warning(sessionId, "沙箱容器创建失败，代码执行已禁用"));
            }
        }
    }

    /**
     * 初始化上下文
     */
    private LoopContext initializeContext(String sessionId, ManusRequest request, int maxIterations) {
        // 创建并保存会话状态
        SessionState state = SessionState.createInitial(
            sessionId,
            request.getQuery(),
            maxIterations,
            request.getTenantId(),
            request.getUserId()
        );
        state.setTaskStatus(TaskStatus.RUNNING);
        eventStreamService.saveSessionState(sessionId, state);

        // 创建循环上下文
        return LoopContext.builder()
            .sessionId(sessionId)
            .query(request.getQuery())
            .modelId(request.getModelId() != null ? request.getModelId() : 1L)
            .tenantId(request.getTenantId())
            .userId(request.getUserId())
            .currentIteration(0)
            .maxIterations(maxIterations)
            .currentPhase(LoopPhase.ANALYZE)
            .enableCodeExecution(request.getEnableCodeExecution())
            .build();
    }

    /**
     * 分析阶段 - 使用分析智能体进行任务分解
     */
    private TaskAnalysisResult analyzePhaseWithAgent(LoopContext context) {
        try {
            // 构建分析器上下文
            AgentContext analyzerContext = AgentContext.builder()
                .sessionId(context.getSessionId())
                .query(context.getQuery())
                .build();

            // 使用 AnalyzerAgent 执行任务分析
            return agentFactory.getAnalyzerAgent().analyzeTask(analyzerContext);
        } catch (Exception e) {
            log.error("分析智能体执行失败，使用默认分析: {}", e.getMessage());
            // 降级处理：使用简单的LLM调用
            String analysisText = callLLM(ModelRole.ANALYZE,
                ManusPrompts.buildAnalyzePrompt(context.getQuery()));
            return TaskAnalysisResult.parseFromResponse(analysisText);
        }
    }

    /**
     * 执行阶段 - 执行代码或调用工具
     */
    private CodeExecutionResult executePhase(LoopContext context, String plan,
                                             FluxSink<ManusEvent> sink, String sessionId) {
        if (!Boolean.TRUE.equals(context.getEnableCodeExecution())) {
            return null;
        }

        // 解析计划中的代码块
        CodeBlock codeBlock = parseCodeBlock(plan);

        if (codeBlock == null) {
            log.debug("规划中没有代码块需要执行");
            return null;
        }

        // 发送执行开始事件
        emitEvent(sink, sessionId, ManusEvent.execute(
            sessionId, context.getCurrentIteration(), codeBlock.code, codeBlock.type));

        // 执行代码
        CodeExecutionResult result;
        if (codeBlock.type == CodeType.PYTHON) {
            result = codeActEngine.executePython(
                sessionId, codeBlock.code, context.getTenantId(), context.getUserId());
        } else {
            result = codeActEngine.executeShell(
                sessionId, codeBlock.code, context.getTenantId(), context.getUserId());
        }

        // 发送执行结果事件
        emitEvent(sink, sessionId, ManusEvent.codeResult(
            sessionId, context.getCurrentIteration(), result));

        return result;
    }

    /**
     * 生成任务总结
     */
    private String generateSummary(LoopContext context) {
        String prompt = ManusPrompts.buildSummaryPrompt(
            context.getQuery(),
            context.getCurrentIteration(),
            context.getObservationsSummary()
        );

        return callLLM(ModelRole.OBSERVE, prompt);
    }

    /**
     * 判断任务是否完成
     */
    private boolean isTaskComplete(String plan) {
        if (StrUtil.isBlank(plan)) {
            return false;
        }
        return TASK_COMPLETE_PATTERN.matcher(plan).find();
    }

    /**
     * 解析代码块
     */
    private CodeBlock parseCodeBlock(String plan) {
        if (StrUtil.isBlank(plan)) {
            return null;
        }

        // 先尝试匹配 Python 代码
        Matcher pythonMatcher = PYTHON_CODE_PATTERN.matcher(plan);
        if (pythonMatcher.find()) {
            String code = pythonMatcher.group(1).trim();
            if (StrUtil.isNotBlank(code)) {
                return new CodeBlock(CodeType.PYTHON, code);
            }
        }

        // 再尝试匹配 Shell 代码
        Matcher shellMatcher = SHELL_CODE_PATTERN.matcher(plan);
        if (shellMatcher.find()) {
            String code = shellMatcher.group(1).trim();
            if (StrUtil.isNotBlank(code)) {
                return new CodeBlock(CodeType.SHELL, code);
            }
        }

        return null;
    }

    /**
     * 调用大模型
     */
    private String callLLM(ModelRole role, String promptText) {
        try {
            ChatModel chatModel = modelResolver.forRole(role);

            Prompt prompt = new Prompt(promptText);
            ChatResponse response = chatModel.call(prompt);

            if (response != null && response.getResult() != null
                && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }

            return "";
        } catch (Exception e) {
            log.error("调用大模型失败: role={}", role.name(), e);
            return "调用大模型失败: " + e.getMessage();
        }
    }

    /**
     * 处理 NodeOutput 流式输出
     * 参考 AgentResponseConverter 的处理方式，解析工具调用和内容信息并发送事件
     *
     * @param nodeOutputFlux NodeOutput 流
     * @param sink           事件发送器
     * @param sessionId      会话ID
     * @param iteration      当前迭代次数
     * @param agentName      Agent名称
     * @return 收集的执行结果文本
     */
    private String processNodeOutputFlux(Flux<NodeOutput> nodeOutputFlux,
                                         FluxSink<ManusEvent> sink,
                                         String sessionId,
                                         int iteration,
                                         String agentName) {
        List<String> contentParts = new ArrayList<>();

        // 阻塞处理流式输出，逐个发送事件
        nodeOutputFlux.toStream().forEach(nodeOutput -> {
            processNodeOutput(nodeOutput, sink, sessionId, iteration, agentName, contentParts);
        });

        // 返回拼接的内容
        return String.join("", contentParts);
    }

    /**
     * 处理单个 NodeOutput
     * 参考 AgentResponseConverter 的实现
     *
     * @param nodeOutput   节点输出
     * @param sink         事件发送器
     * @param sessionId    会话ID
     * @param iteration    当前迭代次数
     * @param agentName    Agent名称
     * @param contentParts 内容收集列表
     */
    private void processNodeOutput(NodeOutput nodeOutput,
                                   FluxSink<ManusEvent> sink,
                                   String sessionId,
                                   int iteration,
                                   String agentName,
                                   List<String> contentParts) {
        // 获取节点和Agent名称
        String node = nodeOutput.node();
        String outputAgentName = nodeOutput.agent();
        String effectiveAgentName = StrUtil.isNotBlank(outputAgentName) ? outputAgentName : agentName;

        // 处理流式输出
        if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
            Message message = streamingOutput.message();

            if (message == null) {
                return;
            }

            // 处理助手消息
            if (message instanceof AssistantMessage assistantMessage) {
                // 如果包含工具调用，发送工具调用事件
                if (assistantMessage.hasToolCalls()) {
                    assistantMessage.getToolCalls().forEach(toolCall -> {
                        String toolName = toolCall.name();
                        String toolArgs = toolCall.arguments();

                        // 发送工具调用事件
                        emitEvent(sink, sessionId, ManusEvent.toolCall(
                            sessionId,
                            effectiveAgentName,
                            toolName,
                            toolArgs
                        ));

                        log.debug("Agent [{}] 调用工具: {} 参数: {}", effectiveAgentName, toolName, toolArgs);
                    });
                }

                // 处理文本内容
                String text = assistantMessage.getText();
                if (StrUtil.isNotBlank(text)) {
                    contentParts.add(text);

                    // 发送Agent输出事件（流式内容）
                    emitEvent(sink, sessionId, ManusEvent.agentOutput(
                        sessionId,
                        effectiveAgentName,
                        text
                    ));
                }
            } else {
                // 其他类型消息
                String content = message.getText();
                if (StrUtil.isNotBlank(content)) {
                    contentParts.add(content);

                    emitEvent(sink, sessionId, ManusEvent.agentOutput(
                        sessionId,
                        effectiveAgentName,
                        content
                    ));
                }
            }
        }
    }

    /**
     * 发送事件并持久化
     */
    private void emitEvent(FluxSink<ManusEvent> sink, String sessionId, ManusEvent event) {
        try {
            eventStreamService.appendEvent(sessionId, event);
            sink.next(event);
        } catch (Exception e) {
            log.error("发送事件失败", e);
        }
    }

    /**
     * 保存会话状态
     */
    private void saveSessionState(LoopContext context) {
        SessionState state = eventStreamService.restoreSessionState(context.getSessionId());
        if (state != null) {
            state.setCurrentIteration(context.getCurrentIteration());
            state.setCurrentPhase(context.getCurrentPhase());
            state.setCurrentPlan(context.getCurrentPlan());
            state.setObservations(context.getObservations());
            if (context.getContainer() != null) {
                state.setContainerId(context.getContainer().getContainerId());
            }
            eventStreamService.saveSessionState(context.getSessionId(), state);
        }
    }

    /**
     * 代码块内部类
     */
    private record CodeBlock(CodeType type, String code) {
    }
}
