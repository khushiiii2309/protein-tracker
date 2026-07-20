package com.proteintracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class NutritionLookupService {

    private static final Logger log = LoggerFactory.getLogger(NutritionLookupService.class);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Local dictionary of common ingredients (macros per 100g)
    private static final Map<String, FoodMacros> LOCAL_DATABASE = new HashMap<>();

    static {
        // Core macro values per 100g/100ml
        LOCAL_DATABASE.put("chicken breast", new FoodMacros(165, 31.0, 0.0, 3.6));
        LOCAL_DATABASE.put("chicken thigh", new FoodMacros(209, 26.0, 0.0, 11.0));
        LOCAL_DATABASE.put("chicken", new FoodMacros(190, 27.0, 0.0, 8.0));
        LOCAL_DATABASE.put("brown rice", new FoodMacros(111, 2.6, 23.0, 0.9));
        LOCAL_DATABASE.put("white rice", new FoodMacros(130, 2.7, 28.0, 0.3));
        LOCAL_DATABASE.put("rice", new FoodMacros(120, 2.5, 26.0, 0.5));
        LOCAL_DATABASE.put("egg", new FoodMacros(155, 13.0, 1.1, 11.0));
        LOCAL_DATABASE.put("toast", new FoodMacros(265, 9.0, 49.0, 3.2));
        LOCAL_DATABASE.put("sourdough", new FoodMacros(265, 9.0, 49.0, 3.2));
        LOCAL_DATABASE.put("bread", new FoodMacros(265, 9.0, 49.0, 3.2));
        LOCAL_DATABASE.put("avocado", new FoodMacros(160, 2.0, 9.0, 15.0));
        LOCAL_DATABASE.put("banana", new FoodMacros(89, 1.1, 23.0, 0.3));
        LOCAL_DATABASE.put("peanut butter", new FoodMacros(588, 25.0, 20.0, 50.0));
        LOCAL_DATABASE.put("protein powder", new FoodMacros(380, 80.0, 6.0, 6.0));
        LOCAL_DATABASE.put("whey", new FoodMacros(380, 80.0, 6.0, 6.0));
        LOCAL_DATABASE.put("almond milk", new FoodMacros(17, 0.4, 0.6, 1.5));
        LOCAL_DATABASE.put("milk", new FoodMacros(61, 3.2, 4.8, 3.3));
        LOCAL_DATABASE.put("salmon", new FoodMacros(208, 20.0, 0.0, 13.0));
        LOCAL_DATABASE.put("fish", new FoodMacros(208, 20.0, 0.0, 13.0));
        LOCAL_DATABASE.put("steak", new FoodMacros(250, 26.0, 0.0, 15.0));
        LOCAL_DATABASE.put("beef", new FoodMacros(250, 26.0, 0.0, 15.0));
        LOCAL_DATABASE.put("ribeye", new FoodMacros(250, 26.0, 0.0, 15.0));
        LOCAL_DATABASE.put("meat", new FoodMacros(220, 24.0, 0.0, 12.0));
        LOCAL_DATABASE.put("broccoli", new FoodMacros(34, 2.8, 7.0, 0.4));
        LOCAL_DATABASE.put("sweet potato", new FoodMacros(86, 1.6, 20.0, 0.1));
        LOCAL_DATABASE.put("potato", new FoodMacros(77, 2.0, 17.0, 0.1));
        LOCAL_DATABASE.put("veggies", new FoodMacros(40, 2.0, 8.0, 0.2));
        LOCAL_DATABASE.put("vegetables", new FoodMacros(40, 2.0, 8.0, 0.2));
        LOCAL_DATABASE.put("salad", new FoodMacros(40, 2.0, 8.0, 0.2));
        LOCAL_DATABASE.put("butter", new FoodMacros(717, 0.9, 0.1, 81.0));
        LOCAL_DATABASE.put("oil", new FoodMacros(884, 0.0, 0.0, 100.0));
        LOCAL_DATABASE.put("olive oil", new FoodMacros(884, 0.0, 0.0, 100.0));
        LOCAL_DATABASE.put("yogurt", new FoodMacros(59, 10.0, 3.6, 0.4));
        LOCAL_DATABASE.put("greek yogurt", new FoodMacros(59, 10.0, 3.6, 0.4));
        LOCAL_DATABASE.put("granola", new FoodMacros(471, 10.0, 64.0, 20.0));
        LOCAL_DATABASE.put("peas", new FoodMacros(81, 5.4, 14.0, 0.4));
        LOCAL_DATABASE.put("asparagus", new FoodMacros(20, 2.2, 3.9, 0.1));
        LOCAL_DATABASE.put("cheese", new FoodMacros(400, 25.0, 1.3, 33.0));
        LOCAL_DATABASE.put("turkey", new FoodMacros(135, 30.0, 0.0, 1.0));
    }

    public static class FoodMacros {
        public double calories;
        public double protein;
        public double carbs;
        public double fat;

        public FoodMacros() {
            this.calories = 0.0;
            this.protein = 0.0;
            this.carbs = 0.0;
            this.fat = 0.0;
        }

        public FoodMacros(double calories, double protein, double carbs, double fat) {
            this.calories = calories;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
        }

        @Override
        public String toString() {
            return String.format("Calories: %.1f kcal, Protein: %.1fg, Carbs: %.1fg, Fat: %.1fg",
                    calories, protein, carbs, fat);
        }
    }

    public NutritionLookupService(
            @Value("${app.ai.usda.api-key:DEMO_KEY}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Estimates macros for a food item by name and portion size.
     */
    public FoodMacros estimateMacros(String foodName, double quantity, String unit) {
        double weightGrams = convertUnitToGrams(foodName, quantity, unit);
        FoodMacros macrosPer100g = findMacrosPer100g(foodName);

        // Compute total macros for the estimated weight
        double factor = weightGrams / 100.0;
        double calories = macrosPer100g.calories * factor;
        double protein = macrosPer100g.protein * factor;
        double carbs = macrosPer100g.carbs * factor;
        double fat = macrosPer100g.fat * factor;

        return new FoodMacros(calories, protein, carbs, fat);
    }

    /**
     * Converts common visual portion units (pieces, cups, slices) into grams.
     */
    private double convertUnitToGrams(String foodName, double quantity, String unit) {
        if (unit == null || unit.isBlank()) {
            return quantity;
        }
        
        String unitLower = unit.toLowerCase(Locale.ROOT).trim();
        String nameLower = foodName.toLowerCase(Locale.ROOT).trim();

        if (unitLower.contains("piece") || unitLower.contains("qty") || unitLower.contains("count")) {
            if (nameLower.contains("egg")) {
                return quantity * 50.0;
            } else if (nameLower.contains("avocado")) {
                return quantity * 150.0;
            } else if (nameLower.contains("banana")) {
                return quantity * 120.0;
            } else if (nameLower.contains("chicken thigh")) {
                return quantity * 110.0;
            } else if (nameLower.contains("chicken breast")) {
                return quantity * 150.0;
            } else if (nameLower.contains("fillet") || nameLower.contains("steak")) {
                return quantity * 180.0;
            }
            return quantity * 100.0; // Default estimation for an arbitrary "piece"
        }

        if (unitLower.contains("slice")) {
            return quantity * 40.0;
        }
        if (unitLower.contains("egg")) {
            return quantity * 50.0;
        }
        if (unitLower.contains("scoop")) {
            return quantity * 30.0;
        }
        if (unitLower.contains("tablespoon") || unitLower.contains("tbsp")) {
            return quantity * 15.0;
        }
        if (unitLower.contains("teaspoon") || unitLower.contains("tsp")) {
            return quantity * 5.0;
        }
        if (unitLower.contains("cup")) {
            return quantity * 200.0;
        }
        if (unitLower.contains("serving")) {
            return quantity * 100.0;
        }
        if (unitLower.contains("g") || unitLower.contains("ml") || unitLower.contains("gram")) {
            return quantity;
        }

        // Default fallback if unit is unknown
        return quantity;
    }

    /**
     * Tries to find macros per 100g first from local dictionary, then falls back to USDA API.
     */
    private FoodMacros findMacrosPer100g(String foodName) {
        String query = foodName.toLowerCase(Locale.ROOT).trim();

        // 1. Local matching
        FoodMacros localMatch = matchLocalDatabase(query);
        if (localMatch != null) {
            log.info("Local nutrition match found for query '{}' -> {}", query, localMatch);
            return localMatch;
        }

        // 2. USDA API Fallback
        try {
            FoodMacros apiMatch = fetchFromUSDA(query);
            if (apiMatch != null) {
                log.info("USDA database match found for query '{}' -> {}", query, apiMatch);
                return apiMatch;
            }
        } catch (Exception e) {
            log.warn("USDA API lookup failed for query '{}': {}. Using generic fallback.", query, e.getMessage());
        }

        // 3. Generic fallback macros (rough average for unknown foods)
        log.warn("No nutrition database record found for '{}'. Using generic fallback.", query);
        return new FoodMacros(120, 5.0, 15.0, 4.0);
    }

    private FoodMacros matchLocalDatabase(String query) {
        // Try exact match first
        if (LOCAL_DATABASE.containsKey(query)) {
            return LOCAL_DATABASE.get(query);
        }

        // Try substring matching
        for (Map.Entry<String, FoodMacros> entry : LOCAL_DATABASE.entrySet()) {
            if (query.contains(entry.getKey()) || entry.getKey().contains(query)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private FoodMacros fetchFromUSDA(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("https://api.nal.usda.gov/fdc/v1/foods/search?api_key=%s&query=%s&pageSize=1", apiKey, encodedQuery);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("USDA API returned HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode foodsNode = root.path("foods");

        if (!foodsNode.isArray() || foodsNode.isEmpty()) {
            return null;
        }

        JsonNode firstFood = foodsNode.get(0);
        JsonNode nutrientsNode = firstFood.path("foodNutrients");

        if (!nutrientsNode.isArray() || nutrientsNode.isEmpty()) {
            return null;
        }

        double calories = 0.0;
        double protein = 0.0;
        double carbs = 0.0;
        double fat = 0.0;

        for (JsonNode nut : nutrientsNode) {
            String nutName = nut.path("nutrientName").asText("").toLowerCase(Locale.ROOT);
            String unit = nut.path("unitName").asText("").toLowerCase(Locale.ROOT);
            double val = nut.path("value").asDouble(0.0);

            if (nutName.contains("protein")) {
                protein = val;
            } else if (nutName.contains("carbohydrate")) {
                carbs = val;
            } else if (nutName.contains("lipid") || nutName.contains("fat")) {
                fat = val;
            } else if (nutName.contains("energy") && unit.contains("kcal")) {
                calories = val;
            }
        }

        return new FoodMacros(calories, protein, carbs, fat);
    }
}
