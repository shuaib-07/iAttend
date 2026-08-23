package com.iattend.app.feature.devsupport

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.iattend.app.R
import com.iattend.app.core.ui.SquircleIconButton

private const val UPI_ID = "mdshuaib2005-1@okhdfcbank"
private const val GITHUB_URL = "https://github.com/shuaib-07"
private const val LINKEDIN_URL = "https://www.linkedin.com/in/muhammed-shuaib-6430881b5?utm_source=share_via&utm_content=profile&utm_medium=member_android"
private const val EMAIL = "mdshuaib2005@gmail.com"
private const val INSTAGRAM_HANDLE = "@shuaib07_"
private const val INSTAGRAM_URL = "https://instagram.com/shuaib07_"
private const val DEVELOPER_NAME = "Muhammed Shuaib"
private const val APP_DESCRIPTION = "Attendance tracking and timetable planning, without the spreadsheet."

/** Bottom-sheet content (see ModalSheet) - no Scaffold of its own, hosted by SettingsScreen. */
@Composable
fun DeveloperSupportSheetContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            "Unknown"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "iAttend v$versionName", style = MaterialTheme.typography.headlineSmall)
                Text(text = APP_DESCRIPTION, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Developer photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )

                Text(
                    text = "Developed by $DEVELOPER_NAME",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) }, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GitHub")
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINKEDIN_URL))) }, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("LinkedIn")
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL"))) }, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Email")
                    }
                    OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL))) }, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Instagram")
                    }
                }
            }
        }

        Text(
            "Buy the developer a chai",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
        )
        Text(
            "If iAttend's saved you a headache, a chai's always appreciated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val qrBitmap = remember { generateQrBitmap("upi://pay?pa=$UPI_ID&pn=Developer&cu=INR", 512) }
            Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "UPI QR code", modifier = Modifier.size(220.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { clipboard.setText(AnnotatedString(UPI_ID)) },
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("UPI ID", style = MaterialTheme.typography.labelMedium)
                    Text(UPI_ID, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bitmap
}
