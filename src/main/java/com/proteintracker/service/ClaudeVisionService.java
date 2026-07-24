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
 * Calls the Anthropic Claude API with a meal photo as a fallback provider when Gemini is unavailable.
 */
@Component
public class ClaudeVisionService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeVisionService.class);

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

    public ClaudeVisionService(
            @Value("${app.ai.anthropic.api-key:}") String apiKey,
            @Value("${app.ai.anthropic.model:claude-3-5-sonnet-20241022}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @jakarta.annotation.PostConstruct
    public void initLog() {
        if (isConfigured()) {
            log.info("ClaudeVisionService initialized with API key (model: {})", model);
        } else {
            log.warn("ClaudeVisionService initialized WITHOUT an API key. Set ANTHROPIC_API_KEY env var to enable.");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    public AIMealAnalysisService.EstimatedMeal analyze(byte[] imageBytes, String mediaType) throws Exception {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> imageSource = Map.of(
                "type", "base64",
                "media_type", mediaType,
                "data", base64Image
        );

        Map<String, Object> imageContent = Map.of(
                "type", "image",
                "source", imageSource
        );

        Map<String, Object> textContent = Map.of(
                "type", "text",
                "text", "Analyze this meal photo."
        );

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(imageContent, textContent)
        );

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 1024,
                "system", SYSTEM_PROMPT,
                "messages", List.of(userMessage)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = sendWithRetry(request);

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Claude API returned HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new IllegalStateException("Claude API response had no content array: " + truncate(response.body()));
        }

        String text = content.get(0).path("text").asText();
        String jsonText = extractJsonObject(text);

        AIMealAnalysisService.EstimatedMeal result =
                objectMapper.readValue(jsonText, AIMealAnalysisService.EstimatedMeal.class);

        log.info("Claude vision analysis succeeded: {} ({} items, confidence {})",
                result.description, result.foodItems == null ? 0 : result.foodItems.size(), result.confidenceScore);
        return result;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
        int maxAttempts = 3;
        HttpResponse<String> response = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if ((status != 502 && status != 503 && status != 429) || attempt == maxAttempts) {
                return response;
            }
            log.warn("Claude API returned HTTP {} (attempt {}/{}), retrying after backoff", status, attempt, maxAttempts);
            Thread.sleep(1000L * attempt);
        }
        return response;
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
