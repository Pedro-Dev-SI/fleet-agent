package io.github.pedrodevsi.fleetagent.knowledge.dto;

public record DocumentDefinition(
        String path,
        String source,
        String category,
        String title
) {
}
