package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cars", uniqueConstraints = @UniqueConstraint(columnNames = "plate_number"))
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "plate_number", nullable = false, unique = true)
    private String plateNumber;

    @Column(nullable = false)
    private boolean available = true;

    public Car() {}

    public Car(String brand, String model, String plateNumber) {
        this.brand = brand;
        this.model = model;
        this.plateNumber = plateNumber;
        this.available = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
