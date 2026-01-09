package com.lh.manyfoot.async.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 异步任务请求
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManusTaskRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID (可选)
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
}
