# Protein Tracker API Documentation

## Overview
Complete API documentation for the Protein Tracker backend, aligned with the PWR (Energetic) UI design.

---

## Authentication Endpoints

### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "maya@example.com",
  "password": "securePassword123",
  "name": "Maya"
}

Response: {
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "maya@example.com",
  "name": "Maya"
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "maya@example.com",
  "password": "securePassword123"
}

Response: {
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "maya@example.com",
  "name": "Maya"
}
```

---

## Meal Endpoints

### Upload Meal Photo
```http
POST /api/meals/photo
Authorization: Bearer {token}
Content-Type: multipart/form-data

photo: [binary file data]

Response: {
  "id": 1,
  "photoUrl": "meal_20260706_123456.jpg",
  "capturedAt": "2026-07-06T12:34:56Z",
  "status": "PENDING_AI",
  "mealType": null,
  "description": null,
  "totalProteinG": null,
  "totalCaloriesKcal": null,
  "totalCarbsG": null,
  "totalFatG": null,
  "confidenceScore": null,
  "foodItems": []
}
```

### Get Today's Summary
```http
GET /api/meals/today
Authorization: Bearer {token}

Response: {
  "totalProteinG": 82.0,
  "totalCaloriesKcal": 1240.0,
  "totalCarbsG": 96.0,
  "totalFatG": 41.0,
  "proteinGoalG": 140.0,
  "proteinRemainingG": 58.0,
  "caloriesGoalKcal": 2000.0,
  "carbsGoalG": 200.0,
  "fatGoalG": 65.0,
  "currentStreak": 12,
  "aiSuggestion": "A grilled chicken bowl now would put you right on target before your workout.",
  "meals": [
    {
      "id": 2,
      "photoUrl": "meal_20260706_124500.jpg",
      "capturedAt": "2026-07-06T12:45:00Z",
      "status": "CONFIRMED",
      "mealType": "Lunch",
      "description": "Turkey wrap",
      "totalProteinG": 34.0,
      "totalCaloriesKcal": 480.0,
      "totalCarbsG": 42.0,
      "totalFatG": 18.0,
      "confidenceScore": 0.92,
      "foodItems": []
    },
    {
      "id": 1,
      "photoUrl": "meal_20260706_082000.jpg",
      "capturedAt": "2026-07-06T08:20:00Z",
      "status": "CONFIRMED",
      "mealType": "Breakfast",
      "description": "Greek yogurt & berries",
      "totalProteinG": 24.0,
      "totalCaloriesKcal": 320.0,
      "totalCarbsG": 38.0,
      "totalFatG": 8.0,
      "confidenceScore": 0.96,
      "foodItems": []
    }
  ]
}
```

### Get AI Suggestions
```http
GET /api/meals/suggestions
Authorization: Bearer {token}

Response: {
  "suggestion": "A grilled chicken bowl now would put you right on target before your workout.",
  "reason": "You're 58g from your protein goal.",
  "recommendedMeals": [
    "Grilled chicken bowl (42g protein, 520 kcal)",
    "Turkey wrap (34g protein, 480 kcal)",
    "Salmon & quinoa (38g protein, 540 kcal)"
  ]
}
```

### Get Journal (Meal History)
```http
GET /api/meals/journal?days=7
Authorization: Bearer {token}

Response: {
  "currentStreak": 12,
  "streakMessage": "12 day streak 🔥 - You're building a solid habit!",
  "days": [
    {
      "date": "Today, Jul 6",
      "totalProteinG": 82.0,
      "goalHit": false,
      "meals": [...]
    },
    {
      "date": "Yesterday, Jul 5",
      "totalProteinG": 138.0,
      "goalHit": true,
      "meals": [
        {
          "id": 5,
          "photoUrl": "meal_20260705_193000.jpg",
          "capturedAt": "2026-07-05T19:30:00Z",
          "status": "CONFIRMED",
          "mealType": "Dinner",
          "description": "Salmon & quinoa",
          "totalProteinG": 46.0,
          "totalCaloriesKcal": 510.0,
          "totalCarbsG": 52.0,
          "totalFatG": 22.0,
          "confidenceScore": 0.94,
          "foodItems": []
        }
      ]
    }
  ]
}
```

---

## User Profile Endpoints

### Get Profile
```http
GET /api/profile
Authorization: Bearer {token}

Response: {
  "id": 1,
  "email": "maya@example.com",
  "name": "Maya",
  "dailyProteinGoalGrams": 140.0,
  "dailyCaloriesGoalKcal": 2000.0,
  "dailyCarbsGoalGrams": 200.0,
  "dailyFatGoalGrams": 65.0,
  "currentStreak": 12
}
```

### Update Goals
```http
PUT /api/profile/goals
Authorization: Bearer {token}
Content-Type: application/json

{
  "dailyProteinGoalGrams": 150.0,
  "dailyCaloriesGoalKcal": 2200.0,
  "dailyCarbsGoalGrams": 220.0,
  "dailyFatGoalGrams": 70.0
}

Response: {
  "id": 1,
  "email": "maya@example.com",
  "name": "Maya",
  "dailyProteinGoalGrams": 150.0,
  "dailyCaloriesGoalKcal": 2200.0,
  "dailyCarbsGoalGrams": 220.0,
  "dailyFatGoalGrams": 70.0,
  "currentStreak": 12
}
```

---

## Data Models

### Meal Status Enum
- `PENDING_AI` - Photo uploaded, waiting for AI analysis
- `AI_ESTIMATED` - AI has analyzed the meal
- `USER_EDITING` - User is editing AI estimates
- `CONFIRMED` - User has confirmed the meal

### Meal Type (String)
- `"Breakfast"`
- `"Lunch"`
- `"Dinner"`
- `"Snack"`

---

## Features Matching the PWR Design

### ✅ Implemented
1. **Daily Macro Tracking** - All macros (protein, calories, carbs, fats) with goals
2. **Streak Tracking** - Consecutive days with logged meals
3. **AI Suggestions** - Context-aware meal recommendations based on progress and time of day
4. **Meal Photo Upload** - Store meal photos for analysis
5. **Journal View** - Multi-day meal history with goal achievement tracking
6. **Personalized Goals** - Customizable daily macro targets
7. **Confidence Scores** - AI estimation confidence (0.0-1.0)

### 🔄 Ready for AI Integration
- Meal photo analysis (currently returns `PENDING_AI` status)
- Food item detection and macro estimation
- Ingredient recognition

### 🎨 Design Elements Supported
- **Progress Ring** - `totalProteinG` / `proteinGoalG` * 100
- **Streak Display** - `currentStreak` with motivational messages
- **Coach "Reppy"** - AI suggestions with personality
- **Meal Cards** - Complete meal metadata (type, description, time, macros)
- **Goal Tracking** - All four macro goals with remaining amounts

---

## Example Flow: Adding a Meal

1. **User takes photo** → `POST /api/meals/photo`
2. **Backend stores photo** → Returns meal with `PENDING_AI` status
3. **AI analyzes photo** (future integration) → Updates to `AI_ESTIMATED`
4. **User confirms** → Status becomes `CONFIRMED`
5. **Today's summary updates** → `GET /api/meals/today` shows new totals
6. **AI generates suggestion** → Based on new progress
7. **Streak increments** → If first meal of the day

---

## Security
All endpoints except `/api/auth/register` and `/api/auth/login` require JWT authentication via the `Authorization: Bearer {token}` header.

---

## Next Steps for Full AI Integration
1. Integrate with vision AI (OpenAI GPT-4 Vision, Google Vision, or similar)
2. Build food recognition model
3. Implement macro estimation algorithm
4. Add user feedback loop for improved accuracy
5. Create food database for reference matching
