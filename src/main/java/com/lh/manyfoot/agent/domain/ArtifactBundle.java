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
 * 评审与验证 Agent 接收的计划、草稿或执行结果集合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactBundle implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 执行计划对象，通常为 PlanGraph 实例。
     */
    private Object plan;

    /**
     * 领域草稿对象，通常为 DomainDraft 实例。
     */
    private Object draft;

    /**
     * 执行结果对象，通常为工具执行返回的结果。
     */
    private Object result;

    /**
     * 关联的证据包。
     */
    private EvidencePack evidence;

    /**
     * 附加元数据，用于传递扩展信息。
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
