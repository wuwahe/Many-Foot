package com.lh.manyfoot.agent.factory;

import com.lh.manyfoot.agent.impl.DocumentSpecialistAgent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class AgentFactoryTest {

    @Test
    void getAgent_shouldExposeDocumentSpecialistAsDomainSpecialist() {
        DocumentSpecialistAgent documentSpecialistAgent = new DocumentSpecialistAgent(null, null);
        AgentFactory agentFactory = new AgentFactory(
            null,
            null,
            documentSpecialistAgent,
            null,
            null,
            null,
            null  // SupervisorAgent — 不影响本次测试
        );

        assertSame(documentSpecialistAgent, agentFactory.getAgent(AgentFactory.AgentType.DOCUMENT_SPECIALIST));
        assertSame(documentSpecialistAgent, agentFactory.getAgent(AgentFactory.AgentType.DOMAIN_SPECIALIST));
    }
}
