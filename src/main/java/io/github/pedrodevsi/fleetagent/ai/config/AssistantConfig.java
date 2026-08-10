package io.github.pedrodevsi.fleetagent.ai.config;

import io.github.pedrodevsi.fleetagent.ai.application.AssistantAiService;
import io.github.pedrodevsi.fleetagent.ai.guardrail.RentalScopeInputGuardrail;
import io.github.pedrodevsi.fleetagent.ai.guardrail.RentalScopeOutputGuardrail;
import io.github.pedrodevsi.fleetagent.ai.tools.AssistantTools;
import io.github.pedrodevsi.fleetagent.ai.tools.CustomerTools;
import io.github.pedrodevsi.fleetagent.ai.tools.ReservationTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AssistantConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${app.ai.provider:ollama}") String provider,
            @Value("${gemini.api-key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-3.1-flash-lite}") String geminiModel,
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.model:llama3.2}") String ollamaModel
    ) {
        if ("ollama".equalsIgnoreCase(provider)) {
            return OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModel)
                    .temperature(0.2)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        if ("gemini".equalsIgnoreCase(provider)) {
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                throw new IllegalStateException("Configure GEMINI_API_KEY para usar app.ai.provider=gemini");
            }

            return googleAiGeminiChatModel(geminiApiKey, geminiModel);
        }

        throw new IllegalArgumentException("Provider de IA nao suportado: " + provider);
    }

    private GoogleAiGeminiChatModel googleAiGeminiChatModel(String geminiApiKey, String geminiModel) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModel)
                .thinkingConfig(GeminiThinkingConfig.builder()
                        .includeThoughts(true)
                        .thinkingLevel(GeminiThinkingConfig.GeminiThinkingLevel.LOW)
                        .build())
                .returnThinking(true)
                .sendThinking(true)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        // Este provider cria um novo InMemory storage para armazenar o ChatMemory
        // Nós usamos MessageWindow para manter as ultimas 10 mensagens.

        return memoryId -> MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(10)
                .build();
    }

    @Bean
    public AssistantAiService assistant(
            ChatModel model,
            AssistantTools assistantTools,
            CustomerTools customerTools,
            ReservationTools reservationTools,
            RetrievalAugmentor retrievalAugmentor) {
        return AiServices.builder(AssistantAiService.class)
                .chatModel(model)
                .tools(assistantTools, customerTools, reservationTools)
                .chatMemoryProvider(chatMemoryProvider())
                .retrievalAugmentor(retrievalAugmentor)
                .inputGuardrails(new RentalScopeInputGuardrail())
                .outputGuardrails(new RentalScopeOutputGuardrail())
                .build();
    }

}
