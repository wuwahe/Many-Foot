package com.lh.manyfoot.agent.stream.event;

public record Complete(String sessionId) implements ConversationEvent {
}
