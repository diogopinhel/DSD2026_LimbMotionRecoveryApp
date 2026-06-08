# Exercises — Database Setup Guide

This document explains the `exercises` table structure, what each field means, and how to insert the 10 seed records into the V2 backend database.

> **Note:** `thumbnail_url` is intentionally empty in all records for now. It will be filled in a later step when the images are hosted.

---

## Field Reference

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | Integer | Yes | Unique identifier for the exercise. Auto-increment or set manually (1–10). |
| `name` | String | Yes | Display name of the exercise shown in the app. |
| `category` | String | Yes | Groups exercises by type. Values: `Lower Body`, `Gait`, `Flexibility`, `Balance`. |
| `description` | Text | Yes | One or two sentences explaining the purpose of the exercise in a rehab context. Shown in the exercise detail screen. |
| `instructions` | Array of strings | Yes | Step-by-step instructions. Each string is one numbered step. Shown as a list in the app. |
| `gif_url` | String (URL) | Yes | Direct URL to an animated GIF demonstrating the exercise movement. |
| `thumbnail_url` | String (URL) | No | Static image used as the card thumbnail. **Leave empty (`""`) for now.** |
| `muscle_groups` | Array of strings | Yes | Body parts targeted. Values used: `Upper Legs`, `Lower Legs`, `Glutes`, `Abs`. |
| `equipment` | String | Yes | Equipment needed. All current exercises use `Body Weight`. |
| `difficulty` | String | Yes | Exercise difficulty level. Values: `Beginner`, `Intermediate`, `Advanced`. |

---

## Database Structure

### PostgreSQL

```sql
CREATE TABLE exercises (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    category      VARCHAR(50)    NOT NULL,
    description   TEXT           NOT NULL,
    instructions  JSONB          NOT NULL,
    gif_url       TEXT           NOT NULL,
    thumbnail_url TEXT           NOT NULL DEFAULT '',
    muscle_groups JSONB          NOT NULL,
    equipment     VARCHAR(100)   NOT NULL,
    difficulty    VARCHAR(20)    NOT NULL
);
```

### MySQL

```sql
CREATE TABLE exercises (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    category      VARCHAR(50)    NOT NULL,
    description   TEXT           NOT NULL,
    instructions  JSON           NOT NULL,
    gif_url       TEXT           NOT NULL,
    thumbnail_url TEXT           NOT NULL DEFAULT '',
    muscle_groups JSON           NOT NULL,
    equipment     VARCHAR(100)   NOT NULL,
    difficulty    VARCHAR(20)    NOT NULL
);
```

### MongoDB (if NoSQL)

No schema creation needed — just insert the documents directly into a collection called `exercises` using the seed script below.

---

## Seed Data — SQL Insert

Copy and run the following in your database client:

```sql
INSERT INTO exercises (id, name, category, description, instructions, gif_url, thumbnail_url, muscle_groups, equipment, difficulty) VALUES

(1, 'Squat', 'Lower Body',
 'Strengthens quadriceps, glutes and core. Essential for regaining functional leg strength after lower limb surgery.',
 '["Stand with feet shoulder-width apart, toes pointing slightly outward.",
   "Extend your arms forward for balance and keep your chest up.",
   "Slowly bend your knees and sit back with your hips, as if sitting into a chair.",
   "Lower until your knees are parallel with your glutes, or as far as comfortable.",
   "Return to the starting position, pressing through your heels.",
   "Keep your knees aligned with your toes throughout — do not let them cave inward."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/493.gif', '',
 '["Upper Legs", "Glutes", "Abs"]', 'Body Weight', 'Beginner'),

(2, 'Walking Test', 'Gait',
 'Assesses basic gait quality and mobility after lower limb injury or surgery.',
 '["Stand upright with eyes forward and arms relaxed at your sides.",
   "Walk forward at a natural, comfortable pace for 5 metres.",
   "Maintain an even stride, keeping your weight centred.",
   "Turn around and return to the starting position.",
   "Use a walking aid or hold a wall if needed for safety."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/1373.gif', '',
 '["Lower Legs", "Upper Legs", "Glutes"]', 'Body Weight', 'Beginner'),

(3, 'Stair Climbing', 'Lower Body',
 'Builds functional strength and confidence in lower limb joints for everyday activities.',
 '["Stand facing a staircase and hold the handrail for support.",
   "Step up with the stronger or less painful leg first.",
   "Bring the other leg up to join it on the same step.",
   "Continue climbing one step at a time, maintaining upright posture.",
   "To descend, step down with the weaker leg first.",
   "Stop if you feel sharp pain or instability."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/1225.gif', '',
 '["Upper Legs", "Glutes", "Lower Legs"]', 'Body Weight', 'Intermediate'),

(4, 'Straight Leg Raise', 'Lower Body',
 'Strengthens the quadriceps without knee flexion. Ideal for early post-surgery rehabilitation when the knee cannot yet bend.',
 '["Lie flat on your back on the floor with arms at your sides.",
   "Bend one knee with the foot flat on the floor for support.",
   "Keep the other leg straight and tighten its thigh muscle.",
   "Slowly lift the straight leg to approximately 45°, level with the bent knee.",
   "Hold for 2 seconds at the top.",
   "Lower slowly back to the floor.",
   "Complete all reps on one side before switching legs."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/982.gif', '',
 '["Upper Legs", "Abs"]', 'Body Weight', 'Beginner'),

(5, 'Knee Extension', 'Lower Body',
 'Isolates and strengthens the quadriceps through controlled knee extension. Suitable for both machine and chair-based rehabilitation.',
 '["Sit upright on a chair or machine with your back firmly against the support.",
   "Let your feet hang naturally at a 90-degree angle.",
   "Grip the edges of the seat or handles to stabilise yourself.",
   "Slowly extend one or both legs until fully straight.",
   "Pause briefly at the top — do not snap or lock the knees.",
   "Lower slowly back to the starting position.",
   "Use controlled movements throughout; do not swing."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/130.gif', '',
 '["Upper Legs"]', 'Body Weight', 'Beginner'),

(6, 'Ankle Pumps', 'Lower Body',
 'Promotes circulation and reduces swelling in the lower limb. Especially important in the first days after surgery.',
 '["Sit in a chair or lie on your back with your legs comfortably extended.",
   "Slowly flex your foot upward, pulling your toes towards you.",
   "Hold for 2–3 seconds.",
   "Then slowly point your foot downward away from you.",
   "Hold for 2–3 seconds.",
   "Repeat in a continuous pumping motion.",
   "Perform on both ankles, 10–20 repetitions each."]',
 'https://www.physio-pedia.com/images/archive/3/35/20200323205608%21Ankle_pumps.gif', '',
 '["Lower Legs"]', 'Body Weight', 'Beginner'),

(7, 'Hip Abduction', 'Lower Body',
 'Strengthens the hip abductor muscles. Important for knee and hip stability during recovery.',
 '["Lie on your side on the floor or a mat.",
   "Prop yourself up on your bottom elbow, directly beneath your shoulder.",
   "Keep your body in a straight line from head to feet.",
   "Slowly lift your top leg upward as high as you comfortably can without rotating your hips backward.",
   "Pause briefly at the top.",
   "Lower the leg slowly back to the starting position.",
   "Complete all reps on one side before turning over."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/1361.gif', '',
 '["Upper Legs", "Glutes"]', 'Body Weight', 'Beginner'),

(8, 'Calf Raises', 'Lower Body',
 'Strengthens the calf muscles and improves ankle stability. Supports safe return to walking and load-bearing activities.',
 '["Stand with the balls of your feet on the edge of a step, heels hanging off.",
   "Hold a wall or handrail lightly for balance.",
   "Let your heels drop down as far as comfortable to get a full calf stretch.",
   "Slowly raise your heels up as high as possible, squeezing your calf muscles.",
   "Hold at the top for 1–2 seconds.",
   "Lower slowly back to the starting position.",
   "If no step is available, perform flat on the floor for a reduced range."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/1227.gif', '',
 '["Lower Legs"]', 'Body Weight', 'Beginner'),

(9, 'Hamstring Stretch', 'Flexibility',
 'Stretches the hamstring muscles to restore range of motion and prevent tightness after lower limb injury.',
 '["Sit on the floor with both legs extended straight out in front of you.",
   "Place a belt, towel or resistance band around one foot and hold both ends.",
   "Keep your back straight — do not round your spine.",
   "Gently pull back on the belt to draw your toes towards you.",
   "Lean slightly forward from the hips until you feel a stretch along the back of your thigh.",
   "Hold the stretch for 15–30 seconds.",
   "Release slowly and repeat on the other leg."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/932.gif', '',
 '["Upper Legs", "Lower Legs"]', 'Body Weight', 'Intermediate'),

(10, 'Single-Leg Balance', 'Balance',
 'Trains proprioception and joint stability. A key functional milestone in lower limb rehabilitation.',
 '["Stand upright with both arms relaxed at your sides.",
   "Focus on a fixed point in front of you to help maintain balance.",
   "Slowly lift one foot off the floor, keeping the standing knee slightly soft.",
   "Hold the balance on one leg for up to 30 seconds.",
   "Stand next to a wall or sturdy surface as a safety measure if needed.",
   "Lower the foot and rest briefly, then switch sides.",
   "As you progress, try closing your eyes briefly to increase the difficulty."]',
 'https://cdn.jefit.com/assets/img/exercises/gifs/662.gif', '',
 '["Abs", "Glutes", "Upper Legs"]', 'Body Weight', 'Advanced');
```

---

## Seed Data — MongoDB

If the V2 backend uses MongoDB, run the following in the Mongo shell or Compass:

```js
db.exercises.insertMany([
  {
    id: 1, name: "Squat", category: "Lower Body",
    description: "Strengthens quadriceps, glutes and core. Essential for regaining functional leg strength after lower limb surgery.",
    instructions: [
      "Stand with feet shoulder-width apart, toes pointing slightly outward.",
      "Extend your arms forward for balance and keep your chest up.",
      "Slowly bend your knees and sit back with your hips, as if sitting into a chair.",
      "Lower until your knees are parallel with your glutes, or as far as comfortable.",
      "Return to the starting position, pressing through your heels.",
      "Keep your knees aligned with your toes throughout — do not let them cave inward."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/493.gif",
    thumbnail_url: "",
    muscle_groups: ["Upper Legs", "Glutes", "Abs"],
    equipment: "Body Weight", difficulty: "Beginner"
  },
  {
    id: 2, name: "Walking Test", category: "Gait",
    description: "Assesses basic gait quality and mobility after lower limb injury or surgery.",
    instructions: [
      "Stand upright with eyes forward and arms relaxed at your sides.",
      "Walk forward at a natural, comfortable pace for 5 metres.",
      "Maintain an even stride, keeping your weight centred.",
      "Turn around and return to the starting position.",
      "Use a walking aid or hold a wall if needed for safety."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/1373.gif",
    thumbnail_url: "",
    muscle_groups: ["Lower Legs", "Upper Legs", "Glutes"],
    equipment: "Body Weight", difficulty: "Beginner"
  },
  {
    id: 3, name: "Stair Climbing", category: "Lower Body",
    description: "Builds functional strength and confidence in lower limb joints for everyday activities.",
    instructions: [
      "Stand facing a staircase and hold the handrail for support.",
      "Step up with the stronger or less painful leg first.",
      "Bring the other leg up to join it on the same step.",
      "Continue climbing one step at a time, maintaining upright posture.",
      "To descend, step down with the weaker leg first.",
      "Stop if you feel sharp pain or instability."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/1225.gif",
    thumbnail_url: "",
    muscle_groups: ["Upper Legs", "Glutes", "Lower Legs"],
    equipment: "Body Weight", difficulty: "Intermediate"
  },
  {
    id: 4, name: "Straight Leg Raise", category: "Lower Body",
    description: "Strengthens the quadriceps without knee flexion. Ideal for early post-surgery rehabilitation when the knee cannot yet bend.",
    instructions: [
      "Lie flat on your back on the floor with arms at your sides.",
      "Bend one knee with the foot flat on the floor for support.",
      "Keep the other leg straight and tighten its thigh muscle.",
      "Slowly lift the straight leg to approximately 45°, level with the bent knee.",
      "Hold for 2 seconds at the top.",
      "Lower slowly back to the floor.",
      "Complete all reps on one side before switching legs."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/982.gif",
    thumbnail_url: "",
    muscle_groups: ["Upper Legs", "Abs"],
    equipment: "Body Weight", difficulty: "Beginner"
  },
  {
    id: 5, name: "Knee Extension", category: "Lower Body",
    description: "Isolates and strengthens the quadriceps through controlled knee extension. Suitable for both machine and chair-based rehabilitation.",
    instructions: [
      "Sit upright on a chair or machine with your back firmly against the support.",
      "Let your feet hang naturally at a 90-degree angle.",
      "Grip the edges of the seat or handles to stabilise yourself.",
      "Slowly extend one or both legs until fully straight.",
      "Pause briefly at the top — do not snap or lock the knees.",
      "Lower slowly back to the starting position.",
      "Use controlled movements throughout; do not swing."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/130.gif",
    thumbnail_url: "",
    muscle_groups: ["Upper Legs"],
    equipment: "Body Weight", difficulty: "Beginner"
  },
  {
    id: 6, name: "Ankle Pumps", category: "Lower Body",
    description: "Promotes circulation and reduces swelling in the lower limb. Especially important in the first days after surgery.",
    instructions: [
      "Sit in a chair or lie on your back with your legs comfortably extended.",
      "Slowly flex your foot upward, pulling your toes towards you.",
      "Hold for 2–3 seconds.",
      "Then slowly point your foot downward away from you.",
      "Hold for 2–3 seconds.",
      "Repeat in a continuous pumping motion.",
      "Perform on both ankles, 10–20 repetitions each."
    ],
    gif_url: "https://www.physio-pedia.com/images/archive/3/35/20200323205608%21Ankle_pumps.gif",
    thumbnail_url: "",
    muscle_groups: ["Lower Legs"],
    equipment: "Body Weight", difficulty: "Beginner"
  },
  {
    id: 7, name: "Hip Abduction", category: "Lower Body",
    description: "Strengthens the hip abductor muscles. Important for knee and hip stability during recovery.",
    instructions: [
      "Lie on your side on the floor or a mat.",
      "Prop yourself up on your bottom elbow, directly beneath your shoulder.",
      "Keep your body in a straight line from head to feet.",
      "Slowly lift your top leg upward as high as you comfortably can without rotating your hips backward.",
      "Pause briefly at the top.",
      "Lower the leg slowly back to the starting position.",
      "Complete all reps on one side before turning over."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/1361.gif",
    thumbnail_url: "",
    muscle_groups: ["Upper Legs", "Glutes"],
    equipment: "Body Weight", difficulty: "Beginner"
  },
  {
    id: 8, name: "Calf Raises", category: "Lower Body",
    description: "Strengthens the calf muscles and improves ankle stability. Supports safe return to walking and load-bearing activities.",
    instructions: [
      "Stand with the balls of your feet on the edge of a step, heels hanging off.",
      "Hold a wall or handrail lightly for balance.",
      "Let your heels drop down as far as comfortable to get a full calf stretch.",
      "Slowly raise your heels up as high as possible, squeezing your calf muscles.",
      "Hold at the top for 1–2 seconds.",
      "Lower slowly back to the starting position.",
      "If no step is available, perform flat on the floor for a reduced range."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/1227.gif",
    thumbnail_url: "",
    muscle_groups: ["Lower Legs"],
    equipment: "Body Weight", difficulty: "Beginner"
  },
  {
    id: 9, name: "Hamstring Stretch", category: "Flexibility",
    description: "Stretches the hamstring muscles to restore range of motion and prevent tightness after lower limb injury.",
    instructions: [
      "Sit on the floor with both legs extended straight out in front of you.",
      "Place a belt, towel or resistance band around one foot and hold both ends.",
      "Keep your back straight — do not round your spine.",
      "Gently pull back on the belt to draw your toes towards you.",
      "Lean slightly forward from the hips until you feel a stretch along the back of your thigh.",
      "Hold the stretch for 15–30 seconds.",
      "Release slowly and repeat on the other leg."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/932.gif",
    thumbnail_url: "",
    muscle_groups: ["Upper Legs", "Lower Legs"],
    equipment: "Body Weight", difficulty: "Intermediate"
  },
  {
    id: 10, name: "Single-Leg Balance", category: "Balance",
    description: "Trains proprioception and joint stability. A key functional milestone in lower limb rehabilitation.",
    instructions: [
      "Stand upright with both arms relaxed at your sides.",
      "Focus on a fixed point in front of you to help maintain balance.",
      "Slowly lift one foot off the floor, keeping the standing knee slightly soft.",
      "Hold the balance on one leg for up to 30 seconds.",
      "Stand next to a wall or sturdy surface as a safety measure if needed.",
      "Lower the foot and rest briefly, then switch sides.",
      "As you progress, try closing your eyes briefly to increase the difficulty."
    ],
    gif_url: "https://cdn.jefit.com/assets/img/exercises/gifs/662.gif",
    thumbnail_url: "",
    muscle_groups: ["Abs", "Glutes", "Upper Legs"],
    equipment: "Body Weight", difficulty: "Advanced"
  }
]);
```

---

## Updating thumbnail_url Later

When the GitHub image repository is ready, update each record with the corresponding raw URL:

```sql
-- SQL example
UPDATE exercises SET thumbnail_url = 'https://raw.githubusercontent.com/dpinhel/limbmotion-assets/main/squat.png' WHERE id = 1;
UPDATE exercises SET thumbnail_url = 'https://raw.githubusercontent.com/dpinhel/limbmotion-assets/main/walking_test.png' WHERE id = 2;
-- repeat for each exercise
```

```js
// MongoDB example
db.exercises.updateOne({ id: 1 }, { $set: { thumbnail_url: "https://raw.githubusercontent.com/dpinhel/limbmotion-assets/main/squat.png" } });
```

---

## Exercise Summary

| ID | Name | Category | Difficulty |
|---|---|---|---|
| 1 | Squat | Lower Body | Beginner |
| 2 | Walking Test | Gait | Beginner |
| 3 | Stair Climbing | Lower Body | Intermediate |
| 4 | Straight Leg Raise | Lower Body | Beginner |
| 5 | Knee Extension | Lower Body | Beginner |
| 6 | Ankle Pumps | Lower Body | Beginner |
| 7 | Hip Abduction | Lower Body | Beginner |
| 8 | Calf Raises | Lower Body | Beginner |
| 9 | Hamstring Stretch | Flexibility | Intermediate |
| 10 | Single-Leg Balance | Balance | Advanced |
