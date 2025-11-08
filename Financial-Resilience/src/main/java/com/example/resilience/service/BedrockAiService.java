package com.example.resilience.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class BedrockAiService {

    private final BedrockRuntimeClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String modelId;
    private final int maxTokens;
    private final double temperature;

    public BedrockAiService(
            @Value("${bedrock.region:us-east-1}") String region,
            @Value("${bedrock.modelId:anthropic.claude-3-haiku-20240307-v1:0}") String modelId,
            @Value("${bedrock.maxTokens:300}") int maxTokens,
            @Value("${bedrock.temperature:0.2}") double temperature
    ) {
        this.client = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.modelId = modelId;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    public String generate(String prompt) {
        try {
            String body;
            // Different request formats for different model families
            if (modelId.startsWith("anthropic.claude")) {
                // Claude 3 messages format
                body = mapper.writeValueAsString(Map.of(
                        "anthropic_version", "bedrock-2023-05-31",
                        "max_tokens", maxTokens,
                        "temperature", temperature,
                        "messages", List.of(Map.of(
                                "role", "user",
                                "content", List.of(Map.of("type", "text", "text", prompt))
                        ))
                ));
            } else if (modelId.startsWith("amazon.titan")) {
                // Titan text format
                body = mapper.writeValueAsString(Map.of(
                        "inputText", prompt,
                        "textGenerationConfig", Map.of(
                                "maxTokenCount", maxTokens,
                                "temperature", temperature,
                                "topP", 0.9
                        )
                ));
            } else {
                // Generic fallback
                body = mapper.writeValueAsString(Map.of(
                        "prompt", prompt,
                        "max_tokens", maxTokens,
                        "temperature", temperature
                ));
            }

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(body))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            String json = response.body().asString(StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(json);
            
            // Parse response based on model type
            if (modelId.startsWith("anthropic.claude")) {
                // Claude 3: content[0].text
                JsonNode content = root.path("content");
                if (content.isArray() && content.size() > 0) {
                    JsonNode first = content.get(0);
                    if (first.has("text")) {
                        return first.get("text").asText();
                    }
                }
            } else if (modelId.startsWith("amazon.titan")) {
                // Titan: results[0].outputText
                JsonNode results = root.path("results");
                if (results.isArray() && results.size() > 0) {
                    JsonNode first = results.get(0);
                    if (first.has("outputText")) {
                        return first.get("outputText").asText();
                    }
                }
            }
            
            // Fallbacks for other providers
            if (root.has("outputText")) {
                return root.get("outputText").asText();
            }
            if (root.has("generation")) {
                return root.get("generation").asText();
            }
            if (root.has("completions") && root.get("completions").isArray()) {
                return root.get("completions").get(0).asText();
            }
            
            return json;
        } catch (Exception e) {
            return "Error calling Bedrock: " + e.getMessage();
        }
    }
}
