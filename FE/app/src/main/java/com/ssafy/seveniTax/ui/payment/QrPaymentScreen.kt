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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.BrandPurple
import com.ssafy.seveniTax.ui.theme.CardBlue
import com.ssafy.seveniTax.ui.theme.CardGold
import com.ssafy.seveniTax.ui.theme.Divider
import com.ssafy.seveniTax.ui.theme.TextPrimary
import com.ssafy.seveniTax.ui.theme.TextSecondary
import com.ssafy.seveniTax.viewmodel.CardViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun QrPaymentScreen(
    navController: NavController,
    cardViewModel: CardViewModel = hiltViewModel(),
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cardUiState by cardViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCard by remember { mutableIntStateOf(0) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && !cameraPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary
                    )
                }
            }
            Text(
                text = "Pay 결제",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                val card = cardUiState.cards.getOrNull(selectedCard)
                var qrTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
                var remainingSeconds by remember { mutableIntStateOf(60) }
                LaunchedEffect(selectedCard) {
                    qrTimestamp = System.currentTimeMillis() / 1000
                    remainingSeconds = 60
                    while (true) {
                        delay(1_000L)
                        remainingSeconds--
                        if (remainingSeconds <= 0) {
                            qrTimestamp = System.currentTimeMillis() / 1000
                            remainingSeconds = 60
                        }
                    }
                }
                val qrData = "PAY-${card?.cardNumber?.takeLast(4) ?: "0000"}-$qrTimestamp"
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
                            modifier = Modifier.width(200.dp).height(200.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card?.cardNumber ?: "카드를 등록해주세요",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                            fontSize = 11.sp,
                            color = if (remainingSeconds <= 10) Color(0xFFE53935) else TextSecondary
                        )
                    }
                } else {
                    Text("QR코드 생성 실패", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
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

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            items(cardUiState.cards.size) { index ->
                val card = cardUiState.cards[index]
                val isSelected = index == selectedCard
                val animatedWidth by animateDpAsState(
                    targetValue = if (isSelected) 116.dp else 100.dp,
                    animationSpec = tween(200),
                    label = "cardWidth"
                )
                val animatedHeight by animateDpAsState(
                    targetValue = if (isSelected) 160.dp else 140.dp,
                    animationSpec = tween(200),
                    label = "cardHeight"
                )
                val animatedElevation by animateDpAsState(
                    targetValue = if (isSelected) 12.dp else 0.dp,
                    animationSpec = tween(200),
                    label = "cardElevation"
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
                        .background(if (card.type == "personal") CardGold else CardBlue)
                        .clickable { selectedCard = index }
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    val typeName = if (card.type == "personal") "일반 카드" else "사업자 카드"
                    Column {
                        Text(typeName, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(card.cardNumber.takeLast(17), fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

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
                    x,
                    y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        Log.e("QrCode", "QR코드 생성 실패", e)
        null
    }
}
