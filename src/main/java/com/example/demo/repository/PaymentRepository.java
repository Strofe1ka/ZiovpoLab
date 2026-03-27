package com.example.demo.repository;

import com.example.demo.entity.Payment;
import com.example.demo.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.ride.user.id = :userId AND p.paid = true")
    Double sumPaidAmountByUserId(@Param("userId") Long userId);

    void deleteByRideIn(List<Ride> rides);
}
