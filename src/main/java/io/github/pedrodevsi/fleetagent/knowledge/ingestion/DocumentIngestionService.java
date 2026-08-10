package io.github.pedrodevsi.fleetagent.knowledge.ingestion;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Esse é o indexing.
 * Ele faz:
 * Document -> split -> embedding -> store
 */
@Service
public class DocumentIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentIngestionService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public int ingest(Document document) {
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);

        List<TextSegment> segments = splitter.split(document);

        logger.atInfo()
                .addKeyValue("segments", segments.size())
                .log("Documento dividido para geração de embeddings");

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        if (segments.size() != embeddings.size()) {
            logger.atError()
                    .addKeyValue("segments", segments.size())
                    .addKeyValue("embeddings", embeddings.size())
                    .log("Falha ao gerar embeddings para todos os segmentos");

            throw new IllegalStateException("Falha ao gerar embeddings para todos os segmentos.");
        }

        embeddingStore.addAll(embeddings, segments);

        logger.atInfo()
                .addKeyValue("segments", segments.size())
                .log("Embeddings adicionados ao store");

        return segments.size();
    }
}
