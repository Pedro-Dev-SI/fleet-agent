package io.github.pedrodevsi.fleetagent.rental.api;

public interface QuotationUseCase {

    String calculateQuotation(String category, int days);
}
