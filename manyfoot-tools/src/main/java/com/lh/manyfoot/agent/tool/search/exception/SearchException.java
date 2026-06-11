package com.lh.manyfoot.agent.tool.search.exception;

/**
 * 搜索工具异常
 * <p>
 * 搜索层、内容提取层、内容清洗层抛出的异常统一包装为 SearchException，
 * 便于 Agent 层识别和处理。
 *
 * @author airx
 */
public class SearchException extends RuntimeException {

    /**
     * 错误类型分类
     */
    private final ErrorType errorType;

    public SearchException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public SearchException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * 搜索错误类型
     */
    public enum ErrorType {
        /**
         * 配置错误（如缺少 API Key）
         */
        CONFIG_ERROR,

        /**
         * 搜索 API 调用失败
         */
        SEARCH_API_ERROR,

        /**
         * 内容提取失败
         */
        EXTRACTION_ERROR,

        /**
         * 限流触发
         */
        RATE_LIMITED,

        /**
         * 参数不合法
         */
        INVALID_PARAMETER,

        /**
         * 网络超时
         */
        TIMEOUT,

        /**
         * 未知错误
         */
        UNKNOWN
    }
}
