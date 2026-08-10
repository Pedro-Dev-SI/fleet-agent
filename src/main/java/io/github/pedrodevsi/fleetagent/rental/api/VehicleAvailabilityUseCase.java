package io.github.pedrodevsi.fleetagent.rental.api;

import java.util.List;

public interface VehicleAvailabilityUseCase {

    List<AvailableCarResponse> listAllAvailableCarsByCategory(String category);
}
