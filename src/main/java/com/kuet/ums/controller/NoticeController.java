package com.kuet.ums.controller;

import com.kuet.ums.dto.*;
import com.kuet.ums.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService notices;
    public NoticeController(NoticeService notices) { this.notices = notices; }
    @GetMapping public List<NoticeResponse> list() { return notices.list(); }
    @GetMapping("/{id}") public NoticeResponse get(@PathVariable Long id) { return notices.get(id); }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NoticeResponse> create(@Valid @RequestBody NoticeRequest request) {
        NoticeResponse notice = notices.create(request);
        return ResponseEntity.created(URI.create("/api/notices/" + notice.id())).body(notice);
    }
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public NoticeResponse update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) { return notices.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        notices.delete(id);
        return ResponseEntity.noContent().build();
    }
}
