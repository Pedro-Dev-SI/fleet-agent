package io.github.pedrodevsi.fleetagent.ai.api;

import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        List<String> messages
) {
}
