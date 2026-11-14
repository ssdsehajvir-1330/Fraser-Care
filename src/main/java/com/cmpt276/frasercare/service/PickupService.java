package com.cmpt276.frasercare.service;

import com.cmpt276.frasercare.model.PickupRequest;
import com.cmpt276.frasercare.repository.PickupRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PickupService {

    // Order a pickup moves through
    public static final List<String> STATUSES =
            List.of("RECEIVED", "READY", "PICKED_UP");

    private static final int MIN_ITEM_LENGTH = 2;
    private static final int MAX_ITEM_LENGTH = 100;
    private static final int MAX_NOTES_LENGTH = 255;

    private final PickupRequestRepository pickupRepository;

    public PickupService(PickupRequestRepository pickupRepository) {
        this.pickupRepository = pickupRepository;
    }

    // Customer submits a new pickup request
    public PickupRequest createRequest(Long customerId, String itemName, String notes) {

        if (customerId == null) {
            throw new IllegalArgumentException("Customer account is required.");
        }

        String item = itemName == null ? "" : itemName.trim();

        if (item.length() < MIN_ITEM_LENGTH || item.length() > MAX_ITEM_LENGTH) {
            throw new IllegalArgumentException("Item name must be 2 to 100 characters.");
        }

        String cleanNotes = notes == null ? null : notes.trim();

        if (cleanNotes != null && cleanNotes.isEmpty()) {
            cleanNotes = null;
        }

        if (cleanNotes != null && cleanNotes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("Notes must be 255 characters or less.");
        }

        PickupRequest pickup = new PickupRequest();
        pickup.setCustomerId(customerId);
        pickup.setItemName(item);
        pickup.setNotes(cleanNotes);
        pickup.setStatus("RECEIVED");

        return pickupRepository.save(pickup);
    }

    // A customer's own pickups, newest first
    public List<PickupRequest> getCustomerPickups(Long customerId) {

        if (customerId == null) {
            throw new IllegalArgumentException("Customer account is required.");
        }

        return pickupRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    // All pickups for staff, with an optional status filter
    public List<PickupRequest> getAllPickups(String status) {

        if (status == null || status.isBlank()) {
            return pickupRepository.findAllByOrderByCreatedAtDesc();
        }

        String filter = status.trim().toUpperCase();

        if (!STATUSES.contains(filter)) {
            throw new IllegalArgumentException("Invalid pickup status.");
        }

        return pickupRepository.findByStatusOrderByCreatedAtDesc(filter);
    }

    // Staff move a pickup one step forward
    public PickupRequest updateStatus(Long pickupId, String newStatus) {

        if (pickupId == null || pickupId <= 0) {
            throw new IllegalArgumentException("Invalid pickup ID.");
        }

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }

        String next = newStatus.trim().toUpperCase();

        if (!STATUSES.contains(next)) {
            throw new IllegalArgumentException("Invalid pickup status.");
        }

        PickupRequest pickup = pickupRepository.findById(pickupId)
                .orElseThrow(() -> new IllegalArgumentException("Pickup request not found."));

        int current = STATUSES.indexOf(pickup.getStatus());

        if (current == STATUSES.size() - 1) {
            throw new IllegalStateException("This pickup has already been picked up.");
        }

        if (STATUSES.indexOf(next) != current + 1) {
            throw new IllegalStateException(
                    "A " + pickup.getStatus() + " pickup can only be changed to "
                    + STATUSES.get(current + 1) + ".");
        }

        pickup.setStatus(next);

        return pickupRepository.save(pickup);
    }
}