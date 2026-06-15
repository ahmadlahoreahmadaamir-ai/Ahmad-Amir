package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.RecognizerIntent
import android.content.Intent
import android.app.Activity
import java.util.Locale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AnatomyLandmark
import com.example.data.MedicalData
import com.example.data.QuizQuestion
import com.example.data.XraySample

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalAppScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val isApiKeyAvailable = viewModel.isApiKeyAvailable

    // Image assets in view state
    val selectedSample by viewModel.selectedSample.collectAsState()
    val customImageUri by viewModel.customImageUri.collectAsState()
    val customImageBitmap by viewModel.customImageBitmap.collectAsState()

    // Scans/AI States
    val scanUiState by viewModel.scanUiState.collectAsState()
    val scanHistory by viewModel.scanHistory.collectAsState()
    val conceptTutorState by viewModel.conceptTutorState.collectAsState()

    // Quizzes States
    val currentQuizIndex by viewModel.currentQuizIndex.collectAsState()
    val selectedAnswerIndex by viewModel.selectedAnswerIndex.collectAsState()
    val quizScore by viewModel.quizScore.collectAsState()
    val quizFinished by viewModel.quizFinished.collectAsState()

    // Gamification
    val studyStreak by viewModel.studyStreak.collectAsState()
    val scansToday by viewModel.scansCompletedToday.collectAsState()

    // Launcher for file attachment selection
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomImage(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = "Medical Logo Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MediMind AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "Radiology Anatomy AI Tutor",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak tracker",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${studyStreak}d",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = activeTab == ActiveTab.Analyze,
                    onClick = { viewModel.selectTab(ActiveTab.Analyze) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Tab") },
                    label = { Text("Scan Lab") }
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.Guide,
                    onClick = { viewModel.selectTab(ActiveTab.Guide) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Manual Tab") },
                    label = { Text("Atlas") }
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.Voice,
                    onClick = { viewModel.selectTab(ActiveTab.Voice) },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Voice Tab") },
                    label = { Text("Voice Tutor") }
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.Quiz,
                    onClick = { viewModel.selectTab(ActiveTab.Quiz) },
                    icon = { Icon(Icons.Default.FactCheck, contentDescription = "Quizzes Tab") },
                    label = { Text("Exams") }
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.History,
                    onClick = { viewModel.selectTab(ActiveTab.History) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Logs Tab") },
                    label = { Text("History") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views routing
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ActiveTabContent"
            ) { tab ->
                when (tab) {
                    ActiveTab.Analyze -> {
                        ScanAnalyzerView(
                            selectedSample = selectedSample,
                            customImageUri = customImageUri,
                            customImageBitmap = customImageBitmap,
                            scanUiState = scanUiState,
                            isApiKeyAvailable = isApiKeyAvailable,
                            scansToday = scansToday,
                            onSampleClick = { viewModel.selectSample(it) },
                            onUploadLaunch = { galleryLauncher.launch("image/*") },
                            onClearCustom = { viewModel.clearCustomImage() },
                            onRunAnalysis = { viewModel.runMedicalAnalysis(context) }
                        )
                    }
                    ActiveTab.Guide -> {
                        AnatomyGuideView(
                            onLandmarkExplain = { viewModel.askAITutoringOnConcept(it) }
                        )
                    }
                    ActiveTab.Voice -> {
                        VoiceTutorView(
                            viewModel = viewModel
                        )
                    }
                    ActiveTab.Quiz -> {
                        QuizView(
                            currentIndex = currentQuizIndex,
                            selectedIdx = selectedAnswerIndex,
                            score = quizScore,
                            finished = quizFinished,
                            onAnswerSelect = { viewModel.selectQuizAnswer(it) },
                            onProceed = { viewModel.proceedQuiz() },
                            onRestart = { viewModel.restartQuiz() }
                        )
                    }
                    ActiveTab.History -> {
                        HistoryView(
                            scans = scanHistory,
                            onTabSwitch = { viewModel.selectTab(ActiveTab.Analyze) }
                        )
                    }
                }
            }

            // Concept Tutor Overlay Popup dialog for details check-off
            if (conceptTutorState !is ConceptTutorState.Idle) {
                ConceptTutorPopup(
                    state = conceptTutorState,
                    onDismiss = { viewModel.dismissConceptTutor() }
                )
            }
        }
    }
}

// =================== SCAN TAB ===================
@Composable
fun ScanAnalyzerView(
    selectedSample: XraySample?,
    customImageUri: Uri?,
    customImageBitmap: Bitmap?,
    scanUiState: ScanUiState,
    isApiKeyAvailable: Boolean,
    scansToday: Int,
    onSampleClick: (XraySample) -> Unit,
    onUploadLaunch: () -> Unit,
    onClearCustom: () -> Unit,
    onRunAnalysis: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Welcome and goals tracker card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Revision Target",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan chest studies, review physiological landmarks and check pathology tags.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = "$scansToday/3",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "COMPLETED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Selection Control Headers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "1. Select Chest X-Ray Frame",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Tap a clinical case study or import your own image from storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Clinical Cases horizontal gallery
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MedicalData.sampleXrays) { sample ->
                    val isSelected = selectedSample?.id == sample.id
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .height(115.dp)
                            .clickable { onSampleClick(sample) },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sample.category.take(14) + "..",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = sample.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sample.description,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // External Selection buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Import file
                OutlinedButton(
                    onClick = onUploadLaunch,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload Study",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (customImageUri != null) "Change Upload" else "Import Study",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (customImageUri != null) {
                    OutlinedButton(
                        onClick = onClearCustom,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Clear upload",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Active selection display panel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. Active X-Ray Console",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // The Clinical Radiograph Image Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(
                            border = BorderStroke(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF334155),
                                        Color.Black
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        customImageBitmap != null -> {
                            Image(
                                bitmap = customImageBitmap.asImageBitmap(),
                                contentDescription = "User uploaded chest radiograph study",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        selectedSample != null -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedSample.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = selectedSample.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HideImage,
                                    contentDescription = "No image selected",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Ready to receive X-ray Frame",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Clinical telemetry labels overlaid on X-Ray, simulating real hospital displays
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Top Left metadata
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CASE ID: ${selectedSample?.id?.uppercase() ?: "CUSTOM_FILM"}",
                                color = Color(0xFF2DD4BF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "VIEW: POSTEROANTERIOR",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Top Right scale indicator
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "U: Upright upright",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "VOLT: 120 kVp",
                                color = Color.LightGray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Bottom Center scan lines animation overlay if loading
                        if (scanUiState is ScanUiState.Loading) {
                            val infiniteTransition = rememberInfiniteTransition(label = "laser")
                            val laserProgress by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1400, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "laserAnim"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(laserProgress)
                                    .drawBehind {
                                        val y = size.height
                                        if (y > 1) {
                                            drawLine(
                                                color = Color(0xFF2DD4BF),
                                                start = Offset(0f, y),
                                                end = Offset(size.width, y),
                                                strokeWidth = 3f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Core Launch CTA Trigger
        item {
            val buttonColor = if (isApiKeyAvailable) MaterialTheme.colorScheme.primary else Color(0xFF0F766E)
            Button(
                onClick = onRunAnalysis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Search"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isApiKeyAvailable) "AI Study & Label Radiograph" else "AI Simulation Scan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Analysis Success/Idle Feedback area
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. AI Educational Analysis Report",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    when (scanUiState) {
                        is ScanUiState.Idle -> {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = "Manual study mode guide",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Ready to start analysis",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isApiKeyAvailable) {
                                        "Click compile/diagnose to trigger deep Gemini vision identification of anatomical points and landmarks."
                                    } else {
                                        "Gemini local practice framework is active. Click to run standard simulated radiographic labels instantly!"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                        is ScanUiState.Loading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Delineating anatomical margins and landmarks...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Retrieving tutorial curriculum from Gemini...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        is ScanUiState.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Real Render block for response
                                Text(
                                    text = scanUiState.responseText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                // Emergency disclaimer card
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Medical Notice",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Tutor Guideline: This analysis is simulated by MediMind AI for anatomical training purpose only. Clinicians must verify radiograph films via full diagnostic workstations for primary standard patient care.",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 14.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        is ScanUiState.Error -> {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Analysis Interrupted",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                                Text(
                                    text = scanUiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onRunAnalysis,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text("Retry Connection")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// =================== ANATOMY GUIDE TAB ===================
@Composable
fun AnatomyGuideView(
    onLandmarkExplain: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Interactive Anatomy Atlas",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Study fundamental anatomical structures visible on chest radiographs. Tap any structure card to request interactive AI tutoring on that landmark, explaining normal physiology and pathology.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        items(MedicalData.anatomyLandmarks) { landmark ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLandmarkExplain(landmark.name) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (landmark.category) {
                                        "Airway & Mediastinum" -> Icons.Default.Air
                                        "Cardiovascular" -> Icons.Default.FavoriteBorder
                                        "Pleural Space" -> Icons.Default.MedicalServices
                                        "Skeletal" -> Icons.Default.Grid3x3
                                        else -> Icons.Default.AutoStories
                                    },
                                    contentDescription = landmark.category,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = landmark.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = landmark.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Expand details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = landmark.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "SIGNIFICANCE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = landmark.clinicalSignificance,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "DIAGNOSTIC APPEARANCE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = landmark.xrayAppearance,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Explain CTA Button
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onLandmarkExplain(landmark.name) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask AI",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Consult AI Tutor on Pathology",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}


// =================== QUIZ TAB ===================
@Composable
fun QuizView(
    currentIndex: Int,
    selectedIdx: Int,
    score: Int,
    finished: Boolean,
    onAnswerSelect: (Int) -> Unit,
    onProceed: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!finished) {
            val question = MedicalData.coreQuizzes[currentIndex]
            val totalQuestions = MedicalData.coreQuizzes.size

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header progress info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Radiology Board Preparatory",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Topic: Thoracic Landmarks & Interpretation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = "Score: $score/$totalQuestions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Smooth Progress Linear line indicator
                LinearProgressIndicator(
                    progress = { (currentIndex.toFloat() / totalQuestions.toFloat()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )

                // Question Prompt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = "Active Question",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "QUESTION ${currentIndex + 1} OF $totalQuestions",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }

                // Multiple Choice Selection Blocks
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    question.options.forEachIndexed { idx, option ->
                        val hasSelectedAny = selectedIdx != -1
                        val isThisSelectedNow = selectedIdx == idx
                        val isCorrectOption = idx == question.correctAnswerIndex

                        val containerColor = when {
                            !hasSelectedAny -> MaterialTheme.colorScheme.surface
                            isCorrectOption -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                            isThisSelectedNow -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val borderOutlineColor = when {
                            isThisSelectedNow -> MaterialTheme.colorScheme.primary
                            isCorrectOption && hasSelectedAny -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .clickable(enabled = !hasSelectedAny) { onAnswerSelect(idx) },
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = BorderStroke(1.dp, borderOutlineColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Symmetrical Choice Indicator Badge (A, B, C, D)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = if (isThisSelectedNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ('A' + idx).toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isThisSelectedNow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (hasSelectedAny) {
                                    if (isCorrectOption) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Correct Choice",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    } else if (isThisSelectedNow) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Incorrect Choice",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Clinical Reasoning Card upon submission
                if (selectedIdx != -1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Reasoning Explanation",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "CLINICAL EXPLANATION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Secondary Next Action
                    Button(
                        onClick = onProceed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentIndex + 1 < totalQuestions) "Proceed to Next Question" else "Reveal Examination Report",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Proceed"
                        )
                    }
                }
            }
        } else {
            // Certificate/Revision Completion screen!
            QuizCertificateCard(
                score = score,
                totalQuestions = MedicalData.coreQuizzes.size,
                onRestart = onRestart
            )
        }
    }
}

@Composable
fun QuizCertificateCard(
    score: Int,
    totalQuestions: Int,
    onRestart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Verified Certificate Badge",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "MediMind AI Academic Board",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "Certificate of Competency",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Text(
                text = "This is proudly presented to the user for validating proficiency in Chest Radiograph (CXR) basic interpretative anatomy and critical space pathologies. By solving and answering with high diagnostic clinical reasoning: ",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EXAMINATION SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$score / $totalQuestions Correct",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Revise disclaimer
            Text(
                text = "Academic verification completed in cooperation with Gemini tutor libraries.",
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart Quiz"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Relaunch Exam Session", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// =================== HISTORY TAB ===================
@Composable
fun HistoryView(
    scans: List<HistoryScan>,
    onTabSwitch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Learning History & Records",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Browse your historically completed clinical radiography reviews. Review AI labeling remarks to solidify diagnostic competency markers over time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        if (scans.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = "No scan logs",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Study logs are currently clear.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Button(onClick = onTabSwitch) {
                        Text("Initiate First Scan")
                    }
                }
            }
        } else {
            items(scans) { scan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Diagnostic status indicator symbol
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = "Scan status verified",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scan.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = scan.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scan.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


// =================== DIALOG OVERLAYS ===================
@Composable
fun ConceptTutorPopup(
    state: ConceptTutorState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Tutor feedback",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "AI Pathology Tutor",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                when (state) {
                    is ConceptTutorState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "AI is drafting comprehensive landmark notes...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is ConceptTutorState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = state.concept,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.explanation,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 21.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    is ConceptTutorState.Error -> {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close Notes", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// =================== VOICE TUTOR TAB ===================
@Composable
fun VoiceTutorView(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceMessages by viewModel.voiceMessages.collectAsState()
    val isThinking by viewModel.isVoiceTutorThinking.collectAsState()
    val ttsSpeakTrigger by viewModel.ttsSpeakTrigger.collectAsState()

    // --- Text To Speech Lifecycle Setup ---
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isTtsMuted by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                ttsInstance?.setLanguage(Locale.US)
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    // React to new tutor messages and speak them out loud
    LaunchedEffect(ttsSpeakTrigger, isTtsReady, isTtsMuted) {
        val msg = ttsSpeakTrigger
        if (!msg.isNullOrEmpty() && isTtsReady && !isTtsMuted) {
            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "RadiologyVoiceID")
            viewModel.clearTtsSpeakTrigger()
        }
    }

    // --- Speech To Text Setup ---
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenWords = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenWords.isNullOrEmpty()) {
                viewModel.submitSpokenQuestion(spokenWords)
            }
        }
    }

    val recordVoiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask MediMind AI a radiology question...")
        }
    }

    // Waveform Pulsing Animation values for stunning interactive feedback
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulsing")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale_1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale_2"
    )

    var manualText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Auto-scroll to the bottom when new message arrives
    LaunchedEffect(voiceMessages.size) {
        if (voiceMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(voiceMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Voice Header / Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("voice_chat_header"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardVoice,
                        contentDescription = "Voice assistant Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Radiology AI Voice Chat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Discuss anatomical landmarks, X-ray diagnostic methods, and cardiac boundaries in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(
                    onClick = { viewModel.clearVoiceChat() },
                    modifier = Modifier.testTag("voice_clear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Reset chat history",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Suggestions list for rapid prompts injection
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val suggestions = listOf(
                "Explain normal CTR",
                "Pathology of costophrenic angles",
                "What is pneumothorax line?",
                "Trachea deviation causes"
            )
            items(suggestions) { phrase ->
                SuggestionChip(
                    onClick = { 
                        if (tts?.isSpeaking == true) tts?.stop()
                        viewModel.submitSpokenQuestion(phrase) 
                    },
                    label = { Text(phrase) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Spark suggest",
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("voice_suggestion_pill_${phrase.replace(" ", "_")}")
                )
            }
        }

        // Conversation transcript list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (voiceMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Talk lines are empty. Tap microphone to start.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(voiceMessages) { msg ->
                        VoiceTalkBubble(message = msg)
                    }
                    if (isThinking) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "AI is consulting diagnostic charts...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animated speech portal / interaction dashboard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive pulsing waves visualizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isThinking) {
                        // Pulsing medical thinking circles
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer(scaleX = pulseScale1, scaleY = pulseScale1)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer(scaleX = pulseScale2, scaleY = pulseScale2)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    } else {
                        // Standard interactive static wave bar visualization
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeTtsSpeaking = tts?.isSpeaking == true
                            val lineCount = 12
                            for (i in 0 until lineCount) {
                                val animScalar = if (activeTtsSpeaking) {
                                    val localTransition = rememberInfiniteTransition(label = "bar_$i")
                                    val barHeightScale by localTransition.animateFloat(
                                        initialValue = 0.2f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(400 + i * 50, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bar_anim_$i"
                                    )
                                    barHeightScale
                                } else {
                                    0.15f
                                }
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(32.dp * animScalar)
                                        .background(
                                            color = if (activeTtsSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // Interactive controller Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // TTS Audio Speaker output Sound toggle
                    IconButton(
                        onClick = { 
                            isTtsMuted = !isTtsMuted 
                            if (isTtsMuted) tts?.stop()
                        },
                        modifier = Modifier
                            .testTag("voice_muted_toggle_btn")
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isTtsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Toggle text output speech audio",
                            tint = if (isTtsMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Main mic controller trigger walkie-talkie button
                    Button(
                        onClick = {
                            if (tts?.isSpeaking == true) tts?.stop()
                            try {
                                speechLauncher.launch(recordVoiceIntent)
                            } catch (e: Exception) {
                                // Fallback info if speech system unavailable
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("voice_microphone_btn"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speak question into microphone",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Reset conversation history
                    IconButton(
                        onClick = { viewModel.clearVoiceChat() },
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat history log",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Inline manual text sender overlay so users can always input text queries
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        placeholder = { Text("Or type question manually to speak...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        trailingIcon = {
                            if (manualText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (tts?.isSpeaking == true) tts?.stop()
                                        viewModel.submitSpokenQuestion(manualText)
                                        manualText = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Submit typed query",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceTalkBubble(
    message: VoiceMessage
) {
    val bubbleColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    }

    val arrangement = if (message.isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = "Tutor Icon",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 2.dp,
                        bottomEnd = if (message.isUser) 2.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                    color = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (message.isUser) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Icon",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
