package com.example.resilience.config;

import com.example.resilience.model.County;
import com.example.resilience.service.ResilienceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataLoader {

    private static final String CENSUS_API_KEY = "c3b895c40dc66379b8b94a7716a0832ebea452d7";
    private static final String CENSUS_URL = "https://api.census.gov/data/2022/acs/acs5?get=NAME,B19013_001E,B01003_001E&for=county:*&in=state:37&key=" + CENSUS_API_KEY;

    @Bean
    CommandLineRunner init(ResilienceService service) {
        return args -> {
            System.out.println("DataLoader: Fetching NC county data from Census API...");
            try {
                RestTemplate restTemplate = new RestTemplate();
                String response = restTemplate.getForObject(CENSUS_URL, String.class);
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode data = mapper.readTree(response);
                
                if (!data.isArray() || data.size() < 2) {
                    System.err.println("DataLoader: Invalid Census API response");
                    return;
                }
                
                List<County> counties = new ArrayList<>();
                
                // Skip header row (index 0), process data rows
                for (int i = 1; i < data.size(); i++) {
                    JsonNode row = data.get(i);
                    if (row.isArray() && row.size() >= 2) {
                        String name = row.get(0).asText();
                        String countyName = name.replace(" County, North Carolina", "");
                        
                        double medianIncome = 0.0;
                        if (!row.get(1).isNull()) {
                            try {
                                medianIncome = row.get(1).asDouble();
                            } catch (Exception e) {
                                // Use 0 if parse fails
                            }
                        }
                        
                        int population = 50000;
                        if (row.size() > 2 && !row.get(2).isNull()) {
                            try {
                                population = row.get(2).asInt();
                            } catch (Exception e) {
                                // Use 50000 as default if parse fails
                            }
                        }
                        
                        // Create county with normalized median income
                        // We'll normalize after loading all data
                        County county = new County();
                        county.setId((long) i); // Use row index as ID
                        county.setName(countyName);
                        county.setPopulation(population);
                        county.setMedianIncome(medianIncome);
                        // Set defaults for other indicators (these would come from other data sources)
                        county.setUnemploymentRate(0.05); // 5% default
                        county.setCostOfLivingIndex(0.5); // Medium default
                        county.setDisasterRisk(0.1); // Low default
                        
                        counties.add(county);
                    }
                }
                
                // Normalize median income across all counties (0-1 scale)
                if (!counties.isEmpty()) {
                    double minIncome = counties.stream().mapToDouble(County::getMedianIncome).min().orElse(0);
                    double maxIncome = counties.stream().mapToDouble(County::getMedianIncome).max().orElse(1);
                    double range = maxIncome - minIncome;
                    
                    if (range > 0) {
                        counties.forEach(c -> {
                            double normalized = (c.getMedianIncome() - minIncome) / range;
                            c.setMedianIncome(normalized);
                        });
                    }
                }
                
                // Add all counties to the service
                counties.forEach(service::saveCounty);
                
                System.out.println("DataLoader: Successfully loaded " + counties.size() + " NC counties from Census API");
                
            } catch (Exception e) {
                System.err.println("DataLoader: Failed to load Census data: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
