package com.exelynt.booking.service;

import com.exelynt.booking.dto.CreateReservationRequest;
import com.exelynt.booking.dto.ReservationDTO;
import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.exception.AccessDeniedException;
import com.exelynt.booking.exception.InvalidBookingException;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ReservationSpecifications;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;


    // =========================
    // CREATE RESERVATION
    // =========================

    public ReservationDTO createReservation(
            CreateReservationRequest request,
            String username) {

        // Validate time
        if (request.getStartTime() == null ||
                request.getEndTime() == null) {

            throw new InvalidBookingException(
                    "Start time and end time are required.");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {

            throw new InvalidBookingException(
                    "End time must be strictly after start time.");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now())) {

            throw new InvalidBookingException(
                    "Cannot create reservations in the past.");
        }


        // Find resource
        Resource resource = resourceRepository
                .findById(request.getResourceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Resource not found with ID: "
                                        + request.getResourceId()));


        // Get logged-in user
        User user = userRepository
                .findByUserName(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + username));

// Calculate booking duration
long minutes = Duration.between(
        request.getStartTime(),
        request.getEndTime()
).toMinutes();

// Duration must be at least 1 minute
if (minutes < 1) {
    throw new InvalidBookingException(
            "Reservation duration must be at least 1 minute.");
}

double hours = minutes / 60.0;


// Validate resource hourly price
if (resource.getPricePerHour() == null ||
        resource.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {

    throw new InvalidBookingException(
            "Resource price per hour must be greater than zero.");
}


// Calculate reservation price
BigDecimal price = resource
        .getPricePerHour()
        .multiply(BigDecimal.valueOf(hours));

// Calculated reservation price must be positive
if (price.compareTo(BigDecimal.ZERO) <= 0) {
    throw new InvalidBookingException(
            "Reservation price must be greater than zero.");
}


        // Create reservation
        Reservation reservation = new Reservation();

        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());

        // New reservation starts as PENDING
        reservation.setStatus(ReservationStatus.PENDING);

        reservation.setPrice(price);


        Reservation saved =
                reservationRepository.save(reservation);

        return mapToDTO(saved);
    }


    // GET RESERVATIONS
   

    public Page<ReservationDTO> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            String username) {

        User user = userRepository
                .findByUserName(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + username));


        Specification<Reservation> spec =
                Specification
                        .where(ReservationSpecifications.hasStatus(status))
                        .and(ReservationSpecifications.hasMinPrice(minPrice))
                        .and(ReservationSpecifications.hasMaxPrice(maxPrice));


        // USER can see only own reservations
        if (user.getRole() != Role.ROLE_ADMIN) {

            spec = spec.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("user").get("id"),
                                    user.getId()
                            )
            );
        }


        return reservationRepository
                .findAll(spec, pageable)
                .map(this::mapToDTO);
    }

    
    // GET RESERVATION BY ID

    public ReservationDTO getReservationById(
            Long reservationId,
            String username) {

        // Find reservation
        Reservation reservation =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reservation not found with ID: "
                                                + reservationId));


        // Find logged-in user
        User user =
                userRepository
                        .findByUserName(username)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found: "
                                                + username));


        // USER can access only own reservation
        // ADMIN can access any reservation
        if (user.getRole() != Role.ROLE_ADMIN &&
                !reservation.getUser().getId()
                        .equals(user.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to view this reservation.");
        }


        return mapToDTO(reservation);
    }

    // CANCEL RESERVATION
    
    public ReservationDTO cancelReservation(
            Long id,
            String username) {

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reservation not found with ID: "
                                                + id));


        User user = userRepository
                .findByUserName(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + username));


        // USER can cancel only own reservation
        // ADMIN can cancel any reservation
        if (user.getRole() != Role.ROLE_ADMIN &&
                !reservation.getUser().getId()
                        .equals(user.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to cancel this reservation.");
        }


        reservation.setStatus(
                ReservationStatus.CANCELLED);


        return mapToDTO(
                reservationRepository.save(reservation)
        );
    }

    // CONFIRM RESERVATION
    // ADMIN ONLY
    
    public ReservationDTO confirmReservation(Long id) {

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reservation not found with ID: "
                                                + id));


        reservation.setStatus(
                ReservationStatus.CONFIRMED);


        return mapToDTO(
                reservationRepository.save(reservation)
        );
    }


    // DELETE RESERVATION
    // ADMIN ONLY
    
    public void deleteReservation(Long id) {

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reservation not found with ID: "
                                                + id));


        reservationRepository.delete(reservation);
    }

    private ReservationDTO mapToDTO(
            Reservation reservation) {

        return new ReservationDTO(
                reservation.getId(),
                reservation.getUser().getUserName(),
                reservation.getResource().getName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getPrice()
        );
    }
}