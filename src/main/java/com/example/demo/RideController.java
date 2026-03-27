package com.example.demo;

import com.example.demo.dto.RideRequest;
import com.example.demo.dto.RideResponse;
import com.example.demo.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RideController {
    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/rides")
    public String createRide(@RequestBody RideRequest request) {
        return rideService.startRide(
                request.getUserId(),
                request.getCarId(),
                request.getStartTime()
        );
    }

    @GetMapping("/rides")
    public List<RideResponse> getAllRides() {
        return rideService.getAllRides();
    }

    @GetMapping("/rides/{id}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable long id) {
        return rideService.getRideById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/rides/{id}/end")
    public ResponseEntity<String> endRide(@PathVariable long id, @RequestBody RideRequest request) {
        String result = rideService.endRide(id, request.getEndTime(), request.getDistance());
        if (result.equals("Ride not found")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rides/{id}/pay")
    public ResponseEntity<?> payForRide(@PathVariable long id) {
        return rideService.payForRide(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/rides/{id}")
    public ResponseEntity<String> deleteRide(@PathVariable long id) {
        return rideService.deleteRide(id)
                ? ResponseEntity.ok("Ride deleted")
                : ResponseEntity.notFound().build();
    }
}
