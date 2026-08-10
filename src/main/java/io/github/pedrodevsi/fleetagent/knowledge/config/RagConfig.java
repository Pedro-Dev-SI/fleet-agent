package io.github.pedrodevsi.fleetagent.knowledge.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagConfig {

    private static final Logger logger = LoggerFactory.getLogger(RagConfig.class);

    /**
     * Configurando modelo que fará a busca do documento.
     * @return
     */
    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${rag.embedding.base-url:http://localhost:11434}") String baseUrl,
            @Value("${rag.embedding.model:nomic-embed-text}") String modelName
    ) {
        logger.atInfo()
                .addKeyValue("baseUrl", baseUrl)
                .addKeyValue("modelName", modelName)
                .log("Configurando modelo de embedding do RAG");

        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    /**
     * Aqui vamos garantir que os metadados dos documentos da base de coenhcimento
     * entram no contexto enviado para a IA.
     * @param contentRetriever
     * @return
     */
    @Bean
    public RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(DefaultContentInjector.builder()
                        .metadataKeysToInclude(List.of("source", "title", "category"))
                        .build())
                .build();
    }

    /**
     * Guarda os vetores enquanto a aplicação esta rodando
     * @return
     */
//    @Bean
//    public EmbeddingStore<TextSegment> embeddingStore() {
//        return new InMemoryEmbeddingStore<>();
//    }

    /**
     * Guarda os vetores no banco de dados pgvector
     * @param host
     * @param port
     * @param database
     * @param user
     * @param password
     * @param table
     * @param dimension
     * @return
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${rag.vector-store.host}") String host,
            @Value("${rag.vector-store.port}") int port,
            @Value("${rag.vector-store.database}") String database,
            @Value("${rag.vector-store.user}") String user,
            @Value("${rag.vector-store.password}") String password,
            @Value("${rag.vector-store.table}") String table,
            @Value("${rag.vector-store.dimension}") int dimension
    ) {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(table)
                .dimension(dimension)
                .createTable(false)
                .build();
    }

    /**
     * Responsável por buscar trechos relevantes.
     * @return
     */
    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // -> Pega até 3 trechos
                .minScore(0.65) // -> Ignora trechos pouco parecidos
                .build();
    }
}
