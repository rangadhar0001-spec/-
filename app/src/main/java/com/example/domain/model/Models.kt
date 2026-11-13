package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String) {
    ODIA("or"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code == "en") ENGLISH else ODIA
    }
}

enum class NepStage(
    val key: String,
    val formulaPart: String,
    val odiaName: String,
    val englishName: String,
    val ageRangeOdia: String,
    val ageRangeEn: String,
    val classesOdia: String,
    val classesEn: String,
    val classesList: List<String>
) {
    FOUNDATIONAL(
        key = "foundational",
        formulaPart = "5",
        odiaName = "ମୂଳଦୁଆ ସ୍ତର (Foundational)",
        englishName = "Foundational Stage",
        ageRangeOdia = "ବୟସ ୩–୮ ବର୍ଷ",
        ageRangeEn = "Ages 3–8 Years",
        classesOdia = "ବାଲବାଟିକା – ୨ୟ ଶ୍ରେଣୀ",
        classesEn = "Preschool – Class 2",
        classesList = listOf("Balavatika / Preschool", "Class 1 (ପ୍ରଥମ)", "Class 2 (ଦ୍ୱିତୀୟ)")
    ),
    PREPARATORY(
        key = "preparatory",
        formulaPart = "3",
        odiaName = "ପ୍ରସ୍ତୁତି ସ୍ତର (Preparatory)",
        englishName = "Preparatory Stage",
        ageRangeOdia = "ବୟସ ୮–୧୧ ବର୍ଷ",
        ageRangeEn = "Ages 8–11 Years",
        classesOdia = "୩ୟ – ୫ମ ଶ୍ରେଣୀ",
        classesEn = "Classes 3–5",
        classesList = listOf("Class 3 (ତୃତୀୟ)", "Class 4 (ଚତୁର୍ଥ)", "Class 5 (ପଞ୍ଚମ)")
    ),
    MIDDLE(
        key = "middle",
        formulaPart = "3",
        odiaName = "ମଧ୍ୟମ ସ୍ତର (Middle)",
        englishName = "Middle Stage",
        ageRangeOdia = "ବୟସ ୧୧–୧୪ ବର୍ଷ",
        ageRangeEn = "Ages 11–14 Years",
        classesOdia = "୬ଷ୍ଠ – ୮ମ ଶ୍ରେଣୀ",
        classesEn = "Classes 6–8",
        classesList = listOf("Class 6 (ଷଷ୍ଠ)", "Class 7 (ସପ୍ତମ)", "Class 8 (ଅଷ୍ଟମ)")
    ),
    SECONDARY(
        key = "secondary",
        formulaPart = "4",
        odiaName = "ମାଧ୍ୟମିକ ସ୍ତର (Secondary)",
        englishName = "Secondary Stage",
        ageRangeOdia = "ବୟସ ୧୪–୧୮ ବର୍ଷ",
        ageRangeEn = "Ages 14–18 Years",
        classesOdia = "୯ମ – ୧୨ଶ ଶ୍ରେଣୀ",
        classesEn = "Classes 9–12",
        classesList = listOf("Class 9 (ନବମ)", "Class 10 (ଦଶମ)", "Class 11 (ଏକାଦଶ)", "Class 12 (ଦ୍ୱାଦଶ)")
    );

    companion object {
        fun fromKey(key: String): NepStage =
            entries.firstOrNull { it.key == key } ?: PREPARATORY
    }
}

enum class UserRole(val key: String, val odiaLabel: String, val englishLabel: String) {
    TEACHER("teacher", "ଶିକ୍ଷକ / ଶିକ୍ଷୟିତ୍ରୀ", "Teacher"),
    STUDENT("student", "ଶିକ୍ଷାର୍ଥୀ", "Student / Learner"),
    SCHOOL_ADMIN("school_admin", "ବିଦ୍ୟାଳୟ ପ୍ରଶାସକ", "School Admin"),
    APP_ADMIN("app_admin", "ଆପ୍ ପ୍ରଶାସକ", "App Admin");

    companion object {
        fun fromKey(key: String): UserRole =
            entries.firstOrNull { it.key == key } ?: TEACHER
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val role: String = UserRole.TEACHER.key,
    val schoolName: String = "",
    val district: String = "Khordha",
    val subjects: List<String> = emptyList(),
    val profileVisibility: String = "public",
    val languagePref: String = "or",
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = UserRole.TEACHER.key,
    val schoolName: String = "",
    val district: String = "Khordha",
    val subjects: List<String> = emptyList(),
    val profileVisibility: String = "public",
    val followersCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LessonPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val title: String = "",
    val stage: String = NepStage.PREPARATORY.key,
    val classLevel: String = "Class 4 (ଚତୁର୍ଥ)",
    val subject: String = "ସାହିତ୍ୟ / Odia",
    val medium: String = "Odia",
    val topic: String = "",
    val durationMinutes: Long = 45L,
    val planDay: Long = 12L,
    val planMonth: Long = 11L,
    val planYear: Long = 2026L,
    val learningObjectives: String = "",
    val tlmMaterials: String = "",
    val teacherActivities: String = "",
    val studentActivities: String = "",
    val aditiStep: String = "",
    val aditiMinutes: Long = 7L,
    val bodhaStep: String = "",
    val bodhaMinutes: Long = 15L,
    val abhyasaStep: String = "",
    val abhyasaMinutes: Long = 10L,
    val prayogaStep: String = "",
    val prayogaMinutes: Long = 8L,
    val prasaraStep: String = "",
    val prasaraMinutes: Long = 5L,
    val competencyQuestions: String = "",
    val assessmentCriteria: String = "",
    val homeworkActivities: String = "",
    val inclusiveSuggestions: String = "",
    val summary: String = "",
    val teacherReflection: String = "",
    val isAiGenerated: Boolean = true,
    val isVerified: Boolean = false,
    val status: String = "draft",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorRole: String = UserRole.TEACHER.key,
    val authorSchool: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "teaching_idea",
    val stage: String = NepStage.PREPARATORY.key,
    val classLevel: String = "Class 4 (ଚତୁର୍ଥ)",
    val subject: String = "ସାହିତ୍ୟ / Odia",
    val district: String = "Khordha",
    val lessonPlanId: String? = null,
    val lessonPlanTitle: String? = null,
    val planDay: Long = 12L,
    val planMonth: Long = 11L,
    val planYear: Long = 2026L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val text: String = "",
    val postVisibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookmarkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: String = "lesson_plan",
    val title: String = "",
    val subject: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "system",
    val message: String = "",
    val targetId: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TeacherGroup(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val name: String = "",
    val description: String = "",
    val stage: String = NepStage.PREPARATORY.key,
    val subject: String = "ସାହିତ୍ୟ / Odia",
    val district: String = "All Odisha",
    val isPublic: Boolean = true,
    val memberCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetId: String = "",
    val targetType: String = "post",
    val reason: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PanchapadiConfig(
    @DocumentId val id: String = "panchapadi",
    val isActive: Boolean = true,
    val aditiLabel: String = "୧. ଆଦିତି (Aditi)",
    val aditiDesc: String = "ପରିଚୟ ଏବଂ ପୂର୍ବ ଜ୍ଞାନ ସହ ସଂଯୋଗ (Introduction & prior knowledge)",
    val bodhaLabel: String = "୨. ବୋଧ (Bodha)",
    val bodhaDesc: String = "ବିଷୟ ବୁଝାଇବା ଏବଂ ଧାରଣା ବିକାଶ (Explanation & concept understanding)",
    val abhyasaLabel: String = "୩. ଅଭ୍ୟାସ (Abhyasa)",
    val abhyasaDesc: String = "ଅଭ୍ୟାସ, ପ୍ରଶ୍ନୋତ୍ତର ଓ ଛାତ୍ରଛାତ୍ରୀ ଅଂଶଗ୍ରହଣ (Practice & participation)",
    val prayogaLabel: String = "୪. ପ୍ରୟୋଗ (Prayoga)",
    val prayogaDesc: String = "ବାସ୍ତବ ଜୀବନରେ ଜ୍ଞାନର ପ୍ରୟୋଗ (Real-life application)",
    val prasaraLabel: String = "୫. ପ୍ରସାର (Prasara)",
    val prasaraDesc: String = "ପୁନରାଲୋଚନା, ବିସ୍ତାର ଓ ଅଂଶୀଦାର (Extension, revision & sharing)",
    val updatedBy: String = "system",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
