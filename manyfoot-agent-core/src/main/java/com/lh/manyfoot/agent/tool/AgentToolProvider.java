package com.lh.manyfoot.agent.tool;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 智能体工具提供者接口
 * 不同的智能体可以有不同的工具配置
 *
 * @author airx
 */
public interface AgentToolProvider {

    /**
     * 获取工具列表
     *
     * @return 工具回调列表
     */
    List<ToolCallback> getTools();

    /**
     * 获取工具提供者名称
     *
     * @return 提供者名称
     */
    String getProviderName();
}
