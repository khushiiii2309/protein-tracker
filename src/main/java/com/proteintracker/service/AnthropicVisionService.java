package com.proteintracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Messages API with a meal photo and asks Claude to
 * return a structured macro/ingredient estimate as JSON.
 */
@Component
public class AnthropicVisionService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicVisionService.class);

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String SYSTEM_PROMPT = """
            You are a nutrition estimation assistant for a meal-tracking app. You will be \
            shown a photo of a meal. Identify the food items, estimate their portions, and \
            respond with ONLY a single JSON object (no markdown fences, no commentary) \
            matching exactly this shape:

            {
              "description": string (short dish name, e.g. "Grilled Chicken Bowl"),
              "mealType": string (one of "Breakfast", "Lunch", "Dinner", "Snack" - infer from the food),
              "totalProteinG": number,
              "totalCaloriesKcal": number,
              "totalCarbsG": number,
              "totalFatG": number,
              "confidenceScore": number (0.0 to 1.0 - how confident you are in this estimate),
              "foodItems": [
                {
                  "foodName": string,
                  "estimatedQuantity": number,
                  "unit": string (e.g. "g", "piece", "cup"),
                  "proteinG": number,
                  "caloriesKcal": number,
                  "carbsG": number,
                  "fatG": number
                }
              ]
            }

            The four total* fields must equal the sum of the corresponding foodItems values. \
            If the image does not clearly show food, still return your best-guess JSON with a \
            low confidenceScore rather than refusing.
            """;

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicVisionService(
            @Value("${app.ai.anthropic.api-key:}") String apiKey,
            @Value("${app.ai.anthropic.model:claude-sonnet-5}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public AIMealAnalysisService.EstimatedMeal analyze(byte[] imageBytes, String mediaType) throws Exception {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 1024,
                "system", SYSTEM_PROMPT,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "image",
                                        "source", Map.of(
                                                "type", "base64",
                                                "media_type", mediaType,
                                                "data", base64Image
                                        )
                                ),
                                Map.of("type", "text", "text", "Analyze this meal photo.")
                        )
                ))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Anthropic API returned HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentArray = root.path("content");
        if (!contentArray.isArray() || contentArray.isEmpty()) {
            throw new IllegalStateException("Anthropic API response had no content: " + truncate(response.body()));
        }

        String text = contentArray.get(0).path("text").asText();
        String jsonText = extractJsonObject(text);

        AIMealAnalysisService.EstimatedMeal result =
                objectMapper.readValue(jsonText, AIMealAnalysisService.EstimatedMeal.class);

        log.info("Anthropic vision analysis succeeded: {} ({} items, confidence {})",
                result.description, result.foodItems == null ? 0 : result.foodItems.size(), result.confidenceScore);
        return result;
    }

    private String extractJsonObject(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalStateException("Model response did not contain a JSON object: " + truncate(text));
        }
        return trimmed.substring(start, end + 1);
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
