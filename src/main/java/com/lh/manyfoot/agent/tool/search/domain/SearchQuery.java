package com.lh.manyfoot.agent.tool.search.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 搜索查询参数
 * <p>
 * 统一查询接口层接收的参数，包含查询文本、意图分类、分页、地域、时效性等。
 * 参数在调用搜索层前会做清洗（截断、去空格等）。
 *
 * @author airx
 */
@Data
@Builder
public class SearchQuery {

    /**
     * 查询文本（已清洗，最多 400 字符）
     */
    private String query;

    /**
     * 搜索意图分类
     */
    private SearchIntent intent;

    /**
     * 期望返回结果数
     */
    private Integer count;

    /**
     * 国家代码（如 US、CN）
     */
    private String country;

    /**
     * 语言代码（如 en、zh）
     */
    private String language;

    /**
     * 时效性筛选
     * <p>
     * day / week / month / year 等，或 null 表示不限制
     */
    private String freshness;

    /**
     * 历史查询起始日期（与 dateBefore 必须同时传）
     */
    private Instant dateAfter;

    /**
     * 历史查询结束日期（与 dateAfter 必须同时传）
     */
    private Instant dateBefore;

    /**
     * 是否只需要摘要（不需要进入内容提取层）
     */
    private boolean summaryOnly;

    /**
     * 会话 ID（用于限流和缓存）
     */
    private String sessionId;

    /**
     * 获取用于缓存的指纹 key
     * <p>
     * 由 query + country + language + freshness + date_range 哈希生成
     */
    public String getCacheKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(query.trim().toLowerCase());
        if (country != null) sb.append("|").append(country);
        if (language != null) sb.append("|").append(language);
        if (freshness != null) sb.append("|").append(freshness);
        if (dateAfter != null) sb.append("|").append(dateAfter.toEpochMilli());
        if (dateBefore != null) sb.append("|").append(dateBefore.toEpochMilli());
        return sb.toString();
    }
}
