package com.schoolapp.android.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.schoolapp.android.data.*
import com.schoolapp.android.ui.theme.GenesisColors

// ============================================================================
// TEACHER AUTOMATED MARKS ENGINE (NO EXCEL • SINGLE-ENTRY -> AUTO REPORT CARDS)
//  Teacher enters marks once -> System automatically:
//   1) Calculates Totals
//   2) Calculates Percentages
//   3) Assigns Grades per School Rules
//   4) Generates Report Cards (PDF)
//   5) Updates Student Performance History
//   6) Publishes Results to Students/Parents only after Approval
// ============================================================================

@Composable
fun TeacherMarksAutomationScreen(
    onBack: () -> Unit = {},
    showBackButton: Boolean = true
) {
    if (showBackButton) {
        BackHandler { onBack() }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedClass by DiscussionRepository.selectedClass.collectAsState()
    val availableClasses by DiscussionRepository.availableClassesFlow.collectAsState()
    val classSubjectsMap by DiscussionRepository.classSubjectsMap.collectAsState()
    val roster by DiscussionRepository.sectionStudentRoster.collectAsState()
    val allSheets by DiscussionRepository.studentExamSheets.collectAsState()
    val availableExams by DiscussionRepository.availableExamsFlow.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Enter Marks, 1: Report Cards & Approval
    var selectedExam by remember { mutableStateOf("Final Annual Exam") }
    var examDropdownOpen by remember { mutableStateOf(false) }

    var showCreateExamDialog by remember { mutableStateOf(false) }
    var newExamNameInput by remember { mutableStateOf("") }

    var showCreateSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectNameInput by remember { mutableStateOf("") }
    var newSubjectMaxMarksInput by remember { mutableStateOf("100") }
    var subjectDropdownOpen by remember { mutableStateOf(false) }

    var showGradeScaleDialog by remember { mutableStateOf(false) }

    val classSubjects = classSubjectsMap[selectedClass.id]
        ?: DiscussionRepository.getSubjectsForClass(selectedClass.id)
    var selectedSubject by remember(selectedClass.id, classSubjects) {
        mutableStateOf(classSubjects.firstOrNull() ?: "Mathematics")
    }
    var maxMarksInput by remember(selectedExam) {
        mutableStateOf(if (selectedExam.startsWith("Unit Test")) "25" else "100")
    }

    val classRoster = remember(roster, selectedClass.id) {
        roster.filter { it.classId == selectedClass.id && it.verificationStatus != "BLOCKED" }
    }

    val classSheetsForExam = remember(allSheets, selectedClass.id, selectedExam) {
        allSheets.filter { it.classId == selectedClass.id && it.examName == selectedExam }
    }

    // Local draft marks state keyed by studentId for current (classId, exam, subject)
    val draftMarks = remember(selectedClass.id, selectedExam, selectedSubject, classRoster, classSheetsForExam) {
        mutableStateMapOf<String, String>().apply {
            classRoster.forEach { stu ->
                val existingSheet = classSheetsForExam.find { it.studentId == stu.id }
                val existingVal = existingSheet?.subjectMarks?.get(selectedSubject)
                put(stu.id, existingVal?.toString() ?: "")
            }
        }
    }

    // --- Create New Exam Dialog ---
    if (showCreateExamDialog) {
        Dialog(onDismissRequest = { showCreateExamDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = GenesisColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create New Exam",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add a new exam or assessment cycle for your school.",
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newExamNameInput,
                        onValueChange = { newExamNameInput = it },
                        label = { Text("Exam Name (e.g. Unit Test 3, Pre-Board 1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Suggestions:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Unit Test 3", "Midterm Exam", "Pre-Board 1").forEach { suggestion ->
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { newExamNameInput = suggestion }
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateExamDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary)
                        }
                        Button(
                            onClick = {
                                val clean = newExamNameInput.trim()
                                if (clean.isNotBlank()) {
                                    DiscussionRepository.addNewExamName(clean)
                                    selectedExam = clean
                                    Toast.makeText(context, "Created exam: $clean", Toast.LENGTH_SHORT).show()
                                    showCreateExamDialog = false
                                }
                            },
                            enabled = newExamNameInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Create Exam", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- Create New Subject Dialog ---
    if (showCreateSubjectDialog) {
        Dialog(onDismissRequest = { showCreateSubjectDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GenesisColors.Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add New Subject",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adding to ${selectedClass.name}",
                        fontSize = 12.sp,
                        color = GenesisColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newSubjectNameInput,
                        onValueChange = { newSubjectNameInput = it },
                        label = { Text("Subject Name (e.g. Sanskrit, AI, EVS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Suggestions:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Hindi", "Sanskrit", "AI & Robotics", "EVS").forEach { suggestion ->
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { newSubjectNameInput = suggestion }
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateSubjectDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = GenesisColors.TextPrimary)
                        }
                        Button(
                            onClick = {
                                val clean = newSubjectNameInput.trim()
                                if (clean.isNotBlank()) {
                                    DiscussionRepository.addSubjectToClass(
                                        classId = selectedClass.id,
                                        subjectName = clean,
                                        maxMarks = newSubjectMaxMarksInput.toIntOrNull() ?: 100
                                    )
                                    selectedSubject = clean
                                    Toast.makeText(context, "Added $clean to ${selectedClass.name}", Toast.LENGTH_SHORT).show()
                                    showCreateSubjectDialog = false
                                }
                            },
                            enabled = newSubjectNameInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text("Add Subject", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- View Grade Rules & Marks Range Dialog for Selected Exam ---
    if (showGradeScaleDialog) {
        val maxInt = maxMarksInput.toIntOrNull()?.coerceAtLeast(1) ?: 100
        var testMarkInput by remember { mutableStateOf("") }
        val testMarkInt = testMarkInput.toIntOrNull()?.coerceIn(0, maxInt)
        val testGrade = testMarkInt?.let { SchoolGradingPolicy.assignGrade((it * 100.0) / maxInt) }

        data class GradeTier(
            val grade: String,
            val minPct: Double,
            val maxPct: Double,
            val badgeColor: Color,
            val bgColor: Color,
            val remarks: String
        )

        val tiers = listOf(
            GradeTier("A1", 91.0, 100.0, Color(0xFF15803D), Color(0xFFDCFCE7), "Outstanding"),
            GradeTier("A2", 81.0, 90.0, Color(0xFF16A34A), Color(0xFFF0FDF4), "Excellent"),
            GradeTier("B1", 71.0, 80.0, Color(0xFF2563EB), Color(0xFFDBEAFE), "Very Good"),
            GradeTier("B2", 61.0, 70.0, Color(0xFF0284C7), Color(0xFFE0F2FE), "Good"),
            GradeTier("C1", 51.0, 60.0, Color(0xFFD97706), Color(0xFFFEF3C7), "Satisfactory"),
            GradeTier("C2", 41.0, 50.0, Color(0xFFEA580C), Color(0xFFFFEDD5), "Average"),
            GradeTier("D", 33.0, 40.0, Color(0xFF9333EA), Color(0xFFF3E8FF), "Passing Grade"),
            GradeTier("E", 0.0, 32.0, Color(0xFFDC2626), Color(0xFFFEE2E2), "Needs Improvement")
        )

        Dialog(onDismissRequest = { showGradeScaleDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top Row: Title + Close (X)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grading Scale & Marks Range",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$selectedExam • Max Marks: $maxInt",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.Primary
                            )
                        }
                        IconButton(
                            onClick = { showGradeScaleDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GenesisColors.TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Grades for $selectedExam ($selectedSubject) are auto-calculated according to school policy based on $maxInt max marks:",
                            fontSize = 11.sp,
                            color = GenesisColors.TextSecondary,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary, modifier = Modifier.width(42.dp))
                        Text("Marks Range", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary, modifier = Modifier.weight(1.3f))
                        Text("% Range", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary, modifier = Modifier.weight(1.1f))
                        Text("Remarks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary, modifier = Modifier.weight(1.3f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Grade Rows
                    tiers.forEach { tier ->
                        val minMark = kotlin.math.round((tier.minPct * maxInt) / 100.0).toInt().coerceIn(0, maxInt)
                        val maxMark = kotlin.math.round((tier.maxPct * maxInt) / 100.0).toInt().coerceIn(0, maxInt)
                        val rangeDisplay = if (tier.grade == "E") {
                            val upper = kotlin.math.max(0, kotlin.math.round((32.0 * maxInt) / 100.0).toInt())
                            "0 – $upper"
                        } else {
                            "$minMark – $maxMark"
                        }
                        val pctDisplay = if (tier.grade == "E") "< 33%" else "${tier.minPct.toInt()}%–${tier.maxPct.toInt()}%"

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = tier.bgColor,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, tier.badgeColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.width(36.dp)
                                ) {
                                    Text(
                                        text = tier.grade,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tier.badgeColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = rangeDisplay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.TextPrimary,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = pctDisplay,
                                    fontSize = 11.sp,
                                    color = GenesisColors.TextSecondary,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Text(
                                    text = tier.remarks,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = tier.badgeColor,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Interactive Mark Tester
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🎯 Live Mark-to-Grade Calculator",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.Primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = testMarkInput,
                                    onValueChange = { raw ->
                                        testMarkInput = raw.filter { it.isDigit() }.take(3)
                                    },
                                    placeholder = { Text("Test mark (0-$maxInt)", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                if (testGrade != null && testMarkInt != null) {
                                    val pct = (testMarkInt * 100.0) / maxInt
                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                    ) {
                                        Text(
                                            text = "Grade: $testGrade (%.0f%%)".format(pct),
                                            color = Color(0xFF15803D),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showGradeScaleDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
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
        if (showBackButton) {
            SubScreenTopBar(title = "Marks & Report Card Automation", onBack = onBack)
        } else {
            Surface(
                color = GenesisColors.MeteorBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "Marks & Auto Report Cards",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Enter marks once • Auto totals, %, grades & approval",
                        fontSize = 12.sp,
                        color = GenesisColors.SkyAccent
                    )
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = GenesisColors.Primary
        ) {
            val pendingApprovalCount = classSheetsForExam.count { !it.isApproved }
            listOf(
                "Enter Marks Once",
                if (pendingApprovalCount > 0) "Approve & Publish ($pendingApprovalCount)" else "Report Cards"
            ).forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == idx) GenesisColors.Primary else GenesisColors.TextSecondary
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 90.dp)
        ) {
            // Class & Exam Selector Bar
            item {
                ClassSectionDropdownButton(
                    selectedClass = selectedClass,
                    availableClasses = availableClasses,
                    onClassSelected = { cls ->
                        DiscussionRepository.switchClass(cls, coroutineScope)
                    },
                    label = "Class & Section"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.Primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { examDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Exam", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                                Text(selectedExam, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select Exam", tint = GenesisColors.Primary)
                        }
                    }

                    DropdownMenu(
                        expanded = examDropdownOpen,
                        onDismissRequest = { examDropdownOpen = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        availableExams.forEach { examTitle ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = examTitle,
                                            fontWeight = if (examTitle == selectedExam) FontWeight.Bold else FontWeight.Normal,
                                            color = if (examTitle == selectedExam) GenesisColors.Primary else GenesisColors.TextPrimary
                                        )
                                        if (examTitle == selectedExam) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GenesisColors.Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedExam = examTitle
                                    examDropdownOpen = false
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = GenesisColors.Primary)
                            },
                            text = {
                                Text(
                                    text = "➕ Create New Exam",
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                examDropdownOpen = false
                                newExamNameInput = ""
                                showCreateExamDialog = true
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Quick Button to open the Grading Scale & Marks Range pop-up for the selected exam
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGradeScaleDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Grade, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Grading Scale & Marks Range ($selectedExam)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (selectedTab == 0) {
                // TAB 0: FAST BATCH SUBJECT MARK ENTRY (NO EXCEL NEEDED)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subject", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextSecondary)
                        TextButton(
                            onClick = {
                                newSubjectNameInput = ""
                                newSubjectMaxMarksInput = "100"
                                showCreateSubjectDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Subject", color = GenesisColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { subjectDropdownOpen = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = GenesisColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedSubject,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GenesisColors.TextPrimary
                                    )
                                }
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select Subject", tint = GenesisColors.TextSecondary)
                            }
                        }

                        DropdownMenu(
                            expanded = subjectDropdownOpen,
                            onDismissRequest = { subjectDropdownOpen = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            classSubjects.forEach { subj ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = subj,
                                                fontWeight = if (subj == selectedSubject) FontWeight.Bold else FontWeight.Normal,
                                                color = if (subj == selectedSubject) GenesisColors.Primary else GenesisColors.TextPrimary
                                            )
                                            if (subj == selectedSubject) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = GenesisColors.Primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedSubject = subj
                                        subjectDropdownOpen = false
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = GenesisColors.Primary)
                                },
                                text = {
                                    Text(
                                        text = "➕ Create New Subject",
                                        color = GenesisColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    subjectDropdownOpen = false
                                    newSubjectNameInput = ""
                                    newSubjectMaxMarksInput = "100"
                                    showCreateSubjectDialog = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Max Marks for $selectedSubject",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Text(
                                        text = "Type custom max marks or tap presets",
                                        fontSize = 11.sp,
                                        color = GenesisColors.TextSecondary
                                    )
                                }

                                OutlinedTextField(
                                    value = maxMarksInput,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }.take(4)
                                        maxMarksInput = filtered
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(96.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = GenesisColors.Primary
                                    ),
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Custom Max Marks",
                                            tint = GenesisColors.TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick Presets:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GenesisColors.TextSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("20", "25", "50", "80", "100").forEach { preset ->
                                        val active = maxMarksInput == preset
                                        Surface(
                                            color = if (active) GenesisColors.Primary else Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (active) GenesisColors.Primary else GenesisColors.Border),
                                            modifier = Modifier.clickable { maxMarksInput = preset }
                                        ) {
                                            Text(
                                                text = preset,
                                                color = if (active) Color.White else GenesisColors.TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGradeScaleDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enter marks once — totals, %, school grades, PDF report cards & student history update automatically.",
                                    fontSize = 12.sp,
                                    color = GenesisColors.MeteorBlack,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Tap to view Grade Rules & Marks Range for $selectedExam ➔",
                                    fontSize = 11.sp,
                                    color = GenesisColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (classRoster.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No verified students in ${selectedClass.name} yet. Add or verify students in Student Control.",
                                fontSize = 13.sp,
                                color = GenesisColors.TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(classRoster, key = { it.id }) { stu ->
                        val maxInt = maxMarksInput.toIntOrNull()?.coerceAtLeast(1) ?: 100
                        val typedVal = draftMarks[stu.id].orEmpty()
                        val numericMark = typedVal.toIntOrNull()
                        val livePct = if (numericMark != null) (numericMark.coerceIn(0, maxInt) * 100.0) / maxInt else null
                        val liveGrade = livePct?.let { SchoolGradingPolicy.assignGrade(it) }

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
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFFEFF6FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stu.rollNo,
                                        color = GenesisColors.Primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stu.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                                    if (liveGrade != null && livePct != null) {
                                        Text(
                                            text = "Auto: %.0f%% • Grade %s".format(livePct, liveGrade),
                                            fontSize = 11.sp,
                                            color = GenesisColors.Success,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text("Enter marks / $maxInt", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                                    }
                                }
                                OutlinedTextField(
                                    value = typedVal,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(3)
                                        draftMarks[stu.id] = digits
                                    },
                                    placeholder = { Text("0-$maxInt", fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.width(92.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val maxInt = maxMarksInput.toIntOrNull()?.coerceAtLeast(1) ?: 100
                                val parsedMap = classRoster.mapNotNull { stu ->
                                    val m = draftMarks[stu.id]?.toIntOrNull()
                                    if (m != null) stu.id to m.coerceIn(0, maxInt) else null
                                }.toMap()

                                if (parsedMap.isEmpty()) {
                                    Toast.makeText(context, "Enter marks for at least one student.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val count = DiscussionRepository.saveClassSubjectMarksBatch(
                                        classId = selectedClass.id,
                                        examName = selectedExam,
                                        subject = selectedSubject,
                                        maxMarks = maxInt,
                                        marksByStudentId = parsedMap
                                    )
                                    Toast.makeText(
                                        context,
                                        "Saved $selectedSubject for $count student(s). Totals, grades & report cards ready for approval!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    selectedTab = 1
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Auto-Calculate Report Cards", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // TAB 1: AUTO-GENERATED REPORT CARDS, HISTORY & APPROVAL WORKFLOW
                item {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("School Grading Rule", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GenesisColors.TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(SchoolGradingPolicy.ruleSummary, fontSize = 11.sp, color = GenesisColors.TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val pendingCount = classSheetsForExam.count { !it.isApproved }
                    if (pendingCount > 0) {
                        Button(
                            onClick = {
                                val published = DiscussionRepository.approveAllClassExamResults(
                                    classId = selectedClass.id,
                                    examName = selectedExam,
                                    approverName = "Class Teacher"
                                )
                                Toast.makeText(
                                    context,
                                    "Approved & published $published $selectedExam report card(s) to students & parents!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approve & Publish All ($pendingCount Pending)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (classSheetsForExam.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No mark sheets generated yet for ${selectedClass.name} ($selectedExam). Enter marks in the first tab to auto-generate totals, grades & report cards.",
                                fontSize = 13.sp,
                                color = GenesisColors.TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(classSheetsForExam, key = { it.id }) { sheet ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (sheet.isApproved) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
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
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color(0xFFEFF6FF), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(sheet.rollNo, color = GenesisColors.Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(sheet.studentName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
                                            Text(
                                                text = "Total: ${sheet.totalObtained}/${sheet.totalMax}  •  ${"%.1f".format(sheet.percentage)}%  •  Grade ${sheet.assignedGrade}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GenesisColors.Primary
                                            )
                                        }
                                    }
                                    Surface(
                                        color = if (sheet.isApproved) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = if (sheet.isApproved) "Published" else "Pending Approval",
                                            color = if (sheet.isApproved) GenesisColors.Success else Color(0xFFB45309),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (sheet.performanceTrend.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "History Trend: ${sheet.performanceTrend}",
                                        fontSize = 12.sp,
                                        color = GenesisColors.TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val lines = mutableListOf(
                                                "SUBJECT                    MARKS         MAX       GRADE",
                                                "--------------------------------------------------------"
                                            )
                                            sheet.subjectMarks.forEach { (sub, obt) ->
                                                val max = sheet.subjectMaxMarks[sub] ?: 100
                                                val subPct = (obt * 100.0) / max.coerceAtLeast(1)
                                                val g = SchoolGradingPolicy.assignGrade(subPct)
                                                lines.add("${sub.padEnd(26, ' ')} ${obt.toString().padEnd(13, ' ')} ${max.toString().padEnd(9, ' ')} $g")
                                            }
                                            lines.add("--------------------------------------------------------")
                                            lines.add("TOTAL: ${sheet.totalObtained}/${sheet.totalMax}  |  ${"%.1f".format(sheet.percentage)}%  |  OVERALL GRADE: ${sheet.assignedGrade}")

                                            val saved = generateAndSaveSchoolPdf(
                                                context = context,
                                                fileNameBase = "ReportCard_${sheet.rollNo}_${sheet.studentName.replace(" ", "_")}",
                                                documentCategory = "Official Auto-Generated Report Card • AY 2026-27",
                                                documentTitle = "${sheet.examName} Results",
                                                subtitleLine = "${sheet.studentName} (Roll #${sheet.rollNo})  |  ${selectedClass.name}",
                                                metaLines = listOf(
                                                    "History: ${sheet.performanceTrend}",
                                                    "Approval Status: ${if (sheet.isApproved) "Approved & Published (${sheet.approvedBy})" else "Draft Preview (Pending Approval)"}"
                                                ),
                                                bodyLines = lines
                                            )
                                            if (saved != null) {
                                                Toast.makeText(context, "Generated PDF: ${saved.fileName}", Toast.LENGTH_SHORT).show()
                                                openPdfUri(context, saved.openableUri)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("PDF Card", color = GenesisColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (!sheet.isApproved) {
                                        Button(
                                            onClick = {
                                                DiscussionRepository.approveStudentExamResult(sheet.id, "Class Teacher")
                                                Toast.makeText(context, "Approved & published ${sheet.studentName}'s results!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Approve", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Simple 4-Step Guide at Bottom for Teachers
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "How Auto Marks Works (4 Simple Steps)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GenesisColors.TextPrimary
                        )
                        Text("1. Choose Class & Section from the 2 dropdowns above.", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Text("2. Select the Exam (e.g. Unit Test 1 / Term 1) and Subject.", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Text("3. Type marks for each student — grades & % calculate automatically.", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Text("4. Tap Save Marks, then open Report Cards to approve & publish.", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                    }
                }
            }
        }
    }
}

