package com.lh.manyfoot.event.domain;

import lombok.Getter;

/**
 * Agent Loop 阶段枚举
 * 参考 Manus 的 分析→规划→执行→观察 循环
 *
 * @author airx
 */
@Getter
public enum LoopPhase {

    /**
     * 分析阶段 - 理解当前状态和目标
     */
    ANALYZE("analyze", "分析", "分析当前状态和用户目标"),

    /**
     * 规划阶段 - 制定下一步行动计划
     */
    PLAN("plan", "规划", "制定下一步行动计划"),

    /**
     * 执行阶段 - 执行代码或调用工具
     */
    EXECUTE("execute", "执行", "执行代码或调用工具"),

    /**
     * 观察阶段 - 分析执行结果
     */
    OBSERVE("observe", "观察", "分析执行结果并更新状态");

    private final String code;
    private final String name;
    private final String description;

    LoopPhase(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 获取下一个阶段
     */
    public LoopPhase next() {
        return switch (this) {
            case ANALYZE -> PLAN;
            case PLAN -> EXECUTE;
            case EXECUTE -> OBSERVE;
            case OBSERVE -> ANALYZE; // 循环回到分析阶段
        };
    }

    /**
     * 根据代码获取枚举
     */
    public static LoopPhase fromCode(String code) {
        for (LoopPhase phase : values()) {
            if (phase.getCode().equalsIgnoreCase(code)) {
                return phase;
            }
        }
        return ANALYZE;
    }
}
