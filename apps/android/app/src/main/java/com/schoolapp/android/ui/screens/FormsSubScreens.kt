package com.schoolapp.android.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.schoolapp.android.data.DiscussionRepository
import com.schoolapp.android.data.StudentProfile
import com.schoolapp.android.ui.theme.GenesisColors

@Composable
fun LeaveApplicationScreen(
    onBack: () -> Unit,
    onSubmitLeave: (leaveType: String, startDate: String, endDate: String, reason: String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var leaveType by remember { mutableStateOf("Sick Leave") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var attachedDocName by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName = "Supporting_Document"
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) displayName = cursor.getString(idx) ?: displayName
                    }
                }
            }
            attachedDocName = displayName
        }
    }

    val leaveHistory by DiscussionRepository.leaveRequests.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Leave Application", onBack = onBack)

        if (isSubmitted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFDCFCE7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = GenesisColors.Success, modifier = Modifier.size(38.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Leave Application Submitted", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your $leaveType request ($startDate to $endDate) has been sent to the Class Teacher for approval.",
                    fontSize = 14.sp,
                    color = GenesisColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                StatusChip("Pending Review")
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Text("Back to Services", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Leave Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Sick Leave", "Casual Leave", "Family Function").forEach { type ->
                        val selected = leaveType == type
                        Surface(
                            color = if (selected) GenesisColors.Primary else Color.White,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, if (selected) GenesisColors.Primary else GenesisColors.Border),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable { leaveType = type }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = type,
                                    color = if (selected) Color.White else GenesisColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Start Date", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it; errorText = null },
                    placeholder = { Text("DD MMM YYYY (e.g. 19 Nov 2026)", fontSize = 13.sp) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = GenesisColors.Primary,
                            modifier = Modifier.clickable { startDate = "19 Nov 2026" }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("End Date", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it; errorText = null },
                    placeholder = { Text("DD MMM YYYY (e.g. 20 Nov 2026)", fontSize = 13.sp) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = GenesisColors.Primary,
                            modifier = Modifier.clickable { endDate = "20 Nov 2026" }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reason for Leave", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    VoiceCaptureChipButton(
                        label = "Voice Reason",
                        promptHint = "Speak reason for student leave...",
                        isAcademicText = false,
                        onResultText = { spoken ->
                            reason = mergeVoiceText(reason, spoken)
                            errorText = null
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; errorText = null },
                    placeholder = { Text("Type or speak reason for leave...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Supporting Medical / Leave Note (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = if (attachedDocName != null) Color(0xFFDCFCE7) else Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (attachedDocName != null) GenesisColors.Success else GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { docPickerLauncher.launch("*/*") }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            if (attachedDocName != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = if (attachedDocName != null) GenesisColors.Success else GenesisColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (attachedDocName != null) "Attached: $attachedDocName" else "Select PDF or Photo from Phone",
                            color = if (attachedDocName != null) GenesisColors.Success else GenesisColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorText!!, color = GenesisColors.Emergency, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (startDate.isBlank() || endDate.isBlank() || reason.isBlank()) {
                            errorText = "Please fill in Start Date, End Date, and Reason."
                        } else {
                            onSubmitLeave(leaveType, startDate, endDate, reason)
                            isSubmitted = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Text("Submit Leave Application", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                if (leaveHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Leave Application History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    leaveHistory.forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${item.leaveType} (${item.startDate} – ${item.endDate})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(item.reason, fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                }
                                StatusChip(item.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeeDetailsScreen(
    student: StudentProfile,
    onBack: () -> Unit,
    onPaySuccess: () -> Unit = {}
) {
    BackHandler { onBack() }
    val termFee = student.totalFeeInr
    val annualFee = termFee * 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Fee Structure", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "${student.classTitle} • AY 2026-27",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFBFDBFE)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹ %,d / Term".format(termFee),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Annual Fee: ₹ %,d (3 Terms)".format(annualFee),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDBEAFE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Term Fee Breakdown",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GenesisColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    FeeRow("Tuition Fee", "₹ %,d".format(student.tuitionFeeInr))
                    Spacer(modifier = Modifier.height(12.dp))
                    FeeRow("Examination & Lab Fee", "₹ %,d".format(student.examFeeInr))
                    Spacer(modifier = Modifier.height(12.dp))
                    FeeRow("Sports & Smart Class Fee", "₹ %,d".format(student.activityFeeInr))
                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = GenesisColors.Border)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Per Term", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
                        Text("₹ %,d".format(termFee), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = GenesisColors.Primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Term-Wise Fee Schedule",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GenesisColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            val terms = listOf(
                Triple("Term 1", "June – September", "₹ %,d".format(termFee)),
                Triple("Term 2", "October – December", "₹ %,d".format(termFee)),
                Triple("Term 3", "January – March", "₹ %,d".format(termFee))
            )

            terms.forEach { (termTitle, periodText, amountText) ->
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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(termTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(periodText, fontSize = 13.sp, color = GenesisColors.TextSecondary)
                        }
                        Text(
                            text = amountText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GenesisColors.Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeRow(label: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = GenesisColors.TextSecondary)
        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
    }
}

@Composable
fun TransportPassScreen(
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
        SubScreenTopBar(title = "Bus Transport Pass", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Assigned Bus Route", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = GenesisColors.Primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(student.busRoute, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Verified RFID Boarding Pass Active", fontSize = 12.sp, color = GenesisColors.Success, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = GenesisColors.Border)

                    Text("Boarding Stop", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(student.pickupStop, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Morning Pickup", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(student.pickupTime, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Afternoon Drop", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(student.dropTime, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Stop change request submitted to Transport Officer.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.Primary)
                    ) {
                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Request Stop / Route Change", color = GenesisColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = GenesisColors.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Transport Helpline: +91 40 2345 6789 (Available 6:30 AM – 5:00 PM on working days).",
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryRenewalScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val books by DiscussionRepository.libraryBooks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Library Book Renewal", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Issued Library Books", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            books.forEach { book ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Author: ${book.author}", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            }
                            StatusChip(if (book.isRenewed) "Approved" else "Due Soon")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Issued: ${book.issueDate} • Due: ${book.dueDate}", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { DiscussionRepository.renewLibraryBook(book.id) },
                            enabled = !book.isRenewed,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                        ) {
                            Text(
                                text = if (book.isRenewed) "Renewed for 14 Days" else "Request 14-Day Renewal",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateStudentProfileScreen(
    student: StudentProfile,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var phone by remember(student.id) { mutableStateOf(student.parentPhone) }
    var email by remember(student.id) { mutableStateOf(student.parentEmail) }
    var address by remember(student.id) { mutableStateOf(student.address) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Student & Parent Profile", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = student.avatarUrl,
                contentDescription = student.fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(3.dp, GenesisColors.Primary, CircleShape)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(student.fullName, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
            Text("${student.classTitle} • ID #${student.studentCode}", fontSize = 13.sp, color = GenesisColors.TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "🔒 Official Class & Section Verified",
                    color = GenesisColors.Success,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Parent Contact Number *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = GenesisColors.Success) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Parent Email Address (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("parent.email@gmail.com (Optional)", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GenesisColors.Primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        Text("Residential Address *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextPrimary)
                        VoiceCaptureChipButton(
                            label = "Voice Address",
                            promptHint = "Speak residential address...",
                            isAcademicText = false,
                            onResultText = { spoken -> address = mergeVoiceText(address, spoken.removeSuffix("?")) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FetchCurrentLocationButton(
                        onAddressFetched = { gpsAddress ->
                            address = gpsAddress
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = GenesisColors.Primary) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (phone.count { it.isDigit() } < 10) {
                        Toast.makeText(context, "Please enter a valid 10-digit Parent Contact number.", Toast.LENGTH_SHORT).show()
                    } else {
                        DiscussionRepository.updateActiveStudentProfile(phone.trim(), email.trim(), address.trim(), context)
                        Toast.makeText(context, "Parent contact & address saved!", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
            ) {
                Text("Save Contact & Address", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ComplaintSuggestionScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    var category by remember { mutableStateOf("Academic") }
    var priority by remember { mutableStateOf("Normal") }
    var message by remember { mutableStateOf("") }
    var createdTicketId by remember { mutableStateOf<String?>(null) }

    val tickets by DiscussionRepository.helpdeskTickets.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Parent Helpdesk & Suggestions", onBack = onBack)

        if (createdTicketId != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GenesisColors.Success, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text("Helpdesk Ticket Registered", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Your feedback has been routed to the Principal's Desk under Ticket #$createdTicketId. Expected response within 24 working hours.",
                    fontSize = 13.sp,
                    color = GenesisColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        createdTicketId = null
                        message = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Text("View Ticket Status", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Department Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Academic", "Transport", "Accounts / Fees", "Facilities", "Safety").forEach { cat ->
                        SubjectChip(label = cat, selected = category == cat, onClick = { category = cat })
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Priority Level", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Normal", "High Priority", "Urgent").forEach { p ->
                        val activeCol = if (p == "Urgent") GenesisColors.Emergency else GenesisColors.Primary
                        SubjectChip(label = p, selected = priority == p, activeColor = activeCol, onClick = { priority = p })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Concern or Suggestion", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    VoiceCaptureChipButton(
                        label = "Voice Dictate",
                        promptHint = "Speak your message or suggestion clearly...",
                        isAcademicText = false,
                        onResultText = { spoken -> message = mergeVoiceText(message, spoken) }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Share your concern or suggestion confidentially with school management...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (message.isNotBlank()) {
                            val ticket = DiscussionRepository.submitHelpdeskTicket(category, priority, message)
                            createdTicketId = ticket.ticketId
                        }
                    },
                    enabled = message.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Text("Submit to Principal's Desk", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (tickets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Tracked Helpdesk Tickets", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    tickets.forEach { t ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${t.ticketId} • ${t.category}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GenesisColors.Primary)
                                    StatusChip(if (t.status == "Resolved") "Approved" else "Pending Review")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(t.message, fontSize = 12.sp, color = GenesisColors.TextPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Priority: ${t.priority} • Logged: ${t.createdDate}", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReviewRequestScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val requests by DiscussionRepository.leaveRequests.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Staff Leave & Request Approvals", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Student Leave & Service Requests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            requests.forEach { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.studentName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            StatusChip(req.status)
                        }
                        Text("${req.className} • ${req.leaveType}", fontSize = 12.sp, color = GenesisColors.Primary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Dates: ${req.startDate} to ${req.endDate}", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                        Text("Reason: ${req.reason}", fontSize = 13.sp, color = GenesisColors.TextPrimary)

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { DiscussionRepository.updateLeaveStatus(req.id, "Approved") },
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { DiscussionRepository.updateLeaveStatus(req.id, "Rejected") },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GenesisColors.Emergency),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GenesisColors.Emergency),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject", color = GenesisColors.Emergency, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

