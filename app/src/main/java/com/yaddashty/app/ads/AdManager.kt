package com.yaddashty.app.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import ir.tapsell.plus.AdRequestCallback
import ir.tapsell.plus.AdShowListener
import ir.tapsell.plus.TapsellPlus
import ir.tapsell.plus.TapsellPlusInitListener
import ir.tapsell.plus.model.AdNetworkError
import ir.tapsell.plus.model.AdNetworks
import ir.tapsell.plus.model.TapsellPlusAdModel
import ir.tapsell.plus.model.TapsellPlusErrorModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ۱ تا ۸. مدیریت کامل تبلیغات ویدیویی پاداشی تپسل پلاس (AdManager)
 * - دارای تابع showRewarded(onReward, onFail)
 * - پیش‌بارگذاری خودکار تبلیغ (Preload) تا کاربر منتظر نماند
 * - اعطای جایزه فقط در صورت تماشای کامل ویدیو (کال‌بک onRewarded)
 * - عدم اعطای جایزه در صورت بستن زودهنگام ویدیو توسط کاربر
 * - بررسی وضعیت اینترنت و آماده بودن تبلیغ («الان تبلیغی موجود نیست»)
 */
object AdManager {
    private const val TAG = "TapsellAdManager"

    private var isInitialized = false
    private var isLoading = false
    private var preloadedResponseId: String? = null

    private val _adReadyFlow = MutableStateFlow(false)
    val adReadyFlow: StateFlow<Boolean> = _adReadyFlow.asStateFlow()

    private val _statusMessageFlow = MutableStateFlow("در حال آماده‌سازی تبلیغ...")
    val statusMessageFlow: StateFlow<String> = _statusMessageFlow.asStateFlow()

    /**
     * بررسی اتصال اینترنت دستگاه (اندروید ۸ به بالا / API 26+)
     */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * بررسی آماده بودن تبلیغ از قبل لود شده
     */
    fun isRewardedAdReady(context: Context): Boolean {
        return isOnline(context) && !preloadedResponseId.isNullOrEmpty()
    }

    /**
     * مقداردهی اولیه SDK تپسل پلاس و شروع خودکار Preload
     */
    fun initialize(activity: Activity, onStateChanged: (() -> Unit)? = null) {
        if (!isOnline(activity)) {
            _adReadyFlow.value = false
            _statusMessageFlow.value = "الان تبلیغی موجود نیست"
            onStateChanged?.invoke()
            return
        }

        if (isInitialized) {
            if (preloadedResponseId.isNullOrEmpty() && !isLoading) {
                preload(activity, onStateChanged)
            }
            return
        }

        TapsellPlus.initialize(
            activity,
            TapsellConfig.activeAppKey,
            object : TapsellPlusInitListener {
                override fun onInitializeSuccess(adNetworks: AdNetworks) {
                    isInitialized = true
                    if (TapsellConfig.testMode) {
                        TapsellPlus.setDebugMode(Log.DEBUG)
                    }
                    TapsellPlus.setGDPRConsent(activity, true)
                    Log.d(TAG, "TapsellPlus initialized: ${adNetworks.name}")
                    preload(activity, onStateChanged)
                }

                override fun onInitializeFailed(
                    adNetworks: AdNetworks,
                    adNetworkError: AdNetworkError
                ) {
                    Log.e(TAG, "Init failed: ${adNetworkError.errorMessage}")
                    _adReadyFlow.value = false
                    _statusMessageFlow.value = "الان تبلیغی موجود نیست"
                    onStateChanged?.invoke()
                }
            }
        )
    }

    /**
     * ۳. لود کردن تبلیغ از قبل (Preload) تا کاربر منتظر نماند
     */
    fun preload(activity: Activity, onStateChanged: (() -> Unit)? = null) {
        if (!isOnline(activity)) {
            preloadedResponseId = null
            _adReadyFlow.value = false
            _statusMessageFlow.value = "الان تبلیغی موجود نیست"
            onStateChanged?.invoke()
            return
        }

        if (isLoading || !preloadedResponseId.isNullOrEmpty()) {
            return
        }

        isLoading = true
        _statusMessageFlow.value = "در حال پیش‌بارگذاری تبلیغ..."

        TapsellPlus.requestRewardedVideoAd(
            activity,
            TapsellConfig.activeRewardedZoneId,
            object : AdRequestCallback() {
                override fun response(tapsellPlusAdModel: TapsellPlusAdModel) {
                    super.response(tapsellPlusAdModel)
                    isLoading = false
                    preloadedResponseId = tapsellPlusAdModel.responseId
                    _adReadyFlow.value = true
                    _statusMessageFlow.value = "تبلیغ آماده پخش است (Preloaded)"
                    Log.d(TAG, "Preloaded responseId: $preloadedResponseId")
                    onStateChanged?.invoke()
                }

                override fun error(message: String) {
                    isLoading = false
                    preloadedResponseId = null
                    _adReadyFlow.value = false
                    _statusMessageFlow.value = "الان تبلیغی موجود نیست"
                    Log.e(TAG, "Preload error: $message")
                    onStateChanged?.invoke()
                }
            }
        )
    }

    /**
     * ۱، ۴ و ۵. نمایش تبلیغ ویدیویی پاداشی:
     * showRewarded(onReward, onFail)
     * - جایزه فقط وقتی داده می‌شود که ویدیو کامل دیده شد (onRewarded).
     * - اگر کاربر وسط بست، جایزه نده.
     * - اگر تبلیغ آماده نبود یا اینترنت نبود، پیام «الان تبلیغی موجود نیست» نشان بده.
     */
    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onFail: (String) -> Unit
    ) {
        val responseId = preloadedResponseId
        if (!isOnline(activity) || responseId.isNullOrEmpty()) {
            _adReadyFlow.value = false
            _statusMessageFlow.value = "الان تبلیغی موجود نیست"
            onFail("الان تبلیغی موجود نیست")
            if (isOnline(activity) && !isLoading) {
                preload(activity)
            }
            return
        }

        var rewardCompleted = false
        preloadedResponseId = null
        _adReadyFlow.value = false

        TapsellPlus.showRewardedVideoAd(
            activity,
            responseId,
            object : AdShowListener() {
                override fun onOpened(tapsellPlusAdModel: TapsellPlusAdModel) {
                    super.onOpened(tapsellPlusAdModel)
                    Log.d(TAG, "Ad opened: ${tapsellPlusAdModel.responseId}")
                }

                override fun onRewarded(tapsellPlusAdModel: TapsellPlusAdModel) {
                    super.onRewarded(tapsellPlusAdModel)
                    // شرط ۴: فقط وقتی ویدیو کامل تماشا شد پرچم جایزه فعال می‌شود
                    rewardCompleted = true
                    Log.d(TAG, "Ad rewarded callback triggered!")
                }

                override fun onClosed(tapsellPlusAdModel: TapsellPlusAdModel) {
                    super.onClosed(tapsellPlusAdModel)
                    if (rewardCompleted) {
                        onReward()
                    } else {
                        // کاربر وسط ویدیو بست -> جایزه داده نمی‌شود
                        onFail("ویدیو کامل دیده نشد؛ جایزه فقط با تماشای کامل داده می‌شود.")
                    }
                    // بلافاصله تبلیغ بعدی را Preload کن
                    preload(activity)
                }

                override fun onError(tapsellPlusErrorModel: TapsellPlusErrorModel) {
                    super.onError(tapsellPlusErrorModel)
                    onFail("الان تبلیغی موجود نیست")
                    preload(activity)
                }
            }
        )
    }

    /**
     * بریج جاوااسکریپت برای ارتباط مستقیم موتور گرافیکی WebView با SDK نیتیو تپسل پلاس
     */
    class WebBridge(
        private val activity: Activity,
        private val webViewProvider: () -> WebView?
    ) {
        private fun sendEventToJs(event: String, payload: String = "") {
            activity.runOnUiThread {
                val safePayload = payload.replace("'", "\\'")
                webViewProvider()?.evaluateJavascript(
                    "window.onNativeTapsellCallback && window.onNativeTapsellCallback('$event', '$safePayload');",
                    null
                )
            }
        }

        @JavascriptInterface
        fun initialize(appKey: String, testMode: Boolean) {
            TapsellConfig.testMode = testMode
            activity.runOnUiThread {
                AdManager.initialize(activity) {
                    if (isRewardedAdReady(activity)) {
                        sendEventToJs("onAdResponse", preloadedResponseId ?: "")
                    }
                }
                sendEventToJs("onInitializeSuccess", "TAPSELL_NATIVE")
            }
        }

        @JavascriptInterface
        fun preloadRewardedAd(zoneId: String) {
            activity.runOnUiThread {
                AdManager.preload(activity) {
                    if (isRewardedAdReady(activity)) {
                        sendEventToJs("onAdResponse", preloadedResponseId ?: "")
                    } else {
                        sendEventToJs("onAdRequestError", "الان تبلیغی موجود نیست")
                    }
                }
            }
        }

        @JavascriptInterface
        fun isRewardedAdReady(): Boolean {
            return AdManager.isRewardedAdReady(activity)
        }

        @JavascriptInterface
        fun showRewardedAd() {
            activity.runOnUiThread {
                sendEventToJs("onOpened", preloadedResponseId ?: "")
                AdManager.showRewarded(
                    activity = activity,
                    onReward = {
                        sendEventToJs("onRewarded", "REWARD_EARNED")
                        sendEventToJs("onClosed", "COMPLETED")
                    },
                    onFail = { reason ->
                        if (reason == "الان تبلیغی موجود نیست") {
                            sendEventToJs("onShowError", reason)
                        } else {
                            sendEventToJs("onClosed", "EARLY_EXIT")
                        }
                    }
                )
            }
        }
    }
}
