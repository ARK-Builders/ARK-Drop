package dev.arkbuilders.drop.app.ui.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

data class CustomAvatar(
    val id: String,
    val bitmap: Bitmap,
    val base64: String
)

@Composable
fun AvatarCreatorSheet(
    onAvatarCreated: (CustomAvatar) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    
    // Create temporary file for camera capture
    val photoFile = remember {
        createImageFile(context)
    }
    
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPermissionDialog = false
            capturedImageUri = photoUri
        } else {
            showPermissionDialog = true
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                val bitmap = loadBitmapFromUri(context, uri)
                bitmap?.let {
                    selectedBitmap = it
                    showCropDialog = true
                }
            }
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            bitmap?.let { bmp ->
                selectedBitmap = bmp
                showCropDialog = true
            }
        }
    }
    
    // Main sheet content
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create Avatar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Close"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Camera option
        AvatarCreationOption(
            icon = Icons.Default.Check,
            title = "Take Photo",
            subtitle = "Use camera to take a new photo",
            onClick = {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                        cameraLauncher.launch(photoUri)
                    }
                    else -> {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Gallery option
        AvatarCreationOption(
            icon = Icons.Default.Check,
            title = "Choose from Gallery",
            subtitle = "Select an existing photo",
            onClick = {
                galleryLauncher.launch("image/*")
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
    
    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Camera Permission Required") },
            text = { Text("Please grant camera permission to take photos for your avatar.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Crop dialog
    if (showCropDialog && selectedBitmap != null) {
        AvatarCropDialog(
            bitmap = selectedBitmap!!,
            onCropComplete = { croppedBitmap ->
                val customAvatar = CustomAvatar(
                    id = "custom_${System.currentTimeMillis()}",
                    bitmap = croppedBitmap,
                    base64 = AvatarUtils.bitmapToBase64(croppedBitmap)
                )
                onAvatarCreated(customAvatar)
                showCropDialog = false
                selectedBitmap = null
            },
            onDismiss = {
                showCropDialog = false
                selectedBitmap = null
            }
        )
    }
}

@Composable
fun AvatarCreationOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFF4285F4),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AvatarCropDialog(
    bitmap: Bitmap,
    onCropComplete: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var cropSize by remember { mutableStateOf(300) }
    val croppedBitmap = remember(bitmap, cropSize) {
        cropToCircle(bitmap, cropSize)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crop Avatar") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preview
                if (croppedBitmap != null) {
                    Image(
                        bitmap = croppedBitmap.asImageBitmap(),
                        contentDescription = "Avatar preview",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Size: ${cropSize}px",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Slider(
                    value = cropSize.toFloat(),
                    onValueChange = { cropSize = it.toInt() },
                    valueRange = 200f..500f,
                    steps = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    croppedBitmap?.let(onCropComplete)
                }
            ) {
                Text("Create Avatar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility functions
private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "AVATAR_${timeStamp}_",
        ".jpg",
        storageDir
    )
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

private fun cropToCircle(bitmap: Bitmap, size: Int): Bitmap? {
    return try {
        val scaledBitmap = bitmap.scale(size, size)
        val output = createBitmap(size, size)
        
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)
        
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = -0xbdbdbe
        canvas.drawOval(rectF, paint)
        
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaledBitmap, rect, rect, paint)
        
        output
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Extension to save custom avatars locally
object CustomAvatarManager {
    private const val CUSTOM_AVATARS_DIR = "custom_avatars"
    
    fun saveCustomAvatar(context: Context, avatar: CustomAvatar): String? {
        return try {
            val dir = File(context.filesDir, CUSTOM_AVATARS_DIR)
            if (!dir.exists()) dir.mkdirs()
            
            val file = File(dir, "${avatar.id}.png")
            val outputStream = FileOutputStream(file)
            
            avatar.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun loadCustomAvatar(context: Context, avatarId: String): Bitmap? {
        return try {
            val file = File(context.filesDir, "$CUSTOM_AVATARS_DIR/$avatarId.png")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun deleteCustomAvatar(context: Context, avatarId: String): Boolean {
        return try {
            val file = File(context.filesDir, "$CUSTOM_AVATARS_DIR/$avatarId.png")
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}