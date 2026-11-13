package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.L
import com.example.domain.model.LessonPlan
import com.example.domain.model.LessonPlanProgress
import com.example.domain.model.Nep2020Guidelines
import com.example.domain.model.NepProgressEvaluator
import com.example.domain.model.NepStage
import com.example.domain.model.OutcomeStatus
import com.example.domain.model.PanchapadiConfig
import com.example.ui.components.DayMonthYearPickerRow
import com.example.ui.components.StageBadgeChip
import com.example.ui.components.StageFilterRow
import com.example.ui.components.stageBackgroundColor
import com.example.ui.components.stageForegroundColor
import com.example.ui.theme.AiDraftAmber
import com.example.ui.theme.AiDraftAmberBg
import com.example.ui.theme.KonarkSaffron
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.StageSecondaryColor
import com.example.ui.theme.TerracottaCoral
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldBg
import com.example.util.DateUtils

/**
 * Screen 14: NEP 2020 Panchapadi & Learning Outcomes Progress Tracker Module.
 */
@Composable
fun NepProgressTrackerScreen(
    language: AppLanguage,
    plans: List<LessonPlan>,
    progressByPlanId: Map<String, LessonPlanProgress>,
    panchapadiConfig: PanchapadiConfig,
    selectedPlanId: String?,
    onSelectPlan: (String?) -> Unit,
    onSaveProgress: (LessonPlan, LessonPlanProgress) -> Unit,
    onBuildOutcomeNext: (LessonPlanProgress, Int, OutcomeStatus) -> LessonPlanProgress,
    onOpenPlanEditor: (LessonPlan) -> Unit,
    onOpenGenerator: () -> Unit
) {
    var stageFilter by remember { mutableStateOf<NepStage?>(null) }

    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    if (selectedPlan != null) {
        val existingOrDefault = remember(selectedPlan.id, progressByPlanId[selectedPlan.id]) {
            progressByPlanId[selectedPlan.id] ?: run {
                val today = DateUtils.today()
                LessonPlanProgress(
                    id = selectedPlan.id,
                    userId = selectedPlan.userId,
                    lessonPlanId = selectedPlan.id,
                    stage = selectedPlan.stage,
                    lastTrackedDay = today.day.toLong(),
                    lastTrackedMonth = today.month.toLong(),
                    lastTrackedYear = today.year.toLong()
                )
            }
        }
        PlanDetailedProgressView(
            plan = selectedPlan,
            initialProgress = existingOrDefault,
            panchapadiConfig = panchapadiConfig,
            language = language,
            onBackToOverview = { onSelectPlan(null) },
            onSave = { updated -> onSaveProgress(selectedPlan, updated) },
            onBuildOutcomeNext = onBuildOutcomeNext,
            onOpenEditor = { onOpenPlanEditor(selectedPlan) }
        )
        return
    }

    val filteredPlans = remember(plans, stageFilter) {
        plans.filter { stageFilter == null || it.stage == stageFilter?.key }
    }
    val summary = remember(filteredPlans, progressByPlanId) {
        NepProgressEvaluator.aggregateSummary(filteredPlans, progressByPlanId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = L(language, "NEP 2020 ଓ ପଞ୍ଚପଦୀ ପ୍ରଗତି ଟ୍ରାକର୍", "NEP 2020 & Panchapadi Progress"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = L(
                            language,
                            "ପଞ୍ଚପଦୀ ୫-ପଦକ୍ଷେପ, NEP ନିର୍ଦ୍ଦେଶାବଳୀ ଏବଂ ଶିକ୍ଷଣ ଫଳାଫଳ ଅଗ୍ରଗତି।",
                            "Track lesson execution against Panchapadi steps, NEP 2020 stage guidelines & learning outcomes."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Overall Analytics Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L(language, "ସାମଗ୍ରିକ ଶ୍ରେଣୀ ପ୍ରଗତି", "Overall Execution Progress"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PeacockTeal
                            )
                            Text(
                                text = L(
                                    language,
                                    "${summary.trackedPlansCount}/${summary.totalPlans} ଯୋଜନା ଟ୍ରାକ୍ ହୋଇଛି • ପ୍ରସ୍ତୁତି: ${summary.averageReadinessPercent}%",
                                    "${summary.trackedPlansCount}/${summary.totalPlans} plans tracked • Readiness: ${summary.averageReadinessPercent}%"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PeacockTeal
                        ) {
                            Text(
                                text = "${summary.averageExecutionPercent}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (summary.averageExecutionPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = PeacockTeal,
                        trackColor = Color.White.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricMiniPill(
                            label = L(language, "ଅର୍ଜିତ ଫଳାଫଳ", "Outcomes Achieved"),
                            value = "${summary.achievedOutcomes}/${summary.totalOutcomes}",
                            color = VerifiedEmerald
                        )
                        MetricMiniPill(
                            label = L(language, "ଚାଲିଛି", "In Progress"),
                            value = "${summary.inProgressOutcomes}",
                            color = KonarkSaffron
                        )
                        MetricMiniPill(
                            label = L(language, "ସମ୍ପୂର୍ଣ୍ଣ ଯୋଜନା", "Completed Plans"),
                            value = "${summary.fullyCompletedPlansCount}",
                            color = PeacockTeal
                        )
                    }
                }
            }
        }

        // Panchapadi 5-Step Mastery Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = L(language, "ପଞ୍ଚପଦୀ ୫-ପଦକ୍ଷେପ ପ୍ରୟୋଗ ହାର", "Panchapadi 5-Step Execution Rate"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    PanchapadiStepBar(panchapadiConfig.aditiLabel, summary.aditiRatePercent, PeacockTeal)
                    PanchapadiStepBar(panchapadiConfig.bodhaLabel, summary.bodhaRatePercent, TerracottaCoral)
                    PanchapadiStepBar(panchapadiConfig.abhyasaLabel, summary.abhyasaRatePercent, KonarkSaffron)
                    PanchapadiStepBar(panchapadiConfig.prayogaLabel, summary.prayogaRatePercent, StageSecondaryColor)
                    PanchapadiStepBar(panchapadiConfig.prasaraLabel, summary.prasaraRatePercent, VerifiedEmerald)
                }
            }
        }

        // Stage Filter
        item {
            StageFilterRow(
                selectedStage = stageFilter,
                language = language,
                onSelectStage = { stageFilter = it }
            )
        }

        item {
            Text(
                text = L(
                    language,
                    "ପାଠ୍ୟ ଯୋଜନା ଅନୁସାରେ ଟ୍ରାକ୍ କରନ୍ତୁ (${filteredPlans.size})",
                    "Track Lesson Plans (${filteredPlans.size})"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredPlans.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L(
                                language,
                                "ପ୍ରଗତି ଟ୍ରାକ୍ କରିବାକୁ ପ୍ରଥମେ ଏକ ପାଠ୍ୟ ଯୋଜନା ତିଆରି ବା ଡାଉନଲୋଡ୍ କରନ୍ତୁ।",
                                "Create or save a lesson plan first to track its NEP 2020 & Panchapadi progress."
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = onOpenGenerator) {
                            Text(L(language, "ନୂତନ ଯୋଜନା ତିଆରି", "Create Lesson Plan"))
                        }
                    }
                }
            }
        } else {
            items(filteredPlans, key = { it.id }) { plan ->
                val readiness = remember(plan) { NepProgressEvaluator.evaluatePlanReadiness(plan) }
                val progress = progressByPlanId[plan.id]
                val execPercent = if (progress != null) {
                    NepProgressEvaluator.computeCompletionPercent(plan, progress)
                } else {
                    0
                }
                val outcomes = remember(plan.learningObjectives, plan.topic) {
                    NepProgressEvaluator.extractOutcomes(plan)
                }
                val achievedCount = progress?.achievedOutcomeIndices?.count { it.toInt() in outcomes.indices } ?: 0
                val stage = NepStage.fromKey(plan.stage)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPlan(plan.id) }
                        .testTag("progress_plan_card_${plan.id}"),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StageBadgeChip(stage, language)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (execPercent >= 80) VerifiedEmeraldBg else AiDraftAmberBg
                            ) {
                                Text(
                                    text = "$execPercent% ${L(language, "ସମ୍ପୂର୍ଣ୍ଣ", "Done")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (execPercent >= 80) VerifiedEmerald else AiDraftAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = plan.title.ifBlank { plan.topic },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${plan.classLevel} • ${plan.subject}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LinearProgressIndicator(
                            progress = { (execPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = stageForegroundColor(stage),
                            trackColor = stageBackgroundColor(stage)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = L(
                                    language,
                                    "NEP ପ୍ରସ୍ତୁତି: ${readiness.readinessPercent}%",
                                    "Plan Readiness: ${readiness.readinessPercent}%"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = L(
                                    language,
                                    "ଫଳାଫଳ: $achievedCount/${outcomes.size}",
                                    "Outcomes: $achievedCount/${outcomes.size}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDetailedProgressView(
    plan: LessonPlan,
    initialProgress: LessonPlanProgress,
    panchapadiConfig: PanchapadiConfig,
    language: AppLanguage,
    onBackToOverview: () -> Unit,
    onSave: (LessonPlanProgress) -> Unit,
    onBuildOutcomeNext: (LessonPlanProgress, Int, OutcomeStatus) -> LessonPlanProgress,
    onOpenEditor: () -> Unit
) {
    var draft by remember(plan.id, initialProgress) { mutableStateOf(initialProgress) }
    BackHandler { onBackToOverview() }
    val readiness = remember(plan) { NepProgressEvaluator.evaluatePlanReadiness(plan) }
    val stageGuidelines = remember(plan.stage) { Nep2020Guidelines.guidelinesForStage(plan.stage) }
    val outcomes = remember(plan.learningObjectives, plan.topic) { NepProgressEvaluator.extractOutcomes(plan) }
    val livePercent = NepProgressEvaluator.computeCompletionPercent(plan, draft)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Back button + Plan Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToOverview) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L(language, "ପଛକୁ", "Back"))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title.ifBlank { plan.topic },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${plan.classLevel} • ${plan.subject}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onOpenEditor) {
                    Text(L(language, "ସମ୍ପାଦନ", "Edit"))
                }
            }
        }

        // Live Completion + Readiness Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = L(language, "ଶ୍ରେଣୀ କାର୍ଯ୍ୟକାରୀ ପ୍ରଗତି", "Classroom Execution Progress"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$livePercent%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PeacockTeal
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (livePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        color = PeacockTeal
                    )
                    Text(
                        text = L(
                            language,
                            "ଯୋଜନା ଗଠନ ପ୍ରସ୍ତୁତି: ${readiness.readinessPercent}% (ପଞ୍ଚପଦୀ ${readiness.panchapadiReadyCount}/5 ଲେଖାଯାଇଛି)",
                            "Plan Structure Readiness: ${readiness.readinessPercent}% (${readiness.panchapadiReadyCount}/5 Panchapadi steps authored)"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (readiness.missingItemsEn.isNotEmpty()) {
                        Surface(color = AiDraftAmberBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AiDraftAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = L(
                                        language,
                                        "ଯୋଡ଼ିବାକୁ ସୁପାରିଶ: ${readiness.missingItemsOdia.joinToString(", ")}",
                                        "Missing in plan: ${readiness.missingItemsEn.joinToString(", ")}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AiDraftAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 1: 5 Panchapadi Steps Execution Checklist
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = L(language, "୧. ପଞ୍ଚପଦୀ ୫-ପଦକ୍ଷେପ ଯାଞ୍ଚ ତାଲିକା", "1. Panchapadi 5-Step Delivery Checklist"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PanchapadiCheckboxRow(
                        label = "${panchapadiConfig.aditiLabel} (${plan.aditiMinutes} min)",
                        subLabel = plan.aditiStep.ifBlank { panchapadiConfig.aditiDesc },
                        checked = draft.aditiCompleted,
                        testTag = "check_aditi",
                        onCheckedChange = { draft = draft.copy(aditiCompleted = it) }
                    )
                    PanchapadiCheckboxRow(
                        label = "${panchapadiConfig.bodhaLabel} (${plan.bodhaMinutes} min)",
                        subLabel = plan.bodhaStep.ifBlank { panchapadiConfig.bodhaDesc },
                        checked = draft.bodhaCompleted,
                        testTag = "check_bodha",
                        onCheckedChange = { draft = draft.copy(bodhaCompleted = it) }
                    )
                    PanchapadiCheckboxRow(
                        label = "${panchapadiConfig.abhyasaLabel} (${plan.abhyasaMinutes} min)",
                        subLabel = plan.abhyasaStep.ifBlank { panchapadiConfig.abhyasaDesc },
                        checked = draft.abhyasaCompleted,
                        testTag = "check_abhyasa",
                        onCheckedChange = { draft = draft.copy(abhyasaCompleted = it) }
                    )
                    PanchapadiCheckboxRow(
                        label = "${panchapadiConfig.prayogaLabel} (${plan.prayogaMinutes} min)",
                        subLabel = plan.prayogaStep.ifBlank { panchapadiConfig.prayogaDesc },
                        checked = draft.prayogaCompleted,
                        testTag = "check_prayoga",
                        onCheckedChange = { draft = draft.copy(prayogaCompleted = it) }
                    )
                    PanchapadiCheckboxRow(
                        label = "${panchapadiConfig.prasaraLabel} (${plan.prasaraMinutes} min)",
                        subLabel = plan.prasaraStep.ifBlank { panchapadiConfig.prasaraDesc },
                        checked = draft.prasaraCompleted,
                        testTag = "check_prasara",
                        onCheckedChange = { draft = draft.copy(prasaraCompleted = it) }
                    )
                }
            }
        }

        // Section 2: Stage-Specific NEP 2020 Guidelines
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = L(language, "୨. NEP 2020 ସ୍ତର-ନିର୍ଦ୍ଦିଷ୍ଟ ନିର୍ଦ୍ଦେଶାବଳୀ", "2. NEP 2020 Stage Guidelines"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    stageGuidelines.forEach { spec ->
                        val isChecked = spec.id in draft.checkedGuidelineIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updatedList = if (isChecked) {
                                        draft.checkedGuidelineIds - spec.id
                                    } else {
                                        draft.checkedGuidelineIds + spec.id
                                    }
                                    draft = draft.copy(checkedGuidelineIds = updatedList)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updatedList = if (checked) {
                                        (draft.checkedGuidelineIds + spec.id).distinct()
                                    } else {
                                        draft.checkedGuidelineIds - spec.id
                                    }
                                    draft = draft.copy(checkedGuidelineIds = updatedList)
                                }
                            )
                            Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                                Text(
                                    text = "[${spec.code}] ${if (language == AppLanguage.ODIA) spec.titleOdia else spec.titleEn}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (language == AppLanguage.ODIA) spec.descOdia else spec.descEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Learning Outcomes Status Tracker
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = L(language, "୩. ଶିକ୍ଷଣ ଫଳାଫଳ (Learning Outcomes) ପ୍ରଗତି", "3. Learning Outcomes Mastery"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    outcomes.forEachIndexed { idx, outcomeText ->
                        val status = NepProgressEvaluator.outcomeStatusFor(draft, idx)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${idx + 1}. $outcomeText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = status == OutcomeStatus.NOT_STARTED,
                                    onClick = { draft = onBuildOutcomeNext(draft, idx, OutcomeStatus.NOT_STARTED) },
                                    label = { Text(L(language, "ଆରମ୍ଭ ନାହିଁ", "Not Started")) }
                                )
                                FilterChip(
                                    selected = status == OutcomeStatus.IN_PROGRESS,
                                    onClick = { draft = onBuildOutcomeNext(draft, idx, OutcomeStatus.IN_PROGRESS) },
                                    label = { Text(L(language, "ଚାଲିଛି", "In Progress")) }
                                )
                                FilterChip(
                                    selected = status == OutcomeStatus.ACHIEVED,
                                    onClick = { draft = onBuildOutcomeNext(draft, idx, OutcomeStatus.ACHIEVED) },
                                    leadingIcon = if (status == OutcomeStatus.ACHIEVED) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    label = { Text(L(language, "ଅର୍ଜିତ", "Achieved")) }
                                )
                            }
                            if (idx < outcomes.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Section 4: Classroom Pedagogy Verification, Date & Notes
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = L(language, "୪. ଶ୍ରେଣୀ କକ୍ଷ ପ୍ରମାଣୀକରଣ ଓ ତାରିଖ", "4. Classroom Verification & Date"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    PedagogySwitchRow(
                        label = L(language, "TLM / ଶିକ୍ଷଣ ସାମଗ୍ରୀ ବ୍ୟବହୃତ", "Teaching-Learning Materials (TLM) used"),
                        checked = draft.tlmUsedInClass,
                        onCheckedChange = { draft = draft.copy(tlmUsedInClass = it) }
                    )
                    PedagogySwitchRow(
                        label = L(language, "ମୂଲ୍ୟାଙ୍କନ / ଦକ୍ଷତା ପ୍ରଶ୍ନ କରାଯାଇଛି", "Formative assessment conducted"),
                        checked = draft.assessmentConducted,
                        onCheckedChange = { draft = draft.copy(assessmentConducted = it) }
                    )
                    PedagogySwitchRow(
                        label = L(language, "ସମାବେଶୀ ଶିକ୍ଷଣ ସହାୟତା ପ୍ରୟୋଗ", "Inclusive / differentiated support applied"),
                        checked = draft.inclusiveSupportApplied,
                        onCheckedChange = { draft = draft.copy(inclusiveSupportApplied = it) }
                    )

                    DayMonthYearPickerRow(
                        day = draft.lastTrackedDay.toInt(),
                        month = draft.lastTrackedMonth.toInt(),
                        year = draft.lastTrackedYear.toInt(),
                        language = language,
                        onDateChanged = { d, m, y ->
                            draft = draft.copy(lastTrackedDay = d.toLong(), lastTrackedMonth = m.toLong(), lastTrackedYear = y.toLong())
                        }
                    )

                    OutlinedTextField(
                        value = draft.classroomNotes,
                        onValueChange = { draft = draft.copy(classroomNotes = it) },
                        label = { Text(L(language, "ଶିକ୍ଷକଙ୍କ ଶ୍ରେଣୀ ପର୍ଯ୍ୟବେକ୍ଷଣ / ଟିପ୍ପଣୀ", "Teacher Classroom Observation & Notes")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Button(
                        onClick = { onSave(draft) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_progress_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L(language, "NEP 2020 ପ୍ରଗତି ସୁରକ୍ଷିତ କରନ୍ତୁ ($livePercent%)", "Save Progress ($livePercent%)"))
                    }
                }
            }
        }
    }
}

@Composable
private fun PanchapadiStepBar(label: String, percent: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(text = "$percent%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color
        )
    }
}

@Composable
private fun MetricMiniPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PanchapadiCheckboxRow(
    label: String,
    subLabel: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PedagogySwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
