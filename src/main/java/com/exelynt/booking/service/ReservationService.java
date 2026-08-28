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
import java.math.RoundingMode;
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

        // CREATE RESERVATION

        public ReservationDTO createReservation(CreateReservationRequest request, String username) {

                validateBookingTimes(request.getStartTime(), request.getEndTime());

                Resource resource = resourceRepository.findById(request.getResourceId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Resource not found with ID: " + request.getResourceId()));

                User user = userRepository.findByUserName(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

                BigDecimal price = calculatePrice(resource, request.getStartTime(), request.getEndTime());

                Reservation reservation = new Reservation();
                reservation.setUser(user);
                reservation.setResource(resource);
                reservation.setStartTime(request.getStartTime());
                reservation.setEndTime(request.getEndTime());
                reservation.setStatus(ReservationStatus.PENDING);
                reservation.setPrice(price);

                Reservation saved = reservationRepository.save(reservation);

                return mapToDTO(saved);
        }

        private void validateBookingTimes(LocalDateTime startTime, LocalDateTime endTime) {

                if (startTime == null || endTime == null) {
                        throw new InvalidBookingException("Start time and end time are required.");
                }

                if (!endTime.isAfter(startTime)) {
                        throw new InvalidBookingException("End time must be strictly after start time.");
                }

                if (startTime.isBefore(LocalDateTime.now())) {
                        throw new InvalidBookingException("Cannot create reservations in the past.");
                }

                if (Duration.between(startTime, endTime).toMinutes() < 1) {
                        throw new InvalidBookingException("Reservation duration must be at least 1 minute.");
                }
        }

        private BigDecimal calculatePrice(Resource resource, LocalDateTime startTime, LocalDateTime endTime) {

                if (resource.getPricePerHour() == null ||
                                resource.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidBookingException("Resource price per hour must be greater than zero.");
                }

                long minutes = Duration.between(startTime, endTime).toMinutes();
                BigDecimal hours = BigDecimal.valueOf(minutes)
                                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

                BigDecimal price = resource.getPricePerHour()
                                .multiply(hours)
                                .setScale(2, RoundingMode.HALF_UP);

                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidBookingException("Reservation price must be greater than zero.");
                }

                return price;
        }
        // ==========================================================
        // GET RESERVATIONS (paginated, filtered)
        // ==========================================================

        public Page<ReservationDTO> getReservations(
                        ReservationStatus status,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        Pageable pageable,
                        String username) {

                User user = userRepository.findByUserName(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

                Specification<Reservation> spec = Specification
                                .where(ReservationSpecifications.hasStatus(status))
                                .and(ReservationSpecifications.hasMinPrice(minPrice))
                                .and(ReservationSpecifications.hasMaxPrice(maxPrice));

                // USER can see only own reservations; ADMIN sees all
                if (user.getRole() != Role.ROLE_ADMIN) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), user.getId()));
                }

                return reservationRepository.findAll(spec, pageable)
                                .map(this::mapToDTO);
        }

        // ==========================================================
        // GET RESERVATION BY ID
        // ==========================================================

        public ReservationDTO getReservationById(Long reservationId, String username) {

                Reservation reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reservation not found with ID: " + reservationId));

                User user = userRepository.findByUserName(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

                if (user.getRole() != Role.ROLE_ADMIN &&
                                !reservation.getUser().getId().equals(user.getId())) {
                        throw new AccessDeniedException("You are not authorized to view this reservation.");
                }

                return mapToDTO(reservation);
        }

        // ==========================================================
        // CANCEL RESERVATION
        // ==========================================================

        public ReservationDTO cancelReservation(Long id, String username) {

                Reservation reservation = reservationRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reservation not found with ID: " + id));

                User user = userRepository.findByUserName(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

                if (user.getRole() != Role.ROLE_ADMIN &&
                                !reservation.getUser().getId().equals(user.getId())) {
                        throw new AccessDeniedException("You are not authorized to cancel this reservation.");
                }

                reservation.setStatus(ReservationStatus.CANCELLED);

                return mapToDTO(reservationRepository.save(reservation));
        }

        // ==========================================================
        // CONFIRM RESERVATION (ADMIN only — enforced via @PreAuthorize in controller)
        // ==========================================================

        public ReservationDTO confirmReservation(Long id) {

                Reservation reservation = reservationRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reservation not found with ID: " + id));

                reservation.setStatus(ReservationStatus.CONFIRMED);

                return mapToDTO(reservationRepository.save(reservation));
        }

        // ==========================================================
        // DELETE RESERVATION (ADMIN only — enforced via @PreAuthorize in controller)
        // ==========================================================

        public void deleteReservation(Long id) {

                Reservation reservation = reservationRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reservation not found with ID: " + id));

                reservationRepository.delete(reservation);
        }

        // MAPPING
        private ReservationDTO mapToDTO(Reservation reservation) {

                return new ReservationDTO(
                                reservation.getId(),
                                reservation.getUser().getUserName(),
                                reservation.getResource().getName(),
                                reservation.getStartTime(),
                                reservation.getEndTime(),
                                reservation.getStatus(),
                                reservation.getPrice());
        }
}