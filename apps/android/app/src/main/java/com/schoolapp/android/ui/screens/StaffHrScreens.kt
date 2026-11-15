package com.schoolapp.android.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
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
import com.schoolapp.android.data.*
import com.schoolapp.android.ui.theme.GenesisColors

// ============================================================================
// STAFF & HR MANAGEMENT (PHASE 2)
//  • Teacher / Staff: My Profile, 1-tap Capture Attendance (500 m + Fingerprint), Leave Applications
//  • Principal / Admin: Staff Directory, Staff Attendance Register, Leave Approvals
// ============================================================================

@Composable
fun StaffHrHubScreen(
    initialTab: Int = 0,
    onBack: () -> Unit,
    onOpenSubstitutionHub: () -> Unit = {}
) {
    BackHandler { onBack() }
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val isAdmin = currentRole == AppRole.ADMIN
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val tabs = if (isAdmin) listOf("Staff", "Attendance", "Leave") else listOf("Profile", "Attendance", "Leave")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        SubScreenTopBar(title = if (isAdmin) "Staff & HR" else "My Staff Desk", onBack = onBack)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = GenesisColors.Primary
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == idx) GenesisColors.Primary else GenesisColors.TextSecondary
                        )
                    }
                )
            }
        }

        if (isAdmin) {
            when (selectedTab) {
                0 -> AdminStaffDirectoryTab()
                1 -> AdminStaffAttendanceTab()
                else -> AdminLeaveApprovalsTab(onOpenSubstitutionHub = onOpenSubstitutionHub)
            }
        } else {
            when (selectedTab) {
                0 -> MyStaffProfileTab()
                1 -> MyStaffAttendanceTab()
                else -> MyLeaveApplicationsTab()
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Teacher / Staff — My Profile
// ----------------------------------------------------------------------------

@Composable
private fun MyStaffProfileTab() {
    val context = LocalContext.current
    val staffList by DiscussionRepository.staffDirectory.collectAsState()
    val currentStaffId by DiscussionRepository.currentStaffId.collectAsState()
    val me = staffList.find { it.id == currentStaffId } ?: return
    val colleagues = staffList.filter { it.id != me.id }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedColleague by remember { mutableStateOf<StaffMemberRecord?>(null) }

    if (showEditDialog) {
        EditStaffContactDialog(
            staff = me,
            onDismiss = { showEditDialog = false },
            onSave = { phone, email, qualification ->
                DiscussionRepository.updateStaffContactProfile(me.id, phone, email, qualification)
                showEditDialog = false
                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    selectedColleague?.let { colleague ->
        val live = staffList.find { it.id == colleague.id } ?: colleague
        StaffProfileDetailDialog(
            staff = live,
            onDismiss = { selectedColleague = null },
            onCall = {
                if (live.phone.isNotBlank()) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${live.phone}")))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            StaffHeaderCard(staff = me)
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            SectionCard(title = "Contact") {
                ProfileRow(Icons.Default.Phone, "Phone", me.phone.ifBlank { "—" })
                ProfileRow(Icons.Default.Email, "Email", me.email.ifBlank { "—" })
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            SectionCard(title = "Employment") {
                ProfileRow(Icons.Default.Badge, "Employee ID", me.employeeId.ifBlank { "—" })
                ProfileRow(Icons.Default.Apartment, "Department", me.department)
                ProfileRow(Icons.Default.School, "Qualification", me.qualification.ifBlank { "—" })
                ProfileRow(Icons.Default.Event, "Joined", me.joiningDate.ifBlank { "—" })
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            SectionCard(title = "Teaching Load") {
                ProfileRow(Icons.Default.MenuBook, "Subjects", me.subjects.ifBlank { "—" })
                ProfileRow(Icons.Default.Groups, "Classes", me.classesAssigned)
                ProfileRow(Icons.Default.Schedule, "Periods / Week", me.workloadPeriodsPerWeek.toString())
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            OutlinedButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GenesisColors.Primary)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Contact Details", color = GenesisColors.Primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Employee ID, department and classes are managed by the school office.",
                fontSize = 12.sp,
                color = GenesisColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "School Teachers & Staff (${colleagues.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GenesisColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(colleagues, key = { it.id }) { colleague ->
            StaffDirectoryRow(
                staff = colleague,
                onClick = { selectedColleague = colleague }
            )
        }
    }
}

@Composable
private fun EditStaffContactDialog(
    staff: StaffMemberRecord,
    onDismiss: () -> Unit,
    onSave: (phone: String, email: String, qualification: String) -> Unit
) {
    var phone by remember { mutableStateOf(staff.phone) }
    var email by remember { mutableStateOf(staff.email) }
    var qualification by remember { mutableStateOf(staff.qualification) }
    val phoneValid = phone.filter { it.isDigit() }.length >= 10

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Edit Contact Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GenesisColors.TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    isError = !phoneValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Qualification") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = GenesisColors.TextPrimary)
                    }
                    Button(
                        onClick = { onSave(phone, email, qualification) },
                        enabled = phoneValid,
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

// ----------------------------------------------------------------------------
// Teacher / Staff — My Attendance (1-tap Capture Attendance with 500 m GPS + Fingerprint)
// ----------------------------------------------------------------------------

@Composable
private fun MyStaffAttendanceTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentStaffId by DiscussionRepository.currentStaffId.collectAsState()
    val attendance by DiscussionRepository.staffAttendance.collectAsState()
    val geofence by DiscussionRepository.schoolCampusGeofence.collectAsState()
    val (isCheckingGps, startVerifiedCheckIn) = rememberTeacherGeofenceFingerprintCheckIn(currentStaffId)

    var isCapturingStart by remember { mutableStateOf(false) }
    var liveDistanceMeters by remember { mutableStateOf<Int?>(null) }

    fun refreshLiveDistance(targetGeofence: SchoolCampusGeofence = geofence) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return
        scope.launch {
            val loc = obtainCurrentDeviceLocation(context, preferFresh = true)
            if (loc != null) {
                liveDistanceMeters = calculateDistanceMeters(
                    loc.latitude,
                    loc.longitude,
                    targetGeofence.latitude,
                    targetGeofence.longitude
                )
            }
        }
    }

    fun captureStartPointNow() {
        isCapturingStart = true
        scope.launch {
            val loc = captureStartingCampusGps(context)
            isCapturingStart = false
            if (loc != null) {
                liveDistanceMeters = 0
                Toast.makeText(
                    context,
                    "Starting location captured! Stay within 500 m for fingerprint attendance.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Turn ON GPS / Location Services to capture start point.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val startLocationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            captureStartPointNow()
        }
    }

    // If starting GPS isn't captured yet and permission isn't granted, ask permission on start to capture once.
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse && !geofence.isCalibratedFromDevice) {
            startLocationPermLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else if (hasFine || hasCoarse) {
            refreshLiveDistance(geofence)
        }
    }

    val myDays = attendance.filter { it.staffId == currentStaffId }
    val today = myDays.find { it.dateLabel == "Today" }
    val history = myDays.filter { it.dateLabel != "Today" }

    val presentCount = myDays.count { it.status == "Present" }
    val lateCount = myDays.count { it.status == "Late" }
    val leaveCount = myDays.count { it.status == "On Leave" }

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
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Today • ${geofence.radiusMeters} m Campus Geofence",
                        color = Color(0xFFBFDBFE),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = today?.status ?: "Attendance Not Captured",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TimeStat("Captured At", today?.checkIn ?: "--:--")
                    if (!today?.verificationMethod.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Verified: ${today!!.verificationMethod}",
                            color = Color(0xFF86EFAC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        today?.status == "On Leave" -> {
                            Surface(color = Color(0xFF172554), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "You are on approved leave today",
                                    color = Color(0xFFDBEAFE),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        today?.checkIn == null -> {
                            Button(
                                onClick = { startVerifiedCheckIn() },
                                enabled = !isCheckingGps,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                            ) {
                                if (isCheckingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying 500 m GPS...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                } else {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Capture Attendance (500 m)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                        else -> {
                            Surface(color = Color(0xFF14532D), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Attendance Captured ✓ at ${today.checkIn}",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Active 500 m Campus / Captured Start Location Anchor Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "500 m Attendance Zone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GenesisColors.TextPrimary
                            )
                        }
                        Surface(
                            color = if (geofence.isCalibratedFromDevice) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (geofence.isCalibratedFromDevice) "Start Captured" else "Eden Garden Preset",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (geofence.isCalibratedFromDevice) GenesisColors.Success else GenesisColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = geofence.schoolName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Text(
                        text = "Anchor: %.5f, %.5f • Stay within %d m".format(
                            geofence.latitude,
                            geofence.longitude,
                            geofence.radiusMeters
                        ),
                        fontSize = 11.sp,
                        color = GenesisColors.TextSecondary
                    )
                    liveDistanceMeters?.let { dist ->
                        val inside = dist <= geofence.radiusMeters
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (inside) {
                                "Live distance: $dist m — Inside ${geofence.radiusMeters} m (Fingerprint ready)"
                            } else {
                                "Live distance: $dist m — Outside ${geofence.radiusMeters} m zone"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (inside) GenesisColors.Success else GenesisColors.Emergency
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    captureStartPointNow()
                                } else {
                                    startLocationPermLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            },
                            enabled = !isCapturingStart,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(
                                text = if (isCapturingStart) "Capturing..." else "Capture Start Here",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.Primary,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                DiscussionRepository.resetCampusToEdenGardenPune(context)
                                refreshLiveDistance(DiscussionRepository.schoolCampusGeofence.value)
                                Toast.makeText(
                                    context,
                                    "Zone set to Eden Garden, Viman Nagar, Pune (500 m)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text(
                                text = "Eden Garden, Pune",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryStatCard("Present", presentCount.toString(), GenesisColors.Success, Modifier.weight(1f))
                SummaryStatCard("Late", lateCount.toString(), GenesisColors.Warning, Modifier.weight(1f))
                SummaryStatCard("Leave", leaveCount.toString(), GenesisColors.Primary, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Recent Days", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(history, key = { it.id }) { day ->
            AttendanceDayRow(
                title = day.dateLabel,
                subtitle = if (day.checkIn != null) "Captured at ${day.checkIn}" else "Not captured",
                status = day.status
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Teacher / Staff — Leave Applications
// ----------------------------------------------------------------------------

@Composable
private fun MyLeaveApplicationsTab() {
    val context = LocalContext.current
    val currentStaffId by DiscussionRepository.currentStaffId.collectAsState()
    val leaves by DiscussionRepository.staffLeaveRequests.collectAsState()
    val myLeaves = leaves.filter { it.staffId == currentStaffId }
    val balance = DiscussionRepository.staffLeaveBalance(currentStaffId, leaves)
    var showApplyDialog by remember { mutableStateOf(false) }

    if (showApplyDialog) {
        ApplyStaffLeaveDialog(
            balance = balance,
            onDismiss = { showApplyDialog = false },
            onSubmit = { type, startOffset, days, reason ->
                DiscussionRepository.applyStaffLeave(currentStaffId, type, startOffset, days, reason)
                showApplyDialog = false
                Toast.makeText(context, "Leave request sent to Principal", Toast.LENGTH_SHORT).show()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Leave Balance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                balance.forEach { (type, pair) ->
                    LeaveBalanceCard(
                        label = type.removeSuffix(" Leave"),
                        remaining = pair.first,
                        total = pair.second,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showApplyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply for Leave", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("My Applications", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (myLeaves.isEmpty()) {
            item { EmptyHint("No leave applications yet") }
        }

        items(myLeaves, key = { it.id }) { leave ->
            LeaveRequestCard(
                leave = leave,
                showStaffName = false,
                actions = {
                    if (leave.status == "Pending") {
                        TextButton(onClick = {
                            DiscussionRepository.cancelStaffLeave(leave.id)
                            Toast.makeText(context, "Leave request withdrawn", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Withdraw", color = GenesisColors.Emergency, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ApplyStaffLeaveDialog(
    balance: Map<String, Pair<Int, Int>>,
    onDismiss: () -> Unit,
    onSubmit: (type: String, startOffsetDays: Int, days: Int, reason: String) -> Unit
) {
    var leaveType by remember { mutableStateOf(StaffLeavePolicy.annualQuota.keys.first()) }
    var startOffset by remember { mutableIntStateOf(1) }
    var days by remember { mutableIntStateOf(1) }
    var reason by remember { mutableStateOf("") }
    val remaining = balance[leaveType]?.first ?: 0
    val (fromLabel, toLabel) = DiscussionRepository.buildLeaveDateRange(startOffset, days)
    val canSubmit = days <= remaining && reason.isNotBlank()

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
                Text("Apply for Leave", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GenesisColors.TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                FieldLabel("Leave Type")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StaffLeavePolicy.annualQuota.keys.forEach { type ->
                        ChoicePill(
                            label = "${type.removeSuffix(" Leave")} (${balance[type]?.first ?: 0})",
                            selected = leaveType == type,
                            onClick = { leaveType = type }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                FieldLabel("Starting")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Today", 1 to "Tomorrow", 2 to "Day After").forEach { (offset, label) ->
                        ChoicePill(label = label, selected = startOffset == offset, onClick = { startOffset = offset })
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                FieldLabel("Number of Days")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Default.Remove, enabled = days > 1) { days-- }
                    Text(
                        text = "$days",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = GenesisColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    StepperButton(Icons.Default.Add, enabled = days < 30) { days++ }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (days == 1) fromLabel else "$fromLabel → $toLabel",
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary,
                        maxLines = 2
                    )
                }
                if (days > remaining) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Only $remaining day(s) of ${leaveType} left",
                        fontSize = 12.sp,
                        color = GenesisColors.Emergency,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FieldLabel("Reason")
                    VoiceCaptureChipButton(
                        label = "Speak",
                        promptHint = "Say your leave reason...",
                        isAcademicText = false,
                        onResultText = { spoken -> reason = mergeVoiceText(reason, spoken) }
                    )
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("e.g. Medical appointment") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = GenesisColors.TextPrimary)
                    }
                    Button(
                        onClick = { onSubmit(leaveType, startOffset, days, reason) },
                        enabled = canSubmit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                    ) {
                        Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Principal / Admin — Staff Directory
// ----------------------------------------------------------------------------

@Composable
private fun AdminStaffDirectoryTab() {
    val context = LocalContext.current
    val staffList by DiscussionRepository.staffDirectory.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedStaff by remember { mutableStateOf<StaffMemberRecord?>(null) }

    val filtered = staffList.filter {
        query.isBlank() ||
            it.name.contains(query, true) ||
            it.department.contains(query, true) ||
            it.subjects.contains(query, true)
    }

    selectedStaff?.let { s ->
        val live = staffList.find { it.id == s.id } ?: s
        StaffProfileDetailDialog(
            staff = live,
            onDismiss = { selectedStaff = null },
            onCall = {
                if (live.phone.isNotBlank()) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${live.phone}")))
                }
            }
        )
    }

    val presentCount = staffList.count { it.attendanceStatus == "Present" || it.attendanceStatus == "Late" }
    val leaveCount = staffList.count { it.attendanceStatus == "On Leave" }
    val notInCount = staffList.count { it.attendanceStatus == "Not Checked In" || it.attendanceStatus == "Absent" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryStatCard("Total", staffList.size.toString(), GenesisColors.TextPrimary, Modifier.weight(1f))
                SummaryStatCard("In School", presentCount.toString(), GenesisColors.Success, Modifier.weight(1f))
                SummaryStatCard("On Leave", leaveCount.toString(), GenesisColors.Primary, Modifier.weight(1f))
                SummaryStatCard("Not In", notInCount.toString(), GenesisColors.Emergency, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search name, subject or department") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(filtered, key = { it.id }) { staff ->
            StaffDirectoryRow(
                staff = staff,
                onClick = { selectedStaff = staff }
            )
        }
    }
}

@Composable
private fun StaffDirectoryRow(
    staff: StaffMemberRecord,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialAvatar(staff.name, 42)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
                Text(
                    text = "${staff.designation} • ${staff.department}",
                    fontSize = 12.sp,
                    color = GenesisColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StaffStatusChip(staff.attendanceStatus)
        }
    }
}

@Composable
private fun StaffProfileDetailDialog(
    staff: StaffMemberRecord,
    onDismiss: () -> Unit,
    onCall: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                StaffHeaderCard(staff = staff)
                Spacer(modifier = Modifier.height(12.dp))
                ProfileRow(Icons.Default.Badge, "Employee ID", staff.employeeId.ifBlank { "—" })
                ProfileRow(Icons.Default.Phone, "Phone", staff.phone.ifBlank { "—" })
                ProfileRow(Icons.Default.Email, "Email", staff.email.ifBlank { "—" })
                ProfileRow(Icons.Default.School, "Qualification", staff.qualification.ifBlank { "—" })
                ProfileRow(Icons.Default.Event, "Joined", staff.joiningDate.ifBlank { "—" })
                ProfileRow(Icons.Default.Groups, "Classes", staff.classesAssigned)
                ProfileRow(Icons.Default.Schedule, "Periods / Week", staff.workloadPeriodsPerWeek.toString())
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Close", color = GenesisColors.TextPrimary)
                    }
                    Button(
                        onClick = onCall,
                        enabled = staff.phone.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Principal / Admin — Staff Attendance Register
// ----------------------------------------------------------------------------

@Composable
private fun AdminStaffAttendanceTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val staffList by DiscussionRepository.staffDirectory.collectAsState()
    val attendance by DiscussionRepository.staffAttendance.collectAsState()
    val geofence by DiscussionRepository.schoolCampusGeofence.collectAsState()
    var isCalibrating by remember { mutableStateOf(false) }

    fun calibrateFromCurrentGps() {
        isCalibrating = true
        scope.launch {
            val loc = obtainCurrentDeviceLocation(context, preferFresh = true)
            isCalibrating = false
            if (loc != null) {
                DiscussionRepository.updateSchoolCampusLocation(loc.latitude, loc.longitude, 500, context = context)
                Toast.makeText(
                    context,
                    "School 500 m center updated to current GPS!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Turn ON GPS and try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val gpsCalibratePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            calibrateFromCurrentGps()
        }
    }

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
                    Text(
                        text = "SCHOOL CAMPUS GEOFENCE (${geofence.radiusMeters} M RADIUS)",
                        color = GenesisColors.SkyAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = geofence.schoolName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lat %.5f, Lng %.5f".format(geofence.latitude, geofence.longitude),
                        color = Color(0xFFDBEAFE),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                calibrateFromCurrentGps()
                            } else {
                                gpsCalibratePermLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                        },
                        enabled = !isCalibrating,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCalibrating) "Detecting GPS..." else "Set Current GPS as School Location",
                            color = GenesisColors.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Today's Staff Attendance & Check-In Times", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Teachers verify fingerprint within 500 m of school. Exact check-in time & method are logged below.",
                fontSize = 12.sp,
                color = GenesisColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(staffList, key = { it.id }) { staff ->
            val today = attendance.find { it.staffId == staff.id && it.dateLabel == "Today" }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GenesisColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(staff.name, 36)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextPrimary)
                            Text(
                                text = when {
                                    today?.checkIn != null -> {
                                        val timePart = "Captured ${today.checkIn}"
                                        val methodPart = today.verificationMethod.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
                                        timePart + methodPart
                                    }
                                    else -> "Attendance not captured yet"
                                },
                                fontSize = 12.sp,
                                color = if (today?.checkIn != null) GenesisColors.Success else GenesisColors.TextSecondary,
                                fontWeight = if (today?.checkIn != null) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        StaffStatusChip(staff.attendanceStatus)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Present", "P", GenesisColors.Success),
                            Triple("Absent", "A", GenesisColors.Emergency),
                            Triple("On Leave", "Leave", GenesisColors.Primary)
                        ).forEach { (status, label, color) ->
                            val selected = staff.attendanceStatus == status ||
                                (status == "Present" && staff.attendanceStatus == "Late")
                            Surface(
                                color = if (selected) color else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) color else GenesisColors.Border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clickable { DiscussionRepository.adminMarkStaffAttendance(staff.id, status) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color.White else GenesisColors.TextSecondary,
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
    }
}

// ----------------------------------------------------------------------------
// Principal / Admin — Leave Approvals
// ----------------------------------------------------------------------------

@Composable
private fun AdminLeaveApprovalsTab(onOpenSubstitutionHub: () -> Unit) {
    val context = LocalContext.current
    val leaves by DiscussionRepository.staffLeaveRequests.collectAsState()
    val pending = leaves.filter { it.status == "Pending" }
    val reviewed = leaves.filter { it.status != "Pending" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Pending Approval (${pending.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (pending.isEmpty()) {
            item { EmptyHint("No pending leave requests") }
        }
        items(pending, key = { it.id }) { leave ->
            LeaveRequestCard(
                leave = leave,
                showStaffName = true,
                actions = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                DiscussionRepository.rejectStaffLeave(leave.id)
                                Toast.makeText(context, "Leave rejected", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, GenesisColors.Emergency),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reject", color = GenesisColors.Emergency, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                DiscussionRepository.approveStaffLeave(leave.id)
                                Toast.makeText(
                                    context,
                                    "Approved • Substitute request created automatically",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Success)
                        ) {
                            Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text("Recently Reviewed", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GenesisColors.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(reviewed, key = { it.id }) { leave ->
            LeaveRequestCard(
                leave = leave,
                showStaffName = true,
                actions = {
                    if (leave.substituteRequested) {
                        TextButton(onClick = onOpenSubstitutionHub) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Substitutes", color = GenesisColors.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Shared building blocks
// ----------------------------------------------------------------------------

@Composable
private fun StaffHeaderCard(staff: StaffMemberRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GenesisColors.MeteorBlack),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = staff.name.replace(Regex("^(Mr\\.|Mrs\\.|Ms\\.|Dr\\.)\\s*"), "").take(1).uppercase(),
                    color = GenesisColors.MeteorBlack,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(staff.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(staff.designation, color = Color(0xFFDBEAFE), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(color = Color(0xFF172554), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = "${staff.staffType} Staff",
                        color = Color(0xFFBFDBFE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GenesisColors.TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GenesisColors.Primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = GenesisColors.TextSecondary, modifier = Modifier.width(110.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = GenesisColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimeStat(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFFBFDBFE), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Text(label, fontSize = 12.sp, color = GenesisColors.TextSecondary)
        }
    }
}

@Composable
private fun LeaveBalanceCard(label: String, remaining: Int, total: Int, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 12.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$remaining", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GenesisColors.Primary)
                Text(" / $total", fontSize = 13.sp, color = GenesisColors.TextSecondary, modifier = Modifier.padding(bottom = 3.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else remaining / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = GenesisColors.Primary,
                trackColor = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun LeaveRequestCard(
    leave: StaffLeaveRequest,
    showStaffName: Boolean,
    actions: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (showStaffName) leave.staffName else leave.leaveType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GenesisColors.TextPrimary
                    )
                    Text(
                        text = buildString {
                            if (showStaffName) append("${leave.leaveType} • ")
                            append(if (leave.days == 1) leave.fromDate else "${leave.fromDate} → ${leave.toDate}")
                            append(" • ${leave.days} day${if (leave.days > 1) "s" else ""}")
                        },
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary
                    )
                }
                LeaveStatusChip(leave.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(leave.reason, fontSize = 14.sp, color = GenesisColors.TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (leave.reviewedBy.isNotBlank()) "Reviewed by ${leave.reviewedBy}" else "Applied ${leave.appliedOn}",
                    fontSize = 11.sp,
                    color = GenesisColors.TextSecondary
                )
                actions()
            }
        }
    }
}

@Composable
private fun LeaveStatusChip(status: String) {
    val (bg, fg) = when (status) {
        "Approved" -> Color(0xFFDCFCE7) to GenesisColors.Success
        "Rejected" -> Color(0xFFFEE2E2) to GenesisColors.Emergency
        else -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(status, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun StaffStatusChip(status: String) {
    val (bg, fg) = when (status) {
        "Present" -> Color(0xFFDCFCE7) to GenesisColors.Success
        "Late" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        "On Leave" -> Color(0xFFDBEAFE) to GenesisColors.Primary
        "Absent" -> Color(0xFFFEE2E2) to GenesisColors.Emergency
        else -> Color(0xFFF1F5F9) to GenesisColors.TextSecondary
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp)) {
        Text(status, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun AttendanceDayRow(title: String, subtitle: String, status: String) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GenesisColors.TextPrimary)
                Text(subtitle, fontSize = 13.sp, color = GenesisColors.TextSecondary)
            }
            StaffStatusChip(status)
        }
    }
}

@Composable
private fun InitialAvatar(name: String, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .background(Color(0xFFDBEAFE), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.replace(Regex("^(Mr\\.|Mrs\\.|Ms\\.|Dr\\.)\\s*"), "").take(1).uppercase(),
            color = GenesisColors.MeteorBlack,
            fontWeight = FontWeight.Bold,
            fontSize = (sizeDp / 2.6).sp
        )
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) GenesisColors.Primary else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) GenesisColors.Primary else GenesisColors.Border),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else GenesisColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (enabled) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
        shape = CircleShape,
        modifier = Modifier
            .size(36.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) GenesisColors.Primary else GenesisColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GenesisColors.TextSecondary)
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = GenesisColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    )
}

