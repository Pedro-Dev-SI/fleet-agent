package io.github.pedrodevsi.fleetagent.rental.application;

import io.github.pedrodevsi.fleetagent.rental.api.QuotationUseCase;
import io.github.pedrodevsi.fleetagent.rental.domain.RentalCategory;
import io.github.pedrodevsi.fleetagent.rental.repository.RentalCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class QuotationService implements QuotationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(QuotationService.class);

    private final RentalCategoryRepository rentalCategoryRepository;

    public QuotationService(RentalCategoryRepository rentalCategoryRepository) {
        this.rentalCategoryRepository = rentalCategoryRepository;
    }

    public String calculateQuotation(
        String category,
        int days
    ) {
        logger.info("Request to calculate quotation for category: {} and days: {}", category, days);

        if (category == null || category.isBlank()) {
            logger.warn("Quotation request rejected because category is null or blank");
            return "Informe uma categoria valida: economico, suv ou premium.";
        }

        if (days <= 0) {
            logger.warn("Quotation request rejected because days is invalid: {}", days);
            return "Informe uma quantidade de dias maior que zero.";
        }

        String normalizedCategory = category.trim().toLowerCase();

        RentalCategory rentalCategory = rentalCategoryRepository.findByCodeIgnoreCase(normalizedCategory)
                .orElseThrow(() -> new RentalCategoryNotFoundException(
                        "Categoria de codigo: " + category + " nao encontrada"
                ));
        logger.info("Rental category found for quotation: {}", normalizedCategory);

        BigDecimal base = rentalCategory.getDailyBasePrice();
        BigDecimal rate = rentalCategory.getInsuranceRate();

        BigDecimal subtotal = base.multiply(BigDecimal.valueOf(days));
        BigDecimal total = subtotal.multiply(BigDecimal.ONE.add(rate))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal insurancePercentage = rate.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);

        logger.info("Quotation calculated for category: {} and days: {}", normalizedCategory, days);

        return String.format(
                "Cotação: %s por %d dias -> R$ %s (inclui seguro %s%%)",
                normalizedCategory, days, formatCurrencyValue(total), insurancePercentage
        );
    }

    private String formatCurrencyValue(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                .replace(".", ",");
    }
}
