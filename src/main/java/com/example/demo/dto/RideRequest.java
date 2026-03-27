package com.example.demo.dto;

public class RideRequest {
    private Long userId;
    private Long carId;
    private String startTime;
    private String endTime;
    private double distance;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
}
