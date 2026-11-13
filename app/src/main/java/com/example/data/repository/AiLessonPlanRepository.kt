package com.example.data.repository

import android.util.Log
import com.example.domain.model.AppLanguage
import com.example.domain.model.LessonPlan
import com.example.domain.model.NepStage
import com.example.domain.model.PanchapadiConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiLessonPlanRepository {

    private val lessonPlanSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Clear lesson title in target language"),
            "learningObjectives" to Schema.string(description = "3 to 4 bulleted NEP 2020 competency learning objectives"),
            "tlmMaterials" to Schema.string(description = "Required teaching-learning materials including low-cost local Odisha materials"),
            "teacherActivities" to Schema.string(description = "Overall teacher facilitation activities"),
            "studentActivities" to Schema.string(description = "Overall active student participation activities"),
            "aditiStep" to Schema.string(description = "Step 1 Aditi: introductory activity and connection with prior knowledge"),
            "aditiMinutes" to Schema.integer(description = "Suggested minutes for Aditi"),
            "bodhaStep" to Schema.string(description = "Step 2 Bodha: concept explanation and development of understanding"),
            "bodhaMinutes" to Schema.integer(description = "Suggested minutes for Bodha"),
            "abhyasaStep" to Schema.string(description = "Step 3 Abhyasa: guided practice, exercises, and participation"),
            "abhyasaMinutes" to Schema.integer(description = "Suggested minutes for Abhyasa"),
            "prayogaStep" to Schema.string(description = "Step 4 Prayoga: real-life application in Odisha local context"),
            "prayogaMinutes" to Schema.integer(description = "Suggested minutes for Prayoga"),
            "prasaraStep" to Schema.string(description = "Step 5 Prasara: extension, revision, and sharing"),
            "prasaraMinutes" to Schema.integer(description = "Suggested minutes for Prasara"),
            "competencyQuestions" to Schema.string(description = "Competency-based questions and classroom activities"),
            "assessmentCriteria" to Schema.string(description = "Formative assessment questions and evaluation criteria"),
            "homeworkActivities" to Schema.string(description = "Homework plus remedial and enrichment activities"),
            "inclusiveSuggestions" to Schema.string(description = "Inclusive teaching suggestions for diverse learning needs and CWSN"),
            "summary" to Schema.string(description = "Short concise lesson summary"),
            "teacherReflection" to Schema.string(description = "Prompt questions for post-lesson teacher reflection")
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = lessonPlanSchema
            }
        )
    }

    private val textModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    suspend fun generateFullLessonPlan(
        userId: String,
        authorName: String,
        stage: NepStage,
        classLevel: String,
        subject: String,
        medium: String,
        topic: String,
        customOutcomes: String,
        durationMinutes: Int,
        planDay: Int,
        planMonth: Int,
        planYear: Int,
        panchapadiConfig: PanchapadiConfig,
        outputLanguage: AppLanguage
    ): Result<LessonPlan> {
        val targetLangInstruction = if (outputLanguage == AppLanguage.ODIA) {
            "Write all generated lesson plan text primarily in clear, natural, age-appropriate Odia Unicode script (ଓଡ଼ିଆ), keeping technical terms in brackets where helpful."
        } else {
            "Write all generated lesson plan text in clear, natural English suitable for Odisha teachers, including Odia Panchapadi step names."
        }

        val prompt = """
            You are an expert Odisha SCERT & NEP 2020 curriculum pedagogy specialist.
            Create a complete, practical Panchapadi lesson plan aligned with India's NEP 2020 5+3+3+4 framework and Odisha school curriculum.

            Parameters:
            - NEP 2020 Stage: ${stage.odiaName} (${stage.ageRangeEn})
            - Class: $classLevel
            - Subject: $subject
            - Medium of Instruction: $medium
            - Topic (may be in Odia or English): $topic
            - Teacher Desired Learning Outcomes: ${customOutcomes.ifBlank { "Age-appropriate SCERT Odisha competencies" }}
            - Total Lesson Duration: $durationMinutes minutes
            - Scheduled Date (Day/Month/Year): $planDay/$planMonth/$planYear

            Panchapadi Step Definitions configured by the institution:
            1. ${panchapadiConfig.aditiLabel}: ${panchapadiConfig.aditiDesc}
            2. ${panchapadiConfig.bodhaLabel}: ${panchapadiConfig.bodhaDesc}
            3. ${panchapadiConfig.abhyasaLabel}: ${panchapadiConfig.abhyasaDesc}
            4. ${panchapadiConfig.prayogaLabel}: ${panchapadiConfig.prayogaDesc}
            5. ${panchapadiConfig.prasaraLabel}: ${panchapadiConfig.prasaraDesc}

            $targetLangInstruction
            Ensure the sum of the 5 step minutes approximately equals $durationMinutes minutes.
            Make sure every field is rich, classroom-ready, and culturally grounded in Odisha's context.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val jsonStr = response.text ?: throw IllegalStateException("AI model returned an empty response.")
            val json = JSONObject(jsonStr)

            Result.success(
                LessonPlan(
                    userId = userId,
                    authorName = authorName,
                    title = json.optString("title", topic).take(200),
                    stage = stage.key,
                    classLevel = classLevel,
                    subject = subject,
                    medium = medium,
                    topic = topic.take(200),
                    durationMinutes = durationMinutes.toLong(),
                    planDay = planDay.toLong(),
                    planMonth = planMonth.toLong(),
                    planYear = planYear.toLong(),
                    learningObjectives = json.optString("learningObjectives"),
                    tlmMaterials = json.optString("tlmMaterials"),
                    teacherActivities = json.optString("teacherActivities"),
                    studentActivities = json.optString("studentActivities"),
                    aditiStep = json.optString("aditiStep"),
                    aditiMinutes = json.optLong("aditiMinutes", 7L).coerceIn(1L, 120L),
                    bodhaStep = json.optString("bodhaStep"),
                    bodhaMinutes = json.optLong("bodhaMinutes", 15L).coerceIn(1L, 120L),
                    abhyasaStep = json.optString("abhyasaStep"),
                    abhyasaMinutes = json.optLong("abhyasaMinutes", 10L).coerceIn(1L, 120L),
                    prayogaStep = json.optString("prayogaStep"),
                    prayogaMinutes = json.optLong("prayogaMinutes", 8L).coerceIn(1L, 120L),
                    prasaraStep = json.optString("prasaraStep"),
                    prasaraMinutes = json.optLong("prasaraMinutes", 5L).coerceIn(1L, 120L),
                    competencyQuestions = json.optString("competencyQuestions"),
                    assessmentCriteria = json.optString("assessmentCriteria"),
                    homeworkActivities = json.optString("homeworkActivities"),
                    inclusiveSuggestions = json.optString("inclusiveSuggestions"),
                    summary = json.optString("summary"),
                    teacherReflection = json.optString("teacherReflection"),
                    isAiGenerated = true,
                    isVerified = false,
                    status = "draft"
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Lesson plan generation timed out", e)
            Result.failure(IllegalStateException("AI generation timed out after 60s. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Lesson plan generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun regenerateSection(
        plan: LessonPlan,
        sectionTitle: String,
        currentContent: String,
        outputLanguage: AppLanguage
    ): Result<String> {
        val langHint = if (outputLanguage == AppLanguage.ODIA) {
            "Respond ONLY in clear, natural Odia Unicode (ଓଡ଼ିଆ)."
        } else {
            "Respond ONLY in clear, natural English."
        }
        val prompt = """
            Improve and regenerate ONLY the section "$sectionTitle" for this Odisha NEP 2020 Panchapadi lesson plan:
            - Class: ${plan.classLevel}, Subject: ${plan.subject}, Topic: ${plan.topic}
            - Existing section draft: $currentContent

            Provide fresh, creative, classroom-ready activities/questions in plain text (no markdown headers or code blocks).
            $langHint
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                textModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Model returned empty section text."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Section regeneration timed out", e)
            Result.failure(IllegalStateException("Section regeneration timed out. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Section regeneration failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiLessonPlanRepo"
    }
}
