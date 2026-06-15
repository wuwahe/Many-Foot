package com.lh.manyfoot.agent.context;

import lombok.Builder;
import lombok.Data;

/**
 * 智能体附件元数据。
 * <p>
 * 附件只描述上传文件的位置和类型，不在上下文中保存文件内容：
 * 图片由执行策略转换为多模态 {@code Media} 交给模型；普通文件保留沙箱路径，
 * 由具备 MCP/工具能力的子 Agent 自行读取。
 */
@Data
@Builder
public class AgentAttachment {

    /**
     * 容器内路径，例如 /workspace/data/demo.png。
     */
    private String sandboxPath;

    /**
     * 宿主机路径，用于应用进程把图片作为 Resource 交给多模态模型。
     */
    private String hostPath;

    /**
     * 文件名。
     */
    private String filename;

    /**
     * MIME 类型，例如 image/png、text/plain。
     */
    private String mimeType;

    /**
     * 是否为可直接交给多模态模型的图片。
     */
    private boolean image;
}
