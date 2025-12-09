package com.corems.common.utils.db.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class TestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String firstName;
    private String lastName;
    private String provider;

    private Instant createdAt;
    private Double balance;
    private Boolean isActive;

    public TestEntity() {}

    public TestEntity(String email, String firstName, String lastName, String provider) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.provider = provider;
    }

    // new constructor used by tests
    public TestEntity(String email, String firstName, String lastName, String provider, Instant createdAt, Double balance, Boolean isActive) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.provider = provider;
        this.createdAt = createdAt;
        this.balance = balance;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getProvider() { return provider; }

    public Instant getCreatedAt() { return createdAt; }
    public Double getBalance() { return balance; }
    public Boolean getIsActive() { return isActive; }

    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setBalance(Double balance) { this.balance = balance; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
