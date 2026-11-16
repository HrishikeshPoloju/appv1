package com.schoolapp.android.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolapp.android.R
import com.schoolapp.android.data.StudentProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * 100% Exact Pixel-Perfect Attendance Record Screen
 * Matches the reference mockup (media_1794849460919_06c3f9d1.jpg):
 * - Deep dark theme (#111111) with circular back button & student class header
 * - Hero 94.0% Overall Attendance card with reading student illustration
 * - Three stat tiles: 134 Total Days, 126 Present, 8 Absent
 * - Horizontal month selector chips (Jun '26 to Nov '26 active)
 * - November 2026 calendar with hand-drawn yellow sketched circles for Present days,
 *   orange outlined circle for Absent day (Nov 8), dark circles for off-days, and beaker flask
 * - Bottom attendance legend (Present, Absent, Off / Upcoming)
 */
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
            .background(Color(0xFF111111))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // 1. Top App Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back Button
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
                    text = "${student.fullName} • ${student.className} – Section ${student.section}",
                    fontSize = 13.sp,
                    color = Color(0xFFA3A3A3),
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Hero Card: 94.0% Overall Attendance + Student Character
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner_attendance_hero),
                contentDescription = "94.0% Overall Attendance",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(514f / 171f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Three Stat Cards (Total Days, Present, Absent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stat 1: Total Days
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
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
                            text = "134",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24) // Yellow
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Days",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Stat 2: Present
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
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
                            text = "126",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Present",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Stat 3: Absent
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
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
                            text = "8",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316) // Muted orange
                        )
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Absent",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Horizontal Month Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEachIndexed { index, (_, label) ->
                val isSelected = index == selectedMonthIndex
                Surface(
                    color = if (isSelected) Color(0xFFFDC52A) else Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable { selectedMonthIndex = index }
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

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Calendar Card
        Card(
            modifier = Modifier
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
                        // Left Arrow
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFDC52A))
                                .clickable {
                                    if (selectedMonthIndex > 0) selectedMonthIndex--
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Prev",
                                tint = Color(0xFF111111),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Month Name
                        Text(
                            text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${currentYearMonth.year}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Right Arrow
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFDC52A))
                            .clickable {
                                if (selectedMonthIndex < months.lastIndex) selectedMonthIndex++
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next",
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

                    // Days Grid for November 2026 (Nov 1 is Sunday)
                    val daysInMonth = currentYearMonth.lengthOfMonth()
                    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek
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
                                    val isSunday = col == 0
                                    val isSaturday = col == 6

                                    // Attendance state based on the reference mockup:
                                    // Present: Days 2, 3, 4, 5, 6, 7, 9, 10
                                    // Absent: Day 8 (Sunday special class or recorded absent)
                                    // Off: Days 1, 14, 15, 21, 22, 28, 29 (Sundays & 2nd/4th Saturdays)
                                    // Upcoming/unmarked: Days 11, 12, 13, 16, 17, 18, 19, 20, 23, 24, 25, 26, 27, 30
                                    val isPresent = dayNum in listOf(2, 3, 4, 5, 6, 7, 9, 10)
                                    val isAbsent = dayNum == 8
                                    val isOff = dayNum in listOf(1, 14, 15, 21, 22, 28, 29)

                                    CalendarDayCell(
                                        dayNumber = dayNum,
                                        isPresent = isPresent,
                                        isAbsent = isAbsent,
                                        isOff = isOff,
                                        isSelected = selectedDay == dayNum,
                                        onClick = { selectedDay = dayNum }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Legend: Present, Absent, Off / Upcoming
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                // 6. Science Beaker Flask Illustration in Bottom Right Corner
                Image(
                    painter = painterResource(id = R.drawable.ic_attendance_beaker),
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 44.dp)
                )
            }
        }
    }
}

/**
 * Custom Day Cell:
 * - Present: Textured/sketched yellow circle (#FDC52A) with bold black day text
 * - Absent: Outlined orange circle (#F97316) with orange day text
 * - Off: Dark circle (#2A2A2A) with muted grey day text
 * - Normal: Plain white day text
 */
@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isPresent: Boolean,
    isAbsent: Boolean,
    isOff: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isPresent -> {
                // Hand-drawn sketch circle effect
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

                    // Ring 1
                    drawCircle(
                        color = Color(0xFFF97316),
                        radius = radius * 0.94f,
                        center = center,
                        style = Stroke(width = 1.8.dp.toPx())
                    )

                    // Sketch Ring 2
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
                // Dark grey off-day circle
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
                // Normal weekday text
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
