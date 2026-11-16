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
import com.schoolapp.android.data.StudentProfile
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ============================================================================
// EXACT FIGMA ATTENDANCE SCREEN IMPLEMENTATION (Node 3:142)
// Built with 7 modular, production-grade reusable components without static screenshot backgrounds
// ============================================================================

@Composable
fun StudentAttendanceRecordScreen(
    student: StudentProfile,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    // Academic Year Months: June 2026 to November 2026
    val months = remember {
        listOf(
            YearMonth.of(2026, 6) to "Jun '26",
            YearMonth.of(2026, 7) to "Jul '26",
            YearMonth.of(2026, 8) to "Aug '26",
            YearMonth.of(2026, 9) to "Sep '26",
            YearMonth.of(2026, 10) to "Oct '26",
            YearMonth.of(2026, 11) to "Nov '26"
        )
    }
    var selectedMonthIndex by remember { mutableIntStateOf(5) } // Default: Nov '26
    val currentYearMonth = months[selectedMonthIndex].first
    var selectedDay by remember { mutableStateOf<Int?>(10) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111)) // Obsidian Dark background
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // 1. AttendanceHeader Component
        AttendanceHeader(
            studentName = student.fullName,
            studentClass = "${student.className} – Section ${student.section}",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. AttendanceSummaryCard Component (Native UI with Student Illustration Asset)
        AttendanceSummaryCard(
            percentage = "94.0%",
            label = "Overall Attendance",
            status = "Regular"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. AttendanceStatCard Row Component
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AttendanceStatCard(
                count = "134",
                label = "Total Days",
                valueColor = Color(0xFFFBBF24),
                icon = Icons.Default.CalendarMonth,
                modifier = Modifier.weight(1f)
            )
            AttendanceStatCard(
                count = "126",
                label = "Present",
                valueColor = Color.White,
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
            AttendanceStatCard(
                count = "8",
                label = "Absent",
                valueColor = Color(0xFFF97316),
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

        // 5. AttendanceCalendar Component
        AttendanceCalendar(
            yearMonth = currentYearMonth,
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

// ----------------------------------------------------------------------------
// COMPONENT 1: AttendanceHeader
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
        // Circular Back Button Pod
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF262626))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = "Attendance Record",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$studentName • $studentClass",
                fontSize = 13.sp,
                color = Color(0xFFA3A3A3),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 2: AttendanceSummaryCard (Native Gradient, Typography & Asset)
// ----------------------------------------------------------------------------
@Composable
fun AttendanceSummaryCard(
    percentage: String,
    label: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(156.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF3F2B00), // Deep warm amber
                            Color(0xFF251A04), // Dark transitional gold
                            Color(0xFF140F03)  // Charcoal gold
                        )
                    )
                )
        ) {
            // Student Reading Illustration (Figma Node 3:253)
            Image(
                painter = painterResource(id = R.drawable.img_attendance_student),
                contentDescription = "Student Learning",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
            )

            // Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 120.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = percentage,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFACC15),
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = Color(0xFF262626),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = status,
                            color = Color(0xFFD4D4D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 3: AttendanceStatCard
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
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
                    fontWeight = FontWeight.Bold,
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
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 4: MonthSelector
// ----------------------------------------------------------------------------
@Composable
fun MonthSelector(
    months: List<String>,
    selectedIndex: Int,
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
                color = if (isSelected) Color(0xFFFDC52A) else Color(0xFF1E1E1E),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.clickable { onMonthSelected(index) }
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF111111) else Color(0xFFCBD5E1),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 5: AttendanceCalendar
// ----------------------------------------------------------------------------
@Composable
fun AttendanceCalendar(
    yearMonth: YearMonth,
    selectedDay: Int?,
    onDaySelected: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentDays = remember { setOf(2, 3, 4, 5, 6, 7, 9, 10) }
    val absentDays = remember { setOf(8) }
    val offDays = remember { setOf(1, 14, 15, 21, 22, 28, 29) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
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
                    // Left Arrow Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFDC52A))
                            .clickable { onPrevMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = Color(0xFF111111),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${yearMonth.year}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Right Arrow Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFDC52A))
                            .clickable { onNextMonth() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = Color(0xFF111111),
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
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
                                AttendanceDateCell(
                                    dayNumber = dayNum,
                                    isPresent = presentDays.contains(dayNum),
                                    isAbsent = absentDays.contains(dayNum),
                                    isOff = offDays.contains(dayNum),
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

            // Science Beaker Flask Illustration (Figma Node 3:150)
            Image(
                painter = painterResource(id = R.drawable.ic_attendance_beaker),
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 14.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 6: AttendanceDateCell (Canvas Sketched Hand-Drawn Indicators)
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
                // Hand-drawn sketch circle effect with golden yellow fill
                Canvas(modifier = Modifier.size(34.dp)) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Warm golden yellow fill
                    drawCircle(
                        color = Color(0xFFFDC52A),
                        radius = radius * 0.95f,
                        center = center
                    )

                    // Hand-drawn sketch outer ring 1
                    drawCircle(
                        color = Color(0xFFEAB308),
                        radius = radius * 0.96f,
                        center = center,
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 6f, 18f, 4f))
                        )
                    )

                    // Hand-drawn sketch outer ring 2
                    drawCircle(
                        color = Color(0xFFCA8A04),
                        radius = radius * 0.90f,
                        center = center,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                        )
                    )
                }
                Text(
                    text = "$dayNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111) // Crisp black
                )
            }

            isAbsent -> {
                // Outlined orange sketch ring
                Canvas(modifier = Modifier.size(34.dp)) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = Color(0xFFF97316),
                        radius = radius * 0.94f,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    drawCircle(
                        color = Color(0xFFFB923C).copy(alpha = 0.5f),
                        radius = radius * 0.88f,
                        center = center,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                        )
                    )
                }
                Text(
                    text = "$dayNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF97316)
                )
            }

            isOff -> {
                // Dark grey off-day circle pod
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF282828)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$dayNumber",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF737373)
                    )
                }
            }

            else -> {
                // Normal scheduled weekday text
                Text(
                    text = "$dayNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 7: AttendanceLegend
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
                .background(Color(0xFFFDC52A))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Present", fontSize = 12.sp, color = Color.White)

        Spacer(modifier = Modifier.width(18.dp))

        // Absent
        Box(
            modifier = Modifier
                .size(10.dp)
                .border(1.5.dp, Color(0xFFF97316), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Absent", fontSize = 12.sp, color = Color.White)

        Spacer(modifier = Modifier.width(18.dp))

        // Off / Upcoming
        Box(
            modifier = Modifier
                .size(10.dp)
                .border(1.5.dp, Color(0xFF525252), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Off / Upcoming", fontSize = 12.sp, color = Color(0xFFA3A3A3))
    }
}
