package com.lh.manyfoot.codeact.domain;

import lombok.Getter;

/**
 * 代码类型枚举
 *
 * @author airx
 */
@Getter
public enum CodeType {

    /**
     * Python 代码
     */
    PYTHON("python", "Python", "py"),

    /**
     * Shell 脚本
     */
    SHELL("shell", "Shell", "sh"),

    /**
     * Bash 脚本
     */
    BASH("bash", "Bash", "sh");

    private final String code;
    private final String name;
    private final String extension;

    CodeType(String code, String name, String extension) {
        this.code = code;
        this.name = name;
        this.extension = extension;
    }

    /**
     * 根据代码获取枚举
     */
    public static CodeType fromCode(String code) {
        if (code == null) {
            return PYTHON;
        }
        for (CodeType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return PYTHON;
    }
}
