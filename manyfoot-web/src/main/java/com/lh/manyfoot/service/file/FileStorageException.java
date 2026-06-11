package com.lh.manyfoot.service.file;

import org.springframework.http.HttpStatus;

/**
 * 文件存储业务异常
 * <p>
 * 携带 HTTP 状态码，便于 Controller 层直接映射为对应的 HTTP 响应。
 *
 * @author airx
 */
public class FileStorageException extends RuntimeException {

    private final HttpStatus status;

    public FileStorageException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public FileStorageException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 请求参数错误（400）
     */
    public static FileStorageException badRequest(String message) {
        return new FileStorageException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 资源未找到（404）
     */
    public static FileStorageException notFound(String message) {
        return new FileStorageException(HttpStatus.NOT_FOUND, message);
    }

    /**
     * 请求体过大（413）
     */
    public static FileStorageException payloadTooLarge(String message) {
        return new FileStorageException(HttpStatus.PAYLOAD_TOO_LARGE, message);
    }

    /**
     * 服务不可用（503）
     */
    public static FileStorageException serviceUnavailable(String message) {
        return new FileStorageException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    /**
     * 服务器内部错误（500）
     */
    public static FileStorageException serverError(String message) {
        return new FileStorageException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * 服务器内部错误（500），带原因
     */
    public static FileStorageException serverError(String message, Throwable cause) {
        return new FileStorageException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
