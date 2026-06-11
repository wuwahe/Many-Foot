package com.lh.manyfoot.agent.tool.search.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.lh.manyfoot.agent.tool.search.domain.PageContent;
import com.lh.manyfoot.agent.tool.search.exception.SearchException;
import com.lh.manyfoot.config.properties.WebSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.concurrent.*;

/**
 * 内容提取器
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentExtractor {

    private final WebSearchConfig config;
    private final SearchCacheManager cacheManager;

    private static final String MODE_WEB_FETCH = "WEB_FETCH";
    private static final String MODE_JINA_STANDARD = "JINA_STANDARD";
    private static final String MODE_JINA_STREAM = "JINA_STREAM";

    /**
     * 提取单个 URL 的内容
     */
    public PageContent extract(String url) {
        // 检查缓存
        PageContent cached = cacheManager.getPageContent(url, MODE_WEB_FETCH);
        if (cached != null) {
            return cached;
        }

        // 判断提取路径
        if (shouldUseJinaDirectly(url)) {
            return extractWithJina(url, MODE_JINA_STREAM);
        }

        // 先尝试 web_fetch
        PageContent content = extractWithWebFetch(url);
        if (!content.isSuccess() || content.getContentLength() < config.getExtraction().getMinContentLength()) {
            log.debug("web_fetch 内容不足或失败，降级到 Jina Reader: url={}", url);
            content = extractWithJina(url, MODE_JINA_STANDARD);
        }

        if (content.isSuccess()) {
            cacheManager.cachePageContent(content);
        }
        return content;
    }

    /**
     * 批量并发提取多个 URL
     */
    public List<PageContent> extractBatch(List<String> urls) {
        int maxConcurrency = config.getExtraction().getMaxConcurrency();
        ExecutorService executor = Executors.newFixedThreadPool(maxConcurrency);
        List<Future<PageContent>> futures = new java.util.ArrayList<>();

        for (String url : urls) {
            futures.add(executor.submit(() -> extract(url)));
        }

        List<PageContent> results = new java.util.ArrayList<>();
        for (Future<PageContent> future : futures) {
            try {
                PageContent content = future.get(60, TimeUnit.SECONDS);
                results.add(content);
            } catch (Exception e) {
                log.warn("批量提取单个 URL 失败", e);
                results.add(PageContent.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
            }
        }

        executor.shutdown();
        return results;
    }

    private boolean shouldUseJinaDirectly(String url) {
        List<String> jsHeavyDomains = config.getExtraction().getJsHeavyDomains();
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;
            String domain = host.toLowerCase();
            for (String jsDomain : jsHeavyDomains) {
                if (domain.contains(jsDomain.toLowerCase())) {
                    return true;
                }
            }
            // PDF 直接走 Jina
            if (url.toLowerCase().endsWith(".pdf")) {
                return true;
            }
        } catch (Exception e) {
            log.debug("URL 解析失败: {}", url);
        }
        return false;
    }

    private PageContent extractWithWebFetch(String url) {
        try {
            HttpRequest request = HttpRequest.get(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; ManyFootBot/1.0)")
                .timeout(config.getExtraction().getWebFetchTimeoutSeconds() * 1000);

            String html;
            try (HttpResponse response = request.execute()) {
                html = response.body();
            }

            // 简单提取：去掉 script/style，取 body 文本
            String text = cleanHtml(html);
            int maxChars = config.getExtraction().getMaxContentChars();
            if (text.length() > maxChars) {
                text = truncate(text, maxChars);
            }

            return PageContent.builder()
                .url(url)
                .title(extractTitle(html))
                .content(text)
                .source(extractDomain(url))
                .extractionMode(MODE_WEB_FETCH)
                .contentLength(text.length())
                .success(true)
                .build();

        } catch (Exception e) {
            log.warn("web_fetch 提取失败: url={}", url, e);
            return PageContent.builder()
                .url(url)
                .success(false)
                .errorMessage("web_fetch 失败: " + e.getMessage())
                .build();
        }
    }

    private PageContent extractWithJina(String url, String mode) {
        WebSearchConfig.Jina jina = config.getJina();
        String jinaUrl = jina.getReaderUrl() + url;

        try {
            HttpRequest request = HttpRequest.get(jinaUrl)
                .timeout(jina.getStreamTimeoutSeconds() * 1000);

            if (StrUtil.isNotBlank(jina.getApiKey())) {
                request.header("Authorization", "Bearer " + jina.getApiKey());
            }

            if (MODE_JINA_STREAM.equals(mode)) {
                request.header("Accept", "text/event-stream");
            }

            String responseBody;
            try (HttpResponse response = request.execute()) {
                responseBody = response.body();
            }

            // Jina Reader 返回的是清洁 Markdown
            int maxChars = config.getExtraction().getMaxContentChars();
            String content = responseBody;
            if (content.length() > maxChars) {
                content = truncate(content, maxChars);
            }

            return PageContent.builder()
                .url(url)
                .content(content)
                .source(extractDomain(url))
                .extractionMode(mode)
                .contentLength(content.length())
                .success(true)
                .build();

        } catch (Exception e) {
            log.warn("Jina Reader 提取失败: url={}, mode={}", url, mode, e);
            return PageContent.builder()
                .url(url)
                .success(false)
                .errorMessage("Jina Reader 失败: " + e.getMessage())
                .build();
        }
    }

    private String cleanHtml(String html) {
        if (StrUtil.isBlank(html)) return "";
        // 移除 script 和 style
        String text = html.replaceAll("<script[^\u003e]*\u003e[\\s\\S]*?\u003c/script\u003e", " ");
        text = text.replaceAll("<style[^\u003e]*\u003e[\\s\\S]*?\u003c/style\u003e", " ");
        // 移除 HTML 标签
        text = text.replaceAll("<[^\u003e]+\u003e", " ");
        // 解码 HTML 实体
        text = text.replace("\u0026nbsp;", " ")
            .replace("\u0026lt;", "<")
            .replace("\u0026gt;", ">")
            .replace("\u0026amp;", "\u0026");
        // 压缩空白
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private String extractTitle(String html) {
        if (StrUtil.isBlank(html)) return "";
        int start = html.indexOf("<title>");
        int end = html.indexOf("</title>");
        if (start != -1 && end != -1 && end > start) {
            return html.substring(start + 7, end).trim();
        }
        return "";
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

    private String truncate(String text, int maxChars) {
        WebSearchConfig.Cleaning cleaning = config.getCleaning();
        int head = cleaning.getHeadChars();
        int tail = cleaning.getTailChars();

        if (head + tail >= text.length()) {
            return text;
        }

        return text.substring(0, head) + "\n\n... [内容已截断，中间部分省略] ...\n\n" + text.substring(text.length() - tail);
    }
}
