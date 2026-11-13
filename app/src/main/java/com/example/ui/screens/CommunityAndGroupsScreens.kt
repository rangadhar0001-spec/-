package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.AppLanguage
import com.example.domain.model.CommunityPost
import com.example.domain.model.L
import com.example.domain.model.LessonPlan
import com.example.domain.model.NepStage
import com.example.domain.model.OdishaCurriculum
import com.example.domain.model.PostComment
import com.example.domain.model.PublicProfile
import com.example.domain.model.TeacherGroup
import com.example.ui.components.DayMonthYearPickerRow
import com.example.ui.components.ReportDialog
import com.example.ui.components.StageBadgeChip
import com.example.ui.components.StageFilterRow
import com.example.ui.theme.KonarkSaffron
import com.example.ui.viewmodel.UiState
import com.example.util.DateUtils

/**
 * Screen 8: Community Feed for Odisha Teachers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    language: AppLanguage,
    currentUserId: String,
    blockedUserIds: List<String>,
    postsState: UiState<List<CommunityPost>>,
    comments: List<PostComment>,
    onOpenCreatePost: () -> Unit,
    onToggleLike: (CommunityPost) -> Unit,
    onOpenComments: (String?) -> Unit,
    onAddComment: (CommunityPost, String) -> Unit,
    onBookmarkPost: (CommunityPost) -> Unit,
    onFollowAuthor: (String) -> Unit,
    onBlockUser: (String) -> Unit,
    onReportPost: (postId: String, reason: String) -> Unit,
    onDeletePost: (String) -> Unit,
    onSeedSampleCommunity: () -> Unit
) {
    val context = LocalContext.current
    var stageFilter by remember { mutableStateOf<NepStage?>(null) }
    var districtFilter by remember { mutableStateOf(OdishaCurriculum.districts.first()) }
    var showDistrictMenu by remember { mutableStateOf(false) }

    var commentingPost by remember { mutableStateOf<CommunityPost?>(null) }
    var commentDraft by remember { mutableStateOf("") }
    var reportingPostId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filters Row
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                StageFilterRow(
                    selectedStage = stageFilter,
                    language = language,
                    onSelectStage = { stageFilter = it }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedButton(onClick = { showDistrictMenu = true }) {
                        Text("${L(language, "ଜିଲ୍ଲା", "District")}: $districtFilter", maxLines = 1)
                    }
                    DropdownMenu(expanded = showDistrictMenu, onDismissRequest = { showDistrictMenu = false }) {
                        OdishaCurriculum.districts.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d) },
                                onClick = {
                                    districtFilter = d
                                    showDistrictMenu = false
                                }
                            )
                        }
                    }
                }
            }

            when (postsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(postsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val filteredPosts = postsState.data.filter { post ->
                        post.userId !in blockedUserIds &&
                            (stageFilter == null || post.stage == stageFilter?.key) &&
                            (districtFilter.startsWith("All Odisha") || post.district == districtFilter)
                    }
                    if (filteredPosts.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = L(language, "ସମୁଦାୟ ଫିଡ୍ ଖାଲି ଅଛି", "No community posts match your filters"),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onSeedSampleCommunity) {
                                Text(L(language, "ନମୁନା ପୋଷ୍ଟ ଓ ଦଳ ଯୋଡ଼ନ୍ତୁ", "Load Odisha Sample Post & Group"))
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredPosts, key = { it.id }) { post ->
                                CommunityPostCard(
                                    post = post,
                                    isOwnPost = post.userId == currentUserId,
                                    language = language,
                                    onLike = { onToggleLike(post) },
                                    onComment = {
                                        commentingPost = post
                                        onOpenComments(post.id)
                                    },
                                    onBookmark = { onBookmarkPost(post) },
                                    onShare = {
                                        val shareText = "${post.title}\n\n${post.content}\n— ${post.authorName} (${post.district})"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Post"))
                                    },
                                    onFollow = { onFollowAuthor(post.userId) },
                                    onBlock = { onBlockUser(post.userId) },
                                    onReport = { reportingPostId = post.id },
                                    onDelete = { onDeletePost(post.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenCreatePost,
            containerColor = KonarkSaffron,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("community_create_post_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = L(language, "ନୂତନ ପୋଷ୍ଟ", "New Post"))
        }
    }

    if (commentingPost != null) {
        ModalBottomSheet(
            onDismissRequest = {
                commentingPost = null
                onOpenComments(null)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = L(language, "ମନ୍ତବ୍ୟ (Comments)", "Comments"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (comments.isEmpty()) {
                    Text(
                        text = L(language, "ପ୍ରଥମ ମନ୍ତବ୍ୟ ଦିଅନ୍ତୁ", "Be the first to comment"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    comments.forEach { c ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(c.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(c.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text(L(language, "ମନ୍ତବ୍ୟ ଲେଖନ୍ତୁ...", "Write a comment...")) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val target = commentingPost ?: return@IconButton
                            if (commentDraft.isNotBlank()) {
                                onAddComment(target, commentDraft)
                                commentDraft = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (reportingPostId != null) {
        ReportDialog(
            language = language,
            onDismiss = { reportingPostId = null },
            onSubmit = { reason ->
                reportingPostId?.let { onReportPost(it, reason) }
                reportingPostId = null
            }
        )
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    isOwnPost: Boolean,
    language: AppLanguage,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onFollow: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.firstOrNull()?.uppercase() ?: "T",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(post.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${post.authorSchool} • ${post.district} • ${DateUtils.formatDate(post.planDay.toInt(), post.planMonth.toInt(), post.planYear.toInt(), language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!isOwnPost) {
                            DropdownMenuItem(
                                text = { Text(L(language, "ଅନୁସରଣ କରନ୍ତୁ", "Follow Educator")) },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                                onClick = { showMenu = false; onFollow() }
                            )
                            DropdownMenuItem(
                                text = { Text(L(language, "ରିପୋର୍ଟ କରନ୍ତୁ", "Report Post")) },
                                leadingIcon = { Icon(Icons.Default.Flag, null) },
                                onClick = { showMenu = false; onReport() }
                            )
                            DropdownMenuItem(
                                text = { Text(L(language, "ବ୍ଲକ୍ କରନ୍ତୁ", "Block User")) },
                                onClick = { showMenu = false; onBlock() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(L(language, "ବିଲୋପ କରନ୍ତୁ", "Delete Post")) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StageBadgeChip(NepStage.fromKey(post.stage), language)
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = "${post.classLevel} • ${post.subject}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(post.content, style = MaterialTheme.typography.bodyMedium)

            if (!post.lessonPlanTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "📎 ${L(language, "ସଂଲଗ୍ନ ପାଠ୍ୟ ଯୋଜନା", "Attached Plan")}: ${post.lessonPlanTitle}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onLike) {
                    Icon(Icons.Default.Favorite, contentDescription = "Like", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.likesCount}")
                }
                TextButton(onClick = onComment) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${post.commentsCount}")
                }
                IconButton(onClick = onBookmark) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Bookmark")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        }
    }
}

/**
 * Screen 9: Create Post & Share Lesson Plan Screen (with zero-permission Android Photo Picker & Day/Month/Year).
 */
@Composable
fun CreatePostScreen(
    language: AppLanguage,
    userDistrict: String,
    myPlans: List<LessonPlan>,
    preselectedPlan: LessonPlan?,
    onPublishPost: (
        title: String,
        content: String,
        category: String,
        stage: NepStage,
        classLevel: String,
        subject: String,
        district: String,
        day: Int,
        month: Int,
        year: Int,
        lessonPlanId: String?,
        lessonPlanTitle: String?
    ) -> Unit
) {
    val today = remember { DateUtils.today() }
    var title by remember(preselectedPlan) { mutableStateOf(preselectedPlan?.title ?: "") }
    var content by remember(preselectedPlan) {
        mutableStateOf(
            if (preselectedPlan != null) {
                "${preselectedPlan.summary}\n\n${preselectedPlan.aditiStep}"
            } else {
                ""
            }
        )
    }
    var category by remember { mutableStateOf(if (preselectedPlan != null) "lesson_plan" else "teaching_idea") }
    var stage by remember(preselectedPlan) {
        mutableStateOf(preselectedPlan?.let { NepStage.fromKey(it.stage) } ?: NepStage.PREPARATORY)
    }
    var classLevel by remember(preselectedPlan) {
        mutableStateOf(preselectedPlan?.classLevel ?: stage.classesList.first())
    }
    var subject by remember(preselectedPlan) {
        mutableStateOf(preselectedPlan?.subject ?: (OdishaCurriculum.subjectsByStage[stage]?.firstOrNull() ?: "Odia"))
    }
    var district by remember { mutableStateOf(userDistrict.ifBlank { OdishaCurriculum.districts[1] }) }
    var planDay by remember(preselectedPlan) { mutableIntStateOf(preselectedPlan?.planDay?.toInt() ?: today.day) }
    var planMonth by remember(preselectedPlan) { mutableIntStateOf(preselectedPlan?.planMonth?.toInt() ?: today.month) }
    var planYear by remember(preselectedPlan) { mutableIntStateOf(preselectedPlan?.planYear?.toInt() ?: today.year) }
    var attachedPlan by remember(preselectedPlan) { mutableStateOf(preselectedPlan) }

    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        pickedImageUri = uri
    }

    var showPlanMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = L(language, "ନୂତନ ପୋଷ୍ଟ ଓ ପାଠ୍ୟ ଯୋଜନା ଅଂଶୀଦାର", "Create Post & Share Plan"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "lesson_plan" to L(language, "ପାଠ୍ୟ ଯୋଜନା", "Lesson Plan"),
                    "teaching_idea" to L(language, "ଶିକ୍ଷଣ ଧାରଣା", "Teaching Idea"),
                    "activity" to L(language, "ଶ୍ରେଣୀ କାର୍ଯ୍ୟ", "Activity"),
                    "resource" to L(language, "ସମ୍ବଳ", "Resource")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(L(language, "ଶୀର୍ଷକ (Title)", "Post Title")) },
                modifier = Modifier.fillMaxWidth().testTag("create_post_title")
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(L(language, "ବିବରଣୀ (Content)", "Description / Ideas")) },
                modifier = Modifier.fillMaxWidth().testTag("create_post_content"),
                minLines = 4
            )
        }

        item {
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showPlanMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(attachedPlan?.title?.take(22) ?: L(language, "ପାଠ୍ୟ ଯୋଜନା ଯୋଡ଼ନ୍ତୁ", "Attach Plan"), maxLines = 1)
                    }
                    DropdownMenu(expanded = showPlanMenu, onDismissRequest = { showPlanMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(L(language, "କୌଣସି ନୁହେଁ", "None")) },
                            onClick = { attachedPlan = null; showPlanMenu = false }
                        )
                        myPlans.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.title.ifBlank { p.topic }) },
                                onClick = {
                                    attachedPlan = p
                                    stage = NepStage.fromKey(p.stage)
                                    classLevel = p.classLevel
                                    subject = p.subject
                                    showPlanMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L(language, "ଫଟୋ ବାଛନ୍ତୁ", "Photo Picker"))
                }
            }

            if (pickedImageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = pickedImageUri,
                    contentDescription = "Selected classroom TLM preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        item {
            Button(
                onClick = {
                    onPublishPost(
                        title,
                        content,
                        category,
                        stage,
                        classLevel,
                        subject,
                        district,
                        planDay,
                        planMonth,
                        planYear,
                        attachedPlan?.id,
                        attachedPlan?.title
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_post_button")
            ) {
                Text(L(language, "ସମୁଦାୟରେ ପ୍ରକାଶ କରନ୍ତୁ", "Publish to Community"))
            }
        }
    }
}

/**
 * Screen 10: Search, Subjects & Teacher Groups Screen.
 */
@Composable
fun SearchAndGroupsScreen(
    language: AppLanguage,
    groups: List<TeacherGroup>,
    educators: List<PublicProfile>,
    publishedPlans: List<LessonPlan>,
    onOpenPlan: (LessonPlan) -> Unit,
    onJoinOrLeaveGroup: (String) -> Unit,
    onCreateGroup: (name: String, desc: String, stage: NepStage, subject: String, district: String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStage by remember { mutableStateOf<NepStage?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(L(language, "ବିଷୟ, ଜିଲ୍ଲା, ଦଳ ବା ପାଠ୍ୟ ଯୋଜନା ଖୋଜନ୍ତୁ...", "Search subjects, districts, groups, plans...")) },
                modifier = Modifier.fillMaxWidth().testTag("search_groups_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            StageFilterRow(
                selectedStage = selectedStage,
                language = language,
                onSelectStage = { selectedStage = it }
            )
        }

        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text(L(language, "ଶିକ୍ଷକ ଦଳ (${groups.size})", "Groups")) })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text(L(language, "ପ୍ରକାଶିତ ଯୋଜନା", "Plans")) })
            Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text(L(language, "ଶିକ୍ଷକବୃନ୍ଦ", "Educators")) })
        }

        when (tabIndex) {
            0 -> {
                val filteredGroups = groups.filter { g ->
                    (selectedStage == null || g.stage == selectedStage?.key) &&
                        (searchQuery.isBlank() || g.name.contains(searchQuery, true) || g.subject.contains(searchQuery, true) || g.district.contains(searchQuery, true))
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedButton(
                            onClick = { showCreateGroupDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(L(language, "ନୂତନ ବିଷୟ/ଜିଲ୍ଲା ଦଳ ଗଠନ କରନ୍ତୁ", "Create New Teacher Group"))
                        }
                    }
                    items(filteredGroups, key = { it.id }) { group ->
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(group.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${group.subject} • ${group.district} • ${group.memberCount} members", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(onClick = { onJoinOrLeaveGroup(group.id) }) {
                                    Text(L(language, "ଯୋଗ/ଛାଡ଼", "Join"))
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val filteredPlans = publishedPlans.filter { p ->
                    (selectedStage == null || p.stage == selectedStage?.key) &&
                        (searchQuery.isBlank() || p.title.contains(searchQuery, true) || p.subject.contains(searchQuery, true) || p.topic.contains(searchQuery, true))
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPlans, key = { it.id }) { plan ->
                        LessonPlanSummaryCard(plan = plan, language = language, onClick = { onOpenPlan(plan) })
                    }
                }
            }
            2 -> {
                val filteredEducators = educators.filter { e ->
                    searchQuery.isBlank() || e.displayName.contains(searchQuery, true) || e.district.contains(searchQuery, true) || e.schoolName.contains(searchQuery, true)
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEducators, key = { it.id }) { edu ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(edu.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${edu.schoolName} • ${edu.district}", style = MaterialTheme.typography.bodySmall)
                                Text(edu.subjects.joinToString(", "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var stage by remember { mutableStateOf(NepStage.PREPARATORY) }
        var subject by remember { mutableStateOf("ଓଡ଼ିଆ ସାହିତ୍ୟ (Odia Language)") }
        var district by remember { mutableStateOf(OdishaCurriculum.districts.first()) }
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text(L(language, "ନୂତନ ଶିକ୍ଷକ ଦଳ", "New Teacher Group")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(L(language, "ଦଳର ନାମ", "Group Name")) })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(L(language, "ବିବରଣୀ", "Description")) })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateGroup(name, desc, stage, subject, district)
                        showCreateGroupDialog = false
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(L(language, "ଗଠନ କରନ୍ତୁ", "Create"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text(L(language, "ବାତିଲ୍", "Cancel"))
                }
            }
        )
    }
}
