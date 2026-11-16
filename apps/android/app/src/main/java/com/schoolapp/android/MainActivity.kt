package com.schoolapp.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DynamicForm
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolapp.android.data.AppRole
import com.schoolapp.android.data.DiscussionRepository
import com.schoolapp.android.ui.screens.AppSplashScreen
import com.schoolapp.android.ui.screens.DoubtsScreen
import com.schoolapp.android.ui.screens.FormsScreen
import com.schoolapp.android.ui.screens.HomeScreen
import com.schoolapp.android.ui.screens.MyClassScreen
import com.schoolapp.android.ui.screens.StudentFirstTimeLoginScreen
import com.schoolapp.android.ui.screens.TeacherMarksAutomationScreen
import com.schoolapp.android.ui.theme.GenesisColors
import com.schoolapp.android.ui.theme.SchoolAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.parseColor("#1E40AF")
            )
        )
        setContent {
            SchoolAppTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val isLoggedIn by DiscussionRepository.isStudentLoggedIn.collectAsState()
                var isAppLoading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    DiscussionRepository.initSession(context, coroutineScope)
                }

                if (isAppLoading) {
                    AppSplashScreen(
                        onLoadingComplete = { isAppLoading = false }
                    )
                } else if (!isLoggedIn) {
                    StudentFirstTimeLoginScreen(
                        onLoginConfirmed = { fullName, className, section, parentPhone, parentEmail, address ->
                            DiscussionRepository.completeInitialStudentOnboarding(
                                context = context,
                                fullName = fullName,
                                className = className,
                                section = section,
                                parentPhone = parentPhone,
                                parentEmail = parentEmail,
                                address = address,
                                scope = coroutineScope
                            )
                        }
                    )
                } else {
                    MainScreen(
                        onOpenChatGPT = { prompt, imageUri -> openChatGPT(prompt, imageUri) }
                    )
                }
            }
        }
    }

    private fun openChatGPT(prompt: String? = null, imageUri: Uri? = null) {
        if (imageUri != null) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    if (!prompt.isNullOrBlank()) {
                        putExtra(Intent.EXTRA_TEXT, prompt)
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage("com.openai.chatgpt")
                }
                startActivity(shareIntent)
                return
            } catch (_: Exception) {
                try {
                    val chooser = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, imageUri)
                            if (!prompt.isNullOrBlank()) {
                                putExtra(Intent.EXTRA_TEXT, prompt)
                            }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Send Doubt Photo to AI / ChatGPT"
                    )
                    startActivity(chooser)
                    return
                } catch (_: Exception) {
                    // Fall through to browser
                }
            }
        }

        val url = if (prompt.isNullOrBlank()) {
            "https://chatgpt.com/"
        } else {
            "https://chatgpt.com/?q=" + java.net.URLEncoder.encode(prompt, "UTF-8")
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}

@Composable
fun MainScreen(onOpenChatGPT: (String?, Uri?) -> Unit) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var tabResetSignal by remember { mutableIntStateOf(0) }
    val currentRole by DiscussionRepository.currentRole.collectAsState()
    val isTeacherRole = currentRole == AppRole.TEACHER

    val items = if (isTeacherRole) {
        listOf("Home", "My Class", "Marks", "Staff & HR")
    } else {
        listOf("Home", "My Class", "Doubts", "Forms")
    }
    val icons = if (isTeacherRole) {
        listOf(Icons.Default.Home, Icons.Default.School, Icons.Default.AutoAwesome, Icons.Default.Badge)
    } else {
        listOf(Icons.Default.Home, Icons.Default.School, Icons.Default.AutoAwesome, Icons.Default.DynamicForm)
    }

    // Pressing Back on My Class, Doubts, or Forms brings the user to Home instead of exiting the app
    BackHandler(enabled = selectedItem != 0) {
        selectedItem = 0
        tabResetSignal++
    }

    Scaffold(
        // MeteorBlack container paints the top system status-bar area seamlessly in Royal School Blue
        containerColor = GenesisColors.MeteorBlack,
        bottomBar = {
            Column {
                HorizontalDivider(color = GenesisColors.Border, thickness = 1.dp)
                NavigationBar(
                    containerColor = GenesisColors.Surface,
                    tonalElevation = 0.dp
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(icons[index], contentDescription = item) },
                            label = {
                                Text(
                                    text = item,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedItem == index) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            selected = selectedItem == index,
                            onClick = {
                                selectedItem = index
                                tabResetSignal++
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GenesisColors.Primary,
                                selectedTextColor = GenesisColors.Primary,
                                indicatorColor = GenesisColors.Primary.copy(alpha = 0.12f),
                                unselectedIconColor = GenesisColors.TextSecondary,
                                unselectedTextColor = GenesisColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GenesisColors.Background),
            contentAlignment = Alignment.Center
        ) {
            when (selectedItem) {
                0 -> HomeScreen(
                    onNavigateToTab = {
                        selectedItem = it
                        tabResetSignal++
                    },
                    onOpenChatGPT = { prompt, imageUri -> onOpenChatGPT(prompt, imageUri) },
                    resetSignal = tabResetSignal
                )
                1 -> MyClassScreen(
                    onOpenChatGPT = { prompt, imageUri -> onOpenChatGPT(prompt, imageUri) },
                    onNavigateHome = {
                        selectedItem = 0
                        tabResetSignal++
                    },
                    resetSignal = tabResetSignal
                )
                2 -> if (isTeacherRole) {
                    TeacherMarksAutomationScreen(
                        onBack = {
                            selectedItem = 0
                            tabResetSignal++
                        },
                        showBackButton = false
                    )
                } else {
                    DoubtsScreen(
                        onOpenChatGPT = { prompt, imageUri -> onOpenChatGPT(prompt, imageUri) },
                        onNavigateHome = {
                            selectedItem = 0
                            tabResetSignal++
                        }
                    )
                }
                3 -> FormsScreen(
                    onNavigateHome = {
                        selectedItem = 0
                        tabResetSignal++
                    },
                    resetSignal = tabResetSignal
                )
            }
        }
    }
}
