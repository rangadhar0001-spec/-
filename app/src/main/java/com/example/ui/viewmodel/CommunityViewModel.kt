package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.CommunityRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.BookmarkItem
import com.example.domain.model.CommunityPost
import com.example.domain.model.ContentReport
import com.example.domain.model.NepStage
import com.example.domain.model.PostComment
import com.example.domain.model.PublicProfile
import com.example.domain.model.TeacherGroup
import com.example.domain.model.UserNotification
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityViewModel(
    private val communityRepo: CommunityRepository,
    private val authProfileRepo: AuthProfileRepository,
    private val currentUserId: String
) : ViewModel() {

    val postsState: StateFlow<UiState<List<CommunityPost>>> = communityRepo.observePublicFeed()
        .map<List<CommunityPost>, UiState<List<CommunityPost>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing community feed", fsErr ?: error)
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "Failed to load community feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val bookmarks: StateFlow<List<BookmarkItem>> = communityRepo.observeBookmarks(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<UserNotification>> = communityRepo.observeNotifications(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val groups: StateFlow<List<TeacherGroup>> = communityRepo.observePublicGroups()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val educators: StateFlow<List<PublicProfile>> = authProfileRepo.observePublicEducators()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reports: StateFlow<List<ContentReport>> = communityRepo.observePendingReports()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val commentsForPost: StateFlow<List<PostComment>> = _activeCommentPostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList())
            else communityRepo.observeComments(postId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _communityMessage = MutableStateFlow<String?>(null)
    val communityMessage: StateFlow<String?> = _communityMessage.asStateFlow()

    fun openComments(postId: String?) {
        _activeCommentPostId.value = postId
    }

    fun createPost(
        authorName: String,
        authorRole: String,
        authorSchool: String,
        title: String,
        content: String,
        category: String,
        stage: NepStage,
        classLevel: String,
        subject: String,
        district: String,
        planDay: Int,
        planMonth: Int,
        planYear: Int,
        lessonPlanId: String?,
        lessonPlanTitle: String?,
        lang: AppLanguage,
        onCreated: () -> Unit
    ) {
        if (title.isBlank() || content.isBlank()) {
            _communityMessage.value = if (lang == AppLanguage.ODIA) {
                "ଦୟାକରି ଶୀର୍ଷକ ଏବଂ ବିଷୟବସ୍ତୁ ଲେଖନ୍ତୁ"
            } else {
                "Please enter title and content"
            }
            return
        }
        viewModelScope.launch {
            val res = communityRepo.createPost(
                CommunityPost(
                    userId = currentUserId,
                    authorName = authorName,
                    authorRole = authorRole,
                    authorSchool = authorSchool,
                    title = title,
                    content = content,
                    category = category,
                    stage = stage.key,
                    classLevel = classLevel,
                    subject = subject,
                    district = district,
                    lessonPlanId = lessonPlanId,
                    lessonPlanTitle = lessonPlanTitle,
                    planDay = planDay.toLong(),
                    planMonth = planMonth.toLong(),
                    planYear = planYear.toLong()
                )
            )
            if (res.isSuccess) {
                _communityMessage.value = if (lang == AppLanguage.ODIA) "ପୋଷ୍ଟ ପ୍ରକାଶିତ ହେଲା!" else "Post shared!"
                onCreated()
            } else {
                _communityMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to create post"
            }
        }
    }

    fun toggleLike(post: CommunityPost, senderName: String) {
        viewModelScope.launch {
            communityRepo.toggleLike(post, currentUserId, senderName)
        }
    }

    fun addComment(post: CommunityPost, authorName: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = communityRepo.addComment(post, currentUserId, authorName, text)
            if (res.isFailure) {
                _communityMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to comment"
            }
        }
    }

    fun toggleBookmark(targetId: String, targetType: String, title: String, subject: String, lang: AppLanguage) {
        viewModelScope.launch {
            val res = communityRepo.toggleBookmark(currentUserId, targetId, targetType, title, subject)
            res.onSuccess { saved ->
                _communityMessage.value = if (saved) {
                    if (lang == AppLanguage.ODIA) "ବୁକମାର୍କରେ ସୁରକ୍ଷିତ ହେଲା" else "Saved to bookmarks"
                } else {
                    if (lang == AppLanguage.ODIA) "ବୁକମାର୍କରୁ ହଟାଗଲା" else "Removed from bookmarks"
                }
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            communityRepo.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun createGroup(name: String, description: String, stage: NepStage, subject: String, district: String, userName: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val res = communityRepo.createGroup(
                TeacherGroup(
                    creatorId = currentUserId,
                    name = name,
                    description = description.ifBlank { "$subject ($district) teacher collaboration group" },
                    stage = stage.key,
                    subject = subject,
                    district = district
                ),
                currentUserId,
                userName
            )
            _communityMessage.value = if (res.isSuccess) "ଶିକ୍ଷକ ଦଳ ଗଠିତ ହେଲା!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun joinOrLeaveGroup(groupId: String, userName: String, lang: AppLanguage) {
        viewModelScope.launch {
            val res = communityRepo.joinOrLeaveGroup(groupId, currentUserId, userName)
            res.onSuccess { joined ->
                _communityMessage.value = if (joined) {
                    if (lang == AppLanguage.ODIA) "ଦଳରେ ଯୋଗଦେଲେ" else "Joined group"
                } else {
                    if (lang == AppLanguage.ODIA) "ଦଳ ଛାଡ଼ିଲେ" else "Left group"
                }
            }
        }
    }

    fun followEducator(targetUserId: String, lang: AppLanguage) {
        viewModelScope.launch {
            authProfileRepo.followEducator(targetUserId)
            _communityMessage.value = if (lang == AppLanguage.ODIA) "ଅନୁସରଣ କରାଗଲା" else "Following educator"
        }
    }

    fun reportContent(targetId: String, targetType: String, reason: String, lang: AppLanguage) {
        if (reason.isBlank()) return
        viewModelScope.launch {
            val res = communityRepo.submitReport(currentUserId, targetId, targetType, reason)
            _communityMessage.value = if (res.isSuccess) {
                if (lang == AppLanguage.ODIA) "ରିପୋର୍ଟ ସମୀକ୍ଷା ପାଇଁ ପଠାଗଲା" else "Report submitted for review"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to submit report"
            }
        }
    }

    fun moderateReport(reportId: String, status: String, hidePostId: String? = null) {
        viewModelScope.launch {
            val res = communityRepo.moderateReport(reportId, status, hidePostId)
            _communityMessage.value = if (res.isSuccess) "Moderation updated" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            communityRepo.deletePost(postId)
        }
    }

    fun seedSyntheticCommunity(authorName: String, schoolName: String, district: String) {
        val today = DateUtils.today()
        viewModelScope.launch {
            communityRepo.createPost(
                CommunityPost(
                    userId = currentUserId,
                    authorName = authorName,
                    authorRole = "teacher",
                    authorSchool = schoolName,
                    title = "ଆଦିତି ପଦକ୍ଷେପରେ କାହାଣୀ ଓ ଚିତ୍ର କାର୍ଡ ବ୍ୟବହାର",
                    content = "ମୂଳଦୁଆ ଓ ପ୍ରସ୍ତୁତି ସ୍ତରରେ ଓଡ଼ିଆ ବର୍ଣ୍ଣମାଳା ପଢ଼ାଇବା ସମୟରେ ସ୍ଥାନୀୟ ପନିପରିବା ଓ ଫଳର ଚିତ୍ର କାର୍ଡ ବ୍ୟବହାର କଲେ ପିଲାମାନେ ଶୀଘ୍ର ମନେ ରଖନ୍ତି।",
                    category = "teaching_idea",
                    stage = NepStage.FOUNDATIONAL.key,
                    classLevel = "Class 2 (ଦ୍ୱିତୀୟ)",
                    subject = "ମାତୃଭାଷା ଓଡ଼ିଆ (Odia Literacy)",
                    district = district,
                    planDay = today.day.toLong(),
                    planMonth = today.month.toLong(),
                    planYear = today.year.toLong()
                )
            )
            communityRepo.createGroup(
                TeacherGroup(
                    creatorId = currentUserId,
                    name = "ଓଡ଼ିଆ ଓ ଗଣିତ ଶିକ୍ଷକ ମଞ୍ଚ — $district",
                    description = "NEP 2020 ପଞ୍ଚପଦୀ ପାଠ୍ୟ ଯୋଜନା ଓ TLM ଅଂଶୀଦାର ପାଇଁ ଜିଲ୍ଲାସ୍ତରୀୟ ଶିକ୍ଷକ ଦଳ।",
                    stage = NepStage.PREPARATORY.key,
                    subject = "ଓଡ଼ିଆ ସାହିତ୍ୟ (Odia Language)",
                    district = district
                ),
                currentUserId,
                authorName
            )
            _communityMessage.value = "ନମୁନା ପୋଷ୍ଟ ଓ ଶିକ୍ଷକ ଦଳ ଯୋଡ଼ାଗଲା!"
        }
    }

    fun clearMessage() {
        _communityMessage.value = null
    }

    private companion object {
        const val TAG = "CommunityVM"
    }
}
