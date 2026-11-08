package com.example.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.BuildConfig
import com.example.data.security.EntitlementCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Premium access is checked against the server; local preferences only hold a Keystore-encrypted purchase token. */
object PlayEntitlementVerifier {
    suspend fun isActive(context: android.content.Context): Boolean = withContext(Dispatchers.IO) {
        val testMode = context.getSharedPreferences("clg1_entitlements", 0).getBoolean("test_plus_active", false)
        if (testMode) return@withContext true
        val productId = BuildConfig.CLG1_PLUS_PRODUCT_ID
        val verifierUrl = BuildConfig.BILLING_VERIFY_URL
        if (productId.isBlank() || verifierUrl.isBlank()) return@withContext false
        val encrypted = context.getSharedPreferences("clg1_entitlements", 0).getString("purchase_token_enc", null) ?: return@withContext false
        val token = EntitlementCipher.decrypt(encrypted) ?: return@withContext false
        runCatching { verifyOnServer(context, productId, token, verifierUrl) }.getOrDefault(false)
    }
}

private fun verifyOnServer(context: android.content.Context, productId: String, purchaseToken: String, verifierUrl: String): Boolean {
    val body = JSONObject().put("packageName", context.packageName).put("productId", productId)
        .put("purchaseToken", purchaseToken).toString()
        .toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder().url(verifierUrl).post(body).build()
    val response = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build().newCall(request).execute()
    return try {
        if (!response.isSuccessful) false
        else JSONObject(response.body?.string().orEmpty()).optBoolean("active", false)
    } finally {
        response.close()
    }
}

/** Play purchase UI with detailed breakdown of FREE vs PLUS models. */
@Composable
fun PlayBillingCard() {
    val context = LocalContext.current
    val entitlementPrefs = remember { context.getSharedPreferences("clg1_entitlements", 0) }
    var testPlusEnabled by remember { mutableStateOf(entitlementPrefs.getBoolean("test_plus_active", false)) }
    var status by remember { mutableStateOf("Basic marks and calculations remain 100% free forever.") }
    var product by remember { mutableStateOf<ProductDetails?>(null) }
    var connected by remember { mutableStateOf(false) }
    val productId = BuildConfig.CLG1_PLUS_PRODUCT_ID
    val verifierUrl = BuildConfig.BILLING_VERIFY_URL

    val client = remember {
        BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
                    purchases.forEach { purchase -> processPurchase(context.applicationContext, purchase, productId, verifierUrl) { status = it } }
                } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) status = "Purchase cancelled. No payment was taken."
                else if (result.responseCode != BillingClient.BillingResponseCode.OK) status = "Google Play Billing: ${result.debugMessage.take(140)}"
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
    }

    DisposableEffect(client) {
        if (productId.isBlank()) status = "Set CLG1_PLUS_PRODUCT_ID in environment and create the same product in Play Console."
        else if (verifierUrl.isBlank()) status = "Billing is wired, but premium stays locked until a secure purchase-verification endpoint is configured."
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connected = result.responseCode == BillingClient.BillingResponseCode.OK
                if (connected && productId.isNotBlank()) {
                    val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                        QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(BillingClient.ProductType.SUBS).build()
                    )).build()
                    client.queryProductDetailsAsync(params) { queryResult, details ->
                        if (queryResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            product = details.productDetailsList.firstOrNull()
                            if (product == null) status = "Premium product is not published or the product ID is incorrect."
                        } else status = "Could not load Play product: ${queryResult.debugMessage.take(120)}"
                    }
                    val owned = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
                    client.queryPurchasesAsync(owned) { queryResult, purchases ->
                        if (queryResult.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach {
                            processPurchase(context.applicationContext, it, productId, verifierUrl) { status = it }
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() { connected = false; status = "Google Play connection paused. It will reconnect automatically." }
        })
        onDispose { client.endConnection() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Membership & Access Model",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // FREE TIER CARD
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FREE TIER",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Included",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TierFeatureItem("Existing attendance and timetable capabilities")
                    TierFeatureItem("Academic setup wizard and template import/export")
                    TierFeatureItem("Unlimited manual marks entry and correction")
                    TierFeatureItem("Basic calculations: internal total, subject result, percentage, basic SGPA/CGPA")
                    TierFeatureItem("Exam countdown and basic reminders")
                    TierFeatureItem("Basic target marks calculator")
                    TierFeatureItem("First 10 successful Deep Analytics reports free")
                }
            }

            // PLUS TIER CARD
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Plus",
                                tint = Color(0xFFE6B94A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "PLUS TIER",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF9E7A6).copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (testPlusEnabled) "Active (Preview)" else "Introductory",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF785100)
                            )
                        }
                    }

                    // Price display (dynamic from Play Store, or introductory offer description)
                    val priceText = product?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                        ?: "Example introductory offer: ₹15 for 90 days"
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "(Experimental introductory pricing; not hard-coded)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TierFeatureItem("Advanced / deep analytics during active entitlement", isPlus = true)
                    TierFeatureItem("Multiple what-if scenarios and richer projections", isPlus = true)
                    TierFeatureItem("Advanced historical comparisons and PDF reports", isPlus = true)
                    TierFeatureItem("Other premium features after user testing establishes value", isPlus = true)

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val activity = context as? Activity
                            val details = product
                            if (activity == null || details == null || !connected) status = "Google Play product is not ready."
                            else {
                                val offer = details.subscriptionOfferDetails?.firstOrNull()
                                if (offer == null) status = "No eligible subscription offer is available for this Play account."
                                else {
                                    val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build()
                                    val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item)).build()
                                    val result = client.launchBillingFlow(activity, params)
                                    if (result.responseCode != BillingClient.BillingResponseCode.OK) status = "Could not start purchase: ${result.debugMessage.take(120)}"
                                }
                            }
                        },
                        enabled = connected && product != null && productId.isNotBlank() && verifierUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1065))
                    ) {
                        Text("Subscribe with Google Play", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            if (!connected) status = "Google Play is not connected."
                            else {
                                val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
                                client.queryPurchasesAsync(params) { result, purchases ->
                                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                        if (purchases.isNotEmpty()) purchases.forEach { processPurchase(context.applicationContext, it, productId, verifierUrl) { status = it } }
                                        else {
                                            context.getSharedPreferences("clg1_entitlements", 0).edit().remove("purchase_token_enc").apply()
                                            status = "No active Plus purchase was found for this Play account."
                                        }
                                    } else status = "Restore check could not reach Google Play: ${result.debugMessage.take(120)}."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore purchases")
                    }

                    // Developer / Preview Plus toggle
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Preview Plus mode (Testing)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Unlock what-if scenarios and unlimited PDF reports for evaluation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = testPlusEnabled,
                            onCheckedChange = { active ->
                                testPlusEnabled = active
                                entitlementPrefs.edit().putBoolean("test_plus_active", active).apply()
                                status = if (active) "Plus mode unlocked for testing and evaluation." else "Plus test mode turned off."
                            }
                        )
                    }
                }
            }

            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TierFeatureItem(text: String, isPlus: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPlus) Icons.Default.Star else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isPlus) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun processPurchase(
    context: android.content.Context,
    purchase: Purchase,
    productId: String,
    verifierUrl: String,
    onStatus: (String) -> Unit
) {
    if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
        onStatus("Purchase pending. Plus will remain locked until Google confirms payment.")
        return
    }
    if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || productId !in purchase.products) return
    if (verifierUrl.isBlank()) {
        onStatus("Purchase received, but not activated: secure server verification is not configured.")
        return
    }
    onStatus("Verifying purchase securely…")
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val verified = verifyOnServer(context, productId, purchase.purchaseToken, verifierUrl)
            if (!verified) throw IllegalStateException("Google Play did not confirm an active entitlement")
            val encryptedToken = EntitlementCipher.encrypt(purchase.purchaseToken)
            context.getSharedPreferences("clg1_entitlements", 0).edit()
                .putString("purchase_token_enc", encryptedToken)
                .putLong("plus_verified_at", System.currentTimeMillis())
                .putString("plus_product_id", productId)
                .apply()
            withContext(Dispatchers.Main) { onStatus("Google Play purchase verified. Plus entitlement is active.") }
        } catch (error: Exception) {
            withContext(Dispatchers.Main) { onStatus("Purchase is not activated: ${error.message ?: "verification failed"}. Try Restore purchases later.") }
        }
    }
}
