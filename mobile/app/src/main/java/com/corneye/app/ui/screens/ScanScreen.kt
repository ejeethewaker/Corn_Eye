// Scan Screen
// Camera and gallery picker for capturing a corn leaf photo to analyse.
package com.corneye.app.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.corneye.app.navigation.Screen
import com.corneye.app.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            showCamera = true
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            capturedImageUri = it
            isAnalyzing = true
            performAnalysis(context, navController, imageFile = null, imageUri = it) {
                isAnalyzing = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    var selectedTab by remember { mutableIntStateOf(2) }

    if (showCamera && hasCameraPermission) {
        // Full camera view
        CameraView(
            navController = navController,
            context = context,
            lifecycleOwner = lifecycleOwner,
            cameraExecutor = cameraExecutor,
            imageCapture = imageCapture,
            onImageCaptureReady = { imageCapture = it },
            isAnalyzing = isAnalyzing,
            onAnalyzing = { isAnalyzing = it },
            galleryLauncher = { galleryLauncher.launch("image/*") },
            onBack = { showCamera = false }
        )
    } else {
        // Pre-scan UI matching the mockup
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                        when (index) {
                            0 -> navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                            1 -> navController.navigate(Screen.Profile.route)
                            2 -> { /* Already on Scan */ }
                            3 -> navController.navigate(Screen.Notifications.route)
                            4 -> navController.navigate(Screen.Settings.route)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .verticalScroll(rememberScrollState())
            ) {
                // Status bar background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldenBackground)
                        .background(StatusBarGold)
                        .windowInsetsPadding(WindowInsets.statusBars)
                )

                // Top header - Golden with back arrow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldenBackground)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Scan  Corn",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Scan area with golden background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldenBackground)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Cream/beige scan zone with corner brackets
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFF5E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Corner brackets
                        // Top-left
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .size(40.dp, 4.dp)
                                    .background(GreenPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .size(4.dp, 40.dp)
                                    .background(GreenPrimary)
                            )
                            // Top-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(40.dp, 4.dp)
                                    .background(GreenPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(4.dp, 40.dp)
                                    .background(GreenPrimary)
                            )
                            // Bottom-left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                                    .size(40.dp, 4.dp)
                                    .background(GreenPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                                    .size(4.dp, 40.dp)
                                    .background(GreenPrimary)
                            )
                            // Bottom-right
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(40.dp, 4.dp)
                                    .background(GreenPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(4.dp, 40.dp)
                                    .background(GreenPrimary)
                            )
                        }

                        // Camera icon in center
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Tips section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldenBackground)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Tips for Better Results:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Take photos in good lighting conditions (natural daylight preferred)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Focus on individual corn leaves with clear visibility of symptoms",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Avoid blurry images - hold steady or use camera timer",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Button area with light background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background)
                        .padding(vertical = 20.dp)
                ) {
                    // Capture Photo button
                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                showCamera = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "Capture Photo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraView(
    navController: NavController,
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraExecutor: ExecutorService,
    imageCapture: ImageCapture?,
    onImageCaptureReady: (ImageCapture) -> Unit,
    isAnalyzing: Boolean,
    onAnalyzing: (Boolean) -> Unit,
    galleryLauncher: () -> Unit,
    onBack: () -> Unit
) {
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewViewRef = previewView
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                        .build()

                    onImageCaptureReady(imgCapture)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imgCapture
                        )
                        cameraInstance = camera
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                val change = event.changes.first()
                                val pv = previewViewRef ?: continue
                                val cam = cameraInstance ?: continue
                                val factory = pv.meteringPointFactory
                                val point = factory.createPoint(
                                    change.position.x,
                                    change.position.y
                                )
                                val action = FocusMeteringAction.Builder(point)
                                    .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                                cam.cameraControl.startFocusAndMetering(action)
                            }
                        }
                    }
                }
        )

        // Top bar overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = White
                )
            }
            Text(
                text = "Scan Corn Leaf",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Guide frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                // Corner indicators
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(40.dp, 4.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(4.dp, 40.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp, 4.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(4.dp, 40.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(40.dp, 4.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(4.dp, 40.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(40.dp, 4.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(4.dp, 40.dp)
                        .background(GreenLight, RoundedCornerShape(2.dp))
                )
            }
        }

        // Instruction text
        Text(
            text = "Position the corn leaf inside the frame",
            fontSize = 14.sp,
            color = White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery button
            IconButton(
                onClick = galleryLauncher,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(White.copy(alpha = 0.2f))
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Capture button
            IconButton(
                onClick = {
                    if (!isAnalyzing) {
                        captureAndAnalyze(
                            context = context,
                            imageCapture = imageCapture,
                            executor = cameraExecutor,
                            navController = navController,
                            onAnalyzing = onAnalyzing
                        )
                    }
                },
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(White)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    tint = White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Flash toggle
            IconButton(
                onClick = {
                    val cam = cameraInstance
                    if (cam != null && cam.cameraInfo.hasFlashUnit()) {
                        isFlashOn = !isFlashOn
                        cam.cameraControl.enableTorch(isFlashOn)
                        imageCapture?.flashMode = if (isFlashOn)
                            ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFlashOn) GreenPrimary.copy(alpha = 0.5f)
                        else White.copy(alpha = 0.2f)
                    )
            ) {
                Icon(
                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = if (isFlashOn) Color.Yellow else White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Analyzing overlay
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = GreenLight,
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Analyzing...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detecting diseases on your corn leaf",
                        fontSize = 14.sp,
                        color = White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun captureAndAnalyze(
    context: Context,
    imageCapture: ImageCapture?,
    executor: ExecutorService,
    navController: NavController,
    onAnalyzing: (Boolean) -> Unit
) {
    imageCapture ?: return

    val photoFile = File(
        context.cacheDir,
        "corn_scan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    onAnalyzing(true)

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                performAnalysis(context, navController, imageFile = photoFile, imageUri = null) {
                    onAnalyzing(false)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onAnalyzing(false)
                Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private fun performAnalysis(
    context: Context,
    navController: NavController,
    imageFile: java.io.File?,
    imageUri: android.net.Uri?,
    onComplete: () -> Unit
) {
    Thread {
        val uriString = imageFile?.toURI()?.toString() ?: imageUri?.toString() ?: ""

        // ── Stage 1: Green pixel pre-filter (fast, no model) ─────────────
        // Reject obviously non-plant images before running the classifier.
        val leafDetector = CornLeafDetector(context)
        leafDetector.initialize()

        val passesColorCheck: Boolean = when {
            imageFile != null -> leafDetector.isCornLeafFromFile(imageFile)
            imageUri  != null -> leafDetector.isCornLeafFromUri(imageUri)
            else              -> false
        }
        leafDetector.close()

        if (!passesColorCheck) {
            android.util.Log.d("ScanScreen", "Color pre-filter: FAILED → InvalidScan")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete()
                navController.navigate(Screen.InvalidScan.createRoute("not_corn")) {
                    popUpTo(Screen.Scan.route)
                }
            }
            return@Thread
        }

        // ── Stage 1.5: Background removal for display ─────────────────────
        // Segment the leaf from its background so all result screens show
        // the isolated leaf (black background), like a lab photo.
        // Classification in Stage 2 still uses the ORIGINAL file/URI —
        // the model was trained without background removal.
        val displayUriString: String = try {
            val rawBitmap: Bitmap? = when {
                imageFile != null -> android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                imageUri  != null -> context.contentResolver.openInputStream(imageUri)
                    ?.use { android.graphics.BitmapFactory.decodeStream(it) }
                else -> null
            }
            val segmented = rawBitmap?.let { LeafBackgroundRemover.removeBackground(it) }
            if (segmented != null) {
                val segFile = LeafBackgroundRemover.saveToCacheFile(
                    context, segmented,
                    "corn_seg_${System.currentTimeMillis()}.jpg"
                )
                android.net.Uri.fromFile(segFile).toString()
            } else {
                android.util.Log.w("ScanScreen", "BG removal failed — using original image")
                uriString
            }
        } catch (e: Exception) {
            android.util.Log.w("ScanScreen", "BG removal skipped: ${e.message}")
            uriString
        }

        // ── Stage 2: Disease classification with OOD gates ───────────────
        // Single model: 5 classes (4 corn diseases + Invalid).
        // OOD gates: reject if class=="Invalid" OR confidence<70% OR entropy>0.8
        val classifier = CornDiseaseClassifier(context)
        val modelLoaded = classifier.initialize()

        val tfliteResult: CornDiseaseClassifier.Result? = when {
            !modelLoaded      -> null
            imageFile != null -> classifier.classifyFromFile(imageFile)
            imageUri  != null -> classifier.classifyFromUri(imageUri)
            else              -> null
        }
        classifier.close()

        // Check OOD gates
        if (tfliteResult == null || !tfliteResult.isValid) {
            val reason = when {
                tfliteResult == null -> "model_error"
                else -> "ood_rejected"
            }
            android.util.Log.d("ScanScreen", "OOD gate: REJECTED (${tfliteResult?.label}, " +
                "conf=${tfliteResult?.confidence}, entropy=${tfliteResult?.entropy}) → InvalidScan")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete()
                navController.navigate(Screen.InvalidScan.createRoute(reason)) {
                    popUpTo(Screen.Scan.route)
                }
            }
            return@Thread
        }

        val label      = tfliteResult.label
        val confidence = tfliteResult.confidence

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onComplete()
            navController.navigate(
                Screen.Analyzing.createRoute(label, confidence, displayUriString)
            )
        }
    }.start()
}
