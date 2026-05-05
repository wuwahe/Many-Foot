package com.lh.manyfoot.controller.dto;

import lombok.Data;

/**
 * AI 对话请求体
 *
 * @author airx
 */
@Data
public class ChatRequest {

    /**
     * 会话 ID，用于标识一次对话上下文。
     * 前端应生成并持久化，同一轮对话保持不变。
     */
    private String sessionId;

    /**
     * 用户消息内容
     */
    private String message;
}
