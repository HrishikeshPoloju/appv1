package com.schoolapp.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolapp.android.R
import com.schoolapp.android.ui.theme.GenesisColors
import com.schoolapp.android.ui.theme.LocalSchoolBrand

// ============================================================================
// VECTOR LINE-ART & LEARNING ILLUSTRATIONS (Inspired by Reference Images 1, 2, 3)
// Clean 2D line art with crisp vector paths for topic cards, milestones & hero banners
// ============================================================================

/**
 * 1. STUDENT DISCUSSION ILLUSTRATION (Image 2 - Peach Card: "Everyday Conversations")
 * Two students discussing concepts, shaking hands/collaborating with friendly line art.
 */
@Composable
fun StudentDiscussionIllustration(
    modifier: Modifier = Modifier.size(100.dp, 80.dp),
    tint: Color = Color(0xFF1E293B)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Left Student: Head, Hair & Body
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.28f, h * 0.32f))
        drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.28f, h * 0.32f), style = stroke)

        // Left Student Hair
        val hairLeft = Path().apply {
            moveTo(w * 0.18f, h * 0.28f)
            quadraticTo(w * 0.25f, h * 0.16f, w * 0.36f, h * 0.22f)
            quadraticTo(w * 0.38f, h * 0.28f, w * 0.36f, h * 0.34f)
        }
        drawPath(hairLeft, color = tint, style = stroke)

        // Left Student Torso & Shirt
        val bodyLeft = Path().apply {
            moveTo(w * 0.28f, h * 0.44f)
            lineTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.15f, h * 0.65f)
            lineTo(w * 0.15f, h * 0.95f)
            lineTo(w * 0.42f, h * 0.95f)
            lineTo(w * 0.42f, h * 0.65f)
            close()
        }
        drawPath(bodyLeft, color = Color.White)
        drawPath(bodyLeft, color = tint, style = stroke)

        // Left Student Tie
        drawLine(color = tint, start = Offset(w * 0.28f, h * 0.54f), end = Offset(w * 0.28f, h * 0.72f), strokeWidth = 2.dp.toPx())

        // Right Student: Head & Body
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.72f, h * 0.32f))
        drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.72f, h * 0.32f), style = stroke)

        // Right Student Hair
        val hairRight = Path().apply {
            moveTo(w * 0.62f, h * 0.28f)
            quadraticTo(w * 0.72f, h * 0.16f, w * 0.82f, h * 0.26f)
            quadraticTo(w * 0.84f, h * 0.32f, w * 0.80f, h * 0.36f)
        }
        drawPath(hairRight, color = tint, style = stroke)

        // Right Student Coat / Blazer
        val bodyRight = Path().apply {
            moveTo(w * 0.72f, h * 0.44f)
            lineTo(w * 0.72f, h * 0.52f)
            lineTo(w * 0.58f, h * 0.65f)
            lineTo(w * 0.58f, h * 0.95f)
            lineTo(w * 0.85f, h * 0.95f)
            lineTo(w * 0.85f, h * 0.65f)
            close()
        }
        drawPath(bodyRight, color = Color.White)
        drawPath(bodyRight, color = tint, style = stroke)

        // Handshake / Discussion Center Arms
        val armLeft = Path().apply {
            moveTo(w * 0.38f, h * 0.65f)
            lineTo(w * 0.50f, h * 0.75f)
        }
        drawPath(armLeft, color = tint, style = stroke)

        val armRight = Path().apply {
            moveTo(w * 0.62f, h * 0.65f)
            lineTo(w * 0.50f, h * 0.75f)
        }
        drawPath(armRight, color = tint, style = stroke)

        // Small handshake clasp
        drawCircle(color = tint, radius = 3.dp.toPx(), center = Offset(w * 0.50f, h * 0.75f))
    }
}

/**
 * 2. STUDENT WHITEBOARD ILLUSTRATION (Image 2 - Mint Green Card: "Workplace / Classroom Basics")
 * Student presenting formulas / charts at a classroom whiteboard.
 */
@Composable
fun StudentWhiteboardIllustration(
    modifier: Modifier = Modifier.size(100.dp, 80.dp),
    tint: Color = Color(0xFF1E293B)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Whiteboard Frame on the left
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.08f, h * 0.18f),
            size = Size(w * 0.48f, h * 0.65f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.18f),
            size = Size(w * 0.48f, h * 0.65f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = stroke
        )

        // Horizontal Chart Lines / Diagrams inside Whiteboard
        drawLine(color = tint, start = Offset(w * 0.14f, h * 0.32f), end = Offset(w * 0.38f, h * 0.32f), strokeWidth = 2.dp.toPx())
        drawLine(color = tint, start = Offset(w * 0.14f, h * 0.44f), end = Offset(w * 0.32f, h * 0.44f), strokeWidth = 2.dp.toPx())
        drawLine(color = tint, start = Offset(w * 0.14f, h * 0.56f), end = Offset(w * 0.42f, h * 0.56f), strokeWidth = 2.dp.toPx())

        // Circular Pie/Concept on Whiteboard
        drawCircle(color = tint, radius = w * 0.07f, center = Offset(w * 0.42f, h * 0.38f), style = stroke)

        // Student Presenter on the right
        drawCircle(color = Color.White, radius = w * 0.11f, center = Offset(w * 0.74f, h * 0.32f))
        drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.74f, h * 0.32f), style = stroke)

        // Hair Bob
        val hair = Path().apply {
            moveTo(w * 0.63f, h * 0.32f)
            quadraticTo(w * 0.74f, h * 0.18f, w * 0.85f, h * 0.32f)
            lineTo(w * 0.85f, h * 0.42f)
            lineTo(w * 0.63f, h * 0.42f)
            close()
        }
        drawPath(hair, color = tint)

        // Body / Blazer
        val body = Path().apply {
            moveTo(w * 0.74f, h * 0.43f)
            lineTo(w * 0.62f, h * 0.62f)
            lineTo(w * 0.62f, h * 0.95f)
            lineTo(w * 0.88f, h * 0.95f)
            lineTo(w * 0.88f, h * 0.62f)
            close()
        }
        drawPath(body, color = Color.White)
        drawPath(body, color = tint, style = stroke)

        // Arm pointing to Whiteboard
        val pointingArm = Path().apply {
            moveTo(w * 0.64f, h * 0.62f)
            lineTo(w * 0.46f, h * 0.50f)
        }
        drawPath(pointingArm, color = tint, style = stroke)
    }
}

/**
 * 3. STUDENT CELEBRATION ILLUSTRATION (Image 2 - Periwinkle Blue Card: "Social Settings / Milestones")
 * Student celebrating with raised arms and confetti/sparklers.
 */
@Composable
fun StudentCelebrationIllustration(
    modifier: Modifier = Modifier.size(100.dp, 80.dp),
    tint: Color = Color(0xFF1E293B)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Floating Confetti / Sparklers
        drawLine(color = Color(0xFFF59E0B), start = Offset(w * 0.18f, h * 0.22f), end = Offset(w * 0.24f, h * 0.28f), strokeWidth = 2.5.dp.toPx())
        drawLine(color = Color(0xFF3B82F6), start = Offset(w * 0.32f, h * 0.14f), end = Offset(w * 0.36f, h * 0.22f), strokeWidth = 2.dp.toPx())
        drawLine(color = Color(0xFFEF4444), start = Offset(w * 0.64f, h * 0.14f), end = Offset(w * 0.68f, h * 0.22f), strokeWidth = 2.dp.toPx())
        drawLine(color = Color(0xFF10B981), start = Offset(w * 0.78f, h * 0.22f), end = Offset(w * 0.84f, h * 0.28f), strokeWidth = 2.5.dp.toPx())

        // Sparkler Starbursts
        drawCircle(color = Color(0xFFF59E0B), radius = 2.5.dp.toPx(), center = Offset(w * 0.20f, h * 0.36f))
        drawCircle(color = Color(0xFF8B5CF6), radius = 2.5.dp.toPx(), center = Offset(w * 0.82f, h * 0.36f))

        // Student Head
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.42f))
        drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.42f), style = stroke)

        // Cheerful smiling hair & face
        val hair = Path().apply {
            moveTo(w * 0.40f, h * 0.38f)
            quadraticTo(w * 0.50f, h * 0.24f, w * 0.60f, h * 0.38f)
        }
        drawPath(hair, color = tint, style = stroke)

        // Student Body
        val body = Path().apply {
            moveTo(w * 0.50f, h * 0.54f)
            lineTo(w * 0.38f, h * 0.68f)
            lineTo(w * 0.38f, h * 0.95f)
            lineTo(w * 0.62f, h * 0.95f)
            lineTo(w * 0.62f, h * 0.68f)
            close()
        }
        drawPath(body, color = Color.White)
        drawPath(body, color = tint, style = stroke)

        // Raised Arms Holding Sparklers (V for Victory)
        val leftArm = Path().apply {
            moveTo(w * 0.38f, h * 0.68f)
            lineTo(w * 0.24f, h * 0.42f)
            lineTo(w * 0.20f, h * 0.36f)
        }
        drawPath(leftArm, color = tint, style = stroke)

        val rightArm = Path().apply {
            moveTo(w * 0.62f, h * 0.68f)
            lineTo(w * 0.76f, h * 0.42f)
            lineTo(w * 0.80f, h * 0.36f)
        }
        drawPath(rightArm, color = tint, style = stroke)
    }
}

/**
 * 4. STUDENT STUDY DESK ILLUSTRATION (Image 2 - Pastel Pink Card: "Professional Development")
 * Two students collaborating over textbooks and tea/coffee cups.
 */
@Composable
fun StudentReadingDeskIllustration(
    modifier: Modifier = Modifier.size(100.dp, 80.dp),
    tint: Color = Color(0xFF1E293B)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Study Desk Table Surface
        drawLine(color = tint, start = Offset(w * 0.08f, h * 0.78f), end = Offset(w * 0.92f, h * 0.78f), strokeWidth = 2.5.dp.toPx())

        // Coffee Cups / Books on Desk
        drawRoundRect(color = Color.White, topLeft = Offset(w * 0.22f, h * 0.68f), size = Size(w * 0.10f, h * 0.10f), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(w * 0.22f, h * 0.68f), size = Size(w * 0.10f, h * 0.10f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)

        drawRoundRect(color = Color.White, topLeft = Offset(w * 0.68f, h * 0.68f), size = Size(w * 0.10f, h * 0.10f), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(w * 0.68f, h * 0.68f), size = Size(w * 0.10f, h * 0.10f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)

        // Open Notebook in center
        drawRoundRect(color = Color.White, topLeft = Offset(w * 0.40f, h * 0.72f), size = Size(w * 0.20f, h * 0.06f), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(color = tint, topLeft = Offset(w * 0.40f, h * 0.72f), size = Size(w * 0.20f, h * 0.06f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)

        // Left Student (leaning in)
        drawCircle(color = Color.White, radius = w * 0.11f, center = Offset(w * 0.28f, h * 0.40f))
        drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.28f, h * 0.40f), style = stroke)
        val leftBody = Path().apply {
            moveTo(w * 0.28f, h * 0.51f)
            lineTo(w * 0.18f, h * 0.78f)
            lineTo(w * 0.38f, h * 0.78f)
            close()
        }
        drawPath(leftBody, color = Color.White)
        drawPath(leftBody, color = tint, style = stroke)

        // Right Student (leaning in)
        drawCircle(color = Color.White, radius = w * 0.11f, center = Offset(w * 0.72f, h * 0.40f))
        drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.72f, h * 0.40f), style = stroke)
        val rightBody = Path().apply {
            moveTo(w * 0.72f, h * 0.51f)
            lineTo(w * 0.62f, h * 0.78f)
            lineTo(w * 0.82f, h * 0.78f)
            close()
        }
        drawPath(rightBody, color = Color.White)
        drawPath(rightBody, color = tint, style = stroke)
    }
}

/**
 * 5. SCIENTIST DISCOVERY ILLUSTRATION (Image 3 - Hero Card: "Scientific Wonders")
 * Student scientist with lab equipment, book & atomic orbit lines.
 */
@Composable
fun ScientistDiscoveryIllustration(
    modifier: Modifier = Modifier.size(110.dp, 90.dp),
    tint: Color = Color(0xFF1E293B)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Atomic Orbit Rings
        val orbit1 = Path().apply {
            moveTo(w * 0.65f, h * 0.20f)
            cubicTo(w * 0.95f, h * 0.10f, w * 0.95f, h * 0.50f, w * 0.65f, h * 0.40f)
            cubicTo(w * 0.35f, h * 0.30f, w * 0.35f, h * 0.70f, w * 0.65f, h * 0.60f)
        }
        drawPath(orbit1, color = Color(0xFFF59E0B).copy(alpha = 0.4f), style = Stroke(width = 1.5.dp.toPx()))

        // Central Nucleus Dot
        drawCircle(color = Color(0xFFF59E0B), radius = 3.dp.toPx(), center = Offset(w * 0.65f, h * 0.38f))

        // Scientist Student on Left
        drawCircle(color = Color.White, radius = w * 0.12f, center = Offset(w * 0.34f, h * 0.34f))
        drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.34f, h * 0.34f), style = stroke)

        // Scientist Safety Spectacles
        drawRoundRect(color = tint, topLeft = Offset(w * 0.26f, h * 0.30f), size = Size(w * 0.18f, h * 0.08f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)

        // Scientist Lab Coat
        val coat = Path().apply {
            moveTo(w * 0.34f, h * 0.46f)
            lineTo(w * 0.20f, h * 0.62f)
            lineTo(w * 0.20f, h * 0.95f)
            lineTo(w * 0.52f, h * 0.95f)
            lineTo(w * 0.52f, h * 0.62f)
            close()
        }
        drawPath(coat, color = Color.White)
        drawPath(coat, color = tint, style = stroke)

        // Open Science Notebook / Beaker in Hands
        val book = Path().apply {
            moveTo(w * 0.38f, h * 0.68f)
            lineTo(w * 0.52f, h * 0.62f)
            lineTo(w * 0.52f, h * 0.78f)
            lineTo(w * 0.38f, h * 0.84f)
            close()
        }
        drawPath(book, color = Color(0xFFFEF3C7))
        drawPath(book, color = tint, style = stroke)
    }
}

/**
 * 6. ARCHED MILESTONE ILLUSTRATION (Image 1 - Arched Cards: "Reached Milestones" / "Evolver")
 * Cute student reading books with arched background header.
 */
@Composable
fun ArchedMilestoneIllustration(
    modifier: Modifier = Modifier.size(80.dp, 80.dp),
    accentColor: Color = Color(0xFF8B5CF6)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top Arch Background
        val arch = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.45f)
            quadraticTo(w * 0.5f, 0f, w, h * 0.45f)
            lineTo(w, h)
            close()
        }
        drawPath(arch, color = accentColor.copy(alpha = 0.15f))

        // Student Head & Cheerful Face
        val headRadius = w * 0.18f
        val headCenter = Offset(w * 0.5f, h * 0.52f)
        drawCircle(color = Color.White, radius = headRadius, center = headCenter)
        drawCircle(color = Color(0xFF1E293B), radius = headRadius, center = headCenter, style = Stroke(width = 2.dp.toPx()))

        // Cute Spectacles
        drawCircle(color = Color(0xFF1E293B), radius = 5.dp.toPx(), center = Offset(headCenter.x - 6.dp.toPx(), headCenter.y - 2.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))
        drawCircle(color = Color(0xFF1E293B), radius = 5.dp.toPx(), center = Offset(headCenter.x + 6.dp.toPx(), headCenter.y - 2.dp.toPx()), style = Stroke(width = 1.5.dp.toPx()))

        // Book Stack on top of head (Image 1 Style)
        drawRoundRect(
            color = Color(0xFFF59E0B),
            topLeft = Offset(w * 0.32f, h * 0.22f),
            size = Size(w * 0.36f, h * 0.10f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF3B82F6),
            topLeft = Offset(w * 0.28f, h * 0.12f),
            size = Size(w * 0.44f, h * 0.10f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

/**
 * 7. HEADER VECTOR BACKDROP
 * Clean no-op so all headers across every app role stay crisp with zero decorative circles/bubbles.
 */
@Composable
fun HeaderVectorBackdrop(
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    // Intentionally empty for a clean, distraction-free UI across all roles.
}

/**
 * 8. CARD CORNER VECTOR ACCENT
 * Clean no-op so all cards across every app role stay crisp with zero decorative circles/bubbles.
 */
@Composable
fun CardCornerVectorAccent(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF2563EB)
) {
    // Intentionally empty for a clean, distraction-free UI across all roles.
}

enum class EmptyStateIllustrationType {
    DOUBTS, NOTES, NOTICES, ACHIEVEMENTS, HOMEWORK
}

/**
 * 9. ILLUSTRATED EMPTY STATE CARD
 * Uses the user's real illustrations from /assets (asset_doubts, asset_notes, asset_myclass,
 * asset_homeworks, asset_results) cleanly framed with zero crude stick-figure drawings.
 */
@Composable
fun EmptyStateIllustrationCard(
    title: String,
    subtitle: String,
    illustrationType: EmptyStateIllustrationType,
    drawableRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val brand = LocalSchoolBrand.current
    val resolvedRes = drawableRes ?: when (illustrationType) {
        EmptyStateIllustrationType.DOUBTS -> R.drawable.asset_doubts
        EmptyStateIllustrationType.NOTES -> R.drawable.asset_notes
        EmptyStateIllustrationType.NOTICES -> R.drawable.asset_myclass
        EmptyStateIllustrationType.ACHIEVEMENTS -> R.drawable.asset_results
        EmptyStateIllustrationType.HOMEWORK -> R.drawable.asset_homeworks
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, GenesisColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(resolvedRes),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GenesisColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = GenesisColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = brand.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = actionLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
