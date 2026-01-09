package com.lh.manyfoot.orchestrator.domain;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务分析结果
 *
 * @author airx
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAnalysisResult {

    /**
     * 任务复杂度
     */
    private TaskComplexity complexity;

    /**
     * 任务分析说明
     */
    private String taskAnalysis;

    /**
     * 子任务列表
     */
    @Builder.Default
    private List<Subtask> subtasks = new ArrayList<>();

    /**
     * 执行计划说明
     */
    private String executionPlan;

    /**
     * 原始分析文本
     */
    private String rawAnalysis;

    /**
     * JSON代码块匹配正则
     */
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    /**
     * 从LLM响应解析分析结果
     *
     * @param response LLM响应文本
     * @return 分析结果
     */
    public static TaskAnalysisResult parseFromResponse(String response) {
        if (StrUtil.isBlank(response)) {
            return createDefaultResult("空响应");
        }

        try {
            // 尝试从JSON代码块中提取
            Matcher matcher = JSON_BLOCK_PATTERN.matcher(response);
            String jsonStr = null;

            if (matcher.find()) {
                jsonStr = matcher.group(1).trim();
            } else if (response.trim().startsWith("{")) {
                // 直接是JSON格式
                jsonStr = response.trim();
            }

            if (jsonStr != null) {
                return parseFromJson(jsonStr, response);
            }

            // 无法解析为JSON，返回默认结果
            return createDefaultResult(response);

        } catch (Exception e) {
            log.warn("解析分析结果失败，使用默认结果: {}", e.getMessage());
            return createDefaultResult(response);
        }
    }

    /**
     * 从JSON字符串解析
     */
    private static TaskAnalysisResult parseFromJson(String jsonStr, String rawResponse) {
        JSONObject json = JSONUtil.parseObj(jsonStr);

        TaskAnalysisResult result = new TaskAnalysisResult();
        result.setRawAnalysis(rawResponse);

        // 解析复杂度
        String complexityStr = json.getStr("complexity", "MEDIUM");
        try {
            result.setComplexity(TaskComplexity.valueOf(complexityStr.toUpperCase()));
        } catch (Exception e) {
            result.setComplexity(TaskComplexity.MEDIUM);
        }

        // 解析任务分析
        result.setTaskAnalysis(json.getStr("taskAnalysis", ""));

        // 解析执行计划
        result.setExecutionPlan(json.getStr("executionPlan", ""));

        // 解析子任务列表
        List<Subtask> subtasks = new ArrayList<>();
        if (json.containsKey("subtasks")) {
            json.getJSONArray("subtasks").forEach(item -> {
                JSONObject subtaskJson = (JSONObject) item;
                Subtask subtask = Subtask.builder()
                    .id(subtaskJson.getInt("id", subtasks.size() + 1))
                    .name(subtaskJson.getStr("name", ""))
                    .description(subtaskJson.getStr("description", ""))
                    .type(parseSubtaskType(subtaskJson.getStr("type")))
                    .dependencies(subtaskJson.getBeanList("dependencies", Integer.class))
                    .parallel(subtaskJson.getBool("parallel", false))
                    .build();
                subtasks.add(subtask);
            });
        }
        result.setSubtasks(subtasks);

        return result;
    }

    /**
     * 解析子任务类型
     */
    private static SubtaskType parseSubtaskType(String typeStr) {
        if (StrUtil.isBlank(typeStr)) {
            return SubtaskType.CODE_EXECUTION;
        }
        try {
            return SubtaskType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            return SubtaskType.CODE_EXECUTION;
        }
    }

    /**
     * 创建默认结果（当解析失败时）
     */
    private static TaskAnalysisResult createDefaultResult(String rawResponse) {
        Subtask defaultSubtask = Subtask.builder()
            .id(1)
            .name("执行任务")
            .description(rawResponse)
            .type(SubtaskType.CODE_EXECUTION)
            .dependencies(new ArrayList<>())
            .parallel(false)
            .build();

        return TaskAnalysisResult.builder()
            .complexity(TaskComplexity.MEDIUM)
            .taskAnalysis(rawResponse)
            .subtasks(List.of(defaultSubtask))
            .executionPlan("按顺序执行任务")
            .rawAnalysis(rawResponse)
            .build();
    }

    /**
     * 获取下一个待执行的子任务
     *
     * @return 下一个待执行的子任务，如果没有则返回null
     */
    public Subtask getNextPendingSubtask() {
        return subtasks.stream()
            .filter(st -> st.getStatus() == Subtask.SubtaskStatus.PENDING)
            .filter(this::areDependenciesCompleted)
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查子任务的所有依赖是否已完成
     */
    private boolean areDependenciesCompleted(Subtask subtask) {
        if (subtask.getDependencies() == null || subtask.getDependencies().isEmpty()) {
            return true;
        }
        return subtask.getDependencies().stream()
            .allMatch(depId -> subtasks.stream()
                .filter(st -> st.getId().equals(depId))
                .anyMatch(st -> st.getStatus() == Subtask.SubtaskStatus.COMPLETED));
    }

    /**
     * 检查是否所有子任务都已完成
     */
    public boolean isAllCompleted() {
        return subtasks.stream()
            .allMatch(st -> st.getStatus() == Subtask.SubtaskStatus.COMPLETED
                || st.getStatus() == Subtask.SubtaskStatus.SKIPPED);
    }

    /**
     * 标记子任务完成
     */
    public void markSubtaskCompleted(Integer subtaskId, String result) {
        subtasks.stream()
            .filter(st -> st.getId().equals(subtaskId))
            .findFirst()
            .ifPresent(st -> {
                st.setStatus(Subtask.SubtaskStatus.COMPLETED);
                st.setResult(result);
            });
    }

    /**
     * 标记子任务失败
     */
    public void markSubtaskFailed(Integer subtaskId, String error) {
        subtasks.stream()
            .filter(st -> st.getId().equals(subtaskId))
            .findFirst()
            .ifPresent(st -> {
                st.setStatus(Subtask.SubtaskStatus.FAILED);
                st.setResult(error);
            });
    }

    /**
     * 转换为可读的任务清单格式
     */
    public String toTaskListFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 任务分析\n");
        sb.append(taskAnalysis).append("\n\n");
        sb.append("## 复杂度: ").append(complexity).append("\n\n");
        sb.append("## 执行步骤\n");

        for (Subtask subtask : subtasks) {
            String checkbox = switch (subtask.getStatus()) {
                case COMPLETED -> "[x]";
                case FAILED -> "[!]";
                case RUNNING -> "[>]";
                default -> "[ ]";
            };
            sb.append("- ").append(checkbox).append(" 步骤").append(subtask.getId())
                .append("(").append(subtask.getType()).append("): ")
                .append(subtask.getName()).append("\n");
            if (StrUtil.isNotBlank(subtask.getDescription())) {
                sb.append("  ").append(subtask.getDescription()).append("\n");
            }
        }

        if (StrUtil.isNotBlank(executionPlan)) {
            sb.append("\n## 执行计划\n").append(executionPlan);
        }

        return sb.toString();
    }
}
