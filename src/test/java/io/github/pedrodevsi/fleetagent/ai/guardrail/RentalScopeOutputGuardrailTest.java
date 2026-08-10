package io.github.pedrodevsi.fleetagent.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RentalScopeOutputGuardrailTest {

    private final RentalScopeOutputGuardrail guardrail = new RentalScopeOutputGuardrail();

    @Test
    void shouldAllowNonBlankAnswer() {
        OutputGuardrailResult result = guardrail.validate(
                AiMessage.from("Cotação: suv por 3 dias -> R$ 907,20.")
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.failures()).isEmpty();
    }

    @Test
    void shouldBlockBlankAnswer() {
        OutputGuardrailResult result = guardrail.validate(AiMessage.from("   "));

        assertThat(result.isFatal()).isTrue();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().getFirst().message())
                .isEqualTo("Houve um problema com a resposta da IA, tente novamente.");
    }

}
