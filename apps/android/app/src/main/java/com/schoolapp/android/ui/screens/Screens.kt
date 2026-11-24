package com.schoolapp.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import com.schoolapp.android.ui.theme.LocalSchoolBrand
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.schoolapp.android.R
import com.schoolapp.android.data.*
import com.schoolapp.android.ui.theme.GenesisColors
import kotlinx.coroutines.delay

private object NavigationBridge {
    var pendingMyClassTab: Int? = null
    var openFeePaymentDirectly: Boolean = false
}

// ============================================================================
// 04 HOME — SCHOOL FEED, STUDENT LINKING & CEO ENTERPRISE HUB
// ============================================================================

private enum class HomeSubRoute {
    FEED,
    ADD_STUDENT,
    NOTICE_DETAILS,
    PUBLISH_NOTICE,
    ATTENDANCE,
    REPORT_CARD,
    DIGITAL_ID,
    ONLINE_QUIZ,
    ASSIGNMENTS,
    EXAM_SCHEDULE,
    CALENDAR,
    SECTION_VERIFICATION,
    SCHOOL_INTELLIGENCE,
    AUTOMATION_ENGINE,
    AI_ASSISTANT,
    MULTI_HOMEWORK,
    SUBSTITUTION_HUB,
    ADMISSIONS_CRM,
    STUDENT_360,
    STAFF_TRANSPORT,
    STAFF_HR,
    FEE_STRUCTURE,
    MARKS_AUTOMATION,
    PROUD_PARENT_QR,
    CLASS_YEAR_MANAGER,
    MISTAKE_ANALYSIS,
    ADMIN_SIBLINGS
}

@Composable
fun HomeScreen(
    onNavigateToTab: (Int) -> Unit = {},
    onOpenChatGPT: (String?, Uri?) -> Unit = { _, _ -> },
    resetSignal: Int = 0
) {
    val coroutineScope = rememberCoroutineScope()
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val linkedStudents by DiscussionRepository.linkedStudents.collectAsState()
    val activeStudent by DiscussionRepository.activeStudent.collectAsState()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val notices by DiscussionRepository.homeNotices.collectAsState()
    val isSiblingsFormSubmitted by DiscussionRepository.isSiblingsFormSubmitted.collectAsState()

    var subRoute by remember { mutableStateOf(HomeSubRoute.FEED) }
    var staffHrInitialTab by remember { mutableIntStateOf(0) }
    var selectedNotice by remember { mutableStateOf<AdminNotice?>(null) }
    var showChangeClassDialog by remember { mutableStateOf(false) }
    var showSiblingsPopup by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Trigger Sibling popup every time when app is opened after 10 seconds until it submits!
    LaunchedEffect(isSiblingsFormSubmitted) {
        if (!isSiblingsFormSubmitted) {
            delay(10_000L)
            showSiblingsPopup = true
        }
    }

    // Tapping Home tab on bottom bar while inside any Home sub-screen resets to Home Feed
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            subRoute = HomeSubRoute.FEED
        }
    }

    // Ensure hardware Back button inside any Home feature returns to Home Feed instead of exiting app
    BackHandler(enabled = subRoute != HomeSubRoute.FEED) {
        subRoute = HomeSubRoute.FEED
    }

    if (showChangeClassDialog) {
        if (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN) {
            TeacherEditStudentDialog(
                currentFullName = activeStudent.fullName,
                currentClassName = activeStudent.className,
                currentSection = activeStudent.section,
                onDismiss = { showChangeClassDialog = false },
                onSaveConfirmed = { newName, newClass, newSec ->
                    showChangeClassDialog = false
                    DiscussionRepository.teacherUpdateStudentDetails(
                        context = context,
                        fullName = newName,
                        className = newClass,
                        section = newSec,
                        scope = coroutineScope
                    )
                    Toast.makeText(
                        context,
                        "Teacher updated student to $newName ($newClass – Section $newSec)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } else {
            StudentLockedProfileDialog(
                fullName = activeStudent.fullName,
                studentCode = activeStudent.studentCode,
                className = activeStudent.className,
                section = activeStudent.section,
                onDismiss = { showChangeClassDialog = false },
                onSwitchToTeacherRole = {
                    showChangeClassDialog = false
                    DiscussionRepository.logout(context)
                    Toast.makeText(
                        context,
                        "Signed out. Choose your role portal to sign in.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSelectNewYearClass = {
                    DiscussionRepository.requestUnlockForNewAcademicYearSelect(context)
                    Toast.makeText(
                        context,
                        "New Academic Year unlocked — select your promoted Class & Section",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    // Load Home notices ONCE per session; trigger feature APIs ONLY when the user clicks that specific feature
    LaunchedEffect(subRoute, selectedClass.id, activeStudent.id) {
        when (subRoute) {
            HomeSubRoute.FEED -> {
                if (DiscussionRepository.shouldLoadHomeFeedOnce(selectedClass.id)) {
                    DiscussionRepository.fetchNoticesFromBackend(coroutineScope)
                }
            }
            HomeSubRoute.REPORT_CARD -> {
                val targetClass = if (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN) selectedClass.id else activeStudent.classId
                DiscussionRepository.fetchClassExamsFromBackend(targetClass, coroutineScope)
                DiscussionRepository.fetchClassSubjectsFromBackend(targetClass, coroutineScope)
                DiscussionRepository.fetchStudentExamResultsFromBackend(activeStudent.id, coroutineScope)
            }
            HomeSubRoute.MARKS_AUTOMATION -> {
                DiscussionRepository.fetchClassSubjectsFromBackend(selectedClass.id, coroutineScope)
                DiscussionRepository.fetchClassExamsFromBackend(selectedClass.id, coroutineScope)
                DiscussionRepository.fetchClassRosterFromBackend(selectedClass.id, coroutineScope)
            }
            HomeSubRoute.ATTENDANCE -> {
                if (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN) {
                    DiscussionRepository.fetchClassRosterFromBackend(selectedClass.id, coroutineScope)
                } else {
                    DiscussionRepository.fetchStudentAttendanceFromBackend(activeStudent.id, coroutineScope)
                }
            }
            HomeSubRoute.ASSIGNMENTS, HomeSubRoute.MULTI_HOMEWORK -> {
                val targetClass = if (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN) selectedClass.id else activeStudent.classId
                DiscussionRepository.fetchHomeworkFromBackend(targetClass, coroutineScope)
            }
            HomeSubRoute.SECTION_VERIFICATION, HomeSubRoute.CLASS_YEAR_MANAGER -> {
                DiscussionRepository.fetchClassRosterFromBackend(selectedClass.id, coroutineScope)
            }
            HomeSubRoute.PROUD_PARENT_QR -> {
                DiscussionRepository.fetchReferralCodeFromBackend(coroutineScope)
            }
            HomeSubRoute.ADMIN_SIBLINGS -> {
                DiscussionRepository.fetchSiblingsFromBackend(coroutineScope)
            }
            HomeSubRoute.SCHOOL_INTELLIGENCE -> {
                DiscussionRepository.fetchSchoolIntelligenceFromBackend(coroutineScope)
            }
            else -> {}
        }
    }

    when (subRoute) {
        HomeSubRoute.ADD_STUDENT -> {
            AddStudentFlowScreen(
                onBack = { subRoute = HomeSubRoute.FEED },
                onStudentAdded = { admNo, name, cls, sec ->
                    DiscussionRepository.addLinkedStudent(admNo, name, cls, sec)
                    subRoute = HomeSubRoute.FEED
                }
            )
        }
        HomeSubRoute.NOTICE_DETAILS -> {
            val notice = selectedNotice ?: notices.firstOrNull()
            if (notice != null) {
                NoticeDetailsScreen(
                    notice = notice,
                    onBack = { subRoute = HomeSubRoute.FEED }
                )
            } else {
                subRoute = HomeSubRoute.FEED
            }
        }
        HomeSubRoute.PUBLISH_NOTICE -> {
            PublishNoticeScreen(
                onBack = { subRoute = HomeSubRoute.FEED },
                onPublish = { title, content, cat, imageUri, targetAudience, startsAtIso, expiresAtIso ->
                    DiscussionRepository.postAdminNotice(
                        title = title,
                        content = content,
                        category = cat,
                        author = if (currentRole == AppRole.ADMIN) "Principal's Office" else "Class Teacher",
                        imageUri = imageUri,
                        targetAudience = targetAudience,
                        startsAtIso = startsAtIso,
                        expiresAtIso = expiresAtIso,
                        scope = coroutineScope
                    )
                    val msg = if (!startsAtIso.isNullOrBlank()) "Notice scheduled successfully!" else "Notice published to school feed!"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    subRoute = HomeSubRoute.FEED
                }
            )
        }
        HomeSubRoute.ATTENDANCE -> {
            AttendanceHubScreen(
                student = activeStudent,
                currentRole = currentRole,
                selectedClass = selectedClass,
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.REPORT_CARD -> {
            ExamResultsReportCardScreen(
                student = activeStudent,
                onOpenProudParentQr = { subRoute = HomeSubRoute.PROUD_PARENT_QR },
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.DIGITAL_ID -> {
            DigitalStudentIdCardScreen(
                student = activeStudent,
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.ONLINE_QUIZ -> {
            PracticeTestsScreen(
                onBack = { subRoute = HomeSubRoute.FEED },
                onOpenChatGPT = onOpenChatGPT
            )
        }
        HomeSubRoute.ASSIGNMENTS -> {
            AssignmentsAndProjectsScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.EXAM_SCHEDULE -> {
            ExaminationScheduleScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.CALENDAR -> {
            AcademicCalendarScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.SECTION_VERIFICATION -> {
            SectionStudentVerificationScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.SCHOOL_INTELLIGENCE -> {
            SchoolIntelligenceAndLeakageScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.AUTOMATION_ENGINE -> {
            SchoolAutomationEngineScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.AI_ASSISTANT -> {
            AiHomeworkBuddyScreen(
                onOpenChatGPT = { prompt, uri -> onOpenChatGPT(prompt, uri) },
                onNavigateHome = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.MULTI_HOMEWORK -> {
            MultiSectionHomeworkScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.SUBSTITUTION_HUB -> {
            TimetableSubstitutionScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.ADMISSIONS_CRM -> {
            AdmissionsCrmScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.STUDENT_360 -> {
            Student360ProfileScreen(
                student = activeStudent,
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.STAFF_TRANSPORT -> {
            StaffAndTransportHubScreen(
                onBack = { subRoute = HomeSubRoute.FEED },
                onOpenStaffHr = { tab ->
                    staffHrInitialTab = tab
                    subRoute = HomeSubRoute.STAFF_HR
                }
            )
        }
        HomeSubRoute.STAFF_HR -> {
            StaffHrHubScreen(
                initialTab = staffHrInitialTab,
                onBack = { subRoute = HomeSubRoute.FEED },
                onOpenSubstitutionHub = { subRoute = HomeSubRoute.SUBSTITUTION_HUB }
            )
        }
        HomeSubRoute.FEE_STRUCTURE -> {
            FeeDetailsScreen(
                student = activeStudent,
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.MARKS_AUTOMATION -> {
            TeacherMarksAutomationScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.PROUD_PARENT_QR -> {
            ProudParentWhatsAppStatusAndQrScreen(
                student = activeStudent,
                onOpenAdmissionsCrm = { subRoute = HomeSubRoute.ADMISSIONS_CRM },
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.CLASS_YEAR_MANAGER -> {
            AcademicYearAndClassManagementScreen(
                onBack = { subRoute = HomeSubRoute.FEED }
            )
        }
        HomeSubRoute.MISTAKE_ANALYSIS -> {
            MistakeAnalysisScreen(
                onBack = { subRoute = HomeSubRoute.FEED },
                onOpenChatGPT = onOpenChatGPT
            )
        }
        HomeSubRoute.ADMIN_SIBLINGS -> {
            if (currentRole == AppRole.ADMIN) {
                AdminSiblingsHubScreen(
                    onBack = { subRoute = HomeSubRoute.FEED }
                )
            } else {
                subRoute = HomeSubRoute.FEED
            }
        }
        HomeSubRoute.FEED -> {
            if (showSiblingsPopup && !isSiblingsFormSubmitted && currentRole != AppRole.ADMIN && currentRole != AppRole.TEACHER) {
                SiblingsCaptureDialog(onDismiss = { showSiblingsPopup = false })
            }

            Scaffold(
                containerColor = GenesisColors.Background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                    if (currentRole == AppRole.ADMIN || currentRole == AppRole.TEACHER) {
                        ExtendedFloatingActionButton(
                            onClick = { subRoute = HomeSubRoute.PUBLISH_NOTICE },
                            containerColor = GenesisColors.Primary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(999.dp),
                            icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                            text = { Text("Publish Notice", color = Color.White, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    if (currentRole == AppRole.STUDENT) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE2F1FC),
                                Color(0xFFEBF3FC),
                                Color(0xFFEFF6FF),
                                Color(0xFFF8FAFC)
                            )
                        )
                    } else {
                        androidx.compose.ui.graphics.SolidColor(GenesisColors.Background)
                    }
                ),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // 1. Meteor Black School Header (Shown for Teacher / Admin / Parent; Student matches reference UI)
            // Top Branded School Header across ALL roles (Student, Parent, Teacher, Admin)
            item {
                val headerPillLabel = if (currentRole == AppRole.TEACHER) {
                    "Staff"
                } else {
                    "${activeStudent.className.removePrefix("Class ").trim()}-${activeStudent.section}"
                }
                val activeSchool by DiscussionRepository.activeSchool.collectAsState()
                SchoolHeader(
                    schoolName = activeSchool.name.uppercase(),
                    classSectionLabel = headerPillLabel,
                    onProfileClick = {
                        if (currentRole == AppRole.TEACHER) {
                            staffHrInitialTab = 0
                            subRoute = HomeSubRoute.STAFF_HR
                        } else {
                            showChangeClassDialog = true
                        }
                    },
                    onNotificationClick = {
                        selectedNotice = notices.firstOrNull()
                        if (selectedNotice != null) {
                            subRoute = HomeSubRoute.NOTICE_DETAILS
                        }
                    }
                )
            }

            // Production Role-Locked Session Bar (strictly isolated by authenticated role + Sign Out)
            item {
                val activeSchool by DiscussionRepository.activeSchool.collectAsState()
                val roleLabel = when (currentRole) {
                    AppRole.STUDENT -> "Student Portal"
                    AppRole.PARENT -> "Parent Portal"
                    AppRole.TEACHER -> "Teacher Portal"
                    AppRole.ADMIN -> "Admin / Principal Portal"
                }
                var showSignOutConfirm by remember { mutableStateOf(false) }
                if (showSignOutConfirm) {
                    AlertDialog(
                        onDismissRequest = { showSignOutConfirm = false },
                        title = {
                            Text(
                                "Sign Out",
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.TextPrimary
                            )
                        },
                        text = {
                            Text(
                                "Sign out of ${activeSchool.name} ($roleLabel)? You will need your school's secret code and password to sign back in.",
                                color = GenesisColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showSignOutConfirm = false
                                    DiscussionRepository.logout(context)
                                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                            ) {
                                Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSignOutConfirm = false }) {
                                Text("Cancel", color = GenesisColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Color(0xFFDBEAFE),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = roleLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.TextPrimary
                                )
                                Text(
                                    text = "${activeSchool.schoolCode.ifBlank { "SCHOOL" }} • Verified Access",
                                    fontSize = 10.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.clickable { showSignOutConfirm = true }
                        ) {
                            Text(
                                text = "Sign Out",
                                color = GenesisColors.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Principal / School Management Executive ROI Command Center
            if (currentRole == AppRole.ADMIN) {
                item {
                    PrincipalExecutiveCommandCard(
                        onOpenRollCall = { subRoute = HomeSubRoute.ATTENDANCE },
                        onOpenFeeReview = { subRoute = HomeSubRoute.FEE_STRUCTURE }
                    )
                }
            }

            // Role-Specific Smart School Operating System Dashboard (Management / Teacher / Parent / Student)
            item {
                RoleSpecificSmartDashboardSection(
                    currentRole = currentRole,
                    activeStudent = activeStudent,
                    selectedClass = selectedClass,
                    onOpenSectionVerification = { subRoute = HomeSubRoute.SECTION_VERIFICATION },
                    onOpenSchoolIntelligence = { subRoute = HomeSubRoute.SCHOOL_INTELLIGENCE },
                    onOpenAutomationEngine = { subRoute = HomeSubRoute.AUTOMATION_ENGINE },
                    onOpenAiAssistant = { subRoute = HomeSubRoute.AI_ASSISTANT },
                    onOpenMultiHomework = { subRoute = HomeSubRoute.MULTI_HOMEWORK },
                    onOpenSubstitutionHub = { subRoute = HomeSubRoute.SUBSTITUTION_HUB },
                    onOpenAdmissionsCrm = { subRoute = HomeSubRoute.ADMISSIONS_CRM },
                    onOpenStudent360 = { subRoute = HomeSubRoute.STUDENT_360 },
                    onOpenStaffTransport = { subRoute = HomeSubRoute.STAFF_TRANSPORT },
                    onOpenAttendance = { subRoute = HomeSubRoute.ATTENDANCE },
                    onOpenReportCard = { subRoute = HomeSubRoute.REPORT_CARD },
                    onOpenFeePayment = { subRoute = HomeSubRoute.FEE_STRUCTURE },
                    onOpenProudParentQr = { subRoute = HomeSubRoute.PROUD_PARENT_QR },
                    onOpenClassAndYearManager = { subRoute = HomeSubRoute.CLASS_YEAR_MANAGER },
                    onOpenStaffHr = { tab ->
                        staffHrInitialTab = tab
                        subRoute = HomeSubRoute.STAFF_HR
                    },
                    onOpenMarksAutomation = { subRoute = HomeSubRoute.MARKS_AUTOMATION },
                    onOpenCalendar = { subRoute = HomeSubRoute.CALENDAR }
                )
            }

            if (currentRole == AppRole.STUDENT) {
                // Student Profile Card matching reference image
                item {
                    ActiveProfileBanner(
                        student = activeStudent,
                        isTeacherOrAdmin = false,
                        onClick = { showChangeClassDialog = true }
                    )
                }

                // Same 3 Auto-Swiping School Hero Banners as Parent mode
                item {
                    SchoolHeroCarousel()
                }
            } else if (currentRole != AppRole.TEACHER) {
                // 2. Active-Student Banner (Shown only for Parent / Admin)
                item {
                    ActiveProfileBanner(
                        student = activeStudent,
                        isTeacherOrAdmin = currentRole == AppRole.ADMIN,
                        onClick = { showChangeClassDialog = true }
                    )
                }

                // 3. Horizontal Linked-Student Profile Selector (Parent mode)
                if (currentRole == AppRole.PARENT) {
                    item {
                        LinkedStudentSelectorRow(
                            students = linkedStudents,
                            activeStudent = activeStudent,
                            onSelectStudent = { student ->
                                DiscussionRepository.selectStudent(student, coroutineScope)
                                Toast.makeText(context, "Switched context to ${student.fullName}", Toast.LENGTH_SHORT).show()
                            },
                            onAddStudentClick = { subRoute = HomeSubRoute.ADD_STUDENT }
                        )
                    }
                } else if (currentRole == AppRole.ADMIN) {
                    item {
                        Surface(
                            color = Color(0xFFF3E8FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { showChangeClassDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Student Record: ${activeStudent.fullName} (${activeStudent.classTitle})",
                                    color = GenesisColors.ClassroomPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Edit Student",
                                    color = GenesisColors.ClassroomPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4. Auto-Rotating 3-Slide School Hero Carousel (Parent / Admin mode)
                item {
                    SchoolHeroCarousel()
                }
            }

            // 5. Role-Focused Feature Tiles (Admin tiles vs Teacher tiles vs Student/Parent tiles)
            item {
                when (currentRole) {
                    AppRole.ADMIN -> {
                        AdminShortcutTilesGrid(
                            onShortcutClick = { label ->
                                when (label) {
                                    "Sibling Leads" -> subRoute = HomeSubRoute.ADMIN_SIBLINGS
                                    "Student Control" -> subRoute = HomeSubRoute.SECTION_VERIFICATION
                                    "Staff HR" -> {
                                        staffHrInitialTab = 0
                                        subRoute = HomeSubRoute.STAFF_HR
                                    }
                                    "Roll Call" -> subRoute = HomeSubRoute.ATTENDANCE
                                    "Auto Marks" -> subRoute = HomeSubRoute.MARKS_AUTOMATION
                                    "Assignments" -> subRoute = HomeSubRoute.ASSIGNMENTS
                                    "Exam Schedule" -> subRoute = HomeSubRoute.EXAM_SCHEDULE
                                    "Fee Structure" -> subRoute = HomeSubRoute.FEE_STRUCTURE
                                    "Digital ID" -> subRoute = HomeSubRoute.DIGITAL_ID
                                    "Calendar" -> subRoute = HomeSubRoute.CALENDAR
                                    "AI Copilot" -> subRoute = HomeSubRoute.AI_ASSISTANT
                                }
                            }
                        )
                    }
                    AppRole.TEACHER -> {
                        TeacherShortcutTilesGrid(
                            onShortcutClick = { label ->
                                when (label) {
                                    "Roll Call" -> subRoute = HomeSubRoute.ATTENDANCE
                                    "Auto Marks" -> subRoute = HomeSubRoute.MARKS_AUTOMATION
                                    "Assignments" -> subRoute = HomeSubRoute.ASSIGNMENTS
                                    "Exam Schedule" -> subRoute = HomeSubRoute.EXAM_SCHEDULE
                                    "Student Control" -> subRoute = HomeSubRoute.SECTION_VERIFICATION
                                    "My Attendance" -> {
                                        staffHrInitialTab = 1
                                        subRoute = HomeSubRoute.STAFF_HR
                                    }
                                    "Staff Leave" -> {
                                        staffHrInitialTab = 2
                                        subRoute = HomeSubRoute.STAFF_HR
                                    }
                                    "Class Doubts" -> {
                                        NavigationBridge.pendingMyClassTab = 0
                                        onNavigateToTab(1)
                                    }
                                    "Upload Notes" -> {
                                        NavigationBridge.pendingMyClassTab = 1
                                        onNavigateToTab(1)
                                    }
                                    "Holidays" -> subRoute = HomeSubRoute.CALENDAR
                                }
                            }
                        )
                    }
                    else -> {
                        // STUDENT & PARENT: Pure learning, assessment, and academic tools (no admin leads)
                        ShortcutTilesGrid(
                            onShortcutClick = { label ->
                                when (label) {
                                    "Homework", "Assignments" -> subRoute = HomeSubRoute.ASSIGNMENTS
                                    "Timetable", "Exam Schedule" -> subRoute = HomeSubRoute.EXAM_SCHEDULE
                                    "Attendance" -> subRoute = HomeSubRoute.ATTENDANCE
                                    "Result", "Report Card" -> subRoute = HomeSubRoute.REPORT_CARD
                                    "Practice Tests", "Online Exams" -> subRoute = HomeSubRoute.ONLINE_QUIZ
                                    "Mistakes", "Mistake Analysis" -> subRoute = HomeSubRoute.MISTAKE_ANALYSIS
                                    "Fee Structure" -> subRoute = HomeSubRoute.FEE_STRUCTURE
                                    "Study Notes" -> {
                                        NavigationBridge.pendingMyClassTab = 1
                                        onNavigateToTab(1)
                                    }
                                    "Class Doubts" -> {
                                        NavigationBridge.pendingMyClassTab = 0
                                        onNavigateToTab(1)
                                    }
                                    "Digital ID" -> subRoute = HomeSubRoute.DIGITAL_ID
                                    "Calendar" -> subRoute = HomeSubRoute.CALENDAR
                                    "Ask AI Tutor" -> onNavigateToTab(2)
                                }
                            }
                        )
                    }
                }
            }

            // 6. Latest Notices Header (Clean — "View All" removed as requested)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latest Notices",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenesisColors.TextPrimary
                    )
                }
            }

            // Notices Feed (with illustrated empty state when no notices exist yet)
            if (notices.isEmpty()) {
                item {
                    EmptyStateIllustrationCard(
                        title = "All Caught Up on Notices",
                        subtitle = "School circulars, event announcements, and emergency alerts will appear right here.",
                        illustrationType = EmptyStateIllustrationType.NOTICES
                    )
                }
            } else {
                items(notices, key = { it.id }) { notice ->
                    NoticeFeedCard(
                        notice = notice,
                        onClick = {
                            selectedNotice = notice
                            subRoute = HomeSubRoute.NOTICE_DETAILS
                        }
                    )
                }
            }
        }
            }
        }
    }
}

private data class SchoolCarouselSlide(
    val imageRes: Int,
    val badge: String,
    val headline: String,
    val subtitle: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SchoolHeroCarousel() {
    val brand = LocalSchoolBrand.current
    val activeSchool by DiscussionRepository.activeSchool.collectAsState()
    val slides = remember(activeSchool.name, activeSchool.tagline) {
        listOf(
            SchoolCarouselSlide(
                imageRes = R.drawable.asset_reading_student,
                badge = activeSchool.schoolCode.ifBlank { "CAMPUS EXCELLENCE" },
                headline = "Building Brighter\nFutures Together",
                subtitle = activeSchool.tagline.ifBlank { "${activeSchool.name} • Smart Campus" }
            ),
            SchoolCarouselSlide(
                imageRes = R.drawable.asset_results,
                badge = "SMART DIGITAL CLASSROOMS",
                headline = "Interactive STEM &\nAI Learning Labs",
                subtitle = "100% Concept Clarity & Olympiad Readiness"
            ),
            SchoolCarouselSlide(
                imageRes = R.drawable.asset_myclass,
                badge = "CHAMPIONS ARENA",
                headline = "Attendance, Sports &\nAcademic Excellence",
                subtitle = "Nurturing Confidence Beyond Textbooks"
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Automatically swipe to the next school photo every 5 seconds; resets cleanly when user swipes manually
    LaunchedEffect(pagerState.settledPage) {
        delay(5000L)
        val nextPage = (pagerState.currentPage + 1) % slides.size
        pagerState.animateScrollToPage(nextPage)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(166.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val slide = slides[page]
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(slide.imageRes),
                        contentDescription = slide.headline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        brand.headerStart.copy(alpha = 0.92f),
                                        brand.headerStart.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                            .width(240.dp)
                    ) {
                        Surface(
                            color = GenesisColors.SkyAccent.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = slide.badge,
                                color = GenesisColors.SkyAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = slide.headline,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 23.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = slide.subtitle,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Live Carousel Indicator Dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.indices.forEach { idx ->
                    val selected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (selected) 18.dp else 6.dp)
                            .background(
                                if (selected) GenesisColors.SkyAccent else Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        }
    }
}

private data class ShortcutTileData(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val bgTint: Color,
    val badgeText: String? = null,
    val iconRes: Int? = null
)

@Composable
private fun AdminShortcutTilesGrid(
    onShortcutClick: (String) -> Unit
) {
    // Clean, consistent palette matching Teacher UI: Primary blue (#2563EB / #DBEAFE) & Teal (#0F766E / #CCFBF1)
    val tiles = listOf(
        ShortcutTileData("Sibling Leads", Icons.Rounded.FamilyRestroom, GenesisColors.Primary, GenesisColors.LightBlue, badgeText = "Leads"),
        ShortcutTileData("Student Control", Icons.Rounded.VerifiedUser, GenesisColors.Primary, GenesisColors.LightBlue),
        ShortcutTileData("Staff HR", Icons.Rounded.Badge, GenesisColors.Teal, Color(0xFFCCFBF1)),
        ShortcutTileData("Roll Call", Icons.Rounded.HowToReg, GenesisColors.Primary, GenesisColors.LightBlue, badgeText = "<60s"),
        ShortcutTileData("Auto Marks", Icons.Rounded.Grade, GenesisColors.Primary, GenesisColors.LightBlue, badgeText = "Auto"),
        ShortcutTileData("Assignments", Icons.Rounded.Assignment, GenesisColors.Teal, Color(0xFFCCFBF1)),
        ShortcutTileData("Exam Schedule", Icons.Rounded.CalendarMonth, GenesisColors.Teal, Color(0xFFCCFBF1)),
        ShortcutTileData("Fee Structure", Icons.Rounded.CurrencyRupee, GenesisColors.Primary, GenesisColors.LightBlue),
        ShortcutTileData("Digital ID", Icons.Rounded.Badge, GenesisColors.Teal, Color(0xFFCCFBF1)),
        ShortcutTileData("Calendar", Icons.Rounded.EventNote, GenesisColors.Teal, Color(0xFFCCFBF1)),
        ShortcutTileData("AI Copilot", Icons.Rounded.AutoAwesome, GenesisColors.Teal, Color(0xFFCCFBF1))
    )
    ShortcutTileGridContent(tiles = tiles, onShortcutClick = onShortcutClick, cleanSimpleStyle = true)
}

@Composable
private fun TeacherShortcutTilesGrid(
    onShortcutClick: (String) -> Unit
) {
    // 10 cleanly extracted 3D teacher icons with transparent backgrounds on crisp white cards
    val tiles = listOf(
        ShortcutTileData("Roll Call", Icons.Rounded.HowToReg, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_teacher_roll_call),
        ShortcutTileData("Auto Marks", Icons.Rounded.Grade, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_teacher_auto_marks),
        ShortcutTileData("Assignments", Icons.Rounded.Assignment, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_teacher_assignments),
        ShortcutTileData("Exam Schedule", Icons.Rounded.CalendarMonth, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_teacher_exam_schedule),
        ShortcutTileData("Student Control", Icons.Rounded.VerifiedUser, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_teacher_student_control),
        ShortcutTileData("My Attendance", Icons.Rounded.Fingerprint, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_teacher_my_attendance),
        ShortcutTileData("Staff Leave", Icons.Rounded.EventBusy, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_teacher_staff_leave),
        ShortcutTileData("Class Doubts", Icons.Rounded.Forum, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_teacher_class_doubts),
        ShortcutTileData("Upload Notes", Icons.Rounded.Description, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_teacher_upload_notes),
        ShortcutTileData("Holidays", Icons.Rounded.EventNote, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_teacher_holidays)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                rowItems.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(102.dp)
                            .clickable { onShortcutClick(item.label) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(item.iconRes!!),
                                contentDescription = item.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                } else if (rowItems.size == 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShortcutTileGridContent(
    tiles: List<ShortcutTileData>,
    onShortcutClick: (String) -> Unit,
    cleanSimpleStyle: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (cleanSimpleStyle) 1.dp else 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clickable { onShortcutClick(item.label) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!cleanSimpleStyle) {
                                CardCornerVectorAccent(
                                    tint = item.iconTint,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(item.bgTint, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!cleanSimpleStyle && item.iconRes != null) {
                                            Image(
                                                painter = painterResource(item.iconRes),
                                                contentDescription = item.label,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Icon(item.icon, contentDescription = item.label, tint = item.iconTint, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GenesisColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (item.badgeText != null) {
                                Surface(
                                    color = if (item.badgeText == "Due") GenesisColors.Emergency else GenesisColors.Primary,
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = item.badgeText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutTilesGrid(
    onShortcutClick: (String) -> Unit
) {
    // 12 cleanly extracted 3D student icons with transparent backgrounds on crisp white cards
    val tiles = listOf(
        ShortcutTileData("Homework", Icons.Rounded.Assignment, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_student_homework),
        ShortcutTileData("Timetable", Icons.Rounded.CalendarViewWeek, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_student_timetable),
        ShortcutTileData("Attendance", Icons.Rounded.HowToReg, GenesisColors.Success, Color.Transparent, iconRes = R.drawable.ic_student_attendance),
        ShortcutTileData("Result", Icons.Rounded.EmojiEvents, GenesisColors.Warning, Color.Transparent, iconRes = R.drawable.ic_student_result),
        ShortcutTileData("Practice Tests", Icons.Rounded.Quiz, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_student_practice_tests),
        ShortcutTileData("Mistakes", Icons.Rounded.FindInPage, GenesisColors.Emergency, Color.Transparent, iconRes = R.drawable.ic_student_mistakes),
        ShortcutTileData("Fee Structure", Icons.Rounded.AccountBalanceWallet, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_student_fee_structure),
        ShortcutTileData("Study Notes", Icons.Rounded.MenuBook, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_student_study_notes),
        ShortcutTileData("Class Doubts", Icons.Rounded.Forum, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_student_class_doubts),
        ShortcutTileData("Digital ID", Icons.Rounded.Badge, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_student_digital_id),
        ShortcutTileData("Calendar", Icons.Rounded.CalendarMonth, GenesisColors.Teal, Color.Transparent, iconRes = R.drawable.ic_student_calendar),
        ShortcutTileData("Ask AI Tutor", Icons.Rounded.AutoAwesome, GenesisColors.Primary, Color.Transparent, iconRes = R.drawable.ic_student_ask_ai)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(102.dp)
                            .clickable { onShortcutClick(item.label) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(item.iconRes!!),
                                contentDescription = item.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeFeedCard(
    notice: AdminNotice,
    onClick: () -> Unit
) {
    val brand = LocalSchoolBrand.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notice.imageUrl != null) {
                AsyncImage(
                    model = notice.imageUrl,
                    contentDescription = notice.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                val (catIcon, catBg, catTint) = when (notice.category.uppercase()) {
                    "EVENT" -> Triple(Icons.Rounded.EmojiEvents, Color(0xFFDCFCE7), GenesisColors.Success)
                    "ACADEMIC" -> Triple(Icons.Rounded.MenuBook, Color(0xFFCCFBF1), GenesisColors.Teal)
                    "EMERGENCY" -> Triple(Icons.Rounded.Warning, Color(0xFFFEE2E2), GenesisColors.Emergency)
                    else -> Triple(Icons.Rounded.Campaign, GenesisColors.LightBlue, GenesisColors.Primary)
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(catBg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = notice.category,
                        tint = catTint,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(notice.category)
                    Text(notice.date, fontSize = 11.sp, color = GenesisColors.TextSecondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notice.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenesisColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notice.content,
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// 05 MY CLASS — DOUBTS & NOTES (WITH CLASS-WISE SUBJECT MANAGER)
// ============================================================================

private enum class MyClassSubRoute {
    MAIN, ASK_DOUBT, UPLOAD_NOTES, VERIFY_SECTION_STUDENTS, CLASS_YEAR_MANAGER
}

@Composable
fun MyClassScreen(
    onOpenChatGPT: (String?, Uri?) -> Unit = { _, _ -> },
    onNavigateHome: () -> Unit = {},
    resetSignal: Int = 0
) {
    var selectedSectionTab by remember {
        val pending = NavigationBridge.pendingMyClassTab
        NavigationBridge.pendingMyClassTab = null
        mutableIntStateOf((pending ?: 0).coerceIn(0, 1))
    } // 0: Doubts, 1: Notes
    var selectedSubjectFilter by remember { mutableStateOf("All") }
    var subRoute by remember { mutableStateOf(MyClassSubRoute.MAIN) }
    var showClassSwitcherMenu by remember { mutableStateOf(false) }
    var showManageSubjectsDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            subRoute = MyClassSubRoute.MAIN
        }
    }

    // Pressing Back inside Classes & Promote or My Class routes cleanly to Home / parent view
    BackHandler {
        if (subRoute == MyClassSubRoute.CLASS_YEAR_MANAGER) {
            subRoute = MyClassSubRoute.MAIN
            onNavigateHome()
        } else if (subRoute != MyClassSubRoute.MAIN) {
            subRoute = MyClassSubRoute.MAIN
        } else {
            onNavigateHome()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val activeStudent by DiscussionRepository.activeStudent.collectAsState()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val availableClasses by DiscussionRepository.availableClassesFlow.collectAsState()
    val classSubjectsMap by DiscussionRepository.classSubjectsMap.collectAsState()
    val discussions by DiscussionRepository.discussions.collectAsState()
    val notes by DiscussionRepository.studyNotes.collectAsState()
    val context = LocalContext.current

    val canSwitchClasses = currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN
    val activeClassSubjects = classSubjectsMap[selectedClass.id]
        ?: DiscussionRepository.getSubjectsForClass(selectedClass.id)
    val filterSubjects = remember(activeClassSubjects) { listOf("All") + activeClassSubjects }

    if (showManageSubjectsDialog) {
        Dialog(onDismissRequest = { showManageSubjectsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Manage Class Subjects",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedClass.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GenesisColors.Primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubjectInput,
                            onValueChange = { newSubjectInput = it },
                            label = { Text("Add Subject (e.g. Sanskrit, AI)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = newSubjectInput.trim()
                                if (clean.isNotBlank()) {
                                    val added = DiscussionRepository.addSubjectToClass(selectedClass.id, clean)
                                    if (added) {
                                        newSubjectInput = ""
                                        Toast.makeText(context, "Added $clean to ${selectedClass.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "$clean is already in this class", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Subjects in ${selectedClass.name} (${activeClassSubjects.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    activeClassSubjects.forEach { subjectName ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subjectName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                IconButton(
                                    onClick = {
                                        if (activeClassSubjects.size > 1) {
                                            DiscussionRepository.removeSubjectFromClass(selectedClass.id, subjectName)
                                            if (selectedSubjectFilter.equals(subjectName, ignoreCase = true)) {
                                                selectedSubjectFilter = "All"
                                            }
                                            Toast.makeText(context, "Removed $subjectName from ${selectedClass.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "At least 1 subject is required", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Subject",
                                        tint = GenesisColors.Emergency,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showManageSubjectsDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val classScopedDiscussions = remember(discussions, selectedClass.id, selectedSubjectFilter) {
        discussions.filter {
            it.classId == selectedClass.id &&
                (selectedSubjectFilter == "All" || it.subject.equals(selectedSubjectFilter, ignoreCase = true))
        }
    }

    val classScopedNotes = remember(notes, selectedClass.id, selectedSubjectFilter) {
        notes.filter {
            it.classId == selectedClass.id &&
                (selectedSubjectFilter == "All" || it.subject.equals(selectedSubjectFilter, ignoreCase = true))
        }
    }

    // Load Notes & Doubts strictly for the logged-in student's class & section when My Class opens
    LaunchedEffect(selectedClass.id) {
        val targetClass = if (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN) selectedClass.id else activeStudent.classId
        DiscussionRepository.fetchDiscussionsFromBackend(targetClass, coroutineScope)
        DiscussionRepository.fetchNotesFromBackend(targetClass, coroutineScope)
        DiscussionRepository.fetchClassSubjectsFromBackend(targetClass, coroutineScope)
    }

    when (subRoute) {
        MyClassSubRoute.ASK_DOUBT -> {
            AskDoubtScreen(
                availableSubjects = activeClassSubjects,
                onBack = { subRoute = MyClassSubRoute.MAIN },
                onPostToClass = { subj, title, details, hasImg, imgUri ->
                    DiscussionRepository.postDoubt(
                        subject = subj,
                        title = title,
                        content = details,
                        hasImage = hasImg,
                        imageUri = imgUri,
                        authorName = activeStudent.fullName,
                        authorRole = currentRole.name,
                        classId = selectedClass.id,
                        scope = coroutineScope
                    )
                    Toast.makeText(context, "Doubt posted to ${selectedClass.name}!", Toast.LENGTH_SHORT).show()
                    subRoute = MyClassSubRoute.MAIN
                },
                onAskAI = { prompt, imageUri ->
                    subRoute = MyClassSubRoute.MAIN
                    onOpenChatGPT(prompt, imageUri)
                }
            )
        }
        MyClassSubRoute.UPLOAD_NOTES -> {
            UploadStudyMaterialScreen(
                availableSubjects = activeClassSubjects,
                onBack = { subRoute = MyClassSubRoute.MAIN },
                onUpload = { subj, chapter, title, localPdfUri, pageCount ->
                    DiscussionRepository.postStudyNote(
                        classId = selectedClass.id,
                        subject = subj,
                        title = title,
                        chapter = chapter,
                        teacherName = "Mrs. Gupta",
                        localPdfUri = localPdfUri,
                        pageCount = pageCount,
                        scope = coroutineScope
                    )
                    Toast.makeText(context, "Study material uploaded to ${selectedClass.name}", Toast.LENGTH_SHORT).show()
                    subRoute = MyClassSubRoute.MAIN
                }
            )
        }
        MyClassSubRoute.VERIFY_SECTION_STUDENTS -> {
            SectionStudentVerificationScreen(
                onBack = { subRoute = MyClassSubRoute.MAIN }
            )
        }
        MyClassSubRoute.CLASS_YEAR_MANAGER -> {
            AcademicYearAndClassManagementScreen(
                onBack = {
                    subRoute = MyClassSubRoute.MAIN
                    onNavigateHome()
                }
            )
        }
        MyClassSubRoute.MAIN -> {
            Scaffold(
                containerColor = GenesisColors.Background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                    if (selectedSectionTab == 0) {
                        ExtendedFloatingActionButton(
                            onClick = { subRoute = MyClassSubRoute.ASK_DOUBT },
                            containerColor = GenesisColors.Primary,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(999.dp),
                            icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                            text = { Text("Ask Doubt", color = Color.White, fontWeight = FontWeight.Bold) }
                        )
                    } else if (selectedSectionTab == 1 && (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN)) {
                        ExtendedFloatingActionButton(
                            onClick = { subRoute = MyClassSubRoute.UPLOAD_NOTES },
                            containerColor = GenesisColors.ClassroomPurple,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(999.dp),
                            icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                            text = { Text("Upload Notes", color = Color.White, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Class Header Bar (Locked for Student/Parent, Switchable Dropdown for Teacher/Admin)
                    val brand = LocalSchoolBrand.current
                    Surface(
                        color = Color.Transparent,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(brand.headerStart, brand.headerEnd)
                                    )
                                )
                        ) {
                            Image(
                                painter = painterResource(R.drawable.asset_myclass),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0.16f)
                            )
                            HeaderVectorBackdrop(
                                accentColor = Color.White,
                                modifier = Modifier.matchParentSize()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = canSwitchClasses) { showClassSwitcherMenu = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = selectedClass.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (canSwitchClasses) "Tap to switch class" else "Assigned Classroom",
                                            fontSize = 11.sp,
                                            color = brand.skyAccent
                                        )
                                    }
                                }
                                if (canSwitchClasses) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Switch Class", tint = Color.White)
                                } else {
                                    Icon(Icons.Rounded.Lock, contentDescription = "Locked Class", tint = brand.skyAccent, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showClassSwitcherMenu,
                                onDismissRequest = { showClassSwitcherMenu = false }
                            ) {
                                availableClasses.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text("${cls.name} (${cls.studentCount} Students)") },
                                        onClick = {
                                            DiscussionRepository.switchClass(cls, coroutineScope)
                                            showClassSwitcherMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = GenesisColors.Primary)
                                    },
                                    text = {
                                        Text(
                                            text = "➕ Create Class or Section",
                                            color = GenesisColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        showClassSwitcherMenu = false
                                        subRoute = MyClassSubRoute.CLASS_YEAR_MANAGER
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GenesisColors.Primary)
                                    },
                                    text = {
                                        Text(
                                            text = "📚 Manage Subjects for ${selectedClass.name}",
                                            color = GenesisColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        showClassSwitcherMenu = false
                                        showManageSubjectsDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Teacher Quick Action Bar (Verify Students | Subjects | Classes & Promote)
                    if (canSwitchClasses) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { subRoute = MyClassSubRoute.VERIFY_SECTION_STUDENTS },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.ClassroomPurple),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Verify Students", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showManageSubjectsDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Subjects", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { subRoute = MyClassSubRoute.CLASS_YEAR_MANAGER },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = GenesisColors.SkyAccent, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Classes & Promote", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Internal Section Selector: Doubts | Notes
                    TabRow(
                        selectedTabIndex = selectedSectionTab,
                        containerColor = Color.White,
                        contentColor = GenesisColors.Primary
                    ) {
                        listOf("Doubts", "Notes").forEachIndexed { idx, title ->
                            Tab(
                                selected = selectedSectionTab == idx,
                                onClick = { selectedSectionTab = idx },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedSectionTab == idx) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedSectionTab == idx) GenesisColors.Primary else GenesisColors.TextSecondary
                                    )
                                }
                            )
                        }
                    }

                    when (selectedSectionTab) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 88.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        filterSubjects.forEach { subj ->
                                            SubjectChip(
                                                label = subj,
                                                selected = selectedSubjectFilter == subj,
                                                onClick = { selectedSubjectFilter = subj }
                                            )
                                        }
                                    }
                                }

                                if (classScopedDiscussions.isEmpty()) {
                                    item {
                                        EmptyStateIllustrationCard(
                                            title = "No Class Doubts Yet",
                                            subtitle = "Have a question on homework or a concept? Post it here for your teachers & classmates to solve together.",
                                            illustrationType = EmptyStateIllustrationType.DOUBTS,
                                            actionLabel = "Ask First Doubt",
                                            onAction = { subRoute = MyClassSubRoute.ASK_DOUBT }
                                        )
                                    }
                                }

                                items(classScopedDiscussions, key = { it.id }) { post ->
                                    ClassroomDoubtCard(
                                        doubt = post,
                                        activeStudent = activeStudent,
                                        currentRole = currentRole,
                                        onAskAiClick = {
                                            val prompt = "Act as an expert CBSE ${post.subject} teacher. Explain simply and directly without lengthy confusing text:\n\"${post.title}\" — ${post.content}\n\nRules:\n1. Core concept & formula in 1-2 lines.\n2. Step-by-step solution (simple and to the point).\n3. End with 1 quick real-world example to make it easy to understand."
                                            val parsedUri = post.imageUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                                            onOpenChatGPT(prompt, parsedUri)
                                        },
                                        onEditDoubt = { newSubj, newTitle, newContent ->
                                            DiscussionRepository.editDoubt(
                                                discussionId = post.id,
                                                newSubject = newSubj,
                                                newTitle = newTitle,
                                                newContent = newContent,
                                                scope = coroutineScope
                                            )
                                            Toast.makeText(context, "Doubt updated!", Toast.LENGTH_SHORT).show()
                                        },
                                        onDeleteDoubt = {
                                            DiscussionRepository.deleteDoubt(post.id, coroutineScope)
                                            Toast.makeText(context, "Doubt deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        onReplySubmit = { replyText, replyImageUri ->
                                            DiscussionRepository.postReply(
                                                discussionId = post.id,
                                                content = replyText,
                                                imageUri = replyImageUri,
                                                authorName = if (currentRole == AppRole.TEACHER) "Pooja Ma'am" else activeStudent.fullName,
                                                authorRole = currentRole.name,
                                                scope = coroutineScope
                                            )
                                        },
                                        onEditReply = { replyId, updatedContent ->
                                            DiscussionRepository.editReply(
                                                discussionId = post.id,
                                                replyId = replyId,
                                                newContent = updatedContent,
                                                scope = coroutineScope
                                            )
                                            Toast.makeText(context, "Comment updated!", Toast.LENGTH_SHORT).show()
                                        },
                                        onDeleteReply = { replyId ->
                                            DiscussionRepository.deleteReply(
                                                discussionId = post.id,
                                                replyId = replyId,
                                                scope = coroutineScope
                                            )
                                            Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 88.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        filterSubjects.forEach { subj ->
                                            SubjectChip(
                                                label = subj,
                                                selected = selectedSubjectFilter == subj,
                                                onClick = { selectedSubjectFilter = subj }
                                            )
                                        }
                                    }
                                }

                                if (classScopedNotes.isEmpty()) {
                                    item {
                                        EmptyStateIllustrationCard(
                                            title = "No Study Notes Uploaded Yet",
                                            subtitle = "Chapter PDFs, revision sheets, and teacher lecture slides for ${selectedClass.name} will appear here.",
                                            illustrationType = EmptyStateIllustrationType.NOTES,
                                            actionLabel = if (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN) "Upload Study Notes" else null,
                                            onAction = if (currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN) {
                                                { subRoute = MyClassSubRoute.UPLOAD_NOTES }
                                            } else null
                                        )
                                    }
                                }

                                items(classScopedNotes, key = { it.id }) { note ->
                                    PdfNoteItemCard(
                                        note = note,
                                        onDownload = {
                                            val saved = downloadAndOpenClassNotePdf(
                                                context = context,
                                                note = note,
                                                classNameLabel = selectedClass.name,
                                                autoOpen = true
                                            )
                                            if (saved != null && !note.isDownloaded) {
                                                DiscussionRepository.toggleNoteDownloaded(note.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun copyUriToCacheFile(context: Context, sourceUri: Uri): Uri? {
    return try {
        val file = java.io.File(context.cacheDir, "doubt_img_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            java.io.FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: Exception) {
        sourceUri
    }
}

@Composable
private fun FullScreenImageDialog(
    imageModel: Any,
    title: String = "Attached Image",
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = imageModel,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 420.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                )
            }
        }
    }
}

@Composable
private fun ClassroomDoubtCard(
    doubt: DiscussionPost,
    activeStudent: StudentProfile,
    currentRole: AppRole,
    onAskAiClick: () -> Unit,
    onEditDoubt: (newSubject: String, newTitle: String, newContent: String) -> Unit,
    onDeleteDoubt: () -> Unit,
    onReplySubmit: (replyText: String, replyImageUri: String?) -> Unit,
    onEditReply: (replyId: String, newContent: String) -> Unit,
    onDeleteReply: (replyId: String) -> Unit
) {
    var expanded by remember { mutableStateOf(doubt.id == "disc-1") }
    var replyText by remember { mutableStateOf("") }
    var replyImageUri by remember { mutableStateOf<Uri?>(null) }
    var replyCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewModalUri by remember { mutableStateOf<String?>(null) }
    var showEditDoubtDialog by remember { mutableStateOf(false) }
    var showDeleteDoubtConfirm by remember { mutableStateOf(false) }
    var replyBeingEdited by remember { mutableStateOf<DiscussionReply?>(null) }
    val context = LocalContext.current

    // Students can edit/delete their doubts (and student doubts in demo) + Teachers/Admins can moderate all
    val canModifyDoubt = doubt.authorRole != "TEACHER" ||
        doubt.authorName.equals(activeStudent.fullName, ignoreCase = true) ||
        doubt.authorName.equals(activeStudent.firstName, ignoreCase = true) ||
        currentRole == AppRole.TEACHER ||
        currentRole == AppRole.ADMIN

    val replyGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cached = copyUriToCacheFile(context, uri) ?: uri
            replyImageUri = cached
            replyCapturedBitmap = null
            Toast.makeText(context, "Solution image selected from Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    val replyCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            replyCapturedBitmap = bitmap
            replyImageUri = saveBitmapToCache(context, bitmap)
            Toast.makeText(context, "Solution photo captured!", Toast.LENGTH_SHORT).show()
        }
    }

    if (previewModalUri != null) {
        FullScreenImageDialog(
            imageModel = Uri.parse(previewModalUri),
            title = "Doubt / Solution Attachment",
            onDismiss = { previewModalUri = null }
        )
    }

    if (showEditDoubtDialog) {
        var editSubj by remember(doubt.id) { mutableStateOf(doubt.subject) }
        var editTitle by remember(doubt.id) { mutableStateOf(doubt.title) }
        var editContent by remember(doubt.id) { mutableStateOf(doubt.content) }

        Dialog(onDismissRequest = { showEditDoubtDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text("Edit Posted Doubt", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiscussionRepository.allSchoolSubjects.forEach { s ->
                            SubjectChip(label = s, selected = editSubj == s, onClick = { editSubj = s })
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Question Title", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        trailingIcon = {
                            VoiceMicIconButton(
                                promptHint = "Speak updated title...",
                                onResultText = { spoken -> editTitle = spoken.removeSuffix("?") }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Question Details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        VoiceCaptureChipButton(
                            label = "Voice Edit",
                            onResultText = { spoken -> editContent = mergeVoiceText(editContent, spoken) }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showEditDoubtDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                if (editTitle.isNotBlank() && editContent.isNotBlank()) {
                                    onEditDoubt(editSubj, editTitle, editContent)
                                    showEditDoubtDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDoubtConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteDoubtConfirm = false },
            title = { Text("Delete Doubt?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently remove this doubt from your classroom discussion?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDoubtConfirm = false
                        onDeleteDoubt()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Emergency)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDoubtConfirm = false }) {
                    Text("Cancel", color = GenesisColors.TextPrimary)
                }
            }
        )
    }

    if (replyBeingEdited != null) {
        val targetReply = replyBeingEdited!!
        var updatedReplyText by remember(targetReply.id) { mutableStateOf(targetReply.content) }

        Dialog(onDismissRequest = { replyBeingEdited = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit Comment / Reply", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        VoiceCaptureChipButton(
                            label = "Voice",
                            onResultText = { spoken -> updatedReplyText = mergeVoiceText(updatedReplyText, spoken) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = updatedReplyText,
                        onValueChange = { updatedReplyText = it },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { replyBeingEdited = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                if (updatedReplyText.isNotBlank()) {
                                    onEditReply(targetReply.id, updatedReplyText)
                                    replyBeingEdited = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author Header + Subject Pill + Edit/Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFDBEAFE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = doubt.authorName.take(1).uppercase(),
                        color = GenesisColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(doubt.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                    Text(doubt.timeAgo, fontSize = 11.sp, color = GenesisColors.TextSecondary)
                }
                Surface(
                    color = Color(0xFFEDE9FE),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = doubt.subject,
                        color = GenesisColors.ClassroomPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (canModifyDoubt) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showEditDoubtDialog = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Doubt",
                            tint = GenesisColors.Primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDoubtConfirm = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Doubt",
                            tint = GenesisColors.Emergency,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = doubt.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GenesisColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = doubt.content,
                fontSize = 13.sp,
                color = GenesisColors.TextSecondary,
                lineHeight = 19.sp
            )

            // Uploaded Doubt Image Preview (from Camera or Gallery)
            if (!doubt.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { previewModalUri = doubt.imageUri }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        AsyncImage(
                            model = Uri.parse(doubt.imageUri),
                            contentDescription = "Question Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "📷 Attached Doubt Photo (Tap to enlarge)",
                                fontSize = 11.sp,
                                color = GenesisColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom",
                                tint = GenesisColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else if (doubt.hasEquationCard) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = doubt.equationFormula,
                            fontSize = 19.sp,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = doubt.equationSubtext,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // High-Contrast Visible Action Buttons: Ask AI Tutor | Responses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAskAiClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.2.dp, GenesisColors.AiGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFECFDF5),
                        contentColor = GenesisColors.AiGreen
                    ),
                    modifier = Modifier.weight(1f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GenesisColors.AiGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Ask AI Tutor",
                        color = GenesisColors.AiGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.2.dp, GenesisColors.Primary.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF8FAFC),
                        contentColor = GenesisColors.TextPrimary
                    ),
                    modifier = Modifier.weight(1f).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (doubt.replies.isNotEmpty()) "Comments (${doubt.replies.size})" else "Comment",
                        color = GenesisColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = GenesisColors.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Doubt Thread
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = GenesisColors.Border)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    doubt.replies.forEach { reply ->
                        val canModifyReply = reply.authorRole != "TEACHER" ||
                            currentRole == AppRole.TEACHER ||
                            currentRole == AppRole.ADMIN
                        DoubtReplyBubble(
                            reply = reply,
                            canEditOrDelete = canModifyReply,
                            onEditClick = { replyBeingEdited = reply },
                            onDeleteClick = { onDeleteReply(reply.id) },
                            onImageClick = { imgUri -> previewModalUri = imgUri }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selected Solution Image Preview (before sending reply)
                if (replyCapturedBitmap != null || replyImageUri != null) {
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (replyCapturedBitmap != null) {
                                Image(
                                    bitmap = replyCapturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Solution Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else if (replyImageUri != null) {
                                AsyncImage(
                                    model = replyImageUri,
                                    contentDescription = "Solution Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Solution Image Attached",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.Success
                                )
                                Text(
                                    text = "Ready to post with your reply",
                                    fontSize = 11.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    replyImageUri = null
                                    replyCapturedBitmap = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Solution Image",
                                    tint = GenesisColors.Emergency
                                )
                            }
                        }
                    }
                }

                // Write a reply bar with Voice Capture + Camera + Gallery options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = {
                            Text(
                                if (currentRole == AppRole.TEACHER) "Write or speak answer..." else "Write or speak reply...",
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 2.dp)
                            ) {
                                VoiceMicIconButton(
                                    promptHint = "Speak your answer or explanation...",
                                    onResultText = { spoken ->
                                        replyText = mergeVoiceText(replyText, spoken)
                                    }
                                )
                                IconButton(
                                    onClick = { replyCameraLauncher.launch() },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Capture Solution Photo",
                                        tint = GenesisColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { replyGalleryLauncher.launch("image/*") },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Upload Solution from Gallery",
                                        tint = GenesisColors.ClassroomPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val canSendReply = replyText.isNotBlank() || replyImageUri != null || replyCapturedBitmap != null
                    IconButton(
                        onClick = {
                            if (canSendReply) {
                                val finalUriStr = replyImageUri?.toString()
                                    ?: replyCapturedBitmap?.let { saveBitmapToCache(context, it)?.toString() }
                                onReplySubmit(replyText, finalUriStr)
                                replyText = ""
                                replyImageUri = null
                                replyCapturedBitmap = null
                            }
                        },
                        enabled = canSendReply,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (canSendReply) GenesisColors.Primary else Color(0xFFCBD5E1),
                                CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Reply", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DoubtReplyBubble(
    reply: DiscussionReply,
    canEditOrDelete: Boolean = false,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val isTeacher = reply.authorRole == "TEACHER" || reply.isVerified
    Surface(
        color = if (isTeacher) Color(0xFFF5F3FF) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isTeacher) Color(0xFFDDD6FE) else GenesisColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(if (isTeacher) GenesisColors.ClassroomPurple else Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(reply.authorName.take(1).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(reply.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GenesisColors.TextPrimary)
                        if (isTeacher) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = GenesisColors.ClassroomPurple,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "Teacher",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (reply.isVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = GenesisColors.Warning, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Teacher Verified Answer",
                                color = Color(0xFFB45309),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (canEditOrDelete) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Comment",
                            tint = GenesisColors.Primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Comment",
                            tint = GenesisColors.Emergency,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reply.content,
                fontSize = 13.sp,
                color = GenesisColors.TextPrimary,
                lineHeight = 19.sp
            )

            if (!reply.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onImageClick(reply.imageUri) }
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        AsyncImage(
                            model = Uri.parse(reply.imageUri),
                            contentDescription = "Uploaded Solution Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "📷 Attached Solution Photo (Tap to view)",
                                fontSize = 11.sp,
                                color = GenesisColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "View Full Solution",
                                tint = GenesisColors.Primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (reply.likesCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, tint = GenesisColors.Warning, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(reply.likesCount.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PdfNoteItemCard(
    note: ClassNote,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    val isDummySample = note.title.contains("Sample_Dummy", ignoreCase = true)
    var isListening by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDummySample) Color(0xFFF0F9FF) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDummySample) GenesisColors.Primary else GenesisColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onDownload() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = GenesisColors.Emergency, modifier = Modifier.size(20.dp))
                        Text("PDF", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Emergency)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (isDummySample) {
                        Surface(
                            color = GenesisColors.Primary,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "TEST PDF DOWNLOAD",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(note.chapter, fontSize = 12.sp, color = GenesisColors.TextSecondary)
                    Text("By ${note.teacherName} • ${note.date}", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips (Inspired by Image 1 Book Reader Screen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "📄 ${note.pageCount} Pages",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⏱️ ~${(note.pageCount * 2).coerceAtLeast(10)} Mins",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "🌐 English",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: [READ] and [LISTEN] (Image 1 Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDownload,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (note.isDownloaded) GenesisColors.Success else GenesisColors.Primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = if (note.isDownloaded) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                        contentDescription = "Read",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (note.isDownloaded) "Open PDF" else "READ NOTES",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        isListening = !isListening
                        Toast.makeText(
                            context,
                            if (isListening) "Playing audio summary for ${note.title}..." else "Audio summary stopped",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isListening) GenesisColors.Success else Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isListening) Color(0xFFECFDF5) else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Headphones,
                        contentDescription = "Listen",
                        tint = if (isListening) GenesisColors.Success else GenesisColors.TextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "LISTENING" else "LISTEN",
                        color = if (isListening) GenesisColors.Success else GenesisColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableLessonRow(slot: LessonSlot) {
    val isOngoing = slot.status == "Ongoing"
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOngoing) Color(0xFFF0FDF4) else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isOngoing) Color(0xFFBBF7D0) else GenesisColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        when (slot.status) {
                            "Ongoing" -> GenesisColors.Success
                            "Next" -> GenesisColors.Primary
                            else -> Color(0xFFCBD5E1)
                        },
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = slot.timeRange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GenesisColors.TextSecondary,
                modifier = Modifier.width(88.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                Text(slot.teacherName, fontSize = 12.sp, color = GenesisColors.TextSecondary)
            }
            if (slot.status.isNotBlank()) {
                StatusChip(slot.status)
            }
        }
    }
}

@Composable
private fun HomeworkDeadlineCard(
    item: HomeworkItem,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) Color(0xFFF0FDF4) else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (item.isCompleted) Color(0xFFBBF7D0) else GenesisColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Mark Homework Completed",
                    tint = if (item.isCompleted) GenesisColors.Success else GenesisColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                Text(
                    text = if (item.isCompleted) "Completed & Ready for Submission" else item.dueText,
                    fontSize = 12.sp,
                    color = if (item.isCompleted) GenesisColors.Success else GenesisColors.Emergency
                )
            }
            StatusChip(item.status)
        }
    }
}

@Composable
private fun AskDoubtScreen(
    availableSubjects: List<String> = DiscussionRepository.allSchoolSubjects,
    onBack: () -> Unit,
    onPostToClass: (subject: String, title: String, details: String, hasImage: Boolean, imageUri: String?) -> Unit,
    onAskAI: (String, Uri?) -> Unit
) {
    BackHandler { onBack() }
    var selectedSubject by remember(availableSubjects) {
        mutableStateOf(availableSubjects.firstOrNull() ?: "Mathematics")
    }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cached = copyUriToCacheFile(context, uri) ?: uri
            selectedImageUri = cached
            capturedBitmap = null
            Toast.makeText(context, "Question image selected from Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = saveBitmapToCache(context, bitmap)
            Toast.makeText(context, "Question photo captured from Camera!", Toast.LENGTH_SHORT).show()
        }
    }

    val hasAttachedImg = selectedImageUri != null || capturedBitmap != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Ask a Doubt", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Choose Subject", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSubjects.forEach { s ->
                    SubjectChip(label = s, selected = selectedSubject == s, onClick = { selectedSubject = s })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Question Title", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                VoiceCaptureChipButton(
                    label = "Voice Title",
                    promptHint = "Speak chapter or question title...",
                    onResultText = { spoken -> title = spoken.removeSuffix("?") }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Eg: Quadratic Equations Ex 4.2 Q3", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Question Details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                VoiceCaptureChipButton(
                    label = "Voice Doubt",
                    promptHint = "Speak your doubt clearly...",
                    onResultText = { spoken -> details = mergeVoiceText(details, spoken) }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                placeholder = { Text("Type or speak your doubt...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Attach Image (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = GenesisColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera", color = GenesisColors.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = GenesisColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gallery", color = GenesisColors.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            if (hasAttachedImg) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Success),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Doubt Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Doubt Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✓ Question Image Attached",
                                fontSize = 13.sp,
                                color = GenesisColors.Success,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Students & teachers in your section will see this photo",
                                fontSize = 11.sp,
                                color = GenesisColors.TextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedImageUri = null
                                capturedBitmap = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Image",
                                tint = GenesisColors.Emergency
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            val canSubmitDoubt = title.isNotBlank() && (details.isNotBlank() || hasAttachedImg)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (canSubmitDoubt) {
                            val finalUriStr = selectedImageUri?.toString()
                                ?: capturedBitmap?.let { saveBitmapToCache(context, it)?.toString() }
                            val finalDetails = details.ifBlank { "Please check the attached question image." }
                            onPostToClass(selectedSubject, title, finalDetails, hasAttachedImg, finalUriStr)
                        }
                    },
                    enabled = canSubmitDoubt,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Text("Post to Class", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val q = if (title.isNotBlank() || details.isNotBlank()) "$title: $details" else "Solve $selectedSubject question from the attached image"
                        val prompt = "Act as an expert CBSE $selectedSubject teacher. Explain simply and directly without lengthy confusing text: $q\n\nRules:\n1. Core concept & formula in 1-2 lines.\n2. Step-by-step solution (simple and to the point).\n3. End with 1 quick real-world example to make it easy to understand."
                        val finalUri = selectedImageUri ?: capturedBitmap?.let { saveBitmapToCache(context, it) }
                        onAskAI(prompt, finalUri)
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.AiGreen)
                ) {
                    Text("Ask AI", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private class OrderedPickMultipleImagesContract : ActivityResultContracts.PickMultipleVisualMedia() {
    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        return super.createIntent(context, input).apply {
            // Request 1, 2, 3... tap-order selection badges on supported Android Photo Pickers (Android 14+ / SDK Ext).
            putExtra("android.provider.extra.PICK_IMAGES_IN_ORDER", true)
        }
    }
}

@Composable
private fun UploadStudyMaterialScreen(
    availableSubjects: List<String> = DiscussionRepository.allSchoolSubjects,
    onBack: () -> Unit,
    onUpload: (subject: String, chapter: String, title: String, localPdfUri: String?, pageCount: Int) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedSubject by remember(availableSubjects) {
        mutableStateOf(availableSubjects.firstOrNull() ?: "Physics")
    }
    var chapter by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedPhotos = remember { mutableStateListOf<Uri>() }
    var convertedPdf by remember { mutableStateOf<SavedPdfResult?>(null) }
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pickedPdfName by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedUri = saveBitmapToCache(context, bitmap)
            if (savedUri != null) {
                selectedPhotos.add(savedUri)
                convertedPdf = null
                pickedPdfUri = null
                Toast.makeText(context, "Photo added (Page ${selectedPhotos.size})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val galleryMultiLauncher = rememberLauncherForActivityResult(
        contract = OrderedPickMultipleImagesContract()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val startPage = selectedPhotos.size + 1
            uris.forEach { uri ->
                selectedPhotos.add(copyUriToCacheFile(context, uri) ?: uri)
            }
            convertedPdf = null
            pickedPdfUri = null
            Toast.makeText(
                context,
                "Added ${uris.size} photo(s) in order (Pg $startPage–${selectedPhotos.size})",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedPdfUri = uri
            pickedPdfName = "${title.ifBlank { selectedSubject }}_Notes.pdf"
            convertedPdf = null
            selectedPhotos.clear()
            Toast.makeText(context, "PDF selected!", Toast.LENGTH_SHORT).show()
        }
    }

    fun convertSelectedPhotosToPdf(): SavedPdfResult? {
        if (selectedPhotos.isEmpty()) return null
        val generated = generatePdfFromImageUris(
            context = context,
            imageUris = selectedPhotos.toList(),
            fileNameBase = title.ifBlank { "${selectedSubject}_Notes" },
            noteTitle = title.ifBlank { "$selectedSubject Class Notes" },
            subject = selectedSubject,
            chapter = chapter.ifBlank { "Class Notes" }
        )
        if (generated != null) {
            convertedPdf = generated
        }
        return generated
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Upload Study Material", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Subject", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSubjects.forEach { s ->
                    SubjectChip(
                        label = s,
                        selected = selectedSubject == s,
                        activeColor = GenesisColors.ClassroomPurple,
                        onClick = { selectedSubject = s }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Note Title", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. Ray Optics Board Notes", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Chapter (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = chapter,
                onValueChange = { chapter = it },
                placeholder = { Text("e.g. Chapter 10 (defaults to Class Notes)", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Easy Direct Photo Upload (Camera / Ordered Gallery Multi-Pick) + Photo-to-PDF Converter
            Text("Attach Photos or PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap Gallery to select multiple photos in order (1, 2, 3...) or snap via Camera, then convert to PDF in 1 tap.",
                fontSize = 12.sp,
                color = GenesisColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera", color = GenesisColors.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        galleryMultiLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = GenesisColors.ClassroomPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", color = GenesisColors.ClassroomPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Pick PDF", tint = GenesisColors.Emergency, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("PDF File", color = GenesisColors.Emergency, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (selectedPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pages in Order (${selectedPhotos.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selectedPhotos.size > 1) {
                            Surface(
                                color = GenesisColors.Primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    val reversed = selectedPhotos.reversed()
                                    selectedPhotos.clear()
                                    selectedPhotos.addAll(reversed)
                                    convertedPdf = null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Reverse Order", tint = GenesisColors.Primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reverse", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.Primary)
                                }
                            }
                        }
                        Surface(
                            color = GenesisColors.Emergency.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                selectedPhotos.clear()
                                convertedPdf = null
                            }
                        ) {
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.Emergency,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    selectedPhotos.forEachIndexed { idx, uri ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(92.dp)
                        ) {
                            Box(modifier = Modifier.size(width = 92.dp, height = 110.dp)) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GenesisColors.Border),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Page ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    color = GenesisColors.Primary,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "Pg ${idx + 1}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = GenesisColors.Emergency,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clickable {
                                            selectedPhotos.removeAt(idx)
                                            convertedPdf = null
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            if (selectedPhotos.size > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        color = if (idx > 0) Color(0xFFE2E8F0) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .size(width = 42.dp, height = 24.dp)
                                            .clickable(enabled = idx > 0) {
                                                val moved = selectedPhotos.removeAt(idx)
                                                selectedPhotos.add(idx - 1, moved)
                                                convertedPdf = null
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.ChevronLeft,
                                                contentDescription = "Move Earlier",
                                                tint = if (idx > 0) GenesisColors.TextPrimary else GenesisColors.TextSecondary.copy(alpha = 0.35f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        color = if (idx < selectedPhotos.lastIndex) Color(0xFFE2E8F0) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .size(width = 42.dp, height = 24.dp)
                                            .clickable(enabled = idx < selectedPhotos.lastIndex) {
                                                val moved = selectedPhotos.removeAt(idx)
                                                selectedPhotos.add(idx + 1, moved)
                                                convertedPdf = null
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = "Move Later",
                                                tint = if (idx < selectedPhotos.lastIndex) GenesisColors.TextPrimary else GenesisColors.TextSecondary.copy(alpha = 0.35f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val res = convertSelectedPhotosToPdf()
                        if (res != null) {
                            Toast.makeText(
                                context,
                                "Converted ${selectedPhotos.size} photo(s) to PDF & saved to Downloads!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "Could not convert photos to PDF.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (convertedPdf == null) "Convert ${selectedPhotos.size} Photo(s) to PDF" else "Re-Convert Photos to PDF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (convertedPdf != null) {
                val pdf = convertedPdf!!
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Success),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PDF Ready (${selectedPhotos.size} pages)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GenesisColors.Success)
                            Text(pdf.fileName, fontSize = 11.sp, color = GenesisColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { openPdfUri(context, pdf.openableUri) }) {
                            Text("Preview", color = GenesisColors.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (pickedPdfUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Success),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Selected PDF Ready", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GenesisColors.Success)
                            Text(pickedPdfName, fontSize = 11.sp, color = GenesisColors.TextSecondary)
                        }
                        IconButton(onClick = { pickedPdfUri = null; pickedPdfName = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove PDF", tint = GenesisColors.Emergency)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Description (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                VoiceCaptureChipButton(
                    label = "Voice Note",
                    isAcademicText = false,
                    onResultText = { spoken -> description = mergeVoiceText(description, spoken) }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Add a short description...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            val canUpload = title.isNotBlank() && (selectedPhotos.isNotEmpty() || convertedPdf != null || pickedPdfUri != null)
            Button(
                onClick = {
                    val finalChapter = chapter.trim().ifBlank { "Class Notes" }
                    val finalTitle = title.trim()
                    val finalPdfUri = convertedPdf?.openableUri?.toString()
                        ?: pickedPdfUri?.toString()
                        ?: convertSelectedPhotosToPdf()?.openableUri?.toString()
                    val finalPages = if (selectedPhotos.isNotEmpty()) selectedPhotos.size else 1
                    onUpload(selectedSubject, finalChapter, finalTitle, finalPdfUri, finalPages)
                },
                enabled = canUpload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.ClassroomPurple)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedPhotos.isNotEmpty() && convertedPdf == null) "Convert to PDF & Upload Notes" else "Upload Notes",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============================================================================
// 06 AI DOUBTS — MULTI-MODAL CHATGPT DOUBT SOLVER
// ============================================================================

fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = java.io.File(context.cacheDir, "doubt_camera_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: Exception) {
        null
    }
}

@Composable
fun DoubtsScreen(
    onOpenChatGPT: (String, Uri?) -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    AiHomeworkBuddyScreen(
        onOpenChatGPT = onOpenChatGPT,
        onNavigateHome = onNavigateHome
    )
}

@Composable
private fun HowItWorksStep(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFFDBEAFE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = GenesisColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, color = GenesisColors.TextSecondary)
    }
}

// ============================================================================
// 07 FORMS & REQUESTS — PRODUCTION SCHOOL SERVICES & PARENT DESK
// ============================================================================

private enum class FormsSubRoute {
    SERVICES, FEES, TRANSPORT, PROFILE, COMPLAINT, ADMIN_REVIEW, PROUD_PARENT_QR, STAFF_HR,
    MARKS_AUTOMATION, STUDENT_CONTROL, ROLL_CALL, CALENDAR, CLASS_MANAGER, SUBSTITUTION_HUB, AI_ASSISTANT,
    ADMIN_SIBLINGS
}

@Composable
fun FormsScreen(
    onNavigateHome: () -> Unit = {},
    resetSignal: Int = 0
) {
    val initialResetSignal = remember { resetSignal }
    var subRoute by remember {
        val openFees = NavigationBridge.openFeePaymentDirectly
        NavigationBridge.openFeePaymentDirectly = false
        mutableStateOf(if (openFees) FormsSubRoute.FEES else FormsSubRoute.SERVICES)
    }
    var staffHrInitialTab by remember { mutableIntStateOf(0) }
    val activeStudent by DiscussionRepository.activeStudent.collectAsState()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val currentStaffId by DiscussionRepository.currentStaffId.collectAsState()
    val staffList by DiscussionRepository.staffDirectory.collectAsState()
    val meStaff = staffList.find { it.id == currentStaffId }
    val context = LocalContext.current

    LaunchedEffect(resetSignal) {
        if (resetSignal > initialResetSignal) {
            if (NavigationBridge.openFeePaymentDirectly) {
                NavigationBridge.openFeePaymentDirectly = false
                subRoute = FormsSubRoute.FEES
            } else {
                subRoute = FormsSubRoute.SERVICES
            }
        }
    }

    BackHandler {
        if (subRoute != FormsSubRoute.SERVICES) {
            subRoute = FormsSubRoute.SERVICES
        } else {
            onNavigateHome()
        }
    }

    // Trigger feature-specific APIs only when a Forms/Operations sub-screen is clicked
    LaunchedEffect(subRoute, selectedClass.id) {
        when (subRoute) {
            FormsSubRoute.MARKS_AUTOMATION -> {
                DiscussionRepository.fetchClassSubjectsFromBackend(selectedClass.id)
                DiscussionRepository.fetchClassExamsFromBackend(selectedClass.id)
                DiscussionRepository.fetchClassRosterFromBackend(selectedClass.id)
            }
            FormsSubRoute.STUDENT_CONTROL, FormsSubRoute.ROLL_CALL -> {
                DiscussionRepository.fetchClassRosterFromBackend(selectedClass.id)
            }
            else -> {}
        }
    }

    when (subRoute) {
        FormsSubRoute.STAFF_HR -> {
            StaffHrHubScreen(
                initialTab = staffHrInitialTab,
                onBack = { subRoute = FormsSubRoute.SERVICES },
                onOpenSubstitutionHub = { subRoute = FormsSubRoute.SUBSTITUTION_HUB }
            )
        }
        FormsSubRoute.MARKS_AUTOMATION -> {
            TeacherMarksAutomationScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.STUDENT_CONTROL -> {
            SectionStudentVerificationScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.ROLL_CALL -> {
            AttendanceHubScreen(
                student = activeStudent,
                currentRole = currentRole,
                selectedClass = selectedClass,
                onBack = { subRoute = FormsSubRoute.SERVICES }
            )
        }
        FormsSubRoute.CALENDAR -> {
            AcademicCalendarScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.CLASS_MANAGER -> {
            AcademicYearAndClassManagementScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.ADMIN_SIBLINGS -> {
            AdminSiblingsHubScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.SUBSTITUTION_HUB -> {
            TimetableSubstitutionScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.AI_ASSISTANT -> {
            AiHomeworkBuddyScreen(
                onOpenChatGPT = { prompt, uri ->
                    val url = "https://chatgpt.com/?q=" + java.net.URLEncoder.encode(prompt, "UTF-8")
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)))
                },
                onNavigateHome = { subRoute = FormsSubRoute.SERVICES }
            )
        }
        FormsSubRoute.FEES -> {
            FeeDetailsScreen(
                student = activeStudent,
                onBack = { subRoute = FormsSubRoute.SERVICES }
            )
        }
        FormsSubRoute.TRANSPORT -> {
            TransportPassScreen(
                student = activeStudent,
                onBack = { subRoute = FormsSubRoute.SERVICES }
            )
        }
        FormsSubRoute.PROFILE -> {
            UpdateStudentProfileScreen(
                student = activeStudent,
                onBack = { subRoute = FormsSubRoute.SERVICES }
            )
        }
        FormsSubRoute.COMPLAINT -> {
            ComplaintSuggestionScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.ADMIN_REVIEW -> {
            AdminReviewRequestScreen(onBack = { subRoute = FormsSubRoute.SERVICES })
        }
        FormsSubRoute.PROUD_PARENT_QR -> {
            ProudParentWhatsAppStatusAndQrScreen(
                student = activeStudent,
                onBack = { subRoute = FormsSubRoute.SERVICES }
            )
        }
        FormsSubRoute.SERVICES -> {
            val brand = LocalSchoolBrand.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GenesisColors.Background)
            ) {
                Surface(
                    color = Color.Transparent,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(brand.headerStart, brand.headerEnd)
                                )
                            )
                    ) {
                        HeaderVectorBackdrop(
                            accentColor = Color.White,
                            modifier = Modifier.matchParentSize()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentRole == AppRole.TEACHER) "Teacher & Staff Hub" else "School Services",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (currentRole == AppRole.TEACHER) {
                                        "${meStaff?.name ?: "Faculty Member"} • ${meStaff?.designation ?: selectedClass.name}"
                                    } else {
                                        "${activeStudent.fullName} • ${activeStudent.classTitle}"
                                    },
                                    fontSize = 12.sp,
                                    color = brand.skyAccent
                                )
                            }
                            Image(
                                painter = painterResource(
                                    if (currentRole == AppRole.TEACHER) R.drawable.ic_hub_digital_id else R.drawable.avatar_student_3d
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 88.dp)
                ) {
                    if (currentRole == AppRole.TEACHER) {
                        // Clean, simple Teacher & Staff menu following the reference palette
                        item {
                            ServiceMenuCard(
                                title = "My Staff Profile & Colleagues",
                                subtitle = "View & edit contact profile, teaching load & staff directory",
                                icon = Icons.Rounded.Badge,
                                useSimpleVectorIcon = true,
                                onClick = {
                                    staffHrInitialTab = 0
                                    subRoute = FormsSubRoute.STAFF_HR
                                }
                            )
                            ServiceMenuCard(
                                title = "My Staff Attendance (Check In / Out)",
                                subtitle = "1-tap daily check-in, check-out & monthly log",
                                icon = Icons.Rounded.Fingerprint,
                                useSimpleVectorIcon = true,
                                isSecondaryAccent = true,
                                onClick = {
                                    staffHrInitialTab = 1
                                    subRoute = FormsSubRoute.STAFF_HR
                                }
                            )
                            ServiceMenuCard(
                                title = "Staff Leave Applications",
                                subtitle = "Leave balance & apply for Casual, Sick or Earned leave",
                                icon = Icons.Rounded.EventBusy,
                                useSimpleVectorIcon = true,
                                isSecondaryAccent = true,
                                onClick = {
                                    staffHrInitialTab = 2
                                    subRoute = FormsSubRoute.STAFF_HR
                                }
                            )
                            ServiceMenuCard(
                                title = "Marks & Auto Report Card Engine",
                                subtitle = "Enter marks once • Auto totals, %, grades, history & approval",
                                icon = Icons.Rounded.Grade,
                                useSimpleVectorIcon = true,
                                onClick = { subRoute = FormsSubRoute.MARKS_AUTOMATION }
                            )
                            ServiceMenuCard(
                                title = "Student Data Control (Verify / Block / Edit)",
                                subtitle = "Verify class logins, edit student data, add student or call parent",
                                icon = Icons.Rounded.VerifiedUser,
                                useSimpleVectorIcon = true,
                                onClick = { subRoute = FormsSubRoute.STUDENT_CONTROL }
                            )
                            ServiceMenuCard(
                                title = "Class Roll Call (<60s Attendance)",
                                subtitle = "1-tap Mark All Present + automatic parent absentee alerts",
                                icon = Icons.Rounded.HowToReg,
                                useSimpleVectorIcon = true,
                                onClick = { subRoute = FormsSubRoute.ROLL_CALL }
                            )
                            ServiceMenuCard(
                                title = "Classes, Promotion & Section Data Cleanup",
                                subtitle = "Create classes/sections, promote students & manage old section data",
                                icon = Icons.Rounded.DomainAdd,
                                useSimpleVectorIcon = true,
                                isSecondaryAccent = true,
                                onClick = { subRoute = FormsSubRoute.CLASS_MANAGER }
                            )
                            ServiceMenuCard(
                                title = "Holiday Calendar Publisher",
                                subtitle = "Declare Total School, Class-specific or Section-specific holidays",
                                icon = Icons.Rounded.CalendarMonth,
                                useSimpleVectorIcon = true,
                                isSecondaryAccent = true,
                                onClick = { subRoute = FormsSubRoute.CALENDAR }
                            )
                            ServiceMenuCard(
                                title = "Service Request Approvals",
                                subtitle = "Review and approve pending classroom requests",
                                icon = Icons.Rounded.FactCheck,
                                useSimpleVectorIcon = true,
                                isSecondaryAccent = true,
                                onClick = { subRoute = FormsSubRoute.ADMIN_REVIEW }
                            )
                        }
                    } else {
                        if (currentRole == AppRole.ADMIN) {
                            item {
                                ServiceMenuCard(
                                    title = "Sibling Admissions Leads",
                                    subtitle = "Track other-school sibling records & admission offers",
                                    icon = Icons.Rounded.FamilyRestroom,
                                    useSimpleVectorIcon = true,
                                    onClick = { subRoute = FormsSubRoute.ADMIN_SIBLINGS }
                                )
                                ServiceMenuCard(
                                    title = "Staff & HR Management",
                                    subtitle = "Staff profiles, attendance register & leave approvals",
                                    icon = Icons.Rounded.Badge,
                                    useSimpleVectorIcon = true,
                                    isSecondaryAccent = true,
                                    onClick = {
                                        staffHrInitialTab = 0
                                        subRoute = FormsSubRoute.STAFF_HR
                                    }
                                )
                                ServiceMenuCard(
                                    title = "Staff Leave Applications",
                                    subtitle = "Approve or reject pending staff leave requests",
                                    icon = Icons.Rounded.EventBusy,
                                    useSimpleVectorIcon = true,
                                    isSecondaryAccent = true,
                                    onClick = {
                                        staffHrInitialTab = 2
                                        subRoute = FormsSubRoute.STAFF_HR
                                    }
                                )
                                ServiceMenuCard(
                                    title = "Staff Request Approvals",
                                    subtitle = "Review and approve school service requests",
                                    icon = Icons.Rounded.FactCheck,
                                    useSimpleVectorIcon = true,
                                    isSecondaryAccent = true,
                                    onClick = { subRoute = FormsSubRoute.ADMIN_REVIEW }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        item {
                            ServiceMenuCard(
                                title = "Fee Structure",
                                subtitle = "Term fee ₹ %,d • View term-wise fee breakdown".format(activeStudent.totalFeeInr),
                                icon = Icons.Default.CurrencyRupee,
                                onClick = { subRoute = FormsSubRoute.FEES }
                            )
                            ServiceMenuCard(
                                title = "Student & Parent Contact Profile",
                                subtitle = "Parent mobile (${activeStudent.parentPhone}), email & GPS address",
                                icon = Icons.Default.Person,
                                onClick = { subRoute = FormsSubRoute.PROFILE }
                            )
                            ServiceMenuCard(
                                title = "Bus Transport & Boarding Pass",
                                subtitle = "${activeStudent.busRoute} • Pickup ${activeStudent.pickupTime}",
                                icon = Icons.Default.DirectionsBus,
                                onClick = { subRoute = FormsSubRoute.TRANSPORT }
                            )
                            ServiceMenuCard(
                                title = "Parent Helpdesk & Suggestions",
                                subtitle = "Raise tracked tickets with Principal & Administration",
                                icon = Icons.Default.SupportAgent,
                                onClick = { subRoute = FormsSubRoute.COMPLAINT }
                            )
                            ServiceMenuCard(
                                title = "Proud Parent WhatsApp Status Card + QR",
                                subtitle = "Share achievement card & Referral QR (Code GSB-${activeStudent.studentCode})",
                                icon = Icons.Default.QrCode2,
                                onClick = { subRoute = FormsSubRoute.PROUD_PARENT_QR }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            ReferralTopCard(
                                referralCode = "GSB-${activeStudent.studentCode}",
                                onCopy = {
                                    val code = "GSB-${activeStudent.studentCode}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("School Referral Code", code))
                                    Toast.makeText(context, "Referral code $code copied!", Toast.LENGTH_SHORT).show()
                                },
                                onOpenStatusCardGenerator = {
                                    subRoute = FormsSubRoute.PROUD_PARENT_QR
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferralTopCard(
    referralCode: String,
    onCopy: () -> Unit,
    onOpenStatusCardGenerator: () -> Unit
) {
    val brand = LocalSchoolBrand.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenStatusCardGenerator() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(brand.headerStart, brand.headerEnd)
                    )
                )
        ) {
            HeaderVectorBackdrop(
                accentColor = Color(0xFFFCD34D),
                modifier = Modifier.matchParentSize()
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROUD PARENT WHATSAPP STATUS CARD + REFERRAL QR",
                            color = Color(0xFFFCD34D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Share Achievement Card & ₹5,000 Scholarship QR",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_exam_trophy),
                        contentDescription = "Achievement Trophy",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = referralCode,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onCopy() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Code", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenStatusCardGenerator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Branded WhatsApp Status Card + QR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ServiceMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconRes: Int? = null,
    useSimpleVectorIcon: Boolean = true,
    isSecondaryAccent: Boolean = false,
    onClick: () -> Unit
) {
    val iconBg = if (isSecondaryAccent) Color(0xFFCCFBF1) else GenesisColors.LightBlue
    val iconFg = if (isSecondaryAccent) GenesisColors.Teal else GenesisColors.Primary

    Card(
        colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconFg, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = GenesisColors.TextSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Open", tint = GenesisColors.TextSecondary)
        }
    }
}
