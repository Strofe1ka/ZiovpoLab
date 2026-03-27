package com.example.demo;

import com.example.demo.dto.PaymentResponse;
import com.example.demo.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<?> createPayment(@RequestBody PaymentCreateRequest request) {
        var result = paymentService.createPayment(request.getRideId());
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.badRequest().body("Ride not found");
    }

    @GetMapping("/payments")
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/payments/{id}/pay")
    public ResponseEntity<PaymentResponse> payPayment(@PathVariable long id) {
        return paymentService.markAsPaid(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<String> deletePayment(@PathVariable long id) {
        return paymentService.deletePayment(id)
                ? ResponseEntity.ok("Payment deleted")
                : ResponseEntity.notFound().build();
    }

    public static class PaymentCreateRequest {
        private Long rideId;

        public Long getRideId() { return rideId; }
        public void setRideId(Long rideId) { this.rideId = rideId; }
    }
}
