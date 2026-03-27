package com.example.demo.service;

import com.example.demo.entity.Car;
import com.example.demo.repository.CarRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    private final CarRepository carRepository;
    private final RideRepository rideRepository;
    private final PaymentRepository paymentRepository;

    public CarService(CarRepository carRepository, RideRepository rideRepository,
                      PaymentRepository paymentRepository) {
        this.carRepository = carRepository;
        this.rideRepository = rideRepository;
        this.paymentRepository = paymentRepository;
    }

    public Car createCar(Car car) {
        car.setAvailable(true);
        return carRepository.save(car);
    }

    public List<Car> getAllCars(Boolean available) {
        if (available != null && available) {
            return carRepository.findByAvailableTrue();
        }
        return carRepository.findAll();
    }

    public List<Car> getAvailableCars() {
        return carRepository.findByAvailableTrue();
    }

    public Optional<Car> getCarById(Long id) {
        return carRepository.findById(id);
    }

    public Optional<Car> updateCar(Long id, Car updatedCar) {
        return carRepository.findById(id)
                .map(car -> {
                    car.setBrand(updatedCar.getBrand());
                    car.setModel(updatedCar.getModel());
                    car.setPlateNumber(updatedCar.getPlateNumber());
                    car.setAvailable(updatedCar.isAvailable());
                    return carRepository.save(car);
                });
    }

    @Transactional
    public boolean deleteCar(Long id) {
        if (!carRepository.existsById(id)) {
            return false;
        }
        return carRepository.findById(id)
                .map(car -> {
                    var rides = rideRepository.findByCar_Id(id);
                    if (!rides.isEmpty()) {
                        paymentRepository.deleteByRideIn(rides);
                        rideRepository.deleteAll(rides);
                    }
                    carRepository.delete(car);
                    return true;
                })
                .orElse(false);
    }
}
