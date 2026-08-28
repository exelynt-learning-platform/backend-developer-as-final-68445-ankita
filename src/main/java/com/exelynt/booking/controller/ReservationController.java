package com.exelynt.booking.controller;

import com.exelynt.booking.dto.CreateReservationRequest;
import com.exelynt.booking.dto.ReservationDTO;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.service.ReservationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@Validated
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    // USER and ADMIN can create reservations
    @PostMapping
    public ResponseEntity<ReservationDTO> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {

        String username = getAuthenticatedUsername(authentication);

        return new ResponseEntity<>(
                reservationService.createReservation(request, username),
                HttpStatus.CREATED);
    }

    // ADMIN sees all reservations
    // USER sees only their own reservations
    @GetMapping
    public ResponseEntity<Page<ReservationDTO>> getReservations(

            @RequestParam(required = false) ReservationStatus status,

            @RequestParam(required = false) BigDecimal minPrice,

            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,

            @RequestParam(defaultValue = "startTime") String sortBy,

            @RequestParam(defaultValue = "asc") String sortDir,

            Authentication authentication) {

        Sort sort;

        if (sortDir.equalsIgnoreCase("desc")) {
            sort = Sort.by(sortBy).descending();
        } else {
            sort = Sort.by(sortBy).ascending();
        }

        PageRequest pageable = PageRequest.of(page, size, sort);

        String username = getAuthenticatedUsername(authentication);

        return ResponseEntity.ok(
                reservationService.getReservations(
                        status,
                        minPrice,
                        maxPrice,
                        pageable,
                        username));
    }

    // Get one reservation
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {

        String username = getAuthenticatedUsername(authentication);

        return ResponseEntity.ok(
                reservationService.getReservationById(
                        id,
                        username));
    }

    // USER can cancel own reservation
    // ADMIN can cancel any reservation
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ReservationDTO> cancelReservation(
            @PathVariable Long id,
            Authentication authentication) {

        String username = getAuthenticatedUsername(authentication);

        return ResponseEntity.ok(
                reservationService.cancelReservation(
                        id,
                        username));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationDTO> confirmReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirmReservation(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // Helper method to safely get authenticated username
    private String getAuthenticatedUsername(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        return authentication.getName();
    }
}
