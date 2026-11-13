package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

enum class OutcomeStatus {
    NOT_STARTED,
    IN_PROGRESS,
    ACHIEVED
}

data class NepGuidelineSpec(
    val id: String,
    val stage: NepStage,
    val code: String,
    val titleOdia: String,
    val titleEn: String,
    val descOdia: String,
    val descEn: String
)

object Nep2020Guidelines {
    val allGuidelines: List<NepGuidelineSpec> = listOf(
        // Foundational (Ages 3-8)
        NepGuidelineSpec(
            id = "f1_play_toy",
            stage = NepStage.FOUNDATIONAL,
            code = "NEP-FS-1",
            titleOdia = "ଖେଳ ଓ ଖେଳନା-ଆଧାରିତ ଶିକ୍ଷଣ",
            titleEn = "Play, Toy & Activity-Based Pedagogy",
            descOdia = "ଗୀତ, ଗପ, ଚିତ୍ର ଓ ସ୍ଥାନୀୟ ଖେଳନା ମାଧ୍ୟମରେ ଆନନ୍ଦଦାୟକ ଶିକ୍ଷଣ।",
            descEn = "Joyful learning through songs, stories, picture cards, and local manipulative toys."
        ),
        NepGuidelineSpec(
            id = "f2_nipun_fln",
            stage = NepStage.FOUNDATIONAL,
            code = "NEP-FS-2",
            titleOdia = "NIPUN ଭାରତ FLN (ସାକ୍ଷରତା ଓ ସଂଖ୍ୟାଜ୍ଞାନ)",
            titleEn = "NIPUN Bharat Foundational Literacy & Numeracy",
            descOdia = "ଶବ୍ଦ ଚିହ୍ନିବା, ପଠନ ଅଭ୍ୟାସ ଓ ମୌଳିକ ଗଣନା ଦକ୍ଷତା ବିକାଶ।",
            descEn = "Building phonological awareness, decoding, reading comprehension, and number sense."
        ),
        NepGuidelineSpec(
            id = "f3_mother_tongue",
            stage = NepStage.FOUNDATIONAL,
            code = "NEP-FS-3",
            titleOdia = "ମାତୃଭାଷା / ଘରୋଇ ଭାଷା ସଂଯୋଗ",
            titleEn = "Mother Tongue / Home Language Bridge",
            descOdia = "ଓଡ଼ିଆ ଓ ଆଞ୍ଚଳିକ ଭାଷାରେ ସହଜ ବାର୍ତ୍ତାଳାପ ଓ ଭାବ ବିନିମୟ।",
            descEn = "Instruction anchored in Odia and children's home language for strong conceptual foundation."
        ),
        // Preparatory (Classes 3-5)
        NepGuidelineSpec(
            id = "p1_discovery",
            stage = NepStage.PREPARATORY,
            code = "NEP-PR-1",
            titleOdia = "ଅନୁସନ୍ଧାନ ଓ କାର୍ଯ୍ୟକଳାପ-ଆଧାରିତ ଶିକ୍ଷଣ",
            titleEn = "Discovery & Activity-Based Learning",
            descOdia = "ପର୍ଯ୍ୟବେକ୍ଷଣ, ପରୀକ୍ଷଣ ଓ ପ୍ରଶ୍ନ ପଚାରିବା ପାଇଁ ଛାତ୍ରଛାତ୍ରୀଙ୍କୁ ପ୍ରୋତ୍ସାହନ।",
            descEn = "Encouraging observation, hands-on exploration, and active inquiry over rote learning."
        ),
        NepGuidelineSpec(
            id = "p2_multilingual",
            stage = NepStage.PREPARATORY,
            code = "NEP-PR-2",
            titleOdia = "ବହୁଭାଷୀ ପଠନ ଓ ଅଭିବ୍ୟକ୍ତି",
            titleEn = "Multilingual Reading Fluency & Expression",
            descOdia = "ଓଡ଼ିଆ ସହ ଅନ୍ୟ ଭାଷାଗୁଡ଼ିକର ଶବ୍ଦଭଣ୍ଡାର ଓ ବାକ୍ୟ ଗଠନ ଅଭ୍ୟାସ।",
            descEn = "Strengthening reading fluency, oral expression, and multilingual vocabulary bridges."
        ),
        NepGuidelineSpec(
            id = "p3_formative",
            stage = NepStage.PREPARATORY,
            code = "NEP-PR-3",
            titleOdia = "ନିରନ୍ତର ଓ ଗଠନମୂଳକ ମୂଲ୍ୟାଙ୍କନ",
            titleEn = "Continuous Formative Assessment",
            descOdia = "କାର୍ଯ୍ୟପତ୍ର, ମୌଖିକ ପ୍ରଶ୍ନ ଓ ପୋର୍ଟଫୋଲିଓ ଦ୍ୱାରା ପ୍ରଗତି ମୂଲ୍ୟାଙ୍କନ।",
            descEn = "Tracking understanding through low-stakes worksheets, oral checks, and portfolios."
        ),
        // Middle (Classes 6-8)
        NepGuidelineSpec(
            id = "m1_experiential",
            stage = NepStage.MIDDLE,
            code = "NEP-MD-1",
            titleOdia = "ଅଭିଜ୍ଞତା-ଆଧାରିତ ଓ ସମନ୍ୱିତ ଶିକ୍ଷଣ",
            titleEn = "Experiential & Cross-Curricular Inquiry",
            descOdia = "ବିଜ୍ଞାନ, ଗଣିତ, ସମାଜ ଓ କଳା ମଧ୍ୟରେ ସମ୍ପର୍କ ସ୍ଥାପନ କରି ଶିକ୍ଷାଦାନ।",
            descEn = "Connecting science, math, social studies, and arts through real experiments and projects."
        ),
        NepGuidelineSpec(
            id = "m2_vocational_art",
            stage = NepStage.MIDDLE,
            code = "NEP-MD-2",
            titleOdia = "କୌଶଳ, କଳା ଓ ବ୍ୟାଗଲେସ୍ ଦିବସ ସଂଯୋଗ",
            titleEn = "Vocational Craft, Art & Bagless Exposure",
            descOdia = "ସ୍ଥାନୀୟ ହସ୍ତଶିଳ୍ପ, ପ୍ରକଳ୍ପ ଓ ବ୍ୟାବହାରିକ ଦକ୍ଷତା ସହିତ ପାଠକୁ ଯୋଡ଼ିବା।",
            descEn = "Integrating local Odisha crafts, hands-on vocational skills, and art-integrated tasks."
        ),
        NepGuidelineSpec(
            id = "m3_competency_hots",
            stage = NepStage.MIDDLE,
            code = "NEP-MD-3",
            titleOdia = "ଦକ୍ଷତା-ଆଧାରିତ ଓ ବିଶ୍ଳେଷଣାତ୍ମକ ପ୍ରଶ୍ନ",
            titleEn = "Competency-Based Analytical Questions",
            descOdia = "ତର୍କ, ସମସ୍ୟା ସମାଧାନ ଓ ଧାରଣାଗତ ସ୍ପଷ୍ଟତା ଯାଞ୍ଚ କରୁଥିବା ପ୍ରଶ୍ନ।",
            descEn = "Assessing conceptual mastery, reasoning, and application rather than memorization."
        ),
        // Secondary (Classes 9-12)
        NepGuidelineSpec(
            id = "s1_critical_thinking",
            stage = NepStage.SECONDARY,
            code = "NEP-SC-1",
            titleOdia = "ସମାଲୋଚନାତ୍ମକ ଚିନ୍ତନ ଓ ଗଭୀର ବିଶ୍ଳେଷଣ",
            titleEn = "Critical Thinking & Multidisciplinary Depth",
            descOdia = "ମୌଳିକ ବିଶ୍ଳେଷଣ, ତଥ୍ୟ-ଆଧାରିତ ଯୁକ୍ତି ଓ ବହୁବିଷୟକ ଦୃଷ୍ଟିକୋଣ।",
            descEn = "Promoting deeper inquiry, evidence-based argumentation, and cross-discipline links."
        ),
        NepGuidelineSpec(
            id = "s2_real_world_project",
            stage = NepStage.SECONDARY,
            code = "NEP-SC-2",
            titleOdia = "ବାସ୍ତବ ସମସ୍ୟା ସମାଧାନ ଓ ଗବେଷଣା ପ୍ରକଳ୍ପ",
            titleEn = "Real-World Problem Solving & Projects",
            descOdia = "ପରିବେଶ, ସମାଜ ଓ କ୍ୟାରିଅର୍ ସହ ଜଡ଼ିତ ବ୍ୟାବହାରିକ ପ୍ରୟୋଗ।",
            descEn = "Applying knowledge to community, environmental, and career-oriented challenges."
        ),
        NepGuidelineSpec(
            id = "s3_inclusive_scaffold",
            stage = NepStage.SECONDARY,
            code = "NEP-SC-3",
            titleOdia = "ସମାବେଶୀ ଶିକ୍ଷଣ ଓ ଭିନ୍ନ ଦକ୍ଷତା ସହାୟତା",
            titleEn = "Inclusive Scaffolding & Differentiated Support",
            descOdia = "CWSN, ଧୀରେ ଶିଖୁଥିବା ଓ ଉନ୍ନତ ଶିକ୍ଷାର୍ଥୀଙ୍କ ପାଇଁ ଉପଯୁକ୍ତ ସହାୟତା।",
            descEn = "Providing targeted remedial scaffolds and enrichment for diverse learner needs."
        )
    )

    fun guidelinesForStage(stageKey: String): List<NepGuidelineSpec> {
        val stage = NepStage.fromKey(stageKey)
        return allGuidelines.filter { it.stage == stage }
    }
}

@IgnoreExtraProperties
data class LessonPlanProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lessonPlanId: String = "",
    val stage: String = NepStage.PREPARATORY.key,
    val aditiCompleted: Boolean = false,
    val bodhaCompleted: Boolean = false,
    val abhyasaCompleted: Boolean = false,
    val prayogaCompleted: Boolean = false,
    val prasaraCompleted: Boolean = false,
    val checkedGuidelineIds: List<String> = emptyList(),
    val achievedOutcomeIndices: List<Long> = emptyList(),
    val inProgressOutcomeIndices: List<Long> = emptyList(),
    val inclusiveSupportApplied: Boolean = false,
    val tlmUsedInClass: Boolean = false,
    val assessmentConducted: Boolean = false,
    val classroomNotes: String = "",
    val lastTrackedDay: Long = 12L,
    val lastTrackedMonth: Long = 11L,
    val lastTrackedYear: Long = 2026L,
    val completionPercent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LessonPlanReadiness(
    val readinessPercent: Int,
    val panchapadiReadyCount: Int,
    val missingItemsOdia: List<String>,
    val missingItemsEn: List<String>
)

data class NepProgressSummary(
    val totalPlans: Int,
    val trackedPlansCount: Int,
    val fullyCompletedPlansCount: Int,
    val averageExecutionPercent: Int,
    val averageReadinessPercent: Int,
    val totalOutcomes: Int,
    val achievedOutcomes: Int,
    val inProgressOutcomes: Int,
    val aditiRatePercent: Int,
    val bodhaRatePercent: Int,
    val abhyasaRatePercent: Int,
    val prayogaRatePercent: Int,
    val prasaraRatePercent: Int,
    val stageBreakdown: Map<NepStage, Int>
)

object NepProgressEvaluator {

    /**
     * Parses individual learning outcomes from a LessonPlan's learningObjectives text.
     */
    fun extractOutcomes(plan: LessonPlan): List<String> {
        val raw = plan.learningObjectives.trim()
        if (raw.isBlank()) {
            val fallback = plan.topic.ifBlank { plan.title }.ifBlank { "Core Lesson Outcome" }
            return listOf(fallback)
        }
        val lines = raw.lines()
            .map { line ->
                line.trim()
                    .replace(Regex("^[•\\-*]+\\s*"), "")
                    .replace(Regex("^[0-9୧୨୩୪୫୬୭୮୯୦]+[.)]\\s*"), "")
                    .trim()
            }
            .filter { it.length >= 3 }
            .distinct()
            .take(25)
        return if (lines.isEmpty()) listOf(raw.take(200)) else lines
    }

    /**
     * Evaluates whether the LessonPlan itself contains the required NEP 2020 & Panchapadi components.
     */
    fun evaluatePlanReadiness(plan: LessonPlan): LessonPlanReadiness {
        val missingOdia = mutableListOf<String>()
        val missingEn = mutableListOf<String>()

        var score = 0
        val totalChecks = 10

        var panchapadiReady = 0
        if (plan.aditiStep.trim().length >= 8) { score++; panchapadiReady++ } else {
            missingOdia.add("ଆଦିତି (Aditi) ପଦକ୍ଷେପ"); missingEn.add("Aditi step content")
        }
        if (plan.bodhaStep.trim().length >= 8) { score++; panchapadiReady++ } else {
            missingOdia.add("ବୋଧ (Bodha) ପଦକ୍ଷେପ"); missingEn.add("Bodha step content")
        }
        if (plan.abhyasaStep.trim().length >= 8) { score++; panchapadiReady++ } else {
            missingOdia.add("ଅଭ୍ୟାସ (Abhyasa) ପଦକ୍ଷେପ"); missingEn.add("Abhyasa step content")
        }
        if (plan.prayogaStep.trim().length >= 8) { score++; panchapadiReady++ } else {
            missingOdia.add("ପ୍ରୟୋଗ (Prayoga) ପଦକ୍ଷେପ"); missingEn.add("Prayoga step content")
        }
        if (plan.prasaraStep.trim().length >= 8) { score++; panchapadiReady++ } else {
            missingOdia.add("ପ୍ରସାର (Prasara) ପଦକ୍ଷେପ"); missingEn.add("Prasara step content")
        }

        if (plan.learningObjectives.trim().length >= 8) score++ else {
            missingOdia.add("ଶିକ୍ଷଣ ଉଦ୍ଦେଶ୍ୟ (Learning Outcomes)"); missingEn.add("Learning objectives")
        }
        if (plan.tlmMaterials.trim().length >= 5) score++ else {
            missingOdia.add("ଶିକ୍ଷଣ ସାମଗ୍ରୀ (TLM)"); missingEn.add("Teaching-learning materials (TLM)")
        }
        if (plan.competencyQuestions.trim().length >= 8) score++ else {
            missingOdia.add("ଦକ୍ଷତା-ଆଧାରିତ ପ୍ରଶ୍ନ"); missingEn.add("Competency-based questions")
        }
        if (plan.assessmentCriteria.trim().length >= 5) score++ else {
            missingOdia.add("ମୂଲ୍ୟାଙ୍କନ ମାନଦଣ୍ଡ"); missingEn.add("Assessment criteria")
        }
        if (plan.inclusiveSuggestions.trim().length >= 5) score++ else {
            missingOdia.add("ସମାବେଶୀ ଶିକ୍ଷଣ ପରାମର୍ଶ"); missingEn.add("Inclusive teaching suggestions")
        }

        val percent = ((score.toDouble() / totalChecks) * 100.0).roundToInt().coerceIn(0, 100)
        return LessonPlanReadiness(
            readinessPercent = percent,
            panchapadiReadyCount = panchapadiReady,
            missingItemsOdia = missingOdia,
            missingItemsEn = missingEn
        )
    }

    /**
     * Computes deterministic 0..100% classroom progress against Panchapadi steps,
     * NEP 2020 stage guidelines, learning outcomes, and TLM/Assessment/Inclusive execution.
     */
    fun computeCompletionPercent(plan: LessonPlan, progress: LessonPlanProgress): Int {
        val stageGuidelines = Nep2020Guidelines.guidelinesForStage(plan.stage)
        val validGuidelineIds = stageGuidelines.map { it.id }.toSet()
        val outcomes = extractOutcomes(plan)

        val panchapadiDone = listOf(
            progress.aditiCompleted,
            progress.bodhaCompleted,
            progress.abhyasaCompleted,
            progress.prayogaCompleted,
            progress.prasaraCompleted
        ).count { it }.toDouble()

        val guidelinesDone = progress.checkedGuidelineIds.count { it in validGuidelineIds }.toDouble()

        var outcomePoints = 0.0
        outcomes.indices.forEach { idx ->
            val idxLong = idx.toLong()
            when {
                idxLong in progress.achievedOutcomeIndices -> outcomePoints += 1.0
                idxLong in progress.inProgressOutcomeIndices -> outcomePoints += 0.5
            }
        }

        val pedagogyChecksDone = listOf(
            progress.tlmUsedInClass,
            progress.assessmentConducted,
            progress.inclusiveSupportApplied
        ).count { it }.toDouble()

        val totalUnits = 5.0 + stageGuidelines.size.toDouble() + outcomes.size.toDouble() + 3.0
        val completedUnits = panchapadiDone + guidelinesDone + outcomePoints + pedagogyChecksDone

        return if (totalUnits <= 0.0) 0 else ((completedUnits / totalUnits) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun outcomeStatusFor(progress: LessonPlanProgress, index: Int): OutcomeStatus {
        val key = index.toLong()
        return when {
            key in progress.achievedOutcomeIndices -> OutcomeStatus.ACHIEVED
            key in progress.inProgressOutcomeIndices -> OutcomeStatus.IN_PROGRESS
            else -> OutcomeStatus.NOT_STARTED
        }
    }

    fun aggregateSummary(
        plans: List<LessonPlan>,
        progressByPlanId: Map<String, LessonPlanProgress>
    ): NepProgressSummary {
        if (plans.isEmpty()) {
            return NepProgressSummary(
                totalPlans = 0,
                trackedPlansCount = 0,
                fullyCompletedPlansCount = 0,
                averageExecutionPercent = 0,
                averageReadinessPercent = 0,
                totalOutcomes = 0,
                achievedOutcomes = 0,
                inProgressOutcomes = 0,
                aditiRatePercent = 0,
                bodhaRatePercent = 0,
                abhyasaRatePercent = 0,
                prayogaRatePercent = 0,
                prasaraRatePercent = 0,
                stageBreakdown = NepStage.entries.associateWith { 0 }
            )
        }

        var sumExecution = 0
        var sumReadiness = 0
        var trackedCount = 0
        var fullCount = 0
        var totalOutcomes = 0
        var achievedOutcomes = 0
        var inProgressOutcomes = 0

        var aditiCount = 0
        var bodhaCount = 0
        var abhyasaCount = 0
        var prayogaCount = 0
        var prasaraCount = 0

        val stageScores = mutableMapOf<NepStage, MutableList<Int>>()
        NepStage.entries.forEach { stageScores[it] = mutableListOf() }

        for (plan in plans) {
            val readiness = evaluatePlanReadiness(plan)
            sumReadiness += readiness.readinessPercent

            val prog = progressByPlanId[plan.id]
            val outcomes = extractOutcomes(plan)
            totalOutcomes += outcomes.size

            val execPercent = if (prog != null) {
                computeCompletionPercent(plan, prog)
            } else {
                0
            }
            sumExecution += execPercent

            if (prog != null) {
                if (execPercent > 0 || prog.classroomNotes.isNotBlank()) trackedCount++
                if (execPercent >= 100) fullCount++
                if (prog.aditiCompleted) aditiCount++
                if (prog.bodhaCompleted) bodhaCount++
                if (prog.abhyasaCompleted) abhyasaCount++
                if (prog.prayogaCompleted) prayogaCount++
                if (prog.prasaraCompleted) prasaraCount++

                outcomes.indices.forEach { idx ->
                    when (outcomeStatusFor(prog, idx)) {
                        OutcomeStatus.ACHIEVED -> achievedOutcomes++
                        OutcomeStatus.IN_PROGRESS -> inProgressOutcomes++
                        OutcomeStatus.NOT_STARTED -> Unit
                    }
                }
            }
            stageScores[NepStage.fromKey(plan.stage)]?.add(execPercent)
        }

        val count = plans.size.toDouble()
        return NepProgressSummary(
            totalPlans = plans.size,
            trackedPlansCount = trackedCount,
            fullyCompletedPlansCount = fullCount,
            averageExecutionPercent = (sumExecution / count).roundToInt(),
            averageReadinessPercent = (sumReadiness / count).roundToInt(),
            totalOutcomes = totalOutcomes,
            achievedOutcomes = achievedOutcomes,
            inProgressOutcomes = inProgressOutcomes,
            aditiRatePercent = ((aditiCount / count) * 100.0).roundToInt(),
            bodhaRatePercent = ((bodhaCount / count) * 100.0).roundToInt(),
            abhyasaRatePercent = ((abhyasaCount / count) * 100.0).roundToInt(),
            prayogaRatePercent = ((prayogaCount / count) * 100.0).roundToInt(),
            prasaraRatePercent = ((prasaraCount / count) * 100.0).roundToInt(),
            stageBreakdown = stageScores.mapValues { (_, list) ->
                if (list.isEmpty()) 0 else list.average().roundToInt()
            }
        )
    }
}
