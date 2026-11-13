package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.L
import com.example.domain.model.NepStage
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.ui.theme.KonarkSaffron
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.PeacockTealLight
import com.example.ui.theme.StageFoundationalBg
import com.example.ui.theme.StageFoundationalColor
import com.example.ui.theme.StageMiddleBg
import com.example.ui.theme.StageMiddleColor
import com.example.ui.theme.StagePreparatoryBg
import com.example.ui.theme.StagePreparatoryColor
import com.example.ui.theme.StageSecondaryBg
import com.example.ui.theme.StageSecondaryColor
import com.example.util.DateUtils

/**
 * Personal information attached top bar displaying the educator's privacy-safe profile summary,
 * role, school/district, Day/Month/Year badge, language toggle, and notifications bell.
 */
@Composable
fun EducatorPersonalTopBar(
    profile: UserProfile?,
    fallbackName: String,
    isAdmin: Boolean,
    language: AppLanguage,
    unreadNotifications: Int,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val today = remember { DateUtils.today() }
    val name = profile?.displayName?.ifBlank { fallbackName } ?: fallbackName
    val role = if (isAdmin) UserRole.APP_ADMIN else UserRole.fromKey(profile?.role ?: UserRole.TEACHER.key)
    val school = profile?.schoolName?.ifBlank { L(language, "ଓଡ଼ିଶା ବିଦ୍ୟାଳୟ", "Odisha School") }
        ?: L(language, "ଓଡ଼ିଶା ବିଦ୍ୟାଳୟ", "Odisha School")
    val district = profile?.district?.substringBefore(" (") ?: "Khordha"
    val visibility = profile?.profileVisibility ?: "public"
    val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "ଶି" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(PeacockTeal, PeacockTealLight)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: App Title + Date (Day / Month / Year) + Language + Bell
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L(language, "ଓଡ଼ିଆ ଶିକ୍ଷା ସାଥୀ", "Odia Sikshya Sathi"),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = L(language, "ତାରିଖ", "Date"),
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = DateUtils.formatDate(today.day, today.month, today.year, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A),
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onToggleLanguage)
                                .testTag("language_toggle_button"),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Switch Language",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.ODIA) "ଓଡ଼ିଆ" else "EN",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("notifications_bell_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifications > 0) {
                                        Badge(containerColor = KonarkSaffron) {
                                            Text(
                                                text = unreadNotifications.coerceAtMost(99).toString(),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = L(language, "ସୂଚନା", "Notifications"),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Attached Personal Information Strip
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenProfile)
                        .testTag("personal_info_topbar_card"),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(KonarkSaffron),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFDE68A).copy(alpha = 0.25f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isAdmin) Icons.Default.VerifiedUser else Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE68A),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (language == AppLanguage.ODIA) role.odiaLabel else role.englishLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFDE68A),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (visibility == "private") {
                                        L(language, "ବ୍ୟକ୍ତିଗତ ପ୍ରୋଫାଇଲ୍ (ଗୋପନୀୟ)", "Private Profile")
                                    } else {
                                        "$school • $district"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = if (visibility == "private") Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = L(language, "ଗୋପନୀୟତା ସ୍ଥିତି", "Privacy status"),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Connectivity & Offline Sync Status Banner shown beneath the top bar whenever offline or pending sync edits exist.
 */
@Composable
fun OfflineSyncStatusBanner(
    isOnline: Boolean,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    language: AppLanguage,
    onSyncNow: () -> Unit,
    onOpenOfflineLibrary: () -> Unit
) {
    if (isOnline && pendingSyncCount == 0 && !isSyncing) return

    val bgColor = when {
        !isOnline -> Color(0xFFFEF3C7)
        isSyncing -> Color(0xFFE0F2FE)
        else -> Color(0xFFECFDF5)
    }
    val fgColor = when {
        !isOnline -> Color(0xFF92400E)
        isSyncing -> Color(0xFF075985)
        else -> Color(0xFF065F46)
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offline_sync_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        !isOnline -> Icons.Default.CloudOff
                        isSyncing -> Icons.Default.Sync
                        else -> Icons.Default.CloudDone
                    },
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        !isOnline && pendingSyncCount > 0 -> L(
                            language,
                            "ଅଫଲାଇନ୍ ମୋଡ୍ • $pendingSyncCount ଟି ପରିବର୍ତ୍ତନ ଅନଲାଇନ୍ ହେଲେ ସିଙ୍କ୍ ହେବ",
                            "Offline mode • $pendingSyncCount edit(s) queued to sync"
                        )
                        !isOnline -> L(
                            language,
                            "ଅଫଲାଇନ୍ ମୋଡ୍ — ଡାଉନଲୋଡ୍ ହୋଇଥିବା ପାଠ୍ୟ ଯୋଜନା ଉପଲବ୍ଧ",
                            "Offline mode — Downloaded lesson plans available"
                        )
                        isSyncing -> L(
                            language,
                            "ଅଫଲାଇନ୍ ପରିବର୍ତ୍ତନ ସିଙ୍କ୍ ହେଉଛି...",
                            "Syncing offline edits..."
                        )
                        else -> L(
                            language,
                            "$pendingSyncCount ଟି ଅଫଲାଇନ୍ ପରିବର୍ତ୍ତନ ସିଙ୍କ୍ ବାକି ଅଛି",
                            "$pendingSyncCount offline edit(s) waiting to sync"
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = fgColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isOnline) {
                TextButton(onClick = onOpenOfflineLibrary) {
                    Text(
                        text = L(language, "ଅଫଲାଇନ୍ ଦେଖନ୍ତୁ", "Offline Plans"),
                        color = fgColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (pendingSyncCount > 0 && !isSyncing) {
                TextButton(
                    onClick = onSyncNow,
                    modifier = Modifier.testTag("banner_sync_now_button")
                ) {
                    Text(
                        text = L(language, "ସିଙ୍କ୍ କରନ୍ତୁ", "Sync Now"),
                        color = fgColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Interactive Day, Month, and Year selector (ଦିନ / ମାସ / ବର୍ଷ)
 */
@Composable
fun DayMonthYearPickerRow(
    day: Int,
    month: Int,
    year: Int,
    language: AppLanguage,
    onDateChanged: (day: Int, month: Int, year: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDayMenu by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    val maxDays = DateUtils.daysInMonth(month, year)
    val clampedDay = day.coerceIn(1, maxDays)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = L(language, "ତାରିଖ (ଦିନ / ମାସ / ବର୍ଷ)", "Date (Day / Month / Year)"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = {
                    val t = DateUtils.today()
                    onDateChanged(t.day, t.month, t.year)
                }
            ) {
                Text(L(language, "ଆଜି (Today)", "Today"))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Day
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showDayMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("day_picker_button")
                ) {
                    Text("${L(language, "ଦିନ", "Day")}: $clampedDay")
                }
                DropdownMenu(
                    expanded = showDayMenu,
                    onDismissRequest = { showDayMenu = false }
                ) {
                    (1..maxDays).forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d.toString()) },
                            onClick = {
                                showDayMenu = false
                                onDateChanged(d, month, year)
                            }
                        )
                    }
                }
            }

            // Month
            Box(modifier = Modifier.weight(1.3f)) {
                OutlinedButton(
                    onClick = { showMonthMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("month_picker_button")
                ) {
                    Text(
                        text = DateUtils.monthLabel(month, language),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = showMonthMenu,
                    onDismissRequest = { showMonthMenu = false }
                ) {
                    (1..12).forEach { m ->
                        DropdownMenuItem(
                            text = { Text("$m - ${DateUtils.monthLabel(m, language)}") },
                            onClick = {
                                showMonthMenu = false
                                val newMax = DateUtils.daysInMonth(m, year)
                                onDateChanged(clampedDay.coerceAtMost(newMax), m, year)
                            }
                        )
                    }
                }
            }

            // Year
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showYearMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("year_picker_button")
                ) {
                    Text(year.toString())
                }
                DropdownMenu(
                    expanded = showYearMenu,
                    onDismissRequest = { showYearMenu = false }
                ) {
                    (2024..2030).forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = {
                                showYearMenu = false
                                val newMax = DateUtils.daysInMonth(month, y)
                                onDateChanged(clampedDay.coerceAtMost(newMax), month, y)
                            }
                        )
                    }
                }
            }
        }
    }
}

fun stageForegroundColor(stage: NepStage): Color = when (stage) {
    NepStage.FOUNDATIONAL -> StageFoundationalColor
    NepStage.PREPARATORY -> StagePreparatoryColor
    NepStage.MIDDLE -> StageMiddleColor
    NepStage.SECONDARY -> StageSecondaryColor
}

fun stageBackgroundColor(stage: NepStage): Color = when (stage) {
    NepStage.FOUNDATIONAL -> StageFoundationalBg
    NepStage.PREPARATORY -> StagePreparatoryBg
    NepStage.MIDDLE -> StageMiddleBg
    NepStage.SECONDARY -> StageSecondaryBg
}

@Composable
fun StageBadgeChip(stage: NepStage, language: AppLanguage) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = stageBackgroundColor(stage)
    ) {
        Text(
            text = if (language == AppLanguage.ODIA) stage.odiaName else stage.englishName,
            style = MaterialTheme.typography.labelSmall,
            color = stageForegroundColor(stage),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StageFilterRow(
    selectedStage: NepStage?,
    language: AppLanguage,
    onSelectStage: (NepStage?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStage == null,
            onClick = { onSelectStage(null) },
            label = { Text(L(language, "ସମସ୍ତ ସ୍ତର", "All Stages")) }
        )
        NepStage.entries.forEach { stage ->
            FilterChip(
                selected = selectedStage == stage,
                onClick = { onSelectStage(if (selectedStage == stage) null else stage) },
                label = { Text(if (language == AppLanguage.ODIA) stage.odiaName.substringBefore(" (") else stage.englishName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = stageBackgroundColor(stage),
                    selectedLabelColor = stageForegroundColor(stage)
                )
            )
        }
    }
}

@Composable
fun ReportDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L(language, "ବିଷୟବସ୍ତୁ ରିପୋର୍ଟ କରନ୍ତୁ", "Report Content")) },
        text = {
            Column {
                Text(
                    text = L(
                        language,
                        "ଅନୁପଯୁକ୍ତ ବା ଭୁଲ ତଥ୍ୟ ବିଷୟରେ କାରଣ ଲେଖନ୍ତୁ:",
                        "Describe why this content violates community or curriculum guidelines:"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(L(language, "କାରଣ", "Reason")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reason.isNotBlank()) onSubmit(reason) },
                enabled = reason.isNotBlank()
            ) {
                Text(L(language, "ପଠାନ୍ତୁ", "Submit"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(L(language, "ବାତିଲ୍", "Cancel"))
            }
        }
    )
}
