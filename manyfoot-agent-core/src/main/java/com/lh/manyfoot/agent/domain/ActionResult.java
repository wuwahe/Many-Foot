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
 * 工具与事务执行 Agent 的动作执行结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 执行状态，如 success、failure、timeout 等。
     */
    private String status;

    /**
     * 执行产出物的存储 URI，可为文件路径或远程地址。
     */
    private String artifactUri;

    /**
     * 执行过程日志列表。
     */
    @Builder.Default
    private List<String> logs = new ArrayList<>();

    /**
     * 错误信息，仅在执行失败时填充。
     */
    private String error;
}
