package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AiLessonPlanRepository
import com.example.data.repository.LessonPlanRepository
import com.example.data.repository.OfflineLessonPlanRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.LessonPlan
import com.example.domain.model.NepStage
import com.example.domain.model.PanchapadiConfig
import com.example.util.ConnectivityObserver
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiGenState {
    data object Idle : AiGenState
    data object Generating : AiGenState
    data class Error(val message: String) : AiGenState
}

class LessonPlanViewModel(
    private val lessonPlanRepo: LessonPlanRepository,
    private val aiRepo: AiLessonPlanRepository,
    private val currentUserId: String,
    private val offlineRepo: OfflineLessonPlanRepository? = null,
    private val connectivity: ConnectivityObserver? = null
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivity?.isOnline
        ?: MutableStateFlow(true).asStateFlow()

    val downloadedPlans: StateFlow<List<LessonPlan>> =
        (offlineRepo?.observeDownloadedPlans() ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val downloadedPlanIds: StateFlow<Set<String>> =
        (offlineRepo?.observeDownloadedPlanIds() ?: flowOf(emptySet()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val pendingSyncIds: StateFlow<Set<String>> =
        (offlineRepo?.observePendingSyncIds(currentUserId) ?: flowOf(emptySet()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val pendingSyncCount: StateFlow<Int> =
        (offlineRepo?.observePendingSyncCount(currentUserId) ?: flowOf(0))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val remoteMyPlansFlow = lessonPlanRepo.observeMyLessonPlans(currentUserId)
        .onEach { plans ->
            offlineRepo?.refreshDownloadedCopies(plans)
        }
        .map<List<LessonPlan>, UiState<List<LessonPlan>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing remote my lesson plans", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load lesson plans"))
        }

    private val localUserPlansFlow = offlineRepo?.observeUserLocalPlans(currentUserId)
        ?: flowOf(emptyList())

    val myPlansState: StateFlow<UiState<List<LessonPlan>>> = combine(
        remoteMyPlansFlow,
        localUserPlansFlow,
        pendingSyncIds,
        isOnline
    ) { remoteState, localPlans, pendingIds, online ->
        mergeRemoteAndLocalPlans(remoteState, localPlans, pendingIds, online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val remotePublishedPlansFlow = lessonPlanRepo.observePublishedLessonPlans()
        .onEach { plans ->
            offlineRepo?.refreshDownloadedCopies(plans)
        }
        .map<List<LessonPlan>, UiState<List<LessonPlan>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing remote published plans", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load published plans"))
        }

    private val localPublishedPlansFlow = offlineRepo?.observeDownloadedPublishedPlans()
        ?: flowOf(emptyList())

    val publishedPlansState: StateFlow<UiState<List<LessonPlan>>> = combine(
        remotePublishedPlansFlow,
        localPublishedPlansFlow,
        pendingSyncIds,
        isOnline
    ) { remoteState, localPlans, pendingIds, online ->
        mergeRemoteAndLocalPlans(remoteState, localPlans, pendingIds, online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val panchapadiConfig: StateFlow<PanchapadiConfig> = lessonPlanRepo.observePanchapadiConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PanchapadiConfig())

    private val _aiGenState = MutableStateFlow<AiGenState>(AiGenState.Idle)
    val aiGenState: StateFlow<AiGenState> = _aiGenState.asStateFlow()

    private val _regeneratingSection = MutableStateFlow<String?>(null)
    val regeneratingSection: StateFlow<String?> = _regeneratingSection.asStateFlow()

    private val _activePlan = MutableStateFlow<LessonPlan?>(null)
    val activePlan: StateFlow<LessonPlan?> = _activePlan.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // Generator form state (preserved across navigation)
    private val _selectedStage = MutableStateFlow(NepStage.PREPARATORY)
    val selectedStage: StateFlow<NepStage> = _selectedStage.asStateFlow()

    private val _selectedClass = MutableStateFlow(NepStage.PREPARATORY.classesList[1])
    val selectedClass: StateFlow<String> = _selectedClass.asStateFlow()

    private val _selectedSubject = MutableStateFlow("ଓଡ଼ିଆ ସାହିତ୍ୟ (Odia Language)")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private var activeLanguage: AppLanguage = AppLanguage.ODIA

    init {
        if (offlineRepo != null && connectivity != null) {
            viewModelScope.launch {
                connectivity.isOnline
                    .filter { it }
                    .collect {
                        syncPendingEditsInternal(notifyOnNothing = false)
                    }
            }
        }
    }

    fun setDisplayLanguage(lang: AppLanguage) {
        activeLanguage = lang
    }

    private fun mergeRemoteAndLocalPlans(
        remoteState: UiState<List<LessonPlan>>,
        localPlans: List<LessonPlan>,
        pendingIds: Set<String>,
        online: Boolean
    ): UiState<List<LessonPlan>> {
        val localById = localPlans.associateBy { it.id }
        return when (remoteState) {
            is UiState.Success -> {
                val mergedMap = LinkedHashMap<String, LessonPlan>()
                // Pending local edits or offline-created plans take precedence over remote snapshots
                for (local in localPlans) {
                    if (local.id in pendingIds || !online) {
                        mergedMap[local.id] = local
                    }
                }
                for (remote in remoteState.data) {
                    if (remote.id !in mergedMap) {
                        mergedMap[remote.id] = remote
                    }
                }
                for (local in localPlans) {
                    if (local.id !in mergedMap) {
                        mergedMap[local.id] = local
                    }
                }
                UiState.Success(mergedMap.values.toList())
            }
            is UiState.Loading -> {
                if (localPlans.isNotEmpty() || !online) {
                    UiState.Success(localPlans)
                } else {
                    UiState.Loading
                }
            }
            is UiState.Error -> {
                if (localPlans.isNotEmpty() || !online) {
                    UiState.Success(localById.values.toList())
                } else {
                    remoteState
                }
            }
        }
    }

    fun setStageAndDefaults(stage: NepStage, classChoice: String? = null, subjectChoice: String? = null) {
        _selectedStage.value = stage
        _selectedClass.value = classChoice ?: stage.classesList.first()
        _selectedSubject.value = subjectChoice ?: (com.example.domain.model.OdishaCurriculum.subjectsByStage[stage]?.firstOrNull() ?: "Odia")
    }

    fun selectPlanForEditing(plan: LessonPlan) {
        _activePlan.value = plan
    }

    fun createBlankPlan(authorName: String) {
        val today = DateUtils.today()
        _activePlan.value = LessonPlan(
            userId = currentUserId,
            authorName = authorName,
            title = "",
            stage = _selectedStage.value.key,
            classLevel = _selectedClass.value,
            subject = _selectedSubject.value,
            medium = "ଓଡ଼ିଆ ମାଧ୍ୟମ (Odia Medium)",
            topic = "",
            durationMinutes = 45L,
            planDay = today.day.toLong(),
            planMonth = today.month.toLong(),
            planYear = today.year.toLong(),
            isAiGenerated = false,
            isVerified = false,
            status = "draft"
        )
    }

    fun updateActivePlan(transform: (LessonPlan) -> LessonPlan) {
        val current = _activePlan.value ?: return
        _activePlan.value = transform(current)
    }

    fun generateWithAi(
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
        outputLanguage: AppLanguage,
        onGeneratedReady: () -> Unit
    ) {
        if (topic.isBlank()) {
            _aiGenState.value = AiGenState.Error(
                if (outputLanguage == AppLanguage.ODIA) "ଦୟାକରି ପାଠର ବିଷୟବସ୍ତୁ ଲେଖନ୍ତୁ" else "Please enter a lesson topic"
            )
            return
        }
        viewModelScope.launch {
            _aiGenState.value = AiGenState.Generating
            val res = aiRepo.generateFullLessonPlan(
                userId = currentUserId,
                authorName = authorName,
                stage = stage,
                classLevel = classLevel,
                subject = subject,
                medium = medium,
                topic = topic,
                customOutcomes = customOutcomes,
                durationMinutes = durationMinutes,
                planDay = planDay,
                planMonth = planMonth,
                planYear = planYear,
                panchapadiConfig = panchapadiConfig.value,
                outputLanguage = outputLanguage
            )
            res.onSuccess { generated ->
                _activePlan.value = generated
                _aiGenState.value = AiGenState.Idle
                onGeneratedReady()
            }.onFailure { err ->
                _aiGenState.value = AiGenState.Error(
                    err.localizedMessage ?: "AI generation failed. Check network or App Check."
                )
            }
        }
    }

    fun regenerateSingleSection(
        sectionKey: String,
        sectionTitle: String,
        currentText: String,
        outputLanguage: AppLanguage,
        applyResult: (LessonPlan, String) -> LessonPlan
    ) {
        val plan = _activePlan.value ?: return
        viewModelScope.launch {
            _regeneratingSection.value = sectionKey
            val res = aiRepo.regenerateSection(plan, sectionTitle, currentText, outputLanguage)
            res.onSuccess { newText ->
                _activePlan.value = applyResult(_activePlan.value ?: plan, newText)
                _actionMessage.value = if (outputLanguage == AppLanguage.ODIA) {
                    "$sectionTitle ପୁନଃନିର୍ମିତ ହେଲା"
                } else {
                    "$sectionTitle regenerated"
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Regeneration failed"
            }
            _regeneratingSection.value = null
        }
    }

    fun saveActivePlan(publish: Boolean, lang: AppLanguage) {
        activeLanguage = lang
        val plan = _activePlan.value ?: return
        val updatedStatus = if (publish) "published" else "draft"
        val planToSave = plan.copy(status = updatedStatus)
        viewModelScope.launch {
            if (offlineRepo != null) {
                val res = offlineRepo.savePlanOfflineFirst(
                    plan = planToSave,
                    currentUserId = currentUserId,
                    authorName = plan.authorName.ifBlank { "Educator" }
                )
                res.onSuccess { result ->
                    _activePlan.value = result.plan
                    _actionMessage.value = when {
                        !result.syncedImmediately -> {
                            if (lang == AppLanguage.ODIA) {
                                "ଅଫଲାଇନରେ ସୁରକ୍ଷିତ ହେଲା — ଇଣ୍ଟରନେଟ୍ ଆସିଲେ ସିଙ୍କ୍ ହେବ"
                            } else {
                                "Saved offline — will sync automatically when online"
                            }
                        }
                        publish -> {
                            if (lang == AppLanguage.ODIA) "ପାଠ୍ୟ ଯୋଜନା ପ୍ରକାଶିତ ହେଲା!" else "Lesson plan published!"
                        }
                        else -> {
                            if (lang == AppLanguage.ODIA) "ଡ୍ରାଫ୍ଟ ସୁରକ୍ଷିତ ହେଲା!" else "Draft saved!"
                        }
                    }
                }.onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Save failed"
                }
            } else {
                val ownedPlan = planToSave.copy(userId = currentUserId)
                val res = if (ownedPlan.id.isBlank()) {
                    lessonPlanRepo.createLessonPlan(ownedPlan).map { newId ->
                        _activePlan.value = ownedPlan.copy(id = newId)
                    }
                } else {
                    lessonPlanRepo.updateLessonPlan(ownedPlan).map {
                        _activePlan.value = ownedPlan
                    }
                }
                _actionMessage.value = if (res.isSuccess) {
                    if (publish) {
                        if (lang == AppLanguage.ODIA) "ପାଠ୍ୟ ଯୋଜନା ପ୍ରକାଶିତ ହେଲା!" else "Lesson plan published!"
                    } else {
                        if (lang == AppLanguage.ODIA) "ଡ୍ରାଫ୍ଟ ସୁରକ୍ଷିତ ହେଲା!" else "Draft saved!"
                    }
                } else {
                    res.exceptionOrNull()?.localizedMessage ?: "Save failed"
                }
            }
        }
    }

    fun toggleOfflineDownload(plan: LessonPlan, lang: AppLanguage) {
        activeLanguage = lang
        val repo = offlineRepo ?: return
        if (plan.id.isBlank()) {
            _actionMessage.value = if (lang == AppLanguage.ODIA) {
                "ପ୍ରଥମେ ପାଠ୍ୟ ଯୋଜନା ସୁରକ୍ଷିତ କରନ୍ତୁ"
            } else {
                "Save the lesson plan before downloading offline"
            }
            return
        }
        viewModelScope.launch {
            val isCurrentlyDownloaded = downloadedPlanIds.value.contains(plan.id)
            if (isCurrentlyDownloaded) {
                val res = repo.removeOfflineDownload(plan.id)
                res.onSuccess { removed ->
                    _actionMessage.value = if (removed) {
                        if (lang == AppLanguage.ODIA) "ଅଫଲାଇନ୍ କପି ହଟାଗଲା" else "Removed from offline storage"
                    } else {
                        if (lang == AppLanguage.ODIA) {
                            "ଅଫଲାଇନ୍ ପରିବର୍ତ୍ତନ ସିଙ୍କ୍ ବାକି ଅଛି — ପ୍ରଥମେ ସିଙ୍କ୍ କରନ୍ତୁ"
                        } else {
                            "Unsynced offline edits kept until synced"
                        }
                    }
                }
            } else {
                val res = repo.downloadForOffline(plan)
                res.onSuccess {
                    _actionMessage.value = if (lang == AppLanguage.ODIA) {
                        "ଅଫଲାଇନ୍ ପାଇଁ ଡିଭାଇସରେ ଡାଉନଲୋଡ୍ ହେଲା!"
                    } else {
                        "Downloaded to device for offline access!"
                    }
                }.onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Offline download failed"
                }
            }
        }
    }

    fun syncNow(lang: AppLanguage) {
        activeLanguage = lang
        viewModelScope.launch {
            syncPendingEditsInternal(notifyOnNothing = true)
        }
    }

    private suspend fun syncPendingEditsInternal(notifyOnNothing: Boolean) {
        val repo = offlineRepo ?: return
        if (_isSyncing.value) return
        _isSyncing.value = true
        try {
            val summary = repo.syncPendingPlans(currentUserId)
            val lang = activeLanguage
            when {
                summary.syncedCount > 0 && summary.failedCount == 0 -> {
                    _actionMessage.value = if (lang == AppLanguage.ODIA) {
                        "${summary.syncedCount}ଟି ଅଫଲାଇନ୍ ପରିବର୍ତ୍ତନ ସଫଳତାର ସହ ସିଙ୍କ୍ ହେଲା!"
                    } else {
                        "Synced ${summary.syncedCount} offline edit(s)!"
                    }
                }
                summary.syncedCount > 0 && summary.failedCount > 0 -> {
                    _actionMessage.value = if (lang == AppLanguage.ODIA) {
                        "${summary.syncedCount}ଟି ସିଙ୍କ୍ ହେଲା, ${summary.failedCount}ଟି ବାକି ଅଛି"
                    } else {
                        "Synced ${summary.syncedCount}, ${summary.failedCount} remaining"
                    }
                }
                summary.failedCount > 0 -> {
                    _actionMessage.value = if (lang == AppLanguage.ODIA) {
                        "ସିଙ୍କ୍ ଅପେକ୍ଷାରେ ଅଛି — ଇଣ୍ଟରନେଟ୍ ସଂଯୋଗ ଯାଞ୍ଚ କରନ୍ତୁ"
                    } else {
                        "Waiting to sync — check internet connection"
                    }
                }
                notifyOnNothing -> {
                    _actionMessage.value = if (lang == AppLanguage.ODIA) {
                        "ସମସ୍ତ ପାଠ୍ୟ ଯୋଜନା ସିଙ୍କ୍ ହୋଇସାରିଛି"
                    } else {
                        "All lesson plans are up to date"
                    }
                }
            }
        } finally {
            _isSyncing.value = false
        }
    }

    fun deletePlan(planId: String, lang: AppLanguage) {
        activeLanguage = lang
        viewModelScope.launch {
            if (offlineRepo != null) {
                val res = offlineRepo.deletePlanOfflineFirst(planId, currentUserId)
                res.onSuccess { syncedNow ->
                    if (_activePlan.value?.id == planId) _activePlan.value = null
                    _actionMessage.value = if (syncedNow) {
                        if (lang == AppLanguage.ODIA) "ପାଠ୍ୟ ଯୋଜନା ବିଲୋପ ହେଲା" else "Lesson plan deleted"
                    } else {
                        if (lang == AppLanguage.ODIA) {
                            "ଅଫଲାଇନରେ ବିଲୋପ ହେଲା — ଅନଲାଇନ୍ ହେଲେ ସିଙ୍କ୍ ହେବ"
                        } else {
                            "Deleted offline — will sync when online"
                        }
                    }
                }.onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Delete failed"
                }
            } else {
                val res = lessonPlanRepo.deleteLessonPlan(planId)
                if (res.isSuccess) {
                    if (_activePlan.value?.id == planId) _activePlan.value = null
                    _actionMessage.value = if (lang == AppLanguage.ODIA) "ପାଠ୍ୟ ଯୋଜନା ବିଲୋପ ହେଲା" else "Lesson plan deleted"
                } else {
                    _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Delete failed"
                }
            }
        }
    }

    fun verifyPlan(planId: String, verified: Boolean) {
        viewModelScope.launch {
            val res = lessonPlanRepo.adminVerifyLessonPlan(planId, verified)
            _actionMessage.value = if (res.isSuccess) "Verification status updated" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun savePanchapadiConfig(config: PanchapadiConfig) {
        viewModelScope.launch {
            val res = lessonPlanRepo.savePanchapadiConfig(config, currentUserId)
            _actionMessage.value = if (res.isSuccess) {
                "ପଞ୍ଚପଦୀ ଢାଞ୍ଚା ସୁରକ୍ଷିତ ହେଲା (Panchapadi labels saved)"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save config"
            }
        }
    }

    fun seedSamplePlans(authorName: String) {
        val today = DateUtils.today()
        viewModelScope.launch {
            val samples = listOf(
                LessonPlan(
                    userId = currentUserId,
                    authorName = authorName,
                    title = "ସଂଖ୍ୟା ଗଣନା ଓ ସ୍ଥାନୀୟ ମାନ (Numbers & Place Value)",
                    stage = NepStage.PREPARATORY.key,
                    classLevel = "Class 4 (ଚତୁର୍ଥ)",
                    subject = "ଗଣିତ (Mathematics)",
                    medium = "ଓଡ଼ିଆ ମାଧ୍ୟମ (Odia Medium)",
                    topic = "୪ ଅଙ୍କ ବିଶିଷ୍ଟ ସଂଖ୍ୟାର ସ୍ଥାନୀୟ ମାନ",
                    durationMinutes = 45L,
                    planDay = today.day.toLong(),
                    planMonth = today.month.toLong(),
                    planYear = today.year.toLong(),
                    learningObjectives = "• ପିଲାମାନେ ୪ ଅଙ୍କ ବିଶିଷ୍ଟ ସଂଖ୍ୟା ପଢ଼ି ଓ ଲେଖି ପାରିବେ।\n• ଏକକ, ଦଶକ, ଶତକ ଓ ହଜାର ସ୍ଥାନୀୟ ମାନ ବୁଝିପାରିବେ।",
                    tlmMaterials = "କାଠି ବିଡ଼ା, ସ୍ଥାନୀୟ ମାନ ଚାର୍ଟ, ଅଙ୍କ କାର୍ଡ (0-9), କଳାପଟା ଓ ଚକ୍।",
                    teacherActivities = "ଅଭିନୟ ଓ ସ୍ଥାନୀୟ ମାନ ଖେଳ ମାଧ୍ୟମରେ ଧାରଣା ସ୍ପଷ୍ଟ କରିବେ।",
                    studentActivities = "ଦଳଗତ କାର୍ଯ୍ୟରେ ଅଙ୍କ କାର୍ଡ ସଜାଇ ସଂଖ୍ୟା ଗଠନ କରିବେ।",
                    aditiStep = "ପିଲାମାନଙ୍କୁ ବଜାରରୁ କିଣାକିଣି ଓ ନୋଟ୍ ଗଣିବା ବିଷୟରେ ପ୍ରଶ୍ନ ପଚାରି ପୂର୍ବ ଜ୍ଞାନ ସହ ଯୋଡ଼ିବା।",
                    aditiMinutes = 7L,
                    bodhaStep = "୧୦-୧୦ କାଠି ବିଡ଼ା ଓ ସ୍ଥାନୀୟ ମାନ ବାକ୍ସ ବ୍ୟବହାର କରି ଏକକରୁ ହଜାର ପର୍ଯ୍ୟନ୍ତ ବିସ୍ତାରିତ ରୂପ ବୁଝାଇବା।",
                    bodhaMinutes = 15L,
                    abhyasaStep = "ଦଳଗତ ଭାବରେ ଅଙ୍କ କାର୍ଡ ବ୍ୟବହାର କରି ୪ ଅଙ୍କ ସଂଖ୍ୟା ତିଆରି ଓ ଖାତାରେ ଲେଖା ଅଭ୍ୟାସ।",
                    abhyasaMinutes = 10L,
                    prayogaStep = "ବିଦ୍ୟାଳୟର ମଧ୍ୟାହ୍ନ ଭୋଜନ ଖର୍ଚ୍ଚ ଓ ବହି ସଂଖ୍ୟା ହିସାବରେ ସ୍ଥାନୀୟ ମାନ ପ୍ରୟୋଗ।",
                    prayogaMinutes = 8L,
                    prasaraStep = "ଘରେ ବାପା-ମାଆଙ୍କ ସହ ମୋବାଇଲ ନମ୍ବର ବା ଗାଡ଼ି ନମ୍ବରର ଅଙ୍କଗୁଡ଼ିକର ସ୍ଥାନୀୟ ମାନ ବାହାର କରିବା।",
                    prasaraMinutes = 5L,
                    competencyQuestions = "୧. ୩୪୫୬ ସଂଖ୍ୟାରେ ୪ର ସ୍ଥାନୀୟ ମାନ କେତେ?\n୨. ୫, ୦, ୨, ୮ ଅଙ୍କ ନେଇ ସବୁଠାରୁ ବଡ଼ ୪ ଅଙ୍କ ସଂଖ୍ୟା ଗଠନ କର।",
                    assessmentCriteria = "ସଂଖ୍ୟା ଗଠନ ସଠିକତା, ମୌଖିକ ଉତ୍ତର ଓ ଶ୍ରେଣୀ କାର୍ଯ୍ୟପତ୍ର ମୂଲ୍ୟାଙ୍କନ।",
                    homeworkActivities = "ପାଠ୍ୟପୁସ୍ତକ ଅଭ୍ୟାସ ୫ଟି ପ୍ରଶ୍ନ; ଧୀରେ ଶିଖୁଥିବା ପିଲାଙ୍କ ପାଇଁ ୩ ଅଙ୍କ ସଂଖ୍ୟା ଅଭ୍ୟାସ।",
                    inclusiveSuggestions = "ଦୃଷ୍ଟି ବାଧିତ ପିଲାଙ୍କ ପାଇଁ ବଡ଼ ଅକ୍ଷର ଓ ସ୍ପର୍ଶକ ବିଡ଼ା (Tactile bundles) ବ୍ୟବହାର।",
                    summary = "ସ୍ଥାନୀୟ ମାନ ଜାଣିଲେ ବଡ଼ ସଂଖ୍ୟା ସହଜରେ ପଢ଼ି ଓ ଗଣି ହୁଏ।",
                    teacherReflection = "ଅଙ୍କ କାର୍ଡ ଖେଳରେ ପିଲାମାନଙ୍କର ଆଗ୍ରହ ଉତ୍ସାହଜନକ ଥିଲା।",
                    isAiGenerated = false,
                    isVerified = false,
                    status = "published"
                ),
                LessonPlan(
                    userId = currentUserId,
                    authorName = authorName,
                    title = "ଜଳ ସଂରକ୍ଷଣ ଓ ଆମ ପରିବେଶ (Water Conservation)",
                    stage = NepStage.MIDDLE.key,
                    classLevel = "Class 7 (ସପ୍ତମ)",
                    subject = "ବିଜ୍ଞାନ (Science)",
                    medium = "ଓଡ଼ିଆ ମାଧ୍ୟମ (Odia Medium)",
                    topic = "ଜଳ ଚକ୍ର ଏବଂ ବର୍ଷା ଜଳ ଅମଳ",
                    durationMinutes = 45L,
                    planDay = today.day.toLong(),
                    planMonth = today.month.toLong(),
                    planYear = today.year.toLong(),
                    learningObjectives = "• ଜଳ ଚକ୍ରର ପ୍ରକ୍ରିୟା ବ୍ୟାଖ୍ୟା କରିପାରିବେ।\n• ଓଡ଼ିଶାର ଗ୍ରାମାଞ୍ଚଳରେ ବର୍ଷା ଜଳ ସଂରକ୍ଷଣ ଉପାୟ ଜାଣିବେ।",
                    tlmMaterials = "ଜଳ ଚକ୍ର ମଡେଲ, ପ୍ଲାଷ୍ଟିକ ବୋତଲ ରେନ୍-ୱାଟର ହାର୍ଭେଷ୍ଟିଂ ମଡେଲ, ଚାର୍ଟ।",
                    teacherActivities = "ପ୍ରଶ୍ନୋତ୍ତର ଓ ପ୍ରକଳ୍ପ ପ୍ରଦର୍ଶନ ମାଧ୍ୟମରେ ଶିକ୍ଷାଦାନ।",
                    studentActivities = "ବିଦ୍ୟାଳୟ ପରିସରରେ ଜଳ ଅପଚୟ ଯାଞ୍ଚ ଓ ପୋଷ୍ଟର ତିଆରି।",
                    aditiStep = "ଖରାଦିନେ ପୋଖରୀ ଓ ନଳକୂପ ଶୁଖିଯିବା ଅନୁଭୂତି ଉପରେ ଆଲୋଚନା।",
                    aditiMinutes = 6L,
                    bodhaStep = "ବାଷ୍ପୀଭବନ, ଘନୀଭବନ ଓ ବର୍ଷଣ ପ୍ରକ୍ରିୟା ଚିତ୍ର ମାଧ୍ୟମରେ ବୁଝାଇବା।",
                    bodhaMinutes = 15L,
                    abhyasaStep = "ଜଳ ଚକ୍ରର ନାମାଙ୍କିତ ଚିତ୍ର ଅଙ୍କନ ଓ ଦଳଗତ କୁଇଜ୍।",
                    abhyasaMinutes = 10L,
                    prayogaStep = "ବିଦ୍ୟାଳୟ ଛାତରୁ ବର୍ଷା ଜଳ ସଂରକ୍ଷଣ ପିଟ୍ ଯୋଜନା ପ୍ରସ୍ତୁତ କରିବା।",
                    prayogaMinutes = 9L,
                    prasaraStep = "ସଚେତନତା ସ୍ଲୋଗାନ ଲେଖି ଗ୍ରାମବାସୀଙ୍କ ସହ ଅଂଶୀଦାର କରିବା।",
                    prasaraMinutes = 5L,
                    competencyQuestions = "ଯଦି ଗଛ କଟାଯାଏ ତେବେ ଜଳ ଚକ୍ର ଉପରେ କି ପ୍ରଭାବ ପଡ଼ିବ?",
                    assessmentCriteria = "ଚିତ୍ର ଅଙ୍କନ, ପ୍ରକଳ୍ପ ରିପୋର୍ଟ ଓ ଦଳଗତ ଉପସ୍ଥାପନା।",
                    homeworkActivities = "ଘରେ ପ୍ରତିଦିନ ବ୍ୟବହୃତ ଜଳ ପରିମାଣ ତାଲିକା ପ୍ରସ୍ତୁତ କର।",
                    inclusiveSuggestions = "ଶ୍ରବଣ ବାଧିତ ଶିକ୍ଷାର୍ଥୀଙ୍କ ପାଇଁ ଭିଜୁଆଲ ଫ୍ଲୋ-ଚାର୍ଟ ଓ ଅଭିନୟ।",
                    summary = "ଜଳ ଅମୂଲ୍ୟ ସମ୍ପଦ; ବର୍ଷା ଜଳ ଅମଳ ଦ୍ୱାରା ଭୂତଳ ଜଳ ସ୍ତର ବଢ଼େ।",
                    teacherReflection = "ପିଲାମାନେ ସ୍ଥାନୀୟ ପୋଖରୀ ବିଷୟରେ ବହୁତ ଭଲ ମତ ରଖିଲେ।",
                    isAiGenerated = false,
                    isVerified = false,
                    status = "published"
                )
            )
            samples.forEach { sample ->
                if (offlineRepo != null) {
                    offlineRepo.savePlanOfflineFirst(sample, currentUserId, authorName)
                } else {
                    lessonPlanRepo.createLessonPlan(sample)
                }
            }
            _actionMessage.value = "ନମୁନା ଓଡ଼ିଶା SCERT ପାଠ୍ୟ ଯୋଜନା ଯୋଡ଼ାଗଲା!"
        }
    }

    fun clearAiError() {
        _aiGenState.value = AiGenState.Idle
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "LessonPlanVM"
    }
}
