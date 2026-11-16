package com.schoolapp.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.schoolapp.android.ui.theme.GenesisColors
import java.text.SimpleDateFormat
import java.util.*

data class AiHomeworkMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val imageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
    val conceptTitle: String = "",
    val coreConcept: String = "",
    val guidedHint: String = "",
    val guidingQuestion: String = "",
    val chatGptPrompt: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHomeworkBuddyScreen(
    onOpenChatGPT: (String, Uri?) -> Unit,
    onNavigateHome: () -> Unit
) {
    BackHandler { onNavigateHome() }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    var inputTask by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var showGuidedModesMenu by remember { mutableStateOf(false) }
    var showAttachDialog by remember { mutableStateOf(false) }
    var guidedMode by remember { mutableStateOf("Guided Hints (No Direct Answers)") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = copyUriToCacheFile(context, uri) ?: uri
            capturedBitmap = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = null
        }
    }

    // Default conversation starter matching the user's reference UI
    val messages = remember {
        mutableStateListOf(
            AiHomeworkMessage(
                isUser = true,
                text = "And what else can be found that is just as interesting but on the topic of education?",
                timestamp = "21:36",
                imageUri = null
            ),
            AiHomeworkMessage(
                isUser = false,
                text = "Here is a guided breakdown of educational concepts you can explore without jumping straight to conclusions:",
                timestamp = "21:41",
                conceptTitle = "Core Socratic Concept: Active vs. Passive Learning",
                coreConcept = "True education happens when students construct mental models through guided questioning rather than memorizing readymade solutions.",
                guidedHint = "Think about how you remember things best: by being told an answer, or by discovering the missing step yourself?",
                guidingQuestion = "What specific topic in your syllabus (e.g. Physics Forces, Algebra, or Chemistry Reactions) would you like guided hints on?",
                chatGptPrompt = "Act as an expert CBSE Socratic Tutor. Do NOT give direct answers. Guide me with conceptual explanations and hints."
            )
        )
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Background gradient: Very light soft pastel blue
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF1F6FB),
                        Color(0xFFE8F0F8),
                        Color(0xFFE2EBF5)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar: Circular Back Button, "AI Assistant" Title, Circular List Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Back Button
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onNavigateHome() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Screen Title
                Text(
                    text = "AI Assistant",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                // Circular Menu / Guided Mode Button
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { showGuidedModesMenu = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Guided Learning Modes",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Chat & Owl Illustration Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item 0: Owl Illustration Hero
                item {
                    AiOwlHeroCard(
                        isThinking = isThinking,
                        guidedMode = guidedMode,
                        onModeClick = { showGuidedModesMenu = true }
                    )
                }

                // Chat Messages
                items(messages) { msg ->
                    if (msg.isUser) {
                        UserMessageBubble(message = msg)
                    } else {
                        AiGuidedResponseBubble(
                            message = msg,
                            onOpenChatGPT = { prompt, uri -> onOpenChatGPT(prompt, uri) }
                        )
                    }
                }

                // Thinking indicator if active
                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = GenesisColors.Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Owl is preparing your guided hints...",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Bottom Section: "Powered by GPT-5" + Rounded Input Pill Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                // "Powered by GPT-5" branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "OpenAI",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Powered by GPT-5",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }

                // White Rounded Input Container
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Image Preview Pill if attached
                        if (selectedImageUri != null || capturedBitmap != null) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    if (capturedBitmap != null) {
                                        Image(
                                            bitmap = capturedBitmap!!.asImageBitmap(),
                                            contentDescription = "Photo",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (selectedImageUri != null) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Photo",
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Problem Photo Attached",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GenesisColors.Success
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                selectedImageUri = null
                                                capturedBitmap = null
                                            }
                                    )
                                }
                            }
                        }

                        // Text Input Field
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (inputTask.isEmpty()) {
                                Text(
                                    text = "Describe your task..",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = inputTask,
                                onValueChange = { inputTask = it },
                                textStyle = TextStyle(
                                    color = Color(0xFF1E293B),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 28.dp, max = 100.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Actions Row: "@ Attach" on Left, Circular Black Send Button on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // "@ Attach" Pill Button
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                                modifier = Modifier.clickable { showAttachDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AlternateEmail,
                                        contentDescription = "Attach",
                                        tint = GenesisColors.Primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Attach",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            // Black Circular Send Button with White Up-Arrow
                            Surface(
                                color = Color.Black,
                                shape = CircleShape,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable {
                                        if (inputTask.isNotBlank() || selectedImageUri != null || capturedBitmap != null) {
                                            val currentTask = inputTask.trim()
                                            val currentUri = selectedImageUri
                                            val currentBmp = capturedBitmap

                                            val userMsg = AiHomeworkMessage(
                                                isUser = true,
                                                text = currentTask.ifBlank { "Guided explanation for the attached textbook problem" },
                                                imageUri = currentUri,
                                                bitmap = currentBmp
                                            )
                                            messages.add(userMsg)

                                            // Reset inputs
                                            inputTask = ""
                                            selectedImageUri = null
                                            capturedBitmap = null
                                            isThinking = true

                                            // Simulate Socratic hint generation
                                            val prompt = buildSocraticPrompt(currentTask, currentUri != null || currentBmp != null)
                                            val guidedResponse = generateGuidedHints(currentTask)

                                            messages.add(
                                                AiHomeworkMessage(
                                                    isUser = false,
                                                    text = guidedResponse.text,
                                                    conceptTitle = guidedResponse.conceptTitle,
                                                    coreConcept = guidedResponse.coreConcept,
                                                    guidedHint = guidedResponse.guidedHint,
                                                    guidingQuestion = guidedResponse.guidingQuestion,
                                                    chatGptPrompt = prompt,
                                                    imageUri = currentUri,
                                                    bitmap = currentBmp
                                                )
                                            )
                                            isThinking = false
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Attachment Source Selector Dialog
    if (showAttachDialog) {
        AlertDialog(
            onDismissRequest = { showAttachDialog = false },
            title = {
                Text("Attach Homework Problem", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "Take a photo of your textbook question or select an image from your device gallery for guided hint analysis.",
                    fontSize = 13.sp,
                    color = GenesisColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAttachDialog = false
                        cameraLauncher.launch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GenesisColors.Primary)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAttachDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery")
                }
            }
        )
    }

    // Guided Modes Bottom Sheet / Dialog
    if (showGuidedModesMenu) {
        AlertDialog(
            onDismissRequest = { showGuidedModesMenu = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = GenesisColors.Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guided Tutor Modes", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Our AI Homework Buddy uses Socratic coaching to help you understand the concept instead of spoon-feeding direct answers.",
                        fontSize = 12.sp,
                        color = GenesisColors.TextSecondary
                    )
                    Divider(color = GenesisColors.Border)
                    listOf(
                        "Guided Hints (No Direct Answers)" to "Breaks the question down and provides Step 1 hint.",
                        "Core Concept & Formula" to "Explains the underlying physics/math principles.",
                        "Step-by-Step Logic Checker" to "Verifies where you might have made an error.",
                        "CBSE Board Marking Scheme" to "Shows how marks are awarded for each step."
                    ).forEach { (title, subtitle) ->
                        Surface(
                            color = if (guidedMode == title) Color(0xFFEFF6FF) else Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (guidedMode == title) GenesisColors.Primary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    guidedMode = title
                                    showGuidedModesMenu = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (guidedMode == title) GenesisColors.Primary else Color(0xFF1E293B))
                                Text(subtitle, fontSize = 11.sp, color = GenesisColors.TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuidedModesMenu = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------
// Wise Owl Illustration Card (Matching User Screenshot Hero)
// ----------------------------------------------------------------------------
@Composable
private fun AiOwlHeroCard(
    isThinking: Boolean,
    guidedMode: String,
    onModeClick: () -> Unit
) {
    // Subtle pulsating animation for "AI Owl is Thinking.."
    val infiniteTransition = rememberInfiniteTransition(label = "owl_thinking")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Owl Illustration Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Vector Canvas drawing the Wise Owl with Graduation Cap, Book & Math Doodles
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(170.dp)
                ) {
                    drawOwlArtwork()
                }

                // Floating Edit / Mode Button on the top right
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 24.dp)
                        .size(38.dp)
                        .clickable { onModeClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Mode",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle: "AI Owl is Thinking.."
            Text(
                text = if (isThinking) "AI Owl is Thinking.." else "AI Owl Guided Tutor",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155).copy(alpha = if (isThinking) dotAlpha else 1.0f),
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.2.sp
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Canvas Vector Drawing of the Wise Owl with Mortarboard, Glasses & Open Book
// ----------------------------------------------------------------------------
private fun DrawScope.drawOwlArtwork() {
    val cx = size.width / 2f
    val cy = size.height / 2f + 8f

    // 1. Decorative Doodles
    // Left: "E=mc²" in orange
    // Right: "+" in Cyan
    drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
        radius = 28.dp.toPx(),
        center = Offset(cx - 100.dp.toPx(), cy - 10.dp.toPx())
    )

    // Plus sign on top right
    val plusX = cx + 80.dp.toPx()
    val plusY = cy - 40.dp.toPx()
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(plusX - 7.dp.toPx(), plusY),
        end = Offset(plusX + 7.dp.toPx(), plusY),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(plusX, plusY - 7.dp.toPx()),
        end = Offset(plusX, plusY + 7.dp.toPx()),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Little coral rectangle bookmark
    drawRoundRect(
        color = Color(0xFFF87171),
        topLeft = Offset(cx - 75.dp.toPx(), cy - 35.dp.toPx()),
        size = Size(14.dp.toPx(), 6.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
    )

    // 2. Owl Head & Body (Warm Amber Brown)
    val owlBrown = Color(0xFFB45309)
    val owlCream = Color(0xFFFEF3C7)
    val owlSize = 48.dp.toPx()

    // Owl Body
    drawOval(
        color = owlBrown,
        topLeft = Offset(cx - owlSize, cy - owlSize * 1.05f),
        size = Size(owlSize * 2, owlSize * 1.9f)
    )

    // Owl Cream Belly
    drawOval(
        color = owlCream,
        topLeft = Offset(cx - owlSize * 0.65f, cy - owlSize * 0.3f),
        size = Size(owlSize * 1.3f, owlSize * 1.1f)
    )

    // Owl Wings (Left & Right)
    drawOval(
        color = Color(0xFF92400E),
        topLeft = Offset(cx - owlSize * 1.15f, cy - owlSize * 0.2f),
        size = Size(owlSize * 0.6f, owlSize * 0.8f)
    )
    drawOval(
        color = Color(0xFF92400E),
        topLeft = Offset(cx + owlSize * 0.55f, cy - owlSize * 0.2f),
        size = Size(owlSize * 0.6f, owlSize * 0.8f)
    )

    // 3. Owl Spectacles / Round Glasses
    val glassesRadius = 14.dp.toPx()
    val glassesY = cy - owlSize * 0.45f
    val glassesLeftX = cx - 14.dp.toPx()
    val glassesRightX = cx + 14.dp.toPx()

    // Eye White Backing
    drawCircle(color = Color.White, radius = glassesRadius, center = Offset(glassesLeftX, glassesY))
    drawCircle(color = Color.White, radius = glassesRadius, center = Offset(glassesRightX, glassesY))

    // Black Spectacle Rims
    drawCircle(
        color = Color(0xFF1E293B),
        radius = glassesRadius,
        center = Offset(glassesLeftX, glassesY),
        style = Stroke(width = 2.5.dp.toPx())
    )
    drawCircle(
        color = Color(0xFF1E293B),
        radius = glassesRadius,
        center = Offset(glassesRightX, glassesY),
        style = Stroke(width = 2.5.dp.toPx())
    )
    // Bridge between glasses
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(glassesLeftX + glassesRadius - 1.dp.toPx(), glassesY),
        end = Offset(glassesRightX - glassesRadius + 1.dp.toPx(), glassesY),
        strokeWidth = 2.5.dp.toPx()
    )

    // Smiling Closed Eyes (Happy Arcs)
    val eyeArcPathLeft = Path().apply {
        moveTo(glassesLeftX - 7.dp.toPx(), glassesY + 1.dp.toPx())
        quadraticBezierTo(glassesLeftX, glassesY - 5.dp.toPx(), glassesLeftX + 7.dp.toPx(), glassesY + 1.dp.toPx())
    }
    drawPath(eyeArcPathLeft, color = Color(0xFF1E293B), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

    val eyeArcPathRight = Path().apply {
        moveTo(glassesRightX - 7.dp.toPx(), glassesY + 1.dp.toPx())
        quadraticBezierTo(glassesRightX, glassesY - 5.dp.toPx(), glassesRightX + 7.dp.toPx(), glassesY + 1.dp.toPx())
    }
    drawPath(eyeArcPathRight, color = Color(0xFF1E293B), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

    // Cute Orange Beak
    val beakPath = Path().apply {
        moveTo(cx - 5.dp.toPx(), glassesY + 8.dp.toPx())
        lineTo(cx + 5.dp.toPx(), glassesY + 8.dp.toPx())
        lineTo(cx, glassesY + 16.dp.toPx())
        close()
    }
    drawPath(beakPath, color = Color(0xFFF59E0B))

    // 4. Mortarboard / Graduation Cap
    val capY = cy - owlSize * 1.05f
    val capPath = Path().apply {
        moveTo(cx, capY - 18.dp.toPx()) // Top vertex
        lineTo(cx + 42.dp.toPx(), capY - 6.dp.toPx()) // Right vertex
        lineTo(cx, capY + 6.dp.toPx()) // Bottom vertex
        lineTo(cx - 42.dp.toPx(), capY - 6.dp.toPx()) // Left vertex
        close()
    }
    drawPath(capPath, color = Color(0xFF0F172A))

    // Cap Skullband underneath
    val skullband = Path().apply {
        moveTo(cx - 18.dp.toPx(), capY - 2.dp.toPx())
        lineTo(cx + 18.dp.toPx(), capY - 2.dp.toPx())
        lineTo(cx + 14.dp.toPx(), capY + 10.dp.toPx())
        lineTo(cx - 14.dp.toPx(), capY + 10.dp.toPx())
        close()
    }
    drawPath(skullband, color = Color(0xFF1E293B))

    // Gold Button & Tassel
    drawCircle(color = Color(0xFFF59E0B), radius = 3.dp.toPx(), center = Offset(cx, capY - 6.dp.toPx()))
    val tasselPath = Path().apply {
        moveTo(cx, capY - 6.dp.toPx())
        quadraticBezierTo(cx + 28.dp.toPx(), capY - 2.dp.toPx(), cx + 36.dp.toPx(), capY + 18.dp.toPx())
    }
    drawPath(tasselPath, color = Color(0xFFF59E0B), style = Stroke(width = 2.dp.toPx()))
    drawCircle(color = Color(0xFFF59E0B), radius = 3.5.dp.toPx(), center = Offset(cx + 36.dp.toPx(), capY + 18.dp.toPx()))

    // 5. Open Book in Front of Owl
    val bookY = cy + owlSize * 0.45f
    val bookWidth = 72.dp.toPx()
    val bookHeight = 28.dp.toPx()

    // Left Page
    val leftPage = Path().apply {
        moveTo(cx, bookY + bookHeight)
        lineTo(cx - bookWidth, bookY + bookHeight * 0.85f)
        lineTo(cx - bookWidth * 0.95f, bookY)
        lineTo(cx, bookY + 4.dp.toPx())
        close()
    }
    drawPath(leftPage, color = Color(0xFFFFFBEB))
    drawPath(leftPage, color = Color(0xFFD97706), style = Stroke(width = 1.dp.toPx()))

    // Right Page
    val rightPage = Path().apply {
        moveTo(cx, bookY + bookHeight)
        lineTo(cx + bookWidth, bookY + bookHeight * 0.85f)
        lineTo(cx + bookWidth * 0.95f, bookY)
        lineTo(cx, bookY + 4.dp.toPx())
        close()
    }
    drawPath(rightPage, color = Color(0xFFFEF9C3))
    drawPath(rightPage, color = Color(0xFFD97706), style = Stroke(width = 1.dp.toPx()))

    // Colored bookmark tabs popping out of book
    drawRoundRect(
        color = Color(0xFF10B981), // Green bookmark
        topLeft = Offset(cx - 30.dp.toPx(), bookY + bookHeight * 0.65f),
        size = Size(8.dp.toPx(), 12.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFFEF4444), // Red bookmark
        topLeft = Offset(cx + 22.dp.toPx(), bookY + bookHeight * 0.7f),
        size = Size(8.dp.toPx(), 14.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
    )
}

// ----------------------------------------------------------------------------
// User Message Bubble (Matching Screenshot White Rounded Card + Attachment)
// ----------------------------------------------------------------------------
@Composable
private fun UserMessageBubble(message: AiHomeworkMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // Attachment Thumbnail (if attached)
        if (message.bitmap != null || message.imageUri != null) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .padding(bottom = 6.dp)
            ) {
                if (message.bitmap != null) {
                    Image(
                        bitmap = message.bitmap.asImageBitmap(),
                        contentDescription = "Homework Problem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else if (message.imageUri != null) {
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "Homework Problem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }

        // Message Bubble
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp
            ),
            shadowElevation = 1.5.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                color = Color(0xFF1E293B),
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Timestamp
        Text(
            text = message.timestamp,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// AI Guided Response Bubble: Conceptual Breakdown & Hints (No Direct Answers!)
// ----------------------------------------------------------------------------
@Composable
private fun AiGuidedResponseBubble(
    message: AiHomeworkMessage,
    onOpenChatGPT: (String, Uri?) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                // Header Tag: Guided Socratic Coaching Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = GenesisColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guided Hints & Concept",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GenesisColors.Primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Intro text
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 1. Concept Title & Explanation
                if (message.coreConcept.isNotBlank()) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🧠 ${message.conceptTitle.ifBlank { "Core Concept" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.coreConcept,
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. Guided Hint (Step 1 without revealing answer)
                if (message.guidedHint.isNotBlank()) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🔍 Step 1 Guided Hint",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.guidedHint,
                                fontSize = 12.sp,
                                color = Color(0xFF047857),
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. Guiding Question to help student solve
                if (message.guidingQuestion.isNotBlank()) {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🎯 Try Answering This:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.guidingQuestion,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309),
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 4. Redirect to ChatGPT Button
                Button(
                    onClick = {
                        val promptToUse = message.chatGptPrompt.ifBlank {
                            "Act as a Socratic CBSE tutor. Guide me through this homework step-by-step with hints without giving the final answer directly: \"${message.text}\""
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ChatGPT Doubt Prompt", promptToUse))
                        Toast.makeText(context, "Opening ChatGPT with Guided Hints prompt...", Toast.LENGTH_SHORT).show()
                        onOpenChatGPT(promptToUse, message.imageUri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open in ChatGPT for Guided Q&A",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.NorthEast,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Timestamp
        Text(
            text = message.timestamp,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// Socratic Guided Hint Helper Engine
// ----------------------------------------------------------------------------
private data class GuidedEngineResult(
    val text: String,
    val conceptTitle: String,
    val coreConcept: String,
    val guidedHint: String,
    val guidingQuestion: String
)

private fun generateGuidedHints(query: String): GuidedEngineResult {
    val lower = query.lowercase()

    return when {
        lower.contains("quadratic") || lower.contains("x^2") || lower.contains("x²") || lower.contains("equation") -> {
            GuidedEngineResult(
                text = "Let's break down this quadratic equation together without rushing to the roots:",
                conceptTitle = "Standard Form & Nature of Roots (ax² + bx + c = 0)",
                coreConcept = "Every quadratic equation can be arranged into standard form. The discriminant D = b² - 4ac tells you whether the roots are real, equal, or distinct.",
                guidedHint = "First, bring all terms to one side so the equation equals zero. Write down what your a, b, and c coefficients are.",
                guidingQuestion = "What values do you get for a, b, and c? Calculate D = b² - 4ac to check if roots exist before factoring!"
            )
        }
        lower.contains("force") || lower.contains("newton") || lower.contains("gravity") || lower.contains("physics") || lower.contains("motion") -> {
            GuidedEngineResult(
                text = "Physics problems become very simple once you identify the given quantities and choose the right governing law:",
                conceptTitle = "Newton's Laws of Motion & Free Body Analysis",
                coreConcept = "Net Force is directly proportional to mass times acceleration (F_net = m·a). Always list your known variables (m, u, v, a, s, t) first.",
                guidedHint = "Draw a quick diagram of the object. Which forces are acting horizontally and which vertically?",
                guidingQuestion = "Are the forces balanced (constant velocity) or unbalanced (accelerating)? Which kinematic equation connects your knowns to the unknown?"
            )
        }
        lower.contains("triangle") || lower.contains("pythagoras") || lower.contains("trig") || lower.contains("sin") || lower.contains("angle") -> {
            GuidedEngineResult(
                text = "Geometry and Trigonometry depend entirely on identifying the right-angled triangle and relative sides:",
                conceptTitle = "Right-Angled Triangle Ratios (sin, cos, tan & Pythagoras)",
                coreConcept = "In any right-angled triangle, a² + b² = c². The trigonometric ratio sin(θ) = Opposite / Hypotenuse, cos(θ) = Adjacent / Hypotenuse.",
                guidedHint = "Label the hypotenuse first (opposite the 90° angle), then label the opposite and adjacent sides relative to your chosen angle θ.",
                guidingQuestion = "Which two sides are known or asked for? That determines whether you need sin, cos, or tan!"
            )
        }
        else -> {
            GuidedEngineResult(
                text = "Here is a conceptual roadmap to solve this question step-by-step:",
                conceptTitle = "Socratic Problem Deconstruction",
                coreConcept = "Break complex academic questions into 3 components: 1) What is given? 2) What principle connects them? 3) What is the final target?",
                guidedHint = "Underline the key terms and numbers in your question. Rephrase the problem in your own words in one sentence.",
                guidingQuestion = "What is the very first calculation or definition you need to establish before solving the rest?"
            )
        }
    }
}

private fun buildSocraticPrompt(userTask: String, hasImage: Boolean): String {
    val task = if (userTask.isNotBlank()) userTask else "Solve the attached textbook problem."
    return """
Act as an expert CBSE Socratic Tutor for Class 10.
The student has submitted this homework problem:
"$task"

STRICT INSTRUCTIONS:
1. DO NOT give direct answers or full solutions immediately.
2. First, explain what the question is really asking in plain, simple English.
3. State the core concept or formula in 1-2 lines.
4. Give a Step 1 Guided Hint to help the student get started.
5. Ask a guiding question so the student can figure out the next step themselves.
6. Provide 1 quick real-world example to make the concept click!
""".trimIndent()
}
