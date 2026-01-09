package com.lh.manyfoot.async.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务恢复请求 (断线重连)
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResumeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 最后接收的事件序号
     */
    private Long lastEventSequence;
}
