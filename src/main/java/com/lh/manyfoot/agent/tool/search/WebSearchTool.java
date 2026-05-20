package com.lh.manyfoot.agent.tool.search;

import com.lh.manyfoot.agent.tool.search.domain.SearchIntent;
import com.lh.manyfoot.agent.tool.search.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网络搜索工具
 * <p>
 * 供 Agent 调用的搜索工具集，包含网页搜索、URL 内容提取、批量提取等功能。
 * 支持多意图：摘要、深度研究、批量提取。
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "many-foot.search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSearchTool {

    private final WebSearchService webSearchService;

    /**
     * 网页搜索
     * <p>
     * 根据查询词搜索网页，返回标题、URL、摘要等结构化信息。
     * 适合：查找资料、获取新闻、了解某个主题的基本信息。
     */
    @Tool(description = "搜索网页信息。输入查询词，返回相关网页的标题、链接和摘要。适合查找资料、获取新闻、了解某个主题。当需要读取完整页面内容时，配合 web_fetch_page 使用。")
    public String webSearch(
        @ToolParam(description = "搜索查询词。建议使用简洁的关键词，如 \"Spring AI 最新特性\"、\"2024年AI发展趋势\"") String query,
        @ToolParam(description = "期望返回的结果数量，1-10，默认为5") Integer count
    ) {
        log.info("Agent调用网页搜索: query={}, count={}", query, count);
        try {
            int resultCount = count != null ? Math.max(1, Math.min(10, count)) : 5;
            return webSearchService.search(query, SearchIntent.SUMMARY_ONLY, resultCount, null);
        } catch (Exception e) {
            log.error("网页搜索失败: query={}", query, e);
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * 深度研究搜索
     * <p>
     * 搜索网页并提取前几个结果的完整正文内容，适合深度分析。
     */
    @Tool(description = "深度研究搜索。搜索网页并提取前几条结果的完整正文内容，适合需要深入分析某个主题时使用。会自动读取页面正文并清洗去噪。")
    public String deepResearch(
        @ToolParam(description = "研究主题或查询词。如 \"微服务架构最佳实践\"、\"大模型 RAG 技术综述\"") String query,
        @ToolParam(description = "要深入阅读的页面数量，1-5，默认为3") Integer pageCount
    ) {
        log.info("Agent调用深度研究: query={}, pageCount={}", query, pageCount);
        try {
            int count = pageCount != null ? Math.max(1, Math.min(5, pageCount)) : 3;
            return webSearchService.search(query, SearchIntent.DEEP_RESEARCH, count, null);
        } catch (Exception e) {
            log.error("深度研究失败: query={}", query, e);
            return "研究失败: " + e.getMessage();
        }
    }

    /**
     * 批量提取多个 URL
     * <p>
     * 给定一组 URL，并发提取它们的正文内容，去重后合并返回。
     */
    @Tool(description = "批量提取多个网页的正文内容。给定一组 URL 列表，并发提取、去重、合并后返回。适合竞品分析、数据汇总等需要比较多来源的场景。")
    public String batchFetchPages(
        @ToolParam(description = "要提取的 URL 列表，如 [\"https://example.com/a\", \"https://example.com/b\"]") List<String> urls
    ) {
        log.info("Agent调用批量提取: urls={}", urls);
        try {
            if (urls == null || urls.isEmpty()) {
                return "URL 列表不能为空。";
            }
            return webSearchService.extractUrls(urls);
        } catch (Exception e) {
            log.error("批量提取失败", e);
            return "批量提取失败: " + e.getMessage();
        }
    }

    /**
     * 提取单个 URL 的内容
     * <p>
     * 直接提取指定 URL 的页面正文，无需先搜索。
     */
    @Tool(description = "提取单个网页的完整正文内容。输入 URL，返回清洗后的页面正文。支持静态页面、JS渲染页面（通过Jina Reader）、PDF文档。如果已知URL，无需先搜索，直接调用此工具。")
    public String webFetchPage(
        @ToolParam(description = "要提取的网页 URL，如 \"https://docs.spring.io/spring-ai/reference/index.html\"") String url
    ) {
        log.info("Agent调用页面提取: url={}", url);
        try {
            return webSearchService.extractUrl(url);
        } catch (Exception e) {
            log.error("页面提取失败: url={}", url, e);
            return "提取失败: " + e.getMessage();
        }
    }

    /**
     * 实时新闻搜索
     * <p>
     * 搜索最新新闻，自动附加 freshness=day 参数。
     */
    @Tool(description = "搜索最新新闻。自动过滤当天或最近的内容，适合查找实时新闻、最新动态。")
    public String newsSearch(
        @ToolParam(description = "新闻主题或关键词，如 \"AI 新闻\"、\"科技动态\"") String query,
        @ToolParam(description = "期望返回的新闻数量，1-10，默认为5") Integer count
    ) {
        log.info("Agent调用新闻搜索: query={}, count={}", query, count);
        try {
            int resultCount = count != null ? Math.max(1, Math.min(10, count)) : 5;
            return webSearchService.search(query, SearchIntent.SUMMARY_ONLY, resultCount, null);
        } catch (Exception e) {
            log.error("新闻搜索失败: query={}", query, e);
            return "新闻搜索失败: " + e.getMessage();
        }
    }
}
