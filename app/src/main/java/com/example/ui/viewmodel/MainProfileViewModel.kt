package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.LanguagePreferences
import com.example.domain.model.AppLanguage
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainProfileViewModel(
    private val authProfileRepo: AuthProfileRepository,
    private val languagePrefs: LanguagePreferences,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String?
) : ViewModel() {

    val language: StateFlow<AppLanguage> = languagePrefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.ODIA)

    val userProfile: StateFlow<UserProfile?> = authProfileRepo.observePrivateProfile(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w("MainProfileVM", "Failed to observe profile", fsErr ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _isAdmin = MutableStateFlow(initialEmail == "jadumani0864@gmail.com")
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authProfileRepo.ensureUserProfile(
                userId = currentUserId,
                displayName = initialDisplayName,
                email = initialEmail,
                role = UserRole.TEACHER.key
            )
            _isAdmin.value = authProfileRepo.checkIsAdmin(currentUserId, initialEmail)
        }
    }

    fun toggleLanguage() {
        val next = if (language.value == AppLanguage.ODIA) AppLanguage.ENGLISH else AppLanguage.ODIA
        viewModelScope.launch {
            languagePrefs.setLanguage(next)
            val profile = userProfile.value
            if (profile != null) {
                authProfileRepo.updateProfile(
                    userId = currentUserId,
                    displayName = profile.displayName,
                    schoolName = profile.schoolName,
                    district = profile.district,
                    subjects = profile.subjects,
                    profileVisibility = profile.profileVisibility,
                    languagePref = next.code
                )
            }
        }
    }

    fun updateProfile(
        displayName: String,
        schoolName: String,
        district: String,
        subjects: List<String>,
        profileVisibility: String
    ) {
        viewModelScope.launch {
            val res = authProfileRepo.updateProfile(
                userId = currentUserId,
                displayName = displayName,
                schoolName = schoolName,
                district = district,
                subjects = subjects,
                profileVisibility = profileVisibility,
                languagePref = language.value.code
            )
            _statusMessage.value = if (res.isSuccess) {
                if (language.value == AppLanguage.ODIA) "ପ୍ରୋଫାଇଲ୍ ସଫଳତାର ସହ ଅପଡେଟ୍ ହେଲା" else "Profile updated successfully"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun toggleBlockUser(targetUserId: String) {
        val blocked = userProfile.value?.blockedUserIds ?: emptyList()
        viewModelScope.launch {
            authProfileRepo.toggleBlockUser(currentUserId, targetUserId, blocked)
            _statusMessage.value = if (targetUserId in blocked) {
                if (language.value == AppLanguage.ODIA) "ବ୍ୟବହାରକାରୀ ଅନବ୍ଲକ୍ ହେଲେ" else "User unblocked"
            } else {
                if (language.value == AppLanguage.ODIA) "ବ୍ୟବହାରକାରୀ ବ୍ଲକ୍ ହେଲେ" else "User blocked"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
