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
 * 领域专家基于证据产出的分析草稿。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分析内容，Markdown 格式。
     */
    private String analysisMd;

    /**
     * 分析过程中做出的假设列表。
     */
    @Builder.Default
    private List<String> assumptions = new ArrayList<>();

    /**
     * 识别出的风险点列表。
     */
    @Builder.Default
    private List<String> risks = new ArrayList<>();

    /**
     * 建议的后续行动列表。
     */
    @Builder.Default
    private List<String> nextActions = new ArrayList<>();
}
