package com.br.langchain4j.customer.api;

import java.util.UUID;

public interface CustomerUseCase {

    CustomerLookupResponse createNewCustomer(CreateCustomerRequest request);

    CustomerLookupResponse findByDocument(String document);

    CustomerLookupResponse findById(UUID id);
}
