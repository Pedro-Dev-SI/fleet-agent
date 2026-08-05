package com.br.langchain4j.knowledge.application;

import com.br.langchain4j.knowledge.dto.DocumentDefinition;
import com.br.langchain4j.knowledge.infra.KnowledgeDocumentRepository;
import com.br.langchain4j.knowledge.infra.KnowledgeEmbeddingRepository;
import com.br.langchain4j.knowledge.ingestion.DocumentIngestionService;
import com.br.langchain4j.knowledge.utils.HashDocument;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final DocumentIngestionService documentIngestionService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeEmbeddingRepository knowledgeEmbeddingRepository;

    public KnowledgeIngestionService(
            DocumentIngestionService documentIngestionService,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeEmbeddingRepository knowledgeEmbeddingRepository
    ) {
        this.documentIngestionService = documentIngestionService;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeEmbeddingRepository = knowledgeEmbeddingRepository;
    }

    @Transactional
    public void ingestIfChanged(DocumentDefinition docDefinition) {
        logger.atInfo()
                .addKeyValue("path", docDefinition.path())
                .addKeyValue("source", docDefinition.source())
                .addKeyValue("category", docDefinition.category())
                .log("Carregando documento da base de conhecimento");

        Document document = ClassPathDocumentLoader.loadDocument(docDefinition.path());

        // O hash identifica a versao real do arquivo. Mesmo source + mesmo hash significa que nada mudou.
        String contentHash = HashDocument.sha256(document.text());

        if (knowledgeDocumentRepository.existsBySourceAndHash(docDefinition.source(), contentHash)) {
            logger.atInfo()
                    .addKeyValue("source", docDefinition.source())
                    .addKeyValue("contentHash", contentHash)
                    .log("Documento ja indexado nessa versao");
            return;
        }

        // Se chegou aqui, o documento é novo ou mudou. Removemos chunks antigos para evitar resposta com politica obsoleta.
        int deleted = knowledgeEmbeddingRepository.deleteBySource(docDefinition.source());

        logger.atInfo()
                .addKeyValue("source", docDefinition.source())
                .addKeyValue("deletedEmbeddings", deleted)
                .log("Embeddings antigos removidos");

        document.metadata().put("title", docDefinition.title());
        document.metadata().put("source", docDefinition.source());
        document.metadata().put("category", docDefinition.category());
        document.metadata().put("content_hash", contentHash);

        // @Transactional para garantir atomicidade para as operacoes JDBC abaixo.
        int segments = documentIngestionService.ingest(document);

        knowledgeDocumentRepository.upsert(
                docDefinition.source(),
                docDefinition.title(),
                docDefinition.category(),
                contentHash
        );

        logger.atInfo()
                .addKeyValue("source", docDefinition.source())
                .addKeyValue("category", docDefinition.category())
                .addKeyValue("contentHash", contentHash)
                .addKeyValue("segments", segments)
                .log("Documento da base de conhecimento indexado");
    }
}
