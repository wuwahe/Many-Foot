package com.lh.manyfoot.agent.exception;

/**
 * 智能体执行异常
 *
 * @author airx
 */
public class AgentExecutionException extends RuntimeException {

    public AgentExecutionException(String message) {
        super(message);
    }

    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentExecutionException(Throwable cause) {
        super(cause);
    }
}
