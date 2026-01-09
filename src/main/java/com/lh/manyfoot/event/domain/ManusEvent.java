package com.lh.manyfoot.event.domain;

import cn.hutool.core.util.IdUtil;
import com.lh.manyfoot.codeact.domain.CodeExecutionResult;
import com.lh.manyfoot.codeact.domain.CodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Manus 事件实体
 * 用于记录 Agent Loop 执行过程中的所有事件
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManusEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 事件类型
     */
    private ManusEventType eventType;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 循环迭代次数
     */
    private Integer iteration;

    /**
     * 当前阶段
     */
    private LoopPhase phase;

    /**
     * 内容
     */
    private String content;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具参数
     */
    private String toolArgs;

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 事件序号 (用于排序)
     */
    private Long sequence;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建循环开始事件
     */
    public static ManusEvent loopStart(String sessionId, int maxIterations) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.LOOP_START)
            .content("Agent Loop 开始，最大迭代次数: " + maxIterations)
            .iteration(0)
            .timestamp(System.currentTimeMillis())
            .metadata(Map.of("maxIterations", maxIterations))
            .build();
    }

    /**
     * 创建循环迭代事件
     */
    public static ManusEvent loopIteration(String sessionId, int iteration) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.LOOP_ITERATION)
            .content("开始第 " + iteration + " 次迭代")
            .iteration(iteration)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建分析阶段事件
     */
    public static ManusEvent analyze(String sessionId, int iteration, String analysis) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.ANALYZE)
            .phase(LoopPhase.ANALYZE)
            .iteration(iteration)
            .content(analysis)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建规划阶段事件
     */
    public static ManusEvent plan(String sessionId, int iteration, String plan) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.PLAN)
            .phase(LoopPhase.PLAN)
            .iteration(iteration)
            .content(plan)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建执行阶段事件
     */
    public static ManusEvent execute(String sessionId, int iteration, String code, CodeType codeType) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.EXECUTE)
            .phase(LoopPhase.EXECUTE)
            .iteration(iteration)
            .content("执行 " + codeType.getName() + " 代码")
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建代码执行结果事件
     */
    public static ManusEvent codeResult(String sessionId, int iteration, CodeExecutionResult result) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.CODE_EXECUTION_COMPLETE)
            .phase(LoopPhase.EXECUTE)
            .iteration(iteration)
            .content(result.isSuccess() ? "代码执行成功" : "代码执行失败")
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建观察阶段事件
     */
    public static ManusEvent observe(String sessionId, int iteration, String observation) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.OBSERVE)
            .phase(LoopPhase.OBSERVE)
            .iteration(iteration)
            .content(observation)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建思考事件
     */
    public static ManusEvent thinking(String sessionId, String agentName, String content) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.THINKING)
            .agentName(agentName)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建工具调用事件
     */
    public static ManusEvent toolCall(String sessionId, String agentName, String toolName, String toolArgs) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.TOOL_CALL)
            .agentName(agentName)
            .toolName(toolName)
            .toolArgs(toolArgs)
            .content("调用工具: " + toolName)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建工具结果事件
     */
    public static ManusEvent toolResult(String sessionId, String agentName, String toolName, String result) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.TOOL_RESULT)
            .agentName(agentName)
            .toolName(toolName)
            .content(result)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建任务完成事件
     */
    public static ManusEvent taskComplete(String sessionId, String summary) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.TASK_COMPLETE)
            .content(summary)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建容器事件
     */
    public static ManusEvent containerCreated(String sessionId, String containerId) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.CONTAINER_CREATED)
            .content("沙箱容器已创建")
            .timestamp(System.currentTimeMillis())
            .metadata(Map.of("containerId", containerId))
            .build();
    }

    /**
     * 创建Agent输出事件
     */
    public static ManusEvent agentOutput(String sessionId, String agentName, String content) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.AGENT_OUTPUT)
            .agentName(agentName)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建错误事件
     */
    public static ManusEvent error(String sessionId, String errorMessage) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.ERROR)
            .content(errorMessage)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 创建警告事件
     */
    public static ManusEvent warning(String sessionId, String warningMessage) {
        return ManusEvent.builder()
            .eventId(IdUtil.fastSimpleUUID())
            .sessionId(sessionId)
            .eventType(ManusEventType.WARNING)
            .content(warningMessage)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
