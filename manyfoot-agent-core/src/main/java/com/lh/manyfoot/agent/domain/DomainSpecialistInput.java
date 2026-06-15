package com.lh.manyfoot.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 领域专家 Agent 的组合输入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainSpecialistInput implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务切片，定义领域专家需要完成的具体任务。
     */
    private TaskSlice taskSlice;

    /**
     * 证据包，提供任务相关的背景信息和引用。
     */
    private EvidencePack evidencePack;
}
