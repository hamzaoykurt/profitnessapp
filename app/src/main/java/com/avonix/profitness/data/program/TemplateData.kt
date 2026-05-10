package com.avonix.profitness.data.program

/** Tek bir template hareketini tanımlar. exerciseName, exercises tablosundaki `name` alanıyla eşleşir. */
data class TemplateExercise(
    val exerciseName: String,
    val sets: Int,
    val reps: Int,           // INT — zaman bazlı egzersizler için saniye cinsinden (ör. Plank = 60)
    val restSeconds: Int = 60,           // set arası dinlenme
    val exerciseRestSeconds: Int = 180,  // son set / egzersiz sonu dinlenmesi
    val targetDurationSeconds: Int? = null,
    val targetDistanceMeters: Float? = null,
    val targetElevationMeters: Float? = null,
    val targetInclinePercent: Float? = null
)

data class TemplateDay(
    val title: String,
    val isRestDay: Boolean = false,
    val exercises: List<TemplateExercise> = emptyList()
)

data class ProgramTemplate(
    val key: String,         // READY_PROGRAMS'daki title ile eşleşir
    val days: List<TemplateDay>
)

// ─── 14 Program Şablonu ────────────────────────────────────────────────────────

val PROGRAM_TEMPLATES: List<ProgramTemplate> = listOf(

    // ── MUSCLE ───────────────────────────────────────────────────────────────

    // Based on Reddit PPL (Metallicadpa) — industry-standard 6-day hypertrophy split
    ProgramTemplate("Push / Pull / Legs", listOf(
        TemplateDay("PUSH — Chest, Shoulders & Triceps", exercises = listOf(
            TemplateExercise("Flat Bench Press",            4, 5,  180),
            TemplateExercise("Incline Dumbbell Fly",        4, 12,  90),
            TemplateExercise("Cable Crossover",             3, 15,  60),
            TemplateExercise("Barbell Shoulder Press",      3,  8, 150),
            TemplateExercise("Lateral Raise",               4, 15,  60),
            TemplateExercise("Triceps Pushdown",            3, 12,  60),
            TemplateExercise("Overhead Triceps Extension",  3, 10,  60)
        )),
        TemplateDay("PULL — Back & Biceps", exercises = listOf(
            TemplateExercise("Deadlift",                    4,  5, 240),
            TemplateExercise("Pull-Up",                     3,  8, 120),
            TemplateExercise("Bent Over Barbell Row",       3,  8, 120),
            TemplateExercise("Lat Pulldown",                4, 10,  90),
            TemplateExercise("Seated Cable Row",            3, 10,  90),
            TemplateExercise("Barbell Biceps Curl",         3, 10,  60),
            TemplateExercise("Hammer Curl",                 3, 12,  60)
        )),
        TemplateDay("LEGS — Quads, Hamstrings & Calves", exercises = listOf(
            TemplateExercise("Squat",                       4,  5, 240),
            TemplateExercise("Romanian Deadlift",           4,  8, 120),
            TemplateExercise("Leg Press",                   3, 12,  90),
            TemplateExercise("Leg Curl",                    3, 12,  90),
            TemplateExercise("Leg Extension",               3, 15,  60),
            TemplateExercise("Standing Calf Raise",         5, 15,  60),
            TemplateExercise("Plank",                       3, 60,  60)
        )),
        TemplateDay("PUSH — Chest, Shoulders & Triceps (Vol)", exercises = listOf(
            TemplateExercise("Incline Bench Press",         4,  8, 120),
            TemplateExercise("Dumbbell Bench Press",        4, 10,  90),
            TemplateExercise("Pec Deck Flye",               3, 15,  60),
            TemplateExercise("Dumbbell Shoulder Press",     4, 10,  90),
            TemplateExercise("Cable Lateral Raise",         3, 15,  60),
            TemplateExercise("Skull Crusher",               3, 10,  60),
            TemplateExercise("Dips (Triceps)",              3, 12,  60)
        )),
        TemplateDay("PULL — Back & Biceps (Vol)", exercises = listOf(
            TemplateExercise("Bent Over Barbell Row",       4, 10, 120),
            TemplateExercise("Single Arm Dumbbell Row",     3, 12,  90),
            TemplateExercise("Hammer Grip Lat Pulldown",    3, 12,  90),
            TemplateExercise("Wide Grip Seated Row",        3, 12,  90),
            TemplateExercise("EZ Bar Curl",                 4, 10,  60),
            TemplateExercise("Concentration Curl",          3, 12,  60)
        )),
        TemplateDay("LEGS — Quads, Hamstrings & Calves (Vol)", exercises = listOf(
            TemplateExercise("Hack Squat",                  4, 10, 120),
            TemplateExercise("Bulgarian Split Squat",       3, 10,  90),
            TemplateExercise("Leg Press",                   3, 15,  90),
            TemplateExercise("Seated Leg Curl",             3, 12,  90),
            TemplateExercise("Leg Extension",               3, 15,  60),
            TemplateExercise("Standing Calf Raise",         4, 20,  60),
            TemplateExercise("Ab Wheel",                    3, 10,  60)
        ))
    )),

    // Based on Arnold Schwarzenegger's original 6-day split from "The Education of a Bodybuilder"
    ProgramTemplate("Arnold Split", listOf(
        TemplateDay("Chest & Back", exercises = listOf(
            TemplateExercise("Flat Bench Press",            4,  8, 120),
            TemplateExercise("Bent Over Barbell Row",       4,  8, 120),
            TemplateExercise("Incline Bench Press",         4, 10,  90),
            TemplateExercise("Pull-Up",                     3,  8, 120),
            TemplateExercise("Cable Crossover",             3, 15,  60),
            TemplateExercise("Seated Cable Row",            3, 12,  90)
        )),
        TemplateDay("Shoulders & Arms", exercises = listOf(
            TemplateExercise("Arnold Press",                4, 10,  90),
            TemplateExercise("Barbell Biceps Curl",         4, 10,  90),
            TemplateExercise("Skull Crusher",               4, 10,  90),
            TemplateExercise("Lateral Raise",               3, 15,  60),
            TemplateExercise("Hammer Curl",                 3, 12,  60),
            TemplateExercise("Triceps Pushdown",            3, 12,  60),
            TemplateExercise("Front Raise",                 3, 12,  60)
        )),
        TemplateDay("Legs", exercises = listOf(
            TemplateExercise("Squat",                       5,  8, 180),
            TemplateExercise("Romanian Deadlift",           4,  8, 120),
            TemplateExercise("Leg Press",                   4, 12,  90),
            TemplateExercise("Leg Curl",                    3, 12,  90),
            TemplateExercise("Standing Calf Raise",         5, 20,  60),
            TemplateExercise("Plank",                       3, 60,  60)
        )),
        TemplateDay("Chest & Back (Vol)", exercises = listOf(
            TemplateExercise("Dumbbell Bench Press",        4, 10,  90),
            TemplateExercise("T-Bar Row",                   4, 10,  90),
            TemplateExercise("Pec Deck Flye",               3, 15,  60),
            TemplateExercise("Lat Pulldown",                3, 12,  90),
            TemplateExercise("Decline Bench Press",         3, 10,  90),
            TemplateExercise("Hyperextension",              3, 15,  60)
        )),
        TemplateDay("Shoulders & Arms (Vol)", exercises = listOf(
            TemplateExercise("Dumbbell Shoulder Press",     4, 10,  90),
            TemplateExercise("EZ Bar Curl",                 4, 10,  90),
            TemplateExercise("Close Grip Bench Press",      4, 10,  90),
            TemplateExercise("Rear Delt Fly",               3, 15,  60),
            TemplateExercise("Preacher Curl",               3, 10,  60),
            TemplateExercise("Dips (Triceps)",              3, 12,  60)
        )),
        TemplateDay("Legs (Vol)", exercises = listOf(
            TemplateExercise("Hack Squat",                  4, 10, 120),
            TemplateExercise("Stiff Leg Deadlift",          4, 10,  90),
            TemplateExercise("Leg Extension",               3, 15,  60),
            TemplateExercise("Seated Leg Curl",             3, 12,  90),
            TemplateExercise("Seated Calf Raise",           4, 20,  60),
            TemplateExercise("Ab Wheel",                    3, 10,  60)
        ))
    )),

    // Based on PHUL (Power Hypertrophy Upper Lower) by Brandon Campbell
    ProgramTemplate("Upper / Lower Split", listOf(
        TemplateDay("Upper — Power", exercises = listOf(
            TemplateExercise("Flat Bench Press",            4,  5, 180),
            TemplateExercise("Bent Over Barbell Row",       4,  5, 180),
            TemplateExercise("Barbell Shoulder Press",      3,  5, 150),
            TemplateExercise("Pull-Up",                     3,  6, 120),
            TemplateExercise("Barbell Biceps Curl",         3,  8,  90),
            TemplateExercise("Close Grip Bench Press",      3,  8,  90)
        )),
        TemplateDay("Lower — Power", exercises = listOf(
            TemplateExercise("Squat",                       4,  5, 240),
            TemplateExercise("Deadlift",                    3,  5, 240),
            TemplateExercise("Romanian Deadlift",           3,  8, 120),
            TemplateExercise("Standing Calf Raise",         4, 12,  60),
            TemplateExercise("Plank",                       3, 60,  60)
        )),
        TemplateDay("Upper — Hypertrophy", exercises = listOf(
            TemplateExercise("Incline Bench Press",         4,  8, 120),
            TemplateExercise("Dumbbell Bench Press",        4, 10,  90),
            TemplateExercise("Lat Pulldown",                4, 10,  90),
            TemplateExercise("Dumbbell Shoulder Press",     3, 12,  90),
            TemplateExercise("Lateral Raise",               3, 15,  60),
            TemplateExercise("Hammer Curl",                 3, 12,  60),
            TemplateExercise("Triceps Pushdown",            3, 12,  60)
        )),
        TemplateDay("Lower — Hypertrophy", exercises = listOf(
            TemplateExercise("Hack Squat",                  4, 10, 120),
            TemplateExercise("Leg Press",                   4, 12,  90),
            TemplateExercise("Leg Curl",                    4, 12,  90),
            TemplateExercise("Bulgarian Split Squat",       3, 10,  90),
            TemplateExercise("Leg Extension",               3, 15,  60),
            TemplateExercise("Seated Calf Raise",           4, 20,  60),
            TemplateExercise("Ab Wheel",                    3, 10,  60)
        ))
    )),

    // Classic bodybuilder "one muscle group per day" split
    ProgramTemplate("Bro Split", listOf(
        TemplateDay("Chest", exercises = listOf(
            TemplateExercise("Flat Bench Press",            4,  8, 120),
            TemplateExercise("Incline Bench Press",         4, 10,  90),
            TemplateExercise("Decline Bench Press",         3, 10,  90),
            TemplateExercise("Dumbbell Flat Fly",           3, 12,  60),
            TemplateExercise("Cable Crossover",             3, 15,  60),
            TemplateExercise("Chest Dips",                  3, 10,  90)
        )),
        TemplateDay("Back", exercises = listOf(
            TemplateExercise("Deadlift",                    4,  5, 240),
            TemplateExercise("Pull-Up",                     4,  8, 120),
            TemplateExercise("Bent Over Barbell Row",       4,  8, 120),
            TemplateExercise("Lat Pulldown",                3, 12,  90),
            TemplateExercise("Seated Cable Row",            3, 12,  90),
            TemplateExercise("Hyperextension",              3, 15,  60)
        )),
        TemplateDay("Shoulders", exercises = listOf(
            TemplateExercise("Barbell Shoulder Press",      4,  8, 120),
            TemplateExercise("Dumbbell Shoulder Press",     3, 10,  90),
            TemplateExercise("Lateral Raise",               4, 15,  60),
            TemplateExercise("Front Raise",                 3, 12,  60),
            TemplateExercise("Rear Delt Fly",               3, 15,  60),
            TemplateExercise("Upright Row",                 3, 12,  90),
            TemplateExercise("Shrug",                       4, 15,  60)
        )),
        TemplateDay("Arms — Biceps & Triceps", exercises = listOf(
            TemplateExercise("Barbell Biceps Curl",         4, 10,  90),
            TemplateExercise("Skull Crusher",               4, 10,  90),
            TemplateExercise("EZ Bar Curl",                 3, 10,  60),
            TemplateExercise("Triceps Pushdown",            3, 12,  60),
            TemplateExercise("Hammer Curl",                 3, 12,  60),
            TemplateExercise("Overhead Triceps Extension",  3, 12,  60),
            TemplateExercise("Preacher Curl",               3, 10,  60),
            TemplateExercise("Dips (Triceps)",              3, 12,  60)
        )),
        TemplateDay("Legs & Core", exercises = listOf(
            TemplateExercise("Squat",                       4,  8, 180),
            TemplateExercise("Romanian Deadlift",           4, 10, 120),
            TemplateExercise("Leg Press",                   3, 12,  90),
            TemplateExercise("Leg Curl",                    3, 12,  90),
            TemplateExercise("Leg Extension",               3, 15,  60),
            TemplateExercise("Standing Calf Raise",         4, 20,  60),
            TemplateExercise("Plank",                       3, 60,  60),
            TemplateExercise("Crunch",                      3, 20,  45)
        ))
    )),

    // ── FAT LOSS ─────────────────────────────────────────────────────────────

    // HIIT protocol — 4 days/week, 30-second work / 30-second rest intervals
    ProgramTemplate("HIIT Metabolik", listOf(
        TemplateDay("Upper Body HIIT", exercises = listOf(
            TemplateExercise("Push-Up",                     4, 15,  30),
            TemplateExercise("Dumbbell Bench Press",        3, 12,  45),
            TemplateExercise("Bent Over Barbell Row",       3, 10,  45),
            TemplateExercise("Burpee",                      3, 10,  30),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Dumbbell Shoulder Press",     3, 12,  45)
        )),
        TemplateDay("Lower Body Circuit", exercises = listOf(
            TemplateExercise("Squat",                       4, 15,  45),
            TemplateExercise("Jump Squat",                  4, 12,  30),
            TemplateExercise("Lunge",                       3, 12,  30),
            TemplateExercise("Box Jump",                    3, 10,  45),
            TemplateExercise("Goblet Squat",                3, 15,  45),
            TemplateExercise("Burpee",                      3, 10,  30)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Full Body HIIT", exercises = listOf(
            TemplateExercise("Burpee",                      4, 15,  30),
            TemplateExercise("Kettlebell Swing",            4, 20,  30),
            TemplateExercise("Box Jump",                    3, 10,  45),
            TemplateExercise("Push-Up",                     3, 15,  30),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Plank",                       3, 60,  30)
        )),
        TemplateDay("Cardio & Core", exercises = listOf(
            TemplateExercise("Jump Rope",                   5, 60,  30),
            TemplateExercise("Crunch",                      3, 20,  30),
            TemplateExercise("Leg Raise",                   3, 15,  30),
            TemplateExercise("Russian Twist",               3, 20,  30),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Plank",                       3, 60,  45)
        ))
    )),

    // Full-body fat-loss circuit — 3 days/week with rest days between sessions
    ProgramTemplate("Fat Burn Full Body", listOf(
        TemplateDay("Full Body A", exercises = listOf(
            TemplateExercise("Goblet Squat",                3, 15,  60),
            TemplateExercise("Push-Up",                     3, 15,  60),
            TemplateExercise("Lunge",                       3, 12,  60),
            TemplateExercise("Single Arm Dumbbell Row",     3, 12,  60),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Plank",                       3, 30,  30)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Full Body B", exercises = listOf(
            TemplateExercise("Romanian Deadlift",           3, 12,  60),
            TemplateExercise("Dumbbell Bench Press",        3, 12,  60),
            TemplateExercise("Squat",                       3, 15,  60),
            TemplateExercise("Lat Pulldown",                3, 12,  60),
            TemplateExercise("Burpee",                      3, 10,  45),
            TemplateExercise("Crunch",                      3, 20,  30)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Full Body C", exercises = listOf(
            TemplateExercise("Kettlebell Swing",            4, 20,  45),
            TemplateExercise("Box Jump",                    3, 10,  45),
            TemplateExercise("Push-Up",                     3, 15,  45),
            TemplateExercise("Jump Squat",                  3, 15,  30),
            TemplateExercise("Ab Wheel",                    3, 10,  60),
            TemplateExercise("Russian Twist",               3, 20,  30)
        ))
    )),

    // High-density circuit training — 5 days/week, short rest periods maximize EPOC
    ProgramTemplate("Metabolik Kondisyon", listOf(
        TemplateDay("Upper Push Circuit", exercises = listOf(
            TemplateExercise("Push-Up",                     4, 15,  30),
            TemplateExercise("Dumbbell Bench Press",        3, 12,  45),
            TemplateExercise("Dumbbell Shoulder Press",     3, 12,  45),
            TemplateExercise("Burpee",                      3, 10,  30),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Triceps Pushdown",            3, 12,  45)
        )),
        TemplateDay("Leg Metabolic", exercises = listOf(
            TemplateExercise("Jump Squat",                  4, 15,  30),
            TemplateExercise("Lunge",                       3, 12,  30),
            TemplateExercise("Box Jump",                    3, 10,  45),
            TemplateExercise("Goblet Squat",                3, 15,  45),
            TemplateExercise("Bulgarian Split Squat",       3, 10,  60),
            TemplateExercise("Leg Press",                   3, 15,  60)
        )),
        TemplateDay("Upper Pull Circuit", exercises = listOf(
            TemplateExercise("Pull-Up",                     3,  8,  60),
            TemplateExercise("Lat Pulldown",                3, 12,  45),
            TemplateExercise("Single Arm Dumbbell Row",     3, 12,  45),
            TemplateExercise("Burpee",                      3, 10,  30),
            TemplateExercise("Kettlebell Swing",            3, 20,  30),
            TemplateExercise("Barbell Biceps Curl",         3, 12,  45)
        )),
        TemplateDay("Full Body EMOM", exercises = listOf(
            TemplateExercise("Burpee",                      4, 12,  30),
            TemplateExercise("Kettlebell Swing",            4, 20,  30),
            TemplateExercise("Box Jump",                    3, 10,  30),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Plank",                       3, 60,  30)
        )),
        TemplateDay("Cardio Finisher", exercises = listOf(
            TemplateExercise("Jump Rope",                   5, 60,  30),
            TemplateExercise("Treadmill Run",               1,  1,   0),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Crunch",                      3, 20,  30),
            TemplateExercise("Side Plank",                  3, 30,  30)
        ))
    )),

    // ── STRENGTH ─────────────────────────────────────────────────────────────

    // StrongLifts 5×5 — linear progression, 3 days/week alternating A/B
    ProgramTemplate("5×5 Güç", listOf(
        TemplateDay("A — Squat, Bench & Barbell Row", exercises = listOf(
            TemplateExercise("Squat",                       5,  5, 180),
            TemplateExercise("Flat Bench Press",            5,  5, 180),
            TemplateExercise("Bent Over Barbell Row",       5,  5, 180)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("B — Squat, OHP & Deadlift", exercises = listOf(
            TemplateExercise("Squat",                       5,  5, 180),
            TemplateExercise("Barbell Shoulder Press",      5,  5, 180),
            TemplateExercise("Deadlift",                    1,  5, 300)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("A — Squat, Bench & Barbell Row (2)", exercises = listOf(
            TemplateExercise("Squat",                       5,  5, 180),
            TemplateExercise("Flat Bench Press",            5,  5, 180),
            TemplateExercise("Bent Over Barbell Row",       5,  5, 180)
        ))
    )),

    // Powerlifting peaking cycle — squat/bench/deadlift focus days + accessories
    ProgramTemplate("Powerlifting Temel", listOf(
        TemplateDay("Squat Focus", exercises = listOf(
            TemplateExercise("Squat",                       5,  5, 240),
            TemplateExercise("Front Squat",                 3,  3, 180),
            TemplateExercise("Romanian Deadlift",           4,  8, 120),
            TemplateExercise("Leg Press",                   3, 10,  90),
            TemplateExercise("Hyperextension",              3, 15,  60)
        )),
        TemplateDay("Bench Focus", exercises = listOf(
            TemplateExercise("Flat Bench Press",            5,  5, 240),
            TemplateExercise("Close Grip Bench Press",      4,  6, 180),
            TemplateExercise("Dumbbell Bench Press",        3, 10,  90),
            TemplateExercise("Triceps Pushdown",            3, 12,  60),
            TemplateExercise("Skull Crusher",               3, 10,  60)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Deadlift Focus", exercises = listOf(
            TemplateExercise("Deadlift",                    5,  3, 300),
            TemplateExercise("Sumo Deadlift",               3,  5, 180),
            TemplateExercise("Bent Over Barbell Row",       4,  8, 120),
            TemplateExercise("Lat Pulldown",                3, 10,  90),
            TemplateExercise("Hyperextension",              3, 15,  60)
        )),
        TemplateDay("Accessory & Strengthening", exercises = listOf(
            TemplateExercise("Barbell Shoulder Press",      4,  6, 120),
            TemplateExercise("Pull-Up",                     4,  8, 120),
            TemplateExercise("Barbell Biceps Curl",         3, 10,  60),
            TemplateExercise("Ab Wheel",                    3, 10,  60),
            TemplateExercise("Plank",                       3, 60,  60)
        ))
    )),

    // GZCLP by u/Sayings — T1: 5×3, T2: 3×10, T3: 2×15 tiered progression
    ProgramTemplate("GZCLP", listOf(
        TemplateDay("Day A — Squat T1 / Bench T2 / Row T3", exercises = listOf(
            TemplateExercise("Squat",                       5,  3, 180),
            TemplateExercise("Flat Bench Press",            3, 10, 120),
            TemplateExercise("Bent Over Barbell Row",       2, 15,  90)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Day B — Bench T1 / Squat T2 / Lat Pull T3", exercises = listOf(
            TemplateExercise("Flat Bench Press",            5,  3, 180),
            TemplateExercise("Squat",                       3, 10, 120),
            TemplateExercise("Lat Pulldown",                2, 15,  90)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Day C — Deadlift T1 / OHP T2 / Leg Press T3", exercises = listOf(
            TemplateExercise("Deadlift",                    5,  3, 300),
            TemplateExercise("Barbell Shoulder Press",      3, 10, 120),
            TemplateExercise("Leg Press",                   2, 15,  90)
        ))
    )),

    // ── ENDURANCE ────────────────────────────────────────────────────────────

    // Base aerobic conditioning — 4 days/week progressive run/row/bike protocol
    ProgramTemplate("Kardiyovasküler Temel", listOf(
        TemplateDay("Long Run", exercises = listOf(
            TemplateExercise("Treadmill Run",               1,  1,   0),
            TemplateExercise("Plank",                       3, 30,  60)
        )),
        TemplateDay("Interval Training", exercises = listOf(
            TemplateExercise("Jump Rope",                   8, 30,  30),
            TemplateExercise("Burpee",                      4, 15,  30),
            TemplateExercise("Mountain Climber",            4, 20,  30),
            TemplateExercise("Box Jump",                    3, 10,  45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Cycling & Rowing", exercises = listOf(
            TemplateExercise("Cycling",                     1,  1,   0),
            TemplateExercise("Rowing Machine",              1,  1,   0),
            TemplateExercise("Leg Raise",                   3, 15,  60)
        )),
        TemplateDay("Tempo Run", exercises = listOf(
            TemplateExercise("Treadmill Run",               1,  1,   0),
            TemplateExercise("Mountain Climber",            3, 20,  45),
            TemplateExercise("Plank",                       3, 45,  60)
        ))
    )),

    // Nick Bare-style hybrid — strength + endurance in the same week
    ProgramTemplate("Hybrid Athlete", listOf(
        TemplateDay("Strength Day", exercises = listOf(
            TemplateExercise("Squat",                       4,  5, 240),
            TemplateExercise("Flat Bench Press",            4,  5, 180),
            TemplateExercise("Deadlift",                    3,  3, 300),
            TemplateExercise("Barbell Shoulder Press",      3,  5, 150),
            TemplateExercise("Pull-Up",                     3,  6, 120)
        )),
        TemplateDay("MetCon", exercises = listOf(
            TemplateExercise("Burpee",                      4, 15,  30),
            TemplateExercise("Kettlebell Swing",            4, 20,  30),
            TemplateExercise("Box Jump",                    3, 10,  30),
            TemplateExercise("Jump Rope",                   3, 60,  30),
            TemplateExercise("Mountain Climber",            3, 20,  30)
        )),
        TemplateDay("Aerobic Capacity", exercises = listOf(
            TemplateExercise("Treadmill Run",               1,  1,   0),
            TemplateExercise("Cycling",                     1,  1,   0),
            TemplateExercise("Plank",                       3, 45,  60)
        )),
        TemplateDay("Strength + Speed", exercises = listOf(
            TemplateExercise("Clean and Press",             4,  6, 120),
            TemplateExercise("Jump Squat",                  4, 10,  30),
            TemplateExercise("Bent Over Barbell Row",       3,  8, 120),
            TemplateExercise("Barbell Biceps Curl",         3,  8,  60),
            TemplateExercise("Triceps Pushdown",            3, 10,  60)
        )),
        TemplateDay("Long MetCon", exercises = listOf(
            TemplateExercise("Rowing Machine",              1,  1,   0),
            TemplateExercise("Burpee",                      5, 15,  30),
            TemplateExercise("Kettlebell Swing",            4, 20,  30),
            TemplateExercise("Box Jump",                    3, 10,  45),
            TemplateExercise("Plank",                       3, 60,  45)
        ))
    )),

    ProgramTemplate("Bisiklet 4 Hafta", listOf(
        TemplateDay("Zone 2 Ride + Core", exercises = listOf(
            TemplateExercise("Cycling",                     1, 45, 60, targetDurationSeconds = 45 * 60, targetDistanceMeters = 14000f),
            TemplateExercise("Plank",                       3, 45, 45),
            TemplateExercise("Side Plank",                  3, 30, 30),
            TemplateExercise("Leg Raise",                   3, 12, 45)
        )),
        TemplateDay("Cyclist Strength", exercises = listOf(
            TemplateExercise("Squat",                       4,  6, 150),
            TemplateExercise("Romanian Deadlift",           3,  8, 120),
            TemplateExercise("Bulgarian Split Squat",       3, 10,  90),
            TemplateExercise("Standing Calf Raise",         4, 15,  60),
            TemplateExercise("Hyperextension",              3, 12,  60)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Tempo & Climb Ride", exercises = listOf(
            TemplateExercise("Cycling",                     1, 40, 60, targetDurationSeconds = 40 * 60, targetDistanceMeters = 16000f, targetElevationMeters = 160f, targetInclinePercent = 3f),
            TemplateExercise("Goblet Squat",                3, 12,  60),
            TemplateExercise("Lunge",                       3, 10,  60),
            TemplateExercise("Plank",                       3, 60,  45)
        )),
        TemplateDay("Bike Conditioning", exercises = listOf(
            TemplateExercise("Cycling",                     1, 30, 60, targetDurationSeconds = 30 * 60, targetDistanceMeters = 12000f, targetInclinePercent = 2f),
            TemplateExercise("Jump Squat",                  3, 10,  45),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Kettlebell Swing",            3, 20,  45)
        )),
        TemplateDay("Long Ride", exercises = listOf(
            TemplateExercise("Cycling",                     1, 75, 60, targetDurationSeconds = 75 * 60, targetDistanceMeters = 25000f, targetElevationMeters = 250f)
        ))
    )),

    ProgramTemplate("5K Koşu Temeli", listOf(
        TemplateDay("Easy Run + Stability", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 30, 60, targetDurationSeconds = 30 * 60, targetDistanceMeters = 3500f, targetInclinePercent = 1f),
            TemplateExercise("Plank",                       3, 45, 45),
            TemplateExercise("Single Leg Glute Bridge",     3, 12, 45),
            TemplateExercise("Standing Calf Raise",         3, 15, 45)
        )),
        TemplateDay("Runner Strength", exercises = listOf(
            TemplateExercise("Goblet Squat",                3, 10,  90),
            TemplateExercise("Romanian Deadlift",           3, 10,  90),
            TemplateExercise("Lunge",                       3, 12,  60),
            TemplateExercise("Bulgarian Split Squat",       3,  8,  75),
            TemplateExercise("Side Plank",                  3, 30,  45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Interval Run", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 28, 60, targetDurationSeconds = 28 * 60, targetDistanceMeters = 4200f, targetInclinePercent = 1f),
            TemplateExercise("Box Jump",                    3,  8, 60),
            TemplateExercise("Mountain Climber",            3, 20, 30)
        )),
        TemplateDay("Long Run + Mobility", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 45, 60, targetDurationSeconds = 45 * 60, targetDistanceMeters = 6000f),
            TemplateExercise("Yoga Flow",                   1, 15, 30, targetDurationSeconds = 15 * 60)
        ))
    )),

    ProgramTemplate("Yüzme Temel", listOf(
        TemplateDay("Technique Swim + Scapula", exercises = listOf(
            TemplateExercise("Swimming",                    1, 30, 60, targetDurationSeconds = 30 * 60, targetDistanceMeters = 800f),
            TemplateExercise("Lat Pulldown",                3, 12, 75),
            TemplateExercise("Rear Delt Fly",               3, 15, 45),
            TemplateExercise("Plank",                       3, 45, 45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Swimmer Strength", exercises = listOf(
            TemplateExercise("Seated Cable Row",            4, 10, 75),
            TemplateExercise("Dumbbell Shoulder Press",     3, 10, 75),
            TemplateExercise("Push-Up",                     3, 12, 60),
            TemplateExercise("Leg Raise",                   3, 15, 45),
            TemplateExercise("Hyperextension",              3, 12, 60)
        )),
        TemplateDay("Threshold Swim", exercises = listOf(
            TemplateExercise("Swimming",                    1, 35, 60, targetDurationSeconds = 35 * 60, targetDistanceMeters = 1200f),
            TemplateExercise("Russian Twist",               3, 20, 30),
            TemplateExercise("Side Plank",                  3, 30, 30)
        )),
        TemplateDay("Distance Swim", exercises = listOf(
            TemplateExercise("Swimming",                    1, 45, 60, targetDurationSeconds = 45 * 60, targetDistanceMeters = 1600f),
            TemplateExercise("Yoga Flow",                   1, 12, 30, targetDurationSeconds = 12 * 60)
        ))
    )),

    ProgramTemplate("Kürek Erg Temel", listOf(
        TemplateDay("Technique Erg + Core", exercises = listOf(
            TemplateExercise("Rowing Machine",              1, 25, 60, targetDurationSeconds = 25 * 60, targetDistanceMeters = 4500f),
            TemplateExercise("Plank",                       3, 45, 45),
            TemplateExercise("Russian Twist",               3, 20, 30),
            TemplateExercise("Hyperextension",              3, 12, 60)
        )),
        TemplateDay("Rowing Strength", exercises = listOf(
            TemplateExercise("Deadlift",                    4,  5, 180),
            TemplateExercise("Squat",                       3,  6, 150),
            TemplateExercise("Bent Over Barbell Row",       4,  8, 90),
            TemplateExercise("Lat Pulldown",                3, 10, 75),
            TemplateExercise("Leg Press",                   3, 12, 90)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Erg Intervals", exercises = listOf(
            TemplateExercise("Rowing Machine",              1, 24, 60, targetDurationSeconds = 24 * 60, targetDistanceMeters = 5000f),
            TemplateExercise("Kettlebell Swing",            3, 20, 45),
            TemplateExercise("Mountain Climber",            3, 20, 30)
        )),
        TemplateDay("Long Erg", exercises = listOf(
            TemplateExercise("Rowing Machine",              1, 40, 60, targetDurationSeconds = 40 * 60, targetDistanceMeters = 8000f),
            TemplateExercise("Yoga Flow",                   1, 12, 30, targetDurationSeconds = 12 * 60)
        ))
    )),

    ProgramTemplate("Yürüyüş & Hiking", listOf(
        TemplateDay("Easy Walk + Mobility", exercises = listOf(
            TemplateExercise("Outdoor Walk",                1, 35, 60, targetDurationSeconds = 35 * 60, targetDistanceMeters = 3000f),
            TemplateExercise("Yoga Flow",                   1, 12, 30, targetDurationSeconds = 12 * 60),
            TemplateExercise("Plank",                       3, 30, 45)
        )),
        TemplateDay("Hiking Strength", exercises = listOf(
            TemplateExercise("Goblet Squat",                3, 12, 75),
            TemplateExercise("Lunge",                       3, 12, 60),
            TemplateExercise("Romanian Deadlift",           3, 10, 90),
            TemplateExercise("Standing Calf Raise",         4, 15, 45),
            TemplateExercise("Hyperextension",              3, 12, 60)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Incline Walk", exercises = listOf(
            TemplateExercise("Outdoor Walk",                1, 45, 60, targetDurationSeconds = 45 * 60, targetDistanceMeters = 4000f, targetElevationMeters = 140f),
            TemplateExercise("Bulgarian Split Squat",       3, 10, 60),
            TemplateExercise("Side Plank",                  3, 30, 30)
        )),
        TemplateDay("Long Walk", exercises = listOf(
            TemplateExercise("Outdoor Walk",                1, 70, 60, targetDurationSeconds = 70 * 60, targetDistanceMeters = 6500f, targetElevationMeters = 180f)
        ))
    )),

    ProgramTemplate("Boks Kondisyon", listOf(
        TemplateDay("Skill Rounds + Rope", exercises = listOf(
            TemplateExercise("Shadow Boxing",               1, 24, 45, targetDurationSeconds = 24 * 60),
            TemplateExercise("Jump Rope",                   1, 12, 45, targetDurationSeconds = 12 * 60),
            TemplateExercise("Plank",                       3, 45, 45)
        )),
        TemplateDay("Boxing Strength", exercises = listOf(
            TemplateExercise("Dumbbell Shoulder Press",     3, 10, 75),
            TemplateExercise("Push-Up",                     4, 12, 45),
            TemplateExercise("Bent Over Barbell Row",       3, 10, 75),
            TemplateExercise("Russian Twist",               3, 20, 30),
            TemplateExercise("Leg Raise",                   3, 15, 45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Fight Conditioning", exercises = listOf(
            TemplateExercise("Shadow Boxing",               1, 30, 45, targetDurationSeconds = 30 * 60),
            TemplateExercise("Burpee",                      4, 10, 45),
            TemplateExercise("Mountain Climber",            4, 20, 30),
            TemplateExercise("Kettlebell Swing",            3, 20, 45)
        )),
        TemplateDay("Mixed Intervals", exercises = listOf(
            TemplateExercise("Jump Rope",                   1, 15, 45, targetDurationSeconds = 15 * 60),
            TemplateExercise("Shadow Boxing",               1, 18, 45, targetDurationSeconds = 18 * 60),
            TemplateExercise("Box Jump",                    3,  8, 60)
        ))
    )),

    ProgramTemplate("Yoga Mobilite", listOf(
        TemplateDay("Flow + Core", exercises = listOf(
            TemplateExercise("Yoga Flow",                   1, 35, 30, targetDurationSeconds = 35 * 60),
            TemplateExercise("Plank",                       3, 45, 45),
            TemplateExercise("Side Plank",                  3, 30, 30)
        )),
        TemplateDay("Posterior Chain Support", exercises = listOf(
            TemplateExercise("Romanian Deadlift",           3, 10, 90),
            TemplateExercise("Goblet Squat",                3, 12, 75),
            TemplateExercise("Hyperextension",              3, 12, 60),
            TemplateExercise("Leg Raise",                   3, 12, 45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Mobility + Breath", exercises = listOf(
            TemplateExercise("Yoga Flow",                   1, 30, 30, targetDurationSeconds = 30 * 60),
            TemplateExercise("Lunge",                       3, 10, 60),
            TemplateExercise("Side Plank",                  3, 30, 30)
        )),
        TemplateDay("Deep Stretch", exercises = listOf(
            TemplateExercise("Yoga Flow",                   1, 40, 30, targetDurationSeconds = 40 * 60)
        ))
    )),

    ProgramTemplate("Futbol Atletik", listOf(
        TemplateDay("Aerobic Base + Core", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 35, 60, targetDurationSeconds = 35 * 60, targetDistanceMeters = 4500f, targetInclinePercent = 1f),
            TemplateExercise("Plank",                       3, 45, 45),
            TemplateExercise("Russian Twist",               3, 20, 30)
        )),
        TemplateDay("Lower Strength", exercises = listOf(
            TemplateExercise("Squat",                       4,  6, 150),
            TemplateExercise("Romanian Deadlift",           3,  8, 120),
            TemplateExercise("Lunge",                       3, 12,  60),
            TemplateExercise("Standing Calf Raise",         4, 15,  45)
        )),
        TemplateDay("Speed Conditioning", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 24, 60, targetDurationSeconds = 24 * 60, targetDistanceMeters = 3600f, targetInclinePercent = 1f),
            TemplateExercise("Box Jump",                    3,  8, 60),
            TemplateExercise("Mountain Climber",            4, 20, 30),
            TemplateExercise("Burpee",                      3, 10, 45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Power + Mobility", exercises = listOf(
            TemplateExercise("Jump Squat",                  4, 10, 45),
            TemplateExercise("Bulgarian Split Squat",       3,  8, 75),
            TemplateExercise("Kettlebell Swing",            3, 20, 45),
            TemplateExercise("Yoga Flow",                   1, 12, 30, targetDurationSeconds = 12 * 60)
        ))
    )),

    ProgramTemplate("Basketbol Atletik", listOf(
        TemplateDay("Jump Mechanics", exercises = listOf(
            TemplateExercise("Box Jump",                    4,  8, 60),
            TemplateExercise("Jump Squat",                  4, 10, 45),
            TemplateExercise("Standing Calf Raise",         4, 15, 45),
            TemplateExercise("Plank",                       3, 45, 45)
        )),
        TemplateDay("Strength Base", exercises = listOf(
            TemplateExercise("Squat",                       4,  6, 150),
            TemplateExercise("Romanian Deadlift",           3,  8, 120),
            TemplateExercise("Dumbbell Shoulder Press",     3, 10, 75),
            TemplateExercise("Bent Over Barbell Row",       3, 10, 75)
        )),
        TemplateDay("Court Conditioning", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 25, 60, targetDurationSeconds = 25 * 60, targetDistanceMeters = 3500f),
            TemplateExercise("Jump Rope",                   1, 10, 45, targetDurationSeconds = 10 * 60),
            TemplateExercise("Mountain Climber",            4, 20, 30)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Unilateral + Core", exercises = listOf(
            TemplateExercise("Bulgarian Split Squat",       3, 10, 75),
            TemplateExercise("Lunge",                       3, 12, 60),
            TemplateExercise("Russian Twist",               3, 20, 30),
            TemplateExercise("Side Plank",                  3, 30, 30)
        ))
    )),

    ProgramTemplate("Tenis Atletik", listOf(
        TemplateDay("Footwork Conditioning", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 25, 60, targetDurationSeconds = 25 * 60, targetDistanceMeters = 3200f),
            TemplateExercise("Jump Rope",                   1, 12, 45, targetDurationSeconds = 12 * 60),
            TemplateExercise("Mountain Climber",            3, 20, 30)
        )),
        TemplateDay("Rotational Strength", exercises = listOf(
            TemplateExercise("Single Arm Dumbbell Row",     3, 10, 75),
            TemplateExercise("Dumbbell Shoulder Press",     3, 10, 75),
            TemplateExercise("Russian Twist",               4, 20, 30),
            TemplateExercise("Plank",                       3, 45, 45)
        )),
        TemplateDay("Lower Body Support", exercises = listOf(
            TemplateExercise("Goblet Squat",                3, 12, 75),
            TemplateExercise("Lunge",                       3, 12, 60),
            TemplateExercise("Romanian Deadlift",           3, 10, 90),
            TemplateExercise("Standing Calf Raise",         3, 15, 45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Match Conditioning", exercises = listOf(
            TemplateExercise("Treadmill Run",               1, 30, 60, targetDurationSeconds = 30 * 60, targetDistanceMeters = 4200f, targetInclinePercent = 1f),
            TemplateExercise("Box Jump",                    3,  8, 60),
            TemplateExercise("Yoga Flow",                   1, 10, 30, targetDurationSeconds = 10 * 60)
        ))
    )),

    // ── BEGINNER ─────────────────────────────────────────────────────────────

    // Starting Strength-inspired — 3 days/week, compound movements only
    ProgramTemplate("Full Body Başlangıç", listOf(
        TemplateDay("Day A — Fundamental Movements", exercises = listOf(
            TemplateExercise("Squat",                       3, 10, 120),
            TemplateExercise("Flat Bench Press",            3, 10, 120),
            TemplateExercise("Bent Over Barbell Row",       3, 10, 120),
            TemplateExercise("Barbell Shoulder Press",      2, 10,  90),
            TemplateExercise("Plank",                       2, 30,  60)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Day B — Fundamental Movements", exercises = listOf(
            TemplateExercise("Romanian Deadlift",           3, 10, 120),
            TemplateExercise("Lat Pulldown",                3, 10, 120),
            TemplateExercise("Dumbbell Bench Press",        3, 10,  90),
            TemplateExercise("Dumbbell Shoulder Press",     2, 10,  90),
            TemplateExercise("Crunch",                      2, 15,  60)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Day C — Full Body Circuit", exercises = listOf(
            TemplateExercise("Goblet Squat",                3, 12,  60),
            TemplateExercise("Push-Up",                     3, 12,  60),
            TemplateExercise("Single Arm Dumbbell Row",     3, 10,  60),
            TemplateExercise("Lunge",                       3, 10,  60),
            TemplateExercise("Mountain Climber",            3, 15,  45)
        ))
    )),

    // Bodyweight-only calisthenics — no equipment required, 3 days/week
    ProgramTemplate("Vücut Ağırlığı", listOf(
        TemplateDay("Upper Body — Push & Pull", exercises = listOf(
            TemplateExercise("Push-Up",                     4, 15,  60),
            TemplateExercise("Chest Dips",                  3, 12,  90),
            TemplateExercise("Diamond Push-Up",             3, 10,  60),
            TemplateExercise("Pull-Up",                     3,  6, 120),
            TemplateExercise("Plank",                       3, 60,  60)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Lower Body — Squat & Lunge", exercises = listOf(
            TemplateExercise("Goblet Squat",                4, 15,  60),
            TemplateExercise("Lunge",                       3, 12,  60),
            TemplateExercise("Bulgarian Split Squat",       3, 10,  60),
            TemplateExercise("Jump Squat",                  3, 12,  45),
            TemplateExercise("Single Leg Glute Bridge",     3, 12,  45)
        )),
        TemplateDay("REST", isRestDay = true),
        TemplateDay("Core & Cardio", exercises = listOf(
            TemplateExercise("Plank",                       3, 60,  30),
            TemplateExercise("Mountain Climber",            3, 20,  30),
            TemplateExercise("Crunch",                      3, 20,  30),
            TemplateExercise("Burpee",                      3, 12,  45),
            TemplateExercise("Leg Raise",                   3, 15,  30),
            TemplateExercise("Side Plank",                  3, 30,  30)
        ))
    ))
)

/** Template key'den ProgramTemplate bulur. */
fun findTemplate(key: String): ProgramTemplate? = PROGRAM_TEMPLATES.find { it.key == key }

/**
 * Bir günün egzersizlerine göre otomatik başlık üretir.
 * En çok tekrar eden 2 target_muscle grubunu alır.
 * ör. ["Göğüs", "Göğüs", "Triceps", "Triceps", "Omuz"] → "GÖĞÜS & TRİSEPS"
 */
fun autoTitle(targetMuscles: List<String>): String {
    if (targetMuscles.isEmpty()) return "ANTRENMAN"
    val topTwo = targetMuscles
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(2)
        .map { it.key.uppercase() }
    return topTwo.joinToString(" & ")
}
