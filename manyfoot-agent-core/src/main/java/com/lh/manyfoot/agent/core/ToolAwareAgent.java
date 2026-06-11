package com.lh.manyfoot.agent.core;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * 支持工具调用的智能体接口
 * 扩展基础Agent接口，增加工具管理能力
 *
 * @param <R> 执行返回类型
 * @author airx
 */
public interface ToolAwareAgent<R> extends Agent<R> {

    /**
     * 获取该智能体可用的工具列表
     *
     * @return 工具回调列表
     */
    List<ToolCallback> getAvailableTools();

    /**
     * 是否需要工具支持
     *
     * @return true表示需要工具
     */
    boolean requiresTools();
}
