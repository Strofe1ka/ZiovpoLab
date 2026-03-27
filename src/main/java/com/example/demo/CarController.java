package com.example.demo;

import com.example.demo.entity.Car;
import com.example.demo.service.CarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CarController {
    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @PostMapping("/cars")
    public Car createCar(@RequestBody Car car) {
        return carService.createCar(car);
    }

    @GetMapping("/cars")
    public List<Car> getAllCars(@RequestParam(required = false) Boolean available) {
        return carService.getAllCars(available);
    }

    @GetMapping("/cars/available")
    public List<Car> getAvailableCars() {
        return carService.getAvailableCars();
    }

    @GetMapping("/cars/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable long id) {
        return carService.getCarById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/cars/{id}")
    public ResponseEntity<Car> updateCar(@PathVariable long id, @RequestBody Car updatedCar) {
        return carService.updateCar(id, updatedCar)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/cars/{id}")
    public ResponseEntity<String> deleteCar(@PathVariable long id) {
        return carService.deleteCar(id)
                ? ResponseEntity.ok("Car deleted")
                : ResponseEntity.notFound().build();
    }
}
