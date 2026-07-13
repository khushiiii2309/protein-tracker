# Testing Guide - Protein Tracker API

## Quick Start Testing

### 1. Start the Application

```powershell
cd "c:\Users\hp\OneDrive\Desktop\protein-tracker"
mvn spring-boot:run
```

Wait for the message: `Started ProteinTrackerApplication in X seconds`

---

## 2. Test with PowerShell (Easy Method)

### Step 1: Register a New User

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"email":"maya@test.com","password":"test1234","name":"Maya"}'

# Save the token for later use
$token = $response.token
Write-Host "Token: $token"
```

### Step 2: Get Today's Summary (Empty at first)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/meals/today" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"}
```

**Expected Response:**
```json
{
  "totalProteinG": 0.0,
  "totalCaloriesKcal": 0.0,
  "totalCarbsG": 0.0,
  "totalFatG": 0.0,
  "proteinGoalG": 140.0,
  "proteinRemainingG": 140.0,
  "caloriesGoalKcal": 2000.0,
  "carbsGoalG": 200.0,
  "fatGoalG": 65.0,
  "currentStreak": 0,
  "aiSuggestion": "Let's crush it! A protein-rich meal will help you hit your 140g daily target.",
  "meals": []
}
```

### Step 3: Upload a Meal Photo

```powershell
# Create a test image file (or use your own)
$testImagePath = "C:\Users\hp\OneDrive\Desktop\protein-tracker\test-meal.jpg"

# If you don't have an image, create a dummy one
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap(640, 480)
$graphics = [System.Drawing.Graphics]::FromImage($bmp)
$graphics.Clear([System.Drawing.Color]::LightGray)
$font = New-Object System.Drawing.Font("Arial", 24)
$graphics.DrawString("Test Meal", $font, [System.Drawing.Brushes]::Black, 200, 200)
$bmp.Save($testImagePath, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$graphics.Dispose()
$bmp.Dispose()

# Upload the photo
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$fileBytes = [System.IO.File]::ReadAllBytes($testImagePath)
$fileEnc = [System.Text.Encoding]::GetEncoding('ISO-8859-1').GetString($fileBytes)

$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"photo`"; filename=`"meal.jpg`"",
    "Content-Type: image/jpeg$LF",
    $fileEnc,
    "--$boundary--$LF"
) -join $LF

Invoke-RestMethod -Uri "http://localhost:8080/api/meals/photo" `
  -Method POST `
  -Headers @{
    "Authorization"="Bearer $token"
    "Content-Type"="multipart/form-data; boundary=$boundary"
  } `
  -Body $bodyLines
```

### Step 4: Get AI Suggestions

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/meals/suggestions" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"} | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "suggestion": "Let's crush it! A protein-rich meal will help you hit your 140g daily target.",
  "reason": "You're 140g from today's goal — you've got this! 💪",
  "recommendedMeals": [
    "Grilled steak with veggies (46g protein, 510 kcal)",
    "Baked salmon & rice (44g protein, 580 kcal)",
    "Chicken stir-fry (38g protein, 490 kcal)"
  ]
}
```

### Step 5: View Your Profile

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/profile" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"} | ConvertTo-Json
```

### Step 6: Update Your Goals

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/profile/goals" `
  -Method PUT `
  -Headers @{
    "Authorization"="Bearer $token"
    "Content-Type"="application/json"
  } `
  -Body '{"dailyProteinGoalGrams":150,"dailyCaloriesGoalKcal":2200}' | ConvertTo-Json
```

### Step 7: View Journal (7-day history)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/meals/journal?days=7" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"} | ConvertTo-Json -Depth 10
```

**Expected Response:**
```json
{
  "currentStreak": 1,
  "streakMessage": "Great start! Keep the momentum going! 🔥",
  "days": [
    {
      "date": "Today, Jul 7",
      "totalProteinG": 0.0,
      "goalHit": false,
      "meals": [...]
    }
  ]
}
```

---

## 3. Test with cURL (Alternative)

### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"maya@test.com\",\"password\":\"test1234\",\"name\":\"Maya\"}"
```

### Get Today's Summary
```bash
curl -X GET http://localhost:8080/api/meals/today \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Get Suggestions
```bash
curl -X GET http://localhost:8080/api/meals/suggestions \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Upload Photo
```bash
curl -X POST http://localhost:8080/api/meals/photo \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -F "photo=@path/to/your/image.jpg"
```

### Get Journal
```bash
curl -X GET "http://localhost:8080/api/meals/journal?days=7" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Get Profile
```bash
curl -X GET http://localhost:8080/api/profile \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Update Goals
```bash
curl -X PUT http://localhost:8080/api/profile/goals \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d "{\"dailyProteinGoalGrams\":150}"
```

---

## 4. Complete PowerShell Test Script

Save this as `test-api.ps1`:

```powershell
# Complete API Test Script
Write-Host "🧪 Starting Protein Tracker API Tests" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$testEmail = "test_$(Get-Random)@example.com"

# Test 1: Register
Write-Host "`n✅ Test 1: Register User" -ForegroundColor Green
$registerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body (@{email=$testEmail; password="test1234"; name="TestUser"} | ConvertTo-Json)

$token = $registerResponse.token
Write-Host "   ✓ User registered: $($registerResponse.email)" -ForegroundColor Gray
Write-Host "   ✓ Token received" -ForegroundColor Gray

# Test 2: Login
Write-Host "`n✅ Test 2: Login" -ForegroundColor Green
$loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body (@{email=$testEmail; password="test1234"} | ConvertTo-Json)
Write-Host "   ✓ Login successful" -ForegroundColor Gray

# Test 3: Get Profile
Write-Host "`n✅ Test 3: Get Profile" -ForegroundColor Green
$profile = Invoke-RestMethod -Uri "$baseUrl/api/profile" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"}
Write-Host "   ✓ Protein Goal: $($profile.dailyProteinGoalGrams)g" -ForegroundColor Gray
Write-Host "   ✓ Calories Goal: $($profile.dailyCaloriesGoalKcal) kcal" -ForegroundColor Gray
Write-Host "   ✓ Current Streak: $($profile.currentStreak) days" -ForegroundColor Gray

# Test 4: Get Today's Summary
Write-Host "`n✅ Test 4: Get Today's Summary" -ForegroundColor Green
$summary = Invoke-RestMethod -Uri "$baseUrl/api/meals/today" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"}
Write-Host "   ✓ Total Protein: $($summary.totalProteinG)g / $($summary.proteinGoalG)g" -ForegroundColor Gray
Write-Host "   ✓ AI Suggestion: $($summary.aiSuggestion)" -ForegroundColor Gray

# Test 5: Get AI Suggestions
Write-Host "`n✅ Test 5: Get AI Suggestions" -ForegroundColor Green
$suggestions = Invoke-RestMethod -Uri "$baseUrl/api/meals/suggestions" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"}
Write-Host "   ✓ Suggestion: $($suggestions.suggestion)" -ForegroundColor Gray
Write-Host "   ✓ Recommended meals: $($suggestions.recommendedMeals.Count)" -ForegroundColor Gray

# Test 6: Get Journal
Write-Host "`n✅ Test 6: Get Journal" -ForegroundColor Green
$journal = Invoke-RestMethod -Uri "$baseUrl/api/meals/journal?days=7" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"}
Write-Host "   ✓ Streak Message: $($journal.streakMessage)" -ForegroundColor Gray
Write-Host "   ✓ Days in journal: $($journal.days.Count)" -ForegroundColor Gray

# Test 7: Update Goals
Write-Host "`n✅ Test 7: Update Goals" -ForegroundColor Green
$updatedProfile = Invoke-RestMethod -Uri "$baseUrl/api/profile/goals" `
  -Method PUT `
  -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
  -Body (@{dailyProteinGoalGrams=150; dailyCaloriesGoalKcal=2200} | ConvertTo-Json)
Write-Host "   ✓ New Protein Goal: $($updatedProfile.dailyProteinGoalGrams)g" -ForegroundColor Gray
Write-Host "   ✓ New Calories Goal: $($updatedProfile.dailyCaloriesGoalKcal) kcal" -ForegroundColor Gray

Write-Host "`n🎉 All tests completed successfully!" -ForegroundColor Cyan
Write-Host "`n📋 Summary:"
Write-Host "   • User: $testEmail"
Write-Host "   • Token: ${token.Substring(0, 20)}..."
Write-Host "   • Streak: $($journal.currentStreak) days"
Write-Host "   • Protein Goal: $($updatedProfile.dailyProteinGoalGrams)g"
```

Run it with:
```powershell
.\test-api.ps1
```

---

## 5. Testing with Postman (Recommended for UI)

1. **Import this collection**: Create a new Postman Collection
2. **Set a collection variable**: 
   - Variable: `baseUrl` = `http://localhost:8080`
   - Variable: `token` = (will be set automatically)

3. **Add these requests**:

### Register (POST)
- URL: `{{baseUrl}}/api/auth/register`
- Body (JSON): 
  ```json
  {
    "email": "maya@test.com",
    "password": "test1234",
    "name": "Maya"
  }
  ```
- Test Script:
  ```javascript
  pm.collectionVariables.set("token", pm.response.json().token);
  ```

### Get Today's Summary (GET)
- URL: `{{baseUrl}}/api/meals/today`
- Headers: `Authorization: Bearer {{token}}`

### Get Suggestions (GET)
- URL: `{{baseUrl}}/api/meals/suggestions`
- Headers: `Authorization: Bearer {{token}}`

### Upload Photo (POST)
- URL: `{{baseUrl}}/api/meals/photo`
- Headers: `Authorization: Bearer {{token}}`
- Body: form-data, key: `photo`, type: File

### Get Journal (GET)
- URL: `{{baseUrl}}/api/meals/journal?days=7`
- Headers: `Authorization: Bearer {{token}}`

---

## 6. What to Expect

### Initial State (No meals logged)
- ✅ Streak: 0
- ✅ Total protein: 0g / 140g
- ✅ AI Suggestion: Encourages you to start tracking
- ✅ Journal: Empty or shows today with no meals

### After Uploading a Photo
- ✅ Meal saved with status: `PENDING_AI`
- ✅ Streak increments to 1 (first meal of the day)
- ✅ Photo stored in `uploads/` folder

### With Manual Data (for full testing)
You can manually add test meals to the database to see:
- ✅ Progress rings calculation
- ✅ Different AI suggestions based on progress
- ✅ Streak counting across multiple days
- ✅ Journal history with multiple meals

---

## 7. Quick Database Check (H2 Console)

If using H2 database (default):

1. Add to `application.yml`:
```yaml
spring:
  h2:
    console:
      enabled: true
```

2. Visit: http://localhost:8080/h2-console
3. JDBC URL: `jdbc:h2:mem:testdb`
4. Username: `sa`
5. Password: (leave empty)

Run queries:
```sql
-- See all users
SELECT * FROM users;

-- See all meals
SELECT * FROM meals;

-- Check streak (meals today)
SELECT COUNT(*) FROM meals 
WHERE user_id = 1 
AND captured_at >= CURRENT_DATE;
```

---

## Troubleshooting

### ❌ "401 Unauthorized"
- Token expired or invalid
- Re-login to get a new token

### ❌ "Connection refused"
- Application not running
- Check: `mvn spring-boot:run` is active

### ❌ "File upload failed"
- Check `uploads/` directory exists
- Windows permissions issue - create manually

### ❌ "No suggestions returned"
- Normal! Suggestions are always returned
- Check response format

---

## Next Steps

Once basic testing works:
1. ✅ Test with real meal photos
2. ✅ Build a simple frontend (React/Vue)
3. ✅ Integrate AI vision service (OpenAI GPT-4 Vision)
4. ✅ Add more meal data to test streak logic
5. ✅ Test edge cases (midnight rollover, timezone handling)

Happy testing! 🚀
