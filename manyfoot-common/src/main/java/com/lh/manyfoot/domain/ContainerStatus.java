package com.lh.manyfoot.domain;

import lombok.Getter;

/**
 * 容器状态枚举
 *
 * @author airx
 */
@Getter
public enum ContainerStatus {

    /**
     * 创建中
     */
    CREATING("creating", "创建中"),

    /**
     * 运行中
     */
    RUNNING("running", "运行中"),

    /**
     * 暂停
     */
    PAUSED("paused", "已暂停"),

    /**
     * 已停止
     */
    STOPPED("stopped", "已停止"),

    /**
     * 失败
     */
    FAILED("failed", "失败"),

    /**
     * 已销毁
     */
    DESTROYED("destroyed", "已销毁");

    private final String code;
    private final String description;

    ContainerStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
