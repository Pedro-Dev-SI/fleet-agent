package com.br.langchain4j.customer.application;

import com.br.langchain4j.customer.api.CustomerUseCase;
import com.br.langchain4j.customer.domain.Customer;
import com.br.langchain4j.customer.domain.enums.CustomerType;
import com.br.langchain4j.customer.api.CreateCustomerRequest;
import com.br.langchain4j.customer.api.CustomerLookupResponse;
import com.br.langchain4j.customer.api.CustomerResponse;
import com.br.langchain4j.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService implements CustomerUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerLookupResponse createNewCustomer(CreateCustomerRequest request) {
        logger.info("Request to create customer with document: {}", request != null ? request.document() : null);

        validateRequest(request);

        String normalizedDocument = normalizeDocument(request.document());

        return customerRepository.findByDocument(normalizedDocument)
                .map(customer -> {
                    logger.info("Customer already exists for document: {}", normalizedDocument);
                    return new CustomerLookupResponse(
                            true,
                            toResponse(customer),
                            "Cliente já existe no sistema"
                    );
                })
                .orElseGet(() -> {
                    var customerResponse = createCustomer(request, normalizedDocument);
                    logger.info("Customer created with document: {}", normalizedDocument);
                    return new CustomerLookupResponse(
                            true,
                            customerResponse,
                            "Cliente criado com sucesso"
                    );
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerLookupResponse findByDocument(String document) {
        logger.info("Request to find customer by document: {}", document);

        if (document == null || document.isBlank()) {
            logger.warn("Customer lookup rejected because document is null or blank");
            return new CustomerLookupResponse(false, null, "Documento nulo");
        }

        String normalizedDocument = normalizeDocument(document);
        Optional<Customer> customerDb = customerRepository.findByDocument(normalizedDocument);

        if (customerDb.isEmpty()) {
            logger.info("Customer not found for document: {}", normalizedDocument);
            return new CustomerLookupResponse(
                    false,
                    null,
                    "Cliente não encontrado"
            );
        }

        var customer = customerDb.get();
        logger.info("Customer found for document: {}", normalizedDocument);

        return new CustomerLookupResponse(
                true,
                toResponse(customer),
                "Usuário encontrado no sistema"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerLookupResponse findById(UUID id) {
        logger.info("Request to find customer by id: {}", id);

        if (id == null) {
            logger.warn("Customer lookup rejected because id is null");
            return new CustomerLookupResponse(false, null, "Identificador do cliente nulo");
        }

        return customerRepository.findById(id)
                .map(customer -> new CustomerLookupResponse(
                        true,
                        toResponse(customer),
                        "Usuário encontrado no sistema"
                ))
                .orElseGet(() -> new CustomerLookupResponse(
                        false,
                        null,
                        "Cliente não encontrado"
                ));
    }

    private CustomerResponse createCustomer(CreateCustomerRequest request, String normalizedDocument) {
        logger.info("Persisting new customer with document: {}", normalizedDocument);

        Customer customer = new Customer(
                request.name().trim(),
                normalizedDocument,
                normalizeOptionalValue(request.email()),
                normalizeOptionalValue(request.phone()),
                parseType(request.type())
        );

        return toResponse(customerRepository.save(customer));
    }

    private void validateRequest(CreateCustomerRequest request) {
        if (request == null) {
            logger.warn("Customer creation rejected because request body is null");
            throw new CustomerValidationException("Dados do cliente são obrigatórios.");
        }

        if (request.name() == null || request.name().isBlank()) {
            logger.warn("Customer creation rejected because name is null or blank");
            throw new CustomerValidationException("Nome do cliente é obrigatório.");
        }

        if (request.document() == null || request.document().isBlank()) {
            logger.warn("Customer creation rejected because document is null or blank");
            throw new CustomerValidationException("Documento do cliente é obrigatório.");
        }
    }

    private CustomerType parseType(String type) {
        if (type == null || type.isBlank()) {
            return CustomerType.INDIVIDUAL;
        }

        try {
            return CustomerType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            logger.warn("Customer creation rejected because customer type is invalid: {}", type);
            throw new CustomerValidationException("Tipo de cliente inválido. Use INDIVIDUAL ou COMPANY.");
        }
    }

    private String normalizeDocument(String document) {
        return document.replaceAll("\\D", "");
    }

    private String normalizeOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getDocument(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getType().name()
        );
    }
}
