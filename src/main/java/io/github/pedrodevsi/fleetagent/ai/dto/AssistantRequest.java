package io.github.pedrodevsi.fleetagent.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AssistantRequest(
        @NotBlank(message = "A mensagem é obrigatoria.")
        @Size(max = 1000, message = "A mensagem deve ter no maximo 1000 caracteres.")
        String message,

        UUID sessionId
) {
}
