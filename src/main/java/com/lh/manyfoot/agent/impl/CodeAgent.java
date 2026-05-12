package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.core.AbstractToolAgent;
import com.lh.manyfoot.agent.prompt.CodeAgentPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.tool.FullToolProvider;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.models.registry.ModelRole;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 代码执行 Agent，负责在沙箱中编写、调试和运行代码。
 */
@Component
public class CodeAgent extends AbstractToolAgent<String> {

    public CodeAgent(ModelResolver modelResolver,
                     CodeAgentPromptProvider promptProvider,
                     FullToolProvider toolProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy(), toolProvider);
    }

    @Override
    public String getName() {
        return "Code_agent";
    }

    @Override
    public String getDescription() {
        return "代码执行智能体，负责编写、调试、运行代码，适用于数据分析、自动化脚本和复杂工具编排";
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.CODE;
    }

    @Override
    protected Set<String> getAvailableTools() {
        return Set.of(
            "writeSandboxFile",
            "readSandboxFile",
            "parseSandboxDocument",
            "listSandboxDirectory",
            "executeShell",
            "executePython",
            "installPythonPackage"
        );
    }
}
