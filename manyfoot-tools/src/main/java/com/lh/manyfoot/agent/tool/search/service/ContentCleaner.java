package com.lh.manyfoot.agent.tool.search.service;

import cn.hutool.core.util.StrUtil;
import com.lh.manyfoot.agent.tool.search.domain.PageContent;
import com.lh.manyfoot.agent.tool.search.domain.SearchResult;
import com.lh.manyfoot.config.properties.WebSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 内容清洗器
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentCleaner {

    private final WebSearchConfig config;

    private static final Pattern COOKIE_PATTERN = Pattern.compile(
        "(?i)(cookie|cookies|privacy policy|terms of service|we use cookies|accept all cookies)");
    private static final Pattern SUBSCRIBE_PATTERN = Pattern.compile(
        "(?i)(subscribe|newsletter|sign up|get updates|join our|email address)");
    private static final Pattern SHARE_PATTERN = Pattern.compile(
        "(?i)(share this|share on|facebook|twitter|linkedin|social media)");
    private static final Pattern RELATED_PATTERN = Pattern.compile(
        "(?i)(related articles|recommended|you may also like|more stories|read more)");

    /**
     * 清洗并格式化搜索结果摘要列表
     */
    public String formatSearchResults(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "未找到相关结果。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("搜索到 ").append(results.size()).append(" 条结果：\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append("[").append(i + 1).append("] ").append(r.getTitle()).append("\n");
            sb.append("URL: ").append(r.getUrl()).append("\n");
            if (r.getPublishedAt() != null) {
                sb.append("时间: ").append(formatTime(r.getPublishedAt())).append("\n");
            }
            String snippet = cleanSnippet(r.getSnippet());
            sb.append("摘要: ").append(snippet).append("\n\n");
        }

        return sb.toString().trim();
    }

    /**
     * 清洗并格式化页面正文内容
     */
    public String formatPageContent(PageContent pageContent) {
        if (pageContent == null || !pageContent.isSuccess()) {
            return "页面内容提取失败" + (pageContent != null ? ": " + pageContent.getErrorMessage() : "") + "。";
        }

        String content = cleanContent(pageContent.getContent());
        StringBuilder sb = new StringBuilder();
        sb.append("[来源: ").append(pageContent.getSource());
        if (pageContent.getPublishedAt() != null) {
            sb.append(" | ").append(formatTime(pageContent.getPublishedAt()));
        }
        sb.append("]\n");
        sb.append(content);

        return sb.toString();
    }

    /**
     * 合并多个页面内容，去重并标注来源
     */
    public String formatBatchContents(List<PageContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return "未提取到任何页面内容。";
        }

        Set<String> seenParagraphs = new HashSet<>();
        StringBuilder sb = new StringBuilder();

        for (PageContent content : contents) {
            if (!content.isSuccess()) {
                continue;
            }

            String cleaned = cleanContent(content.getContent());
            String[] paragraphs = cleaned.split("\n\n+");
            StringBuilder uniqueContent = new StringBuilder();

            for (String para : paragraphs) {
                String normalized = para.trim().toLowerCase().replaceAll("\\s+", " ");
                if (normalized.length() < 20) {
                    uniqueContent.append(para).append("\n\n");
                    continue;
                }
                if (!seenParagraphs.contains(normalized)) {
                    seenParagraphs.add(normalized);
                    uniqueContent.append(para).append("\n\n");
                }
            }

            if (uniqueContent.length() > 0) {
                sb.append("[来源: ").append(content.getSource());
                sb.append(" | ").append(content.getUrl()).append("]\n");
                sb.append(uniqueContent.toString().trim()).append("\n\n");
            }
        }

        return sb.toString().trim();
    }

    private String cleanSnippet(String snippet) {
        if (StrUtil.isBlank(snippet)) return "";
        String cleaned = snippet.replaceAll("\s+", " ").trim();
        // 限制摘要长度
        if (cleaned.length() > 500) {
            cleaned = cleaned.substring(0, 500) + "...";
        }
        return cleaned;
    }

    private String cleanContent(String content) {
        if (StrUtil.isBlank(content)) return "";

        // 1. 去除 Cookie 提示
        content = COOKIE_PATTERN.matcher(content).replaceAll("");
        // 2. 去除订阅提示
        content = SUBSCRIBE_PATTERN.matcher(content).replaceAll("");
        // 3. 去除社交分享
        content = SHARE_PATTERN.matcher(content).replaceAll("");
        // 4. 去除相关推荐
        content = RELATED_PATTERN.matcher(content).replaceAll("");

        // 5. 压缩空白
        content = content.replaceAll("\n{3,}", "\n\n");
        content = content.replaceAll("[ \t]+", " ").trim();

        return content;
    }

    private String formatTime(Instant instant) {
        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
        } catch (Exception e) {
            return instant.toString();
        }
    }
}
