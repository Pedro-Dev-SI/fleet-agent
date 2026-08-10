package io.github.pedrodevsi.fleetagent.rental.application;

import io.github.pedrodevsi.fleetagent.rental.api.VehicleAvailabilityUseCase;
import io.github.pedrodevsi.fleetagent.rental.domain.Car;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import io.github.pedrodevsi.fleetagent.rental.api.AvailableCarResponse;
import io.github.pedrodevsi.fleetagent.rental.repository.CarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService implements VehicleAvailabilityUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CarService.class);


    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public List<AvailableCarResponse> listAllAvailableCarsByCategory(String categoryCode) {
        logger.info("Request to list all cars by category: {}", categoryCode);

        if (categoryCode == null || categoryCode.isBlank()) {
            logger.warn("Car listing request rejected because category is null or blank");
            throw new CategoryNullException("Categoria de carro não pode ser nula");
        }

        List<Car> carsFound = carRepository.findAllByCategoryCodeAndStatus(categoryCode, StatusVeichleEnum.DISPONIVEL);
        logger.info("Found {} available cars for category: {}", carsFound.size(), categoryCode);

        return carsFound.stream().map(car -> new AvailableCarResponse(
                car.getModel(),
                car.getCategory().getCode(),
                car.getStatus().name()
        )).toList();
    }

    public Car findCarByModel(String model) {
        return carRepository.findByModel(model)
                .orElseThrow(() -> new CarModelNotFoundException("Modelo de carro não econtrado no banco de dados"));
    }

    public boolean checkAvailabilityByCarModel(String carModel) {
        return carRepository.existsByModelAndStatus(carModel, StatusVeichleEnum.DISPONIVEL);
    }
}
