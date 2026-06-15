package com.lh.manyfoot.agent.stream;

import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.lh.manyfoot.agent.stream.event.PhaseHint;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.Optional;

/**
 * 流式输出阶段解析器，根据 StreamingOutput 的 OutputType 和消息内容推断当前执行阶段。
 * <p>
 * 纯状态机，非 Spring 组件，每次翻译流时新建实例。
 * <p>
 * 状态转换规则：
 * <ul>
 *   <li>首次 AGENT_MODEL_STREAMING（有文本，无 toolCalls）→ THINKING（seenTool=false）或 RESPONDING（seenTool=true）</li>
 *   <li>AGENT_MODEL_STREAMING（有 toolCalls）→ ACTING，seenTool 置 true</li>
 *   <li>AGENT_TOOL_STREAMING / AGENT_TOOL_FINISHED / AGENT_HOOK_* → ACTING，seenTool=true</li>
 *   <li>AGENT_MODEL_FINISHED / GRAPH_NODE_* → 不切换</li>
 *   <li>outputType == null 但消息形状是有文本无 toolCall → 按"有文本无 toolCalls"分支处理</li>
 * </ul>
 * 仅在 phase 真正变化时返回 PhaseHint，首次发射文本前会先发一次 PhaseHint(THINKING)。
 */
public class PhaseResolver {

    private Phase current;

    private boolean seenTool;

    public PhaseResolver() {
        this.current = null;
        this.seenTool = false;
    }

    /**
     * 根据当前输出类型和消息内容推断阶段变化。
     *
     * @param outputType 流式输出类型，可能为 null
     * @param message    助手消息，可能为 null
     * @return 仅在阶段变化时返回 PhaseHint，否则返回 Optional.empty()
     */
    public Optional<PhaseHint> resolve(OutputType outputType, AssistantMessage message) {
        Phase newPhase = null;

        if (outputType == OutputType.AGENT_MODEL_STREAMING) {
            // 模型流式输出：根据是否有 toolCalls 区分"行动"与"思考/响应"
            if (message != null && message.hasToolCalls()) {
                newPhase = Phase.ACTING;
                seenTool = true;
            } else if (message != null && hasText(message)) {
                // 有文本无 toolCalls：首次为 THINKING，工具调用过后的为 RESPONDING
                newPhase = seenTool ? Phase.RESPONDING : Phase.THINKING;
            }
        } else if (outputType == OutputType.AGENT_TOOL_STREAMING
                || outputType == OutputType.AGENT_TOOL_FINISHED
                || outputType == OutputType.AGENT_HOOK_STREAMING
                || outputType == OutputType.AGENT_HOOK_FINISHED) {
            // 工具执行或 Hook 阶段，统一归为 ACTING
            newPhase = Phase.ACTING;
            seenTool = true;
        } else if (outputType == OutputType.AGENT_MODEL_FINISHED
                || outputType == OutputType.GRAPH_NODE_STREAMING
                || outputType == OutputType.GRAPH_NODE_FINISHED) {
            // 模型结束或图节点事件，不切换阶段
            return Optional.empty();
        } else if (outputType == null) {
            // outputType 缺失时，按消息形状兜底：有文本无 toolCalls 视为思考/响应
            if (message != null && hasText(message) && !message.hasToolCalls()) {
                newPhase = seenTool ? Phase.RESPONDING : Phase.THINKING;
            }
        }

        if (newPhase != null && newPhase != current) {
            current = newPhase;
            return Optional.of(new PhaseHint(newPhase));
        }

        return Optional.empty();
    }

    private static boolean hasText(AssistantMessage message) {
        String text = message.getText();
        return text != null && !text.isEmpty();
    }
}
