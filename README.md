# PWR Protein Tracker

A full-stack meal & macro tracker: snap or upload a photo of a meal, get an
AI-estimated macro breakdown, review/edit it, and track daily protein,
calorie, carb, and fat goals with a streak counter and coaching messages.

Backend: **Spring Boot 3 / Java 21**, JWT auth, JPA + H2.
Frontend: a single static HTML/CSS/vanilla-JS page served by Spring — no
build step, no framework.

## Features

- Email/password auth with JWT (stateless, BCrypt-hashed passwords)
- Meal photo upload via drag-and-drop or live webcam capture
- AI-estimated macros (protein/calories/carbs/fat) with a per-item ingredient
  breakdown, editable before confirming
- Daily summary with progress bars against personalized goals
- Streak tracking (consecutive days with a logged meal) and a rule-based
  "AI coach" (Reppy) that gives time-of-day and progress-aware suggestions
- 7-day journal / meal history view
- Customizable daily macro goals per user

## What's real vs. simulated right now

Being upfront about this, since it matters for anyone evaluating the code:

- **Meal photo analysis calls Google's Gemini vision API for real.**
  [`GeminiVisionService`](src/main/java/com/proteintracker/service/GeminiVisionService.java)
  sends the uploaded image to the Gemini API and asks for a structured JSON
  estimate (dish description, meal type, macros, per-item breakdown,
  confidence score). This only runs when `GEMINI_API_KEY` is set — see
  Configuration below.
- **Automatic fallback when no key is configured, or the API call fails.**
  [`AIMealAnalysisService`](src/main/java/com/proteintracker/service/AIMealAnalysisService.java)
  catches any vision-API error (missing key, network failure, malformed
  response) and falls back to a deterministic heuristic that matches
  keywords in the filename, so meal upload always succeeds even without a
  key or if Gemini is unreachable.
- **Suggestions/coaching are still rule-based**, not model-generated —
  [`AISuggestionService`](src/main/java/com/proteintracker/service/AISuggestionService.java)
  is time-of-day + remaining-macros logic.
- Everything else (auth, persistence, streaks, goals, journal) is fully
  functional against a real database.

## Architecture

```
Browser (static index.html, vanilla JS)
   │  fetch() + JWT bearer token
   ▼
Controller  (AuthController, MealController, UserProfileController)
   │
Service     (AuthService, MealService, UserProfileService,
             AISuggestionService, AIMealAnalysisService, FileStorageService)
   │
Repository  (Spring Data JPA)
   │
H2 file database (data/protein_tracker.mv.db)
```

Auth is stateless JWT via a custom `OncePerRequestFilter`
([`JwtAuthFilter`](src/main/java/com/proteintracker/security/JwtAuthFilter.java)).
Meal photos are stored on local disk (`uploads/`) and served back through a
Spring resource handler.

## Running locally

**Prerequisites:** JDK 21, Maven.

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080` and serves the frontend at `/`.
The H2 database file is created automatically at `data/protein_tracker.mv.db`
on first run (schema is auto-managed via `ddl-auto: update`).

### Configuration

JWT secret defaults to a dev-only value in `application.yml`. For anything
beyond local testing, set a real secret via environment variable:

```bash
JWT_SECRET=<32+ byte random string> mvn spring-boot:run
```

To enable real AI meal analysis, set a Gemini API key before starting
the app (or put it in a local `.env` file — see `.env.example`):

```bash
GEMINI_API_KEY=... mvn spring-boot:run
```

Without it, meal uploads still work — they just use the heuristic fallback
estimator instead of a real vision call (see "What's real vs. simulated").

## API

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for the full endpoint
reference, or [TESTING_GUIDE.md](TESTING_GUIDE.md) for copy-paste
PowerShell/curl examples.

## Known limitations / roadmap

- [ ] Automated test coverage (`src/test` is currently empty)
- [ ] `/uploads/**` is served without per-user access control (photo
      filenames are unguessable UUIDs, but there's no ownership check)
- [ ] No token revocation/logout — JWTs are valid until they expire (24h)
