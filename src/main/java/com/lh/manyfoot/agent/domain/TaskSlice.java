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
 * 下发给领域专家的任务切片。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSlice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务切片唯一标识。
     */
    private String id;

    /**
     * 任务目标描述。
     */
    private String goal;

    /**
     * 所属领域，如技术、业务、法律等。
     */
    private String domain;

    /**
     * 具体指令列表，细化任务执行要求。
     */
    @Builder.Default
    private List<String> instructions = new ArrayList<>();

    /**
     * 约束条件列表，限制任务执行范围。
     */
    @Builder.Default
    private List<String> constraints = new ArrayList<>();
}
