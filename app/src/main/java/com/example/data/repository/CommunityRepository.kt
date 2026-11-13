package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.domain.model.BookmarkItem
import com.example.domain.model.CommunityPost
import com.example.domain.model.ContentReport
import com.example.domain.model.PostComment
import com.example.domain.model.TeacherGroup
import com.example.domain.model.UserNotification
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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

class CommunityRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observePublicFeed(): Flow<List<CommunityPost>> =
        db.collection("posts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CommunityPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, "posts")
                throw error
            }

    fun observeComments(postId: String): Flow<List<PostComment>> =
        db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("postVisibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PostComment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, "posts/$postId/comments")
                throw error
            }

    fun observeBookmarks(userId: String): Flow<List<BookmarkItem>> =
        db.collection("users").document(userId).collection("bookmarks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BookmarkItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "users/$userId/bookmarks")
                }
                throw error
            }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> =
        db.collection("users").document(userId).collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (Firebase.auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "users/$userId/notifications")
                }
                throw error
            }

    fun observePublicGroups(): Flow<List<TeacherGroup>> =
        db.collection("groups")
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TeacherGroup::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.memberCount }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                handleFirestoreError(fsErr ?: error, OperationType.LIST, "groups")
                throw error
            }

    fun observePendingReports(): Flow<List<ContentReport>> =
        db.collection("reports")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ContentReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(emptyList())
            }

    suspend fun createPost(post: CommunityPost): Result<String> = runCatching {
        val uid = post.userId.ifBlank {
            Firebase.auth.currentUser?.uid ?: error("Authenticated user required")
        }
        val docRef = db.collection("posts").document()
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "authorName" to post.authorName.ifBlank { "Educator" }.take(100),
            "authorRole" to post.authorRole.ifBlank { "teacher" }.take(40),
            "authorSchool" to post.authorSchool.take(150),
            "title" to post.title.take(200),
            "content" to post.content.take(5000),
            "category" to (if (post.category in listOf("lesson_plan", "teaching_idea", "activity", "resource")) post.category else "teaching_idea"),
            "stage" to post.stage,
            "classLevel" to post.classLevel.take(40),
            "subject" to post.subject.take(80),
            "district" to post.district.take(60),
            "planDay" to post.planDay.coerceIn(1L, 31L),
            "planMonth" to post.planMonth.coerceIn(1L, 12L),
            "planYear" to post.planYear.coerceIn(2020L, 2100L),
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!post.lessonPlanId.isNullOrBlank()) {
            payload["lessonPlanId"] = post.lessonPlanId.take(128)
        }
        if (!post.lessonPlanTitle.isNullOrBlank()) {
            payload["lessonPlanTitle"] = post.lessonPlanTitle.take(200)
        }
        docRef.set(payload).await()
        docRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts")
    }

    suspend fun toggleLike(post: CommunityPost, userId: String, senderName: String): Result<Boolean> = runCatching {
        val likeRef = db.collection("posts").document(post.id).collection("likes").document(userId)
        val postRef = db.collection("posts").document(post.id)
        val existingLike = likeRef.get().await()
        val batch = db.batch()
        val nowLiked: Boolean
        if (existingLike.exists()) {
            batch.delete(likeRef)
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to (post.likesCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            nowLiked = false
        } else {
            batch.set(
                likeRef,
                mapOf(
                    "userId" to userId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to post.likesCount + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (post.userId != userId) {
                val notifRef = db.collection("users").document(post.userId)
                    .collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to post.userId,
                        "senderId" to userId,
                        "senderName" to senderName.take(100),
                        "type" to "like",
                        "message" to "$senderName ଆପଣଙ୍କ ପୋଷ୍ଟ '${post.title.take(50)}' କୁ ପସନ୍ଦ କରିଛନ୍ତି।",
                        "targetId" to post.id,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            nowLiked = true
        }
        batch.commit().await()
        nowLiked
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}/likes/$userId")
    }

    suspend fun addComment(post: CommunityPost, userId: String, authorName: String, text: String): Result<Unit> = runCatching {
        val commentRef = db.collection("posts").document(post.id).collection("comments").document()
        val postRef = db.collection("posts").document(post.id)
        val batch = db.batch()
        batch.set(
            commentRef,
            mapOf(
                "userId" to userId,
                "authorName" to authorName.take(100),
                "text" to text.trim().take(1000),
                "postVisibility" to post.visibility,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            postRef,
            mapOf(
                "commentsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (post.userId != userId) {
            val notifRef = db.collection("users").document(post.userId)
                .collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "recipientId" to post.userId,
                    "senderId" to userId,
                    "senderName" to authorName.take(100),
                    "type" to "comment",
                    "message" to "$authorName ଆପଣଙ୍କ ପୋଷ୍ଟରେ ମନ୍ତବ୍ୟ ଦେଇଛନ୍ତି: ${text.take(60)}",
                    "targetId" to post.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
    }

    suspend fun toggleBookmark(
        userId: String,
        targetId: String,
        targetType: String,
        title: String,
        subject: String
    ): Result<Boolean> = runCatching {
        val docId = "${targetType}_$targetId".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val ref = db.collection("users").document(userId).collection("bookmarks").document(docId)
        val existing = ref.get().await()
        if (existing.exists()) {
            ref.delete().await()
            false
        } else {
            ref.set(
                mapOf(
                    "userId" to userId,
                    "targetId" to targetId.take(128),
                    "targetType" to targetType,
                    "title" to title.take(200),
                    "subject" to subject.take(80),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/bookmarks")
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> = runCatching {
        db.collection("users").document(userId).collection("notifications").document(notificationId)
            .update("isRead", true)
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/notifications/$notificationId")
    }

    suspend fun createGroup(group: TeacherGroup, userId: String, userName: String): Result<String> = runCatching {
        val groupRef = db.collection("groups").document()
        val memberRef = groupRef.collection("members").document(userId)
        val batch = db.batch()
        batch.set(
            groupRef,
            mapOf(
                "creatorId" to userId,
                "name" to group.name.take(120),
                "description" to group.description.take(1000),
                "stage" to group.stage,
                "subject" to group.subject.take(80),
                "district" to group.district.take(60),
                "isPublic" to true,
                "memberCount" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            memberRef,
            mapOf(
                "userId" to userId,
                "displayName" to userName.take(100),
                "joinedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        groupRef.id
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "groups")
    }

    suspend fun joinOrLeaveGroup(groupId: String, userId: String, userName: String): Result<Boolean> = runCatching {
        val groupRef = db.collection("groups").document(groupId)
        val memberRef = groupRef.collection("members").document(userId)
        val existing = memberRef.get().await()
        val batch = db.batch()
        val joined: Boolean
        if (existing.exists()) {
            batch.delete(memberRef)
            batch.update(
                groupRef,
                mapOf(
                    "memberCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            joined = false
        } else {
            batch.set(
                memberRef,
                mapOf(
                    "userId" to userId,
                    "displayName" to userName.take(100),
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                groupRef,
                mapOf(
                    "memberCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            joined = true
        }
        batch.commit().await()
        joined
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "groups/$groupId/members/$userId")
    }

    suspend fun submitReport(
        reporterId: String,
        targetId: String,
        targetType: String,
        reason: String
    ): Result<Unit> = runCatching {
        val docRef = db.collection("reports").document()
        docRef.set(
            mapOf(
                "reporterId" to reporterId,
                "targetId" to targetId.take(128),
                "targetType" to targetType,
                "reason" to reason.trim().take(500),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "reports")
    }

    suspend fun moderateReport(reportId: String, newStatus: String, hidePostId: String? = null): Result<Unit> = runCatching {
        val batch = db.batch()
        batch.update(
            db.collection("reports").document(reportId),
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (!hidePostId.isNullOrBlank() && newStatus == "action_taken") {
            batch.update(
                db.collection("posts").document(hidePostId),
                mapOf(
                    "visibility" to "hidden",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        db.collection("posts").document(postId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
    }
}
