package com.panelcut3d.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.panelcut3d.app.BuildConfig
import com.panelcut3d.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BillingRepository(
    context: Context,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _price: MutableStateFlow<String?> = MutableStateFlow(null)

    /** Play-formatted localized price, or null until Play has answered. */
    val price: StateFlow<String?> = _price

    private var connected = false
    private var product: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach(::handlePurchase)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase update failed: ${result.responseCode} ${result.debugMessage}")
        }
    }

    private val client = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                    queryProduct()
                    restorePurchases()
                } else {
                    connected = false
                    Log.d(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                connected = false
            }
        })
    }

    fun purchase(activity: Activity, onError: (String) -> Unit) {
        val details = product
        if (!connected || details == null) {
            onError("Google Play is unavailable right now. Please try again later.")
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Could not launch purchase: ${result.responseCode} ${result.debugMessage}")
            onError("The purchase screen could not be opened.")
        }
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                product = queryResult.productDetailsList
                    .firstOrNull { it.productId == BuildConfig.REMOVE_ADS_PRODUCT_ID }
                _price.value = product
                    ?.oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()
                    ?.formattedPrice
            } else {
                Log.d(TAG, "Product query failed: ${result.responseCode} ${result.debugMessage}")
            }
        }
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
            } else {
                Log.d(TAG, "Purchase restore failed: ${result.responseCode} ${result.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (BuildConfig.REMOVE_ADS_PRODUCT_ID !in purchase.products) return

        // Grant the local entitlement immediately. Acknowledgement is then
        // best-effort because a store hiccup must not hide a paid purchase.
        scope.launch {
            settings.update { it.copy(adsRemoved = true) }
        }
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Acknowledge failed: ${result.responseCode} ${result.debugMessage}")
                }
            }
        }
    }

    private companion object {
        const val TAG = "PanelCutBilling"
    }
}
