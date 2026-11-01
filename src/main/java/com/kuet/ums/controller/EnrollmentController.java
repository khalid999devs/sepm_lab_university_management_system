package com.kuet.ums.controller;

import com.kuet.ums.dto.*;
import com.kuet.ums.service.EnrollmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollments;

    public EnrollmentController(EnrollmentService enrollments) {
        this.enrollments = enrollments;
    }

    @GetMapping
    public List<EnrollmentResponse> list() {
        return enrollments.list();
    }

    @GetMapping("/{id}")
    public EnrollmentResponse get(@PathVariable Long id) {
        return enrollments.get(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnrollmentResponse> create(
        @Valid @RequestBody EnrollmentRequest request
    ) {
        EnrollmentResponse enrollment = enrollments.create(request);

        return ResponseEntity.created(URI.create("/api/enrollments/" + enrollment.id())).body(
            enrollment
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enrollments.delete(id);

        return ResponseEntity.noContent().build();
    }
}
