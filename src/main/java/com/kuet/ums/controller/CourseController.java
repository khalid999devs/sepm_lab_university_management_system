package com.kuet.ums.controller;

import com.kuet.ums.dto.*;
import com.kuet.ums.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courses;
    public CourseController(CourseService courses) { this.courses = courses; }
    @GetMapping public List<CourseResponse> list() { return courses.list(); }
    @GetMapping("/{id}") public CourseResponse get(@PathVariable Long id) { return courses.get(id); }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request) {
        CourseResponse course = courses.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + course.id())).body(course);
    }
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) { return courses.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        courses.delete(id);
        return ResponseEntity.noContent().build();
    }
}
