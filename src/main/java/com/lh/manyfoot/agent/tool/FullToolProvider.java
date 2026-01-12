package com.lh.manyfoot.agent.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 完整工具提供者
 * 提供所有可用的工具（适用于Executor智能体）
 * 包含：executePython, executeShell, readSandboxFile, writeSandboxFile, listSandboxDirectory, installPythonPackage
 *
 * @author airx
 */
@Component
@RequiredArgsConstructor
public class FullToolProvider implements AgentToolProvider {

    private final ToolCallbackProvider toolCallbackProvider;

    @Override
    public List<ToolCallback> getTools() {
        return Arrays.asList(toolCallbackProvider.getToolCallbacks());
    }

    @Override
    public String getProviderName() {
        return "FULL_TOOLS";
    }
}
