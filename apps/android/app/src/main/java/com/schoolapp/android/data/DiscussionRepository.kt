package com.schoolapp.android.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class AppRole(val displayName: String, val badgeColor: Long) {
    PARENT("Parent", 0xFF2563EB),
    STUDENT("Student", 0xFF16A34A),
    TEACHER("Teacher", 0xFF7C3AED),
    ADMIN("Admin", 0xFFDC2626)
}

data class StudentProfile(
    val id: String,
    val studentCode: String,
    val rollNo: String = "01",
    val firstName: String,
    val fullName: String,
    val className: String = "Class 10",
    val section: String = "A",
    val bannerClassText: String,
    val classId: String,
    val classTitle: String,
    val avatarUrl: String,
    val isProfileLocked: Boolean = true,
    val verificationStatus: String = "VERIFIED", // "PENDING", "VERIFIED", "BLOCKED"
    val blockedReason: String = "",
    val verifiedBy: String = "Mrs. Gupta",
    val tuitionFeeInr: Int,
    val examFeeInr: Int,
    val activityFeeInr: Int,
    val feeStatus: String = "Pending",
    val feeDueDate: String = "15 Oct 2026",
    val busRoute: String = "Route No. 3 (Viman Nagar – School)",
    val pickupStop: String = "Viman Nagar (Main Gate)",
    val pickupTime: String = "07:15 AM",
    val dropTime: String = "02:30 PM",
    val parentPhone: String = "+91 98765 43210",
    val parentEmail: String = "parent@example.com",
    val address: String = "Viman Nagar, Beeramguda, Hyderabad"
) {
    val totalFeeInr: Int get() = tuitionFeeInr + examFeeInr + activityFeeInr
}

data class SectionRosterStudent(
    val id: String,
    val studentCode: String,
    val rollNo: String,
    val fullName: String,
    val classId: String,
    val className: String,
    val section: String,
    val parentPhone: String,
    val loginTimestamp: String,
    val verificationStatus: String = "PENDING", // "PENDING", "VERIFIED", "BLOCKED"
    val blockedReason: String = "",
    val verifiedBy: String = "",
    val isCurrentDeviceStudent: Boolean = false
)

data class AutomationRuleItem(
    val id: String,
    val title: String,
    val whenTrigger: String,
    val checkCondition: String,
    val thenAction: String,
    val recordTarget: String,
    val category: String, // "Attendance", "Fees", "Academics", "Operations", "Admissions"
    val isEnabled: Boolean = true,
    val executionsCount: Int = 0,
    val lastTriggeredText: String = "Today, 08:15 AM"
)

data class FinancialLeakageAlert(
    val id: String,
    val alertType: String, // "Overdue Installment", "Unmatched Receipt", "Duplicate Entry", "Concession Review", "Unfilled Seats"
    val title: String,
    val details: String,
    val amountImpactInr: Int,
    val severity: String = "High", // "High", "Medium"
    val isVerifiedByHuman: Boolean = false
)

data class FeeTransactionRecord(
    val id: String,
    val studentName: String,
    val classSection: String,
    val amountInr: Int,
    val paymentMode: String, // "UPI Auto-Webhook", "Cash Counter", "Cheque", "Bank Transfer"
    val receiptNo: String,
    val timestamp: String,
    val status: String = "Reconciled"
)

data class SchoolBranchMetrics(
    val branchName: String,
    val studentsCount: Int,
    val attendancePct: String,
    val feeCollectionPct: String,
    val monthlyRevenueLakhs: String
)

data class HomeworkTemplateItem(
    val id: String,
    val subject: String,
    val gradeLevel: String,
    val title: String,
    val instructions: String,
    val maxMarks: Int = 10
)

data class TeacherSubstitutionItem(
    val id: String,
    val absentTeacherName: String,
    val leaveReason: String,
    val periodSlot: String,
    val classSectionName: String,
    val subject: String,
    val suggestedSubstitutes: List<String>,
    val assignedSubstitute: String? = null,
    val status: String = "Action Needed" // "Action Needed", "Assigned & Notified"
)

data class AdmissionEnquiryItem(
    val id: String,
    val childName: String,
    val parentName: String,
    val parentPhone: String,
    val targetGrade: String,
    val counsellorName: String,
    val stage: String, // "New Enquiry", "Follow-Up Scheduled", "Interview Scheduled", "Admitted"
    val nextFollowUp: String,
    val source: String = "Website / Parent Referral"
)

data class StaffMemberRecord(
    val id: String,
    val name: String,
    val designation: String,
    val department: String,
    val attendanceStatus: String, // "Present", "Late", "On Leave", "Absent", "Not Checked In"
    val classesAssigned: String,
    val workloadPeriodsPerWeek: Int,
    val employeeId: String = "",
    val phone: String = "",
    val email: String = "",
    val qualification: String = "",
    val joiningDate: String = "",
    val subjects: String = "",
    val staffType: String = "Teaching" // "Teaching", "Non-Teaching"
)

// Staff HR — daily attendance entry (Fingerprint + 500m Campus Check-In / Check-Out)
data class StaffAttendanceDay(
    val id: String,
    val staffId: String,
    val dateLabel: String,      // e.g. "Today", "Mon, 09 Nov"
    val checkIn: String? = null,
    val checkOut: String? = null,
    val status: String,         // "Present", "Late", "On Leave", "Absent", "Not Checked In"
    val distanceMeters: Int? = null,
    val verificationMethod: String = ""
)

data class SchoolCampusGeofence(
    val schoolName: String = "Eden Garden, Viman Nagar, Pune",
    val latitude: Double = 18.560556,
    val longitude: Double = 73.914722,
    val radiusMeters: Int = 500,
    val isCalibratedFromDevice: Boolean = false
)

// Staff HR — leave application
data class StaffLeaveRequest(
    val id: String,
    val staffId: String,
    val staffName: String,
    val leaveType: String,      // "Casual Leave", "Sick Leave", "Earned Leave"
    val fromDate: String,
    val toDate: String,
    val days: Int,
    val reason: String,
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    val appliedOn: String = "Today",
    val reviewedBy: String = "",
    val substituteRequested: Boolean = false
)

// Annual leave entitlement per staff member (simple, school-standard policy)
object StaffLeavePolicy {
    val annualQuota = linkedMapOf(
        "Casual Leave" to 12,
        "Sick Leave" to 10,
        "Earned Leave" to 15
    )
}

data class BusRouteRecord(
    val id: String,
    val routeNumber: String,
    val routeTitle: String,
    val vehicleNumber: String,
    val driverName: String,
    val driverPhone: String,
    val attendantName: String,
    val studentsAssigned: Int,
    val tripStatus: String, // "On Route - Morning Pickup", "Parked at Campus", "Afternoon Drop Active"
    val nextMaintenanceDue: String
)

data class AttendanceAuditEntry(
    val id: String,
    val timestamp: String,
    val summary: String,
    val triggeredActions: String
)

data class ParentAbsenceExplanation(
    val id: String,
    val studentId: String,
    val studentName: String,
    val classSection: String,
    val dateText: String,
    val parentExplanation: String,
    val status: String = "Submitted by Parent" // "Pending Parent Note", "Submitted by Parent", "Approved by Teacher"
)

data class SchoolBranding(
    val id: String = "00000000-0000-0000-0000-000000000001",
    val schoolCode: String = "GENESIS",
    val name: String = "Genesis School Beeramguda",
    val slug: String = "genesis-high-school",
    val tagline: String = "Official Student & Parent Portal",
    val logoUrl: String = "",
    val primaryColorHex: String = "#1E40AF",
    val accentColorHex: String = "#38BDF8"
)

data class SchoolClass(
    val id: String,
    val name: String,
    val studentCount: Int = 0,
    val dbUuid: String = "",
    val className: String = "",
    val section: String = ""
)

data class ClassNote(
    val id: String,
    val classId: String,
    val subject: String,
    val title: String,
    val chapter: String,
    val fileUrl: String,
    val fileType: String = "PDF",
    val teacherName: String,
    val date: String = "Oct 8, 2026",
    val isDownloaded: Boolean = false,
    val localPdfUri: String? = null,
    val pageCount: Int = 0
)

data class AdminNotice(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val author: String,
    val date: String = "Oct 8, 2026",
    val venue: String = "School Main Ground",
    val eventTime: String = "09:00 AM onwards",
    val attachmentName: String = "Event_Schedule_2026.pdf",
    val attachmentSize: String = "2.4 MB",
    val imageUrl: String? = null,
    val targetAudience: String = "Entire School",
    val startsAtIso: String? = null,
    val expiresAtIso: String? = null,
    val scheduleStatus: String = "ACTIVE"
)

data class DiscussionReply(
    val id: String,
    val discussionId: String,
    val authorName: String,
    val authorRole: String = "STUDENT",
    val content: String,
    val imageUri: String? = null,
    val isVerified: Boolean = false,
    val likesCount: Int = 0,
    val timeAgo: String = "Just now"
)

data class DiscussionPost(
    val id: String,
    val classId: String = "class-10a",
    val authorName: String,
    val authorRole: String = "STUDENT",
    val subject: String,
    val title: String,
    val content: String,
    val imageUri: String? = null,
    val hasEquationCard: Boolean = false,
    val equationFormula: String = "x² - 5x + 6 = 0",
    val equationSubtext: String = "Find the roots.",
    val timeAgo: String = "2 hours ago",
    val repliesCount: Int = 0,
    val replies: List<DiscussionReply> = emptyList()
)

data class LessonSlot(
    val id: String,
    val timeRange: String,
    val subject: String,
    val teacherName: String,
    val status: String = "" // "Ongoing", "Next", or ""
)

data class HomeworkItem(
    val id: String,
    val classId: String,
    val subject: String,
    val title: String,
    val dueText: String,
    val status: String, // "Due Soon", "Pending", or "Completed"
    val instructions: String,
    val maxMarks: Int = 10,
    val isCompleted: Boolean = false,
    val deadlineDateIso: String = "2026-10-20",
    val totalStudents: Int = 40,
    val submittedCount: Int = 34,
    val attachedDocName: String? = "Assignment_Worksheet.pdf",
    val attachedDocSize: String? = "1.4 MB",
    val projectType: String = "Assignment" // "Assignment" or "Project"
)

data class ExamScheduleItem(
    val id: String,
    val classId: String,
    val examName: String,
    val subject: String,
    val dateText: String,
    val dateIso: String,
    val timeSlot: String,
    val roomNo: String = "Hall 3A",
    val syllabus: String,
    val preparationTips: String = "Revise NCERT examples and solve sample papers",
    val maxMarks: Int = 80
)

data class SiblingEntry(
    val id: String = "sib-${System.currentTimeMillis()}",
    val fullName: String,
    val age: Int,
    val currentGrade: String, // e.g. "Class 3", "LKG", "UKG"
    val schoolType: String = "OTHER", // "OTHER" or "GENESIS"
    val schoolName: String = "",
    val reasonOtherSchool: String = "",
    val targetAdmissionYear: String = "2026-2027",
    val admissionStatus: String = "NEW_LEAD" // "NEW_LEAD", "CONTACTED", "VISIT_SCHEDULED", "ENROLLED"
)

data class SiblingSubmission(
    val id: String = "sub-${System.currentTimeMillis()}",
    val studentId: String,
    val studentName: String,
    val className: String,
    val parentName: String,
    val parentPhone: String,
    val parentEmail: String = "",
    val hasSiblings: Boolean = true,
    val siblings: List<SiblingEntry> = emptyList(),
    val submittedAt: String = "15 Nov 2026, 02:30 PM"
)

data class MistakeAnalysisItem(
    val id: String = "mis-${System.currentTimeMillis()}",
    val subject: String,
    val examName: String,
    val mistakeTitle: String,
    val questionNumber: String = "",
    val whatWasWrong: String,
    val howToCorrect: String,
    val realWorldExample: String,
    val questionNote: String = "",
    val testSheetImageUri: String? = null,
    val createdAt: String = "15 Nov 2026"
)

data class RosterAttendanceStudent(
    val id: String,
    val rollNo: String,
    val studentName: String,
    val parentPhone: String,
    val status: String = "Present", // "Present", "Absent", "Leave"
    val classId: String = "class-10c"
)

data class SubjectExamResult(
    val subject: String,
    val examName: String,
    val marksObtained: Int,
    val maxMarks: Int,
    val grade: String,
    val teacherRemark: String
)

enum class HolidayScopeType {
    TOTAL_SCHOOL,
    CLASS_SPECIFIC,
    SECTION_SPECIFIC
}

data class SchoolHolidayItem(
    val id: String,
    val dateIso: String, // YYYY-MM-DD
    val title: String,
    val scopeType: HolidayScopeType,
    val targetClassName: String = "", // e.g. "Class 10"
    val targetClassId: String = "",   // e.g. "class-10a"
    val targetLabel: String = "Total School",
    val createdBy: String = "Principal's Office"
)

data class AcademicCalendarEventItem(
    val id: String,
    val dateIso: String,
    val dateLabel: String,
    val title: String,
    val category: String // "EVENT", "ACADEMIC", "CIRCULAR"
)

data class StudentExamMarkSheet(
    val id: String,
    val studentId: String,
    val studentName: String,
    val rollNo: String,
    val classId: String,
    val examName: String,
    val subjectMarks: Map<String, Int>,
    val subjectMaxMarks: Map<String, Int>,
    val totalObtained: Int,
    val totalMax: Int,
    val percentage: Double,
    val assignedGrade: String,
    val performanceTrend: String = "",
    val isApproved: Boolean = false,
    val approvedBy: String = "",
    val approvedAt: String = ""
)

object SchoolGradingPolicy {
    val ruleSummary = "91-100%: A1 • 81-90%: A2 • 71-80%: B1 • 61-70%: B2 • 51-60%: C1 • 41-50%: C2 • 33-40%: D • <33%: E"

    fun assignGrade(percentage: Double): String = when {
        percentage >= 91.0 -> "A1"
        percentage >= 81.0 -> "A2"
        percentage >= 71.0 -> "B1"
        percentage >= 61.0 -> "B2"
        percentage >= 51.0 -> "C1"
        percentage >= 41.0 -> "C2"
        percentage >= 33.0 -> "D"
        else -> "E"
    }
}

data class HelpdeskTicket(
    val ticketId: String,
    val category: String,
    val priority: String,
    val message: String,
    val createdDate: String,
    val status: String = "Open"
)

data class LeaveRequestItem(
    val id: String,
    val studentName: String,
    val className: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val reason: String,
    val status: String = "Pending Review" // "Pending Review", "Approved", "Rejected"
)

data class LibraryBookItem(
    val id: String,
    val title: String,
    val author: String,
    val issueDate: String,
    val dueDate: String,
    val isRenewed: Boolean = false
)

object DiscussionRepository {
    private const val BASE_URL = "https://school-api-569327735820.asia-south1.run.app/api/v1"
    private const val PREFS_NAME = "genesis_student_prefs"
    private const val API_CACHE_PREFS = "genesis_school_api_cache_v1"
    private const val STATIC_CACHE_TTL_MS = 24L * 60L * 60L * 1000L // 24 hours
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private fun readSchoolCache(schoolId: String, key: String, ttlMs: Long = STATIC_CACHE_TTL_MS): String? {
        val ctx = appContext ?: return null
        val prefs = ctx.getSharedPreferences(API_CACHE_PREFS, Context.MODE_PRIVATE)
        val fullKey = "${schoolId}_$key"
        val ts = prefs.getLong("${fullKey}_ts", 0L)
        if (ts == 0L || (System.currentTimeMillis() - ts) > ttlMs) return null
        return prefs.getString(fullKey, null)?.takeIf { it.isNotBlank() }
    }

    private fun writeSchoolCache(schoolId: String, key: String, payload: String) {
        val ctx = appContext ?: return
        val fullKey = "${schoolId}_$key"
        ctx.getSharedPreferences(API_CACHE_PREFS, Context.MODE_PRIVATE).edit()
            .putString(fullKey, payload)
            .putLong("${fullKey}_ts", System.currentTimeMillis())
            .apply()
    }

    fun invalidateSchoolCache(schoolId: String = _activeSchool.value.id, keyPrefix: String = "") {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(API_CACHE_PREFS, Context.MODE_PRIVATE)
        val prefix = if (keyPrefix.isBlank()) "${schoolId}_" else "${schoolId}_$keyPrefix"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        editor.apply()
    }

    private val defaultSchoolClasses: List<SchoolClass> = (1..10).flatMap { grade ->
        val clsName = "Class $grade"
        listOf("A", "B", "C", "D", "E").map { sec ->
            SchoolClass(
                id = "class-$grade${sec.lowercase()}",
                name = "$clsName – Section $sec",
                studentCount = 0,
                className = clsName,
                section = sec
            )
        }
    }

    private val _classOptions = MutableStateFlow((1..10).map { "Class $it" })
    val classOptionsFlow: StateFlow<List<String>> = _classOptions.asStateFlow()
    val classOptions: List<String> get() = _classOptions.value

    private val _sectionOptions = MutableStateFlow(listOf("A", "B", "C", "D", "E"))
    val sectionOptionsFlow: StateFlow<List<String>> = _sectionOptions.asStateFlow()
    val sectionOptions: List<String> get() = _sectionOptions.value

    private val _sectionsByClass = MutableStateFlow(
        (1..10).associate { "Class $it" to listOf("A", "B", "C", "D", "E") }
    )
    val sectionsByClassFlow: StateFlow<Map<String, List<String>>> = _sectionsByClass.asStateFlow()

    private val _classesLoading = MutableStateFlow(false)
    val classesLoadingFlow: StateFlow<Boolean> = _classesLoading.asStateFlow()

    val allSchoolSubjects = listOf(
        "Mathematics",
        "Physics",
        "Chemistry",
        "Biology",
        "Science",
        "English",
        "Hindi",
        "Telugu",
        "Social Studies",
        "Computer Science",
        "Economics",
        "General Knowledge"
    )

    // Class & Section-Wise Customizable Subject Lists (Editable by Teacher/Admin per Class & Section)
    private val initialClassSubjects: Map<String, List<String>> = mapOf(
        "class-10a" to listOf("Mathematics", "Physics", "Chemistry", "Biology", "Telugu", "Hindi", "English", "Social Studies", "Computer Science"),
        "class-10b" to listOf("Mathematics", "Physics", "Chemistry", "Biology", "Telugu", "Hindi", "English", "Social Studies"),
        "class-10c" to listOf("Mathematics", "Physics", "Chemistry", "Biology", "Telugu", "Hindi", "English", "Social Studies"),
        "class-10d" to listOf("Mathematics", "Physics", "Chemistry", "Biology", "Telugu", "Hindi", "English", "Social Studies"),
        "class-9a" to listOf("Mathematics", "Physics", "Chemistry", "Biology", "Telugu", "Hindi", "English", "Social Studies"),
        "class-9b" to listOf("Mathematics", "Physics", "Chemistry", "Biology", "Telugu", "Hindi", "English", "Social Studies"),
        "class-8a" to listOf("Mathematics", "Science", "English", "Social Studies", "Hindi", "Telugu", "Computer Science"),
        "class-5a" to listOf("Mathematics", "English", "Science", "Social Studies", "Hindi", "Telugu"),
        "class-5f" to listOf("Mathematics", "English", "Science", "Social Studies", "Hindi", "Telugu")
    )
    private val _classSubjectsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val classSubjectsMap: StateFlow<Map<String, List<String>>> = _classSubjectsMap.asStateFlow()

    fun getSubjectsForClass(classId: String): List<String> {
        return _classSubjectsMap.value[classId].orEmpty()
    }

    private val _availableClasses = MutableStateFlow(defaultSchoolClasses)
    val availableClassesFlow: StateFlow<List<SchoolClass>> = _availableClasses.asStateFlow()
    val availableClasses: List<SchoolClass> get() = _availableClasses.value

    private fun rebuildClassOptionFlows(classes: List<SchoolClass>) {
        val grouped = linkedMapOf<String, MutableList<String>>()
        val allSections = linkedSetOf<String>()
        classes.forEach { sc ->
            val clsName = sc.className.ifBlank { sc.name.substringBefore("–").trim() }
            val secName = sc.section.ifBlank { sc.name.substringAfter("Section", "A").trim() }
            if (clsName.isNotBlank()) {
                val list = grouped.getOrPut(clsName) { mutableListOf() }
                if (secName.isNotBlank() && !list.any { it.equals(secName, ignoreCase = true) }) {
                    list.add(secName)
                }
            }
            if (secName.isNotBlank()) {
                allSections.add(secName)
            }
        }
        _availableClasses.value = classes
        _classOptions.value = grouped.keys.toList()
        _sectionsByClass.value = grouped.mapValues { it.value.toList() }
        _sectionOptions.value = allSections.toList()
    }

    private val _activeSchool = MutableStateFlow(SchoolBranding())
    val activeSchool: StateFlow<SchoolBranding> = _activeSchool.asStateFlow()

    @Volatile
    private var authToken: String = ""

    // In-memory per-launch flag: shows siblings popup only once per app launch if dismissed;
    // resets automatically only when the app is killed and opened fresh (and only if still not submitted).
    @Volatile
    var siblingsPopupDismissedThisLaunch: Boolean = false

    /**
     * Opens an HttpURLConnection pre-populated with strict multi-tenant school headers
     * (`x-school-id`, `x-school-slug`, and `Authorization` JWT when logged in)
     * so every single API call across the app is 100% isolated per school and role.
     */
    private fun openSchoolHttpConnection(url: URL): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("x-school-id", _activeSchool.value.id)
            setRequestProperty("x-school-slug", _activeSchool.value.slug)
            if (authToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $authToken")
            }
        }
    }

    private fun parseSchoolBranding(obj: JSONObject?): SchoolBranding {
        if (obj == null) return _activeSchool.value
        return SchoolBranding(
            id = obj.optString("id", _activeSchool.value.id),
            schoolCode = obj.optString("school_code", _activeSchool.value.schoolCode),
            name = obj.optString("name", _activeSchool.value.name),
            slug = obj.optString("slug", _activeSchool.value.slug),
            tagline = obj.optString("tagline", _activeSchool.value.tagline),
            logoUrl = obj.optString("logo_url", ""),
            primaryColorHex = obj.optString("primary_color", "#1E40AF"),
            accentColorHex = obj.optString("accent_color", "#38BDF8")
        )
    }

    private fun persistSchoolBranding(context: Context?, branding: SchoolBranding) {
        _activeSchool.value = branding
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putString("school_id", branding.id)
            ?.putString("school_code", branding.schoolCode)
            ?.putString("school_name", branding.name)
            ?.putString("school_slug", branding.slug)
            ?.putString("school_tagline", branding.tagline)
            ?.putString("school_logo_url", branding.logoUrl)
            ?.putString("school_primary_color", branding.primaryColorHex)
            ?.putString("school_accent_color", branding.accentColorHex)
            ?.apply()
    }

    fun resolveSchoolByCode(
        code: String,
        context: Context? = appContext,
        scope: CoroutineScope = ioScope,
        onResult: (Boolean, String, SchoolBranding?) -> Unit = { _, _, _ -> }
    ) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "Please enter a School Code", null)
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/schools/resolve?code=${java.net.URLEncoder.encode(cleanCode, "UTF-8")}")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                val respCode = conn.responseCode
                val body = (if (respCode in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (respCode in 200..299 && json != null && json.optBoolean("success")) {
                    val branding = parseSchoolBranding(json.optJSONObject("data")?.optJSONObject("school"))
                    persistSchoolBranding(context, branding)
                    fetchSchoolClassesFromBackend(scope)
                    withContext(Dispatchers.Main) {
                        onResult(true, "Switched to ${branding.name}", branding)
                    }
                } else {
                    val errMsg = json?.optString("error").orEmpty().ifBlank { "School code '$cleanCode' not found" }
                    withContext(Dispatchers.Main) {
                        onResult(false, errMsg, null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Could not verify school code: ${e.message}", null)
                }
            }
        }
    }

    fun registerWithPassword(
        context: Context,
        schoolCode: String,
        role: AppRole,
        accessCode: String,
        fullName: String,
        phone: String,
        password: String,
        className: String = "Class 10",
        section: String = "A",
        parentEmail: String = "",
        address: String = "",
        childName: String = "",
        employeeId: String = "",
        department: String = "",
        designation: String = "",
        scope: CoroutineScope,
        onResult: (Boolean, String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/auth/register")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-school-id", _activeSchool.value.id)
                    setRequestProperty("x-user-role", role.name)
                    setRequestProperty("x-student-name", fullName)
                }
                val payload = JSONObject().apply {
                    put("school_code", schoolCode.trim().uppercase())
                    put("role", role.name)
                    put("access_code", accessCode.trim().uppercase())
                    put("full_name", fullName.trim())
                    put("phone", phone.trim())
                    put("email", parentEmail.trim())
                    put("password", password)
                    put("class_name", className)
                    put("section", section)
                    put("parent_phone", phone.trim())
                    put("parent_email", parentEmail.trim())
                    put("address", address.trim())
                    put("child_name", childName.trim())
                    put("employee_id", employeeId.trim())
                    put("department", department.trim())
                    put("designation", designation.trim())
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (code in 200..299 && json != null && json.optBoolean("success")) {
                    val data = json.optJSONObject("data")
                    val token = data?.optString("access_token", "").orEmpty()
                    if (token.isNotBlank()) {
                        authToken = token
                    }
                    val branding = parseSchoolBranding(data?.optJSONObject("school"))
                    persistSchoolBranding(context, branding)
                    val userObj = data?.optJSONObject("user")
                    applyAuthenticatedUserSession(context, userObj, role, branding, token, scope)
                    withContext(Dispatchers.Main) {
                        onResult(true, json.optString("message", "Account created successfully"))
                    }
                } else {
                    val err = json?.optString("error").orEmpty().ifBlank { "Sign up failed. Please check your details and secret code." }
                    withContext(Dispatchers.Main) { onResult(false, err) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Unable to connect. Please check your internet and try again.") }
            }
        }
    }

    fun signInWithPassword(
        context: Context,
        schoolCode: String,
        role: AppRole,
        accessCode: String,
        phone: String,
        password: String,
        scope: CoroutineScope,
        onResult: (Boolean, String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/auth/signin")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-school-id", _activeSchool.value.id)
                }
                val payload = JSONObject().apply {
                    put("school_code", schoolCode.trim().uppercase())
                    put("role", role.name)
                    put("access_code", accessCode.trim().uppercase())
                    put("phone", phone.trim())
                    put("password", password)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (code in 200..299 && json != null && json.optBoolean("success")) {
                    val data = json.optJSONObject("data")
                    val token = data?.optString("access_token", "").orEmpty()
                    if (token.isNotBlank()) {
                        authToken = token
                    }
                    val branding = parseSchoolBranding(data?.optJSONObject("school"))
                    persistSchoolBranding(context, branding)
                    val userObj = data?.optJSONObject("user")
                    val roleStr = userObj?.optString("role", role.name).orEmpty().uppercase()
                    val resolvedRole = when (roleStr) {
                        "ADMIN" -> AppRole.ADMIN
                        "TEACHER" -> AppRole.TEACHER
                        "PARENT" -> AppRole.PARENT
                        else -> AppRole.STUDENT
                    }
                    applyAuthenticatedUserSession(context, userObj, resolvedRole, branding, token, scope)
                    withContext(Dispatchers.Main) {
                        onResult(true, json.optString("message", "Signed in!"))
                    }
                } else {
                    val err = json?.optString("error").orEmpty().ifBlank { "Invalid credentials or role secret code." }
                    withContext(Dispatchers.Main) { onResult(false, err) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Unable to connect. Please check your internet and try again.") }
            }
        }
    }

    fun logout(context: Context) {
        authToken = ""
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("role_auth_v2", false)
            .remove("auth_token")
            .remove("user_role")
            .remove("student_id")
            .remove("student_code")
            .remove("roll_no")
            .remove("full_name")
            .remove("class_name")
            .remove("section")
            .remove("parent_phone")
            .remove("parent_email")
            .remove("address")
            .apply()
        currentRole.value = AppRole.STUDENT
        isStudentLoggedIn.value = false
    }

    private fun applyAuthenticatedUserSession(
        context: Context,
        userObj: JSONObject?,
        role: AppRole,
        branding: SchoolBranding,
        token: String,
        scope: CoroutineScope
    ) {
        val uid = userObj?.optString("id", "").orEmpty().ifBlank { "usr-${System.currentTimeMillis()}" }
        val fullName = userObj?.optString("full_name", "User").orEmpty()
        val className = userObj?.optString("class_name", "Class 10").orEmpty().ifBlank { "Class 10" }
        val section = userObj?.optString("section", "A").orEmpty().ifBlank { "A" }
        val parentPhone = userObj?.optString("parent_phone", userObj.optString("phone", "")).orEmpty()
        val parentEmail = userObj?.optString("parent_email", "").orEmpty()
        val address = userObj?.optString("address", "").orEmpty()
        val studentCode = userObj?.optString("student_code", "").orEmpty()
        val rollNo = userObj?.optString("roll_no", "").orEmpty()

        currentRole.value = role
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("role_auth_v2", true)
            .putBoolean("is_profile_locked", true)
            .putString("auth_token", token)
            .putString("user_role", role.name)
            .putString("student_id", uid)
            .putString("student_code", studentCode)
            .putString("roll_no", rollNo)
            .putString("full_name", fullName)
            .putString("class_name", className)
            .putString("section", section)
            .putString("parent_phone", parentPhone)
            .putString("parent_email", parentEmail)
            .putString("address", address)
            .apply()

        val resolvedClassId = userObj?.optString("class_id", "").orEmpty().ifBlank { buildCanonicalClassId(className, section) }
        applyStudentSessionLocally(
            studentId = uid,
            studentCode = studentCode,
            fullName = fullName,
            className = className,
            section = section,
            classId = resolvedClassId,
            parentPhone = parentPhone,
            parentEmail = parentEmail,
            address = address,
            scope = scope
        )
        fetchSchoolClassesFromBackend(scope)
        isStudentLoggedIn.value = true
    }

    fun fetchSchoolClassesFromBackend(scope: CoroutineScope = ioScope, forceRefresh: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            _classesLoading.value = true
            try {
                val schoolId = _activeSchool.value.id
                if (!forceRefresh) {
                    val cached = readSchoolCache(schoolId, "classes")
                    if (cached != null) {
                        val parsed = parseSchoolClassesJson(cached)
                        if (parsed.isNotEmpty()) {
                            rebuildClassOptionFlows(parsed)
                            _classesLoading.value = false
                            return@launch
                        }
                    }
                }
                val url = URL("$BASE_URL/classes?school_id=$schoolId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("x-school-id", schoolId)
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val fetched = parseSchoolClassesJson(body)
                    if (fetched.isNotEmpty()) {
                        writeSchoolCache(schoolId, "classes", body)
                        rebuildClassOptionFlows(fetched)
                    }
                }
            } catch (_: Exception) {
            } finally {
                _classesLoading.value = false
            }
        }
    }

    private val _currentAcademicYear = MutableStateFlow("2026-2027")
    val currentAcademicYear: StateFlow<String> = _currentAcademicYear.asStateFlow()

    fun buildCanonicalClassId(className: String, section: String): String {
        val cleanClass = className.replace(Regex("(?i)^class\\s*"), "").trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        val cleanSec = section.replace(Regex("(?i)^section\\s*"), "").trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        return "class-$cleanClass$cleanSec"
    }

    private val initialStudents = listOf(
        StudentProfile(
            id = "stu-sahasra",
            studentCode = "2258",
            firstName = "Rachoti Sahasra",
            fullName = "Rachoti Sahasra",
            className = "Class 10",
            section = "A",
            bannerClassText = "Class: 10-A (2026-2027)",
            classId = "class-10a",
            classTitle = "Class 10 – Section A",
            avatarUrl = "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&q=80&w=300",
            tuitionFeeInr = 10000,
            examFeeInr = 3000,
            activityFeeInr = 2000,
            feeStatus = "Pending",
            feeDueDate = "15 Oct 2026"
        ),
        StudentProfile(
            id = "stu-laasya",
            studentCode = "1842",
            firstName = "Rachoti Laasya",
            fullName = "Rachoti Laasya",
            className = "Class 9",
            section = "A",
            bannerClassText = "Class: 9-A (2026-2027)",
            classId = "class-9a",
            classTitle = "Class 9 – Section A",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300",
            tuitionFeeInr = 8500,
            examFeeInr = 2500,
            activityFeeInr = 1500,
            feeStatus = "Paid",
            feeDueDate = "Paid on 02 Oct 2026"
        )
    )

    private val _linkedStudents = MutableStateFlow(initialStudents)
    val linkedStudents: StateFlow<List<StudentProfile>> = _linkedStudents.asStateFlow()

    val isStudentLoggedIn = MutableStateFlow(false)
    val currentRole = MutableStateFlow(AppRole.STUDENT)
    val activeStudent = MutableStateFlow(initialStudents[0])
    val selectedClass = MutableStateFlow(
        SchoolClass(
            id = "class-10a",
            name = "Class 10 – Section A",
            studentCount = 0,
            className = "Class 10",
            section = "A"
        )
    )

    // Notices
    private val initialNotices = listOf(
        AdminNotice(
            id = "notice-1",
            title = "Annual Sports Day 2026",
            content = "Join us for an exciting day of sports, track events, teamwork, and house parades! Parents are cordially invited to attend and cheer.",
            category = "EVENT",
            author = "Principal's Office",
            date = "Oct 8, 2026",
            venue = "School Main Sports Ground",
            eventTime = "09:00 AM onwards",
            attachmentName = "Event_Schedule.pdf",
            attachmentSize = "2.4 MB",
            imageUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&q=80&w=800"
        ),
        AdminNotice(
            id = "notice-2",
            title = "Inter-House Science Exhibition Winners",
            content = "Congratulations to all students who presented working models on clean energy and robotics. Certificates will be distributed in Monday assembly.",
            category = "ACADEMIC",
            author = "Science Department",
            date = "Oct 7, 2026",
            venue = "Main Auditorium",
            eventTime = "10:30 AM",
            attachmentName = "Science_Fair_Results.pdf",
            attachmentSize = "1.1 MB",
            imageUrl = "https://images.unsplash.com/photo-1564069114553-7215e1ff1890?auto=format&fit=crop&q=80&w=800"
        ),
        AdminNotice(
            id = "notice-3",
            title = "Half-Yearly Parent Teacher Meeting (PTM)",
            content = "PTM for Classes V to X will be held this Saturday. Please review your ward's report card and subject progress with respective class teachers.",
            category = "CIRCULAR",
            author = "Academic Coordinator",
            date = "Oct 6, 2026",
            venue = "Respective Classrooms",
            eventTime = "08:30 AM – 12:30 PM",
            attachmentName = "PTM_Slot_Timings.pdf",
            attachmentSize = "840 KB"
        )
    )
    private val _homeNotices = MutableStateFlow<List<AdminNotice>>(emptyList())
    val homeNotices: StateFlow<List<AdminNotice>> = _homeNotices.asStateFlow()

    // Notes (Includes instant Dummy Test PDF + full curriculum PDFs)
    private val initialNotes = listOf(
        ClassNote(
            id = "note-dummy-test-10a",
            classId = "class-10a",
            subject = "Mathematics",
            title = "Sample_Dummy_Notes_Test_Download",
            chapter = "Test PDF Download • Formulas & Solved Examples",
            fileUrl = "https://schoolapp.in/notes/Sample_Dummy_Notes_Test_Download.pdf",
            teacherName = "Genesis Academic Cell",
            date = "Today"
        ),
        ClassNote(
            id = "note-1",
            classId = "class-10a",
            subject = "Physics",
            title = "Ray Optics & Lens Formula Summary",
            chapter = "Chapter 10 • Physics",
            fileUrl = "https://schoolapp.in/notes/physics_ch10.pdf",
            teacherName = "Mrs. Gupta",
            date = "Oct 8, 2026"
        ),
        ClassNote(
            id = "note-2",
            classId = "class-10a",
            subject = "Biology",
            title = "Life Processes Notes",
            chapter = "Chapter 6 • Biology",
            fileUrl = "https://schoolapp.in/notes/bio_ch6.pdf",
            teacherName = "Dr. Sharma",
            date = "Oct 6, 2026"
        ),
        ClassNote(
            id = "note-3",
            classId = "class-10a",
            subject = "Mathematics",
            title = "Quadratic Equations Worksheet",
            chapter = "Chapter 4 • Mathematics",
            fileUrl = "https://schoolapp.in/notes/maths_ch4.pdf",
            teacherName = "Mr. Kumar",
            date = "Oct 4, 2026"
        ),
        ClassNote(
            id = "note-4",
            classId = "class-10a",
            subject = "Chemistry",
            title = "Periodic Table Summary",
            chapter = "Chapter 5 • Chemistry",
            fileUrl = "https://schoolapp.in/notes/chem_ch5.pdf",
            teacherName = "Mrs. Iyer",
            date = "Oct 1, 2026"
        ),
        ClassNote(
            id = "note-5",
            classId = "class-10a",
            subject = "English",
            title = "English Grammar Rules",
            chapter = "Chapter 2 • English",
            fileUrl = "https://schoolapp.in/notes/eng_ch2.pdf",
            teacherName = "Mr. Singh",
            date = "Sep 28, 2026"
        ),
        ClassNote(
            id = "note-6",
            classId = "class-9a",
            subject = "Mathematics",
            title = "Polynomials & Algebraic Identities",
            chapter = "Chapter 2 • Mathematics",
            fileUrl = "https://schoolapp.in/notes/class9_poly.pdf",
            teacherName = "Mr. Kumar",
            date = "Oct 7, 2026"
        )
    )
    private val _studyNotes = MutableStateFlow<List<ClassNote>>(emptyList())
    val studyNotes: StateFlow<List<ClassNote>> = _studyNotes.asStateFlow()

    // Discussions
    private val initialPosts = listOf(
        DiscussionPost(
            id = "disc-1",
            classId = "class-10a",
            authorName = "Aarav Sharma",
            authorRole = "STUDENT",
            subject = "Mathematics",
            title = "Quadratic Equations – Ex 4.2 Q3",
            content = "I am not able to understand how to factorize this expression. Can someone explain step by step?",
            hasEquationCard = true,
            equationFormula = "x² - 5x + 6 = 0",
            equationSubtext = "Find the roots.",
            timeAgo = "2 hours ago",
            repliesCount = 2,
            replies = listOf(
                DiscussionReply(
                    id = "rep-1",
                    discussionId = "disc-1",
                    authorName = "Pooja Ma'am",
                    authorRole = "TEACHER",
                    content = "Here we can factorize the expression as:\nx² - 5x + 6 = (x - 2)(x - 3)\nSo, the roots are x = 2 and x = 3.\nLet me know if you need more explanation!",
                    isVerified = true,
                    likesCount = 12,
                    timeAgo = "1 hour ago"
                ),
                DiscussionReply(
                    id = "rep-2",
                    discussionId = "disc-1",
                    authorName = "Rohan",
                    authorRole = "STUDENT",
                    content = "Thanks ma'am! This is clear now.",
                    isVerified = false,
                    likesCount = 3,
                    timeAgo = "45 mins ago"
                )
            )
        ),
        DiscussionPost(
            id = "disc-2",
            classId = "class-10a",
            authorName = "Priya Patel",
            authorRole = "STUDENT",
            subject = "Physics",
            title = "Refraction through a Glass Prism",
            content = "Why does violet light bend the most when white light passes through a glass prism?",
            hasEquationCard = false,
            timeAgo = "4 hours ago",
            repliesCount = 1,
            replies = listOf(
                DiscussionReply(
                    id = "rep-3",
                    discussionId = "disc-2",
                    authorName = "Mrs. Gupta",
                    authorRole = "TEACHER",
                    content = "Violet light has the shortest wavelength in the visible spectrum, so it experiences the highest refractive index in glass and deviates the most.",
                    isVerified = true,
                    likesCount = 8,
                    timeAgo = "3 hours ago"
                )
            )
        ),
        DiscussionPost(
            id = "disc-3",
            classId = "class-9a",
            authorName = "Rachoti Laasya",
            authorRole = "STUDENT",
            subject = "Mathematics",
            title = "Remainder Theorem Doubt – Ex 2.3",
            content = "How to find the remainder when x³ + 3x² + 3x + 1 is divided by x + 1 without long division?",
            hasEquationCard = true,
            equationFormula = "P(x) = x³ + 3x² + 3x + 1",
            equationSubtext = "Substitute x = -1",
            timeAgo = "1 hour ago",
            repliesCount = 1,
            replies = listOf(
                DiscussionReply(
                    id = "rep-4",
                    discussionId = "disc-3",
                    authorName = "Mr. Kumar",
                    authorRole = "TEACHER",
                    content = "By Remainder Theorem, put x = -1 in P(x):\nP(-1) = (-1)³ + 3(-1)² + 3(-1) + 1 = -1 + 3 - 3 + 1 = 0.",
                    isVerified = true,
                    likesCount = 9,
                    timeAgo = "30 mins ago"
                )
            )
        )
    )
    private val _discussions = MutableStateFlow<List<DiscussionPost>>(emptyList())
    val discussions: StateFlow<List<DiscussionPost>> = _discussions.asStateFlow()

    // Timetable
    val defaultTimetable = listOf(
        LessonSlot("t1", "08:00 – 08:45", "Mathematics", "Mr. Kumar", "Ongoing"),
        LessonSlot("t2", "08:45 – 09:30", "Physics", "Mrs. Gupta", "Next"),
        LessonSlot("t3", "09:45 – 10:30", "English", "Mr. Singh", ""),
        LessonSlot("t4", "10:30 – 11:15", "Chemistry", "Mrs. Iyer", ""),
        LessonSlot("t5", "11:30 – 12:15", "Biology", "Dr. Sharma", ""),
        LessonSlot("t6", "12:15 – 01:00", "Computer Science", "Mr. Verma", ""),
        LessonSlot("t7", "02:00 – 02:45", "Social Science", "Mrs. Nair", "")
    )

    // Homework & Projects
    private val initialHomework = listOf(
        HomeworkItem(
            id = "hw-1",
            classId = "class-10a",
            subject = "Physics",
            title = "Physics Lab Manual Observation & Ray Optics",
            dueText = "Due Tomorrow, 08:30 AM",
            status = "Due Soon",
            instructions = "Complete observation tables 1 & 2 for focal length of convex lens using u-v graphical method. Draw neat ray diagrams with pencil.",
            maxMarks = 20,
            deadlineDateIso = "2026-10-18",
            totalStudents = 40,
            submittedCount = 34,
            attachedDocName = "Physics_Optics_Lab_Manual.pdf",
            attachedDocSize = "2.1 MB",
            projectType = "Assignment"
        ),
        HomeworkItem(
            id = "hw-2",
            classId = "class-10a",
            subject = "Mathematics",
            title = "Quadratic Equations Real-World Application Project",
            dueText = "Due 22 Oct 2026",
            status = "Pending",
            instructions = "Formulate 5 quadratic equations modeling projectile paths and bridge parabolic arches with calculations and graph plots.",
            maxMarks = 25,
            deadlineDateIso = "2026-10-22",
            totalStudents = 40,
            submittedCount = 28,
            attachedDocName = "Maths_Quadratic_Project_Brief.pdf",
            attachedDocSize = "850 KB",
            projectType = "Project"
        ),
        HomeworkItem(
            id = "hw-3",
            classId = "class-10a",
            subject = "Chemistry",
            title = "Acids, Bases & Salts Chemical Reaction Chart",
            dueText = "Due 25 Oct 2026",
            status = "Pending",
            instructions = "Tabulate common salts, their chemical formulas, pH values, and industrial preparation equations (Chlor-alkali process).",
            maxMarks = 15,
            deadlineDateIso = "2026-10-25",
            totalStudents = 40,
            submittedCount = 19,
            attachedDocName = "Chemistry_Salts_Chart_Template.pdf",
            attachedDocSize = "1.2 MB",
            projectType = "Assignment"
        ),
        HomeworkItem(
            id = "hw-4",
            classId = "class-10c",
            subject = "Biology",
            title = "Life Processes - Heart & Blood Circulation Diagram",
            dueText = "Due Tomorrow, 08:30 AM",
            status = "Due Soon",
            instructions = "Draw a double circulation schematic diagram labeling oxygenated and deoxygenated blood pathways with notes on valve functions.",
            maxMarks = 15,
            deadlineDateIso = "2026-10-18",
            totalStudents = 38,
            submittedCount = 31,
            attachedDocName = "Circulation_Diagram_Guide.pdf",
            attachedDocSize = "3.4 MB",
            projectType = "Assignment"
        )
    )
    private val _homeworkList = MutableStateFlow<List<HomeworkItem>>(emptyList())
    val homeworkList: StateFlow<List<HomeworkItem>> = _homeworkList.asStateFlow()

    // Examination Schedules
    private val initialExamSchedules = listOf(
        ExamScheduleItem(
            id = "es-1",
            classId = "class-10a",
            examName = "Half-Yearly Exam",
            subject = "Mathematics",
            dateText = "24 Oct 2026",
            dateIso = "2026-10-24",
            timeSlot = "09:30 AM – 12:30 PM",
            roomNo = "Hall 3A",
            syllabus = "Ch 1: Real Numbers, Ch 2: Polynomials, Ch 3: Linear Equations, Ch 4: Quadratic Equations, Ch 6: Triangles",
            preparationTips = "Focus on 4-mark theorem proofs (BPT) and quadratic word problems.",
            maxMarks = 80
        ),
        ExamScheduleItem(
            id = "es-2",
            classId = "class-10a",
            examName = "Half-Yearly Exam",
            subject = "Science",
            dateText = "27 Oct 2026",
            dateIso = "2026-10-27",
            timeSlot = "09:30 AM – 12:30 PM",
            roomNo = "Hall 3A",
            syllabus = "Physics: Light & Electricity; Chemistry: Chemical Reactions & Acids-Bases; Biology: Life Processes",
            preparationTips = "Practice ray diagrams and chemical balanced equations with states.",
            maxMarks = 80
        ),
        ExamScheduleItem(
            id = "es-3",
            classId = "class-10a",
            examName = "Half-Yearly Exam",
            subject = "English Language & Lit",
            dateText = "30 Oct 2026",
            dateIso = "2026-10-30",
            timeSlot = "09:30 AM – 12:30 PM",
            roomNo = "Hall 3A",
            syllabus = "Reading Comprehension, Analytical Paragraph, Letter to Editor, First Flight Ch 1-6",
            preparationTips = "Format of formal letter and analytical paragraph carries 10 marks.",
            maxMarks = 80
        ),
        ExamScheduleItem(
            id = "es-4",
            classId = "class-10a",
            examName = "Half-Yearly Exam",
            subject = "Social Science",
            dateText = "03 Nov 2026",
            dateIso = "2026-11-03",
            timeSlot = "09:30 AM – 12:30 PM",
            roomNo = "Hall 3A",
            syllabus = "History: Nationalism in Europe; Geo: Resources & Agriculture; Civics: Power Sharing",
            preparationTips = "Practice outline map of India for major dams and agricultural crops.",
            maxMarks = 80
        ),
        ExamScheduleItem(
            id = "es-5",
            classId = "class-10c",
            examName = "Half-Yearly Exam",
            subject = "Mathematics",
            dateText = "24 Oct 2026",
            dateIso = "2026-10-24",
            timeSlot = "09:30 AM – 12:30 PM",
            roomNo = "Hall 2C",
            syllabus = "Ch 1: Real Numbers, Ch 2: Polynomials, Ch 3: Linear Equations, Ch 4: Quadratic Equations",
            preparationTips = "Focus on formula sheet revision and step-by-step calculations.",
            maxMarks = 80
        )
    )
    private var lastLoadedHomeClassId: String? = null

    fun shouldLoadHomeFeedOnce(classId: String): Boolean {
        if (lastLoadedHomeClassId == classId) return false
        lastLoadedHomeClassId = classId
        return true
    }

    private val _examSchedules = MutableStateFlow<List<ExamScheduleItem>>(emptyList())
    val examSchedules: StateFlow<List<ExamScheduleItem>> = _examSchedules.asStateFlow()

    // Sibling Leads & Submissions
    private val _isSiblingsFormSubmitted = MutableStateFlow(false)
    val isSiblingsFormSubmitted: StateFlow<Boolean> = _isSiblingsFormSubmitted.asStateFlow()

    private val initialSiblingSubmissions = listOf(
        SiblingSubmission(
            id = "sub-1",
            studentId = "stu-aarav",
            studentName = "Aarav Sharma",
            className = "Class 10 – Section A",
            parentName = "Rajesh Sharma",
            parentPhone = "+91 98765 43210",
            hasSiblings = true,
            siblings = listOf(
                SiblingEntry(
                    id = "sib-101",
                    fullName = "Rohan Sharma",
                    age = 8,
                    currentGrade = "Class 3",
                    schoolType = "OTHER",
                    schoolName = "Oakridge International School",
                    reasonOtherSchool = "Closer to earlier house. Now relocated near Beeramguda.",
                    targetAdmissionYear = "2026-2027",
                    admissionStatus = "NEW_LEAD"
                )
            ),
            submittedAt = "14 Nov 2026, 05:20 PM"
        ),
        SiblingSubmission(
            id = "sub-2",
            studentId = "stu-sahasra",
            studentName = "Rachoti Sahasra",
            className = "Class 10 – Section A",
            parentName = "Suresh Rachoti",
            parentPhone = "+91 98480 12345",
            hasSiblings = true,
            siblings = listOf(
                SiblingEntry(
                    id = "sib-102",
                    fullName = "Rachoti Ishaan",
                    age = 5,
                    currentGrade = "UKG",
                    schoolType = "OTHER",
                    schoolName = "EuroKids Preschool",
                    reasonOtherSchool = "Pre-primary nearby. Looking for Class 1 admission.",
                    targetAdmissionYear = "2026-2027",
                    admissionStatus = "NEW_LEAD"
                )
            ),
            submittedAt = "15 Nov 2026, 10:15 AM"
        ),
        SiblingSubmission(
            id = "sub-3",
            studentId = "stu-ananya",
            studentName = "Ananya Reddy",
            className = "Class 10 – Section C",
            parentName = "Venkat Reddy",
            parentPhone = "+91 99887 76655",
            hasSiblings = true,
            siblings = listOf(
                SiblingEntry(
                    id = "sib-103",
                    fullName = "Aditya Reddy",
                    age = 13,
                    currentGrade = "Class 8",
                    schoolType = "GENESIS",
                    schoolName = "Genesis School Beeramguda",
                    reasonOtherSchool = "",
                    targetAdmissionYear = "Already Enrolled",
                    admissionStatus = "ENROLLED"
                )
            ),
            submittedAt = "15 Nov 2026, 11:45 AM"
        )
    )
    private val _siblingSubmissions = MutableStateFlow<List<SiblingSubmission>>(emptyList())
    val siblingSubmissions: StateFlow<List<SiblingSubmission>> = _siblingSubmissions.asStateFlow()

    // Mistake Analysis
    private val initialMistakes = listOf(
        MistakeAnalysisItem(
            id = "mis-1",
            subject = "Mathematics",
            examName = "Half-Yearly Exam",
            mistakeTitle = "Quadratic Formula Sign Confusion (-b)",
            questionNumber = "Q14 (Word Problem)",
            whatWasWrong = "Wrote x = -5 ± √(25 - 24)/2 instead of x = -(-5) ± ... when b = -5.",
            howToCorrect = "Always write the formula with brackets: - (b). If b is -5, then -(-5) = +5.",
            realWorldExample = "Like walking backwards: negative of taking 5 steps backward is moving 5 steps forward.",
            questionNote = "Lost 3 marks in 4-mark question",
            createdAt = "12 Nov 2026"
        ),
        MistakeAnalysisItem(
            id = "mis-2",
            subject = "Physics",
            examName = "Unit Test 2",
            mistakeTitle = "Convex Lens Focal Length Sign Error",
            questionNumber = "Q8 (Optics Numerical)",
            whatWasWrong = "Substituted f = -20 cm instead of +20 cm for a convex lens.",
            howToCorrect = "Convex lens has real principal focus on the right (direction of light), so f is always positive (+). Concave lens is negative (-).",
            realWorldExample = "Convex spectacles for farsightedness have (+) power (e.g. +1.5D); never negative.",
            questionNote = "Ray diagram was correct but numerical answer was wrong",
            createdAt = "10 Nov 2026"
        ),
        MistakeAnalysisItem(
            id = "mis-3",
            subject = "Chemistry",
            examName = "Half-Yearly Exam",
            mistakeTitle = "Changing Subscripts in Balancing Equations",
            questionNumber = "Q5 (Chemical Reactions)",
            whatWasWrong = "Wrote H2 + O2 -> H2O2 to balance water formation instead of 2H2 + O2 -> 2H2O.",
            howToCorrect = "Never change chemical subscripts (which changes the substance into hydrogen peroxide!). Only change front coefficients.",
            realWorldExample = "You can order 2 cups of water (2 H2O), but you cannot change water into bleach (H2O2) to drink!",
            questionNote = "Teacher remark: changed chemical compound identity",
            createdAt = "08 Nov 2026"
        )
    )
    private val _mistakeAnalysisList = MutableStateFlow<List<MistakeAnalysisItem>>(emptyList())
    val mistakeAnalysisList: StateFlow<List<MistakeAnalysisItem>> = _mistakeAnalysisList.asStateFlow()

    // Leave Requests
    private val initialLeaveRequests = listOf(
        LeaveRequestItem(
            id = "lv-1",
            studentName = "Rachoti Sahasra (2258)",
            className = "Class 10 – Section A",
            leaveType = "Sick Leave",
            startDate = "12 Oct 2026",
            endDate = "13 Oct 2026",
            reason = "Viral fever and doctor consultation.",
            status = "Approved"
        )
    )
    private val _leaveRequests = MutableStateFlow<List<LeaveRequestItem>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRequestItem>> = _leaveRequests.asStateFlow()

    // Library Books
    private val initialBooks = listOf(
        LibraryBookItem("bk-1", "Concepts of Physics (Vol 1)", "H.C. Verma", "01 Oct 2026", "15 Oct 2026", false),
        LibraryBookItem("bk-2", "NCERT Mathematics Exemplar X", "NCERT Publication", "04 Oct 2026", "18 Oct 2026", false)
    )
    private val _libraryBooks = MutableStateFlow(initialBooks)
    val libraryBooks: StateFlow<List<LibraryBookItem>> = _libraryBooks.asStateFlow()

    // Teacher Roll Call & Student Attendance (Multi-Class & Section Roster)
    private val initialRoster = listOf(
        // Class 10 – Section C (Teacher Command Section)
        RosterAttendanceStudent("r-10c-1", "01", "Naetra Bilal", "+91 98480 99881", "Present", "class-10c"),
        RosterAttendanceStudent("r-10c-2", "02", "Aryan Sharma", "+91 98480 33441", "Present", "class-10c"),
        RosterAttendanceStudent("r-10c-3", "03", "Ananya Deshmukh", "+91 97001 22334", "Absent", "class-10c"),
        RosterAttendanceStudent("r-10c-4", "04", "Reyansh Varma", "+91 99081 77665", "Present", "class-10c"),
        RosterAttendanceStudent("r-10c-5", "05", "Tanvi Kulkarni", "+91 98221 44556", "Absent", "class-10c"),
        RosterAttendanceStudent("r-10c-6", "06", "Mohammed Zaid", "+91 97012 88990", "Leave", "class-10c"),

        // Class 10 – Section A
        RosterAttendanceStudent("r1", "01", "Rachoti Sahasra", "+91 98765 43210", "Present", "class-10a"),
        RosterAttendanceStudent("r2", "02", "Aarav Sharma", "+91 98480 11223", "Present", "class-10a"),
        RosterAttendanceStudent("r3", "03", "Priya Patel", "+91 99890 44556", "Present", "class-10a"),
        RosterAttendanceStudent("r4", "04", "Karthikeya Reddy", "+91 90001 77889", "Absent", "class-10a"),
        RosterAttendanceStudent("r5", "05", "Sneha Rao", "+91 97012 33445", "Present", "class-10a"),
        RosterAttendanceStudent("r6", "06", "Rohan Verma", "+91 91234 55667", "Leave", "class-10a"),

        // Class 10 – Section B
        RosterAttendanceStudent("r-10b-1", "01", "Rahul Deshmukh", "+91 98111 22334", "Present", "class-10b"),
        RosterAttendanceStudent("r-10b-2", "02", "Meera Krishnan", "+91 98222 33445", "Present", "class-10b"),
        RosterAttendanceStudent("r-10b-3", "03", "Sameer Joshi", "+91 99333 44556", "Absent", "class-10b"),

        // Class 9 – Section A
        RosterAttendanceStudent("r-9a-1", "01", "Rachoti Laasya", "+91 98765 43210", "Present", "class-9a"),
        RosterAttendanceStudent("r-9a-2", "02", "Ishaan Verma", "+91 98481 22334", "Present", "class-9a"),
        RosterAttendanceStudent("r-9a-3", "03", "Kavya Nair", "+91 99492 33445", "Leave", "class-9a"),

        // Class 9 – Section B
        RosterAttendanceStudent("r-9b-1", "01", "Siddharth Rao", "+91 98101 44556", "Present", "class-9b"),
        RosterAttendanceStudent("r-9b-2", "02", "Rhea Sengupta", "+91 98202 55667", "Absent", "class-9b"),

        // Class 8 – Section A
        RosterAttendanceStudent("r-8a-1", "01", "Aditya Kulkarni", "+91 98303 66778", "Present", "class-8a"),
        RosterAttendanceStudent("r-8a-2", "02", "Pooja Hegde", "+91 98404 77889", "Present", "class-8a"),

        // Class 5 – Section A
        RosterAttendanceStudent("r-5a-1", "01", "Dhruv Patil", "+91 98505 88990", "Present", "class-5a"),
        RosterAttendanceStudent("r-5a-2", "02", "Anika Roy", "+91 98606 99001", "Present", "class-5a"),

        // Class 5 – Section F
        RosterAttendanceStudent("r-5f-1", "01", "Arjun Reddy", "+91 98707 11223", "Present", "class-5f"),
        RosterAttendanceStudent("r-5f-2", "02", "Diya Joseph", "+91 98808 22334", "Absent", "class-5f")
    )
    private val _rosterAttendance = MutableStateFlow<List<RosterAttendanceStudent>>(emptyList())
    val rosterAttendance: StateFlow<List<RosterAttendanceStudent>> = _rosterAttendance.asStateFlow()

    private val _submittedClassesAttendance = MutableStateFlow<Map<String, String>>(emptyMap())
    val submittedClassesAttendance: StateFlow<Map<String, String>> = _submittedClassesAttendance.asStateFlow()

    fun submitClassAttendance(classId: String) {
        val timeLabel = nowTimeLabel()
        _submittedClassesAttendance.value = _submittedClassesAttendance.value + (classId to timeLabel)
        val currentRoster = _rosterAttendance.value.filter { it.classId == classId }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/attendance")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val recordsArr = JSONArray()
                currentRoster.forEach { stu ->
                    recordsArr.put(
                        JSONObject().apply {
                            put("student_id", stu.id)
                            put("student_name", stu.studentName)
                            put("roll_no", stu.rollNo)
                            put("status", stu.status.uppercase())
                            put("remarks", "Marked via Teacher 30s Roll Call")
                        }
                    )
                }
                val payload = JSONObject().apply {
                    put("class_id", classId)
                    put("records", recordsArr)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun isClassAttendanceSubmitted(classId: String): String? = _submittedClassesAttendance.value[classId]

    // Finished Exams & Subject-Wise Results (Loaded per class from PostgreSQL)
    private val _availableExams = MutableStateFlow<List<String>>(emptyList())
    val availableExamsFlow: StateFlow<List<String>> = _availableExams.asStateFlow()
    val availableExams: List<String> get() = _availableExams.value

    fun addNewExamName(examName: String): Boolean {
        val cleanName = examName.trim()
        if (cleanName.isBlank()) return false
        if (_availableExams.value.any { it.equals(cleanName, ignoreCase = true) }) return false
        _availableExams.value = _availableExams.value + cleanName

        val currentClassId = selectedClass.value.id
        val defaultMax = if (cleanName.startsWith("Unit Test", ignoreCase = true)) 25 else 100
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/classes/$currentClassId/exams")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val payload = JSONObject().apply {
                    put("title", cleanName)
                    put("term", cleanName)
                    put("class_id", currentClassId)
                    put("default_max_marks", defaultMax)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return true
    }

    private val initialExamResultsByExam: Map<String, List<SubjectExamResult>> = mapOf(
        "Unit Test 1" to listOf(
            SubjectExamResult("Mathematics", "Unit Test 1", 24, 25, "", ""),
            SubjectExamResult("Physics", "Unit Test 1", 23, 25, "", ""),
            SubjectExamResult("Chemistry", "Unit Test 1", 21, 25, "", ""),
            SubjectExamResult("Biology", "Unit Test 1", 24, 25, "", ""),
            SubjectExamResult("Telugu", "Unit Test 1", 23, 25, "", ""),
            SubjectExamResult("Hindi", "Unit Test 1", 22, 25, "", ""),
            SubjectExamResult("English", "Unit Test 1", 24, 25, "", ""),
            SubjectExamResult("Social Studies", "Unit Test 1", 22, 25, "", ""),
            SubjectExamResult("Computer Science", "Unit Test 1", 25, 25, "", "")
        ),
        "Unit Test 2" to listOf(
            SubjectExamResult("Mathematics", "Unit Test 2", 25, 25, "", ""),
            SubjectExamResult("Physics", "Unit Test 2", 22, 25, "", ""),
            SubjectExamResult("Chemistry", "Unit Test 2", 23, 25, "", ""),
            SubjectExamResult("Biology", "Unit Test 2", 23, 25, "", ""),
            SubjectExamResult("Telugu", "Unit Test 2", 24, 25, "", ""),
            SubjectExamResult("Hindi", "Unit Test 2", 22, 25, "", ""),
            SubjectExamResult("English", "Unit Test 2", 24, 25, "", ""),
            SubjectExamResult("Social Studies", "Unit Test 2", 21, 25, "", ""),
            SubjectExamResult("Computer Science", "Unit Test 2", 24, 25, "", "")
        ),
        "Quarterly Exam" to listOf(
            SubjectExamResult("Mathematics", "Quarterly Exam", 94, 100, "", ""),
            SubjectExamResult("Physics", "Quarterly Exam", 89, 100, "", ""),
            SubjectExamResult("Chemistry", "Quarterly Exam", 87, 100, "", ""),
            SubjectExamResult("Biology", "Quarterly Exam", 92, 100, "", ""),
            SubjectExamResult("Telugu", "Quarterly Exam", 91, 100, "", ""),
            SubjectExamResult("Hindi", "Quarterly Exam", 86, 100, "", ""),
            SubjectExamResult("English", "Quarterly Exam", 92, 100, "", ""),
            SubjectExamResult("Social Studies", "Quarterly Exam", 88, 100, "", ""),
            SubjectExamResult("Computer Science", "Quarterly Exam", 96, 100, "", "")
        ),
        "Half-Yearly Exam" to listOf(
            SubjectExamResult("Mathematics", "Half-Yearly Exam", 96, 100, "", ""),
            SubjectExamResult("Physics", "Half-Yearly Exam", 92, 100, "", ""),
            SubjectExamResult("Chemistry", "Half-Yearly Exam", 89, 100, "", ""),
            SubjectExamResult("Biology", "Half-Yearly Exam", 94, 100, "", ""),
            SubjectExamResult("Telugu", "Half-Yearly Exam", 93, 100, "", ""),
            SubjectExamResult("Hindi", "Half-Yearly Exam", 88, 100, "", ""),
            SubjectExamResult("English", "Half-Yearly Exam", 95, 100, "", ""),
            SubjectExamResult("Social Studies", "Half-Yearly Exam", 90, 100, "", ""),
            SubjectExamResult("Computer Science", "Half-Yearly Exam", 98, 100, "", "")
        ),
        "Final Annual Exam" to listOf(
            SubjectExamResult("Mathematics", "Final Annual Exam", 97, 100, "", ""),
            SubjectExamResult("Physics", "Final Annual Exam", 94, 100, "", ""),
            SubjectExamResult("Chemistry", "Final Annual Exam", 91, 100, "", ""),
            SubjectExamResult("Biology", "Final Annual Exam", 95, 100, "", ""),
            SubjectExamResult("Telugu", "Final Annual Exam", 95, 100, "", ""),
            SubjectExamResult("Hindi", "Final Annual Exam", 90, 100, "", ""),
            SubjectExamResult("English", "Final Annual Exam", 96, 100, "", ""),
            SubjectExamResult("Social Studies", "Final Annual Exam", 92, 100, "", ""),
            SubjectExamResult("Computer Science", "Final Annual Exam", 99, 100, "", "")
        )
    )
    private val _examResultsByExam = MutableStateFlow<Map<String, List<SubjectExamResult>>>(emptyMap())
    val examResultsByExam: StateFlow<Map<String, List<SubjectExamResult>>> = _examResultsByExam.asStateFlow()

    private val _examResultsFlow = MutableStateFlow<List<SubjectExamResult>>(emptyList())
    val examResultsFlow: StateFlow<List<SubjectExamResult>> = _examResultsFlow.asStateFlow()
    val examResults: List<SubjectExamResult> get() = _examResultsFlow.value

    // Automated Per-Student Exam Mark Sheets (Enter Marks Once -> Auto Totals, %, Rules-Grade, Report Card, History & Approval)
    private fun buildInitialStudentExamSheets(): List<StudentExamMarkSheet> {
        val sampleClass10A = listOf(
            Triple("stu-sahasra", "Rachoti Sahasra", "01") to 0,
            Triple("stu-aarav-10a", "Aarav Sharma", "02") to -5,
            Triple("stu-priya-10a", "Priya Patel", "03") to -2,
            Triple("stu-karthik-10a", "Karthikeya Reddy", "04") to -11
        )
        val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Social Studies", "Computer Science")
        val sheets = mutableListOf<StudentExamMarkSheet>()
        availableExams.forEachIndexed { examIdx, examName ->
            val baseList = initialExamResultsByExam[examName].orEmpty()
            val isApproved = examName != "Final Annual Exam"
            sampleClass10A.forEach { (stuTuple, offset) ->
                val (stuId, stuName, rollNo) = stuTuple
                val subMarks = linkedMapOf<String, Int>()
                val subMax = linkedMapOf<String, Int>()
                subjects.forEach { sub ->
                    val ref = baseList.find { it.subject == sub }
                    val maxM = ref?.maxMarks ?: if (examName.startsWith("Unit Test")) 25 else 100
                    val scaledOffset = if (maxM <= 25) (offset / 4) else offset
                    val obtM = ((ref?.marksObtained ?: 88) + scaledOffset).coerceIn(0, maxM)
                    subMarks[sub] = obtM
                    subMax[sub] = maxM
                }
                val totObt = subMarks.values.sum()
                val totMax = subMax.values.sum().coerceAtLeast(1)
                val pct = (totObt * 100.0) / totMax
                val prevExamName = availableExams.getOrNull(examIdx - 1)
                val prevSheet = if (prevExamName != null) {
                    sheets.find { it.studentId == stuId && it.examName == prevExamName }
                } else null
                val trend = if (prevSheet != null) {
                    val delta = pct - prevSheet.percentage
                    if (delta >= 0) "+%.1f%% vs %s".format(delta, prevExamName) else "%.1f%% vs %s".format(delta, prevExamName)
                } else {
                    "Baseline Exam"
                }
                sheets.add(
                    StudentExamMarkSheet(
                        id = "sheet-$stuId-$examName",
                        studentId = stuId,
                        studentName = stuName,
                        rollNo = rollNo,
                        classId = "class-10a",
                        examName = examName,
                        subjectMarks = subMarks,
                        subjectMaxMarks = subMax,
                        totalObtained = totObt,
                        totalMax = totMax,
                        percentage = pct,
                        assignedGrade = SchoolGradingPolicy.assignGrade(pct),
                        performanceTrend = trend,
                        isApproved = isApproved,
                        approvedBy = if (isApproved) "Principal & Class Teacher" else "",
                        approvedAt = if (isApproved) "Published" else ""
                    )
                )
            }
        }
        return sheets
    }

    private val _studentExamSheets = MutableStateFlow<List<StudentExamMarkSheet>>(emptyList())
    val studentExamSheets: StateFlow<List<StudentExamMarkSheet>> = _studentExamSheets.asStateFlow()

    // School Holidays (Total School, Class-Specific, Section-Specific) -> Highlighted Green on Calendar
    private val initialHolidays = listOf(
        SchoolHolidayItem("hol-1", "2026-08-15", "Independence Day", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-2", "2026-08-28", "Raksha Bandhan", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-3", "2026-09-14", "Ganesh Chaturthi", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-4", "2026-10-02", "Gandhi Jayanti", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-5", "2026-10-20", "Dussehra Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-6", "2026-10-21", "Vijayadashami", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-7", "2026-11-08", "Diwali Celebration Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-8", "2026-11-14", "Children's Day Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-9", "2026-12-25", "Christmas Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-10", "2027-01-13", "Bhogi Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-11", "2027-01-14", "Sankranti Festival Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-12", "2027-01-15", "Kanuma Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-13", "2027-01-26", "Republic Day", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office"),
        SchoolHolidayItem("hol-14", "2027-03-04", "Holi Holiday", HolidayScopeType.TOTAL_SCHOOL, targetLabel = "Total School", createdBy = "Principal's Office")
    )
    private val _schoolHolidays = MutableStateFlow(initialHolidays)
    val schoolHolidays: StateFlow<List<SchoolHolidayItem>> = _schoolHolidays.asStateFlow()

    val academicCalendarEvents = listOf(
        AcademicCalendarEventItem("ev-1", "2026-11-14", "14 Nov 2026", "Children's Day & Science Expo", "EVENT"),
        AcademicCalendarEventItem("ev-2", "2026-11-23", "23 Nov – 28 Nov", "Unit Test – II Assessments (Classes V to XII)", "ACADEMIC"),
        AcademicCalendarEventItem("ev-3", "2026-12-05", "05 Dec 2026", "Parent-Teacher Review Meeting (PTM)", "CIRCULAR"),
        AcademicCalendarEventItem("ev-4", "2026-12-21", "21 Dec 2026", "Annual Sports Championship Meet", "EVENT"),
        AcademicCalendarEventItem("ev-5", "2027-01-13", "13 Jan – 15 Jan", "Sankranti Festival Holidays", "CIRCULAR"),
        AcademicCalendarEventItem("ev-6", "2027-03-10", "10 Mar 2027", "Annual Final Examinations Commence", "ACADEMIC")
    )

    // Full Academic Year Student Attendance Map ("YYYY-MM-DD" -> "Present" | "Absent")
    private fun buildInitialYearlyAttendance(): Map<String, String> {
        val holidayDates = initialHolidays.map { it.dateIso }.toSet()
        val absentDates = setOf(
            "2026-06-18", "2026-07-09", "2026-07-24",
            "2026-08-12", "2026-09-03", "2026-09-22",
            "2026-10-14", "2026-10-29"
        )
        val result = linkedMapOf<String, String>()
        var cursor = java.time.LocalDate.of(2026, 6, 1)
        val endDate = java.time.LocalDate.of(2026, 11, 10)
        while (!cursor.isAfter(endDate)) {
            val iso = cursor.toString()
            if (cursor.dayOfWeek != java.time.DayOfWeek.SUNDAY && iso !in holidayDates) {
                result[iso] = if (iso in absentDates) "Absent" else "Present"
            }
            cursor = cursor.plusDays(1)
        }
        return result
    }

    private val _studentYearlyAttendance = MutableStateFlow<Map<String, String>>(emptyMap())
    val studentYearlyAttendance: StateFlow<Map<String, String>> = _studentYearlyAttendance.asStateFlow()

    // Section-Wise Student Login & Verification Roster (Per Class & Section)
    private val initialSectionStudents = listOf(
        SectionRosterStudent(
            id = "stu-sahasra",
            studentCode = "2258",
            rollNo = "01",
            fullName = "Rachoti Sahasra",
            classId = "class-10a",
            className = "Class 10",
            section = "A",
            parentPhone = "+91 98765 43210",
            loginTimestamp = "Logged in • Verified",
            verificationStatus = "VERIFIED",
            verifiedBy = "Mrs. Gupta",
            isCurrentDeviceStudent = true
        ),
        SectionRosterStudent(
            id = "stu-aarav-10a",
            studentCode = "2261",
            rollNo = "02",
            fullName = "Aarav Sharma",
            classId = "class-10a",
            className = "Class 10",
            section = "A",
            parentPhone = "+91 98480 11223",
            loginTimestamp = "Logged in Today, 07:55 AM",
            verificationStatus = "VERIFIED",
            verifiedBy = "Mrs. Gupta"
        ),
        SectionRosterStudent(
            id = "stu-priya-10a",
            studentCode = "2269",
            rollNo = "03",
            fullName = "Priya Patel",
            classId = "class-10a",
            className = "Class 10",
            section = "A",
            parentPhone = "+91 99890 44556",
            loginTimestamp = "Logged in Today, 08:04 AM",
            verificationStatus = "PENDING"
        ),
        SectionRosterStudent(
            id = "stu-karthik-10a",
            studentCode = "2274",
            rollNo = "04",
            fullName = "Karthikeya Reddy",
            classId = "class-10a",
            className = "Class 10",
            section = "A",
            parentPhone = "+91 90001 77889",
            loginTimestamp = "Logged in Today, 08:11 AM",
            verificationStatus = "PENDING"
        ),
        SectionRosterStudent(
            id = "stu-suspect-10a",
            studentCode = "9981",
            rollNo = "44",
            fullName = "Vikram Unknown (Suspected Entry)",
            classId = "class-10a",
            className = "Class 10",
            section = "A",
            parentPhone = "+91 90000 00099",
            loginTimestamp = "Logged in Yesterday, 09:40 PM",
            verificationStatus = "BLOCKED",
            blockedReason = "Not on Class 10-A physical register during classroom verification",
            verifiedBy = "Mrs. Gupta"
        ),
        // Class 10-B Isolated Roster
        SectionRosterStudent(
            id = "stu-rahul-10b",
            studentCode = "3104",
            rollNo = "01",
            fullName = "Rahul Deshmukh",
            classId = "class-10b",
            className = "Class 10",
            section = "B",
            parentPhone = "+91 98111 22334",
            loginTimestamp = "Logged in Today, 08:00 AM",
            verificationStatus = "VERIFIED",
            verifiedBy = "Mr. Kumar"
        ),
        SectionRosterStudent(
            id = "stu-meera-10b",
            studentCode = "3109",
            rollNo = "02",
            fullName = "Meera Krishnan",
            classId = "class-10b",
            className = "Class 10",
            section = "B",
            parentPhone = "+91 98222 33445",
            loginTimestamp = "Logged in Today, 08:18 AM",
            verificationStatus = "PENDING"
        ),
        // Class 9-A Isolated Roster
        SectionRosterStudent(
            id = "stu-laasya",
            studentCode = "1842",
            rollNo = "01",
            fullName = "Rachoti Laasya",
            classId = "class-9a",
            className = "Class 9",
            section = "A",
            parentPhone = "+91 98765 43210",
            loginTimestamp = "Logged in • Verified",
            verificationStatus = "VERIFIED",
            verifiedBy = "Mr. Kumar"
        )
    )
    private val _sectionStudentRoster = MutableStateFlow<List<SectionRosterStudent>>(emptyList())
    val sectionStudentRoster: StateFlow<List<SectionRosterStudent>> = _sectionStudentRoster.asStateFlow()

    // Central Automation Engine Rules (WHEN -> CHECK CONDITION -> THEN -> RECORD)
    private val initialAutomationRules = listOf(
        AutomationRuleItem(
            id = "auto-absent",
            title = "Absentee Instant Parent Notification",
            whenTrigger = "Student marked Absent in Roll Call",
            checkCondition = "No approved prior leave on record for today",
            thenAction = "Send push alert to parent with 1-tap Absence Explanation form",
            recordTarget = "Attendance Audit Log & Parent Communication Ledger",
            category = "Attendance",
            isEnabled = true,
            executionsCount = 412,
            lastTriggeredText = "Today, 08:12 AM"
        ),
        AutomationRuleItem(
            id = "auto-fee-overdue",
            title = "5-Day Overdue Fee Payment Reminder",
            whenTrigger = "Fee installment overdue by 5 days",
            checkCondition = "Outstanding student balance > ₹0",
            thenAction = "Send automated payment reminder with 1-tap UPI link to parent",
            recordTarget = "Fee Reminder History & Admin Collection Dashboard",
            category = "Fees",
            isEnabled = true,
            executionsCount = 312,
            lastTriggeredText = "Today, 09:00 AM"
        ),
        AutomationRuleItem(
            id = "auto-fee-receipt",
            title = "Instant Payment Webhook & Auto-Receipt",
            whenTrigger = "Parent completes online UPI / Fee Payment",
            checkCondition = "Payment gateway signature verified",
            thenAction = "Update student fee ledger, generate PDF receipt & notify parent",
            recordTarget = "Daily Collection Ledger & Reconciliation Audit",
            category = "Fees",
            isEnabled = true,
            executionsCount = 489,
            lastTriggeredText = "10 mins ago"
        ),
        AutomationRuleItem(
            id = "auto-hw-distribute",
            title = "Homework Assigned & Deadline Reminder",
            whenTrigger = "Teacher assigns homework or approaches 24h deadline",
            checkCondition = "Student submission status is Pending",
            thenAction = "Notify students & include in Parent Consolidated Daily Summary",
            recordTarget = "Class Homework Tracker",
            category = "Academics",
            isEnabled = true,
            executionsCount = 860,
            lastTriggeredText = "Today, 10:30 AM"
        ),
        AutomationRuleItem(
            id = "auto-teacher-sub",
            title = "Teacher Leave -> Auto Substitution Workflow",
            whenTrigger = "Teacher submits leave application",
            checkCondition = "Teacher has scheduled classes today",
            thenAction = "Identify affected periods, suggest conflict-free substitutes & alert Principal",
            recordTarget = "Timetable & Substitution Register",
            category = "Operations",
            isEnabled = true,
            executionsCount = 34,
            lastTriggeredText = "Today, 07:40 AM"
        ),
        AutomationRuleItem(
            id = "auto-marks-grade",
            title = "Single-Entry Marks -> Auto Report Card & Proud Parent Status Card",
            whenTrigger = "Teacher enters subject examination marks",
            checkCondition = "Marks within valid max-marks range (or Grade A1/A2/90%+)",
            thenAction = "Calculate CBSE grades, generate Report Card & unlock Branded WhatsApp Status Card + Referral QR for Parent",
            recordTarget = "Academic Records & Parent WhatsApp Growth Engine",
            category = "Academics",
            isEnabled = true,
            executionsCount = 380,
            lastTriggeredText = "Yesterday, 03:15 PM"
        ),
        AutomationRuleItem(
            id = "auto-attendance-shortage",
            title = "Attendance Shortage Early-Warning (<75%)",
            whenTrigger = "Student rolling 30-day attendance drops below 75%",
            checkCondition = "Student is active enrollment",
            thenAction = "Alert Class Teacher, Principal & Guardian before term exams",
            recordTarget = "School Intelligence At-Risk Register",
            category = "Attendance",
            isEnabled = true,
            executionsCount = 19,
            lastTriggeredText = "Yesterday, 05:00 PM"
        ),
        AutomationRuleItem(
            id = "auto-admission-lead",
            title = "Proud Parent WhatsApp QR Scan -> Counsellor Auto-Assign",
            whenTrigger = "Friend scans Parent WhatsApp Status QR or submits enquiry",
            checkCondition = "Referral code valid & seats available in requested grade",
            thenAction = "Log CRM lead with referring student ID, assign counsellor, apply ₹5,000 scholarship tag & set 24h follow-up",
            recordTarget = "Admissions CRM Pipeline & School ROI Tracker",
            category = "Admissions",
            isEnabled = true,
            executionsCount = 94,
            lastTriggeredText = "2 hours ago"
        )
    )
    private val _automationRules = MutableStateFlow(initialAutomationRules)
    val automationRules: StateFlow<List<AutomationRuleItem>> = _automationRules.asStateFlow()

    // Financial Leakage Prevention System Flags (For Human Verification)
    private val initialLeakageAlerts = listOf(
        FinancialLeakageAlert(
            id = "leak-1",
            alertType = "Overdue Installment",
            title = "14 Students Overdue > 15 Days in Class 10 & 9",
            details = "Term-2 tuition installment reminders triggered automatically. 6 parents promised payment by Friday.",
            amountImpactInr = 168000,
            severity = "High"
        ),
        FinancialLeakageAlert(
            id = "leak-2",
            alertType = "Unmatched Receipt",
            title = "2 Offline Cheque Entries Pending Bank Clearance Verification",
            details = "Cheque #448219 (₹15,000) and #882104 (₹12,500) logged at counter; awaiting bank statement match.",
            amountImpactInr = 27500,
            severity = "High"
        ),
        FinancialLeakageAlert(
            id = "leak-3",
            alertType = "Concession Review",
            title = "1 Unapproved 25% Fee Concession Entry Flagged for Principal Review",
            details = "Manual discount applied on Student ID #3109 without attached scholarship approval note.",
            amountImpactInr = 4500,
            severity = "Medium"
        ),
        FinancialLeakageAlert(
            id = "leak-4",
            alertType = "Unfilled Seats",
            title = "9 Vacant Seats Identified in Class 6-A & Class 8-B",
            details = "11 warm admission enquiries in CRM match these exact grades — converting 5 seats adds ₹2,25,000/yr.",
            amountImpactInr = 225000,
            severity = "Medium"
        )
    )
    private val _financialLeakageAlerts = MutableStateFlow(initialLeakageAlerts)
    val financialLeakageAlerts: StateFlow<List<FinancialLeakageAlert>> = _financialLeakageAlerts.asStateFlow()

    // Fee Transactions Ledger (Online UPI + Offline Cash/Cheque/Bank Transfer)
    private val initialFeeTransactions = listOf(
        FeeTransactionRecord("tx-1", "Aarav Sharma", "Class 10-A", 15000, "UPI Auto-Webhook", "REC-2026-8841", "Today, 10:14 AM", "Reconciled"),
        FeeTransactionRecord("tx-2", "Sneha Rao", "Class 10-A", 15000, "UPI Auto-Webhook", "REC-2026-8840", "Today, 09:42 AM", "Reconciled"),
        FeeTransactionRecord("tx-3", "Rahul Deshmukh", "Class 10-B", 12500, "Cash Counter", "REC-2026-8839", "Yesterday, 02:15 PM", "Reconciled"),
        FeeTransactionRecord("tx-4", "Meera Krishnan", "Class 10-B", 12500, "Cheque", "REC-2026-8838", "Yesterday, 11:30 AM", "Pending Bank Match")
    )
    private val _feeTransactions = MutableStateFlow(initialFeeTransactions)
    val feeTransactions: StateFlow<List<FeeTransactionRecord>> = _feeTransactions.asStateFlow()

    // Multi-Branch Comparison Metrics
    val branchMetrics = listOf(
        SchoolBranchMetrics("Beeramguda Main Campus", 842, "94.2%", "91.8%", "₹24.6L"),
        SchoolBranchMetrics("Miyapur Smart Campus", 615, "92.8%", "89.4%", "₹18.2L"),
        SchoolBranchMetrics("Kukatpally Junior Wing", 420, "95.1%", "94.0%", "₹12.8L")
    )

    // Reusable Homework Templates Library (Module 3)
    val homeworkTemplates = listOf(
        HomeworkTemplateItem(
            id = "tpl-math-quad",
            subject = "Mathematics",
            gradeLevel = "Class 10",
            title = "Quadratic Equations – Nature of Roots Practice (10 Qs)",
            instructions = "Solve Q1–Q10 finding discriminant D = b² - 4ac and state nature of roots clearly in your homework notebook.",
            maxMarks = 15
        ),
        HomeworkTemplateItem(
            id = "tpl-phy-ray",
            subject = "Physics",
            gradeLevel = "Class 10",
            title = "Ray Optics – Mirror & Lens Formula Numericals",
            instructions = "Complete the 8 numerical problems on concave mirror and convex lens magnification with sign convention table.",
            maxMarks = 10
        ),
        HomeworkTemplateItem(
            id = "tpl-sci-frac",
            subject = "Mathematics",
            gradeLevel = "Class 6–9",
            title = "Fractions, Decimals & Word Problems Mastery Sheet",
            instructions = "Complete Section A (mental math) and Section B (step-by-step word problems).",
            maxMarks = 10
        ),
        HomeworkTemplateItem(
            id = "tpl-eng-essay",
            subject = "English",
            gradeLevel = "All Classes",
            title = "Formal Letter to Editor & Grammar Transformation",
            instructions = "Write a 150-word formal letter and complete 10 active-to-passive voice sentences.",
            maxMarks = 10
        )
    )

    // Timetable Substitution Requests (Module 7)
    private val initialSubstitutions = listOf(
        TeacherSubstitutionItem(
            id = "sub-1",
            absentTeacherName = "Mr. R. Verma (Chemistry)",
            leaveReason = "Medical Leave Today",
            periodSlot = "Period 3 (10:15 AM – 11:00 AM)",
            classSectionName = "Class 10 – Section A",
            subject = "Chemistry",
            suggestedSubstitutes = listOf("Mrs. Gupta (Free in P3)", "Ms. Kavita Rao (Free in P3)", "Mr. Kumar (1 Sub this week)"),
            assignedSubstitute = "Mrs. Gupta (Free in P3)",
            status = "Assigned & Notified"
        ),
        TeacherSubstitutionItem(
            id = "sub-2",
            absentTeacherName = "Mr. R. Verma (Chemistry)",
            leaveReason = "Medical Leave Today",
            periodSlot = "Period 5 (12:15 PM – 01:00 PM)",
            classSectionName = "Class 9 – Section A",
            subject = "Science",
            suggestedSubstitutes = listOf("Mr. S. Reddy (Free in P5)", "Pooja Ma'am (Free in P5)"),
            assignedSubstitute = null,
            status = "Action Needed"
        )
    )
    private val _teacherSubstitutions = MutableStateFlow(initialSubstitutions)
    val teacherSubstitutions: StateFlow<List<TeacherSubstitutionItem>> = _teacherSubstitutions.asStateFlow()

    // Admissions & Enquiry CRM Pipeline (Module 10)
    private val initialAdmissionEnquiries = listOf(
        AdmissionEnquiryItem(
            id = "adm-1",
            childName = "Vivaan Sharma",
            parentName = "Rajesh Sharma",
            parentPhone = "+91 98491 22330",
            targetGrade = "Class 6",
            counsellorName = "Ms. Anitha (Admissions Desk)",
            stage = "Interview Scheduled",
            nextFollowUp = "Tomorrow, 11:00 AM",
            source = "Parent Referral (REF84A29B)"
        ),
        AdmissionEnquiryItem(
            id = "adm-2",
            childName = "Diya Nair",
            parentName = "Suresh Nair",
            parentPhone = "+91 97015 88991",
            targetGrade = "Class 9",
            counsellorName = "Ms. Anitha (Admissions Desk)",
            stage = "New Enquiry",
            nextFollowUp = "Today, 04:00 PM",
            source = "Website Enquiry Form"
        ),
        AdmissionEnquiryItem(
            id = "adm-3",
            childName = "Ishaan Gupta",
            parentName = "Neha Gupta",
            parentPhone = "+91 99887 66554",
            targetGrade = "Class 5",
            counsellorName = "Mr. Prakash",
            stage = "Admitted",
            nextFollowUp = "Fee Paid & Enrolled",
            source = "Walk-in Campus Visit"
        )
    )
    private val _admissionEnquiries = MutableStateFlow(initialAdmissionEnquiries)
    val admissionEnquiries: StateFlow<List<AdmissionEnquiryItem>> = _admissionEnquiries.asStateFlow()

    // Staff & HR Directory (Module 11)
    private val initialStaff = listOf(
        StaffMemberRecord(
            "stf-1", "Mrs. S. Gupta", "Senior Physics Faculty", "Science Dept", "Present", "10-A, 10-B, 9-A", 24,
            employeeId = "GSB-T-1012", phone = "+91 98490 11001", email = "s.gupta@genesisschool.in",
            qualification = "M.Sc Physics, B.Ed", joiningDate = "12 Jun 2016", subjects = "Physics"
        ),
        StaffMemberRecord(
            "stf-2", "Mr. K. Kumar", "HOD Mathematics", "Mathematics Dept", "Present", "10-A, 10-B, 9-B", 26,
            employeeId = "GSB-T-1007", phone = "+91 98490 11002", email = "k.kumar@genesisschool.in",
            qualification = "M.Sc Mathematics, B.Ed", joiningDate = "03 Jun 2013", subjects = "Mathematics"
        ),
        StaffMemberRecord(
            "stf-3", "Pooja Ma'am", "English & Class Teacher", "Languages Dept", "Not Checked In", "5-F, 8-A, 10-A", 22,
            employeeId = "GSB-T-1031", phone = "+91 98490 11003", email = "pooja@genesisschool.in",
            qualification = "M.A English, B.Ed", joiningDate = "15 Jun 2019", subjects = "English"
        ),
        StaffMemberRecord(
            "stf-4", "Mr. R. Verma", "Chemistry Teacher", "Science Dept", "On Leave", "9-A, 10-A", 20,
            employeeId = "GSB-T-1024", phone = "+91 98490 11004", email = "r.verma@genesisschool.in",
            qualification = "M.Sc Chemistry, B.Ed", joiningDate = "01 Jul 2018", subjects = "Chemistry"
        ),
        StaffMemberRecord(
            "stf-5", "Ms. Anitha Rao", "Senior Admissions Counsellor", "Admin Office", "Present", "Admissions & Front Desk", 30,
            employeeId = "GSB-S-2004", phone = "+91 98490 11005", email = "admissions@genesisschool.in",
            qualification = "MBA (HR)", joiningDate = "10 Jan 2017", subjects = "-", staffType = "Non-Teaching"
        )
    )
    private val _staffDirectory = MutableStateFlow(initialStaff)
    val staffDirectory: StateFlow<List<StaffMemberRecord>> = _staffDirectory.asStateFlow()

    // The staff member signed in on this device when in Teacher / Staff mode
    val currentStaffId = MutableStateFlow("stf-3")

    // Staff Attendance Register (Today + recent history)
    private val initialStaffAttendance = listOf(
        StaffAttendanceDay("sa-stf-1-0", "stf-1", "Today", "07:48 AM", null, "Present"),
        StaffAttendanceDay("sa-stf-2-0", "stf-2", "Today", "07:52 AM", null, "Present"),
        StaffAttendanceDay("sa-stf-3-0", "stf-3", "Today", null, null, "Not Checked In"),
        StaffAttendanceDay("sa-stf-4-0", "stf-4", "Today", null, null, "On Leave"),
        StaffAttendanceDay("sa-stf-5-0", "stf-5", "Today", "08:05 AM", null, "Present"),
        StaffAttendanceDay("sa-stf-3-1", "stf-3", "Mon, 09 Nov", "07:50 AM", "03:42 PM", "Present"),
        StaffAttendanceDay("sa-stf-3-2", "stf-3", "Sat, 07 Nov", "07:55 AM", "01:10 PM", "Present"),
        StaffAttendanceDay("sa-stf-3-3", "stf-3", "Fri, 06 Nov", "08:22 AM", "03:40 PM", "Late"),
        StaffAttendanceDay("sa-stf-3-4", "stf-3", "Thu, 05 Nov", "07:47 AM", "03:45 PM", "Present"),
        StaffAttendanceDay("sa-stf-3-5", "stf-3", "Wed, 04 Nov", null, null, "On Leave"),
        StaffAttendanceDay("sa-stf-3-6", "stf-3", "Tue, 03 Nov", "07:51 AM", "03:38 PM", "Present")
    )
    private val _staffAttendance = MutableStateFlow(initialStaffAttendance)
    val staffAttendance: StateFlow<List<StaffAttendanceDay>> = _staffAttendance.asStateFlow()

    // Staff Leave Applications
    private val initialStaffLeaves = listOf(
        StaffLeaveRequest(
            id = "slv-1", staffId = "stf-4", staffName = "Mr. R. Verma",
            leaveType = "Sick Leave", fromDate = "10 Nov 2026", toDate = "10 Nov 2026", days = 1,
            reason = "Fever – doctor advised rest", status = "Approved", appliedOn = "Yesterday",
            reviewedBy = "Principal", substituteRequested = true
        ),
        StaffLeaveRequest(
            id = "slv-2", staffId = "stf-1", staffName = "Mrs. S. Gupta",
            leaveType = "Casual Leave", fromDate = "12 Nov 2026", toDate = "12 Nov 2026", days = 1,
            reason = "Family function", status = "Pending", appliedOn = "Today"
        ),
        StaffLeaveRequest(
            id = "slv-3", staffId = "stf-3", staffName = "Pooja Ma'am",
            leaveType = "Casual Leave", fromDate = "04 Nov 2026", toDate = "04 Nov 2026", days = 1,
            reason = "Personal work", status = "Approved", appliedOn = "02 Nov 2026", reviewedBy = "Principal"
        )
    )
    private val _staffLeaveRequests = MutableStateFlow(initialStaffLeaves)
    val staffLeaveRequests: StateFlow<List<StaffLeaveRequest>> = _staffLeaveRequests.asStateFlow()

    // Transport & Safety Bus Routes (Module 12)
    private val initialBusRoutes = listOf(
        BusRouteRecord(
            id = "bus-1",
            routeNumber = "Route No. 3",
            routeTitle = "Viman Nagar – Beeramguda Main Gate",
            vehicleNumber = "TS-07-UA-4821",
            driverName = "Ramesh Yadav",
            driverPhone = "+91 94401 22331",
            attendantName = "Lakshmi Devi",
            studentsAssigned = 42,
            tripStatus = "Parked at Campus (Morning Pickup Complete)",
            nextMaintenanceDue = "18 Nov 2026"
        ),
        BusRouteRecord(
            id = "bus-2",
            routeNumber = "Route No. 7",
            routeTitle = "Miyapur Metro – BHEL Township – Campus",
            vehicleNumber = "TS-07-UA-9104",
            driverName = "Srinivas Goud",
            driverPhone = "+91 94408 77665",
            attendantName = "Padma Bai",
            studentsAssigned = 38,
            tripStatus = "Parked at Campus (Morning Pickup Complete)",
            nextMaintenanceDue = "02 Dec 2026"
        )
    )
    private val _busRoutes = MutableStateFlow(initialBusRoutes)
    val busRoutes: StateFlow<List<BusRouteRecord>> = _busRoutes.asStateFlow()

    // Attendance Audit Log & Connected Parent Absence Explanations (Module 2)
    private val _attendanceAuditLog = MutableStateFlow(
        listOf(
            AttendanceAuditEntry(
                id = "aud-1",
                timestamp = "Today, 08:10 AM",
                summary = "Roll #04 Karthikeya Reddy marked Absent by Mrs. Gupta",
                triggeredActions = "Auto-Push sent to Parent (+91 90001 77889) • Absence explanation requested"
            )
        )
    )
    val attendanceAuditLog: StateFlow<List<AttendanceAuditEntry>> = _attendanceAuditLog.asStateFlow()

    private val _parentAbsenceExplanations = MutableStateFlow(
        listOf(
            ParentAbsenceExplanation(
                id = "abs-1",
                studentId = "r4",
                studentName = "Karthikeya Reddy (Roll #04)",
                classSection = "Class 10 – Section A",
                dateText = "Today",
                parentExplanation = "Mild viral fever; visiting pediatrician this morning.",
                status = "Submitted by Parent"
            )
        )
    )
    val parentAbsenceExplanations: StateFlow<List<ParentAbsenceExplanation>> = _parentAbsenceExplanations.asStateFlow()

    // Parent Helpdesk Tickets
    private val initialTickets = listOf(
        HelpdeskTicket(
            ticketId = "GSB-1042",
            category = "Transport",
            priority = "Normal",
            message = "Kindly share updated GPS timing for Route No. 3 morning stop.",
            createdDate = "05 Oct 2026",
            status = "Resolved"
        )
    )
    private val _helpdeskTickets = MutableStateFlow(initialTickets)
    val helpdeskTickets: StateFlow<List<HelpdeskTicket>> = _helpdeskTickets.asStateFlow()

    fun switchRole(role: AppRole) {
        currentRole.value = role
    }

    /**
     * All section data is loaded strictly from PostgreSQL via Go backend APIs.
     */
    private fun ensureIsolatedSectionData(classId: String, classTitle: String) {
        // No local dummy or starter data injected; data comes strictly from PostgreSQL DB
    }

    private var appContext: Context? = null

    /**
     * Called on app startup in MainActivity.
     * Restores persistent student login from SharedPreferences so already-logged-in students
     * go straight to Home screen.
     */
    fun initSession(context: Context, scope: CoroutineScope) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains("school_id")) {
            _activeSchool.value = SchoolBranding(
                id = prefs.getString("school_id", "00000000-0000-0000-0000-000000000001") ?: "00000000-0000-0000-0000-000000000001",
                schoolCode = prefs.getString("school_code", "GENESIS") ?: "GENESIS",
                name = prefs.getString("school_name", "Genesis School Beeramguda") ?: "Genesis School Beeramguda",
                slug = prefs.getString("school_slug", "genesis-high-school") ?: "genesis-high-school",
                tagline = prefs.getString("school_tagline", "Official Student & Parent Portal") ?: "Official Student & Parent Portal",
                logoUrl = prefs.getString("school_logo_url", "") ?: "",
                primaryColorHex = prefs.getString("school_primary_color", "#1E40AF") ?: "#1E40AF",
                accentColorHex = prefs.getString("school_accent_color", "#38BDF8") ?: "#38BDF8"
            )
        }
        val savedRole = prefs.getString("user_role", null)
        if (savedRole != null) {
            runCatching { AppRole.valueOf(savedRole) }.getOrNull()?.let { currentRole.value = it }
        }
        authToken = prefs.getString("auth_token", "") ?: ""
        // Live-refresh active school branding (name, tagline, logo, colors) at most once per 24h
        // so DB-only school branding updates apply automatically while keeping startup API calls minimal.
        val brandingCacheKey = "branding_${_activeSchool.value.schoolCode.uppercase()}"
        if (readSchoolCache(_activeSchool.value.id, brandingCacheKey) == null) {
            resolveSchoolByCode(_activeSchool.value.schoolCode, context, scope) { ok, _, _ ->
                if (ok) writeSchoolCache(_activeSchool.value.id, brandingCacheKey, "ok")
            }
        }
        if (prefs.contains("campus_lat") && prefs.contains("campus_lng")) {
            _schoolCampusGeofence.value = SchoolCampusGeofence(
                schoolName = prefs.getString("campus_name", "Eden Garden, Viman Nagar, Pune")
                    ?: "Eden Garden, Viman Nagar, Pune",
                latitude = prefs.getString("campus_lat", "18.560556")?.toDoubleOrNull() ?: 18.560556,
                longitude = prefs.getString("campus_lng", "73.914722")?.toDoubleOrNull() ?: 73.914722,
                radiusMeters = prefs.getInt("campus_radius", 500),
                isCalibratedFromDevice = prefs.getBoolean("campus_calibrated", false)
            )
        }
        val loggedIn = prefs.getBoolean("is_logged_in", false)
        val hasVerifiedRoleSession = prefs.getBoolean("role_auth_v2", false) && authToken.isNotBlank()
        if (!loggedIn || !hasVerifiedRoleSession) {
            if (loggedIn && !hasVerifiedRoleSession) {
                // Force fresh role-code login for legacy sessions created before role isolation
                prefs.edit().putBoolean("is_logged_in", false).apply()
            }
            isStudentLoggedIn.value = false
            return
        }

        val studentId = prefs.getString("student_id", "stu-sahasra") ?: "stu-sahasra"
        val studentCode = prefs.getString("student_code", "2258") ?: "2258"
        val fullName = prefs.getString("full_name", "Rachoti Sahasra") ?: "Rachoti Sahasra"
        val className = prefs.getString("class_name", "Class 10") ?: "Class 10"
        val section = prefs.getString("section", "A") ?: "A"
        val parentPhone = prefs.getString("parent_phone", "+91 98765 43210") ?: "+91 98765 43210"
        val parentEmail = prefs.getString("parent_email", "") ?: ""
        val address = prefs.getString("address", "Beeramguda, Hyderabad") ?: "Beeramguda, Hyderabad"
        val classId = prefs.getString("class_id", buildCanonicalClassId(className, section))
            ?: buildCanonicalClassId(className, section)
        _isSiblingsFormSubmitted.value = prefs.getBoolean("siblings_form_submitted", false)

        applyStudentSessionLocally(
            studentId = studentId,
            studentCode = studentCode,
            fullName = fullName,
            className = className,
            section = section,
            classId = classId,
            parentPhone = parentPhone,
            parentEmail = parentEmail,
            address = address,
            scope = scope
        )
        isStudentLoggedIn.value = true
    }

    private fun applyStudentSessionLocally(
        studentId: String,
        studentCode: String,
        fullName: String,
        className: String,
        section: String,
        classId: String,
        parentPhone: String = activeStudent.value.parentPhone,
        parentEmail: String = activeStudent.value.parentEmail,
        address: String = activeStudent.value.address,
        scope: CoroutineScope
    ): StudentProfile {
        val cleanNum = className.replace(Regex("(?i)^class\\s*"), "").trim().uppercase()
        val rawSec = section.replace(Regex("(?i)^section\\s*"), "").trim()
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val normalizedClassName = "Class $cleanNum"
        val classTitle = "Class $cleanNum – Section $cleanSec"
        val bannerText = "Class: $cleanNum-$cleanSec (${_currentAcademicYear.value})"

        if (!_classOptions.value.any { it.equals(normalizedClassName, ignoreCase = true) }) {
            _classOptions.value = _classOptions.value + normalizedClassName
        }
        if (!_sectionOptions.value.any { it.equals(cleanSec, ignoreCase = true) }) {
            _sectionOptions.value = _sectionOptions.value + cleanSec
        }
        if (!_availableClasses.value.any { it.id == classId }) {
            _availableClasses.value = _availableClasses.value + SchoolClass(classId, classTitle, 38)
        }

        val updatedProfile = activeStudent.value.copy(
            id = studentId,
            studentCode = studentCode,
            firstName = fullName.split(" ").firstOrNull() ?: fullName,
            fullName = fullName,
            className = normalizedClassName,
            section = cleanSec,
            bannerClassText = bannerText,
            classId = classId,
            classTitle = classTitle,
            parentPhone = parentPhone,
            parentEmail = parentEmail,
            address = address,
            isProfileLocked = true
        )

        activeStudent.value = updatedProfile
        _linkedStudents.value = listOf(updatedProfile) + _linkedStudents.value.filter { it.id != updatedProfile.id }

        val targetClass = availableClasses.find { it.id == classId }
            ?: SchoolClass(classId, classTitle, 38)
        selectedClass.value = targetClass

        ensureIsolatedSectionData(classId, classTitle)

        // Ensure the logged-in student appears in that Class-Section's Student Verification Roster
        val existingInRoster = _sectionStudentRoster.value.find { it.id == studentId || it.fullName.equals(fullName, ignoreCase = true) }
        val rosterEntry = SectionRosterStudent(
            id = existingInRoster?.id ?: studentId,
            studentCode = studentCode,
            rollNo = existingInRoster?.rollNo ?: updatedProfile.rollNo,
            fullName = fullName,
            classId = classId,
            className = normalizedClassName,
            section = cleanSec,
            parentPhone = parentPhone,
            loginTimestamp = "Logged in • Active Device",
            verificationStatus = existingInRoster?.verificationStatus ?: updatedProfile.verificationStatus,
            blockedReason = existingInRoster?.blockedReason ?: updatedProfile.blockedReason,
            verifiedBy = existingInRoster?.verifiedBy ?: updatedProfile.verifiedBy,
            isCurrentDeviceStudent = true
        )
        _sectionStudentRoster.value = listOf(rosterEntry) + _sectionStudentRoster.value.filter {
            it.id != rosterEntry.id && !it.fullName.equals(fullName, ignoreCase = true)
        }

        // Feature APIs are triggered on demand when the user clicks each feature tile
        return updatedProfile
    }

    /**
     * Completes ONE-TIME initial student onboarding.
     * Stores the student's initial Name, Class, Section, Parent Contact, Optional Email, and Address
     * in SharedPreferences and Backend DB, and permanently locks Class/Section/Name (`is_profile_locked = true`).
     */
    fun completeInitialStudentOnboarding(
        context: Context,
        fullName: String,
        className: String,
        section: String,
        parentPhone: String = "",
        parentEmail: String = "",
        address: String = "",
        scope: CoroutineScope
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyLocked = prefs.getBoolean("is_logged_in", false) && prefs.getBoolean("is_profile_locked", false)
        if (alreadyLocked) {
            initSession(context, scope)
            return
        }

        val existingId = prefs.getString("student_id", null) ?: activeStudent.value.id
        val existingCode = prefs.getString("student_code", null) ?: activeStudent.value.studentCode
        val cleanName = fullName.trim()
        val cleanClass = className.trim()
        val rawSec = section.replace(Regex("(?i)^section\\s*"), "").trim()
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val cleanPhone = parentPhone.trim().ifBlank { activeStudent.value.parentPhone }
        val cleanEmail = parentEmail.trim()
        val cleanAddress = address.trim().ifBlank { activeStudent.value.address }
        val classId = buildCanonicalClassId(cleanClass, cleanSec)

        // Save initial student data and lock profile in SharedPreferences
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("is_profile_locked", true)
            .putString("student_id", existingId)
            .putString("student_code", existingCode)
            .putString("full_name", cleanName)
            .putString("class_name", cleanClass)
            .putString("section", cleanSec)
            .putString("parent_phone", cleanPhone)
            .putString("parent_email", cleanEmail)
            .putString("address", cleanAddress)
            .putString("class_id", classId)
            .apply()

        applyStudentSessionLocally(
            studentId = existingId,
            studentCode = existingCode,
            fullName = cleanName,
            className = cleanClass,
            section = cleanSec,
            classId = classId,
            parentPhone = cleanPhone,
            parentEmail = cleanEmail,
            address = cleanAddress,
            scope = scope
        )
        isStudentLoggedIn.value = true

        // Sync with backend API to persist and lock initial student record in DB
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/auth/student-login")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }
                val payload = JSONObject().apply {
                    put("student_id", existingId)
                    put("student_code", existingCode)
                    put("full_name", cleanName)
                    put("class_name", cleanClass)
                    put("section", cleanSec)
                    put("parent_phone", cleanPhone)
                    put("parent_email", cleanEmail)
                    put("address", cleanAddress)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                if (conn.responseCode in 200..201) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val jsonStr = reader.readText()
                    reader.close()
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val stuObj = json.getJSONObject("data").getJSONObject("student")
                        val serverId = stuObj.optString("id", existingId)
                        val serverCode = stuObj.optString("student_code", existingCode)
                        val serverClassId = stuObj.optString("class_id", classId)

                        prefs.edit()
                            .putString("student_id", serverId)
                            .putString("student_code", serverCode)
                            .putString("class_id", serverClassId)
                            .putBoolean("is_profile_locked", true)
                            .apply()

                        withContext(Dispatchers.Main) {
                            applyStudentSessionLocally(
                                studentId = serverId,
                                studentCode = serverCode,
                                fullName = cleanName,
                                className = cleanClass,
                                section = cleanSec,
                                classId = serverClassId,
                                parentPhone = cleanPhone,
                                parentEmail = cleanEmail,
                                address = cleanAddress,
                                scope = scope
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Offline resilient: local session and SharedPreferences are already updated and locked
            }
        }
    }

    /**
     * Teacher / Admin ONLY action: Updates a student's locked Name, Class, or Section
     * when a student made a mistake during initial onboarding or was promoted/transferred.
     */
    fun teacherUpdateStudentDetails(
        context: Context,
        fullName: String,
        className: String,
        section: String,
        scope: CoroutineScope
    ): Boolean {
        val role = currentRole.value
        if (role != AppRole.TEACHER && role != AppRole.ADMIN) {
            return false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingId = prefs.getString("student_id", null) ?: activeStudent.value.id
        val existingCode = prefs.getString("student_code", null) ?: activeStudent.value.studentCode
        val cleanName = fullName.trim()
        val cleanClass = className.trim()
        val rawSec = section.replace(Regex("(?i)^section\\s*"), "").trim()
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val classId = buildCanonicalClassId(cleanClass, cleanSec)

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("is_profile_locked", true)
            .putString("student_id", existingId)
            .putString("student_code", existingCode)
            .putString("full_name", cleanName)
            .putString("class_name", cleanClass)
            .putString("section", cleanSec)
            .putString("class_id", classId)
            .apply()

        applyStudentSessionLocally(
            studentId = existingId,
            studentCode = existingCode,
            fullName = cleanName,
            className = cleanClass,
            section = cleanSec,
            classId = classId,
            scope = scope
        )

        // Call Teacher-authorized PATCH endpoint on Backend API
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/students/$existingId/class-section")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-user-role", role.name)
                }
                val payload = JSONObject().apply {
                    put("student_id", existingId)
                    put("student_code", existingCode)
                    put("full_name", cleanName)
                    put("class_name", cleanClass)
                    put("section", cleanSec)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode // execute request
            } catch (_: Exception) {
                // Offline resilient
            }
        }
        return true
    }

    fun loginOrUpdateStudentClassSection(
        context: Context,
        fullName: String,
        className: String,
        section: String,
        scope: CoroutineScope
    ) {
        if (!isStudentLoggedIn.value) {
            completeInitialStudentOnboarding(
                context = context,
                fullName = fullName,
                className = className,
                section = section,
                scope = scope
            )
        } else {
            teacherUpdateStudentDetails(context, fullName, className, section, scope)
        }
    }

    fun selectStudent(student: StudentProfile, scope: CoroutineScope) {
        activeStudent.value = student
        val matchingClass = availableClasses.find { it.id == student.classId }
            ?: SchoolClass(student.classId, student.classTitle, 38)
        selectedClass.value = matchingClass
        ensureIsolatedSectionData(student.classId, student.classTitle)
        fetchDiscussionsFromBackend(student.classId, scope)
        fetchNotesFromBackend(student.classId, scope)
    }

    fun addLinkedStudent(
        admissionNo: String,
        fullName: String,
        className: String,
        section: String
    ): StudentProfile {
        val classId = buildCanonicalClassId(className, section)
        val cleanClass = className.replace(Regex("(?i)^class\\s*"), "").trim().uppercase()
        val rawSec = section.replace(Regex("(?i)^section\\s*"), "").trim()
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val classTitle = "Class $cleanClass – Section $cleanSec"
        val newStudent = StudentProfile(
            id = "stu-${System.currentTimeMillis()}",
            studentCode = admissionNo,
            firstName = fullName,
            fullName = fullName,
            className = "Class $cleanClass",
            section = cleanSec,
            bannerClassText = "Class: $cleanClass-$cleanSec (2026-2027)",
            classId = classId,
            classTitle = classTitle,
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
            tuitionFeeInr = 9500,
            examFeeInr = 2500,
            activityFeeInr = 2000
        )
        _linkedStudents.value = _linkedStudents.value + newStudent
        activeStudent.value = newStudent
        selectedClass.value = SchoolClass(classId, classTitle, 36)
        ensureIsolatedSectionData(classId, classTitle)
        return newStudent
    }

    fun switchClass(targetClass: SchoolClass, scope: CoroutineScope) {
        selectedClass.value = targetClass
        ensureIsolatedSectionData(targetClass.id, targetClass.name)
        fetchDiscussionsFromBackend(targetClass.id, scope)
        fetchNotesFromBackend(targetClass.id, scope)
        fetchClassSubjectsFromBackend(targetClass.id, scope)
        fetchClassExamsFromBackend(targetClass.id, scope)
        fetchStudentExamResultsFromBackend(activeStudent.value.id, scope)
        fetchHomeworkFromBackend(targetClass.id, scope)
        fetchNoticesFromBackend(scope)
        fetchClassRosterFromBackend(targetClass.id, scope)
    }

    fun fetchHomeworkFromBackend(classId: String, scope: CoroutineScope = ioScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$classId/homework")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=$classId]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", classId)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val hwArr = json.getJSONObject("data").optJSONArray("homework")
                        val remoteList = mutableListOf<HomeworkItem>()
                        if (hwArr != null) {
                            for (i in 0 until hwArr.length()) {
                                val hObj = hwArr.getJSONObject(i)
                                remoteList.add(
                                    HomeworkItem(
                                        id = hObj.optString("id"),
                                        classId = hObj.optString("class_id", classId),
                                        subject = hObj.optString("subject_name", "Academic"),
                                        title = hObj.optString("title"),
                                        dueText = hObj.optString("due_date", "Due Soon"),
                                        status = "Due Soon",
                                        instructions = hObj.optString("description")
                                    )
                                )
                            }
                        }
                        _homeworkList.value = remoteList + _homeworkList.value.filter { it.classId != classId }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun fetchNoticesFromBackend(scope: CoroutineScope = ioScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/posts")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=${selectedClass.value.id}]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val postsArr = json.getJSONObject("data").optJSONArray("posts")
                        val remoteNotices = mutableListOf<AdminNotice>()
                        if (postsArr != null) {
                            for (i in 0 until postsArr.length()) {
                                val pObj = postsArr.getJSONObject(i)
                                val rawImg = pObj.optString("image_url", "").takeIf { it.isNotBlank() && it != "null" }
                                val rawStart = pObj.optString("starts_at", "").takeIf { it.isNotBlank() && it != "null" }
                                val rawEnd = pObj.optString("expires_at", "").takeIf { it.isNotBlank() && it != "null" }
                                val formattedDate = runCatching {
                                    val createdIso = pObj.optString("created_at", "")
                                    if (createdIso.isNotBlank()) {
                                        java.time.Instant.parse(createdIso)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH))
                                    } else "Today"
                                }.getOrDefault("Today")
                                remoteNotices.add(
                                    AdminNotice(
                                        id = pObj.optString("id"),
                                        title = pObj.optString("title"),
                                        content = pObj.optString("content"),
                                        category = pObj.optString("category", "CIRCULAR"),
                                        author = pObj.optString("author_name", "Principal's Office"),
                                        date = formattedDate,
                                        imageUrl = resolveRenderableMediaUrl(rawImg),
                                        targetAudience = pObj.optString("target_audience", "Entire School").ifBlank { "Entire School" },
                                        startsAtIso = rawStart,
                                        expiresAtIso = rawEnd,
                                        scheduleStatus = pObj.optString("status", "ACTIVE").ifBlank { "ACTIVE" }
                                    )
                                )
                            }
                        }
                        _homeNotices.value = remoteNotices
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun fetchClassRosterFromBackend(classId: String, scope: CoroutineScope = ioScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$classId/students")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=$classId]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", classId)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val stuArr = json.getJSONObject("data").optJSONArray("students")
                        val remoteRoster = mutableListOf<SectionRosterStudent>()
                        val remoteAttRoster = mutableListOf<RosterAttendanceStudent>()
                        if (stuArr != null) {
                            for (i in 0 until stuArr.length()) {
                                val sObj = stuArr.getJSONObject(i)
                                val sId = sObj.optString("id")
                                val sRoll = sObj.optString("roll_no", "").ifBlank { "%02d".format(i + 1) }
                                val sName = sObj.optString("full_name")
                                val sPhone = sObj.optString("parent_phone", "")
                                val sClassId = sObj.optString("class_id", classId)
                                remoteRoster.add(
                                    SectionRosterStudent(
                                        id = sId,
                                        studentCode = sObj.optString("student_code", "2258"),
                                        rollNo = sRoll,
                                        fullName = sName,
                                        classId = sClassId,
                                        className = sObj.optString("class_name", "Class 10"),
                                        section = sObj.optString("section", "A"),
                                        parentPhone = sPhone,
                                        loginTimestamp = "Active Student",
                                        verificationStatus = sObj.optString("verification_status", "VERIFIED")
                                    )
                                )
                                remoteAttRoster.add(
                                    RosterAttendanceStudent(
                                        id = sId,
                                        rollNo = sRoll,
                                        studentName = sName,
                                        parentPhone = sPhone,
                                        status = "Present",
                                        classId = sClassId
                                    )
                                )
                            }
                        }
                        _sectionStudentRoster.value = remoteRoster + _sectionStudentRoster.value.filter { it.classId != classId }
                        _rosterAttendance.value = remoteAttRoster + _rosterAttendance.value.filter { it.classId != classId }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun parseSchoolClassesJson(body: String): List<SchoolClass> {
        val fetched = mutableListOf<SchoolClass>()
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return fetched
        val arr = json.optJSONObject("data")?.optJSONArray("classes") ?: return fetched
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val clsName = obj.optString("class_name", "")
            val sec = obj.optString("section", "A")
            val id = obj.optString("id", buildCanonicalClassId(clsName, sec))
            val dbUuid = obj.optString("db_uuid", "")
            val displayName = obj.optString("name", "$clsName – Section $sec")
            val studentCount = obj.optInt("studentCount", 0)
            fetched.add(
                SchoolClass(
                    id = id,
                    name = displayName,
                    studentCount = studentCount,
                    dbUuid = dbUuid,
                    className = clsName,
                    section = sec
                )
            )
        }
        return fetched
    }

    fun fetchClassSubjectsFromBackend(classId: String, scope: CoroutineScope = ioScope, forceRefresh: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            try {
                val schoolId = _activeSchool.value.id
                if (!forceRefresh) {
                    val cached = readSchoolCache(schoolId, "subjects_$classId")
                    if (cached != null) {
                        val json = runCatching { JSONObject(cached) }.getOrNull()
                        val namesArr = json?.optJSONObject("data")?.optJSONArray("subject_names")
                        if (namesArr != null) {
                            val list = (0 until namesArr.length()).mapNotNull { namesArr.optString(it).trim().ifEmpty { null } }
                            _classSubjectsMap.value = _classSubjectsMap.value + (classId to list)
                            return@launch
                        }
                    }
                }
                val url = URL("$BASE_URL/classes/$classId/subjects")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=$classId]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", classId)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        writeSchoolCache(schoolId, "subjects_$classId", jsonStr)
                        val namesArr = json.getJSONObject("data").optJSONArray("subject_names")
                        val list = mutableListOf<String>()
                        if (namesArr != null) {
                            for (i in 0 until namesArr.length()) {
                                val s = namesArr.optString(i).trim()
                                if (s.isNotEmpty()) list.add(s)
                            }
                        }
                        _classSubjectsMap.value = _classSubjectsMap.value + (classId to list)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun fetchClassExamsFromBackend(classId: String, scope: CoroutineScope = ioScope, forceRefresh: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            try {
                val schoolId = _activeSchool.value.id
                if (!forceRefresh) {
                    val cached = readSchoolCache(schoolId, "exams_$classId")
                    if (cached != null) {
                        val json = runCatching { JSONObject(cached) }.getOrNull()
                        val examsArr = json?.optJSONObject("data")?.optJSONArray("exams")
                        if (examsArr != null) {
                            val remoteExams = mutableListOf<String>()
                            for (i in 0 until examsArr.length()) {
                                val title = examsArr.getJSONObject(i).optString("title").trim()
                                if (title.isNotEmpty() && remoteExams.none { it.equals(title, ignoreCase = true) }) {
                                    remoteExams.add(title)
                                }
                            }
                            _availableExams.value = remoteExams
                            return@launch
                        }
                    }
                }
                val url = URL("$BASE_URL/classes/$classId/exams")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=$classId]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", classId)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        writeSchoolCache(schoolId, "exams_$classId", jsonStr)
                        val examsArr = json.getJSONObject("data").optJSONArray("exams")
                        val remoteExams = mutableListOf<String>()
                        if (examsArr != null) {
                            for (i in 0 until examsArr.length()) {
                                val eObj = examsArr.getJSONObject(i)
                                val title = eObj.optString("title").trim()
                                if (title.isNotEmpty() && remoteExams.none { it.equals(title, ignoreCase = true) }) {
                                    remoteExams.add(title)
                                }
                            }
                        }
                        _availableExams.value = remoteExams
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun fetchStudentExamResultsFromBackend(studentId: String, scope: CoroutineScope) {
        fetchStudentExamResultsFromBackend(studentId, "", scope)
    }

    fun fetchStudentExamResultsFromBackend(
        studentId: String,
        examQueryName: String = "",
        scope: CoroutineScope = ioScope
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val encodedExam = URLEncoder.encode(examQueryName, "UTF-8")
                val urlStr = if (examQueryName.isNotBlank()) {
                    "$BASE_URL/students/$studentId/exam-results?exam_name=$encodedExam&class_id=${selectedClass.value.id}"
                } else {
                    "$BASE_URL/students/$studentId/exam-results?class_id=${selectedClass.value.id}"
                }
                val url = URL(urlStr)
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val data = json.getJSONObject("data")
                        val examName = data.optString("exam_name", examQueryName.ifBlank { "Term 1" })

                        // Parse full results_by_exam map from PostgreSQL
                        val byExamObj = data.optJSONObject("results_by_exam")
                        val updatedByExam = mutableMapOf<String, List<SubjectExamResult>>()
                        if (byExamObj != null) {
                            val keys = byExamObj.keys()
                            while (keys.hasNext()) {
                                val eKey = keys.next()
                                val arr = byExamObj.optJSONArray(eKey)
                                val subList = mutableListOf<SubjectExamResult>()
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        val sObj = arr.getJSONObject(i)
                                        subList.add(
                                            SubjectExamResult(
                                                subject = sObj.optString("subject_name"),
                                                examName = eKey,
                                                marksObtained = sObj.optDouble("marks_obtained", 0.0).toInt(),
                                                maxMarks = sObj.optDouble("max_marks", 100.0).toInt().coerceAtLeast(1),
                                                grade = sObj.optString("grade", ""),
                                                teacherRemark = sObj.optString("remarks", "")
                                            )
                                        )
                                    }
                                }
                                updatedByExam[eKey] = subList
                            }
                        }

                        // Also parse the requested exam's subjects array (even if empty [])
                        val subjectsArr = data.optJSONArray("subjects")
                        val results = mutableListOf<SubjectExamResult>()
                        if (subjectsArr != null) {
                            for (i in 0 until subjectsArr.length()) {
                                val sObj = subjectsArr.getJSONObject(i)
                                results.add(
                                    SubjectExamResult(
                                        subject = sObj.optString("subject_name"),
                                        examName = examName,
                                        marksObtained = sObj.optDouble("marks_obtained", 0.0).toInt(),
                                        maxMarks = sObj.optDouble("max_marks", 100.0).toInt().coerceAtLeast(1),
                                        grade = sObj.optString("grade", ""),
                                        teacherRemark = sObj.optString("remarks", "")
                                    )
                                )
                            }
                        }
                        updatedByExam[examName] = results
                        if (examQueryName.isNotBlank()) {
                            updatedByExam[examQueryName] = results
                        }
                        _examResultsFlow.value = results
                        _examResultsByExam.value = updatedByExam
                    }
                }
            } catch (e: Exception) {
                Log.w("GenesisAPI", "API Error on exam-results: ${e.message}")
            }
        }
    }

    fun fetchStudentAttendanceFromBackend(studentId: String, scope: CoroutineScope = ioScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/students/$studentId/attendance")
                Log.i("GenesisAPI", "--> GET $url")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val data = json.getJSONObject("data")
                        val yearlyObj = data.optJSONObject("yearly_attendance")
                        val updatedMap = mutableMapOf<String, String>()
                        if (yearlyObj != null) {
                            val keys = yearlyObj.keys()
                            while (keys.hasNext()) {
                                val dateKey = keys.next()
                                val statusVal = yearlyObj.optString(dateKey, "Present")
                                updatedMap[dateKey] = statusVal
                            }
                        }
                        _studentYearlyAttendance.value = updatedMap
                    }
                }
            } catch (e: Exception) {
                Log.w("GenesisAPI", "API Error on attendance: ${e.message}")
            }
        }
    }

    fun fetchReferralCodeFromBackend(scope: CoroutineScope = ioScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/referrals/my-code")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=${selectedClass.value.id}]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
            } catch (e: Exception) {
                Log.w("GenesisAPI", "API Error on referrals/my-code: ${e.message}")
            }
        }
    }

    fun fetchSiblingsFromBackend(scope: CoroutineScope = ioScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/admin/siblings")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=${selectedClass.value.id}]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
            } catch (e: Exception) {
                Log.w("GenesisAPI", "API Error on admin/siblings: ${e.message}")
            }
        }
    }

    fun fetchSchoolIntelligenceFromBackend(scope: CoroutineScope = ioScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/school-os/intelligence")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=${selectedClass.value.id}]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")
            } catch (e: Exception) {
                Log.w("GenesisAPI", "API Error on school-os/intelligence: ${e.message}")
            }
        }
    }

    fun toggleNoteDownloaded(noteId: String) {
        _studyNotes.value = _studyNotes.value.map {
            if (it.id == noteId) it.copy(isDownloaded = true) else it
        }
    }

    fun submitLeaveRequest(
        leaveType: String,
        startDate: String,
        endDate: String,
        reason: String
    ) {
        val student = activeStudent.value
        val tempId = "lv-${System.currentTimeMillis()}"
        val item = LeaveRequestItem(
            id = tempId,
            studentName = "${student.fullName} (${student.studentCode})",
            className = student.classTitle,
            leaveType = leaveType,
            startDate = startDate,
            endDate = endDate,
            reason = reason,
            status = "Pending Review"
        )
        _leaveRequests.value = listOf(item) + _leaveRequests.value

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/leaves")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-student-name", student.fullName)
                    setRequestProperty("x-student-id", student.id)
                    setRequestProperty("x-class-id", student.classId)
                }
                val payload = JSONObject().apply {
                    put("student_id", student.id)
                    put("student_name", "${student.fullName} (${student.studentCode})")
                    put("class_id", student.classId)
                    put("class_name", student.classTitle)
                    put("leave_type", leaveType)
                    put("start_date", startDate)
                    put("end_date", endDate)
                    put("reason", reason)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    val realId = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        .optJSONObject("data")?.optString("id").orEmpty()
                    if (realId.isNotBlank()) {
                        _leaveRequests.value = _leaveRequests.value.map {
                            if (it.id == tempId) it.copy(id = realId) else it
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateLeaveStatus(leaveId: String, newStatus: String) {
        _leaveRequests.value = _leaveRequests.value.map {
            if (it.id == leaveId) it.copy(status = newStatus) else it
        }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/leaves/$leaveId/status")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                }
                val payload = JSONObject().apply {
                    put("status", newStatus)
                    put("reviewed_by", if (currentRole.value == AppRole.ADMIN) "School Principal" else "Class Teacher")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun renewLibraryBook(bookId: String) {
        _libraryBooks.value = _libraryBooks.value.map {
            if (it.id == bookId) it.copy(isRenewed = true, dueDate = "29 Oct 2026 (Renewed)") else it
        }
    }

    fun markFeePaidForActiveStudent() {
        val current = activeStudent.value
        val updated = current.copy(feeStatus = "Paid", feeDueDate = "Paid Just Now")
        activeStudent.value = updated
        _linkedStudents.value = _linkedStudents.value.map {
            if (it.id == current.id) updated else it
        }
    }

    fun updateActiveStudentProfile(phone: String, email: String, address: String, context: Context? = null) {
        val current = activeStudent.value
        val updated = current.copy(parentPhone = phone, parentEmail = email, address = address)
        activeStudent.value = updated
        _linkedStudents.value = _linkedStudents.value.map {
            if (it.id == current.id) updated else it
        }
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putString("parent_phone", phone)
            ?.putString("parent_email", email)
            ?.putString("address", address)
            ?.apply()

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/auth/student-profile")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PUT"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-student-name", updated.fullName)
                }
                val payload = JSONObject().apply {
                    put("student_id", updated.id)
                    put("student_code", updated.studentCode)
                    put("roll_no", updated.rollNo)
                    put("full_name", updated.fullName)
                    put("class_name", updated.className)
                    put("section", updated.section)
                    put("parent_phone", phone)
                    put("parent_email", email)
                    put("address", address)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun toggleHomeworkCompleted(hwId: String) {
        _homeworkList.value = _homeworkList.value.map { hw ->
            if (hw.id == hwId) {
                val nextDone = !hw.isCompleted
                hw.copy(
                    isCompleted = nextDone,
                    status = if (nextDone) "Completed" else "Pending"
                )
            } else hw
        }
    }

    fun markStudentAttendance(studentId: String, status: String) {
        val target = _rosterAttendance.value.find { it.id == studentId }
        _rosterAttendance.value = _rosterAttendance.value.map { s ->
            if (s.id == studentId) s.copy(status = status) else s
        }
        if (target != null) {
            val todayIso = java.time.LocalDate.now().toString()
            // Persist student roll call status directly to PostgreSQL attendance_records
            ioScope.launch {
                try {
                    val url = URL("$BASE_URL/attendance")
                    val conn = openSchoolHttpConnection(url).apply {
                        requestMethod = "POST"
                        connectTimeout = 5000
                        readTimeout = 5000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("x-user-role", currentRole.value.name)
                        setRequestProperty("x-class-id", target.classId)
                    }
                    val rec = JSONObject().apply {
                        put("student_id", target.id)
                        put("student_name", target.studentName)
                        put("roll_no", target.rollNo)
                        put("status", status.uppercase())
                        put("remarks", "Marked by teacher in Class Roll Call")
                    }
                    val payload = JSONObject().apply {
                        put("class_id", target.classId)
                        put("date", todayIso)
                        put("records", org.json.JSONArray().put(rec))
                    }
                    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                    conn.responseCode
                } catch (_: Exception) {}
            }

            // Sync today's calendar entry when active student is updated
            if (studentId == activeStudent.value.id || target.studentName.equals(activeStudent.value.fullName, ignoreCase = true)) {
                val calendarStatus = if (status == "Present") "Present" else "Absent"
                _studentYearlyAttendance.value = _studentYearlyAttendance.value + (todayIso to calendarStatus)
            }
            val auditMsg = when (status) {
                "Absent" -> "Auto-Push sent to Parent (${target.parentPhone}) • Absence explanation requested"
                "Leave" -> "Marked Approved Leave • Synced with Leave Ledger"
                else -> "Marked Present in Digital Register"
            }
            _attendanceAuditLog.value = listOf(
                AttendanceAuditEntry(
                    id = "aud-${System.currentTimeMillis()}",
                    timestamp = "Just now",
                    summary = "Roll #${target.rollNo} ${target.studentName} marked $status",
                    triggeredActions = auditMsg
                )
            ) + _attendanceAuditLog.value

            if (status == "Absent") {
                incrementAutomationRuleCounter("auto-absent")
                if (_parentAbsenceExplanations.value.none { it.studentId == studentId && it.dateText == "Today" }) {
                    _parentAbsenceExplanations.value = listOf(
                        ParentAbsenceExplanation(
                            id = "abs-${System.currentTimeMillis()}",
                            studentId = studentId,
                            studentName = "${target.studentName} (Roll #${target.rollNo})",
                            classSection = selectedClass.value.name,
                            dateText = "Today",
                            parentExplanation = "Awaiting parent explanation response...",
                            status = "Pending Parent Note"
                        )
                    ) + _parentAbsenceExplanations.value
                }
            }
        }
    }

    fun markAllPresentInClass(classId: String? = null) {
        val targetClassId = classId ?: selectedClass.value.id
        val classStudents = _rosterAttendance.value.filter { it.classId == targetClassId }
        _rosterAttendance.value = _rosterAttendance.value.map {
            if (it.classId == targetClassId) it.copy(status = "Present") else it
        }
        val targetClassName = availableClasses.find { it.id == targetClassId }?.name ?: selectedClass.value.name
        _attendanceAuditLog.value = listOf(
            AttendanceAuditEntry(
                id = "aud-${System.currentTimeMillis()}",
                timestamp = "Just now",
                summary = "Bulk Roll Call: All students in $targetClassName marked Present",
                triggeredActions = "Completed in 1 tap • Class attendance synced to SchoolOS Dashboard"
            )
        ) + _attendanceAuditLog.value

        if (classStudents.isNotEmpty()) {
            val todayIso = java.time.LocalDate.now().toString()
            ioScope.launch {
                try {
                    val url = URL("$BASE_URL/attendance")
                    val conn = openSchoolHttpConnection(url).apply {
                        requestMethod = "POST"
                        connectTimeout = 5000
                        readTimeout = 5000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("x-user-role", currentRole.value.name)
                        setRequestProperty("x-class-id", targetClassId)
                    }
                    val arr = org.json.JSONArray()
                    classStudents.forEach { stu ->
                        arr.put(JSONObject().apply {
                            put("student_id", stu.id)
                            put("student_name", stu.studentName)
                            put("roll_no", stu.rollNo)
                            put("status", "PRESENT")
                            put("remarks", "1-tap Mark All Present")
                        })
                    }
                    val payload = JSONObject().apply {
                        put("class_id", targetClassId)
                        put("date", todayIso)
                        put("records", arr)
                    }
                    OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                    conn.responseCode
                } catch (_: Exception) {}
            }
        }
    }

    fun submitParentAbsenceExplanation(studentName: String, reason: String) {
        val cleanReason = reason.trim().ifBlank { "Medical / Family reason informed by parent" }
        val tempId = "abs-${System.currentTimeMillis()}"
        _parentAbsenceExplanations.value = listOf(
            ParentAbsenceExplanation(
                id = tempId,
                studentId = activeStudent.value.id,
                studentName = studentName,
                classSection = activeStudent.value.classTitle,
                dateText = "Today",
                parentExplanation = cleanReason,
                status = "Submitted by Parent"
            )
        ) + _parentAbsenceExplanations.value.filter { it.studentName != studentName }

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/absence-explanations")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-student-name", studentName)
                    setRequestProperty("x-student-id", activeStudent.value.id)
                    setRequestProperty("x-class-id", activeStudent.value.classId)
                }
                val payload = JSONObject().apply {
                    put("student_id", activeStudent.value.id)
                    put("student_name", studentName)
                    put("class_id", activeStudent.value.classId)
                    put("class_section", activeStudent.value.classTitle)
                    put("date_text", "Today")
                    put("parent_explanation", cleanReason)
                    put("status", "Submitted by Parent")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    val realId = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        .optJSONObject("data")?.optString("id").orEmpty()
                    if (realId.isNotBlank()) {
                        _parentAbsenceExplanations.value = _parentAbsenceExplanations.value.map {
                            if (it.id == tempId) it.copy(id = realId) else it
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun approveAbsenceExplanation(explanationId: String) {
        _parentAbsenceExplanations.value = _parentAbsenceExplanations.value.map {
            if (it.id == explanationId) it.copy(status = "Approved by Teacher") else it
        }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/absence-explanations/$explanationId/approve")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    // ========================================================================
    // SECTION-WISE STUDENT ROSTER VERIFICATION, BLOCK & UNBLOCK (TEACHER/ADMIN)
    // ========================================================================

    fun verifySectionStudent(studentId: String, scope: CoroutineScope? = null) {
        _sectionStudentRoster.value = _sectionStudentRoster.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    verificationStatus = "VERIFIED",
                    blockedReason = "",
                    verifiedBy = "Verified by Class Teacher"
                )
            } else s
        }
        if (activeStudent.value.id == studentId || _sectionStudentRoster.value.find { it.id == studentId }?.isCurrentDeviceStudent == true) {
            activeStudent.value = activeStudent.value.copy(
                verificationStatus = "VERIFIED",
                blockedReason = "",
                verifiedBy = "Class Teacher"
            )
        }
        syncVerificationWithBackend(studentId, "VERIFIED", "", scope)
    }

    fun verifyAllSectionStudents(classId: String, scope: CoroutineScope? = null) {
        _sectionStudentRoster.value = _sectionStudentRoster.value.map { s ->
            if (s.classId == classId && s.verificationStatus == "PENDING") {
                s.copy(
                    verificationStatus = "VERIFIED",
                    blockedReason = "",
                    verifiedBy = "Verified in Classroom Roll Call"
                )
            } else s
        }
        if (activeStudent.value.classId == classId && activeStudent.value.verificationStatus == "PENDING") {
            activeStudent.value = activeStudent.value.copy(
                verificationStatus = "VERIFIED",
                blockedReason = "",
                verifiedBy = "Class Teacher"
            )
        }
        scope?.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$classId/verify-all")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                OutputStreamWriter(conn.outputStream).use { it.write("{}") }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun blockSectionStudent(studentId: String, reason: String, scope: CoroutineScope? = null) {
        val cleanReason = reason.trim().ifBlank { "Suspected unauthorized or wrong class/section login" }
        _sectionStudentRoster.value = _sectionStudentRoster.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    verificationStatus = "BLOCKED",
                    blockedReason = cleanReason,
                    verifiedBy = "Blocked by Class Teacher"
                )
            } else s
        }
        val target = _sectionStudentRoster.value.find { it.id == studentId }
        if (activeStudent.value.id == studentId || target?.isCurrentDeviceStudent == true) {
            activeStudent.value = activeStudent.value.copy(
                verificationStatus = "BLOCKED",
                blockedReason = cleanReason
            )
        }
        syncVerificationWithBackend(studentId, "BLOCKED", cleanReason, scope)
    }

    fun unblockSectionStudent(studentId: String, scope: CoroutineScope? = null) {
        _sectionStudentRoster.value = _sectionStudentRoster.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    verificationStatus = "VERIFIED",
                    blockedReason = "",
                    verifiedBy = "Unblocked & Verified by Teacher"
                )
            } else s
        }
        val target = _sectionStudentRoster.value.find { it.id == studentId }
        if (activeStudent.value.id == studentId || target?.isCurrentDeviceStudent == true) {
            activeStudent.value = activeStudent.value.copy(
                verificationStatus = "VERIFIED",
                blockedReason = "",
                verifiedBy = "Class Teacher"
            )
        }
        syncVerificationWithBackend(studentId, "VERIFIED", "", scope)
    }

    private fun syncVerificationWithBackend(
        studentId: String,
        status: String,
        reason: String,
        scope: CoroutineScope?
    ) {
        scope?.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/students/$studentId/verification")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val payload = JSONObject().apply {
                    put("verification_status", status)
                    put("blocked_reason", reason)
                    put("verified_by", "Class Teacher")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    // ========================================================================
    // MULTI-SECTION HOMEWORK DISTRIBUTION & TEMPLATES (MODULE 3)
    // ========================================================================

    fun assignHomeworkToMultipleSections(
        targetClassIds: List<String>,
        subject: String,
        title: String,
        dueText: String,
        instructions: String,
        maxMarks: Int = 10
    ): Int {
        val cleanTargets = if (targetClassIds.isEmpty()) listOf(selectedClass.value.id) else targetClassIds
        val cleanDue = dueText.trim().ifBlank { "Due Tomorrow, 09:00 AM" }
        val createdItems = cleanTargets.map { cid ->
            HomeworkItem(
                id = "hw-$cid-${System.currentTimeMillis()}",
                classId = cid,
                subject = subject,
                title = title.trim(),
                dueText = cleanDue,
                status = "Due Soon",
                instructions = instructions.trim(),
                maxMarks = maxMarks
            )
        }
        _homeworkList.value = createdItems + _homeworkList.value
        incrementAutomationRuleCounter("auto-hw-distribute")

        ioScope.launch {
            try {
                val firstClass = cleanTargets.firstOrNull() ?: "class-10a"
                val url = URL("$BASE_URL/classes/$firstClass/homework")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val classIdsArr = JSONArray()
                cleanTargets.forEach { classIdsArr.put(it) }
                val payload = JSONObject().apply {
                    put("class_ids", classIdsArr)
                    put("subject_name", subject)
                    put("title", title.trim())
                    put("description", instructions.trim())
                    put("due_date", cleanDue)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return cleanTargets.size
    }

    fun modifyHomeworkDeadline(id: String, newDeadlineIso: String, newDueText: String) {
        _homeworkList.value = _homeworkList.value.map { item ->
            if (item.id == id) {
                item.copy(deadlineDateIso = newDeadlineIso, dueText = newDueText)
            } else item
        }
    }

    fun addOrUpdateHomework(item: HomeworkItem) {
        val existingIndex = _homeworkList.value.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            val updated = _homeworkList.value.toMutableList()
            updated[existingIndex] = item
            _homeworkList.value = updated
        } else {
            _homeworkList.value = listOf(item) + _homeworkList.value
        }

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/classes/${item.classId}/homework")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val payload = JSONObject().apply {
                    put("class_ids", JSONArray().put(item.classId))
                    put("subject_name", item.subject)
                    put("title", item.title)
                    put("description", item.instructions)
                    put("due_date", item.dueText)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun deleteHomework(id: String) {
        _homeworkList.value = _homeworkList.value.filterNot { it.id == id }
    }

    fun submitStudentHomework(id: String) {
        _homeworkList.value = _homeworkList.value.map { item ->
            if (item.id == id) {
                item.copy(
                    isCompleted = true,
                    status = "Completed",
                    submittedCount = (item.submittedCount + 1).coerceAtMost(item.totalStudents)
                )
            } else item
        }
    }

    fun addOrUpdateExamSchedule(item: ExamScheduleItem) {
        val existingIndex = _examSchedules.value.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            val updated = _examSchedules.value.toMutableList()
            updated[existingIndex] = item
            _examSchedules.value = updated
        } else {
            _examSchedules.value = listOf(item) + _examSchedules.value
        }
    }

    fun deleteExamSchedule(id: String) {
        _examSchedules.value = _examSchedules.value.filterNot { it.id == id }
    }

    // Sibling Admissions Data Submission & Admin Filtering
    fun submitSiblingsForm(context: Context?, submission: SiblingSubmission, scope: CoroutineScope?) {
        _isSiblingsFormSubmitted.value = true
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putBoolean("siblings_form_submitted", true)
            ?.apply()

        _siblingSubmissions.value = listOf(submission) + _siblingSubmissions.value.filterNot { it.studentId == submission.studentId }

        scope?.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/siblings/submit")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val siblingsArray = org.json.JSONArray()
                submission.siblings.forEach { sib ->
                    val sibObj = org.json.JSONObject().apply {
                        put("id", sib.id)
                        put("full_name", sib.fullName)
                        put("age", sib.age)
                        put("current_grade", sib.currentGrade)
                        put("school_type", sib.schoolType)
                        put("school_name", sib.schoolName)
                        put("reason_other_school", sib.reasonOtherSchool)
                        put("target_admission_year", sib.targetAdmissionYear)
                        put("admission_status", sib.admissionStatus)
                    }
                    siblingsArray.put(sibObj)
                }
                val payload = org.json.JSONObject().apply {
                    put("student_id", submission.studentId)
                    put("student_name", submission.studentName)
                    put("class_name", submission.className)
                    put("parent_name", submission.parentName)
                    put("parent_phone", submission.parentPhone)
                    put("parent_email", submission.parentEmail)
                    put("has_siblings", submission.hasSiblings)
                    put("siblings", siblingsArray)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun updateSiblingAdmissionStatus(siblingId: String, newStatus: String) {
        _siblingSubmissions.value = _siblingSubmissions.value.map { sub ->
            val updatedSiblings = sub.siblings.map { sib ->
                if (sib.id == siblingId) sib.copy(admissionStatus = newStatus) else sib
            }
            sub.copy(siblings = updatedSiblings)
        }
    }

    // Mistake Analysis
    fun addMistakeAnalysis(item: MistakeAnalysisItem) {
        _mistakeAnalysisList.value = listOf(item) + _mistakeAnalysisList.value
    }

    // ========================================================================
    // CLASS-WISE SUBJECTS & SINGLE-ENTRY EXAM MARKS (MODULE 5)
    // ========================================================================

    fun addSubjectToClass(classId: String, subjectName: String, maxMarks: Int = 100): Boolean {
        val cleanSubject = subjectName.trim()
        if (cleanSubject.isBlank()) return false
        val currentList = getSubjectsForClass(classId)
        if (currentList.any { it.equals(cleanSubject, ignoreCase = true) }) return false

        val updatedList = currentList + cleanSubject
        _classSubjectsMap.value = _classSubjectsMap.value + (classId to updatedList)

        _examResultsByExam.value = _examResultsByExam.value.mapValues { (examName, list) ->
            if (list.any { it.subject.equals(cleanSubject, ignoreCase = true) }) {
                list
            } else {
                val defaultMax = if (examName.startsWith("Unit Test")) 25 else maxMarks.coerceAtLeast(1)
                val defaultObt = if (examName.startsWith("Unit Test")) 22 else 85
                list + SubjectExamResult(cleanSubject, examName, defaultObt, defaultMax, "", "")
            }
        }
        if (_examResultsFlow.value.none { it.subject.equals(cleanSubject, ignoreCase = true) }) {
            val newResult = SubjectExamResult(
                subject = cleanSubject,
                examName = "Term 1",
                marksObtained = 85,
                maxMarks = maxMarks.coerceAtLeast(1),
                grade = "",
                teacherRemark = ""
            )
            _examResultsFlow.value = _examResultsFlow.value + newResult
        }

        // Persist newly added class subject and default max marks to PostgreSQL via Go backend
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/classes/$classId/subjects")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val payload = JSONObject().apply {
                    put("class_id", classId)
                    put("subject_name", cleanSubject)
                    put("default_max_marks", maxMarks.coerceAtLeast(1))
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return true
    }

    fun removeSubjectFromClass(classId: String, subjectName: String): Boolean {
        val currentList = getSubjectsForClass(classId)
        val updatedList = currentList.filterNot { it.equals(subjectName, ignoreCase = true) }
        if (updatedList.size == currentList.size) return false

        _classSubjectsMap.value = _classSubjectsMap.value + (classId to updatedList)
        _examResultsByExam.value = _examResultsByExam.value.mapValues { (_, list) ->
            list.filterNot { it.subject.equals(subjectName, ignoreCase = true) }
        }
        _examResultsFlow.value = _examResultsFlow.value.filterNot { it.subject.equals(subjectName, ignoreCase = true) }

        // Delete subject from PostgreSQL class_subjects via Go backend
        ioScope.launch {
            try {
                val encodedSubject = URLEncoder.encode(subjectName.trim(), "UTF-8")
                val url = URL("$BASE_URL/classes/$classId/subjects/$encodedSubject")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("x-user-role", "TEACHER")
                }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return true
    }

    fun enterOrUpdateSubjectMarks(
        subject: String,
        examName: String,
        marksObtained: Int,
        maxMarks: Int = 100
    ) {
        val safeMax = maxMarks.coerceAtLeast(1)
        val safeObtained = marksObtained.coerceIn(0, safeMax)
        val newEntry = SubjectExamResult(
            subject = subject,
            examName = examName,
            marksObtained = safeObtained,
            maxMarks = safeMax,
            grade = "",
            teacherRemark = ""
        )

        val currentExamList = _examResultsByExam.value[examName].orEmpty()
        val existsInExam = currentExamList.any { it.subject.equals(subject, ignoreCase = true) }
        val updatedExamList = if (existsInExam) {
            currentExamList.map { if (it.subject.equals(subject, ignoreCase = true)) newEntry else it }
        } else {
            currentExamList + newEntry
        }
        _examResultsByExam.value = _examResultsByExam.value + (examName to updatedExamList)

        val exists = _examResultsFlow.value.any { it.subject.equals(subject, ignoreCase = true) }
        _examResultsFlow.value = if (exists) {
            _examResultsFlow.value.map {
                if (it.subject.equals(subject, ignoreCase = true)) newEntry else it
            }
        } else {
            listOf(newEntry) + _examResultsFlow.value
        }
        incrementAutomationRuleCounter("auto-marks-grade")

        // Persist updated subject marks and max_marks to PostgreSQL via Go backend
        val stu = activeStudent.value
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/classes/${stu.classId}/exam-marks")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val payload = JSONObject().apply {
                    put("exam_name", examName)
                    put("class_id", stu.classId)
                    put("student_id", stu.id)
                    put("student_name", stu.fullName)
                    put("subject_name", subject)
                    put("marks_obtained", safeObtained)
                    put("max_marks", safeMax)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    /**
     * Teacher enters marks once for a subject across a class & exam.
     * System automatically:
     * 1) Calculates Totals, 2) Calculates Percentages, 3) Assigns School-Rule Grades,
     * 4) Prepares Report Cards, 5) Updates Student Performance History,
     * 6) Keeps results in Pending Approval until teacher/principal approves & publishes.
     */
    fun saveClassSubjectMarksBatch(
        classId: String,
        examName: String,
        subject: String,
        maxMarks: Int,
        marksByStudentId: Map<String, Int>
    ): Int {
        val safeMax = maxMarks.coerceAtLeast(1)
        val classRoster = _sectionStudentRoster.value.filter { it.classId == classId && it.verificationStatus != "BLOCKED" }
        val classSubjects = getSubjectsForClass(classId)
        val currentSheets = _studentExamSheets.value.toMutableList()
        var updatedCount = 0

        classRoster.forEach { stu ->
            val rawMark = marksByStudentId[stu.id] ?: return@forEach
            val safeObtained = rawMark.coerceIn(0, safeMax)
            val sheetId = "sheet-${stu.id}-$examName"
            val existingIdx = currentSheets.indexOfFirst { it.id == sheetId }
            val existingSheet = if (existingIdx >= 0) currentSheets[existingIdx] else null

            val mergedMarks = (existingSheet?.subjectMarks ?: classSubjects.associateWith {
                if (safeMax <= 25) 20 else 80
            }).toMutableMap()
            val mergedMax = (existingSheet?.subjectMaxMarks ?: classSubjects.associateWith { safeMax }).toMutableMap()

            mergedMarks[subject] = safeObtained
            mergedMax[subject] = safeMax

            val totObt = mergedMarks.values.sum()
            val totMax = mergedMax.values.sum().coerceAtLeast(1)
            val pct = (totObt * 100.0) / totMax
            val autoGrade = SchoolGradingPolicy.assignGrade(pct)

            // Update student performance history comparison against previous finished exam
            val examIdx = availableExams.indexOf(examName)
            val prevExamName = availableExams.getOrNull(examIdx - 1)
            val prevSheet = if (prevExamName != null) {
                currentSheets.find { it.studentId == stu.id && it.examName == prevExamName }
            } else null
            val trend = if (prevSheet != null) {
                val delta = pct - prevSheet.percentage
                if (delta >= 0) "+%.1f%% vs %s".format(delta, prevExamName) else "%.1f%% vs %s".format(delta, prevExamName)
            } else {
                "Updated Today"
            }

            val updatedSheet = StudentExamMarkSheet(
                id = sheetId,
                studentId = stu.id,
                studentName = stu.fullName,
                rollNo = stu.rollNo,
                classId = classId,
                examName = examName,
                subjectMarks = mergedMarks,
                subjectMaxMarks = mergedMax,
                totalObtained = totObt,
                totalMax = totMax,
                percentage = pct,
                assignedGrade = autoGrade,
                performanceTrend = trend,
                isApproved = false,
                approvedBy = "",
                approvedAt = ""
            )

            if (existingIdx >= 0) {
                currentSheets[existingIdx] = updatedSheet
            } else {
                currentSheets.add(updatedSheet)
            }
            updatedCount++

            // If this student is the active student profile, also keep single-student view synced
            if (stu.id == activeStudent.value.id || stu.isCurrentDeviceStudent) {
                enterOrUpdateSubjectMarks(subject, examName, safeObtained, safeMax)
            }
        }

        _studentExamSheets.value = currentSheets
        incrementAutomationRuleCounter("auto-marks-grade")

        // Sync batch marks and subject maxMarks to PostgreSQL via Go backend
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/classes/$classId/exam-marks")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val recordsArr = JSONArray()
                classRoster.forEach { stu ->
                    val rawMark = marksByStudentId[stu.id] ?: return@forEach
                    val safeObt = rawMark.coerceIn(0, safeMax)
                    recordsArr.put(
                        JSONObject().apply {
                            put("student_id", stu.id)
                            put("student_name", stu.fullName)
                            put("subject_name", subject)
                            put("marks_obtained", safeObt)
                            put("max_marks", safeMax)
                        }
                    )
                }
                val payload = JSONObject().apply {
                    put("exam_name", examName)
                    put("class_id", classId)
                    put("subject_name", subject)
                    put("max_marks", safeMax)
                    put("records", recordsArr)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return updatedCount
    }

    fun approveStudentExamResult(sheetId: String, approverName: String = "Class Teacher") {
        _studentExamSheets.value = _studentExamSheets.value.map { sheet ->
            if (sheet.id == sheetId) {
                sheet.copy(
                    isApproved = true,
                    approvedBy = approverName,
                    approvedAt = "Published Just Now"
                )
            } else sheet
        }
    }

    fun approveAllClassExamResults(classId: String, examName: String, approverName: String = "Class Teacher"): Int {
        var count = 0
        _studentExamSheets.value = _studentExamSheets.value.map { sheet ->
            if (sheet.classId == classId && sheet.examName == examName && !sheet.isApproved) {
                count++
                sheet.copy(
                    isApproved = true,
                    approvedBy = approverName,
                    approvedAt = "Published Just Now"
                )
            } else sheet
        }
        return count
    }

    fun isExamApprovedForStudent(studentId: String, examName: String): Boolean {
        val matching = _studentExamSheets.value.find {
            (it.studentId == studentId || it.studentId == "stu-sahasra") && it.examName == examName
        }
        return matching?.isApproved ?: true
    }

    fun updateRosterStudentRecord(
        studentId: String,
        fullName: String,
        rollNo: String,
        className: String,
        section: String,
        parentPhone: String
    ): Boolean {
        val cleanName = fullName.trim()
        if (cleanName.isBlank()) return false
        val cleanClass = if (className.startsWith("Class", ignoreCase = true) ||
            className.equals("Nursery", true) || className.equals("LKG", true) || className.equals("UKG", true)
        ) {
            className.trim()
        } else {
            "Class ${className.trim()}"
        }
        val rawSec = section.replace(Regex("(?i)^section\\s*"), "").trim().ifBlank { "A" }
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val newClassId = buildCanonicalClassId(cleanClass, cleanSec)
        val newClassTitle = "$cleanClass – Section $cleanSec"

        // Ensure target class/section exists in availableClasses
        if (_availableClasses.value.none { it.id == newClassId }) {
            createNewClassAndSection(cleanClass, cleanSec)
        }

        _sectionStudentRoster.value = _sectionStudentRoster.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    fullName = cleanName,
                    rollNo = rollNo.trim().ifBlank { s.rollNo },
                    classId = newClassId,
                    className = cleanClass,
                    section = cleanSec,
                    parentPhone = parentPhone.trim().ifBlank { s.parentPhone }
                )
            } else s
        }

        _studentExamSheets.value = _studentExamSheets.value.map { sheet ->
            if (sheet.studentId == studentId) {
                sheet.copy(
                    studentName = cleanName,
                    rollNo = rollNo.trim().ifBlank { sheet.rollNo },
                    classId = newClassId
                )
            } else sheet
        }

        val updatedTarget = _sectionStudentRoster.value.find { it.id == studentId }
        if (updatedTarget?.isCurrentDeviceStudent == true || activeStudent.value.id == studentId) {
            val updatedActive = activeStudent.value.copy(
                fullName = cleanName,
                firstName = cleanName.substringBefore(" "),
                className = cleanClass,
                section = cleanSec,
                classId = newClassId,
                classTitle = newClassTitle,
                parentPhone = parentPhone.trim().ifBlank { activeStudent.value.parentPhone }
            )
            activeStudent.value = updatedActive
            _linkedStudents.value = _linkedStudents.value.map {
                if (it.id == updatedActive.id) updatedActive else it
            }
        }

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/students/$studentId/class-section")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val payload = JSONObject().apply {
                    put("full_name", cleanName)
                    put("roll_no", rollNo.trim())
                    put("class_name", cleanClass)
                    put("section", cleanSec)
                    put("parent_phone", parentPhone.trim())
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return true
    }

    fun addStudentToSectionRoster(
        fullName: String,
        rollNo: String,
        className: String,
        section: String,
        parentPhone: String
    ): SectionRosterStudent {
        val cleanName = fullName.trim()
        val cleanClass = if (className.startsWith("Class", ignoreCase = true) ||
            className.equals("Nursery", true) || className.equals("LKG", true) || className.equals("UKG", true)
        ) {
            className.trim()
        } else {
            "Class ${className.trim()}"
        }
        val rawSec = section.replace(Regex("(?i)^section\\s*"), "").trim().ifBlank { "A" }
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val classId = buildCanonicalClassId(cleanClass, cleanSec)
        if (_availableClasses.value.none { it.id == classId }) {
            createNewClassAndSection(cleanClass, cleanSec)
        }
        val entry = SectionRosterStudent(
            id = "stu-${System.currentTimeMillis()}",
            studentCode = (2300..9999).random().toString(),
            rollNo = rollNo.trim().ifBlank { "0${(_sectionStudentRoster.value.count { it.classId == classId } + 1)}" },
            fullName = cleanName,
            classId = classId,
            className = cleanClass,
            section = cleanSec,
            parentPhone = parentPhone.trim().ifBlank { "+91 98000 00000" },
            loginTimestamp = "Added by Class Teacher",
            verificationStatus = "VERIFIED",
            verifiedBy = "Added & Verified by Class Teacher"
        )
        _sectionStudentRoster.value = listOf(entry) + _sectionStudentRoster.value

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/auth/student-login")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", "TEACHER")
                }
                val payload = JSONObject().apply {
                    put("student_id", entry.id)
                    put("student_code", entry.studentCode)
                    put("roll_no", entry.rollNo)
                    put("full_name", entry.fullName)
                    put("class_name", entry.className)
                    put("section", entry.section)
                    put("parent_phone", entry.parentPhone)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return entry
    }

    // ========================================================================
    // SCHOOL HOLIDAYS CALENDAR (TOTAL SCHOOL, CLASS-SPECIFIC, SECTION-SPECIFIC)
    // ========================================================================

    fun isHolidayForStudentOrClass(
        holiday: SchoolHolidayItem,
        classId: String,
        className: String
    ): Boolean {
        return when (holiday.scopeType) {
            HolidayScopeType.TOTAL_SCHOOL -> true
            HolidayScopeType.CLASS_SPECIFIC -> {
                val cleanTarget = holiday.targetClassName.replace(Regex("(?i)^class\\s*"), "").trim()
                val cleanStudentClass = className.replace(Regex("(?i)^class\\s*"), "").trim()
                cleanTarget.equals(cleanStudentClass, ignoreCase = true) ||
                    classId.startsWith("class-${cleanTarget.lowercase()}")
            }
            HolidayScopeType.SECTION_SPECIFIC -> {
                holiday.targetClassId.equals(classId, ignoreCase = true)
            }
        }
    }

    fun addSchoolHoliday(
        dateIso: String,
        title: String,
        scopeType: HolidayScopeType,
        targetClassName: String = "",
        targetClassId: String = "",
        targetLabel: String = "Total School",
        createdBy: String = "Teacher / Admin"
    ): SchoolHolidayItem {
        val tempId = "hol-${System.currentTimeMillis()}"
        val item = SchoolHolidayItem(
            id = tempId,
            dateIso = dateIso,
            title = title.trim().ifBlank { "Declared Holiday" },
            scopeType = scopeType,
            targetClassName = targetClassName,
            targetClassId = targetClassId,
            targetLabel = targetLabel,
            createdBy = createdBy
        )
        _schoolHolidays.value = (_schoolHolidays.value + item).sortedBy { it.dateIso }

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/holidays")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("title", item.title)
                    put("date_iso", dateIso)
                    put("scope_type", scopeType.name)
                    put("target_class", targetClassName.ifBlank { "ALL" })
                    put("target_section", targetClassId.ifBlank { "ALL" })
                    put("description", targetLabel)
                    put("created_by", createdBy)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    val realId = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        .optJSONObject("data")?.optString("id").orEmpty()
                    if (realId.isNotBlank()) {
                        _schoolHolidays.value = _schoolHolidays.value.map {
                            if (it.id == tempId) it.copy(id = realId) else it
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return item
    }

    fun removeSchoolHoliday(holidayId: String) {
        _schoolHolidays.value = _schoolHolidays.value.filterNot { it.id == holidayId }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/holidays/$holidayId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    // ========================================================================
    // FEES, CASH/CHEQUE RECONCILIATION & FINANCIAL LEAKAGE PREVENTION
    // ========================================================================

    fun recordManualFeeCollection(
        studentName: String,
        classSection: String,
        amountInr: Int,
        paymentMode: String
    ): FeeTransactionRecord {
        val rec = FeeTransactionRecord(
            id = "tx-${System.currentTimeMillis()}",
            studentName = studentName.trim(),
            classSection = classSection.trim(),
            amountInr = amountInr,
            paymentMode = paymentMode,
            receiptNo = "REC-2026-${(8850..9999).random()}",
            timestamp = "Just now",
            status = if (paymentMode.equals("Cheque", ignoreCase = true)) "Pending Bank Match" else "Reconciled"
        )
        _feeTransactions.value = listOf(rec) + _feeTransactions.value
        incrementAutomationRuleCounter("auto-fee-receipt")
        return rec
    }

    fun verifyAndResolveLeakageAlert(alertId: String) {
        _financialLeakageAlerts.value = _financialLeakageAlerts.value.map {
            if (it.id == alertId) it.copy(isVerifiedByHuman = true) else it
        }
    }

    // ========================================================================
    // TIMETABLE & AUTOMATED SUBSTITUTE TEACHER MATCHING (MODULE 7)
    // ========================================================================

    fun reportTeacherLeaveAndFindSubstitutes(
        teacherName: String,
        leaveReason: String,
        periodSlot: String,
        classSectionName: String,
        subject: String
    ): TeacherSubstitutionItem {
        val candidatePool = listOf(
            "Mrs. S. Gupta (Free in $periodSlot)",
            "Mr. K. Kumar (Free in $periodSlot • 0 Subs Today)",
            "Pooja Ma'am (Free in $periodSlot)"
        )
        val tempId = "sub-${System.currentTimeMillis()}"
        val item = TeacherSubstitutionItem(
            id = tempId,
            absentTeacherName = teacherName.trim().ifBlank { "Faculty Member" },
            leaveReason = leaveReason.trim().ifBlank { "Personal / Medical Leave" },
            periodSlot = periodSlot,
            classSectionName = classSectionName,
            subject = subject,
            suggestedSubstitutes = candidatePool,
            assignedSubstitute = null,
            status = "Action Needed"
        )
        _teacherSubstitutions.value = listOf(item) + _teacherSubstitutions.value
        incrementAutomationRuleCounter("auto-teacher-sub")

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/teacher/substitutions")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("absent_teacher_name", item.absentTeacherName)
                    put("leave_reason", item.leaveReason)
                    put("period_slot", item.periodSlot)
                    put("class_section_name", item.classSectionName)
                    put("subject", item.subject)
                    put("suggested_substitutes", org.json.JSONArray(candidatePool))
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    val realId = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        .optJSONObject("data")?.optString("id").orEmpty()
                    if (realId.isNotBlank()) {
                        _teacherSubstitutions.value = _teacherSubstitutions.value.map {
                            if (it.id == tempId) it.copy(id = realId) else it
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return item
    }

    fun assignSubstituteTeacher(subId: String, chosenTeacher: String) {
        _teacherSubstitutions.value = _teacherSubstitutions.value.map {
            if (it.id == subId) {
                it.copy(
                    assignedSubstitute = chosenTeacher,
                    status = "Assigned & Notified"
                )
            } else it
        }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/teacher/substitutions/$subId/assign")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("assigned_substitute", chosenTeacher)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    // ========================================================================
    // STAFF & HR MANAGEMENT (PHASE 2): PROFILES, STAFF ATTENDANCE, LEAVE
    // ========================================================================

    private const val STAFF_LATE_AFTER_MINUTES = 8 * 60 + 15 // 08:15 AM

    private fun nowTimeLabel(): String =
        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())

    private fun formatLeaveDate(cal: java.util.Calendar): String =
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(cal.time)

    fun todayLeaveDateLabel(): String = formatLeaveDate(java.util.Calendar.getInstance())

    /** Returns (fromDate, toDate) labels for a leave starting [startOffsetDays] from today lasting [days]. */
    fun buildLeaveDateRange(startOffsetDays: Int, days: Int): Pair<String, String> {
        val start = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, startOffsetDays) }
        val end = (start.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, (days - 1).coerceAtLeast(0)) }
        return formatLeaveDate(start) to formatLeaveDate(end)
    }

    private fun setStaffDirectoryStatus(staffId: String, status: String) {
        _staffDirectory.value = _staffDirectory.value.map {
            if (it.id == staffId) it.copy(attendanceStatus = status) else it
        }
    }

    private fun upsertTodayAttendance(staffId: String, transform: (StaffAttendanceDay) -> StaffAttendanceDay) {
        val existing = _staffAttendance.value.find { it.staffId == staffId && it.dateLabel == "Today" }
        _staffAttendance.value = if (existing != null) {
            _staffAttendance.value.map { if (it.id == existing.id) transform(it) else it }
        } else {
            listOf(transform(StaffAttendanceDay("sa-$staffId-${System.currentTimeMillis()}", staffId, "Today", status = "Not Checked In"))) +
                _staffAttendance.value
        }
    }

    private val _schoolCampusGeofence = MutableStateFlow(SchoolCampusGeofence())
    val schoolCampusGeofence: StateFlow<SchoolCampusGeofence> = _schoolCampusGeofence.asStateFlow()

    fun updateSchoolCampusLocation(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 500,
        label: String? = null,
        context: Context? = null
    ) {
        val updated = _schoolCampusGeofence.value.copy(
            schoolName = label?.takeIf { it.isNotBlank() } ?: "Captured Start Point (Viman Nagar)",
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters.coerceAtLeast(100),
            isCalibratedFromDevice = true
        )
        _schoolCampusGeofence.value = updated
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putString("campus_name", updated.schoolName)
            ?.putString("campus_lat", updated.latitude.toString())
            ?.putString("campus_lng", updated.longitude.toString())
            ?.putInt("campus_radius", updated.radiusMeters)
            ?.putBoolean("campus_calibrated", true)
            ?.apply()
    }

    fun resetCampusToEdenGardenPune(context: Context? = null) {
        val edenGarden = SchoolCampusGeofence()
        _schoolCampusGeofence.value = edenGarden
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putString("campus_name", edenGarden.schoolName)
            ?.putString("campus_lat", edenGarden.latitude.toString())
            ?.putString("campus_lng", edenGarden.longitude.toString())
            ?.putInt("campus_radius", edenGarden.radiusMeters)
            ?.putBoolean("campus_calibrated", false)
            ?.apply()
    }

    /** Fingerprint + 500m Campus Check-In: within 500m marks teacher Present and logs exact Check-In time for Admin. */
    fun staffCheckIn(
        staffId: String,
        distanceMeters: Int? = null,
        verificationMethod: String = "Fingerprint • Within 500m"
    ): String {
        val status = "Present"
        val time = nowTimeLabel()
        val staffName = _staffDirectory.value.find { it.id == staffId }?.name ?: "Staff Member"
        upsertTodayAttendance(staffId) {
            it.copy(
                checkIn = time,
                checkOut = null,
                status = status,
                distanceMeters = distanceMeters,
                verificationMethod = verificationMethod
            )
        }
        setStaffDirectoryStatus(staffId, status)

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/attendance/check-in")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-teacher-name", staffName)
                }
                val payload = JSONObject().apply {
                    put("staff_id", staffId)
                    put("staff_name", staffName)
                    put("date", java.time.LocalDate.now().toString())
                    put("check_in", time)
                    if (distanceMeters != null) put("distance_meters", distanceMeters)
                    put("verification_method", verificationMethod)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
        return status
    }

    /** 1-tap staff Check Out at the end of the day. */
    fun staffCheckOut(staffId: String) {
        val time = nowTimeLabel()
        val staffName = _staffDirectory.value.find { it.id == staffId }?.name ?: "Staff Member"
        upsertTodayAttendance(staffId) { it.copy(checkOut = time) }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/attendance/check-out")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-teacher-name", staffName)
                }
                val payload = JSONObject().apply {
                    put("staff_id", staffId)
                    put("staff_name", staffName)
                    put("date", java.time.LocalDate.now().toString())
                    put("check_out", time)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    /** Principal / Admin correction of a staff member's attendance for today. */
    fun adminMarkStaffAttendance(staffId: String, status: String) {
        val staffName = _staffDirectory.value.find { it.id == staffId }?.name ?: "Staff Member"
        upsertTodayAttendance(staffId) { day ->
            when (status) {
                "Present" -> day.copy(status = "Present", checkIn = day.checkIn ?: nowTimeLabel())
                else -> day.copy(status = status, checkIn = if (status == "Absent" || status == "On Leave") null else day.checkIn, checkOut = null)
            }
        }
        setStaffDirectoryStatus(staffId, status)
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/attendance")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-teacher-name", staffName)
                }
                val payload = JSONObject().apply {
                    put("staff_id", staffId)
                    put("staff_name", staffName)
                    put("date", java.time.LocalDate.now().toString())
                    put("status", status)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun updateStaffContactProfile(staffId: String, phone: String, email: String, qualification: String) {
        val staffName = _staffDirectory.value.find { it.id == staffId }?.name ?: "Staff Member"
        _staffDirectory.value = _staffDirectory.value.map {
            if (it.id == staffId) it.copy(
                phone = phone.trim().ifBlank { it.phone },
                email = email.trim(),
                qualification = qualification.trim().ifBlank { it.qualification }
            ) else it
        }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/$staffId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-teacher-name", staffName)
                }
                val payload = JSONObject().apply {
                    put("full_name", staffName)
                    put("phone", phone.trim())
                    put("email", email.trim())
                    put("qualification", qualification.trim())
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun applyStaffLeave(
        staffId: String,
        leaveType: String,
        startOffsetDays: Int,
        days: Int,
        reason: String
    ): StaffLeaveRequest {
        val staff = _staffDirectory.value.find { it.id == staffId }
        val safeDays = days.coerceIn(1, 30)
        val (from, to) = buildLeaveDateRange(startOffsetDays, safeDays)
        val tempId = "slv-${System.currentTimeMillis()}"
        val request = StaffLeaveRequest(
            id = tempId,
            staffId = staffId,
            staffName = staff?.name ?: "Staff Member",
            leaveType = leaveType,
            fromDate = from,
            toDate = to,
            days = safeDays,
            reason = reason.trim().ifBlank { leaveType },
            status = "Pending",
            appliedOn = "Today"
        )
        _staffLeaveRequests.value = listOf(request) + _staffLeaveRequests.value

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/leaves")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-teacher-name", request.staffName)
                }
                val payload = JSONObject().apply {
                    put("staff_id", staffId)
                    put("staff_name", request.staffName)
                    put("leave_type", leaveType)
                    put("from_date", from)
                    put("to_date", to)
                    put("days", safeDays)
                    put("reason", request.reason)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    val realId = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        .optJSONObject("data")?.optString("id").orEmpty()
                    if (realId.isNotBlank()) {
                        _staffLeaveRequests.value = _staffLeaveRequests.value.map {
                            if (it.id == tempId) it.copy(id = realId) else it
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return request
    }

    fun cancelStaffLeave(leaveId: String) {
        _staffLeaveRequests.value = _staffLeaveRequests.value.filterNot { it.id == leaveId && it.status == "Pending" }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/leaves/$leaveId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    /**
     * Approve a staff leave. Automation: for teaching staff, a substitution request is
     * automatically raised in the Substitution Hub, and today's attendance is set to "On Leave"
     * when the leave starts today.
     */
    fun approveStaffLeave(leaveId: String, reviewer: String = "Principal") {
        val request = _staffLeaveRequests.value.find { it.id == leaveId } ?: return
        val staff = _staffDirectory.value.find { it.id == request.staffId }
        val isTeaching = staff?.staffType == "Teaching"

        if (isTeaching && staff != null) {
            reportTeacherLeaveAndFindSubstitutes(
                teacherName = "${staff.name} (${staff.subjects.ifBlank { staff.department }})",
                leaveReason = "${request.leaveType}: ${request.fromDate} – ${request.toDate}",
                periodSlot = "All periods",
                classSectionName = staff.classesAssigned,
                subject = staff.subjects.ifBlank { staff.department }
            )
        }

        _staffLeaveRequests.value = _staffLeaveRequests.value.map {
            if (it.id == leaveId) it.copy(status = "Approved", reviewedBy = reviewer, substituteRequested = isTeaching) else it
        }

        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/leaves/$leaveId/status")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("status", "Approved")
                    put("reviewed_by", reviewer)
                    put("substitute_requested", isTeaching)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }

        if (request.fromDate == todayLeaveDateLabel()) {
            adminMarkStaffAttendance(request.staffId, "On Leave")
        }
    }

    fun rejectStaffLeave(leaveId: String, reviewer: String = "Principal") {
        _staffLeaveRequests.value = _staffLeaveRequests.value.map {
            if (it.id == leaveId) it.copy(status = "Rejected", reviewedBy = reviewer) else it
        }
        ioScope.launch {
            try {
                val url = URL("$BASE_URL/staff/leaves/$leaveId/status")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("status", "Rejected")
                    put("reviewed_by", reviewer)
                    put("substitute_requested", false)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    /** Remaining leave balance per type for a staff member (quota minus approved + pending days). */
    fun staffLeaveBalance(staffId: String, requests: List<StaffLeaveRequest>): Map<String, Pair<Int, Int>> {
        return StaffLeavePolicy.annualQuota.mapValues { (type, quota) ->
            val used = requests
                .filter { it.staffId == staffId && it.leaveType == type && it.status != "Rejected" }
                .sumOf { it.days }
            (quota - used).coerceAtLeast(0) to quota
        }
    }

    // ========================================================================
    // ADMISSIONS & ENQUIRY CRM PIPELINE (MODULE 10)
    // ========================================================================

    fun createAdmissionEnquiry(
        childName: String,
        parentName: String,
        parentPhone: String,
        targetGrade: String,
        source: String = "Direct App Enquiry"
    ): AdmissionEnquiryItem {
        val item = AdmissionEnquiryItem(
            id = "adm-${System.currentTimeMillis()}",
            childName = childName.trim(),
            parentName = parentName.trim(),
            parentPhone = parentPhone.trim(),
            targetGrade = targetGrade,
            counsellorName = "Ms. Anitha (Auto-Assigned)",
            stage = "New Enquiry",
            nextFollowUp = "Tomorrow, 10:30 AM",
            source = source.trim()
        )
        _admissionEnquiries.value = listOf(item) + _admissionEnquiries.value
        incrementAutomationRuleCounter("auto-admission-lead")
        return item
    }

    fun advanceAdmissionStage(enquiryId: String) {
        _admissionEnquiries.value = _admissionEnquiries.value.map { item ->
            if (item.id == enquiryId) {
                val nextStage = when (item.stage) {
                    "New Enquiry" -> "Follow-Up Scheduled"
                    "Follow-Up Scheduled" -> "Interview Scheduled"
                    else -> "Admitted"
                }
                val nextFollow = if (nextStage == "Admitted") "Fee Paid & Seat Confirmed" else "Scheduled in CRM"
                item.copy(stage = nextStage, nextFollowUp = nextFollow)
            } else item
        }
    }

    // ========================================================================
    // CENTRAL AUTOMATION ENGINE (SECTION 3)
    // ========================================================================

    fun toggleAutomationRule(ruleId: String) {
        _automationRules.value = _automationRules.value.map {
            if (it.id == ruleId) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun incrementAutomationRuleCounter(ruleId: String) {
        _automationRules.value = _automationRules.value.map {
            if (it.id == ruleId && it.isEnabled) {
                it.copy(
                    executionsCount = it.executionsCount + 1,
                    lastTriggeredText = "Just now"
                )
            } else it
        }
    }

    fun triggerBusRouteNotification(routeId: String, nextStatus: String) {
        _busRoutes.value = _busRoutes.value.map {
            if (it.id == routeId) it.copy(tripStatus = nextStatus) else it
        }
    }

    fun submitHelpdeskTicket(category: String, priority: String, message: String): HelpdeskTicket {
        val ticket = HelpdeskTicket(
            ticketId = "GSB-${(1050..9999).random()}",
            category = category,
            priority = priority,
            message = message.trim(),
            createdDate = "Today",
            status = "Open"
        )
        _helpdeskTickets.value = listOf(ticket) + _helpdeskTickets.value
        return ticket
    }

    // ========================================================================
    // ACADEMIC YEAR ROLLOVER, CLASS/SECTION CREATION, STUDENT PROMOTION &
    // SELECTIVE SECTION-WISE DATA RETENTION / DELETION (TEACHER & ADMIN)
    // ========================================================================

    fun createNewClassAndSection(
        classNameInput: String,
        sectionInput: String,
        seatCapacity: Int = 40,
        scope: CoroutineScope? = null
    ): SchoolClass {
        val cleanNum = classNameInput.replace(Regex("(?i)^class\\s*"), "").trim().ifBlank { "10" }
        val rawSec = sectionInput.replace(Regex("(?i)^section\\s*"), "").trim().ifBlank { "A" }
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val normalizedClassName = if (cleanNum.startsWith("NUR", true) || cleanNum.startsWith("LKG", true) || cleanNum.startsWith("UKG", true)) {
            cleanNum.uppercase()
        } else {
            "Class ${cleanNum.uppercase()}"
        }
        val classId = buildCanonicalClassId(normalizedClassName, cleanSec)
        val classTitle = "$normalizedClassName – Section $cleanSec"

        if (!_classOptions.value.any { it.equals(normalizedClassName, ignoreCase = true) }) {
            _classOptions.value = _classOptions.value + normalizedClassName
        }
        if (!_sectionOptions.value.any { it.equals(cleanSec, ignoreCase = true) }) {
            _sectionOptions.value = _sectionOptions.value + cleanSec
        }

        val existing = _availableClasses.value.find { it.id == classId }
        val schoolClass = existing ?: SchoolClass(
            id = classId,
            name = classTitle,
            studentCount = 0,
            className = normalizedClassName,
            section = cleanSec
        )
        if (existing == null) {
            rebuildClassOptionFlows(_availableClasses.value + schoolClass)
        }

        ensureIsolatedSectionData(classId, classTitle)

        scope?.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 4000
                    readTimeout = 4000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("class_id", classId)
                    put("class_name", normalizedClassName)
                    put("section", cleanSec)
                    put("seat_capacity", seatCapacity)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    fetchSchoolClassesFromBackend(scope)
                }
            } catch (_: Exception) {}
        }

        return schoolClass
    }

    fun updateClassAndSection(
        schoolClass: SchoolClass,
        newClassNameInput: String,
        newSectionInput: String,
        scope: CoroutineScope,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanNum = newClassNameInput.replace(Regex("(?i)^class\\s*"), "").trim()
        val rawSec = newSectionInput.replace(Regex("(?i)^section\\s*"), "").trim()
        val cleanSec = if (rawSec.length <= 2) rawSec.uppercase() else rawSec
        val normalizedClassName = when {
            cleanNum.isBlank() -> return
            cleanNum.startsWith("NUR", true) || cleanNum.startsWith("LKG", true) || cleanNum.startsWith("UKG", true) -> cleanNum.uppercase()
            else -> "Class ${cleanNum.uppercase()}"
        }
        val targetParam = schoolClass.dbUuid.ifBlank { schoolClass.id }

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$targetParam")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("class_name", normalizedClassName)
                    put("section", cleanSec)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val msg = runCatching {
                    val j = JSONObject(body)
                    j.optString("message").ifBlank { j.optString("error") }
                }.getOrDefault("")
                if (code in 200..299) {
                    fetchSchoolClassesFromBackend(scope)
                    withContext(Dispatchers.Main) {
                        onResult(true, msg.ifBlank { "Updated to $normalizedClassName – Section $cleanSec" })
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, msg.ifBlank { "Update failed (HTTP $code)" })
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Network error: ${e.message}")
                }
            }
        }
    }

    fun deleteClassAndSection(
        schoolClass: SchoolClass,
        scope: CoroutineScope,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val targetParam = schoolClass.dbUuid.ifBlank { schoolClass.id }
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$targetParam")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val msg = runCatching {
                    val j = JSONObject(body)
                    j.optString("message").ifBlank { j.optString("error") }
                }.getOrDefault("")
                if (code in 200..299) {
                    fetchSchoolClassesFromBackend(scope)
                    withContext(Dispatchers.Main) {
                        onResult(true, msg.ifBlank { "Deleted ${schoolClass.name}" })
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, msg.ifBlank { "Delete failed (HTTP $code)" })
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Network error: ${e.message}")
                }
            }
        }
    }

    fun promoteStudentsToTargetClassSection(
        context: Context,
        fromClassId: String,
        targetClassNameInput: String,
        targetSectionInput: String,
        newAcademicYear: String = "2027-2028",
        scope: CoroutineScope
    ): Int {
        val targetSchoolClass = createNewClassAndSection(
            classNameInput = targetClassNameInput,
            sectionInput = targetSectionInput,
            scope = scope
        )
        val cleanNum = targetClassNameInput.replace(Regex("(?i)^class\\s*"), "").trim().uppercase()
        val cleanSec = targetSectionInput.replace(Regex("(?i)^section\\s*"), "").trim().uppercase()
        val normalizedClassName = "Class $cleanNum"
        _currentAcademicYear.value = newAcademicYear

        val studentsToPromote = _sectionStudentRoster.value.filter { it.classId == fromClassId }
        if (studentsToPromote.isEmpty()) return 0

        _sectionStudentRoster.value = _sectionStudentRoster.value.map { stu ->
            if (stu.classId == fromClassId) {
                stu.copy(
                    classId = targetSchoolClass.id,
                    className = normalizedClassName,
                    section = cleanSec,
                    loginTimestamp = "Promoted to $normalizedClassName-$cleanSec ($newAcademicYear)",
                    verificationStatus = "VERIFIED",
                    verifiedBy = "Academic Year Promotion"
                )
            } else stu
        }

        // If the currently active student on this device was in fromClassId, automatically shift them to the promoted class
        if (activeStudent.value.classId == fromClassId) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("class_name", normalizedClassName)
                .putString("section", cleanSec)
                .putString("class_id", targetSchoolClass.id)
                .apply()

            applyStudentSessionLocally(
                studentId = activeStudent.value.id,
                studentCode = activeStudent.value.studentCode,
                fullName = activeStudent.value.fullName,
                className = normalizedClassName,
                section = cleanSec,
                classId = targetSchoolClass.id,
                scope = scope
            )
        } else {
            selectedClass.value = targetSchoolClass
        }

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$fromClassId/promote")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("target_class_name", normalizedClassName)
                    put("target_section", cleanSec)
                    put("academic_year", newAcademicYear)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }

        return studentsToPromote.size
    }

    fun requestUnlockForNewAcademicYearSelect(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putBoolean("is_profile_locked", false)
            .apply()
        isStudentLoggedIn.value = false
    }

    fun selectiveCleanupSectionData(
        classId: String,
        deleteNotes: Boolean,
        deleteDoubts: Boolean,
        deleteHomework: Boolean,
        deleteStudentRoster: Boolean,
        scope: CoroutineScope? = null
    ): String {
        val removedSummary = mutableListOf<String>()
        if (deleteNotes) {
            val count = _studyNotes.value.count { it.classId == classId }
            _studyNotes.value = _studyNotes.value.filter { it.classId != classId }
            removedSummary.add("$count Notes")
        }
        if (deleteDoubts) {
            val count = _discussions.value.count { it.classId == classId }
            _discussions.value = _discussions.value.filter { it.classId != classId }
            removedSummary.add("$count Doubts")
        }
        if (deleteHomework) {
            val count = _homeworkList.value.count { it.classId == classId }
            _homeworkList.value = _homeworkList.value.filter { it.classId != classId }
            removedSummary.add("$count Homework items")
        }
        if (deleteStudentRoster) {
            val count = _sectionStudentRoster.value.count { it.classId == classId }
            _sectionStudentRoster.value = _sectionStudentRoster.value.filter { it.classId != classId }
            removedSummary.add("$count Student logins")
        }

        scope?.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$classId/selective-cleanup")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                val payload = JSONObject().apply {
                    put("delete_notes", deleteNotes)
                    put("delete_doubts", deleteDoubts)
                    put("delete_homework", deleteHomework)
                    put("delete_roster", deleteStudentRoster)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }

        return if (removedSummary.isEmpty()) {
            "All section data kept intact"
        } else {
            "Deleted ${removedSummary.joinToString(", ")}"
        }
    }

    fun editDoubt(
        discussionId: String,
        newSubject: String,
        newTitle: String,
        newContent: String,
        scope: CoroutineScope
    ) {
        _discussions.value = _discussions.value.map { post ->
            if (post.id == discussionId) {
                post.copy(
                    subject = newSubject.trim(),
                    title = newTitle.trim(),
                    content = newContent.trim(),
                    timeAgo = "Edited just now"
                )
            } else post
        }

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/discussions/$discussionId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PUT"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val payload = JSONObject().apply {
                    put("subject_name", newSubject.trim())
                    put("topic_title", newTitle.trim())
                    put("content", newContent.trim())
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun deleteDoubt(discussionId: String, scope: CoroutineScope) {
        _discussions.value = _discussions.value.filter { it.id != discussionId }

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/discussions/$discussionId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 2500
                    readTimeout = 2500
                }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun editReply(
        discussionId: String,
        replyId: String,
        newContent: String,
        scope: CoroutineScope
    ) {
        _discussions.value = _discussions.value.map { post ->
            if (post.id == discussionId) {
                post.copy(
                    replies = post.replies.map { rep ->
                        if (rep.id == replyId) rep.copy(content = newContent.trim(), timeAgo = "Edited") else rep
                    }
                )
            } else post
        }

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/discussions/$discussionId/replies/$replyId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "PUT"
                    connectTimeout = 2500
                    readTimeout = 2500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val payload = JSONObject().apply {
                    put("content", newContent.trim())
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun deleteReply(
        discussionId: String,
        replyId: String,
        scope: CoroutineScope
    ) {
        _discussions.value = _discussions.value.map { post ->
            if (post.id == discussionId) {
                val remaining = post.replies.filter { it.id != replyId }
                post.copy(
                    repliesCount = remaining.size,
                    replies = remaining
                )
            } else post
        }

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/discussions/$discussionId/replies/$replyId")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 2500
                    readTimeout = 2500
                }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun fetchDiscussionsFromBackend(classId: String = "class-10a", scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$classId/discussions?limit=20")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=$classId]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", classId)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")

                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val discArray = json.getJSONObject("data").optJSONArray("discussions")
                        val list = mutableListOf<DiscussionPost>()
                        if (discArray != null) {
                            for (i in 0 until discArray.length()) {
                                val item = discArray.getJSONObject(i)
                                val id = item.optString("id")
                                val imgUrl = resolveRenderableMediaUrl(item.optString("image_url", "").ifBlank { null })
                                val existingPost = _discussions.value.find { it.id == id }
                                list.add(
                                    DiscussionPost(
                                        id = id,
                                        classId = item.optString("class_id", classId),
                                        authorName = item.optString("author_name", "Student"),
                                        authorRole = item.optString("author_role", "STUDENT"),
                                        subject = item.optString("subject_name", "Mathematics"),
                                        title = item.optString("topic_title", ""),
                                        content = item.optString("content", ""),
                                        imageUri = imgUrl ?: existingPost?.imageUri,
                                        hasEquationCard = false,
                                        timeAgo = "Recent",
                                        repliesCount = item.optInt("replies_count", 0),
                                        replies = existingPost?.replies ?: emptyList()
                                    )
                                )
                            }
                        }
                        withContext(Dispatchers.Main) {
                            _discussions.value = list + _discussions.value.filter { it.classId != classId }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun fetchNotesFromBackend(classId: String = "class-10a", scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/classes/$classId/notes")
                Log.i("GenesisAPI", "--> GET $url [student=${activeStudent.value.fullName}, class=$classId]")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-student-name", activeStudent.value.fullName)
                    setRequestProperty("x-class-id", classId)
                    setRequestProperty("x-user-role", currentRole.value.name)
                }
                Log.i("GenesisAPI", "<-- ${conn.responseCode} GET $url")

                if (conn.responseCode == 200) {
                    val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(jsonStr)
                    if (json.optBoolean("success")) {
                        val notesArray = json.getJSONObject("data").optJSONArray("notes")
                        val list = mutableListOf<ClassNote>()
                        if (notesArray != null) {
                            for (i in 0 until notesArray.length()) {
                                val item = notesArray.getJSONObject(i)
                                list.add(
                                    ClassNote(
                                        id = item.optString("id"),
                                        classId = item.optString("class_id", classId),
                                        subject = item.optString("subject_name", "Mathematics"),
                                        title = item.optString("title", ""),
                                        chapter = item.optString("chapter_name", ""),
                                        fileUrl = resolveRenderableMediaUrl(item.optString("file_url", "")) ?: "",
                                        fileType = item.optString("file_type", "PDF"),
                                        teacherName = item.optString("teacher_name", "Teacher"),
                                        date = "Recent"
                                    )
                                )
                            }
                        }
                        withContext(Dispatchers.Main) {
                            _studyNotes.value = list + _studyNotes.value.filter { it.classId != classId }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun resolveRenderableMediaUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val gcsPrefix = "https://storage.googleapis.com/genesis-school-storage/"
        if (rawUrl.startsWith(gcsPrefix)) {
            val objectKey = rawUrl.removePrefix(gcsPrefix)
            return "$BASE_URL/media/view?key=${java.net.URLEncoder.encode(objectKey, "UTF-8")}"
        }
        return rawUrl
    }

    /**
     * Compresses camera/gallery images down to max 1280px & <= 150 KB JPEG before upload
     * (cuts Object Storage & egress bandwidth by ~85% across all roles). PDFs pass through untouched.
     */
    private fun compressImageBytesIfNeeded(rawBytes: ByteArray, mimeType: String, maxDim: Int = 1280, targetMaxBytes: Int = 150_000): Pair<ByteArray, String> {
        if (!mimeType.startsWith("image/", ignoreCase = true) || rawBytes.size <= targetMaxBytes) {
            return rawBytes to mimeType
        }
        return try {
            val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > maxDim * 2 || (boundsOpts.outHeight / sampleSize) > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return rawBytes to mimeType
            val w = decoded.width
            val h = decoded.height
            val scale = minOf(1f, maxDim.toFloat() / maxOf(w, h).coerceAtLeast(1))
            val scaled = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(decoded, (w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else decoded

            val baos = java.io.ByteArrayOutputStream()
            var quality = 82
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
            while (baos.size() > targetMaxBytes && quality > 40) {
                baos.reset()
                quality -= 12
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
            }
            scaled.recycle()
            baos.toByteArray() to "image/jpeg"
        } catch (_: Exception) {
            rawBytes to mimeType
        }
    }

    private fun uploadLocalUriToCloudStorage(uriString: String?, folder: String, classId: String = selectedClass.value.id): String? {
        if (uriString.isNullOrBlank()) return null
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) return resolveRenderableMediaUrl(uriString)
        val ctx = appContext ?: return null
        return try {
            val parsedUri = android.net.Uri.parse(uriString)
            val rawBytes = ctx.contentResolver.openInputStream(parsedUri)?.use { it.readBytes() } ?: return null
            val detectedMime = ctx.contentResolver.getType(parsedUri) ?: if (folder == "notes") "application/pdf" else "image/jpeg"
            val (bytes, mimeType) = compressImageBytesIfNeeded(rawBytes, detectedMime)
            val ext = when {
                mimeType.contains("pdf") -> "pdf"
                mimeType.contains("png") -> "png"
                else -> "jpg"
            }
            val fileName = "${folder}_${System.currentTimeMillis()}.$ext"
            val boundary = "----GenesisBoundary${System.currentTimeMillis()}"
            val url = URL("$BASE_URL/media/upload")
            val conn = openSchoolHttpConnection(url).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("x-user-role", currentRole.value.name)
                setRequestProperty("x-student-name", activeStudent.value.fullName)
                setRequestProperty("x-student-id", activeStudent.value.id)
                setRequestProperty("x-class-id", classId)
            }
            conn.outputStream.use { out ->
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"folder\"\r\n\r\n$folder\r\n".toByteArray())
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"class_id\"\r\n\r\n$classId\r\n".toByteArray())
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n".toByteArray())
                out.write("Content-Type: $mimeType\r\n\r\n".toByteArray())
                out.write(bytes)
                out.write("\r\n--$boundary--\r\n".toByteArray())
                out.flush()
            }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(resp).optJSONObject("data")?.optString("url")?.ifBlank { null }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun postStudyNote(
        classId: String,
        subject: String,
        title: String,
        chapter: String,
        teacherName: String = "Mrs. Gupta",
        localPdfUri: String? = null,
        pageCount: Int = 0,
        scope: CoroutineScope
    ) {
        val newNote = ClassNote(
            id = "note-${System.currentTimeMillis()}",
            classId = classId,
            subject = subject,
            title = title,
            chapter = if (pageCount > 0) "$chapter • $subject ($pageCount pg PDF)" else "$chapter • $subject",
            fileUrl = "https://storage.googleapis.com/genesis-school-storage/notes/${subject.lowercase()}.pdf",
            fileType = "PDF",
            teacherName = teacherName,
            date = "Just now",
            localPdfUri = localPdfUri,
            pageCount = pageCount
        )
        _studyNotes.value = listOf(newNote) + _studyNotes.value

        scope.launch(Dispatchers.IO) {
            try {
                val uploadedUrl = uploadLocalUriToCloudStorage(localPdfUri, "notes", classId) ?: newNote.fileUrl
                val url = URL("$BASE_URL/classes/$classId/notes")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-teacher-name", teacherName)
                    setRequestProperty("x-class-id", classId)
                }
                val payload = JSONObject().apply {
                    put("subject_name", subject)
                    put("title", title)
                    put("chapter_name", chapter)
                    put("teacher_name", teacherName)
                    put("file_url", uploadedUrl)
                    put("file_type", "PDF")
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                conn.responseCode
            } catch (_: Exception) {}
        }
    }

    fun postAdminNotice(
        title: String,
        content: String,
        category: String = "CIRCULAR",
        author: String = "Principal's Office",
        imageUri: String? = null,
        targetAudience: String = "Entire School",
        startsAtIso: String? = null,
        expiresAtIso: String? = null,
        scope: CoroutineScope
    ) {
        val computedStatus = runCatching {
            val now = java.time.Instant.now()
            val start = startsAtIso?.takeIf { it.isNotBlank() }?.let { java.time.Instant.parse(it) }
            val end = expiresAtIso?.takeIf { it.isNotBlank() }?.let { java.time.Instant.parse(it) }
            when {
                start != null && start.isAfter(now) -> "SCHEDULED"
                end != null && end.isBefore(now) -> "EXPIRED"
                else -> "ACTIVE"
            }
        }.getOrDefault("ACTIVE")

        val tempId = "notice-${System.currentTimeMillis()}"
        val newNotice = AdminNotice(
            id = tempId,
            title = title,
            content = content,
            category = category.uppercase(),
            author = author,
            date = if (computedStatus == "SCHEDULED") "Scheduled" else "Just now",
            imageUrl = imageUri,
            targetAudience = targetAudience,
            startsAtIso = startsAtIso,
            expiresAtIso = expiresAtIso,
            scheduleStatus = computedStatus
        )
        _homeNotices.value = listOf(newNotice) + _homeNotices.value

        scope.launch(Dispatchers.IO) {
            try {
                // Upload local camera/gallery image to private GCS bucket first so it persists in Cloud SQL
                val uploadedImageUrl = uploadLocalUriToCloudStorage(imageUri, "notices", "all") ?: ""

                val url = URL("$BASE_URL/posts")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-school-id", _activeSchool.value.id)
                    setRequestProperty("x-user-role", currentRole.value.name)
                    setRequestProperty("x-teacher-name", author)
                }
                val computedTargetType = when {
                    targetAudience.equals("All School", ignoreCase = true) || targetAudience.equals("Entire School", ignoreCase = true) -> "ALL"
                    targetAudience.contains("Section", ignoreCase = true) -> "SECTION"
                    else -> "CLASS"
                }
                val payload = JSONObject().apply {
                    put("title", title)
                    put("content", content)
                    put("category", category.uppercase())
                    put("author_name", author)
                    put("target_type", computedTargetType)
                    put("target_audience", targetAudience)
                    if (uploadedImageUrl.isNotBlank()) put("image_url", uploadedImageUrl)
                    if (!startsAtIso.isNullOrBlank()) put("starts_at", startsAtIso)
                    if (!expiresAtIso.isNullOrBlank()) put("expires_at", expiresAtIso)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                val code = conn.responseCode
                if (code in 200..299) {
                    fetchNoticesFromBackend(scope)
                }
            } catch (_: Exception) {}
        }
    }

    fun postDoubt(
        subject: String,
        title: String,
        content: String,
        hasImage: Boolean = false,
        imageUri: String? = null,
        authorName: String = activeStudent.value.fullName,
        authorRole: String = currentRole.value.name,
        classId: String = selectedClass.value.id,
        scope: CoroutineScope
    ) {
        val tempId = "disc-${System.currentTimeMillis()}"
        val newPost = DiscussionPost(
            id = tempId,
            classId = classId,
            authorName = authorName,
            authorRole = authorRole,
            subject = subject,
            title = title,
            content = content,
            imageUri = imageUri,
            hasEquationCard = hasImage && imageUri == null,
            equationFormula = if (hasImage) "Attached Question Diagram" else "",
            equationSubtext = if (hasImage) "IMG_20261010.jpg" else "",
            timeAgo = "Just now",
            repliesCount = 0,
            replies = emptyList()
        )
        _discussions.value = listOf(newPost) + _discussions.value

        scope.launch(Dispatchers.IO) {
            try {
                // Upload local content:// photo to Google Cloud Storage first so classmates & teachers can view it
                val cloudImageUrl = uploadLocalUriToCloudStorage(imageUri, "doubts", classId)
                val url = URL("$BASE_URL/classes/$classId/discussions")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-student-name", authorName)
                    setRequestProperty("x-student-id", activeStudent.value.id)
                    setRequestProperty("x-user-role", authorRole)
                    setRequestProperty("x-class-id", classId)
                }
                val payload = JSONObject().apply {
                    put("subject_name", subject)
                    put("topic_title", title)
                    put("content", content)
                    put("author_name", authorName)
                    put("author_role", authorRole)
                    if (!cloudImageUrl.isNullOrBlank()) {
                        put("image_url", cloudImageUrl)
                    }
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val realId = JSONObject(resp).optJSONObject("data")?.optString("id").orEmpty()
                    if (realId.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            _discussions.value = _discussions.value.map { post ->
                                if (post.id == tempId) {
                                    post.copy(id = realId, imageUri = cloudImageUrl ?: post.imageUri)
                                } else post
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun postReply(
        discussionId: String,
        content: String,
        imageUri: String? = null,
        authorName: String = activeStudent.value.fullName,
        authorRole: String = currentRole.value.name,
        scope: CoroutineScope
    ) {
        val isVerified = authorRole == "TEACHER"
        val cleanContent = content.ifBlank { "Attached solution photo" }
        val tempReplyId = "rep-${System.currentTimeMillis()}"
        val newReply = DiscussionReply(
            id = tempReplyId,
            discussionId = discussionId,
            authorName = authorName,
            authorRole = authorRole,
            content = cleanContent,
            imageUri = imageUri,
            isVerified = isVerified,
            likesCount = if (isVerified) 1 else 0,
            timeAgo = "Just now"
        )
        _discussions.value = _discussions.value.map { post ->
            if (post.id == discussionId) {
                post.copy(
                    repliesCount = post.repliesCount + 1,
                    replies = post.replies + newReply
                )
            } else post
        }

        scope.launch(Dispatchers.IO) {
            try {
                val cloudImageUrl = uploadLocalUriToCloudStorage(imageUri, "replies")
                val url = URL("$BASE_URL/discussions/$discussionId/replies")
                val conn = openSchoolHttpConnection(url).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-student-name", authorName)
                    setRequestProperty("x-student-id", activeStudent.value.id)
                    setRequestProperty("x-user-role", authorRole)
                    setRequestProperty("x-class-id", selectedClass.value.id)
                }
                val payload = JSONObject().apply {
                    put("content", cleanContent)
                    put("author_name", authorName)
                    put("author_role", authorRole)
                    if (!cloudImageUrl.isNullOrBlank()) {
                        put("image_url", cloudImageUrl)
                    }
                }
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val realReplyId = JSONObject(resp).optJSONObject("data")?.optString("id").orEmpty()
                    if (realReplyId.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            _discussions.value = _discussions.value.map { post ->
                                if (post.id == discussionId) {
                                    post.copy(
                                        replies = post.replies.map { rep ->
                                            if (rep.id == tempReplyId) rep.copy(id = realReplyId, imageUri = cloudImageUrl ?: rep.imageUri)
                                            else rep
                                        }
                                    )
                                } else post
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
