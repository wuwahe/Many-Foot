package com.lh.manyfoot.agent.stream.event;

public record Failure(String userMessage, Throwable cause) implements ConversationEvent {
}
