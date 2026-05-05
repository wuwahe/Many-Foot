package com.lh.manyfoot.agent.factory;

import com.lh.manyfoot.agent.impl.BusinessSpecialistAgent;
import com.lh.manyfoot.agent.impl.CodeSpecialistAgent;
import com.lh.manyfoot.agent.impl.DataSpecialistAgent;
import com.lh.manyfoot.agent.impl.DocumentSpecialistAgent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class AgentFactoryTest {

    @Test
    void getAgent_shouldExposeConcreteSpecialistsAndCompatibilityDomainEntry() {
        CodeSpecialistAgent codeSpecialistAgent = new CodeSpecialistAgent(null, null);
        BusinessSpecialistAgent businessSpecialistAgent = new BusinessSpecialistAgent(null, null);
        DocumentSpecialistAgent documentSpecialistAgent = new DocumentSpecialistAgent(null, null);
        DataSpecialistAgent dataSpecialistAgent = new DataSpecialistAgent(null, null);
        AgentFactory agentFactory = new AgentFactory(
            null,
            null,
            codeSpecialistAgent,
            businessSpecialistAgent,
            documentSpecialistAgent,
            dataSpecialistAgent,
            null,
            null,
            null,
            null  // SupervisorAgent — 不影响本次测试
        );

        assertSame(codeSpecialistAgent, agentFactory.getAgent(AgentFactory.AgentType.CODE_SPECIALIST));
        assertSame(businessSpecialistAgent, agentFactory.getAgent(AgentFactory.AgentType.BUSINESS_SPECIALIST));
        assertSame(documentSpecialistAgent, agentFactory.getAgent(AgentFactory.AgentType.DOCUMENT_SPECIALIST));
        assertSame(dataSpecialistAgent, agentFactory.getAgent(AgentFactory.AgentType.DATA_SPECIALIST));
        assertSame(businessSpecialistAgent, agentFactory.getAgent(AgentFactory.AgentType.DOMAIN_SPECIALIST));
    }
}
