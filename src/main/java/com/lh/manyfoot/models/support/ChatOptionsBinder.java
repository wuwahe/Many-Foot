package com.lh.manyfoot.models.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 采样参数通用绑定器。
 * <p>
 * 用于把 {@code AiModelConfig#getOptions()} 这种 {@code Map<String, Object>} 形式的
 * 通用参数，按需映射到各家 {@code *ChatOptions.builder()} 的 setter 上。
 * 目的是消除 6+ 个 Factory 里重复的 "读 Double / Integer / List<String>" 样板。
 *
 * <p>支持的 key（大小写不敏感，中划线/下划线/驼峰三种写法都能识别）：
 * <ul>
 *     <li>temperature</li>
 *     <li>top-p</li>
 *     <li>top-k</li>
 *     <li>max-tokens</li>
 *     <li>presence-penalty</li>
 *     <li>frequency-penalty</li>
 *     <li>seed</li>
 *     <li>stop (String 或 List&lt;String&gt;)</li>
 * </ul>
 */
public final class ChatOptionsBinder {

    private ChatOptionsBinder() {
    }

    public static void bindTemperature(Map<String, Object> options, Consumer<Double> setter) {
        Double v = asDouble(options, "temperature");
        if (v != null) {
            setter.accept(v);
        }
    }

    public static void bindTopP(Map<String, Object> options, Consumer<Double> setter) {
        Double v = asDouble(options, "top-p", "topP");
        if (v != null) {
            setter.accept(v);
        }
    }

    public static void bindTopK(Map<String, Object> options, Consumer<Integer> setter) {
        Integer v = asInteger(options, "top-k", "topK");
        if (v != null) {
            setter.accept(v);
        }
    }

    public static void bindMaxTokens(Map<String, Object> options, Consumer<Integer> setter) {
        Integer v = asInteger(options, "max-tokens", "maxTokens", "max-output-tokens");
        if (v != null) {
            setter.accept(v);
        }
    }

    public static void bindPresencePenalty(Map<String, Object> options, Consumer<Double> setter) {
        Double v = asDouble(options, "presence-penalty", "presencePenalty");
        if (v != null) {
            setter.accept(v);
        }
    }

    public static void bindFrequencyPenalty(Map<String, Object> options, Consumer<Double> setter) {
        Double v = asDouble(options, "frequency-penalty", "frequencyPenalty");
        if (v != null) {
            setter.accept(v);
        }
    }

    public static void bindSeed(Map<String, Object> options, Consumer<Integer> setter) {
        Integer v = asInteger(options, "seed");
        if (v != null) {
            setter.accept(v);
        }
    }

    public static void bindStop(Map<String, Object> options, Consumer<List<String>> setter) {
        Object raw = findValue(options, "stop");
        if (raw == null) {
            return;
        }
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            if (!out.isEmpty()) {
                setter.accept(out);
            }
        } else {
            setter.accept(List.of(raw.toString()));
        }
    }

    public static Double asDouble(Map<String, Object> options, String... aliases) {
        Object v = findValue(options, aliases);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(v.toString().trim());
    }

    public static Integer asInteger(Map<String, Object> options, String... aliases) {
        Object v = findValue(options, aliases);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(v.toString().trim());
    }

    /**
     * 在 map 里按 alias 顺序寻找 value，忽略大小写，并把 key 规范化为 {@code lower-case+中划线}
     * 后做匹配，以便同时识别 {@code topP / top-p / top_p}。
     */
    public static Object findValue(Map<String, Object> options, String... aliases) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        List<String> wanted = new ArrayList<>();
        for (String a : aliases) {
            wanted.add(normalize(a));
        }
        for (Map.Entry<String, Object> e : options.entrySet()) {
            String key = normalize(e.getKey());
            if (wanted.contains(key)) {
                return e.getValue();
            }
        }
        // 第二遍：把 camelCase 拆成 kebab-case 再比对
        for (Map.Entry<String, Object> e : options.entrySet()) {
            String key = camelToKebab(e.getKey());
            if (wanted.contains(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String normalize(String s) {
        return Objects.requireNonNullElse(s, "").toLowerCase(Locale.ROOT).replace('_', '-').trim();
    }

    private static String camelToKebab(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else if (c == '_') {
                sb.append('-');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 便捷重载，供 "key 固定就是 'temperature' 那几个" 的场景使用，避免重复写数组。
     */
    public static void ifPresent(Map<String, Object> options, String key, Consumer<Object> consumer) {
        Object v = findValue(options, key);
        if (v != null) {
            consumer.accept(v);
        }
    }

    /**
     * 便于 Factory 内部断言未知 key（目前只打 debug 日志，不强制）。
     */
    public static List<String> unknownKeys(Map<String, Object> options, String... known) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        List<String> knownNorm = Arrays.stream(known).map(ChatOptionsBinder::normalize).toList();
        List<String> unknown = new ArrayList<>();
        for (String k : options.keySet()) {
            String n = normalize(k);
            String camel = camelToKebab(k);
            if (!knownNorm.contains(n) && !knownNorm.contains(camel)) {
                unknown.add(k);
            }
        }
        return unknown;
    }
}
