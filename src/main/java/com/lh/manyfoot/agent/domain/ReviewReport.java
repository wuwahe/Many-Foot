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
 * 评审与验证 Agent 的独立复核报告。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评审决定，如 approve、reject、revise 等。
     */
    private String decision;

    /**
     * 发现的问题列表。
     */
    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

    /**
     * 评审评分，取值范围 0.0 - 100.0。
     */
    private Double score;

    /**
     * 必须修复的事项列表。
     */
    @Builder.Default
    private List<String> requiredFixes = new ArrayList<>();

    /**
     * 验证步骤列表，用于确认修复是否到位。
     */
    @Builder.Default
    private List<String> verificationSteps = new ArrayList<>();

    /**
     * 评审发现的问题条目。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 问题严重程度，如 critical、major、minor、info。
         */
        private String severity;

        /**
         * 问题描述信息。
         */
        private String message;

        /**
         * 问题所在位置，如文件路径、行号或模块名。
         */
        private String location;

        /**
         * 修复建议。
         */
        private String recommendation;
    }
}
