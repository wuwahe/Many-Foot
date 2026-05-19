package com.lh.manyfoot.agent.stream.name;

import java.util.Set;

/**
 * 禁词提供者接口
 * <p>
 * 提供需要在流式输出中被脱敏/屏蔽的内部标识符集合，
 * 包括智能体名称、工具名称、框架内部术语等。
 * <p>
 * 实现方应返回包含所有不应暴露给用户的内部名称的不可变集合。
 *
 * @author airx
 */
public interface ForbiddenNameProvider {

    /**
     * 获取所有禁词 token 集合。
     * <p>
     * 返回的集合中每个元素应已经过 {@code Pattern.quote()} 处理，
     * 可直接用于正则匹配。
     *
     * @return 不可变的禁词集合
     */
    Set<String> forbiddenTokens();
}
