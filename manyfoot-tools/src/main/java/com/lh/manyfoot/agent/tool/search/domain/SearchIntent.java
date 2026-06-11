package com.lh.manyfoot.agent.tool.search.domain;

/**
 * 搜索意图分类
 * <p>
 * 统一查询接口层根据用户输入判断意图类型，决定处理路径：
 * <ul>
 *   <li>SUMMARY_ONLY - 只需摘要，搜索层后直接返回</li>
 *   <li>DEEP_RESEARCH - 需要页面正文，进入内容提取层</li>
 *   <li>BATCH_EXTRACTION - 批量读取多页，并发提取</li>
 *   <li>DIRECT_URL - 已知 URL，跳过搜索层</li>
 * </ul>
 *
 * @author airx
 */
public enum SearchIntent {

    /**
     * 只需摘要（新闻标题、简单事实）
     * <p>
     * 路径：搜索层 → 直接返回摘要
     */
    SUMMARY_ONLY,

    /**
     * 需要页面正文（深度研究、全文分析）
     * <p>
     * 路径：搜索层 → 内容提取层
     */
    DEEP_RESEARCH,

    /**
     * 需要批量读多页（竞品分析、数据汇总）
     * <p>
     * 路径：搜索层 → 并发内容提取
     */
    BATCH_EXTRACTION,

    /**
     * 已知 URL，不需要搜索
     * <p>
     * 路径：跳过搜索层，直接内容提取
     */
    DIRECT_URL,

    /**
     * 未知意图，由系统根据上下文推断
     */
    UNKNOWN
}
