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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.schoolapp.android.data.*
import com.schoolapp.android.ui.theme.GenesisColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ============================================================================
// 1. ATTENDANCE TRACKER & TEACHER 1-TAP ROLL CALL
// ============================================================================

@Composable
fun AttendanceHubScreen(
    student: StudentProfile,
    currentRole: AppRole,
    selectedClass: SchoolClass,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val availableClasses by DiscussionRepository.availableClassesFlow.collectAsState()
    val globalSelectedClass by DiscussionRepository.selectedClass.collectAsState()
    var currentClass by remember(globalSelectedClass.id) { mutableStateOf(globalSelectedClass) }

    val roster by DiscussionRepository.rosterAttendance.collectAsState()
    val submittedAttendanceMap by DiscussionRepository.submittedClassesAttendance.collectAsState()
    val isStaff = currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN

    var showConfirmDialog by remember { mutableStateOf(false) }

    val classRoster = remember(roster, currentClass.id) {
        val filtered = roster.filter { it.classId == currentClass.id }
        if (filtered.isNotEmpty()) filtered else {
            listOf(
                RosterAttendanceStudent("r-${currentClass.id}-1", "01", "Aditya Narayan", "+91 98490 12345", "Present", currentClass.id),
                RosterAttendanceStudent("r-${currentClass.id}-2", "02", "Divya Spandana", "+91 97011 54321", "Present", currentClass.id),
                RosterAttendanceStudent("r-${currentClass.id}-3", "03", "Karan Malhotra", "+91 99881 22334", "Present", currentClass.id),
                RosterAttendanceStudent("r-${currentClass.id}-4", "04", "Sneha Kulkarni", "+91 90112 33445", "Absent", currentClass.id)
            )
        }
    }

    val presentCount = classRoster.count { it.status == "Present" }
    val absentCount = classRoster.count { it.status == "Absent" }
    val leaveCount = classRoster.count { it.status == "Leave" || it.status == "Late" }
    val absentStudents = classRoster.filter { it.status == "Absent" }
    val submissionTime = submittedAttendanceMap[currentClass.id]

    if (showConfirmDialog) {
        Dialog(onDismissRequest = { showConfirmDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = GenesisColors.Primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Confirm Attendance Submission",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = GenesisColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to submit attendance for ${currentClass.name}?",
                        fontSize = 13.sp,
                        color = GenesisColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Students:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Text("${classRoster.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Present:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Text("$presentCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Success)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Absent:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Text(
                                    "$absentCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (absentCount > 0) GenesisColors.Emergency else GenesisColors.TextPrimary
                                )
                            }
                            if (absentStudents.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = GenesisColors.Border, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Absent: " + absentStudents.joinToString(", ") { "${it.studentName} (#${it.rollNo})" },
                                    fontSize = 11.sp,
                                    color = GenesisColors.Emergency,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Reminder: Once submitted, attendance is saved for today and parent records will be updated.",
                        fontSize = 11.sp,
                        color = GenesisColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                DiscussionRepository.submitClassAttendance(currentClass.id)
                                showConfirmDialog = false
                                Toast.makeText(
                                    context,
                                    "Attendance for ${currentClass.name} submitted successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary),
                            modifier = Modifier.weight(1.3f).height(44.dp)
                        ) {
                            Text("Confirm & Submit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            title = if (isStaff) "Class Roll Call (${currentClass.name})" else "Attendance Record",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (isStaff) {
                // Top Class & Section Selector Dropdown Button
                item {
                    ClassSectionDropdownButton(
                        selectedClass = currentClass,
                        availableClasses = availableClasses,
                        onClassSelected = { cls ->
                            currentClass = cls
                            DiscussionRepository.switchClass(cls, coroutineScope)
                        },
                        label = "Class & Section for Attendance"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Class Attendance Register Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CLASS ATTENDANCE REGISTER",
                                        color = GenesisColors.SkyAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentClass.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (submissionTime != null) {
                                    Surface(
                                        color = Color(0xFF14532D),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = "✓ Submitted ($submissionTime)",
                                            color = Color(0xFF86EFAC),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AttendanceMetricPill("Present", presentCount.toString(), Color(0xFF22C55E))
                                AttendanceMetricPill("Absent", absentCount.toString(), Color(0xFFEF4444))
                                AttendanceMetricPill("Leave", leaveCount.toString(), Color(0xFFF59E0B))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    DiscussionRepository.markAllPresentInClass(currentClass.id)
                                    Toast.makeText(
                                        context,
                                        "Marked all ${classRoster.size} students in ${currentClass.name} Present.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark All Present", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Student Roll Call (${classRoster.size} Students)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(classRoster, key = { it.id }) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = entry.rollNo,
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(entry.parentPhone, fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    Triple("P", "Present", GenesisColors.Success),
                                    Triple("A", "Absent", GenesisColors.Emergency),
                                    Triple("Lv", "Leave", GenesisColors.Warning)
                                ).forEach { (shortCode, statusValue, activeColor) ->
                                    val selected = entry.status == statusValue
                                    Surface(
                                        color = if (selected) activeColor else Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (selected) activeColor else GenesisColors.Border),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable {
                                                DiscussionRepository.markStudentAttendance(entry.id, statusValue)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = shortCode,
                                                color = if (selected) Color.White else GenesisColors.TextSecondary,
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

                // Bottom Re-verification of Absent Students (Replaces recent changes audit log)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (absentStudents.isNotEmpty()) Color(0xFFFFF1F2) else Color(0xFFF0FDF4)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (absentStudents.isNotEmpty()) Color(0xFFFECDD3) else Color(0xFFBBF7D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (absentStudents.isNotEmpty()) Icons.Default.EventBusy else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (absentStudents.isNotEmpty()) GenesisColors.Emergency else GenesisColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (absentStudents.isNotEmpty()) "Absent Students (${absentStudents.size}) — Reverify Before Submit" else "Zero Absentees Today",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (absentStudents.isNotEmpty()) Color(0xFF9F1239) else Color(0xFF166534)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (absentStudents.isNotEmpty())
                                    "Please check the absent students below. If any student is present, tap 'Mark Present' before submitting."
                                else
                                    "All ${classRoster.size} students in ${currentClass.name} are marked Present ✓",
                                fontSize = 12.sp,
                                color = if (absentStudents.isNotEmpty()) Color(0xFFBE123C) else Color(0xFF15803D)
                            )
                            if (absentStudents.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    absentStudents.forEach { absentStudent ->
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Surface(
                                                        color = Color(0xFFFFE4E6),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = absentStudent.rollNo,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                                color = GenesisColors.Emergency
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = absentStudent.studentName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = GenesisColors.TextPrimary
                                                        )
                                                        Text(
                                                            text = "Parent: ${absentStudent.parentPhone}",
                                                            fontSize = 11.sp,
                                                            color = GenesisColors.TextSecondary
                                                        )
                                                    }
                                                }
                                                TextButton(
                                                    onClick = {
                                                        DiscussionRepository.markStudentAttendance(absentStudent.id, "Present")
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Mark Present",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GenesisColors.Success
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            showConfirmDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Attendance for ${currentClass.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // Student / Parent Attendance View with Full-Year Interactive Calendar
                item {
                    val yearlyAttendance by DiscussionRepository.studentYearlyAttendance.collectAsState()
                    val allHolidays by DiscussionRepository.schoolHolidays.collectAsState()
                    val studentHolidays = remember(allHolidays, student.classId, student.className) {
                        allHolidays.filter {
                            DiscussionRepository.isHolidayForStudentOrClass(it, student.classId, student.className)
                        }.associateBy { it.dateIso }
                    }

                    val academicYearMonths = remember {
                        (0..11).map { offset -> YearMonth.of(2026, 6).plusMonths(offset.toLong()) }
                    }
                    var currentMonthIndex by remember { mutableIntStateOf(5) } // Default: Nov 2026
                    val activeMonth = academicYearMonths[currentMonthIndex]
                    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.of(2026, 11, 10)) }

                    val presentDays = yearlyAttendance.values.count { it == "Present" }
                    val absentDays = yearlyAttendance.values.count { it == "Absent" }
                    val totalDays = (presentDays + absentDays).coerceAtLeast(1)
                    val attendancePct = (presentDays * 100.0) / totalDays

                    Card(
                        colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${student.fullName} • ${student.classTitle}",
                                        color = Color(0xFFBFDBFE),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "%.1f%%".format(attendancePct),
                                        color = Color.White,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Overall Attendance",
                                        color = Color(0xFFDBEAFE),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Surface(
                                    color = if (attendancePct >= 75.0) Color(0xFF14532D) else Color(0xFF7F1D1D),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = if (attendancePct >= 75.0) "Regular" else "Low (<75%)",
                                        color = if (attendancePct >= 75.0) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AttendanceMetricPill("Total Days", totalDays.toString(), Color(0xFF38BDF8))
                                AttendanceMetricPill("Present", presentDays.toString(), Color(0xFF22C55E))
                                AttendanceMetricPill("Absent", absentDays.toString(), Color(0xFFF59E0B))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Full-Year Month Quick Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        academicYearMonths.forEachIndexed { idx, ym ->
                            val isChosen = idx == currentMonthIndex
                            val label = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} '${ym.year % 100}"
                            Surface(
                                color = if (isChosen) GenesisColors.Primary else Color.White,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, if (isChosen) GenesisColors.Primary else GenesisColors.Border),
                                modifier = Modifier.clickable { currentMonthIndex = idx }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isChosen) Color.White else GenesisColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Monthly Attendance Calendar Card (Transparent Green / Red Bubbles)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (currentMonthIndex > 0) currentMonthIndex-- },
                                    enabled = currentMonthIndex > 0
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = if (currentMonthIndex > 0) GenesisColors.Primary else GenesisColors.Border
                                    )
                                }
                                Text(
                                    text = "${activeMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${activeMonth.year}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                IconButton(
                                    onClick = { if (currentMonthIndex < academicYearMonths.lastIndex) currentMonthIndex++ },
                                    enabled = currentMonthIndex < academicYearMonths.lastIndex
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = if (currentMonthIndex < academicYearMonths.lastIndex) GenesisColors.Primary else GenesisColors.Border
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            AcademicMonthGrid(
                                month = activeMonth,
                                selectedDate = selectedDate,
                                onSelectDate = { selectedDate = it },
                                dayStyleProvider = { date ->
                                    val iso = date.toString()
                                    val attStatus = yearlyAttendance[iso]
                                    val isHoliday = studentHolidays.containsKey(iso) || date.dayOfWeek == DayOfWeek.SUNDAY
                                    when (attStatus) {
                                        "Present" -> CalendarDayVisual(
                                            bgColor = Color(0xFF16A34A).copy(alpha = 0.22f),
                                            borderColor = Color(0xFF16A34A).copy(alpha = 0.65f),
                                            textColor = Color(0xFF14532D)
                                        )
                                        "Absent" -> CalendarDayVisual(
                                            bgColor = Color(0xFFDC2626).copy(alpha = 0.22f),
                                            borderColor = Color(0xFFDC2626).copy(alpha = 0.65f),
                                            textColor = Color(0xFF991B1B)
                                        )
                                        else -> if (isHoliday) {
                                            CalendarDayVisual(
                                                bgColor = Color(0xFFF1F5F9),
                                                borderColor = Color.Transparent,
                                                textColor = GenesisColors.TextSecondary
                                            )
                                        } else {
                                            CalendarDayVisual(
                                                bgColor = Color.Transparent,
                                                borderColor = Color.Transparent,
                                                textColor = GenesisColors.TextPrimary
                                            )
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CalendarLegendDot("Present", Color(0xFF16A34A).copy(alpha = 0.30f), Color(0xFF15803D))
                                Spacer(modifier = Modifier.width(18.dp))
                                CalendarLegendDot("Absent", Color(0xFFDC2626).copy(alpha = 0.30f), Color(0xFFB91C1C))
                                Spacer(modifier = Modifier.width(18.dp))
                                CalendarLegendDot("Off / Upcoming", Color(0xFFF1F5F9), GenesisColors.TextSecondary)
                            }
                        }
                    }

                    // Selected Date Details Pill
                    selectedDate?.let { picked ->
                        Spacer(modifier = Modifier.height(12.dp))
                        val iso = picked.toString()
                        val attStatus = yearlyAttendance[iso]
                        val holiday = studentHolidays[iso]
                        val statusLabel = when {
                            attStatus == "Present" -> "Present • Morning Roll Call"
                            attStatus == "Absent" -> "Absent"
                            holiday != null -> "Holiday • ${holiday.title}"
                            picked.dayOfWeek == DayOfWeek.SUNDAY -> "Sunday (Weekly Off)"
                            else -> "Scheduled School Day"
                        }
                        val pillBg = when (attStatus) {
                            "Present" -> Color(0xFF16A34A).copy(alpha = 0.14f)
                            "Absent" -> Color(0xFFDC2626).copy(alpha = 0.14f)
                            else -> Color.White
                        }
                        val pillTextColor = when (attStatus) {
                            "Present" -> Color(0xFF14532D)
                            "Absent" -> Color(0xFF991B1B)
                            else -> GenesisColors.TextPrimary
                        }
                        Surface(
                            color = pillBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = picked.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                Text(
                                    text = statusLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = pillTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CalendarDayVisual(
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color
)

@Composable
private fun AcademicMonthGrid(
    month: YearMonth,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    dayStyleProvider: (LocalDate) -> CalendarDayVisual
) {
    val weekHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(modifier = Modifier.fillMaxWidth()) {
        weekHeaders.forEach { day ->
            Text(
                text = day,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GenesisColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    val firstOfMonth = month.atDay(1)
    val leadingBlanks = firstOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ..., Saturday = 6
    val totalDays = month.lengthOfMonth()
    val cells = buildList<LocalDate?> {
        repeat(leadingBlanks) { add(null) }
        for (d in 1..totalDays) add(month.atDay(d))
        while (size % 7 != 0) add(null)
    }

    cells.chunked(7).forEach { week ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
        ) {
            week.forEach { date ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (date != null) {
                        val visual = dayStyleProvider(date)
                        val isSelected = selectedDate == date
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(visual.bgColor)
                                .border(
                                    width = if (isSelected) 2.dp else if (visual.borderColor != Color.Transparent) 1.dp else 0.dp,
                                    color = if (isSelected) GenesisColors.Primary else visual.borderColor,
                                    shape = CircleShape
                                )
                                .clickable { onSelectDate(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || visual.bgColor != Color.Transparent) FontWeight.Bold else FontWeight.Medium,
                                color = visual.textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendDot(label: String, bubbleColor: Color, borderColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(bubbleColor)
                .border(1.dp, borderColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AttendanceMetricPill(label: String, value: String, valueColor: Color) {
    Surface(
        color = Color(0xFF172554),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFFDBEAFE), fontSize = 11.sp)
        }
    }
}

// ============================================================================
// 2. EXAM RESULTS & REPORT CARD (MARKS & PERCENTAGE ONLY + CLASS SUBJECT MANAGER)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamResultsReportCardScreen(
    student: StudentProfile,
    onOpenProudParentQr: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val classSubjectsMap by DiscussionRepository.classSubjectsMap.collectAsState()
    val resultsByExam by DiscussionRepository.examResultsByExam.collectAsState()
    val studentExamSheets by DiscussionRepository.studentExamSheets.collectAsState()
    val availableExams by DiscussionRepository.availableExamsFlow.collectAsState()

    var selectedExam by remember { mutableStateOf("Half-Yearly Exam") }
    var examMenuExpanded by remember { mutableStateOf(false) }

    val isStaff = currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN
    val isExamApproved = remember(studentExamSheets, student.id, selectedExam) {
        val matching = studentExamSheets.find {
            (it.studentId == student.id || it.studentId == "stu-sahasra") && it.examName == selectedExam
        }
        matching?.isApproved ?: true
    }
    val targetClassId = if (isStaff) selectedClass.id else student.classId
    val targetClassTitle = if (isStaff) selectedClass.name else student.classTitle
    val activeClassSubjects = classSubjectsMap[targetClassId]
        ?: DiscussionRepository.getSubjectsForClass(targetClassId)

    val examRawResults = resultsByExam[selectedExam].orEmpty()
    // Filter results to the subjects configured for this class & section
    val results = remember(examRawResults, activeClassSubjects, selectedExam) {
        val defaultMax = if (selectedExam.startsWith("Unit Test")) 25 else 100
        val defaultObt = if (selectedExam.startsWith("Unit Test")) 22 else 88
        activeClassSubjects.map { subName ->
            examRawResults.find { it.subject.equals(subName, ignoreCase = true) }
                ?: SubjectExamResult(
                    subject = subName,
                    examName = selectedExam,
                    marksObtained = defaultObt,
                    maxMarks = defaultMax,
                    grade = "",
                    teacherRemark = ""
                )
        }
    }

    val totalObtained = results.sumOf { it.marksObtained }
    val totalMax = results.sumOf { it.maxMarks }.coerceAtLeast(1)
    val percentage = (totalObtained * 100.0) / totalMax

    var showEnterMarksDialog by remember { mutableStateOf(false) }
    var showManageSubjectsDialog by remember { mutableStateOf(false) }
    var selectedSubjectForEntry by remember { mutableStateOf("Mathematics") }
    var marksInput by remember { mutableStateOf("95") }
    var maxMarksInput by remember { mutableStateOf("100") }
    var newSubjectInput by remember { mutableStateOf("") }

    if (showEnterMarksDialog) {
        Dialog(onDismissRequest = { showEnterMarksDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Update Subject Marks", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$selectedSubjectForEntry • $targetClassTitle",
                        fontSize = 13.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = marksInput,
                            onValueChange = { marksInput = it },
                            label = { Text("Marks Obtained") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxMarksInput,
                            onValueChange = { maxMarksInput = it },
                            label = { Text("Max Marks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showEnterMarksDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary)
                        }
                        Button(
                            onClick = {
                                val obt = marksInput.toIntOrNull() ?: 90
                                val max = maxMarksInput.toIntOrNull() ?: 100
                                DiscussionRepository.enterOrUpdateSubjectMarks(
                                    subject = selectedSubjectForEntry,
                                    examName = selectedExam,
                                    marksObtained = obt,
                                    maxMarks = max
                                )
                                showEnterMarksDialog = false
                                Toast.makeText(
                                    context,
                                    "Saved $selectedExam • $selectedSubjectForEntry: $obt / $max",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Save Marks", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

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
                        text = targetClassTitle,
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
                                    val added = DiscussionRepository.addSubjectToClass(targetClassId, clean)
                                    if (added) {
                                        newSubjectInput = ""
                                        Toast.makeText(context, "Added $clean to $targetClassTitle", Toast.LENGTH_SHORT).show()
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
                        text = "Active Subjects (${activeClassSubjects.size})",
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
                                            DiscussionRepository.removeSubjectFromClass(targetClassId, subjectName)
                                            Toast.makeText(context, "Removed $subjectName from $targetClassTitle", Toast.LENGTH_SHORT).show()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Exam Results", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Exam Selector Dropdown (Unit Test 1, Unit Test 2, Quarterly, Half-Yearly, Final Annual)
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GenesisColors.Primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { examMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Selected Exam",
                                    fontSize = 11.sp,
                                    color = GenesisColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = selectedExam,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.Primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Exam",
                                tint = GenesisColors.Primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = examMenuExpanded,
                        onDismissRequest = { examMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        availableExams.forEach { examTitle ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = examTitle,
                                        fontWeight = if (examTitle == selectedExam) FontWeight.Bold else FontWeight.Medium,
                                        color = if (examTitle == selectedExam) GenesisColors.Primary else GenesisColors.TextPrimary
                                    )
                                },
                                trailingIcon = {
                                    if (examTitle == selectedExam) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    selectedExam = examTitle
                                    examMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!isStaff && !isExamApproved) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFFEF3C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Results Awaiting Approval",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$selectedExam marks are being verified and will appear automatically once approved by your Class Teacher.",
                                fontSize = 13.sp,
                                color = GenesisColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$selectedExam • $targetClassTitle",
                                    color = Color(0xFFBFDBFE),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "%.1f%%".format(percentage),
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Total Marks: $totalObtained / $totalMax",
                                    color = Color(0xFFDBEAFE),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))



                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val reportLines = mutableListOf(
                                        "SUBJECT                          MARKS OBTAINED        PERCENTAGE",
                                        "-----------------------------------------------------------------"
                                    )
                                    results.forEach { r ->
                                        val paddedSub = r.subject.padEnd(32, ' ')
                                        val paddedScore = "${r.marksObtained} / ${r.maxMarks}".padEnd(21, ' ')
                                        val subPct = (r.marksObtained * 100) / r.maxMarks.coerceAtLeast(1)
                                        reportLines.add("$paddedSub $paddedScore $subPct%")
                                    }
                                    reportLines.add("-----------------------------------------------------------------")
                                    reportLines.add("TOTAL MARKS: $totalObtained / $totalMax      |      PERCENTAGE: ${"%.1f".format(percentage)}%")

                                    val saved = generateAndSaveSchoolPdf(
                                        context = context,
                                        fileNameBase = "Genesis_ReportCard_${student.studentCode}",
                                        documentCategory = "Official Academic Report Card • AY 2026-27",
                                        documentTitle = "$selectedExam Examination Results",
                                        subtitleLine = "${student.fullName} (ID: ${student.studentCode})  |  $targetClassTitle",
                                        metaLines = listOf(
                                            "Parent Contact: ${student.parentPhone}"
                                        ),
                                        bodyLines = reportLines
                                    )
                                    if (saved != null) {
                                        Toast.makeText(context, "Report Card saved to Downloads/${saved.fileName}", Toast.LENGTH_LONG).show()
                                        openPdfUri(context, saved.openableUri)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download PDF", color = GenesisColors.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = onOpenProudParentQr,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subject-Wise Marks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GenesisColors.TextPrimary
                    )
                    if (isStaff) {
                        Text(
                            text = "Tap subject to edit marks",
                            fontSize = 12.sp,
                            color = GenesisColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(results, key = { it.subject }) { item ->
                val subPercent = (item.marksObtained * 100) / item.maxMarks.coerceAtLeast(1)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = isStaff) {
                            selectedSubjectForEntry = item.subject
                            marksInput = item.marksObtained.toString()
                            maxMarksInput = item.maxMarks.toString()
                            showEnterMarksDialog = true
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.subject,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${item.marksObtained} / ${item.maxMarks}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$subPercent%",
                                        color = GenesisColors.Primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (item.marksObtained / item.maxMarks.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = GenesisColors.Primary,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
            }
        }
    }
}

// ============================================================================
// 3. DIGITAL STUDENT ID CARD & EMERGENCY PARENT CALL PASS
// ============================================================================

@Composable
fun DigitalStudentIdCardScreen(
    student: StudentProfile,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Digital Student ID & Gate Pass", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Meteor Black ID Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(GenesisColors.MeteorBlack, Color(0xFF1E293B))
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GENESIS SCHOOL BEERAMGUDA",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "OFFICIAL DIGITAL IDENTITY CARD • AY 2026-27",
                                color = GenesisColors.SkyAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            AsyncImage(
                                model = student.avatarUrl,
                                contentDescription = student.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, GenesisColors.SkyAccent, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = student.fullName,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = GenesisColors.Primary,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "${student.classTitle}  •  ID #${student.studentCode}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        IdDetailRow("Classroom Status", "🔒 Verified & Locked")
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = GenesisColors.Border)
                        IdDetailRow("Parent / Emergency Contact", student.parentPhone)
                        if (student.parentEmail.isNotBlank()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = GenesisColors.Border)
                            IdDetailRow("Parent Email", student.parentEmail)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = GenesisColors.Border)
                        IdDetailRow("Bus Transport Route", student.busRoute)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = GenesisColors.Border)
                        IdDetailRow("Residential Address", student.address)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.parentPhone}"))
                                context.startActivity(dialIntent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1-Tap Call Parent (${student.parentPhone})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, color = GenesisColors.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================================================
// 4. ASSIGNMENTS & PROJECTS (DEADLINE INDICATOR, ATTACHMENTS & TEACHER CONTROLS)
// ============================================================================

private fun calculateDeadlineIndicator(deadlineIso: String, dueText: String): Pair<String, Color> {
    try {
        val parts = deadlineIso.split("-")
        if (parts.size == 3) {
            val targetCal = java.util.Calendar.getInstance()
            targetCal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 23, 59, 59)
            val nowCal = java.util.Calendar.getInstance()
            val diffMs = targetCal.timeInMillis - nowCal.timeInMillis
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            return when {
                diffDays < 0 -> Pair("❌ Overdue", Color(0xFFDC2626))
                diffDays == 0 -> Pair("🚨 Due Today", Color(0xFFEA580C))
                diffDays == 1 -> Pair("⏳ Due Tomorrow", Color(0xFFD97706))
                else -> Pair("⏳ $diffDays days left", Color(0xFF2563EB))
            }
        }
    } catch (_: Exception) {}
    return if (dueText.contains("Tomorrow", ignoreCase = true)) {
        Pair("⏳ Due Tomorrow", Color(0xFFD97706))
    } else if (dueText.contains("Today", ignoreCase = true)) {
        Pair("🚨 Due Today", Color(0xFFEA580C))
    } else {
        Pair("⏳ $dueText", Color(0xFF2563EB))
    }
}

@Composable
fun AssignmentsAndProjectsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val homeworkList by DiscussionRepository.homeworkList.collectAsState()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val availableClasses by DiscussionRepository.availableClassesFlow.collectAsState()

    var selectedTab by remember { mutableStateOf("All") } // "All", "Assignments", "Projects"
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var currentClassFilter by remember { mutableStateOf(selectedClass.id) }

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var showModifyDialog by remember { mutableStateOf<HomeworkItem?>(null) }
    var showDeadlineDialog by remember { mutableStateOf<HomeworkItem?>(null) }

    val isTeacherOrAdmin = currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN

    // Filtered items
    val filteredList = remember(homeworkList, currentClassFilter, selectedTab) {
        homeworkList.filter { item ->
            val matchClass = item.classId.equals(currentClassFilter, ignoreCase = true) || item.classId.isBlank()
            val matchTab = when (selectedTab) {
                "Assignments" -> item.projectType.equals("Assignment", ignoreCase = true)
                "Projects" -> item.projectType.equals("Project", ignoreCase = true)
                else -> true
            }
            matchClass && matchTab
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Assignments & Projects", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Class Selector & Tabs Header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Class & Section:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.TextSecondary
                            )
                            Box {
                                val currentClassObj = availableClasses.find { it.id.equals(currentClassFilter, ignoreCase = true) }
                                    ?: selectedClass
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { classDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentClassObj.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GenesisColors.TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = GenesisColors.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = classDropdownExpanded,
                                    onDismissRequest = { classDropdownExpanded = false }
                                ) {
                                    availableClasses.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.name) },
                                            onClick = {
                                                currentClassFilter = opt.id
                                                classDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filter Tabs: All, Assignments, Projects
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Assignments", "Projects").forEach { tab ->
                                val selected = selectedTab == tab
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    label = { Text(tab, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GenesisColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF8FAFC),
                                        labelColor = GenesisColors.TextSecondary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (selected) GenesisColors.Primary else GenesisColors.Border)
                                )
                            }
                        }
                    }
                }
            }

            // Teacher Action: Add Assignment / Project
            if (isTeacherOrAdmin) {
                item {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Assignment / Project", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Items List
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No $selectedTab Scheduled", fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No pending items for this class. Everything is up-to-date!", color = GenesisColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    val (indicatorText, indicatorColor) = calculateDeadlineIndicator(item.deadlineDateIso, item.dueText)
                    val submissionPct = if (item.totalStudents > 0) (item.submittedCount * 100) / item.totalStudents else 0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Subject chip, Type badge, Deadline indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = item.subject,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GenesisColors.Primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    val isProject = item.projectType.equals("Project", ignoreCase = true)
                                    Surface(
                                        color = if (isProject) Color(0xFFFCE7F3) else Color(0xFFEDE9FE),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = item.projectType,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProject) Color(0xFFBE185D) else Color(0xFF6D28D9),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Deadline Indicator Chip
                                Surface(
                                    color = indicatorColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = indicatorText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = indicatorColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Title & Description
                            Text(
                                text = item.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.TextPrimary
                            )
                            if (item.instructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.instructions,
                                    fontSize = 13.sp,
                                    color = GenesisColors.TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Due Date Text & Max Marks
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(item.dueText, fontSize = 12.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                }
                                Text("Max Marks: ${item.maxMarks}", fontSize = 12.sp, color = GenesisColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            }

                            // Attached Document Section
                            if (!item.attachedDocName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.attachedDocName ?: "Assignment.pdf",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = GenesisColors.TextPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = item.attachedDocSize ?: "1.2 MB",
                                                    fontSize = 11.sp,
                                                    color = GenesisColors.TextSecondary
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                Toast.makeText(context, "Opening ${item.attachedDocName}...", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Submission Status Progress Bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Submissions: ${item.submittedCount}/${item.totalStudents} students",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Text(
                                        text = "$submissionPct%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GenesisColors.Primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { submissionPct / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = GenesisColors.Primary,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Bottom Actions Row
                            if (isTeacherOrAdmin) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showDeadlineDialog = item },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Modify Deadline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showModifyDialog = item },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                if (item.isCompleted) {
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GenesisColors.Success, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Assignment Submitted Successfully", color = GenesisColors.Success, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            DiscussionRepository.submitStudentHomework(item.id)
                                            Toast.makeText(context, "Submitted ${item.title}!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Submit Assignment", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Modify Deadline Date
    showDeadlineDialog?.let { targetItem ->
        var deadlineInput by remember { mutableStateOf(targetItem.deadlineDateIso) }
        var dueTextInput by remember { mutableStateOf(targetItem.dueText) }

        Dialog(onDismissRequest = { showDeadlineDialog = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Modify Deadline Date", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(targetItem.title, fontSize = 13.sp, color = GenesisColors.TextSecondary, maxLines = 1)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Quick Extension:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("+2 Days", "2026-10-22", "Due 22 Oct 2026, 08:30 AM"),
                            Triple("+5 Days", "2026-10-25", "Due 25 Oct 2026, 08:30 AM"),
                            Triple("+1 Week", "2026-10-27", "Due 27 Oct 2026, 08:30 AM")
                        ).forEach { (label, iso, dueStr) ->
                            OutlinedButton(
                                onClick = {
                                    deadlineInput = iso
                                    dueTextInput = dueStr
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deadlineInput,
                        onValueChange = { deadlineInput = it },
                        label = { Text("Deadline ISO Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dueTextInput,
                        onValueChange = { dueTextInput = it },
                        label = { Text("Display Due Text") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeadlineDialog = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                DiscussionRepository.modifyHomeworkDeadline(targetItem.id, deadlineInput.trim(), dueTextInput.trim())
                                Toast.makeText(context, "Deadline updated!", Toast.LENGTH_SHORT).show()
                                showDeadlineDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Save Deadline", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add / Modify Assignment or Project
    val editingItem = showModifyDialog
    if (showAddDialog || editingItem != null) {
        var title by remember { mutableStateOf(editingItem?.title ?: "") }
        var subject by remember { mutableStateOf(editingItem?.subject ?: "Mathematics") }
        var projectType by remember { mutableStateOf(editingItem?.projectType ?: "Assignment") }
        var deadlineIso by remember { mutableStateOf(editingItem?.deadlineDateIso ?: "2026-10-25") }
        var dueText by remember { mutableStateOf(editingItem?.dueText ?: "Due 25 Oct 2026, 08:30 AM") }
        var instructions by remember { mutableStateOf(editingItem?.instructions ?: "") }
        var maxMarksStr by remember { mutableStateOf((editingItem?.maxMarks ?: 20).toString()) }
        var attachedDocName by remember { mutableStateOf(editingItem?.attachedDocName ?: "Worksheet_Guide.pdf") }

        Dialog(onDismissRequest = {
            showAddDialog = false
            showModifyDialog = null
        }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (editingItem != null) "Edit Assignment Details" else "Add New Assignment / Project",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Type Selector
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Assignment", "Project").forEach { type ->
                            val sel = projectType == type
                            FilterChip(
                                selected = sel,
                                onClick = { projectType = type },
                                label = { Text(type, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject (e.g. Mathematics, Physics)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deadlineIso,
                            onValueChange = { deadlineIso = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxMarksStr,
                            onValueChange = { maxMarksStr = it },
                            label = { Text("Max Marks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dueText,
                        onValueChange = { dueText = it },
                        label = { Text("Due Date Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = attachedDocName,
                        onValueChange = { attachedDocName = it },
                        label = { Text("Attached Document Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions / Details") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            showAddDialog = false
                            showModifyDialog = null
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val maxM = maxMarksStr.toIntOrNull() ?: 20
                                val newItem = HomeworkItem(
                                    id = editingItem?.id ?: "hw-${System.currentTimeMillis()}",
                                    classId = currentClassFilter,
                                    subject = subject.trim(),
                                    title = title.trim(),
                                    dueText = dueText.trim(),
                                    status = "Pending",
                                    instructions = instructions.trim(),
                                    maxMarks = maxM,
                                    deadlineDateIso = deadlineIso.trim(),
                                    totalStudents = editingItem?.totalStudents ?: 40,
                                    submittedCount = editingItem?.submittedCount ?: 0,
                                    attachedDocName = attachedDocName.trim().ifBlank { null },
                                    attachedDocSize = "1.5 MB",
                                    projectType = projectType
                                )
                                DiscussionRepository.addOrUpdateHomework(newItem)
                                Toast.makeText(context, if (editingItem != null) "Assignment updated!" else "Assignment added!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                showModifyDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text(if (editingItem != null) "Update" else "Create", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4B. EXAMINATION SCHEDULE (DATES, SUBJECTS, SYLLABUS & PREPARATION TIPS)
// ============================================================================

@Composable
fun ExaminationScheduleScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val examSchedules by DiscussionRepository.examSchedules.collectAsState()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val availableClasses by DiscussionRepository.availableClassesFlow.collectAsState()

    var selectedExamName by remember { mutableStateOf("Half-Yearly Exam") }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var currentClassFilter by remember { mutableStateOf(selectedClass.id) }

    // Dialog states
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showModifyExamDialog by remember { mutableStateOf<ExamScheduleItem?>(null) }

    val isTeacherOrAdmin = currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN

    // Available exams
    val examNames = remember(examSchedules) {
        val list = examSchedules.map { it.examName }.distinct()
        if (list.isEmpty()) listOf("Half-Yearly Exam", "Final Annual Exam", "Unit Test 1") else list
    }

    val filteredSchedules = remember(examSchedules, currentClassFilter, selectedExamName) {
        examSchedules.filter {
            it.classId.equals(currentClassFilter, ignoreCase = true) &&
            it.examName.equals(selectedExamName, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Examination Schedule", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Controls: Class Selector & Exam Filter
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Class & Section:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.TextSecondary
                            )
                            Box {
                                val currentClassObj = availableClasses.find { it.id.equals(currentClassFilter, ignoreCase = true) }
                                    ?: selectedClass
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { classDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentClassObj.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GenesisColors.TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = GenesisColors.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = classDropdownExpanded,
                                    onDismissRequest = { classDropdownExpanded = false }
                                ) {
                                    availableClasses.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.name) },
                                            onClick = {
                                                currentClassFilter = opt.id
                                                classDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Exam Name Chips
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            examNames.forEach { name ->
                                val selected = selectedExamName == name
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedExamName = name },
                                    label = { Text(name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GenesisColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF8FAFC),
                                        labelColor = GenesisColors.TextSecondary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (selected) GenesisColors.Primary else GenesisColors.Border)
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: Teacher Add Exam + Download Date Sheet
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTeacherOrAdmin) {
                        Button(
                            onClick = { showAddExamDialog = true },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Exam Date", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Downloading $selectedExamName Date Sheet (PDF)...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Schedules List
            if (filteredSchedules.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Schedule for $selectedExamName", fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isTeacherOrAdmin) "Tap 'Add Exam Date' above to set exam dates, syllabus, and preparation guides for this class."
                                else "Exam schedule has not been published yet. Please check back later.",
                                color = GenesisColors.TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredSchedules, key = { it.id }) { schedule ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Date Badge + Subject and Timing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date Pill
                                Surface(
                                    color = GenesisColors.Primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GenesisColors.Primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    val dateParts = schedule.dateText.split(" ")
                                    val day = if (dateParts.isNotEmpty()) dateParts[0] else "24"
                                    val month = if (dateParts.size > 1) dateParts[1].uppercase() else "OCT"
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(day, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                                        Text(month, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = schedule.subject,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GenesisColors.TextPrimary
                                        )
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${schedule.maxMarks} Marks",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GenesisColors.TextPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("⏰ ${schedule.timeSlot}", fontSize = 12.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                        Text("📍 ${schedule.roomNo}", fontSize = 12.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Syllabus Section
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Examination Syllabus:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = schedule.syllabus,
                                        fontSize = 13.sp,
                                        color = GenesisColors.TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // Preparation Tips & Schedule
                            if (schedule.preparationTips.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Preparation Schedule & Strategy:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = schedule.preparationTips,
                                            fontSize = 12.sp,
                                            color = Color(0xFF78350F),
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }

                            // Teacher Modify Action
                            if (isTeacherOrAdmin) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { showModifyExamDialog = schedule },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Modify Schedule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            DiscussionRepository.deleteExamSchedule(schedule.id)
                                            Toast.makeText(context, "Deleted ${schedule.subject} schedule", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add / Modify Exam Schedule
    val editingExam = showModifyExamDialog
    if (showAddExamDialog || editingExam != null) {
        var examName by remember { mutableStateOf(editingExam?.examName ?: selectedExamName) }
        var subject by remember { mutableStateOf(editingExam?.subject ?: "Mathematics") }
        var dateText by remember { mutableStateOf(editingExam?.dateText ?: "28 Oct 2026") }
        var dateIso by remember { mutableStateOf(editingExam?.dateIso ?: "2026-10-28") }
        var timeSlot by remember { mutableStateOf(editingExam?.timeSlot ?: "09:30 AM – 12:30 PM") }
        var roomNo by remember { mutableStateOf(editingExam?.roomNo ?: "Hall 3A") }
        var syllabus by remember { mutableStateOf(editingExam?.syllabus ?: "") }
        var prepTips by remember { mutableStateOf(editingExam?.preparationTips ?: "") }
        var maxMarksStr by remember { mutableStateOf((editingExam?.maxMarks ?: 80).toString()) }

        Dialog(onDismissRequest = {
            showAddExamDialog = false
            showModifyExamDialog = null
        }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (editingExam != null) "Modify Exam Schedule" else "Add Exam Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Exam Name (e.g. Half-Yearly Exam)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text("Date (e.g. 28 Oct 2026)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxMarksStr,
                            onValueChange = { maxMarksStr = it },
                            label = { Text("Max Marks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timeSlot,
                            onValueChange = { timeSlot = it },
                            label = { Text("Time Slot") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = roomNo,
                            onValueChange = { roomNo = it },
                            label = { Text("Hall / Room") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = syllabus,
                        onValueChange = { syllabus = it },
                        label = { Text("Syllabus Breakdown") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prepTips,
                        onValueChange = { prepTips = it },
                        label = { Text("Preparation Tips & Revision Strategy") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            showAddExamDialog = false
                            showModifyExamDialog = null
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (subject.isBlank()) {
                                    Toast.makeText(context, "Please enter subject name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val maxM = maxMarksStr.toIntOrNull() ?: 80
                                val newItem = ExamScheduleItem(
                                    id = editingExam?.id ?: "es-${System.currentTimeMillis()}",
                                    classId = currentClassFilter,
                                    examName = examName.trim(),
                                    subject = subject.trim(),
                                    dateText = dateText.trim(),
                                    dateIso = dateIso.trim(),
                                    timeSlot = timeSlot.trim(),
                                    roomNo = roomNo.trim(),
                                    syllabus = syllabus.trim(),
                                    preparationTips = prepTips.trim(),
                                    maxMarks = maxM
                                )
                                DiscussionRepository.addOrUpdateExamSchedule(newItem)
                                Toast.makeText(context, if (editingExam != null) "Exam schedule updated!" else "Exam schedule added!", Toast.LENGTH_SHORT).show()
                                showAddExamDialog = false
                                showModifyExamDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text(if (editingExam != null) "Update" else "Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4C. PRACTICE TESTS (AI QUIZZES WITH OPTIMIZED CHATGPT PROMPT)
// ============================================================================

@Composable
fun PracticeTestsScreen(
    onBack: () -> Unit,
    onOpenChatGPT: (String?, Uri?) -> Unit = { _, _ -> }
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val classOptions by DiscussionRepository.classOptionsFlow.collectAsState()

    var targetClassName by remember { mutableStateOf(selectedClass.name) }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    var selectedSubject by remember { mutableStateOf("Mathematics") }
    val subjects = listOf("Mathematics", "Science", "Physics", "Chemistry", "Biology", "Social Science", "English")

    var inputTopic by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Activity result launchers for Gallery and Camera
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cached = copyUriToCacheFile(context, uri) ?: uri
            selectedImageUri = cached
            capturedBitmap = null
            Toast.makeText(context, "Question sheet selected from Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = saveBitmapToCache(context, bitmap)
            Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateTestWithChatGPT(customTopic: String? = null) {
        val topic = (customTopic ?: inputTopic).trim()
        val effectiveUri: Uri? = selectedImageUri ?: (capturedBitmap?.let { saveBitmapToCache(context, it) })
        val hasImage = effectiveUri != null

        if (topic.isBlank() && !hasImage) {
            Toast.makeText(context, "Please enter a topic or attach a photo", Toast.LENGTH_SHORT).show()
            return
        }

        val topicDesc = if (topic.isNotBlank()) topic else "the attached question photo / test sheet"
        val prompt = """
            Act as an expert CBSE school teacher for $targetClassName (Subject: $selectedSubject).
            Based on the provided ${if (hasImage) "attached question photo / test sheet" else "topic/chapter"}: "$topicDesc", generate a simple, clear Practice Test.

            Rules:
            1. Generate exactly 5 questions (3 MCQs with options A-D, and 2 short conceptual problems).
            2. Keep questions concise and directly relevant to the core syllabus — no confusing fluff or unnecessary lag.
            3. At the end, provide simple, understandable answer keys and 1 quick real-world example to make the concept clear.
        """.trimIndent()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ChatGPT Practice Test Prompt", prompt))

        Toast.makeText(context, "Opening ChatGPT with custom Practice Test prompt...", Toast.LENGTH_SHORT).show()
        onOpenChatGPT(prompt, effectiveUri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Practice Tests & Quizzes", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Hero Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFEDE9FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, tint = GenesisColors.ClassroomPurple, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Instant CBSE Practice Quizzes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Generate concise 5-question tests with clear step-by-step answers & real-world examples via ChatGPT.", fontSize = 12.sp, color = GenesisColors.TextSecondary, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // Step 1: Select Class & Subject
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Target Class:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                            Box {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { classDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(targetClassName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = classDropdownExpanded,
                                    onDismissRequest = { classDropdownExpanded = false }
                                ) {
                                    val uniqueClasses = classOptions.distinct()
                                    (if (uniqueClasses.isEmpty()) listOf("Class 10", "Class 9", "Class 8") else uniqueClasses).forEach { cls ->
                                        DropdownMenuItem(
                                            text = { Text(cls) },
                                            onClick = {
                                                targetClassName = cls
                                                classDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Subject:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjects.forEach { subj ->
                                val sel = selectedSubject == subj
                                FilterChip(
                                    selected = sel,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GenesisColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF8FAFC),
                                        labelColor = GenesisColors.TextSecondary
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (sel) GenesisColors.Primary else GenesisColors.Border)
                                )
                            }
                        }
                    }
                }
            }

            // Step 2: Input Topic or Photo
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Enter Topic / Photo:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                            VoiceCaptureChipButton(
                                label = "Voice",
                                promptHint = "Speak practice topic or chapter...",
                                onResultText = { spoken ->
                                    inputTopic = if (inputTopic.isBlank()) spoken else "$inputTopic $spoken"
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputTopic,
                            onValueChange = { inputTopic = it },
                            placeholder = { Text("e.g. Quadratic Equations word problems, Lens formula, Periodic trends...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Photo options row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Attached Image Preview
                        if (selectedImageUri != null || capturedBitmap != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        capturedBitmap?.let { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } ?: Icon(Icons.Default.Image, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Question Photo Attached", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                                            Text("Ready to analyze in ChatGPT", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedImageUri = null
                                            capturedBitmap = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Generate Button
                        Button(
                            onClick = { generateTestWithChatGPT() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Practice Test in ChatGPT", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Popular Preset Quizzes
            item {
                Text(
                    text = "Popular CBSE Practice Tests (1-Tap Attempt)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GenesisColors.TextPrimary
                )
            }

            val presetQuizzes = listOf(
                Triple("Mathematics", "Ch 4: Quadratic Equations", "5 high-yield MCQs & word problems on nature of roots & formula method"),
                Triple("Physics", "Ch 10: Light & Ray Optics", "5 practice questions on lens formula, magnification & ray diagrams"),
                Triple("Chemistry", "Ch 1: Chemical Reactions & Equations", "5 questions on balancing, redox reactions & precipitation tests"),
                Triple("Biology", "Ch 6: Life Processes — Circulation", "5 high-yield problems on heart anatomy, blood vessels & xylem/phloem")
            )

            items(presetQuizzes) { (subj, title, desc) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(subj, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                            Text(desc, fontSize = 12.sp, color = GenesisColors.TextSecondary, lineHeight = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedSubject = subj
                                generateTestWithChatGPT(title)
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Attempt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlinePracticeQuizScreen(
    onBack: () -> Unit,
    onOpenChatGPT: (String?, Uri?) -> Unit = { _, _ -> }
) {
    PracticeTestsScreen(onBack = onBack, onOpenChatGPT = onOpenChatGPT)
}

// ============================================================================
// 5. ACADEMIC CALENDAR, GREEN HOLIDAYS & TEACHER/ADMIN HOLIDAY MANAGER
// ============================================================================

@Composable
fun AcademicCalendarScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val activeStudent by DiscussionRepository.activeStudent.collectAsState()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val allHolidays by DiscussionRepository.schoolHolidays.collectAsState()
    val classOptions by DiscussionRepository.classOptionsFlow.collectAsState()
    val availableClasses by DiscussionRepository.availableClassesFlow.collectAsState()
    val events = DiscussionRepository.academicCalendarEvents

    val isStaff = currentRole == AppRole.TEACHER || currentRole == AppRole.ADMIN

    val academicYearMonths = remember {
        (0..11).map { offset -> YearMonth.of(2026, 6).plusMonths(offset.toLong()) }
    }
    var currentMonthIndex by remember { mutableIntStateOf(5) } // Nov 2026
    val activeMonth = academicYearMonths[currentMonthIndex]
    var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 11, 14)) }
    var showDeclareHolidayDialog by remember { mutableStateOf(false) }

    // Holidays visible on the calendar:
    // - Students/Parents see only holidays that apply to their own class & section (or Total School).
    // - Teachers/Admins see all declared school/class/section holidays so they can manage all of them.
    val visibleHolidays = remember(allHolidays, isStaff, activeStudent.classId, activeStudent.className) {
        if (isStaff) {
            allHolidays
        } else {
            allHolidays.filter {
                DiscussionRepository.isHolidayForStudentOrClass(
                    holiday = it,
                    classId = activeStudent.classId,
                    className = activeStudent.className
                )
            }
        }
    }
    val holidaysByDate = remember(visibleHolidays) {
        visibleHolidays.groupBy { it.dateIso }
    }

    if (showDeclareHolidayDialog) {
        DeclareHolidayDialog(
            initialDate = selectedDate,
            classOptions = classOptions,
            availableClasses = availableClasses,
            defaultClassSection = selectedClass,
            onDismiss = { showDeclareHolidayDialog = false },
            onSave = { dateIso, title, scopeType, targetClassName, targetClassId, targetLabel ->
                DiscussionRepository.addSchoolHoliday(
                    dateIso = dateIso,
                    title = title,
                    scopeType = scopeType,
                    targetClassName = targetClassName,
                    targetClassId = targetClassId,
                    targetLabel = targetLabel,
                    createdBy = if (currentRole == AppRole.ADMIN) "Principal / Admin" else "Class Teacher"
                )
                runCatching { LocalDate.parse(dateIso) }.getOrNull()?.let { parsed ->
                    selectedDate = parsed
                    val idx = academicYearMonths.indexOfFirst { it.year == parsed.year && it.month == parsed.month }
                    if (idx >= 0) currentMonthIndex = idx
                }
                showDeclareHolidayDialog = false
                Toast.makeText(context, "Holiday added ($targetLabel)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Academic Calendar 2026-27", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                // Full-Year Month Quick Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    academicYearMonths.forEachIndexed { idx, ym ->
                        val isChosen = idx == currentMonthIndex
                        val label = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} '${ym.year % 100}"
                        Surface(
                            color = if (isChosen) GenesisColors.Primary else Color.White,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, if (isChosen) GenesisColors.Primary else GenesisColors.Border),
                            modifier = Modifier.clickable { currentMonthIndex = idx }
                        ) {
                            Text(
                                text = label,
                                color = if (isChosen) Color.White else GenesisColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentMonthIndex > 0) currentMonthIndex-- },
                                enabled = currentMonthIndex > 0
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Month",
                                    tint = if (currentMonthIndex > 0) GenesisColors.Primary else GenesisColors.Border
                                )
                            }
                            Text(
                                text = "${activeMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${activeMonth.year}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GenesisColors.TextPrimary
                            )
                            IconButton(
                                onClick = { if (currentMonthIndex < academicYearMonths.lastIndex) currentMonthIndex++ },
                                enabled = currentMonthIndex < academicYearMonths.lastIndex
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = if (currentMonthIndex < academicYearMonths.lastIndex) GenesisColors.Primary else GenesisColors.Border
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AcademicMonthGrid(
                            month = activeMonth,
                            selectedDate = selectedDate,
                            onSelectDate = { selectedDate = it },
                            dayStyleProvider = { date ->
                                val iso = date.toString()
                                val hasHoliday = holidaysByDate.containsKey(iso)
                                val hasEvent = events.any { it.dateIso == iso }
                                when {
                                    hasHoliday -> CalendarDayVisual(
                                        bgColor = Color(0xFF16A34A).copy(alpha = 0.24f),
                                        borderColor = Color(0xFF16A34A).copy(alpha = 0.75f),
                                        textColor = Color(0xFF14532D)
                                    )
                                    hasEvent -> CalendarDayVisual(
                                        bgColor = Color(0xFF2563EB).copy(alpha = 0.16f),
                                        borderColor = Color(0xFF2563EB).copy(alpha = 0.5f),
                                        textColor = GenesisColors.Primary
                                    )
                                    date.dayOfWeek == DayOfWeek.SUNDAY -> CalendarDayVisual(
                                        bgColor = Color(0xFFF1F5F9),
                                        borderColor = Color.Transparent,
                                        textColor = GenesisColors.TextSecondary
                                    )
                                    else -> CalendarDayVisual(
                                        bgColor = Color.Transparent,
                                        borderColor = Color.Transparent,
                                        textColor = GenesisColors.TextPrimary
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CalendarLegendDot("Holiday", Color(0xFF16A34A).copy(alpha = 0.30f), Color(0xFF15803D))
                            Spacer(modifier = Modifier.width(18.dp))
                            CalendarLegendDot("School Event", Color(0xFF2563EB).copy(alpha = 0.22f), GenesisColors.Primary)
                            Spacer(modifier = Modifier.width(18.dp))
                            CalendarLegendDot("Sunday", Color(0xFFF1F5F9), GenesisColors.TextSecondary)
                        }

                        if (isStaff) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showDeclareHolidayDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                            ) {
                                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+ List Holiday (${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Selected Date Holiday / Event Info Card
            item {
                val iso = selectedDate.toString()
                val dayHolidays = holidaysByDate[iso].orEmpty()
                val dayEvents = events.filter { it.dateIso == iso }
                Surface(
                    color = if (dayHolidays.isNotEmpty()) Color(0xFF16A34A).copy(alpha = 0.12f) else Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (dayHolidays.isNotEmpty()) Color(0xFF16A34A).copy(alpha = 0.45f) else GenesisColors.Border
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GenesisColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        when {
                            dayHolidays.isNotEmpty() -> {
                                dayHolidays.forEach { hol ->
                                    Text(
                                        text = "Holiday: ${hol.title} (${hol.targetLabel})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF14532D)
                                    )
                                }
                            }
                            dayEvents.isNotEmpty() -> {
                                dayEvents.forEach { ev ->
                                    Text(
                                        text = "${ev.category}: ${ev.title}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GenesisColors.Primary
                                    )
                                }
                            }
                            selectedDate.dayOfWeek == DayOfWeek.SUNDAY -> {
                                Text("Weekly Off (Sunday)", fontSize = 13.sp, color = GenesisColors.TextSecondary)
                            }
                            else -> {
                                Text("Regular Working Day", fontSize = 13.sp, color = GenesisColors.TextSecondary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                Text(
                    text = if (isStaff) "Declared Holidays (${visibleHolidays.size})" else "Holidays for ${activeStudent.classTitle}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GenesisColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(visibleHolidays, key = { it.id }) { hol ->
                val prettyDate = runCatching {
                    LocalDate.parse(hol.dateIso).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
                }.getOrDefault(hol.dateIso)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            runCatching { LocalDate.parse(hol.dateIso) }.getOrNull()?.let { d ->
                                selectedDate = d
                                val idx = academicYearMonths.indexOfFirst { it.year == d.year && it.month == d.month }
                                if (idx >= 0) currentMonthIndex = idx
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF16A34A).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = prettyDate,
                                color = Color(0xFF14532D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .width(88.dp)
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hol.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = hol.targetLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.Success
                            )
                        }
                        if (isStaff) {
                            IconButton(
                                onClick = {
                                    DiscussionRepository.removeSchoolHoliday(hol.id)
                                    Toast.makeText(context, "Removed holiday", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Holiday", tint = GenesisColors.Emergency)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text("School Events & Exam Schedule", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(events, key = { it.id }) { ev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = ev.dateLabel,
                                color = GenesisColors.Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .width(92.dp)
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            CategoryBadge(ev.category)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ev.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeclareHolidayDialog(
    initialDate: LocalDate,
    classOptions: List<String>,
    availableClasses: List<SchoolClass>,
    defaultClassSection: SchoolClass,
    onDismiss: () -> Unit,
    onSave: (
        dateIso: String,
        title: String,
        scopeType: HolidayScopeType,
        targetClassName: String,
        targetClassId: String,
        targetLabel: String
    ) -> Unit
) {
    var pickedDate by remember { mutableStateOf(initialDate) }
    var title by remember { mutableStateOf("") }
    var scopeType by remember { mutableStateOf(HolidayScopeType.TOTAL_SCHOOL) }
    var selectedClassName by remember {
        mutableStateOf(
            classOptions.find { defaultClassSection.name.contains(it, ignoreCase = true) }
                ?: classOptions.lastOrNull()
                ?: "Class 10"
        )
    }
    var selectedSectionClass by remember { mutableStateOf(defaultClassSection) }

    Dialog(onDismissRequest = onDismiss) {
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
                Text("List School Holiday", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GenesisColors.TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // Date Adjuster
                Text("Holiday Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { pickedDate = pickedDate.minusDays(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = GenesisColors.Primary)
                        }
                        Text(
                            text = pickedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GenesisColors.TextPrimary
                        )
                        IconButton(onClick = { pickedDate = pickedDate.plusDays(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = GenesisColors.Primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Holiday Title (e.g. Local Festival)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Applies To", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        HolidayScopeType.TOTAL_SCHOOL to "Total School",
                        HolidayScopeType.CLASS_SPECIFIC to "Specific Class",
                        HolidayScopeType.SECTION_SPECIFIC to "Specific Section"
                    ).forEach { (type, label) ->
                        val active = scopeType == type
                        Surface(
                            color = if (active) GenesisColors.Success else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, if (active) GenesisColors.Success else GenesisColors.Border),
                            modifier = Modifier.clickable { scopeType = type }
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else GenesisColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (scopeType == HolidayScopeType.CLASS_SPECIFIC) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Class (Applies to all sections)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        classOptions.forEach { clsName ->
                            val selected = selectedClassName == clsName
                            Surface(
                                color = if (selected) GenesisColors.Primary else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) GenesisColors.Primary else GenesisColors.Border),
                                modifier = Modifier.clickable { selectedClassName = clsName }
                            ) {
                                Text(
                                    text = clsName,
                                    color = if (selected) Color.White else GenesisColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                } else if (scopeType == HolidayScopeType.SECTION_SPECIFIC) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ClassSectionDropdownButton(
                        selectedClass = selectedSectionClass,
                        availableClasses = availableClasses,
                        onClassSelected = { sec -> selectedSectionClass = sec },
                        label = "Select Class & Section"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = GenesisColors.TextPrimary)
                    }
                    Button(
                        onClick = {
                            val finalTitle = title.trim().ifBlank { "School Holiday" }
                            val (tClass, tId, tLabel) = when (scopeType) {
                                HolidayScopeType.TOTAL_SCHOOL -> Triple("", "", "Total School")
                                HolidayScopeType.CLASS_SPECIFIC -> Triple(selectedClassName, "", "$selectedClassName (All Sections)")
                                HolidayScopeType.SECTION_SPECIFIC -> Triple("", selectedSectionClass.id, selectedSectionClass.name)
                            }
                            onSave(pickedDate.toString(), finalTitle, scopeType, tClass, tId, tLabel)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                    ) {
                        Text("Save Holiday", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. PRINCIPAL / CHAIRMAN EXECUTIVE COMMAND CENTER (₹1 LAKH SCHOOL ROI CARD)
// ============================================================================

@Composable
fun PrincipalExecutiveCommandCard(
    onOpenRollCall: () -> Unit,
    onOpenFeeReview: () -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GenesisColors.MeteorBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PRINCIPAL & MANAGEMENT COMMAND CENTER",
                        color = GenesisColors.SkyAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Campus Operations & Revenue",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(Icons.Default.Insights, contentDescription = null, tint = GenesisColors.SkyAccent)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = GenesisColors.MeteorSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenRollCall() }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Campus Attendance", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("94.6%", color = Color(0xFF4ADE80), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("512 / 541 Present Today", color = Color.White, fontSize = 10.sp)
                    }
                }
                Surface(
                    color = GenesisColors.MeteorSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenFeeReview() }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Term Fee Collected", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("₹18.45 L", color = GenesisColors.SkyAccent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("₹3.2L Due • Tap to Remind", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Gentle automated fee reminder sent to 24 pending parent accounts!",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GenesisColors.SkyAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GenesisColors.SkyAccent)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("1-Tap Send Automated Fee Due Alert", color = GenesisColors.SkyAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// 6. SIBLINGS CAPTURE POPUP DIALOG (2-MINUTE TIMED POPUP FOR FAMILIES)
// ============================================================================

@Composable
fun SiblingsCaptureDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeStudent by DiscussionRepository.activeStudent.collectAsState()

    var hasSiblingsChoice by remember { mutableStateOf<Boolean?>(null) } // null = undecided, false = no, true = yes

    // Sibling entries list
    val siblingsList = remember {
        mutableStateListOf(
            SiblingEntry(
                fullName = "",
                age = 8,
                currentGrade = "Class 3",
                schoolType = "OTHER",
                schoolName = "",
                reasonOtherSchool = "Interested in shifting for 2026-27"
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Top Row: Timer Badge & Close (X) Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱️ Just 2 min to fill the form!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel popup",
                            tint = GenesisColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Family & Sibling Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenesisColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Help us sync family records to unlock priority admissions and exclusive Sibling Fee Concessions for your family.",
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pre-filled Primary Student Info
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Enrolled Student: ${activeStudent.fullName} (${activeStudent.classTitle})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GenesisColors.TextPrimary
                        )
                        Text(
                            text = "Parent Phone: ${activeStudent.parentPhone.ifBlank { "+91 98765 43210" }}",
                            fontSize = 11.sp,
                            color = GenesisColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question: Does this student have siblings?
                Text(
                    text = "Does ${activeStudent.firstName.ifBlank { "this student" }} have any brothers or sisters?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenesisColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            hasSiblingsChoice = false
                            // Single child: submit and finish permanently!
                            val submission = SiblingSubmission(
                                studentId = activeStudent.id,
                                studentName = activeStudent.fullName,
                                className = activeStudent.classTitle,
                                parentName = "Parent of ${activeStudent.fullName}",
                                parentPhone = activeStudent.parentPhone.ifBlank { "+91 98765 43210" },
                                parentEmail = activeStudent.parentEmail,
                                hasSiblings = false,
                                siblings = emptyList()
                            )
                            DiscussionRepository.submitSiblingsForm(context, submission, coroutineScope)
                            Toast.makeText(context, "Family profile updated! Thank you.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (hasSiblingsChoice == false) Color(0xFFEFF6FF) else Color.Transparent
                        )
                    ) {
                        Text("No, Single Child", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { hasSiblingsChoice = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasSiblingsChoice == true) GenesisColors.Primary else Color(0xFF1E293B)
                        )
                    ) {
                        Text("Yes, Has Siblings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // If Yes: Dynamic Sibling Form
                if (hasSiblingsChoice == true) {
                    Spacer(modifier = Modifier.height(16.dp))

                    siblingsList.forEachIndexed { index, sibling ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Sibling #${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                                    if (siblingsList.size > 1) {
                                        IconButton(
                                            onClick = { siblingsList.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = sibling.fullName,
                                    onValueChange = { siblingsList[index] = sibling.copy(fullName = it) },
                                    label = { Text("Sibling Full Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sibling.age.toString(),
                                        onValueChange = {
                                            val parsed = it.toIntOrNull() ?: 1
                                            siblingsList[index] = sibling.copy(age = parsed)
                                        },
                                        label = { Text("Age") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = sibling.currentGrade,
                                        onValueChange = { siblingsList[index] = sibling.copy(currentGrade = it) },
                                        label = { Text("Grade (e.g. Class 3)") },
                                        modifier = Modifier.weight(1.5f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Where is this sibling currently studying?", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isOther = sibling.schoolType == "OTHER"
                                    FilterChip(
                                        selected = isOther,
                                        onClick = { siblingsList[index] = sibling.copy(schoolType = "OTHER") },
                                        label = { Text("In Another School", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFDCFCE7),
                                            selectedLabelColor = Color(0xFF166534)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    val isGenesis = sibling.schoolType == "GENESIS"
                                    FilterChip(
                                        selected = isGenesis,
                                        onClick = { siblingsList[index] = sibling.copy(schoolType = "GENESIS", schoolName = "Genesis School Beeramguda") },
                                        label = { Text("In Genesis School", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFEDE9FE),
                                            selectedLabelColor = Color(0xFF6D28D9)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (sibling.schoolType == "OTHER") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = sibling.schoolName,
                                        onValueChange = { siblingsList[index] = sibling.copy(schoolName = it) },
                                        label = { Text("Current School Name (e.g. DPS, EuroKids)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = sibling.reasonOtherSchool,
                                        onValueChange = { siblingsList[index] = sibling.copy(reasonOtherSchool = it) },
                                        label = { Text("Shifting Interest / Target Admission Year") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            siblingsList.add(
                                SiblingEntry(
                                    fullName = "",
                                    age = 6,
                                    currentGrade = "Class 1",
                                    schoolType = "OTHER",
                                    schoolName = "",
                                    reasonOtherSchool = "Interested in shifting for 2026-27"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Another Sibling", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val validSiblings = siblingsList.filter { it.fullName.isNotBlank() }
                            if (validSiblings.isEmpty()) {
                                Toast.makeText(context, "Please enter sibling name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val submission = SiblingSubmission(
                                studentId = activeStudent.id,
                                studentName = activeStudent.fullName,
                                className = activeStudent.classTitle,
                                parentName = "Parent of ${activeStudent.fullName}",
                                parentPhone = activeStudent.parentPhone.ifBlank { "+91 98765 43210" },
                                parentEmail = activeStudent.parentEmail,
                                hasSiblings = true,
                                siblings = validSiblings
                            )
                            DiscussionRepository.submitSiblingsForm(context, submission, coroutineScope)
                            Toast.makeText(context, "Sibling details saved! Thank you.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Sibling Details (1-Tap)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. ADMIN SIBLINGS ADMISSIONS HUB (WITH FILTERS & DIRECT CALL / WHATSAPP)
// ============================================================================

@Composable
fun AdminSiblingsHubScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val siblingSubmissions by DiscussionRepository.siblingSubmissions.collectAsState()

    var schoolTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "OTHER", "GENESIS"
    var gradeFilter by remember { mutableStateOf("All Grades") }
    var searchQuery by remember { mutableStateOf("") }

    // Flatten into individual sibling records for admin lead targeting
    val allLeads = remember(siblingSubmissions) {
        val list = mutableListOf<Pair<SiblingSubmission, SiblingEntry>>()
        siblingSubmissions.forEach { sub ->
            if (sub.hasSiblings) {
                sub.siblings.forEach { sib ->
                    list.add(Pair(sub, sib))
                }
            }
        }
        list
    }

    val filteredLeads = remember(allLeads, schoolTypeFilter, gradeFilter, searchQuery) {
        allLeads.filter { (sub, sib) ->
            val matchSchool = when (schoolTypeFilter) {
                "OTHER" -> sib.schoolType == "OTHER"
                "GENESIS" -> sib.schoolType == "GENESIS"
                else -> true
            }
            val matchGrade = if (gradeFilter == "All Grades") true else sib.currentGrade.contains(gradeFilter.replace("Pre-Primary", "KG").replace("Primary", "Class").replace("Middle", "Class").replace("High School", "Class 10"), ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchSearch = if (q.isBlank()) true else {
                sib.fullName.lowercase().contains(q) ||
                sub.studentName.lowercase().contains(q) ||
                sub.parentPhone.contains(q) ||
                sib.schoolName.lowercase().contains(q)
            }
            matchSchool && matchGrade && matchSearch
        }
    }

    val otherSchoolLeadsCount = allLeads.count { it.second.schoolType == "OTHER" }
    val genesisSiblingsCount = allLeads.count { it.second.schoolType == "GENESIS" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Sibling Admissions Hub", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary KPI Cards (ROI Standard)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Other School Siblings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$otherSchoolLeadsCount Leads", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF15803D))
                            Text("High conversion target", fontSize = 10.sp, color = Color(0xFF166534))
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9FE)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFC4B5FD)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Genesis Siblings", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5B21B6))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$genesisSiblingsCount Enrolled", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6D28D9))
                            Text("Family loyalty retention", fontSize = 10.sp, color = Color(0xFF5B21B6))
                        }
                    }
                }
            }

            // Search Bar & Filter Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by Sibling name, Parent phone...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GenesisColors.TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // School Type Filter Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Pair("ALL", "All (${allLeads.size})"),
                                Pair("OTHER", "Other School ($otherSchoolLeadsCount)"),
                                Pair("GENESIS", "Genesis ($genesisSiblingsCount)")
                            ).forEach { (key, label) ->
                                val sel = schoolTypeFilter == key
                                FilterChip(
                                    selected = sel,
                                    onClick = { schoolTypeFilter = key },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GenesisColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF8FAFC),
                                        labelColor = GenesisColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Leads List
            if (filteredLeads.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Sibling Leads Found", fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary, fontSize = 16.sp)
                            Text("Try adjusting your filters or search query.", color = GenesisColors.TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredLeads, key = { it.second.id }) { (submission, sibling) ->
                    val isOtherSchool = sibling.schoolType == "OTHER"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isOtherSchool) Color(0xFF86EFAC) else GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Sibling name + Lead Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sibling.fullName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(${sibling.currentGrade}, Age ${sibling.age})", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                }

                                Surface(
                                    color = if (isOtherSchool) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isOtherSchool) "🔥 Target Lead" else "Enrolled",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOtherSchool) Color(0xFF15803D) else GenesisColors.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Current School & Reason
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOtherSchool) "Currently at: ${sibling.schoolName.ifBlank { "Other School" }}" else "Studying at Genesis School",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOtherSchool) Color(0xFFB45309) else GenesisColors.TextPrimary
                                )
                            }
                            if (sibling.reasonOtherSchool.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Notes: ${sibling.reasonOtherSchool}",
                                    fontSize = 12.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Linked Student & Parent
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Enrolled Genesis Student: ${submission.studentName} (${submission.className})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GenesisColors.Primary
                                    )
                                    Text(
                                        text = "Parent: ${submission.parentName} • ${submission.parentPhone}",
                                        fontSize = 11.sp,
                                        color = GenesisColors.TextSecondary
                                    )
                                }
                            }

                            // Admin Actions: 1-Tap Call & WhatsApp Offer
                            if (isOtherSchool) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${submission.parentPhone}"))
                                            context.startActivity(dialIntent)
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call Parent", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val msg = "Dear ${submission.parentName}, as ${submission.studentName} is studying in ${submission.className} at Genesis School Beeramguda, we are pleased to offer an exclusive Sibling Fee Concession for ${sibling.fullName} for ${sibling.currentGrade} admissions (2026-27). Would you like to schedule a quick campus visit?"
                                            val waUrl = "https://api.whatsapp.com/send?phone=${submission.parentPhone.replace("[^0-9]".toRegex(), "")}&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
                                        },
                                        modifier = Modifier.weight(1.3f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp Offer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

// ============================================================================
// 8. MISTAKE ANALYSIS FOR STUDENTS (TEST PAPER EVALUATION & FREQUENT PITFALLS)
// ============================================================================

@Composable
fun MistakeAnalysisScreen(
    onBack: () -> Unit,
    onOpenChatGPT: (String?, Uri?) -> Unit = { _, _ -> }
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val mistakeList by DiscussionRepository.mistakeAnalysisList.collectAsState()

    var selectedSubject by remember { mutableStateOf("Mathematics") }
    val subjects = listOf("Mathematics", "Science", "Physics", "Chemistry", "Biology", "Social Science")

    var examName by remember { mutableStateOf("Half-Yearly Exam") }
    var questionNote by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cached = copyUriToCacheFile(context, uri) ?: uri
            selectedImageUri = cached
            capturedBitmap = null
            Toast.makeText(context, "Test sheet photo selected from Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = saveBitmapToCache(context, bitmap)
            Toast.makeText(context, "Test paper photo captured!", Toast.LENGTH_SHORT).show()
        }
    }

    fun analyzeMistakeWithAI(customTopic: String? = null) {
        val effectiveUri: Uri? = selectedImageUri ?: (capturedBitmap?.let { saveBitmapToCache(context, it) })
        val note = (customTopic ?: questionNote).trim()

        if (effectiveUri == null && note.isBlank()) {
            Toast.makeText(context, "Please attach a photo of your test paper or enter the mistake note", Toast.LENGTH_SHORT).show()
            return
        }

        val prompt = """
            Act as an expert CBSE school teacher for Class 10 ($selectedSubject).
            Analyze this student's completed test paper / assessment sheet from the attached photo.
            Exam: $examName ${if (note.isNotBlank()) "— Specific topic/question note: \"$note\"" else ""}

            Rules:
            1. Pinpoint the exact mistake made in the answer sheet (in 1-2 lines).
            2. Explain why it was wrong and how to solve it correctly (simple step-by-step, no lengthy confusing text).
            3. End with 1 quick real-world example to ensure the student never repeats this mistake.
        """.trimIndent()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ChatGPT Mistake Analysis Prompt", prompt))

        Toast.makeText(context, "Opening AI Mistake Analysis in ChatGPT...", Toast.LENGTH_SHORT).show()
        onOpenChatGPT(prompt, effectiveUri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Mistake Analysis", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Explanation Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFEE2E2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FindInPage, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Test Paper Mistake Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Upload your evaluated test paper. AI pinpoints what went wrong and explains step-by-step how to correct it without lengthy text.", fontSize = 12.sp, color = GenesisColors.TextSecondary, lineHeight = 16.sp)
                        }
                    }
                }
            }

            // Step 1: Select Subject & Exam
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. Select Subject & Assessment:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjects.forEach { subj ->
                                val sel = selectedSubject == subj
                                FilterChip(
                                    selected = sel,
                                    onClick = { selectedSubject = subj },
                                    label = { Text(subj, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GenesisColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFF8FAFC),
                                        labelColor = GenesisColors.TextSecondary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = examName,
                            onValueChange = { examName = it },
                            label = { Text("Assessment / Exam Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Step 2: Upload Test Paper Image
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. Upload Test Paper / Answer Sheet Photo:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch() },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Preview attached test sheet
                        if (selectedImageUri != null || capturedBitmap != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        capturedBitmap?.let { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } ?: Icon(Icons.Default.Image, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Test Paper Sheet Attached", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                                            Text("Ready for AI mistake detection", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedImageUri = null
                                            capturedBitmap = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = questionNote,
                            onValueChange = { questionNote = it },
                            placeholder = { Text("Optional: Specify question or remark (e.g. Q14 lost 3 marks)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { analyzeMistakeWithAI() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Mistake with AI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Frequently Misunderstood Topics (CBSE Assessment Insights)
            item {
                Text(
                    text = "Frequently Misunderstood Topics in $selectedSubject",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GenesisColors.TextPrimary
                )
            }

            val subjectMistakes = mistakeList.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
            val displayMistakes = if (subjectMistakes.isNotEmpty()) subjectMistakes else mistakeList

            items(displayMistakes, key = { it.id }) { mistake ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Common Pitfall: ${mistake.questionNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(mistake.examName, fontSize = 11.sp, color = GenesisColors.TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mistake.mistakeTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFFFF1F2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("❌ What went wrong:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9F1239))
                                Text(mistake.whatWasWrong, fontSize = 12.sp, color = Color(0xFF881337), lineHeight = 17.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("✅ How to correct simply:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                Text(mistake.howToCorrect, fontSize = 12.sp, color = Color(0xFF14532D), lineHeight = 17.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("💡 Quick Real-World Example:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                Text(mistake.realWorldExample, fontSize = 12.sp, color = Color(0xFF78350F), lineHeight = 17.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    analyzeMistakeWithAI(mistake.mistakeTitle)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask AI Explanation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

