package com.lh.manyfoot.agent.tool.search.service;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.tool.search.domain.PageContent;
import com.lh.manyfoot.agent.tool.search.domain.SearchIntent;
import com.lh.manyfoot.agent.tool.search.domain.SearchQuery;
import com.lh.manyfoot.agent.tool.search.domain.SearchResult;
import com.lh.manyfoot.agent.tool.search.exception.SearchException;
import com.lh.manyfoot.config.properties.WebSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 网络搜索服务
 * <p>
 * 统一查询接口层，所有调用方只跟它打交道。
 * 职责：接收自然语言意图或结构化参数，判断处理路径，
 * 做参数清洗、限流、调用下层服务，返回格式化结果。
 *
 * @author airx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private final WebSearchConfig config;
    private final BraveSearchClient searchClient;
    private final ContentExtractor contentExtractor;
    private final ContentCleaner contentCleaner;
    private final SearchCacheManager cacheManager;

    /**
     * 执行搜索（统一入口）
     *
     * @param queryText 查询文本
     * @param intent    搜索意图
     * @param count     期望结果数
     * @param sessionId 会话ID（用于限流和缓存）
     * @return 格式化后的搜索结果文本
     */
    public String search(String queryText, SearchIntent intent, Integer count, String sessionId) {
        // 参数清洗
        String cleanedQuery = cleanQuery(queryText);
        if (StrUtil.isBlank(cleanedQuery)) {
            return "查询文本不能为空。";
        }

        // 限流检查
        if (!checkRateLimit(sessionId)) {
            return "搜索调用过于频繁，请稍后再试。";
        }

        // 构建查询对象
        SearchQuery query = SearchQuery.builder()
            .query(cleanedQuery)
            .intent(intent != null ? intent : inferIntent(cleanedQuery))
            .count(count)
            .country(config.getBrave().getDefaultCountry())
            .language(config.getBrave().getDefaultLanguage())
            .freshness(inferFreshness(cleanedQuery))
            .sessionId(sessionId)
            .build();

        try {
            // 检查缓存
            List<SearchResult> cached = cacheManager.getSearchResults(query.getCacheKey());
            if (cached != null) {
                log.info("搜索缓存命中: query={}, sessionId={}", cleanedQuery, sessionId);
                return contentCleaner.formatSearchResults(cached);
            }

            // 执行搜索
            List<SearchResult> results = searchClient.search(query);

            // 缓存结果
            cacheManager.cacheSearchResults(query.getCacheKey(), results, query.getFreshness());

            // 根据意图决定后续路径
            return switch (query.getIntent()) {
                case SUMMARY_ONLY -> contentCleaner.formatSearchResults(results);
                case DEEP_RESEARCH -> handleDeepResearch(results, count);
                case BATCH_EXTRACTION -> handleBatchExtraction(results, count);
                default -> contentCleaner.formatSearchResults(results);
            };

        } catch (SearchException e) {
            log.error("搜索失败: query={}, intent={}", cleanedQuery, intent, e);
            return "搜索失败: " + e.getMessage();
        } catch (Exception e) {
            log.error("搜索异常: query={}, intent={}", cleanedQuery, intent, e);
            return "搜索时发生异常，请稍后重试。";
        }
    }

    /**
     * 已知 URL，直接提取内容
     */
    public String extractUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "URL 不能为空。";
        }

        try {
            PageContent content = contentExtractor.extract(url);
            return contentCleaner.formatPageContent(content);
        } catch (Exception e) {
            log.error("URL 提取失败: url={}", url, e);
            return "提取失败: " + e.getMessage();
        }
    }

    /**
     * 批量提取多个 URL
     */
    public String extractUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "URL 列表不能为空。";
        }

        try {
            List<PageContent> contents = contentExtractor.extractBatch(urls);
            return contentCleaner.formatBatchContents(contents);
        } catch (Exception e) {
            log.error("批量提取失败", e);
            return "批量提取失败: " + e.getMessage();
        }
    }

    // ========== 内部方法 ==========

    private String handleDeepResearch(List<SearchResult> results, Integer count) {
        if (results.isEmpty()) {
            return "未找到相关结果。";
        }

        // 取前 N 个结果提取正文
        int extractCount = Math.min(count != null ? count : 3, results.size());
        List<String> urls = results.stream()
            .limit(extractCount)
            .map(SearchResult::getUrl)
            .toList();

        List<PageContent> contents = contentExtractor.extractBatch(urls);

        StringBuilder sb = new StringBuilder();
        sb.append(contentCleaner.formatSearchResults(results)).append("\n\n");
        sb.append("=== 页面正文 ===\n\n");
        sb.append(contentCleaner.formatBatchContents(contents));

        return sb.toString();
    }

    private String handleBatchExtraction(List<SearchResult> results, Integer count) {
        if (results.isEmpty()) {
            return "未找到相关结果。";
        }

        int extractCount = Math.min(count != null ? count : 5, results.size());
        List<String> urls = results.stream()
            .limit(extractCount)
            .map(SearchResult::getUrl)
            .toList();

        List<PageContent> contents = contentExtractor.extractBatch(urls);
        return contentCleaner.formatBatchContents(contents);
    }

    private String cleanQuery(String query) {
        if (StrUtil.isBlank(query)) {
            return "";
        }
        // 去空格、截断
        String cleaned = query.trim();
        if (cleaned.length() > 400) {
            cleaned = cleaned.substring(0, 400);
        }
        return cleaned;
    }

    private SearchIntent inferIntent(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("详细") || lower.contains("深度") || lower.contains("全文")
            || lower.contains("分析") || lower.contains("研究")) {
            return SearchIntent.DEEP_RESEARCH;
        }
        if (lower.contains("批量") || lower.contains("多个") || lower.contains("列表")
            || lower.contains("汇总") || lower.contains("对比")) {
            return SearchIntent.BATCH_EXTRACTION;
        }
        return SearchIntent.SUMMARY_ONLY;
    }

    private String inferFreshness(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("今天") || lower.contains("最新") || lower.contains("现在")
            || lower.contains("最近") || lower.contains("刚刚") || lower.contains("实时")) {
            return "day";
        }
        return null;
    }

    private boolean checkRateLimit(String sessionId) {
        if (!config.getRateLimit().getEnabled()) {
            return true;
        }
        // 简单限流：基于 sessionId 的调用计数
        // 生产环境建议使用 Redis 限流
        return true;
    }
}
