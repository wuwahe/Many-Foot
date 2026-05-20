package com.lh.manyfoot.agent.tool.search.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 页面内容提取结果
 * <p>
 * 内容提取层（web_fetch 或 Jina Reader）的输出，
 * 包含清洁后的 Markdown 正文、元信息和提取模式。
 *
 * @author airx
 */
@Data
@Builder
public class PageContent {

    /**
     * 原始 URL
     */
    private String url;

    /**
     * 页面标题
     */
    private String title;

    /**
     * 提取后的 Markdown 正文（已截断）
     */
    private String content;

    /**
     * 来源域名
     */
    private String source;

    /**
     * 发布时间
     */
    private Instant publishedAt;

    /**
     * 提取模式：WEB_FETCH / JINA_STANDARD / JINA_STREAM
     */
    private String extractionMode;

    /**
     * 内容长度（字符数）
     */
    private Integer contentLength;

    /**
     * 提取是否成功
     */
    private boolean success;

    /**
     * 失败原因（success=false 时）
     */
    private String errorMessage;

    /**
     * 获取用于缓存的指纹 key
     */
    public String getCacheKey() {
        return url + "|" + extractionMode;
    }
}
