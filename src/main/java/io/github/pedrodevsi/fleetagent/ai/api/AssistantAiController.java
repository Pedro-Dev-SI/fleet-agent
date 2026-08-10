package io.github.pedrodevsi.fleetagent.ai.api;

import io.github.pedrodevsi.fleetagent.ai.application.AssistantAiService;
import io.github.pedrodevsi.fleetagent.ai.dto.AssistantRequest;
import io.github.pedrodevsi.fleetagent.ai.dto.AssistantResponse;
import dev.langchain4j.service.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/assistant")
public class AssistantAiController {

    private final AssistantAiService assistantAiService;

    public AssistantAiController(AssistantAiService assistantAiService) {
        this.assistantAiService = assistantAiService;
    }

    @PostMapping
    public AssistantResponse askAssistant(@Valid @RequestBody AssistantRequest request) {

        UUID sessionId = request.sessionId() != null
                ? request.sessionId()
                : UUID.randomUUID();

        Result<String> result = assistantAiService.handleRequest(sessionId, request.message());
        return new AssistantResponse(sessionId, result.content());
    }
}
