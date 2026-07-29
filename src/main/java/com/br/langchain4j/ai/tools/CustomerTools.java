package com.br.langchain4j.ai.tools;

import com.br.langchain4j.customer.api.CustomerUseCase;
import com.br.langchain4j.customer.api.CreateCustomerRequest;
import com.br.langchain4j.customer.api.CustomerLookupResponse;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CustomerTools {

    private final CustomerUseCase customerUseCase;

    public CustomerTools(CustomerUseCase customerUseCase) {
        this.customerUseCase = customerUseCase;
    }

    @Tool("Busca usuário usando o documento fornecido por ele")
    public CustomerLookupResponse findCustomerByDocument(
            @P("Documento que o usuário vai informar que deverá ser usado para encontrá-lo no sistema") String document
    ) {
        return customerUseCase.findByDocument(document);
    }

    @Tool("Cadastra um novo cliente com os dados informados.")
    public CustomerLookupResponse createCustomer(
            @P("Nome completo") String name,
            @P("CPF") String document,
            @P("Email") String email,
            @P("Telefone") String phone
    ) {
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                name,
                document,
                email,
                phone,
                "INDIVIDUAL"
        );
        return customerUseCase.createNewCustomer(customerRequest);
    }


}
