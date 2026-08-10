package io.github.pedrodevsi.fleetagent.rental.application;

import io.github.pedrodevsi.fleetagent.rental.domain.RentalCategory;
import io.github.pedrodevsi.fleetagent.rental.repository.RentalCategoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuotationServiceTest {

    private final RentalCategoryRepository rentalCategoryRepository = mock(RentalCategoryRepository.class);
    private final QuotationService quotationService = new QuotationService(rentalCategoryRepository);

    @Test
    void shouldCalculateQuotationUsingRentalCategoryFromRepository() {
        RentalCategory suv = new RentalCategory(
                "suv",
                "SUV",
                new BigDecimal("280.00"),
                new BigDecimal("0.0800"),
                true
        );
        when(rentalCategoryRepository.findByCodeIgnoreCase("suv")).thenReturn(Optional.of(suv));

        String result = quotationService.calculateQuotation(" SUV ", 3);

        assertThat(result).isEqualTo("Cotação: suv por 3 dias -> R$ 907,20 (inclui seguro 8%)");
    }

    @Test
    void shouldReturnFriendlyMessageWhenCategoryIsBlank() {
        String result = quotationService.calculateQuotation(" ", 3);

        assertThat(result).isEqualTo("Informe uma categoria valida: economico, suv ou premium.");
        verify(rentalCategoryRepository, never()).findByCodeIgnoreCase(anyString());
    }

    @Test
    void shouldReturnFriendlyMessageWhenDaysIsZeroOrNegative() {
        String result = quotationService.calculateQuotation("suv", 0);

        assertThat(result).isEqualTo("Informe uma quantidade de dias maior que zero.");
        verify(rentalCategoryRepository, never()).findByCodeIgnoreCase(anyString());
    }

    @Test
    void shouldThrowDomainExceptionWhenCategoryDoesNotExist() {
        when(rentalCategoryRepository.findByCodeIgnoreCase("moto")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotationService.calculateQuotation("moto", 2))
                .isInstanceOf(RentalCategoryNotFoundException.class)
                .hasMessage("Categoria de codigo: moto nao encontrada");
    }
}
