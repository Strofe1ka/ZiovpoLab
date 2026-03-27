package com.example.demo.repository;

import com.example.demo.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByCar_Id(Long carId);
}
