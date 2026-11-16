package com.schoolapp.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.schoolapp.android.data.*
import com.schoolapp.android.ui.theme.GenesisColors
import java.io.File
import java.io.FileOutputStream

// ============================================================================
// 1. ROLE-SPECIFIC SMART SCHOOL OPERATING SYSTEM DASHBOARD CARDS (ON HOME)
//    "One school. One operating system." — Never show every feature to every user.
// ============================================================================

@Composable
fun RoleSpecificSmartDashboardSection(
    currentRole: AppRole,
    activeStudent: StudentProfile,
    selectedClass: SchoolClass,
    onOpenSectionVerification: () -> Unit,
    onOpenSchoolIntelligence: () -> Unit,
    onOpenAutomationEngine: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenMultiHomework: () -> Unit,
    onOpenSubstitutionHub: () -> Unit,
    onOpenAdmissionsCrm: () -> Unit,
    onOpenStudent360: () -> Unit,
    onOpenStaffTransport: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenReportCard: () -> Unit,
    onOpenFeePayment: () -> Unit,
    onOpenProudParentQr: () -> Unit = {},
    onOpenClassAndYearManager: () -> Unit = {},
    onOpenStaffHr: (Int) -> Unit = {},
    onOpenMarksAutomation: () -> Unit = {},
    onOpenCalendar: () -> Unit = {}
) {
    val context = LocalContext.current
    val allSectionRoster by DiscussionRepository.sectionStudentRoster.collectAsState()
    val currentSectionRoster = allSectionRoster.filter { it.classId == selectedClass.id }
    val pendingVerifications = currentSectionRoster.count { it.verificationStatus == "PENDING" }
    val blockedCount = currentSectionRoster.count { it.verificationStatus == "BLOCKED" }
    val leakageAlerts by DiscussionRepository.financialLeakageAlerts.collectAsState()
    val openLeakageCount = leakageAlerts.count { !it.isVerifiedByHuman }
    val homeworkList by DiscussionRepository.homeworkList.collectAsState()
    val classHomework = homeworkList.filter { it.classId == selectedClass.id }
    val pendingHomeworkCount = classHomework.count { it.status != "Completed" }
    val currentStaffId by DiscussionRepository.currentStaffId.collectAsState()
    val staffAttendanceList by DiscussionRepository.staffAttendance.collectAsState()
    val staffLeaveRequests by DiscussionRepository.staffLeaveRequests.collectAsState()
    val myTodayStaffAttendance = staffAttendanceList.find { it.staffId == currentStaffId && it.dateLabel == "Today" }
    val myPendingStaffLeaves = staffLeaveRequests.count { it.staffId == currentStaffId && it.status == "Pending" }
    val (isCheckingStaffGps, startVerifiedCheckIn) = rememberTeacherGeofenceFingerprintCheckIn(currentStaffId)

    var showAbsenceExplanationDialog by remember { mutableStateOf(false) }
    var showParentNotificationPrefsDialog by remember { mutableStateOf(false) }

    if (showAbsenceExplanationDialog) {
        ParentSubmitAbsenceExplanationDialog(
            student = activeStudent,
            onDismiss = { showAbsenceExplanationDialog = false },
            onSubmit = { reason ->
                DiscussionRepository.submitParentAbsenceExplanation(
                    studentName = activeStudent.fullName,
                    reason = reason
                )
                showAbsenceExplanationDialog = false
                Toast.makeText(
                    context,
                    "Absence explanation sent to Class Teacher & Attendance Register!",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    if (showParentNotificationPrefsDialog) {
        ParentNotificationPreferencesDialog(
            onDismiss = { showParentNotificationPrefsDialog = false }
        )
    }

    when (currentRole) {
        AppRole.ADMIN -> {
            // MANAGEMENT & PRINCIPAL "SCHOOL OS" COCKPIT
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GenesisColors.MeteorBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFF1E3A8A),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "🏛️ SCHOOL OPERATING SYSTEM • OWNER & PRINCIPAL",
                                    color = GenesisColors.SkyAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "One School. One Connected Platform.",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            color = Color(0xFF14532D),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { onOpenSchoolIntelligence() }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("4.0x ROI", color = Color(0xFF86EFAC), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                Text("₹20k Saved/mo", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Leakage Prevention Alert Bar
                    if (openLeakageCount > 0) {
                        Surface(
                            color = Color(0xFF451A03),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSchoolIntelligence() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Financial Leakage Prevention: $openLeakageCount items flagged (₹4.25L recoverable)",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Review →",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 2x4 Executive OS Module Pills
                    val adminActions = listOf(
                        Triple("Intelligence & ROI", Icons.Default.Analytics, onOpenSchoolIntelligence),
                        Triple("Verify Students ($pendingVerifications)", Icons.Default.VerifiedUser, onOpenSectionVerification),
                        Triple("Automation Engine", Icons.Default.Bolt, onOpenAutomationEngine),
                        Triple("Admissions CRM", Icons.Default.PersonAddAlt1, onOpenAdmissionsCrm),
                        Triple("Substitution Hub", Icons.Default.SwapHoriz, onOpenSubstitutionHub),
                        Triple("Staff & Transport", Icons.Default.DirectionsBus, onOpenStaffTransport),
                        Triple("360° Student Record", Icons.Default.FolderShared, onOpenStudent360),
                        Triple("School AI Copilot", Icons.Default.AutoAwesome, onOpenAiAssistant)
                    )

                    adminActions.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (label, icon, action) ->
                                Surface(
                                    color = GenesisColors.MeteorSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GenesisColors.MeteorBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clickable { action() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = GenesisColors.SkyAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Proud Parent Branded WhatsApp Status Card + Referral QR Growth Engine CTA
                    Button(
                        onClick = onOpenProudParentQr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🏆 Proud Parent WhatsApp Status Card + Referral QR",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Academic Year Rollover, Create Class/Section & Selective Section Data Control CTA
                    Button(
                        onClick = onOpenClassAndYearManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = GenesisColors.SkyAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎓 Classes, Year Promotion & Section Data Control",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        AppRole.TEACHER -> {
            // TEACHER "TEACHER OS" ZERO-FRICTION COCKPIT
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GenesisColors.MeteorBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFF3B0764),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "👩‍🏫 TEACHER OS • ZERO-FRICTION WORKSPACE",
                                    color = Color(0xFFD8B4FE),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${selectedClass.name} Daily Command",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            color = Color(0xFF14532D),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "⏱ 42m Saved Today",
                                color = Color(0xFF86EFAC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Highlighted Section Student Login Verification Alert Banner
                    Surface(
                        color = if (pendingVerifications > 0) Color(0xFF451A03) else Color(0xFF064E3B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (pendingVerifications > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSectionVerification() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (pendingVerifications > 0) Color(0xFFFBBF24) else Color(0xFF34D399),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Student Verification (${selectedClass.name})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$pendingVerifications Pending  •  $blockedCount Blocked",
                                        color = if (pendingVerifications > 0) Color(0xFFFDE68A) else Color(0xFFA7F3D0),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Surface(
                                color = GenesisColors.Primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Verify",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Teacher / Staff Daily Check-In & HR Desk Strip
                    Surface(
                        color = GenesisColors.MeteorSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.MeteorBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStaffHr(1) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = GenesisColors.SkyAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "My Staff Attendance (500 m Campus)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val statusSubtitle = when {
                                        myTodayStaffAttendance?.status == "On Leave" -> "On Approved Leave Today"
                                        myTodayStaffAttendance?.checkIn != null -> "${myTodayStaffAttendance.status} • Captured at ${myTodayStaffAttendance.checkIn}"
                                        else -> "Scan fingerprint within 500 m to capture attendance"
                                    }
                                    Text(
                                        text = statusSubtitle,
                                        color = GenesisColors.SkyAccent,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (myTodayStaffAttendance?.checkIn == null && myTodayStaffAttendance?.status != "On Leave") {
                                Surface(
                                    color = GenesisColors.Success,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable(enabled = !isCheckingStaffGps) {
                                        startVerifiedCheckIn()
                                    }
                                ) {
                                    Text(
                                        text = if (isCheckingStaffGps) "Checking..." else "Capture",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = GenesisColors.Primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Staff HR →",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val teacherActions = listOf(
                        Triple("<60s Roll Call", Icons.Default.HowToReg, onOpenAttendance),
                        Triple("Student Control ($pendingVerifications)", Icons.Default.VerifiedUser, onOpenSectionVerification),
                        Triple("Auto Marks & Grades", Icons.Default.AutoAwesome, onOpenMarksAutomation),
                        Triple("Create Class & Sec", Icons.Default.AddBusiness, onOpenClassAndYearManager),
                        Triple("Staff Profiles", Icons.Default.Badge) { onOpenStaffHr(0) },
                        Triple("Staff Attendance", Icons.Default.Login) { onOpenStaffHr(1) },
                        Triple(
                            if (myPendingStaffLeaves > 0) "Staff Leave ($myPendingStaffLeaves)" else "Apply Leave",
                            Icons.Default.EventBusy
                        ) { onOpenStaffHr(2) },
                        Triple("Holiday Calendar", Icons.Default.EventNote, onOpenCalendar)
                    )

                    teacherActions.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (label, icon, action) ->
                                Surface(
                                    color = GenesisColors.MeteorSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GenesisColors.MeteorBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clickable { action() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = GenesisColors.SkyAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (currentRole == AppRole.ADMIN) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Academic Year Rollover, Student Promotion & Selective Section Data Control (Admin only)
                        Button(
                            onClick = onOpenClassAndYearManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = GenesisColors.SkyAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Classes, Year Promotion & Section Data Control",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        AppRole.PARENT -> {
            // PARENT CONSOLIDATED DAILY SUMMARY CARD ("Today's School Update")
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFDBEAFE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = GenesisColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Today's School Update",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GenesisColors.TextPrimary
                                )
                                Text(
                                    text = "Consolidated Daily Digest • ${activeStudent.fullName}",
                                    fontSize = 11.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }
                        }
                        TextButton(onClick = { showParentNotificationPrefsDialog = true }) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = GenesisColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alerts", color = GenesisColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConsolidatedUpdateItem(
                                icon = Icons.Default.CheckCircle,
                                iconTint = GenesisColors.Success,
                                label = "Attendance",
                                value = "Present today (94.2% overall)",
                                onClick = onOpenAttendance
                            )
                            Divider(color = GenesisColors.Border)
                            ConsolidatedUpdateItem(
                                icon = Icons.Default.EmojiEvents,
                                iconTint = GenesisColors.ClassroomPurple,
                                label = "Exam Results",
                                value = "Term 1 Score: 91.3%",
                                onClick = onOpenReportCard
                            )
                            Divider(color = GenesisColors.Border)
                            ConsolidatedUpdateItem(
                                icon = Icons.Default.CurrencyRupee,
                                iconTint = GenesisColors.Primary,
                                label = "Fee Structure",
                                value = "₹%,d / Term • View term-wise breakdown".format(activeStudent.totalFeeInr),
                                onClick = onOpenFeePayment
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenStudent360,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "360° Child Record",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onOpenProudParentQr,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🏆 Proud Parent WhatsApp Status Card + Referral QR",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        AppRole.STUDENT -> {
            // STUDENT ACADEMIC COMPANION + CLASS TEACHER VERIFICATION / BLOCKED GUARD
            if (activeStudent.verificationStatus == "BLOCKED") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, GenesisColors.Emergency),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFFFEE2E2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "Blocked",
                                tint = GenesisColors.Emergency,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Account Blocked by Class Teacher",
                            color = GenesisColors.Emergency,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: ${activeStudent.blockedReason.ifBlank { "Unverified student entry in ${activeStudent.classTitle}" }}. Please contact your Class Teacher in the classroom to verify and unblock your account.",
                            color = Color(0xFF7F1D1D),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                DiscussionRepository.switchRole(AppRole.TEACHER)
                                onOpenSectionVerification()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.ClassroomPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Switch to Teacher Mode to Unblock",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsolidatedUpdateItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

// ============================================================================
// 2. CLASS & SECTION STUDENT LOGIN VERIFICATION ROSTER (VERIFY / BLOCK / UNBLOCK)
//    Solves the first-time student onboarding verification without requiring bulk
//    school student uploads upfront!
// ============================================================================

@Composable
fun SectionStudentVerificationScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val allSectionRoster by DiscussionRepository.sectionStudentRoster.collectAsState()
    val currentSectionStudents = remember(allSectionRoster, selectedClass.id) {
        allSectionRoster.filter { it.classId == selectedClass.id }
    }

    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, VERIFIED, BLOCKED
    var studentToBlock by remember { mutableStateOf<SectionRosterStudent?>(null) }
    var studentToEdit by remember { mutableStateOf<SectionRosterStudent?>(null) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var blockReasonText by remember { mutableStateOf("Not enrolled in this class section / Unrecognized entry") }

    val verifiedCount = currentSectionStudents.count { it.verificationStatus == "VERIFIED" }
    val pendingCount = currentSectionStudents.count { it.verificationStatus == "PENDING" }
    val blockedCount = currentSectionStudents.count { it.verificationStatus == "BLOCKED" }

    val filteredStudents = remember(currentSectionStudents, statusFilter) {
        when (statusFilter) {
            "PENDING" -> currentSectionStudents.filter { it.verificationStatus == "PENDING" }
            "VERIFIED" -> currentSectionStudents.filter { it.verificationStatus == "VERIFIED" }
            "BLOCKED" -> currentSectionStudents.filter { it.verificationStatus == "BLOCKED" }
            else -> currentSectionStudents
        }
    }

    if (showAddStudentDialog || studentToEdit != null) {
        val editing = studentToEdit
        var nameInput by remember(editing) { mutableStateOf(editing?.fullName ?: "") }
        var rollInput by remember(editing) { mutableStateOf(editing?.rollNo ?: "") }
        var classInput by remember(editing) {
            mutableStateOf(
                editing?.className ?: selectedClass.name.substringBefore("–").trim()
            )
        }
        var sectionInput by remember(editing) {
            mutableStateOf(
                editing?.section ?: selectedClass.name.substringAfter("Section", "A").trim()
            )
        }
        var phoneInput by remember(editing) { mutableStateOf(editing?.parentPhone ?: "+91 ") }

        Dialog(
            onDismissRequest = {
                showAddStudentDialog = false
                studentToEdit = null
            }
        ) {
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
                        text = if (editing != null) "Edit Student Data" else "Add Student to Roster",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Student Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rollInput,
                            onValueChange = { rollInput = it },
                            label = { Text("Roll No.") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = classInput,
                            onValueChange = { classInput = it },
                            label = { Text("Class") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = sectionInput,
                            onValueChange = { sectionInput = it },
                            label = { Text("Sec") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Parent Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                showAddStudentDialog = false
                                studentToEdit = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary)
                        }
                        Button(
                            onClick = {
                                if (editing != null) {
                                    DiscussionRepository.updateRosterStudentRecord(
                                        studentId = editing.id,
                                        fullName = nameInput,
                                        rollNo = rollInput,
                                        className = classInput,
                                        section = sectionInput,
                                        parentPhone = phoneInput
                                    )
                                    Toast.makeText(context, "Updated $nameInput", Toast.LENGTH_SHORT).show()
                                } else if (nameInput.isNotBlank()) {
                                    DiscussionRepository.addStudentToSectionRoster(
                                        fullName = nameInput,
                                        rollNo = rollInput,
                                        className = classInput,
                                        section = sectionInput,
                                        parentPhone = phoneInput
                                    )
                                    Toast.makeText(context, "Added $nameInput to roster", Toast.LENGTH_SHORT).show()
                                }
                                showAddStudentDialog = false
                                studentToEdit = null
                            },
                            enabled = nameInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (studentToBlock != null) {
        val target = studentToBlock!!
        Dialog(onDismissRequest = { studentToBlock = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = GenesisColors.Emergency)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Block Student",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to block ${target.fullName}? You can unblock them anytime.",
                        fontSize = 13.sp,
                        color = GenesisColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Reason (optional):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = blockReasonText,
                        onValueChange = { blockReasonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { studentToBlock = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary)
                        }
                        Button(
                            onClick = {
                                DiscussionRepository.blockSectionStudent(
                                    studentId = target.id,
                                    reason = blockReasonText.ifBlank { "Not enrolled in section" },
                                    scope = coroutineScope
                                )
                                studentToBlock = null
                                Toast.makeText(
                                    context,
                                    "Blocked ${target.fullName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Emergency)
                        ) {
                            Text("Block", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(
            title = "Verify Students",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Section Selector Dropdown Button
            item {
                val pendingForCurrent = allSectionRoster.count {
                    it.classId == selectedClass.id && it.verificationStatus == "PENDING"
                }
                ClassSectionDropdownButton(
                    selectedClass = selectedClass,
                    availableClasses = DiscussionRepository.availableClasses,
                    onClassSelected = { cls ->
                        DiscussionRepository.switchClass(cls, coroutineScope)
                    },
                    label = "Class & Section:",
                    badgeText = if (pendingForCurrent > 0) "$pendingForCurrent Pending" else null
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Clean Summary Card + Actions
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedClass.name} Roster",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentSectionStudents.size} Students",
                                color = GenesisColors.SkyAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VerificationCountBadge("Pending", pendingCount, Color(0xFFFBBF24), Modifier.weight(1f))
                            VerificationCountBadge("Verified", verifiedCount, Color(0xFF22C55E), Modifier.weight(1f))
                            VerificationCountBadge("Blocked", blockedCount, Color(0xFFEF4444), Modifier.weight(1f))
                        }

                        if (pendingCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    DiscussionRepository.verifyAllSectionStudents(
                                        classId = selectedClass.id,
                                        scope = coroutineScope
                                    )
                                    Toast.makeText(
                                        context,
                                        "Verified all $pendingCount pending student(s)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verify All Pending ($pendingCount)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAddStudentDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GenesisColors.SkyAccent)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add Student", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All (${currentSectionStudents.size})",
                        "PENDING" to "Pending ($pendingCount)",
                        "VERIFIED" to "Verified ($verifiedCount)",
                        "BLOCKED" to "Blocked ($blockedCount)"
                    ).forEach { (key, label) ->
                        val active = statusFilter == key
                        Surface(
                            color = if (active) GenesisColors.Primary else Color.White,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, if (active) GenesisColors.Primary else GenesisColors.Border),
                            modifier = Modifier.clickable { statusFilter = key }
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else GenesisColors.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(filteredStudents, key = { it.id }) { studentItem ->
                val isVerified = studentItem.verificationStatus == "VERIFIED"
                val isBlocked = studentItem.verificationStatus == "BLOCKED"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBlocked) Color(0xFFFEF2F2) else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            isBlocked -> Color(0xFFFECACA)
                            isVerified -> Color(0xFFBBF7D0)
                            else -> Color(0xFFFDE68A)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            when {
                                                isBlocked -> Color(0xFFFEE2E2)
                                                isVerified -> Color(0xFFDCFCE7)
                                                else -> Color(0xFFFEF3C7)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = studentItem.rollNo,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = when {
                                            isBlocked -> GenesisColors.Emergency
                                            isVerified -> GenesisColors.Success
                                            else -> Color(0xFFD97706)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = studentItem.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Text(
                                        text = "Parent: ${studentItem.parentPhone}",
                                        fontSize = 11.sp,
                                        color = GenesisColors.TextSecondary
                                    )
                                }
                            }

                            Surface(
                                color = when {
                                    isBlocked -> Color(0xFFFEE2E2)
                                    isVerified -> Color(0xFFDCFCE7)
                                    else -> Color(0xFFFEF3C7)
                                },
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = when {
                                        isBlocked -> "Blocked"
                                        isVerified -> "Verified"
                                        else -> "Pending"
                                    },
                                    color = when {
                                        isBlocked -> GenesisColors.Emergency
                                        isVerified -> GenesisColors.Success
                                        else -> Color(0xFFB45309)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (isBlocked && studentItem.blockedReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reason: ${studentItem.blockedReason}",
                                fontSize = 11.sp,
                                color = GenesisColors.Emergency,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (isVerified && studentItem.verifiedBy.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = studentItem.verifiedBy,
                                fontSize = 11.sp,
                                color = GenesisColors.Success
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { studentToEdit = studentItem },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", color = GenesisColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${studentItem.parentPhone}")))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = GenesisColors.Success, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Parent", color = GenesisColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isVerified) {
                                Button(
                                    onClick = {
                                        DiscussionRepository.verifySectionStudent(
                                            studentId = studentItem.id,
                                            scope = coroutineScope
                                        )
                                        Toast.makeText(
                                            context,
                                            "Verified ${studentItem.fullName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (isBlocked) {
                                Button(
                                    onClick = {
                                        DiscussionRepository.unblockSectionStudent(
                                            studentId = studentItem.id,
                                            scope = coroutineScope
                                        )
                                        Toast.makeText(
                                            context,
                                            "Unblocked ${studentItem.fullName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Unblock", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        blockReasonText = "Not enrolled in ${selectedClass.name}"
                                        studentToBlock = studentItem
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GenesisColors.Emergency),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = GenesisColors.Emergency, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Block", color = GenesisColors.Emergency, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
private fun VerificationCountBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp)
        }
    }
}

// ============================================================================
// 3. SCHOOL INTELLIGENCE, FINANCIAL LEAKAGE PREVENTION & MONTHLY 4X ROI REPORT
// ============================================================================

@Composable
fun SchoolIntelligenceAndLeakageScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Intelligence, 1: Leakage, 2: 4x ROI Report, 3: Cash/Cheque Ledger
    val leakageAlerts by DiscussionRepository.financialLeakageAlerts.collectAsState()
    val feeTransactions by DiscussionRepository.feeTransactions.collectAsState()
    var showRecordOfflinePaymentDialog by remember { mutableStateOf(false) }

    if (showRecordOfflinePaymentDialog) {
        RecordOfflineFeeCollectionDialog(
            onDismiss = { showRecordOfflinePaymentDialog = false },
            onRecord = { studentName, classSection, amount, mode ->
                DiscussionRepository.recordManualFeeCollection(
                    studentName = studentName,
                    classSection = classSection,
                    amountInr = amount,
                    paymentMode = mode
                )
                showRecordOfflinePaymentDialog = false
                Toast.makeText(context, "Recorded ₹$amount ($mode) & generated audit receipt!", Toast.LENGTH_LONG).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "SchoolOS Intelligence & ROI Hub", onBack = onBack)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = GenesisColors.Primary
        ) {
            listOf("Overview", "Leakage", "4x ROI", "Cash/Cheque").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == idx) GenesisColors.Primary else GenesisColors.TextSecondary
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "LIVE FINANCIAL & OPERATIONAL VISIBILITY",
                                    color = GenesisColors.SkyAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "₹3.82 Cr Collected (AY 2026-27)",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Today: ₹1,84,500  •  This Month: ₹42.6L  •  Outstanding: ₹11.9L",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    VerificationCountBadge("Students", 1240, Color(0xFF38BDF8))
                                    VerificationCountBadge("Attendance", 94, Color(0xFF22C55E))
                                    VerificationCountBadge("Staff Present", 46, Color(0xFFA855F7))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Multi-Branch Performance Comparison", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(DiscussionRepository.branchMetrics, key = { it.branchName }) { branch ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(branch.branchName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${branch.studentsCount} Students", fontSize = 12.sp, color = GenesisColors.Primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Attendance: ${branch.attendancePct}", fontSize = 12.sp, color = GenesisColors.Success, fontWeight = FontWeight.SemiBold)
                                    Text("Collection: ${branch.feeCollectionPct}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Revenue: ${branch.monthlyRevenueLakhs}", fontSize = 12.sp, color = GenesisColors.Primary)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🛡️ School Financial Leakage Prevention System",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Automatically flags overdue fee installments, unmatched bank/cheque receipts, unapproved fee concessions, and vacant seats. Financial records always require human verification.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(leakageAlerts, key = { it.id }) { alert ->
                        val resolved = alert.isVerifiedByHuman
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (resolved) Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryBadge(alert.alertType)
                                    Text(
                                        text = "₹${"%,d".format(alert.amountImpactInr)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (resolved) GenesisColors.Success else GenesisColors.Emergency
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(alert.details, fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (resolved) "✓ Verified by Human Admin" else "Needs Human Verification",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (resolved) GenesisColors.Success else Color(0xFFD97706)
                                    )
                                    if (!resolved) {
                                        Button(
                                            onClick = {
                                                DiscussionRepository.verifyAndResolveLeakageAlert(alert.id)
                                                Toast.makeText(context, "Verified & resolved ${alert.title}", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Verify & Resolve", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // YOUR SCHOOL'S MONTHLY EFFICIENCY REPORT (4X ROI)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Surface(
                                    color = Color(0xFF14532D),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "📈 YOUR SCHOOL'S MONTHLY EFFICIENCY REPORT",
                                        color = Color(0xFF86EFAC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "₹20,000 Value Created This Month",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "vs ₹5,000 Monthly Platform Subscription  =  4.0x Net ROI",
                                    color = GenesisColors.SkyAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val roiMetrics = listOf(
                                    "Automated Fee Reminders Sent" to "1,240 reminders",
                                    "On-Time Fee Collection Improvement" to "+28% faster collection",
                                    "Teacher Admin Hours Saved" to "86 hours saved this month",
                                    "Paper, Diaries & Circular Printing Saved" to "₹18,500 saved",
                                    "Admission Enquiries Tracked & Converted" to "34 leads → 19 admissions",
                                    "Parent Communication Read Rate" to "93.4% verified reads"
                                )

                                roiMetrics.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(k, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                        Text(v, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Divider(color = GenesisColors.MeteorBorder)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val saved = generateAndSaveSchoolPdf(
                                            context = context,
                                            fileNameBase = "Genesis_Monthly_ROI_Efficiency_Report_Nov2026",
                                            documentCategory = "SchoolOS Management Intelligence • Monthly ROI Audit",
                                            documentTitle = "Your School's Monthly Efficiency Report",
                                            subtitleLine = "Genesis School Beeramguda  |  4.0x Verified Return on Investment",
                                            metaLines = listOf(
                                                "Monthly Subscription: INR 5,000   •   Measured Monthly Savings & Recovery: INR 20,000+",
                                                "Teacher Time Saved: 86 Hours   •   Paperless Circular Savings: INR 18,500"
                                            ),
                                            bodyLines = roiMetrics.map { "${it.first}: ${it.second}" }
                                        )
                                        if (saved != null) {
                                            Toast.makeText(context, "Saved ROI Report PDF to Downloads/${saved.fileName}", Toast.LENGTH_LONG).show()
                                            openPdfUri(context, saved.openableUri)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Monthly Efficiency Report PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // OFFLINE CASH, CHEQUE & BANK-TRANSFER ENTRY + RECONCILIATION LEDGER
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Unified Fee Ledger & Reconciliation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("UPI Online + Cash + Cheque + NEFT Audit Trail", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            }
                            Button(
                                onClick = { showRecordOfflinePaymentDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Record Cash/Cheque", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(feeTransactions, key = { it.id }) { txn ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${txn.studentName} (${txn.classSection})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${txn.paymentMode} • ${txn.receiptNo}", fontSize = 12.sp, color = GenesisColors.Primary, fontWeight = FontWeight.SemiBold)
                                    Text(txn.timestamp, fontSize = 11.sp, color = GenesisColors.TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹${"%,d".format(txn.amountInr)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = GenesisColors.Success)
                                    Text(txn.status, fontSize = 10.sp, color = GenesisColors.TextSecondary)
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
private fun RecordOfflineFeeCollectionDialog(
    onDismiss: () -> Unit,
    onRecord: (studentName: String, classSection: String, amount: Int, mode: String) -> Unit
) {
    var studentName by remember { mutableStateOf("Vihaan Reddy") }
    var classSection by remember { mutableStateOf("Class 10-A") }
    var amountText by remember { mutableStateOf("14500") }
    var selectedMode by remember { mutableStateOf("Cash") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Record Offline Cash / Cheque / Bank Fee", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Student Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = classSection,
                    onValueChange = { classSection = it },
                    label = { Text("Class & Section") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (INR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "Cheque", "Bank Transfer").forEach { mode ->
                        SubjectChip(
                            label = mode,
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = GenesisColors.TextPrimary)
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toIntOrNull() ?: 14500
                            onRecord(studentName.ifBlank { "Student" }, classSection, amt, selectedMode)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                    ) {
                        Text("Save & Issue Receipt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. CENTRAL SCHOOL AUTOMATION ENGINE (WHEN -> CHECK CONDITION -> THEN -> RECORD)
// ============================================================================

@Composable
fun SchoolAutomationEngineScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val rules by DiscussionRepository.automationRules.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "School Automation Engine", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "CONNECTED WORKFLOW ENGINE",
                            color = GenesisColors.SkyAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WHEN [Event] → CHECK [Condition] → THEN [Action] → RECORD",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every module triggers the next automatically so teachers and administrators never repeat manual follow-ups.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            items(rules, key = { it.id }) { rule ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (rule.isEnabled) GenesisColors.Border else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadge(rule.category)
                            Switch(
                                checked = rule.isEnabled,
                                onCheckedChange = { DiscussionRepository.toggleAutomationRule(rule.id) }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WHEN: ${rule.whenTrigger}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GenesisColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CHECK: ${rule.checkCondition}",
                            fontSize = 12.sp,
                            color = GenesisColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "THEN: ${rule.thenAction}",
                            fontSize = 12.sp,
                            color = GenesisColors.TextSecondary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Executed ${rule.executionsCount} times (${rule.lastTriggeredText})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.Success
                            )
                            OutlinedButton(
                                onClick = {
                                    DiscussionRepository.incrementAutomationRuleCounter(rule.id)
                                    Toast.makeText(
                                        context,
                                        "Simulated workflow: ${rule.whenTrigger} → ${rule.thenAction}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Test Trigger", color = GenesisColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. MULTI-SECTION HOMEWORK & REUSABLE TEMPLATES LIBRARY (MODULE 3)
// ============================================================================

@Composable
fun MultiSectionHomeworkScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val templates = DiscussionRepository.homeworkTemplates

    var subject by remember { mutableStateOf("Mathematics") }
    var title by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var dueText by remember { mutableStateOf("Due Tomorrow, 09:00 AM") }
    var saveToLibrary by remember { mutableStateOf(true) }
    val selectedSections = remember { mutableStateListOf("class-10a", "class-10b") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Multi-Section Homework & Library", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = "1-Tap Reusable Homework Templates Library",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GenesisColors.TextPrimary
                )
                Text(
                    text = "Tap any template below to pre-fill and assign across multiple sections in seconds:",
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(templates, key = { it.id }) { tpl ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            subject = tpl.subject
                            title = tpl.title
                            instructions = tpl.instructions
                            Toast.makeText(context, "Loaded template: ${tpl.title}", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(tpl.subject)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${tpl.gradeLevel} • ${tpl.maxMarks}M", fontSize = 10.sp, color = GenesisColors.TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(tpl.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(tpl.instructions, fontSize = 11.sp, color = GenesisColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("Use →", color = GenesisColors.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Create & Distribute to Multiple Sections", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Select Target Sections (Distribute Simultaneously):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiscussionRepository.availableClasses.forEach { cls ->
                                val chosen = selectedSections.contains(cls.id)
                                Surface(
                                    color = if (chosen) GenesisColors.Primary else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (chosen) GenesisColors.Primary else GenesisColors.Border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (chosen && selectedSections.size > 1) {
                                                selectedSections.remove(cls.id)
                                            } else if (!chosen) {
                                                selectedSections.add(cls.id)
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cls.name.removePrefix("Class "),
                                            color = if (chosen) Color.White else GenesisColors.TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Homework Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Detailed Instructions & Attachments") },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dueText,
                            onValueChange = { dueText = it },
                            label = { Text("Submission Deadline") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = saveToLibrary, onCheckedChange = { saveToLibrary = it })
                            Text("Save to Reusable Homework Library for next year", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    DiscussionRepository.assignHomeworkToMultipleSections(
                                        targetClassIds = selectedSections.toList(),
                                        subject = subject,
                                        title = title,
                                        dueText = dueText,
                                        instructions = instructions.ifBlank { "Complete in fair notebook." }
                                    )
                                    Toast.makeText(
                                        context,
                                        "Assigned '${title}' to ${selectedSections.size} sections simultaneously!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Assign to ${selectedSections.size} Section(s) & Notify Parents",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. ROLE-AWARE AI SCHOOL ASSISTANT (TEACHER / MANAGEMENT / STUDENT)
// ============================================================================

@Composable
fun AiSchoolAssistantScreen(
    currentRole: AppRole,
    onOpenChatGptHandoff: (String, Uri?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedTab by remember {
        mutableIntStateOf(
            when (currentRole) {
                AppRole.ADMIN -> 1
                AppRole.STUDENT -> 2
                else -> 0
            }
        )
    }
    var topicInput by remember { mutableStateOf("Quadratic Equations — Nature of Roots & Discriminant") }
    var generatedOutput by remember { mutableStateOf<String?>(null) }
    var generatedTitle by remember { mutableStateOf("AI Generated Academic Output") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "SchoolOS AI Copilot", onBack = onBack)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = GenesisColors.Primary
        ) {
            listOf("Teacher AI", "Management AI", "Student AI").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = {
                        selectedTab = idx
                        generatedOutput = null
                    },
                    text = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == idx) GenesisColors.Primary else GenesisColors.TextSecondary
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("👩‍🏫 AI Assistant for Teachers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = "Enter a chapter/topic and generate ready-to-use Homework, Question Papers, Printable Worksheets (PDF), Lesson Plans, Parent Circulars, or Report Card Remarks.",
                                    fontSize = 12.sp,
                                    color = GenesisColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = topicInput,
                                    onValueChange = { topicInput = it },
                                    label = { Text("Chapter / Topic / Context") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val generators = listOf(
                                    "Homework Generator" to "HOMEWORK ASSIGNMENT: $topicInput\n1. Solve 5 conceptual problems on discriminant D = b² - 4ac.\n2. Identify nature of roots for 3x² - 4√3x + 4 = 0.\n3. Challenge: Find k for equal roots in kx(x - 2) + 6 = 0.",
                                    "Question Paper (20M)" to "UNIT TEST QUESTION PAPER (20 MARKS) — $topicInput\nSection A (1M x 4): MCQ on standard form ax² + bx + c = 0.\nSection B (2M x 3): Solve by factorization & quadratic formula.\nSection C (5M x 2): Word problems on speed-distance and age.",
                                    "Printable Worksheet" to "CLASSROOM PRACTICE WORKSHEET — $topicInput\nName: ________________   Roll No: ____   Section: 10-A\nQ1. Compute discriminant D for 2x² - 7x + 3 = 0.\nQ2. State whether roots are real & distinct, equal, or imaginary.\nQ3. Derive the sum and product of roots.",
                                    "40-Min Lesson Plan" to "40-MINUTE CBSE LESSON PLAN — $topicInput\n• 00–08 min: Warm-up hook with parabolic bridge arch visual.\n• 08–22 min: Board derivation of D = b² - 4ac and 3 cases.\n• 22–34 min: Guided pair problem-solving from NCERT Ex 4.4.\n• 34–40 min: Exit ticket & 1-tap Multi-Section Homework dispatch.",
                                    "Parent Announcement" to "Dear Parents,\nOur class has completed '$topicInput' today. A revision worksheet has been shared in the Study Notes tab. Kindly encourage your ward to complete the 15-minute practice questions before Friday's assessment.\nWarm regards, Class Teacher",
                                    "Report Card Comments" to "PERSONALIZED REPORT CARD REMARKS:\n• High Achiever: Demonstrates exceptional analytical clarity in $topicInput.\n• Steady Learner: Shows good conceptual grasp; regular timed practice will boost speed.\n• Needs Support: Requires guided revision of sign conventions and formula substitution."
                                )

                                generators.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { (btnLabel, outputText) ->
                                            Button(
                                                onClick = {
                                                    generatedTitle = btnLabel
                                                    generatedOutput = outputText
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text(btnLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("🏛️ AI Executive Queries for Management", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Tap any executive query to analyze live school operations:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))

                                val managementQueries = listOf(
                                    "Show classes with attendance below 80% this week" to "ATTENDANCE SHORTAGE INSIGHT:\n• Overall school attendance is 94.2%.\n• Only Class 8-B (78.4%) dipped below 80% on Wednesday due to post-sports-day fatigue.\n• Action Taken: Class Teacher notified & 4 shortage alerts sent.",
                                    "List students with fees overdue by 30 days" to "OVERDUE FEE INTELLIGENCE (14 STUDENTS • ₹3,42,000):\n• Class 10-A: 4 students (₹98,000) — WhatsApp/UPI link sent.\n• Class 9-A: 6 students (₹1,48,000) — Installment 2 pending.\n• Class 8-A: 4 students (₹96,000) — Flagged for accounts desk call.",
                                    "Summarize parent complaints this month" to "MONTHLY PARENT FEEDBACK SUMMARY:\n• Total Tickets: 7 (6 Resolved, 1 In Progress).\n• Top Theme: Bus Route #03 morning delay by 6 mins near Miyapur Crossroads (resolved via GPS schedule adjustment)."
                                )

                                managementQueries.forEach { (query, answer) ->
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, GenesisColors.Border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                generatedTitle = query
                                                generatedOutput = answer
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("“$query”", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("🎓 AI Concept Tutor for Students", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Safe, grade-appropriate concept explanations, hints, and multilingual support:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = topicInput,
                                    onValueChange = { topicInput = it },
                                    label = { Text("Enter Concept or Homework Doubt") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            generatedTitle = "Step-by-Step Hint (English + Telugu)"
                                            generatedOutput = "CONCEPT GUIDE: $topicInput\n• Step 1: Compare with ax² + bx + c = 0.\n• Step 2: Calculate Discriminant D = b² - 4ac.\n• Telugu Summary: వర్గ సమీకరణం యొక్క మూలాల స్వభావాన్ని కనుగొనడానికి D = b² - 4ac సూత్రాన్ని ఉపయోగించండి."
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                                    ) {
                                        Text("Get Concept Hint", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            onOpenChatGptHandoff(
                                                "Explain '$topicInput' for a CBSE Class 10 student with a step-by-step example and 2 practice questions.",
                                                null
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F))
                                    ) {
                                        Text("Open in ChatGPT", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (generatedOutput != null) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(generatedTitle, color = GenesisColors.SkyAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(generatedOutput!!, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val saved = generateAndSaveSchoolPdf(
                                        context = context,
                                        fileNameBase = "Genesis_AI_Worksheet_${System.currentTimeMillis() / 1000}",
                                        documentCategory = "SchoolOS AI Academic Generator",
                                        documentTitle = generatedTitle,
                                        subtitleLine = "Topic: $topicInput",
                                        metaLines = listOf("Generated by Genesis SchoolOS AI Copilot"),
                                        bodyLines = generatedOutput!!.lines()
                                    )
                                    if (saved != null) {
                                        Toast.makeText(context, "Saved PDF to Downloads/${saved.fileName}", Toast.LENGTH_LONG).show()
                                        openPdfUri(context, saved.openableUri)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download as Printable PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. TIMETABLE & AUTOMATED TEACHER SUBSTITUTION WORKFLOW (MODULE 7)
// ============================================================================

@Composable
fun TimetableSubstitutionScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val substitutions by DiscussionRepository.teacherSubstitutions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Timetable & Auto-Substitution", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AUTOMATED SUBSTITUTION ENGINE", color = GenesisColors.SkyAccent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Zero Unattended Classrooms", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "When a teacher applies for leave, the system identifies affected periods, checks conflict-free teachers, and notifies substitutes & students automatically.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                DiscussionRepository.reportTeacherLeaveAndFindSubstitutes(
                                    teacherName = "Mrs. Gupta",
                                    leaveReason = "Emergency Medical Leave",
                                    periodSlot = "Period 6 (01:30 PM – 02:15 PM)",
                                    classSectionName = "Class 10 – Section A",
                                    subject = "Biology"
                                )
                                Toast.makeText(context, "Simulated leave: Found 3 conflict-free substitute teachers!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Teacher Leave & Auto-Find Substitutes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            items(substitutions, key = { it.id }) { sub ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${sub.classSectionName} • ${sub.subject}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            StatusChip(if (sub.status == "Assigned & Notified") "Approved" else "Pending")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${sub.periodSlot}  |  Absent: ${sub.absentTeacherName}", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Conflict-Free Available Substitutes (Tap to Assign):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sub.suggestedSubstitutes.forEach { candidate ->
                                val isAssigned = sub.assignedSubstitute == candidate
                                Surface(
                                    color = if (isAssigned) GenesisColors.Success else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isAssigned) GenesisColors.Success else GenesisColors.Border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            DiscussionRepository.assignSubstituteTeacher(sub.id, candidate)
                                            Toast.makeText(context, "Assigned $candidate & notified ${sub.classSectionName}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Text(
                                        text = if (isAssigned) "✓ Assigned: $candidate" else candidate,
                                        color = if (isAssigned) Color.White else GenesisColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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

// ============================================================================
// 8. ADMISSIONS & ENQUIRY CRM PIPELINE (MODULE 10)
// ============================================================================

@Composable
fun AdmissionsCrmScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val enquiries by DiscussionRepository.admissionEnquiries.collectAsState()
    var showAddEnquiryDialog by remember { mutableStateOf(false) }

    if (showAddEnquiryDialog) {
        var childName by remember { mutableStateOf("") }
        var parentName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("+91 ") }
        var targetClass by remember { mutableStateOf("Class 6") }

        Dialog(onDismissRequest = { showAddEnquiryDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("New Admission Enquiry", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(value = childName, onValueChange = { childName = it }, label = { Text("Child Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Parent Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = targetClass, onValueChange = { targetClass = it }, label = { Text("Applying For Class") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (childName.isNotBlank()) {
                                DiscussionRepository.createAdmissionEnquiry(
                                    childName = childName,
                                    parentName = parentName.ifBlank { "Parent" },
                                    parentPhone = phone,
                                    targetGrade = targetClass
                                )
                                showAddEnquiryDialog = false
                                Toast.makeText(context, "Enquiry logged & counsellor assigned automatically!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Create Enquiry & Auto-Assign Counsellor", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Admissions & Enquiry CRM", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Admission Lead Pipeline", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Enquiry → Counsellor → Follow-Up → Interview → Admitted", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                    }
                    Button(
                        onClick = { showAddEnquiryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Lead", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(enquiries, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.childName} (${item.targetGrade})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            CategoryBadge(item.stage)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Parent: ${item.parentName} (${item.parentPhone}) • Source: ${item.source}", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Text("Counsellor: ${item.counsellorName} • Next: ${item.nextFollowUp}", fontSize = 11.sp, color = GenesisColors.Primary, fontWeight = FontWeight.SemiBold)
                        if (item.stage != "Admitted") {
                            Spacer(modifier = Modifier.height(10.dp))
                            val nextStage = when (item.stage) {
                                "New Enquiry" -> "Follow-Up Scheduled"
                                "Follow-Up Scheduled" -> "Interview Scheduled"
                                else -> "Admitted"
                            }
                            Button(
                                onClick = {
                                    DiscussionRepository.advanceAdmissionStage(item.id)
                                    Toast.makeText(context, "Moved ${item.childName} to $nextStage", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                            ) {
                                Text("Advance to '$nextStage' →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 9. CENTRALIZED 360° STUDENT PROFILE & STAFF / TRANSPORT HUB (MODULES 8, 11, 12)
// ============================================================================

@Composable
fun Student360ProfileScreen(
    student: StudentProfile,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val examResults by DiscussionRepository.examResultsFlow.collectAsState()
    val feeTransactions by DiscussionRepository.feeTransactions.collectAsState()
    val leaveRequests by DiscussionRepository.leaveRequests.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "360° Student Master Record", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("SINGLE UNIFIED STUDENT RECORD", color = GenesisColors.SkyAccent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(student.fullName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "${student.classTitle}  •  ID #${student.studentCode}  •  Status: ${student.verificationStatus}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Guardian Contact: ${student.parentPhone}  |  Route: ${student.busRoute}", color = GenesisColors.SkyAccent, fontSize = 11.sp)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1. Attendance & Punctuality Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Overall Attendance: 94.2% (178/189 Days Present) • Above 75% CBSE Norm", fontSize = 12.sp, color = GenesisColors.Success, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("2. Academic Marks & Grade Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        examResults.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(r.subject, fontSize = 12.sp)
                                Text("${r.marksObtained}/${r.maxMarks} (${r.grade})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("3. Fee Payment Ledger & Concessions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        feeTransactions.take(3).forEach { txn ->
                            Text("• ${txn.receiptNo}: ₹${"%,d".format(txn.amountInr)} via ${txn.paymentMode} (${txn.status})", fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("4. Leave History, Certificates & Teacher Observations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Teacher Observation: Consistently participates in STEM lab & peer problem solving.", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Text("• Digital Documents: Bonafide Certificate, Transfer Certificate & ID Card verified.", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Text("• Leave Applications on Record: ${leaveRequests.size} requests tracked.", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun StaffAndTransportHubScreen(
    onBack: () -> Unit,
    onOpenStaffHr: (Int) -> Unit = {}
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val staffList by DiscussionRepository.staffDirectory.collectAsState()
    val staffLeaves by DiscussionRepository.staffLeaveRequests.collectAsState()
    val pendingLeaveCount = staffLeaves.count { it.status == "Pending" }
    val busRoutes by DiscussionRepository.busRoutes.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Staff HR & Transport Safety Hub", onBack = onBack)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = GenesisColors.Primary
        ) {
            val presentStaff = staffList.count { it.attendanceStatus == "Present" || it.attendanceStatus == "Late" }
            listOf("Staff & HR ($presentStaff/${staffList.size})", "Bus Routes & Safety").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == idx) GenesisColors.Primary else GenesisColors.TextSecondary
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (selectedTab == 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenStaffHr(1) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Staff Attendance", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onOpenStaffHr(2) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pendingLeaveCount > 0) Color(0xFFD97706) else GenesisColors.ClassroomPurple
                            )
                        ) {
                            Text(
                                text = if (pendingLeaveCount > 0) "Leave Approvals ($pendingLeaveCount)" else "Leave Requests",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                items(staffList, key = { it.id }) { staff ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onOpenStaffHr(0) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${staff.designation} • ${staff.department}", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Text("Assigned: ${staff.classesAssigned} (${staff.workloadPeriodsPerWeek} periods/wk)", fontSize = 11.sp, color = GenesisColors.Primary)
                            }
                            StatusChip(if (staff.attendanceStatus == "Present" || staff.attendanceStatus == "Late") "Approved" else "Pending")
                        }
                    }
                }
            } else {
                items(busRoutes, key = { it.id }) { route ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${route.routeNumber} — ${route.routeTitle}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("${route.studentsAssigned} Students", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Vehicle: ${route.vehicleNumber}  •  Driver: ${route.driverName} (${route.driverPhone})", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text("Attendant: ${route.attendantName}  •  Maint. Due: ${route.nextMaintenanceDue}", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Live Status: ${route.tripStatus}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Success)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    DiscussionRepository.triggerBusRouteNotification(
                                        route.id,
                                        "Pickup/Drop Alert Sent — Bus ${route.vehicleNumber} is approaching stop"
                                    )
                                    Toast.makeText(context, "Sent Pickup/Drop notification to ${route.studentsAssigned} parents!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Pickup / Drop Alert to Parents", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentSubmitAbsenceExplanationDialog(
    student: StudentProfile,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("Viral fever — advised 1 day rest by family doctor") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Submit Parent Absence Explanation", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Student: ${student.fullName} (${student.classTitle}). This explanation is automatically linked to the Class Teacher's Attendance Register.",
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Absence / Leave") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = GenesisColors.TextPrimary)
                    }
                    Button(
                        onClick = { onSubmit(reason.ifBlank { "Family / Medical reason" }) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Submit to Teacher", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentNotificationPreferencesDialog(
    onDismiss: () -> Unit
) {
    var instantAttendanceAlert by remember { mutableStateOf(true) }
    var consolidatedEveningDigest by remember { mutableStateOf(true) }
    var feeInstallmentReminders by remember { mutableStateOf(true) }
    var preferredLanguage by remember { mutableStateOf("English + Telugu") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Parent Notification Preferences", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Prevent notification fatigue with smart digests:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Instant Absent / Bus Safety Alerts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = instantAttendanceAlert, onCheckedChange = { instantAttendanceAlert = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Consolidated Daily Evening Digest", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = consolidatedEveningDigest, onCheckedChange = { consolidatedEveningDigest = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fee Installment & Receipt Alerts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = feeInstallmentReminders, onCheckedChange = { feeInstallmentReminders = it })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Preferred Language:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "English + Telugu", "Hindi").forEach { lang ->
                        SubjectChip(label = lang, selected = preferredLanguage == lang, onClick = { preferredLanguage = lang })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Text("Save Preferences", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// 10. PROUD PARENT BRANDED WHATSAPP STATUS CARD + SCANNABLE REFERRAL QR ENGINE
//     (Pillar 1 of the ₹1 Lakh School ROI Blueprint — Organic Admissions Growth)
// ============================================================================

private data class ProudParentCardTemplate(
    val id: String,
    val chipLabel: String,
    val badgeTitle: String,
    val headline: String,
    val metricHighlight: String,
    val quoteLine: String
)

@Composable
fun ProudParentWhatsAppStatusAndQrScreen(
    student: StudentProfile,
    onOpenAdmissionsCrm: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val referralCode = "GSB-${student.studentCode}"
    val referralUrl = "https://genesisschool.edu.in/apply?ref=$referralCode"
    val enquiries by DiscussionRepository.admissionEnquiries.collectAsState()
    val examResults by DiscussionRepository.examResultsFlow.collectAsState()
    val totalObtained = examResults.sumOf { it.marksObtained }
    val totalMax = examResults.sumOf { it.maxMarks }.coerceAtLeast(1)
    val livePct = (totalObtained * 100.0) / totalMax
    val liveGrade = when {
        livePct >= 91.0 -> "A1"
        livePct >= 81.0 -> "A2"
        livePct >= 71.0 -> "B1"
        else -> "B2"
    }
    val topSubject = examResults.maxByOrNull { it.marksObtained }
    val referralLeadsCount = enquiries.count {
        it.source.contains("Referral", ignoreCase = true) || it.source.contains("QR", ignoreCase = true)
    }

    val templates = remember(student.fullName, student.classTitle, totalObtained, totalMax) {
        listOf(
            ProudParentCardTemplate(
                id = "honour_roll",
                chipLabel = "🏆 Class Rank & Marks",
                badgeTitle = "TERM-1 HONOUR ROLL • CLASS TOP 3 RANK",
                headline = "${student.fullName} (${student.classTitle})",
                metricHighlight = "${"%.1f".format(livePct)}% ($totalObtained/$totalMax) • Grade $liveGrade Distinction",
                quoteLine = "So proud of ${student.firstName}'s Term-1 Rank & ${topSubject?.subject ?: "Maths"} score (${topSubject?.marksObtained ?: 98}/100) at Genesis School Beeramguda!"
            ),
            ProudParentCardTemplate(
                id = "star_attendance",
                chipLabel = "🌟 94.2% Star Attendance",
                badgeTitle = "STAR PUNCTUALITY & CONSISTENCY AWARD",
                headline = "${student.fullName} (${student.classTitle})",
                metricHighlight = "94.2% Attendance • 100% Homework Mastery",
                quoteLine = "Discipline + daily progress tracking at Genesis School keeps our child ahead every single day!"
            ),
            ProudParentCardTemplate(
                id = "olympiad_scholar",
                chipLabel = "🚀 Subject Topper",
                badgeTitle = "SUBJECT TOPPER & STEM SCHOLAR",
                headline = "${student.fullName} (${student.classTitle})",
                metricHighlight = "${topSubject?.subject ?: "Mathematics"} Topper: ${topSubject?.marksObtained ?: 98}/${topSubject?.maxMarks ?: 100} (Grade ${topSubject?.grade ?: "A1"})",
                quoteLine = "Zero-doubt classroom + CBSE board clarity at Genesis School Beeramguda!"
            ),
            ProudParentCardTemplate(
                id = "admissions_ambassador",
                chipLabel = "🎓 Admissions Pass",
                badgeTitle = "OFFICIAL PARENT ADMISSIONS AMBASSADOR",
                headline = "Parent of ${student.fullName} (${student.classTitle})",
                metricHighlight = "Scan QR for Priority Campus Tour + ₹5,000 Fee Waiver",
                quoteLine = "Admissions Open for AY 2026–27 at Genesis School Beeramguda. Scan our family QR below!"
            )
        )
    }

    var selectedTemplate by remember(templates) { mutableStateOf(templates.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(
            title = "Proud Parent Status Card & QR",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. ROI & Referral Impact Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
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
                        Text(
                            text = "ORGANIC ADMISSIONS GROWTH ENGINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = "Share your child's achievement card to WhatsApp Status. When friends scan the Referral QR, both families get ₹5,000 fee scholarship!",
                            fontSize = 11.sp,
                            color = Color(0xFF065F46),
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF047857),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$referralLeadsCount Leads",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "In CRM",
                                color = Color(0xFFA7F3D0),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Achievement Template Selector
            Text(
                text = "1. Choose Proud Parent Status Card Theme",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = GenesisColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templates.forEach { tpl ->
                    SubjectChip(
                        label = tpl.chipLabel,
                        selected = selectedTemplate.id == tpl.id,
                        activeColor = Color(0xFFD97706),
                        onClick = { selectedTemplate = tpl }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. LIVE BRANDED WHATSAPP STATUS CARD PREVIEW (Meteor Black + Royal Gold + Scannable QR)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                border = BorderStroke(2.dp, Color(0xFFF59E0B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    GenesisColors.MeteorBlack,
                                    Color(0xFF172554),
                                    GenesisColors.MeteorBlack
                                )
                            )
                        )
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top School Crest & Verified Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFF59E0B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = GenesisColors.MeteorBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "GENESIS SCHOOL BEERAMGUDA",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "CBSE Smart Campus • Proud Parent Status Card",
                                    color = Color(0xFFFCD34D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFF14532D),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color(0xFF22C55E))
                        ) {
                            Text(
                                text = "✓ VERIFIED",
                                color = Color(0xFF86EFAC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Golden Achievement Pill
                    Surface(
                        color = Color(0xFF451A03),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "★ ${selectedTemplate.badgeTitle} ★",
                            color = Color(0xFFFDE68A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = selectedTemplate.headline,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = selectedTemplate.metricHighlight,
                        color = GenesisColors.SkyAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "\"${selectedTemplate.quoteLine}\"",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // White Referral QR Code Box inside the Status Card
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Crisp Deterministic Scannable-Style QR Code Canvas
                            ReferralQrCodeBox(
                                seed = referralCode,
                                modifier = Modifier.size(92.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🎁 ₹5,000 FEE SCHOLARSHIP PASS",
                                        color = Color(0xFFB45309),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scan for Priority Admission",
                                    color = GenesisColors.MeteorBlack,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Referral Code: $referralCode",
                                    color = GenesisColors.Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "AY 2026-27 • Nursery to Class 10",
                                    color = GenesisColors.TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Primary Action Buttons: Share Branded PNG Image to WhatsApp Status / Copy Referral Link
            Button(
                onClick = {
                    shareProudParentCardBitmapToWhatsApp(
                        context = context,
                        student = student,
                        template = selectedTemplate,
                        referralCode = referralCode,
                        referralUrl = referralUrl
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Branded Status Card + QR to WhatsApp",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Genesis Referral Link", referralUrl))
                        Toast.makeText(context, "Copied referral link: $referralUrl", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Primary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy QR Link", color = GenesisColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val lead = DiscussionRepository.createAdmissionEnquiry(
                            childName = "Aarav Verma (Referred by ${student.firstName})",
                            parentName = "Vikram Verma",
                            parentPhone = "+91 98490 ${(10000..99999).random()}",
                            targetGrade = student.className,
                            source = "WhatsApp Status QR ($referralCode)"
                        )
                        Toast.makeText(
                            context,
                            "QR Scan Simulated! ${lead.childName} added to Admissions CRM with code $referralCode.",
                            Toast.LENGTH_LONG
                        ).show()
                        onOpenAdmissionsCrm()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate QR Scan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ReferralQrCodeBox(
    seed: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(seed) { buildDeterministicQrMatrix(seed, 21) }
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = matrix.size
            val cellSize = size.width / gridSize.toFloat()
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color(0xFF0D0F14),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.6f, cellSize + 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildDeterministicQrMatrix(seed: String, size: Int = 21): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) }

    fun placeFinderPattern(rowOffset: Int, colOffset: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                grid[rowOffset + r][colOffset + c] = isBorder || isInner
            }
        }
    }

    placeFinderPattern(0, 0)
    placeFinderPattern(0, size - 7)
    placeFinderPattern(size - 7, 0)

    // Timing lines
    for (i in 8 until size - 8) {
        grid[6][i] = i % 2 == 0
        grid[i][6] = i % 2 == 0
    }

    val hashBase = seed.hashCode()
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= size - 8
            val inBottomLeft = r >= size - 8 && c < 8
            if (!inTopLeft && !inTopRight && !inBottomLeft && r != 6 && c != 6) {
                val v = ((r * 37 + c * 19 + hashBase + seed[(r + c) % seed.length].code) and 0xFF)
                grid[r][c] = (v % 2 == 0) || (v % 5 == 0)
            }
        }
    }
    return grid
}

private fun shareProudParentCardBitmapToWhatsApp(
    context: Context,
    student: StudentProfile,
    template: ProudParentCardTemplate,
    referralCode: String,
    referralUrl: String
) {
    try {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // 1. Meteor Black Background + Royal Gold Outer Frame
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0D0F14")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, height - 36f), 42f, 42f, borderPaint)

        // 2. School Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GENESIS SCHOOL BEERAMGUDA", width / 2f, 130f, titlePaint)

        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FCD34D")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PROUD PARENT OFFICIAL ACHIEVEMENT CARD • AY 2026-27", width / 2f, 175f, subHeaderPaint)

        // 3. Achievement Badge Pill
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#451A03")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(140f, 225f, width - 140f, 300f), 40f, 40f, pillPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FDE68A")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("★ ${template.badgeTitle} ★", width / 2f, 273f, badgeTextPaint)

        // 4. Student Name & Metric Highlight
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.fullName, width / 2f, 400f, namePaint)

        val classPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#38BDF8")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${student.classTitle}  •  Roll #${student.rollNo}", width / 2f, 455f, classPaint)

        val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#86EFAC")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(template.metricHighlight, width / 2f, 535f, metricPaint)

        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("\"${template.quoteLine.take(64)}\"", width / 2f, 610f, quotePaint)

        // 5. White Referral QR Card at Bottom
        val whiteBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(80f, 690f, width - 80f, 1240f), 36f, 36f, whiteBoxPaint)

        // Draw QR Matrix on Left of White Box
        val qrMatrix = buildDeterministicQrMatrix(referralCode, 21)
        val qrLeft = 125f
        val qrTop = 745f
        val qrSizePx = 380f
        val cellPx = qrSizePx / qrMatrix.size
        val qrCellPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0D0F14")
            style = Paint.Style.FILL
        }
        for (r in qrMatrix.indices) {
            for (c in qrMatrix[r].indices) {
                if (qrMatrix[r][c]) {
                    canvas.drawRect(
                        qrLeft + c * cellPx,
                        qrTop + r * cellPx,
                        qrLeft + (c + 1) * cellPx + 1f,
                        qrTop + (r + 1) * cellPx + 1f,
                        qrCellPaint
                    )
                }
            }
        }

        // Right-Side Referral Text inside White Box
        val rightX = 545f
        val offerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#B45309")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("₹5,000 SCHOLARSHIP PASS", rightX, 795f, offerPaint)

        val scanTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0D0F14")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Scan QR for Priority", rightX, 855f, scanTitlePaint)
        canvas.drawText("School Admission", rightX, 900f, scanTitlePaint)

        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#2563EB")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Code: $referralCode", rightX, 980f, codePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            textSize = 24f
        }
        canvas.drawText("Admissions Open AY 2026-27", rightX, 1045f, footerPaint)
        canvas.drawText("Beeramguda Main Campus", rightX, 1085f, footerPaint)

        val file = File(context.cacheDir, "proud_parent_card_${student.studentCode}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareCaption = "🏆 Proud Parent Moment at Genesis School Beeramguda!\n" +
            "${student.fullName} (${student.classTitle}) — ${template.metricHighlight}.\n\n" +
            "🎓 Admissions Open for AY 2026-27! Scan the QR on the card or use our Parent Referral Code *$referralCode* ($referralUrl) for a ₹5,000 Fee Scholarship!"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareCaption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Proud Parent Status Card & QR"))
    } catch (e: Exception) {
        Toast.makeText(context, "Sharing referral code $referralCode...", Toast.LENGTH_SHORT).show()
    }
}

// ============================================================================
// 13. ACADEMIC YEAR ROLLOVER, CLASS/SECTION CREATOR & SELECTIVE DATA CONTROL
// ============================================================================

@Composable
fun AcademicYearAndClassManagementScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val availableClasses by DiscussionRepository.availableClassesFlow.collectAsState()
    val classOptions by DiscussionRepository.classOptionsFlow.collectAsState()
    val sectionOptions by DiscussionRepository.sectionOptionsFlow.collectAsState()
    val currentAcademicYear by DiscussionRepository.currentAcademicYear.collectAsState()
    val discussions by DiscussionRepository.discussions.collectAsState()
    val studyNotes by DiscussionRepository.studyNotes.collectAsState()
    val homeworkList by DiscussionRepository.homeworkList.collectAsState()
    val rosterList by DiscussionRepository.sectionStudentRoster.collectAsState()

    // 1. State for Creating New Class & Section (Teacher & Admin)
    var newClassInput by remember { mutableStateOf("Class 10") }
    var newSectionInput by remember { mutableStateOf("D") }

    // 2. State for Academic Year Rollover & Bulk Section Promotion
    var sourceClassId by remember(availableClasses) {
        mutableStateOf(availableClasses.firstOrNull { it.id == "class-9a" }?.id ?: availableClasses.firstOrNull()?.id ?: "class-10a")
    }
    var targetPromoteClass by remember { mutableStateOf("Class 10") }
    var targetPromoteSection by remember { mutableStateOf("A") }
    var targetAcademicYear by remember { mutableStateOf("2027-2028") }

    // 3. State for Selective Section-Wise Data Retention / Cleanup (Admin Control)
    var cleanupClassId by remember(availableClasses) {
        mutableStateOf(availableClasses.firstOrNull { it.id == "class-10a" }?.id ?: availableClasses.firstOrNull()?.id ?: "class-10a")
    }
    var deleteNotesChecked by remember { mutableStateOf(false) } // Default FALSE: Keep reusable study notes for next batch!
    var deleteDoubtsChecked by remember { mutableStateOf(true) } // Default TRUE: Clear previous batch's classroom doubts
    var deleteHomeworkChecked by remember { mutableStateOf(true) } // Default TRUE: Clear previous batch's daily homework
    var deleteRosterChecked by remember { mutableStateOf(false) } // Default FALSE: Keep or promote roster unless resetting

    val selectedCleanupClass = availableClasses.find { it.id == cleanupClassId } ?: availableClasses.firstOrNull()
    val notesCountForSection = studyNotes.count { it.classId == cleanupClassId }
    val doubtsCountForSection = discussions.count { it.classId == cleanupClassId }
    val homeworkCountForSection = homeworkList.count { it.classId == cleanupClassId }
    val rosterCountForSection = rosterList.count { it.classId == cleanupClassId }

    val isAdmin = currentRole == AppRole.ADMIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(
            title = if (isAdmin) "Classes, Promotion & Data Control" else "Create Class & Section",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Overview Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GenesisColors.MeteorBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF1E3A8A),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = if (isAdmin) "🎓 ACADEMIC YEAR & CLASSROOM CONTROL" else "🏫 CLASS & SECTION CREATION",
                                    color = GenesisColors.SkyAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFF14532D),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "AY $currentAcademicYear",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAdmin) "Class Creation, Student Promotion & Section Data Control" else "Create New Class & Section",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAdmin) {
                                "1. Teachers & Admins can create new classes/sections anytime.\n" +
                                "2. Promote an entire section to the upper class in 1 tap when the school year completes.\n" +
                                "3. Selectively keep Study Notes while clearing old Doubts/Homework per section."
                            } else {
                                "Teachers can create new classes or sections anytime. Newly created sections are immediately available for student onboarding and classroom roll call."
                            },
                            color = GenesisColors.SkyAccent,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // =========================================================================
            // MODULE 1: CREATE NEW CLASS & SECTION (TEACHER & ADMIN PERMISSION)
            // =========================================================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = GenesisColors.Primary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "1. Create New Class & Section",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                Text(
                                    text = "Authorized for Teachers & Admins • Live on Student Login",
                                    fontSize = 12.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = newClassInput,
                                onValueChange = { newClassInput = it },
                                label = { Text("Class (e.g. Class 10)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = newSectionInput,
                                onValueChange = { newSectionInput = it },
                                label = { Text("Section Name (e.g. A, Rose, IIT)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Quick Select Class:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            classOptions.forEach { clsName ->
                                SubjectChip(
                                    label = clsName,
                                    selected = newClassInput.equals(clsName, ignoreCase = true),
                                    onClick = { newClassInput = clsName }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Quick Section Suggestions (or type any name above):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("A", "B", "C", "D", "Rose", "Lotus", "IIT", "Alpha").forEach { sec ->
                                SubjectChip(
                                    label = sec,
                                    selected = newSectionInput.equals(sec, ignoreCase = true),
                                    onClick = { newSectionInput = sec }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val created = DiscussionRepository.createNewClassAndSection(
                                    classNameInput = newClassInput,
                                    sectionInput = newSectionInput,
                                    seatCapacity = 40,
                                    scope = coroutineScope
                                )
                                Toast.makeText(
                                    context,
                                    "✅ Created ${created.name}! Students can now select it on login.",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Class & Section (Live for Student Login)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Active School Classes & Sections (${availableClasses.size} total):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableClasses.forEach { schoolClass ->
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Text(
                                        text = schoolClass.name,
                                        color = GenesisColors.Primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // MODULE 2 & 3: ACADEMIC YEAR ROLLOVER, PROMOTION & SELECTIVE DATA CONTROL (ADMIN ONLY)
            // =========================================================================
            if (isAdmin) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GenesisColors.Success)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "2. Academic Year Rollover & Class Promotion",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                Text(
                                    text = "Promote an entire section to upper class or shift sections effortlessly",
                                    fontSize = 12.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val currentSourceClass = availableClasses.find { it.id == sourceClassId } ?: availableClasses.first()
                        ClassSectionDropdownButton(
                            selectedClass = currentSourceClass,
                            availableClasses = availableClasses,
                            onClassSelected = { cls ->
                                sourceClassId = cls.id
                                // Auto-suggest next upper class
                                val currentNum = cls.name.substringBefore("–").replace("Class", "").trim().toIntOrNull()
                                if (currentNum != null && currentNum < 12) {
                                    targetPromoteClass = "Class ${currentNum + 1}"
                                }
                                targetPromoteSection = cls.name.substringAfter("Section ", "A").trim()
                            },
                            label = "Step A — Select Current Class-Section to Promote:"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Step B — Select Target Upper Class, Section & New Academic Year:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = targetPromoteClass,
                                onValueChange = { targetPromoteClass = it },
                                label = { Text("Promote To Class") },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = targetPromoteSection,
                                onValueChange = { targetPromoteSection = it.uppercase().take(2) },
                                label = { Text("Section") },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = targetAcademicYear,
                                onValueChange = { targetAcademicYear = it },
                                label = { Text("Academic Year") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val promotedCount = DiscussionRepository.promoteStudentsToTargetClassSection(
                                    context = context,
                                    fromClassId = sourceClassId,
                                    targetClassNameInput = targetPromoteClass,
                                    targetSectionInput = targetPromoteSection,
                                    newAcademicYear = targetAcademicYear,
                                    scope = coroutineScope
                                )
                                Toast.makeText(
                                    context,
                                    "🎓 Promoted $promotedCount students to $targetPromoteClass – Section $targetPromoteSection ($targetAcademicYear)!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Promote Section Students to $targetPromoteClass-$targetPromoteSection",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = GenesisColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "How Individual Student Shifting Works: Teachers can also shift a single student anytime via the top Profile icon ('Edit Student'), or students can tap 'New Academic Year / Promoted? Select New Class & Section' on their profile card.",
                                    fontSize = 11.sp,
                                    color = GenesisColors.TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // MODULE 3: ADMIN SELECTIVE SECTION-WISE DATA RETENTION / DELETION
            // =========================================================================
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFD97706))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "3. Admin Selective Section-Wise Data Control",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                Text(
                                    text = "Keep reusable Study Notes or selectively delete old Doubts & Homework per section",
                                    fontSize = 12.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val currentCleanupClass = selectedCleanupClass ?: availableClasses.firstOrNull()
                        if (currentCleanupClass != null) {
                            ClassSectionDropdownButton(
                                selectedClass = currentCleanupClass,
                                availableClasses = availableClasses,
                                onClassSelected = { cls -> cleanupClassId = cls.id },
                                label = "Select Class-Section to Manage Data:"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select What to Keep vs Delete in ${selectedCleanupClass?.name ?: "Selected Section"}:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 1: Study Notes (Recommended to Keep)
                        SelectiveDataCheckRow(
                            title = "Study Notes & Chapter PDFs ($notesCountForSection items)",
                            subtitle = if (deleteNotesChecked) "Will be DELETED from this section" else "✅ KEPT for incoming batch (Recommended)",
                            checkedForDeletion = deleteNotesChecked,
                            onCheckedChange = { deleteNotesChecked = it }
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Option 2: Classroom Doubts & Discussions
                        SelectiveDataCheckRow(
                            title = "Previous Batch Classroom Doubts ($doubtsCountForSection items)",
                            subtitle = if (deleteDoubtsChecked) "🗑️ Will be DELETED so new batch starts with a clean doubt feed" else "KEPT in section doubt feed",
                            checkedForDeletion = deleteDoubtsChecked,
                            onCheckedChange = { deleteDoubtsChecked = it }
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Option 3: Daily Homework Assignments
                        SelectiveDataCheckRow(
                            title = "Previous Batch Daily Homework ($homeworkCountForSection items)",
                            subtitle = if (deleteHomeworkChecked) "🗑️ Will be DELETED for the new academic session" else "KEPT in section homework list",
                            checkedForDeletion = deleteHomeworkChecked,
                            onCheckedChange = { deleteHomeworkChecked = it }
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Option 4: Section Student Roster
                        SelectiveDataCheckRow(
                            title = "Section Student Verification Roster ($rosterCountForSection students)",
                            subtitle = if (deleteRosterChecked) "🗑️ Will CLEAR section student list" else "✅ KEPT (use Promotion above to move students to upper class)",
                            checkedForDeletion = deleteRosterChecked,
                            onCheckedChange = { deleteRosterChecked = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (!deleteNotesChecked && !deleteDoubtsChecked && !deleteHomeworkChecked && !deleteRosterChecked) {
                                    Toast.makeText(
                                        context,
                                        "All items are set to KEEP — check at least one category to delete.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                val summary = DiscussionRepository.selectiveCleanupSectionData(
                                    classId = cleanupClassId,
                                    deleteNotes = deleteNotesChecked,
                                    deleteDoubts = deleteDoubtsChecked,
                                    deleteHomework = deleteHomeworkChecked,
                                    deleteStudentRoster = deleteRosterChecked,
                                    scope = coroutineScope
                                )
                                Toast.makeText(
                                    context,
                                    "🧹 Section Cleanup Complete: $summary",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentRole == AppRole.ADMIN) Color(0xFFB45309) else GenesisColors.Primary
                            )
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apply Selective Data Control for ${selectedCleanupClass?.name ?: "Section"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun SelectiveDataCheckRow(
    title: String,
    subtitle: String,
    checkedForDeletion: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = if (checkedForDeletion) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (checkedForDeletion) Color(0xFFFECACA) else Color(0xFFBBF7D0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checkedForDeletion) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenesisColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (checkedForDeletion) GenesisColors.Emergency else GenesisColors.Success,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Checkbox(
                checked = checkedForDeletion,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
