package io.github.pedrodevsi.fleetagent.knowledge.infra;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("rag")
public class RagHealthIndicator implements HealthIndicator {

    private static final String VALIDATION_QUERY = "documentos obrigatorios para locacao";

    private final ContentRetriever contentRetriever;

    public RagHealthIndicator(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Override
    public Health health() {
        try {
            // Esta busca exercita o fluxo principal do RAG:
            // pergunta -> embedding da pergunta -> busca no pgvector -> retorno de chunks relevantes.
            List<Content> contents = contentRetriever.retrieve(Query.from(VALIDATION_QUERY));

            if (contents.isEmpty()) {
                // Se nao voltou nenhum chunk, o banco pode estar vazio, a ingestion pode ter falhado
                // ou o minScore do retriever pode estar alto demais para os documentos atuais.
                return Health.down()
                        .withDetail("query", VALIDATION_QUERY)
                        .withDetail("reason", "Nenhum conteudo recuperado da base vetorial")
                        .build();
            }

            return Health.up()
                    .withDetail("query", VALIDATION_QUERY)
                    .withDetail("retrievedContents", contents.size())
                    .build();
        } catch (Exception exception) {
            // Qualquer erro aqui indica que o RAG nao esta operacional:
            // pode ser Ollama indisponivel, pgvector fora do ar, tabela ausente ou falha de conexao.
            return Health.down(exception)
                    .withDetail("query", VALIDATION_QUERY)
                    .build();
        }
    }
}
