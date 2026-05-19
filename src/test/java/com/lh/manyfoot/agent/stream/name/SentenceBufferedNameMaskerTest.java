package com.lh.manyfoot.agent.stream.name;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SentenceBufferedNameMasker} 单元测试。
 * <p>
 * 覆盖句级缓冲脱敏的各个场景：单 delta 命中、跨 delta 拆名、
 * 多禁词同句、中文/拉丁标点切句、MAX_HOLD 强制截断、大小写不敏感、
 * forceFlush 剩余文本、空禁词集合透传等。
 */
class SentenceBufferedNameMaskerTest {

    // 构造禁词集合（模拟 RegistryForbiddenNameProvider 已按长度降序排列并转义）
    private static Set<String> forbiddenSet(String... names) {
        Set<String> set = new LinkedHashSet<>();
        for (String name : names) {
            set.add(java.util.regex.Pattern.quote(name));
        }
        return set;
    }

    @Test
    @DisplayName("单 delta 命中禁词")
    void singleDelta_shouldMaskForbidden() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(forbiddenSet("Supervisor_agent"));

        String result = masker.accept("Supervisor_agent is working.");

        assertTrue(result.contains("一个内部步骤"), "禁词应被替换为脱敏文本");
        assertFalse(result.contains("Supervisor_agent"), "原禁词不应再出现");
    }

    @Test
    @DisplayName("跨 delta 拆名命中（如 Code + _agent）")
    void crossDeltaSplitName_shouldMask() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(forbiddenSet("Code_agent"));

        String r1 = masker.accept("The Code");
        assertEquals("", r1, "未达句末不应发出文本");

        String r2 = masker.accept("_agent is here.");
        assertTrue(r2.contains("一个内部步骤"), "跨 delta 的禁词应被正确脱敏");
        assertFalse(r2.contains("Code_agent"), "原禁词不应再出现");
    }

    @Test
    @DisplayName("多禁词同句")
    void multipleForbiddenInSameSentence_shouldMaskAll() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(
                forbiddenSet("PlannerRouter_agent", "ToolActionExecutor_agent")
        );

        String result = masker.accept("PlannerRouter_agent and ToolActionExecutor_agent are active.");

        assertEquals(2, countOccurrences(result, "一个内部步骤"), "两个禁词都应被替换");
        assertFalse(result.contains("PlannerRouter_agent"), "PlannerRouter_agent 应被脱敏");
        assertFalse(result.contains("ToolActionExecutor_agent"), "ToolActionExecutor_agent 应被脱敏");
    }

    @Test
    @DisplayName("中文标点切句（。）")
    void chinesePunctuation_shouldSplitSentence() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(forbiddenSet("Chat_agent"));

        String r1 = masker.accept("Chat_agent 正在运行。");
        assertTrue(r1.contains("一个内部步骤"), "中文句末符前应完成脱敏");
        assertFalse(r1.contains("Chat_agent"), "原禁词不应再出现");

        String r2 = masker.accept("这是下一句。");
        assertTrue(r2.contains("这是下一句"), "后续文本应原样通过");
    }

    @Test
    @DisplayName("Latin 标点切句（.!?）")
    void latinPunctuation_shouldSplitSentence() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(forbiddenSet("ResearchRetrieval_agent"));

        String r1 = masker.accept("ResearchRetrieval_agent is running!");
        assertTrue(r1.contains("一个内部步骤"), "! 句末符前应完成脱敏");
        assertFalse(r1.contains("ResearchRetrieval_agent"), "原禁词不应再出现");

        String r2 = masker.accept(" Next sentence?");
        assertTrue(r2.contains("Next sentence"), "? 句末符后文本应原样通过");
    }

    @Test
    @DisplayName("MAX_HOLD(256) 强制 flush，保留 64 字符尾巴")
    void maxHoldForceFlush_shouldPreserveTail() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(forbiddenSet("DocumentSpecialist_agent"));

        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            longText.append("This is a long paragraph without sentence endings ");
        }
        longText.append("DocumentSpecialist_agent ");
        for (int i = 0; i < 10; i++) {
            longText.append("more text ");
        }

        String input = longText.toString();
        assertTrue(input.length() > 256, "输入应超过 256 字符");

        String result = masker.accept(input);

        if (!result.isEmpty()) {
            assertFalse(result.contains("DocumentSpecialist_agent"),
                    "超过 MAX_HOLD 强制截断后，禁词应被脱敏");
        }

        String flushed = masker.forceFlush();
        String combined = result + flushed;
        assertFalse(combined.contains("DocumentSpecialist_agent"),
                "整体文本中禁词应被脱敏");
    }

    @Test
    @DisplayName("大小写不敏感（如 supervisor_agent 匹配 Supervisor_agent）")
    void caseInsensitive_shouldMask() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(forbiddenSet("Supervisor_agent"));

        String result = masker.accept("supervisor_agent and SUPERVISOR_AGENT are both matched.");

        assertEquals(2, countOccurrences(result, "一个内部步骤"),
                "大小写变体都应被脱敏");
        assertFalse(result.contains("supervisor_agent"), "小写变体应被脱敏");
        assertFalse(result.contains("SUPERVISOR_AGENT"), "大写变体应被脱敏");
    }

    @Test
    @DisplayName("forceFlush 剩余文本脱敏")
    void forceFlush_shouldMaskRemaining() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(forbiddenSet("Code_agent"));

        masker.accept("Code_agent");
        String result = masker.forceFlush();

        assertTrue(result.contains("一个内部步骤"), "forceFlush 应对剩余文本脱敏");
        assertFalse(result.contains("Code_agent"), "forceFlush 后不应保留原禁词");
    }

    @Test
    @DisplayName("空禁词集合，文本原样通过")
    void emptyForbiddenSet_shouldPassThrough() {
        SentenceBufferedNameMasker masker = new SentenceBufferedNameMasker(java.util.Collections.emptySet());

        String original = "Supervisor_agent and Code_agent are names.";
        String result = masker.accept(original);

        assertEquals(original, result, "空禁词集合时应原样返回");
    }

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = text.indexOf(target, fromIndex)) != -1) {
            count++;
            fromIndex += target.length();
        }
        return count;
    }
}
