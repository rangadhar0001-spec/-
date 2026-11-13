package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Column
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.repository.AiLessonPlanRepository
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.CommunityRepository
import com.example.data.repository.LanguagePreferences
import com.example.data.repository.LessonPlanRepository
import com.example.data.repository.LessonProgressRepository
import com.example.data.repository.OfflineLessonPlanRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.L
import com.example.domain.model.LessonPlan
import com.example.domain.model.NepProgressEvaluator
import com.example.firebase.authStateFlow
import com.example.firebase.signOutUser
import com.example.ui.components.EducatorPersonalTopBar
import com.example.ui.components.OfflineSyncStatusBanner
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CommunityFeedScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LessonPlanEditorScreen
import com.example.ui.screens.LessonPlanLibraryScreen
import com.example.ui.screens.NepNavigatorScreen
import com.example.ui.screens.NepProgressTrackerScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingAuthScreen
import com.example.ui.screens.PanchapadiGeneratorScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.SearchAndGroupsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.LessonPlanViewModel
import com.example.ui.viewmodel.MainProfileViewModel
import com.example.ui.viewmodel.NepProgressViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.AndroidNetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val NEP_NAV = "nep_navigator"
    const val GENERATOR = "panchapadi_generator"
    const val EDITOR = "lesson_plan_editor"
    const val LIBRARY = "lesson_plan_library"
    const val COMMUNITY = "community_feed"
    const val CREATE_POST = "create_post"
    const val SEARCH_GROUPS = "search_groups"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile_settings"
    const val ADMIN = "admin_dashboard"
    const val PROGRESS = "nep_progress_tracker"
}

private data class NavTab(
    val route: String,
    val labelOdia: String,
    val labelEn: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languagePrefs = remember { LanguagePreferences(context.applicationContext) }
    val unauthLanguage by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ODIA)

    var showSplash by remember { mutableStateOf(true) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        OnboardingAuthScreen(
            language = unauthLanguage,
            onToggleLanguage = {
                val next = if (unauthLanguage == AppLanguage.ODIA) AppLanguage.ENGLISH else AppLanguage.ODIA
                scope.launch { languagePrefs.setLanguage(next) }
            },
            onAuthSuccess = { /* authStateFlow emits signed-in user automatically */ }
        )
    } else {
        androidx.compose.runtime.key(user.uid) {
            AuthenticatedAppShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "ଶିକ୍ଷକ ସାଥୀ",
                userEmail = user.email
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profileVm: MainProfileViewModel = viewModel(
        key = "profile_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                MainProfileViewModel(
                    authProfileRepo = AuthProfileRepository(db),
                    languagePrefs = LanguagePreferences(app),
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName,
                    initialEmail = userEmail
                )
            }
        }
    )

    val networkMonitor = remember(context) { AndroidNetworkMonitor(context.applicationContext) }

    val lessonVm: LessonPlanViewModel = viewModel(
        key = "lessons_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                val remoteRepo = LessonPlanRepository(db)
                val dao = AppDatabase.getInstance(app).offlineLessonPlanDao()
                val offlineRepo = OfflineLessonPlanRepository(
                    dao = dao,
                    remoteRepo = remoteRepo,
                    connectivity = networkMonitor
                )
                LessonPlanViewModel(
                    lessonPlanRepo = remoteRepo,
                    aiRepo = AiLessonPlanRepository(),
                    currentUserId = currentUserId,
                    offlineRepo = offlineRepo,
                    connectivity = networkMonitor
                )
            }
        }
    )

    val communityVm: CommunityViewModel = viewModel(
        key = "community_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                CommunityViewModel(
                    communityRepo = CommunityRepository(db),
                    authProfileRepo = AuthProfileRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val progressVm: NepProgressViewModel = viewModel(
        key = "progress_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                val progressDao = AppDatabase.getInstance(app).lessonProgressDao()
                NepProgressViewModel(
                    progressRepo = LessonProgressRepository(
                        db = db,
                        dao = progressDao,
                        connectivity = networkMonitor
                    ),
                    currentUserId = currentUserId,
                    connectivity = networkMonitor
                )
            }
        }
    )

    val language by profileVm.language.collectAsStateWithLifecycle()
    val userProfile by profileVm.userProfile.collectAsStateWithLifecycle()
    val isAdmin by profileVm.isAdmin.collectAsStateWithLifecycle()
    val profileMsg by profileVm.statusMessage.collectAsStateWithLifecycle()

    val myPlansState by lessonVm.myPlansState.collectAsStateWithLifecycle()
    val publishedPlansState by lessonVm.publishedPlansState.collectAsStateWithLifecycle()
    val panchapadiConfig by lessonVm.panchapadiConfig.collectAsStateWithLifecycle()
    val aiGenState by lessonVm.aiGenState.collectAsStateWithLifecycle()
    val regeneratingSection by lessonVm.regeneratingSection.collectAsStateWithLifecycle()
    val activePlan by lessonVm.activePlan.collectAsStateWithLifecycle()
    val lessonMsg by lessonVm.actionMessage.collectAsStateWithLifecycle()
    val selectedStage by lessonVm.selectedStage.collectAsStateWithLifecycle()
    val selectedClass by lessonVm.selectedClass.collectAsStateWithLifecycle()
    val selectedSubject by lessonVm.selectedSubject.collectAsStateWithLifecycle()
    val isOnline by lessonVm.isOnline.collectAsStateWithLifecycle()
    val downloadedPlans by lessonVm.downloadedPlans.collectAsStateWithLifecycle()
    val downloadedPlanIds by lessonVm.downloadedPlanIds.collectAsStateWithLifecycle()
    val pendingSyncIds by lessonVm.pendingSyncIds.collectAsStateWithLifecycle()
    val pendingSyncCount by lessonVm.pendingSyncCount.collectAsStateWithLifecycle()
    val isSyncing by lessonVm.isSyncing.collectAsStateWithLifecycle()

    LaunchedEffect(language) {
        lessonVm.setDisplayLanguage(language)
    }

    var libraryInitialTab by remember { mutableStateOf(0) }

    val postsState by communityVm.postsState.collectAsStateWithLifecycle()
    val bookmarks by communityVm.bookmarks.collectAsStateWithLifecycle()
    val notifications by communityVm.notifications.collectAsStateWithLifecycle()
    val groups by communityVm.groups.collectAsStateWithLifecycle()
    val educators by communityVm.educators.collectAsStateWithLifecycle()
    val reports by communityVm.reports.collectAsStateWithLifecycle()
    val comments by communityVm.commentsForPost.collectAsStateWithLifecycle()
    val communityMsg by communityVm.communityMessage.collectAsStateWithLifecycle()
    val progressByPlanId by progressVm.progressByPlanId.collectAsStateWithLifecycle()
    val selectedProgressPlanId by progressVm.selectedPlanId.collectAsStateWithLifecycle()
    val pendingProgressSyncCount by progressVm.pendingProgressSyncCount.collectAsStateWithLifecycle()
    val progressMsg by progressVm.progressMessage.collectAsStateWithLifecycle()

    var postAttachPlan by remember { mutableStateOf<LessonPlan?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(profileMsg, lessonMsg, communityMsg, progressMsg) {
        val msg = profileMsg ?: lessonMsg ?: communityMsg ?: progressMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            profileVm.clearStatusMessage()
            lessonVm.clearActionMessage()
            communityVm.clearMessage()
            progressVm.clearMessage()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = remember {
        listOf(
            NavTab(Routes.HOME, "ମୁଖ୍ୟ", "Home", Icons.Default.Home),
            NavTab(Routes.NEP_NAV, "5+3+3+4", "NEP", Icons.Default.AccountTree),
            NavTab(Routes.GENERATOR, "ପଞ୍ଚପଦୀ", "Create", Icons.Default.AutoAwesome),
            NavTab(Routes.LIBRARY, "ଭଣ୍ଡାର", "Library", Icons.AutoMirrored.Filled.LibraryBooks),
            NavTab(Routes.COMMUNITY, "ସମୁଦାୟ", "Community", Icons.Default.Groups)
        )
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val unreadCount = notifications.count { !it.isRead }
    val authorName = userProfile?.displayName?.ifBlank { userDisplayName } ?: userDisplayName
    val authorRole = userProfile?.role ?: "teacher"
    val authorSchool = userProfile?.schoolName ?: "Odisha School"
    val authorDistrict = userProfile?.district ?: "Khordha (ଖୋର୍ଦ୍ଧା)"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                Column {
                    EducatorPersonalTopBar(
                        profile = userProfile,
                        fallbackName = userDisplayName,
                        isAdmin = isAdmin,
                        language = language,
                        unreadNotifications = unreadCount,
                        onToggleLanguage = { profileVm.toggleLanguage() },
                        onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                        onOpenProfile = { navController.navigate(Routes.PROFILE) }
                    )
                    OfflineSyncStatusBanner(
                        isOnline = isOnline,
                        pendingSyncCount = pendingSyncCount + pendingProgressSyncCount,
                        isSyncing = isSyncing,
                        language = language,
                        onSyncNow = {
                            lessonVm.syncNow(language)
                            progressVm.syncNow()
                        },
                        onOpenOfflineLibrary = {
                            libraryInitialTab = 2
                            navigateTopTab(Routes.LIBRARY)
                        }
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.labelEn) },
                                label = {
                                    Text(
                                        text = if (language == AppLanguage.ODIA) tab.labelOdia else tab.labelEn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.labelEn) },
                                label = { Text(if (language == AppLanguage.ODIA) tab.labelOdia else tab.labelEn) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()
                    ) {
                        composable(Routes.HOME) {
                            val myPlans = (myPlansState as? UiState.Success)?.data ?: emptyList()
                            val progressSummary = remember(myPlans, progressByPlanId) {
                                NepProgressEvaluator.aggregateSummary(myPlans, progressByPlanId)
                            }
                            val outcomesSummaryText = L(
                                language,
                                "${progressSummary.achievedOutcomes}/${progressSummary.totalOutcomes} ଶିକ୍ଷଣ ଫଳାଫଳ ଅର୍ଜିତ • ${progressSummary.trackedPlansCount} ଯୋଜନା",
                                "${progressSummary.achievedOutcomes}/${progressSummary.totalOutcomes} outcomes achieved • ${progressSummary.trackedPlansCount} tracked"
                            )
                            HomeDashboardScreen(
                                language = language,
                                myPlansState = myPlansState,
                                downloadedPlanIds = downloadedPlanIds,
                                pendingSyncIds = pendingSyncIds,
                                overallProgressPercent = progressSummary.averageExecutionPercent,
                                outcomesSummaryText = outcomesSummaryText,
                                onOpenNepNavigator = { navigateTopTab(Routes.NEP_NAV) },
                                onOpenGenerator = { stage ->
                                    if (stage != null) lessonVm.setStageAndDefaults(stage)
                                    navigateTopTab(Routes.GENERATOR)
                                },
                                onOpenLibrary = {
                                    libraryInitialTab = 0
                                    navigateTopTab(Routes.LIBRARY)
                                },
                                onOpenCommunity = { navigateTopTab(Routes.COMMUNITY) },
                                onOpenProgressTracker = {
                                    progressVm.selectPlanForTracking(null)
                                    navController.navigate(Routes.PROGRESS)
                                },
                                onOpenSearchGroups = { navController.navigate(Routes.SEARCH_GROUPS) },
                                onOpenPlanEditor = { plan ->
                                    lessonVm.selectPlanForEditing(plan)
                                    navController.navigate(Routes.EDITOR)
                                },
                                onSeedSamples = { lessonVm.seedSamplePlans(authorName) }
                            )
                        }

                        composable(Routes.NEP_NAV) {
                            NepNavigatorScreen(
                                language = language,
                                onCreatePlanForSelection = { stage, cls, subj ->
                                    lessonVm.setStageAndDefaults(stage, cls, subj)
                                    navigateTopTab(Routes.GENERATOR)
                                }
                            )
                        }

                        composable(Routes.GENERATOR) {
                            PanchapadiGeneratorScreen(
                                language = language,
                                initialStage = selectedStage,
                                initialClass = selectedClass,
                                initialSubject = selectedSubject,
                                panchapadiConfig = panchapadiConfig,
                                aiGenState = aiGenState,
                                onGenerateAi = { stage, cls, subj, medium, topic, outcomes, duration, d, m, y ->
                                    lessonVm.generateWithAi(
                                        authorName = authorName,
                                        stage = stage,
                                        classLevel = cls,
                                        subject = subj,
                                        medium = medium,
                                        topic = topic,
                                        customOutcomes = outcomes,
                                        durationMinutes = duration,
                                        planDay = d,
                                        planMonth = m,
                                        planYear = y,
                                        outputLanguage = language,
                                        onGeneratedReady = {
                                            navController.navigate(Routes.EDITOR)
                                        }
                                    )
                                },
                                onCreateManual = {
                                    lessonVm.createBlankPlan(authorName)
                                    navController.navigate(Routes.EDITOR)
                                },
                                onDismissError = { lessonVm.clearAiError() }
                            )
                        }

                        composable(Routes.EDITOR) {
                            val currentId = activePlan?.id ?: ""
                            LessonPlanEditorScreen(
                                plan = activePlan,
                                panchapadiConfig = panchapadiConfig,
                                language = language,
                                regeneratingSection = regeneratingSection,
                                isOnline = isOnline,
                                isDownloadedOffline = currentId.isNotBlank() && currentId in downloadedPlanIds,
                                isPendingSync = currentId.isNotBlank() && currentId in pendingSyncIds,
                                onUpdatePlan = { transform -> lessonVm.updateActivePlan(transform) },
                                onRegenerateSection = { key, title, current, apply ->
                                    lessonVm.regenerateSingleSection(key, title, current, language, apply)
                                },
                                onSavePlan = { publish -> lessonVm.saveActivePlan(publish, language) },
                                onToggleOfflineDownload = { plan -> lessonVm.toggleOfflineDownload(plan, language) },
                                onTrackProgress = { plan ->
                                    progressVm.selectPlanForTracking(plan.id)
                                    navController.navigate(Routes.PROGRESS)
                                },
                                onShareToCommunity = { plan ->
                                    postAttachPlan = plan
                                    navController.navigate(Routes.CREATE_POST)
                                }
                            )
                        }

                        composable(Routes.LIBRARY) {
                            LessonPlanLibraryScreen(
                                language = language,
                                myPlansState = myPlansState,
                                publishedPlansState = publishedPlansState,
                                downloadedPlans = downloadedPlans,
                                downloadedPlanIds = downloadedPlanIds,
                                pendingSyncIds = pendingSyncIds,
                                pendingSyncCount = pendingSyncCount,
                                isOnline = isOnline,
                                isSyncing = isSyncing,
                                bookmarks = bookmarks,
                                initialTab = libraryInitialTab,
                                onOpenPlan = { plan ->
                                    lessonVm.selectPlanForEditing(plan)
                                    navController.navigate(Routes.EDITOR)
                                },
                                onDeletePlan = { id -> lessonVm.deletePlan(id, language) },
                                onToggleOfflineDownload = { plan -> lessonVm.toggleOfflineDownload(plan, language) },
                                onTrackProgress = { plan ->
                                    progressVm.selectPlanForTracking(plan.id)
                                    navController.navigate(Routes.PROGRESS)
                                },
                                onSyncNow = { lessonVm.syncNow(language) },
                                onBookmarkPlan = { plan ->
                                    communityVm.toggleBookmark(
                                        targetId = plan.id,
                                        targetType = "lesson_plan",
                                        title = plan.title.ifBlank { plan.topic },
                                        subject = plan.subject,
                                        lang = language
                                    )
                                },
                                onSeedSamples = { lessonVm.seedSamplePlans(authorName) }
                            )
                        }

                        composable(Routes.COMMUNITY) {
                            CommunityFeedScreen(
                                language = language,
                                currentUserId = currentUserId,
                                blockedUserIds = userProfile?.blockedUserIds ?: emptyList(),
                                postsState = postsState,
                                comments = comments,
                                onOpenCreatePost = {
                                    postAttachPlan = null
                                    navController.navigate(Routes.CREATE_POST)
                                },
                                onToggleLike = { post -> communityVm.toggleLike(post, authorName) },
                                onOpenComments = { postId -> communityVm.openComments(postId) },
                                onAddComment = { post, text -> communityVm.addComment(post, authorName, text) },
                                onBookmarkPost = { post ->
                                    communityVm.toggleBookmark(post.id, "post", post.title, post.subject, language)
                                },
                                onFollowAuthor = { uid -> communityVm.followEducator(uid, language) },
                                onBlockUser = { uid -> profileVm.toggleBlockUser(uid) },
                                onReportPost = { postId, reason -> communityVm.reportContent(postId, "post", reason, language) },
                                onDeletePost = { postId -> communityVm.deletePost(postId) },
                                onSeedSampleCommunity = {
                                    communityVm.seedSyntheticCommunity(authorName, authorSchool, authorDistrict)
                                }
                            )
                        }

                        composable(Routes.CREATE_POST) {
                            val myPlansList = (myPlansState as? UiState.Success)?.data ?: emptyList()
                            CreatePostScreen(
                                language = language,
                                userDistrict = authorDistrict,
                                myPlans = myPlansList,
                                preselectedPlan = postAttachPlan,
                                onPublishPost = { title, content, cat, stage, cls, subj, dist, d, m, y, planId, planTitle ->
                                    communityVm.createPost(
                                        authorName = authorName,
                                        authorRole = authorRole,
                                        authorSchool = authorSchool,
                                        title = title,
                                        content = content,
                                        category = cat,
                                        stage = stage,
                                        classLevel = cls,
                                        subject = subj,
                                        district = dist,
                                        planDay = d,
                                        planMonth = m,
                                        planYear = y,
                                        lessonPlanId = planId,
                                        lessonPlanTitle = planTitle,
                                        lang = language,
                                        onCreated = {
                                            postAttachPlan = null
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            )
                        }

                        composable(Routes.SEARCH_GROUPS) {
                            val pubPlans = (publishedPlansState as? UiState.Success)?.data ?: emptyList()
                            SearchAndGroupsScreen(
                                language = language,
                                groups = groups,
                                educators = educators,
                                publishedPlans = pubPlans,
                                onOpenPlan = { plan ->
                                    lessonVm.selectPlanForEditing(plan)
                                    navController.navigate(Routes.EDITOR)
                                },
                                onJoinOrLeaveGroup = { gid -> communityVm.joinOrLeaveGroup(gid, authorName, language) },
                                onCreateGroup = { name, desc, stage, subj, dist ->
                                    communityVm.createGroup(name, desc, stage, subj, dist, authorName)
                                }
                            )
                        }

                        composable(Routes.NOTIFICATIONS) {
                            NotificationsScreen(
                                language = language,
                                notifications = notifications,
                                onMarkRead = { nid -> communityVm.markNotificationRead(nid) }
                            )
                        }

                        composable(Routes.PROFILE) {
                            ProfileSettingsScreen(
                                language = language,
                                profile = userProfile,
                                isAdmin = isAdmin,
                                panchapadiConfig = panchapadiConfig,
                                onToggleLanguage = { profileVm.toggleLanguage() },
                                onSaveProfile = { name, school, dist, subjs, vis ->
                                    profileVm.updateProfile(name, school, dist, subjs, vis)
                                },
                                onOpenAdminDashboard = { navController.navigate(Routes.ADMIN) },
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }

                        composable(Routes.ADMIN) {
                            val pubPlans = (publishedPlansState as? UiState.Success)?.data ?: emptyList()
                            AdminDashboardScreen(
                                language = language,
                                panchapadiConfig = panchapadiConfig,
                                reports = reports,
                                publishedPlans = pubPlans,
                                onSavePanchapadiConfig = { cfg -> lessonVm.savePanchapadiConfig(cfg) },
                                onVerifyPlan = { pid, verified -> lessonVm.verifyPlan(pid, verified) },
                                onModerateReport = { rid, status, hidePostId ->
                                    communityVm.moderateReport(rid, status, hidePostId)
                                }
                            )
                        }

                        composable(Routes.PROGRESS) {
                            val myPlansList = (myPlansState as? UiState.Success)?.data ?: emptyList()
                            val allTrackablePlans = remember(myPlansList, downloadedPlans) {
                                (myPlansList + downloadedPlans).distinctBy { it.id }
                            }
                            NepProgressTrackerScreen(
                                language = language,
                                plans = allTrackablePlans,
                                progressByPlanId = progressByPlanId,
                                panchapadiConfig = panchapadiConfig,
                                selectedPlanId = selectedProgressPlanId,
                                onSelectPlan = { pid -> progressVm.selectPlanForTracking(pid) },
                                onSaveProgress = { plan, updated -> progressVm.saveUpdatedProgress(plan, updated, language) },
                                onBuildOutcomeNext = { current, idx, status ->
                                    progressVm.buildNextProgressForOutcome(current, idx, status)
                                },
                                onOpenPlanEditor = { plan ->
                                    lessonVm.selectPlanForEditing(plan)
                                    navController.navigate(Routes.EDITOR)
                                },
                                onOpenGenerator = { navigateTopTab(Routes.GENERATOR) }
                            )
                        }
                    }
                }
            }
        }
    }
}
