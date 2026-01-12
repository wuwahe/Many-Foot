package com.lh.manyfoot.config.properties;

import lombok.Getter;

@Getter
public enum VendorEnums {

    GEMINI("Gemini"),
    OPENAI("Openai"),
    DASHSCOPE("Dashscope"),
    OLLAMA("Ollama");

    private final String vendor;

    VendorEnums(String vendor) {
        this.vendor = vendor;
    }
}
