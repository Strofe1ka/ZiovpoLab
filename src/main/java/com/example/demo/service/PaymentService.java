package com.example.demo.service;

import com.example.demo.dto.PaymentResponse;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Ride;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;
    private final CarsharingService carsharingService;

    public PaymentService(PaymentRepository paymentRepository, RideRepository rideRepository,
                          CarsharingService carsharingService) {
        this.paymentRepository = paymentRepository;
        this.rideRepository = rideRepository;
        this.carsharingService = carsharingService;
    }

    @Transactional
    public Optional<PaymentResponse> createPayment(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElse(null);
        if (ride == null) return Optional.empty();

        Payment payment = new Payment();
        payment.setRide(ride);
        payment.setAmount(ride.getCost());
        payment.setPaid(false);
        payment = paymentRepository.save(payment);

        return Optional.of(carsharingService.toPaymentResponse(payment));
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(carsharingService::toPaymentResponse)
                .collect(Collectors.toList());
    }

    public Optional<PaymentResponse> getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(carsharingService::toPaymentResponse);
    }

    @Transactional
    public Optional<PaymentResponse> markAsPaid(Long id) {
        return paymentRepository.findById(id)
                .map(payment -> {
                    payment.setPaid(true);
                    payment = paymentRepository.save(payment);
                    return carsharingService.toPaymentResponse(payment);
                });
    }

    public boolean deletePayment(Long id) {
        if (paymentRepository.existsById(id)) {
            paymentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
