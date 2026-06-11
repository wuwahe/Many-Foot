package com.lh.manyfoot.agent.tool.search.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lh.manyfoot.agent.tool.search.domain.SearchIntent;
import com.lh.manyfoot.agent.tool.search.domain.SearchQuery;
import com.lh.manyfoot.agent.tool.search.domain.SearchResult;
import com.lh.manyfoot.agent.tool.search.exception.SearchException;
import com.lh.manyfoot.config.properties.WebSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Brave Search 客户端
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BraveSearchClient {

    private final WebSearchConfig config;

    /**
     * 执行搜索
     */
    public List<SearchResult> search(SearchQuery query) {
        WebSearchConfig.Brave brave = config.getBrave();

        if (StrUtil.isBlank(brave.getApiKey())) {
            log.warn("Brave API Key 未配置，尝试 Jina Fallback");
            return searchWithJinaFallback(query);
        }

        String url = brave.getBaseUrl();
        int count = Math.min(query.getCount() != null ? query.getCount() : brave.getDefaultCount(), brave.getMaxCount());
        String country = query.getCountry() != null ? query.getCountry() : brave.getDefaultCountry();
        String language = query.getLanguage() != null ? query.getLanguage() : brave.getDefaultLanguage();

        try {
            HttpRequest request = HttpRequest.get(url)
                .header("X-Subscription-Token", brave.getApiKey())
                .header("Accept", "application/json")
                .form("q", query.getQuery())
                .form("count", String.valueOf(count))
                .form("country", country)
                .form("search_lang", language)
                .timeout(brave.getTimeoutSeconds() * 1000);

            if (StrUtil.isNotBlank(query.getFreshness())) {
                request.form("freshness", query.getFreshness());
            }

            String responseBody = executeWithRetry(request, brave.getMaxRetries());
            List<SearchResult> results = parseBraveResponse(responseBody);
            log.info("Brave 搜索完成: query={}, results={}", query.getQuery(), results.size());
            return results;

        } catch (SearchException e) {
            if (e.getErrorType() == SearchException.ErrorType.SEARCH_API_ERROR
                || e.getErrorType() == SearchException.ErrorType.TIMEOUT) {
                log.warn("Brave 搜索失败，降级到 Jina Fallback: {}", e.getMessage());
                return searchWithJinaFallback(query);
            }
            throw e;
        }
    }

    /**
     * Jina Reader 搜索 Fallback
     */
    private List<SearchResult> searchWithJinaFallback(SearchQuery query) {
        WebSearchConfig.Jina jina = config.getJina();
        String url = jina.getSearchUrl() + query.getQuery();

        try {
            HttpRequest request = HttpRequest.get(url)
                .timeout(jina.getTimeoutSeconds() * 1000);

            if (StrUtil.isNotBlank(jina.getApiKey())) {
                request.header("Authorization", "Bearer " + jina.getApiKey());
            }

            String responseBody = request.execute().body();
            List<SearchResult> results = parseJinaSearchResponse(responseBody);
            log.info("Jina 搜索 Fallback 完成: query={}, results={}", query.getQuery(), results.size());
            return results;

        } catch (Exception e) {
            log.error("Jina 搜索 Fallback 也失败", e);
            return Collections.emptyList();
        }
    }

    private String executeWithRetry(HttpRequest request, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= maxRetries) {
            try (HttpResponse response = request.execute()) {
                int status = response.getStatus();
                String body = response.body();

                if (status == 200) {
                    return body;
                }

                if (status == 401) {
                    throw new SearchException("Brave API Key 无效或已过期", SearchException.ErrorType.CONFIG_ERROR);
                }
                if (status == 422) {
                    throw new SearchException("请求参数错误: " + body, SearchException.ErrorType.INVALID_PARAMETER);
                }
                if (status == 429) {
                    lastException = new SearchException("Brave 限流 (429)", SearchException.ErrorType.RATE_LIMITED);
                } else if (status >= 500) {
                    lastException = new SearchException("Brave 服务端错误 (" + status + ")", SearchException.ErrorType.SEARCH_API_ERROR);
                } else {
                    throw new SearchException("Brave 请求失败: " + status + " - " + body, SearchException.ErrorType.SEARCH_API_ERROR);
                }

            } catch (SearchException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
            }

            attempts++;
            if (attempts <= maxRetries) {
                long backoff = (long) Math.pow(2, attempts) * 1000;
                log.warn("Brave 搜索重试 {}/{}, 等待 {}ms", attempts, maxRetries, backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        throw new SearchException("Brave 搜索在 " + maxRetries + " 次重试后仍然失败: " + lastException.getMessage(),
            lastException, SearchException.ErrorType.SEARCH_API_ERROR);
    }

    private List<SearchResult> parseBraveResponse(String json) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JSONObject root = JSONUtil.parseObj(json);
            JSONArray webResults = root.getJSONObject("web").getJSONArray("results");

            for (int i = 0; i < webResults.size(); i++) {
                JSONObject item = webResults.getJSONObject(i);
                String url = item.getStr("url");
                String title = item.getStr("title");
                String description = item.getStr("description");

                // 合并 extra_snippets
                JSONArray extraSnippets = item.getJSONArray("extra_snippets");
                StringBuilder snippet = new StringBuilder(description != null ? description : "");
                if (extraSnippets != null) {
                    for (int j = 0; j < extraSnippets.size(); j++) {
                        snippet.append(" ").append(extraSnippets.getStr(j));
                    }
                }

                String source = extractDomain(url);
                String age = item.getStr("age");
                Instant publishedAt = parseAge(age);

                SearchResult result = SearchResult.builder()
                    .title(title)
                    .url(url)
                    .snippet(snippet.toString().trim())
                    .source(source)
                    .publishedAt(publishedAt)
                    .origin("Brave")
                    .build();

                if (result.isValid()) {
                    results.add(result);
                }
            }
        } catch (Exception e) {
            log.error("解析 Brave 响应失败", e);
        }
        return results;
    }

    private List<SearchResult> parseJinaSearchResponse(String responseBody) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String[] lines = responseBody.split("\n");
            String currentTitle = "";
            String currentUrl = "";
            StringBuilder currentSnippet = new StringBuilder();

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("Title: ")) {
                    if (StrUtil.isNotBlank(currentUrl)) {
                        addJinaResult(results, currentTitle, currentUrl, currentSnippet.toString());
                    }
                    currentTitle = line.substring(7);
                    currentUrl = "";
                    currentSnippet = new StringBuilder();
                } else if (line.startsWith("URL Source: ")) {
                    currentUrl = line.substring(12);
                } else if (line.startsWith("Markdown Content:")) {
                    // 跳过标记行
                } else if (StrUtil.isNotBlank(line) && !line.startsWith("---")) {
                    currentSnippet.append(line).append("\n");
                }
            }

            if (StrUtil.isNotBlank(currentUrl)) {
                addJinaResult(results, currentTitle, currentUrl, currentSnippet.toString());
            }
        } catch (Exception e) {
            log.error("解析 Jina 搜索响应失败", e);
        }
        return results;
    }

    private void addJinaResult(List<SearchResult> results, String title, String url, String content) {
        String snippet = content.length() > 500 ? content.substring(0, 500) + "..." : content;
        SearchResult result = SearchResult.builder()
            .title(title)
            .url(url)
            .snippet(snippet)
            .source(extractDomain(url))
            .origin("Jina")
            .build();
        if (result.isValid()) {
            results.add(result);
        }
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host != null ? host : "";
        } catch (Exception e) {
            return "";
        }
    }

    private Instant parseAge(String age) {
        if (StrUtil.isBlank(age)) {
            return null;
        }
        try {
            // Brave 返回相对时间如 "1 day ago", "2 hours ago"
            // 简单解析：如果是 "X days ago"，减去 X 天
            age = age.toLowerCase().replace(" ago", "");
            String[] parts = age.split(" ");
            if (parts.length == 2) {
                int value = Integer.parseInt(parts[0]);
                String unit = parts[1];
                Duration duration = switch (unit) {
                    case "second", "seconds" -> Duration.ofSeconds(value);
                    case "minute", "minutes" -> Duration.ofMinutes(value);
                    case "hour", "hours" -> Duration.ofHours(value);
                    case "day", "days" -> Duration.ofDays(value);
                    case "week", "weeks" -> Duration.ofDays(value * 7L);
                    case "month", "months" -> Duration.ofDays(value * 30L);
                    case "year", "years" -> Duration.ofDays(value * 365L);
                    default -> null;
                };
                if (duration != null) {
                    return Instant.now().minus(duration);
                }
            }
        } catch (Exception e) {
            log.debug("解析时间失败: {}", age);
        }
        return null;
    }
}
