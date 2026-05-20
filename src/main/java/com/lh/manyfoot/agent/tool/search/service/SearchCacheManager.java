package com.lh.manyfoot.agent.tool.search.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.lh.manyfoot.agent.tool.search.domain.PageContent;
import com.lh.manyfoot.agent.tool.search.domain.SearchResult;
import com.lh.manyfoot.config.properties.WebSearchConfig;
import com.lh.manyfoot.service.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 搜索缓存管理器
 *
 * @author airx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchCacheManager {

    private final WebSearchConfig config;

    /**
     * 获取搜索结果缓存
     */
    @SuppressWarnings("unchecked")
    public List<SearchResult> getSearchResults(String cacheKey) {
        if (!config.getCache().getEnabled()) {
            return null;
        }
        String key = buildSearchKey(cacheKey);
        try {
            List<SearchResult> results = RedisUtils.getCacheObject(key);
            if (results != null) {
                log.debug("搜索缓存命中: key={}", key);
            }
            return results;
        } catch (Exception e) {
            log.warn("读取搜索缓存失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 缓存搜索结果
     */
    public void cacheSearchResults(String cacheKey, List<SearchResult> results, String freshness) {
        if (!config.getCache().getEnabled() || results == null || results.isEmpty()) {
            return;
        }
        String key = buildSearchKey(cacheKey);
        Duration ttl = resolveSearchTtl(freshness);
        try {
            RedisUtils.setCacheObject(key, results, ttl);
            log.debug("搜索缓存已写入: key={}, ttl={}min", key, ttl.toMinutes());
        } catch (Exception e) {
            log.warn("写入搜索缓存失败: key={}", key, e);
        }
    }

    /**
     * 获取页面内容缓存
     */
    public PageContent getPageContent(String url, String mode) {
        if (!config.getCache().getEnabled()) {
            return null;
        }
        String key = buildContentKey(url, mode);
        try {
            PageContent content = RedisUtils.getCacheObject(key);
            if (content != null) {
                log.debug("页面缓存命中: key={}", key);
            }
            return content;
        } catch (Exception e) {
            log.warn("读取页面缓存失败: key={}", key, e);
            return null;
        }
    }

    /**
     * 缓存页面内容
     */
    public void cachePageContent(PageContent pageContent) {
        if (!config.getCache().getEnabled() || pageContent == null || !pageContent.isSuccess()) {
            return;
        }
        String key = buildContentKey(pageContent.getUrl(), pageContent.getExtractionMode());
        Duration ttl = resolveContentTtl(pageContent.getSource());
        try {
            RedisUtils.setCacheObject(key, pageContent, ttl);
            log.debug("页面缓存已写入: key={}, ttl={}min", key, ttl.toMinutes());
        } catch (Exception e) {
            log.warn("写入页面缓存失败: key={}", key, e);
        }
    }

    private String buildSearchKey(String cacheKey) {
        return config.getCache().getKeyPrefix() + ":results:" + DigestUtil.md5Hex(cacheKey);
    }

    private String buildContentKey(String url, String mode) {
        return config.getCache().getKeyPrefix() + ":content:" + DigestUtil.md5Hex(url + "|" + mode);
    }

    private Duration resolveSearchTtl(String freshness) {
        WebSearchConfig.Cache cache = config.getCache();
        if ("day".equalsIgnoreCase(freshness)) {
            return Duration.ofMinutes(cache.getFreshTtlMinutes());
        }
        return Duration.ofMinutes(cache.getGeneralTtlMinutes());
    }

    private Duration resolveContentTtl(String source) {
        WebSearchConfig.Cache cache = config.getCache();
        if (source == null) {
            return Duration.ofMinutes(cache.getPageContentTtlMinutes());
        }
        String domain = source.toLowerCase();
        if (domain.contains("arxiv") || domain.contains("docs") || domain.contains("wiki")) {
            return Duration.ofMinutes(cache.getDocumentContentTtlMinutes());
        }
        if (domain.contains("news") || domain.contains("cnn") || domain.contains("bbc")
            || domain.contains("reuters") || domain.contains("bloomberg")) {
            return Duration.ofMinutes(cache.getNewsContentTtlMinutes());
        }
        return Duration.ofMinutes(cache.getPageContentTtlMinutes());
    }
}
