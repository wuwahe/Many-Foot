package com.lh.manyfoot.codeact.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 代码执行请求
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 执行类型
     */
    @NotNull(message = "代码类型不能为空")
    private CodeType codeType;

    /**
     * 代码内容
     */
    @NotBlank(message = "代码内容不能为空")
    private String code;

    /**
     * 工作目录 (容器内相对路径)
     */
    private String workingDirectory;

    /**
     * 环境变量
     */
    private Map<String, String> environment;

    /**
     * 超时时间(秒)
     */
    @Builder.Default
    private Integer timeout = 60;

    /**
     * 是否异步执行
     */
    @Builder.Default
    private Boolean async = false;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文件名 (可选，用于保存代码文件)
     */
    private String fileName;
}
