package com.lh.manyfoot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 沙箱容器实体
 *
 * @author airx
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxContainer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 容器ID (Docker Container ID)
     */
    private String containerId;

    /**
     * 容器名称
     */
    private String containerName;

    /**
     * 关联的会话ID
     */
    private String sessionId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 容器状态
     */
    private ContainerStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后活动时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 工作目录路径 (宿主机)
     */
    private String workspacePath;

    /**
     * 容器IP地址
     */
    private String ipAddress;

    /**
     * 镜像名称
     */
    private String imageName;

    /**
     * 内存限制 (MB)
     */
    private Long memoryLimit;

    /**
     * CPU 限制
     */
    private Double cpuLimit;

    /**
     * 更新最后活动时间
     */
    public void touch() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 判断容器是否空闲超时
     *
     * @param idleTimeoutMinutes 空闲超时分钟数
     * @return 是否超时
     */
    public boolean isIdleTimeout(int idleTimeoutMinutes) {
        if (lastActiveTime == null) {
            return false;
        }
        return lastActiveTime.plusMinutes(idleTimeoutMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * 判断容器是否可用
     */
    public boolean isAvailable() {
        return status == ContainerStatus.RUNNING;
    }
}
