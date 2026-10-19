package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonCyanLight

/**
 * High-performance CameraView using CameraX to display a live camera feed
 * for scanning and capturing movie / TV scenes.
 */
@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    onFrameCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            LiveCameraScanner(
                onFrameCaptured = onFrameCaptured,
                onClose = onClose
            )
        } else {
            CameraPermissionPlaceholder(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onClose = onClose
            )
        }
    }
}

@Composable
private fun LiveCameraScanner(
    onFrameCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Scanning line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_sweep"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Live Preview View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("camerax_preview_view"),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Viewfinder Cinema Frame & Laser overlay
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            val frameWidth = maxWidth
            val frameHeight = frameWidth * 0.65f // Cinematic aspect ratio

            Box(
                modifier = Modifier
                    .width(frameWidth)
                    .height(frameHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, ElectricCyan.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            ) {
                // Moving laser scanner line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .offset(y = frameHeight * laserProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    ElectricCyan.copy(alpha = 0.7f),
                                    NeonCyanLight,
                                    ElectricCyan.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Corner bracket graphics
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val cornerLength = 26.dp.toPx()
                    val cornerColor = Color(0xFFF5F6F8).copy(alpha = 0.95f)

                    // Top-left corner
                    drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                    drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

                    // Top-right corner
                    drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                    drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)

                    // Bottom-left corner
                    drawLine(cornerColor, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                    drawLine(cornerColor, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)

                    // Bottom-right corner
                    drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                    drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
                }

                // Subtitle guide in frame
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF12131A).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF222430), RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ALIGN FRAME WITH SCREEN",
                        color = Color(0xFFF5F6F8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF181A22).copy(alpha = 0.8f), CircleShape)
                    .border(1.dp, Color(0xFF222430), CircleShape)
                    .testTag("camera_close_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close Camera",
                    tint = Color(0xFFF5F6F8)
                )
            }

            // Scanner Title Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF181A22).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF222430), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(ElectricCyan, CircleShape)
                    )
                    Text(
                        text = "CINEMA SCANNER",
                        color = Color(0xFFF5F6F8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Flash / Torch toggle
            IconButton(
                onClick = {
                    isFlashOn = !isFlashOn
                    camera?.cameraControl?.enableTorch(isFlashOn)
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF181A22).copy(alpha = 0.8f), CircleShape)
                    .border(1.dp, Color(0xFF222430), CircleShape)
                    .testTag("camera_flash_toggle")
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = if (isFlashOn) "Turn Flash Off" else "Turn Flash On",
                    tint = if (isFlashOn) ElectricCyan else Color(0xFF8E93A4)
                )
            }
        }

        // Bottom Capture & Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch Lens (Front/Back)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("camera_switch_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Switch Camera Lens",
                        tint = Color.White
                    )
                }

                // Shutter / Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(enabled = !isCapturing) {
                            if (isCapturing) return@clickable
                            isCapturing = true

                            imageCapture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        try {
                                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                            val bitmap = imageProxy.toBitmap()
                                            val rotatedBitmap = if (rotationDegrees != 0) {
                                                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                            } else {
                                                bitmap
                                            }
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                onFrameCaptured(rotatedBitmap)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                            }
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                        }
                                    }
                                }
                            )
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan)
                                .border(3.dp, Color(0xFF12131A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Capture Scene",
                                tint = Color(0xFF12131A),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Spacer / Placeholder for symmetry
                Spacer(modifier = Modifier.size(48.dp))
            }

            Text(
                text = "Tap to capture frame and match scene",
                color = Color(0xFFF5F6F8).copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun CameraPermissionPlaceholder(
    onRequestPermission: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12131A))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ElectricCyan.copy(alpha = 0.12f))
                .border(1.5.dp, ElectricCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Camera Access Required",
            color = Color(0xFFF5F6F8),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "To scan and identify cinema scenes directly from your screen or display, please grant camera access.",
            color = Color(0xFF8E93A4),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF06080D)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("grant_camera_permission_button")
        ) {
            Text("Enable Camera", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFF8E93A4)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(44.dp)
        ) {
            Text("Cancel", fontSize = 14.sp)
        }
    }
}

/**
 * Embedded camera preview area designed to sit directly inside the main screen layout.
 * Includes live CameraX viewfinder, animated scanning laser, corner reticles,
 * torch toggle, and an instant capture shutter button.
 */
@Composable
fun InlineCameraScanner(
    modifier: Modifier = Modifier,
    onFrameCaptured: (Bitmap) -> Unit,
    onExpandFullScreen: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1015))
            .border(1.dp, Color(0xFF2D2D32), RoundedCornerShape(16.dp))
            .testTag("inline_camera_preview_area")
    ) {
        if (hasCameraPermission) {
            InlineLiveViewfinder(
                onFrameCaptured = onFrameCaptured,
                onExpandFullScreen = onExpandFullScreen
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricCyan.copy(alpha = 0.15f))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Live Camera Scene Scanner",
                    color = Color(0xFFF5F6F8),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grant camera access to scan scenes directly from your TV or screen",
                    color = Color(0xFF8E93A4),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF06080D)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("enable_camera_inline_button")
                ) {
                    Text("Enable Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InlineLiveViewfinder(
    onFrameCaptured: (Bitmap) -> Unit,
    onExpandFullScreen: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Scanning laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "inline_scanner_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inline_laser_sweep"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Live Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Overlay with scanning laser and corner brackets
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            val h = maxHeight
            // Subtle warm amber guideline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = h * laserProgress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                ElectricCyan.copy(alpha = 0.5f),
                                NeonCyanLight.copy(alpha = 0.8f),
                                ElectricCyan.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Corner brackets
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                val cornerLength = 18.dp.toPx()
                val cornerColor = ElectricCyan.copy(alpha = 0.7f)

                // Top-left
                drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
                // Top-right
                drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
                // Bottom-left
                drawLine(cornerColor, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                drawLine(cornerColor, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
                // Bottom-right
                drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                drawLine(cornerColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
            }
        }

        // Top badges & controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live scanning indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1015).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF2D2D32), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    )
                    Text(
                        text = "VIEWFINDER ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3F4F6),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Controls: Flash toggle & Expand fullscreen
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("inline_camera_flash_toggle")
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isFlashOn) ElectricCyan else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onExpandFullScreen,
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("inline_camera_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Full Screen",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom Capture Shutter Trigger
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Button(
                onClick = {
                    if (isCapturing) return@Button
                    isCapturing = true

                    imageCapture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                try {
                                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                    val bitmap = imageProxy.toBitmap()
                                    val rotatedBitmap = if (rotationDegrees != 0) {
                                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                    } else {
                                        bitmap
                                    }
                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                        onFrameCaptured(rotatedBitmap)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                    }
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                                ContextCompat.getMainExecutor(context).execute {
                                    isCapturing = false
                                }
                            }
                        }
                    )
                },
                enabled = !isCapturing,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF06080D)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("inline_camera_snap_button")
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color(0xFF12131A),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanning...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SCAN SCENE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}
