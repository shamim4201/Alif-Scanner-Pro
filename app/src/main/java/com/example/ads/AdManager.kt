package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"

    // Real User AdMob IDs
    const val REAL_APP_ID = "ca-app-pub-5838132584781260~8987824372"
    const val REAL_BANNER_AD_UNIT_ID = "ca-app-pub-5838132584781260/4081101725"
    const val REAL_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5838132584781260/6664913368"

    // Official Google Test Ad Unit IDs (Used in debug/testing to protect user account from accidental invalid traffic)
    private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Set to true to use real IDs in production release, or false for Google test ads during emulator testing
    var useRealAds: Boolean = true

    fun getBannerAdUnitId(): String {
        return if (useRealAds) REAL_BANNER_AD_UNIT_ID else TEST_BANNER_AD_UNIT_ID
    }

    fun getInterstitialAdUnitId(): String {
        return if (useRealAds) REAL_INTERSTITIAL_AD_UNIT_ID else TEST_INTERSTITIAL_AD_UNIT_ID
    }

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob SDK Initialized: $initializationStatus")
                loadInterstitialAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdMob", e)
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = getInterstitialAdUnitId()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Interstitial Ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.w(TAG, "Interstitial Ad failed to load: ${error.message}. Retrying with test ID if needed.")
                    // Fallback to test ID in development if new user ad unit is still undergoing Google activation
                    if (useRealAds) {
                        InterstitialAd.load(
                            context,
                            TEST_INTERSTITIAL_AD_UNIT_ID,
                            adRequest,
                            object : InterstitialAdLoadCallback() {
                                override fun onAdLoaded(testAd: InterstitialAd) {
                                    interstitialAd = testAd
                                }
                                override fun onAdFailedToLoad(err: LoadAdError) {}
                            }
                        )
                    }
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdClosed()
                }
            }
            ad.show(activity)
        } else {
            // No ad ready, proceed immediately
            loadInterstitialAd(activity)
            onAdClosed()
        }
    }
}

@Composable
fun RealAdMobBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdManager.getBannerAdUnitId()
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
