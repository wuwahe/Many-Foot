package com.lh.manyfoot.agent.prompt;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.orchestrator.domain.Subtask;
import org.springframework.stereotype.Component;

/**
 * 子任务执行器智能体提示词提供者
 *
 * @author airx
 */
@Component
public class SubtaskExecutorPromptProvider implements AgentPromptProvider {

    @Override
    public String buildSystemPrompt(AgentContext context) {
        Subtask subtask = context.getCurrentSubtask();
        String taskType = subtask != null ? subtask.getType().name() : "CODE_EXECUTION";

        return String.format("""
            你是一个专门执行特定子任务的智能体。

            ## 当前会话id: %s

            ## 你的能力
            1. **executePython** - 执行Python代码
            2. **executeShell** - 执行Shell命令
            3. **readSandboxFile** - 读取沙箱中的文件
            4. **writeSandboxFile** - 写入文件到沙箱
            5. **listSandboxDirectory** - 列出目录内容
            6. **installPythonPackage** - 安装Python包

            ## 当前任务类型: %s

            ## 工作原则
            1. 专注于当前分配的子任务
            2. 执行完成后输出清晰的结果
            3. 遇到问题及时报告

            ## 输出格式
            完成任务后，请输出：
            - 执行的操作
            - 执行结果
            - 是否成功
            """, context.getSessionId(), taskType);
    }

    @Override
    public String buildUserInput(AgentContext context) {
        Subtask subtask = context.getCurrentSubtask();
        if (subtask == null) {
            return context.getQuery();
        }

        return String.format("""
            ## 当前子任务
            - 名称：%s
            - 描述：%s
            - 类型：%s

            ## 上下文
            %s

            请执行此子任务。
            """,
            subtask.getName(),
            subtask.getDescription(),
            subtask.getType(),
            StrUtil.isNotBlank(context.getAdditionalContext()) ? context.getAdditionalContext() : "无"
        );
    }
}
