package com.example.billing

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

/** Play purchase UI. No entitlement is granted until the configured HTTPS verifier approves it. */
@Composable
fun PlayBillingCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Premium access is optional. Basic marks and calculations remain free.") }
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
        if (productId.isBlank()) status = "Set CLG1_PLUS_PRODUCT_ID in GitHub Actions secrets/environment and create the same product in Play Console."
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

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Clg 1% Plus", style = MaterialTheme.typography.titleMedium)
            Text(product?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                ?: "Price shown by Google Play when product is configured", style = MaterialTheme.typography.headlineSmall)
            Text("Advanced historical analytics and premium reporting. No personal academic data is uploaded by billing.", style = MaterialTheme.typography.bodySmall)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
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
            }, enabled = connected && product != null && productId.isNotBlank() && verifierUrl.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Subscribe with Google Play") }
            OutlinedButton(onClick = {
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
                        } else status = "Restore check could not reach Google Play: ${result.debugMessage.take(120)}. Existing verification data was retained."
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Restore purchases") }
        }
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
