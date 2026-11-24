package com.schoolapp.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.schoolapp.android.data.DiscussionRepository
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fullscreen App Splash & Loading Screen with cool animated vector learning illustrations,
 * glowing orbital rings, floating academic symbols, and sleek progress indicators.
 */
@Composable
fun AppSplashScreen(
    onLoadingComplete: () -> Unit
) {
    val activeSchool by DiscussionRepository.activeSchool.collectAsState()
    val brandPrimary = remember(activeSchool.primaryColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(activeSchool.primaryColorHex)) }
            .getOrDefault(Color(0xFF1E40AF))
    }
    val brandAccent = remember(activeSchool.accentColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(activeSchool.accentColorHex)) }
            .getOrDefault(Color(0xFF38BDF8))
    }

    val progressAnim = remember { Animatable(0f) }
    var statusText by remember(activeSchool.name) { mutableStateOf("Initializing ${activeSchool.name}...") }

    // Infinite transitions for ambient floating & rotating effects
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")
    
    // Continuous slow orbit rotation
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRot"
    )

    // Pulsing halo scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Floating vertical oscillation for elements
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatOffset"
    )

    // Drive progress and status updates
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 0.35f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        statusText = "Syncing Student Classrooms & Timetables..."

        progressAnim.animateTo(
            targetValue = 0.75f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        statusText = "Loading 3D Student Hub & AI Tutors..."

        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        statusText = "Welcome to ${activeSchool.name}!"
        delay(250)
        onLoadingComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1120),
                        brandPrimary.copy(alpha = 0.88f),
                        brandPrimary,
                        Color(0xFF0B1120)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Background Glow Particle Canvas
        AmbientBackgroundCanvas(orbitRotation = orbitRotation, pulseScale = pulseScale)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 2. Dynamic School Digital Campus Header Pill
            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, brandAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "School Logo",
                        tint = brandAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activeSchool.schoolCode.uppercase()} SMART CAMPUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Central Hero Animated School Emblem / Learning Universe
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset(y = floatOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background radial glow tinted dynamically to the school's accent color
                Box(
                    modifier = Modifier
                        .size(185.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    brandAccent.copy(alpha = 0.42f),
                                    brandPrimary.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Animated Vector Graphic: Orbiting Atoms, Academic Shield & Graduation Cap
                VectorLearningHubCanvas(
                    orbitRotation = orbitRotation,
                    pulseScale = pulseScale,
                    progress = progressAnim.value
                )

                // If the school uploaded a custom logo in DB/GCS, display it cleanly centered in a glowing crest
                if (activeSchool.logoUrl.isNotBlank()) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(3.dp, brandAccent),
                        modifier = Modifier.size(92.dp)
                    ) {
                        AsyncImage(
                            model = DiscussionRepository.resolveRenderableMediaUrl(activeSchool.logoUrl),
                            contentDescription = activeSchool.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Dynamic School Title & Tagline (zero Play Store update needed for new schools)
            Text(
                text = activeSchool.name.uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = activeSchool.tagline,
                fontSize = 13.sp,
                color = brandAccent.copy(alpha = 0.95f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 5. Sleek School-Branded Shimmer Loading Bar
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnim.value)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        brandAccent,
                                        Color.White.copy(alpha = 0.9f),
                                        brandAccent
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Percentage & Dynamic Micro-Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "${(progressAnim.value * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color(0xFF67E8F9),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * High-performance vector Canvas drawing:
 * 1. Double rotating planetary/atomic orbits with glowing electron beads
 * 2. Golden celebration trophy & starbursts
 * 3. 3D-styled graduation cap with tassel
 * 4. Open glowing knowledge book with vector page lines
 */
@Composable
private fun VectorLearningHubCanvas(
    orbitRotation: Float,
    pulseScale: Float,
    progress: Float,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // 1. Outer Orbit Ring 1 (tilted 30 deg)
        rotate(degrees = orbitRotation, pivot = center) {
            drawOval(
                color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                topLeft = Offset(w * 0.08f, h * 0.28f),
                size = Size(w * 0.84f, h * 0.44f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )
            // Orbiting Cyan Satellite / Electron Bead
            drawCircle(
                color = Color(0xFF06B6D4),
                radius = 5.dp.toPx(),
                center = Offset(w * 0.08f, h * 0.50f)
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(w * 0.08f, h * 0.50f)
            )
        }

        // 2. Outer Orbit Ring 2 (tilted -45 deg, counter-rotating)
        rotate(degrees = -orbitRotation * 1.3f, pivot = center) {
            drawOval(
                color = Color(0xFFA855F7).copy(alpha = 0.45f),
                topLeft = Offset(w * 0.12f, h * 0.22f),
                size = Size(w * 0.76f, h * 0.56f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Orbiting Purple Sparkle Bead
            drawCircle(
                color = Color(0xFFC084FC),
                radius = 4.5.dp.toPx(),
                center = Offset(w * 0.88f, h * 0.50f)
            )
        }

        // 3. Central Shield / Rounded Hexagon with Vibrant Blue Gradient
        val shieldPath = Path().apply {
            moveTo(center.x, h * 0.22f)
            lineTo(w * 0.78f, h * 0.34f)
            lineTo(w * 0.74f, h * 0.65f)
            quadraticTo(center.x, h * 0.85f, center.x, h * 0.85f)
            quadraticTo(w * 0.26f, h * 0.65f, w * 0.26f, h * 0.65f)
            lineTo(w * 0.22f, h * 0.34f)
            close()
        }
        drawPath(
            path = shieldPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2563EB), Color(0xFF1E40AF), Color(0xFF1E1B4B))
            )
        )
        drawPath(
            path = shieldPath,
            color = Color(0xFF60A5FA),
            style = Stroke(width = 2.dp.toPx())
        )

        // 4. Open Knowledge Book inside Shield
        val bookCenterY = center.y + 6.dp.toPx()
        val bookW = w * 0.38f
        val bookH = h * 0.22f
        val leftPage = Path().apply {
            moveTo(center.x, bookCenterY)
            quadraticTo(center.x - bookW * 0.5f, bookCenterY - 6.dp.toPx(), center.x - bookW, bookCenterY - 2.dp.toPx())
            lineTo(center.x - bookW, bookCenterY + bookH)
            quadraticTo(center.x - bookW * 0.5f, bookCenterY + bookH - 4.dp.toPx(), center.x, bookCenterY + bookH + 4.dp.toPx())
            close()
        }
        drawPath(leftPage, color = Color.White)
        drawPath(leftPage, color = Color(0xFF93C5FD), style = Stroke(width = 1.5.dp.toPx()))

        val rightPage = Path().apply {
            moveTo(center.x, bookCenterY)
            quadraticTo(center.x + bookW * 0.5f, bookCenterY - 6.dp.toPx(), center.x + bookW, bookCenterY - 2.dp.toPx())
            lineTo(center.x + bookW, bookCenterY + bookH)
            quadraticTo(center.x + bookW * 0.5f, bookCenterY + bookH - 4.dp.toPx(), center.x, bookCenterY + bookH + 4.dp.toPx())
            close()
        }
        drawPath(rightPage, color = Color(0xFFF1F5F9))
        drawPath(rightPage, color = Color(0xFF93C5FD), style = Stroke(width = 1.5.dp.toPx()))

        // Book Page Spine & Text Line Accents
        drawLine(
            color = Color(0xFF1E40AF),
            start = Offset(center.x, bookCenterY),
            end = Offset(center.x, bookCenterY + bookH + 4.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
        // Left text lines
        drawLine(color = Color(0xFF94A3B8), start = Offset(center.x - bookW * 0.75f, bookCenterY + 12.dp.toPx()), end = Offset(center.x - bookW * 0.25f, bookCenterY + 10.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        drawLine(color = Color(0xFF94A3B8), start = Offset(center.x - bookW * 0.75f, bookCenterY + 20.dp.toPx()), end = Offset(center.x - bookW * 0.25f, bookCenterY + 18.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        // Right text lines
        drawLine(color = Color(0xFF94A3B8), start = Offset(center.x + bookW * 0.25f, bookCenterY + 10.dp.toPx()), end = Offset(center.x + bookW * 0.75f, bookCenterY + 12.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        drawLine(color = Color(0xFF94A3B8), start = Offset(center.x + bookW * 0.25f, bookCenterY + 18.dp.toPx()), end = Offset(center.x + bookW * 0.75f, bookCenterY + 20.dp.toPx()), strokeWidth = 1.5.dp.toPx())

        // 5. 3D Floating Graduation Cap perched above Book
        val capY = h * 0.28f
        val capDiamond = Path().apply {
            moveTo(center.x, capY - 14.dp.toPx())
            lineTo(center.x + w * 0.22f, capY)
            lineTo(center.x, capY + 14.dp.toPx())
            lineTo(center.x - w * 0.22f, capY)
            close()
        }
        drawPath(capDiamond, color = Color(0xFF0F172A))
        drawPath(capDiamond, color = Color(0xFF38BDF8), style = Stroke(width = 1.5.dp.toPx()))

        // Cap Skull Cap Underneath
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - w * 0.12f, capY + 6.dp.toPx()),
            size = Size(w * 0.24f, h * 0.08f)
        )

        // Golden Tassel swinging slightly
        val tasselAngle = sin(orbitRotation * 0.05f) * 6f
        val tasselStart = Offset(center.x, capY)
        val tasselEnd = Offset(center.x + w * 0.18f + tasselAngle, capY + 18.dp.toPx())
        drawLine(color = Color(0xFFF59E0B), start = tasselStart, end = tasselEnd, strokeWidth = 2.dp.toPx())
        drawCircle(color = Color(0xFFF59E0B), radius = 3.dp.toPx(), center = tasselEnd)

        // 6. Floating Starbursts around Hub
        drawFourPointStar(center = Offset(w * 0.16f, h * 0.18f), radius = 6.dp.toPx() * pulseScale, color = Color(0xFFFBBF24))
        drawFourPointStar(center = Offset(w * 0.84f, h * 0.20f), radius = 7.dp.toPx() * pulseScale, color = Color(0xFF38BDF8))
        drawFourPointStar(center = Offset(w * 0.20f, h * 0.75f), radius = 5.dp.toPx() * pulseScale, color = Color(0xFF34D399))
    }
}

/**
 * Draws ambient floating dust particles / stars
 */
@Composable
private fun AmbientBackgroundCanvas(orbitRotation: Float, pulseScale: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val particles = listOf(
            Triple(0.12f, 0.15f, 2.5.dp.toPx()),
            Triple(0.85f, 0.12f, 3.dp.toPx()),
            Triple(0.25f, 0.35f, 1.8.dp.toPx()),
            Triple(0.78f, 0.40f, 2.2.dp.toPx()),
            Triple(0.15f, 0.65f, 3.2.dp.toPx()),
            Triple(0.88f, 0.70f, 2.8.dp.toPx()),
            Triple(0.30f, 0.88f, 2.0.dp.toPx()),
            Triple(0.70f, 0.90f, 2.6.dp.toPx())
        )

        particles.forEachIndexed { i, (px, py, r) ->
            val dynamicOffset = sin(orbitRotation * 0.02f + i) * 6.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.25f + (sin(orbitRotation * 0.05f + i) * 0.15f)),
                radius = r * pulseScale,
                center = Offset(w * px, h * py + dynamicOffset)
            )
        }
    }
}

/**
 * Utility function to draw clean 4-point vector sparkles / stars
 */
private fun DrawScope.drawFourPointStar(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color = color)
}

/**
 * Reusable modal loading overlay with the vector learning animations
 * for in-app async tasks (e.g., student verification, grade card generation, exam sync).
 */
@Composable
fun AppVectorLoadingOverlay(
    message: String = "Processing...",
    onDismissRequest: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OverlayAnim")
    val rot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rot"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VectorLearningHubCanvas(
                        orbitRotation = rot,
                        pulseScale = 1f,
                        progress = 0.7f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
