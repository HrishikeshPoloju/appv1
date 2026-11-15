package com.schoolapp.android.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// ============================================================================
// HIGH-FIDELITY ILLUSTRATED LOGOS MATCHING REFERENCE IMAGE 2
// (Homework, Timetable, Attendance, Result + School Services)
// ============================================================================

/**
 * 1. HOMEWORK LOGO: Open textbook with lined pages, standing orange-red pencil,
 *    and circular badge on top corner.
 */
@Composable
fun HomeworkIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Book Cover Backing (Tan/Caramel Brown)
        drawRoundRect(
            color = Color(0xFFC27838),
            topLeft = Offset(w * 0.08f, h * 0.22f),
            size = Size(w * 0.70f, h * 0.68f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Left Book Page (Clean White)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.12f, h * 0.26f),
            size = Size(w * 0.31f, h * 0.60f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Right Book Page (Soft Off-White)
        drawRoundRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(w * 0.43f, h * 0.26f),
            size = Size(w * 0.32f, h * 0.60f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Spine Crease
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(w * 0.43f, h * 0.26f),
            end = Offset(w * 0.43f, h * 0.86f),
            strokeWidth = 2f
        )

        // Ruled Text Lines on Left Page
        drawLine(
            color = Color(0xFF475569),
            start = Offset(w * 0.16f, h * 0.44f),
            end = Offset(w * 0.38f, h * 0.44f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF475569),
            start = Offset(w * 0.16f, h * 0.56f),
            end = Offset(w * 0.38f, h * 0.56f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF475569),
            start = Offset(w * 0.16f, h * 0.68f),
            end = Offset(w * 0.34f, h * 0.68f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // Ruled Text Lines on Right Page
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(w * 0.47f, h * 0.44f),
            end = Offset(w * 0.69f, h * 0.44f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(w * 0.47f, h * 0.56f),
            end = Offset(w * 0.69f, h * 0.56f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(w * 0.47f, h * 0.68f),
            end = Offset(w * 0.63f, h * 0.68f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // Pencil Body (Vibrant Coral/Orange)
        drawRoundRect(
            color = Color(0xFFEF4444),
            topLeft = Offset(w * 0.77f, h * 0.22f),
            size = Size(w * 0.14f, h * 0.52f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Pencil Eraser (Pink)
        drawRoundRect(
            color = Color(0xFFFB7185),
            topLeft = Offset(w * 0.77f, h * 0.14f),
            size = Size(w * 0.14f, h * 0.08f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Silver Ferrule Band
        drawRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(w * 0.77f, h * 0.22f),
            size = Size(w * 0.14f, h * 0.04f)
        )

        // Pencil Cone Tip (Peach Wood)
        val tipPath = Path().apply {
            moveTo(w * 0.77f, h * 0.74f)
            lineTo(w * 0.91f, h * 0.74f)
            lineTo(w * 0.84f, h * 0.90f)
            close()
        }
        drawPath(tipPath, color = Color(0xFFFED7AA))

        // Pencil Graphite Lead
        val leadPath = Path().apply {
            moveTo(w * 0.81f, h * 0.84f)
            lineTo(w * 0.87f, h * 0.84f)
            lineTo(w * 0.84f, h * 0.90f)
            close()
        }
        drawPath(leadPath, color = Color(0xFF334155))

        // Circular Badge on Top-Left Corner (Cyan Badge with Gold Icon)
        drawCircle(
            color = Color(0xFF0284C7),
            radius = w * 0.15f,
            center = Offset(w * 0.22f, h * 0.24f)
        )
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = w * 0.12f,
            center = Offset(w * 0.22f, h * 0.24f)
        )
        // Gold House/Star inside badge
        val badgeIcon = Path().apply {
            moveTo(w * 0.22f, h * 0.17f)
            lineTo(w * 0.28f, h * 0.23f)
            lineTo(w * 0.26f, h * 0.23f)
            lineTo(w * 0.26f, h * 0.29f)
            lineTo(w * 0.18f, h * 0.29f)
            lineTo(w * 0.18f, h * 0.23f)
            lineTo(w * 0.16f, h * 0.23f)
            close()
        }
        drawPath(badgeIcon, color = Color(0xFFFEF08A))
    }
}

/**
 * 2. TIMETABLE LOGO: Blue calendar grid with cute pink circular clock at bottom right.
 */
@Composable
fun TimetableIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Calendar White Backing
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(w * 0.72f, h * 0.72f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(w * 0.72f, h * 0.72f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 2f)
        )

        // Top Ocean Blue Banner
        val headerPath = Path().apply {
            moveTo(w * 0.10f, h * 0.28f)
            lineTo(w * 0.10f, h * 0.18f)
            quadraticBezierTo(w * 0.10f, h * 0.12f, w * 0.18f, h * 0.12f)
            lineTo(w * 0.74f, h * 0.12f)
            quadraticBezierTo(w * 0.82f, h * 0.12f, w * 0.82f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.28f)
            close()
        }
        drawPath(headerPath, color = Color(0xFF2563EB))

        // Top Binder Rings / Tabs
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.26f, h * 0.19f))
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.46f, h * 0.19f))
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.66f, h * 0.19f))

        // Calendar Grid Dots / Cells (Blue Day Matrix)
        val cols = 4
        val rows = 3
        val startX = w * 0.18f
        val startY = h * 0.35f
        val cellW = w * 0.10f
        val cellH = h * 0.08f
        val gapX = w * 0.045f
        val gapY = h * 0.045f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // Skip bottom-right where clock sits
                if (r == rows - 1 && c >= cols - 2) continue
                drawRoundRect(
                    color = Color(0xFF3B82F6),
                    topLeft = Offset(startX + c * (cellW + gapX), startY + r * (cellH + gapY)),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }

        // Clock Badge at Bottom-Right Corner (Pink / Rose)
        val clockCenter = Offset(w * 0.74f, h * 0.72f)
        val clockRadius = w * 0.21f

        // White halo border
        drawCircle(
            color = Color.White,
            radius = clockRadius + 3f,
            center = clockCenter
        )
        // Pink outer rim
        drawCircle(
            color = Color(0xFFF43F5E),
            radius = clockRadius,
            center = clockCenter
        )
        // White clock face
        drawCircle(
            color = Color.White,
            radius = clockRadius * 0.82f,
            center = clockCenter
        )

        // Clock Tick Marks
        drawLine(
            color = Color(0xFFFDA4AF),
            start = Offset(clockCenter.x, clockCenter.y - clockRadius * 0.72f),
            end = Offset(clockCenter.x, clockCenter.y - clockRadius * 0.52f),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0xFFFDA4AF),
            start = Offset(clockCenter.x + clockRadius * 0.52f, clockCenter.y),
            end = Offset(clockCenter.x + clockRadius * 0.72f, clockCenter.y),
            strokeWidth = 1.5f
        )

        // Clock Hands (Rose pink pointing to 3:00)
        drawLine(
            color = Color(0xFFE11D48),
            start = clockCenter,
            end = Offset(clockCenter.x, clockCenter.y - clockRadius * 0.52f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFE11D48),
            start = clockCenter,
            end = Offset(clockCenter.x + clockRadius * 0.55f, clockCenter.y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        // Center Pin
        drawCircle(color = Color(0xFFBE123C), radius = 2f, center = clockCenter)
    }
}

/**
 * 3. ATTENDANCE LOGO: Student figure + Calendar sheet with green checkmark.
 */
@Composable
fun AttendanceIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // --- Left Side: Student Avatar ---
        // Blue Torso / Shirt
        drawRoundRect(
            color = Color(0xFF0284C7),
            topLeft = Offset(w * 0.10f, h * 0.56f),
            size = Size(w * 0.34f, h * 0.36f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // V-Neck White Collar
        val collarPath = Path().apply {
            moveTo(w * 0.22f, h * 0.56f)
            lineTo(w * 0.27f, h * 0.68f)
            lineTo(w * 0.32f, h * 0.56f)
            close()
        }
        drawPath(collarPath, color = Color.White)

        // Neck
        drawRect(
            color = Color(0xFFFCD34D),
            topLeft = Offset(w * 0.23f, h * 0.46f),
            size = Size(w * 0.08f, h * 0.12f)
        )

        // Student Head (Peach/Tan)
        drawCircle(
            color = Color(0xFFFDE68A),
            radius = w * 0.15f,
            center = Offset(w * 0.27f, h * 0.36f)
        )

        // Brown Hair (Stylish Swoop)
        val hairPath = Path().apply {
            moveTo(w * 0.12f, h * 0.36f)
            quadraticBezierTo(w * 0.14f, h * 0.18f, w * 0.30f, h * 0.20f)
            quadraticBezierTo(w * 0.42f, h * 0.22f, w * 0.40f, h * 0.36f)
            lineTo(w * 0.34f, h * 0.30f)
            lineTo(w * 0.20f, h * 0.30f)
            close()
        }
        drawPath(hairPath, color = Color(0xFF78350F))

        // --- Right Side: Verified Attendance Calendar Sheet ---
        val calLeft = w * 0.48f
        val calTop = h * 0.16f
        val calW = w * 0.44f
        val calH = h * 0.60f

        // White Paper Sheet
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(calLeft, calTop),
            size = Size(calW, calH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(calLeft, calTop),
            size = Size(calW, calH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 1.8f)
        )

        // Red Header Bar on Calendar
        val calHeader = Path().apply {
            moveTo(calLeft, calTop + calH * 0.28f)
            lineTo(calLeft, calTop + 6f)
            quadraticBezierTo(calLeft, calTop, calLeft + 6f, calTop)
            lineTo(calLeft + calW - 6f, calTop)
            quadraticBezierTo(calLeft + calW, calTop, calLeft + calW, calTop + 6f)
            lineTo(calLeft + calW, calTop + calH * 0.28f)
            close()
        }
        drawPath(calHeader, color = Color(0xFFEF4444))

        // Small Binder Rings on Red Top
        drawCircle(color = Color(0xFF94A3B8), radius = 2f, center = Offset(calLeft + calW * 0.30f, calTop + 3f))
        drawCircle(color = Color(0xFF94A3B8), radius = 2f, center = Offset(calLeft + calW * 0.70f, calTop + 3f))

        // Big Vibrant Green Checkmark (Attendance Verified)
        val checkPath = Path().apply {
            moveTo(calLeft + calW * 0.22f, calTop + calH * 0.58f)
            lineTo(calLeft + calW * 0.45f, calTop + calH * 0.78f)
            lineTo(calLeft + calW * 0.82f, calTop + calH * 0.42f)
        }
        drawPath(
            checkPath,
            color = Color(0xFF22C55E),
            style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * 4. RESULT LOGO: Clipboard with lined exam sheet, yellow top clip,
 *    and circular green checkmark badge at top right.
 */
@Composable
fun ResultIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Clipboard Backing (Wood Tone / Dark Slate)
        drawRoundRect(
            color = Color(0xFF64748B),
            topLeft = Offset(w * 0.12f, h * 0.14f),
            size = Size(w * 0.68f, h * 0.76f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = Color(0xFF475569),
            topLeft = Offset(w * 0.14f, h * 0.16f),
            size = Size(w * 0.64f, h * 0.72f),
            cornerRadius = CornerRadius(7f, 7f)
        )

        // White Exam Results Paper Sheet
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.20f, h * 0.22f),
            size = Size(w * 0.52f, h * 0.62f),
            cornerRadius = CornerRadius(5f, 5f)
        )

        // Yellow Metal Binder Clip at Top
        drawRoundRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(w * 0.32f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.14f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Clip rivet holes
        drawCircle(color = Color(0xFF78350F), radius = 2.5f, center = Offset(w * 0.39f, h * 0.15f))
        drawCircle(color = Color(0xFF78350F), radius = 2.5f, center = Offset(w * 0.53f, h * 0.15f))

        // Ruled Score Lines on Exam Sheet
        val lineY = listOf(h * 0.38f, h * 0.50f, h * 0.62f, h * 0.74f)
        lineY.forEachIndexed { i, y ->
            drawLine(
                color = if (i == 0) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                start = Offset(w * 0.26f, y),
                end = Offset(w * (0.64f - (i % 2) * 0.06f), y),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }

        // Circular Green Verified Badge on Top Right
        val badgeCenter = Offset(w * 0.76f, h * 0.22f)
        val badgeR = w * 0.17f

        // White border
        drawCircle(
            color = Color.White,
            radius = badgeR + 2.5f,
            center = badgeCenter
        )
        // Green circle
        drawCircle(
            color = Color(0xFF22C55E),
            radius = badgeR,
            center = badgeCenter
        )
        // White checkmark
        val markPath = Path().apply {
            moveTo(badgeCenter.x - badgeR * 0.45f, badgeCenter.y)
            lineTo(badgeCenter.x - badgeR * 0.10f, badgeCenter.y + badgeR * 0.38f)
            lineTo(badgeCenter.x + badgeR * 0.50f, badgeCenter.y - badgeR * 0.40f)
        }
        drawPath(
            markPath,
            color = Color.White,
            style = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * 5. FEE STRUCTURE LOGO: Indigo / Emerald wallet with shiny gold Rupee coin.
 */
@Composable
fun FeeIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Wallet Back
        drawRoundRect(
            color = Color(0xFF1E3A8A),
            topLeft = Offset(w * 0.12f, h * 0.24f),
            size = Size(w * 0.76f, h * 0.60f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Wallet Flap
        drawRoundRect(
            color = Color(0xFF2563EB),
            topLeft = Offset(w * 0.12f, h * 0.34f),
            size = Size(w * 0.76f, h * 0.50f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Golden Rupee Badge
        val coinCenter = Offset(w * 0.68f, h * 0.56f)
        drawCircle(color = Color(0xFFFBBF24), radius = w * 0.16f, center = coinCenter)
        drawCircle(color = Color(0xFFF59E0B), radius = w * 0.12f, center = coinCenter)
    }
}

/**
 * 6. STUDY NOTES LOGO: Clean document with red "PDF" tab and folded corner.
 */
@Composable
fun NotesIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Document Paper
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.18f, h * 0.14f),
            size = Size(w * 0.64f, h * 0.76f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(w * 0.18f, h * 0.14f),
            size = Size(w * 0.64f, h * 0.76f),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 2f)
        )

        // Red PDF Header Badge
        drawRoundRect(
            color = Color(0xFFDC2626),
            topLeft = Offset(w * 0.18f, h * 0.14f),
            size = Size(w * 0.34f, h * 0.20f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Ruled Note Lines
        drawLine(color = Color(0xFF94A3B8), start = Offset(w * 0.26f, h * 0.46f), end = Offset(w * 0.74f, h * 0.46f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFF94A3B8), start = Offset(w * 0.26f, h * 0.58f), end = Offset(w * 0.74f, h * 0.58f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFF94A3B8), start = Offset(w * 0.26f, h * 0.70f), end = Offset(w * 0.60f, h * 0.70f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}

/**
 * 7. CLASS DOUBTS LOGO: Friendly dual speech bubbles with question mark.
 */
@Composable
fun DoubtsIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Teal Main Chat Bubble
        drawRoundRect(
            color = Color(0xFF0D9488),
            topLeft = Offset(w * 0.10f, h * 0.18f),
            size = Size(w * 0.56f, h * 0.46f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        // Violet Sub Bubble
        drawRoundRect(
            color = Color(0xFF7C3AED),
            topLeft = Offset(w * 0.38f, h * 0.42f),
            size = Size(w * 0.52f, h * 0.44f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // White question mark dots
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.26f, h * 0.40f))
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.38f, h * 0.40f))
        drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.64f, h * 0.64f))
    }
}

/**
 * 8. AI COPILOT / ASK AI LOGO: Smiling friendly AI companion with glowing purple/cyan aura.
 */
@Composable
fun AiTutorIllustration(modifier: Modifier = Modifier.size(38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft Violet Glow
        drawCircle(
            color = Color(0xFFEDE9FE),
            radius = w * 0.44f,
            center = Offset(w * 0.50f, h * 0.50f)
        )

        // Robot Head Body
        drawRoundRect(
            color = Color(0xFF7C3AED),
            topLeft = Offset(w * 0.20f, h * 0.24f),
            size = Size(w * 0.60f, h * 0.54f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Visor Screen (Cyan)
        drawRoundRect(
            color = Color(0xFF22D3EE),
            topLeft = Offset(w * 0.28f, h * 0.34f),
            size = Size(w * 0.44f, h * 0.28f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Happy Robot Eyes (Dark Navy)
        drawCircle(color = Color(0xFF0F172A), radius = 2.5f, center = Offset(w * 0.40f, h * 0.48f))
        drawCircle(color = Color(0xFF0F172A), radius = 2.5f, center = Offset(w * 0.60f, h * 0.48f))

        // Cute Golden Antenna Star
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(w * 0.50f, h * 0.24f),
            end = Offset(w * 0.50f, h * 0.12f),
            strokeWidth = 2.5f
        )
        drawCircle(color = Color(0xFFFBBF24), radius = 3.5f, center = Offset(w * 0.50f, h * 0.10f))
    }
}

/**
 * 9. HERO 3D ART: Graduation Cap & Stacked Books illustration matching reference image 1.
 */
@Composable
fun ReadyToLearnHeroIllustration(modifier: Modifier = Modifier.size(90.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // --- Bottom Book (Blue Book) ---
        val book3 = Path().apply {
            moveTo(w * 0.20f, h * 0.82f)
            lineTo(w * 0.78f, h * 0.74f)
            lineTo(w * 0.90f, h * 0.84f)
            lineTo(w * 0.30f, h * 0.92f)
            close()
        }
        drawPath(book3, color = Color(0xFF3B82F6))

        // --- Middle Book (Coral/Pink Book) ---
        val book2 = Path().apply {
            moveTo(w * 0.25f, h * 0.68f)
            lineTo(w * 0.82f, h * 0.60f)
            lineTo(w * 0.92f, h * 0.70f)
            lineTo(w * 0.34f, h * 0.78f)
            close()
        }
        drawPath(book2, color = Color(0xFFF43F5E))

        // --- Top Book (Gold/Yellow Spiral Notebook) ---
        val book1 = Path().apply {
            moveTo(w * 0.28f, h * 0.54f)
            lineTo(w * 0.82f, h * 0.46f)
            lineTo(w * 0.90f, h * 0.56f)
            lineTo(w * 0.36f, h * 0.64f)
            close()
        }
        drawPath(book1, color = Color(0xFFFBBF24))

        // --- Graduation Cap (Violet Mortarboard at Top) ---
        val capDiamond = Path().apply {
            moveTo(w * 0.58f, h * 0.12f)
            lineTo(w * 0.94f, h * 0.24f)
            lineTo(w * 0.62f, h * 0.36f)
            lineTo(w * 0.26f, h * 0.24f)
            close()
        }
        drawPath(capDiamond, color = Color(0xFF6D28D9))

        // Cap Skull Cap Underneath
        val capBase = Path().apply {
            moveTo(w * 0.44f, h * 0.28f)
            lineTo(w * 0.76f, h * 0.28f)
            lineTo(w * 0.72f, h * 0.40f)
            lineTo(w * 0.48f, h * 0.40f)
            close()
        }
        drawPath(capBase, color = Color(0xFF5B21B6))

        // Gold Cap Tassel
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(w * 0.60f, h * 0.24f),
            end = Offset(w * 0.22f, h * 0.36f),
            strokeWidth = 2.5f
        )
        drawCircle(color = Color(0xFFF59E0B), radius = 3.5f, center = Offset(w * 0.22f, h * 0.38f))
    }
}
