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
 * 工具与事务执行 Agent 接收的明确动作调用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionCall implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 动作名称，对应可调用的工具或事务标识。
     */
    private String name;

    /**
     * 动作调用参数，键值对形式。
     */
    @Builder.Default
    private Map<String, Object> args = new HashMap<>();

    /**
     * 幂等键，用于防止重复执行同一动作。
     */
    private String idempotencyKey;
}
