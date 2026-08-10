package io.github.pedrodevsi.fleetagent.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class RentalScopeOutputGuardrail implements OutputGuardrail {

    @Override
    public OutputGuardrailResult validate(AiMessage llmResponse) {

        String answer = llmResponse.text();

        if (answer == null || answer.isBlank()) {
            return fatal("Houve um problema com a resposta da IA, tente novamente.");
        }

        return success();

    }
}
