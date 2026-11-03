package com.yaddashty.app.ads

/**
 * ۶ و ۷. فایل Config مجزا برای کلید برنامه (APP_KEY)، شناسه تبلیغ ویدیویی جایزه‌دار (ZONE_ID)
 * و حالت تست (testMode) مطابق مستندات رسمی تپسل پلاس (TapsellPlus SDK 2.3.3)
 */
object TapsellConfig {
    // فعال بودن حالت تست به صورت پیش‌فرض (قابل تغییر در زمان اجرا)
    @Volatile
    var testMode: Boolean = true

    // کلید برنامه و Zone ID محیط عملیاتی (Production)
    const val PROD_APP_KEY = "alsoatsrtrotpqacegkehkaiieckldhrgsbspqtgqnbrrfccrtbdomgjtahflchkqtqosa"
    const val PROD_REWARDED_ZONE_ID = "5cfaa802e8d17f0001ffb28e"

    // کلید برنامه و Zone ID رسمی تست تپسل پلاس (مطابق DefaultConfigs.java در مخزن رسمی تپسل)
    const val TEST_APP_KEY = "alsoatsrtrotpqacegkehkaiieckldhrgsbspqtgqnbrrfccrtbdomgjtahflchkqtqosa"
    const val TEST_REWARDED_ZONE_ID = "5cfaa802e8d17f0001ffb28e"

    val activeAppKey: String
        get() = if (testMode) TEST_APP_KEY else PROD_APP_KEY

    val activeRewardedZoneId: String
        get() = if (testMode) TEST_REWARDED_ZONE_ID else PROD_REWARDED_ZONE_ID
}
