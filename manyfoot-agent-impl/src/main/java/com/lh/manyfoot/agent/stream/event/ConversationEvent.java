package com.lh.manyfoot.agent.stream.event;

public sealed interface ConversationEvent
        permits NarrationDelta, PhaseHint, Complete, Failure {
}
