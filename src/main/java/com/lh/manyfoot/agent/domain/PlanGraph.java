package com.lh.manyfoot.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 规划与路由 Agent 产出的执行计划图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanGraph implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 计划步骤列表。
     */
    @Builder.Default
    private List<PlanStep> steps = new ArrayList<>();

    /**
     * 顺序、并发、交接或群聊等执行模式。
     */
    private String mode;

    /**
     * 计划整体完成标准。
     */
    @Builder.Default
    private List<String> completionCriteria = new ArrayList<>();

    /**
     * 预算、轮次、时间、工具调用次数等计划约束说明。
     */
    private String budget;

    /**
     * 失败后重新规划策略。
     */
    private String replanningPolicy;

    /**
     * 单个计划节点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanStep implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 步骤唯一标识。
         */
        private String id;

        /**
         * 步骤名称。
         */
        private String name;

        /**
         * 步骤描述。
         */
        private String description;

        /**
         * 目标智能体标识，指定执行该步骤的 Agent。
         */
        private String targetAgent;

        /**
         * 依赖的前置步骤 ID 列表。
         */
        @Builder.Default
        private List<String> dependencies = new ArrayList<>();

        /**
         * 是否可与其他步骤并行执行。
         */
        @Builder.Default
        private Boolean parallel = false;

        /**
         * 交接输入说明，定义从上游步骤接收的数据。
         */
        @Builder.Default
        private List<String> handoffInputs = new ArrayList<>();

        /**
         * 预期输出说明。
         */
        @Builder.Default
        private List<String> expectedOutputs = new ArrayList<>();

        /**
         * 验收标准列表。
         */
        @Builder.Default
        private List<String> acceptanceCriteria = new ArrayList<>();
    }
}
