package com.lh.manyfoot.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 可追溯证据包，供领域专家和评审复用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidencePack implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事实列表，包含经过验证的断言。
     */
    @Builder.Default
    private List<Fact> facts = new ArrayList<>();

    /**
     * 引用列表，记录信息来源。
     */
    @Builder.Default
    private List<Citation> citations = new ArrayList<>();

    /**
     * 整体置信度，取值范围 0.0 - 1.0。
     */
    private Double confidence;

    /**
     * 信息缺口列表，标识尚未覆盖或存疑的内容。
     */
    @Builder.Default
    private List<String> informationGaps = new ArrayList<>();

    /**
     * 事实条目。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Fact implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 事实断言内容。
         */
        private String claim;

        /**
         * 来源标识，关联到具体的引用。
         */
        private String sourceId;

        /**
         * 该事实的置信度，取值范围 0.0 - 1.0。
         */
        private Double confidence;
    }

    /**
     * 引用条目。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 引用唯一标识。
         */
        private String id;

        /**
         * 引用标题。
         */
        private String title;

        /**
         * 引用来源 URL。
         */
        private String url;

        /**
         * 引用摘录内容。
         */
        private String excerpt;

        /**
         * 检索时间，ISO 8601 格式。
         */
        private String retrievedAt;
    }
}
