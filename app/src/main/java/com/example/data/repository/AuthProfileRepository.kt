package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.domain.model.PublicProfile
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthProfileRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observePrivateProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, "users/$userId")
                }
                throw error
            }

    fun observePublicEducators(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .whereEqualTo("profileVisibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, "public_profiles")
                throw error
            }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        role: String = UserRole.TEACHER.key,
        schoolName: String = "Odisha Govt. School",
        district: String = "Khordha (ଖୋର୍ଦ୍ଧା)"
    ): Result<UserProfile> = runCatching {
        val userRef = db.collection("users").document(userId)
        val publicRef = db.collection("public_profiles").document(userId)
        val existingSnap = userRef.get().await()
        if (existingSnap.exists()) {
            val existing = existingSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (existing != null) return@runCatching existing
        }

        val safeName = displayName.ifBlank { "ଶିକ୍ଷକ ସାଥୀ" }.take(100)
        val safeRole = if (role in listOf("teacher", "student")) role else "teacher"
        val defaultSubjects = listOf("ଓଡ଼ିଆ ସାହିତ୍ୟ (Odia)", "ଗଣିତ (Mathematics)")

        val privatePayload = mutableMapOf<String, Any>(
            "userId" to userId,
            "displayName" to safeName,
            "role" to safeRole,
            "schoolName" to schoolName.ifBlank { "Odisha School" }.take(150),
            "district" to district.ifBlank { "Khordha (ଖୋର୍ଦ୍ଧା)" }.take(60),
            "subjects" to defaultSubjects,
            "profileVisibility" to "public",
            "languagePref" to "or",
            "blockedUserIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!email.isNullOrBlank()) {
            privatePayload["email"] = email.take(150)
        }

        val publicPayload = mapOf(
            "userId" to userId,
            "displayName" to safeName,
            "role" to safeRole,
            "schoolName" to schoolName.ifBlank { "Odisha School" }.take(150),
            "district" to district.ifBlank { "Khordha (ଖୋର୍ଦ୍ଧା)" }.take(60),
            "subjects" to defaultSubjects,
            "profileVisibility" to "public",
            "followersCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(userRef, privatePayload)
        batch.set(publicRef, publicPayload)
        batch.commit().await()

        UserProfile(
            id = userId,
            userId = userId,
            displayName = safeName,
            email = email,
            role = safeRole,
            schoolName = schoolName,
            district = district,
            subjects = defaultSubjects,
            profileVisibility = "public",
            languagePref = "or"
        )
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "users/$userId")
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        schoolName: String,
        district: String,
        subjects: List<String>,
        profileVisibility: String,
        languagePref: String
    ): Result<Unit> = runCatching {
        val safeName = displayName.trim().ifBlank { "ଶିକ୍ଷକ ସାଥୀ" }.take(100)
        val safeSchool = schoolName.trim().take(150)
        val safeDistrict = district.trim().take(60)
        val safeSubjects = subjects.take(10).map { it.take(60) }
        val safeVisibility = if (profileVisibility in listOf("public", "school_only", "private")) {
            profileVisibility
        } else {
            "public"
        }
        val safeLang = if (languagePref == "en") "en" else "or"

        val batch = db.batch()
        batch.update(
            db.collection("users").document(userId),
            mapOf(
                "displayName" to safeName,
                "schoolName" to safeSchool,
                "district" to safeDistrict,
                "subjects" to safeSubjects,
                "profileVisibility" to safeVisibility,
                "languagePref" to safeLang,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("public_profiles").document(userId),
            mapOf(
                "displayName" to safeName,
                "schoolName" to safeSchool,
                "district" to safeDistrict,
                "subjects" to safeSubjects,
                "profileVisibility" to safeVisibility,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
    }

    suspend fun toggleBlockUser(currentUserId: String, targetUserId: String, currentBlocked: List<String>): Result<Unit> = runCatching {
        val updated = if (targetUserId in currentBlocked) {
            currentBlocked - targetUserId
        } else {
            (currentBlocked + targetUserId).take(50)
        }
        db.collection("users").document(currentUserId).update(
            mapOf(
                "blockedUserIds" to updated,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$currentUserId")
    }

    suspend fun followEducator(targetUserId: String): Result<Unit> = runCatching {
        db.collection("public_profiles").document(targetUserId)
            .update(
                mapOf(
                    "followersCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "public_profiles/$targetUserId")
    }

    suspend fun checkIsAdmin(userId: String, email: String?): Boolean {
        if (email == "jadumani0864@gmail.com") return true
        return try {
            val doc = db.collection("admins").document(userId).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }
}
