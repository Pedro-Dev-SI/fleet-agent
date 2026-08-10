package io.github.pedrodevsi.fleetagent.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

public class RentalScopeInputGuardrail implements InputGuardrail {

    @Override
    public InputGuardrailResult validate(UserMessage userMessage){
        String text = normalize(userMessage.singleText());

        if (hasPromptInjection(text)) {
            return fatal("Não posso ajudar com esse tipo de solicitação.");
        }

        if (isClearlyOutOfScope(text)) {
            return fatal("Só posso ajudar com locação corporativa de veículos.");
        }

        return success();
    }

    private boolean hasPromptInjection(String text) {
        return text.contains("ignore as instrucoes")
                || text.contains("ignore as regras")
                || text.contains("system prompt")
                || text.contains("prompt do sistema")
                || text.contains("jailbreak")
                || text.contains("modo desenvolvedor");
    }

    private boolean isClearlyOutOfScope(String text) {
        return text.contains("receita de bolo")
                || text.contains("capital da franca")
                || text.contains("programacao em java")
                || text.contains("bitcoin")
                || text.contains("futebol")
                || text.contains("filme")
                || text.contains("musica");
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}
