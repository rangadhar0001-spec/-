package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.L
import com.example.domain.model.LessonPlan
import com.example.domain.model.NepStage
import com.example.domain.model.OdishaCurriculum
import com.example.ui.components.StageBadgeChip
import com.example.ui.components.stageBackgroundColor
import com.example.ui.components.stageForegroundColor
import com.example.ui.theme.KonarkSaffron
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.TerracottaCoral
import com.example.ui.viewmodel.UiState
import com.example.util.DateUtils

/**
 * Screen 3: Home Dashboard with Educational Categories, Quick Actions, and Recent Plans.
 */
@Composable
fun HomeDashboardScreen(
    language: AppLanguage,
    myPlansState: UiState<List<LessonPlan>>,
    downloadedPlanIds: Set<String> = emptySet(),
    pendingSyncIds: Set<String> = emptySet(),
    overallProgressPercent: Int = 0,
    outcomesSummaryText: String = "",
    onOpenNepNavigator: () -> Unit,
    onOpenGenerator: (NepStage?) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenProgressTracker: () -> Unit = {},
    onOpenSearchGroups: () -> Unit,
    onOpenPlanEditor: (LessonPlan) -> Unit,
    onSeedSamples: () -> Unit
) {
    val recentPlans = (myPlansState as? UiState.Success)?.data?.take(3) ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_odia_hero_banner),
                        contentDescription = L(language, "ଶିକ୍ଷା ସାଥୀ ବାନର", "Sikshya Sathi banner"),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.15f),
                                        Color(0xFF063A40).copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = L(
                                    language,
                                    "ପଞ୍ଚପଦୀ AI ପାଠ୍ୟ ଯୋଜନା",
                                    "Panchapadi AI Lesson Planner"
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L(
                                    language,
                                    "ଓଡ଼ିଶା SCERT ଓ NEP 2020 (5+3+3+4) ଅନୁସାରେ ଶ୍ରେଣୀ କକ୍ଷ ପ୍ରସ୍ତୁତି",
                                    "Odisha SCERT & NEP 2020 5+3+3+4 classroom-ready planning"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Grid (2x2)
        item {
            Text(
                text = L(language, "ଶୀଘ୍ର କାର୍ଯ୍ୟ (Quick Actions)", "Quick Actions"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = L(language, "AI ଯୋଜନା\nତିଆରି", "AI Panchapadi\nGenerator"),
                    icon = Icons.Default.AutoAwesome,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    testTag = "home_quick_generator",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenGenerator(null) }
                )
                QuickActionTile(
                    title = L(language, "5+3+3+4\nଦର୍ଶକ", "NEP 2020\nNavigator"),
                    icon = Icons.Default.AccountTree,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = KonarkSaffron,
                    testTag = "home_quick_nep",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenNepNavigator
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = L(language, "ପାଠ୍ୟ\nଭଣ୍ଡାର", "Saved\nLibrary"),
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = TerracottaCoral,
                    testTag = "home_quick_library",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLibrary
                )
                QuickActionTile(
                    title = L(language, "ଶିକ୍ଷକ\nସମୁଦାୟ", "Teacher\nCommunity"),
                    icon = Icons.Default.Groups,
                    containerColor = Color(0xFFE0E7FF),
                    contentColor = Color(0xFF3730A3),
                    testTag = "home_quick_community",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCommunity
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProgressTracker)
                    .testTag("home_nep_progress_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = PeacockTeal,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = L(language, "NEP 2020 ଓ ପଞ୍ଚପଦୀ ପ୍ରଗତି ଟ୍ରାକର୍", "NEP 2020 & Panchapadi Progress"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PeacockTeal
                                )
                                if (outcomesSummaryText.isNotBlank()) {
                                    Text(
                                        text = outcomesSummaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$overallProgressPercent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PeacockTeal
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (overallProgressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = PeacockTeal
                    )
                }
            }
        }

        // 4 NEP 2020 Educational Categories
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L(language, "NEP 2020 ୪ଟି ଶିକ୍ଷା ସ୍ତର", "4 NEP 2020 Stages"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onOpenSearchGroups) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(L(language, "ଖୋଜନ୍ତୁ", "Explore"))
                }
            }
        }

        items(NepStage.entries) { stage ->
            NepStageCategoryCard(
                stage = stage,
                language = language,
                onClick = { onOpenGenerator(stage) }
            )
        }

        // Recent Lesson Plans Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L(language, "ଆପଣଙ୍କ ସାମ୍ପ୍ରତିକ ଯୋଜନା", "Your Recent Plans"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (recentPlans.isEmpty()) {
                    OutlinedButton(onClick = onSeedSamples) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L(language, "ନମୁନା ଯୋଜନା", "Load Samples"))
                    }
                }
            }
        }

        if (recentPlans.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = L(
                                language,
                                "ଏପର୍ଯ୍ୟନ୍ତ କୌଣସି ପାଠ୍ୟ ଯୋଜନା ନାହିଁ। ନୂତନ ପଞ୍ଚପଦୀ ଯୋଜନା ତିଆରି କରନ୍ତୁ!",
                                "No saved lesson plans yet. Generate your first Panchapadi plan!"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentPlans, key = { it.id }) { plan ->
                LessonPlanSummaryCard(
                    plan = plan,
                    language = language,
                    isDownloaded = plan.id in downloadedPlanIds,
                    isPendingSync = plan.id in pendingSyncIds,
                    onClick = { onOpenPlanEditor(plan) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NepStageCategoryCard(
    stage: NepStage,
    language: AppLanguage,
    onClick: () -> Unit
) {
    val fg = stageForegroundColor(stage)
    val bg = stageBackgroundColor(stage)
    val illustrationRes = if (stage == NepStage.FOUNDATIONAL || stage == NepStage.PREPARATORY) {
        R.drawable.img_stage_foundational
    } else {
        R.drawable.img_stage_secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("nep_stage_card_${stage.key}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(fg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stage.formulaPart,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.ODIA) stage.odiaName else stage.englishName,
                    style = MaterialTheme.typography.titleMedium,
                    color = fg,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == AppLanguage.ODIA) {
                        "${stage.ageRangeOdia} • ${stage.classesOdia}"
                    } else {
                        "${stage.ageRangeEn} • ${stage.classesEn}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Image(
                painter = painterResource(id = illustrationRes),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun LessonPlanSummaryCard(
    plan: LessonPlan,
    language: AppLanguage,
    isDownloaded: Boolean = false,
    isPendingSync: Boolean = false,
    onClick: () -> Unit
) {
    val stage = NepStage.fromKey(plan.stage)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("lesson_plan_card_${plan.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StageBadgeChip(stage, language)
                Text(
                    text = DateUtils.formatDate(plan.planDay.toInt(), plan.planMonth.toInt(), plan.planYear.toInt(), language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = plan.title.ifBlank { plan.topic },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${plan.classLevel} • ${plan.subject} • ${plan.durationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (plan.isVerified) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = when {
                            plan.isVerified -> L(language, "ପ୍ରମାଣିତ (Verified)", "Verified")
                            plan.isAiGenerated -> L(language, "AI ଡ୍ରାଫ୍ଟ", "AI Draft")
                            else -> L(language, "ଡ୍ରାଫ୍ଟ", "Draft")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (plan.isVerified) Color(0xFF047857) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (plan.status == "published") L(language, "ପ୍ରକାଶିତ", "Published") else L(language, "ସଂରକ୍ଷିତ", "Saved"),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (isDownloaded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = L(language, "ଅଫଲାଇନ୍", "Offline"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (isPendingSync) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = L(language, "ସିଙ୍କ୍ ବାକି", "Pending Sync"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB91C1C),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 4: NEP 2020 5+3+3+4 Visual Navigation Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NepNavigatorScreen(
    language: AppLanguage,
    onCreatePlanForSelection: (stage: NepStage, classLevel: String, subject: String) -> Unit
) {
    var selectedStage by remember { mutableStateOf(NepStage.FOUNDATIONAL) }
    var selectedClass by remember(selectedStage) { mutableStateOf(selectedStage.classesList.first()) }
    var selectedSubject by remember(selectedStage) {
        mutableStateOf(OdishaCurriculum.subjectsByStage[selectedStage]?.firstOrNull() ?: "Odia")
    }
    var selectedBoard by remember { mutableStateOf(OdishaCurriculum.boards.first()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = L(language, "NEP 2020: 5+3+3+4 ଶିକ୍ଷା ଗଠନ", "NEP 2020: 5+3+3+4 Visual Guide"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = L(
                    language,
                    "ସ୍ତର, ଶ୍ରେଣୀ, ବିଷୟ ଓ ଓଡ଼ିଶା ବୋର୍ଡ ବାଛି ପଞ୍ଚପଦୀ ପାଠ୍ୟ ଯୋଜନା ଆରମ୍ଭ କରନ୍ତୁ",
                    "Tap any 5+3+3+4 block to explore Odisha SCERT classes and start a lesson plan"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Visual 5 + 3 + 3 + 4 Formula Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NepStage.entries.forEach { stage ->
                    val isSelected = stage == selectedStage
                    val fg = stageForegroundColor(stage)
                    val bg = stageBackgroundColor(stage)
                    val weight = when (stage) {
                        NepStage.FOUNDATIONAL -> 1.35f
                        NepStage.SECONDARY -> 1.15f
                        else -> 0.95f
                    }
                    Surface(
                        modifier = Modifier
                            .weight(weight)
                            .height(92.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedStage = stage }
                            .testTag("nep_formula_block_${stage.key}"),
                        color = if (isSelected) fg else bg,
                        shadowElevation = if (isSelected) 6.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stage.formulaPart,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isSelected) Color.White else fg,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stage.odiaName.substringBefore(" ("),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else fg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Selected Stage Details Card
        item {
            val fg = stageForegroundColor(selectedStage)
            val bg = stageBackgroundColor(selectedStage)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = bg.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == AppLanguage.ODIA) selectedStage.odiaName else selectedStage.englishName,
                        style = MaterialTheme.typography.titleLarge,
                        color = fg,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedStage.ageRangeOdia} (${selectedStage.ageRangeEn}) • ${selectedStage.classesOdia}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Select Board / Syllabus
                    Text(
                        text = L(language, "ପାଠ୍ୟକ୍ରମ / ବୋର୍ଡ (Syllabus Board)", "Curriculum / Board"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OdishaCurriculum.boards.chunked(2).forEach { rowBoards ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBoards.forEach { board ->
                                FilterChip(
                                    selected = selectedBoard == board,
                                    onClick = { selectedBoard = board },
                                    label = { Text(board, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = L(language, "ଶ୍ରେଣୀ ବାଛନ୍ତୁ (Select Class)", "Select Class"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    selectedStage.classesList.forEach { cls ->
                        FilterChip(
                            selected = selectedClass == cls,
                            onClick = { selectedClass = cls },
                            label = { Text(cls) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = L(language, "ବିଷୟ ବାଛନ୍ତୁ (Select Subject)", "Select Subject"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    (OdishaCurriculum.subjectsByStage[selectedStage] ?: emptyList()).forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onCreatePlanForSelection(selectedStage, selectedClass, selectedSubject) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("nep_launch_generator_button")
                    ) {
                        Text(
                            text = L(
                                language,
                                "ଏହି ବିଷୟରେ ପଞ୍ଚପଦୀ ଯୋଜନା ତିଆରି କରନ୍ତୁ",
                                "Generate Panchapadi Plan for This"
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
