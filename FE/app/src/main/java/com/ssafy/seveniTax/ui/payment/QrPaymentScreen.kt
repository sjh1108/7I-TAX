package com.ssafy.seveniTax.ui.payment

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private data class MockCard(
    val name: String,
    val last4: String,
    val color: Color
)

private val mockCards = listOf(
    MockCard("신한카드", "5678", Color(0xFF2196F3)),
    MockCard("국민카드", "2342", Color(0xFF1A237E)),
    MockCard("현대카드", "9018", Color(0xFFE53935)),
    MockCard("삼성카드", "1234", Color(0xFF00897B)),
)

@Composable
fun QrPaymentScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=바코드, 1=QR스캔
    var selectedCard by remember { mutableIntStateOf(0) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    // QR스캔 탭 선택 시 카메라 권한 요청
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && !cameraPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        // ── 상단 바 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Pay 결제",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 바코드 / QR스캔 탭 ──
        Row(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .border(1.dp, Divider, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            TabButton("바코드", selectedTab == 0, Modifier.weight(1f)) {
                selectedTab = 0
                scannedResult = null
            }
            TabButton("QR스캔", selectedTab == 1, Modifier.weight(1f)) {
                selectedTab = 1
                scannedResult = null
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 컨텐츠 영역 ──
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Divider, RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == 0) {
                // ── QR코드 생성 (1분마다 갱신) ──
                val card = mockCards[selectedCard]
                var qrTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
                LaunchedEffect(selectedCard) {
                    qrTimestamp = System.currentTimeMillis() / 1000
                    while (true) {
                        delay(60_000L)
                        qrTimestamp = System.currentTimeMillis() / 1000
                    }
                }
                val qrData = "PAY-${card.last4}-$qrTimestamp"
                val qrBitmap = remember(qrData) { generateQrCode(qrData) }

                if (qrBitmap != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR코드",
                            modifier = Modifier
                                .size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${card.name} ••••${card.last4}",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text("QR코드 생성 실패", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                // ── QR 스캔 (카메라) ──
                if (scannedResult != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "스캔 완료",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scannedResult!!,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPurple)
                                .clickable { scannedResult = null }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text("다시 스캔", color = Color.White, fontSize = 14.sp)
                        }
                    }
                } else if (cameraPermissionGranted) {
                    QrScannerView(
                        onQrScanned = { result ->
                            scannedResult = result
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("카메라 권한이 필요합니다", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPurple)
                                .clickable {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text("권한 허용", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 카드 슬라이더 ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            items(mockCards.size) { index ->
                val card = mockCards[index]
                val isSelected = index == selectedCard
                val animatedWidth by animateDpAsState(
                    targetValue = if (isSelected) 116.dp else 100.dp,
                    animationSpec = tween(200), label = "cardWidth"
                )
                val animatedHeight by animateDpAsState(
                    targetValue = if (isSelected) 160.dp else 140.dp,
                    animationSpec = tween(200), label = "cardHeight"
                )
                val animatedElevation by animateDpAsState(
                    targetValue = if (isSelected) 12.dp else 0.dp,
                    animationSpec = tween(200), label = "cardElevation"
                )
                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(animatedHeight)
                        .shadow(
                            elevation = animatedElevation,
                            shape = RoundedCornerShape(12.dp),
                            clip = false
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(card.color)
                        .clickable { selectedCard = index }
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(card.name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("••••${card.last4}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // ── 카드 추가 버튼 ──
            item {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Divider, RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Route.CardTypeSelect.path) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QrScannerView(
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var hasScanned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !hasScanned) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val barcode = barcodes.firstOrNull()
                                if (barcode != null && !hasScanned) {
                                    hasScanned = true
                                    val value = barcode.rawValue ?: "알 수 없는 데이터"
                                    onQrScanned(value)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrScanner", "카메라 바인딩 실패", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (selected) TextPrimary else Color.Transparent)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

private fun generateQrCode(data: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        Log.e("QrCode", "QR코드 생성 실패", e)
        null
    }
}
