package com.lh.manyfoot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * 网络搜索工具配置
 * <p>
 * 配置搜索层、内容提取层、缓存层等参数。
 * 所有 API Key 必须外部化配置，不进入代码和 git。
 *
 * @author airx
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "many-foot.search")
public class WebSearchConfig {

    /**
     * 是否启用网络搜索工具
     */
    private Boolean enabled = true;

    /**
     * Brave Search API 配置
     */
    private Brave brave = new Brave();

    /**
     * Jina Reader 配置
     */
    private Jina jina = new Jina();

    /**
     * 内容提取配置
     */
    private Extraction extraction = new Extraction();

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    /**
     * 内容清洗配置
     */
    private Cleaning cleaning = new Cleaning();

    // ========== 嵌套配置类 ==========

    @Data
    public static class Brave {
        /**
         * Brave Search API Key，必须外部化配置
         */
        private String apiKey;

        /**
         * API 基础地址
         */
        private String baseUrl = "https://api.search.brave.com/res/v1/web/search";

        /**
         * 默认返回结果数量
         */
        private Integer defaultCount = 5;

        /**
         * 最大返回结果数量
         */
        private Integer maxCount = 10;

        /**
         * 默认国家代码
         */
        private String defaultCountry = "US";

        /**
         * 默认语言代码
         */
        private String defaultLanguage = "en";

        /**
         * 搜索超时时间（秒）
         */
        private Integer timeoutSeconds = 15;

        /**
         * 429/5xx 时指数退避重试次数
         */
        private Integer maxRetries = 3;
    }

    @Data
    public static class Jina {
        /**
         * Jina Reader API Key（可选，无 Key 时限制更严格）
         */
        private String apiKey;

        /**
         * Reader 基础 URL
         */
        private String readerUrl = "https://r.jina.ai/";

        /**
         * 搜索端点（作为 Brave 的 Fallback）
         */
        private String searchUrl = "https://s.jina.ai/";

        /**
         * 请求超时时间（秒）
         */
        private Integer timeoutSeconds = 30;

        /**
         * 流模式超时时间（秒）
         */
        private Integer streamTimeoutSeconds = 60;

        /**
         * 是否启用图片识别（x-with-generated-alt）
         */
        private Boolean imageRecognition = false;
    }

    @Data
    public static class Extraction {
        /**
         * 静态页面最小内容长度，低于此值降级到 Jina Reader
         */
        private Integer minContentLength = 200;

        /**
         * 静态页面提取超时（秒）
         */
        private Integer webFetchTimeoutSeconds = 15;

        /**
         * 需要 JS 渲染的域名黑名单
         */
        private List<String> jsHeavyDomains = List.of(
            "twitter.com", "x.com", "linkedin.com", "facebook.com",
            "instagram.com", "youtube.com", "tiktok.com"
        );

        /**
         * 单次批量提取最大并发数
         */
        private Integer maxConcurrency = 3;

        /**
         * 内容截断保留的最大 token 数（按字符估算）
         */
        private Integer maxContentChars = 12000;
    }

    @Data
    public static class RateLimit {
        /**
         * 是否启用限流
         */
        private Boolean enabled = true;

        /**
         * 单次对话搜索调用上限
         */
        private Integer maxSearchesPerSession = 5;

        /**
         * 单次对话页面提取上限
         */
        private Integer maxExtractionsPerSession = 10;

        /**
         * Jina Reader 无 API Key 时的 RPM 限制
         */
        private Integer jinaRpmWithoutKey = 20;

        /**
         * Jina Reader 有 API Key 时的 RPM 限制
         */
        private Integer jinaRpmWithKey = 100;
    }

    @Data
    public static class Cache {
        /**
         * 是否启用缓存
         */
        private Boolean enabled = true;

        /**
         * Redis key 前缀
         */
        private String keyPrefix = "manyfoot:search";

        /**
         * 一般查询缓存 TTL（分钟）
         */
        private Long generalTtlMinutes = 60L;

        /**
         * 实时查询（freshness=day）缓存 TTL（分钟）
         */
        private Long freshTtlMinutes = 5L;

        /**
         * 历史区间查询缓存 TTL（分钟）
         */
        private Long historicalTtlMinutes = 1440L;

        /**
         * 页面内容缓存 TTL（分钟）
         */
        private Long pageContentTtlMinutes = 120L;

        /**
         * 新闻页面内容缓存 TTL（分钟）
         */
        private Long newsContentTtlMinutes = 30L;

        /**
         * 文档类页面内容缓存 TTL（分钟）
         */
        private Long documentContentTtlMinutes = 1440L;
    }

    @Data
    public static class Cleaning {
        /**
         * 是否启用内容清洗
         */
        private Boolean enabled = true;

        /**
         * 需要去除的 CSS 选择器列表
         */
        private List<String> removeSelectors = List.of(
            "nav", "header", "footer", "aside", ".cookie-banner",
            ".newsletter", ".subscription", ".social-share",
            ".related-articles", ".recommended", "#comments"
        );

        /**
         * 截断策略：保留头部字符数
         */
        private Integer headChars = 6000;

        /**
         * 截断策略：保留尾部字符数
         */
        private Integer tailChars = 3000;
    }
}
