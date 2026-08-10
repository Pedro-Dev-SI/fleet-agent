package io.github.pedrodevsi.fleetagent.knowledge.infra;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int deleteBySource(String source) {
        return jdbcTemplate.update(
                """
                  DELETE FROM knowledge_embeddings
                  WHERE metadata ->> 'source' = ?
                  """,
                source
        );
    }
}

