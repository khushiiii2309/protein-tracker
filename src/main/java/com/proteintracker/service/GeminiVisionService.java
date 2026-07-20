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
 * Calls the Google Gemini API with a meal photo and asks for a structured
 * macro/ingredient estimate as JSON.
 */
@Component
public class GeminiVisionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionService.class);

    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final String SYSTEM_PROMPT = """
            You are a nutrition estimation assistant for a meal-tracking app. You will be \
            shown a photo of a meal. Identify the food items, estimate their portions, and \
            respond with ONLY a single JSON object (no markdown fences, no commentary) \
            matching exactly this shape:

            {
              "description": string (short dish name, e.g. "Grilled Chicken Bowl"),
              "mealType": string (one of "Breakfast", "Lunch", "Dinner", "Snack" - infer from the food),
              "confidenceScore": number (0.0 to 1.0 - overall detection confidence),
              "detectionConfidence": number (0.0 to 1.0 - confidence in detecting ingredients),
              "portionConfidence": number (0.0 to 1.0 - confidence in portion size estimates),
              "nutritionConfidence": number (0.0 to 1.0 - confidence in nutrition resolution lookup),
              "foodItems": [
                {
                  "foodName": string,
                  "estimatedQuantity": number (the likely cooked portion weight or quantity),
                  "minQuantity": number (minimum expected portion weight/quantity),
                  "maxQuantity": number (maximum expected portion weight/quantity),
                  "unit": string (e.g. "g", "piece", "cup")
                }
              ]
            }

            Do NOT guess or estimate calories, protein, carbs, or fat. Focus entirely on \
            identifying the ingredients and estimating their cooked weights or portion sizes \
            with a realistic range (minQuantity and maxQuantity). \
            If the image does not clearly show food, still return your best-guess JSON with \
            low confidence scores rather than refusing.
            """;

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiVisionService(
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-3.5-flash}") String model) {
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
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(
                                Map.of("inline_data", Map.of(
                                        "mime_type", mediaType,
                                        "data", base64Image
                                )),
                                Map.of("text", "Analyze this meal photo.")
                        )
                )),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 1024
                )
        );

        String url = String.format(API_URL_TEMPLATE, model, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Gemini API returned HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini API response had no candidates: " + truncate(response.body()));
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Gemini API response had no content parts: " + truncate(response.body()));
        }

        String text = parts.get(0).path("text").asText();
        String jsonText = extractJsonObject(text);

        AIMealAnalysisService.EstimatedMeal result =
                objectMapper.readValue(jsonText, AIMealAnalysisService.EstimatedMeal.class);

        log.info("Gemini vision analysis succeeded: {} ({} items, confidence {})",
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
