package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.LessonProgressRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.LessonPlan
import com.example.domain.model.LessonPlanProgress
import com.example.domain.model.NepProgressEvaluator
import com.example.domain.model.OutcomeStatus
import com.example.util.ConnectivityObserver
import com.example.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NepProgressViewModel(
    private val progressRepo: LessonProgressRepository,
    private val currentUserId: String,
    private val connectivity: ConnectivityObserver? = null
) : ViewModel() {

    val progressByPlanId: StateFlow<Map<String, LessonPlanProgress>> =
        progressRepo.observeProgressForUser(currentUserId)
            .map { list -> list.associateBy { it.lessonPlanId.ifBlank { it.id } } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing lesson progress", error)
                emit(emptyMap())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val pendingProgressSyncCount: StateFlow<Int> =
        progressRepo.observePendingCount(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _selectedPlanId = MutableStateFlow<String?>(null)
    val selectedPlanId: StateFlow<String?> = _selectedPlanId.asStateFlow()

    private val _progressMessage = MutableStateFlow<String?>(null)
    val progressMessage: StateFlow<String?> = _progressMessage.asStateFlow()

    init {
        if (connectivity != null) {
            viewModelScope.launch {
                connectivity.isOnline
                    .filter { it }
                    .collect {
                        progressRepo.syncPendingProgress(currentUserId)
                    }
            }
        }
    }

    fun selectPlanForTracking(planId: String?) {
        _selectedPlanId.value = planId
    }

    fun defaultProgressFor(plan: LessonPlan): LessonPlanProgress {
        val existing = progressByPlanId.value[plan.id]
        if (existing != null) return existing
        val today = DateUtils.today()
        return LessonPlanProgress(
            id = plan.id,
            userId = currentUserId,
            lessonPlanId = plan.id,
            stage = plan.stage,
            lastTrackedDay = today.day.toLong(),
            lastTrackedMonth = today.month.toLong(),
            lastTrackedYear = today.year.toLong(),
            completionPercent = 0L
        )
    }

    fun saveUpdatedProgress(plan: LessonPlan, updated: LessonPlanProgress, lang: AppLanguage) {
        val percent = NepProgressEvaluator.computeCompletionPercent(plan, updated).toLong()
        val withScore = updated.copy(
            id = plan.id,
            userId = currentUserId,
            lessonPlanId = plan.id,
            stage = plan.stage,
            completionPercent = percent
        )
        viewModelScope.launch {
            val res = progressRepo.saveProgress(withScore)
            res.onSuccess { result ->
                _progressMessage.value = if (result.syncedImmediately) {
                    if (lang == AppLanguage.ODIA) "NEP 2020 ପ୍ରଗତି ସୁରକ୍ଷିତ ହେଲା ($percent%)" else "NEP 2020 progress saved ($percent%)"
                } else {
                    if (lang == AppLanguage.ODIA) "ଅଫଲାଇନରେ ପ୍ରଗତି ସୁରକ୍ଷିତ ହେଲା ($percent%)" else "Progress saved offline ($percent%) — will sync online"
                }
            }.onFailure { err ->
                _progressMessage.value = err.localizedMessage ?: "Failed to save progress"
            }
        }
    }

    fun buildNextProgressForOutcome(
        current: LessonPlanProgress,
        outcomeIndex: Int,
        newStatus: OutcomeStatus
    ): LessonPlanProgress {
        val idxLong = outcomeIndex.toLong()
        val achieved = current.achievedOutcomeIndices.filterNot { it == idxLong }.toMutableList()
        val inProg = current.inProgressOutcomeIndices.filterNot { it == idxLong }.toMutableList()
        when (newStatus) {
            OutcomeStatus.ACHIEVED -> achieved.add(idxLong)
            OutcomeStatus.IN_PROGRESS -> inProg.add(idxLong)
            OutcomeStatus.NOT_STARTED -> Unit
        }
        return current.copy(
            achievedOutcomeIndices = achieved.distinct(),
            inProgressOutcomeIndices = inProg.distinct()
        )
    }

    fun syncNow() {
        viewModelScope.launch {
            progressRepo.syncPendingProgress(currentUserId)
        }
    }

    fun clearMessage() {
        _progressMessage.value = null
    }

    private companion object {
        const val TAG = "NepProgressVM"
    }
}
