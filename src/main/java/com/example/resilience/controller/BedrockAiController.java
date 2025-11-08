package com.example.resilience.controller;

import com.example.resilience.service.BedrockAiService;
import com.example.resilience.service.ResilienceService;
import com.example.resilience.model.County;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = {"http://localhost:8501", "http://localhost:3000"})
public class BedrockAiController {

    private final BedrockAiService bedrockAiService;
    private final ResilienceService resilienceService;

    public BedrockAiController(BedrockAiService bedrockAiService, ResilienceService resilienceService) {
        this.bedrockAiService = bedrockAiService;
        this.resilienceService = resilienceService;
    }

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> query(@RequestBody Map<String, Object> payload) {
        Object p = payload.get("prompt");
        String userPrompt = p == null ? "" : p.toString().trim();
        if (userPrompt.isEmpty()) {
            return Map.of("reply", "Prompt is empty.");
        }

        // Get current weights from request or use defaults
        @SuppressWarnings("unchecked")
        Map<String, Double> weights = (Map<String, Double>) payload.getOrDefault("weights", Map.of(
            "income", 0.5,
            "unemployment", 0.25,
            "cost", 0.15,
            "disaster", 0.10
        ));

        // Build context with current data
        String context = buildDataContext(weights);
        
        // Combine context with user's question
        String fullPrompt = context + "\n\nUser question: " + userPrompt + 
                          "\n\nProvide a clear, concise answer based on the data above.";

        String reply = bedrockAiService.generate(fullPrompt);
        
        return Map.of(
                "reply", reply,
                "model", "bedrock:" + System.getProperty("bedrock.modelId", "titan-text-express"),
                "length", reply.length()
        );
    }

    private String buildDataContext(Map<String, Double> weights) {
        List<County> counties = resilienceService.getAll().values().stream()
            .collect(Collectors.toList());
        
        // Get top 10 and bottom 10 by score
        List<County> sortedByScore = counties.stream()
            .sorted((a, b) -> Double.compare(
                resilienceService.scoreCounty(b),
                resilienceService.scoreCounty(a)
            ))
            .collect(Collectors.toList());

        List<County> top10 = sortedByScore.stream().limit(10).collect(Collectors.toList());
        List<County> bottom10 = sortedByScore.stream()
            .skip(Math.max(0, sortedByScore.size() - 10))
            .collect(Collectors.toList());

        // Calculate statistics
        double avgScore = counties.stream()
            .mapToDouble(resilienceService::scoreCounty)
            .average()
            .orElse(0.0);
        
        double avgIncome = counties.stream()
            .mapToDouble(County::getMedianIncome)
            .average()
            .orElse(0.0);

        StringBuilder context = new StringBuilder();
        context.append("You are analyzing North Carolina county financial resilience data. Here is the current dataset:\n\n");
        
        context.append("SCORING FORMULA:\n");
        context.append(String.format("- Income weight: %.0f%%\n", weights.get("income") * 100));
        context.append(String.format("- Unemployment weight: %.0f%%\n", weights.get("unemployment") * 100));
        context.append(String.format("- Cost of Living weight: %.0f%%\n", weights.get("cost") * 100));
        context.append(String.format("- Disaster Risk weight: %.0f%%\n", weights.get("disaster") * 100));
        context.append("Population penalty: -5% for counties under 10,000; -8% for under 2,000\n\n");

        context.append("DATASET STATISTICS:\n");
        context.append(String.format("- Total counties: %d\n", counties.size()));
        context.append(String.format("- Average resilience score: %.3f (out of 1.0)\n", avgScore));
        context.append(String.format("- Average normalized median income: %.3f\n", avgIncome));
        context.append("\n");

        context.append("TOP 10 MOST RESILIENT COUNTIES:\n");
        for (int i = 0; i < top10.size(); i++) {
            County c = top10.get(i);
            double score = resilienceService.scoreCounty(c);
            context.append(String.format("%d. %s - Score: %.3f (Pop: %,d, Income: %.3f, Unemployment: %.3f, Cost: %.3f, Disaster: %.3f)\n",
                i + 1, c.getName(), score, c.getPopulation(), 
                c.getMedianIncome(), c.getUnemploymentRate(), 
                c.getCostOfLivingIndex(), c.getDisasterRisk()));
        }
        context.append("\n");

        context.append("BOTTOM 10 LEAST RESILIENT COUNTIES:\n");
        for (int i = 0; i < bottom10.size(); i++) {
            County c = bottom10.get(i);
            double score = resilienceService.scoreCounty(c);
            context.append(String.format("%d. %s - Score: %.3f (Pop: %,d, Income: %.3f, Unemployment: %.3f, Cost: %.3f, Disaster: %.3f)\n",
                i + 1, c.getName(), score, c.getPopulation(),
                c.getMedianIncome(), c.getUnemploymentRate(),
                c.getCostOfLivingIndex(), c.getDisasterRisk()));
        }

        return context.toString();
    }
}
