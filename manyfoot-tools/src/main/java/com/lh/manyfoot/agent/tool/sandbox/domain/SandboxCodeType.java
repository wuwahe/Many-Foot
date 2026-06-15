package com.lh.manyfoot.agent.tool.sandbox.domain;

import lombok.Getter;

/**
 * 沙箱支持的代码类型枚举
 *
 * @author airx
 */
@Getter
public enum SandboxCodeType {

    PYTHON("python", "Python", "py"),

    SHELL("shell", "Shell", "sh"),

    BASH("bash", "Bash", "sh");

    private final String code;
    private final String name;
    private final String extension;

    SandboxCodeType(String code, String name, String extension) {
        this.code = code;
        this.name = name;
        this.extension = extension;
    }

    public static SandboxCodeType fromCode(String code) {
        if (code == null) {
            return PYTHON;
        }
        for (SandboxCodeType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return PYTHON;
    }
}
