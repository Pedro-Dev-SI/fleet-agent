package com.br.langchain4j.rental.application;

import com.br.langchain4j.rental.api.VehicleAvailabilityUseCase;
import com.br.langchain4j.rental.domain.Car;
import com.br.langchain4j.rental.domain.enums.StatusVeichleEnum;
import com.br.langchain4j.rental.api.AvailableCarResponse;
import com.br.langchain4j.rental.repository.CarRepository;
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
