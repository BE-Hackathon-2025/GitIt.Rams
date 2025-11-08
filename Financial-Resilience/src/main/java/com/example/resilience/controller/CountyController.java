package com.example.resilience.controller;

import com.example.resilience.model.County;
import com.example.resilience.service.ResilienceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CountyController {

    private final ResilienceService service;

    public CountyController(ResilienceService service) {
        this.service = service;
    }

    @GetMapping("/counties")
    public Collection<County> list() {
        return service.getAll().values();
    }

    @GetMapping("/counties/{id}")
    public ResponseEntity<County> get(@PathVariable Long id) {
        County c = service.findById(id);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(c);
    }

    @PostMapping("/counties")
    public ResponseEntity<County> createOrUpdate(@RequestBody County county) {
        if (county == null || county.getId() == null) {
            // For simplicity require an id; clients can generate a numeric id or we can extend to assign one.
            return ResponseEntity.badRequest().build();
        }
        service.saveCounty(county);
        return ResponseEntity.ok(county);
    }

    @GetMapping("/score/{id}")
    public ResponseEntity<Map<String, Object>> score(@PathVariable Long id) {
        County c = service.findById(id);
        if (c == null) return ResponseEntity.notFound().build();
        double s = service.scoreCounty(c);
        String explanation = service.explain(c);
        return ResponseEntity.ok(Map.of("score", s, "explanation", explanation, "county", c));
    }

    @PostMapping("/score")
    public ResponseEntity<Map<String,Object>> scorePayload(@RequestBody County c) {
        double s = service.scoreCounty(c);
        String explanation = service.explain(c);
        return ResponseEntity.ok(Map.of("score", s, "explanation", explanation, "county", c));
    }
}
