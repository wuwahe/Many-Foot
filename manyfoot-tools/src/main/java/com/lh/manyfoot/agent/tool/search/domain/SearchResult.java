package com.lh.manyfoot.agent.tool.search.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 标准化搜索结果项
 * <p>
 * 由搜索层（Brave Search 或 Jina Search）返回的统一格式。
 * 过滤掉 url 或 snippet 为空的无效条目。
 *
 * @author airx
 */
@Data
@Builder
public class SearchResult {

    /**
     * 结果标题
     */
    private String title;

    /**
     * 结果 URL
     */
    private String url;

    /**
     * 结果摘要（已合并 extra_snippets）
     */
    private String snippet;

    /**
     * 来源域名
     */
    private String source;

    /**
     * 发布时间（绝对时间戳）
     */
    private Instant publishedAt;

    /**
     * 原始来源（标记来自 Brave 或 Jina Fallback）
     */
    private String origin;

    /**
     * 判断是否为有效结果
     */
    public boolean isValid() {
        return url != null && !url.isBlank()
            && snippet != null && !snippet.isBlank();
    }
}
