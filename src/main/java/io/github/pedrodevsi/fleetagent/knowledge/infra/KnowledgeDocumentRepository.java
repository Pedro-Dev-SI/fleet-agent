package io.github.pedrodevsi.fleetagent.knowledge.infra;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeDocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsBySourceAndHash(String source, String contentHash) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT (*)
                FROM knowledge_documents
                WHERE source = ?
                AND content_hash = ?
                """,
                Integer.class,
                source,
                contentHash
        );

        return count != null && count > 0;
    }

    public void upsert(String source, String title, String category, String contentHash) {
        jdbcTemplate.update(
                """
                INSERT INTO knowledge_documents (
                    source,
                    title,
                    category,
                    content_hash,
                    ingested_at
                )
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (source)
                DO UPDATE SET
                    title = EXCLUDED.title,
                    category = EXCLUDED.category,
                    content_hash = EXCLUDED.content_hash,
                    ingested_at = EXCLUDED.ingested_at
                """,
                source,
                title,
                category,
                contentHash
        );
    }
}
