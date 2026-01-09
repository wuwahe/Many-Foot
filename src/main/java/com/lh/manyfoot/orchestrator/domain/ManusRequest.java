package com.lh.manyfoot.orchestrator.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Manus 执行请求
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManusRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID (可选，不提供则自动生成)
     */
    private String sessionId;

    /**
     * 用户查询
     */
    @NotBlank(message = "查询内容不能为空")
    private String query;

    /**
     * 最大迭代次数
     */
    @Builder.Default
    private Integer maxIterations = 10;

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
     * 是否启用代码执行
     */
    @Builder.Default
    private Boolean enableCodeExecution = true;

    /**
     * 执行超时时间(秒)
     */
    @Builder.Default
    private Integer timeout = 300;
}
