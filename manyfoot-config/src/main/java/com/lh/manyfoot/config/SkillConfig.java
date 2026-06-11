package com.lh.manyfoot.config;

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Skills 配置类。
 * <p>
 * 只注册 {@link SkillRegistry} bean，由各个智能体按需通过 {@link com.lh.manyfoot.agent.core.AbstractToolAgent#getAvailableSkills()}
 * 声明自己需要的 skills，实现按智能体隔离加载。
 *
 * @author airx
 */
@Configuration
public class SkillConfig {

    @Bean
    @ConditionalOnProperty(name = "many-foot.skills.enabled", havingValue = "true")
    public SkillRegistry skillRegistry() {
        return ClasspathSkillRegistry.builder().classpathPath("skills").build();
    }
}
