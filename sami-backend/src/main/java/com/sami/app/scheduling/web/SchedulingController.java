package com.sami.app.scheduling.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.scheduling.api.BookingCommand;
import com.sami.app.scheduling.api.BookingResult;
import com.sami.app.scheduling.api.SlotRequest;
import com.sami.app.scheduling.api.TimeSlot;
import com.sami.app.scheduling.domain.Schedule;
import com.sami.app.scheduling.repository.AppointmentTypeRepository;
import com.sami.app.scheduling.repository.ResourceCategoryRepository;
import com.sami.app.scheduling.repository.ResourceStatusRepository;
import com.sami.app.scheduling.repository.SchedulableResourceRepository;
import com.sami.app.scheduling.repository.ScheduleCategoryRepository;
import com.sami.app.scheduling.repository.ScheduleStatusRepository;
import com.sami.app.scheduling.service.AvailabilityService;
import com.sami.app.scheduling.service.SchedulingService;
import com.sami.app.scheduling.service.WaitingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST surface for appointments, resources and the waiting list.
 *
 * <p>Permissions use the {@code appointments:} namespace, not
 * {@code scheduler:} — V19 already owns the latter for the background job
 * runner, and conflating them would grant job-control rights to anyone allowed
 * to read a booking.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointments, resource reservations and availability")
public class SchedulingController {

    private final SchedulingService scheduling;
    private final AvailabilityService availability;
    private final WaitingListService waitingList;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final ScheduleStatusRepository statusRepository;
    private final ScheduleCategoryRepository categoryRepository;
    private final SchedulableResourceRepository resourceRepository;
    private final ResourceCategoryRepository resourceCategoryRepository;
    private final ResourceStatusRepository resourceStatusRepository;

    // -----------------------------------------------------------------
    // Booking
    // -----------------------------------------------------------------

    /**
     * Always 200, even for a conflict: "that slot is taken, here are three
     * others" is a business answer, and the caller branches on
     * {@code data.booked} rather than on the HTTP status.
     */
    @PostMapping
    @PreAuthorize("@authz.has('appointments:create')")
    @Operation(summary = "Create an appointment")
    public ApiResponse<BookingResult> create(@Valid @RequestBody BookingCommand command) {
        return ApiResponse.ok(scheduling.requestBooking(command));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("@authz.has('appointments:edit')")
    @Operation(summary = "Move an appointment to a new time")
    public ApiResponse<BookingResult> reschedule(@PathVariable Long id,
                                                 @RequestParam Instant startsAt,
                                                 @RequestParam(required = false) Integer durationMinutes) {
        return ApiResponse.ok(scheduling.reschedule(id, startsAt, durationMinutes));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authz.has('appointments:cancel')")
    @Operation(summary = "Cancel an appointment and release its resources")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long id,
                                                   @RequestParam(required = false) String reason) {
        Schedule schedule = scheduling.cancel(id, reason);
        return ApiResponse.ok(Map.of("id", schedule.getId(),
                "status", schedule.getStatus().getCode()));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("@authz.has('appointments:check-in')")
    @Operation(summary = "Register customer arrival")
    public ApiResponse<Map<String, Object>> checkIn(@PathVariable Long id) {
        Schedule schedule = scheduling.checkIn(id);
        return ApiResponse.ok(Map.of(
                "id", schedule.getId(),
                "status", schedule.getStatus().getCode(),
                "arrivedLate", schedule.isArrivedLate(),
                "lateByMinutes", schedule.getLateByMinutes() == null ? 0 : schedule.getLateByMinutes()));
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("@authz.has('appointments:check-in')")
    @Operation(summary = "Complete an appointment")
    public ApiResponse<Map<String, Object>> checkOut(@PathVariable Long id,
                                                     @RequestParam(required = false) String notes) {
        Schedule schedule = scheduling.checkOut(id, notes);
        return ApiResponse.ok(Map.of("id", schedule.getId(),
                "status", schedule.getStatus().getCode()));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("@authz.has('appointments:edit')")
    @Operation(summary = "Mark an appointment as a no-show")
    public ApiResponse<Map<String, Object>> noShow(@PathVariable Long id) {
        Schedule schedule = scheduling.markNoShow(id);
        return ApiResponse.ok(Map.of("id", schedule.getId(),
                "status", schedule.getStatus().getCode()));
    }

    // -----------------------------------------------------------------
    // Availability
    // -----------------------------------------------------------------

    @PostMapping("/availability")
    @PreAuthorize("@authz.has('appointments:view')")
    @Operation(summary = "Find free slots matching a request")
    public ApiResponse<List<TimeSlot>> availability(@Valid @RequestBody SlotRequest request,
                                                    @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(availability.findSlots(request, Math.min(limit, 100)));
    }

    // -----------------------------------------------------------------
    // Waiting list
    // -----------------------------------------------------------------

    @PostMapping("/waiting-list/{entryId}/promote")
    @PreAuthorize("@authz.has('appointments:manage-waiting-list')")
    @Operation(summary = "Record that a waiting list entry became an appointment")
    public ApiResponse<Map<String, Object>> promote(@PathVariable Long entryId,
                                                    @RequestParam Long scheduleId) {
        waitingList.markPromoted(entryId, scheduleId, true);
        return ApiResponse.ok(Map.of("entryId", entryId, "scheduleId", scheduleId));
    }

    @PostMapping("/waiting-list/{entryId}/cancel")
    @PreAuthorize("@authz.has('appointments:manage-waiting-list')")
    @Operation(summary = "Cancel a waiting list entry")
    public ApiResponse<Map<String, Object>> cancelWaiting(@PathVariable Long entryId,
                                                          @RequestParam(required = false) String reason) {
        waitingList.cancel(entryId, reason);
        return ApiResponse.ok(Map.of("entryId", entryId));
    }

    // -----------------------------------------------------------------
    // Catalogue — everything configurable, so the UI builds itself
    // -----------------------------------------------------------------

    @GetMapping("/catalog")
    @PreAuthorize("@authz.has('appointments:view')")
    @Operation(summary = "Configurable types, statuses, categories and resources")
    public ApiResponse<Map<String, Object>> catalog() {
        return ApiResponse.ok(Map.of(
                "appointmentTypes", appointmentTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                        .stream().map(t -> Map.of(
                                "id", t.getId(),
                                "code", t.getCode(), "name", t.getName(),
                                "defaultDurationMinutes", t.getDefaultDurationMinutes(),
                                "minDurationMinutes", t.getMinDurationMinutes(),
                                "maxDurationMinutes", t.getMaxDurationMinutes(),
                                "requiresCustomer", t.isRequiresCustomer(),
                                "requiresSupplier", t.isRequiresSupplier(),
                                "allowsSelfService", t.isAllowsSelfService(),
                                "color", t.getColor() == null ? "" : t.getColor())).toList(),
                "statuses", statusRepository.findAllByOrderByDisplayOrderAsc()
                        .stream().map(s -> Map.of(
                                "code", s.getCode(), "name", s.getName(),
                                "isTerminal", s.isTerminal(),
                                "allowsCancel", s.isAllowsCancel(),
                                "allowsCheckIn", s.isAllowsCheckIn(),
                                "color", s.getColor() == null ? "" : s.getColor())).toList(),
                "categories", categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                        .stream().map(c -> Map.of("code", c.getCode(), "name", c.getName())).toList(),
                "resourceCategories", resourceCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                        .stream().map(c -> Map.of("code", c.getCode(), "name", c.getName(),
                                "requiresSkillMatch", c.isRequiresSkillMatch())).toList(),
                "resourceStatuses", resourceStatusRepository.findAllByOrderByDisplayOrderAsc()
                        .stream().map(s -> Map.of("code", s.getCode(), "name", s.getName(),
                                "allowsBooking", s.isAllowsBooking())).toList()));
    }

    @GetMapping("/resources")
    @PreAuthorize("@authz.has('appointments:view')")
    @Operation(summary = "List schedulable resources")
    public ApiResponse<List<Map<String, Object>>> resources() {
        return ApiResponse.ok(resourceRepository.findByIsActiveTrueOrderByPriorityDescDisplayOrderAsc()
                .stream().map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "code", r.getResourceCode(),
                        "name", r.getName(),
                        "category", r.getCategory().getCode(),
                        "status", r.getStatus().getCode(),
                        "acceptsBookings", r.acceptsBookings(),
                        "capacity", r.getCapacity())).toList());
    }
}
