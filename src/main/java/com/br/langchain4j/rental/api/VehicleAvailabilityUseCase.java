package com.br.langchain4j.rental.api;

import java.util.List;

public interface VehicleAvailabilityUseCase {

    List<AvailableCarResponse> listAllAvailableCarsByCategory(String category);
}
