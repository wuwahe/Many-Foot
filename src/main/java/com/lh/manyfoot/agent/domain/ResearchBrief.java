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
 * 检索与证据 Agent 的检索任务说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchBrief implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 需要检索解答的问题列表。
     */
    @Builder.Default
    private List<String> questions = new ArrayList<>();

    /**
     * 检索范围界定，如领域、时间范围等。
     */
    private String scope;

    /**
     * 优选来源列表，指定优先检索的数据源。
     */
    @Builder.Default
    private List<String> preferredSources = new ArrayList<>();

    /**
     * 排除项列表，指定不应包含的信息类型或来源。
     */
    @Builder.Default
    private List<String> exclusions = new ArrayList<>();
}
