package com.schoolapp.android.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.schoolapp.android.data.AdminNotice
import com.schoolapp.android.data.DiscussionRepository
import com.schoolapp.android.ui.theme.GenesisColors

@Composable
fun StudentFirstTimeLoginScreen(
    onLoginConfirmed: (
        fullName: String,
        className: String,
        section: String,
        parentPhone: String,
        parentEmail: String,
        address: String
    ) -> Unit
) {
    val classOptions by DiscussionRepository.classOptionsFlow.collectAsState()
    val sectionOptions by DiscussionRepository.sectionOptionsFlow.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("Class 10") }
    var selectedSection by remember { mutableStateOf("A") }
    var parentPhone by remember { mutableStateOf("+91 ") }
    var parentEmail by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCautionDialog by remember { mutableStateOf(false) }

    if (showCautionDialog) {
        StudentVerificationCautionDialog(
            fullName = fullName.trim(),
            className = selectedClass,
            section = selectedSection,
            parentPhone = parentPhone.trim(),
            parentEmail = parentEmail.trim(),
            address = address.trim(),
            onDismiss = { showCautionDialog = false },
            onConfirm = {
                showCautionDialog = false
                onLoginConfirmed(
                    fullName.trim(),
                    selectedClass,
                    selectedSection,
                    parentPhone.trim(),
                    parentEmail.trim(),
                    address.trim()
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Meteor Black Top School Branding Header with seamless status bar contrast
        Surface(
            color = GenesisColors.MeteorBlack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(GenesisColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "GENESIS SCHOOL BEERAMGUDA",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Official Student & Parent Onboarding",
                    color = GenesisColors.SkyAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Caution Info Banner
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Strict Classroom Verification: Enter your genuine Full Name, Class, Section & Parent Contact. Your entry goes directly to your Class Teacher's Section Roster—fake or wrong-section logins will be BLOCKED by your teacher.",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Student Full Name (As per School Records) *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            errorMsg = null
                        },
                        placeholder = {
                            Text("e.g. Rachoti Sahasra", fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = GenesisColors.Primary)
                        },
                        trailingIcon = {
                            VoiceMicIconButton(
                                promptHint = "Speak student's full name...",
                                isAcademicText = false,
                                onResultText = { spoken ->
                                    fullName = spoken.removeSuffix(".").removeSuffix("?")
                                    errorMsg = null
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "2. Choose Your Class *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your current class from the options below:",
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    classOptions.chunked(4).forEach { rowClasses ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowClasses.forEach { cls ->
                                val isSelected = selectedClass == cls
                                Surface(
                                    color = if (isSelected) GenesisColors.Primary else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) GenesisColors.Primary else GenesisColors.Border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clickable { selectedClass = cls }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = cls,
                                            color = if (isSelected) Color.White else GenesisColors.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "3. Choose Your Section *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your assigned classroom section:",
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    sectionOptions.chunked(6).forEach { rowSecs ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSecs.forEach { sec ->
                                val isSelected = selectedSection == sec
                                Surface(
                                    color = if (isSelected) GenesisColors.ClassroomPurple else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) GenesisColors.ClassroomPurple else GenesisColors.Border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { selectedSection = sec }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = sec,
                                            color = if (isSelected) Color.White else GenesisColors.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "4. Parent / Guardian Mobile Number *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = {
                            parentPhone = it
                            errorMsg = null
                        },
                        placeholder = { Text("+91 98765 43210", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Call, contentDescription = null, tint = GenesisColors.Success)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "5. Parent Email Address (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = parentEmail,
                        onValueChange = { parentEmail = it },
                        placeholder = { Text("parent.name@gmail.com (Optional)", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = GenesisColors.Primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "6. Residential Address *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GenesisColors.TextPrimary
                        )
                        VoiceCaptureChipButton(
                            label = "Voice Address",
                            promptHint = "Speak your house number, street and area...",
                            isAcademicText = false,
                            onResultText = { spoken ->
                                address = mergeVoiceText(address, spoken.removeSuffix("?"))
                                errorMsg = null
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    FetchCurrentLocationButton(
                        onAddressFetched = { gpsAddress ->
                            address = gpsAddress
                            errorMsg = null
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            errorMsg = null
                        },
                        placeholder = {
                            Text("House / Flat No, Colony, Landmark, City...", fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null, tint = GenesisColors.Primary)
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Live Selection Summary Pill
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "SELECTED CLASSROOM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.Primary
                                )
                                Text(
                                    text = "$selectedClass – Section $selectedSection",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.Navy
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = GenesisColors.Primary
                            )
                        }
                    }

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMsg!!,
                            color = GenesisColors.Emergency,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val digitCount = parentPhone.count { it.isDigit() }
                            when {
                                fullName.trim().length < 2 -> {
                                    errorMsg = "Please enter your full name according to school records."
                                }
                                digitCount < 10 -> {
                                    errorMsg = "Please enter a valid 10-digit Parent Mobile Number."
                                }
                                address.trim().length < 5 -> {
                                    errorMsg = "Please enter your address or tap 'Fetch My Current Location Address'."
                                }
                                else -> {
                                    errorMsg = null
                                    showCautionDialog = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text(
                            text = "Verify & Login to Classroom",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentVerificationCautionDialog(
    fullName: String,
    className: String,
    section: String,
    parentPhone: String = "",
    parentEmail: String = "",
    address: String = "",
    isTeacherOverride: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFEF3C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTeacherOverride) Icons.Default.VerifiedUser else Icons.Default.WarningAmber,
                        contentDescription = "Caution",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isTeacherOverride) "Confirm Teacher Student Update" else "Re-verify Before Profile Lock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenesisColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isTeacherOverride) {
                        "You are updating this student's official school record as a Teacher/Admin. The student's isolated study notes and classroom discussion feed will immediately switch to this Class & Section."
                    } else {
                        "STRICT WARNING: Your login will appear on the $className-$section Class Teacher Verification Roster. Please verify your Full Name, Class, and Section carefully."
                    },
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isTeacherOverride) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = GenesisColors.Emergency,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Class Teachers verify every student in the classroom. Suspected or wrong-section accounts will be BLOCKED by the teacher, and Class/Section will be locked after confirmation.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.Emergency,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Student Full Name:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text(fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = GenesisColors.Border)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Selected Class:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text(className, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = GenesisColors.Border)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Selected Section:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text("Section $section", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.ClassroomPurple)
                        }
                        if (parentPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = GenesisColors.Border)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Parent Contact:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Text(parentPhone, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Success)
                            }
                        }
                        if (parentEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = GenesisColors.Border)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Parent Email:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                Text(parentEmail, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                            }
                        }
                        if (address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = GenesisColors.Border)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Address:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(address, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GenesisColors.TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit / Recheck", color = GenesisColors.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                    ) {
                        Text(
                            if (isTeacherOverride) "Save Update" else "Lock & Confirm",
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

@Composable
fun StudentLockedProfileDialog(
    fullName: String,
    studentCode: String,
    className: String,
    section: String,
    onDismiss: () -> Unit,
    onSwitchToTeacherRole: () -> Unit,
    onSelectNewYearClass: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFDCFCE7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Profile",
                        tint = GenesisColors.Success,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "🔒 LOCKED AFTER ONBOARDING",
                        color = GenesisColors.Success,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Student Classroom Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GenesisColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your Name, Class, and Section were verified and locked during first-time onboarding. Students cannot change these details themselves.",
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Student Name:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text(fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = GenesisColors.Border)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Student ID:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text("#$studentCode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = GenesisColors.Border)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Assigned Class:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text(className, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.Primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = GenesisColors.Border)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Assigned Section:", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            Text("Section $section", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GenesisColors.ClassroomPurple)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = Color(0xFFF3E8FF),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = GenesisColors.ClassroomPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Need a correction?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.ClassroomPurple
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only your Class Teacher or School Admin can modify your Name, Class, or Section.",
                            fontSize = 11.sp,
                            color = Color(0xFF581C87),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSwitchToTeacherRole,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GenesisColors.ClassroomPurple)
                    ) {
                        Text(
                            "Teacher Mode",
                            color = GenesisColors.ClassroomPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Got It", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onSelectNewYearClass,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Upgrade,
                        contentDescription = null,
                        tint = GenesisColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Academic Year / Promoted? Select New Class & Section",
                        color = GenesisColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TeacherEditStudentDialog(
    currentFullName: String,
    currentClassName: String,
    currentSection: String,
    onDismiss: () -> Unit,
    onSaveConfirmed: (fullName: String, className: String, section: String) -> Unit
) {
    val classOptions by DiscussionRepository.classOptionsFlow.collectAsState()
    val sectionOptions by DiscussionRepository.sectionOptionsFlow.collectAsState()
    var fullName by remember { mutableStateOf(currentFullName) }
    var selectedClass by remember {
        mutableStateOf(
            if (classOptions.contains(currentClassName)) currentClassName else "Class 10"
        )
    }
    var selectedSection by remember {
        mutableStateOf(
            if (sectionOptions.contains(currentSection)) currentSection else "A"
        )
    }
    var showConfirmStep by remember { mutableStateOf(false) }

    if (showConfirmStep) {
        StudentVerificationCautionDialog(
            fullName = fullName.trim().ifBlank { currentFullName },
            className = selectedClass,
            section = selectedSection,
            isTeacherOverride = true,
            onDismiss = { showConfirmStep = false },
            onConfirm = {
                showConfirmStep = false
                onSaveConfirmed(fullName.trim().ifBlank { currentFullName }, selectedClass, selectedSection)
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Surface(
                    color = Color(0xFFF3E8FF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "👩‍🏫 TEACHER / ADMIN AUTHORIZED EDIT",
                        color = GenesisColors.ClassroomPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Edit Student School Details",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GenesisColors.TextPrimary
                        )
                        Text(
                            text = "Modify locked student Name, Class, or Section",
                            fontSize = 12.sp,
                            color = GenesisColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Student Full Name (Official Record)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Assign Class", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                classOptions.chunked(4).forEach { rowClasses ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowClasses.forEach { cls ->
                            val isSelected = selectedClass == cls
                            Surface(
                                color = if (isSelected) GenesisColors.Primary else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) GenesisColors.Primary else GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable { selectedClass = cls }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = cls,
                                        color = if (isSelected) Color.White else GenesisColors.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Assign Section", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                sectionOptions.chunked(6).forEach { rowSecs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowSecs.forEach { sec ->
                            val isSelected = selectedSection == sec
                            Surface(
                                color = if (isSelected) GenesisColors.ClassroomPurple else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) GenesisColors.ClassroomPurple else GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { selectedSection = sec }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = sec,
                                        color = if (isSelected) Color.White else GenesisColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { showConfirmStep = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.ClassroomPurple)
                    ) {
                        Text("Save by Teacher", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeClassSectionDialog(
    currentFullName: String,
    currentClassName: String,
    currentSection: String,
    onDismiss: () -> Unit,
    onSaveConfirmed: (fullName: String, className: String, section: String) -> Unit
) {
    TeacherEditStudentDialog(
        currentFullName = currentFullName,
        currentClassName = currentClassName,
        currentSection = currentSection,
        onDismiss = onDismiss,
        onSaveConfirmed = onSaveConfirmed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentFlowScreen(
    onBack: () -> Unit,
    onStudentAdded: (admissionNo: String, fullName: String, className: String, section: String) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Enter Details, 2: Verify, 3: Success
    BackHandler {
        if (currentStep == 2) currentStep = 1 else onBack()
    }
    var admissionNumber by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("Rachoti Aarav") }
    var dob by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("10") }
    var selectedSection by remember { mutableStateOf("A") }
    var otpCode by remember { mutableStateOf("482910") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Add Another Student", onBack = onBack)

        // 3-Step Stepper Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepIndicatorItem(step = 1, label = "Student Details", currentStep = currentStep)
            Divider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                color = if (currentStep >= 2) GenesisColors.Primary else GenesisColors.Border,
                thickness = 2.dp
            )
            StepIndicatorItem(step = 2, label = "Verify", currentStep = currentStep)
            Divider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                color = if (currentStep >= 3) GenesisColors.Primary else GenesisColors.Border,
                thickness = 2.dp
            )
            StepIndicatorItem(step = 3, label = "Complete", currentStep = currentStep)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (currentStep) {
                1 -> {
                    Text("Admission Number", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = admissionNumber,
                        onValueChange = {
                            admissionNumber = it
                            errorMsg = null
                        },
                        placeholder = { Text("Enter admission number (e.g. 2419)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Student Full Name", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        placeholder = { Text("Enter student name", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Date of Birth", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        placeholder = { Text("Select date of birth (DD/MM/YYYY)", fontSize = 13.sp) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = GenesisColors.TextSecondary,
                                modifier = Modifier.clickable { dob = "14/08/2012" }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Class", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("5", "8", "9", "10").forEach { cls ->
                            SubjectChip(
                                label = "Class $cls",
                                selected = selectedClass == cls,
                                onClick = { selectedClass = cls }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Section", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "B", "C", "F").forEach { sec ->
                            SubjectChip(
                                label = "Sec $sec",
                                selected = selectedSection == sec,
                                onClick = { selectedSection = sec }
                            )
                        }
                    }

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(errorMsg!!, color = GenesisColors.Emergency, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (admissionNumber.isBlank()) {
                                errorMsg = "Please enter a valid Admission Number."
                            } else {
                                currentStep = 2
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Verify Student", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                2 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Student Record Found", fontSize = 12.sp, color = GenesisColors.Success, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(studentName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Admission ID: $admissionNumber • Class $selectedClass-$selectedSection", fontSize = 13.sp, color = GenesisColors.TextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Enter 6-digit OTP sent to registered parent mobile (+91 98765 *****):", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text("Verification OTP") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { currentStep = 3 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Confirm & Link Student", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                3 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFDCFCE7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = GenesisColors.Success,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Student Linked Successfully!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "$studentName (ID: $admissionNumber) has been linked to your parent account. You can now switch profiles anytime from Home.",
                                fontSize = 13.sp,
                                color = GenesisColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    onStudentAdded(admissionNumber, studentName, selectedClass, selectedSection)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                            ) {
                                Text("Go to Student Home", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicatorItem(step: Int, label: String, currentStep: Int) {
    val active = currentStep >= step
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (active) GenesisColors.Primary else Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (currentStep > step) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = step.toString(),
                    color = if (active) Color.White else GenesisColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) GenesisColors.Primary else GenesisColors.TextSecondary
        )
    }
}

@Composable
fun NoticeDetailsScreen(
    notice: AdminNotice,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Notice Details", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            notice.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = notice.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            CategoryBadge(notice.category)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notice.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GenesisColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${notice.date} • Posted by ${notice.author}",
                fontSize = 12.sp,
                color = GenesisColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = notice.content,
                fontSize = 14.sp,
                color = GenesisColors.TextPrimary,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Venue: ${notice.venue}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Time: ${notice.eventTime}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Attachments (1)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = GenesisColors.Emergency)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notice.attachmentName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(notice.attachmentSize, fontSize = 12.sp, color = GenesisColors.TextSecondary)
                    }
                    IconButton(onClick = {
                        val saved = generateAndSaveSchoolPdf(
                            context = context,
                            fileNameBase = notice.attachmentName.removeSuffix(".pdf"),
                            documentCategory = "Official School Circular • ${notice.category}",
                            documentTitle = notice.title,
                            subtitleLine = "Issued by ${notice.author}  |  ${notice.date}",
                            metaLines = listOf(
                                "Venue: ${notice.venue}   •   Schedule: ${notice.eventTime}"
                            ),
                            bodyLines = listOf(
                                notice.content,
                                "",
                                "For queries, kindly reach out to the Genesis School Administration Desk."
                            )
                        )
                        if (saved != null) {
                            Toast.makeText(context, "Saved to Downloads/${saved.fileName}", Toast.LENGTH_LONG).show()
                            openPdfUri(context, saved.openableUri)
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = GenesisColors.Primary)
                    }
                }
            }
        }
    }
}

@Composable
fun PublishNoticeScreen(
    onBack: () -> Unit,
    onPublish: (title: String, content: String, category: String) -> Unit
) {
    BackHandler { onBack() }
    var selectedCategory by remember { mutableStateOf("Event") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hasBannerImage by remember { mutableStateOf(false) }
    var sendToAll by remember { mutableStateOf(true) }

    val categories = listOf("Event", "Academic", "Circular", "Emergency")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = "Publish Notice", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    val activeColor = if (cat == "Emergency") GenesisColors.Emergency else GenesisColors.Primary
                    SubjectChip(
                        label = cat,
                        selected = selectedCategory == cat,
                        activeColor = activeColor,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Title", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                VoiceCaptureChipButton(
                    label = "Voice Title",
                    promptHint = "Speak notice headline...",
                    isAcademicText = false,
                    onResultText = { spoken -> title = spoken.removeSuffix(".").removeSuffix("?") }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Enter notice title", fontSize = 13.sp) },
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
                Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                VoiceCaptureChipButton(
                    label = "Voice Dictate",
                    promptHint = "Speak notice announcement clearly...",
                    isAcademicText = false,
                    onResultText = { spoken -> description = mergeVoiceText(description, spoken) }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Type or speak your announcement...", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Upload Image (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = if (hasBannerImage) Color(0xFFDCFCE7) else Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (hasBannerImage) GenesisColors.Success else GenesisColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clickable { hasBannerImage = !hasBannerImage }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (hasBannerImage) Icons.Default.CheckCircle else Icons.Default.Upload,
                        contentDescription = null,
                        tint = if (hasBannerImage) GenesisColors.Success else GenesisColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasBannerImage) "Event_Banner_2026.jpg Attached" else "+ Add Image",
                        color = if (hasBannerImage) GenesisColors.Success else GenesisColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Target Audience (Module 6 — Connected Communication)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            var selectedTargetGroup by remember { mutableStateOf("Entire School") }
            val targetGroups = listOf("Entire School", "Specific Classes", "Specific Sections", "Selected Parents", "Individual Guardian")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                targetGroups.chunked(2).forEach { rowTargets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTargets.forEach { target ->
                            val active = selectedTargetGroup == target
                            Surface(
                                color = if (active) GenesisColors.Primary else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (active) GenesisColors.Primary else GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedTargetGroup = target
                                        sendToAll = target == "Entire School"
                                    }
                            ) {
                                Text(
                                    text = target,
                                    color = if (active) Color.White else GenesisColors.TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Multilingual Parent Template (1-Tap Fill)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectChip(
                    label = "English",
                    selected = false,
                    onClick = {
                        title = "PTM & Term-1 Progress Review — Saturday"
                        description = "Dear Parents, Parent-Teacher Meeting for Term-1 progress review will be held this Saturday from 09:00 AM to 12:30 PM. Read acknowledgment is tracked."
                    }
                )
                SubjectChip(
                    label = "Telugu (తెలుగు)",
                    selected = false,
                    onClick = {
                        title = "తల్లిదండ్రుల-ఉపాధ్యాయుల సమావేశం (PTM)"
                        description = "ప్రియమైన తల్లిదండ్రులకు, ఈ శనివారం ఉదయం 09:00 నుండి 12:30 వరకు పాఠశాలలో PTM నిర్వహించబడుతుంది. దయచేసి తప్పక హాజరు కాగలరు."
                    }
                )
                SubjectChip(
                    label = "Hindi (हिंदी)",
                    selected = false,
                    onClick = {
                        title = "अभिभावक-शिक्षक बैठक (PTM) सूचना"
                        description = "प्रिय अभिभावक, इस शनिवार सुबह 09:00 से 12:30 बजे तक विद्यालय में PTM आयोजित की जाएगी। कृपया समय पर पधारें।"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onPublish(title, description, selectedCategory)
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
            ) {
                Text("Publish Notice", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

