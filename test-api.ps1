# Complete API Test Script for Protein Tracker
# Run this after starting the application with: mvn spring-boot:run

Write-Host "🚀 Protein Tracker API Test Suite" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$testEmail = "maya_$(Get-Random -Maximum 9999)@test.com"

try {
    # Test 1: Register
    Write-Host "`n[1/8] 📝 Registering new user..." -ForegroundColor Yellow
    $registerBody = @{
        email = $testEmail
        password = "test1234"
        name = "Maya"
    } | ConvertTo-Json
    
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $registerBody
    
    $token = $registerResponse.token
    Write-Host "   ✅ User registered: $($registerResponse.name) ($($registerResponse.email))" -ForegroundColor Green
    Write-Host "   🔑 Token: $($token.Substring(0, 30))..." -ForegroundColor Gray

    # Test 2: Login
    Write-Host "`n[2/8] 🔐 Testing login..." -ForegroundColor Yellow
    $loginBody = @{
        email = $testEmail
        password = "test1234"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $loginBody
    
    Write-Host "   ✅ Login successful!" -ForegroundColor Green

    # Test 3: Get Profile
    Write-Host "`n[3/8] 👤 Fetching user profile..." -ForegroundColor Yellow
    $profile = Invoke-RestMethod -Uri "$baseUrl/api/profile" `
        -Method GET `
        -Headers @{"Authorization"="Bearer $token"}
    
    Write-Host "   ✅ Profile loaded:" -ForegroundColor Green
    Write-Host "      • Daily Protein Goal: $($profile.dailyProteinGoalGrams)g" -ForegroundColor Gray
    Write-Host "      • Daily Calories Goal: $($profile.dailyCaloriesGoalKcal) kcal" -ForegroundColor Gray
    Write-Host "      • Daily Carbs Goal: $($profile.dailyCarbsGoalGrams)g" -ForegroundColor Gray
    Write-Host "      • Daily Fat Goal: $($profile.dailyFatGoalGrams)g" -ForegroundColor Gray
    Write-Host "      • Current Streak: $($profile.currentStreak) days" -ForegroundColor Gray

    # Test 4: Get Today's Summary
    Write-Host "`n[4/8] 📊 Getting today's summary..." -ForegroundColor Yellow
    $summary = Invoke-RestMethod -Uri "$baseUrl/api/meals/today" `
        -Method GET `
        -Headers @{"Authorization"="Bearer $token"}
    
    Write-Host "   ✅ Today's stats:" -ForegroundColor Green
    Write-Host "      • Protein: $($summary.totalProteinG)g / $($summary.proteinGoalG)g (remaining: $($summary.proteinRemainingG)g)" -ForegroundColor Gray
    Write-Host "      • Calories: $($summary.totalCaloriesKcal) / $($summary.caloriesGoalKcal) kcal" -ForegroundColor Gray
    Write-Host "      • Streak: $($summary.currentStreak) days" -ForegroundColor Gray
    Write-Host "      • Meals logged: $($summary.meals.Count)" -ForegroundColor Gray
    Write-Host "      • AI says: '$($summary.aiSuggestion)'" -ForegroundColor Cyan

    # Test 5: Get AI Suggestions
    Write-Host "`n[5/8] 🤖 Getting AI meal suggestions..." -ForegroundColor Yellow
    $suggestions = Invoke-RestMethod -Uri "$baseUrl/api/meals/suggestions" `
        -Method GET `
        -Headers @{"Authorization"="Bearer $token"}
    
    Write-Host "   ✅ AI Recommendations:" -ForegroundColor Green
    Write-Host "      💡 $($suggestions.suggestion)" -ForegroundColor Cyan
    Write-Host "      📌 Reason: $($suggestions.reason)" -ForegroundColor Gray
    Write-Host "      🍽️  Recommended meals:" -ForegroundColor Gray
    foreach ($meal in $suggestions.recommendedMeals) {
        Write-Host "         • $meal" -ForegroundColor Gray
    }

    # Test 6: Upload a Photo (create dummy file)
    Write-Host "`n[6/8] 📸 Uploading meal photo..." -ForegroundColor Yellow
    
    # Create a temporary test image
    $tempImage = "$env:TEMP\test-meal-$(Get-Random).jpg"
    Add-Type -AssemblyName System.Drawing
    $bmp = New-Object System.Drawing.Bitmap(800, 600)
    $graphics = [System.Drawing.Graphics]::FromImage($bmp)
    $graphics.Clear([System.Drawing.Color]::FromArgb(45, 45, 45))  # Dark background
    $font = New-Object System.Drawing.Font("Arial", 32, [System.Drawing.FontStyle]::Bold)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(180, 255, 0))  # Neon green
    $graphics.DrawString("Test Meal", $font, $brush, 250, 250)
    $graphics.DrawString("🥗", $font, $brush, 350, 300)
    $bmp.Save($tempImage, [System.Drawing.Imaging.ImageFormat]::Jpeg)
    $graphics.Dispose()
    $bmp.Dispose()
    $font.Dispose()
    $brush.Dispose()
    
    # Upload using multipart/form-data
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    $fileBytes = [System.IO.File]::ReadAllBytes($tempImage)
    $fileEnc = [System.Text.Encoding]::GetEncoding('ISO-8859-1').GetString($fileBytes)
    
    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"photo`"; filename=`"meal.jpg`"",
        "Content-Type: image/jpeg$LF",
        $fileEnc,
        "--$boundary--$LF"
    ) -join $LF
    
    $uploadResponse = Invoke-RestMethod -Uri "$baseUrl/api/meals/photo" `
        -Method POST `
        -Headers @{
            "Authorization"="Bearer $token"
            "Content-Type"="multipart/form-data; boundary=$boundary"
        } `
        -Body $bodyLines
    
    Write-Host "   ✅ Photo uploaded successfully!" -ForegroundColor Green
    Write-Host "      • Meal ID: $($uploadResponse.id)" -ForegroundColor Gray
    Write-Host "      • Status: $($uploadResponse.status)" -ForegroundColor Gray
    Write-Host "      • Photo URL: $($uploadResponse.photoUrl)" -ForegroundColor Gray
    
    Remove-Item $tempImage -ErrorAction SilentlyContinue

    # Test 7: Get Journal
    Write-Host "`n[7/8] 📖 Fetching meal journal..." -ForegroundColor Yellow
    $journal = Invoke-RestMethod -Uri "$baseUrl/api/meals/journal?days=7" `
        -Method GET `
        -Headers @{"Authorization"="Bearer $token"}
    
    Write-Host "   ✅ Journal loaded:" -ForegroundColor Green
    Write-Host "      • Current Streak: $($journal.currentStreak) days" -ForegroundColor Gray
    Write-Host "      • $($journal.streakMessage)" -ForegroundColor Cyan
    Write-Host "      • Days in journal: $($journal.days.Count)" -ForegroundColor Gray
    
    foreach ($day in $journal.days) {
        $goalIcon = if ($day.goalHit) { "✅" } else { "⏳" }
        Write-Host "         $goalIcon $($day.date): $($day.totalProteinG)g protein, $($day.meals.Count) meals" -ForegroundColor Gray
    }

    # Test 8: Update Goals
    Write-Host "`n[8/8] ⚙️  Updating goals..." -ForegroundColor Yellow
    $updateBody = @{
        dailyProteinGoalGrams = 150.0
        dailyCaloriesGoalKcal = 2200.0
        dailyCarbsGoalGrams = 220.0
        dailyFatGoalGrams = 70.0
    } | ConvertTo-Json
    
    $updatedProfile = Invoke-RestMethod -Uri "$baseUrl/api/profile/goals" `
        -Method PUT `
        -Headers @{
            "Authorization"="Bearer $token"
            "Content-Type"="application/json"
        } `
        -Body $updateBody
    
    Write-Host "   ✅ Goals updated successfully!" -ForegroundColor Green
    Write-Host "      • New Protein Goal: $($updatedProfile.dailyProteinGoalGrams)g" -ForegroundColor Gray
    Write-Host "      • New Calories Goal: $($updatedProfile.dailyCaloriesGoalKcal) kcal" -ForegroundColor Gray
    Write-Host "      • New Carbs Goal: $($updatedProfile.dailyCarbsGoalGrams)g" -ForegroundColor Gray
    Write-Host "      • New Fat Goal: $($updatedProfile.dailyFatGoalGrams)g" -ForegroundColor Gray

    # Summary
    Write-Host "`n=================================" -ForegroundColor Cyan
    Write-Host "🎉 ALL TESTS PASSED! 🎉" -ForegroundColor Green
    Write-Host "=================================" -ForegroundColor Cyan
    Write-Host "`n📋 Test Summary:" -ForegroundColor White
    Write-Host "   ✅ User registration and login" -ForegroundColor Green
    Write-Host "   ✅ Profile management" -ForegroundColor Green
    Write-Host "   ✅ Daily summary with goals" -ForegroundColor Green
    Write-Host "   ✅ AI meal suggestions" -ForegroundColor Green
    Write-Host "   ✅ Photo upload" -ForegroundColor Green
    Write-Host "   ✅ Meal journal" -ForegroundColor Green
    Write-Host "   ✅ Goal customization" -ForegroundColor Green
    Write-Host "   ✅ Streak tracking" -ForegroundColor Green
    
    Write-Host "`n🔑 Your test credentials:" -ForegroundColor White
    Write-Host "   Email: $testEmail" -ForegroundColor Gray
    Write-Host "   Password: test1234" -ForegroundColor Gray
    Write-Host "   Token: $($token.Substring(0, 40))..." -ForegroundColor Gray
    
    Write-Host "`n💡 Next steps:" -ForegroundColor White
    Write-Host "   • Check uploaded photo in: uploads/$($uploadResponse.photoUrl)" -ForegroundColor Gray
    Write-Host "   • View API docs: API_DOCUMENTATION.md" -ForegroundColor Gray
    Write-Host "   • Test with Postman for better debugging" -ForegroundColor Gray
    Write-Host "   • Integrate with frontend UI" -ForegroundColor Gray

} catch {
    Write-Host "`n❌ TEST FAILED!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`n💡 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "   1. Make sure the application is running: mvn spring-boot:run" -ForegroundColor Gray
    Write-Host "   2. Check if port 8080 is available" -ForegroundColor Gray
    Write-Host "   3. Verify database is configured correctly" -ForegroundColor Gray
    Write-Host "   4. Check application logs for errors" -ForegroundColor Gray
}
