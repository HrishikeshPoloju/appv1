package com.schoolapp.android.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.schoolapp.android.R
import com.schoolapp.android.data.AdminNotice
import com.schoolapp.android.data.AppRole
import com.schoolapp.android.data.DiscussionRepository
import com.schoolapp.android.ui.theme.GenesisColors
import com.schoolapp.android.ui.theme.LocalSchoolBrand
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeSchool by DiscussionRepository.activeSchool.collectAsState()
    val classOptions by DiscussionRepository.classOptionsFlow.collectAsState()
    val sectionsByClass by DiscussionRepository.sectionsByClassFlow.collectAsState()
    val classesLoading by DiscussionRepository.classesLoadingFlow.collectAsState()

    val brandColor = remember(activeSchool.primaryColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(activeSchool.primaryColorHex)) }
            .getOrDefault(GenesisColors.Primary)
    }

    var schoolCodeInput by remember { mutableStateOf(activeSchool.schoolCode) }
    var schoolVerifyStatus by remember { mutableStateOf<String?>(null) }
    var isVerifyingSchool by remember { mutableStateOf(false) }

    // Step 1: null (Select School & Role Portal) -> Step 2: selectedRole (Dedicated Role Login / Sign Up)
    var activeRolePortal by remember { mutableStateOf<AppRole?>(null) }
    // 0 = First-Time Sign Up, 1 = Sign In (Existing Account)
    var authModeTab by remember { mutableStateOf(0) }

    // Shared + Role-specific fields
    var roleSecretCode by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var parentEmail by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    // Parent-specific
    var childName by remember { mutableStateOf("") }
    // Teacher-specific
    var employeeId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    // Admin-specific
    var designation by remember { mutableStateOf("Principal") }

    var isSubmittingAuth by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCautionDialog by remember { mutableStateOf(false) }

    // Hardware Back inside Step 2 returns to Step 1 (Role Portal Selector)
    BackHandler(enabled = activeRolePortal != null) {
        activeRolePortal = null
        errorMsg = null
    }

    // Fetch live school classes & sections from server once when login screen opens
    LaunchedEffect(Unit) {
        DiscussionRepository.fetchSchoolClassesFromBackend(coroutineScope)
    }

    // Auto-select first available class when loaded
    LaunchedEffect(classOptions) {
        if (classOptions.isNotEmpty() && selectedClass !in classOptions) {
            selectedClass = classOptions.first()
        }
    }

    val sectionsForSelectedClass = remember(selectedClass, sectionsByClass) {
        sectionsByClass[selectedClass].orEmpty()
    }

    LaunchedEffect(selectedClass, sectionsForSelectedClass) {
        if (sectionsForSelectedClass.isNotEmpty() && selectedSection !in sectionsForSelectedClass) {
            selectedSection = sectionsForSelectedClass.first()
        } else if (sectionsForSelectedClass.isEmpty()) {
            selectedSection = ""
        }
    }

    val formattedParentPhone = remember(parentPhone) {
        val digits = parentPhone.filter { it.isDigit() }.take(10)
        if (digits.isNotBlank()) "+91 $digits" else ""
    }

    if (showCautionDialog && activeRolePortal == AppRole.STUDENT) {
        StudentVerificationCautionDialog(
            fullName = fullName.trim(),
            className = selectedClass,
            section = selectedSection,
            parentPhone = formattedParentPhone,
            parentEmail = parentEmail.trim(),
            address = address.trim(),
            onDismiss = { showCautionDialog = false },
            onConfirm = {
                showCautionDialog = false
                isSubmittingAuth = true
                DiscussionRepository.registerWithPassword(
                    context = context,
                    schoolCode = schoolCodeInput.ifBlank { activeSchool.schoolCode },
                    role = AppRole.STUDENT,
                    accessCode = roleSecretCode.trim(),
                    fullName = fullName.trim(),
                    phone = formattedParentPhone,
                    password = password,
                    className = selectedClass,
                    section = selectedSection,
                    parentEmail = parentEmail.trim(),
                    address = address.trim(),
                    scope = coroutineScope
                ) { ok, msg ->
                    isSubmittingAuth = false
                    if (ok) {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        errorMsg = msg
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Dynamic School Branding Header (changes live when School Code is resolved from DB)
        val brand = LocalSchoolBrand.current
        Surface(
            color = Color.Transparent,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(brandColor, brand.headerEnd)
                        )
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.asset_reading_student),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.24f)
                )
                HeaderVectorBackdrop(
                    accentColor = Color.White,
                    modifier = Modifier.matchParentSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.16f), CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeSchool.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = DiscussionRepository.resolveRenderableMediaUrl(activeSchool.logoUrl),
                                contentDescription = activeSchool.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = activeSchool.name.uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeSchool.tagline,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // STEP 1: School Code + 4 Isolated Role Portals
            if (activeRolePortal == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "1. Enter Your School Code *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GenesisColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter the school code given by your campus office to load your school's portal.",
                            fontSize = 12.sp,
                            color = GenesisColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = schoolCodeInput,
                                onValueChange = {
                                    schoolCodeInput = it.uppercase().take(20)
                                    schoolVerifyStatus = null
                                    errorMsg = null
                                },
                                placeholder = { Text("e.g. GENESIS or OAKRIDGE", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Apartment, contentDescription = null, tint = brandColor) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    isVerifyingSchool = true
                                    DiscussionRepository.resolveSchoolByCode(
                                        code = schoolCodeInput,
                                        context = context,
                                        scope = coroutineScope
                                    ) { ok, msg, _ ->
                                        isVerifyingSchool = false
                                        schoolVerifyStatus = if (ok) "✓ $msg" else msg
                                    }
                                },
                                enabled = !isVerifyingSchool && schoolCodeInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(
                                    text = if (isVerifyingSchool) "..." else "Load",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (schoolVerifyStatus != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = schoolVerifyStatus!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (schoolVerifyStatus!!.startsWith("✓")) GenesisColors.Success else GenesisColors.Emergency
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "2. Choose Your Role Portal",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = GenesisColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Each role has its own dedicated login and requires your school's secret access code.",
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                data class RolePortalOption(
                    val role: AppRole,
                    val title: String,
                    val subtitle: String,
                    val icon: androidx.compose.ui.graphics.vector.ImageVector,
                    val tint: Color,
                    val softBg: Color
                )
                val roleOptions = listOf(
                    RolePortalOption(
                        role = AppRole.STUDENT,
                        title = "Student",
                        subtitle = "Homework, Timetable, Attendance, Results & AI Tutor",
                        icon = Icons.Rounded.School,
                        tint = Color(0xFF2563EB),
                        softBg = Color(0xFFDBEAFE)
                    ),
                    RolePortalOption(
                        role = AppRole.TEACHER,
                        title = "Teacher",
                        subtitle = "Roll Call, Auto Marks, Assignments & Class Roster",
                        icon = Icons.Rounded.MenuBook,
                        tint = Color(0xFF1D4ED8),
                        softBg = Color(0xFFE0E7FF)
                    ),
                    RolePortalOption(
                        role = AppRole.ADMIN,
                        title = "Admin / Principal",
                        subtitle = "Campus Operations, Staff HR, Classes & Analytics",
                        icon = Icons.Rounded.AdminPanelSettings,
                        tint = Color(0xFF0F766E),
                        softBg = Color(0xFFD1FAE5)
                    )
                )

                roleOptions.forEach { opt ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                activeRolePortal = opt.role
                                roleSecretCode = ""
                                errorMsg = null
                                authModeTab = 0
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(opt.softBg, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = opt.title,
                                    tint = opt.tint,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = opt.subtitle,
                                    fontSize = 12.sp,
                                    color = GenesisColors.TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(opt.softBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowForward,
                                    contentDescription = "Continue as ${opt.title}",
                                    tint = opt.tint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // STEP 2: Dedicated Role Login / Registration Screen
                val role = activeRolePortal!!
                val roleTitle = when (role) {
                    AppRole.STUDENT -> "Student"
                    AppRole.PARENT -> "Parent"
                    AppRole.TEACHER -> "Teacher"
                    AppRole.ADMIN -> "Admin / Principal"
                }
                val secretCodeLabel = when (role) {
                    AppRole.STUDENT -> "Student Secret Code *"
                    AppRole.PARENT -> "Parent / Student Secret Code *"
                    AppRole.TEACHER -> "Teacher Secret Code *"
                    AppRole.ADMIN -> "Admin Secret Code *"
                }
                val secretCodeHint = when (role) {
                    AppRole.STUDENT, AppRole.PARENT -> "Provided by your school office"
                    AppRole.TEACHER -> "Staff code provided by school admin"
                    AppRole.ADMIN -> "Principal / management access code"
                }

                // Role Header + Change Role button
                Surface(
                    color = Color(0xFFDBEAFE),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    activeRolePortal = null
                                    errorMsg = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBack,
                                    contentDescription = "Change Role",
                                    tint = Color(0xFF1D4ED8)
                                )
                            }
                            Column {
                                Text(
                                    text = "$roleTitle Portal",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GenesisColors.TextPrimary
                                )
                                Text(
                                    text = "${activeSchool.name} (${schoolCodeInput.ifBlank { activeSchool.schoolCode }})",
                                    fontSize = 11.sp,
                                    color = GenesisColors.TextSecondary
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                activeRolePortal = null
                                errorMsg = null
                            }
                        ) {
                            Text("Change Role", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Switcher: First-Time Sign Up vs Sign In
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "First-Time Sign Up", 1 to "Sign In").forEach { (tabIdx, label) ->
                        val selected = authModeTab == tabIdx
                        Surface(
                            color = if (selected) brandColor else Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) brandColor else GenesisColors.Border),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    authModeTab = tabIdx
                                    errorMsg = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.White else GenesisColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (authModeTab == 1) {
                    // DEDICATED ROLE SIGN IN
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "$roleTitle Sign In",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Text(
                                text = "Verify your $roleTitle secret code and enter your registered mobile number & password.",
                                fontSize = 12.sp,
                                color = GenesisColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = roleSecretCode,
                                onValueChange = {
                                    roleSecretCode = it.uppercase().take(32)
                                    errorMsg = null
                                },
                                label = { Text(secretCodeLabel) },
                                placeholder = { Text(secretCodeHint, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = brandColor) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = parentPhone,
                                onValueChange = {
                                    parentPhone = it.filter { ch -> ch.isDigit() }.take(10)
                                    errorMsg = null
                                },
                                label = { Text("10-Digit Mobile Number *") },
                                prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMsg = null
                                },
                                label = { Text("Password *") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = brandColor) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password"
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (errorMsg != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = errorMsg!!, color = GenesisColors.Emergency, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    val digits = parentPhone.filter { it.isDigit() }
                                    when {
                                        roleSecretCode.trim().length < 3 -> errorMsg = "Please enter your school's $roleTitle Secret Code."
                                        digits.length != 10 -> errorMsg = "Enter your 10-digit registered mobile number."
                                        password.length < 4 -> errorMsg = "Enter your password (min 4 characters)."
                                        else -> {
                                            errorMsg = null
                                            isSubmittingAuth = true
                                            DiscussionRepository.signInWithPassword(
                                                context = context,
                                                schoolCode = schoolCodeInput.ifBlank { activeSchool.schoolCode },
                                                role = role,
                                                accessCode = roleSecretCode.trim(),
                                                phone = formattedParentPhone,
                                                password = password,
                                                scope = coroutineScope
                                            ) { ok, msg ->
                                                isSubmittingAuth = false
                                                if (ok) {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    errorMsg = msg
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmittingAuth,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                            ) {
                                Text(
                                    text = if (isSubmittingAuth) "Verifying & Signing In..." else "Sign In as $roleTitle",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // DEDICATED ROLE FIRST-TIME REGISTRATION
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GenesisColors.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "$roleTitle Registration",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Text(
                                text = "Complete the $roleTitle details below to activate your account.",
                                fontSize = 12.sp,
                                color = GenesisColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 1. Role Secret Access Code (mandatory for all 4 roles)
                            Text(
                                text = secretCodeLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = roleSecretCode,
                                onValueChange = {
                                    roleSecretCode = it.uppercase().take(32)
                                    errorMsg = null
                                },
                                placeholder = { Text(secretCodeHint, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = brandColor) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Full Name (label tailored to role)
                            val nameLabel = when (role) {
                                AppRole.STUDENT -> "Student Full Name (As per School Records) *"
                                AppRole.PARENT -> "Parent / Guardian Full Name *"
                                AppRole.TEACHER -> "Teacher Full Name *"
                                AppRole.ADMIN -> "Principal / Admin Full Name *"
                            }
                            Text(
                                text = nameLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    errorMsg = null
                                },
                                placeholder = { Text("Enter full name", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = brandColor) },
                                trailingIcon = {
                                    VoiceMicIconButton(
                                        promptHint = "Speak full name...",
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

                            // ROLE-SPECIFIC INPUTS
                            when (role) {
                                AppRole.PARENT -> {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Child's Full Name *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = childName,
                                        onValueChange = {
                                            childName = it
                                            errorMsg = null
                                        },
                                        placeholder = { Text("e.g. Rachoti Sahasra", fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null, tint = Color(0xFF0F766E)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                AppRole.TEACHER -> {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Employee / Staff ID *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = employeeId,
                                        onValueChange = {
                                            employeeId = it.uppercase().take(24)
                                            errorMsg = null
                                        },
                                        placeholder = { Text("e.g. TCH-104", fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null, tint = brandColor) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Department / Primary Subject *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = department,
                                        onValueChange = {
                                            department = it
                                            errorMsg = null
                                        },
                                        placeholder = { Text("e.g. Mathematics, Science, English", fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = brandColor) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                AppRole.ADMIN -> {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Designation *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GenesisColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    listOf("Principal", "Vice Principal", "School Owner", "Administrator").chunked(2).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            row.forEach { d ->
                                                val sel = designation == d
                                                Surface(
                                                    color = if (sel) brandColor else Color(0xFFF8FAFC),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, if (sel) brandColor else GenesisColors.Border),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
                                                        .clickable { designation = d }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = d,
                                                            color = if (sel) Color.White else GenesisColors.TextPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                AppRole.STUDENT -> {}
                            }

                            // Class & Section Picker (for Student and Parent roles only)
                            if (role == AppRole.STUDENT || role == AppRole.PARENT) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (role == AppRole.PARENT) "Child's Class *" else "Choose Your Class *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (classesLoading && classOptions.isEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = brandColor)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading school classes...", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                                    }
                                } else {
                                    classOptions.chunked(4).forEach { rowClasses ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowClasses.forEach { cls ->
                                                val isSelected = selectedClass == cls
                                                Surface(
                                                    color = if (isSelected) brandColor else Color(0xFFF8FAFC),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, if (isSelected) brandColor else GenesisColors.Border),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
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
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (role == AppRole.PARENT) "Child's Section *" else "Choose Your Section *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GenesisColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                sectionsForSelectedClass.chunked(4).forEach { rowSecs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowSecs.forEach { sec ->
                                            val isSelected = selectedSection == sec
                                            Surface(
                                                color = if (isSelected) Color(0xFF0F766E) else Color(0xFFF8FAFC),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F766E) else GenesisColors.Border),
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
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mobile Number (strict 10 digits)
                            val phoneLabel = when (role) {
                                AppRole.STUDENT -> "Parent / Guardian Mobile Number (10 Digits) *"
                                AppRole.PARENT -> "Parent Mobile Number (10 Digits) *"
                                AppRole.TEACHER -> "Teacher Mobile Number (10 Digits) *"
                                AppRole.ADMIN -> "Official Mobile Number (10 Digits) *"
                            }
                            Text(
                                text = phoneLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = parentPhone,
                                onValueChange = { input ->
                                    parentPhone = input.filter { it.isDigit() }.take(10)
                                    errorMsg = null
                                },
                                placeholder = { Text("9876543210", fontSize = 13.sp) },
                                prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
                                supportingText = {
                                    Text(
                                        text = if (parentPhone.length == 10) "✓ Valid 10-digit number" else "${parentPhone.length}/10 digits",
                                        color = if (parentPhone.length == 10) GenesisColors.Success else GenesisColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Email (mandatory for Admin, optional for Student/Parent/Teacher)
                            val emailLabel = when (role) {
                                AppRole.ADMIN -> "Official School Email *"
                                AppRole.TEACHER -> "Official School Email (Recommended)"
                                else -> "Email Address (Optional)"
                            }
                            Text(
                                text = emailLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = parentEmail,
                                onValueChange = {
                                    parentEmail = it
                                    errorMsg = null
                                },
                                placeholder = { Text("name@school.edu", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = brandColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password
                            Text(
                                text = "Create Account Password *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GenesisColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMsg = null
                                },
                                placeholder = { Text("Minimum 4 characters", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = brandColor) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password"
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Residential Address (Required for Student only)
                            if (role == AppRole.STUDENT) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Residential Address *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
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
                                    placeholder = { Text("House / Flat No, Colony, Landmark, City...", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null, tint = brandColor) },
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                    val digits = parentPhone.filter { it.isDigit() }
                                    when {
                                        roleSecretCode.trim().length < 3 -> {
                                            errorMsg = "Please enter your school's $roleTitle Secret Code."
                                        }
                                        fullName.trim().length < 2 -> {
                                            errorMsg = "Please enter your full name."
                                        }
                                        role == AppRole.PARENT && childName.trim().length < 2 -> {
                                            errorMsg = "Please enter your child's full name."
                                        }
                                        role == AppRole.TEACHER && (employeeId.trim().isEmpty() || department.trim().isEmpty()) -> {
                                            errorMsg = "Please enter your Employee ID and Department / Subject."
                                        }
                                        role == AppRole.ADMIN && !parentEmail.contains("@") -> {
                                            errorMsg = "Please enter a valid Official School Email."
                                        }
                                        digits.length != 10 -> {
                                            errorMsg = "Please enter a valid 10-digit Mobile Number."
                                        }
                                        password.trim().length < 4 -> {
                                            errorMsg = "Please create a password (minimum 4 characters)."
                                        }
                                        role == AppRole.STUDENT && address.trim().length < 5 -> {
                                            errorMsg = "Please enter your residential address."
                                        }
                                        role == AppRole.STUDENT -> {
                                            errorMsg = null
                                            showCautionDialog = true
                                        }
                                        else -> {
                                            errorMsg = null
                                            isSubmittingAuth = true
                                            DiscussionRepository.registerWithPassword(
                                                context = context,
                                                schoolCode = schoolCodeInput.ifBlank { activeSchool.schoolCode },
                                                role = role,
                                                accessCode = roleSecretCode.trim(),
                                                fullName = fullName.trim(),
                                                phone = formattedParentPhone,
                                                password = password,
                                                className = selectedClass,
                                                section = selectedSection,
                                                parentEmail = parentEmail.trim(),
                                                address = address.trim(),
                                                childName = childName.trim(),
                                                employeeId = employeeId.trim(),
                                                department = department.trim(),
                                                designation = designation,
                                                scope = coroutineScope
                                            ) { ok, msg ->
                                                isSubmittingAuth = false
                                                if (ok) {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    errorMsg = msg
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmittingAuth,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                            ) {
                                Text(
                                    text = if (isSubmittingAuth) "Verifying & Creating Account..." else "Create $roleTitle Account",
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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Scrollable content area so buttons are pinned and never cut off
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons pinned: Guaranteed visible with explicit contrast and zero vertical clipping
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Edit / Recheck",
                            color = GenesisColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isTeacherOverride) "Save Update" else "Lock & Confirm",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
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
                        border = BorderStroke(1.dp, GenesisColors.Border)
                    ) {
                        Text(
                            "Sign Out",
                            color = GenesisColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
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

private fun formatNoticeScheduleDisplay(iso: String?): String {
    if (iso.isNullOrBlank()) return "Not set"
    return runCatching {
        val zdt = Instant.parse(iso).atZone(ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH))
    }.getOrDefault(iso)
}

private fun formatCountdownDuration(secondsRemaining: Long): String {
    val safe = secondsRemaining.coerceAtLeast(0L)
    val days = safe / 86400
    val hours = (safe % 86400) / 3600
    val mins = (safe % 3600) / 60
    val secs = safe % 60
    return if (days > 0) "${days}d ${hours}h ${mins}m ${secs}s"
    else "${hours}h ${mins}m ${secs}s"
}

@Composable
fun NoticeDetailsScreen(
    notice: AdminNotice,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    // Live 1-second ticker for real-time schedule / expiry countdown
    var nowInstant by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(notice.id, notice.startsAtIso, notice.expiresAtIso) {
        while (true) {
            nowInstant = Instant.now()
            delay(1000L)
        }
    }

    val startInstant = remember(notice.startsAtIso) {
        notice.startsAtIso?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }
    val endInstant = remember(notice.expiresAtIso) {
        notice.expiresAtIso?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            notice.imageUrl?.takeIf { it.isNotBlank() }?.let { imgUrl ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = DiscussionRepository.resolveRenderableMediaUrl(imgUrl) ?: imgUrl,
                        contentDescription = notice.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    )
                }
            }

            // Live Schedule & Expiry Countdown Card (whenever start or expiry time limit is configured)
            if (startInstant != null || endInstant != null) {
                val isScheduled = startInstant != null && startInstant.isAfter(nowInstant)
                val isExpired = endInstant != null && endInstant.isBefore(nowInstant)
                val badgeLabel: String
                val badgeBg: Color
                val badgeFg: Color
                val timerHeadline: String
                when {
                    isScheduled -> {
                        val secs = Duration.between(nowInstant, startInstant!!).seconds
                        badgeLabel = "SCHEDULED"
                        badgeBg = GenesisColors.LightBlue
                        badgeFg = GenesisColors.Primary
                        timerHeadline = "Triggers in ${formatCountdownDuration(secs)}"
                    }
                    isExpired -> {
                        badgeLabel = "EXPIRED"
                        badgeBg = Color(0xFFFEE2E2)
                        badgeFg = GenesisColors.Emergency
                        timerHeadline = "Notice validity window ended"
                    }
                    endInstant != null -> {
                        val secs = Duration.between(nowInstant, endInstant).seconds
                        badgeLabel = "LIVE ACTIVE"
                        badgeBg = Color(0xFFDCFCE7)
                        badgeFg = GenesisColors.Success
                        timerHeadline = "Expires in ${formatCountdownDuration(secs)}"
                    }
                    else -> {
                        badgeLabel = "LIVE ACTIVE"
                        badgeBg = Color(0xFFDCFCE7)
                        badgeFg = GenesisColors.Success
                        timerHeadline = "Active now (No expiry)"
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GenesisColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Timer, contentDescription = null, tint = badgeFg, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = timerHeadline,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GenesisColors.TextPrimary
                                )
                            }
                            Surface(color = badgeBg, shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeFg,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = GenesisColors.Border, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start / Trigger", fontSize = 11.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (startInstant != null) formatNoticeScheduleDisplay(notice.startsAtIso) else "Immediate",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GenesisColors.TextPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End / Expiry", fontSize = 11.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (endInstant != null) formatNoticeScheduleDisplay(notice.expiresAtIso) else "No Time Limit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GenesisColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Main Notice Content Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(notice.category)
                        Surface(color = Color(0xFFCCFBF1), shape = RoundedCornerShape(999.dp)) {
                            Text(
                                text = notice.targetAudience,
                                color = GenesisColors.Teal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GenesisColors.Border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = notice.content,
                        fontSize = 14.sp,
                        color = GenesisColors.TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Place, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Venue: ${notice.venue}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GenesisColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Time: ${notice.eventTime}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GenesisColors.TextPrimary)
                    }
                }
            }

            Text("Attachments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GenesisColors.TextPrimary)

            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                shape = RoundedCornerShape(14.dp),
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
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF", tint = GenesisColors.Emergency)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notice.attachmentName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
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
                                "Venue: ${notice.venue}   •   Schedule: ${notice.eventTime}",
                                "Audience: ${notice.targetAudience}"
                            ),
                            bodyLines = listOf(
                                notice.content,
                                "",
                                "For queries, kindly reach out to the School Administration Desk."
                            )
                        )
                        if (saved != null) {
                            Toast.makeText(context, "Saved to Downloads/${saved.fileName}", Toast.LENGTH_LONG).show()
                            openPdfUri(context, saved.openableUri)
                        }
                    }) {
                        Icon(Icons.Rounded.Download, contentDescription = "Download", tint = GenesisColors.Primary)
                    }
                }
            }
        }
    }
}

@Composable
fun PublishNoticeScreen(
    onBack: () -> Unit,
    onPublish: (
        title: String,
        content: String,
        category: String,
        imageUri: String?,
        targetAudience: String,
        startsAtIso: String?,
        expiresAtIso: String?
    ) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val classOptions by DiscussionRepository.classOptionsFlow.collectAsState()
    val sectionsByClass by DiscussionRepository.sectionsByClassFlow.collectAsState()

    LaunchedEffect(Unit) {
        DiscussionRepository.fetchSchoolClassesFromBackend(coroutineScope)
    }

    var selectedCategory by remember { mutableStateOf("Event") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // 3-Dropdown Working Target Audience (All School / Specific Class / Specific Section from school's API)
    var audienceMode by remember { mutableStateOf("All School") }
    var audienceMenuExpanded by remember { mutableStateOf(false) }
    var selectedTargetClass by remember { mutableStateOf("") }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var selectedTargetSection by remember { mutableStateOf("") }
    var sectionMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(classOptions) {
        if (classOptions.isNotEmpty() && selectedTargetClass !in classOptions) {
            selectedTargetClass = classOptions.first()
        }
    }
    val sectionsForTargetClass = remember(selectedTargetClass, sectionsByClass) {
        sectionsByClass[selectedTargetClass].orEmpty()
    }
    LaunchedEffect(selectedTargetClass, sectionsForTargetClass) {
        if (sectionsForTargetClass.isNotEmpty() && selectedTargetSection !in sectionsForTargetClass) {
            selectedTargetSection = sectionsForTargetClass.first()
        } else if (sectionsForTargetClass.isEmpty()) {
            selectedTargetSection = ""
        }
    }

    // Real Camera & Gallery Notice Image Upload state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cached = copyUriToCacheFile(context, uri) ?: uri
            selectedImageUri = cached
            capturedBitmap = null
            Toast.makeText(context, "Notice image selected from Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = saveBitmapToCache(context, bitmap)
            Toast.makeText(context, "Notice photo captured from Camera", Toast.LENGTH_SHORT).show()
        }
    }

    // Schedule Trigger (Start Date & Time) + Expiry Time Limit (End Date & Time)
    var isScheduledStart by remember { mutableStateOf(false) }
    var startDateTime by remember { mutableStateOf(ZonedDateTime.now().plusHours(1).withSecond(0).withNano(0)) }
    var hasTimeLimit by remember { mutableStateOf(true) }
    var selectedDurationPreset by remember { mutableStateOf("3 Days") }
    var endDateTime by remember { mutableStateOf(ZonedDateTime.now().plusDays(3).withSecond(0).withNano(0)) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH) }

    fun openDatePicker(current: ZonedDateTime, onPicked: (ZonedDateTime) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onPicked(current.withYear(year).withMonth(month + 1).withDayOfMonth(day))
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    fun openTimePicker(current: ZonedDateTime, onPicked: (ZonedDateTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onPicked(current.withHour(hour).withMinute(minute).withSecond(0).withNano(0))
            },
            current.hour,
            current.minute,
            false
        ).show()
    }

    val categories = listOf("Event", "Academic", "Circular", "Emergency")
    val audienceModes = listOf("All School", "Specific Class", "Specific Section")
    val durationPresets = listOf("24 Hours", "3 Days", "7 Days", "Custom", "No Expiry")

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CARD 1: Category, Title & Description
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = selectedCategory == cat
                            val activeColor = if (cat == "Emergency") GenesisColors.Emergency else GenesisColors.Primary
                            Surface(
                                color = if (selected) activeColor else GenesisColors.Background,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) activeColor else GenesisColors.Border),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (selected) Color.White else GenesisColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Title", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextPrimary)
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
                        placeholder = { Text("Enter notice headline", fontSize = 13.sp, color = GenesisColors.TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GenesisColors.Primary,
                            unfocusedBorderColor = GenesisColors.Border,
                            focusedContainerColor = GenesisColors.Surface,
                            unfocusedContainerColor = GenesisColors.Background
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextPrimary)
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
                        placeholder = { Text("Type or speak your announcement...", fontSize = 13.sp, color = GenesisColors.TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GenesisColors.Primary,
                            unfocusedBorderColor = GenesisColors.Border,
                            focusedContainerColor = GenesisColors.Surface,
                            unfocusedContainerColor = GenesisColors.Background
                        )
                    )
                }
            }

            // CARD 2: Notice Banner Image Upload (Real Camera + Gallery -> Cloud GCS & DB)
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(GenesisColors.LightBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Notice Image (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                            Text("Add a poster, circular photo, or event banner", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = GenesisColors.Background,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (selectedImageUri != null || capturedBitmap != null) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    capturedBitmap?.let { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Notice Banner",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } ?: AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Notice Banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Surface(
                                        color = Color(0xFF0F172A).copy(alpha = 0.72f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(30.dp)
                                            .clickable {
                                                selectedImageUri = null
                                                capturedBitmap = null
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = GenesisColors.Success, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Photo attached and ready to publish", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.Success)
                                }
                            } else {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Attach a poster, circular photo, or event banner", fontSize = 12.sp, color = GenesisColors.TextSecondary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { cameraLauncher.launch() },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GenesisColors.Border),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = GenesisColors.Surface,
                                        contentColor = GenesisColors.TextPrimary
                                    )
                                ) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                }
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GenesisColors.Border),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = GenesisColors.Surface,
                                        contentColor = GenesisColors.TextPrimary
                                    )
                                ) {
                                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // CARD 3: Schedule Trigger & Time Limit (Start Date/Time + End/Expiry Date/Time)
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFCCFBF1), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Schedule & Time Limit", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                            Text("Control when this notice triggers and auto-expires", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trigger Mode: Publish Now vs Schedule Start
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(false to "Publish Now", true to "Schedule Trigger").forEach { (scheduled, label) ->
                            val active = isScheduledStart == scheduled
                            Surface(
                                color = if (active) GenesisColors.Primary else GenesisColors.Background,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (active) GenesisColors.Primary else GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable {
                                        isScheduledStart = scheduled
                                        if (scheduled && endDateTime.isBefore(startDateTime.plusHours(1))) {
                                            endDateTime = startDateTime.plusDays(3)
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (scheduled) Icons.Rounded.EventAvailable else Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = if (active) Color.White else GenesisColors.TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        color = if (active) Color.White else GenesisColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (isScheduledStart) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Trigger Start Date & Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = GenesisColors.Background,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openDatePicker(startDateTime) { startDateTime = it } }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(startDateTime.format(dateFormatter), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                }
                            }
                            Surface(
                                color = GenesisColors.Background,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openTimePicker(startDateTime) { startDateTime = it } }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(startDateTime.format(timeFormatter), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = GenesisColors.Border, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Notice Time Limit (Auto-Expiry)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationPresets.forEach { preset ->
                            val active = selectedDurationPreset == preset
                            Surface(
                                color = if (active) GenesisColors.Teal else GenesisColors.Background,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (active) GenesisColors.Teal else GenesisColors.Border),
                                modifier = Modifier.clickable {
                                    selectedDurationPreset = preset
                                    val base = if (isScheduledStart) startDateTime else ZonedDateTime.now().withSecond(0).withNano(0)
                                    when (preset) {
                                        "24 Hours" -> { hasTimeLimit = true; endDateTime = base.plusHours(24) }
                                        "3 Days" -> { hasTimeLimit = true; endDateTime = base.plusDays(3) }
                                        "7 Days" -> { hasTimeLimit = true; endDateTime = base.plusDays(7) }
                                        "Custom" -> { hasTimeLimit = true }
                                        "No Expiry" -> { hasTimeLimit = false }
                                    }
                                }
                            ) {
                                Text(
                                    text = preset,
                                    color = if (active) Color.White else GenesisColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (hasTimeLimit) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("End / Expiry Date & Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = GenesisColors.Background,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDurationPreset = "Custom"
                                        openDatePicker(endDateTime) { endDateTime = it }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(endDateTime.format(dateFormatter), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                }
                            }
                            Surface(
                                color = GenesisColors.Background,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDurationPreset = "Custom"
                                        openTimePicker(endDateTime) { endDateTime = it }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(endDateTime.format(timeFormatter), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                }
                            }
                        }
                    }

                    // Live Schedule Summary Pill
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = GenesisColors.LightBlue.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val startSummary = if (isScheduledStart) "${startDateTime.format(dateFormatter)}, ${startDateTime.format(timeFormatter)}" else "Immediately"
                            val endSummary = if (hasTimeLimit) "${endDateTime.format(dateFormatter)}, ${endDateTime.format(timeFormatter)}" else "No expiry"
                            Text(
                                text = "Start: $startSummary  •  Ends: $endSummary",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GenesisColors.Primary
                            )
                        }
                    }
                }
            }

            // CARD 4: Simple 3-Dropdown Target Audience (All / Class / Section from School's Live Classes API)
            Card(
                colors = CardDefaults.cardColors(containerColor = GenesisColors.Surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(GenesisColors.LightBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Target Audience", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                            Text("Choose who receives this notice", fontSize = 11.sp, color = GenesisColors.TextSecondary)
                        }
                    }

                    // Dropdown 1: All School / Specific Class / Specific Section
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = GenesisColors.Background,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GenesisColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { audienceMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Audience", fontSize = 11.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                    Text(audienceMode, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GenesisColors.TextPrimary)
                                }
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Select Audience", tint = GenesisColors.Primary)
                            }
                        }
                        DropdownMenu(
                            expanded = audienceMenuExpanded,
                            onDismissRequest = { audienceMenuExpanded = false },
                            modifier = Modifier.background(GenesisColors.Surface)
                        ) {
                            audienceModes.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode,
                                            fontWeight = if (mode == audienceMode) FontWeight.Bold else FontWeight.Medium,
                                            color = if (mode == audienceMode) GenesisColors.Primary else GenesisColors.TextPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        if (mode == audienceMode) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    onClick = {
                                        audienceMode = mode
                                        audienceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Dropdown 2: Select Class (for Specific Class or Specific Section)
                    if (audienceMode == "Specific Class" || audienceMode == "Specific Section") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = GenesisColors.Background,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GenesisColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = classOptions.isNotEmpty()) { classMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Class", fontSize = 11.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = selectedTargetClass.ifBlank { "No classes available" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GenesisColors.TextPrimary
                                        )
                                    }
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Select Class", tint = GenesisColors.Primary)
                                }
                            }
                            DropdownMenu(
                                expanded = classMenuExpanded,
                                onDismissRequest = { classMenuExpanded = false },
                                modifier = Modifier.background(GenesisColors.Surface)
                            ) {
                                classOptions.forEach { cls ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = cls,
                                                fontWeight = if (cls == selectedTargetClass) FontWeight.Bold else FontWeight.Medium,
                                                color = if (cls == selectedTargetClass) GenesisColors.Primary else GenesisColors.TextPrimary
                                            )
                                        },
                                        trailingIcon = {
                                            if (cls == selectedTargetClass) {
                                                Icon(Icons.Rounded.Check, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        onClick = {
                                            selectedTargetClass = cls
                                            classMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Dropdown 3: Select Section (only after selecting Class in Specific Section mode)
                    if (audienceMode == "Specific Section" && selectedTargetClass.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = GenesisColors.Background,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, GenesisColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = sectionsForTargetClass.isNotEmpty()) { sectionMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Section", fontSize = 11.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = if (selectedTargetSection.isNotBlank()) "Section $selectedTargetSection" else "No sections available",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GenesisColors.TextPrimary
                                        )
                                    }
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Select Section", tint = GenesisColors.Teal)
                                }
                            }
                            DropdownMenu(
                                expanded = sectionMenuExpanded,
                                onDismissRequest = { sectionMenuExpanded = false },
                                modifier = Modifier.background(GenesisColors.Surface)
                            ) {
                                sectionsForTargetClass.forEach { sec ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Section $sec",
                                                fontWeight = if (sec == selectedTargetSection) FontWeight.Bold else FontWeight.Medium,
                                                color = if (sec == selectedTargetSection) GenesisColors.Teal else GenesisColors.TextPrimary
                                            )
                                        },
                                        trailingIcon = {
                                            if (sec == selectedTargetSection) {
                                                Icon(Icons.Rounded.Check, contentDescription = null, tint = GenesisColors.Teal, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        onClick = {
                                            selectedTargetSection = sec
                                            sectionMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        val effectiveStartIso = if (isScheduledStart) startDateTime.toInstant().toString() else null
                        val effectiveEndIso = if (hasTimeLimit) endDateTime.toInstant().toString() else null
                        if (isScheduledStart && hasTimeLimit && !endDateTime.isAfter(startDateTime)) {
                            Toast.makeText(context, "End date/time must be after the Start date/time", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val resolvedAudience = when (audienceMode) {
                            "Specific Class" -> selectedTargetClass.ifBlank { "All School" }
                            "Specific Section" -> if (selectedTargetClass.isNotBlank() && selectedTargetSection.isNotBlank()) {
                                "$selectedTargetClass - Section $selectedTargetSection"
                            } else {
                                selectedTargetClass.ifBlank { "All School" }
                            }
                            else -> "All School"
                        }
                        onPublish(
                            title.trim(),
                            description.trim(),
                            selectedCategory,
                            selectedImageUri?.toString(),
                            resolvedAudience,
                            effectiveStartIso,
                            effectiveEndIso
                        )
                    }
                },
                enabled = title.isNotBlank() && description.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
            ) {
                Icon(
                    imageVector = if (isScheduledStart) Icons.Rounded.EventAvailable else Icons.Rounded.Campaign,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScheduledStart) "Schedule Notice" else "Publish Notice Now",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

