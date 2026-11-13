package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.BookmarkItem
import com.example.domain.model.L
import com.example.domain.model.LessonPlan
import com.example.domain.model.NepStage
import com.example.domain.model.OdishaCurriculum
import com.example.domain.model.PanchapadiConfig
import com.example.ui.components.DayMonthYearPickerRow
import com.example.ui.components.StageBadgeChip
import com.example.ui.components.StageFilterRow
import com.example.ui.theme.AiDraftAmber
import com.example.ui.theme.AiDraftAmberBg
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldBg
import com.example.ui.viewmodel.AiGenState
import com.example.ui.viewmodel.UiState
import com.example.util.DateUtils
import com.example.util.PdfExporter

/**
 * Screen 5: Panchapadi Lesson Plan Generator (AI + Manual with Day/Month/Year).
 */
@Composable
fun PanchapadiGeneratorScreen(
    language: AppLanguage,
    initialStage: NepStage,
    initialClass: String,
    initialSubject: String,
    panchapadiConfig: PanchapadiConfig,
    aiGenState: AiGenState,
    onGenerateAi: (
        stage: NepStage,
        classLevel: String,
        subject: String,
        medium: String,
        topic: String,
        outcomes: String,
        durationMinutes: Int,
        day: Int,
        month: Int,
        year: Int
    ) -> Unit,
    onCreateManual: () -> Unit,
    onDismissError: () -> Unit
) {
    val today = remember { DateUtils.today() }
    var stage by remember(initialStage) { mutableStateOf(initialStage) }
    var classLevel by remember(initialClass) { mutableStateOf(initialClass) }
    var subject by remember(initialSubject) { mutableStateOf(initialSubject) }
    var medium by remember { mutableStateOf(OdishaCurriculum.mediums.first()) }
    var topic by remember { mutableStateOf("") }
    var outcomes by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var planDay by remember { mutableIntStateOf(today.day) }
    var planMonth by remember { mutableIntStateOf(today.month) }
    var planYear by remember { mutableIntStateOf(today.year) }

    var showClassMenu by remember { mutableStateOf(false) }
    var showSubjectMenu by remember { mutableStateOf(false) }
    var showMediumMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = L(language, "ପଞ୍ଚପଦୀ ପାଠ୍ୟ ଯୋଜନା ନିର୍ମାତା", "Panchapadi Lesson Plan Generator"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = L(
                    language,
                    "ଓଡ଼ିଆ ବା ଇଂରାଜୀରେ ବିଷୟ ଲେଖନ୍ତୁ — AI ଓଡ଼ିଆରେ ସମ୍ପୂର୍ଣ୍ଣ ୫-ପଦକ୍ଷେପ ଯୋଜନା ପ୍ରସ୍ତୁତ କରିବ।",
                    "Enter a topic in Odia or English to draft a structured 5-step Panchapadi lesson plan."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stage selector
        item {
            Text(
                text = L(language, "୧. ଶିକ୍ଷା ସ୍ତର (NEP Stage)", "1. Educational Stage"),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NepStage.entries.forEach { item ->
                    FilterChip(
                        selected = stage == item,
                        onClick = {
                            stage = item
                            classLevel = item.classesList.first()
                            subject = OdishaCurriculum.subjectsByStage[item]?.firstOrNull() ?: subject
                        },
                        label = {
                            Text(if (language == AppLanguage.ODIA) item.odiaName.substringBefore(" (") else item.englishName)
                        }
                    )
                }
            }
        }

        // Class, Subject, Medium pickers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showClassMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(classLevel, maxLines = 1)
                    }
                    DropdownMenu(expanded = showClassMenu, onDismissRequest = { showClassMenu = false }) {
                        stage.classesList.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls) },
                                onClick = {
                                    classLevel = cls
                                    showClassMenu = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showSubjectMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(subject, maxLines = 1)
                    }
                    DropdownMenu(expanded = showSubjectMenu, onDismissRequest = { showSubjectMenu = false }) {
                        (OdishaCurriculum.subjectsByStage[stage] ?: emptyList()).forEach { subj ->
                            DropdownMenuItem(
                                text = { Text(subj) },
                                onClick = {
                                    subject = subj
                                    showSubjectMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showMediumMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${L(language, "ମାଧ୍ୟମ", "Medium")}: $medium")
                }
                DropdownMenu(expanded = showMediumMenu, onDismissRequest = { showMediumMenu = false }) {
                    OdishaCurriculum.mediums.forEach { med ->
                        DropdownMenuItem(
                            text = { Text(med) },
                            onClick = {
                                medium = med
                                showMediumMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Day, Month, Year Picker
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    DayMonthYearPickerRow(
                        day = planDay,
                        month = planMonth,
                        year = planYear,
                        language = language,
                        onDateChanged = { d, m, y ->
                            planDay = d
                            planMonth = m
                            planYear = y
                        }
                    )
                }
            }
        }

        // Topic & Learning Outcomes & Duration
        item {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it; onDismissError() },
                label = { Text(L(language, "ପାଠର ବିଷୟବସ୍ତୁ (ଓଡ଼ିଆ / English)", "Lesson Topic (Odia or English)")) },
                placeholder = { Text(L(language, "ଯଥା: ଭଗ୍ନାଂଶ, ସୌରଜଗତ, ଗୋପବନ୍ଧୁ ଦାସ...", "e.g., Fractions, Solar System, Water Cycle")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generator_topic_input"),
                singleLine = false,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = outcomes,
                onValueChange = { outcomes = it },
                label = { Text(L(language, "ଇଚ୍ଛିତ ଶିକ୍ଷଣ ଫଳାଫଳ (ବୈକଳ୍ପିକ)", "Learning Outcomes (Optional)")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generator_outcomes_input"),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = L(language, "ସମୟ ଅବଧି (ମିନିଟ୍)", "Duration (Minutes)"),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(30, 40, 45, 60, 90).forEach { mins ->
                    FilterChip(
                        selected = durationMinutes == mins,
                        onClick = { durationMinutes = mins },
                        label = { Text("$mins min") }
                    )
                }
            }
        }

        // Configurable Panchapadi Preview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = L(language, "ପଞ୍ଚପଦୀ ୫ଟି ପଦକ୍ଷେପ (ସଂସ୍ଥାଗତ ଢାଞ୍ଚା)", "Panchapadi 5-Step Methodology"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("• ${panchapadiConfig.aditiLabel}: ${panchapadiConfig.aditiDesc}", style = MaterialTheme.typography.bodySmall)
                    Text("• ${panchapadiConfig.bodhaLabel}: ${panchapadiConfig.bodhaDesc}", style = MaterialTheme.typography.bodySmall)
                    Text("• ${panchapadiConfig.abhyasaLabel}: ${panchapadiConfig.abhyasaDesc}", style = MaterialTheme.typography.bodySmall)
                    Text("• ${panchapadiConfig.prayogaLabel}: ${panchapadiConfig.prayogaDesc}", style = MaterialTheme.typography.bodySmall)
                    Text("• ${panchapadiConfig.prasaraLabel}: ${panchapadiConfig.prasaraDesc}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (aiGenState is AiGenState.Error) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiGenState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Generate & Manual Actions
        item {
            val isGenerating = aiGenState is AiGenState.Generating
            Button(
                onClick = {
                    onGenerateAi(
                        stage,
                        classLevel,
                        subject,
                        medium,
                        topic,
                        outcomes,
                        durationMinutes,
                        planDay,
                        planMonth,
                        planYear
                    )
                },
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_ai_plan_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(L(language, "AI ପଞ୍ଚପଦୀ ଯୋଜନା ପ୍ରସ୍ତୁତ କରୁଛି...", "Generating Panchapadi Draft..."))
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L(language, "AI ଦ୍ୱାରା ପଞ୍ଚପଦୀ ଯୋଜନା ତିଆରି କରନ୍ତୁ", "Generate Panchapadi Plan with AI"))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCreateManual,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_manual_plan_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(L(language, "ସ୍ୱୟଂ (Manual) ଖାଲି ଯୋଜନା ଲେଖନ୍ତୁ", "Create Manual Blank Lesson Plan"))
            }
        }
    }
}

/**
 * Screen 6: Lesson Plan Preview & Editor (with AI Draft badge, per-section regeneration, Save/Publish, PDF Export).
 */
@Composable
fun LessonPlanEditorScreen(
    plan: LessonPlan?,
    panchapadiConfig: PanchapadiConfig,
    language: AppLanguage,
    regeneratingSection: String?,
    isOnline: Boolean = true,
    isDownloadedOffline: Boolean = false,
    isPendingSync: Boolean = false,
    onUpdatePlan: ((LessonPlan) -> LessonPlan) -> Unit,
    onRegenerateSection: (key: String, title: String, current: String, apply: (LessonPlan, String) -> LessonPlan) -> Unit,
    onSavePlan: (publish: Boolean) -> Unit,
    onToggleOfflineDownload: (LessonPlan) -> Unit = {},
    onTrackProgress: (LessonPlan) -> Unit = {},
    onShareToCommunity: (LessonPlan) -> Unit
) {
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(false) }
    var pdfMessage by remember { mutableStateOf<String?>(null) }

    if (plan == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(L(language, "କୌଣସି ପାଠ୍ୟ ଯୋଜନା ଚୟନ ହୋଇନାହିଁ", "No lesson plan selected"))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI Draft Warning / Verified Banner
        item {
            val isVerified = plan.isVerified
            Surface(
                color = if (isVerified) VerifiedEmeraldBg else AiDraftAmberBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isVerified) VerifiedEmerald else AiDraftAmber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when {
                            isVerified -> L(
                                language,
                                "ପ୍ରଶାସକଙ୍କ ଦ୍ୱାରା ପ୍ରମାଣିତ (Officially Verified Plan)",
                                "Officially Verified by Administrator"
                            )
                            plan.isAiGenerated -> L(
                                language,
                                "AI ପ୍ରସ୍ତୁତ ଡ୍ରାଫ୍ଟ — ଶିକ୍ଷକଙ୍କ ସମୀକ୍ଷା ଆବଶ୍ୟକ। ଏହା ସରକାରୀ ଭାବେ ଅନୁମୋଦିତ ନୁହେଁ।",
                                "AI-Generated Draft — Teacher review required. Not officially approved until verified."
                            )
                            else -> L(
                                language,
                                "ଶିକ୍ଷକଙ୍କ ଡ୍ରାଫ୍ଟ ଯୋଜନା",
                                "Teacher Draft Lesson Plan"
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isVerified) VerifiedEmerald else AiDraftAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

            if (!isOnline || isDownloadedOffline || isPendingSync) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isOnline) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = L(language, "ଅଫଲାଇନ୍ ସମ୍ପାଦନ (ଅନଲାଇନ୍ ହେଲେ ସିଙ୍କ୍ ହେବ)", "Editing offline (auto-syncs when online)"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (isDownloadedOffline) {
                            Surface(
                                color = Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.OfflinePin, contentDescription = null, tint = Color(0xFF0369A1), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = L(language, "ଅଫଲାଇନ୍ ଉପଲବ୍ଧ", "Saved Offline"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF0369A1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (isPendingSync) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = L(language, "ସିଙ୍କ୍ ବାକି ଅଛି", "Pending Sync"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB91C1C),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // Action Bar: Toggle Edit/Preview, Save, Publish, Offline Download, PDF Export, Share
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier.testTag("editor_toggle_mode_button")
                ) {
                    Icon(if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditMode) L(language, "ପୂର୍ବାବଲୋକନ", "Preview") else L(language, "ସମ୍ପାଦନ", "Edit"))
                }
                Button(
                    onClick = { onSavePlan(false) },
                    modifier = Modifier.testTag("editor_save_draft_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L(language, "ସୁରକ୍ଷିତ", "Save Draft"))
                }
                Button(
                    onClick = { onSavePlan(true) },
                    modifier = Modifier.testTag("editor_publish_button")
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L(language, "ପ୍ରକାଶ କରନ୍ତୁ", "Publish"))
                }
                OutlinedButton(
                    onClick = { onToggleOfflineDownload(plan) },
                    modifier = Modifier.testTag("editor_offline_download_button")
                ) {
                    Icon(
                        imageVector = if (isDownloadedOffline) Icons.Default.OfflinePin else Icons.Default.DownloadForOffline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isDownloadedOffline) {
                            L(language, "ଅଫଲାଇନ୍ ଅଛି", "Offline Ready")
                        } else {
                            L(language, "ଅଫଲାଇନ୍ ଡାଉନଲୋଡ୍", "Save Offline")
                        }
                    )
                }
                OutlinedButton(
                    onClick = {
                        val res = PdfExporter.exportAndShareLessonPlan(context, plan, panchapadiConfig, language)
                        pdfMessage = if (res.isSuccess) {
                            L(language, "PDF ପ୍ରସ୍ତୁତ ହେଲା ଓ ସେୟାର୍ ଖୋଲାଗଲା", "PDF exported & share sheet opened")
                        } else {
                            res.exceptionOrNull()?.localizedMessage ?: "PDF export failed"
                        }
                    },
                    modifier = Modifier.testTag("editor_export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = { onShareToCommunity(plan) },
                    modifier = Modifier.testTag("editor_share_community_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L(language, "ସମୁଦାୟରେ ଦିଅନ୍ତୁ", "Post"))
                }
                if (plan.id.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onTrackProgress(plan) },
                        modifier = Modifier.testTag("editor_track_progress_button")
                    ) {
                        Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L(language, "ପ୍ରଗତି ଟ୍ରାକ୍", "Track NEP"))
                    }
                }
            }
            if (pdfMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = pdfMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Metadata Header (Title + Stage + Class + Subject + Day/Month/Year)
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StageBadgeChip(NepStage.fromKey(plan.stage), language)
                    if (isEditMode) {
                        OutlinedTextField(
                            value = plan.title,
                            onValueChange = { v -> onUpdatePlan { it.copy(title = v) } },
                            label = { Text(L(language, "ପାଠ ଶୀର୍ଷକ", "Lesson Title")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DayMonthYearPickerRow(
                            day = plan.planDay.toInt(),
                            month = plan.planMonth.toInt(),
                            year = plan.planYear.toInt(),
                            language = language,
                            onDateChanged = { d, m, y ->
                                onUpdatePlan { it.copy(planDay = d.toLong(), planMonth = m.toLong(), planYear = y.toLong()) }
                            }
                        )
                    } else {
                        Text(
                            text = plan.title.ifBlank { plan.topic },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${plan.classLevel} • ${plan.subject} • ${plan.medium} • ${plan.durationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${L(language, "ତାରିଖ", "Date")}: ${DateUtils.formatDate(plan.planDay.toInt(), plan.planMonth.toInt(), plan.planYear.toInt(), language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Editable & AI-Regenerable Sections
        val sections = listOf(
            SectionSpec("objectives", L(language, "ଶିକ୍ଷଣ ଉଦ୍ଦେଶ୍ୟ (Learning Objectives)", "Learning Objectives"), plan.learningObjectives, null) { p, v -> p.copy(learningObjectives = v) },
            SectionSpec("tlm", L(language, "ଶିକ୍ଷଣ-ଶିକ୍ଷା ସାମଗ୍ରୀ (TLM)", "Teaching-Learning Materials"), plan.tlmMaterials, null) { p, v -> p.copy(tlmMaterials = v) },
            SectionSpec("teacher_act", L(language, "ଶିକ୍ଷକଙ୍କ କାର୍ଯ୍ୟ (Teacher Activities)", "Teacher Activities"), plan.teacherActivities, null) { p, v -> p.copy(teacherActivities = v) },
            SectionSpec("student_act", L(language, "ଛାତ୍ରଛାତ୍ରୀଙ୍କ କାର୍ଯ୍ୟ (Student Activities)", "Student Activities"), plan.studentActivities, null) { p, v -> p.copy(studentActivities = v) },
            SectionSpec("aditi", "${panchapadiConfig.aditiLabel} (${plan.aditiMinutes} min)", "${panchapadiConfig.aditiLabel} (${plan.aditiMinutes} min)", plan.aditiStep, plan.aditiMinutes.toInt()) { p, v -> p.copy(aditiStep = v) },
            SectionSpec("bodha", "${panchapadiConfig.bodhaLabel} (${plan.bodhaMinutes} min)", "${panchapadiConfig.bodhaLabel} (${plan.bodhaMinutes} min)", plan.bodhaStep, plan.bodhaMinutes.toInt()) { p, v -> p.copy(bodhaStep = v) },
            SectionSpec("abhyasa", "${panchapadiConfig.abhyasaLabel} (${plan.abhyasaMinutes} min)", "${panchapadiConfig.abhyasaLabel} (${plan.abhyasaMinutes} min)", plan.abhyasaStep, plan.abhyasaMinutes.toInt()) { p, v -> p.copy(abhyasaStep = v) },
            SectionSpec("prayoga", "${panchapadiConfig.prayogaLabel} (${plan.prayogaMinutes} min)", "${panchapadiConfig.prayogaLabel} (${plan.prayogaMinutes} min)", plan.prayogaStep, plan.prayogaMinutes.toInt()) { p, v -> p.copy(prayogaStep = v) },
            SectionSpec("prasara", "${panchapadiConfig.prasaraLabel} (${plan.prasaraMinutes} min)", "${panchapadiConfig.prasaraLabel} (${plan.prasaraMinutes} min)", plan.prasaraStep, plan.prasaraMinutes.toInt()) { p, v -> p.copy(prasaraStep = v) },
            SectionSpec("competency", L(language, "ଦକ୍ଷତା-ଆଧାରିତ ପ୍ରଶ୍ନ (Competency Qs)", "Competency-Based Questions"), plan.competencyQuestions, null) { p, v -> p.copy(competencyQuestions = v) },
            SectionSpec("assessment", L(language, "ମୂଲ୍ୟାଙ୍କନ ମାନଦଣ୍ଡ (Assessment)", "Assessment Criteria"), plan.assessmentCriteria, null) { p, v -> p.copy(assessmentCriteria = v) },
            SectionSpec("homework", L(language, "ଗୃହକାର୍ଯ୍ୟ ଓ ଉପଚାରାତ୍ମକ କାର୍ଯ୍ୟ", "Homework & Remedial/Enrichment"), plan.homeworkActivities, null) { p, v -> p.copy(homeworkActivities = v) },
            SectionSpec("inclusive", L(language, "ସମାବେଶୀ ଶିକ୍ଷଣ ପରାମର୍ଶ (Inclusive)", "Inclusive Teaching Suggestions"), plan.inclusiveSuggestions, null) { p, v -> p.copy(inclusiveSuggestions = v) },
            SectionSpec("summary", L(language, "ସାରାଂଶ (Summary)", "Lesson Summary"), plan.summary, null) { p, v -> p.copy(summary = v) },
            SectionSpec("reflection", L(language, "ଶିକ୍ଷକଙ୍କ ଚିନ୍ତନ (Reflection)", "Teacher Reflection"), plan.teacherReflection, null) { p, v -> p.copy(teacherReflection = v) }
        )

        items(sections, key = { it.key }) { spec ->
            val isRegen = regeneratingSection == spec.key
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.ODIA) spec.titleOdia else spec.titleEn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                onRegenerateSection(spec.key, spec.titleEn, spec.content, spec.apply)
                            },
                            enabled = regeneratingSection == null
                        ) {
                            if (isRegen) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(L(language, "ପୁନଃନିର୍ମାଣ", "Regenerate"))
                        }
                    }

                    if (isEditMode) {
                        OutlinedTextField(
                            value = spec.content,
                            onValueChange = { newText ->
                                onUpdatePlan { spec.apply(it, newText) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    } else {
                        Text(
                            text = spec.content.ifBlank { L(language, "(ଖାଲି — ସମ୍ପାଦନ ବଟନ୍ ଦବାଇ ଲେଖନ୍ତୁ)", "(Empty — tap Edit to add)") },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private data class SectionSpec(
    val key: String,
    val titleOdia: String,
    val titleEn: String,
    val content: String,
    val minutes: Int?,
    val apply: (LessonPlan, String) -> LessonPlan
) {
    constructor(
        key: String,
        title: String,
        content: String,
        minutes: Int?,
        apply: (LessonPlan, String) -> LessonPlan
    ) : this(key, title, title, content, minutes, apply)
}

/**
 * Screen 7: Lesson Plan Library (My Drafts, Published Plans, Offline Downloads, Bookmarks).
 */
@Composable
fun LessonPlanLibraryScreen(
    language: AppLanguage,
    myPlansState: UiState<List<LessonPlan>>,
    publishedPlansState: UiState<List<LessonPlan>>,
    downloadedPlans: List<LessonPlan> = emptyList(),
    downloadedPlanIds: Set<String> = emptySet(),
    pendingSyncIds: Set<String> = emptySet(),
    pendingSyncCount: Int = 0,
    isOnline: Boolean = true,
    isSyncing: Boolean = false,
    bookmarks: List<BookmarkItem>,
    initialTab: Int = 0,
    onOpenPlan: (LessonPlan) -> Unit,
    onDeletePlan: (String) -> Unit,
    onToggleOfflineDownload: (LessonPlan) -> Unit = {},
    onTrackProgress: (LessonPlan) -> Unit = {},
    onSyncNow: () -> Unit = {},
    onBookmarkPlan: (LessonPlan) -> Unit,
    onSeedSamples: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var stageFilter by remember { mutableStateOf<NepStage?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(L(language, "ମୋ ଯୋଜନା", "My Plans"), maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(L(language, "ପ୍ରକାଶିତ", "Published"), maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(L(language, "ଅଫଲାଇନ୍ (${downloadedPlans.size})", "Offline (${downloadedPlans.size})"), maxLines = 1) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(L(language, "ବୁକମାର୍କ (${bookmarks.size})", "Saved (${bookmarks.size})"), maxLines = 1) }
            )
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            StageFilterRow(
                selectedStage = stageFilter,
                language = language,
                onSelectStage = { stageFilter = it }
            )
        }

        when (selectedTab) {
            0, 1 -> {
                val state = if (selectedTab == 0) myPlansState else publishedPlansState
                when (state) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val filtered = state.data.filter { plan ->
                            stageFilter == null || plan.stage == stageFilter?.key
                        }
                        if (filtered.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = L(language, "କୌଣସି ପାଠ୍ୟ ଯୋଜନା ମିଳିଲା ନାହିଁ", "No lesson plans found"),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (selectedTab == 0) {
                                    OutlinedButton(onClick = onSeedSamples) {
                                        Text(L(language, "ନମୁନା ଓଡ଼ିଶା SCERT ଯୋଜନା ଯୋଡ଼ନ୍ତୁ", "Load Odisha Sample Plans"))
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filtered, key = { it.id }) { plan ->
                                    val isDownloaded = plan.id in downloadedPlanIds
                                    val isPending = plan.id in pendingSyncIds
                                    Column {
                                        LessonPlanSummaryCard(
                                            plan = plan,
                                            language = language,
                                            isDownloaded = isDownloaded,
                                            isPendingSync = isPending,
                                            onClick = { onOpenPlan(plan) }
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = { onToggleOfflineDownload(plan) },
                                                modifier = Modifier.testTag("download_offline_btn_${plan.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isDownloaded) Icons.Default.OfflinePin else Icons.Default.DownloadForOffline,
                                                    contentDescription = if (isDownloaded) {
                                                        L(language, "ଅଫଲାଇନ୍ ହଟାନ୍ତୁ", "Remove offline copy")
                                                    } else {
                                                        L(language, "ଅଫଲାଇନ୍ ଡାଉନଲୋଡ୍", "Download for offline")
                                                    },
                                                    tint = if (isDownloaded) Color(0xFF0369A1) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onTrackProgress(plan) },
                                                modifier = Modifier.testTag("track_progress_btn_${plan.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Insights,
                                                    contentDescription = L(language, "NEP ପ୍ରଗତି ଟ୍ରାକ୍", "Track NEP Progress"),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(onClick = { onBookmarkPlan(plan) }) {
                                                Icon(Icons.Default.Bookmark, contentDescription = "Bookmark")
                                            }
                                            IconButton(
                                                onClick = {
                                                    PdfExporter.exportAndShareLessonPlan(context, plan, PanchapadiConfig(), language)
                                                }
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                                            }
                                            if (selectedTab == 0) {
                                                IconButton(onClick = { onDeletePlan(plan.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                val filteredOffline = downloadedPlans.filter { plan ->
                    stageFilter == null || plan.stage == stageFilter?.key
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    if (pendingSyncCount > 0) {
                        Surface(
                            color = if (isOnline) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = L(
                                        language,
                                        "$pendingSyncCount ଟି ଅଫଲାଇନ୍ ପରିବର୍ତ୍ତନ ସିଙ୍କ୍ ପାଇଁ ଅପେକ୍ଷା କରିଛି",
                                        "$pendingSyncCount offline edit(s) waiting to sync"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isOnline) {
                                    Button(
                                        onClick = onSyncNow,
                                        enabled = !isSyncing,
                                        modifier = Modifier.testTag("library_sync_now_button")
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(L(language, "ସିଙ୍କ୍", "Sync"))
                                    }
                                }
                            }
                        }
                    }

                    if (filteredOffline.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = L(
                                    language,
                                    "କୌଣସି ଅଫଲାଇନ୍ ପାଠ୍ୟ ଯୋଜନା ଡାଉନଲୋଡ୍ ହୋଇନାହିଁ",
                                    "No lesson plans downloaded for offline access yet"
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = L(
                                    language,
                                    "ଇଣ୍ଟରନେଟ୍ ବିନା ପଢ଼ିବା ଓ ସମ୍ପାଦନ ପାଇଁ ଯେକୌଣସି ଯୋଜନାରେ ଡାଉନଲୋଡ୍ ଆଇକନ୍ ଦବାନ୍ତୁ।",
                                    "Tap the download icon on any lesson plan to read and edit it without internet."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredOffline, key = { it.id }) { plan ->
                                val isPending = plan.id in pendingSyncIds
                                Column {
                                    LessonPlanSummaryCard(
                                        plan = plan,
                                        language = language,
                                        isDownloaded = true,
                                        isPendingSync = isPending,
                                        onClick = { onOpenPlan(plan) }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { onToggleOfflineDownload(plan) }) {
                                            Icon(Icons.Default.OfflinePin, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(L(language, "ଡିଭାଇସରୁ ହଟାନ୍ତୁ", "Remove Offline"))
                                        }
                                        IconButton(
                                            onClick = {
                                                PdfExporter.exportAndShareLessonPlan(context, plan, PanchapadiConfig(), language)
                                            }
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                if (bookmarks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(L(language, "କୌଣସି ବୁକମାର୍କ ନାହିଁ", "No bookmarked items yet"))
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bm ->
                            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(bm.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${bm.targetType} • ${bm.subject}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
