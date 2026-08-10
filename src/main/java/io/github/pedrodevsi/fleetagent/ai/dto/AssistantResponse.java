package io.github.pedrodevsi.fleetagent.ai.dto;

import java.util.UUID;

public record AssistantResponse(UUID sessionId, String answer) {
}
