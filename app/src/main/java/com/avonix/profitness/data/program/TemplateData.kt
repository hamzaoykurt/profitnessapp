package com.avonix.profitness.data.program

/** Tek bir template hareketini tanımlar. exerciseName, exercises tablosundaki `name` alanıyla eşleşir. */
data class TemplateExercise(
    val exerciseName: String,
    val sets: Int,
    val reps: Int,           // INT — zaman bazlı egzersizler için saniye cinsinden (ör. Plank = 60)
    val restSeconds: Int = 90
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

// ─── 16 Program Şablonu ────────────────────────────────────────────────────────

val PROGRAM_TEMPLATES: List<ProgramTemplate> = listOf(

    // ── KAS GELİŞİMİ ─────────────────────────────────────────────────────────

    ProgramTemplate("Push / Pull / Legs", listOf(
        TemplateDay("PUSH — Göğüs & Omuz & Triceps", exercises = listOf(
            TemplateExercise("Düz Bench Press", 4, 8),
            TemplateExercise("Eğimli Bench Press", 4, 10),
            TemplateExercise("Barbell Omuz Presi", 3, 10),
            TemplateExercise("Yana Yükselme", 3, 15, 60),
            TemplateExercise("Triceps Pushdown", 3, 12, 60),
            TemplateExercise("Skull Crusher", 3, 10)
        )),
        TemplateDay("PULL — Sırt & Biceps", exercises = listOf(
            TemplateExercise("Barfiks", 3, 8),
            TemplateExercise("Bent Over Barbell Row", 4, 8),
            TemplateExercise("Lat Pulldown", 4, 10),
            TemplateExercise("Barbell Biceps Curl", 4, 10),
            TemplateExercise("Çekiç Curl", 3, 12, 60)
        )),
        TemplateDay("LEGS — Bacak & Core", exercises = listOf(
            TemplateExercise("Squat", 4, 8),
            TemplateExercise("Romanian Deadlift", 4, 10),
            TemplateExercise("Leg Press", 3, 12),
            TemplateExercise("Leg Curl", 3, 12),
            TemplateExercise("Ayakta Calf Raise", 4, 15, 60),
            TemplateExercise("Plank", 3, 60, 60)
        )),
        TemplateDay("PUSH — Göğüs & Omuz & Triceps (2)", exercises = listOf(
            TemplateExercise("Dambıl Bench Press", 4, 10),
            TemplateExercise("Eğimli Dambıl Flye", 3, 12),
            TemplateExercise("Dambıl Omuz Presi", 4, 10),
            TemplateExercise("Ön Yükselme", 3, 12, 60),
            TemplateExercise("Overhead Triceps Extension", 3, 12, 60),
            TemplateExercise("Dips (Triceps)", 3, 12)
        )),
        TemplateDay("PULL — Sırt & Biceps (2)", exercises = listOf(
            TemplateExercise("Deadlift", 4, 5),
            TemplateExercise("Seated Cable Row", 3, 12),
            TemplateExercise("T-Bar Row", 3, 10),
            TemplateExercise("EZ Bar Curl", 4, 10),
            TemplateExercise("Konsantrasyon Curl", 3, 12, 60)
        )),
        TemplateDay("LEGS — Bacak & Core (2)", exercises = listOf(
            TemplateExercise("Hack Squat", 4, 10),
            TemplateExercise("Bulgarian Split Squat", 3, 10),
            TemplateExercise("Leg Extension", 3, 15),
            TemplateExercise("Oturarak Leg Curl", 3, 12),
            TemplateExercise("Ayakta Calf Raise", 3, 20, 60),
            TemplateExercise("Ab Wheel", 3, 10, 60)
        ))
    )),

    ProgramTemplate("Arnold Split", listOf(
        TemplateDay("Göğüs & Sırt", exercises = listOf(
            TemplateExercise("Düz Bench Press", 4, 8),
            TemplateExercise("Bent Over Barbell Row", 4, 8),
            TemplateExercise("Eğimli Bench Press", 3, 10),
            TemplateExercise("Lat Pulldown", 3, 10),
            TemplateExercise("Kablo Çapraz Geçişi", 3, 15, 60),
            TemplateExercise("Barfiks", 3, 8)
        )),
        TemplateDay("Omuz & Kol", exercises = listOf(
            TemplateExercise("Arnold Press", 4, 10),
            TemplateExercise("Barbell Biceps Curl", 4, 10),
            TemplateExercise("Skull Crusher", 4, 10),
            TemplateExercise("Yana Yükselme", 3, 15, 60),
            TemplateExercise("Çekiç Curl", 3, 12, 60),
            TemplateExercise("Triceps Pushdown", 3, 12, 60)
        )),
        TemplateDay("Bacak", exercises = listOf(
            TemplateExercise("Squat", 5, 8),
            TemplateExercise("Romanian Deadlift", 4, 8),
            TemplateExercise("Leg Press", 4, 12),
            TemplateExercise("Leg Curl", 3, 12),
            TemplateExercise("Ayakta Calf Raise", 4, 20, 60)
        )),
        TemplateDay("Göğüs & Sırt (2)", exercises = listOf(
            TemplateExercise("Dambıl Bench Press", 4, 10),
            TemplateExercise("T-Bar Row", 4, 10),
            TemplateExercise("Pec Deck Flye", 3, 15, 60),
            TemplateExercise("Geniş Tutuş Seated Row", 3, 12),
            TemplateExercise("Dips (Göğüs)", 3, 10)
        )),
        TemplateDay("Omuz & Kol (2)", exercises = listOf(
            TemplateExercise("Barbell Omuz Presi", 4, 8),
            TemplateExercise("EZ Bar Curl", 4, 10),
            TemplateExercise("Close Grip Bench Press", 4, 10),
            TemplateExercise("Ön Yükselme", 3, 12, 60),
            TemplateExercise("Preacher Curl", 3, 10, 60),
            TemplateExercise("Dips (Triceps)", 3, 12)
        )),
        TemplateDay("Bacak (2)", exercises = listOf(
            TemplateExercise("Hack Squat", 4, 10),
            TemplateExercise("Stiff Leg Deadlift", 4, 10),
            TemplateExercise("Leg Extension", 3, 15),
            TemplateExercise("Oturarak Leg Curl", 3, 12),
            TemplateExercise("Oturarak Calf Raise", 4, 20, 60),
            TemplateExercise("Plank", 3, 60, 60)
        ))
    )),

    ProgramTemplate("Upper / Lower Split", listOf(
        TemplateDay("Üst Vücut — Güç", exercises = listOf(
            TemplateExercise("Düz Bench Press", 4, 5),
            TemplateExercise("Bent Over Barbell Row", 4, 5),
            TemplateExercise("Barbell Omuz Presi", 3, 5),
            TemplateExercise("Barbell Biceps Curl", 3, 8),
            TemplateExercise("Close Grip Bench Press", 3, 8)
        )),
        TemplateDay("Alt Vücut — Güç", exercises = listOf(
            TemplateExercise("Squat", 4, 5),
            TemplateExercise("Romanian Deadlift", 4, 5),
            TemplateExercise("Ayakta Calf Raise", 4, 12),
            TemplateExercise("Ab Wheel", 3, 10, 60)
        )),
        TemplateDay("Üst Vücut — Hacim", exercises = listOf(
            TemplateExercise("Dambıl Bench Press", 4, 10),
            TemplateExercise("Lat Pulldown", 4, 10),
            TemplateExercise("Dambıl Omuz Presi", 3, 12),
            TemplateExercise("Yana Yükselme", 3, 15, 60),
            TemplateExercise("Çekiç Curl", 3, 12, 60),
            TemplateExercise("Triceps Pushdown", 3, 12, 60)
        )),
        TemplateDay("Alt Vücut — Hacim", exercises = listOf(
            TemplateExercise("Leg Press", 4, 12),
            TemplateExercise("Leg Curl", 4, 12),
            TemplateExercise("Leg Extension", 3, 15),
            TemplateExercise("Bulgarian Split Squat", 3, 10),
            TemplateExercise("Oturarak Calf Raise", 4, 20, 60)
        ))
    )),

    ProgramTemplate("Bro Split", listOf(
        TemplateDay("Göğüs", exercises = listOf(
            TemplateExercise("Düz Bench Press", 4, 8),
            TemplateExercise("Eğimli Bench Press", 4, 10),
            TemplateExercise("Dambıl Flat Flye", 3, 12),
            TemplateExercise("Kablo Çapraz Geçişi", 3, 15, 60),
            TemplateExercise("Dips (Göğüs)", 3, 10)
        )),
        TemplateDay("Sırt", exercises = listOf(
            TemplateExercise("Deadlift", 4, 5),
            TemplateExercise("Barfiks", 3, 8),
            TemplateExercise("Bent Over Barbell Row", 4, 10),
            TemplateExercise("Lat Pulldown", 3, 12),
            TemplateExercise("Seated Cable Row", 3, 12)
        )),
        TemplateDay("Omuz", exercises = listOf(
            TemplateExercise("Barbell Omuz Presi", 4, 8),
            TemplateExercise("Dambıl Omuz Presi", 3, 10),
            TemplateExercise("Yana Yükselme", 4, 15, 60),
            TemplateExercise("Ön Yükselme", 3, 12, 60),
            TemplateExercise("Arka Omuz Flye", 3, 15, 60)
        )),
        TemplateDay("Kol — Biceps & Triceps", exercises = listOf(
            TemplateExercise("Barbell Biceps Curl", 4, 10),
            TemplateExercise("Skull Crusher", 4, 10),
            TemplateExercise("Çekiç Curl", 3, 12, 60),
            TemplateExercise("Triceps Pushdown", 3, 12, 60),
            TemplateExercise("Preacher Curl", 3, 10),
            TemplateExercise("Overhead Triceps Extension", 3, 12)
        )),
        TemplateDay("Bacak & Core", exercises = listOf(
            TemplateExercise("Squat", 4, 8),
            TemplateExercise("Romanian Deadlift", 4, 10),
            TemplateExercise("Leg Press", 3, 12),
            TemplateExercise("Leg Curl", 3, 12),
            TemplateExercise("Ayakta Calf Raise", 4, 20, 60),
            TemplateExercise("Plank", 3, 60, 60)
        ))
    )),

    // ── YAĞ YAKIMI ────────────────────────────────────────────────────────────

    ProgramTemplate("HIIT Metabolik", listOf(
        TemplateDay("Üst Vücut HIIT", exercises = listOf(
            TemplateExercise("Şınav", 4, 15, 30),
            TemplateExercise("Burpee", 3, 15, 30),
            TemplateExercise("Dambıl Bench Press", 3, 12),
            TemplateExercise("Mountain Climber", 3, 20, 30),
            TemplateExercise("Dambıl Omuz Presi", 3, 12)
        )),
        TemplateDay("Alt Vücut Devre", exercises = listOf(
            TemplateExercise("Squat", 4, 15, 45),
            TemplateExercise("Atlamalı Squat", 3, 15, 30),
            TemplateExercise("Ateş Adımı (Lunge)", 3, 12, 30),
            TemplateExercise("Box Jump", 3, 10, 45),
            TemplateExercise("Burpee", 3, 12, 30)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Full Body HIIT", exercises = listOf(
            TemplateExercise("Burpee", 4, 15, 30),
            TemplateExercise("Kettlebell Swing", 3, 20, 30),
            TemplateExercise("Box Jump", 3, 10, 45),
            TemplateExercise("Mountain Climber", 3, 20, 30),
            TemplateExercise("Plank", 3, 60, 30)
        )),
        TemplateDay("Cardio & Core", exercises = listOf(
            TemplateExercise("Koşu Bandı Koşusu", 1, 1, 0),
            TemplateExercise("Crunch", 3, 20, 30),
            TemplateExercise("Leg Raise", 3, 15, 30),
            TemplateExercise("Russian Twist", 3, 20, 30),
            TemplateExercise("Plank", 3, 60, 45)
        ))
    )),

    ProgramTemplate("Fat Burn Full Body", listOf(
        TemplateDay("Full Body A", exercises = listOf(
            TemplateExercise("Squat", 3, 15, 60),
            TemplateExercise("Şınav", 3, 15, 60),
            TemplateExercise("Ateş Adımı (Lunge)", 3, 12, 60),
            TemplateExercise("Dambıl Tek Kol Rowing", 3, 12, 60),
            TemplateExercise("Plank", 3, 30, 30)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Full Body B", exercises = listOf(
            TemplateExercise("Romanian Deadlift", 3, 12, 60),
            TemplateExercise("Dambıl Bench Press", 3, 12, 60),
            TemplateExercise("Goblet Squat", 3, 15, 60),
            TemplateExercise("Lat Pulldown", 3, 12, 60),
            TemplateExercise("Mountain Climber", 3, 20, 30)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Full Body C", exercises = listOf(
            TemplateExercise("Burpee", 3, 12, 45),
            TemplateExercise("Kettlebell Swing", 3, 20, 45),
            TemplateExercise("Box Jump", 3, 10, 45),
            TemplateExercise("Şınav", 3, 15, 45),
            TemplateExercise("Ab Wheel", 3, 10, 60)
        ))
    )),

    ProgramTemplate("Metabolik Kondisyon", listOf(
        TemplateDay("Üst Push Devre", exercises = listOf(
            TemplateExercise("Şınav", 4, 15, 30),
            TemplateExercise("Dambıl Bench Press", 3, 12, 45),
            TemplateExercise("Dambıl Omuz Presi", 3, 12, 45),
            TemplateExercise("Burpee", 3, 10, 30),
            TemplateExercise("Mountain Climber", 3, 20, 30)
        )),
        TemplateDay("Bacak Metabolik", exercises = listOf(
            TemplateExercise("Atlamalı Squat", 4, 15, 30),
            TemplateExercise("Ateş Adımı (Lunge)", 3, 12, 30),
            TemplateExercise("Box Jump", 3, 10, 45),
            TemplateExercise("Goblet Squat", 3, 15, 45),
            TemplateExercise("Leg Press", 3, 15, 60)
        )),
        TemplateDay("Üst Pull Devre", exercises = listOf(
            TemplateExercise("Barfiks", 3, 8, 45),
            TemplateExercise("Lat Pulldown", 3, 12, 45),
            TemplateExercise("Dambıl Tek Kol Rowing", 3, 12, 45),
            TemplateExercise("Burpee", 3, 10, 30),
            TemplateExercise("Kettlebell Swing", 3, 20, 30)
        )),
        TemplateDay("Full Body EMOM", exercises = listOf(
            TemplateExercise("Burpee", 4, 12, 30),
            TemplateExercise("Kettlebell Swing", 4, 20, 30),
            TemplateExercise("Box Jump", 3, 10, 30),
            TemplateExercise("Plank", 3, 60, 30)
        )),
        TemplateDay("Cardio Finisher", exercises = listOf(
            TemplateExercise("Koşu Bandı Koşusu", 1, 1, 0),
            TemplateExercise("İp Atlama", 3, 1, 30),
            TemplateExercise("Mountain Climber", 3, 20, 30),
            TemplateExercise("Crunch", 3, 20, 30)
        ))
    )),

    // ── GÜÇ ──────────────────────────────────────────────────────────────────

    ProgramTemplate("5×5 Güç", listOf(
        TemplateDay("A — Squat & Bench & Row", exercises = listOf(
            TemplateExercise("Squat", 5, 5),
            TemplateExercise("Düz Bench Press", 5, 5),
            TemplateExercise("Bent Over Barbell Row", 5, 5)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("B — Squat & OHP & Deadlift", exercises = listOf(
            TemplateExercise("Squat", 5, 5),
            TemplateExercise("Barbell Omuz Presi", 5, 5),
            TemplateExercise("Deadlift", 1, 5)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("A — Squat & Bench & Row (2)", exercises = listOf(
            TemplateExercise("Squat", 5, 5),
            TemplateExercise("Düz Bench Press", 5, 5),
            TemplateExercise("Bent Over Barbell Row", 5, 5)
        ))
    )),

    ProgramTemplate("Powerlifting Temel", listOf(
        TemplateDay("Squat Odak", exercises = listOf(
            TemplateExercise("Squat", 5, 5),
            TemplateExercise("Ön Squat", 3, 3),
            TemplateExercise("Romanian Deadlift", 4, 8),
            TemplateExercise("Leg Press", 3, 10),
            TemplateExercise("Hyperextension", 3, 15, 60)
        )),
        TemplateDay("Bench Press Odak", exercises = listOf(
            TemplateExercise("Düz Bench Press", 5, 5),
            TemplateExercise("Close Grip Bench Press", 4, 6),
            TemplateExercise("Dambıl Bench Press", 3, 10),
            TemplateExercise("Triceps Pushdown", 3, 12, 60),
            TemplateExercise("Skull Crusher", 3, 10)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Deadlift Odak", exercises = listOf(
            TemplateExercise("Deadlift", 5, 3),
            TemplateExercise("Sumo Deadlift", 3, 5),
            TemplateExercise("Bent Over Barbell Row", 4, 8),
            TemplateExercise("Lat Pulldown", 3, 10),
            TemplateExercise("Hyperextension", 3, 15, 60)
        )),
        TemplateDay("Aksesuar & Güçlendirme", exercises = listOf(
            TemplateExercise("Barbell Omuz Presi", 4, 6),
            TemplateExercise("Barfiks", 4, 8),
            TemplateExercise("Barbell Biceps Curl", 3, 10),
            TemplateExercise("Plank", 3, 60, 60)
        ))
    )),

    ProgramTemplate("GZCLP", listOf(
        TemplateDay("Gün A — Squat T1 + Bench T2 + Row T3", exercises = listOf(
            TemplateExercise("Squat", 5, 3),
            TemplateExercise("Düz Bench Press", 3, 10),
            TemplateExercise("Bent Over Barbell Row", 3, 15, 60)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Gün B — Bench T1 + Squat T2 + Row T3", exercises = listOf(
            TemplateExercise("Düz Bench Press", 5, 3),
            TemplateExercise("Squat", 3, 10),
            TemplateExercise("Lat Pulldown", 3, 15, 60)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Gün C — Deadlift T1 + OHP T2 + Leg T3", exercises = listOf(
            TemplateExercise("Deadlift", 5, 3),
            TemplateExercise("Barbell Omuz Presi", 3, 10),
            TemplateExercise("Leg Press", 3, 15, 60)
        ))
    )),

    // ── DAYANIKLILIK ──────────────────────────────────────────────────────────

    ProgramTemplate("Kardiyovasküler Temel", listOf(
        TemplateDay("Uzun Mesafe Koşu", exercises = listOf(
            TemplateExercise("Koşu Bandı Koşusu", 1, 1, 0),
            TemplateExercise("Plank", 3, 30, 60)
        )),
        TemplateDay("Interval Antrenmanı", exercises = listOf(
            TemplateExercise("İp Atlama", 5, 1, 30),
            TemplateExercise("Burpee", 3, 15, 30),
            TemplateExercise("Mountain Climber", 3, 20, 30)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Bisiklet / Kürek", exercises = listOf(
            TemplateExercise("Bisiklet", 1, 1, 0),
            TemplateExercise("Kürek Makinesi", 1, 1, 0),
            TemplateExercise("Leg Raise", 3, 15, 60)
        )),
        TemplateDay("Tempo Koşu", exercises = listOf(
            TemplateExercise("Koşu Bandı Koşusu", 1, 1, 0),
            TemplateExercise("Mountain Climber", 3, 20, 45)
        ))
    )),

    ProgramTemplate("Hybrid Athlete", listOf(
        TemplateDay("Güç Antrenmanı", exercises = listOf(
            TemplateExercise("Squat", 4, 5),
            TemplateExercise("Düz Bench Press", 4, 5),
            TemplateExercise("Deadlift", 3, 3),
            TemplateExercise("Barbell Omuz Presi", 3, 5)
        )),
        TemplateDay("MetCon", exercises = listOf(
            TemplateExercise("Burpee", 4, 15, 30),
            TemplateExercise("Kettlebell Swing", 4, 20, 30),
            TemplateExercise("Box Jump", 3, 10, 30),
            TemplateExercise("İp Atlama", 3, 1, 30)
        )),
        TemplateDay("Aerobik Kapasite", exercises = listOf(
            TemplateExercise("Koşu Bandı Koşusu", 1, 1, 0),
            TemplateExercise("Bisiklet", 1, 1, 0)
        )),
        TemplateDay("Güç + Hız", exercises = listOf(
            TemplateExercise("Clean and Press", 4, 6),
            TemplateExercise("Atlamalı Squat", 3, 15, 30),
            TemplateExercise("Barbell Biceps Curl", 3, 8),
            TemplateExercise("Triceps Pushdown", 3, 10, 60)
        )),
        TemplateDay("Uzun MetCon", exercises = listOf(
            TemplateExercise("Kürek Makinesi", 1, 1, 0),
            TemplateExercise("Burpee", 5, 15, 30),
            TemplateExercise("Kettlebell Swing", 4, 20, 30),
            TemplateExercise("Plank", 3, 60, 45)
        ))
    )),

    // ── BAŞLANGIÇ ────────────────────────────────────────────────────────────

    ProgramTemplate("Full Body Başlangıç", listOf(
        TemplateDay("Gün A — Temel Hareketler", exercises = listOf(
            TemplateExercise("Squat", 3, 10),
            TemplateExercise("Düz Bench Press", 3, 10),
            TemplateExercise("Bent Over Barbell Row", 3, 10),
            TemplateExercise("Plank", 2, 30, 60)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Gün B — Temel Hareketler", exercises = listOf(
            TemplateExercise("Romanian Deadlift", 3, 10),
            TemplateExercise("Barbell Omuz Presi", 3, 10),
            TemplateExercise("Lat Pulldown", 3, 10),
            TemplateExercise("Crunch", 2, 15, 60)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Gün C — Full Body Devre", exercises = listOf(
            TemplateExercise("Goblet Squat", 3, 12, 60),
            TemplateExercise("Şınav", 3, 12, 60),
            TemplateExercise("Dambıl Tek Kol Rowing", 3, 10, 60),
            TemplateExercise("Ateş Adımı (Lunge)", 3, 10, 60),
            TemplateExercise("Mountain Climber", 3, 15, 45)
        ))
    )),

    ProgramTemplate("Vücut Ağırlığı", listOf(
        TemplateDay("Üst Vücut — Şınav & Dips", exercises = listOf(
            TemplateExercise("Şınav", 4, 15),
            TemplateExercise("Dips (Triceps)", 3, 12),
            TemplateExercise("Diamond Push-Up", 3, 10),
            TemplateExercise("Barfiks", 3, 6)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Alt Vücut — Squat & Lunge", exercises = listOf(
            TemplateExercise("Goblet Squat", 4, 15, 60),
            TemplateExercise("Ateş Adımı (Lunge)", 3, 12, 60),
            TemplateExercise("Bulgarian Split Squat", 3, 10, 60),
            TemplateExercise("Atlamalı Squat", 3, 12, 45)
        )),
        TemplateDay("DİNLENME", isRestDay = true),
        TemplateDay("Core & Cardio", exercises = listOf(
            TemplateExercise("Plank", 3, 60, 30),
            TemplateExercise("Mountain Climber", 3, 20, 30),
            TemplateExercise("Crunch", 3, 20, 30),
            TemplateExercise("Burpee", 3, 12, 45),
            TemplateExercise("Leg Raise", 3, 15, 30)
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
