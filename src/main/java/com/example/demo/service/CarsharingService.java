package com.example.demo.service;

import com.example.demo.dto.PaymentResponse;
import com.example.demo.dto.RideResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarsharingService {
    private static final double COST_PER_KM = 10.0;

    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final PaymentRepository paymentRepository;

    public CarsharingService(CarRepository carRepository, UserRepository userRepository,
                             RideRepository rideRepository, PaymentRepository paymentRepository) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.paymentRepository = paymentRepository;
    }

    // Business operation 1: Start a ride
    @Transactional
    public String startRide(Long userId, Long carId, String startTime) {
        Car car = carRepository.findById(carId).orElse(null);
        if (car == null) return "Car not found";

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "User not found";

        if (!car.isAvailable()) return "Car not available";

        Ride ride = new Ride();
        ride.setUser(user);
        ride.setCar(car);
        ride.setStartTime(startTime);
        ride = rideRepository.save(ride);

        car.setAvailable(false);
        carRepository.save(car);

        return "Ride started with id " + ride.getId();
    }

    // Business operation 2: End a ride
    @Transactional
    public String endRide(Long rideId, String endTime, double distance) {
        Ride ride = rideRepository.findById(rideId).orElse(null);
        if (ride == null) return "Ride not found";

        double cost = distance * COST_PER_KM;
        ride.setEndTime(endTime);
        ride.setDistance(distance);
        ride.setCost(cost);
        rideRepository.save(ride);

        Car car = ride.getCar();
        car.setAvailable(true);
        carRepository.save(car);

        return "Ride ended. Cost: " + cost;
    }

    // Business operation 3: Pay for a ride (create Payment and set paid = true)
    @Transactional
    public PaymentResponse payForRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElse(null);
        if (ride == null) throw new RuntimeException("Ride not found");

        Payment payment = new Payment();
        payment.setRide(ride);
        payment.setAmount(ride.getCost());
        payment.setPaid(true);
        payment = paymentRepository.save(payment);

        return new PaymentResponse(payment.getId(), ride.getId(), payment.getAmount(), true);
    }

    // Business operation 4: Get user income (sum of all paid rides)
    public double getUserIncome(Long userId) {
        Double sum = paymentRepository.sumPaidAmountByUserId(userId);
        return sum != null ? sum : 0.0;
    }

    // Business operation 5: List available cars
    public List<Car> getAvailableCars() {
        return carRepository.findByAvailableTrue();
    }

    public RideResponse toRideResponse(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getUser().getId(),
                ride.getCar().getId(),
                ride.getStartTime(),
                ride.getEndTime(),
                ride.getDistance(),
                ride.getCost()
        );
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRide().getId(),
                payment.getAmount(),
                payment.isPaid()
        );
    }
}
