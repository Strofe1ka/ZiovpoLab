package com.example.demo.dto;

public class RideResponse {
    private Long id;
    private Long userId;
    private Long carId;
    private String startTime;
    private String endTime;
    private double distance;
    private double cost;

    public RideResponse() {}

    public RideResponse(Long id, Long userId, Long carId, String startTime, String endTime, double distance, double cost) {
        this.id = id;
        this.userId = userId;
        this.carId = carId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distance = distance;
        this.cost = cost;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
}
