package com.lh.manyfoot.agent.stream.event;

import com.lh.manyfoot.agent.stream.Phase;

public record PhaseHint(Phase phase) implements ConversationEvent {
}
