package com.example.resilience.controller;

import com.example.resilience.model.County;
import com.example.resilience.service.ResilienceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides a single enriched GeoJSON combining NCDOT county polygons with
 * resilience metrics (population, normalized income, unemployment, cost, disaster risk, score).
 */
@RestController
@RequestMapping("/api")
public class ResilienceGeoJsonController {

    private static final String NCDOT_GEOJSON_URL = "https://gis11.services.ncdot.gov/arcgis/rest/services/NCDOT_CountyBdy_Poly/MapServer/0/query?outFields=*&where=1%3D1&f=geojson";

    private final RestTemplate restTemplate;
    private final ResilienceService resilienceService;

    public ResilienceGeoJsonController(ResilienceService resilienceService) {
        this.resilienceService = resilienceService;
        // Configure RestTemplate with longer timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    @GetMapping(value = "/resilience-geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getEnrichedGeoJson() {
        Map<String, Object> base;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tmp = restTemplate.getForObject(NCDOT_GEOJSON_URL, Map.class);
            base = tmp;
        } catch (Exception ex) {
            return ResponseEntity.status(502).body(error("Failed to fetch NCDOT GeoJSON: " + ex.getMessage()));
        }
        if (base == null || !"FeatureCollection".equals(base.get("type"))) {
            return ResponseEntity.status(502).body(error("Invalid base GeoJSON from NCDOT"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> features = (List<Map<String, Object>>) base.get("features");
        if (features == null) {
            return ResponseEntity.status(502).body(error("Missing features array"));
        }

        // Build lookup of county metrics keyed by upper-case CountyName sans " County"
        Map<String, County> countyByName = resilienceService.getAll().values().stream()
                .collect(Collectors.toMap(
                        c -> c.getName().replace(" County", "").trim().toUpperCase(),
                        c -> c
                ));

        for (Map<String, Object> feature : features) {
            @SuppressWarnings("unchecked") Map<String, Object> props = (Map<String, Object>) feature.get("properties");
            if (props == null) continue;
            Object rawName = props.get("CountyName");
            if (rawName == null) continue;
            String key = rawName.toString().trim().toUpperCase();
            County c = countyByName.get(key);
            if (c != null) {
                double score = resilienceService.scoreCounty(c);
                props.put("population", c.getPopulation());
                props.put("medianIncome", c.getMedianIncome());
                props.put("unemploymentRate", c.getUnemploymentRate());
                props.put("costOfLivingIndex", c.getCostOfLivingIndex());
                props.put("disasterRisk", c.getDisasterRisk());
                props.put("resilienceScore", score);
            } else {
                props.put("resilienceScore", 0.0);
            }
        }

        Map<String, Object> enriched = new HashMap<>();
        enriched.put("type", "FeatureCollection");
        enriched.put("features", features);
        enriched.put("source", "NCDOT + Census ACS (enriched)");
        return ResponseEntity.ok(enriched);
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> e = new HashMap<>();
        e.put("error", msg);
        return e;
    }
}
