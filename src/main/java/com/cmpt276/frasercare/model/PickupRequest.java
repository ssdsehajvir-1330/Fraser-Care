package com.cmpt276.frasercare.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "pickup_requests")
public class PickupRequest {

    private static final ZoneId TIME_ZONE = ZoneId.of("America/Vancouver");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pickup_id")
    private Long pickupId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "notes")
    private String notes;

    @Column(nullable = false)
    private String status = "RECEIVED";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PickupRequest() {
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now(TIME_ZONE);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now(TIME_ZONE);
    }

    public Long getPickupId() {
        return pickupId;
    }

    public void setPickupId(Long pickupId) {
        this.pickupId = pickupId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}