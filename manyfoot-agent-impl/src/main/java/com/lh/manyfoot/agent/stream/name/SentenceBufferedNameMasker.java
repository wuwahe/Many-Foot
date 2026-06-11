package com.lh.manyfoot.agent.stream.name;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 句级缓冲脱敏器
 * <p>
 * 在流式输出过程中，以句子为缓冲粒度对内部标识符进行脱敏处理。
 * 由于流式 token 的碎片化特性，不能对每个 delta 单独做正则替换
 * （可能会截断禁词），因此需要先缓冲到句末再做整体脱敏。
 * <p>
 * <h3>算法概述</h3>
 * <ol>
 *   <li>每次 {@link #accept(String)} 将 delta 追加到内部缓冲区</li>
 *   <li>在缓冲区中查找最后一个句末符（。！？.!?\n）</li>
 *   <li>句末符之前（含）的部分作为"安全区"切出，进行脱敏后返回</li>
 *   <li>若缓冲区超过 {@value #MAX_HOLD} 字符仍未找到句末符，强制截断</li>
 *   <li>流结束时调用 {@link #forceFlush()} 处理剩余缓冲区</li>
 * </ol>
 * <p>
 * <h3>安全截断策略</h3>
 * 当缓冲区超过 {@value #MAX_HOLD} 字符但找不到句末符时，
 * 强制截断保留最后 {@value #MAX_SAFE_TAIL} 字符在缓冲区中，
 * 以避免在禁词中间截断导致脱敏遗漏。
 *
 * @author airx
 * @see ForbiddenNameProvider
 */
@Slf4j
public class SentenceBufferedNameMasker {

    /**
     * 缓冲区最大持有字符数。
     * <p>
     * 超过此阈值仍未遇到句末符时，触发强制截断。
     */
    private static final int MAX_HOLD = 256;

    /**
     * 强制截断时保留在缓冲区中的尾部字符数。
     * <p>
     * 保留足够长的尾部以覆盖可能横跨截断点的禁词。
     */
    private static final int MAX_SAFE_TAIL = 64;

    /**
     * 句末符匹配正则。
     * <p>
     * 匹配中文句末符（。！？）和英文句末符（.!?\n）。
     */
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？.!?\n]");

    /**
     * 脱敏替换的目标文本。
     */
    private static final String MASK_REPLACEMENT = "\u4e00\u4e2a\u5185\u90e8\u6b65\u9aa4"; // 一个内部步骤

    /**
     * 预编译的禁词匹配正则。
     * <p>
     * 使用 {@code CASE_INSENSITIVE} 标志实现大小写不敏感匹配，
     * 所有禁词通过 {@code |} 连接，正则引擎自动实现最长匹配。
     */
    private final Pattern forbiddenPattern;

    /**
     * 内部文本缓冲区。
     */
    private final StringBuilder pending = new StringBuilder();

    /**
     * 构造函数。
     * <p>
     * 将禁词集合编译为单个正则模式。
     * 禁词应已通过 {@code Pattern.quote()} 处理，
     * 并按长度降序排列（由 {@link RegistryForbiddenNameProvider} 保证）。
     *
     * @param forbidden 已转义的禁词集合
     */
    public SentenceBufferedNameMasker(Set<String> forbidden) {
        if (forbidden == null || forbidden.isEmpty()) {
            // 空集合时使用一个永不匹配的正则，避免后续 null 判断
            this.forbiddenPattern = Pattern.compile("(?!a)a");
            log.info("SentenceBufferedNameMasker 初始化完成（禁词集合为空，脱敏功能禁用）");
        } else {
            // 将所有已转义的禁词用 | 连接，构建交替匹配正则
            String regex = String.join("|", forbidden);
            this.forbiddenPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            log.info("SentenceBufferedNameMasker 初始化完成，共 {} 个禁词模式", forbidden.size());
        }
    }

    /**
     * 喂入流式文本片段。
     * <p>
     * 将 delta 追加到内部缓冲区，然后尝试在缓冲区中查找句末符。
     * 找到句末符时，将句末符之前（含）的部分进行脱敏后返回。
     * <p>
     * <b>返回值可能为空字符串</b>，表示当前没有可安全发出的文本
     * （文本仍在缓冲区中等待句末符）。
     *
     * @param delta 流式输出的文本片段
     * @return 已脱敏的安全文本（可能为空）
     */
    public String accept(String delta) {
        if (delta == null || delta.isEmpty()) {
            return "";
        }

        pending.append(delta);

        // 查找最后一个句末符的位置
        int lastSentenceEnd = findLastSentenceEnd(pending);

        if (lastSentenceEnd >= 0) {
            // 找到句末符：切出安全区（含句末符本身）
            String safe = pending.substring(0, lastSentenceEnd + 1);
            // 保留句末符之后的部分继续缓冲
            pending.delete(0, lastSentenceEnd + 1);
            return mask(safe);
        }

        // 未找到句末符：检查是否超过最大持有阈值
        if (pending.length() > MAX_HOLD) {
            // 强制截断：保留尾部 MAX_SAFE_TAIL 字符在缓冲区中
            int cutoff = pending.length() - MAX_SAFE_TAIL;
            String forceOut = pending.substring(0, cutoff);
            pending.delete(0, cutoff);
            log.debug("缓冲区超过 {} 字符未遇句末符，强制截断 {} 字符", MAX_HOLD, forceOut.length());
            return mask(forceOut);
        }

        // 文本量不足，继续缓冲
        return "";
    }

    /**
     * 强制刷新缓冲区。
     * <p>
     * 在流结束时调用，处理缓冲区中剩余的所有文本。
     * 对剩余文本执行脱敏后返回。
     *
     * @return 缓冲区剩余文本（已脱敏）
     */
    public String forceFlush() {
        if (pending.isEmpty()) {
            return "";
        }
        String remaining = pending.toString();
        pending.setLength(0);
        return mask(remaining);
    }

    /**
     * 在文本中查找最后一个句末符的位置。
     *
     * @param sb 待搜索的文本
     * @param <T> StringBuilder 或 CharSequence
     * @return 最后一个句末符的索引，未找到返回 -1
     */
    private <T extends CharSequence> int findLastSentenceEnd(T sb) {
        int lastIndex = -1;
        Matcher matcher = SENTENCE_END.matcher(sb);
        while (matcher.find()) {
            lastIndex = matcher.start();
        }
        return lastIndex;
    }

    /**
     * 对文本执行禁词脱敏。
     * <p>
     * 使用预编译的正则模式匹配所有禁词，统一替换为 {@value #MASK_REPLACEMENT}。
     * 如果发生替换，记录 warn 日志。
     *
     * @param text 待脱敏的文本
     * @return 脱敏后的文本
     */
    private String mask(String text) {
        if (text.isEmpty()) {
            return text;
        }

        Matcher matcher = forbiddenPattern.matcher(text);
        if (!matcher.find()) {
            // 无命中，原文返回
            return text;
        }

        // 收集所有命中的禁词名称用于日志
        Set<String> matchedNames = new LinkedHashSet<>();
        Matcher nameCollector = forbiddenPattern.matcher(text);
        while (nameCollector.find()) {
            matchedNames.add(nameCollector.group());
        }

        // 执行全局替换
        String masked = matcher.replaceAll(MASK_REPLACEMENT);

        // 记录日志：截取前 80 个字符作为摘要
        String snippet = text.length() > 80 ? text.substring(0, 80) + "..." : text;
        log.warn("Masked forbidden token(s): {} snippet='{}'", matchedNames, snippet);

        return masked;
    }
}
