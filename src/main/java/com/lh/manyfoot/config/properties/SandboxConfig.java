package com.lh.manyfoot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 沙箱容器配置类
 * 参考 ManyFoot 的沙箱执行模式配置
 *
 * @author airx
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "many-foot.sandbox")
public class SandboxConfig {

    /**
     * 是否启用沙箱功能
     */
    private Boolean enabled = true;

    /**
     * Docker 镜像名称
     */
    private String imageName = "many-foot-sandbox:python3.11";

    /**
     * 容器内存限制 (MB)
     */
    private Long memoryLimit = 512L;

    /**
     * CPU 限制 (核心数)
     */
    private Double cpuLimit = 1.0;

    /**
     * 单次执行超时时间 (秒)
     */
    private Integer executionTimeout = 300;

    /**
     * 容器空闲超时时间 (分钟)
     * 超过此时间未活动的容器将被自动清理
     */
    private Integer idleTimeout = 30;

    /**
     * 最大并发容器数
     */
    private Integer maxContainers = 10;

    /**
     * 容器内工作目录挂载路径
     */
    private String workspaceMount = "/workspace";

    /**
     * 宿主机工作目录基础路径
     */
    private String hostWorkspacePath = "/tmp/many-foot-sandbox";

    /**
     * 网络模式 (none/bridge/host)
     */
    private String networkMode = "bridge";

    /**
     * 是否启用网络访问
     */
    private Boolean networkEnabled = true;

    /**
     * Docker 主机地址
     * Windows: tcp://localhost:2375 或 npipe:////./pipe/docker_engine
     * Linux/Mac: unix:///var/run/docker.sock
     */
    private String dockerHost;

    /**
     * 容器前缀名称
     */
    private String containerPrefix = "many-foot-sandbox-";

    /**
     * 是否以特权模式运行 (不推荐)
     */
    private Boolean privileged = false;

    /**
     * 是否只读根文件系统
     */
    private Boolean readOnlyRootfs = false;

    /**
     * Python 执行器路径
     */
    private String pythonPath = "/usr/bin/python3";

    /**
     * Shell 执行器路径
     */
    private String shellPath = "/bin/bash";

    /**
     * 应用本机附件临时存储路径。
     * <p>
     * 容器与应用不在同一台机器时，多模态模型仍需从本地读取图片文件。
     * 上传文件时会在本机此路径下保存一份副本，供 {@code AgentMessageFactory} 构建
     * {@code Media} 使用。路径格式：{localAttachmentPath}/{sessionId}/data/{filename}。
     */
    private String localAttachmentPath = System.getProperty("java.io.tmpdir") + "/many-foot-attachments";
}
