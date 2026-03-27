package com.example.demo.service;

import com.example.demo.dto.PaymentResponse;
import com.example.demo.dto.RideResponse;
import com.example.demo.entity.Ride;
import com.example.demo.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RideService {
    private final RideRepository rideRepository;
    private final CarsharingService carsharingService;

    public RideService(RideRepository rideRepository, CarsharingService carsharingService) {
        this.rideRepository = rideRepository;
        this.carsharingService = carsharingService;
    }

    public String startRide(Long userId, Long carId, String startTime) {
        return carsharingService.startRide(userId, carId, startTime);
    }

    public List<RideResponse> getAllRides() {
        return rideRepository.findAll().stream()
                .map(carsharingService::toRideResponse)
                .collect(Collectors.toList());
    }

    public Optional<RideResponse> getRideById(Long id) {
        return rideRepository.findById(id)
                .map(carsharingService::toRideResponse);
    }

    public String endRide(Long rideId, String endTime, double distance) {
        return carsharingService.endRide(rideId, endTime, distance);
    }

    public Optional<PaymentResponse> payForRide(Long rideId) {
        try {
            return Optional.of(carsharingService.payForRide(rideId));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public boolean deleteRide(Long id) {
        if (rideRepository.existsById(id)) {
            rideRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
