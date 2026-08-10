package io.github.pedrodevsi.fleetagent.ai.api;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.OutputGuardrailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                messages
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InputGuardrailException.class)
    public ResponseEntity<ApiErrorResponse> handleInputGuardrail(InputGuardrailException exception) {
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(400, "Guardrail failed", List.of(exception.getMessage()))
        );
    }

    @ExceptionHandler(OutputGuardrailException.class)
    public ResponseEntity<ApiErrorResponse> handleOutputGuardrail(OutputGuardrailException exception) {
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(400, "Guardrail failed", List.of(exception.getMessage()))
        );
    }
}
