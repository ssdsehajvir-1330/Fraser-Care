package com.cmpt276.frasercare.dto;

import com.cmpt276.frasercare.model.PickupRequest;

import java.time.LocalDateTime;

// Pickup info sent to the staff dashboard, with the customer's name
public record PickupResponse(
        Long pickupId,
        Long customerId,
        String customerName,
        String itemName,
        String notes,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PickupResponse from(PickupRequest pickup, String customerName) {
        return new PickupResponse(
                pickup.getPickupId(),
                pickup.getCustomerId(),
                customerName,
                pickup.getItemName(),
                pickup.getNotes(),
                pickup.getStatus(),
                pickup.getCreatedAt(),
                pickup.getUpdatedAt());
    }
}