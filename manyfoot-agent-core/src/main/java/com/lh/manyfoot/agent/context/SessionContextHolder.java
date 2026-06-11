package com.lh.manyfoot.agent.context;

/**
 * 会话ID上下文持有者
 * <p>
 * 使用 {@link InheritableThreadLocal} 存储当前请求的会话ID，
 * 支持父子线程之间的会话ID传递。
 * <p>
 * <h3>使用场景</h3>
 * <ul>
 *   <li>父代理（Supervisor）执行时设置会话ID</li>
 *   <li>子代理（通过 AgentTool 调用）在子线程中继承父会话ID</li>
 *   <li>确保整个请求链中的会话ID一致性</li>
 * </ul>
 * <p>
 * <h3>线程安全</h3>
 * <ul>
 *   <li>使用 {@link InheritableThreadLocal} 确保子线程继承父线程的会话ID</li>
 *   <li>每个请求线程拥有独立的会话ID存储</li>
 *   <li>支持并发请求，互不干扰</li>
 * </ul>
 * <p>
 * <h3>生命周期管理</h3>
 * <ul>
 *   <li>在请求开始时调用 {@link #setSessionId(String)} 设置会话ID</li>
 *   <li>在请求结束时调用 {@link #clear()} 清理资源，防止内存泄漏</li>
 *   <li>建议在 try-finally 块中使用，确保资源清理</li>
 * </ul>
 *
 * @author airx
 * @see AgentContext
 */
public class SessionContextHolder {

    /**
     * 使用 InheritableThreadLocal 存储会话ID
     * <p>
     * InheritableThreadLocal 的特性：
     * <ul>
     *   <li>父线程设置的值会自动被子线程继承</li>
     *   <li>子线程创建时复制父线程的值</li>
     *   <li>子线程修改值不会影响父线程</li>
     * </ul>
     * <p>
     * 注意：如果使用线程池，线程可能已经存在，不会继承新的值。
     * 在这种情况下，需要在任务提交前设置会话ID。
     */
    private static final InheritableThreadLocal<String> SESSION_ID_HOLDER = new InheritableThreadLocal<>();

    /**
     * 当前请求的 AgentContext，用于 Supervisor 通过 ToolCallback 调用子 Agent 时传递附件等上下文元数据。
     */
    private static final InheritableThreadLocal<AgentContext> AGENT_CONTEXT_HOLDER = new InheritableThreadLocal<>();

    /**
     * 设置当前线程的会话ID
     *
     * @param sessionId 会话ID
     */
    public static void setSessionId(String sessionId) {
        SESSION_ID_HOLDER.set(sessionId);
    }

    /**
     * 设置当前线程的智能体上下文。
     *
     * @param context 智能体上下文
     */
    public static void setAgentContext(AgentContext context) {
        AGENT_CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前线程的智能体上下文。
     *
     * @return 智能体上下文，如果未设置则返回 null
     */
    public static AgentContext getAgentContext() {
        return AGENT_CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前线程的会话ID
     *
     * @return 会话ID，如果未设置则返回 null
     */
    public static String getSessionId() {
        return SESSION_ID_HOLDER.get();
    }

    /**
     * 获取当前线程的会话ID，如果未设置则返回默认值
     *
     * @param defaultValue 默认值
     * @return 会话ID，如果未设置则返回默认值
     */
    public static String getSessionId(String defaultValue) {
        String sessionId = SESSION_ID_HOLDER.get();
        return sessionId != null ? sessionId : defaultValue;
    }

    /**
     * 清除当前线程的会话ID
     * <p>
     * 应在请求结束时调用，防止内存泄漏。
     * 建议在 try-finally 块中使用：
     * <pre>
     * try {
     *     SessionContextHolder.setSessionId(sessionId);
     *     // 执行业务逻辑
     * } finally {
     *     SessionContextHolder.clear();
     * }
     * </pre>
     */
    public static void clear() {
        SESSION_ID_HOLDER.remove();
        AGENT_CONTEXT_HOLDER.remove();
    }

    /**
     * 检查当前线程是否设置了会话ID
     *
     * @return 如果设置了会话ID则返回 true，否则返回 false
     */
    public static boolean hasSessionId() {
        return SESSION_ID_HOLDER.get() != null;
    }
}
