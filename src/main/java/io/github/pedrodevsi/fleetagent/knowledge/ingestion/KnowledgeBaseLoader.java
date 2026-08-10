package io.github.pedrodevsi.fleetagent.knowledge.ingestion;

import io.github.pedrodevsi.fleetagent.knowledge.application.KnowledgeIngestionService;
import io.github.pedrodevsi.fleetagent.knowledge.dto.DocumentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * Aqui quando o spring boot sobe, ele ja indexa os documentos.
 */
@Component
public class KnowledgeBaseLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseLoader.class);

    private final KnowledgeIngestionService knowledgeIngestionService;

    public KnowledgeBaseLoader(KnowledgeIngestionService knowledgeIngestionService) {
        this.knowledgeIngestionService = knowledgeIngestionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.atInfo().log("Iniciando carregamento da base de conhecimento");

        knowledgeIngestionService.ingestIfChanged(new DocumentDefinition("knowledge/politica-combustivel.md", "politica-combustivel.md", "combustivel", "Política de Combustível"));
        knowledgeIngestionService.ingestIfChanged(new DocumentDefinition("knowledge/politica-documentos.md", "politica-documentos.md", "documentos", "Política de Documentos"));
        knowledgeIngestionService.ingestIfChanged(new DocumentDefinition("knowledge/politica-pagamentos.md", "politica-pagamentos.md", "pagamentos", "Política de Pagamentos"));
        knowledgeIngestionService.ingestIfChanged(new DocumentDefinition("knowledge/politica-seguros.md", "politica-seguros.md", "seguros", "Política de Seguros"));
        knowledgeIngestionService.ingestIfChanged(new DocumentDefinition("knowledge/processo-locacao.md", "processo-locacao.md", "processo-locacao", "Processo de Locação"));

        logger.atInfo().log("Carregamento da base de conhecimento finalizado");
    }
}
