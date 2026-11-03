# 🎮 بازی دو بعدی نبرد نئون کهکشانی (Neon Cyber Galaxy 2D) + SDK تپسل پلاس

<!-- build-info:start -->
> 🛠 **آخرین بیلد موفق:** `2026-11-03T19:15:07Z`  
> 📱 **پشتیبانی اندروید:** اندروید ۸.۰ به بالا (`minSdk 26` / `targetSdk 35`)  
> 📦 **حجم فایل APK:** 22.59 مگابایت · نسخه: **۱**  
> 🔐 **امضای فایل (SHA-256):** `498e1f3379de49c38129d96c566c594b29dd1cc5adb3a8951e6ae748359894ec`  
> 🔏 **امضای گواهی دیجیتال APK (Cert SHA-256):** `6bee3c0a4a5f807151e9593925265a0ab55c24d7114816216e7c9e2fe2d3f36b`  
> 📥 **[دانلود مستقیم فایل APK (GitHub Release)](https://github.com/havedays67-ai/yaddashty/releases/latest/download/yaddashty-v1.apk)** · **[دانلود مستقیم از شاخه اصلی (Raw APK)](https://github.com/havedays67-ai/yaddashty/raw/main/releases/yaddashty-v1.apk)**
<!-- build-info:end -->

---

## 🚀 ویژگی‌های بازی و پیاده‌سازی تبلیغ ویدیویی پاداشی تپسل (Tapsell Plus SDK)

این پروژه یک **بازی دو بعدی پرگرافیک نئون کهکشانی** برای **اندروید ۸.۰ به بالا (`minSdk = 26`)** است که تمامی الزامات رسمی **SDK تپسل پلاس (`ir.tapsell.plus:tapsell-plus-sdk-android:2.3.3`)** در آن پیاده‌سازی شده است:

1. **فایل مستقل `AdManager.kt` با تابع `showRewarded(onReward, onFail)`:**
   - مسیر: [`app/src/main/java/com/yaddashty/app/ads/AdManager.kt`](app/src/main/java/com/yaddashty/app/ads/AdManager.kt)
2. **رعایت مستندات رسمی فعلی SDK تپسل پلاس (`2.3.3`):**
   - استفاده از `TapsellPlus.initialize`، `TapsellPlus.requestRewardedVideoAd` و `TapsellPlus.showRewardedVideoAd`.
3. **پیش‌بارگذاری خودکار تبلیغ (`Preload`):**
   - تبلیغ در لحظه ورود به بازی و بلافاصله پس از بسته شدن هر تبلیغ در پس‌زمینه لود می‌شود تا کاربر در صفحه Game Over منتظر نماند.
4. **اعطای جایزه فقط در صورت تماشای کامل ویدیو (`onRewarded`):**
   - جایزه (`+۱ جان اضافه`) صرفاً زمانی داده می‌شود که کال‌بک `onRewarded` فراخوانی شده باشد. اگر کاربر ویدیو را در میانه پخش ببندد (`onClosed` بدون `onRewarded`)، هیچ جایزه‌ای تعلق نمی‌گیرد.
5. **بررسی وضعیت اینترنت و آمادگی تبلیغ:**
   - در صورت قطع بودن اینترنت یا آماده نبودن تبلیغ، دکمه غیرفعال شده و پیام **«الان تبلیغی موجود نیست»** نمایش داده می‌شود.
6. **فایل تنظیمات جداگانه (`TapsellConfig.kt`):**
   - مسیر: [`app/src/main/java/com/yaddashty/app/ads/TapsellConfig.kt`](app/src/main/java/com/yaddashty/app/ads/TapsellConfig.kt) شامل `APP_KEY` و `REWARDED_ZONE_ID`.
7. **پشتیبانی از حالت تست (`testMode`):**
   - فعال بودن حالت تست با کلیدها و Zone ID رسمی تست تپسل و قابلیت تغییر زنده در نوار بالای بازی.
8. **دکمه «ببین تا یک جان اضافه بگیر» در صفحه Game Over:**
   - مسیر: [`app/src/main/java/com/yaddashty/app/ui/screens/NeonGameScreen.kt`](app/src/main/java/com/yaddashty/app/ui/screens/NeonGameScreen.kt) متصل به `AdManager.showRewarded`.

---

## 📲 دانلود و نصب روی اندروید ۸ به بالا

1. فایل **[yaddashty-v1.apk](https://github.com/havedays67-ai/yaddashty/releases/latest/download/yaddashty-v1.apk)** را از لینک بالای همین صفحه دانلود کنید.
2. روی دستگاه‌های دارای **اندروید ۸.۰ (API 26) تا اندروید ۱۵ (API 35)** نصب و اجرا نمایید.

## 🧱 ساختار معماری پروژه

```
app/src/main/
├── assets/game/index.html                         # موتور گرافیکی اولترا ۲ بعدی + پنل تست تپسل
├── java/com/yaddashty/app/
│   ├── MainActivity.kt                            # میزبان اصلی بازی و مقداردهی اولیه AdManager
│   ├── ads/
│   │   ├── TapsellConfig.kt                       # کلید برنامه، Zone ID و حالت تست (شروط ۶ و ۷)
│   │   └── AdManager.kt                           # مدیریت Preload و showRewarded(onReward, onFail)
│   └── ui/screens/
│       └── NeonGameScreen.kt                      # گیم‌پلی ۲ بعدی ۶۰ فریم و صفحه Game Over (شرط ۸)
```
