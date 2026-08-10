package io.github.pedrodevsi.fleetagent.ai.tools;

import io.github.pedrodevsi.fleetagent.rental.api.QuotationUseCase;
import io.github.pedrodevsi.fleetagent.rental.api.VehicleAvailabilityUseCase;
import io.github.pedrodevsi.fleetagent.rental.api.AvailableCarResponse;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AssistantTools {

    private final QuotationUseCase quotationUseCase;
    private final VehicleAvailabilityUseCase vehicleAvailabilityUseCase;

    public AssistantTools(QuotationUseCase quotationUseCase, VehicleAvailabilityUseCase vehicleAvailabilityUseCase) {
        this.quotationUseCase = quotationUseCase;
        this.vehicleAvailabilityUseCase = vehicleAvailabilityUseCase;
    }


    @Tool("Calcula o valor total do aluguel corporativo com base na categoria do carro e número de dias.")
    public String calculateQuotation(
            @P("As categorias existentes: economico, suv e premium") String category,
            @P("Dias que o cliente deseja locar o carro: deve ser maior do que zero") int days
    ) {
        return quotationUseCase.calculateQuotation(category, days);
    }
    @Tool("Retorna a lista de carros disponíveis para locação dada a categoria.")
    public List<AvailableCarResponse> checkAvailableCarsByCategory(
            @P("Codigo da categoria: economico, suv e premium") String category
    ) {
        return vehicleAvailabilityUseCase.listAllAvailableCarsByCategory(category);
    }
}
