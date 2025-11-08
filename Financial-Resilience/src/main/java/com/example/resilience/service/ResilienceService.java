package com.example.resilience.service;

import com.example.resilience.model.County;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResilienceService {

    // In-memory store (populated by DataLoader)
    private final Map<Long, County> counties = new HashMap<>();

    public void saveCounty(County c) {
        counties.put(c.getId(), c);
    }

    public Map<Long, County> getAll() {
        return counties;
    }

    public County findById(Long id) {
        return counties.get(id);
    }

    /**
     * Mock "AI" scoring function. This uses a weighted heuristic to compute resilience.
     * Higher score = more resilient (0..1)
     */
    public double scoreCounty(County c) {
        if (c == null) return 0.0;

        // Normalize inputs to 0..1 expected range. Assume medianIncome already normalized roughly 0..1.
        double income = clamp(c.getMedianIncome(), 0.0, 1.0);
        double unemp = clamp(c.getUnemploymentRate(), 0.0, 1.0);
        double cost = clamp(c.getCostOfLivingIndex(), 0.0, 1.0);
        double disaster = clamp(c.getDisasterRisk(), 0.0, 1.0);

        // We want high income, low unemployment, low cost, low disaster risk
        double score = 0.5 * income + 0.25 * (1 - unemp) + 0.15 * (1 - cost) + 0.10 * (1 - disaster);

        // Slight adjustment: smaller populations may be less resilient in some contexts; penalize very small counties lightly
        double popPenalty = 0.0;
        if (c.getPopulation() < 10000) popPenalty = 0.05;
        if (c.getPopulation() < 2000) popPenalty = 0.08;

        score = score - popPenalty;
        return clamp(score, 0.0, 1.0);
    }

    public String explain(County c) {
        double s = scoreCounty(c);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Estimated resilience score: %.3f. ", s));
        sb.append("Factors: ");
        sb.append(String.format("Income=%.2f, Unemployment=%.2f, Cost=%.2f, DisasterRisk=%.2f.",
                c.getMedianIncome(), c.getUnemploymentRate(), c.getCostOfLivingIndex(), c.getDisasterRisk()));
        return sb.toString();
    }

    private double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}
