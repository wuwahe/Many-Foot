package com.lh.manyfoot.agent.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 专业智能体结构化 JSON 响应解析工具。
 */
@Slf4j
public final class SpecialistJsonUtils {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    private SpecialistJsonUtils() {
    }

    public static String toJson(Object value) {
        return JSONUtil.toJsonStr(value);
    }

    public static <T> T parseResponse(String response, Class<T> targetType, Supplier<T> fallbackSupplier) {
        if (StrUtil.isBlank(response)) {
            return fallbackSupplier.get();
        }

        try {
            return JSONUtil.toBean(extractJson(response), targetType);
        } catch (Exception e) {
            log.warn("解析专业智能体响应失败，使用兜底结果: targetType={}, reason={}", targetType.getSimpleName(), e.getMessage());
            return fallbackSupplier.get();
        }
    }

    private static String extractJson(String response) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return response.trim();
    }
}
