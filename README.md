<div dir="rtl" align="center">

# 📝 یادداشتی

**دفترچهٔ یادداشتِ آفلاین، خصوصی و فارسی — سبک، سریع و بدون حساب کاربری**

[![Build APK](https://github.com/havedays67-ai/yaddashty/actions/workflows/android.yml/badge.svg)](https://github.com/havedays67-ai/yaddashty/actions/workflows/android.yml)
[![Version](https://img.shields.io/badge/version-1-0F766E)](https://github.com/havedays67-ai/yaddashty/releases/tag/v1)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/license-MIT-C2703D)](LICENSE)

### ⬇️ [دانلود مستقیم APK — یادداشتی نسخهٔ ۱](https://github.com/havedays67-ai/yaddashty/releases/latest/download/yaddashty-v1.apk)

> اگر لینک بالا باز نشد، به بخش [Releases](https://github.com/havedays67-ai/yaddashty/releases/tag/v1) بروید و فایل `yaddashty-v1.apk` را دانلود کنید.
> فایل ساخته‌شده در همین مخزن هم نگه داشته می‌شود: [`releases/yaddashty-v1.apk`](releases/yaddashty-v1.apk)

</div>

<!-- build-info:start -->
> 🛠 **آخرین بیلد موفق:** `2026-10-29T12:16:22Z`  
> 📦 حجم فایل: 16.84 مگابایت · نسخه: **۱**  
> 🔐 SHA-256: `773ef7d75fd8ee5d338bf24591c050071dfd046572cf3c09264d51bc4cb149cd`  
> [دانلود مستقیم APK](https://github.com/havedays67-ai/yaddashty/releases/latest/download/yaddashty-v1.apk)
<!-- build-info:end -->

---

<div dir="rtl">

## ✨ چرا یادداشتی؟

- **کاملاً آفلاین:** هیچ سروری، هیچ حسابی و هیچ ردیابی‌ای وجود ندارد؛ داده‌ها فقط در دیتابیس Room روی گوشی شما می‌مانند.
- **فارسی و راست‌به‌چپ:** کل رابط کاربری فارسی است و چیدمان از ابتدا RTL طراحی شده.
- **سریع و سبک:** Jetpack Compose + Room، بدون کتابخانه‌های سنگین و بدون تبلیغات.
- **حالت روشن و تیره:** با تمِ کاغذی گرم و تمِ شبانهٔ آرام، همراه با رنگ‌های ملایم برای هر یادداشت.

## امکانات نسخهٔ ۱

| امکان | توضیح |
| --- | --- |
| ایجاد و ویرایش | عنوان، متن، برچسب، رنگ کارت و ذخیرهٔ خودکار |
| سنجاق کردن | یادداشت‌های مهم بالای فهرست می‌مانند |
| جستجو | جستجوی زنده در عنوان، متن و برچسب‌ها |
| برچسب‌ها | دسته‌بندی با چند برچسب و فیلتر سریع |
| بایگانی | کنار گذاشتن یادداشت‌ها بدون حذف |
| سطل زباله | حذف نرم، بازگردانی و پاک‌سازی کامل |
| تکثیر | ساخت نسخهٔ کپی از هر یادداشت |
| اشتراک‌گذاری | ارسال متن یادداشت با هر برنامه‌ای |
| پشتیبان JSON | خروجی گرفتن از همهٔ یادداشت‌ها |
| تم | سیستم / روشن / تیره و ذخیرهٔ انتخاب |

## 📲 نصب

1. فایل [APK نسخهٔ ۱](https://github.com/havedays67-ai/yaddashty/releases/latest/download/yaddashty-v1.apk) را دانلود کنید.
2. اگر اندروید هشدار داد، اجازهٔ «نصب از منابع ناشناس» را برای مرورگر یا فایل‌منیجر فعال کنید.
3. فایل را باز کنید و نصب را تأیید کنید. اپلیکیشن روی اندروید ۸٫۰ (API 26) و بالاتر اجرا می‌شود.

> نسخهٔ فعلی **۱** است و هنوز منتشر نشده؛ تغییرات جدید همگی با همین نسخه در همان مخزن ساخته می‌شوند.

## 🧱 معماری

```
app/
├── MainActivity.kt              # میزبان Compose و ناوبری
├── data/
│   ├── Note.kt                  # موجودیت Room + DAO + دیتابیس + NoteRepository
│   └── AppContainer.kt          # کانتینر وابستگی + DataStore تنظیمات
└── ui/
    ├── NotesViewModel.kt        # StateFlow ها و عملیات
    ├── Format.kt                # تاریخ/عدد فارسی
    ├── theme/Theme.kt           # رنگ‌ها و تایپوگرافی
    └── screens/                 # فهرست، ویرایشگر، تنظیمات
```

شرح کامل لایه‌ها، مدل داده و تصمیم‌های طراحی در [ARCHITECTURE.md](ARCHITECTURE.md) آمده است. اپلیکیشن از الگوی **MVVM + Repository** استفاده می‌کند: UI (Compose) ← ViewModel (StateFlow) ← Repository ← Room/DataStore.

## 🛠 ساخت از سورس

```bash
# پیش‌نیاز: JDK 17 و Android SDK 35
gradle :app:assembleDebug
# خروجی: app/build/outputs/apk/debug/app-debug.apk
```

بیلد خودکار روی GitHub Actions انجام می‌شود و APK را در Releases و پوشهٔ [`releases/`](releases/) می‌گذارد.

## 🔐 حریم خصوصی

یادداشتی هیچ داده‌ای جمع‌آوری نمی‌کند، به اینترنت نیاز ندارد و مجوز خاصی از اندروید درخواست نمی‌کند. پشتیبان‌گیری فقط با انتخاب خود شما و از طریق اشتراک‌گذاری فایل انجام می‌شود.

## 🗺 نقشهٔ راه

- [ ] یادآور و آلارم برای یادداشت‌ها
- [ ] چک‌لیست داخل یادداشت
- [ ] قفل برنامه با اثر انگشت
- [ ] ویجت صفحهٔ اصلی
- [ ] ورود/خروجی Markdown

## 🤝 مشارکت

Issue و Pull Request پذیرفته می‌شود. لطفاً پیش از ارسال، بیلد را با `gradle :app:assembleDebug` و تست‌های موجود بررسی کنید.

## 📄 مجوز

این پروژه با مجوز [MIT](LICENSE) منتشر شده است.

</div>
