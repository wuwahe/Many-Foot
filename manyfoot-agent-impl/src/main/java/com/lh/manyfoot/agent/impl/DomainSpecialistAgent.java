package com.lh.manyfoot.agent.impl;

import com.lh.manyfoot.agent.context.AgentContext;
import com.lh.manyfoot.agent.core.AbstractAgent;
import com.lh.manyfoot.agent.domain.DomainDraft;
import com.lh.manyfoot.agent.domain.DomainSpecialistInput;
import com.lh.manyfoot.agent.domain.EvidencePack;
import com.lh.manyfoot.agent.domain.TaskSlice;
import com.lh.manyfoot.agent.prompt.AgentPromptProvider;
import com.lh.manyfoot.agent.strategy.SyncCallStrategy;
import com.lh.manyfoot.agent.support.SpecialistJsonUtils;
import com.lh.manyfoot.models.registry.ModelResolver;
import com.lh.manyfoot.domain.ModelRole;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 领域专家 Agent 抽象基类，负责封装“专业判断和专业产出”的公共执行流程。
 *
 * <p>领域专家是一类 Agent，而不是单个具体 Agent。它不负责调度、不检索资料、也不执行系统写操作；
 * 具体专业方向通过子类提供名称、描述、专业画像和领域匹配规则。
 */
public abstract class DomainSpecialistAgent extends AbstractAgent<String> {

    protected DomainSpecialistAgent(ModelResolver modelResolver,
                                    AgentPromptProvider promptProvider) {
        super(modelResolver, promptProvider, new SyncCallStrategy());
    }

    @Override
    protected ModelRole getModelRole() {
        return ModelRole.DOMAIN_SPECIALIST;
    }

    /**
     * 专家类型，用于工厂注册、计划路由和兜底选择保持同一套语义。
     */
    public abstract SpecialistType getSpecialistType();

    /**
     * 专业画像会被注入系统提示词，约束子类只在自己的专业范围内产出结论。
     */
    protected abstract String getSpecialistProfile();

    /**
     * 用于从通用 DOMAIN_SPECIALIST 路由中选择更合适的具体专家。
     */
    protected abstract Collection<String> getDomainKeywords();

    public boolean supports(TaskSlice taskSlice) {
        if (taskSlice == null) {
            return false;
        }
        String searchableText = searchableText(taskSlice);
        return getDomainKeywords().stream()
            .filter(Objects::nonNull)
            .map(keyword -> keyword.toLowerCase(Locale.ROOT))
            .anyMatch(searchableText::contains);
    }

    public DomainDraft draft(String sessionId, TaskSlice taskSlice, EvidencePack evidencePack) {
        DomainSpecialistInput input = DomainSpecialistInput.builder()
            .taskSlice(taskSlice)
            .evidencePack(evidencePack)
            .build();
        AgentContext context = AgentContext.builder()
            .sessionId(sessionId)
            .query(SpecialistJsonUtils.toJson(input))
            .additionalContext(buildSpecialistContext())
            .build();
        String response = execute(context);
        return SpecialistJsonUtils.parseResponse(response, DomainDraft.class, () -> fallbackDraft(response));
    }

    private String buildSpecialistContext() {
        return """
            ## 当前具体领域专家
            - 名称：%s
            - 类型：%s
            - 描述：%s

            ## 专业画像
            %s
            """.formatted(getName(), getSpecialistType().name(), getDescription(), getSpecialistProfile());
    }

    private String searchableText(TaskSlice taskSlice) {
        return Stream.of(
                taskSlice.getDomain(),
                taskSlice.getGoal(),
                String.join(" ", safeList(taskSlice.getInstructions())),
                String.join(" ", safeList(taskSlice.getConstraints()))
            )
            .filter(Objects::nonNull)
            .map(value -> value.toLowerCase(Locale.ROOT))
            .reduce("", (left, right) -> left + " " + right);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private DomainDraft fallbackDraft(String response) {
        return DomainDraft.builder()
            .analysisMd(response)
            .risks(List.of("模型响应未能解析为 DomainDraft JSON"))
            .nextActions(List.of("必要时交由 Code_agent 通过代码执行、数据分析或脚本验证补充结果"))
            .build();
    }

    public enum SpecialistType {
        CODE,
        BUSINESS,
        DOCUMENT,
        DATA
    }
}
