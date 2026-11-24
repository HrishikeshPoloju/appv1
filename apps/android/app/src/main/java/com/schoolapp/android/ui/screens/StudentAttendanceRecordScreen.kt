package com.schoolapp.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolapp.android.R
import com.schoolapp.android.data.DiscussionRepository
import com.schoolapp.android.data.StudentProfile
import com.schoolapp.android.ui.theme.GenesisColors
import com.schoolapp.android.ui.theme.LocalSchoolBrand
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ============================================================================
// ATTENDANCE RECORD SCREEN — CLEAN WHITE/LIGHT THEME MATCHING ALL OTHER PAGES
// ============================================================================

@Composable
fun StudentAttendanceRecordScreen(
    student: StudentProfile,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val brand = LocalSchoolBrand.current

    // Always fetch live attendance from backend API when screen opens
    LaunchedEffect(student.id) {
        DiscussionRepository.fetchStudentAttendanceFromBackend(student.id)
    }

    // Dynamic 6-month window ending on the current month
    val months = remember {
        val nowYm = YearMonth.now()
        (5 downTo 0).map { offset ->
            val ym = nowYm.minusMonths(offset.toLong())
            val shortMonth = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            val shortYear = (ym.year % 100).toString().padStart(2, '0')
            ym to "$shortMonth '$shortYear"
        }
    }
    var selectedMonthIndex by remember { mutableIntStateOf(months.lastIndex) }
    val currentYearMonth = months[selectedMonthIndex].first
    val yearlyAttendance by DiscussionRepository.studentYearlyAttendance.collectAsState()
    val totalDaysCount = yearlyAttendance.size
    val presentDaysCount = yearlyAttendance.values.count { it.equals("Present", ignoreCase = true) }
    val absentDaysCount = yearlyAttendance.values.count { it.equals("Absent", ignoreCase = true) }
    val attendancePctText = if (totalDaysCount > 0) "%.1f%%".format((presentDaysCount * 100.0) / totalDaysCount) else "0.0%"
    val attendanceStatusLabel = when {
        totalDaysCount == 0 -> "No Records"
        (presentDaysCount * 100.0 / totalDaysCount) >= 90.0 -> "Regular"
        (presentDaysCount * 100.0 / totalDaysCount) >= 75.0 -> "Good"
        else -> "Low"
    }

    var selectedDay by remember { mutableStateOf<Int?>(10) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GenesisColors.Background)
    ) {
        // Branded Top Bar matching all other screens
        SubScreenTopBar(title = "Attendance Record", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Student subtitle strip
            Text(
                text = "${student.fullName} • ${student.className} – Section ${student.section}",
                fontSize = 13.sp,
                color = GenesisColors.TextSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )

            // 2. AttendanceSummaryCard Component (Live from PostgreSQL DB)
            AttendanceSummaryCard(
                percentage = attendancePctText,
                label = "Overall Attendance",
                status = attendanceStatusLabel
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. AttendanceStatCard Row Component (Live from PostgreSQL DB)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AttendanceStatCard(
                    count = totalDaysCount.toString(),
                    label = "Total Days",
                    valueColor = brand.primary,
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatCard(
                    count = presentDaysCount.toString(),
                    label = "Present",
                    valueColor = GenesisColors.Success,
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatCard(
                    count = absentDaysCount.toString(),
                    label = "Absent",
                    valueColor = GenesisColors.Emergency,
                    icon = Icons.Default.NotificationsOff,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. MonthSelector Component
            MonthSelector(
                months = months.map { it.second },
                selectedIndex = selectedMonthIndex,
                onMonthSelected = { selectedMonthIndex = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. AttendanceCalendar Component (strictly driven by live API response)
            AttendanceCalendar(
                yearMonth = currentYearMonth,
                yearlyAttendance = yearlyAttendance,
                selectedDay = selectedDay,
                onDaySelected = { selectedDay = it },
                onPrevMonth = {
                    if (selectedMonthIndex > 0) selectedMonthIndex--
                },
                onNextMonth = {
                    if (selectedMonthIndex < months.lastIndex) selectedMonthIndex++
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7. AttendanceLegend Component
            AttendanceLegend()
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 1: AttendanceHeader (Light Theme)
// ----------------------------------------------------------------------------
@Composable
fun AttendanceHeader(
    studentName: String,
    studentClass: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, GenesisColors.Border, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = GenesisColors.TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = "Attendance Record",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GenesisColors.TextPrimary,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$studentName • $studentClass",
                fontSize = 13.sp,
                color = GenesisColors.TextSecondary,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 2: AttendanceSummaryCard (Branded Gradient Card on Light Page)
// ----------------------------------------------------------------------------
@Composable
fun AttendanceSummaryCard(
    percentage: String,
    label: String,
    status: String,
    modifier: Modifier = Modifier
) {
    val brand = LocalSchoolBrand.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(138.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            Image(
                painter = painterResource(id = R.drawable.img_attendance_student),
                contentDescription = "Student Learning",
                contentScale = ContentScale.FillHeight,
                alignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
            )

            // Seamless brand-blue patch covering the baked-in "gular" outline behind the Regular pill
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to brand.headerStart,
                            0.58f to brand.headerStart,
                            0.68f to Color.Transparent
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = percentage,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = brand.skyAccent,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = status,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 3: AttendanceStatCard (Crisp White Card)
// ----------------------------------------------------------------------------
@Composable
fun AttendanceStatCard(
    count: String,
    label: String,
    valueColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, GenesisColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = count,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = GenesisColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 4: MonthSelector (Light Theme Pills)
// ----------------------------------------------------------------------------
@Composable
fun MonthSelector(
    months: List<String>,
    selectedIndex: Int,
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalSchoolBrand.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        months.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Surface(
                color = if (isSelected) brand.primary else Color.White,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, if (isSelected) brand.primary else GenesisColors.Border),
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier.clickable { onMonthSelected(index) }
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else GenesisColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 5: AttendanceCalendar (Clean White Card)
// ----------------------------------------------------------------------------
@Composable
fun AttendanceCalendar(
    yearMonth: YearMonth,
    yearlyAttendance: Map<String, String>,
    selectedDay: Int?,
    onDaySelected: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brand = LocalSchoolBrand.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, GenesisColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brand.softTint)
                            .clickable { onPrevMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = brand.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${yearMonth.year}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenesisColors.TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brand.softTint)
                            .clickable { onNextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = brand.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Days of Week (S M T W T F S)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                        Box(
                            modifier = Modifier.width(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GenesisColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calendar Grid
                val daysInMonth = yearMonth.lengthOfMonth()
                val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek
                val dayOffset = if (firstDayOfWeek == DayOfWeek.SUNDAY) 0 else firstDayOfWeek.value
                val totalCells = (dayOffset + daysInMonth + 6) / 7 * 7

                for (row in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - dayOffset + 1

                            if (dayNum in 1..daysInMonth) {
                                val isoDate = yearMonth.atDay(dayNum).toString()
                                val apiStatus = yearlyAttendance[isoDate]
                                AttendanceDateCell(
                                    dayNumber = dayNum,
                                    isPresent = apiStatus.equals("Present", ignoreCase = true),
                                    isAbsent = apiStatus.equals("Absent", ignoreCase = true),
                                    isOff = false,
                                    isSelected = selectedDay == dayNum,
                                    onClick = { onDaySelected(dayNum) }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(38.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Science Beaker Flask Illustration
            Image(
                painter = painterResource(id = R.drawable.ic_attendance_beaker),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 14.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 6: AttendanceDateCell (High-Contrast Light Theme Cells)
// ----------------------------------------------------------------------------
@Composable
fun AttendanceDateCell(
    dayNumber: Int,
    isPresent: Boolean,
    isAbsent: Boolean,
    isOff: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isPresent -> {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7))
                        .border(1.5.dp, GenesisColors.Success, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$dayNumber",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
            }

            isAbsent -> {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2))
                        .border(1.5.dp, GenesisColors.Emergency, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$dayNumber",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenesisColors.Emergency
                    )
                }
            }

            isOff -> {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$dayNumber",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            else -> {
                // No API record for this day — keep clean white cell
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$dayNumber",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = GenesisColors.TextPrimary
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 7: AttendanceLegend (Light Theme)
// ----------------------------------------------------------------------------
@Composable
fun AttendanceLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Present
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(GenesisColors.Success)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Present", fontSize = 12.sp, color = GenesisColors.TextPrimary, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.width(18.dp))

        // Absent
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(GenesisColors.Emergency)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Absent", fontSize = 12.sp, color = GenesisColors.TextPrimary, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.width(18.dp))

        // Off / Upcoming
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
                .border(1.dp, Color(0xFF94A3B8), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Off / Upcoming", fontSize = 12.sp, color = GenesisColors.TextSecondary, fontWeight = FontWeight.Medium)
    }
}
