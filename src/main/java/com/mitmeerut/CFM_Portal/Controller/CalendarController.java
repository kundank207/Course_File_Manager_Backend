package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.CalendarEvent;
import com.mitmeerut.CFM_Portal.Repository.CalendarEventRepository;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class CalendarController {

    private final CalendarEventRepository calendarEventRepository;

    public CalendarController(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "month", required = false) String month) {
        try {
            Long deptId = userDetails.getDepartmentId();
            if (deptId == null) {
                // Try harder to find the department if not in principal
                if (userDetails.getTeacher() != null && userDetails.getTeacher().getDepartment() != null) {
                    deptId = userDetails.getTeacher().getDepartment().getId();
                }
            }

            if (deptId == null) {
                // Return empty instead of error to keep frontend happy
                java.util.Map<String, Object> empty = new java.util.HashMap<>();
                empty.put("events", new java.util.ArrayList<>());
                empty.put("upcoming", new java.util.ArrayList<>());
                return ResponseEntity.ok(empty);
            }

            List<CalendarEvent> events;
            if (month != null && !month.isEmpty()) {
                try {
                    java.time.YearMonth ym = java.time.YearMonth.parse(month);
                    LocalDate start = ym.atDay(1);
                    LocalDate end = ym.atEndOfMonth();
                    events = calendarEventRepository.findByDepartmentAndMonth(deptId, start, end);
                } catch (Exception e) {
                    events = calendarEventRepository.findByDepartmentId(deptId);
                }
            } else {
                events = calendarEventRepository.findByDepartmentId(deptId);
            }

            List<CalendarEvent> upcoming = calendarEventRepository.findUpcomingEvents(deptId, LocalDate.now());

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("events", events != null ? events : new java.util.ArrayList<>());
            response.put("upcoming", upcoming != null ? upcoming : new java.util.ArrayList<>());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/events")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HOD')")
    public ResponseEntity<?> createEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CalendarEvent event) {
        event.setDepartmentId(userDetails.getDepartmentId());
        event.setCreatedBy(userDetails.getUserId());
        return ResponseEntity.ok(calendarEventRepository.save(event));
    }

    @DeleteMapping("/events/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HOD')")
    public ResponseEntity<?> deleteEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        return calendarEventRepository.findById(id).map(event -> {
            if (!event.getDepartmentId().equals(userDetails.getDepartmentId())) {
                return ResponseEntity.status(403).build();
            }
            calendarEventRepository.delete(event);
            return ResponseEntity.ok(java.util.Map.of("success", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<CalendarEvent>> getUpcomingEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long deptId = userDetails.getDepartmentId();
        if (deptId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<CalendarEvent> events = calendarEventRepository.findUpcomingEvents(deptId, LocalDate.now());
        return ResponseEntity.ok(events);
    }
}
