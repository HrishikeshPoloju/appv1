package com.schoolapp.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.schoolapp.android.data.AppRole
import com.schoolapp.android.data.StudentProfile
import com.schoolapp.android.ui.theme.GenesisColors

@Composable
fun SchoolHeader(
    schoolName: String = "GENESIS SCHOOL BEERAMGUDA",
    classSectionLabel: String = "",
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Surface(
        color = GenesisColors.MeteorBlack,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(GenesisColors.MeteorSurface, CircleShape)
                            .border(1.dp, GenesisColors.SkyAccent.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = GenesisColors.SkyAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = schoolName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Smart Digital Campus",
                            color = GenesisColors.SkyAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Profile Icon / Class & Section Pill in high-contrast Meteor Surface
                    Surface(
                        color = GenesisColors.MeteorSurface,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, GenesisColors.SkyAccent.copy(alpha = 0.55f)),
                        modifier = Modifier.clickable { onProfileClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Student Profile",
                                tint = GenesisColors.SkyAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            if (classSectionLabel.isNotBlank()) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = classSectionLabel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(GenesisColors.MeteorSurface, CircleShape)
                            .border(1.dp, GenesisColors.MeteorBorder, CircleShape)
                            .clickable { onNotificationClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = GenesisColors.MeteorBorder, thickness = 1.dp)
        }
    }
}

@Composable
fun ActiveProfileBanner(
    student: StudentProfile,
    isTeacherOrAdmin: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        color = GenesisColors.Navy,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = student.avatarUrl,
                contentDescription = student.fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${student.fullName}  (${student.studentCode})",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = student.bannerClassText,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }
            Surface(
                color = if (isTeacherOrAdmin) GenesisColors.ClassroomPurple.copy(alpha = 0.35f) else GenesisColors.Success.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isTeacherOrAdmin) Color(0xFFD8B4FE) else Color(0xFF86EFAC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isTeacherOrAdmin) Icons.Default.Edit else Icons.Default.Lock,
                        contentDescription = if (isTeacherOrAdmin) "Teacher Edit Student" else "Profile Locked",
                        tint = if (isTeacherOrAdmin) Color(0xFFD8B4FE) else Color(0xFF86EFAC),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTeacherOrAdmin) "Teacher Edit" else "Locked",
                        color = if (isTeacherOrAdmin) Color(0xFFD8B4FE) else Color(0xFF86EFAC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun LinkedStudentSelectorRow(
    students: List<StudentProfile>,
    activeStudent: StudentProfile,
    onSelectStudent: (StudentProfile) -> Unit,
    onAddStudentClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        students.forEach { student ->
            val isSelected = student.id == activeStudent.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelectStudent(student) }
                    .widthIn(min = 64.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = student.avatarUrl,
                        contentDescription = student.firstName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) GenesisColors.Primary else GenesisColors.Border,
                                shape = CircleShape
                            )
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(GenesisColors.Primary, CircleShape)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = student.firstName,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) GenesisColors.Primary else GenesisColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // + Add Student Control
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onAddStudentClick() }
                .widthIn(min = 56.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFF64748B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Student",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GenesisColors.TextSecondary
            )
        }
    }
}

@Composable
fun PrototypeRoleBar(
    currentRole: AppRole,
    onRoleSelected: (AppRole) -> Unit
) {
    Surface(
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PROTOTYPE ROLE:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GenesisColors.Primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppRole.values().forEach { role ->
                    val selected = role == currentRole
                    Surface(
                        color = if (selected) Color(role.badgeColor) else Color.White,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, if (selected) Color(role.badgeColor) else GenesisColors.Border),
                        modifier = Modifier.clickable { onRoleSelected(role) }
                    ) {
                        Text(
                            text = role.displayName,
                            color = if (selected) Color.White else GenesisColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    val (bg, fg) = when (category.uppercase()) {
        "EVENT" -> Color(0xFFDCFCE7) to GenesisColors.Success
        "ACADEMIC" -> Color(0xFFEDE9FE) to GenesisColors.ClassroomPurple
        "EMERGENCY" -> Color(0xFFFEE2E2) to GenesisColors.Emergency
        else -> Color(0xFFDBEAFE) to GenesisColors.Primary
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = category.uppercase(),
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SubjectChip(
    label: String,
    selected: Boolean,
    activeColor: Color = GenesisColors.Primary,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) activeColor else Color.White,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) activeColor else GenesisColors.Border),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else GenesisColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        "Ongoing", "Approved", "Paid", "Completed", "Resolved" -> Color(0xFFDCFCE7) to GenesisColors.Success
        "Next", "Open" -> Color(0xFFDBEAFE) to GenesisColors.Primary
        "Due Soon", "Pending Review" -> Color(0xFFFEF3C7) to GenesisColors.Warning
        "Pending", "Rejected" -> Color(0xFFFEE2E2) to GenesisColors.Emergency
        else -> Color(0xFFF1F5F9) to GenesisColors.TextSecondary
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = status,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit
) {
    Surface(
        color = GenesisColors.MeteorBlack,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

