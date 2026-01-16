package com.lh.manyfoot.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 完整工具提供者
 * 提供所有可用的工具（适用于Executor智能体）
 * 包含本地工具和 MCP 远程工具
 *
 * @author airx
 */
@Slf4j
@Component
public class FullToolProvider implements AgentToolProvider {

    private final List<ToolCallback> allTools;

    public FullToolProvider(List<ToolCallbackProvider> toolCallbackProviders) {
        this.allTools = new ArrayList<>();
        for (ToolCallbackProvider provider : toolCallbackProviders) {
            allTools.addAll(Arrays.asList(provider.getToolCallbacks()));
        }
        log.info("完整工具提供者初始化完成，共 {} 个工具: {}",
                allTools.size(),
                allTools.stream()
                        .map(tool -> tool.getToolDefinition().name())
                        .toList());
    }

    @Override
    public List<ToolCallback> getTools() {
        return allTools;
    }

    @Override
    public String getProviderName() {
        return "FULL_TOOLS";
    }
}
