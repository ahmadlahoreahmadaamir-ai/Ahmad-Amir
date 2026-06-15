package com.example.data

data class AnatomyLandmark(
    val id: String,
    val name: String,
    val category: String, // e.g. "Airway", "Bones", "Cardiac", "Diaphragm"
    val description: String,
    val clinicalSignificance: String,
    val commonPathologies: String,
    val xrayAppearance: String
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val category: String
)

data class XraySample(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val prompt: String
)

object MedicalData {
    val sampleXrays = listOf(
        XraySample(
            id = "normal_cxr",
            title = "Normal Chest Radiograph",
            category = "Physiological Reference",
            description = "A standard posteroanterior (PA) view showcasing normal expansion, clear lung fields, normal-sized cardiac silhouette, and sharp costophrenic angles.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e6/Normal_posteroanterior_chest_radiograph.jpg",
            prompt = "You are a professional Medical Diagnostic Radiology Tutor. Analyze this chest X-ray. It is a Normal Posteroanterior Chest Radiograph. Identify the major visible anatomical structures (trachea, lungs, clavicles, ribs, cardiac silhouette, diaphragm, gastric bubble). Highlight what looks normal, list 3 educational landmarks, and provide two revision multiple choice questions (with explanations) for medical students based on standard PA X-ray landmarks."
        ),
        XraySample(
            id = "cardiomegaly_cxr",
            title = "Cardiomegaly View",
            category = "Cardiovascular Landmark",
            description = "An anteroposterior (AP) view where the cardiothoracic ratio is significantly increased (greater than 50% width of the chest cavity). Useful for teaching cardiac silhouette boundaries.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/a/a2/Cardiomegaly_X-ray.jpg",
            prompt = "You are a professional Medical Diagnostic Radiology Tutor. Analyze this chest X-ray showing Cardiomegaly (Enlarged Cardiac Silhouette). Explain the cardiothoracic ratio, identify the boundaries of the heart, describe how to calculate CTR on a standard chest radiograph, and list two clinical teaching points about left vs right ventricular enlargement on PA/Lateral views."
        ),
        XraySample(
            id = "pneumothorax_cxr",
            title = "Pneumothorax Study",
            category = "Pleural Space Pathology",
            description = "A PA radiograph demonstrating a collapsed right lung with a visible visceral pleural line and absence of lung markings peripherally. A critical emergency medicine teaching case.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/1/1a/Pneumothorax_CO_PA_10-9.jpg",
            prompt = "You are a professional Medical Diagnostic Radiology Tutor. Analyze this chest X-ray showing Pneumothorax (Air in pleural space). Highlight the visceral pleural line, the hyperlucency of the affected hemithorax, the absence of lung markings peripherally, and explain the difference between a simple pneumothorax and a life-threatening tension pneumothorax clinically."
        )
    )

    val anatomyLandmarks = listOf(
        AnatomyLandmark(
            id = "trachea",
            name = "Trachea (Airway)",
            category = "Airway & Mediastinum",
            description = "The central air-filled column descending midline from the neck into the chest, bifurcating at the carina.",
            clinicalSignificance = "Should be midline. Deviation can indicate volume loss (pulling trachea) or tension pneumothorax/masses (pushing trachea away).",
            commonPathologies = "Tracheal deviation, tracheal stenosis, foreign body aspiration.",
            xrayAppearance = "Appears as a vertical dark (radiolucent) tube filled with air, usually positioned midline over the cervical and thoracic vertebrae."
        ),
        AnatomyLandmark(
            id = "cardiac_silhouette",
            name = "Cardiac Silhouette (Heart)",
            category = "Cardiovascular",
            description = "The shadow of the heart and pericardium, occupying the lower half of the mediastinal width.",
            clinicalSignificance = "The diameter should be less than 50% of the maximum thoracic diameter on a PA projection (Cardiothoracic Ratio, CTR).",
            commonPathologies = "Cardiomegaly, pericardial effusion, congestive heart failure details.",
            xrayAppearance = "A dense, pale (radiopaque) central structure offset slightly to the left, boasting distinct right and left heart borders."
        ),
        AnatomyLandmark(
            id = "costophrenic_angles",
            name = "Costophrenic Angles",
            category = "Pleural Space",
            description = "The sharp, acute angles formed where the diaphragm meets the chest wall on either side.",
            clinicalSignificance = "Must be sharp and pointed. Blunting indicates the presence of fluid (pleural effusion) or thickening.",
            commonPathologies = "Pleural effusion (blunting, meniscus sign), pleurisy, pleural plaques.",
            xrayAppearance = "Sharp, dark triangular recess at the most inferior lateral margins of the left and right lung fields."
        ),
        AnatomyLandmark(
            id = "hemidiaphragms",
            name = "Hemidiaphragms",
            category = "Respiratory muscle",
            description = "The dome-shaped muscular barriers partition the thoracic cavity from the abdominal cavity.",
            clinicalSignificance = "The right dome is normally 1-2 cm higher than the left dome due to the underlying liver. Flattening indicates chronic hyperinflation.",
            commonPathologies = "Diaphragmatic paralysis, COPD hyperinflation (flattened diaphragms), free air under diaphragm (pneumoperitoneum).",
            xrayAppearance = "Smooth, curved radiopaque arches forming the floor of the lungs, with clear borders against the dark lung tissue."
        ),
        AnatomyLandmark(
            id = "clavicles",
            name = "Clavicles (Collar Bones)",
            category = "Skeletal",
            description = "Symmetrical horizontal bones anchoring the sternum to the shoulders.",
            clinicalSignificance = "Equidistant positioning of medial clavicle heads relative to the tracheal spinous processes confirms the patient was not rotated.",
            commonPathologies = "Clavicle fractures, acromioclavicular separation, osteolytic lesions.",
            xrayAppearance = "Distinct horizontal white bands extending across the top margins of the upper lung lobes bilaterally."
        ),
        AnatomyLandmark(
            id = "lung_lobes",
            name = "Lung Fields (Lobes)",
            category = "Respiratory Parenchyma",
            description = "The air-filled tissue of the right (3 lobes) and left (2 lobes) lung fields.",
            clinicalSignificance = "Should be evenly dark (radiolucent) with fine, light branching vascular markings tapering towards the periphery.",
            commonPathologies = "Pneumonia (consolidation), pulmonary edema, lung nodules, atelectasis.",
            xrayAppearance = "Dark, spongy appearance with branching vascular lines (pulmonary vessels) fanning out from the hila."
        )
    )

    val coreQuizzes = listOf(
        QuizQuestion(
            id = "q1",
            question = "Which of the following describes the most common cause of blunted costophrenic angles on a chest radiograph?",
            options = listOf(
                "Accumulation of fluid in the pleural cavity (Pleural Effusion)",
                "Hyperinflation of the lung tissue due to COPD",
                "Acute lobar pneumonia within the upper right lobe",
                "Tracheal deviation secondary to pneumothorax"
            ),
            correctAnswerIndex = 0,
            explanation = "Fluid in the pleural cavity naturally pools at the lowest gravitational point in the chest. In an upright patient, this causes blunting or loss of the sharp, pointed appearance of the costophrenic angles, forming a classic meniscus sign.",
            category = "Anatomy & Pathologies"
        ),
        QuizQuestion(
            id = "q2",
            question = "A cardiac silhouette width exceeding 50% of the thoracic diameter on an upright PA view indicates:",
            options = listOf(
                "Pneumomediastinum",
                "Cardiomegaly",
                "Normal physiological variation on PA view",
                "Severe emphysema"
            ),
            correctAnswerIndex = 1,
            explanation = "Cardiomegaly is diagnostic when the cardiothoracic ratio (CTR) exceeds 0.5 (or 50%) on a standard, full-inspiration upright PA radiograph. On AP projections, the heart is magnified and CTR is less reliable.",
            category = "Cardiovascular"
        ),
        QuizQuestion(
            id = "q3",
            question = "How can a clinician confirm that a chest radiograph is properly centered with no patient rotation?",
            options = listOf(
                "The patient's trachea is severely deviated to the right",
                "The stomach bubble is located high beneath the left hemidiaphragm",
                "The medial ends of both clavicles are equidistant from the vertebral spinous processes",
                "The right lung base is exactly level with the left lung base"
            ),
            correctAnswerIndex = 2,
            explanation = "Patient placement check relies on checking that the spinous processes in the upper thorax lie exactly midway between the medial heads of the clavicles. Asymmetric spacing indicates rotation.",
            category = "X-ray Quality"
        ),
        QuizQuestion(
            id = "q4",
            question = "Air and gas pooling underneath the right hemidiaphragm on an upright chest X-ray is a sign of:",
            options = listOf(
                "Pneumothorax (air in pleura)",
                "Pneumoperitoneum (perforated abdominal viscus)",
                "Gastric bubble (normal stomach gas)",
                "Basal pneumonic consolidation"
            ),
            correctAnswerIndex = 1,
            explanation = "Air beneath the diaphragm (crescent of radiolucency) represents free gas in the peritoneal cavity, signaling a perforated bowel/viscus. This is a surgical emergency! Contrast this with normal stomach gas which sits under the left diaphragm.",
            category = "Emergency Radiology"
        )
    )
}
