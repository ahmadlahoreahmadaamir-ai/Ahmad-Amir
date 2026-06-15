package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GeminiRequest
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.MedicalData
import com.example.data.QuizQuestion
import com.example.data.XraySample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class Success(val responseText: String) : ScanUiState()
    data class Error(val errorMessage: String) : ScanUiState()
}

sealed class ConceptTutorState {
    object Idle : ConceptTutorState()
    object Loading : ConceptTutorState()
    data class Success(val concept: String, val explanation: String) : ConceptTutorState()
    data class Error(val message: String) : ConceptTutorState()
}

enum class ActiveTab {
    Analyze,
    Guide,
    Quiz,
    Voice,
    History
}

data class VoiceMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class HistoryScan(
    val id: String,
    val title: String,
    val date: String,
    val summary: String,
    val imageUrl: String? = null,
    val imageUriString: String? = null
)

class MedicalViewModel : ViewModel() {

    // Active screen navigation
    private val _activeTab = MutableStateFlow(ActiveTab.Analyze)
    val activeTab: StateFlow<ActiveTab> = _activeTab.asStateFlow()

    // Key API Key Status
    val isApiKeyAvailable: Boolean = BuildConfig.GEMINI_API_KEY.isNotEmpty() && 
            BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    // Image state for Analysis
    private val _selectedSample = MutableStateFlow<XraySample?>(MedicalData.sampleXrays.first())
    val selectedSample: StateFlow<XraySample?> = _selectedSample.asStateFlow()

    private val _customImageUri = MutableStateFlow<Uri?>(null)
    val customImageUri: StateFlow<Uri?> = _customImageUri.asStateFlow()

    private val _customImageBitmap = MutableStateFlow<Bitmap?>(null)
    val customImageBitmap: StateFlow<Bitmap?> = _customImageBitmap.asStateFlow()

    // Analysis Status state
    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    // History Scans log
    private val _scanHistory = MutableStateFlow<List<HistoryScan>>(
        listOf(
            HistoryScan(
                id = "demo_1",
                title = "Study: Normal Thoracic Frame",
                date = "Jun 13, 2026",
                summary = "Normal vascular distributions fanning from trachea. Clear subdiaphragmatic spaces and acute costophrenic recesses.",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e6/Normal_posteroanterior_chest_radiograph.jpg"
            )
        )
    )
    val scanHistory: StateFlow<List<HistoryScan>> = _scanHistory.asStateFlow()

    // Ask AI concept study helper state
    private val _conceptTutorState = MutableStateFlow<ConceptTutorState>(ConceptTutorState.Idle)
    val conceptTutorState: StateFlow<ConceptTutorState> = _conceptTutorState.asStateFlow()

    // Quiz Mode state
    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _selectedAnswerIndex = MutableStateFlow(-1)
    val selectedAnswerIndex: StateFlow<Int> = _selectedAnswerIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _quizFinished = MutableStateFlow(false)
    val quizFinished: StateFlow<Boolean> = _quizFinished.asStateFlow()

    // Training Goals / Streak Tracker
    private val _studyStreak = MutableStateFlow(4) // Start with 4-day streak for gamification
    val studyStreak: StateFlow<Int> = _studyStreak.asStateFlow()

    private val _scansCompletedToday = MutableStateFlow(0)
    val scansCompletedToday: StateFlow<Int> = _scansCompletedToday.asStateFlow()

    fun selectTab(tab: ActiveTab) {
        _activeTab.value = tab
    }

    fun selectSample(sample: XraySample) {
        _selectedSample.value = sample
        _customImageUri.value = null
        _customImageBitmap.value = null
        _scanUiState.value = ScanUiState.Idle
    }

    fun setCustomImage(context: Context, uri: Uri) {
        _customImageUri.value = uri
        _selectedSample.value = null
        _scanUiState.value = ScanUiState.Idle
        viewModelScope.launch {
            val bitmap = loadUriBitmapAsSoftware(context, uri)
            _customImageBitmap.value = bitmap
        }
    }

    fun clearCustomImage() {
        _customImageUri.value = null
        _customImageBitmap.value = null
        _selectedSample.value = MedicalData.sampleXrays.first()
        _scanUiState.value = ScanUiState.Idle
    }

    // Submit Analysis content to Direct REST API
    fun runMedicalAnalysis(context: Context) {
        if (_scanUiState.value is ScanUiState.Loading) return

        _scanUiState.value = ScanUiState.Loading

        viewModelScope.launch {
            try {
                // Get Base64 image representation
                val bitmapToAnalyze = when {
                    _customImageBitmap.value != null -> _customImageBitmap.value
                    _selectedSample.value != null -> {
                        val url = _selectedSample.value!!.imageUrl
                        fetchBitmapFromUrl(context, url)
                    }
                    else -> null
                }

                if (bitmapToAnalyze == null) {
                    _scanUiState.value = ScanUiState.Error("Failed to render and prepare X-ray image for analysis.")
                    return@launch
                }

                val base64Data = bitmapToAnalyze.toBase64String()
                val activePrompt = _selectedSample.value?.prompt 
                    ?: "Analyze this chest radiograph (X-ray). List visible anatomical margins, landmarks, and highlights for medical education purposes."

                val key = BuildConfig.GEMINI_API_KEY
                if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
                    // Fallback simulated clinical response when API Key is missing, so students can fully interact with the feature!
                    simulateAnalysisResponse()
                    return@launch
                }

                val requestBody = GeminiRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = activePrompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                            )
                        )
                    )
                )

                val response = RetrofitClient.service.generateContent(key, requestBody)
                val aiAnalysis = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (aiAnalysis != null) {
                    _scanUiState.value = ScanUiState.Success(aiAnalysis)

                    // Add to log list
                    val scanTitle = _selectedSample.value?.title ?: "Custom Chest Study"
                    val studySummary = aiAnalysis.substringBefore("\n\n").take(150) + "..."
                    val imageSourceUrl = _selectedSample.value?.imageUrl
                    val customUriStr = _customImageUri.value?.toString()

                    _scanHistory.value = listOf(
                        HistoryScan(
                            id = "scan_${System.currentTimeMillis()}",
                            title = "Study: $scanTitle",
                            date = getCurrentFormattedDate(),
                            summary = studySummary,
                            imageUrl = imageSourceUrl,
                            imageUriString = customUriStr
                        )
                    ) + _scanHistory.value

                    _scansCompletedToday.value += 1
                } else {
                    _scanUiState.value = ScanUiState.Error("A response was returned, but no teaching content could be parsed.")
                }

            } catch (e: Exception) {
                Log.e("MedicalViewModel", "Error analyzing chest X-ray", e)
                _scanUiState.value = ScanUiState.Error("Connection error: ${e.localizedMessage ?: "Please verify your network settings."}")
            }
        }
    }

    private fun simulateAnalysisResponse() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800) // Simulated AI thinking
            
            val sample = _selectedSample.value
            val simulatedText = if (sample != null) {
                when (sample.id) {
                    "normal_cxr" -> """
                        ## 🩻 Clinical Chest X-Ray Medical Diagnosis Summary
                        
                        This is a Posteroanterior (PA) projection of a normal adult chest radiograph.
                        
                        ### 🔑 Identified Anatomy & Learning Milestones
                        1. **Trachea**: Well-centralized and midline. Carina visualizes clearly without tapering or deviation.
                        2. **Pulmonary Fields**: Bilateral expansion is symmetrical, high-volume. The vascular distribution flows symmetrically without consolidation or focal reticulations.
                        3. **Cardiac Borders**: Right atrium border, posterior left ventricle profile, and aortic arch margin appear normal in size, position, and tone. Cardiothoracic ratio calculated under 50%.
                        4. **Pleural Spaces**: Bilateral costophrenic and cardiophrenic recesses are acute, pointed, and clear of pleural thickening or transudative blunting.
                        
                        ### ⚕️ Clinical Tutorial Focus
                        When scanning high-quality PA projections, always start counting ribs from the posterior apex to assess effort. If fewer than 9 posterior ribs exist above the right diaphragm, suspect poor lung expansion rather than disease. Ensure clavicles form a symmetrical T-frame against the central vertebral spinous markings.
                        
                        ### 🎓 Quick Practice Questions for Students
                        - **Q1**: What is the normal cardiothoracic ratio on a standard upright PA chest X-ray?
                          *Answer*: Less than or equal to 50% (0.50). AP projections falsely magnify this ratio.
                        - **Q2**: A blunt or flattened costophrenic angle typically represents pooling of what?
                          *Answer*: Fluid in the pleural cavity (Pleural Effusion).
                    """.trimIndent()
                    "cardiomegaly_cxr" -> """
                        ## 🩻 Clinical Cardiomegaly Chest Study Instruction
                        
                        An upright film featuring dramatic transversal enlargement of the middle lower cardiac profile.
                        
                        ### 🔑 Identified Anatomy & Learning Milestones
                        1. **Cardiac Silhouette**: Transversal cardiac width extends beyond the 50% sagittal hemithorax midline divider. The measured CTR is approximately **62%**, confirming severe enlargement.
                        2. **Left Ventricular Bulge**: Sphericity is highly pronounced on the lateral apex margin, indicative of hypertrophic dialated left ventricle.
                        3. **Hilar Engorgements**: Prominent hilar projections fanning vascular columns represent increased pulmonary venous pressures.
                        
                        ### ⚕️ Clinical Tutorial Focus
                        To calculate the Cardiothoracic Ratio (CTR) accurately: measure the maximum horizontal distance of the heart silhouette (from the right heart border to the leftmost apex border) and divide it by the maximum internal chest width (measured above the level of the diaphragm domes). CTR > 0.5 is diagnostic for cardiomegaly in an upright PA radiograph.
                        
                        ### 🎓 Quick Practice Questions for Students
                        - **Q1**: Why is the AP (Anteroposterior) view unreliable for diagnosing cardiomegaly?
                          *Answer*: Due to divergent X-ray physics; the heart is anterior and farther from the detector, which amplifies its silhouette shadows.
                    """.trimIndent()
                    else -> """
                        ## 🩻 Clinical Right Pneumothorax Teaching Review
                        
                        A classic posteroanterior (PA) chest film showcasing high lucency with loss of lateral air ventilation pathways.
                        
                        ### 🔑 Identified Anatomy & Learning Milestones
                        1. **Visceral Pleural Line**: A fine, sharp white line outlining the collapsed right upper and middle lung tissue, offset from the chest wall.
                        2. **Hyperlucent Peripheral Zone**: Sizable right lateral space showing zero bronchovascular markings. This is pure air pocketed inside the pleural vault.
                        3. **Mediastinal Balance**: Tracheal column and cardiac border are currently midline. This indicates a **simple pneumothorax** rather than tension displacement.
                        
                        ### ⚕️ Clinical Tutorial Focus
                        In emergency clinical situations, look out for tracheal shift and rapid arterial drops. If the trachea and mediastinal boundary are pushed contralateral (to the left), the simple pneumothorax has transitioned into a life-threatening **Tension Pneumothorax**. This is an immediate indication for needle decompression in the 2nd intercostal space, midclavicular line.
                    """.trimIndent()
                }
            } else {
                """
                    ## 🩻 Custom Chest Study AI Summary
                    
                    The uploaded image shows anatomical alignments resembling a chest X-ray study frame.
                    
                    ### 🔬 Structure Delineations
                    - Symmetrical clavicle lines, matching skeletal ribs, and visible central column.
                    - Bilateral pulmonary lobes and central soft cardiovasculary density structures.
                    
                    *Simulation Disclaimer: This analysis represents simulated teaching feedback from MediMind AI's local model. Insert your Gemini API Key in the Secrets Panel to query active diagnostic neural models.*
                """.trimIndent()
            }

            _scanUiState.value = ScanUiState.Success(simulatedText)

            val scanTitle = _selectedSample.value?.title ?: "Custom Uploaded Study"
            _scanHistory.value = listOf(
                HistoryScan(
                    id = "scan_${System.currentTimeMillis()}",
                    title = "Study: $scanTitle",
                    date = getCurrentFormattedDate(),
                    summary = "Physiological structural landmarks mapped. Simulated clinical teaching points generated.",
                    imageUrl = _selectedSample.value?.imageUrl,
                    imageUriString = _customImageUri.value?.toString()
                )
            ) + _scanHistory.value

            _scansCompletedToday.value += 1
        }
    }

    // Dynamic Explain Landmark helper using direct REST client
    fun askAITutoringOnConcept(conceptName: String) {
        _conceptTutorState.value = ConceptTutorState.Loading
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    simulateConceptExplanation(conceptName)
                    return@launch
                }

                val prompt = "You are a friendly and expert Medical Radiology Tutor. " +
                        "Provide a concise, engaging, and highly informative clinical explanation of this anatomical landmark or clinical concept: \"$conceptName\". " +
                        "Explain (1) its appearance on a chest X-ray, (2) its clinical significance, and (3) what happens in common pathological states (e.g., blunting, deviation, consolidation). Keep it under 200 words and use elegant bullet points."

                val requestBody = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )

                val response = RetrofitClient.service.generateContent(apiKey, requestBody)
                val explanation = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (explanation != null) {
                    _conceptTutorState.value = ConceptTutorState.Success(conceptName, explanation)
                } else {
                    _conceptTutorState.value = ConceptTutorState.Error("Could not retrieve AI tutorial for $conceptName.")
                }
            } catch (e: Exception) {
                _conceptTutorState.value = ConceptTutorState.Error("Tutor connection failed. Local guides are fully available offline!")
            }
        }
    }

    private fun simulateConceptExplanation(concept: String) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            val explanation = when (concept) {
                "Trachea (Airway)" -> """
                    * **X-ray Appearance**: Shows up as a prominent dark midline tube carrying air, superimposed on the lower cervical and upper thoracic vertebral bodies.
                    * **Clinical Significance**: Acts as a critical reference line. Always look where it sits. Symmetrical clavicles must be equidistant to it.
                    * **Pathology Focus**: Tracheal deviation signals pressure shifting. Contralateral shift means push (tension pneumothorax, large effusions). Ipsilateral shift means pull (lobar atelectasis, fibrosing scars).
                """.trimIndent()
                "Cardiac Silhouette (Heart)" -> """
                    * **X-ray Appearance**: Large, heart-shaped, solid light central shadow that spans across the lower chest cavity, mainly offset to the left of the spinal midline.
                    * **Clinical Significance**: Evaluates cardiac size. Standard Cardiothoracic Ratio (CTR) on PA views must be under 0.5 (heart widest width divided by internal thoracic cage diameter).
                    * **Pathology Focus**: CTR > 0.5 rules in Cardiomegaly. Water-bottle shaped symmetrical expansion can signify pericardial effusion (fluid build-up in pericardium).
                """.trimIndent()
                "Costophrenic Angles" -> """
                    * **X-ray Appearance**: Sharp, deep triangular pointed clefts forming the lower left and right pockets of the lungs abutting the thoracic wall.
                    * **Clinical Significance**: Represents the deepest potential pleural space where fluid pools first in an upright patient.
                    * **Pathology Focus**: If the angle is rounded off (blunted) and forms a crescent line, it is a **Pleural Effusion**. Often takes 150-200ml of pleural fluid to blunt these on a PA view.
                """.trimIndent()
                "Hemidiaphragms" -> """
                    * **X-ray Appearance**: Two beautiful curved white arches forming the chest floor interface. The right arch is normally elevated (1.5 cm) relative to the left due to liver volume below.
                    * **Clinical Significance**: Represents major ventilary respiratory movement of muscles.
                    * **Pathology Focus**: Flattening reflects hyperinflation from emphysema or asthma. Free crescent of air below represents **Pneumoperitoneum**, highlighting a gastrointestinal rupture event.
                """.trimIndent()
                else -> """
                    * **X-ray Appearance**: Uniformly dark regions with branching cobwebs of vascular trees dispersing outward from the hila.
                    * **Clinical Significance**: Monitors respiratory oxygenation.
                    * **Pathology Focus**: Cloudy white consolidations represent alveolar filling (pneumonia, fluid, blood, or collapse). Perfect dark margins without markings indicate a **Pneumothorax** (air leak).
                """.trimIndent()
            }
            _conceptTutorState.value = ConceptTutorState.Success(concept, explanation)
        }
    }

    fun dismissConceptTutor() {
        _conceptTutorState.value = ConceptTutorState.Idle
    }

    // Quiz action triggers
    fun selectQuizAnswer(index: Int) {
        if (_selectedAnswerIndex.value != -1) return // Already submitted answer
        _selectedAnswerIndex.value = index
        
        val currentQuestion = MedicalData.coreQuizzes[_currentQuizIndex.value]
        if (index == currentQuestion.correctAnswerIndex) {
            _quizScore.value += 1
        }
    }

    fun proceedQuiz() {
        val nextIndex = _currentQuizIndex.value + 1
        if (nextIndex < MedicalData.coreQuizzes.size) {
            _currentQuizIndex.value = nextIndex
            _selectedAnswerIndex.value = -1
        } else {
            _quizFinished.value = true
        }
    }

    fun restartQuiz() {
        _currentQuizIndex.value = 0
        _selectedAnswerIndex.value = -1
        _quizScore.value = 0
        _quizFinished.value = false
    }

    // Voice Tutor Conversations
    private val _voiceMessages = MutableStateFlow<List<VoiceMessage>>(
        listOf(
            VoiceMessage(
                text = "Hello! I am your MediMind AI Radiology Voice Tutor. Speak any question about thoracic anatomy, landmarks, pathologies, or clinical techniques, and I will discuss it with you!",
                isUser = false
            )
        )
    )
    val voiceMessages: StateFlow<List<VoiceMessage>> = _voiceMessages.asStateFlow()

    private val _isVoiceTutorThinking = MutableStateFlow(false)
    val isVoiceTutorThinking: StateFlow<Boolean> = _isVoiceTutorThinking.asStateFlow()

    private val _ttsSpeakTrigger = MutableStateFlow<String?>(null)
    val ttsSpeakTrigger: StateFlow<String?> = _ttsSpeakTrigger.asStateFlow()

    fun clearTtsSpeakTrigger() {
        _ttsSpeakTrigger.value = null
    }

    fun submitSpokenQuestion(question: String) {
        if (question.isBlank()) return
        
        // Append user question
        val userMsg = VoiceMessage(text = question, isUser = true)
        _voiceMessages.value = _voiceMessages.value + userMsg
        
        _isVoiceTutorThinking.value = true
        
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                var contentResponse = ""
                
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Fallback intelligent clinical simulator response
                    contentResponse = getSimulatedVoiceResponse(question)
                } else {
                    val prompt = "You are a professional, spoken-voice AI Medical Radiology Tutor. " +
                            "Speak in natural, supportive, and engaging sentences. " +
                            "Provide a clear, educational, and highly concise answer (under 80 words) to this student's question: \"$question\". " +
                            "Do NOT use any markdown formatting such as asterisks (**), bullets, headings, or hashes because this response will be read out loud to the student. Speak in standard conversational text."
                    
                    val requestBody = GeminiRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt))))
                    )
                    
                    val response = RetrofitClient.service.generateContent(apiKey, requestBody)
                    contentResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "I heard your question, but I was unable to compile a tutoring response. Please try again."
                }
                
                // Clean response of any residual asterisks or markdown syntax
                val cleanedResponse = contentResponse
                    .replace("*", "")
                    .replace("#", "")
                    .replace("_", "")
                    .trim()
                
                val assistantMsg = VoiceMessage(text = cleanedResponse, isUser = false)
                _voiceMessages.value = _voiceMessages.value + assistantMsg
                _ttsSpeakTrigger.value = cleanedResponse
                
            } catch (e: Exception) {
                val errMsg = "Connection error. I couldn't reach the tutoring servers, but my local speech engine is active! Please try speaking again."
                _voiceMessages.value = _voiceMessages.value + VoiceMessage(text = errMsg, isUser = false)
                _ttsSpeakTrigger.value = errMsg
            } finally {
                _isVoiceTutorThinking.value = false
            }
        }
    }

    private fun getSimulatedVoiceResponse(question: String): String {
        val q = question.lowercase()
        return when {
            q.contains("trachea") || q.contains("airway") -> {
                "The trachea is the main airway seen as a dark, vertical air column in the middle of a chest X-ray. It should always be centered midway between the medial ends of both clavicles. If it is pushed or shifted to one side, this signals a major shift in lung volume or pressure, such as a tension pneumothorax or chest fluid effusion."
            }
            q.contains("heart") || q.contains("cardiac") || q.contains("cardiomegaly") -> {
                "In a normal posteroanterior chest X-ray, the cardiothoracic ratio should not exceed fifty percent. An enlarged heart, known as cardiomegaly, is diagnosed when the cardiac width exceeds this threshold. Note that anteroposterior views naturally magnify the heart footprint, so posteroanterior film views are much more reliable."
            }
            q.contains("pneumothorax") || q.contains("collapse") || q.contains("pleural") -> {
                "A pneumothorax represents air escaping into the pleural cavity, which causes the lung to collapse under pressure. On an X-ray, you will see a fine white visceral pleural line with a completely black, hyperlucent space surrounding it, showing no pulmonary vascular network lines. If it shifts the heart contralaterally, it is a surgical tension emergency!"
            }
            q.contains("diaphragm") || q.contains("angle") || q.contains("costophrenic") -> {
                "The costophrenic angles are those sharp, deep triangular pockets formed where the lungs touch the chest wall. Healthy ones are very pointy and sharp. If they look blunted or flat, this usually indicates pleural fluid pooling there due to gravity, which is a classic pleural effusion. It typically takes about two hundred milliliters of fluid to blunt this on an upright radiograph."
            }
            q.contains("hello") || q.contains("hi") || q.contains("hey") -> {
                "Hello there! I am your AI Radiology Tutor. Tell me what thoracic pathology, structure, or exam technique you would like to discuss! I can answer questions about the cardiac profile, trachea alignment, costophrenic recesses, or pneumothorax diagnostics."
            }
            else -> {
                "That is an excellent radiological question. On standard chest X-rays, evaluating the anatomical alignment starts with checking the quality parameters: proper inspiration showing ten posterior ribs, correct centering with centered clavicles, and clear penetration. What aspect of this should we study next?"
            }
        }
    }

    fun clearVoiceChat() {
        _voiceMessages.value = listOf(
            VoiceMessage(
                text = "Chat cleared! What shall we discuss next? Speak or type your question about radiology anatomy.",
                isUser = false
            )
        )
        _ttsSpeakTrigger.value = null
    }

    // Utility helpers
    private suspend fun fetchBitmapFromUrl(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // software bitmap is required to avoid graphics mapping errors and compress
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun loadUriBitmapAsSoftware(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Bitmap.toBase64String(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun getCurrentFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}
