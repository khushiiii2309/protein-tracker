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

- **Meal photo analysis is currently a heuristic, not a vision model.**
  [`AIMealAnalysisService`](src/main/java/com/proteintracker/service/AIMealAnalysisService.java)
  matches keywords in the uploaded filename (or falls back to a deterministic
  hash-based rotation through a handful of canned meals) — it does not look
  at the actual image contents yet. The upload → review → confirm flow, the
  data model, and the UI are all built around a real vision-AI call; wiring
  one in is the next step (see Roadmap).
- **Suggestions/coaching are rule-based**, not model-generated —
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

## API

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for the full endpoint
reference, or [TESTING_GUIDE.md](TESTING_GUIDE.md) for copy-paste
PowerShell/curl examples.

## Known limitations / roadmap

- [ ] Wire `AIMealAnalysisService` to a real vision model (send image bytes,
      get back structured macro estimates) instead of filename matching
- [ ] Automated test coverage (`src/test` is currently empty)
- [ ] `/uploads/**` is served without per-user access control (photo
      filenames are unguessable UUIDs, but there's no ownership check)
- [ ] No token revocation/logout — JWTs are valid until they expire (24h)
