package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.domain.model.ContentReport
import com.example.domain.model.L
import com.example.domain.model.LessonPlan
import com.example.domain.model.OdishaCurriculum
import com.example.domain.model.PanchapadiConfig
import com.example.domain.model.UserNotification
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.ui.theme.DangerRed

/**
 * Screen 11: Notifications Screen.
 */
@Composable
fun NotificationsScreen(
    language: AppLanguage,
    notifications: List<UserNotification>,
    onMarkRead: (String) -> Unit
) {
    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(L(language, "କୌଣସି ନୂତନ ସୂଚନା ନାହିଁ", "No notifications yet"))
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = L(language, "ସୂଚନା (Notifications)", "Notifications"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        items(notifications, key = { it.id }) { notif ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!notif.isRead) onMarkRead(notif.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(notif.senderName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * Screen 12: Teacher Profile, Privacy Controls, Panchapadi Step Customization & Settings.
 */
@Composable
fun ProfileSettingsScreen(
    language: AppLanguage,
    profile: UserProfile?,
    isAdmin: Boolean,
    panchapadiConfig: PanchapadiConfig,
    onToggleLanguage: () -> Unit,
    onSaveProfile: (displayName: String, schoolName: String, district: String, subjects: List<String>, visibility: String) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var schoolName by remember(profile) { mutableStateOf(profile?.schoolName ?: "") }
    var district by remember(profile) { mutableStateOf(profile?.district ?: OdishaCurriculum.districts[1]) }
    var visibility by remember(profile) { mutableStateOf(profile?.profileVisibility ?: "public") }
    var subjectsText by remember(profile) { mutableStateOf(profile?.subjects?.joinToString(", ") ?: "Odia, Mathematics") }
    var showDistrictMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = L(language, "ଶିକ୍ଷକ ପ୍ରୋଫାଇଲ୍ ଓ ସେଟିଂସ୍", "Educator Profile & Settings"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Personal Details Card
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${L(language, "ଭୂମିକା (Role)", "Role")}: ${UserRole.fromKey(profile?.role ?: "teacher").odiaLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(L(language, "ନାମ", "Display Name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text(L(language, "ବିଦ୍ୟାଳୟର ନାମ", "School Name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        OutlinedButton(onClick = { showDistrictMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("${L(language, "ଜିଲ୍ଲା", "District")}: $district")
                        }
                        DropdownMenu(expanded = showDistrictMenu, onDismissRequest = { showDistrictMenu = false }) {
                            OdishaCurriculum.districts.drop(1).forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = { district = d; showDistrictMenu = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = subjectsText,
                        onValueChange = { subjectsText = it },
                        label = { Text(L(language, "ବିଷୟସମୂହ (କମା ଦ୍ୱାରା ଅଲଗା)", "Subjects (comma-separated)")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = L(language, "ପ୍ରୋଫାଇଲ୍ ଗୋପନୀୟତା (Privacy Controls)", "Profile Privacy Visibility"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "public" to L(language, "ସାର୍ବଜନୀନ", "Public"),
                            "school_only" to L(language, "ବିଦ୍ୟାଳୟ", "School Only"),
                            "private" to L(language, "ଗୋପନୀୟ", "Private")
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = visibility == key,
                                onClick = { visibility = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val subjs = subjectsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onSaveProfile(displayName, schoolName, district, subjs, visibility)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                    ) {
                        Text(L(language, "ପ୍ରୋଫାଇଲ୍ ସୁରକ୍ଷିତ କରନ୍ତୁ", "Save Profile"))
                    }
                }
            }
        }

        // Language Preference
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(L(language, "ଆପ୍ ଭାଷା (App Language)", "App Language"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (language == AppLanguage.ODIA) "ଓଡ଼ିଆ (Default Primary)" else "English (Optional)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedButton(onClick = onToggleLanguage) {
                        Text(if (language == AppLanguage.ODIA) "Switch to English" else "ଓଡ଼ିଆକୁ ବଦଳାନ୍ତୁ")
                    }
                }
            }
        }

        // Panchapadi Active Labels Summary
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = L(language, "ସକ୍ରିୟ ପଞ୍ଚପଦୀ ଢାଞ୍ଚା", "Active Panchapadi Framework"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("1. ${panchapadiConfig.aditiLabel}", style = MaterialTheme.typography.bodySmall)
                    Text("2. ${panchapadiConfig.bodhaLabel}", style = MaterialTheme.typography.bodySmall)
                    Text("3. ${panchapadiConfig.abhyasaLabel}", style = MaterialTheme.typography.bodySmall)
                    Text("4. ${panchapadiConfig.prayogaLabel}", style = MaterialTheme.typography.bodySmall)
                    Text("5. ${panchapadiConfig.prasaraLabel}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Admin Dashboard Entry
        if (isAdmin) {
            item {
                Button(
                    onClick = onOpenAdminDashboard,
                    modifier = Modifier.fillMaxWidth().testTag("open_admin_dashboard_button")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L(language, "ଆଡମିନ୍ ଡ୍ୟାଶବୋର୍ଡ (Admin Dashboard)", "Open Admin Dashboard"))
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(L(language, "ସାଇନ୍ ଆଉଟ୍ (Sign Out)", "Sign Out"))
            }
        }
    }
}

/**
 * Screen 13: Admin Dashboard for Content Moderation, Plan Verification & Configurable Panchapadi Labels.
 */
@Composable
fun AdminDashboardScreen(
    language: AppLanguage,
    panchapadiConfig: PanchapadiConfig,
    reports: List<ContentReport>,
    publishedPlans: List<LessonPlan>,
    onSavePanchapadiConfig: (PanchapadiConfig) -> Unit,
    onVerifyPlan: (planId: String, verified: Boolean) -> Unit,
    onModerateReport: (reportId: String, status: String, hidePostId: String?) -> Unit
) {
    var aditiLabel by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.aditiLabel) }
    var aditiDesc by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.aditiDesc) }
    var bodhaLabel by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.bodhaLabel) }
    var bodhaDesc by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.bodhaDesc) }
    var abhyasaLabel by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.abhyasaLabel) }
    var abhyasaDesc by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.abhyasaDesc) }
    var prayogaLabel by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.prayogaLabel) }
    var prayogaDesc by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.prayogaDesc) }
    var prasaraLabel by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.prasaraLabel) }
    var prasaraDesc by remember(panchapadiConfig) { mutableStateOf(panchapadiConfig.prasaraDesc) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = L(language, "ଆଡମିନ୍ ପରିଚାଳନା ଡ୍ୟାଶବୋର୍ଡ", "Admin Content & Moderation Dashboard"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Configurable Panchapadi Step Labels
        item {
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = L(language, "ପଞ୍ଚପଦୀ ପଦକ୍ଷେପ ନାମ ଓ ବିବରଣୀ ସମ୍ପାଦନ", "Configure Panchapadi Labels & Descriptions"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(value = aditiLabel, onValueChange = { aditiLabel = it }, label = { Text("Step 1 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = aditiDesc, onValueChange = { aditiDesc = it }, label = { Text("Step 1 Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodhaLabel, onValueChange = { bodhaLabel = it }, label = { Text("Step 2 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodhaDesc, onValueChange = { bodhaDesc = it }, label = { Text("Step 2 Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = abhyasaLabel, onValueChange = { abhyasaLabel = it }, label = { Text("Step 3 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = abhyasaDesc, onValueChange = { abhyasaDesc = it }, label = { Text("Step 3 Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prayogaLabel, onValueChange = { prayogaLabel = it }, label = { Text("Step 4 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prayogaDesc, onValueChange = { prayogaDesc = it }, label = { Text("Step 4 Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prasaraLabel, onValueChange = { prasaraLabel = it }, label = { Text("Step 5 Label") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = prasaraDesc, onValueChange = { prasaraDesc = it }, label = { Text("Step 5 Description") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            onSavePanchapadiConfig(
                                panchapadiConfig.copy(
                                    aditiLabel = aditiLabel,
                                    aditiDesc = aditiDesc,
                                    bodhaLabel = bodhaLabel,
                                    bodhaDesc = bodhaDesc,
                                    abhyasaLabel = abhyasaLabel,
                                    abhyasaDesc = abhyasaDesc,
                                    prayogaLabel = prayogaLabel,
                                    prayogaDesc = prayogaDesc,
                                    prasaraLabel = prasaraLabel,
                                    prasaraDesc = prasaraDesc
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(L(language, "ପଞ୍ଚପଦୀ ସେଟିଂସ୍ ସୁରକ୍ଷିତ କରନ୍ତୁ", "Save Panchapadi Framework"))
                    }
                }
            }
        }

        // Reported Content Moderation Queue
        item {
            Text(
                text = L(language, "ରିପୋର୍ଟ ସମୀକ୍ଷା (${reports.size})", "Moderation Reports (${reports.size})"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (reports.isEmpty()) {
            item {
                Text(L(language, "କୌଣସି ବିଚାରାଧୀନ ରିପୋର୍ଟ ନାହିଁ", "No pending reports"))
            }
        } else {
            items(reports, key = { it.id }) { report ->
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${report.targetType}: ${report.targetId}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(report.reason, style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${report.status}", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onModerateReport(report.id, "dismissed", null) }) {
                                Text(L(language, "ଖାରଜ", "Dismiss"))
                            }
                            Button(
                                onClick = {
                                    onModerateReport(
                                        report.id,
                                        "action_taken",
                                        if (report.targetType == "post") report.targetId else null
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                            ) {
                                Text(L(language, "ପୋଷ୍ଟ ଲୁଚାନ୍ତୁ", "Hide Content"))
                            }
                        }
                    }
                }
            }
        }

        // Lesson Plan Verification Queue
        item {
            Text(
                text = L(language, "ପାଠ୍ୟ ଯୋଜନା ପ୍ରମାଣୀକରଣ (Verification)", "Lesson Plan Verification"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(publishedPlans, key = { it.id }) { plan ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(plan.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${plan.authorName} • ${plan.subject}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onVerifyPlan(plan.id, !plan.isVerified) }) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (plan.isVerified) L(language, "ପ୍ରମାଣିତ", "Verified") else L(language, "ପ୍ରମାଣ କରନ୍ତୁ", "Verify"))
                    }
                }
            }
        }
    }
}
