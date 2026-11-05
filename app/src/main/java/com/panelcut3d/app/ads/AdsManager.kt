package com.panelcut3d.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.panelcut3d.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the two full-screen AdMob formats. Banner ads are composed separately.
 * Loading a full-screen ad again is deliberately best-effort: ads never block
 * a core editor flow if the network or inventory is unavailable.
 */
class AdsManager(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var lastInterstitialAt = 0L

    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady

    init {
        MobileAds.initialize(appContext) {
            mainHandler.post {
                _sdkReady.value = true
                loadInterstitial()
                loadRewarded()
            }
        }
    }

    private fun loadInterstitial() {
        if (interstitial != null) return
        InterstitialAd.load(
            appContext,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    Log.d(TAG, "Interstitial unavailable: ${error.code}")
                }
            },
        )
    }

    fun loadRewarded() {
        if (rewarded != null) return
        RewardedAd.load(
            appContext,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    _rewardedReady.value = false
                    Log.d(TAG, "Rewarded ad unavailable: ${error.code}")
                }
            },
        )
    }

    /**
     * Shows an interstitial when one is ready. [onComplete] is always called
     * exactly once, including when ads are removed or inventory is empty.
     */
    fun showInterstitial(
        activity: Activity?,
        isAdFree: Boolean,
        onComplete: () -> Unit,
    ) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (isAdFree || activity == null || ad == null || now - lastInterstitialAt < MIN_INTERSTITIAL_INTERVAL_MS) {
            if (!isAdFree && ad == null) loadInterstitial()
            onComplete()
            return
        }

        lastInterstitialAt = now
        interstitial = null
        var completed = false
        fun finish() {
            if (completed) return
            completed = true
            loadInterstitial()
            onComplete()
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "Interstitial failed to show: ${error.code}")
                finish()
            }
        }
        runCatching { ad.show(activity) }.onFailure { finish() }
    }

    fun showRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onUnavailable: () -> Unit,
    ) {
        val ad = rewarded
        if (activity == null || ad == null) {
            loadRewarded()
            onUnavailable()
            return
        }

        rewarded = null
        _rewardedReady.value = false
        var rewardedOnce = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadRewarded()

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "Rewarded ad failed to show: ${error.code}")
                loadRewarded()
            }
        }
        runCatching {
            ad.show(activity) {
                if (!rewardedOnce) {
                    rewardedOnce = true
                    onReward()
                }
            }
        }.onFailure {
            loadRewarded()
            onUnavailable()
        }
    }

    private companion object {
        const val TAG = "PanelCutAds"
        const val MIN_INTERSTITIAL_INTERVAL_MS = 90_000L
    }
}
