package com.lh.manyfoot.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 只读工具提供者
 * 只提供读取类的工具（适用于Observer智能体）
 * 包含：readSandboxFile, parseSandboxDocument, listSandboxDirectory
 *
 * @author airx
 */
@Slf4j
@Component
public class ReadOnlyToolProvider implements AgentToolProvider {

    /**
     * 只读工具名称集合
     */
    private static final Set<String> READ_ONLY_TOOL_NAMES = Set.of(
        "readSandboxFile",
        "parseSandboxDocument",
        "listSandboxDirectory"
    );

    private final List<ToolCallback> readOnlyTools;

    public ReadOnlyToolProvider(List<ToolCallbackProvider> toolCallbackProviders) {
        // 从所有 provider 中筛选只读工具
        this.readOnlyTools = toolCallbackProviders.stream()
            .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
            .filter(tool -> READ_ONLY_TOOL_NAMES.contains(tool.getToolDefinition().name()))
            .collect(Collectors.toList());

        log.info("只读工具提供者初始化完成，共 {} 个工具: {}",
            readOnlyTools.size(),
            readOnlyTools.stream()
                .map(tool -> tool.getToolDefinition().name())
                .collect(Collectors.joining(", ")));
    }

    @Override
    public List<ToolCallback> getTools() {
        return readOnlyTools;
    }

    @Override
    public String getProviderName() {
        return "READ_ONLY_TOOLS";
    }
}
