package com.lh.manyfoot.orchestrator.domain;

import com.lh.manyfoot.codeact.domain.CodeExecutionResult;
import com.lh.manyfoot.domain.SandboxContainer;
import com.lh.manyfoot.event.domain.LoopPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Loop 上下文
 * 保存循环执行过程中的所有状态信息
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoopContext {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 原始查询
     */
    private String query;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 当前迭代次数
     */
    private Integer currentIteration;

    /**
     * 最大迭代次数
     */
    private Integer maxIterations;

    /**
     * 当前阶段
     */
    private LoopPhase currentPhase;

    /**
     * 沙箱容器
     */
    private SandboxContainer container;

    /**
     * 累积的观察结果
     */
    @Builder.Default
    private List<String> observations = new ArrayList<>();

    /**
     * 执行历史
     */
    @Builder.Default
    private List<CodeExecutionResult> executeHistory = new ArrayList<>();

    /**
     * 当前计划
     */
    private String currentPlan;

    /**
     * 分析后的任务清单（文本格式）
     */
    private String currentAnalysis;

    /**
     * 任务分析结果（结构化）
     */
    private TaskAnalysisResult taskAnalysisResult;

    /**
     * 上下文变量
     */
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 是否启用代码执行
     */
    @Builder.Default
    private Boolean enableCodeExecution = true;

    /**
     * 是否已完成
     */
    @Builder.Default
    private Boolean completed = false;

    /**
     * 完成原因
     */
    private String completionReason;

    /**
     * 添加观察结果
     */
    public void addObservation(String observation) {
        if (this.observations == null) {
            this.observations = new ArrayList<>();
        }
        this.observations.add(observation);
    }

    /**
     * 添加执行历史
     */
    public void addExecuteResult(CodeExecutionResult result) {
        if (this.executeHistory == null) {
            this.executeHistory = new ArrayList<>();
        }
        this.executeHistory.add(result);
    }

    /**
     * 获取最近N次观察结果
     */
    public List<String> getRecentObservations(int count) {
        if (observations == null || observations.isEmpty()) {
            return new ArrayList<>();
        }
        int size = observations.size();
        if (size <= count) {
            return new ArrayList<>(observations);
        }
        return new ArrayList<>(observations.subList(size - count, size));
    }

    /**
     * 获取所有观察结果的摘要
     */
    public String getObservationsSummary() {
        if (observations == null || observations.isEmpty()) {
            return "暂无观察结果";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < observations.size(); i++) {
            sb.append("第").append(i + 1).append("次观察:\n");
            sb.append(observations.get(i)).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, value);
    }

    /**
     * 获取变量
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        if (this.variables == null) {
            return null;
        }
        return (T) this.variables.get(key);
    }

    /**
     * 标记任务完成
     */
    public void markCompleted(String reason) {
        this.completed = true;
        this.completionReason = reason;
    }
}
