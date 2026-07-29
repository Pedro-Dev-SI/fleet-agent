package com.br.langchain4j.rental.api;

public interface QuotationUseCase {

    String calculateQuotation(String category, int days);
}
