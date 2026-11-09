package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.util.UpiLink
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Who receives the tea. Change here only. */
object SupportConfig {
    const val UPI_ID = "arunsanna071@okaxis"
    const val PAYEE_NAME = "Arun Sanna"
    const val NOTE = "Tea for Clg 1"
    val AMOUNTS = listOf(10, 20, 50, 100)
}

private data class UpiApp(val label: String, val packageName: String, val icon: ImageBitmap?)

/** Lists the UPI apps installed on this phone (needs the <queries> entry for the upi scheme in the manifest). */
@Suppress("DEPRECATION")
private fun findUpiApps(context: Context, probeLink: String): List<UpiApp> {
    val pm = context.packageManager
    val probe = Intent(Intent.ACTION_VIEW, Uri.parse(probeLink))
    val infos = if (Build.VERSION.SDK_INT >= 33)
        pm.queryIntentActivities(probe, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    else pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
    return infos.distinctBy { it.activityInfo.packageName }.map { ri ->
        UpiApp(
            label = ri.loadLabel(pm).toString(),
            packageName = ri.activityInfo.packageName,
            icon = runCatching { ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        )
    }.sortedBy { it.label.lowercase() }
}

private fun qrBitmap(text: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"))
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}

/**
 * "Buy me a tea": a voluntary UPI tip. Nothing in the app is unlocked by it.
 *
 * Why this replaces the old single "ofc" button:
 *  - amounts are sent with the link (apps open straight on the pay screen),
 *  - installed UPI apps are listed and opened directly instead of through a generic chooser,
 *  - the result the UPI app reports is read and shown honestly (it is not proof of payment),
 *  - if an app refuses the link, the UPI id can be copied or paid by scanning a QR from another phone.
 */
@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var amount by remember { mutableStateOf<Int?>(20) }
    var showQr by remember { mutableStateOf(false) }
    var outcomeText by remember { mutableStateOf<String?>(null) }

    val link = remember(amount) { UpiLink.build(SupportConfig.UPI_ID, SupportConfig.PAYEE_NAME, amount, SupportConfig.NOTE) }
    val apps = remember { findUpiApps(context, UpiLink.build(SupportConfig.UPI_ID, SupportConfig.PAYEE_NAME, null, SupportConfig.NOTE)) }
    val qr = remember(link, showQr) { if (showQr) runCatching { qrBitmap(link).asImageBitmap() }.getOrNull() else null }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        outcomeText = if (result.resultCode == Activity.RESULT_CANCELED && data == null)
            "No payment was reported. You may have cancelled, or your UPI app does not send results back. Check your bank or UPI app if unsure."
        else UpiLink.message(UpiLink.parseResult(data?.getStringExtra("response"), data?.getStringExtra("Status")))
    }

    fun launch(packageName: String?) {
        val base = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        if (packageName != null) base.setPackage(packageName)
        val toStart = if (packageName == null) Intent.createChooser(base, "Pay with") else base
        try {
            launcher.launch(toStart)
        } catch (e: ActivityNotFoundException) {
            outcomeText = "That app could not open the payment. Use another app, copy the UPI ID, or scan the QR code."
        } catch (e: SecurityException) {
            outcomeText = "Android blocked opening that app. Copy the UPI ID or scan the QR code instead."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 340.dp), shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.foundation.Image(painter = painterResource(id = R.drawable.ic_tea_cup), contentDescription = "Tea cup", modifier = Modifier.size(72.dp))
                Text("Buy me a tea", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text("A voluntary tip. It does not unlock any features.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SupportConfig.AMOUNTS.forEach { a ->
                        FilterChip(selected = amount == a, onClick = { amount = a }, label = { Text("\u20B9$a") }, modifier = Modifier.testTag("tea_amount_$a"))
                    }
                    FilterChip(selected = amount == null, onClick = { amount = null }, label = { Text("Any amount") })
                }

                if (apps.isEmpty()) {
                    Text("No UPI app was found on this phone. Copy the UPI ID or show the QR code and pay from another phone.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                } else {
                    Text("Pay with", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                    apps.take(6).forEach { app ->
                        Surface(
                            onClick = { launch(app.packageName) }, shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth().testTag("upi_app_${app.packageName}")
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(32.dp)) else Box(Modifier.size(32.dp))
                                Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    TextButton(onClick = { launch(null) }) { Text("More apps...") }
                }

                Text("Payee: ${SupportConfig.PAYEE_NAME}\n${SupportConfig.UPI_ID}", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Check this name in your UPI app before you pay.", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(SupportConfig.UPI_ID))
                        Toast.makeText(context, "UPI ID copied", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.weight(1f).testTag("copy_upi")) { Text("Copy ID") }
                    OutlinedButton(onClick = { showQr = !showQr }, modifier = Modifier.weight(1f).testTag("show_qr")) { Text(if (showQr) "Hide QR" else "Show QR") }
                }
                if (qr != null) {
                    Image(bitmap = qr, contentDescription = "UPI QR code for ${SupportConfig.PAYEE_NAME}", modifier = Modifier.size(200.dp))
                    Text("Scan with any UPI app on another phone.", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }

                outcomeText?.let {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Button(onClick = onDismiss, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().height(46.dp)) { Text("Close") }
            }
        }
    }
}
