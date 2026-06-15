package com.lh.manyfoot.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Supervisor 下发给规划与路由 Agent 的任务规格。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSpec implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户目标或业务目标。
     */
    private String goal;

    /**
     * 约束、预算、偏好、禁止项等结构化条件。
     */
    @Builder.Default
    private Map<String, Object> constraints = new HashMap<>();
}
